package com.qaryati.qaryati.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
public abstract class AbstractIntegrationTest {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int port;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected record ApiResponse(int status, String body) {
    }

    protected ApiResponse call(String method, String path, String token, String json) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json");
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        builder.method(method, json == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json));

        HttpResponse<String> response = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new ApiResponse(response.statusCode(), response.body());
    }

    protected String uniqueName(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    protected String credentials(String username, String password) {
        return "{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password);
    }

    protected String registerUser(String username) throws Exception {
        String json = "{\"username\":\"%s\",\"email\":\"%s@example.com\",\"password\":\"password123\"}"
                .formatted(username, username);
        ApiResponse response = call("POST", "/api/v1/auth/register", null, json);
        assertEquals(200, response.status());
        return extractString(response.body(), "token");
    }

    protected String login(String username) throws Exception {
        ApiResponse response = call("POST", "/api/v1/auth/login", null, credentials(username, "password123"));
        assertEquals(200, response.status());
        return extractString(response.body(), "token");
    }

    /** Registers a user, promotes them in the database, and logs in again so the token carries the new role. */
    protected String registerVerifier(String username) throws Exception {
        registerUser(username);
        jdbcTemplate.update("UPDATE users SET role = 'VERIFIER' WHERE username = ?", username);
        return login(username);
    }

    protected String extractString(String body, String field) {
        Matcher matcher = Pattern.compile("\"" + field + "\":\"([^\"]+)\"").matcher(body);
        if (!matcher.find()) {
            throw new AssertionError("Field '" + field + "' not found in: " + body);
        }
        return matcher.group(1);
    }

    protected long extractLong(String body, String field) {
        Matcher matcher = Pattern.compile("\"" + field + "\":(\\d+)").matcher(body);
        if (!matcher.find()) {
            throw new AssertionError("Field '" + field + "' not found in: " + body);
        }
        return Long.parseLong(matcher.group(1));
    }
}