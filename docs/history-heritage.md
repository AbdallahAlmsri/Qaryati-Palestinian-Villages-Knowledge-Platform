# History and Heritage (T2)

## Overview

This module manages historical events, landmarks, and cultural attributes for Palestinian villages.

## Features

- Add historical events with dates, descriptions, and sources.
- Add landmarks with types, locations, and sources.
- Add village attributes such as traditional food and cultural heritage.
- View records associated with a village.
- Review submitted records using the PENDING, VERIFIED, and REJECTED statuses.

## API Endpoints

All endpoints use the prefix /api/v1/villages/{villageId}.

| Method | Endpoint | Access |
|---|---|---|
| GET | /historical-events | Public |
| POST | /historical-events | Authenticated |
| PATCH | /historical-events/{entityId}/review | VERIFIER or ADMIN |
| GET | /landmarks | Public |
| POST | /landmarks | Authenticated |
| PATCH | /landmarks/{entityId}/review | VERIFIER or ADMIN |
| GET | /attributes | Public |
| POST | /attributes | Authenticated |
| PATCH | /attributes/{entityId}/review | VERIFIER or ADMIN |

## Review Workflow

1. An authenticated user submits a record.
2. The record starts with PENDING status.
3. A VERIFIER or ADMIN can approve or reject it.
4. A reviewed record cannot be reviewed again.

## Database

Flyway migration V6__create_history_heritage_tables.sql creates:

- historical_events
- landmarks
- village_attributes

Each record is linked to a village and stores its source, status, creator, and review information.

## Testing

HistoryWorkflowIntegrationTest contains four integration tests covering the three record types, permissions, validation, and review behavior.

The complete test suite passed with 58 tests, 0 failures, and 0 errors.

Run all tests:

    .\mvnw.cmd test
