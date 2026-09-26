# D3 — Séquence : marquer sa présence (EF3, EF4)

Ordre des contrôles réellement implémenté dans `PresenceService.marquerPresence` (RG1, RG5, RG6,
RG7, RG21). Chaque réponse correspond au code HTTP du contrat `POST /api/presences`.

**Mis à jour à l'étape 4 (K48-11)** : le blocage après cinq codes inconnus (RG7) est vérifié
**avant** la recherche du code — sinon il ne protégerait rien — et seul un code inconnu compte
comme échec (H5).

```mermaid
sequenceDiagram
    actor E as Étudiant
    participant F as Front
    participant API as PresenceController
    participant S as PresenceService
    participant T as TentativeCodeService
    participant DB as Base

    E->>F: saisit le code
    F->>API: POST /api/presences { code, etudiantId }
    API->>S: marquerPresence(requete)
    alt champ manquant (validation)
        API-->>F: 400 { code: "CHAMP_MANQUANT" }
    end
    S->>DB: chercher l'étudiant
    S->>T: verifierNonBloque(etudiantId)
    alt bloqué depuis moins de 2 min (RG7)
        T-->>S: TROP_DE_TENTATIVES
        S-->>API: exception
        API-->>F: 429 { code: "TROP_DE_TENTATIVES" }
    end
    S->>DB: session par (code, promotion de l'étudiant)
    alt code inconnu ou d'une autre promotion (RG6, H4)
        S->>T: enregistrerEchec(etudiantId) — 5e échec : blocage 2 min
        S-->>API: exception
        API-->>F: 400 { code: "CODE_INCONNU" }
    else code expiré ou session clôturée (RG1, RG21)
        S-->>API: exception
        API-->>F: 410 { code: "CODE_EXPIRE" }
    else étudiant déjà présent (RG5)
        S-->>API: exception
        API-->>F: 409 { code: "DEJA_PRESENT" }
    else cas nominal
        S->>DB: INSERT presence (UNIQUE session/étudiant, ENF6)
        alt envoi simultané refusé par la contrainte UNIQUE
            DB-->>S: violation de contrainte
            API-->>F: 409 { code: "DEJA_PRESENT" } (jamais 500)
        else insertion réussie
            S->>T: reinitialiser(etudiantId)
            S-->>API: Presence
            API-->>F: 201 { id, sessionId, etudiantId, source: "ETUDIANT" }
        end
    end
```
