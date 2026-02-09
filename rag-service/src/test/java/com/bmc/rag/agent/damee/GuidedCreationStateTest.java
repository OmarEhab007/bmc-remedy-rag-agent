package com.bmc.rag.agent.damee;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GuidedCreationState}.
 */
class GuidedCreationStateTest {

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @Test
        void initial_createsInitialState() {
            GuidedCreationState state = GuidedCreationState.initial("s1", "u1");

            assertThat(state.getPhase()).isEqualTo(GuidedCreationState.Phase.INITIAL);
            assertThat(state.getSessionId()).isEqualTo("s1");
            assertThat(state.getUserId()).isEqualTo("u1");
            assertThat(state.getCollectedFields()).isEmpty();
        }

        @Test
        void selectingService_createsSelectionState() {
            DameeService service1 = DameeService.builder()
                    .serviceId("10101")
                    .nameEn("Service 1")
                    .build();

            GuidedCreationState state = GuidedCreationState.selectingService(
                    "s1", "u1", List.of(service1), "query");

            assertThat(state.getPhase()).isEqualTo(GuidedCreationState.Phase.AWAITING_SERVICE_SELECTION);
            assertThat(state.getCandidateServices()).hasSize(1);
            assertThat(state.getOriginalQuery()).isEqualTo("query");
        }

        @Test
        void confirmingService_createsConfirmationState() {
            DameeService service = DameeService.builder()
                    .serviceId("10101")
                    .nameEn("Service 1")
                    .build();

            GuidedCreationState state = GuidedCreationState.confirmingService(
                    "s1", "u1", service, "query");

            assertThat(state.getPhase()).isEqualTo(GuidedCreationState.Phase.CONFIRMING_SERVICE);
            assertThat(state.getSelectedService()).isEqualTo(service);
            assertThat(state.getOriginalQuery()).isEqualTo("query");
        }
    }

    @Nested
    @DisplayName("State Transitions")
    class StateTransitions {

        @Test
        void startGatheringFields_transitionsToGathering() {
            DameeService service = DameeService.builder()
                    .serviceId("10101")
                    .nameEn("VPN Request")
                    .requiredFields(List.of("vpnType", "justification"))
                    .build();

            GuidedCreationState state = GuidedCreationState.initial("s1", "u1");
            state.startGatheringFields(service);

            assertThat(state.getPhase()).isEqualTo(GuidedCreationState.Phase.GATHERING_FIELDS);
            assertThat(state.getSelectedService()).isEqualTo(service);
            assertThat(state.getCurrentFieldIndex()).isEqualTo(0);
            assertThat(state.getCurrentFieldName()).isEqualTo("vpnType");
        }

        @Test
        void recordFieldValue_recordsAndAdvances() {
            DameeService service = DameeService.builder()
                    .serviceId("10101")
                    .nameEn("VPN Request")
                    .requiredFields(List.of("vpnType", "justification"))
                    .build();

            GuidedCreationState state = GuidedCreationState.initial("s1", "u1");
            state.startGatheringFields(service);
            state.recordFieldValue("vpnType", "Remote access");

            assertThat(state.getCollectedFields()).containsEntry("vpnType", "Remote access");
            assertThat(state.getCurrentFieldIndex()).isEqualTo(1);
            assertThat(state.getCurrentFieldName()).isEqualTo("justification");
            assertThat(state.getPhase()).isEqualTo(GuidedCreationState.Phase.GATHERING_FIELDS);
        }

        @Test
        void recordFieldValue_lastField_transitionsToConfirmation() {
            DameeService service = DameeService.builder()
                    .serviceId("10101")
                    .nameEn("VPN Request")
                    .requiredFields(List.of("vpnType"))
                    .build();

            GuidedCreationState state = GuidedCreationState.initial("s1", "u1");
            state.startGatheringFields(service);
            state.recordFieldValue("vpnType", "Remote");

            assertThat(state.getPhase()).isEqualTo(GuidedCreationState.Phase.AWAITING_CONFIRMATION);
            assertThat(state.areAllFieldsCollected()).isTrue();
        }

        @Test
        void markSubmitted_transitionsToSubmitted() {
            GuidedCreationState state = GuidedCreationState.initial("s1", "u1");
            state.markSubmitted("WO12345");

            assertThat(state.getPhase()).isEqualTo(GuidedCreationState.Phase.SUBMITTED);
            assertThat(state.getRequestNumber()).isEqualTo("WO12345");
        }

        @Test
        void markCancelled_transitionsToCancelled() {
            GuidedCreationState state = GuidedCreationState.initial("s1", "u1");
            state.markCancelled();

            assertThat(state.getPhase()).isEqualTo(GuidedCreationState.Phase.CANCELLED);
        }
    }

    @Nested
    @DisplayName("Field Management")
    class FieldManagement {

        @Test
        void getNextRequiredField_returnsFirstUncollected() {
            DameeService service = DameeService.builder()
                    .serviceId("10101")
                    .nameEn("VPN Request")
                    .requiredFields(List.of("field1", "field2", "field3"))
                    .build();

            GuidedCreationState state = GuidedCreationState.initial("s1", "u1");
            state.startGatheringFields(service);
            state.getCollectedFields().put("field1", "value1");

            String nextField = state.getNextRequiredField();
            assertThat(nextField).isEqualTo("field2");
        }

        @Test
        void getNextRequiredField_allCollected_returnsNull() {
            DameeService service = DameeService.builder()
                    .serviceId("10101")
                    .nameEn("VPN Request")
                    .requiredFields(List.of("field1"))
                    .build();

            GuidedCreationState state = GuidedCreationState.initial("s1", "u1");
            state.startGatheringFields(service);
            state.getCollectedFields().put("field1", "value1");

            String nextField = state.getNextRequiredField();
            assertThat(nextField).isNull();
        }

        @Test
        void getNextRequiredField_noService_returnsNull() {
            GuidedCreationState state = GuidedCreationState.initial("s1", "u1");

            String nextField = state.getNextRequiredField();
            assertThat(nextField).isNull();
        }

        @Test
        void getNextRequiredField_noRequiredFields_returnsNull() {
            DameeService service = DameeService.builder()
                    .serviceId("10101")
                    .nameEn("Simple Service")
                    .requiredFields(null)
                    .build();

            GuidedCreationState state = GuidedCreationState.initial("s1", "u1");
            state.startGatheringFields(service);

            String nextField = state.getNextRequiredField();
            assertThat(nextField).isNull();
        }

        @Test
        void areAllFieldsCollected_allCollected_returnsTrue() {
            DameeService service = DameeService.builder()
                    .serviceId("10101")
                    .nameEn("VPN Request")
                    .requiredFields(List.of("field1", "field2"))
                    .build();

            GuidedCreationState state = GuidedCreationState.initial("s1", "u1");
            state.startGatheringFields(service);
            state.getCollectedFields().put("field1", "value1");
            state.getCollectedFields().put("field2", "value2");

            assertThat(state.areAllFieldsCollected()).isTrue();
        }

        @Test
        void areAllFieldsCollected_someRemaining_returnsFalse() {
            DameeService service = DameeService.builder()
                    .serviceId("10101")
                    .nameEn("VPN Request")
                    .requiredFields(List.of("field1", "field2"))
                    .build();

            GuidedCreationState state = GuidedCreationState.initial("s1", "u1");
            state.startGatheringFields(service);
            state.getCollectedFields().put("field1", "value1");

            assertThat(state.areAllFieldsCollected()).isFalse();
        }

        @Test
        void areAllFieldsCollected_noService_returnsTrue() {
            GuidedCreationState state = GuidedCreationState.initial("s1", "u1");

            assertThat(state.areAllFieldsCollected()).isTrue();
        }

        @Test
        void getRemainingFieldCount_calculatesCorrectly() {
            DameeService service = DameeService.builder()
                    .serviceId("10101")
                    .nameEn("VPN Request")
                    .requiredFields(List.of("field1", "field2", "field3"))
                    .build();

            GuidedCreationState state = GuidedCreationState.initial("s1", "u1");
            state.startGatheringFields(service);
            state.getCollectedFields().put("field1", "value1");

            assertThat(state.getRemainingFieldCount()).isEqualTo(2);
        }

        @Test
        void getRemainingFieldCount_noService_returnsZero() {
            GuidedCreationState state = GuidedCreationState.initial("s1", "u1");

            assertThat(state.getRemainingFieldCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Summary and Display")
    class SummaryAndDisplay {

        @Test
        void getCollectedFieldsSummary_formatsCorrectly() {
            GuidedCreationState state = GuidedCreationState.initial("s1", "u1");
            state.getCollectedFields().put("vpnType", "Remote access");
            state.getCollectedFields().put("justification", "Work from home");

            String summary = state.getCollectedFieldsSummary();

            // Field names are formatted from camelCase to Title Case
            assertThat(summary).contains("Vpn Type"); // vpnType -> Vpn Type
            assertThat(summary).contains("Remote access");
            assertThat(summary).contains("Justification");
            assertThat(summary).contains("Work from home");
        }

        @Test
        void getCollectedFieldsSummary_emptyFields_returnsEmpty() {
            GuidedCreationState state = GuidedCreationState.initial("s1", "u1");

            String summary = state.getCollectedFieldsSummary();

            assertThat(summary).isEmpty();
        }

        @Test
        void formatFieldName_camelCaseToTitleCase() {
            GuidedCreationState state = GuidedCreationState.initial("s1", "u1");
            state.getCollectedFields().put("requestDate", "2024-01-01");

            String summary = state.getCollectedFieldsSummary();

            // The formatted field name should be "Request Date" (camelCase to Title Case)
            assertThat(summary).contains("Request Date");
        }
    }

    @Nested
    @DisplayName("State Validation")
    class StateValidation {

        @Test
        void isExpired_freshState_returnsFalse() {
            GuidedCreationState state = GuidedCreationState.initial("s1", "u1");

            assertThat(state.isExpired()).isFalse();
        }

        @Test
        void isExpired_oldState_returnsTrue() {
            GuidedCreationState state = GuidedCreationState.builder()
                    .phase(GuidedCreationState.Phase.INITIAL)
                    .sessionId("s1")
                    .userId("u1")
                    .createdAt(Instant.now().minusSeconds(31 * 60)) // 31 minutes ago
                    .build();

            assertThat(state.isExpired()).isTrue();
        }

        @Test
        void isActive_initialState_returnsTrue() {
            GuidedCreationState state = GuidedCreationState.initial("s1", "u1");

            assertThat(state.isActive()).isTrue();
        }

        @Test
        void isActive_submittedState_returnsFalse() {
            GuidedCreationState state = GuidedCreationState.initial("s1", "u1");
            state.markSubmitted("WO123");

            assertThat(state.isActive()).isFalse();
        }

        @Test
        void isActive_cancelledState_returnsFalse() {
            GuidedCreationState state = GuidedCreationState.initial("s1", "u1");
            state.markCancelled();

            assertThat(state.isActive()).isFalse();
        }

        @Test
        void isActive_gatheringFieldsState_returnsTrue() {
            DameeService service = DameeService.builder()
                    .serviceId("10101")
                    .nameEn("VPN Request")
                    .requiredFields(List.of("vpnType"))
                    .build();

            GuidedCreationState state = GuidedCreationState.initial("s1", "u1");
            state.startGatheringFields(service);

            assertThat(state.isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("Pending Action Management")
    class PendingActionManagement {

        @Test
        void setPendingActionId_storesId() {
            GuidedCreationState state = GuidedCreationState.initial("s1", "u1");
            state.setPendingActionId("action123");

            assertThat(state.getPendingActionId()).isEqualTo("action123");
        }

        @Test
        void setConfirmationToken_storesToken() {
            GuidedCreationState state = GuidedCreationState.initial("s1", "u1");
            state.setConfirmationToken("token123");

            assertThat(state.getConfirmationToken()).isEqualTo("token123");
        }
    }

    @Nested
    @DisplayName("Builder Pattern")
    class BuilderPattern {

        @Test
        void builder_createsCompleteState() {
            DameeService service = DameeService.builder()
                    .serviceId("10101")
                    .nameEn("Test Service")
                    .build();

            GuidedCreationState state = GuidedCreationState.builder()
                    .phase(GuidedCreationState.Phase.GATHERING_FIELDS)
                    .selectedService(service)
                    .currentFieldName("field1")
                    .currentFieldIndex(0)
                    .userId("u1")
                    .sessionId("s1")
                    .originalQuery("test query")
                    .pendingActionId("action123")
                    .build();

            assertThat(state.getPhase()).isEqualTo(GuidedCreationState.Phase.GATHERING_FIELDS);
            assertThat(state.getSelectedService()).isEqualTo(service);
            assertThat(state.getCurrentFieldName()).isEqualTo("field1");
            assertThat(state.getCurrentFieldIndex()).isEqualTo(0);
            assertThat(state.getUserId()).isEqualTo("u1");
            assertThat(state.getSessionId()).isEqualTo("s1");
            assertThat(state.getOriginalQuery()).isEqualTo("test query");
            assertThat(state.getPendingActionId()).isEqualTo("action123");
        }
    }
}
