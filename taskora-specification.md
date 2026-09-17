# Spécification — Taskora
### SaaS de gestion de projets/tâches avec copilote IA

**Auteur :** Maryam Helfa
**Contexte :** Projet de démonstration pour candidature stage PFE — Inspirigence
**Méthode :** Vibe coding avec Claude Code (spécification → génération → relecture → tests → déploiement)

---

## 1. Objectif du projet

Construire une application web de gestion de projets et de tâches (type Trello/Linear simplifié), destinée à des équipes, avec une fonctionnalité d'intelligence artificielle intégrée : un résumé automatique de l'avancement d'un projet généré par LLM.

L'objectif n'est pas de tout construire d'un coup, mais de suivre un développement incrémental et maîtrisé : chaque brique est spécifiée, générée avec Claude Code, relue, testée, puis validée avant de passer à la suivante.

---

## 2. Stack technique

| Composant | Choix |
|---|---|
| Frontend | Angular (dernière version stable) |
| Backend | Spring Boot (Java) |
| Base de données | PostgreSQL |
| Authentification | JWT (email/mot de passe) |
| Conteneurisation | Docker + docker-compose |
| IA | API Claude (Anthropic) |
| Hébergement cible | Railway / Render / VPS (à définir en phase de déploiement) |
| Tests | JUnit (backend), Jasmine/Karma ou Jest (frontend) |

---

## 3. Modèle de données (entités principales)

### User
- id, email, mot de passe (hashé), nom, prénom, date de création

### Team (équipe)
- id, nom, date de création, propriétaire (User)

### TeamMember
- id, team_id, user_id, rôle (`ADMIN` / `MEMBER`)

### Project (projet)
- id, team_id, nom, description, date de création, statut (`ACTIVE` / `ARCHIVED`)

### Task (tâche)
- id, project_id, titre, description, statut (`TODO` / `IN_PROGRESS` / `DONE`), priorité (`LOW` / `MEDIUM` / `HIGH`), assigné_à (User, nullable), date d'échéance, date de création, date de mise à jour

### SubTask (sous-tâche)
- id, task_id, titre, terminé (booléen)

### Comment (commentaire)
- id, task_id, user_id, contenu, date de création

### ActivityLog (historique d'activité)
- id, project_id ou task_id, user_id, type d'action, description, date

---

## 4. Fonctionnalités — MVP (priorité 1)

1. **Authentification** : inscription, connexion, déconnexion (JWT)
2. **Équipes** : créer une équipe, inviter des membres (par email), gérer les rôles (admin/membre)
3. **Projets** : créer/modifier/archiver un projet au sein d'une équipe
4. **Tâches** :
   - créer/modifier/supprimer une tâche
   - déplacer une tâche entre colonnes (To Do / In Progress / Done) — vue Kanban avec drag & drop
   - assigner une tâche à un membre
   - définir priorité et échéance
5. **Sous-tâches** : ajouter une checklist simple à une tâche
6. **Commentaires** : commenter une tâche
7. **Dashboard** : vue d'ensemble multi-projets avec indicateurs simples (nb tâches par statut, tâches en retard)
8. **Historique d'activité** : liste chronologique des actions sur un projet

## 5. Fonctionnalités — Bonus (priorité 2, si le temps le permet)

- Filtres et recherche (par assigné, priorité, tag)
- Étiquettes/tags personnalisables sur les tâches
- Notifications in-app (échéance proche, tâche assignée)
- Vue calendrier en plus de la vue Kanban

---

## 6. Fonctionnalité IA — Résumé automatique d'avancement

**Comportement attendu :**
Sur la page d'un projet, un bouton « Générer un résumé » envoie à l'API Claude le contexte suivant :
- liste des tâches avec statut, priorité, échéance, assigné
- date du jour

**Prompt (à affiner) :**
> "Voici l'état actuel du projet [nom]. Génère un résumé en français, en 5 à 8 lignes, structuré en trois parties : ce qui avance bien, ce qui est en retard ou à risque, et une recommandation de priorité pour les jours à venir. Ton professionnel et synthétique, pas de jargon inutile."

**Exigences techniques :**
- Appel côté backend (Spring Boot) vers l'API Claude, jamais depuis le frontend (protection de la clé API)
- Gestion des erreurs (timeout, quota dépassé) avec message clair côté utilisateur
- Affichage d'un indicateur de chargement pendant la génération
- Le résumé généré peut être régénéré à la demande (pas de cache obligatoire pour le MVP)

---

## 7. Endpoints API (aperçu)

```
POST   /api/auth/register
POST   /api/auth/login

GET    /api/teams
POST   /api/teams
POST   /api/teams/{id}/members

GET    /api/projects
POST   /api/projects
PUT    /api/projects/{id}
DELETE /api/projects/{id}

GET    /api/projects/{id}/tasks
POST   /api/tasks
PUT    /api/tasks/{id}
DELETE /api/tasks/{id}
PATCH  /api/tasks/{id}/status

POST   /api/tasks/{id}/subtasks
PATCH  /api/subtasks/{id}

POST   /api/tasks/{id}/comments

GET    /api/projects/{id}/activity

POST   /api/projects/{id}/ai-summary
```

---

## 8. Exigences non fonctionnelles

- Code versionné sur GitHub avec commits atomiques et messages clairs
- README avec instructions d'installation et de lancement (local + Docker)
- Variables sensibles (clé API, secrets JWT) en variables d'environnement, jamais en dur
- Tests unitaires minimum sur : authentification, création/modification de tâche, appel IA (mock)

---

## 9. Livrables finaux

- Dépôt GitHub public (ou privé avec accès partagé)
- Application déployée avec URL de démo accessible
- README clair (installation, architecture, choix techniques)
- Courte présentation (3 lignes + démo de 2-3 min) expliquant la méthode de pilotage utilisée avec Claude Code : ce qui a été spécifié, généré, relu et corrigé manuellement

---

## 10. Méthode de travail (à respecter et documenter)

Pour chaque module (auth, projets, tâches, IA, etc.) :
1. Rédiger un prompt de spécification précis pour Claude Code, basé sur ce document
2. Faire générer le code
3. Relire ligne par ligne, comprendre chaque partie
4. Corriger/ajuster manuellement ce qui ne convient pas
5. Tester (manuellement puis avec des tests automatisés)
6. Committer avec un message décrivant le module terminé

Garder une trace (notes personnelles) des décisions prises et des corrections apportées au code généré — cette trace servira de base à la présentation finale.
