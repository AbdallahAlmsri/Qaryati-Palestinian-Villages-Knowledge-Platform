package com.qaryati.qaryati.exception;

import com.qaryati.qaryati.village.GovernorateNotFoundException;
import com.qaryati.qaryati.village.VillageNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.qaryati.qaryati.population.InvalidReviewException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GovernorateNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(GovernorateNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(errorBody("GOVERNORATE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = errorBody("VALIDATION_FAILED", "One or more fields are invalid");
        body.put("fields", fieldErrors);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    private Map<String, Object> errorBody(String code, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("code", code);
        body.put("message", message);
        String correlationId = org.slf4j.MDC.get("correlationId");
        if (correlationId != null) {
            body.put("correlationId", correlationId);
        }
        return body;
    }

    @ExceptionHandler(VillageNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleVillageNotFound(VillageNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(errorBody("VILLAGE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(org.springframework.dao.DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(errorBody("DUPLICATE_OR_INVALID_DATA", "This record conflicts with an existing one (e.g., duplicate village/year/source) or violates a data constraint"));
    }

    @ExceptionHandler(InvalidReviewException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidReview(InvalidReviewException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(errorBody("INVALID_REVIEW", ex.getMessage()));
    }

    @ExceptionHandler(com.qaryati.qaryati.geocoding.GeocodingUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleGeocodingUnavailable(com.qaryati.qaryati.geocoding.GeocodingUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(errorBody("GEOCODING_UNAVAILABLE", ex.getMessage()));
    }

    @ExceptionHandler(com.qaryati.qaryati.common.InvalidQueryException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidQuery(com.qaryati.qaryati.common.InvalidQueryException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(errorBody("INVALID_QUERY", ex.getMessage()));
    }

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(com.qaryati.qaryati.auth.InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(
            com.qaryati.qaryati.auth.InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(errorBody("INVALID_CREDENTIALS", ex.getMessage()));
    }

    @ExceptionHandler(com.qaryati.qaryati.auth.UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserExists(
            com.qaryati.qaryati.auth.UserAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(errorBody("USER_ALREADY_EXISTS", ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(
            org.springframework.http.converter.HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody("MALFORMED_REQUEST", "Request body is missing or is not valid JSON"));
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody("INVALID_PARAMETER", "Parameter '" + ex.getName() + "' has an invalid value"));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(errorBody("FORBIDDEN", "You do not have permission to perform this action"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        // Spring's own web errors (404, 405, 415, missing parameter...) already carry a status code
        if (ex instanceof org.springframework.web.ErrorResponse errorResponse) {
            HttpStatus status = HttpStatus.valueOf(errorResponse.getStatusCode().value());
            return ResponseEntity.status(status).body(errorBody(status.name(), status.getReasonPhrase()));
        }
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody("INTERNAL_ERROR", "An unexpected error occurred"));
    }
}
