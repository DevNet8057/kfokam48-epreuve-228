package com.kfokam.k48.controller;

import com.kfokam.k48.dto.MarquerPresenceRequete;
import com.kfokam.k48.dto.PresenceResponse;
import com.kfokam.k48.service.PresenceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrat imposé : POST /api/presences (api/contrat.yaml).
 */
@RestController
@RequestMapping("/api/presences")
public class PresenceController {

    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @PostMapping
    public ResponseEntity<PresenceResponse> marquerPresence(@Valid @RequestBody MarquerPresenceRequete requete) {
        PresenceResponse reponse = presenceService.marquerPresence(requete);
        return ResponseEntity.status(HttpStatus.CREATED).body(reponse);
    }
}
