package com.bmc.rag.api.dto.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * OpenAI-compatible chat completion request DTO.
 * See: https://platform.openai.com/docs/api-reference/chat/create
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatCompletionRequest {

    /**
     * ID of the model to use.
     * For BMC RAG, this is ignored - always uses the configured RAG pipeline.
     */
    @Builder.Default
    private String model = "bmc-remedy-rag";

    /**
     * A list of messages comprising the conversation so far.
     */
    @NotEmpty(message = "Messages list cannot be empty")
    private List<ChatMessage> messages;

    /**
     * Whether to stream partial message deltas as SSE events.
     */
    @Builder.Default
    private boolean stream = false;

    /**
     * Sampling temperature (0-2). Higher = more random.
     * Ignored by BMC RAG - uses backend configuration.
     */
    private Double temperature;

    /**
     * Nucleus sampling. Top P probability mass to consider.
     * Ignored by BMC RAG - uses backend configuration.
     */
    @JsonProperty("top_p")
    private Double topP;

    /**
     * Maximum tokens to generate in the response.
     * Ignored by BMC RAG - uses backend configuration.
     */
    @JsonProperty("max_tokens")
    private Integer maxTokens;

    /**
     * Number of completions to generate.
     * BMC RAG only supports n=1.
     */
    @Builder.Default
    private Integer n = 1;

    /**
     * Stop sequences. Generation stops when encountered.
     * Ignored by BMC RAG.
     */
    private List<String> stop;

    /**
     * Presence penalty (-2.0 to 2.0).
     * Ignored by BMC RAG - uses backend configuration.
     */
    @JsonProperty("presence_penalty")
    private Double presencePenalty;

    /**
     * Frequency penalty (-2.0 to 2.0).
     * Ignored by BMC RAG - uses backend configuration.
     */
    @JsonProperty("frequency_penalty")
    private Double frequencyPenalty;

    /**
     * Optional user identifier for abuse monitoring.
     */
    private String user;

    /**
     * Optional additional parameters.
     */
    private Map<String, Object> additionalProperties;

    /**
     * A list of tools the model may call.
     * OpenAI function calling support.
     */
    private List<Tool> tools;

    /**
     * Controls which (if any) tool is called by the model.
     * "none" means no tool, "auto" means model decides, or specific tool name.
     */
    @JsonProperty("tool_choice")
    private Object toolChoice;

    /**
     * Tool definition for function calling.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Tool {
        private String type; // "function"
        private ToolFunction function;
    }

    /**
     * Function definition within a tool.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolFunction {
        private String name;
        private String description;
        private Map<String, Object> parameters;
    }

    /**
     * Check if tools are available in the request.
     */
    public boolean hasTools() {
        return tools != null && !tools.isEmpty();
    }

    /**
     * Get tool by name.
     */
    public Tool getToolByName(String name) {
        if (tools == null) return null;
        return tools.stream()
            .filter(t -> t.getFunction() != null && name.equals(t.getFunction().getName()))
            .findFirst()
            .orElse(null);
    }

    /**
     * Extract the last user message from the conversation.
     */
    public String getLastUserMessage() {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        // Find the last user message
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage msg = messages.get(i);
            if ("user".equals(msg.getRole())) {
                return msg.getContent();
            }
        }
        return "";
    }

    /**
     * Check if there's a system message in the conversation.
     */
    public boolean hasSystemMessage() {
        if (messages == null) return false;
        return messages.stream().anyMatch(m -> "system".equals(m.getRole()));
    }
}
