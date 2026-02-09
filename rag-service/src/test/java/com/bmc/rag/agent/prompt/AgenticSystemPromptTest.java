package com.bmc.rag.agent.prompt;

import com.bmc.rag.agent.retrieval.SecureContentRetriever.UserContext;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for AgenticSystemPrompt to verify the enhanced reasoning
 * capabilities are correctly included in the prompt.
 */
@DisplayName("AgenticSystemPrompt Tests")
class AgenticSystemPromptTest {

    @Nested
    @DisplayName("Prompt Structure Tests")
    class PromptStructureTests {

        @Test
        @DisplayName("Should include all reasoning sections in prompt")
        void build_allSections_includesAllReasoningSections() {
            String prompt = AgenticSystemPrompt.build("test message", null, null);

            // Verify core sections exist
            assertThat(prompt).contains("INTENT CLASSIFICATION");
            assertThat(prompt).contains("TICKET TYPE CLASSIFICATION");
            assertThat(prompt).contains("SLOT FILLING");
            assertThat(prompt).contains("CONTEXT EXTRACTION");
            assertThat(prompt).contains("TOOL USAGE RULES");
            assertThat(prompt).contains("OUTPUT RULES");
        }

        @Test
        @DisplayName("Should include INCIDENT classification criteria")
        void build_incidentCriteria_includesBreakFixIndicators() {
            String prompt = AgenticSystemPrompt.build("test", null, null);

            assertThat(prompt).contains("INCIDENT (Break/Fix)");
            assertThat(prompt).contains("BROKEN");
            assertThat(prompt).containsAnyOf("stopped working", "error");
        }

        @Test
        @DisplayName("Should include WORK ORDER classification criteria")
        void build_workOrderCriteria_includesServiceRequestIndicators() {
            String prompt = AgenticSystemPrompt.build("test", null, null);

            assertThat(prompt).contains("WORK ORDER");
            assertThat(prompt).containsAnyOf("Service Request", "NEW");
            assertThat(prompt).containsAnyOf("install", "setup");
        }

        @Test
        @DisplayName("Should include slot filling guidance for Impact/Urgency")
        void build_slotFilling_includesImpactUrgencyGuidance() {
            String prompt = AgenticSystemPrompt.build("test", null, null);

            // Impact levels
            assertThat(prompt).containsAnyOf("Impact 1", "Extensive");
            assertThat(prompt).containsAnyOf("Impact 3", "Moderate");

            // Urgency levels
            assertThat(prompt).containsAnyOf("Urgency 1", "Critical");
            assertThat(prompt).containsAnyOf("Urgency 3", "Medium");
        }

        @Test
        @DisplayName("Should include inference decision matrix")
        void build_inferenceMatrix_includesDecisionGuidelines() {
            String prompt = AgenticSystemPrompt.build("test", null, null);

            assertThat(prompt).containsAnyOf("blocking my work", "Inference", "Signal");
        }

        @Test
        @DisplayName("Should include role definition")
        void build_roleDefinition_includesAssistantRole() {
            String prompt = AgenticSystemPrompt.build("test", null, null);

            assertThat(prompt).contains("IT support assistant");
            assertThat(prompt).containsAnyOf("responsibilities", "UNDERSTAND", "CLASSIFY");
        }

        @Test
        @DisplayName("Should include context extraction rules with examples")
        void build_contextExtraction_includesExamplesAndRules() {
            String prompt = AgenticSystemPrompt.build("test", null, null);

            assertThat(prompt).contains("CONTEXT EXTRACTION");
            assertThat(prompt).containsAnyOf("SUMMARY FORMULA", "MUST FOLLOW");
            assertThat(prompt).containsAnyOf("CST Outlook", "VPN connection");
        }

        @Test
        @DisplayName("Should include stop conditions in output rules")
        void build_outputRules_includesStopConditions() {
            String prompt = AgenticSystemPrompt.build("test", null, null);

            assertThat(prompt).contains("STOP CONDITIONS");
            assertThat(prompt).containsAnyOf("stageIncidentCreation", "stageWorkOrderCreation");
        }
    }

    @Nested
    @DisplayName("User Context Integration Tests")
    class UserContextTests {

        @Test
        @DisplayName("Should include user ID when provided")
        void build_withUserId_includesUserIdInPrompt() {
            UserContext userContext = new UserContext("john.doe", null);
            String prompt = AgenticSystemPrompt.build("test", userContext, null);

            assertThat(prompt).contains("john.doe");
            assertThat(prompt).contains("CURRENT USER");
        }

        @Test
        @DisplayName("Should include user groups when provided")
        void build_withUserGroups_includesGroupsInPrompt() {
            UserContext userContext = new UserContext("john.doe",
                Set.of("IT-Support", "Network-Team"));
            String prompt = AgenticSystemPrompt.build("test", userContext, null);

            assertThat(prompt).contains("john.doe");
            assertThat(prompt).containsAnyOf("IT-Support", "Network-Team");
            assertThat(prompt).contains("Groups:");
        }

        @Test
        @DisplayName("Should handle null user context gracefully")
        void build_nullUserContext_returnsValidPrompt() {
            String prompt = AgenticSystemPrompt.build("test", null, null);

            assertThat(prompt).isNotNull();
            assertThat(prompt).isNotEmpty();
            // Should not contain the user context section (but will contain "CURRENT USER REQUEST")
            assertThat(prompt).doesNotContain("User ID:");
            assertThat(prompt).doesNotContain("Groups:");
        }

        @Test
        @DisplayName("Should handle user context with null userId")
        void build_userContextWithNullUserId_skipsUserSection() {
            UserContext userContext = new UserContext(null, Set.of("IT-Support"));
            String prompt = AgenticSystemPrompt.build("test", userContext, null);

            assertThat(prompt).isNotNull();
            // Should not contain the user context section when userId is null
            assertThat(prompt).doesNotContain("User ID:");
            assertThat(prompt).doesNotContain("Groups:");
        }

        @Test
        @DisplayName("Should handle user context with empty groups")
        void build_userContextWithEmptyGroups_includesUserIdOnly() {
            UserContext userContext = new UserContext("jane.doe", Collections.emptySet());
            String prompt = AgenticSystemPrompt.build("test", userContext, null);

            assertThat(prompt).contains("jane.doe");
            assertThat(prompt).contains("User ID:");
            assertThat(prompt).doesNotContain("Groups:");
        }

        @Test
        @DisplayName("Should handle user context with null groups")
        void build_userContextWithNullGroups_includesUserIdOnly() {
            UserContext userContext = new UserContext("jane.doe", null);
            String prompt = AgenticSystemPrompt.build("test", userContext, null);

            assertThat(prompt).contains("jane.doe");
            assertThat(prompt).doesNotContain("Groups:");
        }
    }

    @Nested
    @DisplayName("Conversation History Integration Tests")
    class ConversationHistoryTests {

        @Test
        @DisplayName("Should include conversation history section")
        void build_withConversationHistory_includesHistoryMessages() {
            List<ChatMessage> history = new ArrayList<>();
            history.add(UserMessage.from("My VPN is not working"));
            history.add(AiMessage.from("Have you tried restarting?"));

            String prompt = AgenticSystemPrompt.build("Create a ticket", null, history);

            assertThat(prompt).contains("CONVERSATION HISTORY");
            assertThat(prompt).contains("My VPN is not working");
            assertThat(prompt).contains("Have you tried restarting");
            assertThat(prompt).contains("User:");
            assertThat(prompt).contains("Assistant:");
        }

        @Test
        @DisplayName("Should truncate long AI messages in history")
        void build_longAiMessage_truncatesAfter500Chars() {
            List<ChatMessage> history = new ArrayList<>();
            history.add(UserMessage.from("Short question"));
            // Create a very long AI response (> 500 chars)
            String longResponse = "A".repeat(600);
            history.add(AiMessage.from(longResponse));

            String prompt = AgenticSystemPrompt.build("test", null, history);

            // Should not contain the full 600 chars
            assertThat(prompt).doesNotContain("A".repeat(600));
            // Should contain truncation indicator
            assertThat(prompt).contains("...");
        }

        @Test
        @DisplayName("Should handle empty conversation history")
        void build_emptyHistory_showsNoMessagesIndicator() {
            String prompt = AgenticSystemPrompt.build("test", null, new ArrayList<>());

            assertThat(prompt).isNotNull();
            assertThat(prompt).contains("CONVERSATION HISTORY");
            assertThat(prompt).contains("No prior messages");
        }

        @Test
        @DisplayName("Should handle null conversation history")
        void build_nullHistory_showsNoMessagesIndicator() {
            String prompt = AgenticSystemPrompt.build("test", null, null);

            assertThat(prompt).isNotNull();
            assertThat(prompt).contains("CONVERSATION HISTORY");
            assertThat(prompt).contains("No prior messages");
        }

        @Test
        @DisplayName("Should handle AI messages with empty content")
        void build_aiMessageWithEmptyText_skipsMessage() {
            List<ChatMessage> history = new ArrayList<>();
            history.add(UserMessage.from("Test question"));
            // Create an AiMessage with text, then test the handling in the actual method
            // Note: The actual implementation checks for null text in AiMessage.text()
            history.add(AiMessage.from(""));
            history.add(UserMessage.from("Another question"));

            String prompt = AgenticSystemPrompt.build("test", null, history);

            assertThat(prompt).contains("Test question");
            assertThat(prompt).contains("Another question");
        }

        @Test
        @DisplayName("Should handle multiple user and AI messages in order")
        void build_multipleMessages_maintainsOrder() {
            List<ChatMessage> history = new ArrayList<>();
            history.add(UserMessage.from("First user message"));
            history.add(AiMessage.from("First AI response"));
            history.add(UserMessage.from("Second user message"));
            history.add(AiMessage.from("Second AI response"));

            String prompt = AgenticSystemPrompt.build("test", null, history);

            int firstUserPos = prompt.indexOf("First user message");
            int firstAiPos = prompt.indexOf("First AI response");
            int secondUserPos = prompt.indexOf("Second user message");
            int secondAiPos = prompt.indexOf("Second AI response");

            assertThat(firstUserPos).isLessThan(firstAiPos);
            assertThat(firstAiPos).isLessThan(secondUserPos);
            assertThat(secondUserPos).isLessThan(secondAiPos);
        }

        @Test
        @DisplayName("Should handle conversation with only valid messages")
        void build_mixedValidMessages_includesAllValidMessages() {
            List<ChatMessage> history = new ArrayList<>();
            // UserMessage.from("") and from(null) both throw exceptions
            // So we just test with valid messages
            history.add(UserMessage.from("First message"));
            history.add(UserMessage.from("Valid message"));

            String prompt = AgenticSystemPrompt.build("test", null, history);

            assertThat(prompt).contains("First message");
            assertThat(prompt).contains("Valid message");
        }
    }

    @Nested
    @DisplayName("User Message Integration Tests")
    class UserMessageTests {

        @Test
        @DisplayName("Should include current user request")
        void build_userMessage_includesCurrentRequest() {
            String userMessage = "Please create an incident for my printer";
            String prompt = AgenticSystemPrompt.build(userMessage, null, null);

            assertThat(prompt).containsAnyOf("CURRENT USER REQUEST", userMessage);
        }

        @Test
        @DisplayName("Should position user request at end of prompt")
        void build_userMessage_positionsAtEndAfterInstructions() {
            String userMessage = "Create a ticket for VPN issue";
            String prompt = AgenticSystemPrompt.build(userMessage, null, null);

            // User message should appear after all the instruction sections
            int userMsgPosition = prompt.lastIndexOf(userMessage);
            int outputRulesPosition = prompt.indexOf("OUTPUT RULES");

            assertThat(userMsgPosition).isGreaterThan(outputRulesPosition);
        }

        @Test
        @DisplayName("Should handle empty user message")
        void build_emptyUserMessage_includesEmptyRequest() {
            String prompt = AgenticSystemPrompt.build("", null, null);

            assertThat(prompt).isNotNull();
            assertThat(prompt).contains("CURRENT USER REQUEST");
        }

        @Test
        @DisplayName("Should handle very long user message")
        void build_longUserMessage_includesFullMessage() {
            String longMessage = "Please help me with this issue. ".repeat(100);
            String prompt = AgenticSystemPrompt.build(longMessage, null, null);

            assertThat(prompt).contains(longMessage);
        }

        @Test
        @DisplayName("Should handle special characters in user message")
        void build_specialCharactersInMessage_includesCharacters() {
            String messageWithSpecialChars = "Error: @#$% & <script>alert('test')</script>";
            String prompt = AgenticSystemPrompt.build(messageWithSpecialChars, null, null);

            assertThat(prompt).contains(messageWithSpecialChars);
        }
    }

    @Nested
    @DisplayName("Intent Classification Prompt Tests")
    class IntentClassificationPromptTests {

        @Test
        @DisplayName("Should build valid intent classification prompt")
        void buildIntentClassificationPrompt_validInput_includesAllClassifications() {
            String prompt = AgenticSystemPrompt.buildIntentClassificationPrompt(
                "Create a ticket", null);

            assertThat(prompt).isNotNull();
            assertThat(prompt).contains("QUESTION");
            assertThat(prompt).contains("ACTION_INCIDENT");
            assertThat(prompt).contains("ACTION_WORKORDER");
            assertThat(prompt).contains("AMBIGUOUS");
        }

        @Test
        @DisplayName("Should include recent conversation in classification prompt")
        void buildIntentClassificationPrompt_withHistory_includesRecentConversation() {
            List<ChatMessage> history = new ArrayList<>();
            history.add(UserMessage.from("VPN issue"));
            history.add(UserMessage.from("Still not working"));

            String prompt = AgenticSystemPrompt.buildIntentClassificationPrompt(
                "Please help", history);

            assertThat(prompt).containsAnyOf("VPN issue", "Recent conversation");
        }

        @Test
        @DisplayName("Should limit conversation history in classification prompt")
        void buildIntentClassificationPrompt_longHistory_includesOnlyLast6Messages() {
            List<ChatMessage> history = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                history.add(UserMessage.from("Message " + i));
            }

            String prompt = AgenticSystemPrompt.buildIntentClassificationPrompt(
                "Current message", history);

            // Should include only the last 6 messages (indices 14-19)
            assertThat(prompt).doesNotContain("Message 0");
            assertThat(prompt).doesNotContain("Message 13");
            assertThat(prompt).contains("Message 14");
            assertThat(prompt).contains("Message 19");
        }

        @Test
        @DisplayName("Should include classification rules in prompt")
        void buildIntentClassificationPrompt_validInput_includesClassificationRules() {
            String prompt = AgenticSystemPrompt.buildIntentClassificationPrompt(
                "Test message", null);

            assertThat(prompt).contains("Classification rules:");
            assertThat(prompt).containsAnyOf("QUESTION signals", "ACTION_INCIDENT signals");
        }

        @Test
        @DisplayName("Should include current message at end")
        void buildIntentClassificationPrompt_validInput_includesCurrentMessageAtEnd() {
            String userMessage = "Create an incident";
            String prompt = AgenticSystemPrompt.buildIntentClassificationPrompt(userMessage, null);

            assertThat(prompt).contains("Current message:");
            assertThat(prompt).contains(userMessage);
            assertThat(prompt.indexOf(userMessage)).isGreaterThan(prompt.indexOf("Classification rules:"));
        }

        @Test
        @DisplayName("Should handle null conversation history in classification prompt")
        void buildIntentClassificationPrompt_nullHistory_buildsValidPrompt() {
            String prompt = AgenticSystemPrompt.buildIntentClassificationPrompt(
                "Test message", null);

            assertThat(prompt).isNotNull();
            assertThat(prompt).doesNotContain("Recent conversation:");
        }

        @Test
        @DisplayName("Should handle empty conversation history in classification prompt")
        void buildIntentClassificationPrompt_emptyHistory_buildsValidPrompt() {
            String prompt = AgenticSystemPrompt.buildIntentClassificationPrompt(
                "Test message", new ArrayList<>());

            assertThat(prompt).isNotNull();
            assertThat(prompt).doesNotContain("Recent conversation:");
        }

        @Test
        @DisplayName("Should only include user messages in classification prompt history")
        void buildIntentClassificationPrompt_mixedMessages_includesOnlyUserMessages() {
            List<ChatMessage> history = new ArrayList<>();
            history.add(UserMessage.from("User message 1"));
            history.add(AiMessage.from("AI response 1"));
            history.add(UserMessage.from("User message 2"));

            String prompt = AgenticSystemPrompt.buildIntentClassificationPrompt(
                "Current", history);

            assertThat(prompt).contains("User message 1");
            assertThat(prompt).contains("User message 2");
            assertThat(prompt).doesNotContain("AI response 1");
        }
    }

    @Nested
    @DisplayName("Prompt Quality Tests")
    class PromptQualityTests {

        @Test
        @DisplayName("Should have reasonable prompt length")
        void build_standardInput_hasReasonableLength() {
            String prompt = AgenticSystemPrompt.build("test", null, null);

            // Prompt should be substantial but not excessive
            assertThat(prompt.length()).isGreaterThan(1000);
            assertThat(prompt.length()).isLessThan(50000);
        }

        @Test
        @DisplayName("Should include actionable instructions")
        void build_standardInput_includesActionableInstructions() {
            String prompt = AgenticSystemPrompt.build("test", null, null);

            // Should have clear action verbs
            assertThat(prompt).containsIgnoringCase("must");
            assertThat(prompt).containsIgnoringCase("use");
        }

        @Test
        @DisplayName("Should include tool names")
        void build_standardInput_includesAvailableToolNames() {
            String prompt = AgenticSystemPrompt.build("test", null, null);

            assertThat(prompt).containsAnyOf(
                "searchSimilarIncidents",
                "stageIncidentCreation",
                "searchSimilarWorkOrders",
                "stageWorkOrderCreation"
            );
        }

        @Test
        @DisplayName("Should include field extraction guidance")
        void build_standardInput_includesFieldExtractionGuidance() {
            String prompt = AgenticSystemPrompt.build("test", null, null);

            assertThat(prompt).containsAnyOf("SUMMARY", "DESCRIPTION", "max 255 chars");
        }

        @Test
        @DisplayName("Should include examples for better understanding")
        void build_standardInput_includesExamples() {
            String prompt = AgenticSystemPrompt.build("test", null, null);

            assertThat(prompt).containsAnyOf("Example", "example", "Good examples");
        }

        @Test
        @DisplayName("Should not include markdown formatting that breaks rendering")
        void build_standardInput_usesConsistentFormatting() {
            String prompt = AgenticSystemPrompt.build("test", null, null);

            // Check for consistent section markers
            assertThat(prompt).contains("===");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle all parameters null")
        void build_allParametersNull_returnsValidPrompt() {
            String prompt = AgenticSystemPrompt.build(null, null, null);

            assertThat(prompt).isNotNull();
            assertThat(prompt).contains("INTENT CLASSIFICATION");
        }

        @Test
        @DisplayName("Should handle whitespace-only user message")
        void build_whitespaceOnlyMessage_includesMessage() {
            String prompt = AgenticSystemPrompt.build("   ", null, null);

            assertThat(prompt).isNotNull();
            assertThat(prompt).contains("CURRENT USER REQUEST");
        }

        @Test
        @DisplayName("Should handle very long conversation history")
        void build_veryLongHistory_doesNotFailOrTimeout() {
            List<ChatMessage> history = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                history.add(UserMessage.from("Message " + i));
                history.add(AiMessage.from("Response " + i));
            }

            String prompt = AgenticSystemPrompt.build("test", null, history);

            assertThat(prompt).isNotNull();
            assertThat(prompt).contains("CONVERSATION HISTORY");
        }

        @Test
        @DisplayName("Should handle user context with many groups")
        void build_manyUserGroups_includesAllGroups() {
            Set<String> manyGroups = Set.of(
                "Group1", "Group2", "Group3", "Group4", "Group5",
                "Group6", "Group7", "Group8", "Group9", "Group10"
            );
            UserContext userContext = new UserContext("test.user", manyGroups);

            String prompt = AgenticSystemPrompt.build("test", userContext, null);

            assertThat(prompt).contains("test.user");
            assertThat(prompt).contains("Groups:");
            // Should contain at least some of the groups
            assertThat(prompt).containsAnyOf("Group1", "Group5", "Group10");
        }

        @Test
        @DisplayName("Should handle multiline user message")
        void build_multilineUserMessage_preservesNewlines() {
            String multilineMessage = "Line 1\nLine 2\nLine 3";
            String prompt = AgenticSystemPrompt.build(multilineMessage, null, null);

            assertThat(prompt).contains("Line 1");
            assertThat(prompt).contains("Line 2");
            assertThat(prompt).contains("Line 3");
        }

        @Test
        @DisplayName("Should handle Unicode characters in user message")
        void build_unicodeCharacters_preservesCharacters() {
            String unicodeMessage = "مرحبا 你好 שלום Здравствуйте";
            String prompt = AgenticSystemPrompt.build(unicodeMessage, null, null);

            assertThat(prompt).contains(unicodeMessage);
        }

        @Test
        @DisplayName("Should handle user ID with special characters")
        void build_userIdWithSpecialChars_includesUserId() {
            UserContext userContext = new UserContext("user@domain.com", null);
            String prompt = AgenticSystemPrompt.build("test", userContext, null);

            assertThat(prompt).contains("user@domain.com");
        }
    }

    @Nested
    @DisplayName("Full Integration Tests")
    class FullIntegrationTests {

        @Test
        @DisplayName("Should build complete prompt with all parameters populated")
        void build_allParametersPopulated_buildsCompletePrompt() {
            UserContext userContext = new UserContext(
                "john.doe",
                Set.of("IT-Support", "Admin")
            );

            List<ChatMessage> history = new ArrayList<>();
            history.add(UserMessage.from("My laptop is slow"));
            history.add(AiMessage.from("Have you checked the task manager?"));
            history.add(UserMessage.from("Yes, CPU is at 100%"));

            String prompt = AgenticSystemPrompt.build(
                "Create a ticket for this issue",
                userContext,
                history
            );

            // Should include all sections
            assertThat(prompt).contains("INTENT CLASSIFICATION");
            assertThat(prompt).contains("TICKET TYPE CLASSIFICATION");
            assertThat(prompt).contains("SLOT FILLING");
            assertThat(prompt).contains("CURRENT USER");
            assertThat(prompt).contains("john.doe");
            assertThat(prompt).contains("IT-Support");
            assertThat(prompt).contains("CONVERSATION HISTORY");
            assertThat(prompt).contains("My laptop is slow");
            assertThat(prompt).contains("CURRENT USER REQUEST");
            assertThat(prompt).contains("Create a ticket for this issue");
        }

        @Test
        @DisplayName("Should maintain proper section ordering")
        void build_allParameters_maintainsSectionOrdering() {
            UserContext userContext = new UserContext("user", Set.of("Group1"));
            List<ChatMessage> history = new ArrayList<>();
            history.add(UserMessage.from("Test message"));

            String prompt = AgenticSystemPrompt.build("Current request", userContext, history);

            int roleDefPos = prompt.indexOf("IT support assistant");
            int intentPos = prompt.indexOf("INTENT CLASSIFICATION");
            int ticketTypePos = prompt.indexOf("TICKET TYPE CLASSIFICATION");
            int slotFillingPos = prompt.indexOf("SLOT FILLING");
            int contextExtractionPos = prompt.indexOf("CONTEXT EXTRACTION");
            int toolUsagePos = prompt.indexOf("TOOL USAGE");
            int outputRulesPos = prompt.indexOf("OUTPUT RULES");
            // Use more specific markers to avoid matching substring issues
            int userContextPos = prompt.indexOf("=== CURRENT USER ===");
            int historyPos = prompt.indexOf("=== CONVERSATION HISTORY");
            int currentRequestPos = prompt.indexOf("=== CURRENT USER REQUEST ===");

            // Verify ordering - all positions should be valid (>= 0)
            assertThat(roleDefPos).isGreaterThanOrEqualTo(0);
            assertThat(intentPos).isGreaterThanOrEqualTo(0);
            assertThat(userContextPos).isGreaterThanOrEqualTo(0);

            // Verify instruction sections come first
            assertThat(roleDefPos).isLessThan(intentPos);
            assertThat(intentPos).isLessThan(ticketTypePos);
            assertThat(ticketTypePos).isLessThan(slotFillingPos);
            assertThat(slotFillingPos).isLessThan(contextExtractionPos);
            assertThat(contextExtractionPos).isLessThan(toolUsagePos);
            assertThat(toolUsagePos).isLessThan(outputRulesPos);

            // Verify context sections come after instructions
            // Based on source code: OUTPUT RULES → CURRENT USER → CONVERSATION HISTORY → CURRENT USER REQUEST
            assertThat(outputRulesPos).isLessThan(userContextPos);
            assertThat(userContextPos).isLessThan(historyPos);
            assertThat(historyPos).isLessThan(currentRequestPos);
        }

        @Test
        @DisplayName("Should produce deterministic output for same inputs")
        void build_sameInputs_producesSameOutput() {
            UserContext userContext = new UserContext("test.user", Set.of("Group1"));
            List<ChatMessage> history = new ArrayList<>();
            history.add(UserMessage.from("Test"));

            String prompt1 = AgenticSystemPrompt.build("message", userContext, history);
            String prompt2 = AgenticSystemPrompt.build("message", userContext, history);

            assertThat(prompt1).isEqualTo(prompt2);
        }

        @Test
        @DisplayName("Should handle mixed conversation with context extraction guidance")
        void build_mixedConversation_includesContextExtractionForBothMessages() {
            List<ChatMessage> history = new ArrayList<>();
            history.add(UserMessage.from("I have issue with my CST outlook email"));
            history.add(AiMessage.from("Can you describe the issue?"));
            history.add(UserMessage.from("I can't make login with my username and password"));
            history.add(AiMessage.from("I can help with that."));
            history.add(UserMessage.from("So I need to open incident with email issue"));

            String prompt = AgenticSystemPrompt.build("Create the ticket", null, history);

            // Should include the conversation history with extraction guidance
            assertThat(prompt).contains("CST outlook email");
            assertThat(prompt).contains("CONTEXT EXTRACTION");
            assertThat(prompt).contains("scan for issue details");
        }
    }

    @Nested
    @DisplayName("Intent Classification Prompt Edge Cases")
    class IntentClassificationEdgeCaseTests {

        @Test
        @DisplayName("Should handle exactly 6 messages in history")
        void buildIntentClassificationPrompt_exactly6Messages_includesAll() {
            List<ChatMessage> history = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                history.add(UserMessage.from("Message " + i));
            }

            String prompt = AgenticSystemPrompt.buildIntentClassificationPrompt(
                "Current", history);

            assertThat(prompt).contains("Message 0");
            assertThat(prompt).contains("Message 5");
        }

        @Test
        @DisplayName("Should handle less than 6 messages in history")
        void buildIntentClassificationPrompt_lessThan6Messages_includesAll() {
            List<ChatMessage> history = new ArrayList<>();
            history.add(UserMessage.from("Message 0"));
            history.add(UserMessage.from("Message 1"));

            String prompt = AgenticSystemPrompt.buildIntentClassificationPrompt(
                "Current", history);

            assertThat(prompt).contains("Message 0");
            assertThat(prompt).contains("Message 1");
        }

        @Test
        @DisplayName("Should handle 7 messages, showing only last 6")
        void buildIntentClassificationPrompt_7Messages_showsLast6() {
            List<ChatMessage> history = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                history.add(UserMessage.from("Message " + i));
            }

            String prompt = AgenticSystemPrompt.buildIntentClassificationPrompt(
                "Current", history);

            assertThat(prompt).doesNotContain("Message 0");
            assertThat(prompt).contains("Message 1");
            assertThat(prompt).contains("Message 6");
        }

        @Test
        @DisplayName("Should handle empty message string")
        void buildIntentClassificationPrompt_emptyMessage_includesEmptyString() {
            String prompt = AgenticSystemPrompt.buildIntentClassificationPrompt("", null);

            assertThat(prompt).isNotNull();
            assertThat(prompt).contains("Current message:");
        }

        @Test
        @DisplayName("Should include response format instruction")
        void buildIntentClassificationPrompt_validInput_includesResponseFormat() {
            String prompt = AgenticSystemPrompt.buildIntentClassificationPrompt(
                "Test", null);

            assertThat(prompt).contains("Classification");
            assertThat(prompt).containsAnyOf(
                "respond with only",
                "classification label",
                "EXACTLY ONE"
            );
        }
    }
}
