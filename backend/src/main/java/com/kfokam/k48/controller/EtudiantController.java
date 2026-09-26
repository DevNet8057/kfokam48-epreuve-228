package com.kfokam.k48.controller;

import com.kfokam.k48.dto.MonExerciceResponse;
import com.kfokam.k48.dto.RelectureResumeResponse;
import com.kfokam.k48.service.ExerciceService;
import com.kfokam.k48.service.RelectureService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrat ajouté : GET /api/etudiants/{id}/relectures (EF8), GET /api/etudiants/{id}/exercices (EF11).
 */
@RestController
@RequestMapping("/api/etudiants")
public class EtudiantController {

    private final RelectureService relectureService;
    private final ExerciceService exerciceService;

    public EtudiantController(RelectureService relectureService, ExerciceService exerciceService) {
        this.relectureService = relectureService;
        this.exerciceService = exerciceService;
    }

    @GetMapping("/{id}/relectures")
    public List<RelectureResumeResponse> listerRelecturesAFaire(@PathVariable Long id) {
        return relectureService.listerRelecturesAFaire(id);
    }

    @GetMapping("/{id}/exercices")
    public List<MonExerciceResponse> listerExercices(@PathVariable Long id) {
        return exerciceService.listerExercicesDeLEtudiant(id);
    }
}
