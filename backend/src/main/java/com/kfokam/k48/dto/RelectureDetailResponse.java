package com.kfokam.k48.dto;

import com.kfokam.k48.domain.Relecture;
import java.time.Instant;

public record RelectureDetailResponse(
        Long id,
        Long exerciceId,
        String lien,
        Instant ouverteAt,
        Integer note,
        String commentaire,
        Instant rendueAt
) {
    public static RelectureDetailResponse from(Relecture relecture) {
        return new RelectureDetailResponse(
                relecture.getId(),
                relecture.getExercice().getId(),
                relecture.getExercice().getLien(),
                relecture.getOuverteAt(),
                relecture.getNote(),
                relecture.getCommentaire(),
                relecture.getRendueAt());
    }
}
