# D2 — Modèle de données

Doit correspondre colonne par colonne à `backend/src/main/resources/db/migration/V1__schema_initial.sql`.

```mermaid
erDiagram
    PROMOTION ||--o{ ETUDIANT : "regroupe"
    PROMOTION ||--o{ SESSION_COURS : "planifie"
    SESSION_COURS ||--o{ PRESENCE : "enregistre"
    ETUDIANT ||--o{ PRESENCE : "marque"
    SESSION_COURS ||--o{ EXERCICE : "reçoit"
    ETUDIANT ||--o{ EXERCICE : "dépose (auteur)"
    EXERCICE ||--o| RELECTURE : "est relu via"
    ETUDIANT ||--o{ RELECTURE : "relit (relecteur)"
    ETUDIANT ||--o| TENTATIVE_CODE : "compte ses erreurs"

    PROMOTION {
        bigint id PK
        varchar nom
    }
    ETUDIANT {
        bigint id PK
        varchar nom
        bigint promotion_id FK
    }
    SESSION_COURS {
        bigint id PK
        varchar titre
        bigint promotion_id FK
        char code
        timestamptz ouverture_at
        timestamptz expiration_at
        timestamptz cloture_at
    }
    PRESENCE {
        bigint id PK
        bigint session_id FK
        bigint etudiant_id FK
        varchar source
        timestamptz marquee_at
    }
    EXERCICE {
        bigint id PK
        bigint session_id FK
        bigint auteur_id FK
        varchar lien
        varchar statut
        timestamptz depose_at
    }
    RELECTURE {
        bigint id PK
        bigint exercice_id FK
        bigint relecteur_id FK
        timestamptz ouverte_at
        integer note
        text commentaire
        timestamptz rendue_at
    }
    TENTATIVE_CODE {
        bigint etudiant_id PK_FK
        integer echecs_consecutifs
        timestamptz bloque_jusqu_a
    }
```

Contraintes notables : `presence` a `UNIQUE(session_id, etudiant_id)` (RG5, ENF6) ; `exercice` a
`UNIQUE(session_id, auteur_id)` (RG9) ; `relecture.exercice_id` est `UNIQUE` (un seul relecteur par
exercice, RG12). Le relecteur n'est pas une entité séparée : c'est un `etudiant`.
