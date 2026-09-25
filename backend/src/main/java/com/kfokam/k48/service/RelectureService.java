package com.kfokam.k48.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.kfokam.k48.domain.Relecture;
import com.kfokam.k48.domain.StatutExercice;
import com.kfokam.k48.dto.RelectureDetailResponse;
import com.kfokam.k48.dto.RelectureResumeResponse;
import com.kfokam.k48.dto.RendreRelectureRequete;
import com.kfokam.k48.error.BusinessException;
import com.kfokam.k48.repository.EtudiantRepository;
import com.kfokam.k48.repository.RelectureRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * EF8 : liste et détail des relectures assignées. RG15 (seul le relecteur désigné),
 * RG16 (l'ouverture fige l'attribution), H9 (ouverte_at à la première ouverture).
 */
@Service
public class RelectureService {

    private final RelectureRepository relectureRepository;
    private final EtudiantRepository etudiantRepository;
    private final Clock clock;

    public RelectureService(RelectureRepository relectureRepository, EtudiantRepository etudiantRepository, Clock clock) {
        this.relectureRepository = relectureRepository;
        this.etudiantRepository = etudiantRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<RelectureResumeResponse> listerRelecturesAFaire(Long etudiantId) {
        if (!etudiantRepository.existsById(etudiantId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "ETUDIANT_INCONNU", "Cet étudiant n'existe pas.");
        }
        return relectureRepository.findByRelecteurIdAndRendueAtIsNull(etudiantId).stream()
                .map(RelectureResumeResponse::from)
                .toList();
    }

    @Transactional
    public RelectureDetailResponse obtenirRelecture(Long relectureId, Long etudiantIdAppelant) {
        Relecture relecture = relectureRepository.findById(relectureId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "RELECTURE_INCONNUE", "Cette relecture n'existe pas."));

        if (!relecture.getRelecteur().getId().equals(etudiantIdAppelant)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "RELECTEUR_NON_ASSIGNE",
                    "Vous n'êtes pas le relecteur assigné à cet exercice.");
        }

        if (relecture.getOuverteAt() == null) {
            relecture.setOuverteAt(clock.instant());
            if (relecture.getExercice().getStatut() == StatutExercice.EN_ATTENTE_RELECTURE) {
                relecture.getExercice().setStatut(StatutExercice.EN_COURS_RELECTURE);
            }
        }

        return RelectureDetailResponse.from(relecture);
    }

    /**
     * EF9 : rendre la relecture, définitive (RG14, C1). RG2 (jamais l'auteur), RG15 (relecteur désigné),
     * RG20 (session clôturée). RG3 : note entière 0-20, jamais arrondie.
     */
    @Transactional
    public void rendreRelecture(Long relectureId, Long etudiantIdAppelant, RendreRelectureRequete requete) {
        int note = validerNote(requete.note());

        Relecture relecture = relectureRepository.findById(relectureId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "RELECTURE_INCONNUE", "Cette relecture n'existe pas."));

        if (relecture.getExercice().getAuteur().getId().equals(etudiantIdAppelant)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "AUTO_RELECTURE", "Vous ne pouvez pas relire votre propre exercice.");
        }

        if (!relecture.getRelecteur().getId().equals(etudiantIdAppelant)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "RELECTEUR_NON_ASSIGNE",
                    "Vous n'êtes pas le relecteur assigné à cet exercice.");
        }

        if (relecture.getRendueAt() != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "RELECTURE_DEJA_RENDUE", "Cette relecture a déjà été rendue.");
        }

        if (relecture.getExercice().getSession().getClotureAt() != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "SESSION_CLOTUREE", "Cette session est clôturée.");
        }

        Instant maintenant = clock.instant();
        relecture.setNote(note);
        relecture.setCommentaire(requete.commentaire());
        relecture.setRendueAt(maintenant);
        if (relecture.getOuverteAt() == null) {
            // Rendue sans ouverture préalable : ouverte_at prend la valeur de rendue_at.
            relecture.setOuverteAt(maintenant);
        }

        // C2 (deux relecteurs) : RELU seulement quand TOUS les relecteurs assignés ont rendu.
        // Un seul rendu sur deux -> l'exercice reste EN_COURS_RELECTURE, sa note est provisoire (RG18 v2).
        boolean tousRendus = relectureRepository.findByExercice_Id(relecture.getExercice().getId()).stream()
                .allMatch(r -> r.getRendueAt() != null);
        if (tousRendus) {
            relecture.getExercice().setStatut(StatutExercice.RELU);
        } else {
            relecture.getExercice().setStatut(StatutExercice.EN_COURS_RELECTURE);
        }
    }

    private int validerNote(JsonNode note) {
        if (note == null || note.isNull() || !note.isIntegralNumber()) {
            throw noteInvalide();
        }
        int valeur = note.intValue();
        if (valeur < 0 || valeur > 20) {
            throw noteInvalide();
        }
        return valeur;
    }

    private BusinessException noteInvalide() {
        return new BusinessException(HttpStatus.BAD_REQUEST, "NOTE_INVALIDE", "La note doit être un entier compris entre 0 et 20.");
    }
}
