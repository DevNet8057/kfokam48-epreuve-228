package com.kfokam.k48.dto;

import com.kfokam.k48.domain.Etudiant;

public record EtudiantResponse(Long id, String nom, Long promotionId) {
    public static EtudiantResponse from(Etudiant etudiant) {
        return new EtudiantResponse(etudiant.getId(), etudiant.getNom(), etudiant.getPromotion().getId());
    }
}
