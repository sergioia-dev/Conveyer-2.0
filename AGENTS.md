# AGENTS.md — Conveyer-2.0

## Quick start

```bash
# Dev shell (Java 25, Maven, Postgres client, Redis client)
nix develop .#backend

# Start infra (Postgres :5432, Redis :6379)
docker compose -f backend/docker-compose.yml up -d

# Run the app (auto-sources .env)
nix run .#default
```

## Key commands

| Action | Command (from repo root) |
|---|---|
| Run app | `cd backend && ./mvnw spring-boot:run` |
| Run all tests | `cd backend && ./mvnw test` |
| Run single test | `cd backend && ./mvnw test -Dtest=MainTests` |
| Infra up | `docker compose -f backend/docker-compose.yml up -d` |
| Infra down | `docker compose -f backend/docker-compose.yml down` |

## Architecture

- **Entrypoint**: `backend/src/main/java/conveyer/backend/Main.java`
- **Stack**: Spring Boot 4.0.6 / Java 25 / Maven
- **Build**: Maven wrapper (`backend/mvnw`) — no system Maven needed
- **No frontend, CI, or linter/formatter configured**

## Infrastructure requirements

- Postgres on port **5432**, Redis on port 6379 — see `backend/docker-compose.yml`
- Both must be running before the app starts

## Environment

- `backend/.env` template contains all required env vars (DB, Redis, JWT, GitHub OAuth)
- Nix flake shells auto-source `.env`; otherwise source it manually
- `application.yaml` reads every secret from env vars — app **will not start** without `.env`

## Notable conventions

- **JPA `ddl-auto: create-drop`** — schema rebuilt on every restart; data lost unless changed
- **Security**: stateless sessions, CORS only for `http://localhost:5173`, OAuth2 login + JWT filter fully implemented in `SecurityConfig.java`
- **Package base**: `conveyer.backend.*` (e.g., `conveyer.backend.Main`, `conveyer.backend.configuration.security.*`)
- **Entity package**: `conveyer.backend.persistance.model` — note the misspelling **persistance** (keep it for consistency)
- **Test strategy**: Controller tests use `@WebMvcTest` with mocked services (no infra needed); `MainTests` uses `@SpringBootTest` and requires running Postgres and Redis
