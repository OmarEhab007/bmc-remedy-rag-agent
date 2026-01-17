package com.bmc.rag.api.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST API.
 * SECURITY: Does not expose internal error details in production.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    /**
     * Check if running in development mode.
     */
    private boolean isDevMode() {
        return "dev".equalsIgnoreCase(activeProfile);
    }

    /**
     * Handle authentication errors.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("error", "Authentication required");
        response.put("status", 401);
        response.put("timestamp", Instant.now().toString());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handle authorization errors.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("error", "Access denied");
        response.put("status", 403);
        response.put("timestamp", Instant.now().toString());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Handle validation errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                (a, b) -> a
            ));

        Map<String, Object> response = new HashMap<>();
        response.put("error", "Validation failed");
        response.put("status", 400);
        response.put("timestamp", Instant.now().toString());
        response.put("details", fieldErrors);

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle illegal argument exceptions.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("error", ex.getMessage());
        response.put("status", 400);
        response.put("timestamp", Instant.now().toString());

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle general exceptions.
     * SECURITY: Does not expose internal error details - uses error ID for correlation.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        // Generate unique error ID for correlation
        String errorId = UUID.randomUUID().toString().substring(0, 8);
        log.error("Unexpected error [{}]: {}", errorId, ex.getMessage(), ex);

        Map<String, Object> response = new HashMap<>();
        response.put("error", "An unexpected error occurred");
        response.put("status", 500);
        response.put("timestamp", Instant.now().toString());
        response.put("errorId", errorId);

        // Only expose details in dev mode
        if (isDevMode()) {
            response.put("message", ex.getMessage());
            response.put("type", ex.getClass().getSimpleName());
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handle runtime exceptions.
     * SECURITY: Does not expose internal error details - uses error ID for correlation.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        // Generate unique error ID for correlation
        String errorId = UUID.randomUUID().toString().substring(0, 8);
        log.error("Runtime error [{}]: {}", errorId, ex.getMessage(), ex);

        Map<String, Object> response = new HashMap<>();
        response.put("error", "Operation failed");
        response.put("status", 500);
        response.put("timestamp", Instant.now().toString());
        response.put("errorId", errorId);

        // Only expose details in dev mode
        if (isDevMode()) {
            response.put("message", ex.getMessage());
            response.put("type", ex.getClass().getSimpleName());
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
