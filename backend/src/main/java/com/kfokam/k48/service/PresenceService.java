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
 * Ordre des contrôles conforme au diagramme de séquence D3 (handoff §6.5).
 * RG13 (attribution d'un relecteur) sera complétée avec K48-7, quand les exercices existeront.
 */
@Service
public class PresenceService {

    private final EtudiantRepository etudiantRepository;
    private final SessionCoursRepository sessionCoursRepository;
    private final PresenceRepository presenceRepository;
    private final Clock clock;

    public PresenceService(
            EtudiantRepository etudiantRepository,
            SessionCoursRepository sessionCoursRepository,
            PresenceRepository presenceRepository,
            Clock clock) {
        this.etudiantRepository = etudiantRepository;
        this.sessionCoursRepository = sessionCoursRepository;
        this.presenceRepository = presenceRepository;
        this.clock = clock;
    }

    @Transactional
    public PresenceResponse marquerPresence(MarquerPresenceRequete requete) {
        Etudiant etudiant = etudiantRepository.findById(requete.etudiantId())
                // Aucun code dédié imposé pour un étudiant inconnu sur cette opération (contrat) :
                // sans étudiant valide, la promotion ne peut pas être résolue -> CODE_INCONNU.
                .orElseThrow(this::codeInconnu);

        String code = requete.code().trim().toUpperCase();

        SessionCours session = sessionCoursRepository
                .findFirstByCodeAndPromotionIdOrderByOuvertureAtDesc(code, etudiant.getPromotion().getId())
                .orElseThrow(this::codeInconnu);

        Instant maintenant = clock.instant();
        if (session.getClotureAt() != null || maintenant.isAfter(session.getExpirationAt())) {
            throw new BusinessException(HttpStatus.GONE, "CODE_EXPIRE", "Le code de présence a expiré.");
        }

        if (presenceRepository.existsBySessionIdAndEtudiantId(session.getId(), etudiant.getId())) {
            throw dejaPresent();
        }

        try {
            Presence presence = presenceRepository.save(
                    new Presence(session, etudiant, SourcePresence.ETUDIANT, maintenant));
            return PresenceResponse.from(presence);
        } catch (DataIntegrityViolationException race) {
            // ENF6 : deux envois simultanés ne doivent jamais produire un 500.
            throw dejaPresent();
        }
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
