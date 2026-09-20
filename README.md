# Taskora

Application de gestion de projets et de tâches (équipes, projets, tableau Kanban, sous-tâches, commentaires, tableau de bord, résumé IA de l'avancement). Stack : Angular 20 (frontend), Spring Boot 3 / Java 21 (backend), PostgreSQL, JWT, Docker Compose.

Ce document couvre l'installation, le lancement, l'architecture et les choix techniques. Le détail fonctionnel complet (cahier des charges) se trouve dans `taskora-specification.md`.

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

## Architecture

### Vue d'ensemble

Une API REST (backend) consommée par une SPA (frontend), toutes deux stateless côté serveur : l'état de session vit uniquement dans le token JWT porté par le client. Communication en JSON sous `/api`, une seule base PostgreSQL partagée.

```
Angular (SPA, port 4200)  --HTTP/JSON-->  Spring Boot (API REST, port 8080)  --JDBC-->  PostgreSQL
        |                                          |
   JWT en localStorage                    JwtAuthenticationFilter (stateless)
```

### Backend (Spring Boot)

Organisation **par domaine métier** (package par fonctionnalité) plutôt que par couche technique : `auth`, `user`, `team`, `project`, `task` (tasks + subtasks + comments), `activity`, `dashboard`, `ai`, `security`, `exception`. Chaque domaine suit le même schéma interne : `Controller` (mapping HTTP) → `Service` (logique métier, transactions) → `Repository` (Spring Data JPA), avec des DTO (records Java) en entrée/sortie — les entités JPA ne sont jamais exposées directement dans les réponses HTTP.

Points structurants :

- **Autorisation par appartenance d'équipe** plutôt que par rôles Spring Security globaux : un utilisateur peut être `ADMIN` d'une équipe et simple `MEMBER` d'une autre, donc les droits ne peuvent pas être portés par un rôle unique sur le principal. `TeamAccessService.requireMembership(...)` centralise cette vérification et est réutilisé par tous les autres services (projets, tâches, sous-tâches, commentaires, journal d'activité, résumé IA, tableau de bord) pour éviter de la dupliquer.
- **Sécurité JWT stateless** : `JwtService` signe/valide les tokens (HS256), `JwtAuthenticationFilter` lit l'en-tête `Authorization` sur chaque requête, `SecurityConfig` force `SessionCreationPolicy.STATELESS` (aucune session serveur). Mots de passe hashés en BCrypt.
- **Journal d'activité** alimenté explicitement : `ActivityLogService.log(...)` est appelé depuis les services métier (projet créé/modifié, tâche créée/modifiée/déplacée/supprimée, commentaire ajouté) plutôt que via des événements Spring ou de l'AOP — plus direct à suivre pour la taille du projet.
- **Sous-tâches et commentaires sans endpoint de listage dédié** : ils sont embarqués directement dans `TaskResponse`, pour afficher le détail complet d'une tâche en un seul aller-retour réseau.
- **Erreurs centralisées** : `GlobalExceptionHandler` (`@RestControllerAdvice`) traduit chaque exception métier (`ResourceNotFoundException`, `ForbiddenActionException`, `ConflictException`, `AiServiceException`, etc.) en code HTTP et message JSON cohérents.
- **Intégration IA** : `AiSummaryService` utilise le SDK officiel `com.anthropic:anthropic-java` (pas d'appel HTTP fait main), construit un prompt à partir de l'état réel des tâches du projet, et mappe chaque famille d'erreur du SDK (quota, timeout, erreur serveur) vers une réponse HTTP 503 avec un message explicite.
- **Schéma de base versionné par Flyway** (`V1__init_schema.sql`, pas de `ddl-auto: update`) : 8 tables — `users`, `teams`, `team_members`, `projects`, `tasks`, `subtasks`, `comments`, `activity_logs` — avec contraintes `CHECK` pour les enums et `ON DELETE CASCADE`/`SET NULL` selon la sémantique métier de chaque relation.

### Frontend (Angular)

Standalone components uniquement (pas de `NgModule`), organisés en trois dossiers :

- `core/` : un service HTTP par ressource (`auth`, `team`, `project`, `task`, `activity`, `dashboard`), les modèles TypeScript alignés sur les DTO backend, un guard de route (`authGuard`) et un intercepteur HTTP (`authInterceptor`) qui ajoute le token JWT à chaque requête sortante.
- `features/` : une page par route (`auth/login`, `auth/register`, `dashboard`, `teams`, `projects/project-detail` + sa modale `task-detail-modal`), chargées en lazy-loading via `loadComponent`.
- `shared/` : composants réutilisés entre pages (`TopNav`).

Autres choix notables :

- **Gestion d'état via les signals natifs Angular** (`signal`/`computed`/`effect`) directement dans chaque composant — pas de librairie de state management (NgRx, Akita...), le périmètre de l'application (formulaires + listes chargées par page) ne le justifie pas.
- **Kanban** : `@angular/cdk/drag-drop` pour le glisser-déposer entre colonnes, avec mise à jour optimiste de l'affichage et retour en arrière automatique si l'appel `PATCH /tasks/{id}/status` échoue côté serveur.
- **Authentification côté client** : token JWT + informations utilisateur en `localStorage` (`AuthService`) ; pas de refresh token, la session expire avec le token (`JWT_EXPIRATION_MS`).
- **Un seul jeu de styles CSS partagés** (`styles.css`, variables CSS pour couleurs/rayons) plutôt qu'un framework UI complet — cohérent avec un projet de démonstration où la vitesse d'itération prime sur un design system élaboré.

### Déploiement

Docker Compose à 3 services (`db` / `backend` / `frontend`). Le frontend est buildé en production et servi par Nginx, qui proxy `/api` vers le service `backend` : un seul point d'entrée HTTP côté client et pas de CORS à gérer en production (le CORS n'est nécessaire qu'en développement local, où Angular et Spring tournent sur des ports différents).

## Choix techniques

| Choix | Pourquoi |
|---|---|
| Angular 20 (standalone + signals) | Dernière version stable au démarrage du projet ; les signals réduisent le boilerplate RxJS pour de l'état local simple, sans dépendance supplémentaire. |
| Spring Boot 3 / Java 21 | Écosystème mature et productif (Spring Data JPA, Spring Security, Flyway) pour une API REST classique. |
| PostgreSQL | SGBD relationnel robuste, adapté au modèle très relationnel du domaine (équipes → projets → tâches → sous-tâches/commentaires). |
| JWT stateless plutôt que sessions | Pas de store de session à partager entre instances ; adapté à une SPA qui consomme une API REST sans état serveur. |
| Autorisation par appartenance d'équipe (pas de rôles Spring Security globaux) | Reflète le vrai modèle métier : un rôle est toujours relatif à une équipe (`TeamRole`), jamais global à l'utilisateur. |
| SDK officiel `anthropic-java` plutôt qu'appel HTTP manuel | Moins de code à maintenir ; erreurs (quota, timeout, 5xx) déjà typées par le SDK plutôt qu'à re-parser depuis des réponses HTTP brutes. |
| Modèle `claude-haiku-4-5` par défaut (configurable via `ANTHROPIC_MODEL`) | Le résumé de projet est une tâche courte et peu exigeante ; un modèle plus gros n'apporterait pas de valeur proportionnelle au coût pour ce cas d'usage. |
| Docker Compose plutôt qu'un orchestrateur (Kubernetes...) | Trois services suffisent pour une démo et un déploiement simple ; un orchestrateur plus lourd serait disproportionné. |
| Tests unitaires Mockito ciblés (pas d'intégration MockMvc/Testcontainers systématique) | Couvre l'exigence minimale de la spec (authentification, création/modification de tâche, appel IA mocké) sans alourdir la CI ; `BackendApplicationTests` reste le seul test d'intégration (chargement du contexte + migration Flyway réelle) pour valider que tout se câble correctement. |
