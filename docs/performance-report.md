# Performance Report (T13)

Load tests with k6 against the village API, a bottleneck found with `EXPLAIN ANALYZE`, one index added
to fix it (`V11__add_search_trigram_indexes.sql`), and the same tests run again.

How to run everything yourself: [loadtest/README.md](../loadtest/README.md).

## Setup

| | |
|---|---|
| Date | 29 October 2026 |
| Machine | One Windows 11 laptop (20 logical cores). App, database and k6 all on the same machine |
| App | Spring Boot jar, `dev` profile, logging at INFO |
| Database | PostgreSQL 16.15 in Docker (`postgres:16`), default settings |
| Load tool | k6 1.0.0 (`grafana/k6:1.0.0` Docker image) |
| Data | 20,001 villages and 26,667 village names (`seed-villages.sql` with `village_count=20000`) |
| Read test | `read-villages.js`, 50 virtual users, 15 s ramp-up, 1 min at peak, 10 s ramp-down |
| Write test | `write-villages.js`, 10 virtual users, same stages |

Before every run the seed is loaded again, so every run starts from the same data.

Everything runs on one laptop, so the numbers are useful for comparing *before* and *after*. They are
not a prediction of production capacity.

## 1. Baseline (no search index)

| Request | avg | median | p95 | max |
|---|---|---|---|---|
| list (page + sort) | 17.7 ms | 17.5 ms | 28.5 ms | 48 ms |
| filter (one governorate) | 16.3 ms | 17.0 ms | 22.9 ms | 41 ms |
| **search (`q=`)** | **65.4 ms** | **66.0 ms** | **82.2 ms** | **124 ms** |

6,735 requests, 0 failed, about 79 requests per second. **Search was almost 4 times slower than listing.**

Writes (10 users): create a village averaged 9.9 ms (p95 14.3 ms) and adding a name 10.5 ms (p95 15.3 ms),
with 0 failures. Writes were not a problem.

## 2. Finding the cause

`GET /api/v1/villages?q=hanina` builds this query (simplified):

```sql
SELECT v.* FROM villages v
WHERE lower(v.location_description) LIKE '%hanina%'
   OR EXISTS (SELECT 1 FROM village_names vn
              WHERE vn.village_id = v.id AND lower(vn.name) LIKE '%hanina%')
ORDER BY v.id LIMIT 20;
```

Spring Data also runs a `count(...)` with the same `WHERE` to fill in `totalElements`.

`EXPLAIN ANALYZE` showed the name lookup as a **`Seq Scan on village_names`**: all 26,667 names were read
and 26,003 thrown away to find 664 matches, on every search. A normal B-tree index cannot help, because
the pattern starts with `%`, so there is no fixed prefix to look up.

## 3. The fix

`V11__add_search_trigram_indexes.sql` enables the `pg_trgm` extension (part of PostgreSQL) and adds a GIN
trigram index on `lower(village_names.name)`. A trigram index splits text into 3-letter pieces, so it can
answer `LIKE '%term%'` without reading every row. It is on `lower(name)` because that is the exact
expression the query uses.

**We also tried an index on `lower(villages.location_description)` and dropped it.** `EXPLAIN ANALYZE`
showed that PostgreSQL never used it for this query, because the description test is combined with the
name subquery using `OR`. An index nobody uses only costs time on every insert, so V11 does not create it.

After the change, the plan uses the index:

```
Bitmap Index Scan on idx_village_names_name_trgm  (actual time=0.149..0.149 rows=664)
```

| Query | Before | After |
|---|---|---|
| search page (`LIMIT 20`) | 10.8 ms | **1.9 ms** |
| search count | 64.2 ms | 53.1 ms |

## 4. After the index (same tests, same data)

| Request | avg before | avg after | p95 before | p95 after |
|---|---|---|---|---|
| list | 17.7 ms | 18.2 ms | 28.5 ms | 29.6 ms |
| filter | 16.3 ms | 16.5 ms | 22.9 ms | 23.2 ms |
| **search** | **65.4 ms** | **46.1 ms (−30%)** | **82.2 ms** | **61.6 ms (−25%)** |

6,871 requests, 0 failed. List and filter did not change (they do not search), which shows the
difference comes from the index and not from the machine.

Writes with the index:

| Request | avg before | avg after | p95 before | p95 after |
|---|---|---|---|---|
| create village | 9.9 ms | 10.0 ms | 14.3 ms | 13.3 ms |
| add name | 10.5 ms | 10.8 ms | 15.3 ms | 14.2 ms |

The extra index has no measurable cost on writes at this scale.

## 5. What is left

Search is still slower than listing. Most of the remaining time is in the **count query**:

- It still reads every village (`Seq Scan on villages`), because the `OR` on the description stops
  PostgreSQL from using an index for that part.
- PostgreSQL overestimates how many rows will match (10,004 instead of about 500), so it decides the
  query is expensive and compiles it with JIT, which alone took around 12 ms of the count query.

Options if search needs to get faster, in order of effort:

1. Turn off JIT for this database (`ALTER DATABASE qaryati SET jit = off`). It is a one-line setting and
   JIT rarely pays off for short API queries.
2. Write the search as two index-friendly parts joined with `UNION` (description matches, then name
   matches) as a native query. That lets a description index be used too.
3. Do not count on every page (use a `Slice` instead of a `Page`), if the client does not need
   `totalElements`.

None of these were needed to pass the thresholds: every request type stayed well under the 500 ms p95
goal at 50 users, with 0 failed requests in every run.

## Notes on measuring

- **Re-seeding leaves dead rows.** The seed deletes the previous load test data and inserts it again. Until
  PostgreSQL cleans up the deleted rows, the planner overestimates the cost of the index and skips it.
  In the first "after" run the index was ignored and search only improved from 53 to 44 ms. A real
  database cleans up automatically (autovacuum), so `seed-villages.sql` now ends with `VACUUM ANALYZE`.
- The before and after runs in sections 1 and 4 were made back to back, on the same data, with only the
  index changed between them.
