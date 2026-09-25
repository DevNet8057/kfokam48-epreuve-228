package com.kfokam.k48.dto;

import com.kfokam.k48.domain.Relecture;

/** Sans le nom de l'auteur (EF8). */
public record RelectureResumeResponse(Long id, Long exerciceId, String lien, String statutExercice) {
    public static RelectureResumeResponse from(Relecture relecture) {
        return new RelectureResumeResponse(
                relecture.getId(),
                relecture.getExercice().getId(),
                relecture.getExercice().getLien(),
                relecture.getExercice().getStatut().name());
    }
}
