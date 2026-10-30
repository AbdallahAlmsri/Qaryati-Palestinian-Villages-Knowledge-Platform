# ADR 007: A Trigram Index for Village Name Search

## Status
Accepted

## Context
`GET /api/v1/villages?q=term` finds villages whose description or any of their names contains the term. The name
part is a case-insensitive "contains" match: `lower(village_names.name) LIKE '%term%'`.

The load test on 20,001 villages and 26,667 names (`docs/performance-report.md`) showed that search was the slowest
read by a wide margin: about 65 ms on average and 82 ms at the 95th percentile, against about 17 ms for list and
filter. `EXPLAIN ANALYZE` showed why: a pattern that starts with `%` cannot use a normal B-tree index, so every search
read every row of `village_names`.

## Decision
Add a GIN index using the `pg_trgm` extension on the exact expression the query uses (migration V11):

```sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_village_names_name_trgm ON village_names USING gin (lower(name) gin_trgm_ops);
```

No index is added on `villages.location_description`. `EXPLAIN ANALYZE` showed PostgreSQL does not use one there,
because the description test is combined with `OR` with the name subquery, so such an index would only slow down
writes.

## Alternatives Considered

**A normal B-tree index on `lower(name)`**
- Cons: only helps "starts with" searches. Useless for "contains".

**PostgreSQL full-text search (`tsvector`, `to_tsquery`)**
- Pros: ranking, stemming, and built for documents.
- Cons: village names are short proper nouns, and stemming rules for them would be wrong or missing. It also changes
  what a match means (whole words instead of any part of a name), which would change the API's behaviour.

**An external search engine (Elasticsearch or similar)**
- Pros: powerful ranking and typo tolerance.
- Cons: another service to run and keep in sync with the database, for a few thousand names.

**No index**
- Pros: no extension and no write cost.
- Cons: search time grows linearly with the number of names.

**Indexing `location_description`**
- Rejected as above: the planner does not use it for this query.

## Rationale
- A trigram index is the standard PostgreSQL answer to "contains" searches and keeps the existing query and the existing
  meaning of a match.
- It was chosen from evidence, not guesswork: the plan before the change, the dropped description index, and the
  measurements after.

## Results
Measured with k6, 50 virtual users, on one laptop with the application and database together (single run per case):

| Request | avg before | avg after | p95 before | p95 after |
|---|---|---|---|---|
| **search** | 65.4 ms | **46.1 ms (-30%)** | 82.2 ms | **61.6 ms (-25%)** |
| list (control) | 17.7 ms | 18.2 ms | 28.5 ms | 29.6 ms |
| filter (control) | 16.3 ms | 16.5 ms | 22.9 ms | 23.2 ms |

The search page query itself went from 10.8 ms to 1.9 ms. List and filter, which do not use the index, did not change,
which shows the improvement comes from the index and not from a different machine state. Write cost stayed within
noise (create village 9.9 ms to 10.0 ms, add name 10.5 ms to 10.8 ms).

## Consequences
- **The extension is required.** `pg_trgm` is a trusted extension on PostgreSQL 13 and later, so the database owner can
  create it (it works on the `postgres:16` image used by Docker Compose and the tests). Some managed databases need an
  administrator to enable it first; the migration fails with a "permission denied" message if not.
- **Short terms.** The index cannot help a search term shorter than three characters, which still scans the table.
- **The total count still scans.** The page query is now fast, but the paged response also counts all matches
  (64 ms before, 53 ms after). If the count becomes the limit, options are dropping the exact total, caching it, or an
  estimated count.
- **Size and writes.** The index adds disk space and a small cost to each name insert. At current volumes this is not
  measurable.
- **Numbers are from single runs on one machine.** They show direction and relative size, not production capacity.
  Repeating each run and reporting the range would make them stronger.
