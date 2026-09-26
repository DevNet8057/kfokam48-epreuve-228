# Journal de bord — K48

Format par étape : **Fait** / **Bloqué** (+ durée) / **IA** (+ vérification). Rédigé au fil de l'eau,
pas d'un bloc à la fin.

## Étape 1 — Analyse

**Fait** : besoin client recueilli (CLIENT.md, 16 Q/R), règles de gestion RG1-RG22, hypothèses H1-H14,
arbitrages C1/T1/T2, exigences EF1-13/ENF1-9 consolidées ; contrat `api/contrat.yaml` complété (5
opérations imposées + 9 ajoutées) et validé OpenAPI 3.0 (`redocly lint`) ; diagrammes D1 (cas
d'utilisation), D2 (modèle de données), D3 (séquence de présence), D4 (cycle de vie exercice, bonus).

**Bloqué** : l'ordre méthodologique n'a pas été respecté — les tickets d'outillage et de socle
(K48-22, K48-23) ont été codés et fusionnés avant que ce document n'existe. Repris et documenté a
posteriori (section 10 du cahier des charges). Durée de la reprise (rédaction + vérification croisée
avec le code déjà écrit) : ~45 min.

**IA** : l'agent a rédigé le cahier des charges et les diagrammes à partir du code et des tests déjà
en place (vérification : chaque RG citée correspond à un test automatisé existant dans
`backend/src/test`), plutôt que l'inverse. Vérification humaine : relecture de ce document avant
`[JALON] analyse`.

## Étape 2 — Version 0.1 (stories Must)

**Fait** : socle backend (Spring Boot 3, Flyway V1/V2, gestion d'erreurs unique, Jackson strict,
Swagger sur le contrat), socle frontend (React + Vite + Ant Design, 3 espaces, couche `src/api/`),
identification par profil (K48-25), puis les 7 stories Must : K48-5 (ouverture de session), K48-6
(marquer la présence), K48-7 (dépôt + tirage du relecteur), K48-8 (liste des relectures), K48-9
(rendre la relecture), K48-10 (tableau formateur), K48-4 (docker compose). Une branche et une PR par
ticket, fusion en merge commit, tests verts à chaque étape (30 tests unitaires/intégration au total).

**Bloqué** : le moteur Docker était injoignable au premier essai dans l'environnement d'exécution de
l'agent (Docker Desktop non démarré, puis contexte distant en timeout) ; résolu ~20 min plus tard.
Une fois Docker up, `docker compose up --build` a révélé deux bugs invisibles sur H2 :
1. `mvnw` commité avec des fins de ligne CRLF (checkout Windows) → `./mvnw: not found` dans le
   conteneur Linux. Corrigé par `.gitattributes` (`eol=lf`) + renormalisation du fichier.
2. Le type `CHAR(6)` de `session_cours.code` est rapporté par PostgreSQL réel sous le nom `bpchar`,
   que la validation de schéma Hibernate ne reconnaissait pas (H2 est plus tolérant et laissait
   passer) → `SchemaManagementException` au démarrage. Corrigé en remplaçant `columnDefinition` par
   `@JdbcTypeCode(SqlTypes.CHAR)`, qui compare le bon code JDBC plutôt qu'un texte.

**IA** : chaque story a été implémentée avec un test unitaire ou d'intégration ciblé sur sa règle de
gestion principale (ex. K48-7 : l'auteur n'est jamais tiré même seul présent ; K48-9 : une note
décimale ou textuelle est refusée sans arrondi). Vérification : `./mvnw test` et `npm run build`
rejoués avant chaque fusion, puis recette automatisée (appels HTTP réels) rejouée sur `main` après
chaque merge, et enfin `docker compose up --build` rejoué en réel (pas seulement en local) pour K48-4,
ce qui a permis de détecter les deux bugs ci-dessus qu'une recette purement H2 aurait laissés passer.

## Étape 3 — Enveloppe

**Fait** : bug client (« deux étudiants, un seul apparaît ») → issue #39 / K48-26 ouverte avant tout
code, test Vitest qui échoue (commit rouge `456de84`) puis correctif (rafraîchissement automatique du
tableau), PR #40. Changement de besoin (deux relecteurs, moyenne, note provisoire) → issue #41 /
K48-27 : analyse mise à jour d'abord (RG12, RG13, RG18, C2, D4), contrat documenté, migration V3
ajoutée, code, PR #42. Correctif et évolution sur deux branches et deux PR distinctes. Sacrifice
écrit : l'écran « note provisoire » (EF11) reporté à l'étape 4.

**Bloqué** : ~40 min sur le diagnostic du bug. Hypothèse initiale (course entre deux `POST
/api/presences`) réfutée par l'expérience : deux requêtes concurrentes réelles contre PostgreSQL
passent toutes les deux (201/201) et une double soumission donne bien 201 puis 409. La vraie cause
était l'absence de rafraîchissement du tableau formateur. ~30 min aussi sur la migration V3 : le nom
de la contrainte UNIQUE générée par V1 diffère entre H2 et PostgreSQL, d'où une migration Java qui le
retrouve dans `information_schema`.

**IA** : l'IA proposait d'emblée une correction de concurrence côté serveur. Vérifié en rejouant le
scénario en vrai (curl parallèles contre Docker/PostgreSQL, comptage en base) avant d'écrire une ligne :
l'hypothèse était fausse, ce qui a évité de « corriger » un code correct. Migration V3 vérifiée sur
les deux moteurs : tests H2 verts, puis `docker compose up` et contrôle des lignes `relecture` en base.

## Étape 4 — Version 1.0

**Fait** : les 5 stories Should/Could restantes enchaînées jusqu'à Terminé : K48-11 (blocage 2 min
après 5 codes inconnus, horloge déplacée en test), K48-12 (présence manuelle par le formateur +
nouvelle tentative d'attribution RG13), K48-13 (clôture de session, irréversible), K48-14 (note et
commentaire reçus, jamais l'identité du relecteur — RG17), K48-15 (remplacement du lien tant qu'aucune
relecture n'est ouverte — RG16/RG20). Chaque story : branche dédiée, tests unitaires ciblés sur sa
règle de gestion, `./mvnw test` et `npm run build` verts avant fusion, PR avec `Closes #n`, merge
commit, recette rejouée sur `main`, transitions Jira jusqu'à Terminé avec commentaire de preuve. Deux
bugs supplémentaires trouvés et corrigés par la même discipline rouge/vert que l'étape 3 :
- **K48-29** : sous H2 (tests), un exercice ne pouvait toujours pas avoir deux relecteurs après la
  migration V3, alors que PostgreSQL réel l'acceptait. Cause : H2 laisse vivre l'index unique
  `exercice_id` porté par la clé étrangère même après la suppression de la contrainte nommée. Migration
  Java V4 ajoutée (jamais de modification de V3), portable via `DatabaseMetaData` (no-op sur
  PostgreSQL, vérifié par `docker exec ... psql`).
- **K48-30** : en rejouant la recette de K48-15 dans un vrai navigateur (et non plus seulement en
  `curl`), tout appel de l'écran React vers l'API échouait (préflight CORS bloqué, 403). Un fichier de
  correctif (`WebConfiguration`) existait déjà sur le disque mais n'avait jamais été committé —
  retrouvé en fichier non suivi de `git status`. Issue #54 / K48-30 ouverte, test rouge (fichier
  temporairement retiré, 403 reproduit) puis vert (fichier restauré et committé, 60/60 tests, vérifié
  en navigateur réel).

**Bloqué** : aucun blocage technique notable sur les stories elles-mêmes. Le principal risque identifié
a posteriori est méthodologique : les bugs K48-29 et K48-30 n'ont été détectés qu'en poussant la
vérification au-delà des tests automatisés (respectivement un second moteur de base de données réel,
et un vrai navigateur plutôt que `curl`) — signe que la recette « tests verts » seule ne suffit pas à
garantir un produit qui fonctionne en conditions réelles.

**IA** : pour chacune des 5 stories, le test a été écrit pour la règle de gestion la plus stricte du
ticket (ex. K48-14 : assertion explicite qu'aucune chaîne « relecteur » n'apparaît dans le corps de la
réponse JSON). Les deux bugs K48-29/K48-30 ont été trouvés en continuant la méthode de l'étape 3
(vérifier en conditions réelles avant de déclarer une story terminée), pas par relecture de code seule.

## Étape 5 — Soumission

**Fait** : voir `SOUMISSION.md` à la racine pour l'état final (dépôt, commit, choix technique,
commande de démarrage).
