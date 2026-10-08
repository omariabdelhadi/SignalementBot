# Signalement App : Agent IA

Service Spring Boot qui héberge le **chatbot (agent IA)** de l'application de signalement citoyen. Il répond aux questions des utilisateurs en interrogeant le backend, et adapte ses services au rôle de l'utilisateur (Administrateur / Utilisateur).

> **Dépôts liés :** [backend-signalement](https://github.com/omariabdelhadi/backend-signalement) · [frontend-signalement](https://github.com/omariabdelhadi/frontend-signalement)

## Architecture

```
Frontend (Angular) ───► Agent IA (ce dépôt) ───► Backend (API REST) ───► PostgreSQL
        │                                                ▲
        └────────────────────────────────────────────────┘
```

Le frontend envoie les questions à l'agent IA, qui interroge le backend pour répondre. Le frontend communique aussi directement avec le backend pour le reste de l'application.

## Technologies

| Domaine | Technologies |
|---|---|
| Langage | Java 21 |
| Framework | Spring Boot, API REST |
| Agent IA | Spring AI / LangChain, OpenAI |
| Mémoire de conversation | En mémoire (aucune base de données) |
| Build | Maven |

## Lancer le projet

### Prérequis

- JDK 21
- Une clé API OpenAI
- Le [backend](https://github.com/omariabdelhadi/backend-signalement) démarré

### Installation et démarrage

```bash
git clone https://github.com/omariabdelhadi/agent-signalement.git
cd agent-signalement
```

La clé OpenAI se configure par variable d'environnement, jamais dans le code.

Sous Windows (PowerShell) :

```powershell
$env:OPENAI_API_KEY="votre_cle_openai"
.\mvnw.cmd spring-boot:run
```

Sous Linux ou macOS :

```bash
export OPENAI_API_KEY="votre_cle_openai"
./mvnw spring-boot:run
```

Le port est défini par `server.port` dans `src/main/resources/application.properties`. Il doit être différent de celui du backend (8080) et correspondre à l'adresse appelée par le frontend.

La mémoire de conversation est stockée en mémoire : elle est effacée à chaque arrêt du service.
