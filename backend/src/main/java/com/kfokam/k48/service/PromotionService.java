package com.kfokam.k48.service;

import com.kfokam.k48.dto.EtudiantResponse;
import com.kfokam.k48.dto.PromotionResponse;
import com.kfokam.k48.error.BusinessException;
import com.kfokam.k48.repository.EtudiantRepository;
import com.kfokam.k48.repository.PromotionRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * K48-25 : identification par profil (EF2). Pas de mot de passe (Q1) — l'étudiant/relecteur
 * choisit sa promotion puis son nom dans une liste.
 */
@Service
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final EtudiantRepository etudiantRepository;

    public PromotionService(PromotionRepository promotionRepository, EtudiantRepository etudiantRepository) {
        this.promotionRepository = promotionRepository;
        this.etudiantRepository = etudiantRepository;
    }

    @Transactional(readOnly = true)
    public List<PromotionResponse> listerPromotions() {
        return promotionRepository.findAll().stream().map(PromotionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<EtudiantResponse> listerEtudiantsDeLaPromotion(Long promotionId) {
        if (!promotionRepository.existsById(promotionId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "PROMOTION_INCONNUE", "Cette promotion n'existe pas.");
        }
        return etudiantRepository.findByPromotionIdOrderByNomAsc(promotionId).stream().map(EtudiantResponse::from).toList();
    }
}
