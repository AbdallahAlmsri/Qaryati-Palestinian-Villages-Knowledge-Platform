# Load testing (T13)

Tools and data for the k6 performance evaluation. Everything here runs against your **local** database only.

## 1. Seed test data

A table with one village hides slow queries, so first load a few thousand fake villages with their names.
The database container must be running (`docker compose up -d db`) and the app must have started once,
so Flyway has created the tables.

```bash
docker exec -i qaryati-db psql -U qaryati_user -d qaryati -v village_count=5000 < loadtest/seed/seed-villages.sql
```

- `village_count` is optional (default 5000).
- Every run first removes the previous seed, and the data is generated from a fixed random seed,
  so each run produces the same villages. Before/after measurements then compare like with like.
- Seeded villages are spread over the existing governorates, with coordinates inside the West Bank and Gaza.
  Each gets a current name, and every third village also gets a historical name.
- Every seeded village's description ends with `(load test data)`.

To remove the seed again:

```bash
docker exec -i qaryati-db psql -U qaryati_user -d qaryati < loadtest/seed/remove-seed-villages.sql
```

Use the `DB_USERNAME` from your `.env` if you changed it.

## 2. Run the k6 scenarios

k6 runs in Docker, so there is nothing to install. Start the app first (`./mvnw spring-boot:run`, or
`docker compose up --build`), then from the project root:

```bash
docker run --rm -i -v "$PWD/loadtest:/loadtest" grafana/k6:1.0.0 run /loadtest/k6/read-villages.js
```

In Git Bash on Windows, prefix the command with `MSYS_NO_PATHCONV=1` and use `$(pwd -W)` instead of `$PWD`.
In PowerShell, use `${PWD}`.

Inside the container, `host.docker.internal` is your computer, which is where the app listens. Options go
after `run`, for example `run -e VUS=50 -e DURATION=2m /loadtest/k6/read-villages.js`:

| Option | Default | Meaning |
|---|---|---|
| `BASE_URL` | `http://host.docker.internal:8080` | Where the app is |
| `VUS` | `20` | Peak number of virtual users |
| `DURATION` | `1m` | How long the peak is held, after a 15 s ramp-up |

### Read scenario (`read-villages.js`)

Each virtual user repeatedly browses the public village list, with 0.25–0.75 s of think time:

- **40% list:** a random page (size 20) with a random sort
- **40% search:** `q=` with a word from the seeded names and descriptions, or one that matches nothing
- **20% filter:** villages of one governorate, with a random sort

Requests are tagged `list`, `search` and `filter`, so the summary shows response times per type. The run fails
if more than 1% of requests fail or any type's p95 goes above 500 ms.
