# D2 — Modèle de données

Correspond colonne par colonne aux migrations `V1__schema_initial.sql`, `V2__donnees_demo.sql` et
`db/migration/V3__DeuxRelecteurs.java`.

**Mis à jour après l'étape 3 (C2)** : un exercice a désormais jusqu'à **deux** lignes `relecture`
(une par relecteur). V3 a remplacé la contrainte `UNIQUE(exercice_id)` par
`UNIQUE(exercice_id, relecteur_id)`.

```mermaid
erDiagram
    PROMOTION ||--o{ ETUDIANT : "regroupe"
    PROMOTION ||--o{ SESSION_COURS : "planifie"
    SESSION_COURS ||--o{ PRESENCE : "enregistre"
    ETUDIANT ||--o{ PRESENCE : "marque"
    SESSION_COURS ||--o{ EXERCICE : "reçoit"
    ETUDIANT ||--o{ EXERCICE : "dépose (auteur)"
    EXERCICE ||--o{ RELECTURE : "est relu via (0 à 2)"
    ETUDIANT ||--o{ RELECTURE : "relit (relecteur)"
    ETUDIANT ||--o| TENTATIVE_CODE : "compte ses erreurs"

    PROMOTION {
        bigint id PK
        varchar nom "UNIQUE"
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
        char code "6 caractères"
        timestamptz ouverture_at
        timestamptz expiration_at
        timestamptz cloture_at "nullable"
    }
    PRESENCE {
        bigint id PK
        bigint session_id FK
        bigint etudiant_id FK
        varchar source "ETUDIANT | FORMATEUR"
        timestamptz marquee_at
    }
    EXERCICE {
        bigint id PK
        bigint session_id FK
        bigint auteur_id FK
        varchar lien
        varchar statut "SANS_RELECTEUR | EN_ATTENTE_RELECTURE | EN_COURS_RELECTURE | RELU"
        timestamptz depose_at
    }
    RELECTURE {
        bigint id PK
        bigint exercice_id FK
        bigint relecteur_id FK
        timestamptz ouverte_at "nullable"
        integer note "nullable, 0 à 20"
        text commentaire "nullable"
        timestamptz rendue_at "nullable"
    }
    TENTATIVE_CODE {
        bigint etudiant_id PK
        integer echecs_consecutifs
        timestamptz bloque_jusqu_a "nullable"
    }
```

Contraintes notables :

| Table | Contrainte | Règle |
|---|---|---|
| `presence` | `UNIQUE(session_id, etudiant_id)` | RG5, ENF6 |
| `exercice` | `UNIQUE(session_id, auteur_id)` | RG9 |
| `relecture` | `UNIQUE(exercice_id, relecteur_id)` (V3, remplace `UNIQUE(exercice_id)` de V1) | RG12 v2 |
| `relecture` | `CHECK (note IS NULL OR note BETWEEN 0 AND 20)` | RG3 |

Le relecteur n'est pas une entité séparée : c'est un `etudiant`.
