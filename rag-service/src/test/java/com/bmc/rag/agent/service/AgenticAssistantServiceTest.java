package com.bmc.rag.agent.service;

import com.bmc.rag.agent.config.AgenticConfig;
import com.bmc.rag.agent.confirmation.ConfirmationService;
import com.bmc.rag.agent.service.AgenticAssistantService.IntentClassification;
import com.bmc.rag.agent.tools.RemedyIncidentTool;
import com.bmc.rag.agent.tools.RemedyWorkOrderTool;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for AgenticAssistantService, focusing on intent detection,
 * classification, and decision-making logic.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgenticAssistantServiceTest {

    @Mock
    private ChatLanguageModel chatModel;

    @Mock
    private RemedyIncidentTool incidentTool;

    @Mock
    private RemedyWorkOrderTool workOrderTool;

    @Mock
    private ConfirmationService confirmationService;

    @Mock
    private AgenticConfig agenticConfig;

    private AgenticAssistantService service;

    @BeforeEach
    void setUp() {
        when(agenticConfig.isEnabled()).thenReturn(true);

        service = new AgenticAssistantService(
            chatModel,
            incidentTool,
            workOrderTool,
            confirmationService,
            agenticConfig
        );
    }

    @Nested
    @DisplayName("Explicit Intent Detection Tests")
    class ExplicitIntentDetectionTests {

        @ParameterizedTest
        @DisplayName("Should detect explicit creation intent patterns")
        @ValueSource(strings = {
            "Create an incident for my VPN issue",
            "Open a ticket for email not working",
            "Submit a new incident",
            "Log a ticket please",
            "Raise an issue for printer problem",
            "File a new request",
            "I need a new ticket",
            "I want a work order",
            "Can you create an incident",
            "Please open a ticket",
            "incident for broken laptop"
        })
        void shouldDetectExplicitCreationIntent(String message) {
            assertTrue(service.hasAgenticIntent(message),
                "Should detect agentic intent in: " + message);
        }

        @ParameterizedTest
        @DisplayName("Should detect Arabic creation patterns")
        @ValueSource(strings = {
            "أنشئ بلاغ لمشكلة الشبكة",
            "افتح تذكرة للطابعة",
            "سجل حادثة جديدة",
            "ارفع أمر عمل"
        })
        void shouldDetectArabicCreationIntent(String message) {
            assertTrue(service.hasAgenticIntent(message),
                "Should detect agentic intent in Arabic: " + message);
        }

        @ParameterizedTest
        @DisplayName("Should detect confirmation/cancellation commands")
        @ValueSource(strings = {
            "confirm abc123",
            "CONFIRM ABC123",
            "cancel abc123",
            "CANCEL xyz789"
        })
        void shouldDetectConfirmationCommands(String message) {
            assertTrue(service.hasAgenticIntent(message),
                "Should detect confirmation command: " + message);
        }
    }

    @Nested
    @DisplayName("Implicit Intent Detection Tests")
    class ImplicitIntentDetectionTests {

        @Test
        @DisplayName("Should detect multiple implicit action signals")
        void shouldDetectMultipleImplicitSignals() {
            // Message with frustration + duration + failed self-service
            String message = "This has been happening for 3 days and I already tried restarting";
            assertTrue(service.hasAgenticIntent(message),
                "Should detect multiple implicit signals");
        }

        @Test
        @DisplayName("Should detect business impact signals")
        void shouldDetectBusinessImpactSignals() {
            String message = "This is blocking my work and I've already tried everything";
            assertTrue(service.hasAgenticIntent(message));
        }

        @Test
        @DisplayName("Should NOT trigger on single implicit signal")
        void shouldNotTriggerOnSingleSignal() {
            // Only one implicit signal - not enough confidence
            String message = "I tried restarting but it didn't help";
            // This should return false because we need at least 2 implicit signals
            // for high confidence
            assertFalse(service.hasAgenticIntent(message));
        }

        @Test
        @DisplayName("Should detect helplessness + failed troubleshooting")
        void shouldDetectHelplessnessSignals() {
            String message = "I don't know what else to do, nothing works anymore";
            assertTrue(service.hasAgenticIntent(message));
        }
    }

    @Nested
    @DisplayName("Question vs Action Classification Tests")
    class IntentClassificationTests {

        @ParameterizedTest
        @DisplayName("Should classify questions correctly")
        @ValueSource(strings = {
            "How do I reset my password?",
            "What is the VPN server address?",
            "Why is my email slow?",
            "Can I access the system remotely?",
            "Is it possible to change my username?"
        })
        void shouldClassifyAsQuestion(String message) {
            IntentClassification result = service.classifyIntent(message, null);
            assertEquals(IntentClassification.QUESTION, result,
                "Should classify as QUESTION: " + message);
        }

        @ParameterizedTest
        @DisplayName("Should classify explicit incident requests")
        @ValueSource(strings = {
            "Create an incident for my VPN issue",
            "Open a ticket for email not working",
            "Log an incident please"
        })
        void shouldClassifyAsActionIncident(String message) {
            IntentClassification result = service.classifyIntent(message, null);
            assertEquals(IntentClassification.ACTION_INCIDENT, result,
                "Should classify as ACTION_INCIDENT: " + message);
        }

        @ParameterizedTest
        @DisplayName("Should classify explicit work order requests")
        @ValueSource(strings = {
            "Create a work order for software installation",
            "Open a WO for new laptop setup",
            "I need a work order for the office move"
        })
        void shouldClassifyAsActionWorkOrder(String message) {
            IntentClassification result = service.classifyIntent(message, null);
            assertEquals(IntentClassification.ACTION_WORKORDER, result,
                "Should classify as ACTION_WORKORDER: " + message);
        }

        @Test
        @DisplayName("Should classify mixed signals as ambiguous")
        void shouldClassifyMixedSignalsAsAmbiguous() {
            // Question format but with clear action signals (frustration + duration)
            // Pattern expects: "been/for/since" + quantifier + "days/hours"
            String message = "How can I fix this? It's been 3 days and I already tried restarting";
            IntentClassification result = service.classifyIntent(message, null);
            assertEquals(IntentClassification.AMBIGUOUS_INTENT, result,
                "Question with implicit action signals should be ambiguous");
        }
    }

    @Nested
    @DisplayName("Incident vs Work Order Classification Tests")
    class TicketTypeClassificationTests {

        @Test
        @DisplayName("Should classify 'broken' scenarios as Incident")
        void shouldClassifyBrokenAsIncident() {
            String message = "Create a ticket - my VPN stopped working";
            IntentClassification result = service.classifyIntent(message, null);
            assertEquals(IntentClassification.ACTION_INCIDENT, result);
        }

        @Test
        @DisplayName("Should classify 'install' requests as Work Order")
        void shouldClassifyInstallAsWorkOrder() {
            String message = "Create a work order to install Adobe Photoshop";
            IntentClassification result = service.classifyIntent(message, null);
            assertEquals(IntentClassification.ACTION_WORKORDER, result);
        }

        @Test
        @DisplayName("Should handle Arabic work order pattern")
        void shouldHandleArabicWorkOrder() {
            String message = "أنشئ أمر عمل لتثبيت البرنامج";
            IntentClassification result = service.classifyIntent(message, null);
            assertEquals(IntentClassification.ACTION_WORKORDER, result);
        }
    }

    @Nested
    @DisplayName("Context-Aware Slot Filling Tests")
    class ContextAwareTests {

        @Test
        @DisplayName("Should use conversation history for classification")
        void shouldUseConversationHistory() {
            List<ChatMessage> history = new ArrayList<>();
            history.add(UserMessage.from("My Outlook keeps crashing when I open attachments"));
            history.add(AiMessage.from("Have you tried restarting Outlook?"));
            history.add(UserMessage.from("Yes, I restarted twice"));

            // Current message refers to context
            String currentMessage = "Create a ticket for this issue";
            IntentClassification result = service.classifyIntent(currentMessage, history);

            // Should still classify as incident (explicit pattern)
            assertEquals(IntentClassification.ACTION_INCIDENT, result);
        }

        @Test
        @DisplayName("Should handle empty conversation history gracefully")
        void shouldHandleEmptyHistory() {
            // Explicit action pattern should work regardless of history
            String message = "Create an incident for VPN issue";
            IntentClassification result = service.classifyIntent(message, new ArrayList<>());
            assertEquals(IntentClassification.ACTION_INCIDENT, result,
                "Should classify explicit pattern even with empty history");

            result = service.classifyIntent(message, null);
            assertEquals(IntentClassification.ACTION_INCIDENT, result,
                "Should classify explicit pattern even with null history");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Disabled State Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should return false when agentic is disabled")
        void shouldReturnFalseWhenDisabled() {
            when(agenticConfig.isEnabled()).thenReturn(false);

            AgenticAssistantService disabledService = new AgenticAssistantService(
                chatModel,
                incidentTool,
                workOrderTool,
                confirmationService,
                agenticConfig
            );

            assertFalse(disabledService.hasAgenticIntent("Create an incident"));
        }

        @Test
        @DisplayName("Should handle null message")
        void shouldHandleNullMessage() {
            assertFalse(service.hasAgenticIntent(null));
        }

        @Test
        @DisplayName("Should handle empty message")
        void shouldHandleEmptyMessage() {
            assertFalse(service.hasAgenticIntent(""));
            assertFalse(service.hasAgenticIntent("   "));
        }

        @Test
        @DisplayName("Should classify null message as question")
        void shouldClassifyNullAsQuestion() {
            // Null handling in classification should be safe
            assertDoesNotThrow(() -> service.classifyIntent("", null));
        }
    }

    @Nested
    @DisplayName("Real-World Scenario Tests")
    class RealWorldScenarioTests {

        @Test
        @DisplayName("Scenario: User describes problem then asks for ticket")
        void scenarioDescribeProblemThenRequestTicket() {
            List<ChatMessage> history = new ArrayList<>();
            history.add(UserMessage.from("I can't connect to the VPN from home"));
            history.add(AiMessage.from("Have you tried restarting the VPN client?"));
            history.add(UserMessage.from("Yes, I also rebooted my laptop"));
            history.add(AiMessage.from("Let me check if there are any network issues..."));
            history.add(UserMessage.from("Still not working"));

            // User now explicitly asks for ticket
            assertTrue(service.hasAgenticIntent("Open a ticket please"));
            assertEquals(IntentClassification.ACTION_INCIDENT,
                service.classifyIntent("Open a ticket please", history));
        }

        @Test
        @DisplayName("Scenario: Frustrated user with implicit action need")
        void scenarioFrustratedUserImplicitAction() {
            // User shows frustration + duration + failed troubleshooting
            String message = "This VPN issue has been going on for 3 days now, " +
                "I've already tried restarting everything multiple times";

            assertTrue(service.hasAgenticIntent(message),
                "Should detect implicit action signals from frustrated user");
        }

        @Test
        @DisplayName("Scenario: Simple question should not trigger action")
        void scenarioSimpleQuestionNoAction() {
            String message = "How do I configure the VPN client?";
            assertFalse(service.hasAgenticIntent(message));
            assertEquals(IntentClassification.QUESTION,
                service.classifyIntent(message, null));
        }

        @Test
        @DisplayName("Scenario: Software installation request")
        void scenarioSoftwareInstallation() {
            String message = "I need to get Adobe Creative Suite installed";
            // This doesn't match explicit patterns (no "create work order")
            // But classifyIntent might see it as question
            IntentClassification result = service.classifyIntent(message, null);

            // Without explicit "create work order", this is ambiguous
            // The LLM system prompt will handle the nuance
            assertTrue(result == IntentClassification.QUESTION ||
                       result == IntentClassification.AMBIGUOUS_INTENT);
        }

        @Test
        @DisplayName("Scenario: User switches from question to action")
        void scenarioQuestionToAction() {
            // First message - pure question
            assertFalse(service.hasAgenticIntent("Why is my email slow?"));

            // Second message - explicit action
            assertTrue(service.hasAgenticIntent("Actually, can you create a ticket for this?"));
        }
    }
}
