# Taskora

Application de gestion de projets et de tâches (équipes, projets, tableau Kanban, sous-tâches, commentaires, tableau de bord, résumé IA de l'avancement). Stack : Angular 20 (frontend), Spring Boot 3 / Java 21 (backend), PostgreSQL, JWT, Docker Compose.

Ce document couvre uniquement l'installation et le lancement. Le détail des fonctionnalités et des choix techniques se trouve dans `taskora-specification.md`.

## Prérequis

### Option A — Tout via Docker (recommandé)

- Docker Engine + le plugin Docker Compose (`docker compose version`)

### Option B — Développement local (hot-reload)

- Java 21 et Maven 3.9+
- Node.js 20+ et npm
- Une base PostgreSQL 16 accessible (le plus simple : via Docker, voir plus bas)

## Configuration

Copier le fichier d'exemple et éditer les valeurs sensibles :

```bash
cp .env.example .env
```

| Variable | Rôle | Défaut si absente |
|---|---|---|
| `DB_NAME` / `DB_USER` / `DB_PASSWORD` | Base PostgreSQL | `taskora` / `taskora` / `taskora` |
| `JWT_SECRET` | Secret de signature des tokens JWT (256 bits minimum) | secret de dev, **à remplacer hors usage local** |
| `JWT_EXPIRATION_MS` | Durée de validité d'un token | `86400000` (24h) |
| `ANTHROPIC_API_KEY` | Clé API Anthropic, nécessaire uniquement pour le bouton "Résumé IA" | vide (le reste de l'app fonctionne sans) |
| `ANTHROPIC_MODEL` | Modèle Claude utilisé pour le résumé | `claude-haiku-4-5` |

Sans `ANTHROPIC_API_KEY`, toute l'application fonctionne normalement ; seul le résumé IA renverra un message d'erreur explicite.

## Option A — Lancement avec Docker Compose

Depuis la racine du projet :

```bash
docker compose up --build
```

Cela démarre trois services :

- `db` : PostgreSQL, exposé sur `localhost:5433`
- `backend` : API Spring Boot, exposée sur `localhost:8081`
- `frontend` : Angular buildé et servi par Nginx, exposé sur `localhost:4200`, qui proxy `/api` vers le service `backend`

Une fois les conteneurs démarrés, ouvrir **http://localhost:4200**.

Les ports hôte (`5433`, `8081`, `4200`) sont fixés directement dans `docker-compose.yml` (partie gauche du `"HOTE:CONTENEUR"`) ; si l'un d'eux est déjà utilisé sur votre machine, modifiez-le directement dans ce fichier.

Pour arrêter :

```bash
docker compose down
```

Pour réinitialiser aussi la base de données (supprime le volume) :

```bash
docker compose down -v
```

## Option B — Développement local

### 1. Démarrer une base PostgreSQL

Le plus simple, sans toucher au reste de la stack Docker Compose :

```bash
docker compose up -d db
```

(ou une instance PostgreSQL 16 locale avec les identifiants de `.env`, exposée sur le port `5433`.)

### 2. Lancer le backend

```bash
cd backend
SERVER_PORT=8090 mvn spring-boot:run
```

Le port `8090` est celui attendu par défaut par le frontend en mode développement (voir `frontend/src/environments/environment.ts`). Sans variables d'environnement supplémentaires, le backend utilise les valeurs par défaut de `application.yml` (base sur `localhost:5433`, identifiants `taskora`/`taskora`, secret JWT de dev). Pour tester le résumé IA en local, exporter `ANTHROPIC_API_KEY` avant de lancer la commande.

Vérifier que le backend répond :

```bash
curl http://localhost:8090/api/health
```

### 3. Lancer le frontend

```bash
cd frontend
npm install
ng serve
```

Ouvrir **http://localhost:4200**.

## Premier lancement

1. Ouvrir l'application et créer un compte (page d'inscription).
2. Créer une équipe, puis un projet dans cette équipe (page "Équipes").
3. Ouvrir le projet pour créer des tâches et les faire glisser entre les colonnes du tableau Kanban.
4. Cliquer sur une tâche pour voir son détail (sous-tâches, commentaires).
5. Le tableau de bord agrège automatiquement les indicateurs de tous les projets accessibles.

## Tests backend

```bash
cd backend
mvn test
```

Nécessite une base PostgreSQL accessible (voir étape 1 ci-dessus) pour les tests d'intégration.
