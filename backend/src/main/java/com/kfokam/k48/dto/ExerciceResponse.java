package com.kfokam.k48.dto;

import com.kfokam.k48.domain.Exercice;

public record ExerciceResponse(Long id, String statut) {
    public static ExerciceResponse from(Exercice exercice) {
        return new ExerciceResponse(exercice.getId(), exercice.getStatut().name());
    }
}
