# Soumission — KFOKAM48

**Nom et prénom(s) :** NJEUTCHOU KOUEKOUA Benilde Visentin
**Matricule :** 228
**Centre :** Yaoundé

**Dépôt GitHub (public) :** https://github.com/DevNet8057/kfokam48-epreuve-228

**Commit final :** `d846dd829c01b9c3e266025faa1e0eb760376f9c` (`[JALON] v1.0`, branche `main`)

**Frontend choisi :** React (Vite + Ant Design v5) — stack déjà maîtrisée, une seule page suffit à
couvrir les trois espaces (formateur, étudiant, relecteur) et Ant Design fournit formulaires, tableaux
et grille responsive sans temps passé sur le CSS (voir `README.md`, section « Choix techniques »).

**Commande de démarrage :**

```bash
git clone https://github.com/DevNet8057/kfokam48-epreuve-228.git
cd kfokam48-epreuve-228
docker compose up --build
```

- Frontend : http://localhost:5173
- API + Swagger UI (contrat `api/contrat.yaml` affiché tel quel) : http://localhost:8080/swagger-ui.html
- Démarrage vérifié à neuf le 2026-09-26 depuis un clone propre (répertoire temporaire, sans état
  local préexistant) : build des deux images, migration Flyway (V1 à V4) appliquée automatiquement,
  60/60 tests backend verts, `npm run build` vert.

## État du backlog à la soumission

Tout le backlog non-Epic du projet Jira K48 est au statut **Terminé** : les 7 stories Must (K48-4 à
K48-10, K48-25), les 5 stories Should/Could (K48-11 à K48-15), l'enveloppe étape 3 (bug K48-26,
évolution K48-27), et les bugs trouvés en cours de route (K48-22 déclenchement CI, K48-29 index H2,
K48-30 CORS navigateur). Détail complet dans `CHANGELOG.md` et `docs/JOURNAL.md`.

## Jalons

`[JALON] depart` → `[JALON] analyse` → `[JALON] v0.1` → `[JALON] v1.0`, tous présents sur `main`.
Écart assumé et documenté (`docs/CAHIER_DES_CHARGES.md`, section 10) : `[JALON] analyse` a été posé
après les premiers commits de code (K48-22, K48-23), et non avant — malus accepté, expliqué, non
corrigible sans réécriture d'historique (interdite par le sujet).
