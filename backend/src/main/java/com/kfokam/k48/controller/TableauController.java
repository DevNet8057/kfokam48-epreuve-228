package com.kfokam.k48.controller;

import com.kfokam.k48.dto.LigneTableauResponse;
import com.kfokam.k48.service.TableauService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrat imposé : GET /api/tableau?promotionId= (api/contrat.yaml).
 */
@RestController
public class TableauController {

    private final TableauService tableauService;

    public TableauController(TableauService tableauService) {
        this.tableauService = tableauService;
    }

    @GetMapping("/api/tableau")
    public List<LigneTableauResponse> obtenirTableau(@RequestParam Long promotionId) {
        return tableauService.obtenirTableau(promotionId);
    }
}
