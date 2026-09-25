package com.kfokam.k48.controller;

import com.kfokam.k48.dto.RelectureResumeResponse;
import com.kfokam.k48.service.RelectureService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrat ajouté : GET /api/etudiants/{id}/relectures (EF8).
 */
@RestController
@RequestMapping("/api/etudiants")
public class EtudiantController {

    private final RelectureService relectureService;

    public EtudiantController(RelectureService relectureService) {
        this.relectureService = relectureService;
    }

    @GetMapping("/{id}/relectures")
    public List<RelectureResumeResponse> listerRelecturesAFaire(@PathVariable Long id) {
        return relectureService.listerRelecturesAFaire(id);
    }
}
