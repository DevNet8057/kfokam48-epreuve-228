package com.kfokam.k48.dto;

import com.kfokam.k48.domain.Presence;

public record PresenceResponse(Long id, Long sessionId, Long etudiantId, String source) {
    public static PresenceResponse from(Presence presence) {
        return new PresenceResponse(
                presence.getId(),
                presence.getSession().getId(),
                presence.getEtudiant().getId(),
                presence.getSource().name());
    }
}
