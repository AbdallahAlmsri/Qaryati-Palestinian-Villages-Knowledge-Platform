# Configuration

The application uses Spring profiles. The profile is chosen with `SPRING_PROFILES_ACTIVE`.
If none is set, `dev` is used.

| Profile | Used for | Secrets |
|---|---|---|
| dev | Running from IntelliJ on a laptop | Safe local defaults in `application-dev.properties` |
| test | Automated tests (`mvn test`) | Test-only values; database from Testcontainers; geocoding points at a closed port |
| prod | `docker compose up` and any production-like run | Required from the environment; the app refuses to start without them |

## Environment variables

| Variable | Used by | Meaning |
|---|---|---|
| DB_URL | prod (dev has a default) | JDBC URL of the database |
| DB_USERNAME / DB_PASSWORD | all | Database credentials |
| APP_JWT_SECRET | prod (dev has a default) | Signing key for login tokens, at least 64 characters |
| APP_CORS_ALLOWED_ORIGINS | prod | Browser origins allowed to call the API |
| APP_RATE_LIMIT_AUTH_MAX_REQUESTS / _WINDOW_SECONDS | optional | Login rate limit (10 per 60 seconds by default) |
| APP_GEOCODING_BASE_URL | optional | Geocoding service address |

## Rules

- Real secrets live only in `.env` (ignored by Git) or in the deployment environment.
- `.env.example` is the template; copy it to `.env`.
- Never add a default value for a secret in `application-prod.properties`.