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
