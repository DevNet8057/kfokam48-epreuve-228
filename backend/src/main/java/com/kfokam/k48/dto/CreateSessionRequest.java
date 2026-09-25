package com.kfokam.k48.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSessionRequest(
        @NotBlank String titre,
        @NotNull Long promotionId
) {
}
