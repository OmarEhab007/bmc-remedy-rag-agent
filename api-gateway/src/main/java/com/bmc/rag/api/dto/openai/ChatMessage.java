package com.bmc.rag.api.dto.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAI-compatible chat message DTO.
 * Represents a single message in a conversation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessage {

    /**
     * The role of the message sender.
     * Valid values: "system", "user", "assistant"
     */
    private String role;

    /**
     * The content of the message.
     */
    private String content;

    /**
     * Optional name of the participant (for multi-user scenarios).
     */
    private String name;

    /**
     * Create a system message.
     */
    public static ChatMessage system(String content) {
        return ChatMessage.builder()
            .role("system")
            .content(content)
            .build();
    }

    /**
     * Create a user message.
     */
    public static ChatMessage user(String content) {
        return ChatMessage.builder()
            .role("user")
            .content(content)
            .build();
    }

    /**
     * Create an assistant message.
     */
    public static ChatMessage assistant(String content) {
        return ChatMessage.builder()
            .role("assistant")
            .content(content)
            .build();
    }
}
