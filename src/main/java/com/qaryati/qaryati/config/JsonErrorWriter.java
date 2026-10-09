package com.qaryati.qaryati.config;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;

import java.io.IOException;
import java.time.Instant;

final class JsonErrorWriter {

    private JsonErrorWriter() {
    }

    static void write(HttpServletResponse response, int status, String code, String message) throws IOException {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);

        StringBuilder json = new StringBuilder()
                .append("{\"code\":\"").append(code).append("\"")
                .append(",\"message\":\"").append(message.replace("\"", "'")).append("\"")
                .append(",\"timestamp\":\"").append(Instant.now()).append("\"");
        if (correlationId != null) {
            json.append(",\"correlationId\":\"").append(correlationId).append("\"");
        }
        json.append("}");

        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(json.toString());
    }
}