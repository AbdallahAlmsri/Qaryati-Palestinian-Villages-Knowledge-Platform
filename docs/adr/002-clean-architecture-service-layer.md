# ADR 002: Clean Architecture — Service Layer Separation

## Status
Accepted

## Context
The project specification requires that business/domain rules remain "cohesive,
testable, and independent from unnecessary infrastructure concerns," and that
module boundaries be clear and intentional. An early implementation of the
Village feature placed business logic (e.g., validating that a referenced
governorate exists) directly inside the REST controller, coupling that rule to
HTTP-specific types (`ResponseEntity`) and making it untestable without starting
a full web server.

## Decision
We introduced an explicit **Controller → Service → Repository** layering for
every domain feature, starting with Village:
- **Controller** (`VillageController`): translates HTTP requests/responses only.
  No business logic.
- **Service** (`VillageService`): holds business rules (e.g., governorate
  existence check) and orchestrates repository calls. Framework-agnostic —
  throws plain exceptions, not HTTP-specific types.
- **Repository** (`VillageRepository`): data access only, via Spring Data JPA.
- **Exception handling**: domain-specific exceptions (e.g.,
  `GovernorateNotFoundException`) are translated into structured HTTP error
  responses by a single `GlobalExceptionHandler`, rather than each controller
  building its own error responses ad hoc.

## Alternatives Considered

**Controller calling Repository directly (no service layer)**
- Pros: Less code, faster to write initially.
- Cons: Business rules become untestable without a running web server; rules
  get duplicated or inconsistently applied as more controllers are added;
  violates the spec's explicit testability and separation-of-concerns
  requirements.

**Full Hexagonal/Ports-and-Adapters (explicit port interfaces for every
repository and external call)**
- Pros: Maximum decoupling; business layer would have zero dependency on
  Spring Data JPA types at all.
- Cons: Significant additional boilerplate (interface + implementation for
  every repository) for a project of this scope; the benefit is most valuable
  when infrastructure is swapped often, which is not expected here except for
  the external geocoding provider, where we apply this pattern specifically
  (see future ADR on external API integration).

## Rationale
- **Testability:** `VillageServiceTest` mocks both repositories and tests the
  governorate-existence rule in isolation, with no database or Spring context
  required — runs in milliseconds, directly satisfying the
  testing-structure requirement.
- **Consistency:** `GlobalExceptionHandler` ensures every error response
  follows the same structured JSON shape (`timestamp`, `code`, `message`),
  satisfying the "consistent structured error format" requirement, rather than
  each controller inventing its own.
- **Maintainability:** New domain rules (e.g., future population-record
  validation) have an obvious, consistent home — the service layer — rather
  than being scattered across controllers as the system grows.

## Consequences
- Slightly more files per feature (controller + service + exception, vs.
  controller alone).
- Requires discipline to keep business logic out of controllers as new
  features are added — this ADR exists partly to make that expectation
  explicit for both team members.
- Pattern will be applied consistently going forward, starting with the
  population_records feature.