package com.bmc.rag.api.integration.teams;

import com.bmc.rag.agent.retrieval.SecureContentRetriever.UserContext;
import com.bmc.rag.agent.service.RagAssistantService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Handler for Microsoft Teams bot interactions.
 * Processes incoming messages and generates AI-powered responses with Adaptive Cards.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "teams.bot", name = "enabled", havingValue = "true")
public class TeamsBotHandler {

    private final RagAssistantService ragAssistantService;
    private final TeamsBotConfig config;
    private final ObjectMapper objectMapper;

    /**
     * Process an incoming message from Teams.
     *
     * @param activity The incoming activity from Teams
     * @return Response activity to send back
     */
    public JsonNode processMessage(JsonNode activity) {
        String activityType = activity.path("type").asText();

        if ("message".equals(activityType)) {
            return handleMessage(activity);
        } else if ("conversationUpdate".equals(activityType)) {
            return handleConversationUpdate(activity);
        }

        log.debug("Ignoring activity type: {}", activityType);
        return null;
    }

    /**
     * Handle an incoming text message.
     */
    private JsonNode handleMessage(JsonNode activity) {
        String text = activity.path("text").asText();
        String conversationId = activity.path("conversation").path("id").asText();
        String userId = activity.path("from").path("id").asText();
        String userName = activity.path("from").path("name").asText();

        log.info("Received message from {} ({}): {}", userName, userId,
            text.length() > 50 ? text.substring(0, 50) + "..." : text);

        // Remove bot mention if present
        text = removeBotMention(text);

        // Build user context for ReBAC (in production, would fetch from Azure AD)
        UserContext userContext = new UserContext(userId, Collections.emptySet());

        // Process with RAG service
        String sessionId = "teams-" + conversationId;
        RagAssistantService.ChatResponseDto response = ragAssistantService.chat(sessionId, text, userContext);

        // Build response
        if (config.isIncludeCitations() && !response.getSources().isEmpty()) {
            return buildAdaptiveCardResponse(activity, response);
        } else {
            return buildTextResponse(activity, response.getResponse());
        }
    }

    /**
     * Handle conversation update events (member added/removed).
     */
    private JsonNode handleConversationUpdate(JsonNode activity) {
        JsonNode membersAdded = activity.path("membersAdded");

        if (membersAdded.isArray() && membersAdded.size() > 0) {
            String botId = activity.path("recipient").path("id").asText();

            for (JsonNode member : membersAdded) {
                if (botId.equals(member.path("id").asText())) {
                    // Bot was added to conversation - send welcome message
                    return buildWelcomeCard(activity);
                }
            }
        }

        return null;
    }

    /**
     * Build a simple text response.
     */
    private JsonNode buildTextResponse(JsonNode activity, String text) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("type", "message");
        response.put("text", truncateText(text));
        response.set("conversation", activity.path("conversation").deepCopy());
        response.set("from", activity.path("recipient").deepCopy());
        response.set("recipient", activity.path("from").deepCopy());
        response.put("replyToId", activity.path("id").asText());
        return response;
    }

    /**
     * Build an Adaptive Card response with citations.
     */
    private JsonNode buildAdaptiveCardResponse(JsonNode activity, RagAssistantService.ChatResponseDto chatResponse) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("type", "message");
        response.set("conversation", activity.path("conversation").deepCopy());
        response.set("from", activity.path("recipient").deepCopy());
        response.set("recipient", activity.path("from").deepCopy());
        response.put("replyToId", activity.path("id").asText());

        // Build Adaptive Card
        ObjectNode card = objectMapper.createObjectNode();
        card.put("type", "AdaptiveCard");
        card.put("$schema", "http://adaptivecards.io/schemas/adaptive-card.json");
        card.put("version", "1.4");

        ArrayNode body = objectMapper.createArrayNode();

        // Main response text
        ObjectNode textBlock = objectMapper.createObjectNode();
        textBlock.put("type", "TextBlock");
        textBlock.put("text", truncateText(chatResponse.getResponse()));
        textBlock.put("wrap", true);
        body.add(textBlock);

        // Sources section
        if (!chatResponse.getSources().isEmpty()) {
            ObjectNode sourcesHeader = objectMapper.createObjectNode();
            sourcesHeader.put("type", "TextBlock");
            sourcesHeader.put("text", "**Sources:**");
            sourcesHeader.put("spacing", "Medium");
            body.add(sourcesHeader);

            ObjectNode columnSet = objectMapper.createObjectNode();
            columnSet.put("type", "ColumnSet");
            ArrayNode columns = objectMapper.createArrayNode();

            for (String source : chatResponse.getSources()) {
                ObjectNode column = objectMapper.createObjectNode();
                column.put("type", "Column");
                column.put("width", "auto");

                ArrayNode columnItems = objectMapper.createArrayNode();
                ObjectNode badge = objectMapper.createObjectNode();
                badge.put("type", "TextBlock");
                badge.put("text", formatSourceBadge(source));
                badge.put("color", getSourceColor(source));
                badge.put("size", "Small");
                columnItems.add(badge);

                column.set("items", columnItems);
                columns.add(column);
            }

            columnSet.set("columns", columns);
            body.add(columnSet);
        }

        card.set("body", body);

        // Wrap in attachment
        ArrayNode attachments = objectMapper.createArrayNode();
        ObjectNode attachment = objectMapper.createObjectNode();
        attachment.put("contentType", "application/vnd.microsoft.card.adaptive");
        attachment.set("content", card);
        attachments.add(attachment);

        response.set("attachments", attachments);

        return response;
    }

    /**
     * Build a welcome card for when the bot is added to a conversation.
     */
    private JsonNode buildWelcomeCard(JsonNode activity) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("type", "message");
        response.set("conversation", activity.path("conversation").deepCopy());
        response.set("from", activity.path("recipient").deepCopy());

        ObjectNode card = objectMapper.createObjectNode();
        card.put("type", "AdaptiveCard");
        card.put("$schema", "http://adaptivecards.io/schemas/adaptive-card.json");
        card.put("version", "1.4");

        ArrayNode body = objectMapper.createArrayNode();

        // Header with icon
        ObjectNode header = objectMapper.createObjectNode();
        header.put("type", "TextBlock");
        header.put("text", "👋 Hello! I'm " + config.getDisplayName());
        header.put("size", "Large");
        header.put("weight", "Bolder");
        body.add(header);

        // Description
        ObjectNode desc = objectMapper.createObjectNode();
        desc.put("type", "TextBlock");
        desc.put("text", "I can help you with IT support questions by searching through:");
        desc.put("wrap", true);
        body.add(desc);

        // Capabilities list
        ObjectNode list = objectMapper.createObjectNode();
        list.put("type", "TextBlock");
        list.put("text", "• 📚 Knowledge Articles\n• 🎫 Incident Records\n• 📋 Work Orders\n• 🔄 Change Requests");
        list.put("wrap", true);
        body.add(list);

        // Example prompts
        ObjectNode examples = objectMapper.createObjectNode();
        examples.put("type", "TextBlock");
        examples.put("text", "**Try asking:**\n_\"How do I reset my password?\"_\n_\"What's the status of INC000012345?\"_");
        examples.put("wrap", true);
        examples.put("spacing", "Medium");
        body.add(examples);

        card.set("body", body);

        ArrayNode attachments = objectMapper.createArrayNode();
        ObjectNode attachment = objectMapper.createObjectNode();
        attachment.put("contentType", "application/vnd.microsoft.card.adaptive");
        attachment.set("content", card);
        attachments.add(attachment);

        response.set("attachments", attachments);

        return response;
    }

    /**
     * Remove bot mention from message text.
     */
    private String removeBotMention(String text) {
        // Remove @mentions which appear as <at>BotName</at>
        return text.replaceAll("<at>[^<]*</at>\\s*", "").trim();
    }

    /**
     * Truncate text to max message length.
     */
    private String truncateText(String text) {
        if (text.length() <= config.getMaxMessageLength()) {
            return text;
        }
        return text.substring(0, config.getMaxMessageLength() - 3) + "...";
    }

    /**
     * Format source string as badge text.
     * Sources are in format "TYPE ID" or "TYPE-ID" (e.g., "INC INC000012345")
     */
    private String formatSourceBadge(String source) {
        if (source == null || source.isEmpty()) {
            return "📄 Unknown";
        }

        // Parse the source string to extract type and ID
        String upperSource = source.toUpperCase();
        String label;
        if (upperSource.contains("KB") || upperSource.contains("KNOWLEDGE")) {
            label = "📚 KB";
        } else if (upperSource.contains("INC")) {
            label = "🎫 INC";
        } else if (upperSource.contains("WO") || upperSource.contains("WORKORDER")) {
            label = "📋 WO";
        } else if (upperSource.contains("CHG") || upperSource.contains("CHANGE")) {
            label = "🔄 CHG";
        } else if (upperSource.contains("PBM") || upperSource.contains("PROBLEM")) {
            label = "🔍 PBM";
        } else {
            label = "📄";
        }

        // Extract ID if present (last part after space or the whole thing)
        String id = source.contains(" ") ? source.substring(source.lastIndexOf(" ") + 1) : source;
        return label + " " + id;
    }

    /**
     * Get Adaptive Card color for source string.
     */
    private String getSourceColor(String source) {
        if (source == null) return "Default";
        String upperSource = source.toUpperCase();

        if (upperSource.contains("KB") || upperSource.contains("KNOWLEDGE")) {
            return "Good"; // Green
        } else if (upperSource.contains("INC")) {
            return "Accent"; // Blue
        } else if (upperSource.contains("WO") || upperSource.contains("WORKORDER")) {
            return "Warning"; // Orange
        } else if (upperSource.contains("CHG") || upperSource.contains("CHANGE")) {
            return "Accent"; // Purple (closest in Adaptive Cards)
        } else if (upperSource.contains("PBM") || upperSource.contains("PROBLEM")) {
            return "Attention"; // Red
        }
        return "Default";
    }
}
