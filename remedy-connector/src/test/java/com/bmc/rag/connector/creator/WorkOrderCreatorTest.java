package com.bmc.rag.connector.creator;

import com.bmc.rag.connector.connection.ThreadLocalARContext;
import com.bmc.rag.connector.connection.ThreadLocalARContext.ARConnectionException;
import com.bmc.rag.connector.dto.CreationResult;
import com.bmc.rag.connector.dto.WorkOrderCreationRequest;
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

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WorkOrderCreator.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkOrderCreatorTest {

    @Mock
    private ThreadLocalARContext mockArContext;

    @InjectMocks
    private WorkOrderCreator workOrderCreator;

    private WorkOrderCreationRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = WorkOrderCreationRequest.builder()
            .summary("Install new server")
            .description("Install and configure new application server")
            .workOrderType(1) // Project Work
            .priority(2) // Medium
            .build();
    }

    @Test
    void createWorkOrder_validRequest_returnsSuccess() {
        // Given
        String expectedEntryId = "entry-456";
        String expectedWorkOrderId = "WO0000001";
        CreationResult expectedResult = CreationResult.success(expectedEntryId, expectedWorkOrderId, FieldIdConstants.WorkOrder.FORM_NAME);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedResult);

        // When
        CreationResult result = workOrderCreator.createWorkOrder(validRequest);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getEntryId()).isEqualTo(expectedEntryId);
        assertThat(result.getRecordId()).isEqualTo(expectedWorkOrderId);
        assertThat(result.getFormName()).isEqualTo(FieldIdConstants.WorkOrder.FORM_NAME);
    }

    @Test
    void createWorkOrder_invalidWorkOrderType_returnsFailure() {
        // Given
        WorkOrderCreationRequest invalidRequest = WorkOrderCreationRequest.builder()
            .summary("Test")
            .description("Test")
            .workOrderType(5) // Invalid
            .priority(2)
            .build();

        // When
        CreationResult result = workOrderCreator.createWorkOrder(invalidRequest);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Invalid work order type");
    }

    @Test
    void createWorkOrder_invalidPriority_returnsFailure() {
        // Given
        WorkOrderCreationRequest invalidRequest = WorkOrderCreationRequest.builder()
            .summary("Test")
            .description("Test")
            .workOrderType(1)
            .priority(4) // Invalid
            .build();

        // When
        CreationResult result = workOrderCreator.createWorkOrder(invalidRequest);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Invalid priority value");
    }

    @Test
    void validateRequest_endDateBeforeStartDate_returnsError() {
        // Given
        Instant now = Instant.now();
        WorkOrderCreationRequest invalidRequest = WorkOrderCreationRequest.builder()
            .summary("Test")
            .description("Test")
            .workOrderType(1)
            .priority(2)
            .scheduledStartDate(now)
            .scheduledEndDate(now.minusSeconds(3600)) // Before start
            .build();

        // When
        List<String> errors = workOrderCreator.validateRequest(invalidRequest);

        // Then
        assertThat(errors).contains("Scheduled end date cannot be before start date");
    }

    @Test
    void isAvailable_enabled_returnsTrue() {
        // Given
        when(mockArContext.isEnabled()).thenReturn(true);
        when(mockArContext.verifyConnection()).thenReturn(true);

        // When
        boolean result = workOrderCreator.isAvailable();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isAvailable_disabled_returnsFalse() {
        when(mockArContext.isEnabled()).thenReturn(false);
        assertThat(workOrderCreator.isAvailable()).isFalse();
    }

    @Test
    void isAvailable_connectionFailed_returnsFalse() {
        when(mockArContext.isEnabled()).thenReturn(true);
        when(mockArContext.verifyConnection()).thenReturn(false);
        assertThat(workOrderCreator.isAvailable()).isFalse();
    }

    @Test
    void createWorkOrder_connectionException_returnsFailureWithErrorCode() {
        when(mockArContext.executeWithRetry(any()))
            .thenThrow(new ARConnectionException("ARERR 93 Server timeout"));
        CreationResult result = workOrderCreator.createWorkOrder(validRequest);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("ARERR 93");
    }

    @Test
    void createWorkOrder_unexpectedException_returnsFailure() {
        when(mockArContext.executeWithRetry(any()))
            .thenThrow(new RuntimeException("Unexpected error"));
        CreationResult result = workOrderCreator.createWorkOrder(validRequest);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Unexpected error");
    }

    @Test
    void createWorkOrder_withAllOptionalFields_returnsSuccess() {
        Instant now = Instant.now();
        WorkOrderCreationRequest fullRequest = WorkOrderCreationRequest.builder()
            .summary("Install server")
            .description("Full installation")
            .workOrderType(1)
            .priority(2)
            .requesterFirstName("John")
            .requesterLastName("Doe")
            .locationCompany("Acme Corp")
            .categoryTier1("Infrastructure")
            .categoryTier2("Server")
            .categoryTier3("Install")
            .assignedGroup("Server Team")
            .scheduledStartDate(now)
            .scheduledEndDate(now.plusSeconds(86400))
            .build();

        CreationResult expected = CreationResult.success("e-1", "WO0001", FieldIdConstants.WorkOrder.FORM_NAME);
        when(mockArContext.executeWithRetry(any())).thenReturn(expected);

        CreationResult result = workOrderCreator.createWorkOrder(fullRequest);
        assertThat(result.isSuccess()).isTrue();
        verify(mockArContext).executeWithRetry(any());
    }

    @Nested
    @DisplayName("validateRequest")
    class ValidateRequest {

        @Test
        void validRequest_returnsNoErrors() {
            List<String> errors = workOrderCreator.validateRequest(validRequest);
            assertThat(errors).isEmpty();
        }

        @Test
        void nullSummary_returnsError() {
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                .summary(null).description("Desc").workOrderType(1).priority(2).build();
            List<String> errors = workOrderCreator.validateRequest(request);
            assertThat(errors).contains("Summary is required");
        }

        @Test
        void blankSummary_returnsError() {
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                .summary("  ").description("Desc").workOrderType(1).priority(2).build();
            List<String> errors = workOrderCreator.validateRequest(request);
            assertThat(errors).contains("Summary is required");
        }

        @Test
        void summaryTooLong_returnsError() {
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                .summary("A".repeat(256)).description("Desc").workOrderType(1).priority(2).build();
            List<String> errors = workOrderCreator.validateRequest(request);
            assertThat(errors).contains("Summary must not exceed 255 characters");
        }

        @Test
        void nullDescription_returnsError() {
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                .summary("Test").description(null).workOrderType(1).priority(2).build();
            List<String> errors = workOrderCreator.validateRequest(request);
            assertThat(errors).contains("Description is required");
        }

        @Test
        void blankDescription_returnsError() {
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                .summary("Test").description("  ").workOrderType(1).priority(2).build();
            List<String> errors = workOrderCreator.validateRequest(request);
            assertThat(errors).contains("Description is required");
        }

        @Test
        void descriptionTooLong_returnsError() {
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                .summary("Test").description("A".repeat(32001)).workOrderType(1).priority(2).build();
            List<String> errors = workOrderCreator.validateRequest(request);
            assertThat(errors).contains("Description must not exceed 32000 characters");
        }

        @Test
        void invalidWorkOrderType_returnsError() {
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                .summary("Test").description("Desc").workOrderType(5).priority(2).build();
            List<String> errors = workOrderCreator.validateRequest(request);
            assertThat(errors).contains("Work order type must be between 0 and 4");
        }

        @Test
        void invalidPriority_returnsError() {
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                .summary("Test").description("Desc").workOrderType(1).priority(4).build();
            List<String> errors = workOrderCreator.validateRequest(request);
            assertThat(errors).contains("Priority must be between 0 and 3");
        }

        @Test
        void validDates_noError() {
            Instant now = Instant.now();
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                .summary("Test").description("Desc").workOrderType(1).priority(2)
                .scheduledStartDate(now).scheduledEndDate(now.plusSeconds(3600)).build();
            List<String> errors = workOrderCreator.validateRequest(request);
            assertThat(errors).noneMatch(e -> e.contains("date"));
        }

        @Test
        void multipleErrors_returnsAll() {
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                .summary(null).description(null).workOrderType(5).priority(4).build();
            List<String> errors = workOrderCreator.validateRequest(request);
            assertThat(errors).hasSizeGreaterThanOrEqualTo(4);
        }

        @Test
        void summaryExactly255Chars_noError() {
            String summary255 = "A".repeat(255);
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                .summary(summary255).description("Desc").workOrderType(1).priority(2).build();
            List<String> errors = workOrderCreator.validateRequest(request);
            assertThat(errors).noneMatch(e -> e.contains("Summary"));
        }

        @Test
        void descriptionExactly32000Chars_noError() {
            String description32000 = "A".repeat(32000);
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                .summary("Test").description(description32000).workOrderType(1).priority(2).build();
            List<String> errors = workOrderCreator.validateRequest(request);
            assertThat(errors).noneMatch(e -> e.contains("Description"));
        }

        @Test
        void validWorkOrderType0to4_noError() {
            for (int type = 0; type <= 4; type++) {
                WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test").description("Desc").workOrderType(type).priority(2).build();
                List<String> errors = workOrderCreator.validateRequest(request);
                assertThat(errors).noneMatch(e -> e.contains("Work order type"));
            }
        }

        @Test
        void validPriority0to3_noError() {
            for (int priority = 0; priority <= 3; priority++) {
                WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test").description("Desc").workOrderType(1).priority(priority).build();
                List<String> errors = workOrderCreator.validateRequest(request);
                assertThat(errors).noneMatch(e -> e.contains("Priority must be"));
            }
        }

        @Test
        void sameDates_noError() {
            Instant now = Instant.now();
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                .summary("Test").description("Desc").workOrderType(1).priority(2)
                .scheduledStartDate(now).scheduledEndDate(now).build();
            List<String> errors = workOrderCreator.validateRequest(request);
            assertThat(errors).noneMatch(e -> e.contains("date"));
        }

        @Test
        void onlyStartDate_noError() {
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                .summary("Test").description("Desc").workOrderType(1).priority(2)
                .scheduledStartDate(Instant.now()).build();
            List<String> errors = workOrderCreator.validateRequest(request);
            assertThat(errors).isEmpty();
        }

        @Test
        void onlyEndDate_noError() {
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                .summary("Test").description("Desc").workOrderType(1).priority(2)
                .scheduledEndDate(Instant.now()).build();
            List<String> errors = workOrderCreator.validateRequest(request);
            assertThat(errors).isEmpty();
        }
    }

    @Test
    void createWorkOrder_arErrorWithNullMessage_extractsNullErrorCode() {
        // Given
        ARConnectionException exceptionWithNullMessage = new ARConnectionException("message") {
            @Override
            public String getMessage() {
                return null;
            }
        };
        when(mockArContext.executeWithRetry(any())).thenThrow(exceptionWithNullMessage);

        // When
        CreationResult result = workOrderCreator.createWorkOrder(validRequest);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCode()).isNull();
    }

    @Test
    void createWorkOrder_arErrorWithoutSpace_extractsNullErrorCode() {
        // Given
        when(mockArContext.executeWithRetry(any()))
            .thenThrow(new ARConnectionException("ARERR93Timeout"));

        // When
        CreationResult result = workOrderCreator.createWorkOrder(validRequest);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCode()).isNull();
    }

    @Test
    void createWorkOrder_negativeWorkOrderType_returnsFailure() {
        // Given
        WorkOrderCreationRequest invalidRequest = WorkOrderCreationRequest.builder()
            .summary("Test")
            .description("Test")
            .workOrderType(-1)
            .priority(2)
            .build();

        // When
        CreationResult result = workOrderCreator.createWorkOrder(invalidRequest);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Invalid work order type");
    }

    @Test
    void createWorkOrder_negativePriority_returnsFailure() {
        // Given
        WorkOrderCreationRequest invalidRequest = WorkOrderCreationRequest.builder()
            .summary("Test")
            .description("Test")
            .workOrderType(1)
            .priority(-1)
            .build();

        // When
        CreationResult result = workOrderCreator.createWorkOrder(invalidRequest);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Invalid priority value");
    }

    @Test
    void createWorkOrder_withRequesterInfo_mapsCorrectly() {
        Instant now = Instant.now();
        WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
            .summary("Requester test")
            .description("Test with requester")
            .workOrderType(1)
            .priority(2)
            .requesterFirstName("Bob")
            .requesterLastName("Johnson")
            .locationCompany("Tech Solutions Inc")
            .build();

        CreationResult expected = CreationResult.success("e-1", "WO0001", FieldIdConstants.WorkOrder.FORM_NAME);
        when(mockArContext.executeWithRetry(any())).thenReturn(expected);

        CreationResult result = workOrderCreator.createWorkOrder(request);
        assertThat(result.isSuccess()).isTrue();
        verify(mockArContext).executeWithRetry(any());
    }

    @Test
    void createWorkOrder_withCategories_mapsCorrectly() {
        WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
            .summary("Category test")
            .description("Test with categories")
            .workOrderType(1)
            .priority(2)
            .categoryTier1("Maintenance")
            .categoryTier2("Preventive")
            .categoryTier3("Quarterly")
            .build();

        CreationResult expected = CreationResult.success("e-1", "WO0001", FieldIdConstants.WorkOrder.FORM_NAME);
        when(mockArContext.executeWithRetry(any())).thenReturn(expected);

        CreationResult result = workOrderCreator.createWorkOrder(request);
        assertThat(result.isSuccess()).isTrue();
        verify(mockArContext).executeWithRetry(any());
    }

    @Test
    void createWorkOrder_withAssignedGroup_mapsCorrectly() {
        WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
            .summary("Assignment test")
            .description("Test with assignment")
            .workOrderType(1)
            .priority(2)
            .assignedGroup("Maintenance Team")
            .build();

        CreationResult expected = CreationResult.success("e-1", "WO0001", FieldIdConstants.WorkOrder.FORM_NAME);
        when(mockArContext.executeWithRetry(any())).thenReturn(expected);

        CreationResult result = workOrderCreator.createWorkOrder(request);
        assertThat(result.isSuccess()).isTrue();
        verify(mockArContext).executeWithRetry(any());
    }

    @Test
    void createWorkOrder_withStartDateOnly_mapsCorrectly() {
        Instant now = Instant.now();
        WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
            .summary("Start date test")
            .description("Test with start date only")
            .workOrderType(1)
            .priority(2)
            .scheduledStartDate(now)
            .build();

        CreationResult expected = CreationResult.success("e-1", "WO0001", FieldIdConstants.WorkOrder.FORM_NAME);
        when(mockArContext.executeWithRetry(any())).thenReturn(expected);

        CreationResult result = workOrderCreator.createWorkOrder(request);
        assertThat(result.isSuccess()).isTrue();
        verify(mockArContext).executeWithRetry(any());
    }

    @Test
    void createWorkOrder_withEndDateOnly_mapsCorrectly() {
        Instant now = Instant.now();
        WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
            .summary("End date test")
            .description("Test with end date only")
            .workOrderType(1)
            .priority(2)
            .scheduledEndDate(now)
            .build();

        CreationResult expected = CreationResult.success("e-1", "WO0001", FieldIdConstants.WorkOrder.FORM_NAME);
        when(mockArContext.executeWithRetry(any())).thenReturn(expected);

        CreationResult result = workOrderCreator.createWorkOrder(request);
        assertThat(result.isSuccess()).isTrue();
        verify(mockArContext).executeWithRetry(any());
    }

}
