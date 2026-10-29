-- T13: speed up GET /api/v1/villages?q=...
-- The search matches lower(village_names.name) LIKE '%term%'. A normal B-tree index cannot help a pattern
-- that starts with %, so every search read every village name. A trigram (pg_trgm) GIN index can.
-- The index is on lower(name) so it matches the expression the query uses.
--
-- No index on villages.location_description: EXPLAIN ANALYZE showed PostgreSQL does not use one for this
-- query (the description test is OR-ed with the name subquery), so it would only slow down writes.
-- See docs/performance-report.md for the before/after measurements.

-- pg_trgm ships with PostgreSQL and is a trusted extension, so the database owner can enable it
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_village_names_name_trgm
    ON village_names USING gin (lower(name) gin_trgm_ops);
