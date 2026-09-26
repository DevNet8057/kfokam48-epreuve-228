# Cahier des charges — K48 : présences et relectures entre pairs

Auteur : 228 · Version 1.3 · Frontend choisi : **React** (Vite + Ant Design), parce que c'est la stack
maîtrisée, qu'une application monopage suffit aux trois espaces, et qu'Ant Design fournit formulaires,
tableaux et grille responsive sans temps passé sur le CSS (non noté).

Historique des versions : v1.0 analyse initiale · v1.1 contrat complété · v1.2 changement de besoin
de l'étape 3 (C2, deux relecteurs) · v1.3 mise en conformité avec les dix sections imposées.

## 1. Contexte et objectif

La direction de la formation KFOKAM48 suit aujourd'hui la présence et les exercices à la main. Elle
veut une application qui permette :

1. à un formateur d'ouvrir une session de cours et d'obtenir un code de présence ;
2. à un étudiant de saisir ce code pour marquer sa présence ;
3. à un étudiant de déposer le lien de son exercice pour une session ;
4. à un étudiant d'être assigné à la relecture de l'exercice d'un pair (note et commentaire) ;
5. au formateur de voir un tableau : présence et moyenne des notes par étudiant.

Objectif : fiabiliser la présence (code qui expire, anti-devinette), rendre la relecture par les pairs
automatique et équitable, et donner au formateur une vue d'ensemble exacte, sans calcul manuel.

## 2. Acteurs et rôles

| Acteur | Ce qu'il peut faire |
|---|---|
| Formateur | Ouvrir une session et obtenir son code, ajouter une présence à la main, clôturer une session, consulter le tableau de sa promotion |
| Étudiant | S'identifier (promotion puis nom), marquer sa présence avec le code, déposer le lien de son exercice, remplacer ce lien tant que la relecture n'a pas commencé, consulter la note et le commentaire reçus |
| Relecteur | Un étudiant tiré au sort pour relire l'exercice d'un pair de la même session : voir les exercices à relire, rendre une note et un commentaire définitifs |
| Système | Générer le code, tirer les relecteurs au hasard, calculer les moyennes |

Un relecteur n'est pas un compte distinct : c'est un étudiant, dans un autre rôle.

## 3. Périmètre

**Inclus** : identification sans mot de passe (choix du nom dans une liste), sessions et codes de
présence, présence par code et présence manuelle, blocage après cinq erreurs, dépôt et remplacement de
lien, tirage de deux relecteurs, rendu de relecture, clôture de session, tableau formateur, note reçue
par l'étudiant, API REST documentée par `api/contrat.yaml`, démarrage en une commande avec données de
démonstration.

**Exclu** : authentification et gestion de mots de passe (Q1, risque d'usurpation accepté — H13),
création de promotions et d'étudiants depuis l'interface (chargés par migration), notifications
(mail, SMS), historique des modifications, export du tableau, tenue de plusieurs formateurs par
promotion, internationalisation.

## 4. Exigences fonctionnelles

| Réf | Exigence | Critère d'acceptation | Priorité |
|---|---|---|---|
| EF1 | Le formateur ouvre une session et obtient un code de présence | Quand j'envoie un titre et une promotion existante, alors je reçois `201 { id, code, ouvertureAt, expirationAt }` avec `expirationAt = ouvertureAt + 15 min` | Must |
| EF2 | L'étudiant ou le relecteur s'identifie en choisissant son nom | Quand je choisis mon profil, ma promotion puis mon nom, alors mon nom s'affiche dans l'en-tête et mes appels portent mon identité | Must |
| EF3 | L'étudiant marque sa présence avec le code | Quand je saisis un code valide et non expiré, alors ma présence apparaît dans le tableau du formateur ; code expiré → `410 CODE_EXPIRE` ; déjà présent → `409 DEJA_PRESENT` | Must |
| EF4 | Un étudiant qui se trompe cinq fois de code est bloqué deux minutes | Quand je saisis cinq codes inconnus d'affilée, alors toute tentative pendant deux minutes renvoie `429 TROP_DE_TENTATIVES`, même avec le bon code | Should |
| EF5 | Le formateur ajoute une présence à la main | Quand j'ajoute un étudiant à une session non clôturée, alors sa présence est créée avec `source = FORMATEUR` et le tableau affiche « ajoutée par le formateur » | Should |
| EF6 | L'étudiant dépose le lien de son exercice | Quand j'envoie une URL http(s) valide pour une session de ma promotion non clôturée, alors je reçois `201 { id, statut }` | Must |
| EF7 | Le système attribue des relecteurs au hasard | Quand j'ai déposé et qu'au moins un autre étudiant est présent, alors un ou deux relecteurs distincts sont tirés, jamais moi | Must |
| EF8 | Le relecteur voit les exercices qu'il doit relire | Quand je choisis mon nom côté relecteur, alors je vois les exercices qui me sont assignés et non rendus, sans le nom de l'auteur | Must |
| EF9 | Le relecteur rend une note et un commentaire définitifs | Quand j'envoie une note entière de 0 à 20 et un commentaire, alors je reçois `200` ; une seconde tentative renvoie `409 RELECTURE_DEJA_RENDUE` | Must |
| EF10 | Le formateur consulte le tableau de sa promotion | Quand je demande le tableau, alors j'obtiens une ligne par étudiant (même sans présence) avec présences, exercices déposés, moyenne et relectures en attente | Must |
| EF11 | L'étudiant consulte la note et le commentaire reçus | Quand mon exercice a été relu, alors je vois la note (provisoire si un seul relecteur sur deux a rendu) et les commentaires, jamais l'identité des relecteurs | Should |
| EF12 | L'étudiant remplace le lien de son exercice | Quand aucune relecture n'a commencé et que la session n'est pas clôturée, alors mon nouveau lien remplace l'ancien ; sinon `409 REMPLACEMENT_IMPOSSIBLE` | Could |
| EF13 | Le formateur clôture une session | Quand je clôture, alors la clôture est irréversible et bloque dépôts, remplacements, présences et relectures (`409 SESSION_CLOTUREE`) | Should |

## 5. Exigences non fonctionnelles

| Réf | Exigence | Comment on la vérifie |
|---|---|---|
| ENF1 | Écrans utilisables à 360 px de large (usage sur téléphone) | Affichage dans l'outil de développement du navigateur en 360 px : aucun défilement horizontal de page |
| ENF2 | Volumétrie cible : 5 promotions × 60 étudiants × 40 sessions | Le tableau est construit en un aller par table, sans requête par étudiant (lecture de `TableauService`) |
| ENF3 | Le tableau formateur répond en moins de 2 s à cette volumétrie | Mesure du temps de réponse de `GET /api/tableau` sur un jeu de données de cette taille |
| ENF4 | Toutes les erreurs au format `{ code, message }`, jamais de stack trace | `@RestControllerAdvice` unique ; tests qui vérifient le champ `code` des réponses d'erreur |
| ENF5 | L'heure de référence est celle du serveur, en UTC | `Clock` injecté ; tests avec `Clock.fixed` |
| ENF6 | Unicité garantie même en cas d'envois simultanés, jamais de `500` | Contraintes `UNIQUE` en base ; deux requêtes concurrentes rejouées → `201` puis `409` |
| ENF7 | Démarrage en trois commandes maximum, avec données de démonstration | `git clone`, `cd`, `docker compose up --build` testés depuis un clone vierge |
| ENF8 | Les tests tournent sur un poste vierge, sans base locale | `./mvnw test` sur H2 en mode PostgreSQL ; `npm test` sur jsdom |
| ENF9 | Swagger UI affiche le contrat `api/contrat.yaml` tel quel | Ouverture de `http://localhost:8080/swagger-ui.html` |

## 6. Règles de gestion

| Réf | Règle | Source |
|---|---|---|
| RG1 | Le code expire 15 min après l'ouverture de la session ; au-delà → `410 CODE_EXPIRE` | Q2 |
| RG2 | Un étudiant ne relit jamais son propre exercice → `403 AUTO_RELECTURE` | Q5 |
| RG3 | Note entière de 0 à 20 inclus ; décimale ou texte refusés, jamais arrondis → `400 NOTE_INVALIDE` | Q9 |
| RG4 | Code de 6 caractères lisibles (sans 0, O, 1, I), unique parmi les codes actifs | H3 |
| RG5 | Une seule présence par étudiant et par session, quelle que soit la source → `409 DEJA_PRESENT` | Contrat, Q14 |
| RG6 | Présence uniquement dans une session de sa promotion ; sinon `400 CODE_INCONNU` | H4 |
| RG7 | Cinq codes inconnus consécutifs → blocage deux minutes (`429 TROP_DE_TENTATIVES`), même avec le bon code ; compteur remis à zéro après une présence réussie ou la fin du blocage | Q4, H5 |
| RG8 | Présence manuelle possible jusqu'à la clôture, même code expiré, avec `source = FORMATEUR` | Q14, H6 |
| RG9 | Un seul exercice par étudiant et par session → `409 EXERCICE_DEJA_DEPOSE` | Contrat |
| RG10 | Dépôt possible jusqu'à la clôture, même par un absent | Q12, H7 |
| RG11 | Le lien est une URL absolue `http` ou `https` → sinon `400 LIEN_INVALIDE` | Contrat |
| RG12 | **(v2)** Deux relecteurs distincts par exercice, tirés au hasard parmi les présents de la session, auteur exclu, les moins chargés en priorité | Q7, enveloppe (C2, remplace Q6) |
| RG13 | Un seul candidat → un seul relecteur ; aucun → `SANS_RELECTEUR` ; les relecteurs manquants sont retentés à chaque nouvelle présence | T2 |
| RG14 | Une relecture rendue est définitive → `409 RELECTURE_DEJA_RENDUE` | Q15 (C1) |
| RG15 | Seul un relecteur désigné rend la relecture, identifié par `X-Etudiant-Id` ; auteur → `403 AUTO_RELECTURE` ; autre étudiant → `403 RELECTEUR_NON_ASSIGNE` | Q5, T1 |
| RG16 | Le lien est remplaçable tant qu'aucune relecture n'a commencé et que la session n'est pas clôturée | Q13, H9 |
| RG17 | L'étudiant relu voit note et commentaire, jamais l'identité de ses relecteurs | Q8 |
| RG18 | **(v2)** Note d'un exercice = moyenne des notes rendues pour cet exercice (une seule rendue sur deux = note provisoire) ; moyenne de l'étudiant = moyenne de ses exercices notés, arrondie à 2 décimales, `null` sans note, calculée uniquement par l'API | Q16, enveloppe (C2) |
| RG19 | Relectures en attente = relectures assignées et non rendues | Q16 |
| RG20 | Clôture irréversible ; ensuite plus de dépôt, remplacement, présence manuelle ni relecture → `409 SESSION_CLOTUREE` (remplacement : `409 REMPLACEMENT_IMPOSSIBLE`) | Q10, Q12, H1 |
| RG21 | La clôture rend le code inutilisable, même avant 15 min → `410 CODE_EXPIRE` | Q3, H1 |
| RG22 | Dépôt uniquement pour une session de sa promotion → `400 ETUDIANT_HORS_PROMOTION` | H4 |

## 7. Zones d'ombre, hypothèses et contradictions

| Point | Réponse client (Qx) ou hypothèse | Décision retenue | Pourquoi |
|---|---|---|---|
| **C1** — Correction d'une note | Q10 « oui, tant que la session n'est pas clôturée » contredit Q15 « non, c'est définitif » | **Q15 retenue** : relecture définitive (RG14) | Le contrat imposé prévoit `409 RELECTURE_DEJA_RENDUE` : appliquer Q10 violerait B2 |
| **T1** — Trou : qui appelle `POST /api/relectures/{id}` ? | Corps limité à `{ note, commentaire }`, pas d'authentification (Q1) | En-tête `X-Etudiant-Id` ; absent → `400 IDENTITE_MANQUANTE` | Seul moyen de vérifier RG2 et RG15 sans toucher au chemin, au verbe ni au corps imposés |
| **T2** — Aucun candidat relecteur | Non prévu (Q7 suppose des présents) | `SANS_RELECTEUR`, tirage retenté à chaque nouvelle présence (RG13) | L'exercice n'est jamais perdu et finit relu dès qu'un pair arrive |
| **C2** — Nombre de relecteurs (étape 3) | Q6 « un seul » remplacé par l'enveloppe : « deux pairs différents, moyenne des deux, provisoire si un seul a rendu » | Deux relecteurs (RG12 v2), note par exercice (RG18 v2), migration V3 | Nouveau besoin du client, prioritaire sur Q6 |
| H1 — « Fin de session » | Q3, Q12 | Expiration du code à +15 min, clôture explicite par le formateur | Q12 autorise le dépôt après la fin, jusqu'à la clôture : ce sont deux moments distincts |
| H2 — Promotion | Non précisé | Entité simple (id, nom), chargée par les données de démonstration | Suffit au tableau et à l'identification |
| H3 — Forme du code | Non précisé | 6 caractères majuscules/chiffres, sans 0, O, 1, I | Lisible au tableau, 32⁶ combinaisons |
| H4 — Code d'une autre promotion | Non précisé | Traité comme `CODE_INCONNU` | Ne révèle pas l'existence de sessions d'autres promotions |
| H5 — Ce qui compte comme erreur (Q4) | Q4 | Seul un code inconnu compte ; pendant le blocage → `429 TROP_DE_TENTATIVES` | Un code expiré ou une double présence ne sont pas des tentatives de devinette |
| H6 — Présence manuelle après expiration | Q14 | Autorisée jusqu'à la clôture | Q14 vise justement l'étudiant qui n'a pas pu saisir le code |
| H7 — Absent et relecture | Q7, Q12 | Un absent peut déposer, ne peut pas relire | Q7 limite les relecteurs aux présents |
| H8 — Équité du tirage | Q7 | Au hasard parmi les présents les moins chargés | Évite qu'un même étudiant ait toutes les relectures |
| H9 — « Commencer à relire » (Q13) | Q13 | Dès que le relecteur ouvre l'exercice (`ouverte_at`) | Seul événement observable avant le rendu |
| H10 — Relecture jamais rendue | Q11 | Reste « en attente », comptée dans `exercicesEnAttente` | Q11 : « je dois le voir clairement dans mon tableau » |
| H11 — Présence à chaque session (Q16) | Q16 | `presences` gardé (contrat) + `detailPresences` ajouté | Le contrat impose un entier, Q16 demande le détail |
| H12 — Exercices en attente | Q11 | Champ `exercicesEnAttente` ajouté au tableau | Rend Q11 visible sans modifier les champs imposés |
| H13 — Usurpation d'identité | Q1 | Risque accepté, hors périmètre | Q1 : « ne perdez pas de temps là-dessus » |
| H14 — Référence inconnue | Non précisé | Dans le corps → `400` + code dédié ; dans l'URL → `404` + code dédié | Convention REST cohérente avec le contrat |

## 8. Contraintes techniques

- **B1** Java 17 ou plus, Maven, wrapper `mvnw` commité.
- **B2** Contrat `api/contrat.yaml` respecté à la lettre : chemins, verbes, codes de statut, format d'erreur `{ code, message }`.
- **B3** Couches contrôleur / service / repository ; aucun accès base dans un contrôleur ; DTO, jamais d'entité JPA en JSON.
- **B4** Validation des entrées et `@RestControllerAdvice` unique ; aucune stack trace renvoyée.
- **B5** Schéma versionné par Flyway (V1 schéma, V2 démonstration, V3 deux relecteurs) ; `ddl-auto=validate`.
- **B6** Un test unitaire sur une règle métier réelle et un test d'intégration sur un endpoint, sur poste vierge.
- **F1** Framework déclaré et justifié dans le README ; le build passe.
- **F2** Trois écrans : formateur, étudiant, relecteur.
- **F3** Appels API dans `src/api/` uniquement ; états de chargement et d'erreur ; moyenne jamais recalculée côté frontend.
- **Démarrage** : `docker compose up --build`, données de démonstration chargées.

## 9. Livrables

| Livrable | Emplacement |
|---|---|
| Cahier des charges, journal | `docs/CAHIER_DES_CHARGES.md`, `docs/JOURNAL.md` |
| Diagrammes D1 à D4 (Mermaid) | `docs/diagrammes/` |
| Contrat d'API | `api/contrat.yaml` |
| Backend Spring Boot, migrations, tests | `backend/` |
| Frontend React, tests | `frontend/` |
| Démarrage | `docker-compose.yml`, `README.md` |
| Historique des versions | `CHANGELOG.md` |
| Backlog | Issues GitHub `[K48-n]` (copie de suivi Jira) |
| Soumission | `SOUMISSION.md` téléversé sur la plateforme |

## 10. Démarche prévue

1. **Analyse** : ce cahier des charges, D1 à D4, backlog en issues, contrat complété → `[JALON] analyse`.
2. **Version 0.1** : stories Must, une branche et une PR par issue, issue fermée par `Closes #n` → `[JALON] v0.1`.
3. **Enveloppe** : issue avant tout code, bug reproduit par un test qui échoue, migration ajoutée, contrat et analyse mis à jour dans un commit qui le dit, correctif et évolution sur deux branches.
4. **Version 1.0** : Should puis Could, `CHANGELOG.md`, README testé depuis un clone vierge, backlog trié → `[JALON] v1.0`.
5. **Soumission** : `SOUMISSION.md` avec le hash complet du dernier commit, lien vérifié en navigation privée.

**Definition of Done** : une issue est terminée quand ses critères « quand … alors … » sont couverts,
que `./mvnw test`, `npm test` et `npm run build` sont verts, que la PR qui la ferme (`Closes #n`) est
fusionnée dans `main`, et que le comportement a été rejoué sur l'application démarrée.

**Ordre de sacrifice en cas de retard** : EF12, puis EF4, EF11, EF5, EF13.

**Écarts constatés, assumés** :
- Le premier commit `[JALON] depart` suit une ancienne version des consignes ; le surveillant a autorisé
  à le conserver, l'historique n'a pas été réécrit.
- `[JALON] analyse` a été posé **après** les premiers commits de code (K48-22 à K48-10) : l'analyse a été
  formalisée a posteriori. L'historique n'a pas été réécrit pour le masquer (cela aurait exigé un
  `push --force` sur `main` et faussé l'ordre réel du travail).
- À l'étape 3, l'écran « note provisoire » (EF11) a été reporté faute de temps ; il est repris à l'étape 4.

## Annexe — CLIENT.md (questions et réponses du client)

- **Q1** Mot de passe ? Non, l'étudiant choisit son nom dans une liste.
- **Q2** Le code expire ? Oui, 15 minutes après l'ouverture de la session.
- **Q3** Présence après la fin de la session ? Non.
- **Q4** Erreurs de code répétées ? Qu'il réessaie ; au bout de cinq erreurs, le bloquer deux minutes.
- **Q5** Relire son propre exercice ? Jamais.
- **Q6** Combien de relecteurs ? Un seul. *(remplacée par C2 à l'étape 3)*
- **Q7** Qui choisit le relecteur ? Le système, au hasard, parmi les étudiants présents à cette session.
- **Q8** Le relu voit sa note ? Oui, note et commentaire, mais pas le nom du relecteur.
- **Q9** Sur combien ? Sur 20, en nombres entiers.
- **Q10** Le relecteur peut corriger sa note ? Oui, tant que la session n'est pas clôturée. *(écartée, C1)*
- **Q11** Relecture jamais rendue ? L'exercice reste « en attente », visible dans le tableau.
- **Q12** Dépôt après la fin de la session ? Oui, jusqu'à la clôture par le formateur.
- **Q13** Remplacer le lien ? Oui, tant que personne n'a commencé à le relire.
- **Q14** Présence ajoutée à la main ? Oui, marquée « ajouté par le formateur ».
- **Q15** Note définitive une fois envoyée ? Oui. *(retenue, C1)*
- **Q16** Contenu du tableau ? Par étudiant : présence à chaque session, exercices déposés, moyenne des notes reçues, relectures encore à faire.
