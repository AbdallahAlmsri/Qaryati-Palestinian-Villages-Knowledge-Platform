# ADR 005: Population Records Are Append-Only Claims With a Review Workflow

## Status
Accepted

## Context
Population figures for Palestinian villages come from different sources: censuses, surveys, historical studies, local
records. They disagree, they are incomplete, and some are wrong. The platform must keep the history of a village's
population over time, and the project requirements ask us to avoid silently overwriting conflicting or incomplete
data. They also ask for a clear rule on who may create, verify or remove information.

Questions to settle:

1. Can a population record be edited or deleted after it is submitted?
2. What happens when two sources give different numbers for the same village and year?
3. Who decides that a record is trustworthy, and what do readers see before that decision?

## Decision
**A population record is a claim, not a fact.** It states: in year Y, village V had population P, according to
source S, submitted by user U at time T.

1. **Append-only.** There is no endpoint to edit or delete a record. A correction is a new record. This keeps the
   history of what was claimed and by whom.
2. **One value per source and year.** The database enforces `UNIQUE (village_id, year, source)`. The same source
   cannot give two numbers for the same year (the API answers `409 DUPLICATE_OR_INVALID_DATA`), but two different
   sources can disagree and both records are kept.
3. **Review workflow.** A new record has status `PENDING`. A user with the `VERIFIER` or `ADMIN` role moves it
   once to `VERIFIED` or `REJECTED`. A reviewed record cannot be reviewed again, and `PENDING` is not a valid
   decision. The review stores the reviewer, the time and optional review notes, and writes an audit entry
   (see ADR 003).
4. **Validation at two levels.** The request requires `population >= 0`, `year` between 1800 and 2100 and a
   non-blank `source` (at most 255 characters). The database repeats these rules as `CHECK` constraints, so data that
   bypasses the API is also rejected.
5. **Statistics use verified data only.** `GET /villages/{id}/population/stats` (native SQL) computes minimum,
   maximum, average and count over the `VERIFIED` records between the required `fromYear` and `toYear`. Pending and
   rejected claims never affect a statistic.
6. **Who can do what.** Any logged-in user can submit. Only `VERIFIER` and `ADMIN` can review. Reading is public.

## Alternatives Considered

**Edit records in place**
- Pros: the simplest data model, one row per village and year.
- Cons: the previous value and its author are lost. This is exactly the silent overwriting we must avoid.

**Edit in place with a history table**
- Pros: fixes typos without a new record.
- Cons: two tables, edit permissions to design, and "who is right" becomes unclear when contributors edit each
  other's numbers. Append-only plus rejection gives the same outcome with less machinery.

**One record per village and year, with a "disputed" flag**
- Pros: a simple read model with one number per year.
- Cons: someone must pick the number, which hides the disagreement that readers need to see.

**Automatic conflict resolution (rank sources, newest wins)**
- Pros: no human work.
- Cons: ranking sources is a judgement about history. We do not want the code to make it silently.

**Two reviewers for every record (four-eyes)**
- Pros: stronger protection against mistakes.
- Cons: too slow for the number of verifiers a small project will have. It can be added later on top of the same
  statuses.

## Rationale
- Treating a number as a sourced claim fits the domain: historians cite, they do not overwrite.
- Keeping rejected records (instead of deleting them) means the same wrong number is not submitted again and again
  without anyone noticing.
- Putting the uniqueness rule in the database, not only in the service, makes it hold under concurrent requests.

## Consequences
- **Disagreement stays visible.** If two verified sources give different numbers for a year, both remain. Statistics
  count them as two observations, so a disputed year weighs twice in the average. A reviewer can reject one if it is
  judged wrong.
- **Readers see unreviewed data.** `GET /villages/{id}/population` returns records of every status, each with its
  `status` field, so a client can show or hide pending and rejected rows. The statistics are the trusted view.
  Hiding unreviewed records from anonymous users would be a small change if the project decides it wants that.
- **Self-review is not blocked.** A verifier can verify a record they submitted. A rule "a record cannot be
  reviewed by its creator" is a one-line check in `PopulationRecordService.reviewRecord` and a good next hardening.
- **Reviewer notes are private.** `reviewNotes` and the reviewer's name are shown only to `VERIFIER` and `ADMIN`.
- **Time zone.** `created_at` and `reviewed_at` in `population_records` are `timestamp` without a time zone, unlike
  the later tables. It has no effect while there is one server, but should be unified in a future migration.
- The same workflow (status, creator, reviewer, source) is reused for village names, historical events, landmarks,
  village attributes, agriculture, economy and facilities, so contributors and verifiers learn one process.