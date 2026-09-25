package com.kfokam.k48.controller;

import com.kfokam.k48.dto.EtudiantResponse;
import com.kfokam.k48.dto.PromotionResponse;
import com.kfokam.k48.service.PromotionService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrat ajouté (K48-25) : GET /api/promotions, GET /api/promotions/{id}/etudiants.
 */
@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping
    public List<PromotionResponse> listerPromotions() {
        return promotionService.listerPromotions();
    }

    @GetMapping("/{id}/etudiants")
    public List<EtudiantResponse> listerEtudiantsDeLaPromotion(@PathVariable Long id) {
        return promotionService.listerEtudiantsDeLaPromotion(id);
    }
}
