package com.kfokam.k48.dto;

import com.kfokam.k48.domain.SessionCours;
import java.time.Instant;

/** Liste des sessions d'une promotion, côté formateur (présence manuelle, clôture). */
public record SessionResumeResponse(
        Long id,
        String titre,
        String code,
        Instant ouvertureAt,
        Instant expirationAt,
        Instant clotureAt
) {
    public static SessionResumeResponse from(SessionCours session) {
        return new SessionResumeResponse(session.getId(), session.getTitre(), session.getCode(),
                session.getOuvertureAt(), session.getExpirationAt(), session.getClotureAt());
    }
}
