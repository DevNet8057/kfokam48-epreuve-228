package com.kfokam.k48.controller;

import com.kfokam.k48.dto.CreateSessionRequest;
import com.kfokam.k48.dto.SessionResponse;
import com.kfokam.k48.service.SessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrat imposé : POST /api/sessions (api/contrat.yaml).
 */
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<SessionResponse> ouvrirSession(@Valid @RequestBody CreateSessionRequest requete) {
        SessionResponse reponse = sessionService.ouvrirSession(requete);
        return ResponseEntity.status(HttpStatus.CREATED).body(reponse);
    }
}
