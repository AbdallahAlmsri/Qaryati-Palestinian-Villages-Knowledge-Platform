# ADR 006: Cache Geocoding Results Only

## Status
Accepted

## Context
Caching makes some requests faster and cheaper, and it can also make them wrong: a cache can serve data that changed.
We need to decide what to cache, for how long, and what must never be cached.

The platform has two very different kinds of data:

- **Geocoding answers** from the public OpenStreetMap Nominatim service. They are slow (a network call), limited by
  Nominatim's usage policy (about one request per second, see `docs/geocoding-hardening.md`), and they almost never
  change.
- **Our own data**: villages, names, population, heritage records and permissions. It changes whenever someone
  contributes or a verifier reviews, and readers are told that "verified" means verified now.

## Decision
1. **Cache only `LocationProvider.geocode(query)`.** It uses Spring's cache abstraction (`@Cacheable("geocodeResults")`)
   with an in-process Caffeine cache, keyed by the query text.
2. **Entries expire after 24 hours and the cache holds at most 1,000 entries**
   (`spring.cache.caffeine.spec=maximumSize=1000,expireAfterWrite=24h` in `application.properties`).
3. **Failures and empty answers are not cached.** The method has `unless = "#result == null"`, and the fallback used
   when the provider fails returns an empty result. A temporary Nominatim outage is therefore not remembered for a day.
4. **Do not cache our own data.** Village lists, searches, statistics, nearby searches, review queues and the
   user's role are always read from the database.

## Alternatives Considered

**Cache village reads (lists, search, statistics)**
- Pros: fewer database queries.
- Cons: every write and review would need to invalidate the right entries, and a missed invalidation shows data
  that is no longer verified. The queries are already fast: the load test (`docs/performance-report.md`) measured about
  18 ms on average for a page of 20,000 villages and about 46 ms for a search after the index in ADR 007. The risk is
  larger than the gain.

**Cache the logged-in user's role**
- Pros: saves one query per request.
- Cons: a demoted administrator would keep their rights until the entry expires. The security design deliberately
  reads the role from the database on every request so a change applies immediately.

**Redis (shared cache)**
- Pros: shared between several application instances and survives restarts.
- Cons: another container to run, secure and explain for one endpoint's benefit. We run one instance, so an
  in-process cache is enough.

**HTTP caching headers (`Cache-Control`, `ETag`) on read endpoints**
- Pros: lets browsers and proxies cache, with no server memory.
- Cons: the same freshness question as caching village reads. Possible later for the reference data
  (`/governorates`, municipalities), which changes only through migrations.

**No cache at all**
- Pros: nothing to configure.
- Cons: the same Arabic village name would call Nominatim again each time, wasting the one-request-per-second budget.

## Rationale
- Geocoding results are a good cache candidate on every axis: expensive, rate-limited, stable, and safe to be slightly
  out of date.
- Our own data is the opposite: cheap to read, changing, and where freshness is part of the product.
- A size limit and an expiry keep memory bounded and let corrected map data eventually reach us.

## Consequences
- The cache lives in one application instance and is empty after a restart. With several instances each one would
  keep its own copy (acceptable; Redis is the next step if that happens).
- A village whose name cannot be found is looked up again on every request. The provider's rate limiter keeps this to
  one request per second, and the circuit breaker stops calls while Nominatim is failing.
- The key is the query text only, so two villages with the same Arabic name share one entry, even if they are in
  different governorates. The check compares against the first match Nominatim returns for that name inside
  Palestine (`country-codes=ps`); a name that exists in two places can therefore produce a misleading mismatch.
  Adding the governorate to the query is the fix if this becomes a problem.
- `GeocodingCacheConfigurationTest` checks that the cache has both an expiry and a size limit, so removing the
  setting fails the build.
