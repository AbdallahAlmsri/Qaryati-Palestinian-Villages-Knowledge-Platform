\# T6 – Update, Delete \& Role Management



\## Overview



Task T6 adds administrative village management and user role management to the Qaryati Palestinian Villages Knowledge Platform.



The implementation includes:

\- Updating existing village information.

\- Deleting villages.

\- Changing user roles.

\- Restricting administrative operations to ADMIN users.

\- Applying updated user roles immediately, even when an existing JWT is reused.



\## 1. Village Management



\### Update Village



\*Endpoint:\* PUT /api/v1/villages/{villageId}



\*Required role:\* ADMIN



Request example:



{

&#x20; "governorateId": 1,

&#x20; "latitude": 32.2211,

&#x20; "longitude": 35.2544,

&#x20; "locationDescription": "Updated village location",

&#x20; "elevationM": 450

}



The endpoint:

\- Verifies that the village exists.

\- Verifies that the specified governorate exists.

\- Validates latitude and longitude ranges.

\- Updates the village information.

\- Returns the updated village with HTTP 200.



\### Delete Village



\*Endpoint:\* DELETE /api/v1/villages/{villageId}



\*Required role:\* ADMIN



The endpoint:

\- Verifies that the village exists.

\- Deletes the village.

\- Returns HTTP 204 on successful deletion.



Deletion is permanent. Related records with cascading foreign keys may also be deleted. Records protected by restrictive foreign keys can prevent deletion and result in HTTP 409.



\## 2. User Role Management



\### Update User Role



\*Endpoint:\* PATCH /api/v1/users/{userId}/role



\*Required role:\* ADMIN



Request example:



{

&#x20; "role": "VERIFIER"

}



Supported roles:

\- VIEWER

\- CONTRIBUTOR

\- VERIFIER

\- ADMIN



Successful response example:



{

&#x20; "id": 2,

&#x20; "username": "example\_user",

&#x20; "role": "VERIFIER"

}



The endpoint:

\- Verifies that the target user exists.

\- Validates the requested role.

\- Updates the role in the database.

\- Returns HTTP 200.

\- Prevents an administrator from removing their own ADMIN role.



\## 3. Security



All three T6 endpoints require the ADMIN role.



Authentication and authorization are enforced through Spring Security and method-level @PreAuthorize checks.



The JWT authentication filter retrieves the user's current role from the database on every authenticated request.



This ensures that a user whose ADMIN role has been removed immediately loses administrative permissions, even when using a previously issued JWT.



\## 4. HTTP Responses



| Status | Meaning |

|--------|---------|

| 200 | Village updated or user role changed successfully |

| 204 | Village deleted successfully |

| 400 | Malformed JSON or unsupported role value |

| 401 | Authentication required |

| 403 | Insufficient permissions |

| 404 | Target user does not exist |

| 409 | Self-demotion conflict or database integrity conflict |

| 422 | Invalid village input or village/governorate not found |



\## 5. Testing



Integration tests were added for village management and role management.



\### VillageManagementIntegrationTest



Covers:

\- Successful village update.

\- Successful village deletion.

\- Unauthorized requests.

\- Forbidden requests from non-admin users.

\- Missing villages.

\- Invalid update data.



\### RoleManagementIntegrationTest



Covers:

\- Successful role changes.

\- Unauthorized requests.

\- Forbidden requests from non-admin users.

\- Missing users.

\- Invalid role values.

\- Prevention of administrator self-demotion.

\- Immediate permission removal after administrator demotion.



\## 6. Verification



Command:



./mvnw test



Final verification result:



\- Tests run: 78

\- Failures: 0

\- Errors: 0

\- Skipped: 0

\- Build: SUCCESS



\## 7. Implementation Notes



\- Village updates and deletions use transactional service methods.

\- Existing village creation and search functionality is preserved.

\- Role changes are persisted through UserRepository.

\- No new database migration was required for T6.

\- Village deletion behavior depends on the existing database foreign-key constraints.

