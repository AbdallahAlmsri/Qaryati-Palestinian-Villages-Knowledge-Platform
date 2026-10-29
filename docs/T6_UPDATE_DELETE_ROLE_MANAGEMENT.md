# T6 - Update, Delete & Role Management

## Overview
T6 supports administrative village updates, village deletion, and user role management.

## Village Management
- PUT /api/v1/villages/{villageId} requires ADMIN and returns 200 on success.
- DELETE /api/v1/villages/{villageId} requires ADMIN and returns 204 on success.
- A village with population records cannot be deleted. The API returns 409 with error code VILLAGE_DELETE_CONFLICT.
- Deletion is permanent. Other related records may be deleted through database cascade rules.

## User Role Management
- PATCH /api/v1/users/{userId}/role requires ADMIN.
- Supported roles: VIEWER, CONTRIBUTOR, VERIFIER, ADMIN.
- A successful role change returns 200.
- Missing user: 404, USER_NOT_FOUND.
- Administrator self-demotion: 409, USER_ROLE_CONFLICT.
- Null role at service level: 422, INVALID_USER_ROLE.
- An unsupported role value in the JSON request returns 400.

## Security and Permissions
All three administrative endpoints require ADMIN. Spring Security enforces access restrictions. Current database roles are checked on authenticated requests, so demoted administrators lose access even when reusing an old JWT.

Unauthenticated requests return 401; authenticated users without sufficient permissions receive 403.

## Audit and Logging
HTTP request logging includes correlation identifiers. This is operational logging, not a persistent database audit trail. T6 does not implement an audit-log table or audit service.

## Testing
- VillageServiceTest: 9 tests passed.
- RoleManagementIntegrationTest: 7 tests passed.
- Full test-suite verification is required before finalizing this PR.

## Implementation Notes
Village updates and deletions use transactional service methods. Village deletion checks for linked population records before deleting. No new database migration is required for T6.