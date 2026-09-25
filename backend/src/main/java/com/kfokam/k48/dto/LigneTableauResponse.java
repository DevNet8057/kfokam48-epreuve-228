package com.kfokam.k48.dto;

import java.util.List;

public record LigneTableauResponse(
        Long etudiantId,
        String nom,
        int presences,
        int exercicesDeposes,
        Double moyenne,
        int relecturesEnAttente,
        List<DetailPresenceResponse> detailPresences,
        int exercicesEnAttente
) {
}
