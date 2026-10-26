Agriculture, Economy and Facilities (T3)



Overview



This module manages agricultural crops, economic activities, and public facilities associated with Palestinian villages.



Features



\* Add agricultural crops with names, descriptions, and sources.

\* Add economic activities with names, descriptions, and sources.

\* Add village facilities with names, types, descriptions, and sources.

\* View agriculture, economy, and facilities records for a village.

\* Review submitted records using the PENDING, VERIFIED, and REJECTED statuses.

\* Store creator and reviewer information for each record.



API Endpoints



All endpoints use the prefix /api/v1/villages/{villageId}.



Method	Endpoint	Access

GET	/agriculture	Public

POST	/agriculture	Authenticated

PATCH	/agriculture/{entityId}/review	VERIFIER or ADMIN

GET	/economy	Public

POST	/economy	Authenticated

PATCH	/economy/{entityId}/review	VERIFIER or ADMIN

GET	/facilities	Public

POST	/facilities	Authenticated

PATCH	/facilities/{entityId}/review	VERIFIER or ADMIN



Review Workflow



1\. An authenticated user submits an agriculture, economy, or facilities record.

2\. The record starts with PENDING status.

3\. A VERIFIER or ADMIN can approve or reject the record.

4\. Approved records receive VERIFIED status.

5\. Rejected records receive REJECTED status.

6\. A reviewed record cannot be reviewed again.



Database



Flyway migration V7\_\_create\_economy\_agriculture\_facilities.sql creates:



\* village\_agriculture

\* village\_economy

\* village\_facilities



Each record is linked to a village and stores its source, status, creator, creation timestamp, reviewer, and review timestamp.



Validation and Error Handling



\* Required fields are validated before record creation.

\* Creating records requires authentication.

\* Reviewing records requires VERIFIER or ADMIN permissions.

\* Records can only be reviewed while their status is PENDING.

\* Invalid review attempts return HTTP 422.

\* Requests without authentication return HTTP 401.

\* Users without review permissions receive HTTP 403.



Testing



EconomyAgricultureFacilitiesIntegrationTest contains three integration tests covering agriculture, economy, and facilities workflows.



The tests verify record creation, retrieval, authentication, authorization, validation, and review behavior.



The complete test suite passed with 66 tests, 0 failures, and 0 errors.



Run all tests:



.\\mvnw.cmd clean test



