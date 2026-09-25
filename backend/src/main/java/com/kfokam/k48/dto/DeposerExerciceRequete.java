package com.kfokam.k48.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeposerExerciceRequete(
        @NotNull Long sessionId,
        @NotNull Long etudiantId,
        @NotBlank String lien
) {
}
