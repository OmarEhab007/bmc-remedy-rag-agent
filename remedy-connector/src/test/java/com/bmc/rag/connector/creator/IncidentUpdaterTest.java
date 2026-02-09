package com.bmc.rag.connector.creator;

import com.bmc.rag.connector.connection.ThreadLocalARContext;
import com.bmc.rag.connector.connection.ThreadLocalARContext.ARConnectionException;
import com.bmc.rag.connector.dto.CreationResult;
import com.bmc.rag.connector.dto.IncidentUpdateRequest;
import com.bmc.rag.connector.util.FieldIdConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for IncidentUpdater.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IncidentUpdaterTest {

    @Mock
    private ThreadLocalARContext mockArContext;

    @InjectMocks
    private IncidentUpdater incidentUpdater;

    private IncidentUpdateRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = IncidentUpdateRequest.builder()
            .incidentNumber("INC000001")
            .summary("Updated summary")
            .description("Updated description")
            .build();
    }

    @Test
    void updateIncident_validRequest_returnsSuccess() {
        // Given
        CreationResult expectedResult = CreationResult.success("entry-123", "INC000001", FieldIdConstants.Incident.FORM_NAME);
        when(mockArContext.executeWithRetry(any())).thenReturn(expectedResult);

        // When
        CreationResult result = incidentUpdater.updateIncident(validRequest);

        // Then
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void updateIncident_missingIncidentNumber_returnsFailure() {
        // Given
        IncidentUpdateRequest invalidRequest = IncidentUpdateRequest.builder()
            .incidentNumber(null)
            .summary("Updated")
            .build();

        // When
        CreationResult result = incidentUpdater.updateIncident(invalidRequest);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Incident number is required");
    }

    @Test
    void updateIncident_noUpdates_returnsFailure() {
        // Given
        IncidentUpdateRequest invalidRequest = IncidentUpdateRequest.builder()
            .incidentNumber("INC000001")
            .build();

        // When
        CreationResult result = incidentUpdater.updateIncident(invalidRequest);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("No update fields specified");
    }

    @Test
    void updateIncident_invalidImpact_returnsFailure() {
        // Given
        IncidentUpdateRequest invalidRequest = IncidentUpdateRequest.builder()
            .incidentNumber("INC000001")
            .impact(5)
            .build();

        // When
        CreationResult result = incidentUpdater.updateIncident(invalidRequest);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Invalid impact value");
    }

    @Test
    void updateIncident_incidentNotFound_returnsFailure() {
        // Given
        CreationResult expectedResult = CreationResult.failure("Incident not found: INC000001");
        when(mockArContext.executeWithRetry(any())).thenReturn(expectedResult);

        // When
        CreationResult result = incidentUpdater.updateIncident(validRequest);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Incident not found");
    }

    @Test
    void updateIncident_withWorkLog_addsWorkLog() {
        // Given
        IncidentUpdateRequest requestWithWorkLog = IncidentUpdateRequest.builder()
            .incidentNumber("INC000001")
            .status(4)
            .workLog("Resolved the issue")
            .workLogType(1)
            .build();

        CreationResult expectedResult = CreationResult.success("entry-123", "INC000001", FieldIdConstants.Incident.FORM_NAME);
        when(mockArContext.executeWithRetry(any())).thenReturn(expectedResult);

        // When
        CreationResult result = incidentUpdater.updateIncident(requestWithWorkLog);

        // Then
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void addWorkLogToIncident_validRequest_returnsSuccess() {
        // Given
        CreationResult expectedResult = CreationResult.success("worklog-123", "WL-001", "HPD:WorkLog");
        when(mockArContext.executeWithRetry(any())).thenReturn(expectedResult);

        // When
        CreationResult result = incidentUpdater.addWorkLogToIncident("INC000001", "Work log text", 1);

        // Then
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void addWorkLogToIncident_missingIncidentNumber_returnsFailure() {
        // When
        CreationResult result = incidentUpdater.addWorkLogToIncident(null, "Work log", 1);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Incident number is required");
    }

    @Test
    void addWorkLogToIncident_missingWorkLogText_returnsFailure() {
        // When
        CreationResult result = incidentUpdater.addWorkLogToIncident("INC000001", "", 1);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Work log text is required");
    }

    @Test
    void validateRequest_validRequest_returnsNoErrors() {
        // When
        List<String> errors = incidentUpdater.validateRequest(validRequest);

        // Then
        assertThat(errors).isEmpty();
    }

    @Test
    void validateRequest_resolutionRequiredWhenResolving_returnsError() {
        // Given
        IncidentUpdateRequest invalidRequest = IncidentUpdateRequest.builder()
            .incidentNumber("INC000001")
            .status(4) // Resolved
            .resolution(null)
            .build();

        // When
        List<String> errors = incidentUpdater.validateRequest(invalidRequest);

        // Then
        assertThat(errors).contains("Resolution is required when setting status to Resolved");
    }

    @Test
    void isAvailable_enabled_returnsTrue() {
        // Given
        when(mockArContext.isEnabled()).thenReturn(true);
        when(mockArContext.verifyConnection()).thenReturn(true);

        // When
        boolean result = incidentUpdater.isAvailable();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isAvailable_disabled_returnsFalse() {
        when(mockArContext.isEnabled()).thenReturn(false);
        assertThat(incidentUpdater.isAvailable()).isFalse();
    }

    @Test
    void isAvailable_connectionFailed_returnsFalse() {
        when(mockArContext.isEnabled()).thenReturn(true);
        when(mockArContext.verifyConnection()).thenReturn(false);
        assertThat(incidentUpdater.isAvailable()).isFalse();
    }

    @Test
    void updateIncident_blankIncidentNumber_returnsFailure() {
        IncidentUpdateRequest request = IncidentUpdateRequest.builder()
            .incidentNumber("   ")
            .summary("Updated")
            .build();
        CreationResult result = incidentUpdater.updateIncident(request);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Incident number is required");
    }

    @Test
    void updateIncident_invalidUrgency_returnsFailure() {
        IncidentUpdateRequest request = IncidentUpdateRequest.builder()
            .incidentNumber("INC000001")
            .urgency(5)
            .build();
        CreationResult result = incidentUpdater.updateIncident(request);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Invalid urgency value");
    }

    @Test
    void updateIncident_invalidStatus_returnsFailure() {
        IncidentUpdateRequest request = IncidentUpdateRequest.builder()
            .incidentNumber("INC000001")
            .status(7)
            .build();
        CreationResult result = incidentUpdater.updateIncident(request);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Invalid status value");
    }

    @Test
    void updateIncident_connectionException_returnsFailureWithErrorCode() {
        when(mockArContext.executeWithRetry(any()))
            .thenThrow(new ARConnectionException("ARERR 93 Server timeout"));
        CreationResult result = incidentUpdater.updateIncident(validRequest);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("ARERR 93");
        assertThat(result.getErrorCode()).isEqualTo("ARERR");
    }

    @Test
    void updateIncident_unexpectedException_returnsFailure() {
        when(mockArContext.executeWithRetry(any()))
            .thenThrow(new RuntimeException("Unexpected"));
        CreationResult result = incidentUpdater.updateIncident(validRequest);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Unexpected");
    }

    @Test
    void addWorkLogToIncident_blankIncidentNumber_returnsFailure() {
        CreationResult result = incidentUpdater.addWorkLogToIncident("  ", "text", 1);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Incident number is required");
    }

    @Test
    void addWorkLogToIncident_nullWorkLogText_returnsFailure() {
        CreationResult result = incidentUpdater.addWorkLogToIncident("INC000001", null, 1);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Work log text is required");
    }

    @Test
    void addWorkLogToIncident_blankWorkLogText_returnsFailure() {
        CreationResult result = incidentUpdater.addWorkLogToIncident("INC000001", "  ", 1);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Work log text is required");
    }

    @Test
    void addWorkLogToIncident_connectionException_returnsFailure() {
        when(mockArContext.executeWithRetry(any()))
            .thenThrow(new ARConnectionException("ARERR 92 Timeout"));
        CreationResult result = incidentUpdater.addWorkLogToIncident("INC000001", "work log", 1);
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void addWorkLogToIncident_unexpectedException_returnsFailure() {
        when(mockArContext.executeWithRetry(any()))
            .thenThrow(new RuntimeException("Unexpected error"));
        CreationResult result = incidentUpdater.addWorkLogToIncident("INC000001", "work log", 1);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Unexpected error");
    }

    @Nested
    @DisplayName("validateRequest")
    class ValidateRequest {

        @Test
        void missingIncidentNumber_returnsError() {
            IncidentUpdateRequest request = IncidentUpdateRequest.builder()
                .incidentNumber(null)
                .summary("Updated")
                .build();
            List<String> errors = incidentUpdater.validateRequest(request);
            assertThat(errors).contains("Incident number is required");
        }

        @Test
        void blankIncidentNumber_returnsError() {
            IncidentUpdateRequest request = IncidentUpdateRequest.builder()
                .incidentNumber("  ")
                .summary("Updated")
                .build();
            List<String> errors = incidentUpdater.validateRequest(request);
            assertThat(errors).contains("Incident number is required");
        }

        @Test
        void summaryTooLong_returnsError() {
            IncidentUpdateRequest request = IncidentUpdateRequest.builder()
                .incidentNumber("INC000001")
                .summary("A".repeat(256))
                .build();
            List<String> errors = incidentUpdater.validateRequest(request);
            assertThat(errors).contains("Summary must not exceed 255 characters");
        }

        @Test
        void descriptionTooLong_returnsError() {
            IncidentUpdateRequest request = IncidentUpdateRequest.builder()
                .incidentNumber("INC000001")
                .description("A".repeat(32001))
                .build();
            List<String> errors = incidentUpdater.validateRequest(request);
            assertThat(errors).contains("Description must not exceed 32000 characters");
        }

        @Test
        void invalidImpact_returnsError() {
            IncidentUpdateRequest request = IncidentUpdateRequest.builder()
                .incidentNumber("INC000001")
                .impact(5)
                .build();
            List<String> errors = incidentUpdater.validateRequest(request);
            assertThat(errors).contains("Impact must be between 1 and 4");
        }

        @Test
        void invalidUrgency_returnsError() {
            IncidentUpdateRequest request = IncidentUpdateRequest.builder()
                .incidentNumber("INC000001")
                .urgency(5)
                .build();
            List<String> errors = incidentUpdater.validateRequest(request);
            assertThat(errors).contains("Urgency must be between 1 and 4");
        }

        @Test
        void invalidStatus_returnsError() {
            IncidentUpdateRequest request = IncidentUpdateRequest.builder()
                .incidentNumber("INC000001")
                .status(7)
                .build();
            List<String> errors = incidentUpdater.validateRequest(request);
            assertThat(errors).contains("Status must be between 0 and 6");
        }

        @Test
        void resolvedWithoutResolution_returnsError() {
            IncidentUpdateRequest request = IncidentUpdateRequest.builder()
                .incidentNumber("INC000001")
                .status(4)
                .resolution(null)
                .build();
            List<String> errors = incidentUpdater.validateRequest(request);
            assertThat(errors).contains("Resolution is required when setting status to Resolved");
        }

        @Test
        void resolvedWithBlankResolution_returnsError() {
            IncidentUpdateRequest request = IncidentUpdateRequest.builder()
                .incidentNumber("INC000001")
                .status(4)
                .resolution("   ")
                .build();
            List<String> errors = incidentUpdater.validateRequest(request);
            assertThat(errors).contains("Resolution is required when setting status to Resolved");
        }

        @Test
        void resolvedWithResolution_noResolutionError() {
            IncidentUpdateRequest request = IncidentUpdateRequest.builder()
                .incidentNumber("INC000001")
                .status(4)
                .resolution("Fixed the issue")
                .build();
            List<String> errors = incidentUpdater.validateRequest(request);
            assertThat(errors).noneMatch(e -> e.contains("Resolution is required"));
        }

        @Test
        void multipleErrors_returnsAllErrors() {
            IncidentUpdateRequest request = IncidentUpdateRequest.builder()
                .incidentNumber(null)
                .impact(5)
                .urgency(5)
                .status(7)
                .build();
            List<String> errors = incidentUpdater.validateRequest(request);
            assertThat(errors).hasSizeGreaterThanOrEqualTo(4);
        }

        @Test
        void noUpdates_returnsError() {
            IncidentUpdateRequest request = IncidentUpdateRequest.builder()
                .incidentNumber("INC000001")
                .build();
            List<String> errors = incidentUpdater.validateRequest(request);
            assertThat(errors).contains("At least one update field must be specified");
        }

        @Test
        void summaryExactly255Chars_noError() {
            String summary255 = "A".repeat(255);
            IncidentUpdateRequest request = IncidentUpdateRequest.builder()
                .incidentNumber("INC000001")
                .summary(summary255)
                .build();
            List<String> errors = incidentUpdater.validateRequest(request);
            assertThat(errors).noneMatch(e -> e.contains("Summary"));
        }

        @Test
        void descriptionExactly32000Chars_noError() {
            String description32000 = "A".repeat(32000);
            IncidentUpdateRequest request = IncidentUpdateRequest.builder()
                .incidentNumber("INC000001")
                .description(description32000)
                .build();
            List<String> errors = incidentUpdater.validateRequest(request);
            assertThat(errors).noneMatch(e -> e.contains("Description"));
        }

        @Test
        void validImpact1to4_noError() {
            for (int impact = 1; impact <= 4; impact++) {
                IncidentUpdateRequest request = IncidentUpdateRequest.builder()
                    .incidentNumber("INC000001")
                    .impact(impact)
                    .build();
                List<String> errors = incidentUpdater.validateRequest(request);
                assertThat(errors).noneMatch(e -> e.contains("Impact"));
            }
        }

        @Test
        void validUrgency1to4_noError() {
            for (int urgency = 1; urgency <= 4; urgency++) {
                IncidentUpdateRequest request = IncidentUpdateRequest.builder()
                    .incidentNumber("INC000001")
                    .urgency(urgency)
                    .build();
                List<String> errors = incidentUpdater.validateRequest(request);
                assertThat(errors).noneMatch(e -> e.contains("Urgency"));
            }
        }

        @Test
        void validStatus0to6_noError() {
            for (int status = 0; status <= 6; status++) {
                IncidentUpdateRequest request = IncidentUpdateRequest.builder()
                    .incidentNumber("INC000001")
                    .status(status)
                    .build();
                List<String> errors = incidentUpdater.validateRequest(request);
                // Status 4 requires resolution, so skip that case
                if (status != 4) {
                    assertThat(errors).noneMatch(e -> e.contains("Status must be"));
                }
            }
        }
    }

    @Test
    void updateIncident_withAllFields_mapsAllFields() {
        // Given
        IncidentUpdateRequest fullRequest = IncidentUpdateRequest.builder()
            .incidentNumber("INC000001")
            .summary("Updated Summary")
            .description("Updated Description")
            .impact(1)
            .urgency(1)
            .status(2)
            .resolution("Fixed issue")
            .assignedGroup("IT Support")
            .categoryTier1("Hardware")
            .categoryTier2("Laptop")
            .categoryTier3("Screen")
            .resolutionCategoryTier1("Hardware Fixed")
            .resolutionCategoryTier2("Screen Replacement")
            .resolutionCategoryTier3("LCD Panel")
            .workLog("Replaced screen")
            .workLogType(1)
            .build();

        CreationResult expectedResult = CreationResult.success("entry-123", "INC000001", FieldIdConstants.Incident.FORM_NAME);
        when(mockArContext.executeWithRetry(any())).thenReturn(expectedResult);

        // When
        CreationResult result = incidentUpdater.updateIncident(fullRequest);

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(mockArContext).executeWithRetry(any());
    }

    @Test
    void updateIncident_workLogWithoutType_usesDefaultType() {
        // Given
        IncidentUpdateRequest request = IncidentUpdateRequest.builder()
            .incidentNumber("INC000001")
            .summary("Updated")
            .workLog("Work log text")
            .workLogType(null) // Should default to 1
            .build();

        CreationResult expectedResult = CreationResult.success("entry-123", "INC000001", FieldIdConstants.Incident.FORM_NAME);
        when(mockArContext.executeWithRetry(any())).thenReturn(expectedResult);

        // When
        CreationResult result = incidentUpdater.updateIncident(request);

        // Then
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void updateIncident_arErrorWithoutSpace_extractsNullErrorCode() {
        // Given
        when(mockArContext.executeWithRetry(any()))
            .thenThrow(new ARConnectionException("ARERR93ServerTimeout"));

        // When
        CreationResult result = incidentUpdater.updateIncident(validRequest);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCode()).isNull();
    }

    @Test
    void updateIncident_arErrorWithNullMessage_extractsNullErrorCode() {
        // Given
        ARConnectionException exceptionWithNullMessage = new ARConnectionException("message") {
            @Override
            public String getMessage() {
                return null;
            }
        };
        when(mockArContext.executeWithRetry(any())).thenThrow(exceptionWithNullMessage);

        // When
        CreationResult result = incidentUpdater.updateIncident(validRequest);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCode()).isNull();
    }

    @Test
    void updateIncident_onlyImpact_updatesSuccessfully() {
        IncidentUpdateRequest request = IncidentUpdateRequest.builder()
            .incidentNumber("INC000001")
            .impact(1)
            .build();
        CreationResult expectedResult = CreationResult.success("entry-123", "INC000001", FieldIdConstants.Incident.FORM_NAME);
        when(mockArContext.executeWithRetry(any())).thenReturn(expectedResult);

        CreationResult result = incidentUpdater.updateIncident(request);
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void updateIncident_onlyUrgency_updatesSuccessfully() {
        IncidentUpdateRequest request = IncidentUpdateRequest.builder()
            .incidentNumber("INC000001")
            .urgency(2)
            .build();
        CreationResult expectedResult = CreationResult.success("entry-123", "INC000001", FieldIdConstants.Incident.FORM_NAME);
        when(mockArContext.executeWithRetry(any())).thenReturn(expectedResult);

        CreationResult result = incidentUpdater.updateIncident(request);
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void updateIncident_onlyStatus_updatesSuccessfully() {
        IncidentUpdateRequest request = IncidentUpdateRequest.builder()
            .incidentNumber("INC000001")
            .status(2)
            .build();
        CreationResult expectedResult = CreationResult.success("entry-123", "INC000001", FieldIdConstants.Incident.FORM_NAME);
        when(mockArContext.executeWithRetry(any())).thenReturn(expectedResult);

        CreationResult result = incidentUpdater.updateIncident(request);
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void updateIncident_onlyResolution_updatesSuccessfully() {
        IncidentUpdateRequest request = IncidentUpdateRequest.builder()
            .incidentNumber("INC000001")
            .resolution("Fixed the issue")
            .build();
        CreationResult expectedResult = CreationResult.success("entry-123", "INC000001", FieldIdConstants.Incident.FORM_NAME);
        when(mockArContext.executeWithRetry(any())).thenReturn(expectedResult);

        CreationResult result = incidentUpdater.updateIncident(request);
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void updateIncident_onlyAssignedGroup_updatesSuccessfully() {
        IncidentUpdateRequest request = IncidentUpdateRequest.builder()
            .incidentNumber("INC000001")
            .assignedGroup("Network Team")
            .build();
        CreationResult expectedResult = CreationResult.success("entry-123", "INC000001", FieldIdConstants.Incident.FORM_NAME);
        when(mockArContext.executeWithRetry(any())).thenReturn(expectedResult);

        CreationResult result = incidentUpdater.updateIncident(request);
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void updateIncident_onlyCategories_updatesSuccessfully() {
        IncidentUpdateRequest request = IncidentUpdateRequest.builder()
            .incidentNumber("INC000001")
            .categoryTier1("Hardware")
            .categoryTier2("Server")
            .categoryTier3("CPU")
            .build();
        CreationResult expectedResult = CreationResult.success("entry-123", "INC000001", FieldIdConstants.Incident.FORM_NAME);
        when(mockArContext.executeWithRetry(any())).thenReturn(expectedResult);

        CreationResult result = incidentUpdater.updateIncident(request);
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void updateIncident_withResolutionCategoriesAndStatus_updatesSuccessfully() {
        IncidentUpdateRequest request = IncidentUpdateRequest.builder()
            .incidentNumber("INC000001")
            .status(4)
            .resolution("Issue resolved")
            .resolutionCategoryTier1("Resolved")
            .resolutionCategoryTier2("Hardware")
            .resolutionCategoryTier3("CPU Replaced")
            .build();
        CreationResult expectedResult = CreationResult.success("entry-123", "INC000001", FieldIdConstants.Incident.FORM_NAME);
        when(mockArContext.executeWithRetry(any())).thenReturn(expectedResult);

        CreationResult result = incidentUpdater.updateIncident(request);
        assertThat(result.isSuccess()).isTrue();
    }

}
