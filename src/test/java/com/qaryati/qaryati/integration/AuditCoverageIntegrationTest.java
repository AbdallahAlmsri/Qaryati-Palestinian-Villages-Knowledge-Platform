package com.qaryati.qaryati.integration;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuditCoverageIntegrationTest extends AbstractIntegrationTest {

    private static String villageJson(String description) {
        return "{\"governorateId\":1,\"latitude\":32.25,\"longitude\":35.3,"
                + "\"locationDescription\":\"" + description + "\",\"elevationM\":600}";
    }

    private String registerAdmin(String username) throws Exception {
        registerUser(username);
        jdbcTemplate.update("UPDATE users SET role = 'ADMIN' WHERE username = ?", username);
        return login(username);
    }

    /** The one audit row for this action, with the actor's username. Fails if there is not exactly one. */
    private Map<String, Object> auditRow(String action, String entityType, long entityId) {
        return jdbcTemplate.queryForMap(
                "SELECT a.old_value, a.new_value, u.username AS actor "
                        + "FROM audit_logs a LEFT JOIN users u ON u.id = a.actor_id "
                        + "WHERE a.action = ? AND a.entity_type = ? AND a.entity_id = ?",
                action, entityType, entityId);
    }

    @Test
    void createVillage_isAudited() throws Exception {
        String username = uniqueName("auditcreator");
        String token = registerUser(username);

        ApiResponse created = call("POST", "/api/v1/villages", token, villageJson("Audited creation"));
        assertEquals(201, created.status(), created.body());
        long villageId = extractLong(created.body(), "id");

        Map<String, Object> row = auditRow("CREATE", "VILLAGE", villageId);
        assertEquals(username, row.get("actor"));
        assertNull(row.get("old_value"));
        assertTrue(row.get("new_value").toString().contains("Audited creation"));
    }

    @Test
    void updateVillage_isAuditedWithOldAndNewValues() throws Exception {
        String adminName = uniqueName("auditadmin");
        String admin = registerAdmin(adminName);
        long villageId = extractLong(
                call("POST", "/api/v1/villages", admin, villageJson("Before update")).body(), "id");

        ApiResponse updated = call("PUT", "/api/v1/villages/" + villageId, admin, villageJson("After update"));
        assertEquals(200, updated.status(), updated.body());

        Map<String, Object> row = auditRow("UPDATE", "VILLAGE", villageId);
        assertEquals(adminName, row.get("actor"));
        assertTrue(row.get("old_value").toString().contains("Before update"));
        assertTrue(row.get("new_value").toString().contains("After update"));
    }

    @Test
    void deleteVillage_isAudited() throws Exception {
        String adminName = uniqueName("auditadmin");
        String admin = registerAdmin(adminName);
        long villageId = extractLong(
                call("POST", "/api/v1/villages", admin, villageJson("To be deleted")).body(), "id");

        ApiResponse deleted = call("DELETE", "/api/v1/villages/" + villageId, admin, null);
        assertEquals(204, deleted.status());

        Map<String, Object> row = auditRow("DELETE", "VILLAGE", villageId);
        assertEquals(adminName, row.get("actor"));
        assertTrue(row.get("old_value").toString().contains("To be deleted"));
        assertNull(row.get("new_value"));
    }

    @Test
    void roleChange_isAudited() throws Exception {
        String adminName = uniqueName("auditadmin");
        String admin = registerAdmin(adminName);
        String targetName = uniqueName("audittarget");
        registerUser(targetName);
        long targetId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE username = ?", Long.class, targetName);

        ApiResponse changed = call("PATCH", "/api/v1/users/" + targetId + "/role", admin,
                "{\"role\":\"VERIFIER\"}");
        assertEquals(200, changed.status(), changed.body());

        Map<String, Object> row = auditRow("ROLE_CHANGE", "USER", targetId);
        assertEquals(adminName, row.get("actor"));
        assertEquals("CONTRIBUTOR", row.get("old_value"));
        assertEquals("VERIFIER", row.get("new_value"));
    }
}