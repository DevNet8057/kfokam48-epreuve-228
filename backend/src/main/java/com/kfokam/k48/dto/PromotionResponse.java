package com.kfokam.k48.dto;

import com.kfokam.k48.domain.Promotion;

public record PromotionResponse(Long id, String nom) {
    public static PromotionResponse from(Promotion promotion) {
        return new PromotionResponse(promotion.getId(), promotion.getNom());
    }
}
