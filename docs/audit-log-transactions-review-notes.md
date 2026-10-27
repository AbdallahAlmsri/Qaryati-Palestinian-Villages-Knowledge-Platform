\# T7 — Audit Log, Transactions \& Review Notes



\## Overview

This task introduces audit logging and review notes for content verification workflows. Review operations are transactional so that content changes and their corresponding audit entries succeed or fail together.



\## Database Changes

Migration: V8\_\_create\_audit\_logs\_and\_review\_notes.sql



\- Created audit\_logs with actor, action, entity type, entity ID, previous value, new value, and timestamp.

\- Added indexes for actor, entity lookup, and creation time.

\- Added review\_notes to population records, historical events, landmarks, village attributes, and village names.



\## Audit Logging

\- Implemented AuditLog, AuditLogRepository, and AuditLogService.

\- Audit entries record the authenticated actor when available.

\- Review operations log the entity and its previous and new statuses.

\- Audit recording requires an active transaction using Propagation.MANDATORY.



\## Review Workflows

\- Population records, historical events, landmarks, village attributes, and village names support review notes.

\- Village names support PATCH /api/v1/villages/{villageId}/names/{nameId}/review.

\- Village-name reviews require the VERIFIER or ADMIN role.

\- Review status changes are limited to valid review transitions.



\## Transaction Management

Review operations use @Transactional to keep content updates and audit logging atomic.



If audit persistence fails, the review transaction rolls back, preventing partially committed changes.



\## Testing

\- Audit log unit tests cover actor attribution, unauthenticated context, and persistence failures.

\- Population review tests verify review notes and audit recording.

\- Integration tests verify successful audit persistence and rollback when audit persistence fails.

\- Full test suite: \*68 tests passed, 0 failures, 0 errors\*.



\## Migration Dependency

V8 must be merged after the branch containing migration V7 to preserve Flyway migration ordering.



