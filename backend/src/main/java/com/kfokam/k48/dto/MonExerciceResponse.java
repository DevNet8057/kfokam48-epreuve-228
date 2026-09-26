package com.kfokam.k48.dto;

import java.util.List;

/**
 * EF11 : un exercice vu par son auteur. RG17 : aucun champ ne permet d'identifier les relecteurs.
 * RG18 v2 : note = moyenne des notes rendues ; provisoire tant que tous les relecteurs n'ont pas rendu.
 */
public record MonExerciceResponse(
        Long id,
        Long sessionId,
        String titreSession,
        String lien,
        String statut,
        Double note,
        boolean provisoire,
        List<String> commentaires
) {
}
