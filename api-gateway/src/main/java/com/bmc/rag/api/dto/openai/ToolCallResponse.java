package com.bmc.rag.api.dto.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * OpenAI-compatible response for tool/function calls.
 * When the model decides to call a tool, this response format is used.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolCallResponse {

    private String id;
    private String object;
    private long created;
    private String model;
    private List<ToolCallChoice> choices;
    private Usage usage;

    /**
     * Choice containing tool calls.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCallChoice {
        private int index;
        private ToolCallMessage message;
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    /**
     * Message with tool calls.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCallMessage {
        private String role;
        private String content;
        @JsonProperty("tool_calls")
        private List<ToolCall> toolCalls;
    }

    /**
     * Individual tool call.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCall {
        private String id;
        private String type;
        private ToolCallFunction function;
    }

    /**
     * Function call details.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCallFunction {
        private String name;
        private String arguments; // JSON string
    }

    /**
     * Token usage.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private int promptTokens;
        @JsonProperty("completion_tokens")
        private int completionTokens;
        @JsonProperty("total_tokens")
        private int totalTokens;
    }

    /**
     * Create a tool call response for create_incident.
     */
    public static ToolCallResponse createIncident(String summary, String description, String model) {
        String callId = "call_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);

        // Build arguments JSON
        String args = String.format(
            "{\"summary\":\"%s\",\"description\":\"%s\"}",
            escapeJson(summary),
            escapeJson(description)
        );

        return ToolCallResponse.builder()
            .id("chatcmpl-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24))
            .object("chat.completion")
            .created(System.currentTimeMillis() / 1000)
            .model(model)
            .choices(Collections.singletonList(
                ToolCallChoice.builder()
                    .index(0)
                    .message(ToolCallMessage.builder()
                        .role("assistant")
                        .content(null)
                        .toolCalls(Collections.singletonList(
                            ToolCall.builder()
                                .id(callId)
                                .type("function")
                                .function(ToolCallFunction.builder()
                                    .name("create_incident")
                                    .arguments(args)
                                    .build())
                                .build()
                        ))
                        .build())
                    .finishReason("tool_calls")
                    .build()
            ))
            .usage(Usage.builder()
                .promptTokens(summary.length() / 4)
                .completionTokens(args.length() / 4)
                .totalTokens((summary.length() + args.length()) / 4)
                .build())
            .build();
    }

    /**
     * Create a tool call response for search_incidents.
     */
    public static ToolCallResponse searchIncidents(String query, String model) {
        String callId = "call_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);

        String args = String.format("{\"query\":\"%s\"}", escapeJson(query));

        return ToolCallResponse.builder()
            .id("chatcmpl-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24))
            .object("chat.completion")
            .created(System.currentTimeMillis() / 1000)
            .model(model)
            .choices(Collections.singletonList(
                ToolCallChoice.builder()
                    .index(0)
                    .message(ToolCallMessage.builder()
                        .role("assistant")
                        .content(null)
                        .toolCalls(Collections.singletonList(
                            ToolCall.builder()
                                .id(callId)
                                .type("function")
                                .function(ToolCallFunction.builder()
                                    .name("search_incidents")
                                    .arguments(args)
                                    .build())
                                .build()
                        ))
                        .build())
                    .finishReason("tool_calls")
                    .build()
            ))
            .usage(Usage.builder()
                .promptTokens(query.length() / 4)
                .completionTokens(args.length() / 4)
                .totalTokens((query.length() + args.length()) / 4)
                .build())
            .build();
    }

    /**
     * Create a tool call response for confirm_action.
     */
    public static ToolCallResponse confirmAction(String actionId, String model) {
        String callId = "call_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);

        String args = String.format("{\"action_id\":\"%s\"}", escapeJson(actionId));

        return ToolCallResponse.builder()
            .id("chatcmpl-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24))
            .object("chat.completion")
            .created(System.currentTimeMillis() / 1000)
            .model(model)
            .choices(Collections.singletonList(
                ToolCallChoice.builder()
                    .index(0)
                    .message(ToolCallMessage.builder()
                        .role("assistant")
                        .content(null)
                        .toolCalls(Collections.singletonList(
                            ToolCall.builder()
                                .id(callId)
                                .type("function")
                                .function(ToolCallFunction.builder()
                                    .name("confirm_action")
                                    .arguments(args)
                                    .build())
                                .build()
                        ))
                        .build())
                    .finishReason("tool_calls")
                    .build()
            ))
            .usage(Usage.builder()
                .promptTokens(10)
                .completionTokens(args.length() / 4)
                .totalTokens(10 + args.length() / 4)
                .build())
            .build();
    }

    /**
     * Escape special characters for JSON string.
     */
    private static String escapeJson(String text) {
        if (text == null) return "";
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
