# Qaryati — Palestinian Villages Knowledge Platform

Qaryati (قريتي, "my village") is a REST API that collects sourced, reviewable knowledge about Palestinian
villages: where they are, what they were and are called, and how their population changed over time.

Every contributed fact carries a **source**, a **status** (`PENDING`, `VERIFIED` or `REJECTED`) and the user who
added it. Contributors submit data, verifiers review it, and official statistics only count verified records.

## Contents

- [Scope](#scope)
- [Tech stack](#tech-stack)
- [Run it](#run-it)
- [Environment variables](#environment-variables)
- [Run the tests](#run-the-tests)
- [Using the API](#using-the-api)
- [Project structure](#project-structure)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [Further documentation](#further-documentation)

## Scope

**In scope**

- Villages with their governorate, municipality, coordinates and elevation, with paging, filtering, search and sorting
- Village names (current, historical, alternate) and the origin of each name
- Population history as an append-only record, with a review workflow and statistics
- History and heritage: historical events, landmarks and cultural attributes
- Agriculture, economy and public facilities
- Geography: governorates, municipalities and a "villages near this one" search
- A check of stored coordinates against an external geocoding service (OpenStreetMap Nominatim), using the
  village's verified Arabic name
- Accounts with roles: `VIEWER`, `CONTRIBUTOR`, `VERIFIER` and `ADMIN`. Administrators can edit and delete
  villages and change users' roles
- An audit log of review decisions, written in the same transaction as the review

**Out of scope**

- A user interface. Qaryati is an API only.
- Complaints, service requests or any ticket workflow. The platform describes villages; it does not manage them.

## Tech stack

| Part | Choice |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4 (Web MVC, Data JPA, Security, Validation) |
| Database | PostgreSQL 16, schema managed by Flyway |
| Authentication | JWT bearer tokens |
| Resilience | Resilience4j (retry, circuit breaker) and Caffeine cache for geocoding |
| API documentation | springdoc OpenAPI with Swagger UI |
| Tests | JUnit 5, Mockito, Testcontainers (real PostgreSQL) |
| Build and run | Maven (wrapper included), Docker, Docker Compose |
| CI | GitHub Actions runs every test on each Pull Request and on `main` |

## Run it

### What you need

- [Docker Desktop](https://www.docker.com/products/docker-desktop/), running
- Git
- JDK 21, only if you want to run the app or the tests outside Docker. You do not need Maven: use the
  included wrapper `./mvnw` (macOS/Linux, Git Bash) or `mvnw.cmd` (Windows Command Prompt / PowerShell).

### 1. Clone and configure

```bash
git clone https://github.com/AbdallahAlmsri/Qaryati-Palestinian-Villages-Knowledge-Platform.git
cd Qaryati-Palestinian-Villages-Knowledge-Platform
cp .env.example .env
```

Open `.env` and set your own values (see [Environment variables](#environment-variables)). Git ignores `.env`,
so your passwords and secrets never get committed. On Windows without Git Bash, use `copy .env.example .env`.

### 2a. Run everything in Docker (simplest)

```bash
docker compose up --build
```

This starts PostgreSQL and the application with the `prod` profile. Wait for `Started QaryatiApplication`
in the log, then open <http://localhost:8080/api/v1/villages>. You should see JSON.

Stop it with `Ctrl+C`, or `docker compose down` from another terminal.

### 2b. Run the app from your IDE or the command line (for development)

Start only the database in Docker:

```bash
docker compose up -d db
```

It listens on port **5544** on your computer. Then run the application with the `dev` profile (the default):

```bash
./mvnw spring-boot:run
```

Or open the project in IntelliJ IDEA, set the project SDK to 21, and run `QaryatiApplication`.

The `dev` profile has safe local defaults for the database and JWT secret, so it works as soon as the
database container is running. Flyway creates the tables and sample data on first start.

> Run either 2a or 2b, not both at once: they both use port 8080.

Once it is running, <http://localhost:8080/swagger-ui.html> lists every endpoint with its request and response
fields, and lets you try them from the browser (click **Authorize** and paste a token for protected ones).

## Environment variables

These live in `.env` (copied from `.env.example`). Docker Compose reads them automatically.

| Variable | Required for | Meaning |
|---|---|---|
| `DB_NAME` | Docker | Database name. Default `qaryati` |
| `DB_USERNAME` / `DB_PASSWORD` | Docker, `prod` | Database credentials |
| `DB_PORT` | Docker | Port the database is published on, on your computer. Default `5544` |
| `DB_URL` | `prod` outside Compose | JDBC URL, for example `jdbc:postgresql://localhost:5544/qaryati` |
| `APP_JWT_SECRET` | `prod` | Key that signs login tokens. At least 64 characters. Generate one with `openssl rand -base64 64` |
| `APP_CORS_ALLOWED_ORIGINS` | `prod` | Comma-separated browser origins allowed to call the API |
| `APP_JWT_EXPIRATION_MS` | optional | Token lifetime. Default 3600000 (one hour) |
| `APP_RATE_LIMIT_AUTH_MAX_REQUESTS` / `APP_RATE_LIMIT_AUTH_WINDOW_SECONDS` | optional | Login rate limit per IP. Default 10 per 60 seconds |
| `APP_GEOCODING_BASE_URL` | optional | Geocoding service address. Default Nominatim |
| `APP_GEOCODING_CONNECT_TIMEOUT_MS` / `APP_GEOCODING_READ_TIMEOUT_MS` | optional | How long to wait for Nominatim. Default 3000 and 5000 |
| `APP_GEOCODING_RATE_LIMIT_MS` | optional | Minimum time between two Nominatim requests. Default 1000 (Nominatim's limit of one per second) |
| `SPRING_PROFILES_ACTIVE` | optional | `dev` (default), `test` or `prod` |

The `prod` profile has **no defaults for secrets**: if one is missing, the application refuses to start.
See [docs/configuration.md](docs/configuration.md) for how the profiles differ.

## Run the tests

Docker Desktop must be running, because the integration tests start a temporary PostgreSQL with Testcontainers.

```bash
./mvnw test
```

- **Unit tests** test each service on its own with Mockito.
- **Integration tests** (`src/test/.../integration`) start the whole application on a random port against a
  real PostgreSQL 16 and call it over HTTP.
- No test calls a real external service: in the `test` profile the geocoding client points at a closed local port.

`./mvnw verify` runs the same tests and also builds the JAR. CI runs `mvn -B verify` on every Pull Request,
and `main` only accepts changes with a passing check.

## Using the API

Every endpoint is under `/api/v1`. Reading is public. Writing needs a token, and reviewing needs the
`VERIFIER` or `ADMIN` role.

### Log in

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"sara","email":"sara@example.com","password":"password123"}'
```

Both register and login return `{"token": "..."}`. Send it on protected requests:

```bash
curl -X POST http://localhost:8080/api/v1/villages \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"governorateId":1,"latitude":32.25,"longitude":35.30,"locationDescription":"North of Nablus","elevationM":600}'
```

Tokens last one hour. The user's role is read from the database on every request, so a role change takes
effect immediately, even for a token issued before it.

### Endpoints

| Method | Path | Who | What it does |
|---|---|---|---|
| POST | `/auth/register` | Anyone | Creates an account (always a `CONTRIBUTOR`) and returns a token |
| POST | `/auth/login` | Anyone | Returns a token |
| GET | `/villages` | Anyone | A page of villages. Parameters: `page`, `size` (1–100), `sort` (e.g. `elevationM,desc`), `governorateId`, `q` (searches descriptions and names) |
| POST | `/villages` | Logged in | Creates a village. `municipalityId` is optional and must be in the same governorate |
| PUT | `/villages/{id}` | `ADMIN` | Updates a village's governorate, coordinates, description and elevation |
| DELETE | `/villages/{id}` | `ADMIN` | Deletes a village and its names and heritage records. Refused with `409 VILLAGE_DELETE_CONFLICT` if it has population records |
| GET | `/villages/{id}/nearby` | Anyone | Villages within `radiusKm` (default 10, max 100), nearest first, with `distanceKm`. `limit` 1–100, default 20 |
| GET | `/villages/{id}/population/stats?fromYear=&toYear=` | Anyone | Min, max, average and count of `VERIFIED` records |
| GET | `/villages/{id}/geocode-check` | Anyone | Compares the stored coordinates with what Nominatim returns for the village's verified Arabic name. `422 MISSING_VERIFIED_ARABIC_NAME` if it has none, `503 GEOCODING_UNAVAILABLE` if Nominatim cannot be reached |
| GET | `/governorates` | Anyone | All governorates, by name |
| GET | `/governorates/{id}/municipalities` | Anyone | The municipalities of one governorate |
| PATCH | `/users/{id}/role` | `ADMIN` | Changes a user's role, for example `{"role":"VERIFIER"}`. An admin cannot demote themself |

**Contributed records.** Each village has eight kinds of record that anyone can read, any logged-in user can
add to, and a verifier reviews. They all work the same way:

| Method | Path | Who | What it does |
|---|---|---|---|
| GET | `/villages/{id}/{kind}` | Anyone | The village's records of that kind |
| POST | `/villages/{id}/{kind}` | Logged in | Adds a record with its source. It starts as `PENDING` |
| PATCH | `/villages/{id}/{kind}/{recordId}/review` | `VERIFIER`, `ADMIN` | Sets it to `VERIFIED` or `REJECTED`, once |

where `{kind}` is one of:

| `{kind}` | Holds |
|---|---|
| `names` | Current, historical and alternate names, with language, period and origin |
| `population` | A population figure for a year. Listed oldest first |
| `historical-events` | Dated events with a description |
| `landmarks` | Landmarks with a type and optional coordinates |
| `attributes` | Cultural attributes, such as traditional food and cultural heritage |
| `agriculture` | Crops grown in the village |
| `economy` | Economic activities |
| `facilities` | Public facilities, such as schools and clinics |

A review sends the decision and an optional note:

```json
{ "status": "VERIFIED", "reviewNotes": "Matches the 1945 Village Statistics" }
```

Reviewing a record that is no longer `PENDING` returns `422`. For names, population, historical events,
landmarks and attributes, the note is stored and the review is written to the audit log (who, what, old and
new status) in the same transaction, so a review is never saved without its log entry. Agriculture, economy
and facilities reviews only change the status.

### Errors

Every error, including 401, 403, 404 and 500, has the same JSON shape:

```json
{
  "timestamp": "2026-10-25T10:15:30Z",
  "code": "VILLAGE_NOT_FOUND",
  "message": "Village with id 42 does not exist",
  "correlationId": "3f2c9a..."
}
```

Validation errors add a `fields` object naming each invalid field. Every response carries an
`X-Correlation-Id` header, and the same ID appears in the server logs, so a reported error can be traced.

## Project structure

```
.
├── src/main/java/com/qaryati/qaryati
│   ├── auth/          Register, login, JWT creation and the JWT request filter
│   ├── user/          User entity, roles and role management
│   ├── village/       Villages: create, list/search, update, delete, nearby search
│   ├── villagename/   Village names and name origins
│   ├── population/    Population history, review workflow, statistics (native SQL)
│   ├── history/       Historical events, landmarks and cultural attributes
│   ├── economy/       Agriculture, economy and facilities
│   ├── audit/         Audit log of review decisions
│   ├── governorate/   Governorates and municipalities
│   ├── geocoding/     Nominatim client behind the LocationProvider interface (timeouts, rate limit)
│   ├── config/        Security, CORS, login rate limit, correlation IDs, JSON 401/403 responses
│   ├── exception/     GlobalExceptionHandler: the single error format
│   └── common/        Shared types such as PageResponse
├── src/main/resources
│   ├── application.properties          Settings shared by every profile
│   ├── application-{dev,test,prod}.properties
│   └── db/migration/                   Flyway migrations V1, V2, ...
├── src/test/java/...                   Unit tests per package, integration tests in integration/
├── docs/
│   ├── adr/                            Architecture Decision Records
│   ├── diagrams/                       ERD and architecture diagram
│   ├── configuration.md                Profiles and secrets
│   └── *.md                            One page per feature (see Further documentation)
├── .github/                            CI workflow and Pull Request template
├── Dockerfile                          Two-stage build: Maven build, then a JRE-only image
├── docker-compose.yml                  PostgreSQL + the application
└── .env.example                        Template for your local .env
```

Each topic package follows the same layers:

- **Controller:** HTTP only. Reads the request and returns a DTO.
- **Service:** the rules. It throws a plain exception that `GlobalExceptionHandler` turns into the error format.
- **Repository:** talks to the database through Spring Data JPA, with native SQL where it is clearer or faster.
- **DTOs** are Java records with validation annotations. Entities are never returned directly.

Database changes are always a **new** migration file with the next free number (for example
`V12__add_something.sql`). Never edit a migration that has already been merged. Check open Pull Requests
too: two branches that both add the same number will clash when the second one is merged.

## Troubleshooting

| What you see | Why | Fix |
|---|---|---|
| `Connection refused` when the app starts | The database container is not running | `docker ps`, then `docker compose up -d db` |
| Tests fail with `Could not find a valid Docker environment` | Docker Desktop is not running | Start Docker Desktop and wait for "Engine running" |
| `Port 8080 already in use` | The Docker copy of the app is still running | `docker stop qaryati-app` |
| `password authentication failed` after changing `.env` | PostgreSQL only reads its password when the volume is first created | `docker compose down -v` (this deletes your local data) |
| Flyway `validate` error after pulling | A new migration has a lower number than one your local database already ran | `docker compose down -v`, then start again |
| `401` on a request | The token is missing or expired | Log in again and send `Authorization: Bearer <token>` |
| `403` on a review | Your user is a `CONTRIBUTOR` | See below |

**Getting a verifier for local testing.** Register a user, then:

```bash
docker exec -it qaryati-db psql -U qaryati_user -d qaryati -c "UPDATE users SET role = 'VERIFIER' WHERE username = 'your_username';"
```

The new role works straight away, with the same token. Use the `DB_USERNAME` from your `.env` if you changed
it. Set `role = 'ADMIN'` the same way to get an administrator, who can then change other users' roles with
`PATCH /api/v1/users/{id}/role`.

## Contributing

1. Branch from an up-to-date `main`.
2. Commit small and often, with a short title (under 72 characters) and a few lines on what and why.
3. Run `./mvnw test` before pushing.
4. Open a Pull Request using the template. CI must be green and someone else reviews it before it is merged.

Every feature ships with unit tests for its service and at least one integration test. Important design
decisions get an ADR in `docs/adr`.

## Further documentation

- [ADR 001: Backend framework choice](docs/adr/001-backend-framework-choice.md)
- [ADR 002: Clean architecture and the service layer](docs/adr/002-clean-architecture-service-layer.md)
- [ADR 003: Audit logs share the business transaction](docs/adr/003-audit-log-transaction-boundary.md)
- [Configuration and profiles](docs/configuration.md)
- [Database ERD](docs/diagrams/erd.md) and [architecture diagram](docs/diagrams/architecture.md)
- Swagger UI at <http://localhost:8080/swagger-ui.html> while the app is running

Feature pages:

- [History and heritage](docs/history-heritage.md)
- [Agriculture, economy and facilities](docs/agriculture-economy-facilities.md)
- [Geocoding hardening](docs/geocoding-hardening.md)
- [Update, delete and role management](docs/T6_UPDATE_DELETE_ROLE_MANAGEMENT.md)
- [Audit log, transactions and review notes](docs/audit-log-transactions-review-notes.md)
