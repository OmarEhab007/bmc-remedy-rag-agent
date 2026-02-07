package com.bmc.rag.agent.prompt;

import com.bmc.rag.agent.retrieval.SecureContentRetriever.UserContext;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AgenticSystemPrompt to verify the enhanced reasoning
 * capabilities are correctly included in the prompt.
 */
class AgenticSystemPromptTest {

    @Nested
    @DisplayName("Prompt Structure Tests")
    class PromptStructureTests {

        @Test
        @DisplayName("Should include all reasoning sections in prompt")
        void shouldIncludeAllReasoningSections() {
            String prompt = AgenticSystemPrompt.build("test message", null, null);

            // Verify core sections exist
            assertTrue(prompt.contains("INTENT CLASSIFICATION"),
                "Should include intent classification section");
            assertTrue(prompt.contains("TICKET TYPE CLASSIFICATION"),
                "Should include ticket type classification section");
            assertTrue(prompt.contains("SLOT FILLING"),
                "Should include slot filling section");
            assertTrue(prompt.contains("CONTEXT EXTRACTION"),
                "Should include context extraction section");
            assertTrue(prompt.contains("TOOL USAGE RULES"),
                "Should include tool usage rules");
            assertTrue(prompt.contains("OUTPUT RULES"),
                "Should include output rules");
        }

        @Test
        @DisplayName("Should include INCIDENT classification criteria")
        void shouldIncludeIncidentCriteria() {
            String prompt = AgenticSystemPrompt.build("test", null, null);

            assertTrue(prompt.contains("INCIDENT (Break/Fix)"));
            assertTrue(prompt.contains("BROKEN"));
            assertTrue(prompt.contains("stopped working") || prompt.contains("error"));
        }

        @Test
        @DisplayName("Should include WORK ORDER classification criteria")
        void shouldIncludeWorkOrderCriteria() {
            String prompt = AgenticSystemPrompt.build("test", null, null);

            assertTrue(prompt.contains("WORK ORDER"));
            assertTrue(prompt.contains("Service Request") || prompt.contains("NEW"));
            assertTrue(prompt.contains("install") || prompt.contains("setup"));
        }

        @Test
        @DisplayName("Should include slot filling guidance for Impact/Urgency")
        void shouldIncludeSlotFillingGuidance() {
            String prompt = AgenticSystemPrompt.build("test", null, null);

            // Impact levels
            assertTrue(prompt.contains("Impact 1") || prompt.contains("Extensive"));
            assertTrue(prompt.contains("Impact 3") || prompt.contains("Moderate"));

            // Urgency levels
            assertTrue(prompt.contains("Urgency 1") || prompt.contains("Critical"));
            assertTrue(prompt.contains("Urgency 3") || prompt.contains("Medium"));
        }

        @Test
        @DisplayName("Should include inference decision matrix")
        void shouldIncludeInferenceMatrix() {
            String prompt = AgenticSystemPrompt.build("test", null, null);

            assertTrue(prompt.contains("blocking my work") ||
                       prompt.contains("Inference") ||
                       prompt.contains("Signal"));
        }
    }

    @Nested
    @DisplayName("User Context Integration Tests")
    class UserContextTests {

        @Test
        @DisplayName("Should include user ID when provided")
        void shouldIncludeUserId() {
            UserContext userContext = new UserContext("john.doe", null);
            String prompt = AgenticSystemPrompt.build("test", userContext, null);

            assertTrue(prompt.contains("john.doe"),
                "Should include user ID in prompt");
        }

        @Test
        @DisplayName("Should include user groups when provided")
        void shouldIncludeUserGroups() {
            UserContext userContext = new UserContext("john.doe",
                Set.of("IT-Support", "Network-Team"));
            String prompt = AgenticSystemPrompt.build("test", userContext, null);

            assertTrue(prompt.contains("john.doe"));
            assertTrue(prompt.contains("IT-Support") || prompt.contains("Network-Team"));
        }

        @Test
        @DisplayName("Should handle null user context gracefully")
        void shouldHandleNullUserContext() {
            String prompt = AgenticSystemPrompt.build("test", null, null);

            assertNotNull(prompt);
            assertTrue(prompt.length() > 0);
            // Should not throw exception
        }
    }

    @Nested
    @DisplayName("Conversation History Integration Tests")
    class ConversationHistoryTests {

        @Test
        @DisplayName("Should include conversation history section")
        void shouldIncludeConversationHistory() {
            List<ChatMessage> history = new ArrayList<>();
            history.add(UserMessage.from("My VPN is not working"));
            history.add(AiMessage.from("Have you tried restarting?"));

            String prompt = AgenticSystemPrompt.build("Create a ticket", null, history);

            assertTrue(prompt.contains("CONVERSATION HISTORY"),
                "Should include conversation history section");
            assertTrue(prompt.contains("My VPN is not working"),
                "Should include user messages from history");
            assertTrue(prompt.contains("Have you tried restarting"),
                "Should include assistant messages from history");
        }

        @Test
        @DisplayName("Should truncate long AI messages in history")
        void shouldTruncateLongAiMessages() {
            List<ChatMessage> history = new ArrayList<>();
            history.add(UserMessage.from("Short question"));
            // Create a very long AI response (> 500 chars)
            String longResponse = "A".repeat(600);
            history.add(AiMessage.from(longResponse));

            String prompt = AgenticSystemPrompt.build("test", null, history);

            // Should not contain the full 600 chars
            assertFalse(prompt.contains("A".repeat(600)),
                "Should truncate long AI messages");
            // Should contain truncation indicator
            assertTrue(prompt.contains("...") || prompt.contains("A".repeat(400)),
                "Should indicate truncation or show truncated content");
        }

        @Test
        @DisplayName("Should handle empty conversation history")
        void shouldHandleEmptyHistory() {
            String prompt = AgenticSystemPrompt.build("test", null, new ArrayList<>());

            assertNotNull(prompt);
            assertTrue(prompt.contains("CONVERSATION HISTORY"));
            assertTrue(prompt.contains("No prior messages") ||
                       prompt.contains("test")); // Current message should be there
        }

        @Test
        @DisplayName("Should handle null conversation history")
        void shouldHandleNullHistory() {
            String prompt = AgenticSystemPrompt.build("test", null, null);

            assertNotNull(prompt);
            // Should not throw exception
        }
    }

    @Nested
    @DisplayName("User Message Integration Tests")
    class UserMessageTests {

        @Test
        @DisplayName("Should include current user request")
        void shouldIncludeCurrentUserRequest() {
            String userMessage = "Please create an incident for my printer";
            String prompt = AgenticSystemPrompt.build(userMessage, null, null);

            assertTrue(prompt.contains("CURRENT USER REQUEST") ||
                       prompt.contains(userMessage),
                "Should include the current user request");
        }

        @Test
        @DisplayName("Should position user request at end of prompt")
        void shouldPositionUserRequestAtEnd() {
            String userMessage = "Create a ticket for VPN issue";
            String prompt = AgenticSystemPrompt.build(userMessage, null, null);

            // User message should appear after all the instruction sections
            int userMsgPosition = prompt.lastIndexOf(userMessage);
            int outputRulesPosition = prompt.indexOf("OUTPUT RULES");

            assertTrue(userMsgPosition > outputRulesPosition,
                "User message should appear after instruction sections");
        }
    }

    @Nested
    @DisplayName("Intent Classification Prompt Tests")
    class IntentClassificationPromptTests {

        @Test
        @DisplayName("Should build valid intent classification prompt")
        void shouldBuildIntentClassificationPrompt() {
            String prompt = AgenticSystemPrompt.buildIntentClassificationPrompt(
                "Create a ticket", null);

            assertNotNull(prompt);
            assertTrue(prompt.contains("QUESTION"));
            assertTrue(prompt.contains("ACTION_INCIDENT"));
            assertTrue(prompt.contains("ACTION_WORKORDER"));
            assertTrue(prompt.contains("AMBIGUOUS"));
        }

        @Test
        @DisplayName("Should include recent conversation in classification prompt")
        void shouldIncludeRecentConversationInClassificationPrompt() {
            List<ChatMessage> history = new ArrayList<>();
            history.add(UserMessage.from("VPN issue"));
            history.add(UserMessage.from("Still not working"));

            String prompt = AgenticSystemPrompt.buildIntentClassificationPrompt(
                "Please help", history);

            assertTrue(prompt.contains("VPN issue") ||
                       prompt.contains("Recent conversation"));
        }

        @Test
        @DisplayName("Should limit conversation history in classification prompt")
        void shouldLimitHistoryInClassificationPrompt() {
            List<ChatMessage> history = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                history.add(UserMessage.from("Message " + i));
            }

            String prompt = AgenticSystemPrompt.buildIntentClassificationPrompt(
                "Current message", history);

            // Should not include all 20 messages (limit is 6)
            assertFalse(prompt.contains("Message 0") && prompt.contains("Message 19"),
                "Should limit history in classification prompt");
        }
    }

    @Nested
    @DisplayName("Prompt Quality Tests")
    class PromptQualityTests {

        @Test
        @DisplayName("Should have reasonable prompt length")
        void shouldHaveReasonableLength() {
            String prompt = AgenticSystemPrompt.build("test", null, null);

            // Prompt should be substantial but not excessive
            assertTrue(prompt.length() > 1000,
                "Prompt should be substantial (> 1000 chars)");
            assertTrue(prompt.length() < 50000,
                "Prompt should not be excessive (< 50000 chars)");
        }

        @Test
        @DisplayName("Should include actionable instructions")
        void shouldIncludeActionableInstructions() {
            String prompt = AgenticSystemPrompt.build("test", null, null);

            // Should have clear action verbs
            assertTrue(prompt.contains("MUST") || prompt.contains("must"),
                "Should include mandatory instructions");
            assertTrue(prompt.contains("Use") || prompt.contains("use"),
                "Should include usage instructions");
        }

        @Test
        @DisplayName("Should include tool names")
        void shouldIncludeToolNames() {
            String prompt = AgenticSystemPrompt.build("test", null, null);

            assertTrue(prompt.contains("searchSimilarIncidents") ||
                       prompt.contains("stageIncidentCreation"),
                "Should mention available tools");
        }
    }
}
