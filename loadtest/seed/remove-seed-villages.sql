-- Removes every village created by seed-villages.sql, with their names and any population records.
-- Run it against your LOCAL database:
--
--   docker exec -i qaryati-db psql -U qaryati_user -d qaryati < loadtest/seed/remove-seed-villages.sql

\set ON_ERROR_STOP on

BEGIN;

-- population_records has no ON DELETE CASCADE, so its rows go first; village names and heritage rows cascade
DELETE FROM population_records
WHERE village_id IN (SELECT id FROM villages WHERE location_description LIKE '%(load test data)');

DELETE FROM villages WHERE location_description LIKE '%(load test data)';

COMMIT;

SELECT count(*) AS villages_left FROM villages;
