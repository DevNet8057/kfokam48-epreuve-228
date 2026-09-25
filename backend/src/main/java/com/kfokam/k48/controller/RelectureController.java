package com.kfokam.k48.controller;

import com.kfokam.k48.dto.RelectureDetailResponse;
import com.kfokam.k48.dto.RendreRelectureRequete;
import com.kfokam.k48.error.BusinessException;
import com.kfokam.k48.service.RelectureService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrat ajouté : GET /api/relectures/{id} (EF8). T1 : identité par X-Etudiant-Id.
 */
@RestController
@RequestMapping("/api/relectures")
public class RelectureController {

    private final RelectureService relectureService;

    public RelectureController(RelectureService relectureService) {
        this.relectureService = relectureService;
    }

    @GetMapping("/{id}")
    public RelectureDetailResponse obtenirRelecture(
            @PathVariable Long id,
            @RequestHeader(value = "X-Etudiant-Id", required = false) Long etudiantId) {
        if (etudiantId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "IDENTITE_MANQUANTE", "L'en-tête X-Etudiant-Id est requis.");
        }
        return relectureService.obtenirRelecture(id, etudiantId);
    }

    @PostMapping("/{id}")
    public ResponseEntity<Void> rendreRelecture(
            @PathVariable Long id,
            @RequestHeader(value = "X-Etudiant-Id", required = false) Long etudiantId,
            @Valid @RequestBody RendreRelectureRequete requete) {
        if (etudiantId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "IDENTITE_MANQUANTE", "L'en-tête X-Etudiant-Id est requis.");
        }
        relectureService.rendreRelecture(id, etudiantId, requete);
        return ResponseEntity.ok().build();
    }
}
