package com.qaryati.qaryati.integration;

import com.qaryati.qaryati.audit.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditTransactionIntegrationTest extends AbstractIntegrationTest {

    @MockitoSpyBean
    private AuditLogRepository auditLogRepositorySpy;

    private static final String VILLAGE_JSON = """
            {
              "governorateId": 1,
              "latitude": 32.25,
              "longitude": 35.3,
              "locationDescription": "Audit transaction test",
              "elevationM": 600
            }
            """;

    @Test
    void successfulReview_createsAuditLog() throws Exception {
        String contributor = registerUser(uniqueName("contrib"));
        String verifier = registerVerifier(uniqueName("verifier"));

        long villageId = extractLong(
                call("POST", "/api/v1/villages", contributor, VILLAGE_JSON).body(),
                "id"
        );

        String base = "/api/v1/villages/" + villageId + "/population";

        ApiResponse created = call(
                "POST",
                base,
                contributor,
                """
                {
                  "population": 4500,
                  "year": 2020,
                  "source": "PCBS"
                }
                """
        );

        assertEquals(201, created.status());
        long recordId = extractLong(created.body(), "id");

        ApiResponse reviewed = call(
                "PATCH",
                base + "/" + recordId + "/review",
                verifier,
                """
                {
                  "status": "VERIFIED",
                  "reviewNotes": "Checked by verifier"
                }
                """
        );

        assertEquals(200, reviewed.status());

        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM audit_logs
                WHERE entity_type = 'POPULATION_RECORD'
                  AND entity_id = ?
                  AND action = 'REVIEW'
                  AND old_value = 'PENDING'
                  AND new_value = 'VERIFIED'
                """,
                Integer.class,
                recordId
        );

        assertEquals(1, count);

        String notes = jdbcTemplate.queryForObject(
                "SELECT review_notes FROM population_records WHERE id = ?",
                String.class,
                recordId
        );

        assertEquals("Checked by verifier", notes);
    }

    @Test
    void auditFailure_rollsBackPopulationReview() throws Exception {
        String contributor = registerUser(uniqueName("contrib"));
        String verifier = registerVerifier(uniqueName("verifier"));

        long villageId = extractLong(
                call("POST", "/api/v1/villages", contributor, VILLAGE_JSON).body(),
                "id"
        );

        String base = "/api/v1/villages/" + villageId + "/population";

        ApiResponse created = call(
                "POST",
                base,
                contributor,
                """
                {
                  "population": 3500,
                  "year": 2021,
                  "source": "PCBS"
                }
                """
        );

        assertEquals(201, created.status());
        long recordId = extractLong(created.body(), "id");

        try {
            doThrow(new RuntimeException("Simulated audit failure"))
                    .when(auditLogRepositorySpy)
                    .save(any());

            ApiResponse response = call(
                    "PATCH",
                    base + "/" + recordId + "/review",
                    verifier,
                    """
                    {
                      "status": "VERIFIED",
                      "reviewNotes": "Should rollback"
                    }
                    """
            );

            assertTrue(response.status() >= 500);

            String status = jdbcTemplate.queryForObject(
                    "SELECT status FROM population_records WHERE id = ?",
                    String.class,
                    recordId
            );

            assertEquals("PENDING", status);

            String notes = jdbcTemplate.queryForObject(
                    "SELECT review_notes FROM population_records WHERE id = ?",
                    String.class,
                    recordId
            );

            assertNull(notes);

            Integer count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM audit_logs
                    WHERE entity_type = 'POPULATION_RECORD'
                      AND entity_id = ?
                    """,
                    Integer.class,
                    recordId
            );

            assertEquals(0, count);
        } finally {
            reset(auditLogRepositorySpy);
        }
    }
}
