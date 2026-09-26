# Journal des modifications — K48

Format inspiré de [Keep a Changelog](https://keepachangelog.com/fr/). Les entrées reprennent les
tickets K48 fusionnés dans `main`, dans l'ordre chronologique des jalons.

## [JALON] v1.0 — Backlog complet (Should/Could + correctifs)

### Ajouté
- K48-11 : blocage de 2 minutes après 5 codes de présence inconnus consécutifs (RG7, H5).
- K48-12 : ajout manuel d'une présence par le formateur, avec nouvelle tentative d'attribution des
  relecteurs pour les exercices en attente (RG5, RG8, RG13).
- K48-13 : clôture irréversible d'une session par le formateur (RG20, RG21).
- K48-14 : consultation par l'étudiant de la note et du commentaire reçus, sans jamais exposer
  l'identité du relecteur (RG17).
- K48-15 : remplacement du lien d'un exercice par son auteur tant qu'aucune relecture n'a commencé
  (RG16, RG20).

### Corrigé
- K48-29 : sous H2 (tests), un exercice ne pouvait pas avoir deux relecteurs distincts après la
  migration V3, à cause d'un index unique résiduel propre à H2 ; migration V4 ajoutée (V3 non modifiée).
- K48-30 : le frontend ne pouvait appeler aucun endpoint de l'API depuis un vrai navigateur (préflight
  CORS bloqué) ; configuration CORS ajoutée et committée.

## [JALON] analyse / v0.1 — Socle et backlog Must

### Ajouté
- K48-22 : outillage du dépôt (hook pré-push, vérification locale, déclenchement CI sur PR).
- K48-23 : socle backend Spring Boot versionné (Flyway, gestion d'erreurs unique).
- K48-16/17/18/20 : cahier des charges, diagrammes D1-D4, contrat `api/contrat.yaml`, journal étape 1-2.
- K48-24/25 : socle frontend (3 espaces, couche `src/api/`), identification par profil sans mot de passe.
- K48-5 : ouverture de session et génération d'un code de présence.
- K48-6 : marquage de présence par l'étudiant avec son code.
- K48-7 : dépôt d'un exercice et tirage aléatoire d'un relecteur parmi les présents.
- K48-8 : liste et ouverture des relectures assignées.
- K48-9 : rendu définitif d'une note et d'un commentaire.
- K48-10 : tableau de présence et de notes du formateur.
- K48-4 : démarrage complet de l'application avec `docker compose up --build`.

### Corrigé
- Fins de ligne CRLF sur `mvnw` empêchant son exécution dans le conteneur Linux.
- Validation Hibernate du type `CHAR(6)` (`session_cours.code`) incompatible avec le `bpchar` réel de
  PostgreSQL.

## Étape 3 — Enveloppe (bug + évolution de périmètre)

### Corrigé
- K48-26 : le tableau formateur ne se rafraîchissait pas automatiquement après une nouvelle présence.

### Modifié
- K48-27 : passage à deux relecteurs distincts par exercice, note = moyenne des deux (remplace Q6),
  avec note provisoire tant qu'un seul des deux a rendu sa relecture (RG12 v2, RG13, RG18 v2, C2).
- K48-28 : cahier des charges restructuré selon les dix sections imposées par le sujet.
