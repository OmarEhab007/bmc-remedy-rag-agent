package com.bmc.rag.agent.confirmation;

import com.bmc.rag.agent.confirmation.ConfirmationService.ConfirmationResult;
import com.bmc.rag.agent.confirmation.PendingAction.ActionStatus;
import com.bmc.rag.agent.confirmation.PendingAction.ActionType;
import com.bmc.rag.agent.security.AgenticRateLimiter;
import com.bmc.rag.connector.creator.IncidentCreator;
import com.bmc.rag.connector.creator.IncidentUpdater;
import com.bmc.rag.connector.creator.WorkOrderCreator;
import com.bmc.rag.connector.dto.CreationResult;
import com.bmc.rag.connector.dto.IncidentCreationRequest;
import com.bmc.rag.connector.dto.IncidentUpdateRequest;
import com.bmc.rag.connector.dto.WorkOrderCreationRequest;
import com.bmc.rag.store.entity.ActionAuditEntity;
import com.bmc.rag.store.repository.ActionAuditRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConfirmationService.
 */
@ExtendWith(MockitoExtension.class)
class ConfirmationServiceTest {

    @Mock
    private IncidentCreator incidentCreator;

    @Mock
    private IncidentUpdater incidentUpdater;

    @Mock
    private WorkOrderCreator workOrderCreator;

    @Mock
    private ActionAuditRepository auditRepository;

    @Mock
    private AgenticRateLimiter rateLimiter;

    private ConfirmationService confirmationService;

    private static final String SESSION_ID = "test-session";
    private static final String USER_ID = "test-user";
    private static final int TIMEOUT_MINUTES = 5;

    @BeforeEach
    void setUp() {
        confirmationService = new ConfirmationService(
            incidentCreator,
            incidentUpdater,
            workOrderCreator,
            auditRepository,
            rateLimiter,
            TIMEOUT_MINUTES
        );

        // Stub auditRepository.save() globally - called by all stage*() methods
        lenient().when(auditRepository.save(any(ActionAuditEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void stageIncidentCreation_validRequest_returnsPendingAction() {
        // Given
        IncidentCreationRequest request = IncidentCreationRequest.builder()
            .summary("VPN connection issue")
            .description("Cannot connect to VPN")
            .impact(3)
            .urgency(3)
            .build();

        // When
        PendingAction action = confirmationService.stageIncidentCreation(SESSION_ID, USER_ID, request);

        // Then
        assertNotNull(action);
        assertEquals(SESSION_ID, action.getSessionId());
        assertEquals(USER_ID, action.getUserId());
        assertEquals(ActionType.INCIDENT_CREATE, action.getActionType());
        assertEquals(ActionStatus.PENDING, action.getStatus());
        assertNotNull(action.getActionId());
        assertNotNull(action.getExpiresAt());
        assertNotNull(action.getPreview());
        assertEquals(request, action.getPayload());

        // Verify audit repository was called
        ArgumentCaptor<ActionAuditEntity> auditCaptor = ArgumentCaptor.forClass(ActionAuditEntity.class);
        verify(auditRepository).save(auditCaptor.capture());

        ActionAuditEntity audit = auditCaptor.getValue();
        assertEquals(action.getActionId(), audit.getActionId());
        assertEquals(SESSION_ID, audit.getSessionId());
        assertEquals(USER_ID, audit.getUserId());
        assertEquals(ActionAuditEntity.ActionType.INCIDENT_CREATE, audit.getActionType());
    }

    @Test
    void stageWorkOrderCreation_validRequest_returnsPendingAction() {
        // Given
        WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
            .summary("Install software")
            .description("Install Adobe Acrobat")
            .build();

        // When
        PendingAction action = confirmationService.stageWorkOrderCreation(SESSION_ID, USER_ID, request);

        // Then
        assertNotNull(action);
        assertEquals(ActionType.WORK_ORDER_CREATE, action.getActionType());
        assertEquals(ActionStatus.PENDING, action.getStatus());
        assertEquals(request, action.getPayload());

        verify(auditRepository).save(any(ActionAuditEntity.class));
    }

    @Test
    void stageIncidentUpdate_validRequest_returnsPendingAction() {
        // Given
        IncidentUpdateRequest request = IncidentUpdateRequest.builder()
            .incidentNumber("INC000123")
            .status(4)
            .resolution("VPN service restarted")
            .build();

        // When
        PendingAction action = confirmationService.stageIncidentUpdate(SESSION_ID, USER_ID, request);

        // Then
        assertNotNull(action);
        assertEquals(ActionType.INCIDENT_UPDATE, action.getActionType());
        assertEquals(ActionStatus.PENDING, action.getStatus());
        assertEquals(request, action.getPayload());

        verify(auditRepository).save(any(ActionAuditEntity.class));
    }

    @Test
    void confirm_validAction_executesSuccessfully() {
        // Given
        IncidentCreationRequest request = IncidentCreationRequest.builder()
            .summary("Test incident")
            .description("Test description")
            .impact(3)
            .urgency(3)
            .build();

        PendingAction stagedAction = confirmationService.stageIncidentCreation(SESSION_ID, USER_ID, request);
        String actionId = stagedAction.getActionId();

        // Mock successful creation
        CreationResult creationResult = CreationResult.success("entry-1", "INC000999", "HPD:Help Desk");
        when(incidentCreator.createIncident(any(IncidentCreationRequest.class)))
            .thenReturn(creationResult);

        ActionAuditEntity mockAudit = new ActionAuditEntity();
        when(auditRepository.findByActionId(actionId))
            .thenReturn(Optional.of(mockAudit));

        // When
        ConfirmationResult result = confirmationService.confirm(actionId, SESSION_ID, USER_ID);

        // Then
        assertTrue(result.success());
        assertFalse(result.cancelled());
        assertEquals("INC000999", result.recordId());
        assertEquals("Successfully created INC000999", result.message());

        verify(incidentCreator).createIncident(request);
        verify(rateLimiter).recordAction(USER_ID);
        verify(auditRepository, atLeastOnce()).save(any(ActionAuditEntity.class));
    }

    @Test
    void confirm_nonExistentAction_returnsFailure() {
        // When
        ConfirmationResult result = confirmationService.confirm("non-existent-id", SESSION_ID, USER_ID);

        // Then
        assertFalse(result.success());
        assertEquals("Action not found or already expired", result.message());

        verify(incidentCreator, never()).createIncident(any());
        verify(rateLimiter, never()).recordAction(any());
    }

    @Test
    void confirm_wrongSession_returnsFailure() {
        // Given
        IncidentCreationRequest request = IncidentCreationRequest.builder()
            .summary("Test incident")
            .description("Test description")
            .impact(3)
            .urgency(3)
            .build();

        PendingAction stagedAction = confirmationService.stageIncidentCreation(SESSION_ID, USER_ID, request);
        String actionId = stagedAction.getActionId();

        // When - try to confirm with wrong session
        ConfirmationResult result = confirmationService.confirm(actionId, "wrong-session", USER_ID);

        // Then
        assertFalse(result.success());
        assertEquals("Action does not belong to this session", result.message());

        verify(incidentCreator, never()).createIncident(any());
    }

    @Test
    void confirm_expiredAction_returnsFailure() {
        // Given - create an action with past expiry time
        IncidentCreationRequest request = IncidentCreationRequest.builder()
            .summary("Test incident")
            .description("Test description")
            .impact(3)
            .urgency(3)
            .build();

        when(auditRepository.save(any(ActionAuditEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        PendingAction stagedAction = confirmationService.stageIncidentCreation(SESSION_ID, USER_ID, request);

        // Manually set expiry to past
        stagedAction.setExpiresAt(Instant.now().minusSeconds(60));
        String actionId = stagedAction.getActionId();

        // When
        ConfirmationResult result = confirmationService.confirm(actionId, SESSION_ID, USER_ID);

        // Then
        assertFalse(result.success());
        assertEquals("Action has expired", result.message());

        verify(incidentCreator, never()).createIncident(any());
    }

    @Test
    void confirm_creationFails_returnsFailure() {
        // Given
        IncidentCreationRequest request = IncidentCreationRequest.builder()
            .summary("Test incident")
            .description("Test description")
            .impact(3)
            .urgency(3)
            .build();

        PendingAction stagedAction = confirmationService.stageIncidentCreation(SESSION_ID, USER_ID, request);
        String actionId = stagedAction.getActionId();

        // Mock failed creation
        CreationResult creationResult = CreationResult.failure("Database connection error");
        when(incidentCreator.createIncident(any(IncidentCreationRequest.class)))
            .thenReturn(creationResult);

        ActionAuditEntity mockAudit = new ActionAuditEntity();
        when(auditRepository.findByActionId(actionId))
            .thenReturn(Optional.of(mockAudit));

        // When
        ConfirmationResult result = confirmationService.confirm(actionId, SESSION_ID, USER_ID);

        // Then
        assertFalse(result.success());
        assertEquals("Database connection error", result.message());

        verify(incidentCreator).createIncident(request);
        verify(rateLimiter, never()).recordAction(any()); // Should NOT record on failure
    }

    @Test
    void cancel_validAction_returnsSuccess() {
        // Given
        IncidentCreationRequest request = IncidentCreationRequest.builder()
            .summary("Test incident")
            .description("Test description")
            .impact(3)
            .urgency(3)
            .build();

        PendingAction stagedAction = confirmationService.stageIncidentCreation(SESSION_ID, USER_ID, request);
        String actionId = stagedAction.getActionId();

        ActionAuditEntity mockAudit = new ActionAuditEntity();
        when(auditRepository.findByActionId(actionId))
            .thenReturn(Optional.of(mockAudit));

        // When
        ConfirmationResult result = confirmationService.cancel(actionId, SESSION_ID, USER_ID);

        // Then
        assertFalse(result.success());
        assertTrue(result.cancelled());
        assertEquals("Action cancelled successfully", result.message());

        verify(auditRepository, atLeastOnce()).save(any(ActionAuditEntity.class));
    }

    @Test
    void cancel_nonExistentAction_returnsFailure() {
        // When
        ConfirmationResult result = confirmationService.cancel("non-existent-id", SESSION_ID, USER_ID);

        // Then
        assertFalse(result.success());
        assertFalse(result.cancelled());
        assertEquals("Action not found", result.message());
    }

    @Test
    void cancel_wrongSession_returnsFailure() {
        // Given
        IncidentCreationRequest request = IncidentCreationRequest.builder()
            .summary("Test incident")
            .description("Test description")
            .impact(3)
            .urgency(3)
            .build();

        PendingAction stagedAction = confirmationService.stageIncidentCreation(SESSION_ID, USER_ID, request);
        String actionId = stagedAction.getActionId();

        // When
        ConfirmationResult result = confirmationService.cancel(actionId, "wrong-session", USER_ID);

        // Then
        assertFalse(result.success());
        assertEquals("Action does not belong to this session", result.message());
    }

    @Test
    void getAction_existingAction_returnsAction() {
        // Given
        IncidentCreationRequest request = IncidentCreationRequest.builder()
            .summary("Test incident")
            .description("Test description")
            .impact(3)
            .urgency(3)
            .build();

        PendingAction stagedAction = confirmationService.stageIncidentCreation(SESSION_ID, USER_ID, request);
        String actionId = stagedAction.getActionId();

        // When
        Optional<PendingAction> result = confirmationService.getAction(actionId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(actionId, result.get().getActionId());
    }

    @Test
    void getAction_nonExistentAction_returnsEmpty() {
        // When
        Optional<PendingAction> result = confirmationService.getAction("non-existent-id");

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void getPendingActionsForSession_returnsPendingActions() {
        // Given
        IncidentCreationRequest request1 = IncidentCreationRequest.builder()
            .summary("Incident 1")
            .description("Description 1")
            .impact(3)
            .urgency(3)
            .build();

        IncidentCreationRequest request2 = IncidentCreationRequest.builder()
            .summary("Incident 2")
            .description("Description 2")
            .impact(2)
            .urgency(2)
            .build();

        confirmationService.stageIncidentCreation(SESSION_ID, USER_ID, request1);
        confirmationService.stageIncidentCreation(SESSION_ID, USER_ID, request2);

        // Stage one for different session
        confirmationService.stageIncidentCreation("other-session", USER_ID, request1);

        // When
        List<PendingAction> actions = confirmationService.getPendingActionsForSession(SESSION_ID);

        // Then
        assertEquals(2, actions.size());
        assertTrue(actions.stream().allMatch(a -> SESSION_ID.equals(a.getSessionId())));
        assertTrue(actions.stream().allMatch(a -> a.getStatus() == ActionStatus.PENDING));
    }

    @Test
    void getPendingActionsForUser_returnsPendingActions() {
        // Given
        IncidentCreationRequest request = IncidentCreationRequest.builder()
            .summary("Incident")
            .description("Description")
            .impact(3)
            .urgency(3)
            .build();

        confirmationService.stageIncidentCreation(SESSION_ID, USER_ID, request);
        confirmationService.stageIncidentCreation("session-2", USER_ID, request);

        // Stage one for different user
        confirmationService.stageIncidentCreation(SESSION_ID, "other-user", request);

        // When
        List<PendingAction> actions = confirmationService.getPendingActionsForUser(USER_ID);

        // Then
        assertEquals(2, actions.size());
        assertTrue(actions.stream().allMatch(a -> USER_ID.equals(a.getUserId())));
    }

    @Test
    void getCacheStats_returnsStatistics() {
        // Given
        IncidentCreationRequest request = IncidentCreationRequest.builder()
            .summary("Test incident")
            .description("Test description")
            .impact(3)
            .urgency(3)
            .build();

        confirmationService.stageIncidentCreation(SESSION_ID, USER_ID, request);

        // When
        ConfirmationService.CacheStats stats = confirmationService.getCacheStats();

        // Then
        assertNotNull(stats);
        assertTrue(stats.size() >= 1);
        assertEquals(0, stats.hitCount()); // No cache hits yet
    }
}
