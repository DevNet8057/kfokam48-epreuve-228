# CLAUDE.md — Règles de l'agent de développement (projet K48)

Ce fichier est lu par l'agent à chaque session. Il décrit **exactement** ce qu'il fait quand on lui donne un numéro de ticket (« fais K48-6 »). Si une règle de ce fichier contredit une demande, l'agent s'arrête et demande.

## 1. Sources de vérité

| Sujet | Source qui fait foi | Copie de suivi |
|---|---|---|
| Besoin, règles de gestion (EF, RG, H) | `docs/CAHIER_DES_CHARGES.md` | Confluence K48 · 01 |
| Contrat d'API | `api/contrat.yaml` | Confluence K48 · 04 |
| Modèle de données | `docs/diagrammes/D2_modele_donnees.md` + migrations Flyway | Confluence K48 · 03 et 08 |
| Travail à faire | Issue GitHub `[K48-n] …` (notée) | Ticket Jira K48-n (flux de travail) |

En cas d'écart entre GitHub et Jira/Confluence, **GitHub gagne**, et l'agent corrige la copie.

## 2. Prérequis

- `gh` authentifié sur le compte `DevNet8057` (`gh auth status`).
- Connecteur Atlassian (MCP) actif, site `jacquelinetagne.atlassian.net`, projet `K48`.
- Java 17, Node 18+, Docker.

## 3. Flux de statuts Jira

```
Backlog → À faire → Backend en cours → Frontend en cours → En revue → Prêt à tester → Terminé
                         ↑                  ↑                  │               │
                         └──── retour de revue (commentaire) ──┘               │
                                            └── bug de recette : ticket Bug lié, la story attend ──┘
```

| Statut | Qui le pose | Condition pour y entrer |
|---|---|---|
| À faire | Humain (planification du sprint) | Ticket prêt : critères « quand … alors … », EF/RG, estimation |
| Backend en cours | Agent | Branche créée, issue GitHub jumelle trouvée |
| Frontend en cours | Agent | Backend terminé : `./mvnw test` vert, commits poussés. Un ticket sans partie backend saute ce statut |
| En revue | Agent | `npm run build` vert, PR ouverte avec `Closes #n`, CI verte |
| Prêt à tester | Agent, **après accord humain** | PR fusionnée dans `main` |
| Terminé | **Humain uniquement** | Recette passée sur `main` (voir §7) |

**Correspondance colonne → statut Jira** (à utiliser pour les transitions) : « Backend en cours » → statut `En cours` ; les autres colonnes portent le même nom que leur statut (`À faire`, `Frontend en cours`, `En revue`, `Prêt à tester`, `Terminé`).

Un ticket bloqué garde son statut et reçoit le drapeau « Flagged » avec un commentaire qui dit pourquoi.

## 4. Procédure quand on me donne « K48-n »

1. **Lire** le ticket Jira K48-n : description, critères, liens « est bloqué par », commentaires. Si un ticket bloquant n'est pas au moins « Prêt à tester », **s'arrêter et le signaler**.
2. **Trouver l'issue GitHub jumelle** : `gh issue list --search "[K48-n] in:title" --state open`. Si elle n'existe pas, s'arrêter et le signaler (le backlog GitHub est noté, il doit exister avant le code).
3. **Lire la doc citée** par le ticket (RG, contrat, D2/D3/D4) avant d'écrire la moindre ligne.
4. **Créer la branche** depuis `main` à jour :
   `git switch main && git pull && git switch -c <type>/K48-n-<sujet-court>`
   Types : `feature/` (story), `chore/` (initialisation, outillage), `fix/` (bug).
5. **Jira → Backend en cours**, commentaire modèle A (§6).
6. **Backend** : service + repository + contrôleur + DTO + tests. Commits atomiques. `./mvnw test` doit passer. Pousser.
7. **Jira → Frontend en cours**, commentaire modèle B.
8. **Frontend** : écran avec **Ant Design**, appels via `src/api/` uniquement, états chargement / erreur / vide. `npm run build` doit passer. Pousser.
9. **Ouvrir la PR** : `gh pr create --base main --title "[K48-n] <titre de l'issue>" --body-file` (modèle `.github/pull_request_template.md`, avec `Closes #<numéro GitHub>`).
10. **Attendre la CI verte**, puis **Jira → En revue**, commentaire modèle C (lien PR + étapes pour tester).
11. **Me faire signe** : « K48-n prêt en revue : <lien PR>. Pour tester : … ». **S'arrêter là.**
12. **Après mon accord** (« OK K48-n » dans le chat, ou PR approuvée sur GitHub) :
    `gh pr merge <n> --merge --delete-branch` (**merge commit, jamais squash** : l'historique atomique est noté).
    Puis **Jira → Prêt à tester**, commentaire modèle D (hash du merge).
13. **Si je refuse** avec un commentaire : revenir au statut concerné, corriger sur la même branche, repousser, reprendre à l'étape 10.

## 5. Conventions Git

- Message de commit : `K48-n <verbe à l'impératif> <objet> (RGx)`. Exemple : `K48-6 Refuser un code expiré (RG1)`.
- Le **dernier commit** de la branche, ou la PR, contient `Closes #<numéro GitHub>`.
- Un commit = une idée. Jamais `update`, `fix`, `wip`, `test2`.
- `main` ne reçoit que des PR fusionnées. **Jamais de push direct sur `main`, jamais de `push --force` sur `main`.**
- Le préfixe `[JALON]` est réservé aux trois jalons, posés **par l'humain** uniquement.
- Dans chaque clone, exécuter `./scripts/enable-dev-hooks.ps1` : le hook pré-push lance `scripts/verify.sh`. Il exécute les tests Maven et le build React dès que les projets existent. Le workflow GitHub peut être lancé manuellement dès que les runners hébergés sont disponibles.

## 6. Modèles de commentaires Jira

**A · Démarrage** — `Démarrage. Branche : <nom>. Issue GitHub : #<n> <lien>. Règles concernées : RGx, RGy. Doc lue : <pages>.`

**B · Backend terminé** — `Backend terminé. Endpoints : <liste>. Tests : <noms> (verts). Commits : <hashs courts>. Écarts avec le ticket : <aucun | description>.`

**C · En revue** — `PR : <lien>. CI : verte. Pour tester : 1) … 2) … 3) …. Critères couverts : <liste>. Non couverts : <liste et raison>.`

**D · Fusionné** — `Fusionné dans main (<hash>). Issue GitHub #<n> fermée. En attente de recette.`

**E · Bloqué** — `Bloqué : <raison>. Il faut : <action ou décision attendue>.`

Chaque commentaire porte les liens utiles : issue GitHub, PR, page Confluence de la règle concernée.

## 7. Recette (Prêt à tester → Terminé)

Avant chaque jalon, l'humain teste **tous** les tickets « Prêt à tester » sur `main` démarré par `docker compose up --build` :
- chaque critère « quand … alors … » est rejoué, backend (Swagger UI) puis écran ;
- si tout passe : Jira → Terminé ;
- si un critère échoue : ticket **Bug** créé, lié à la story (« Blocks »), avec les étapes de reproduction ; la story reste en « Prêt à tester » jusqu'à la clôture du bug.

## 8. Interdits absolus

- Modifier une migration Flyway déjà commitée : toujours en **ajouter** une nouvelle.
- Modifier un chemin, un verbe, un corps ou un code de retour imposé par `api/contrat.yaml`.
- Exposer une entité JPA en JSON, appeler un repository depuis un contrôleur.
- Recalculer une règle métier dans le frontend (la moyenne vient de l'API).
- Commiter `target/`, `node_modules/`, `dist/`, `.env` ou un secret.
- Fusionner une PR sans accord humain, ou avec une CI rouge.
- Déplacer un ticket vers « Terminé ».

## 9. Commande spéciale : « synchronise le backlog »

Pour chaque ticket K48 (Story, Tâche, Bug ; pas les Epic) sans issue GitHub jumelle :
1. créer l'issue avec `gh issue create --title "[K48-n] <résumé>" --label <Must|Should|Could> [--label init|bug]` et le corps du ticket (critères, EF/RG, estimation) ;
2. commenter le ticket Jira : `Issue GitHub jumelle : #<n> <lien>`.
