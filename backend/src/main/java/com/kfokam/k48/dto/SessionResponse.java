package com.kfokam.k48.dto;

import com.kfokam.k48.domain.SessionCours;
import java.time.Instant;

public record SessionResponse(
        Long id,
        String code,
        Instant ouvertureAt,
        Instant expirationAt
) {
    public static SessionResponse from(SessionCours session) {
        return new SessionResponse(session.getId(), session.getCode(), session.getOuvertureAt(), session.getExpirationAt());
    }
}
