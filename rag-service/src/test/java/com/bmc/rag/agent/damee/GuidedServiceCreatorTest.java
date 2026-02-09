package com.bmc.rag.agent.damee;

import com.bmc.rag.agent.confirmation.ConfirmationService;
import com.bmc.rag.agent.confirmation.PendingAction;
import com.bmc.rag.agent.damee.GuidedServiceCreator.GuidedResponse;
import com.bmc.rag.agent.damee.ServiceIntentMatcher.ServiceMatchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GuidedServiceCreator}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GuidedServiceCreatorTest {

    @Mock
    private ServiceIntentMatcher intentMatcher;

    @Mock
    private DameeServiceCatalog catalog;

    @Mock
    private ConfirmationService confirmationService;

    @InjectMocks
    private GuidedServiceCreator creator;

    private DameeService testService;

    @BeforeEach
    void setUp() {
        testService = DameeService.builder()
                .serviceId("10513")
                .nameEn("VPN Request")
                .nameAr("طلب VPN")
                .descriptionEn("VPN access")
                .category("IT Services")
                .requiredFields(List.of("vpnType", "justification"))
                .workflow(List.of(
                        DameeService.WorkflowStep.builder()
                                .order(1)
                                .description("Fill Form")
                                .build(),
                        DameeService.WorkflowStep.builder()
                                .order(2)
                                .description("Manager Approval")
                                .build()
                ))
                .build();
    }

    @Test
    void processMessage_initialQueryHighConfidence_returnsConfirmation() {
        // Given: high confidence match
        ServiceMatchResult match = ServiceMatchResult.highConfidence(testService);
        when(intentMatcher.matchService(anyString())).thenReturn(match);

        // When: process initial message
        GuidedResponse response = creator.processMessage("session1", "user1", "I need VPN");

        // Then: returns confirmation request
        assertThat(response.getMessage()).contains("VPN Request");
        assertThat(response.getOptions()).isNotEmpty();
        assertThat(response.getState()).isNotNull();
        assertThat(response.getState().getPhase()).isEqualTo(GuidedCreationState.Phase.CONFIRMING_SERVICE);
    }

    @Test
    void processMessage_initialQueryMultipleMatches_returnsSelection() {
        // Given: multiple matches
        ServiceMatchResult match = ServiceMatchResult.clarificationNeeded(List.of(testService));
        when(intentMatcher.matchService(anyString())).thenReturn(match);

        // When: process initial message
        GuidedResponse response = creator.processMessage("session1", "user1", "I need help");

        // Then: returns selection request
        assertThat(response.getMessage()).contains("multiple services");
        assertThat(response.getState().getPhase()).isEqualTo(GuidedCreationState.Phase.AWAITING_SERVICE_SELECTION);
    }

    @Test
    void processMessage_initialQueryNoMatch_showsCategories() {
        // Given: no match
        ServiceMatchResult match = ServiceMatchResult.noMatch("Not found");
        when(intentMatcher.matchService(anyString())).thenReturn(match);

        // When: process initial message
        GuidedResponse response = creator.processMessage("session1", "user1", "unknown request");

        // Then: shows categories
        assertThat(response.isShowCategories()).isTrue();
    }

    @Test
    void processMessage_serviceConfirmationYes_startsGathering() {
        // Given: confirmed service
        ServiceMatchResult match = ServiceMatchResult.highConfidence(testService);
        when(intentMatcher.matchService(anyString())).thenReturn(match);

        // First message to set up confirmation
        creator.processMessage("session1", "user1", "I need VPN");

        // When: confirm with yes
        GuidedResponse response = creator.processMessage("session1", "user1", "yes");

        // Then: starts gathering fields
        assertThat(response.getState().getPhase()).isEqualTo(GuidedCreationState.Phase.GATHERING_FIELDS);
        assertThat(response.getMessage()).contains("Question");
    }

    @Test
    void processMessage_serviceConfirmationNo_showsCategories() {
        // Given: high confidence match
        ServiceMatchResult match = ServiceMatchResult.highConfidence(testService);
        when(intentMatcher.matchService(anyString())).thenReturn(match);

        // First message to set up confirmation
        creator.processMessage("session1", "user1", "I need VPN");

        // When: reject with no
        GuidedResponse response = creator.processMessage("session1", "user1", "no");

        // Then: shows categories
        assertThat(response.isShowCategories()).isTrue();
        assertThat(response.getState().getPhase()).isEqualTo(GuidedCreationState.Phase.INITIAL);
    }

    @Test
    void processMessage_serviceConfirmationCancel_cancelsFlow() {
        // Given: high confidence match
        ServiceMatchResult match = ServiceMatchResult.highConfidence(testService);
        when(intentMatcher.matchService(anyString())).thenReturn(match);

        // First message to set up confirmation
        creator.processMessage("session1", "user1", "I need VPN");

        // When: cancel
        GuidedResponse response = creator.processMessage("session1", "user1", "cancel");

        // Then: cancels flow
        assertThat(response.isCancelled()).isTrue();
        assertThat(creator.hasActiveFlow("session1")).isFalse();
    }

    @Test
    void processMessage_gatheringFieldsValidInput_recordsField() {
        // Given: in gathering phase
        ServiceMatchResult match = ServiceMatchResult.highConfidence(testService);
        when(intentMatcher.matchService(anyString())).thenReturn(match);
        creator.processMessage("session1", "user1", "I need VPN");
        creator.processMessage("session1", "user1", "yes");

        // When: provide field value
        GuidedResponse response = creator.processMessage("session1", "user1", "Remote access");

        // Then: records field and asks for next
        assertThat(response.getState().getCollectedFields()).isNotEmpty();
        assertThat(response.getMessage()).contains("Question");
    }

    @Test
    void processMessage_gatheringFieldsEmptyInput_promptsAgain() {
        // Given: in gathering phase
        ServiceMatchResult match = ServiceMatchResult.highConfidence(testService);
        when(intentMatcher.matchService(anyString())).thenReturn(match);
        creator.processMessage("session1", "user1", "I need VPN");
        creator.processMessage("session1", "user1", "yes");

        // When: provide empty value
        GuidedResponse response = creator.processMessage("session1", "user1", "   ");

        // Then: prompts again
        assertThat(response.getMessage()).contains("provide a value");
    }

    @Test
    void processMessage_gatheringFieldsCancel_cancelsFlow(){
        // Given: in gathering phase
        ServiceMatchResult match = ServiceMatchResult.highConfidence(testService);
        when(intentMatcher.matchService(anyString())).thenReturn(match);
        creator.processMessage("session1", "user1", "I need VPN");
        creator.processMessage("session1", "user1", "yes");

        // When: cancel
        GuidedResponse response = creator.processMessage("session1", "user1", "cancel");

        // Then: cancels flow
        assertThat(response.isCancelled()).isTrue();
    }

    @Test
    void processMessage_allFieldsCollected_showsConfirmation() {
        // Given: in gathering phase
        ServiceMatchResult match = ServiceMatchResult.highConfidence(testService);
        when(intentMatcher.matchService(anyString())).thenReturn(match);
        creator.processMessage("session1", "user1", "I need VPN");
        creator.processMessage("session1", "user1", "yes");

        // When: provide all required fields
        creator.processMessage("session1", "user1", "Remote access");
        GuidedResponse response = creator.processMessage("session1", "user1", "Work from home");

        // Then: shows confirmation
        assertThat(response.getState().getPhase()).isEqualTo(GuidedCreationState.Phase.AWAITING_CONFIRMATION);
        assertThat(response.getMessage()).contains("Summary");
        assertThat(response.getOptions()).isNotEmpty();
    }

    @Test
    void processMessage_finalConfirmationConfirm_stagesAction() throws Exception {
        // Given: in confirmation phase with pending action
        ServiceMatchResult match = ServiceMatchResult.highConfidence(testService);
        when(intentMatcher.matchService(anyString())).thenReturn(match);

        PendingAction pendingAction = PendingAction.builder()
                .actionId("action123")
                .actionType(PendingAction.ActionType.WORK_ORDER_CREATE)
                .preview("Test preview")
                .build();

        when(confirmationService.stageWorkOrderCreation(anyString(), anyString(), any()))
                .thenReturn(pendingAction);

        creator.processMessage("session1", "user1", "I need VPN");
        creator.processMessage("session1", "user1", "yes");
        creator.processMessage("session1", "user1", "Remote access");
        creator.processMessage("session1", "user1", "Work from home");

        // When: confirm submission
        GuidedResponse response = creator.processMessage("session1", "user1", "confirm");

        // Then: stages action
        assertThat(response.getPendingActionId()).isNotNull();
        verify(confirmationService).stageWorkOrderCreation(anyString(), anyString(), any());
    }

    @Test
    void processMessage_finalConfirmationEdit_returnsToGathering() {
        // Given: in confirmation phase
        ServiceMatchResult match = ServiceMatchResult.highConfidence(testService);
        when(intentMatcher.matchService(anyString())).thenReturn(match);
        creator.processMessage("session1", "user1", "I need VPN");
        creator.processMessage("session1", "user1", "yes");
        creator.processMessage("session1", "user1", "Remote access");
        creator.processMessage("session1", "user1", "Work from home");

        // When: request edit
        GuidedResponse response = creator.processMessage("session1", "user1", "edit");

        // Then: returns to gathering
        assertThat(response.getState().getPhase()).isEqualTo(GuidedCreationState.Phase.GATHERING_FIELDS);
        assertThat(response.getState().getCollectedFields()).isEmpty();
    }

    @Test
    void processMessage_finalConfirmationCancel_cancelsFlow() {
        // Given: in confirmation phase
        ServiceMatchResult match = ServiceMatchResult.highConfidence(testService);
        when(intentMatcher.matchService(anyString())).thenReturn(match);
        creator.processMessage("session1", "user1", "I need VPN");
        creator.processMessage("session1", "user1", "yes");
        creator.processMessage("session1", "user1", "Remote access");
        creator.processMessage("session1", "user1", "Work from home");

        // When: cancel
        GuidedResponse response = creator.processMessage("session1", "user1", "cancel");

        // Then: cancels flow
        assertThat(response.isCancelled()).isTrue();
    }

    @Test
    void processMessage_serviceSelectionByNumber_selectsService() {
        // Given: multiple services to choose from
        DameeService service2 = DameeService.builder()
                .serviceId("10242")
                .nameEn("Email Service")
                .requiredFields(List.of("requestType"))
                .build();

        ServiceMatchResult match = ServiceMatchResult.clarificationNeeded(List.of(testService, service2));
        when(intentMatcher.matchService(anyString())).thenReturn(match);
        creator.processMessage("session1", "user1", "I need help");

        // When: select by number
        GuidedResponse response = creator.processMessage("session1", "user1", "1");

        // Then: selects first service and starts gathering
        assertThat(response.getState().getPhase()).isEqualTo(GuidedCreationState.Phase.GATHERING_FIELDS);
        assertThat(response.getState().getSelectedService().getServiceId()).isEqualTo("10513");
    }

    @Test
    void processMessage_serviceSelectionByName_selectsService() {
        // Given: multiple services
        ServiceMatchResult match = ServiceMatchResult.clarificationNeeded(List.of(testService));
        when(intentMatcher.matchService(anyString())).thenReturn(match);
        creator.processMessage("session1", "user1", "I need help");

        // When: select by name
        GuidedResponse response = creator.processMessage("session1", "user1", "VPN");

        // Then: selects matching service
        assertThat(response.getState().getPhase()).isEqualTo(GuidedCreationState.Phase.GATHERING_FIELDS);
    }

    @Test
    void processMessage_serviceSelectionInvalid_promptsAgain() {
        // Given: multiple services
        ServiceMatchResult match = ServiceMatchResult.clarificationNeeded(List.of(testService));
        when(intentMatcher.matchService(anyString())).thenReturn(match);
        creator.processMessage("session1", "user1", "I need help");

        // When: provide invalid selection
        GuidedResponse response = creator.processMessage("session1", "user1", "999");

        // Then: prompts again
        assertThat(response.getMessage()).contains("didn't understand");
        assertThat(response.getState().getPhase()).isEqualTo(GuidedCreationState.Phase.AWAITING_SERVICE_SELECTION);
    }

    @Test
    void processMessage_serviceSelectionCancel_cancelsFlow() {
        // Given: multiple services
        ServiceMatchResult match = ServiceMatchResult.clarificationNeeded(List.of(testService));
        when(intentMatcher.matchService(anyString())).thenReturn(match);
        creator.processMessage("session1", "user1", "I need help");

        // When: cancel
        GuidedResponse response = creator.processMessage("session1", "user1", "cancel");

        // Then: cancels flow
        assertThat(response.isCancelled()).isTrue();
    }

    @Test
    void clearSession_removesState() {
        // Given: active session
        ServiceMatchResult match = ServiceMatchResult.highConfidence(testService);
        when(intentMatcher.matchService(anyString())).thenReturn(match);
        creator.processMessage("session1", "user1", "I need VPN");

        // When: clear session
        creator.clearSession("session1");

        // Then: session is removed
        assertThat(creator.hasActiveFlow("session1")).isFalse();
    }

    @Test
    void hasActiveFlow_noSession_returnsFalse() {
        // When: check non-existent session
        boolean hasFlow = creator.hasActiveFlow("nonexistent");

        // Then: returns false
        assertThat(hasFlow).isFalse();
    }

    @Test
    void hasActiveFlow_activeSession_returnsTrue() {
        // Given: active session
        ServiceMatchResult match = ServiceMatchResult.highConfidence(testService);
        when(intentMatcher.matchService(anyString())).thenReturn(match);
        creator.processMessage("session1", "user1", "I need VPN");

        // When: check active flow
        boolean hasFlow = creator.hasActiveFlow("session1");

        // Then: returns true
        assertThat(hasFlow).isTrue();
    }

    @Test
    void getState_existingSession_returnsState() {
        // Given: active session
        ServiceMatchResult match = ServiceMatchResult.highConfidence(testService);
        when(intentMatcher.matchService(anyString())).thenReturn(match);
        creator.processMessage("session1", "user1", "I need VPN");

        // When: get state
        GuidedCreationState state = creator.getState("session1");

        // Then: returns state
        assertThat(state).isNotNull();
        assertThat(state.getSessionId()).isEqualTo("session1");
    }

    @Test
    void getState_nonexistentSession_returnsNull() {
        // When: get state for non-existent session
        GuidedCreationState state = creator.getState("nonexistent");

        // Then: returns null
        assertThat(state).isNull();
    }

    @Test
    void guidedResponse_cancelled_hasCorrectProperties() {
        // When: create cancelled response
        GuidedResponse response = GuidedResponse.cancelled();

        // Then: has correct properties
        assertThat(response.isCancelled()).isTrue();
        assertThat(response.getMessage()).contains("cancelled");
    }

    @Test
    void guidedResponse_error_hasCorrectProperties() {
        // When: create error response
        GuidedResponse response = GuidedResponse.error("Test error");

        // Then: has correct properties
        assertThat(response.isError()).isTrue();
        assertThat(response.getMessage()).contains("Test error");
    }

    @Test
    void processMessage_executeConfirmedAction_submitsSuccessfully() throws Exception {
        // Given: staged action ready for confirmation
        ServiceMatchResult match = ServiceMatchResult.highConfidence(testService);
        when(intentMatcher.matchService(anyString())).thenReturn(match);

        PendingAction pendingAction = PendingAction.builder()
                .actionId("action123")
                .actionType(PendingAction.ActionType.WORK_ORDER_CREATE)
                .preview("Test preview")
                .build();

        when(confirmationService.stageWorkOrderCreation(anyString(), anyString(), any()))
                .thenReturn(pendingAction);

        ConfirmationService.ConfirmationResult confirmResult =
                ConfirmationService.ConfirmationResult.success("WO001", "Success");

        when(confirmationService.confirm(anyString(), anyString(), anyString()))
                .thenReturn(confirmResult);

        // Flow to confirmation
        creator.processMessage("session1", "user1", "I need VPN");
        creator.processMessage("session1", "user1", "yes");
        creator.processMessage("session1", "user1", "Remote access");
        creator.processMessage("session1", "user1", "Work from home");
        creator.processMessage("session1", "user1", "confirm"); // Stage

        // When: confirm the pending action
        GuidedResponse response = creator.processMessage("session1", "user1", "confirm");

        // Then: submits successfully
        assertThat(response.isSubmitted()).isTrue();
        assertThat(response.getRequestNumber()).isEqualTo("WO001");
        verify(confirmationService).confirm(eq("action123"), eq("session1"), eq("user1"));
    }

    @Test
    void processMessage_multipleSessionsIndependent_success() {
        // Given: two different sessions
        ServiceMatchResult match1 = ServiceMatchResult.highConfidence(testService);
        ServiceMatchResult match2 = ServiceMatchResult.highConfidence(testService);
        when(intentMatcher.matchService(anyString())).thenReturn(match1, match2);

        // When: process messages for both sessions
        creator.processMessage("session1", "user1", "I need VPN");
        creator.processMessage("session2", "user2", "I need VPN");

        // Then: both have independent states
        assertThat(creator.getState("session1")).isNotNull();
        assertThat(creator.getState("session2")).isNotNull();
        assertThat(creator.getState("session1").getUserId()).isEqualTo("user1");
        assertThat(creator.getState("session2").getUserId()).isEqualTo("user2");
    }

    @Test
    void processMessage_finalConfirmation_executesPendingAction() throws Exception {
        // Given: staged action with pending action ID
        ServiceMatchResult match = ServiceMatchResult.highConfidence(testService);
        when(intentMatcher.matchService(anyString())).thenReturn(match);

        PendingAction pendingAction = PendingAction.builder()
                .actionId("action123")
                .actionType(PendingAction.ActionType.WORK_ORDER_CREATE)
                .preview("Test preview")
                .build();

        when(confirmationService.stageWorkOrderCreation(anyString(), anyString(), any()))
                .thenReturn(pendingAction);

        ConfirmationService.ConfirmationResult confirmResult =
                ConfirmationService.ConfirmationResult.success("WO001", "Success");
        when(confirmationService.confirm(eq("action123"), anyString(), anyString()))
                .thenReturn(confirmResult);

        // Flow to confirmation with pending action
        creator.processMessage("session1", "user1", "I need VPN");
        creator.processMessage("session1", "user1", "yes");
        creator.processMessage("session1", "user1", "Remote access");
        creator.processMessage("session1", "user1", "Work from home");
        creator.processMessage("session1", "user1", "confirm"); // Stage

        // When: confirm the pending action
        GuidedResponse response = creator.processMessage("session1", "user1", "confirm");

        // Then: executes successfully
        assertThat(response.isSubmitted()).isTrue();
        assertThat(response.getRequestNumber()).isEqualTo("WO001");
        verify(confirmationService).confirm(eq("action123"), eq("session1"), eq("user1"));
    }

    @Test
    void processMessage_finalConfirmation_executionFails() throws Exception {
        // Given: staged action
        ServiceMatchResult match = ServiceMatchResult.highConfidence(testService);
        when(intentMatcher.matchService(anyString())).thenReturn(match);

        PendingAction pendingAction = PendingAction.builder()
                .actionId("action123")
                .actionType(PendingAction.ActionType.WORK_ORDER_CREATE)
                .preview("Test preview")
                .build();

        when(confirmationService.stageWorkOrderCreation(anyString(), anyString(), any()))
                .thenReturn(pendingAction);

        ConfirmationService.ConfirmationResult confirmResult =
                ConfirmationService.ConfirmationResult.failure("Failed to create");
        when(confirmationService.confirm(anyString(), anyString(), anyString()))
                .thenReturn(confirmResult);

        // Flow to confirmation
        creator.processMessage("session1", "user1", "I need VPN");
        creator.processMessage("session1", "user1", "yes");
        creator.processMessage("session1", "user1", "Remote");
        creator.processMessage("session1", "user1", "Work");
        creator.processMessage("session1", "user1", "confirm"); // Stage

        // When: confirm execution fails
        GuidedResponse response = creator.processMessage("session1", "user1", "confirm");

        // Then: returns error
        assertThat(response.isError()).isTrue();
        assertThat(response.getMessage()).contains("Failed");
    }

    @Test
    void processMessage_finalConfirmationCancel_cancelsPendingAction() throws Exception {
        // Given: staged action with pending action
        ServiceMatchResult match = ServiceMatchResult.highConfidence(testService);
        when(intentMatcher.matchService(anyString())).thenReturn(match);

        PendingAction pendingAction = PendingAction.builder()
                .actionId("action123")
                .actionType(PendingAction.ActionType.WORK_ORDER_CREATE)
                .preview("Test")
                .build();

        when(confirmationService.stageWorkOrderCreation(anyString(), anyString(), any()))
                .thenReturn(pendingAction);

        // Flow to confirmation
        creator.processMessage("session1", "user1", "I need VPN");
        creator.processMessage("session1", "user1", "yes");
        creator.processMessage("session1", "user1", "Remote");
        creator.processMessage("session1", "user1", "Work");
        creator.processMessage("session1", "user1", "confirm"); // Stage

        // When: cancel the pending action
        GuidedResponse response = creator.processMessage("session1", "user1", "cancel");

        // Then: cancels action through service
        assertThat(response.isCancelled()).isTrue();
        verify(confirmationService).cancel(eq("action123"), eq("session1"), eq("user1"));
    }

    @Test
    void processMessage_finalConfirmationEdit_cancelsPendingAction() throws Exception {
        // Given: staged action with pending action
        ServiceMatchResult match = ServiceMatchResult.highConfidence(testService);
        when(intentMatcher.matchService(anyString())).thenReturn(match);

        PendingAction pendingAction = PendingAction.builder()
                .actionId("action123")
                .actionType(PendingAction.ActionType.WORK_ORDER_CREATE)
                .preview("Test")
                .build();

        when(confirmationService.stageWorkOrderCreation(anyString(), anyString(), any()))
                .thenReturn(pendingAction);

        // Flow to confirmation
        creator.processMessage("session1", "user1", "I need VPN");
        creator.processMessage("session1", "user1", "yes");
        creator.processMessage("session1", "user1", "Remote");
        creator.processMessage("session1", "user1", "Work");
        creator.processMessage("session1", "user1", "confirm"); // Stage

        // When: request edit
        GuidedResponse response = creator.processMessage("session1", "user1", "edit");

        // Then: cancels pending action and returns to gathering
        assertThat(response.getState().getPhase()).isEqualTo(GuidedCreationState.Phase.GATHERING_FIELDS);
        assertThat(response.getState().getCollectedFields()).isEmpty();
        verify(confirmationService).cancel(eq("action123"), eq("session1"), eq("user1"));
    }

    @Test
    void processMessage_finalConfirmation_executionThrowsException() throws Exception {
        // Given: staged action
        ServiceMatchResult match = ServiceMatchResult.highConfidence(testService);
        when(intentMatcher.matchService(anyString())).thenReturn(match);

        PendingAction pendingAction = PendingAction.builder()
                .actionId("action123")
                .actionType(PendingAction.ActionType.WORK_ORDER_CREATE)
                .preview("Test")
                .build();

        when(confirmationService.stageWorkOrderCreation(anyString(), anyString(), any()))
                .thenReturn(pendingAction);

        when(confirmationService.confirm(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Connection failed"));

        // Flow to confirmation
        creator.processMessage("session1", "user1", "I need VPN");
        creator.processMessage("session1", "user1", "yes");
        creator.processMessage("session1", "user1", "Remote");
        creator.processMessage("session1", "user1", "Work");
        creator.processMessage("session1", "user1", "confirm"); // Stage

        // When: execution throws exception
        GuidedResponse response = creator.processMessage("session1", "user1", "confirm");

        // Then: returns error
        assertThat(response.isError()).isTrue();
        assertThat(response.getMessage()).contains("Connection failed");
    }

    @Test
    void processMessage_gatheringFieldsSkip_promptsAgain() {
        // Given: in gathering phase
        ServiceMatchResult match = ServiceMatchResult.highConfidence(testService);
        when(intentMatcher.matchService(anyString())).thenReturn(match);
        creator.processMessage("session1", "user1", "I need VPN");
        creator.processMessage("session1", "user1", "yes");

        // When: try to skip
        GuidedResponse response = creator.processMessage("session1", "user1", "skip");

        // Then: prompts again for current field
        assertThat(response.getMessage()).contains("Question");
    }

    @Test
    void processMessage_gatheringFieldsBack_promptsAgain() {
        // Given: in gathering phase
        ServiceMatchResult match = ServiceMatchResult.highConfidence(testService);
        when(intentMatcher.matchService(anyString())).thenReturn(match);
        creator.processMessage("session1", "user1", "I need VPN");
        creator.processMessage("session1", "user1", "yes");

        // When: try to go back
        GuidedResponse response = creator.processMessage("session1", "user1", "back");

        // Then: prompts again for current field
        assertThat(response.getMessage()).contains("Question");
    }

    @Test
    void processMessage_serviceSelectionArabicCancel_cancels() {
        // Given: in service selection phase
        ServiceMatchResult match = ServiceMatchResult.clarificationNeeded(List.of(testService));
        when(intentMatcher.matchService(anyString())).thenReturn(match);
        creator.processMessage("session1", "user1", "I need help");

        // When: cancel in Arabic
        GuidedResponse response = creator.processMessage("session1", "user1", "إلغاء");

        // Then: cancels flow
        assertThat(response.isCancelled()).isTrue();
    }

    @Test
    void processMessage_serviceSelectionByArabicName_selectsService() {
        // Given: multiple services with Arabic names
        ServiceMatchResult match = ServiceMatchResult.clarificationNeeded(List.of(testService));
        when(intentMatcher.matchService(anyString())).thenReturn(match);
        creator.processMessage("session1", "user1", "I need help");

        // When: select by Arabic name
        GuidedResponse response = creator.processMessage("session1", "user1", "طلب VPN");

        // Then: selects matching service
        assertThat(response.getState().getPhase()).isEqualTo(GuidedCreationState.Phase.GATHERING_FIELDS);
    }

    @Test
    void processMessage_serviceConfirmationArabicResponses_handled() {
        // Given: in confirmation phase
        ServiceMatchResult match = ServiceMatchResult.highConfidence(testService);
        when(intentMatcher.matchService(anyString())).thenReturn(match);
        creator.processMessage("session1", "user1", "I need VPN");

        // When: confirm in Arabic
        GuidedResponse response = creator.processMessage("session1", "user1", "نعم");

        // Then: proceeds to gathering
        assertThat(response.getState().getPhase()).isEqualTo(GuidedCreationState.Phase.GATHERING_FIELDS);
    }

    @Test
    void processMessage_serviceConfirmationArabicNo_showsCategories() {
        // Given: in confirmation phase
        ServiceMatchResult match = ServiceMatchResult.highConfidence(testService);
        when(intentMatcher.matchService(anyString())).thenReturn(match);
        when(intentMatcher.getServiceCategoriesDisplay()).thenReturn("Categories...");
        creator.processMessage("session1", "user1", "I need VPN");

        // When: reject in Arabic
        GuidedResponse response = creator.processMessage("session1", "user1", "لا");

        // Then: shows categories
        assertThat(response.isShowCategories()).isTrue();
    }

    @Test
    void processMessage_gatheringFieldsArabicCancel_cancels() {
        // Given: in gathering phase
        ServiceMatchResult match = ServiceMatchResult.highConfidence(testService);
        when(intentMatcher.matchService(anyString())).thenReturn(match);
        creator.processMessage("session1", "user1", "I need VPN");
        creator.processMessage("session1", "user1", "yes");

        // When: cancel in Arabic
        GuidedResponse response = creator.processMessage("session1", "user1", "إلغاء");

        // Then: cancels flow
        assertThat(response.isCancelled()).isTrue();
    }

    @Test
    void processMessage_submitRequest_failsStaging() {
        // Given: ready to submit
        ServiceMatchResult match = ServiceMatchResult.highConfidence(testService);
        when(intentMatcher.matchService(anyString())).thenReturn(match);

        when(confirmationService.stageWorkOrderCreation(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Staging failed"));

        // Flow to confirmation
        creator.processMessage("session1", "user1", "I need VPN");
        creator.processMessage("session1", "user1", "yes");
        creator.processMessage("session1", "user1", "Remote");
        creator.processMessage("session1", "user1", "Work");

        // When: submit fails
        GuidedResponse response = creator.processMessage("session1", "user1", "confirm");

        // Then: returns error
        assertThat(response.isError()).isTrue();
        assertThat(response.getMessage()).contains("Staging failed");
    }

    @Test
    void processMessage_serviceSelectionFallbackToIntentMatch_success() {
        // Given: in service selection with invalid number
        DameeService service2 = DameeService.builder()
                .serviceId("10242")
                .nameEn("Email Service")
                .requiredFields(List.of("requestType"))
                .build();

        ServiceMatchResult match1 = ServiceMatchResult.clarificationNeeded(List.of(testService));
        ServiceMatchResult match2 = ServiceMatchResult.highConfidence(service2);
        when(intentMatcher.matchService(anyString())).thenReturn(match1, match2);

        creator.processMessage("session1", "user1", "I need help");

        // When: provide text that matches a different service
        GuidedResponse response = creator.processMessage("session1", "user1", "I need email help");

        // Then: uses intent matcher fallback
        assertThat(response.getState().getPhase()).isEqualTo(GuidedCreationState.Phase.GATHERING_FIELDS);
    }

    // ====================================================================
    // Coverage gap tests: getFieldQuestion switch cases
    // ====================================================================

    @Nested
    @DisplayName("getFieldQuestion switch branches")
    class GetFieldQuestionTests {

        /**
         * Helper: creates a service with a single required field, drives the flow
         * to GATHERING_FIELDS, and returns the response (which contains the question).
         */
        private GuidedResponse driveToFieldQuestion(String fieldName) {
            DameeService svc = DameeService.builder()
                    .serviceId("svc-1")
                    .nameEn("Test Service")
                    .requiredFields(List.of(fieldName))
                    .build();

            ServiceMatchResult match = ServiceMatchResult.highConfidence(svc);
            // Need two returns: first for initial query, second is unused but safe
            when(intentMatcher.matchService(anyString())).thenReturn(match);

            // Initial message -> CONFIRMING_SERVICE
            creator.processMessage("fq-" + fieldName, "user1", "test");
            // Confirm -> GATHERING_FIELDS (returns question for the single field)
            return creator.processMessage("fq-" + fieldName, "user1", "yes");
        }

        @Test
        void applicationName_returnsApplicationQuestion() {
            GuidedResponse r = driveToFieldQuestion("applicationName");
            assertThat(r.getMessage()).contains("Which application is this for?");
        }

        @Test
        void serviceOption_returnsServiceOptionQuestion() {
            GuidedResponse r = driveToFieldQuestion("serviceOption");
            assertThat(r.getMessage()).contains("Which service option do you need?");
        }

        @Test
        void softwareName_returnsSoftwareQuestion() {
            GuidedResponse r = driveToFieldQuestion("softwareName");
            assertThat(r.getMessage()).contains("Which software do you need installed?");
        }

        @Test
        void vpnType_returnsVpnQuestion() {
            GuidedResponse r = driveToFieldQuestion("vpnType");
            assertThat(r.getMessage()).contains("What type of VPN access do you need?");
        }

        @Test
        void deviceType_returnsDeviceQuestion() {
            GuidedResponse r = driveToFieldQuestion("deviceType");
            assertThat(r.getMessage()).contains("What type of device is this for?");
        }

        @Test
        void requestDate_returnsDateQuestion() {
            GuidedResponse r = driveToFieldQuestion("requestDate");
            assertThat(r.getMessage()).contains("When do you need this?");
        }

        @Test
        void caseDetails_returnsCaseDetailsQuestion() {
            GuidedResponse r = driveToFieldQuestion("caseDetails");
            assertThat(r.getMessage()).contains("Please provide the case details");
        }

        @Test
        void dataRequirements_returnsDataRequirementsQuestion() {
            GuidedResponse r = driveToFieldQuestion("dataRequirements");
            assertThat(r.getMessage()).contains("What data or requirements do you need?");
        }

        @Test
        void unknownField_returnsDefaultQuestion() {
            GuidedResponse r = driveToFieldQuestion("customField");
            assertThat(r.getMessage()).contains("Please provide: Custom field");
        }

        @Test
        void description_returnsDescriptionQuestion() {
            GuidedResponse r = driveToFieldQuestion("description");
            assertThat(r.getMessage()).contains("Please describe your request in detail");
        }

        @Test
        void justification_returnsJustificationQuestion() {
            GuidedResponse r = driveToFieldQuestion("justification");
            assertThat(r.getMessage()).contains("What is the business justification");
        }
    }

    // ====================================================================
    // Coverage gap tests: handleFinalConfirmation unknown response
    // ====================================================================

    @Nested
    @DisplayName("handleFinalConfirmation - unknown response")
    class FinalConfirmationUnknownResponseTests {

        @Test
        void unknownResponse_returnsPromptWithOptions() {
            // Drive to AWAITING_CONFIRMATION phase
            ServiceMatchResult match = ServiceMatchResult.highConfidence(testService);
            when(intentMatcher.matchService(anyString())).thenReturn(match);

            creator.processMessage("sess-unk", "user1", "I need VPN");
            creator.processMessage("sess-unk", "user1", "yes");
            creator.processMessage("sess-unk", "user1", "Remote access");
            creator.processMessage("sess-unk", "user1", "Work from home");

            // Now in AWAITING_CONFIRMATION. Send an unknown response.
            GuidedResponse response = creator.processMessage("sess-unk", "user1", "maybe");

            assertThat(response.getMessage()).contains("Please respond with 'Confirm'");
            assertThat(response.getOptions()).hasSize(3);
            assertThat(response.getOptions().get(0).getValue()).isEqualTo("confirm");
            assertThat(response.getOptions().get(1).getValue()).isEqualTo("edit");
            assertThat(response.getOptions().get(2).getValue()).isEqualTo("cancel");
        }
    }

    // ====================================================================
    // Coverage gap tests: executeConfirmedAction with null service
    // ====================================================================

    @Nested
    @DisplayName("executeConfirmedAction - null service fallback")
    class ExecuteConfirmedActionNullServiceTests {

        @Test
        void nullService_usesDefaultServiceName() throws Exception {
            // Drive flow to staged action
            ServiceMatchResult match = ServiceMatchResult.highConfidence(testService);
            when(intentMatcher.matchService(anyString())).thenReturn(match);

            PendingAction pendingAction = PendingAction.builder()
                    .actionId("action-null-svc")
                    .actionType(PendingAction.ActionType.WORK_ORDER_CREATE)
                    .preview("Preview")
                    .build();

            when(confirmationService.stageWorkOrderCreation(anyString(), anyString(), any()))
                    .thenReturn(pendingAction);

            ConfirmationService.ConfirmationResult confirmResult =
                    ConfirmationService.ConfirmationResult.success("WO-NULL", "OK");
            when(confirmationService.confirm(anyString(), anyString(), anyString()))
                    .thenReturn(confirmResult);

            creator.processMessage("sess-null-svc", "user1", "I need VPN");
            creator.processMessage("sess-null-svc", "user1", "yes");
            creator.processMessage("sess-null-svc", "user1", "Remote");
            creator.processMessage("sess-null-svc", "user1", "Work");
            creator.processMessage("sess-null-svc", "user1", "confirm"); // Stage

            // Manually clear selected service to trigger null fallback
            GuidedCreationState state = creator.getState("sess-null-svc");
            state.setSelectedService(null);

            // When: confirm with null service
            GuidedResponse response = creator.processMessage("sess-null-svc", "user1", "confirm");

            // Then: uses "Service Request" as fallback
            assertThat(response.isSubmitted()).isTrue();
            assertThat(response.getMessage()).contains("Service Request");
            assertThat(response.getMessage()).contains("WO-NULL");
        }
    }

    // ====================================================================
    // Coverage gap tests: buildSubmissionPreview
    // ====================================================================

    @Nested
    @DisplayName("buildSubmissionPreview - VIP bypass and null workflow")
    class BuildSubmissionPreviewTests {

        @Test
        void vipBypass_showsVipMessage() {
            // Create a VIP-enabled service
            DameeService vipService = DameeService.builder()
                    .serviceId("vip-svc")
                    .nameEn("VIP Service")
                    .category("IT Services")
                    .requiredFields(List.of("description"))
                    .vipBypass(true)
                    .workflow(List.of(
                            DameeService.WorkflowStep.builder()
                                    .order(1)
                                    .description("Submit Form")
                                    .build()
                    ))
                    .build();

            ServiceMatchResult match = ServiceMatchResult.highConfidence(vipService);
            when(intentMatcher.matchService(anyString())).thenReturn(match);

            creator.processMessage("sess-vip", "user1", "test");
            creator.processMessage("sess-vip", "user1", "yes");

            // Provide all fields, which triggers transitionToConfirmation -> buildSubmissionPreview
            GuidedResponse response = creator.processMessage("sess-vip", "user1", "My description");

            // Then: preview contains VIP bypass message
            assertThat(response.getMessage()).contains("VIP Status");
            assertThat(response.getMessage()).contains("Manager approval may be bypassed");
        }

        @Test
        void nullWorkflow_showsStandardWorkflow() {
            // Create a service with null workflow
            DameeService noWorkflowService = DameeService.builder()
                    .serviceId("no-wf-svc")
                    .nameEn("No Workflow Service")
                    .category("IT Services")
                    .requiredFields(List.of("description"))
                    .workflow(null)
                    .build();

            ServiceMatchResult match = ServiceMatchResult.highConfidence(noWorkflowService);
            when(intentMatcher.matchService(anyString())).thenReturn(match);

            creator.processMessage("sess-no-wf", "user1", "test");
            creator.processMessage("sess-no-wf", "user1", "yes");

            // Provide all fields
            GuidedResponse response = creator.processMessage("sess-no-wf", "user1", "My description");

            // Then: shows "Standard workflow"
            assertThat(response.getMessage()).contains("Standard workflow");
        }

        @Test
        void withWorkflowSteps_showsStepDescriptions() {
            // The testService already has workflow steps
            ServiceMatchResult match = ServiceMatchResult.highConfidence(testService);
            when(intentMatcher.matchService(anyString())).thenReturn(match);

            creator.processMessage("sess-wf", "user1", "I need VPN");
            creator.processMessage("sess-wf", "user1", "yes");
            creator.processMessage("sess-wf", "user1", "Remote access");

            // Provide last field to trigger confirmation preview
            GuidedResponse response = creator.processMessage("sess-wf", "user1", "Work from home");

            // Then: shows workflow step descriptions
            assertThat(response.getMessage()).contains("Fill Form");
            assertThat(response.getMessage()).contains("Manager Approval");
        }
    }

    // ====================================================================
    // Coverage gap tests: formatFieldName in GuidedServiceCreator
    // ====================================================================

    @Nested
    @DisplayName("formatFieldName via getFieldQuestion default case")
    class FormatFieldNameTests {

        @Test
        void multiWordCamelCase_convertsToTitleCase() {
            // Drive to field question with a camelCase field that hits the default case
            DameeService svc = DameeService.builder()
                    .serviceId("fmt-svc")
                    .nameEn("Format Test")
                    .requiredFields(List.of("myCustomFieldName"))
                    .build();

            ServiceMatchResult match = ServiceMatchResult.highConfidence(svc);
            when(intentMatcher.matchService(anyString())).thenReturn(match);

            creator.processMessage("sess-fmt", "user1", "test");
            GuidedResponse response = creator.processMessage("sess-fmt", "user1", "yes");

            // Default case: "Please provide: My custom field name"
            assertThat(response.getMessage()).contains("My custom field name");
        }
    }
}
