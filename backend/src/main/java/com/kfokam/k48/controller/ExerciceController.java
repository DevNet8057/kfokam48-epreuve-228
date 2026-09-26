package com.kfokam.k48.controller;

import com.kfokam.k48.dto.DeposerExerciceRequete;
import com.kfokam.k48.dto.ExerciceResponse;
import com.kfokam.k48.dto.RemplacerLienRequete;
import com.kfokam.k48.error.BusinessException;
import com.kfokam.k48.service.ExerciceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrat imposé : POST /api/exercices. Ajouté : PUT /api/exercices/{id} (EF12, api/contrat.yaml).
 */
@RestController
@RequestMapping("/api/exercices")
public class ExerciceController {

    private final ExerciceService exerciceService;

    public ExerciceController(ExerciceService exerciceService) {
        this.exerciceService = exerciceService;
    }

    @PostMapping
    public ResponseEntity<ExerciceResponse> deposerExercice(@Valid @RequestBody DeposerExerciceRequete requete) {
        ExerciceResponse reponse = exerciceService.deposerExercice(requete);
        return ResponseEntity.status(HttpStatus.CREATED).body(reponse);
    }

    @PutMapping("/{id}")
    public ExerciceResponse remplacerLien(
            @PathVariable Long id,
            @RequestHeader(value = "X-Etudiant-Id", required = false) Long etudiantId,
            @Valid @RequestBody RemplacerLienRequete requete) {
        if (etudiantId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "IDENTITE_MANQUANTE", "L'en-tête X-Etudiant-Id est requis.");
        }
        return exerciceService.remplacerLien(id, etudiantId, requete);
    }
}
