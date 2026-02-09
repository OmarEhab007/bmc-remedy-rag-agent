package com.bmc.rag.connector.creator;

import com.bmc.rag.connector.connection.ThreadLocalARContext;
import com.bmc.rag.connector.connection.ThreadLocalARContext.ARConnectionException;
import com.bmc.rag.connector.dto.CreationResult;
import com.bmc.rag.connector.dto.IncidentCreationRequest;
import com.bmc.rag.connector.util.FieldIdConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for IncidentCreator.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IncidentCreatorTest {

    @Mock
    private ThreadLocalARContext mockArContext;

    @InjectMocks
    private IncidentCreator incidentCreator;

    private IncidentCreationRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = IncidentCreationRequest.builder()
            .summary("VPN connection issue")
            .description("User cannot connect to VPN from home office")
            .impact(2)
            .urgency(3)
            .requesterFirstName("John")
            .requesterLastName("Doe")
            .requesterCompany("Acme Corp")
            .categoryTier1("Infrastructure")
            .categoryTier2("Network")
            .categoryTier3("VPN")
            .assignedGroup("Network Support")
            .build();
    }

    @Test
    void createIncident_validRequest_returnsSuccess() {
        // Given
        String expectedEntryId = "entry-123";
        String expectedIncidentNumber = "INC000001";
        CreationResult expectedResult = CreationResult.success(expectedEntryId, expectedIncidentNumber, FieldIdConstants.Incident.FORM_NAME);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedResult);

        // When
        CreationResult result = incidentCreator.createIncident(validRequest);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getEntryId()).isEqualTo(expectedEntryId);
        assertThat(result.getRecordId()).isEqualTo(expectedIncidentNumber);
        assertThat(result.getFormName()).isEqualTo(FieldIdConstants.Incident.FORM_NAME);
    }

    @Test
    void createIncident_invalidImpact_returnsFailure() {
        // Given
        IncidentCreationRequest invalidRequest = IncidentCreationRequest.builder()
            .summary("Test")
            .description("Test")
            .impact(5) // Invalid
            .urgency(3)
            .build();

        // When
        CreationResult result = incidentCreator.createIncident(invalidRequest);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Invalid impact value");
    }

    @Test
    void createIncident_invalidUrgency_returnsFailure() {
        // Given
        IncidentCreationRequest invalidRequest = IncidentCreationRequest.builder()
            .summary("Test")
            .description("Test")
            .impact(2)
            .urgency(0) // Invalid
            .build();

        // When
        CreationResult result = incidentCreator.createIncident(invalidRequest);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Invalid urgency value");
    }

    @Test
    void createIncident_connectionFailure_returnsFailure() {
        // Given
        when(mockArContext.executeWithRetry(any()))
            .thenThrow(new ARConnectionException("Connection failed"));

        // When
        CreationResult result = incidentCreator.createIncident(validRequest);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Connection failed");
    }

    @Test
    void createIncident_arException_returnsFailure() {
        // Given
        when(mockArContext.executeWithRetry(any()))
            .thenThrow(new ARConnectionException("ARERR 93: Timeout"));

        // When
        CreationResult result = incidentCreator.createIncident(validRequest);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("ARERR 93");
    }

    @Test
    void createIncident_allOptionalFields_mapsCorrectly() {
        // Given
        IncidentCreationRequest fullRequest = IncidentCreationRequest.builder()
            .summary("Test")
            .description("Test Description")
            .impact(2)
            .urgency(3)
            .requesterFirstName("John")
            .requesterLastName("Doe")
            .requesterCompany("Acme Corp")
            .categoryTier1("Infrastructure")
            .categoryTier2("Network")
            .categoryTier3("VPN")
            .assignedGroup("Network Support")
            .serviceType("User Service Request")
            .configurationItem("CI-12345")
            .location("Building A")
            .build();

        String expectedEntryId = "entry-123";
        String expectedIncidentNumber = "INC000001";
        CreationResult expectedResult = CreationResult.success(expectedEntryId, expectedIncidentNumber, FieldIdConstants.Incident.FORM_NAME);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedResult);

        // When
        CreationResult result = incidentCreator.createIncident(fullRequest);

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(mockArContext).executeWithRetry(any());
    }

    @Test
    void validateRequest_validRequest_returnsNoErrors() {
        // When
        List<String> errors = incidentCreator.validateRequest(validRequest);

        // Then
        assertThat(errors).isEmpty();
    }

    @Test
    void validateRequest_missingSummary_returnsError() {
        // Given
        IncidentCreationRequest invalidRequest = IncidentCreationRequest.builder()
            .summary(null)
            .description("Test")
            .impact(2)
            .urgency(3)
            .build();

        // When
        List<String> errors = incidentCreator.validateRequest(invalidRequest);

        // Then
        assertThat(errors).contains("Summary is required");
    }

    @Test
    void validateRequest_summaryTooLong_returnsError() {
        // Given
        String longSummary = "A".repeat(256);
        IncidentCreationRequest invalidRequest = IncidentCreationRequest.builder()
            .summary(longSummary)
            .description("Test")
            .impact(2)
            .urgency(3)
            .build();

        // When
        List<String> errors = incidentCreator.validateRequest(invalidRequest);

        // Then
        assertThat(errors).contains("Summary must not exceed 255 characters");
    }

    @Test
    void validateRequest_missingDescription_returnsError() {
        // Given
        IncidentCreationRequest invalidRequest = IncidentCreationRequest.builder()
            .summary("Test")
            .description("")
            .impact(2)
            .urgency(3)
            .build();

        // When
        List<String> errors = incidentCreator.validateRequest(invalidRequest);

        // Then
        assertThat(errors).contains("Description is required");
    }

    @Test
    void validateRequest_descriptionTooLong_returnsError() {
        // Given
        String longDescription = "A".repeat(32001);
        IncidentCreationRequest invalidRequest = IncidentCreationRequest.builder()
            .summary("Test")
            .description(longDescription)
            .impact(2)
            .urgency(3)
            .build();

        // When
        List<String> errors = incidentCreator.validateRequest(invalidRequest);

        // Then
        assertThat(errors).contains("Description must not exceed 32000 characters");
    }

    @Test
    void validateRequest_invalidImpact_returnsError() {
        // Given
        IncidentCreationRequest invalidRequest = IncidentCreationRequest.builder()
            .summary("Test")
            .description("Test")
            .impact(5)
            .urgency(3)
            .build();

        // When
        List<String> errors = incidentCreator.validateRequest(invalidRequest);

        // Then
        assertThat(errors).contains("Impact must be between 1 and 4");
    }

    @Test
    void validateRequest_invalidUrgency_returnsError() {
        // Given
        IncidentCreationRequest invalidRequest = IncidentCreationRequest.builder()
            .summary("Test")
            .description("Test")
            .impact(2)
            .urgency(5)
            .build();

        // When
        List<String> errors = incidentCreator.validateRequest(invalidRequest);

        // Then
        assertThat(errors).contains("Urgency must be between 1 and 4");
    }

    @Test
    void isAvailable_enabled_returnsTrue() {
        // Given
        when(mockArContext.isEnabled()).thenReturn(true);
        when(mockArContext.verifyConnection()).thenReturn(true);

        // When
        boolean result = incidentCreator.isAvailable();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isAvailable_disabled_returnsFalse() {
        // Given
        when(mockArContext.isEnabled()).thenReturn(false);

        // When
        boolean result = incidentCreator.isAvailable();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isAvailable_connectionFailed_returnsFalse() {
        // Given
        when(mockArContext.isEnabled()).thenReturn(true);
        when(mockArContext.verifyConnection()).thenReturn(false);

        // When
        boolean result = incidentCreator.isAvailable();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void createIncident_unexpectedException_returnsFailure() {
        // Given
        when(mockArContext.executeWithRetry(any()))
            .thenThrow(new RuntimeException("Unexpected error"));

        // When
        CreationResult result = incidentCreator.createIncident(validRequest);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Unexpected error");
    }

    @Test
    void createIncident_arErrorWithoutErrorCode_extractsNull() {
        // Given
        when(mockArContext.executeWithRetry(any()))
            .thenThrow(new ARConnectionException("Generic error without code"));

        // When
        CreationResult result = incidentCreator.createIncident(validRequest);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCode()).isNull();
    }

    @Test
    void createIncident_arErrorWithIncompleteErrorCode_extractsNull() {
        // Given
        when(mockArContext.executeWithRetry(any()))
            .thenThrow(new ARConnectionException("ARERR"));

        // When
        CreationResult result = incidentCreator.createIncident(validRequest);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCode()).isNull();
    }

    @Test
    void validateRequest_blankSummary_returnsError() {
        // Given
        IncidentCreationRequest invalidRequest = IncidentCreationRequest.builder()
            .summary("  ")
            .description("Test")
            .impact(2)
            .urgency(3)
            .build();

        // When
        List<String> errors = incidentCreator.validateRequest(invalidRequest);

        // Then
        assertThat(errors).contains("Summary is required");
    }

    @Test
    void validateRequest_blankDescription_returnsError() {
        // Given
        IncidentCreationRequest invalidRequest = IncidentCreationRequest.builder()
            .summary("Test")
            .description("   ")
            .impact(2)
            .urgency(3)
            .build();

        // When
        List<String> errors = incidentCreator.validateRequest(invalidRequest);

        // Then
        assertThat(errors).contains("Description is required");
    }

    @Test
    void validateRequest_nullDescription_returnsError() {
        // Given
        IncidentCreationRequest invalidRequest = IncidentCreationRequest.builder()
            .summary("Test")
            .description(null)
            .impact(2)
            .urgency(3)
            .build();

        // When
        List<String> errors = incidentCreator.validateRequest(invalidRequest);

        // Then
        assertThat(errors).contains("Description is required");
    }

    @Test
    void validateRequest_multipleErrors_returnsAllErrors() {
        // Given
        IncidentCreationRequest invalidRequest = IncidentCreationRequest.builder()
            .summary(null)
            .description(null)
            .impact(5)
            .urgency(5)
            .build();

        // When
        List<String> errors = incidentCreator.validateRequest(invalidRequest);

        // Then
        assertThat(errors).hasSizeGreaterThanOrEqualTo(4);
    }

    @Test
    void validateRequest_summaryExactly255Chars_noError() {
        // Given
        String summary255 = "A".repeat(255);
        IncidentCreationRequest validRequest = IncidentCreationRequest.builder()
            .summary(summary255)
            .description("Test")
            .impact(2)
            .urgency(3)
            .build();

        // When
        List<String> errors = incidentCreator.validateRequest(validRequest);

        // Then
        assertThat(errors).noneMatch(e -> e.contains("Summary"));
    }

    @Test
    void validateRequest_descriptionExactly32000Chars_noError() {
        // Given
        String description32000 = "A".repeat(32000);
        IncidentCreationRequest validRequest = IncidentCreationRequest.builder()
            .summary("Test")
            .description(description32000)
            .impact(2)
            .urgency(3)
            .build();

        // When
        List<String> errors = incidentCreator.validateRequest(validRequest);

        // Then
        assertThat(errors).noneMatch(e -> e.contains("Description"));
    }

    @Test
    void createIncident_minimalFields_createsSuccessfully() {
        // Given
        IncidentCreationRequest minimalRequest = IncidentCreationRequest.builder()
            .summary("Minimal incident")
            .description("Minimal description")
            .impact(3)
            .urgency(3)
            .build();

        String expectedEntryId = "entry-minimal";
        String expectedIncidentNumber = "INC000999";
        CreationResult expectedResult = CreationResult.success(expectedEntryId, expectedIncidentNumber, FieldIdConstants.Incident.FORM_NAME);
        when(mockArContext.executeWithRetry(any())).thenReturn(expectedResult);

        // When
        CreationResult result = incidentCreator.createIncident(minimalRequest);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getEntryId()).isEqualTo(expectedEntryId);
    }

    @Test
    void createIncident_withServiceType_mapsCorrectly() {
        // Given
        IncidentCreationRequest request = IncidentCreationRequest.builder()
            .summary("Service type test")
            .description("Test with service type")
            .impact(2)
            .urgency(2)
            .serviceType("User Service Request")
            .build();

        CreationResult expectedResult = CreationResult.success("entry-123", "INC000001", FieldIdConstants.Incident.FORM_NAME);
        when(mockArContext.executeWithRetry(any())).thenReturn(expectedResult);

        // When
        CreationResult result = incidentCreator.createIncident(request);

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(mockArContext).executeWithRetry(any());
    }

    @Test
    void createIncident_withConfigurationItem_mapsCorrectly() {
        // Given
        IncidentCreationRequest request = IncidentCreationRequest.builder()
            .summary("CI test")
            .description("Test with CI")
            .impact(2)
            .urgency(2)
            .configurationItem("CI-SERVER-01")
            .build();

        CreationResult expectedResult = CreationResult.success("entry-123", "INC000001", FieldIdConstants.Incident.FORM_NAME);
        when(mockArContext.executeWithRetry(any())).thenReturn(expectedResult);

        // When
        CreationResult result = incidentCreator.createIncident(request);

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(mockArContext).executeWithRetry(any());
    }

    @Test
    void createIncident_withLocation_mapsCorrectly() {
        // Given
        IncidentCreationRequest request = IncidentCreationRequest.builder()
            .summary("Location test")
            .description("Test with location")
            .impact(2)
            .urgency(2)
            .location("Building A, Floor 3")
            .build();

        CreationResult expectedResult = CreationResult.success("entry-123", "INC000001", FieldIdConstants.Incident.FORM_NAME);
        when(mockArContext.executeWithRetry(any())).thenReturn(expectedResult);

        // When
        CreationResult result = incidentCreator.createIncident(request);

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(mockArContext).executeWithRetry(any());
    }

    @Test
    void createIncident_withRequesterInfo_mapsCorrectly() {
        // Given
        IncidentCreationRequest request = IncidentCreationRequest.builder()
            .summary("Requester test")
            .description("Test with requester")
            .impact(2)
            .urgency(2)
            .requesterFirstName("Jane")
            .requesterLastName("Smith")
            .requesterCompany("Tech Corp")
            .build();

        CreationResult expectedResult = CreationResult.success("entry-123", "INC000001", FieldIdConstants.Incident.FORM_NAME);
        when(mockArContext.executeWithRetry(any())).thenReturn(expectedResult);

        // When
        CreationResult result = incidentCreator.createIncident(request);

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(mockArContext).executeWithRetry(any());
    }

    @Test
    void createIncident_withCategoryTiers_mapsCorrectly() {
        // Given
        IncidentCreationRequest request = IncidentCreationRequest.builder()
            .summary("Category test")
            .description("Test with categories")
            .impact(2)
            .urgency(2)
            .categoryTier1("Software")
            .categoryTier2("Application")
            .categoryTier3("CRM")
            .build();

        CreationResult expectedResult = CreationResult.success("entry-123", "INC000001", FieldIdConstants.Incident.FORM_NAME);
        when(mockArContext.executeWithRetry(any())).thenReturn(expectedResult);

        // When
        CreationResult result = incidentCreator.createIncident(request);

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(mockArContext).executeWithRetry(any());
    }

    @Test
    void createIncident_withAssignedGroup_mapsCorrectly() {
        // Given
        IncidentCreationRequest request = IncidentCreationRequest.builder()
            .summary("Assignment test")
            .description("Test with assignment")
            .impact(2)
            .urgency(2)
            .assignedGroup("Application Support")
            .build();

        CreationResult expectedResult = CreationResult.success("entry-123", "INC000001", FieldIdConstants.Incident.FORM_NAME);
        when(mockArContext.executeWithRetry(any())).thenReturn(expectedResult);

        // When
        CreationResult result = incidentCreator.createIncident(request);

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(mockArContext).executeWithRetry(any());
    }

}
