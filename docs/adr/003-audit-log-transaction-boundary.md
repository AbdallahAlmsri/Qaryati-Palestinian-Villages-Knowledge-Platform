# ADR 003: Audit Logs Share the Business Transaction

## Status
Accepted

## Context
Review operations change the status of village-related records and create audit entries. An audit entry must not describe a change that failed to persist.

## Decision
Audit writes for review operations participate in the same database transaction as the corresponding business update. Review services use Spring's @Transactional boundary, and audit persistence participates in that transaction.

## Consequences
- Successful reviews persist their status changes and audit entries together.
- If the transaction rolls back, both changes roll back.
- Audit persistence failures prevent the review transaction from committing.
- Audit coverage is not yet complete for all mutation operations; remaining gaps must be tracked separately.