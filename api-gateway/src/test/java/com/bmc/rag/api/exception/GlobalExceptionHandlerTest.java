package com.bmc.rag.api.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GlobalExceptionHandler.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("Authentication Exception Tests")
    class AuthenticationExceptionTests {

        @Test
        @DisplayName("handleAuthenticationException_returnsUnauthorizedStatus")
        void handleAuthenticationException_returnsUnauthorizedStatus() {
            AuthenticationException exception = mock(AuthenticationException.class);
            when(exception.getMessage()).thenReturn("Invalid credentials");

            ResponseEntity<Map<String, Object>> response = exceptionHandler.handleAuthenticationException(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("error")).isEqualTo("Authentication required");
            assertThat(response.getBody().get("status")).isEqualTo(401);
            assertThat(response.getBody().get("timestamp")).isNotNull();
        }
    }

    @Nested
    @DisplayName("Access Denied Exception Tests")
    class AccessDeniedExceptionTests {

        @Test
        @DisplayName("handleAccessDeniedException_returnsForbiddenStatus")
        void handleAccessDeniedException_returnsForbiddenStatus() {
            AccessDeniedException exception = new AccessDeniedException("User lacks permission");

            ResponseEntity<Map<String, Object>> response = exceptionHandler.handleAccessDeniedException(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("error")).isEqualTo("Access denied");
            assertThat(response.getBody().get("status")).isEqualTo(403);
            assertThat(response.getBody().get("timestamp")).isNotNull();
        }
    }

    @Nested
    @DisplayName("Validation Exception Tests")
    class ValidationExceptionTests {

        @Test
        @DisplayName("handleValidationErrors_returnsBadRequestWithFieldErrors")
        void handleValidationErrors_returnsBadRequestWithFieldErrors() {
            BindingResult bindingResult = mock(BindingResult.class);
            MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

            FieldError fieldError1 = new FieldError("chatRequest", "text", "must not be blank");
            FieldError fieldError2 = new FieldError("chatRequest", "userId", "must not be null");
            when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

            ResponseEntity<Map<String, Object>> response = exceptionHandler.handleValidationErrors(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("error")).isEqualTo("Validation failed");
            assertThat(response.getBody().get("status")).isEqualTo(400);
            assertThat(response.getBody().get("timestamp")).isNotNull();

            @SuppressWarnings("unchecked")
            Map<String, String> details = (Map<String, String>) response.getBody().get("details");
            assertThat(details).isNotNull();
            assertThat(details.get("text")).isEqualTo("must not be blank");
            assertThat(details.get("userId")).isEqualTo("must not be null");
        }

        @Test
        @DisplayName("handleValidationErrors_fieldErrorWithNullMessage_usesDefaultMessage")
        void handleValidationErrors_fieldErrorWithNullMessage_usesDefaultMessage() {
            BindingResult bindingResult = mock(BindingResult.class);
            MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

            FieldError fieldError = new FieldError("chatRequest", "text", null, false, null, null, null);
            when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

            ResponseEntity<Map<String, Object>> response = exceptionHandler.handleValidationErrors(exception);

            @SuppressWarnings("unchecked")
            Map<String, String> details = (Map<String, String>) response.getBody().get("details");
            assertThat(details.get("text")).isEqualTo("Invalid value");
        }

        @Test
        @DisplayName("handleValidationErrors_duplicateFieldErrors_keepsFirstError")
        void handleValidationErrors_duplicateFieldErrors_keepsFirstError() {
            BindingResult bindingResult = mock(BindingResult.class);
            MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

            FieldError fieldError1 = new FieldError("chatRequest", "text", "must not be blank");
            FieldError fieldError2 = new FieldError("chatRequest", "text", "size must be between 1 and 10000");
            when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

            ResponseEntity<Map<String, Object>> response = exceptionHandler.handleValidationErrors(exception);

            @SuppressWarnings("unchecked")
            Map<String, String> details = (Map<String, String>) response.getBody().get("details");
            assertThat(details.get("text")).isEqualTo("must not be blank");
        }
    }

    @Nested
    @DisplayName("Illegal Argument Exception Tests")
    class IllegalArgumentExceptionTests {

        @Test
        @DisplayName("handleIllegalArgument_returnsBadRequestWithMessage")
        void handleIllegalArgument_returnsBadRequestWithMessage() {
            IllegalArgumentException exception = new IllegalArgumentException("Invalid parameter value");

            ResponseEntity<Map<String, Object>> response = exceptionHandler.handleIllegalArgument(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("error")).isEqualTo("Invalid parameter value");
            assertThat(response.getBody().get("status")).isEqualTo(400);
            assertThat(response.getBody().get("timestamp")).isNotNull();
        }
    }

    @Nested
    @DisplayName("General Exception Tests")
    class GeneralExceptionTests {

        @Test
        @DisplayName("handleGeneralException_devMode_includesExceptionDetails")
        void handleGeneralException_devMode_includesExceptionDetails() {
            ReflectionTestUtils.setField(exceptionHandler, "activeProfile", "dev");
            Exception exception = new Exception("Database connection failed");

            ResponseEntity<Map<String, Object>> response = exceptionHandler.handleGeneralException(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("error")).isEqualTo("An unexpected error occurred");
            assertThat(response.getBody().get("status")).isEqualTo(500);
            assertThat(response.getBody().get("timestamp")).isNotNull();
            assertThat(response.getBody().get("errorId")).isNotNull();
            assertThat(response.getBody().get("message")).isEqualTo("Database connection failed");
            assertThat(response.getBody().get("type")).isEqualTo("Exception");
        }

        @Test
        @DisplayName("handleGeneralException_prodMode_hidesExceptionDetails")
        void handleGeneralException_prodMode_hidesExceptionDetails() {
            ReflectionTestUtils.setField(exceptionHandler, "activeProfile", "prod");
            Exception exception = new Exception("Internal database error");

            ResponseEntity<Map<String, Object>> response = exceptionHandler.handleGeneralException(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("error")).isEqualTo("An unexpected error occurred");
            assertThat(response.getBody().get("status")).isEqualTo(500);
            assertThat(response.getBody().get("timestamp")).isNotNull();
            assertThat(response.getBody().get("errorId")).isNotNull();
            assertThat(response.getBody().get("message")).isNull();
            assertThat(response.getBody().get("type")).isNull();
        }

        @Test
        @DisplayName("handleGeneralException_generatesUniqueErrorId")
        void handleGeneralException_generatesUniqueErrorId() {
            Exception exception = new Exception("Test error");

            ResponseEntity<Map<String, Object>> response1 = exceptionHandler.handleGeneralException(exception);
            ResponseEntity<Map<String, Object>> response2 = exceptionHandler.handleGeneralException(exception);

            String errorId1 = (String) response1.getBody().get("errorId");
            String errorId2 = (String) response2.getBody().get("errorId");

            assertThat(errorId1).isNotNull();
            assertThat(errorId2).isNotNull();
            assertThat(errorId1).isNotEqualTo(errorId2);
            assertThat(errorId1).hasSize(8); // UUID substring(0, 8)
        }
    }

    @Nested
    @DisplayName("Runtime Exception Tests")
    class RuntimeExceptionTests {

        @Test
        @DisplayName("handleRuntimeException_devMode_includesExceptionDetails")
        void handleRuntimeException_devMode_includesExceptionDetails() {
            ReflectionTestUtils.setField(exceptionHandler, "activeProfile", "dev");
            RuntimeException exception = new RuntimeException("Null pointer error");

            ResponseEntity<Map<String, Object>> response = exceptionHandler.handleRuntimeException(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("error")).isEqualTo("Operation failed");
            assertThat(response.getBody().get("status")).isEqualTo(500);
            assertThat(response.getBody().get("timestamp")).isNotNull();
            assertThat(response.getBody().get("errorId")).isNotNull();
            assertThat(response.getBody().get("message")).isEqualTo("Null pointer error");
            assertThat(response.getBody().get("type")).isEqualTo("RuntimeException");
        }

        @Test
        @DisplayName("handleRuntimeException_prodMode_hidesExceptionDetails")
        void handleRuntimeException_prodMode_hidesExceptionDetails() {
            ReflectionTestUtils.setField(exceptionHandler, "activeProfile", "prod");
            RuntimeException exception = new RuntimeException("Internal error");

            ResponseEntity<Map<String, Object>> response = exceptionHandler.handleRuntimeException(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("error")).isEqualTo("Operation failed");
            assertThat(response.getBody().get("status")).isEqualTo(500);
            assertThat(response.getBody().get("timestamp")).isNotNull();
            assertThat(response.getBody().get("errorId")).isNotNull();
            assertThat(response.getBody().get("message")).isNull();
            assertThat(response.getBody().get("type")).isNull();
        }

        @Test
        @DisplayName("handleRuntimeException_generatesUniqueErrorId")
        void handleRuntimeException_generatesUniqueErrorId() {
            RuntimeException exception = new RuntimeException("Test error");

            ResponseEntity<Map<String, Object>> response1 = exceptionHandler.handleRuntimeException(exception);
            ResponseEntity<Map<String, Object>> response2 = exceptionHandler.handleRuntimeException(exception);

            String errorId1 = (String) response1.getBody().get("errorId");
            String errorId2 = (String) response2.getBody().get("errorId");

            assertThat(errorId1).isNotNull();
            assertThat(errorId2).isNotNull();
            assertThat(errorId1).isNotEqualTo(errorId2);
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("handleGeneralException_nullMessage_handlesGracefully")
        void handleGeneralException_nullMessage_handlesGracefully() {
            ReflectionTestUtils.setField(exceptionHandler, "activeProfile", "dev");
            Exception exception = new Exception((String) null);

            ResponseEntity<Map<String, Object>> response = exceptionHandler.handleGeneralException(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("message")).isNull();
        }

        @Test
        @DisplayName("handleIllegalArgument_emptyMessage_returnsEmptyError")
        void handleIllegalArgument_emptyMessage_returnsEmptyError() {
            IllegalArgumentException exception = new IllegalArgumentException("");

            ResponseEntity<Map<String, Object>> response = exceptionHandler.handleIllegalArgument(exception);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("error")).isEqualTo("");
        }
    }
}
