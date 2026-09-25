# K48 — Présences et relectures entre pairs

Application pour KFOKAM48 : ouverture de session avec code de présence, marquage de présence,
dépôt d'exercice, relecture par les pairs et tableau de suivi du formateur.

## Choix techniques

- **Backend : Java 17, Spring Boot 3, Maven** — imposé par le sujet (B1). Couches
  `controller` / `service` / `repository` / `domain` / `dto` / `error` / `config`, migrations
  **Flyway** versionnées, tests **JUnit 5 + Mockito** sur **H2 en mode PostgreSQL** (aucune base
  locale nécessaire pour `./mvnw test`).
- **Frontend : React + Vite + Ant Design v5** — stack déjà maîtrisée, une seule page suffit à
  couvrir les trois espaces (formateur, étudiant, relecteur), et Ant Design fournit formulaires,
  tableaux et grille responsive (mobile 360 px, ENF1) sans temps passé sur le CSS.
- **Contrat d'API unique** : `api/contrat.yaml` (OpenAPI 3.0), affiché tel quel par Swagger UI —
  c'est la source de vérité, jamais une documentation générée après coup.

## Démarrer le projet (poste vierge)

Backend :

```bash
cd backend
./mvnw spring-boot:run
```

- API sur `http://localhost:8080`
- Contrat / Swagger UI sur `http://localhost:8080/swagger-ui.html`

Frontend, dans un second terminal :

```bash
cd frontend
npm install
npm run dev
```

- Application sur `http://localhost:5173`

> `docker compose up --build` (une seule commande, avec données de démonstration) arrive avec
> K48-4.

## Données de démonstration

Chargées par la migration `V2__donnees_demo.sql` : une promotion (`id=1`) et trois étudiants.

## Tests

```bash
cd backend
./mvnw test
```
