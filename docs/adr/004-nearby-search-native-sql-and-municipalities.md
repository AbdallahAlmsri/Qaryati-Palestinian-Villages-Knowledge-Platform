# ADR 004: Nearby Villages with Native SQL (Haversine) Instead of PostGIS, and Municipalities as Reference Data

## Status
Accepted

## Context
T4 adds geography to the API: governorates, municipalities, and
`GET /api/v1/villages/{id}/nearby?radiusKm=`, which returns the villages within a radius, nearest first.

Two decisions had to be made:

1. **How to compute distances.** Villages store `latitude` and `longitude` as plain `DECIMAL` columns
   (from V1). The distance between two points on the Earth's surface is not a straight-line distance on
   a flat map, and the query has to return distances, filter by radius and sort, ideally inside the
   database so only the matching rows travel to the application.
2. **How new municipalities get into the system.** V10 seeds the main city municipality of each
   governorate, but there are many more municipalities, and the API has no way to create one.

## Decision

**Distances:** a native SQL query in `VillageRepository.findNearby` using the **Haversine formula**
(Earth radius 6371 km) on the existing columns, with no database extension. Before the formula runs,
a **latitude/longitude bounding box** around the village discards rows that cannot be within the radius,
so the trigonometry only runs for nearby rows. `ASIN(LEAST(1, ...))` protects against floating-point
results slightly above 1. The service validates the input first (`0 < radiusKm <= 100`, not NaN,
`1 <= limit <= 100`) so the query never runs with values that make no sense.

**Municipalities:** municipalities are **reference data managed through Flyway migrations**. A new
municipality is added by a new migration (for example `V11__add_municipalities_nablus.sql`), reviewed in
a Pull Request like any other change. There is no create or update endpoint for now.

## Alternatives Considered

**PostGIS (`geography` column, GiST index, `ST_DWithin` / `ST_Distance`)**
- Pros: the standard tool for spatial data. Distances on the WGS84 ellipsoid are more accurate, and a
  GiST index makes radius searches fast on very large tables.
- Cons: the stock `postgres:16` image does not include PostGIS. Docker Compose and the Testcontainers
  setup would have to switch to a `postgis/postgis` image, a migration would have to enable the
  extension and add a new column, and Hibernate would need `hibernate-spatial`. We have already had
  to pin a dependency because Spring Boot 4 has no managed version for it (`spring-boot-starter-aop`),
  so adding another integration carried a real compatibility risk for one endpoint.

**PostgreSQL `cube` + `earthdistance` extensions**
- Pros: ship with standard PostgreSQL and provide `earth_distance` and an indexable `earth_box`.
- Cons: still an extension to enable and explain, and the formula is hidden inside a function. The
  plain Haversine query shows exactly what is computed, which matters for a project graded on
  engineering decisions and gives a second example of raw SQL next to the population statistics.

**Compute distances in Java**
- Pros: no SQL at all.
- Cons: every village would have to be loaded into memory for every request, which gets slower as the
  table grows and wastes database and network time. Rejected.

**An ADMIN endpoint for municipalities (instead of migrations)**
- Pros: no deployment needed to add a municipality.
- Cons: municipalities are official administrative units that change rarely. An endpoint needs role
  rules, validation, auditing and tests, and the role management and audit log are being built in
  T6/T7 at the same time. A migration is reviewed, versioned and identical in every environment.

## Rationale
- **Accuracy is enough:** Haversine treats the Earth as a sphere, an error of well under 1% compared with
  the ellipsoid. For "villages within 10 km" that is a few metres to tens of metres, which does not change
  which villages are returned in a way that matters to users.
- **Performance is enough for the data size:** the platform covers hundreds of present-day localities, or low thousands including historical
  villages, not millions.
  The bounding box keeps the expensive part small. If the T13 load tests show the nearby search is a
  bottleneck, the next step is an index on `latitude` (or `latitude, longitude`) so the box filter can
  use it, which is still far simpler than PostGIS.
- **Simplicity and portability:** works on the existing `postgres:16` image, the existing columns and
  Testcontainers with no changes, and the query can be read and checked by the whole team.
- **Testability:** unit tests check the validation and the bounding-box maths; an integration test places
  villages 1 km, 5 km and 30 km from an anchor and checks the order, the radius cut-off and the computed
  distance.

## Consequences
- Distances are approximate (spherical model). Acceptable for this domain; documented here.
- Without an index on the coordinates, the box filter still reads every village row. Fine at the current
  scale; T13 measures it, and the fix above is ready if needed.
- If the project later needs polygons (village boundaries, land areas), moving to PostGIS should be
  reconsidered. This ADR would then be superseded.
- Adding a municipality requires a migration and a Pull Request, not an API call. If the team later
  wants administrators to manage municipalities at run time, an ADMIN-only endpoint can be added on top
  of the T6 role rules without changing the table.
