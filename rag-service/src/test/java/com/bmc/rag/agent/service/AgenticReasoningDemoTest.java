package com.bmc.rag.agent.service;

import com.bmc.rag.agent.config.AgenticConfig;
import com.bmc.rag.agent.confirmation.ConfirmationService;
import com.bmc.rag.agent.prompt.AgenticSystemPrompt;
import com.bmc.rag.agent.retrieval.SecureContentRetriever.UserContext;
import com.bmc.rag.agent.service.AgenticAssistantService.IntentClassification;
import com.bmc.rag.agent.tools.RemedyIncidentTool;
import com.bmc.rag.agent.tools.RemedyWorkOrderTool;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.Mockito.lenient;

/**
 * Demonstration test showing the agent's reasoning capabilities
 * with various sample messages.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Agentic Reasoning Demo")
@Disabled("Demo test for manual inspection only")
class AgenticReasoningDemoTest {

    @Mock private ChatLanguageModel chatModel;
    @Mock private RemedyIncidentTool incidentTool;
    @Mock private RemedyWorkOrderTool workOrderTool;
    @Mock private ConfirmationService confirmationService;
    @Mock private AgenticConfig agenticConfig;

    private AgenticAssistantService service;

    @BeforeEach
    void setUp() {
        lenient().when(agenticConfig.isEnabled()).thenReturn(true);
        service = new AgenticAssistantService(
            chatModel, incidentTool, workOrderTool, confirmationService, agenticConfig
        );
    }

    @Test
    @DisplayName("Demo: Intent Detection and Classification")
    void demoIntentDetectionAndClassification() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("AGENTIC REASONING DEMO - Intent Detection & Classification");
        System.out.println("=".repeat(80));

        // Test messages organized by expected behavior
        // Format: {message, expected_intent, expected_has_agentic}
        String[][] testCases = {
            // ═══════════════════════════════════════════════════════════════════
            // QUESTIONS - Pure informational requests (no agentic trigger)
            // ═══════════════════════════════════════════════════════════════════
            {"How do I reset my password?", "QUESTION", "false"},
            {"What is the VPN server address?", "QUESTION", "false"},
            {"Why is my email slow?", "QUESTION", "false"},
            {"Can I access the system remotely?", "QUESTION", "false"},

            // ═══════════════════════════════════════════════════════════════════
            // EXPLICIT INCIDENT REQUESTS - Clear ticket creation intent
            // ═══════════════════════════════════════════════════════════════════
            {"Create an incident for my VPN issue", "ACTION_INCIDENT", "true"},
            {"Open a ticket for email not working", "ACTION_INCIDENT", "true"},
            {"Please log an incident", "ACTION_INCIDENT", "true"},
            {"Please create an incident for printer problem", "ACTION_INCIDENT", "true"},

            // ═══════════════════════════════════════════════════════════════════
            // EXPLICIT WORK ORDER REQUESTS
            // ═══════════════════════════════════════════════════════════════════
            {"Create a work order for software installation", "ACTION_WORKORDER", "true"},
            {"Open a work order to set up new laptop", "ACTION_WORKORDER", "true"},
            {"Create a new WO for office move", "ACTION_WORKORDER", "true"},

            // ═══════════════════════════════════════════════════════════════════
            // IMPLICIT ACTION SIGNALS - Multiple signals trigger agentic
            // ═══════════════════════════════════════════════════════════════════
            {"This has been happening for 3 days and I already tried everything", "ACTION_INCIDENT", "true"},
            // Note: Single implicit signal = AMBIGUOUS_INTENT (not enough for action)
            {"It's blocking my work", "AMBIGUOUS_INTENT", "false"},

            // ═══════════════════════════════════════════════════════════════════
            // ARABIC MESSAGES
            // ═══════════════════════════════════════════════════════════════════
            {"أنشئ بلاغ لمشكلة الشبكة", "ACTION_INCIDENT", "true"},
            {"افتح أمر عمل لتثبيت البرنامج", "ACTION_WORKORDER", "true"},

            // ═══════════════════════════════════════════════════════════════════
            // AMBIGUOUS MESSAGES - Need clarification
            // ═══════════════════════════════════════════════════════════════════
            // Question format + action signals = AMBIGUOUS_INTENT
            {"How can I fix this? It's been 3 days and I already tried restarting", "AMBIGUOUS_INTENT", "true"},
            // Vague request without clear intent
            {"Help with my email", "QUESTION", "false"},
        };

        System.out.println("\n┌─────────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│ Message                                          │ Intent    │ Agentic? │");
        System.out.println("├─────────────────────────────────────────────────────────────────────────────┤");

        for (String[] testCase : testCases) {
            String message = testCase[0];
            String expectedIntent = testCase[1];
            boolean expectedAgentic = Boolean.parseBoolean(testCase[2]);

            boolean hasAgentic = service.hasAgenticIntent(message);
            IntentClassification intent = service.classifyIntent(message, null);

            String truncatedMsg = message.length() > 48
                ? message.substring(0, 45) + "..."
                : message;

            String status = (hasAgentic == expectedAgentic && intent.name().equals(expectedIntent))
                ? "✓" : "✗";

            System.out.printf("│ %-48s │ %-9s │ %-8s │ %s%n",
                truncatedMsg, intent, hasAgentic, status);
        }

        System.out.println("└─────────────────────────────────────────────────────────────────────────────┘");
    }

    @Test
    @DisplayName("Demo: Context-Aware Slot Filling")
    void demoContextAwareSlotFilling() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("AGENTIC REASONING DEMO - Context-Aware Slot Filling");
        System.out.println("=".repeat(80));

        // Simulate a conversation where user describes problem then asks for ticket
        List<ChatMessage> conversation = new ArrayList<>();
        conversation.add(UserMessage.from("I can't connect to the VPN from home"));
        conversation.add(AiMessage.from("Have you tried restarting the VPN client?"));
        conversation.add(UserMessage.from("Yes, I also rebooted my laptop. Still getting error 809"));
        conversation.add(AiMessage.from("Error 809 usually indicates a firewall issue. Let me check..."));
        conversation.add(UserMessage.from("It's been like this since this morning, blocking my work"));

        String currentMessage = "Please create a ticket for this issue";

        System.out.println("\n📜 CONVERSATION HISTORY:");
        System.out.println("─".repeat(60));
        for (ChatMessage msg : conversation) {
            if (msg instanceof UserMessage um) {
                System.out.println("User: " + um.singleText());
            } else if (msg instanceof AiMessage am) {
                System.out.println("Assistant: " + am.text());
            }
        }
        System.out.println("─".repeat(60));
        System.out.println("Current: " + currentMessage);
        System.out.println();

        // Classify intent with context
        IntentClassification intent = service.classifyIntent(currentMessage, conversation);
        boolean hasAgentic = service.hasAgenticIntent(currentMessage);

        System.out.println("🔍 ANALYSIS:");
        System.out.println("  • Has Agentic Intent: " + hasAgentic);
        System.out.println("  • Intent Classification: " + intent);
        System.out.println();

        System.out.println("📋 EXPECTED SLOT EXTRACTION (by LLM):");
        System.out.println("  • Summary: \"VPN connection failure with error 809\"");
        System.out.println("  • Description: \"User unable to connect to VPN from home since this");
        System.out.println("                  morning. Error 809 displayed. Troubleshooting attempted:");
        System.out.println("                  restarted VPN client, rebooted laptop. Issue blocking work.\"");
        System.out.println("  • Impact: 3 (Moderate - single user affected)");
        System.out.println("  • Urgency: 2 (High - blocking work, since morning)");
    }

    @Test
    @DisplayName("Demo: Generated System Prompt Preview")
    void demoGeneratedSystemPrompt() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("AGENTIC REASONING DEMO - System Prompt Structure");
        System.out.println("=".repeat(80));

        // Build sample conversation
        List<ChatMessage> history = new ArrayList<>();
        history.add(UserMessage.from("My Outlook keeps crashing when I open attachments"));
        history.add(AiMessage.from("What type of attachments cause the crash?"));
        history.add(UserMessage.from("PDF files mainly, it's been happening for 2 days"));

        UserContext userContext = new UserContext("john.doe", Set.of("IT-Support", "Network-Team"));
        String currentMessage = "Create an incident please";

        // Generate the prompt
        String prompt = AgenticSystemPrompt.build(currentMessage, userContext, history);

        System.out.println("\n📊 PROMPT STATISTICS:");
        System.out.println("  • Total length: " + prompt.length() + " characters");
        System.out.println("  • Estimated tokens: ~" + (prompt.length() / 4) + " tokens");
        System.out.println();

        // Show key sections
        System.out.println("📑 PROMPT SECTIONS INCLUDED:");
        String[] sections = {
            "INTENT CLASSIFICATION",
            "TICKET TYPE CLASSIFICATION",
            "SLOT FILLING",
            "CONTEXT EXTRACTION",
            "TOOL USAGE RULES",
            "OUTPUT RULES",
            "CURRENT USER",
            "CONVERSATION HISTORY"
        };

        for (String section : sections) {
            boolean included = prompt.contains(section);
            System.out.println("  " + (included ? "✓" : "✗") + " " + section);
        }

        System.out.println();
        System.out.println("📝 CONVERSATION HISTORY IN PROMPT:");
        System.out.println("─".repeat(60));

        // Extract and show conversation history section
        int histStart = prompt.indexOf("CONVERSATION HISTORY");
        int histEnd = prompt.indexOf("CURRENT USER REQUEST");
        if (histStart > 0 && histEnd > histStart) {
            String histSection = prompt.substring(histStart, histEnd);
            // Print first 500 chars
            System.out.println(histSection.substring(0, Math.min(500, histSection.length())));
            if (histSection.length() > 500) System.out.println("...[truncated]");
        }
        System.out.println("─".repeat(60));
    }

    @Test
    @DisplayName("Demo: Incident vs Work Order Decision Flow")
    void demoIncidentVsWorkOrderDecision() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("AGENTIC REASONING DEMO - Incident vs Work Order Decision");
        System.out.println("=".repeat(80));

        String[][] scenarios = {
            // message, expected type, reason
            {"My VPN stopped working this morning", "INCIDENT", "Was working → now broken"},
            {"I need Adobe Photoshop installed", "WORK_ORDER", "Requesting new software"},
            {"Excel keeps crashing on large files", "INCIDENT", "Application error/crash"},
            {"Please add me to the finance SharePoint", "WORK_ORDER", "Access request"},
            {"The printer on 3rd floor is jammed", "INCIDENT", "Hardware malfunction"},
            {"Upgrade my laptop to Windows 11", "WORK_ORDER", "System upgrade request"},
            {"Can't login since password expired", "INCIDENT", "Authentication failure"},
            {"Set up a new user account for intern", "WORK_ORDER", "Account provisioning"},
        };

        System.out.println("\n┌───────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│ Scenario                              │ Type       │ Reasoning            │");
        System.out.println("├───────────────────────────────────────────────────────────────────────────┤");

        for (String[] scenario : scenarios) {
            String message = scenario[0];
            String expectedType = scenario[1];
            String reason = scenario[2];

            // For classification, we need explicit creation pattern
            String classifyMsg = "Create a ticket: " + message;
            IntentClassification intent = service.classifyIntent(classifyMsg, null);

            String actualType = intent == IntentClassification.ACTION_WORKORDER ? "WORK_ORDER" : "INCIDENT";
            String status = actualType.equals(expectedType) ? "✓" : "?";

            String truncatedMsg = message.length() > 38
                ? message.substring(0, 35) + "..."
                : message;

            System.out.printf("│ %-39s │ %-10s │ %-20s │ %s%n",
                truncatedMsg, expectedType, reason, status);
        }

        System.out.println("└───────────────────────────────────────────────────────────────────────────┘");
        System.out.println();
        System.out.println("Legend: ✓ = Correctly classified by pattern matching");
        System.out.println("        ? = Requires LLM semantic analysis (pattern doesn't distinguish)");
        System.out.println();
        System.out.println("Note: The system prompt contains detailed rules for the LLM to make");
        System.out.println("      semantic distinctions between BROKEN (Incident) vs NEW/CHANGE (Work Order)");
    }
}
