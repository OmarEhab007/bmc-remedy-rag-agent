package com.bmc.rag.agent.tools;

import com.bmc.rag.agent.confirmation.ConfirmationService;
import com.bmc.rag.agent.confirmation.PendingAction;
import com.bmc.rag.agent.security.AgenticRateLimiter;
import com.bmc.rag.agent.security.InputValidator;
import com.bmc.rag.connector.dto.WorkOrderCreationRequest;
import com.bmc.rag.store.service.VectorStoreService;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RemedyWorkOrderToolTest {

    @Mock
    private VectorStoreService vectorStoreService;

    @Mock
    private ConfirmationService confirmationService;

    @Mock
    private InputValidator inputValidator;

    @Mock
    private AgenticRateLimiter rateLimiter;

    @InjectMocks
    private RemedyWorkOrderTool workOrderTool;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(workOrderTool, "duplicateThreshold", 0.85);
        RemedyIncidentTool.setContext("session-1", "user-1");
    }

    @AfterEach
    void tearDown() {
        RemedyIncidentTool.clearContext();
    }

    @Nested
    @DisplayName("searchSimilarWorkOrders")
    class SearchSimilarWorkOrders {

        @Test
        void searchSimilarWorkOrders_noResults_returnsNoSimilar() {
            when(vectorStoreService.searchByType("install software", "WorkOrder", 5, 0.3))
                    .thenReturn(List.of());

            String result = workOrderTool.searchSimilarWorkOrders("install software", null);
            assertThat(result).contains("No similar work orders found");
        }

        @Test
        void searchSimilarWorkOrders_withResults_formatsOutput() {
            var searchResult = mock(VectorStoreService.SearchResult.class);
            when(searchResult.getSourceId()).thenReturn("WO000001");
            when(searchResult.getScore()).thenReturn(0.80f);
            when(searchResult.getTextSegment()).thenReturn("Install Adobe on desktop");
            when(searchResult.getMetadata()).thenReturn(java.util.Map.of("title", "Adobe Install"));

            when(vectorStoreService.searchByType("install software", "WorkOrder", 5, 0.3))
                    .thenReturn(List.of(searchResult));

            String result = workOrderTool.searchSimilarWorkOrders("install software", null);
            assertThat(result).contains("WO000001");
            assertThat(result).contains("Adobe Install");
            assertThat(result).contains("80%");
        }

        @Test
        void searchSimilarWorkOrders_maxResultsCapped() {
            when(vectorStoreService.searchByType(anyString(), eq("WorkOrder"), eq(10), anyDouble()))
                    .thenReturn(List.of());

            workOrderTool.searchSimilarWorkOrders("test", 20);
            verify(vectorStoreService).searchByType("test", "WorkOrder", 10, 0.3);
        }

        @Test
        void searchSimilarWorkOrders_exception_returnsError() {
            when(vectorStoreService.searchByType(anyString(), anyString(), anyInt(), anyDouble()))
                    .thenThrow(new RuntimeException("Search error"));

            String result = workOrderTool.searchSimilarWorkOrders("test", null);
            assertThat(result).contains("Error");
        }

        @Test
        void searchSimilarWorkOrders_highSimilarity_showsDuplicateWarning() {
            var searchResult = mock(VectorStoreService.SearchResult.class);
            when(searchResult.getSourceId()).thenReturn("WO000001");
            when(searchResult.getScore()).thenReturn(0.92f);
            when(searchResult.getTextSegment()).thenReturn("Install Adobe on desktop");
            when(searchResult.getMetadata()).thenReturn(java.util.Map.of("title", "Adobe Install"));

            when(vectorStoreService.searchByType("install software", "WorkOrder", 5, 0.3))
                    .thenReturn(List.of(searchResult));

            String result = workOrderTool.searchSimilarWorkOrders("install software", null);
            assertThat(result).contains("Warning");
            assertThat(result).contains("highly similar");
            assertThat(result).contains("duplicates");
        }

        @Test
        void searchSimilarWorkOrders_withNullMetadata_handlesGracefully() {
            var searchResult = mock(VectorStoreService.SearchResult.class);
            when(searchResult.getSourceId()).thenReturn("WO000001");
            when(searchResult.getScore()).thenReturn(0.70f);
            when(searchResult.getTextSegment()).thenReturn("Some work order");
            when(searchResult.getMetadata()).thenReturn(null);

            when(vectorStoreService.searchByType("test", "WorkOrder", 5, 0.3))
                    .thenReturn(List.of(searchResult));

            String result = workOrderTool.searchSimilarWorkOrders("test", null);
            assertThat(result).contains("No title");
        }

        @Test
        void searchSimilarWorkOrders_longTextSegment_truncates() {
            var searchResult = mock(VectorStoreService.SearchResult.class);
            when(searchResult.getSourceId()).thenReturn("WO000001");
            when(searchResult.getScore()).thenReturn(0.70f);
            when(searchResult.getTextSegment()).thenReturn("A".repeat(200));
            when(searchResult.getMetadata()).thenReturn(java.util.Map.of("title", "Title"));

            when(vectorStoreService.searchByType("test", "WorkOrder", 5, 0.3))
                    .thenReturn(List.of(searchResult));

            String result = workOrderTool.searchSimilarWorkOrders("test", null);
            assertThat(result).contains("...");
        }
    }

    @Nested
    @DisplayName("stageWorkOrderCreation")
    class StageWorkOrderCreation {

        @Test
        void stageWorkOrderCreation_rateLimited_returnsWarning() {
            when(rateLimiter.isRateLimited("user-1")).thenReturn(true);
            when(rateLimiter.getStatus("user-1")).thenReturn(
                    new AgenticRateLimiter.RateLimitStatus(10, 0, true));

            String result = workOrderTool.stageWorkOrderCreation(
                    "Install software", "Need Adobe Creative Suite", 0, 2);
            assertThat(result).contains("Rate limit exceeded");
        }

        @Test
        void stageWorkOrderCreation_invalidSummary_returnsError() {
            when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
            when(inputValidator.validateSummary(anyString())).thenReturn(
                    new InputValidator.ValidationResult(false, List.of("Too short"), List.of(), null));

            String result = workOrderTool.stageWorkOrderCreation("x", "Description", 0, 2);
            assertThat(result).contains("Validation Error");
        }

        @Test
        void stageWorkOrderCreation_invalidType_returnsError() {
            when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
            when(inputValidator.validateSummary(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "Summary"));
            when(inputValidator.validateDescription(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "Desc"));
            when(inputValidator.isValidWorkOrderType(5)).thenReturn(false);

            String result = workOrderTool.stageWorkOrderCreation("Summary", "Desc", 5, 2);
            assertThat(result).contains("Validation Error");
            assertThat(result).contains("Work order type");
        }

        @Test
        void stageWorkOrderCreation_invalidPriority_returnsError() {
            when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
            when(inputValidator.validateSummary(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "Summary"));
            when(inputValidator.validateDescription(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "Desc"));
            when(inputValidator.isValidWorkOrderType(0)).thenReturn(true);
            when(inputValidator.isValidPriority(5)).thenReturn(false);

            String result = workOrderTool.stageWorkOrderCreation("Summary", "Desc", 0, 5);
            assertThat(result).contains("Validation Error");
            assertThat(result).contains("Priority");
        }

        @Test
        void stageWorkOrderCreation_validInput_returnsConfirmation() {
            when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
            when(inputValidator.validateSummary(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "Install Software"));
            when(inputValidator.validateDescription(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "Need Adobe"));
            when(inputValidator.isValidWorkOrderType(0)).thenReturn(true);
            when(inputValidator.isValidPriority(2)).thenReturn(true);
            when(vectorStoreService.searchByType(anyString(), anyString(), anyInt(), anyDouble()))
                    .thenReturn(List.of());

            PendingAction mockAction = mock(PendingAction.class);
            when(mockAction.getConfirmationPrompt()).thenReturn("Confirm WO creation");
            when(confirmationService.stageWorkOrderCreation(
                    eq("session-1"), eq("user-1"), any(WorkOrderCreationRequest.class)))
                    .thenReturn(mockAction);

            String result = workOrderTool.stageWorkOrderCreation(
                    "Install Software", "Need Adobe Creative Suite", 0, 2);
            assertThat(result).isEqualTo("Confirm WO creation");
        }

        @Test
        void stageWorkOrderCreation_duplicatesFound_returnsWarning() {
            when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
            when(inputValidator.validateSummary(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "Summary"));
            when(inputValidator.validateDescription(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "Desc"));
            when(inputValidator.isValidWorkOrderType(0)).thenReturn(true);
            when(inputValidator.isValidPriority(2)).thenReturn(true);

            var dup = mock(VectorStoreService.SearchResult.class);
            when(dup.getSourceId()).thenReturn("WO000999");
            when(dup.getScore()).thenReturn(0.90f);
            when(dup.getMetadata()).thenReturn(java.util.Map.of("title", "Existing WO"));
            when(vectorStoreService.searchByType(anyString(), eq("WorkOrder"), anyInt(), eq(0.85)))
                    .thenReturn(List.of(dup));

            String result = workOrderTool.stageWorkOrderCreation("Summary", "Desc", 0, 2);
            assertThat(result).contains("Potential duplicates");
            assertThat(result).contains("WO000999");
        }
    }

    @Nested
    @DisplayName("stageScheduledWorkOrder")
    class StageScheduledWorkOrder {

        @Test
        void stageScheduledWorkOrder_validInput_setsScheduledDates() {
            when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
            when(inputValidator.validateSummary(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "Summary"));
            when(inputValidator.validateDescription(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "Desc"));
            when(inputValidator.isValidWorkOrderType(0)).thenReturn(true);
            when(inputValidator.isValidPriority(2)).thenReturn(true);
            when(inputValidator.validateCategory(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "IT"));
            when(vectorStoreService.searchByType(anyString(), anyString(), anyInt(), anyDouble()))
                    .thenReturn(List.of());

            PendingAction mockAction = mock(PendingAction.class);
            when(mockAction.getConfirmationPrompt()).thenReturn("Confirm scheduled WO");
            when(confirmationService.stageWorkOrderCreation(any(), any(), any()))
                    .thenReturn(mockAction);

            String result = workOrderTool.stageScheduledWorkOrder(
                    "Summary", "Desc", 0, 2, 3, 5, "IT", "Support Team");
            assertThat(result).isEqualTo("Confirm scheduled WO");

            verify(confirmationService).stageWorkOrderCreation(
                    eq("session-1"), eq("user-1"), argThat(req ->
                            req.getScheduledStartDate() != null && req.getScheduledEndDate() != null));
        }

        @Test
        void stageScheduledWorkOrder_rateLimited_returnsWarning() {
            when(rateLimiter.isRateLimited("user-1")).thenReturn(true);
            when(rateLimiter.getStatus("user-1")).thenReturn(
                    new AgenticRateLimiter.RateLimitStatus(10, 0, true));

            String result = workOrderTool.stageScheduledWorkOrder(
                    "Summary", "Desc", 0, 2, 1, 1, null, null);
            assertThat(result).contains("Rate limit exceeded");
        }

        @Test
        void stageScheduledWorkOrder_invalidSummary_returnsError() {
            when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
            when(inputValidator.validateSummary(anyString())).thenReturn(
                    new InputValidator.ValidationResult(false, List.of("Too short"), List.of(), null));

            String result = workOrderTool.stageScheduledWorkOrder(
                    "x", "Description", 0, 2, 1, 1, null, null);
            assertThat(result).contains("Validation Error");
        }

        @Test
        void stageScheduledWorkOrder_invalidDescription_returnsError() {
            when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
            when(inputValidator.validateSummary(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "Summary"));
            when(inputValidator.validateDescription(anyString())).thenReturn(
                    new InputValidator.ValidationResult(false, List.of("Too short"), List.of(), null));

            String result = workOrderTool.stageScheduledWorkOrder(
                    "Summary", "x", 0, 2, 1, 1, null, null);
            assertThat(result).contains("Validation Error");
        }

        @Test
        void stageScheduledWorkOrder_invalidType_returnsError() {
            when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
            when(inputValidator.validateSummary(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "Summary"));
            when(inputValidator.validateDescription(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "Desc"));
            when(inputValidator.isValidWorkOrderType(99)).thenReturn(false);

            String result = workOrderTool.stageScheduledWorkOrder(
                    "Summary", "Desc", 99, 2, 1, 1, null, null);
            assertThat(result).contains("Validation Error");
        }

        @Test
        void stageScheduledWorkOrder_invalidPriority_returnsError() {
            when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
            when(inputValidator.validateSummary(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "Summary"));
            when(inputValidator.validateDescription(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "Desc"));
            when(inputValidator.isValidWorkOrderType(0)).thenReturn(true);
            when(inputValidator.isValidPriority(99)).thenReturn(false);

            String result = workOrderTool.stageScheduledWorkOrder(
                    "Summary", "Desc", 0, 99, 1, 1, null, null);
            assertThat(result).contains("Validation Error");
        }

        @Test
        void stageScheduledWorkOrder_nullDays_usesDefaults() {
            when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
            when(inputValidator.validateSummary(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "Summary"));
            when(inputValidator.validateDescription(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "Desc"));
            when(inputValidator.isValidWorkOrderType(0)).thenReturn(true);
            when(inputValidator.isValidPriority(2)).thenReturn(true);

            PendingAction mockAction = mock(PendingAction.class);
            when(mockAction.getConfirmationPrompt()).thenReturn("Confirm");
            when(confirmationService.stageWorkOrderCreation(any(), any(), any()))
                    .thenReturn(mockAction);

            workOrderTool.stageScheduledWorkOrder(
                    "Summary", "Desc", 0, 2, null, null, null, null);

            verify(confirmationService).stageWorkOrderCreation(
                    any(), any(), argThat(req ->
                            req.getScheduledStartDate() != null && req.getScheduledEndDate() != null));
        }
    }

    @Nested
    @DisplayName("listPendingWorkOrders")
    class ListPendingWorkOrders {

        @Test
        void listPendingWorkOrders_noPending_returnsEmpty() {
            when(confirmationService.getPendingActionsForSession("session-1"))
                    .thenReturn(List.of());

            String result = workOrderTool.listPendingWorkOrders();
            assertThat(result).contains("No pending work order creations");
        }

        @Test
        void listPendingWorkOrders_withPending_listsActions() {
            PendingAction action = mock(PendingAction.class);
            when(action.getActionType()).thenReturn(PendingAction.ActionType.WORK_ORDER_CREATE);
            when(action.getActionId()).thenReturn("wo-action-1");
            when(action.getPreview()).thenReturn("Line1\nLine2\n**Summary:** Install SW\nLine4");
            when(action.getExpiresAt()).thenReturn(Instant.now().plusSeconds(300));

            when(confirmationService.getPendingActionsForSession("session-1"))
                    .thenReturn(List.of(action));

            String result = workOrderTool.listPendingWorkOrders();
            assertThat(result).contains("Pending Work Order Creations");
            assertThat(result).contains("wo-action-1");
        }

        @Test
        void listPendingWorkOrders_filtersNonWorkOrderActions() {
            PendingAction incidentAction = mock(PendingAction.class);
            when(incidentAction.getActionType()).thenReturn(PendingAction.ActionType.INCIDENT_CREATE);

            when(confirmationService.getPendingActionsForSession("session-1"))
                    .thenReturn(List.of(incidentAction));

            String result = workOrderTool.listPendingWorkOrders();
            assertThat(result).contains("No pending work order creations");
        }
    }
}
