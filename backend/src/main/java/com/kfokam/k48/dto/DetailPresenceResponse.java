package com.kfokam.k48.dto;

/** H11 : présence à chaque session, avec la source si ajoutée par le formateur. */
public record DetailPresenceResponse(Long sessionId, String titre, boolean present, String source) {
}
