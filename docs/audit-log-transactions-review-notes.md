# T7 — Audit Log, Transactions & Review Notes

## Overview
This task introduces audit logging and review notes for content verification workflows. Review operations use transactions to keep status updates and audit entries atomic.

## Database Changes
Migration: V9__create_audit_logs_and_review_notes.sql

- Created audit_logs with actor, action, entity type, entity ID, previous value, new value, and timestamp.
- Added indexes for actor, entity lookup, and creation time.
- Added review_notes to population records, historical events, landmarks, village attributes, and village names.

## Audit Logging
- Implemented AuditLog, AuditLogRepository, and AuditLogService.
- Audit entries record the authenticated actor when available.
- Review operations record previous and new statuses.
- Audit recording requires an active transaction using Propagation.MANDATORY.

## Review Workflows
- Population records, historical events, landmarks, village attributes, and village names support review notes.
- Village-name reviews require VERIFIER or ADMIN permissions.
- Review metadata is restricted to VERIFIER and ADMIN in API responses.
- Invalid review transitions return HTTP 422.
- Missing village-name records return HTTP 404.

## Transaction Management
Review operations use @Transactional to keep content updates and audit logging atomic. Audit persistence failures cause the transaction to roll back.

See [ADR 003](adr/003-audit-log-transaction-boundary.md).

## Testing
- Audit log unit tests cover actor attribution and persistence failures.
- Review workflow integration tests cover permissions and status transitions.
- Village-name integration tests cover review-note privacy.
- Full test suite: *69 tests passed, 0 failures, 0 errors*.

## Known Gaps
Audit entries are written for review decisions (population records, names, events, landmarks and attributes), role changes (ROLE_CHANGE on USER), and village create, update and delete (CREATE, UPDATE and DELETE on VILLAGE). Remaining gaps: creation of contributed records, every change to agriculture, economy and facilities records (which also lack review notes), and an endpoint to read the audit log.
## Migration Dependency
The migration uses version V9 to avoid conflicting with the existing migration sequence.