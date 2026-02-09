package com.bmc.rag.agent.tools;

import com.bmc.rag.agent.confirmation.ConfirmationService;
import com.bmc.rag.agent.confirmation.PendingAction;
import com.bmc.rag.agent.security.AgenticRateLimiter;
import com.bmc.rag.agent.security.InputValidator;
import com.bmc.rag.connector.dto.IncidentCreationRequest;
import com.bmc.rag.store.service.VectorStoreService;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RemedyIncidentToolTest {

    @Mock
    private VectorStoreService vectorStoreService;

    @Mock
    private ConfirmationService confirmationService;

    @Mock
    private InputValidator inputValidator;

    @Mock
    private AgenticRateLimiter rateLimiter;

    @InjectMocks
    private RemedyIncidentTool incidentTool;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(incidentTool, "duplicateThreshold", 0.85);
        RemedyIncidentTool.setContext("session-1", "user-1");
    }

    @AfterEach
    void tearDown() {
        RemedyIncidentTool.clearContext();
    }

    @Nested
    @DisplayName("Context Management")
    class ContextManagement {

        @Test
        void setContext_setsThreadLocal() {
            RemedyIncidentTool.setContext("s1", "u1");
            // No exception when calling tool methods
        }

        @Test
        void setContext_withHistory_setsThreadLocal() {
            List<ChatMessage> history = List.of(UserMessage.from("test"));
            RemedyIncidentTool.setContext("s1", "u1", history);
            // No exception when calling tool methods
        }

        @Test
        void clearContext_removesThreadLocal() {
            RemedyIncidentTool.clearContext();
            // Next tool call should fail without context
        }
    }

    @Nested
    @DisplayName("Context Management Extended")
    class ContextManagementExtended {

        @Test
        void getContext_throwsException_whenNoContext() {
            RemedyIncidentTool.clearContext();

            // Methods that access context should throw IllegalStateException
            assertThatThrownBy(() -> incidentTool.stageIncidentCreation("test", "desc", 3, 3))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No context set");
        }

        @Test
        void getContext_throwsException_whenNoContextWithDetails() {
            RemedyIncidentTool.clearContext();

            assertThatThrownBy(() -> incidentTool.stageIncidentWithDetails(
                    "test", "desc", 3, 3, null, null, null, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No context set");
        }

        @Test
        void getContext_throwsException_whenNoContextListPending() {
            RemedyIncidentTool.clearContext();

            assertThatThrownBy(() -> incidentTool.listPendingIncidents())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No context set");
        }
    }

    @Nested
    @DisplayName("searchSimilarIncidents")
    class SearchSimilarIncidents {

        @Test
        void searchSimilarIncidents_noResults_returnsNoSimilar() {
            when(vectorStoreService.searchByType("VPN issue", "Incident", 5, 0.3))
                    .thenReturn(List.of());

            String result = incidentTool.searchSimilarIncidents("VPN issue", null);
            assertThat(result).contains("No similar incidents found");
        }

        @Test
        void searchSimilarIncidents_resultWithNoTitle_handlesGracefully() {
            var searchResult = mock(com.bmc.rag.store.service.VectorStoreService.SearchResult.class);
            when(searchResult.getSourceId()).thenReturn("INC000001");
            when(searchResult.getScore()).thenReturn(0.75f);
            when(searchResult.getTextSegment()).thenReturn("VPN connection timeout error");
            when(searchResult.getMetadata()).thenReturn(null);

            when(vectorStoreService.searchByType("VPN issue", "Incident", 5, 0.3))
                    .thenReturn(List.of(searchResult));

            String result = incidentTool.searchSimilarIncidents("VPN issue", null);
            assertThat(result).contains("INC000001");
            assertThat(result).contains("No title");
        }

        @Test
        void searchSimilarIncidents_resultWithEmptyText_handlesGracefully() {
            var searchResult = mock(com.bmc.rag.store.service.VectorStoreService.SearchResult.class);
            when(searchResult.getSourceId()).thenReturn("INC000001");
            when(searchResult.getScore()).thenReturn(0.75f);
            when(searchResult.getTextSegment()).thenReturn(null);
            when(searchResult.getMetadata()).thenReturn(java.util.Map.of("title", "VPN Issue"));

            when(vectorStoreService.searchByType("VPN issue", "Incident", 5, 0.3))
                    .thenReturn(List.of(searchResult));

            String result = incidentTool.searchSimilarIncidents("VPN issue", null);
            assertThat(result).contains("INC000001");
            assertThat(result).contains("VPN Issue");
        }

        @Test
        void searchSimilarIncidents_longTextPreview_truncated() {
            var searchResult = mock(com.bmc.rag.store.service.VectorStoreService.SearchResult.class);
            when(searchResult.getSourceId()).thenReturn("INC000001");
            when(searchResult.getScore()).thenReturn(0.75f);
            when(searchResult.getTextSegment()).thenReturn("A".repeat(200));
            when(searchResult.getMetadata()).thenReturn(java.util.Map.of("title", "Issue"));

            when(vectorStoreService.searchByType("test", "Incident", 5, 0.3))
                    .thenReturn(List.of(searchResult));

            String result = incidentTool.searchSimilarIncidents("test", null);
            assertThat(result).contains("...");
        }

        @Test
        void searchSimilarIncidents_withResults_formatsOutput() {
            var searchResult = mock(com.bmc.rag.store.service.VectorStoreService.SearchResult.class);
            when(searchResult.getSourceId()).thenReturn("INC000001");
            when(searchResult.getScore()).thenReturn(0.75f);
            when(searchResult.getTextSegment()).thenReturn("VPN connection timeout error");
            when(searchResult.getMetadata()).thenReturn(java.util.Map.of("title", "VPN Timeout"));

            when(vectorStoreService.searchByType("VPN issue", "Incident", 5, 0.3))
                    .thenReturn(List.of(searchResult));

            String result = incidentTool.searchSimilarIncidents("VPN issue", null);
            assertThat(result).contains("INC000001");
            assertThat(result).contains("VPN Timeout");
            assertThat(result).contains("75%");
        }

        @Test
        void searchSimilarIncidents_highSimilarity_showsDuplicateWarning() {
            var searchResult = mock(com.bmc.rag.store.service.VectorStoreService.SearchResult.class);
            when(searchResult.getSourceId()).thenReturn("INC000001");
            when(searchResult.getScore()).thenReturn(0.90f);
            when(searchResult.getTextSegment()).thenReturn("Duplicate issue");
            when(searchResult.getMetadata()).thenReturn(null);

            when(vectorStoreService.searchByType(anyString(), eq("Incident"), anyInt(), anyDouble()))
                    .thenReturn(List.of(searchResult));

            String result = incidentTool.searchSimilarIncidents("Duplicate issue", 5);
            assertThat(result).contains("Warning");
            assertThat(result).contains("duplicate");
        }

        @Test
        void searchSimilarIncidents_maxResultsCapped_at10() {
            when(vectorStoreService.searchByType(anyString(), eq("Incident"), eq(10), anyDouble()))
                    .thenReturn(List.of());

            incidentTool.searchSimilarIncidents("test", 50);
            verify(vectorStoreService).searchByType("test", "Incident", 10, 0.3);
        }

        @Test
        void searchSimilarIncidents_nullMaxResults_defaults5() {
            when(vectorStoreService.searchByType(anyString(), eq("Incident"), eq(5), anyDouble()))
                    .thenReturn(List.of());

            incidentTool.searchSimilarIncidents("test", null);
            verify(vectorStoreService).searchByType("test", "Incident", 5, 0.3);
        }

        @Test
        void searchSimilarIncidents_exception_returnsError() {
            when(vectorStoreService.searchByType(anyString(), anyString(), anyInt(), anyDouble()))
                    .thenThrow(new RuntimeException("DB error"));

            String result = incidentTool.searchSimilarIncidents("test", null);
            assertThat(result).contains("Error");
            assertThat(result).contains("DB error");
        }
    }

    @Nested
    @DisplayName("stageIncidentCreation")
    class StageIncidentCreation {

        @Test
        void stageIncidentCreation_rateLimited_returnsWarning() {
            when(rateLimiter.isRateLimited("user-1")).thenReturn(true);
            when(rateLimiter.getStatus("user-1")).thenReturn(
                    new AgenticRateLimiter.RateLimitStatus(10, 0, true));

            String result = incidentTool.stageIncidentCreation(
                    "VPN issue", "Cannot connect to VPN", 3, 3);
            assertThat(result).contains("Rate limit exceeded");
        }

        @Test
        void stageIncidentCreation_invalidSummary_returnsValidationError() {
            when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
            when(inputValidator.validateSummary(anyString())).thenReturn(
                    new InputValidator.ValidationResult(false, List.of("Summary too short"), List.of(), null));

            String result = incidentTool.stageIncidentCreation("x", "Description", 3, 3);
            assertThat(result).contains("Validation Error");
        }

        @Test
        void stageIncidentCreation_invalidImpact_returnsError() {
            when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
            when(inputValidator.validateSummary(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "Clean summary"));
            when(inputValidator.validateDescription(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "Clean desc"));
            when(inputValidator.isValidImpact(5)).thenReturn(false);

            String result = incidentTool.stageIncidentCreation(
                    "VPN Connection Issue", "Cannot connect", 5, 3);
            assertThat(result).contains("Validation Error");
            assertThat(result).contains("Impact");
        }

        @Test
        void stageIncidentCreation_invalidUrgency_returnsError() {
            when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
            when(inputValidator.validateSummary(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "Clean summary"));
            when(inputValidator.validateDescription(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "Clean desc"));
            when(inputValidator.isValidImpact(3)).thenReturn(true);
            when(inputValidator.isValidUrgency(0)).thenReturn(false);

            String result = incidentTool.stageIncidentCreation(
                    "VPN Connection Issue", "Cannot connect", 3, 0);
            assertThat(result).contains("Validation Error");
            assertThat(result).contains("Urgency");
        }

        @Test
        void stageIncidentCreation_validInput_returnsConfirmation() {
            when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
            when(inputValidator.validateSummary(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "VPN Connection Issue"));
            when(inputValidator.validateDescription(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "Cannot connect to VPN"));
            when(inputValidator.isValidImpact(3)).thenReturn(true);
            when(inputValidator.isValidUrgency(3)).thenReturn(true);
            when(vectorStoreService.searchByType(anyString(), anyString(), anyInt(), anyDouble()))
                    .thenReturn(List.of());

            PendingAction mockAction = mock(PendingAction.class);
            when(mockAction.getConfirmationPrompt()).thenReturn("Please confirm incident creation");
            when(confirmationService.stageIncidentCreation(
                    eq("session-1"), eq("user-1"), any(IncidentCreationRequest.class)))
                    .thenReturn(mockAction);

            String result = incidentTool.stageIncidentCreation(
                    "VPN Connection Issue", "Cannot connect to VPN from home", 3, 3);
            assertThat(result).isEqualTo("Please confirm incident creation");
        }

        @Test
        void stageIncidentCreation_duplicatesFound_returnsWarning() {
            when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
            when(inputValidator.validateSummary(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "VPN Issue"));
            when(inputValidator.validateDescription(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "Desc"));
            when(inputValidator.isValidImpact(3)).thenReturn(true);
            when(inputValidator.isValidUrgency(3)).thenReturn(true);

            var dup = mock(com.bmc.rag.store.service.VectorStoreService.SearchResult.class);
            when(dup.getSourceId()).thenReturn("INC000999");
            when(dup.getScore()).thenReturn(0.90f);
            when(dup.getMetadata()).thenReturn(java.util.Map.of("title", "Existing VPN Issue"));
            when(vectorStoreService.searchByType(anyString(), eq("Incident"), anyInt(), eq(0.85)))
                    .thenReturn(List.of(dup));

            String result = incidentTool.stageIncidentCreation(
                    "VPN Issue", "Cannot connect", 3, 3);
            assertThat(result).contains("Potential duplicates");
            assertThat(result).contains("INC000999");
        }

        @Test
        void stageIncidentCreation_longSummary_truncated() {
            String longSummary = "A".repeat(260);
            when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
            when(inputValidator.validateSummary(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), longSummary.substring(0, 250)));
            when(inputValidator.validateDescription(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "Desc"));
            when(inputValidator.isValidImpact(3)).thenReturn(true);
            when(inputValidator.isValidUrgency(3)).thenReturn(true);
            when(vectorStoreService.searchByType(anyString(), anyString(), anyInt(), anyDouble()))
                    .thenReturn(List.of());

            PendingAction mockAction = mock(PendingAction.class);
            when(mockAction.getConfirmationPrompt()).thenReturn("Confirm");
            when(confirmationService.stageIncidentCreation(any(), any(), any()))
                    .thenReturn(mockAction);

            String result = incidentTool.stageIncidentCreation(longSummary, "Description", 3, 3);
            assertThat(result).isEqualTo("Confirm");
        }
    }

    @Nested
    @DisplayName("stageIncidentWithDetails")
    class StageIncidentWithDetails {

        @Test
        void stageIncidentWithDetails_rateLimited_returnsWarning() {
            when(rateLimiter.isRateLimited("user-1")).thenReturn(true);
            when(rateLimiter.getStatus("user-1")).thenReturn(
                    new AgenticRateLimiter.RateLimitStatus(10, 0, true));

            String result = incidentTool.stageIncidentWithDetails(
                    "VPN issue", "Cannot connect", 3, 3, "John", "Doe", "Network", "IT Support");
            assertThat(result).contains("Rate limit exceeded");
        }

        @Test
        void stageIncidentWithDetails_invalidSummary_returnsError() {
            when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
            when(inputValidator.validateSummary(anyString())).thenReturn(
                    new InputValidator.ValidationResult(false, List.of("Too short"), List.of(), null));

            String result = incidentTool.stageIncidentWithDetails("x", "desc", 3, 3, null, null, null, null);
            assertThat(result).contains("Validation Error");
        }

        @Test
        void stageIncidentWithDetails_invalidDescription_returnsError() {
            when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
            when(inputValidator.validateSummary(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "Valid"));
            when(inputValidator.validateDescription(anyString())).thenReturn(
                    new InputValidator.ValidationResult(false, List.of("Too short"), List.of(), null));

            String result = incidentTool.stageIncidentWithDetails("VPN issue", "x", 3, 3, null, null, null, null);
            assertThat(result).contains("Validation Error");
        }

        @Test
        void stageIncidentWithDetails_invalidImpact_returnsError() {
            when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
            when(inputValidator.validateSummary(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "Valid"));
            when(inputValidator.validateDescription(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "Valid"));
            when(inputValidator.isValidImpact(5)).thenReturn(false);

            String result = incidentTool.stageIncidentWithDetails("VPN issue", "desc", 5, 3, null, null, null, null);
            assertThat(result).contains("Validation Error").contains("Impact");
        }

        @Test
        void stageIncidentWithDetails_invalidUrgency_returnsError() {
            when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
            when(inputValidator.validateSummary(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "Valid"));
            when(inputValidator.validateDescription(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "Valid"));
            when(inputValidator.isValidImpact(3)).thenReturn(true);
            when(inputValidator.isValidUrgency(0)).thenReturn(false);

            String result = incidentTool.stageIncidentWithDetails("VPN issue", "desc", 3, 0, null, null, null, null);
            assertThat(result).contains("Validation Error").contains("Urgency");
        }

        @Test
        void stageIncidentWithDetails_withOptionalFields_stagesSuccessfully() {
            when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
            when(inputValidator.validateSummary(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "VPN issue"));
            when(inputValidator.validateDescription(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "desc"));
            when(inputValidator.isValidImpact(3)).thenReturn(true);
            when(inputValidator.isValidUrgency(3)).thenReturn(true);
            when(inputValidator.validateName("John")).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "John"));
            when(inputValidator.validateName("Doe")).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "Doe"));
            when(inputValidator.validateCategory("Network")).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "Network"));
            when(inputValidator.validateCategory("IT Support")).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "IT Support"));

            PendingAction mockAction = mock(PendingAction.class);
            when(mockAction.getConfirmationPrompt()).thenReturn("Confirm");
            when(confirmationService.stageIncidentCreation(any(), any(), any()))
                    .thenReturn(mockAction);

            String result = incidentTool.stageIncidentWithDetails(
                    "VPN issue", "Cannot connect", 3, 3, "John", "Doe", "Network", "IT Support");
            assertThat(result).isEqualTo("Confirm");
        }

        @Test
        void stageIncidentWithDetails_invalidOptionalName_skipped() {
            when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
            when(inputValidator.validateSummary(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "VPN issue"));
            when(inputValidator.validateDescription(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "desc"));
            when(inputValidator.isValidImpact(3)).thenReturn(true);
            when(inputValidator.isValidUrgency(3)).thenReturn(true);
            when(inputValidator.validateName("Invalid123!@#")).thenReturn(
                    new InputValidator.ValidationResult(false, List.of("Invalid"), List.of(), null));

            PendingAction mockAction = mock(PendingAction.class);
            when(mockAction.getConfirmationPrompt()).thenReturn("Confirm");
            when(confirmationService.stageIncidentCreation(any(), any(), any()))
                    .thenReturn(mockAction);

            String result = incidentTool.stageIncidentWithDetails(
                    "VPN issue", "desc", 3, 3, "Invalid123!@#", null, null, null);
            assertThat(result).isEqualTo("Confirm");
        }

        @Test
        void stageIncidentWithDetails_blankOptionalFields_ignored() {
            when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
            when(inputValidator.validateSummary(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "VPN"));
            when(inputValidator.validateDescription(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "desc"));
            when(inputValidator.isValidImpact(3)).thenReturn(true);
            when(inputValidator.isValidUrgency(3)).thenReturn(true);

            PendingAction mockAction = mock(PendingAction.class);
            when(mockAction.getConfirmationPrompt()).thenReturn("Confirm");
            when(confirmationService.stageIncidentCreation(any(), any(), any()))
                    .thenReturn(mockAction);

            String result = incidentTool.stageIncidentWithDetails(
                    "VPN", "desc", 3, 3, "   ", "", null, "   ");
            assertThat(result).isEqualTo("Confirm");
        }
    }

    @Nested
    @DisplayName("listPendingIncidents")
    class ListPendingIncidents {

        @Test
        void listPendingIncidents_noPending_returnsEmpty() {
            when(confirmationService.getPendingActionsForSession("session-1"))
                    .thenReturn(List.of());

            String result = incidentTool.listPendingIncidents();
            assertThat(result).contains("No pending incident creations");
        }

        @Test
        void listPendingIncidents_withPending_listsActions() {
            PendingAction action = mock(PendingAction.class);
            when(action.getActionType()).thenReturn(PendingAction.ActionType.INCIDENT_CREATE);
            when(action.getActionId()).thenReturn("action-1");
            when(action.getPreview()).thenReturn("Line1\nLine2\n**Summary:** VPN Issue\nLine4");
            when(action.getExpiresAt()).thenReturn(Instant.now().plusSeconds(300));

            when(confirmationService.getPendingActionsForSession("session-1"))
                    .thenReturn(List.of(action));

            String result = incidentTool.listPendingIncidents();
            assertThat(result).contains("Pending Incident Creations");
            assertThat(result).contains("action-1");
        }

        @Test
        void listPendingIncidents_filtersNonIncidentActions() {
            PendingAction woAction = mock(PendingAction.class);
            when(woAction.getActionType()).thenReturn(PendingAction.ActionType.WORK_ORDER_CREATE);

            when(confirmationService.getPendingActionsForSession("session-1"))
                    .thenReturn(List.of(woAction));

            String result = incidentTool.listPendingIncidents();
            assertThat(result).contains("No pending incident creations");
        }
    }

    @Nested
    @DisplayName("Conversation Extraction")
    class ConversationExtraction {

        @Test
        void stageIncidentCreation_vagueSummary_extractsFromConversation() {
            List<ChatMessage> history = List.of(
                    UserMessage.from("My VPN is not working, I get a timeout error when connecting"),
                    UserMessage.from("create a ticket for this issue")
            );
            RemedyIncidentTool.setContext("session-1", "user-1", history);

            when(rateLimiter.isRateLimited(anyString())).thenReturn(false);

            // First validation fails (vague summary)
            when(inputValidator.validateSummary("this issue")).thenReturn(
                    new InputValidator.ValidationResult(false,
                            List.of(InputValidator.VAGUE_SUMMARY_ERROR_PREFIX + "too vague"), List.of(), null));

            // After extraction, second validation succeeds
            when(inputValidator.validateSummary(argThat(s -> s != null && s.contains("VPN")))).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "VPN is not working, timeout error"));
            when(inputValidator.validateDescription(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "VPN timeout"));
            when(inputValidator.isValidImpact(3)).thenReturn(true);
            when(inputValidator.isValidUrgency(3)).thenReturn(true);
            when(vectorStoreService.searchByType(anyString(), anyString(), anyInt(), anyDouble()))
                    .thenReturn(List.of());

            PendingAction mockAction = mock(PendingAction.class);
            when(mockAction.getConfirmationPrompt()).thenReturn("Confirm enriched");
            when(confirmationService.stageIncidentCreation(any(), any(), any()))
                    .thenReturn(mockAction);

            String result = incidentTool.stageIncidentCreation("this issue", "desc", 3, 3);
            assertThat(result).isEqualTo("Confirm enriched");
        }

        @Test
        void stageIncidentCreation_vagueSummaryNoConversation_returnsValidationError() {
            RemedyIncidentTool.setContext("session-1", "user-1", null);

            when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
            when(inputValidator.validateSummary("this issue")).thenReturn(
                    new InputValidator.ValidationResult(false,
                            List.of(InputValidator.VAGUE_SUMMARY_ERROR_PREFIX + "too vague"), List.of(), null));

            String result = incidentTool.stageIncidentCreation("this issue", "desc", 3, 3);
            assertThat(result).contains("Validation Error");
        }

        @Test
        void stageIncidentCreation_vagueSummaryEmptyConversation_returnsValidationError() {
            RemedyIncidentTool.setContext("session-1", "user-1", List.of());

            when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
            when(inputValidator.validateSummary("vague")).thenReturn(
                    new InputValidator.ValidationResult(false,
                            List.of(InputValidator.VAGUE_SUMMARY_ERROR_PREFIX + "vague"), List.of(), null));

            String result = incidentTool.stageIncidentCreation("vague", "desc", 3, 3);
            assertThat(result).contains("Validation Error");
        }

        @Test
        void stageIncidentCreation_vagueSummaryExtractionFailsRevalidation_returnsError() {
            List<ChatMessage> history = List.of(
                    UserMessage.from("My VPN is not working, I get a timeout error"),
                    UserMessage.from("create ticket for this")
            );
            RemedyIncidentTool.setContext("session-1", "user-1", history);

            when(rateLimiter.isRateLimited(anyString())).thenReturn(false);

            // First validation fails
            when(inputValidator.validateSummary("this")).thenReturn(
                    new InputValidator.ValidationResult(false,
                            List.of(InputValidator.VAGUE_SUMMARY_ERROR_PREFIX + "too vague"), List.of(), null));

            // Second validation ALSO fails
            when(inputValidator.validateSummary(argThat(s -> s != null && s.contains("VPN")))).thenReturn(
                    new InputValidator.ValidationResult(false,
                            List.of("Still invalid"), List.of(), null));

            String result = incidentTool.stageIncidentCreation("this", "desc", 3, 3);
            assertThat(result).contains("Validation Error");
        }

        @Test
        void stageIncidentCreation_nonVagueError_skipsExtraction() {
            RemedyIncidentTool.setContext("session-1", "user-1", null);

            when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
            when(inputValidator.validateSummary("x")).thenReturn(
                    new InputValidator.ValidationResult(false,
                            List.of("Too short"), List.of(), null));

            String result = incidentTool.stageIncidentCreation("x", "desc", 3, 3);
            assertThat(result).contains("Validation Error").contains("Too short");
        }

        @Test
        void extractIssueFromConversation_skipsTicketRequestsWithoutProblemIndicators() {
            List<ChatMessage> history = List.of(
                    UserMessage.from("create a ticket"),
                    UserMessage.from("open an incident")
            );
            RemedyIncidentTool.setContext("session-1", "user-1", history);

            when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
            when(inputValidator.validateSummary("vague")).thenReturn(
                    new InputValidator.ValidationResult(false,
                            List.of(InputValidator.VAGUE_SUMMARY_ERROR_PREFIX + "vague"), List.of(), null));

            String result = incidentTool.stageIncidentCreation("vague", "desc", 3, 3);
            assertThat(result).contains("Validation Error");
        }

        @Test
        void extractIssueFromConversation_includesTicketRequestWithProblemIndicators() {
            List<ChatMessage> history = List.of(
                    UserMessage.from("create a ticket for my broken VPN connection error")
            );
            RemedyIncidentTool.setContext("session-1", "user-1", history);

            when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
            when(inputValidator.validateSummary("vague")).thenReturn(
                    new InputValidator.ValidationResult(false,
                            List.of(InputValidator.VAGUE_SUMMARY_ERROR_PREFIX + "vague"), List.of(), null));
            when(inputValidator.validateSummary(argThat(s -> s != null && s.contains("broken")))).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "VPN broken"));
            when(inputValidator.validateDescription(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "desc"));
            when(inputValidator.isValidImpact(3)).thenReturn(true);
            when(inputValidator.isValidUrgency(3)).thenReturn(true);
            when(vectorStoreService.searchByType(anyString(), anyString(), anyInt(), anyDouble()))
                    .thenReturn(List.of());

            PendingAction mockAction = mock(PendingAction.class);
            when(mockAction.getConfirmationPrompt()).thenReturn("Confirm");
            when(confirmationService.stageIncidentCreation(any(), any(), any()))
                    .thenReturn(mockAction);

            String result = incidentTool.stageIncidentCreation("vague", "desc", 3, 3);
            assertThat(result).isEqualTo("Confirm");
        }

        @Test
        void generateSummaryFromIssue_truncatesLongText() {
            List<ChatMessage> history = List.of(
                    UserMessage.from("My application is not working. " + "A".repeat(300))
            );
            RemedyIncidentTool.setContext("session-1", "user-1", history);

            when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
            when(inputValidator.validateSummary("vague")).thenReturn(
                    new InputValidator.ValidationResult(false,
                            List.of(InputValidator.VAGUE_SUMMARY_ERROR_PREFIX + "vague"), List.of(), null));
            when(inputValidator.validateSummary(argThat(s -> s != null && s.length() <= 200))).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "Application not working"));
            when(inputValidator.validateDescription(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "desc"));
            when(inputValidator.isValidImpact(3)).thenReturn(true);
            when(inputValidator.isValidUrgency(3)).thenReturn(true);
            when(vectorStoreService.searchByType(anyString(), anyString(), anyInt(), anyDouble()))
                    .thenReturn(List.of());

            PendingAction mockAction = mock(PendingAction.class);
            when(mockAction.getConfirmationPrompt()).thenReturn("Confirm");
            when(confirmationService.stageIncidentCreation(any(), any(), any()))
                    .thenReturn(mockAction);

            String result = incidentTool.stageIncidentCreation("vague", "desc", 3, 3);
            assertThat(result).isEqualTo("Confirm");
        }

        @Test
        void generateSummaryFromIssue_takesFirstSentence() {
            List<ChatMessage> history = List.of(
                    UserMessage.from("My email is not working. I tried restarting Outlook. Nothing helps.")
            );
            RemedyIncidentTool.setContext("session-1", "user-1", history);

            when(rateLimiter.isRateLimited(anyString())).thenReturn(false);
            when(inputValidator.validateSummary("issue")).thenReturn(
                    new InputValidator.ValidationResult(false,
                            List.of(InputValidator.VAGUE_SUMMARY_ERROR_PREFIX + "vague"), List.of(), null));
            when(inputValidator.validateSummary(argThat(s -> s != null && s.contains("email")))).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "Email not working"));
            when(inputValidator.validateDescription(anyString())).thenReturn(
                    new InputValidator.ValidationResult(true, List.of(), List.of(), "desc"));
            when(inputValidator.isValidImpact(3)).thenReturn(true);
            when(inputValidator.isValidUrgency(3)).thenReturn(true);
            when(vectorStoreService.searchByType(anyString(), anyString(), anyInt(), anyDouble()))
                    .thenReturn(List.of());

            PendingAction mockAction = mock(PendingAction.class);
            when(mockAction.getConfirmationPrompt()).thenReturn("Confirm");
            when(confirmationService.stageIncidentCreation(any(), any(), any()))
                    .thenReturn(mockAction);

            String result = incidentTool.stageIncidentCreation("issue", "desc", 3, 3);
            assertThat(result).isEqualTo("Confirm");
        }
    }
}
