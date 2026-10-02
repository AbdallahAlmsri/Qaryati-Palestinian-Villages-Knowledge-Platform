# ADR 001: Backend Framework Choice — Spring Boot

## Status
Accepted

## Context
The project requires choosing one backend technology from Spring Boot (Java), Node.js
(Express/NestJS), or Django REST Framework/FastAPI (Python). The choice must be justified
against maintainability, security, testability, scalability, and development efficiency,
since the project is evaluated primarily on software engineering quality rather than
feature count.

## Decision
We chose **Spring Boot 4 (Java 21)**.

## Alternatives Considered

**Node.js (NestJS)**
- Pros: Strong module/DI system comparable to Spring, TypeScript type safety.
- Cons: Security (Spring Security), transaction management, and observability all
  require more manual wiring and more custom decisions to defend in documentation.

**Django REST Framework / FastAPI (Python)**
- Pros: Fast to prototype, Django's ORM and admin are mature.
- Cons: Less structural enforcement of module boundaries and DI, meaning more of the
  "well-engineered" burden falls on team discipline rather than framework defaults.

## Rationale
- **Maintainability:** Spring's dependency injection and package-by-domain structure
  (see ADR 002) keep module boundaries explicit and enforced by the framework itself,
  not just convention.
- **Security:** Spring Security provides production-grade authentication/authorization
  primitives (password hashing via BCrypt, JWT integration, method-level authorization)
  out of the box, directly satisfying the mandatory auth/authz requirement.
- **Testability:** Constructor-based dependency injection makes services trivially
  mockable (see VillageServiceTest), enabling fast unit tests with zero database or
  web server dependency — directly satisfying the testing-structure requirement.
- **Scalability:** Virtual threads (Java 21) and Spring's mature connection pooling
  (HikariCP) handle concurrent load efficiently, relevant to the mandatory performance/
  load-testing requirement.
- **Development efficiency:** Spring Boot's auto-configuration (Flyway, JPA, Security)
  reduced setup time for infrastructure concerns, letting development time focus on
  domain logic.

## Consequences
- Steeper initial learning curve than Express/FastAPI for a team newer to Java.
- More verbose than Python/Node equivalents for simple CRUD operations.
- Strong ecosystem support for every other mandatory requirement (Docker, k6
  compatibility, OpenAPI docs via springdoc) offsets this verbosity.