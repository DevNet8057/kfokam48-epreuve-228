# D4 — Cycle de vie d'un exercice (bonus)

Correspond aux valeurs de `StatutExercice` (backend) et à la contrainte CHECK de `exercice.statut`.

```mermaid
stateDiagram-v2
    [*] --> SANS_RELECTEUR : dépôt, aucun autre étudiant présent (RG13)
    [*] --> EN_ATTENTE_RELECTURE : dépôt, relecteur tiré (RG12)
    SANS_RELECTEUR --> EN_ATTENTE_RELECTURE : nouvelle présence permet un tirage (RG13)
    EN_ATTENTE_RELECTURE --> EN_COURS_RELECTURE : le relecteur ouvre l'exercice (H9)
    EN_ATTENTE_RELECTURE --> RELU : relecture rendue sans ouverture préalable (ouverte_at = rendue_at)
    EN_COURS_RELECTURE --> RELU : note et commentaire rendus (EF9)
    RELU --> [*]
```

Le remplacement du lien (EF12) n'est possible qu'en `SANS_RELECTEUR` ou `EN_ATTENTE_RELECTURE`, et
avant clôture de la session (RG16, RG20). À la clôture, l'état est figé : tout ce qui n'est pas
`RELU` compte dans `exercicesEnAttente` du tableau formateur (H12).
