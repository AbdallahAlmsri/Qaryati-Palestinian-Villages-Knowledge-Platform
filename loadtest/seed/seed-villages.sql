-- Load-test seed data (T13): thousands of villages with names, so slow queries show up under k6.
-- A tiny table hides slow queries, because PostgreSQL can scan a few rows quickly whatever the plan.
--
-- This is NOT a Flyway migration on purpose: fake villages must never reach a real database.
-- Run it by hand against your LOCAL database only:
--
--   docker exec -i qaryati-db psql -U qaryati_user -d qaryati -v village_count=5000 < loadtest/seed/seed-villages.sql
--
-- village_count is optional (default 5000). Running the script again first removes the previous seed,
-- and setseed() makes the "random" data identical every run, so before/after measurements compare like with like.
-- Every seeded village is marked by its description ending in '(load test data)'; remove-seed-villages.sql deletes them.

\set ON_ERROR_STOP on
\if :{?village_count}
\else
    \set village_count 5000
\endif

BEGIN;

-- Start from a clean slate so repeated runs never pile up duplicates
DELETE FROM population_records
WHERE village_id IN (SELECT id FROM villages WHERE location_description LIKE '%(load test data)');
DELETE FROM villages WHERE location_description LIKE '%(load test data)';

SELECT setseed(0.42);

-- One row per village with every random choice made up front.
-- Picking from arrays (not subqueries) matters: an uncorrelated subquery runs once, giving every row the same value.
-- The id comes straight from the villages sequence, so the names below can be linked without a lookup.
CREATE TEMP TABLE seed ON COMMIT DROP AS
WITH choices AS (
    SELECT (SELECT array_agg(id ORDER BY id) FROM governorates) AS governorate_ids,
           ARRAY['Beit', 'Kafr', 'Deir', 'Ein', 'Khirbet', 'Tell', 'Bir', 'Wadi', 'Ras', 'Arab', '', ''] AS prefixes,
           ARRAY['Hanina', 'Sahour', 'Jala', 'Qaddum', 'Lahia', 'Ula', 'Rima', 'Nuba', 'Fajjar', 'Ummar',
                 'Dajan', 'Iksa', 'Surif', 'Zakariya', 'Samiya', 'Rafat', 'Burqa', 'Sinjil', 'Turmus', 'Yabrud',
                 'Qira', 'Haris', 'Deek', 'Masha', 'Zawiya', 'Bidya', 'Awarta', 'Aqraba', 'Duma', 'Majdal',
                 'Yatta', 'Dura', 'Sair', 'Shuyukh', 'Taffuh', 'Nahhalin', 'Battir', 'Wallaja', 'Artas', 'Husan'] AS roots,
           ARRAY['on a hilltop surrounded by olive groves',
                 'in a valley with terraced farmland',
                 'near a natural spring',
                 'on the slopes above the coastal plain',
                 'beside old stone terraces and vineyards',
                 'at the edge of the desert',
                 'among almond and fig orchards',
                 'close to an ancient Roman road'] AS landscapes
),
picks AS (
    SELECT n,
           c.governorate_ids[1 + floor(random() * cardinality(c.governorate_ids))::int] AS governorate_id,
           c.prefixes[1 + floor(random() * cardinality(c.prefixes))::int]               AS prefix,
           c.roots[1 + floor(random() * cardinality(c.roots))::int]                     AS root,
           c.landscapes[1 + floor(random() * cardinality(c.landscapes))::int]           AS landscape,
           round((31.25 + random() * 1.30)::numeric, 6)                                  AS latitude,  -- roughly the
           round((34.25 + random() * 1.35)::numeric, 6)                                  AS longitude, -- West Bank and Gaza
           (200 + floor(random() * 800))::int                                            AS elevation_m
    FROM generate_series(1, :village_count) AS n
    CROSS JOIN choices c
)
SELECT nextval(pg_get_serial_sequence('villages', 'id')) AS village_id,
       p.*,
       trim(p.prefix || ' ' || p.root) || ' ' || p.n AS village_name
FROM picks p;

INSERT INTO villages (id, governorate_id, latitude, longitude, location_description, elevation_m)
SELECT s.village_id, s.governorate_id, s.latitude, s.longitude,
       s.village_name || ', a village in ' || g.name || ' governorate ' || s.landscape || ' (load test data)',
       s.elevation_m
FROM seed s
JOIN governorates g ON g.id = s.governorate_id;

-- Every village gets its current name; a third also get a historical name,
-- so name search (q=...) has to look through more rows than there are villages
INSERT INTO village_names (village_id, name, name_type, language, source, status)
SELECT village_id, village_name, 'current', 'en', 'Load test seed', 'VERIFIED'
FROM seed;

INSERT INTO village_names (village_id, name, name_type, language, valid_to, source, status)
SELECT village_id, 'Khirbet ' || root || ' al-Qadima ' || n, 'historical', 'en', 1948, 'Load test seed', 'VERIFIED'
FROM seed
WHERE n % 3 = 0;

COMMIT;

-- Clean up the rows deleted above and refresh the statistics. Without the VACUUM, the dead rows left
-- by re-seeding make the planner skip the search index (V11), which a real database's autovacuum would
-- have cleaned up. VACUUM cannot run inside BEGIN/COMMIT, so it stays down here.
VACUUM ANALYZE villages;
VACUUM ANALYZE village_names;

SELECT count(*) AS seeded_villages FROM villages WHERE location_description LIKE '%(load test data)';
SELECT count(*) AS seeded_names
FROM village_names vn JOIN villages v ON v.id = vn.village_id
WHERE v.location_description LIKE '%(load test data)';
