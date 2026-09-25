package com.kfokam.k48.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;

/**
 * `note` est capturé en JsonNode (pas Integer) pour distinguer nous-mêmes 12.5 ou "15" d'un entier
 * valide et renvoyer 400 NOTE_INVALIDE (RG3) plutôt que la 400 CHAMP_MANQUANT générique de Jackson.
 */
public record RendreRelectureRequete(JsonNode note, @NotBlank String commentaire) {
}
