package com.kfokam.k48.service;

import com.kfokam.k48.domain.Relecture;
import com.kfokam.k48.domain.StatutExercice;
import com.kfokam.k48.dto.RelectureDetailResponse;
import com.kfokam.k48.dto.RelectureResumeResponse;
import com.kfokam.k48.error.BusinessException;
import com.kfokam.k48.repository.EtudiantRepository;
import com.kfokam.k48.repository.RelectureRepository;
import java.time.Clock;
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
}
