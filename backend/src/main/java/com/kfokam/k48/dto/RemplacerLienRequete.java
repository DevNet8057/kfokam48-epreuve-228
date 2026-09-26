package com.kfokam.k48.dto;

import jakarta.validation.constraints.NotBlank;

public record RemplacerLienRequete(@NotBlank String lien) {
}
