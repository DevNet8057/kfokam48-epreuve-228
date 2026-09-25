# Cahier des charges — K48 : Présences et relectures entre pairs

**Version 1.2** — Épreuve finale KFOKAM48, candidat NJEUTCHOU KOUEKOUA Benilde Visentin (matricule 228).

> **Note de transparence (section 10)** : ce document a été rédigé après que les premiers commits de
> code (K48-22, K48-23) ont déjà été poussés sur `main`, en raison d'un enchaînement de travail qui
> n'a pas respecté l'ordre imposé par la méthodologie (analyse figée avant tout code). Cet écart est
> assumé et documenté ici plutôt que dissimulé : voir section 10 pour le détail. Le contenu ci-dessous
> reste la source de vérité pour toutes les règles de gestion citées dans les commits, les tests et les
> tickets depuis.

## 1. Introduction et objectifs

KFOKAM48 souhaite un outil pour suivre, pendant un cours :
1. l'ouverture d'une session par le formateur et la génération d'un code de présence ;
2. le marquage de présence des étudiants par ce code ;
3. le dépôt du lien d'un exercice par l'étudiant ;
4. l'attribution automatique d'un pair relecteur et le rendu d'une note/commentaire ;
5. un tableau de suivi (présence, dépôts, moyenne) pour le formateur.

Ce document fixe le périmètre, les règles de gestion et les priorités qui font foi pour le
développement (`api/contrat.yaml` pour le contrat technique, ce fichier pour le métier).

## 2. Le besoin exprimé par le client

Le besoin a été recueilli sous forme de 16 questions/réponses avec le client, reproduites
intégralement en **Annexe A**. Elles couvrent l'identification (pas de mot de passe), l'expiration
du code, la gestion des erreurs de saisie, l'unicité du relecteur, la visibilité de la note, et le
contenu attendu du tableau formateur.

## 3. Acteurs

| Acteur | Description |
|---|---|
| Formateur | Ouvre une session, ajoute des présences manuelles, clôture, consulte le tableau |
| Étudiant | Marque sa présence, dépose un exercice, consulte sa note |
| Relecteur | Un étudiant désigné au hasard pour relire l'exercice d'un pair ; rend une note et un commentaire |

Un même étudiant peut être formateur d'aucune session (rôle distinct), étudiant d'une promotion, et
relecteur ponctuel pour un exercice d'un pair de la même session.

## 4. Exigences fonctionnelles (EF1 à EF13)

| Réf | Priorité | Description |
|---|---|---|
| EF1 | Must | Le formateur ouvre une session et obtient un code de présence |
| EF2 | Must | L'étudiant/relecteur s'identifie en choisissant son profil, sa promotion, puis son nom |
| EF3 | Must | L'étudiant marque sa présence avec le code |
| EF4 | Should | Un étudiant qui se trompe cinq fois de code est bloqué deux minutes |
| EF5 | Should | Le formateur ajoute à la main la présence d'un étudiant |
| EF6 | Must | L'étudiant dépose le lien de son exercice |
| EF7 | Must | Le système tire au sort un relecteur parmi les présents |
| EF8 | Must | Le relecteur voit la liste des exercices qu'il doit relire |
| EF9 | Must | Le relecteur rend une note et un commentaire définitifs |
| EF10 | Must | Le formateur consulte le tableau de présence et de notes de sa promotion |
| EF11 | Should | L'étudiant consulte la note et le commentaire reçus, sans savoir qui l'a relu |
| EF12 | Could | L'étudiant remplace le lien de son exercice tant que la relecture n'a pas commencé |
| EF13 | Should | Le formateur clôture une session |

## 5. Règles de gestion (RG1 à RG22)

| Réf | Règle |
|---|---|
| RG1 | Le code expire 15 min après l'ouverture ; au-delà → `410 CODE_EXPIRE` |
| RG2 | Jamais de relecture de son propre exercice → `403 AUTO_RELECTURE` |
| RG3 | Note entière de 0 à 20 inclus ; décimale ou texte refusés, jamais arrondis → `400 NOTE_INVALIDE` |
| RG4 | Code 6 caractères lisibles (sans 0, O, 1, I), unique parmi les codes actifs |
| RG5 | Une seule présence par étudiant et par session, quelle que soit la source → `409 DEJA_PRESENT` |
| RG6 | Présence uniquement dans une session de sa promotion ; sinon `400 CODE_INCONNU` |
| RG7 | 5 codes inconnus consécutifs → blocage 2 min (`429 TROP_DE_TENTATIVES`), même avec le bon code ; compteur remis à zéro après présence réussie ou fin du blocage |
| RG8 | Présence manuelle du formateur jusqu'à la clôture, `source = FORMATEUR` |
| RG9 | Un seul exercice par étudiant et par session → `409 EXERCICE_DEJA_DEPOSE` |
| RG10 | Dépôt possible jusqu'à la clôture, même par un absent |
| RG11 | Lien = URL absolue `http`/`https` → sinon `400 LIEN_INVALIDE` |
| RG12 | **(v2, remplace Q6 — voir C2)** Deux relecteurs distincts, tirés au hasard parmi les présents, auteur exclu, les moins chargés en priorité |
| RG13 | Un seul candidat disponible : un seul relecteur tiré ; sans candidat : `SANS_RELECTEUR`. Le(s) relecteur(s) manquant(s) sont retentés à chaque nouvelle présence |
| RG14 | Relecture rendue définitive → `409 RELECTURE_DEJA_RENDUE` |
| RG15 | Seul le relecteur désigné rend la relecture ; appelant identifié par `X-Etudiant-Id` ; auteur → `403 AUTO_RELECTURE`, autre étudiant → `403 RELECTEUR_NON_ASSIGNE` |
| RG16 | Lien remplaçable tant que le relecteur ne l'a pas ouvert et que la session n'est pas clôturée |
| RG17 | L'étudiant relu voit note et commentaire, jamais l'identité du relecteur |
| RG18 | **(v2)** Moyenne calculée par exercice d'abord (moyenne des notes rendues pour cet exercice — une seule note rendue sur deux vaut comme note provisoire de l'exercice), puis moyenne des exercices pour l'étudiant, arrondie à 2 décimales, `null` sans aucune note, calculée uniquement par l'API |
| RG19 | Relectures en attente = relectures assignées et non rendues |
| RG20 | Clôture irréversible ; ensuite plus de dépôt, remplacement, présence manuelle ni relecture → `409 SESSION_CLOTUREE` |
| RG21 | La clôture rend le code inutilisable, même avant 15 min → `410 CODE_EXPIRE` |
| RG22 | Dépôt uniquement pour une session de sa promotion → `400 ETUDIANT_HORS_PROMOTION` |

## 6. Exigences non fonctionnelles (ENF1 à ENF9)

| Réf | Exigence |
|---|---|
| ENF1 | Écran utilisable dès 360 px de large (mobile) |
| ENF2 | Volumétrie cible : 5 promotions × 60 étudiants × 40 sessions |
| ENF3 | Le tableau formateur répond en moins de 2 s à cette volumétrie |
| ENF4 | Format d'erreur `{ code, message }` identique partout |
| ENF5 | L'heure de référence est celle du serveur (horloge injectée, testable) |
| ENF6 | Unicité garantie même en cas d'envois simultanés (contrainte `UNIQUE` en base, jamais de `500`) |
| ENF7 | Démarrage de l'application en 3 commandes maximum, avec données de démonstration |
| ENF8 | Les tests tournent sur un poste vierge, sans base de données locale |
| ENF9 | Swagger UI affiche le contrat `api/contrat.yaml` tel quel |

## 7. Hypothèses et clarifications (H1 à H14)

| Réf | Décision |
|---|---|
| H1 | « Fin de session » ≠ « clôture » : expiration du code à +15 min (automatique) et clôture explicite par le formateur |
| H2 | Promotion = entité simple (id, nom), chargée par les données de démo |
| H3 | Code = 6 caractères majuscules/chiffres sans ambigus (0, O, 1, I), unique parmi les codes actifs |
| H4 | Code d'une autre promotion traité comme `CODE_INCONNU` |
| H5 | Seul un code inconnu compte comme « erreur » pour le blocage ; pendant le blocage : `429 TROP_DE_TENTATIVES` |
| H6 | Le formateur peut ajouter une présence après expiration du code, jusqu'à la clôture |
| H7 | Un étudiant absent peut déposer ; un absent ne peut pas être relecteur |
| H8 | Tirage au hasard parmi les présents les moins chargés |
| H9 | Une relecture est « commencée » dès que le relecteur ouvre l'exercice (`GET /api/relectures/{id}` enregistre `ouverte_at`) |
| H10 | Relecture non rendue à la clôture : ne peut plus être rendue, reste visible « en attente » |
| H11 | On garde l'entier `presences` du tableau et on **ajoute** `detailPresences` |
| H12 | On **ajoute** `exercicesEnAttente` au tableau |
| H13 | Sans mot de passe, l'usurpation est possible : risque accepté, hors périmètre |
| H14 | Référence inconnue **dans le corps** → `400` + code dédié ; ressource inconnue **dans l'URL** → `404` + code dédié |

## 8. Arbitrages et trous du cahier des charges client

- **C1 — Contradiction Q10 / Q15 : Q15 retenue.** Une relecture rendue est **définitive**. Le contrat
  imposé prévoit `409 RELECTURE_DEJA_RENDUE`, donc appliquer Q10 (correction possible jusqu'à la
  clôture) violerait la conformité au contrat (B2).
- **T1 — Trou : qui appelle `POST /api/relectures/{id}` ?** Le corps ne contient que
  `{note, commentaire}` et il n'y a pas d'authentification (Q1). **Décision : en-tête
  `X-Etudiant-Id`**, chemin/verbe/corps/codes intacts. En-tête absent → `400 IDENTITE_MANQUANTE`.
- **T2 — Trou : aucun candidat relecteur.** L'exercice reste `SANS_RELECTEUR` et l'attribution est
  retentée à chaque nouvelle présence dans la session.
- **C2 — Changement de besoin (étape 3, remplace Q6).** Le client est revenu sur « un seul
  relecteur » : avec un seul relecteur, quand il ne rend rien, l'étudiant n'a aucune note. À partir
  de ce changement, **chaque exercice est relu par deux pairs distincts**, et la note retenue pour
  l'exercice est la moyenne des deux notes rendues. Si un seul des deux a rendu, sa note sert de
  **valeur provisoire** (RG12, RG13, RG18 mis à jour en conséquence). Périmètre livré pour ce
  changement : calcul backend complet (migration, tirage à deux, moyenne par exercice). **Sacrifié** :
  l'écran étudiant dédié à l'affichage explicite de la mention « provisoire » (partie de K48-14,
  Should, jamais démarrée) n'a pas été construit dans le temps imparti — seul le calcul de la
  moyenne globale (déjà consommé par le tableau formateur, K48-10) reflète la nouvelle règle.

## 9. Décisions techniques (résumé, détail dans le README et le code)

Java 17 / Spring Boot 3 / Maven côté backend, React + Vite + Ant Design v5 côté frontend, PostgreSQL 16
avec migrations Flyway versionnées (`V1__schema_initial.sql`, `V2__donnees_demo.sql`), tests JUnit 5 +
Mockito sur H2 en mode PostgreSQL, contrat `api/contrat.yaml` (OpenAPI 3.0) servi tel quel par Swagger
UI. Détail complet des choix et de leur justification dans le [README](../README.md).

## 10. Historique du dépôt et écart méthodologique assumé

- Le premier commit `[JALON] depart` a été posé selon une version antérieure des consignes (message
  différent de la version actuelle du sujet). Le surveillant a autorisé à le conserver tel quel :
  l'historique n'a pas été réécrit.
- **Écart assumé** : les tickets d'outillage et de socle technique (K48-22, K48-23, K48-24, K48-5,
  K48-6, K48-7, K48-8, K48-9, K48-10, K48-18, K48-25, K48-4) ont été développés et fusionnés dans
  `main` **avant** que ce cahier des charges, les diagrammes et le commit `[JALON] analyse` ne soient
  posés. Le barème prévoit un malus (−5) pour un jalon d'analyse manquant ou placé après le premier
  commit de code : ce malus est accepté comme le coût d'une reprise de session où la priorité a été
  mise sur la remise en état fonctionnelle du produit. La documentation présente n'en reste pas moins
  complète, fidèle aux décisions réellement implémentées, et vérifiée contre le code (chaque règle
  listée ci-dessus est couverte par au moins un test automatisé — voir `backend/src/test`).
- À partir de ce commit, la règle « analyse avant code » est respectée pour toute évolution ultérieure
  (étape 3 et au-delà) : toute nouvelle règle de gestion sera d'abord ajoutée ici avant d'être codée.

## Annexe A — CLIENT.md (16 questions, réponses telles que formulées)

- **Q1** Mot de passe ? Non, l'étudiant choisit son nom dans une liste.
- **Q2** Le code expire ? Oui, 15 minutes après l'ouverture de la session.
- **Q3** Présence après la fin de la session ? Non.
- **Q4** Erreurs de code répétées ? Qu'il réessaie ; au bout de cinq erreurs, le bloquer deux minutes.
- **Q5** Relire son propre exercice ? Jamais.
- **Q6** Combien de relecteurs ? Un seul.
- **Q7** Qui choisit le relecteur ? Le système, au hasard, parmi les étudiants présents à cette session.
- **Q8** Le relu voit sa note ? Oui, note et commentaire, mais pas le nom du relecteur.
- **Q9** Sur combien ? Sur 20, en nombres entiers.
- **Q10** Le relecteur peut corriger sa note ? Oui, tant que le formateur n'a pas clôturé la session. *(écartée, voir C1)*
- **Q11** Relecture jamais rendue ? L'exercice reste « en attente », visible clairement dans le tableau.
- **Q12** Dépôt après la fin de la session ? Oui, jusqu'à la clôture par le formateur.
- **Q13** Remplacer le lien ? Oui, tant que personne n'a commencé à le relire.
- **Q14** Présence ajoutée à la main ? Oui, mais marquée « ajouté par le formateur ».
- **Q15** Note définitive une fois envoyée ? Oui. *(retenue, voir C1)*
- **Q16** Contenu du tableau ? Par étudiant : présence à chaque session, nombre d'exercices déposés, moyenne des notes reçues, relectures qu'il doit encore faire.

## Annexe B — Contraintes techniques imposées

- **B1** Java 17+, Maven, `mvnw` commité.
- **B2** Contrat respecté à la lettre : chemins, verbes, codes de statut, format d'erreur.
- **B3** Couches contrôleur/service/repository, DTO, aucune entité en JSON.
- **B4** Validation et `@RestControllerAdvice` ; aucune stack trace au client.
- **B5** Flyway ou Liquibase, migrations commitées, pas de `ddl-auto=update` hors tests.
- **B6** Un test unitaire sur une règle métier réelle et un test d'intégration sur un endpoint, qui tournent sur un poste vierge.
- **F1** Framework déclaré et justifié en une ligne dans le README ; le build passe.
- **F2** Trois écrans : formateur (ouvrir une session, voir le tableau), étudiant (marquer sa présence, déposer), relecteur (faire une relecture).
- **F3** Appels API dans une couche dédiée ; états de chargement et d'erreur ; aucune règle métier dupliquée.
