# D4 — Cycle de vie d'un exercice (bonus)

Correspond aux valeurs de `StatutExercice` (backend) et à la contrainte CHECK de `exercice.statut`.

**Mis à jour (C2, étape 3)** : jusqu'à **deux** relecteurs distincts par exercice (RG12 v2). L'état
`RELU` n'est atteint que lorsque **tous** les relecteurs assignés ont rendu leur note ; si un seul
des deux a rendu, l'exercice reste `EN_COURS_RELECTURE` et sa note est provisoire (RG18 v2).

```mermaid
stateDiagram-v2
    [*] --> SANS_RELECTEUR : dépôt, aucun autre étudiant présent (RG13)
    [*] --> EN_ATTENTE_RELECTURE : dépôt, un ou deux relecteurs tirés (RG12 v2)
    SANS_RELECTEUR --> EN_ATTENTE_RELECTURE : nouvelle présence permet un tirage (RG13)
    EN_ATTENTE_RELECTURE --> EN_COURS_RELECTURE : un relecteur ouvre l'exercice (H9)
    EN_COURS_RELECTURE --> EN_COURS_RELECTURE : un seul des deux a rendu -> note provisoire (RG18 v2)
    EN_ATTENTE_RELECTURE --> RELU : un seul relecteur assigné et il rend (pas de second candidat)
    EN_COURS_RELECTURE --> RELU : tous les relecteurs assignés ont rendu (EF9)
```

Le remplacement du lien (EF12) n'est possible qu'en `SANS_RELECTEUR` ou `EN_ATTENTE_RELECTURE`, et
avant clôture de la session (RG16, RG20). À la clôture, l'état est figé : tout ce qui n'est pas
`RELU` compte dans `exercicesEnAttente` du tableau formateur (H12).
