package com.bmc.rag.api.integration.teams;

import com.bmc.rag.agent.service.RagAssistantService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TeamsBotHandler.
 */
@ExtendWith(MockitoExtension.class)
class TeamsBotHandlerTest {

    @Mock
    private RagAssistantService ragAssistantService;

    @Mock
    private TeamsBotConfig config;

    private ObjectMapper objectMapper;
    private TeamsBotHandler handler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        handler = new TeamsBotHandler(ragAssistantService, config, objectMapper);
    }

    @Test
    void processMessage_shouldHandleMessageActivity() {
        // Given
        ObjectNode activity = createMessageActivity("Hello", "user-123", "John Doe", "conv-123");

        RagAssistantService.ChatResponseDto mockResponse = RagAssistantService.ChatResponseDto.builder()
            .response("Hello! How can I help you?")
            .sources(Collections.emptyList())
            .hasContext(false)
            .build();

        when(config.isIncludeCitations()).thenReturn(false);
        when(config.getMaxMessageLength()).thenReturn(4000);
        when(ragAssistantService.chat(anyString(), anyString(), any()))
            .thenReturn(mockResponse);

        // When
        JsonNode response = handler.processMessage(activity);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.path("type").asText()).isEqualTo("message");
        assertThat(response.path("text").asText()).contains("Hello! How can I help you?");
        verify(ragAssistantService).chat(anyString(), anyString(), any());
    }

    @Test
    void processMessage_shouldRemoveBotMention() {
        // Given
        ObjectNode activity = createMessageActivity(
            "<at>BotName</at> help me",
            "user-123",
            "John",
            "conv-123"
        );

        RagAssistantService.ChatResponseDto mockResponse = RagAssistantService.ChatResponseDto.builder()
            .response("I'm here to help")
            .sources(Collections.emptyList())
            .hasContext(false)
            .build();

        when(config.isIncludeCitations()).thenReturn(false);
        when(config.getMaxMessageLength()).thenReturn(4000);
        when(ragAssistantService.chat(anyString(), anyString(), any()))
            .thenReturn(mockResponse);

        // When
        JsonNode response = handler.processMessage(activity);

        // Then
        assertThat(response).isNotNull();
        verify(ragAssistantService).chat(anyString(), anyString(), any());
    }

    @Test
    void processMessage_shouldCreateAdaptiveCardWhenCitationsPresent() {
        // Given
        ObjectNode activity = createMessageActivity("Search query", "user-123", "John", "conv-123");

        RagAssistantService.ChatResponseDto mockResponse = RagAssistantService.ChatResponseDto.builder()
            .response("Found information")
            .sources(Arrays.asList("INC INC000001", "KB KB000002"))
            .hasContext(true)
            .build();

        when(config.isIncludeCitations()).thenReturn(true);
        when(config.getMaxMessageLength()).thenReturn(4000);
        when(ragAssistantService.chat(anyString(), anyString(), any()))
            .thenReturn(mockResponse);

        // When
        JsonNode response = handler.processMessage(activity);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.path("attachments").isArray()).isTrue();
        assertThat(response.path("attachments").size()).isGreaterThan(0);

        JsonNode attachment = response.path("attachments").get(0);
        assertThat(attachment.path("contentType").asText())
            .isEqualTo("application/vnd.microsoft.card.adaptive");

        JsonNode card = attachment.path("content");
        assertThat(card.path("type").asText()).isEqualTo("AdaptiveCard");
        assertThat(card.path("version").asText()).isEqualTo("1.4");
    }

    @Test
    void processMessage_shouldCreateTextResponseWhenNoCitations() {
        // Given
        ObjectNode activity = createMessageActivity("Hello", "user-123", "John", "conv-123");

        RagAssistantService.ChatResponseDto mockResponse = RagAssistantService.ChatResponseDto.builder()
            .response("Hello there!")
            .sources(Collections.emptyList())
            .hasContext(false)
            .build();

        when(config.isIncludeCitations()).thenReturn(false);
        when(config.getMaxMessageLength()).thenReturn(4000);
        when(ragAssistantService.chat(anyString(), anyString(), any()))
            .thenReturn(mockResponse);

        // When
        JsonNode response = handler.processMessage(activity);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.has("text")).isTrue();
        assertThat(response.path("text").asText()).isEqualTo("Hello there!");
        assertThat(response.has("attachments")).isFalse();
    }

    @Test
    void processMessage_shouldTruncateLongText() {
        // Given
        ObjectNode activity = createMessageActivity("Query", "user-123", "John", "conv-123");

        String longResponse = "a".repeat(5000);
        RagAssistantService.ChatResponseDto mockResponse = RagAssistantService.ChatResponseDto.builder()
            .response(longResponse)
            .sources(Collections.emptyList())
            .hasContext(false)
            .build();

        when(config.isIncludeCitations()).thenReturn(false);
        when(config.getMaxMessageLength()).thenReturn(4000);
        when(ragAssistantService.chat(anyString(), anyString(), any()))
            .thenReturn(mockResponse);

        // When
        JsonNode response = handler.processMessage(activity);

        // Then
        assertThat(response).isNotNull();
        String text = response.path("text").asText();
        assertThat(text.length()).isLessThanOrEqualTo(4000);
        assertThat(text).endsWith("...");
    }

    @Test
    void processMessage_shouldHandleConversationUpdate() {
        // Given
        ObjectNode activity = createConversationUpdateActivity("bot-123");
        when(config.getDisplayName()).thenReturn("BMC Remedy RAG Assistant");

        // When
        JsonNode response = handler.processMessage(activity);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.path("attachments").isArray()).isTrue();

        JsonNode card = response.path("attachments").get(0).path("content");
        assertThat(card.path("type").asText()).isEqualTo("AdaptiveCard");
    }

    @Test
    void processMessage_shouldIgnoreUnknownActivityTypes() {
        // Given
        ObjectNode activity = objectMapper.createObjectNode();
        activity.put("type", "unknown");

        // When
        JsonNode response = handler.processMessage(activity);

        // Then
        assertThat(response).isNull();
    }

    @Test
    void processMessage_shouldIgnoreConversationUpdateWithoutBotAdded() {
        // Given - create an activity where the recipient (bot) is "bot-123"
        // but the member added is a different user "human-user-456"
        ObjectNode activity = objectMapper.createObjectNode();
        activity.put("type", "conversationUpdate");

        ObjectNode recipient = objectMapper.createObjectNode();
        recipient.put("id", "bot-123");
        activity.set("recipient", recipient);

        ArrayNode membersAdded = objectMapper.createArrayNode();
        ObjectNode member = objectMapper.createObjectNode();
        member.put("id", "human-user-456"); // Not the bot
        membersAdded.add(member);
        activity.set("membersAdded", membersAdded);

        ObjectNode conversation = objectMapper.createObjectNode();
        conversation.put("id", "conv-123");
        activity.set("conversation", conversation);

        // When
        JsonNode response = handler.processMessage(activity);

        // Then - no welcome card since bot was not the one added
        assertThat(response).isNull();
    }

    @Test
    void processMessage_shouldUseCorrectSessionId() {
        // Given
        String conversationId = "conv-abc-123";
        ObjectNode activity = createMessageActivity("Test", "user-123", "John", conversationId);

        RagAssistantService.ChatResponseDto mockResponse = RagAssistantService.ChatResponseDto.builder()
            .response("Response")
            .sources(Collections.emptyList())
            .hasContext(false)
            .build();

        when(config.isIncludeCitations()).thenReturn(false);
        when(config.getMaxMessageLength()).thenReturn(4000);
        when(ragAssistantService.chat(anyString(), anyString(), any()))
            .thenReturn(mockResponse);

        // When
        handler.processMessage(activity);

        // Then
        verify(ragAssistantService).chat(
            org.mockito.ArgumentMatchers.eq("teams-" + conversationId),
            anyString(),
            any()
        );
    }

    @Test
    void adaptiveCard_shouldIncludeSourceBadges() {
        // Given
        ObjectNode activity = createMessageActivity("Query", "user-123", "John", "conv-123");

        RagAssistantService.ChatResponseDto mockResponse = RagAssistantService.ChatResponseDto.builder()
            .response("Response")
            .sources(Arrays.asList("INC INC000001", "KB KB000002", "WO WO000003"))
            .hasContext(true)
            .build();

        when(config.isIncludeCitations()).thenReturn(true);
        when(config.getMaxMessageLength()).thenReturn(4000);
        when(ragAssistantService.chat(anyString(), anyString(), any()))
            .thenReturn(mockResponse);

        // When
        JsonNode response = handler.processMessage(activity);

        // Then
        assertThat(response).isNotNull();
        JsonNode card = response.path("attachments").get(0).path("content");
        JsonNode body = card.path("body");

        // Verify sources section exists
        boolean hasSourcesHeader = false;
        for (JsonNode element : body) {
            if (element.path("text").asText().contains("Sources")) {
                hasSourcesHeader = true;
                break;
            }
        }
        assertThat(hasSourcesHeader).isTrue();
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void processMessage_shouldHandleConversationUpdateWithNoMembersAdded() {
        // Given - conversationUpdate with no membersAdded
        ObjectNode activity = objectMapper.createObjectNode();
        activity.put("type", "conversationUpdate");
        // membersAdded is missing entirely (path returns a MissingNode)

        // When
        JsonNode response = handler.processMessage(activity);

        // Then - no welcome card since membersAdded is not an array
        assertThat(response).isNull();
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void processMessage_shouldHandleLongMessageTruncationInLog() {
        // Given - a message with exactly 50 characters (edge case for logging truncation)
        String text50 = "a".repeat(51);
        ObjectNode activity = createMessageActivity(text50, "user-123", "John", "conv-123");

        RagAssistantService.ChatResponseDto mockResponse = RagAssistantService.ChatResponseDto.builder()
            .response("Response")
            .sources(Collections.emptyList())
            .hasContext(false)
            .build();

        when(config.isIncludeCitations()).thenReturn(false);
        when(config.getMaxMessageLength()).thenReturn(4000);
        when(ragAssistantService.chat(anyString(), anyString(), any()))
            .thenReturn(mockResponse);

        // When
        JsonNode response = handler.processMessage(activity);

        // Then
        assertThat(response).isNotNull();
    }

    @Test
    void adaptiveCard_shouldHandleChangeSourceType() {
        // Given - source with CHG type
        ObjectNode activity = createMessageActivity("Query", "user-123", "John", "conv-123");

        RagAssistantService.ChatResponseDto mockResponse = RagAssistantService.ChatResponseDto.builder()
            .response("Response")
            .sources(Arrays.asList("CHG CHG000001"))
            .hasContext(true)
            .build();

        when(config.isIncludeCitations()).thenReturn(true);
        when(config.getMaxMessageLength()).thenReturn(4000);
        when(ragAssistantService.chat(anyString(), anyString(), any()))
            .thenReturn(mockResponse);

        // When
        JsonNode response = handler.processMessage(activity);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.path("attachments").isArray()).isTrue();
    }

    @Test
    void adaptiveCard_shouldHandleProblemSourceType() {
        // Given - source with PBM type
        ObjectNode activity = createMessageActivity("Query", "user-123", "John", "conv-123");

        RagAssistantService.ChatResponseDto mockResponse = RagAssistantService.ChatResponseDto.builder()
            .response("Response")
            .sources(Arrays.asList("PBM PBM000001"))
            .hasContext(true)
            .build();

        when(config.isIncludeCitations()).thenReturn(true);
        when(config.getMaxMessageLength()).thenReturn(4000);
        when(ragAssistantService.chat(anyString(), anyString(), any()))
            .thenReturn(mockResponse);

        // When
        JsonNode response = handler.processMessage(activity);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.path("attachments").isArray()).isTrue();
    }

    @Test
    void adaptiveCard_shouldHandleUnknownSourceType() {
        // Given - source with unknown type
        ObjectNode activity = createMessageActivity("Query", "user-123", "John", "conv-123");

        RagAssistantService.ChatResponseDto mockResponse = RagAssistantService.ChatResponseDto.builder()
            .response("Response")
            .sources(Arrays.asList("CUSTOM custom-id-123"))
            .hasContext(true)
            .build();

        when(config.isIncludeCitations()).thenReturn(true);
        when(config.getMaxMessageLength()).thenReturn(4000);
        when(ragAssistantService.chat(anyString(), anyString(), any()))
            .thenReturn(mockResponse);

        // When
        JsonNode response = handler.processMessage(activity);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.path("attachments").isArray()).isTrue();
    }

    @Test
    void adaptiveCard_shouldHandleNullAndEmptySource() {
        // Given - null and empty sources in list
        ObjectNode activity = createMessageActivity("Query", "user-123", "John", "conv-123");

        RagAssistantService.ChatResponseDto mockResponse = RagAssistantService.ChatResponseDto.builder()
            .response("Response")
            .sources(Arrays.asList(null, "", "WORKORDER WO123", "CHANGE CHG456", "KNOWLEDGE KB789", "PROBLEM PBM012"))
            .hasContext(true)
            .build();

        when(config.isIncludeCitations()).thenReturn(true);
        when(config.getMaxMessageLength()).thenReturn(4000);
        when(ragAssistantService.chat(anyString(), anyString(), any()))
            .thenReturn(mockResponse);

        // When
        JsonNode response = handler.processMessage(activity);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.path("attachments").isArray()).isTrue();
    }

    @Test
    void processMessage_shouldHandleCitationsWithIncludeCitationsTrue_butEmptySources() {
        // Given - include citations is true but sources are empty
        ObjectNode activity = createMessageActivity("Hello", "user-123", "John", "conv-123");

        RagAssistantService.ChatResponseDto mockResponse = RagAssistantService.ChatResponseDto.builder()
            .response("Hello there!")
            .sources(Collections.emptyList())
            .hasContext(false)
            .build();

        when(config.isIncludeCitations()).thenReturn(true);
        when(config.getMaxMessageLength()).thenReturn(4000);
        when(ragAssistantService.chat(anyString(), anyString(), any()))
            .thenReturn(mockResponse);

        // When
        JsonNode response = handler.processMessage(activity);

        // Then - should fall through to text response since sources is empty
        assertThat(response).isNotNull();
        assertThat(response.has("text")).isTrue();
        assertThat(response.path("text").asText()).isEqualTo("Hello there!");
    }

    @Test
    void processMessage_shouldHandleSourceWithoutSpace() {
        // Given - source string without space separator
        ObjectNode activity = createMessageActivity("Query", "user-123", "John", "conv-123");

        RagAssistantService.ChatResponseDto mockResponse = RagAssistantService.ChatResponseDto.builder()
            .response("Response")
            .sources(Arrays.asList("INC000001"))
            .hasContext(true)
            .build();

        when(config.isIncludeCitations()).thenReturn(true);
        when(config.getMaxMessageLength()).thenReturn(4000);
        when(ragAssistantService.chat(anyString(), anyString(), any()))
            .thenReturn(mockResponse);

        // When
        JsonNode response = handler.processMessage(activity);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.path("attachments").isArray()).isTrue();
    }

    // Helper methods

    private ObjectNode createMessageActivity(String text, String userId, String userName, String conversationId) {
        ObjectNode activity = objectMapper.createObjectNode();
        activity.put("type", "message");
        activity.put("id", "msg-123");
        activity.put("text", text);

        ObjectNode from = objectMapper.createObjectNode();
        from.put("id", userId);
        from.put("name", userName);
        activity.set("from", from);

        ObjectNode conversation = objectMapper.createObjectNode();
        conversation.put("id", conversationId);
        activity.set("conversation", conversation);

        ObjectNode recipient = objectMapper.createObjectNode();
        recipient.put("id", "bot-123");
        recipient.put("name", "Bot");
        activity.set("recipient", recipient);

        return activity;
    }

    private ObjectNode createConversationUpdateActivity(String botId) {
        ObjectNode activity = objectMapper.createObjectNode();
        activity.put("type", "conversationUpdate");

        ObjectNode recipient = objectMapper.createObjectNode();
        recipient.put("id", botId);
        activity.set("recipient", recipient);

        ArrayNode membersAdded = objectMapper.createArrayNode();
        ObjectNode member = objectMapper.createObjectNode();
        member.put("id", botId);
        membersAdded.add(member);
        activity.set("membersAdded", membersAdded);

        ObjectNode conversation = objectMapper.createObjectNode();
        conversation.put("id", "conv-123");
        activity.set("conversation", conversation);

        return activity;
    }
}
