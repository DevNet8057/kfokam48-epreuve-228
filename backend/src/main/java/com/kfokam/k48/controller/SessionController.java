package com.kfokam.k48.controller;

import com.kfokam.k48.dto.AjouterPresenceRequete;
import com.kfokam.k48.dto.CreateSessionRequest;
import com.kfokam.k48.dto.PresenceResponse;
import com.kfokam.k48.dto.SessionResponse;
import com.kfokam.k48.dto.SessionResumeResponse;
import com.kfokam.k48.service.PresenceService;
import com.kfokam.k48.service.SessionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrat imposé : POST /api/sessions. Ajoutés : GET /api/sessions?promotionId=,
 * POST /api/sessions/{id}/presences (api/contrat.yaml).
 */
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;
    private final PresenceService presenceService;

    public SessionController(SessionService sessionService, PresenceService presenceService) {
        this.sessionService = sessionService;
        this.presenceService = presenceService;
    }

    @PostMapping
    public ResponseEntity<SessionResponse> ouvrirSession(@Valid @RequestBody CreateSessionRequest requete) {
        SessionResponse reponse = sessionService.ouvrirSession(requete);
        return ResponseEntity.status(HttpStatus.CREATED).body(reponse);
    }

    @GetMapping
    public List<SessionResumeResponse> listerSessions(@RequestParam Long promotionId) {
        return sessionService.listerSessions(promotionId);
    }

    @PostMapping("/{id}/presences")
    public ResponseEntity<PresenceResponse> ajouterPresenceManuelle(
            @PathVariable Long id, @Valid @RequestBody AjouterPresenceRequete requete) {
        PresenceResponse reponse = presenceService.ajouterPresenceManuelle(id, requete.etudiantId());
        return ResponseEntity.status(HttpStatus.CREATED).body(reponse);
    }
}
