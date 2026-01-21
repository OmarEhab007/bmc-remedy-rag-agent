package com.bmc.rag.api.dto.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAI-compatible chat completion choice DTO.
 * Represents a single completion choice in the response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatCompletionChoice {

    /**
     * The index of this choice in the list.
     */
    private int index;

    /**
     * The generated message for this choice.
     */
    private ChatMessage message;

    /**
     * The reason the model stopped generating.
     * Possible values: "stop", "length", "content_filter", null
     */
    @JsonProperty("finish_reason")
    private String finishReason;

    /**
     * Create a choice with a complete message.
     */
    public static ChatCompletionChoice of(int index, String content, String finishReason) {
        return ChatCompletionChoice.builder()
            .index(index)
            .message(ChatMessage.assistant(content))
            .finishReason(finishReason)
            .build();
    }
}
