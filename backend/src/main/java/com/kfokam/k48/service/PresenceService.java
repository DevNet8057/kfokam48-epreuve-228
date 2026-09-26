package com.kfokam.k48.service;

import com.kfokam.k48.domain.Etudiant;
import com.kfokam.k48.domain.Presence;
import com.kfokam.k48.domain.SessionCours;
import com.kfokam.k48.domain.SourcePresence;
import com.kfokam.k48.dto.MarquerPresenceRequete;
import com.kfokam.k48.dto.PresenceResponse;
import com.kfokam.k48.error.BusinessException;
import com.kfokam.k48.repository.EtudiantRepository;
import com.kfokam.k48.repository.PresenceRepository;
import com.kfokam.k48.repository.SessionCoursRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * EF3 : l'étudiant marque sa présence avec le code. Règles RG1 (expiration), RG5 (une seule
 * présence), RG6/H4 (code de sa promotion uniquement), RG21 (clôture rend le code inutilisable).
 * RG7 (K48-11) : le blocage est vérifié avant la recherche du code, sinon il ne protège rien ;
 * seul un code inconnu compte comme échec (H5). Ordre des contrôles conforme au diagramme D3.
 */
@Service
public class PresenceService {

    private final EtudiantRepository etudiantRepository;
    private final SessionCoursRepository sessionCoursRepository;
    private final PresenceRepository presenceRepository;
    private final TentativeCodeService tentativeCodeService;
    private final AttributionService attributionService;
    private final Clock clock;

    public PresenceService(
            EtudiantRepository etudiantRepository,
            SessionCoursRepository sessionCoursRepository,
            PresenceRepository presenceRepository,
            TentativeCodeService tentativeCodeService,
            AttributionService attributionService,
            Clock clock) {
        this.etudiantRepository = etudiantRepository;
        this.sessionCoursRepository = sessionCoursRepository;
        this.presenceRepository = presenceRepository;
        this.tentativeCodeService = tentativeCodeService;
        this.attributionService = attributionService;
        this.clock = clock;
    }

    @Transactional
    public PresenceResponse marquerPresence(MarquerPresenceRequete requete) {
        Etudiant etudiant = etudiantRepository.findById(requete.etudiantId())
                // Aucun code dédié imposé pour un étudiant inconnu sur cette opération (contrat) :
                // sans étudiant valide, la promotion ne peut pas être résolue -> CODE_INCONNU.
                .orElseThrow(this::codeInconnu);

        tentativeCodeService.verifierNonBloque(etudiant.getId());

        String code = requete.code().trim().toUpperCase();

        SessionCours session = sessionCoursRepository
                .findFirstByCodeAndPromotionIdOrderByOuvertureAtDesc(code, etudiant.getPromotion().getId())
                .orElse(null);
        if (session == null) {
            tentativeCodeService.enregistrerEchec(etudiant.getId());
            throw codeInconnu();
        }

        Instant maintenant = clock.instant();
        if (session.getClotureAt() != null || maintenant.isAfter(session.getExpirationAt())) {
            throw new BusinessException(HttpStatus.GONE, "CODE_EXPIRE", "Le code de présence a expiré.");
        }

        if (presenceRepository.existsBySessionIdAndEtudiantId(session.getId(), etudiant.getId())) {
            throw dejaPresent();
        }

        Presence presence;
        try {
            presence = presenceRepository.save(new Presence(session, etudiant, SourcePresence.ETUDIANT, maintenant));
        } catch (DataIntegrityViolationException race) {
            // ENF6 : deux envois simultanés ne doivent jamais produire un 500.
            throw dejaPresent();
        }
        tentativeCodeService.reinitialiser(etudiant.getId());
        attributionService.retenterAttribution(session);
        return PresenceResponse.from(presence);
    }

    /**
     * EF5 : le formateur ajoute une présence à la main. RG8 : jusqu'à la clôture, même code expiré
     * (H6), avec source = FORMATEUR (Q14). RG5 : une seule présence. RG13 : attribution retentée.
     */
    @Transactional
    public PresenceResponse ajouterPresenceManuelle(Long sessionId, Long etudiantId) {
        SessionCours session = sessionCoursRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "SESSION_INCONNUE", "Cette session n'existe pas."));

        Etudiant etudiant = etudiantRepository.findById(etudiantId)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "ETUDIANT_INCONNU", "Cet étudiant n'existe pas."));

        if (!session.getPromotion().getId().equals(etudiant.getPromotion().getId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "ETUDIANT_HORS_PROMOTION",
                    "Cet étudiant n'appartient pas à la promotion de la session.");
        }

        if (session.getClotureAt() != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "SESSION_CLOTUREE", "Cette session est clôturée.");
        }

        if (presenceRepository.existsBySessionIdAndEtudiantId(session.getId(), etudiant.getId())) {
            throw dejaPresent();
        }

        Presence presence;
        try {
            presence = presenceRepository.save(new Presence(session, etudiant, SourcePresence.FORMATEUR, clock.instant()));
        } catch (DataIntegrityViolationException race) {
            throw dejaPresent();
        }
        attributionService.retenterAttribution(session);
        return PresenceResponse.from(presence);
    }

    private BusinessException codeInconnu() {
        return new BusinessException(HttpStatus.BAD_REQUEST, "CODE_INCONNU",
                "Ce code ne correspond à aucune session active de votre promotion.");
    }

    private BusinessException dejaPresent() {
        return new BusinessException(HttpStatus.CONFLICT, "DEJA_PRESENT",
                "Vous avez déjà marqué votre présence à cette session.");
    }
}
