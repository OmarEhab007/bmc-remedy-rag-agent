package com.bmc.rag.api.dto.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * OpenAI-compatible chat completion response DTO.
 * See: https://platform.openai.com/docs/api-reference/chat/object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatCompletionResponse {

    /**
     * A unique identifier for this completion.
     */
    private String id;

    /**
     * The object type, always "chat.completion".
     */
    @Builder.Default
    private String object = "chat.completion";

    /**
     * Unix timestamp of when the completion was created.
     */
    private long created;

    /**
     * The model used for the completion.
     */
    private String model;

    /**
     * A list of chat completion choices.
     */
    private List<ChatCompletionChoice> choices;

    /**
     * Token usage information.
     */
    private UsageInfo usage;

    /**
     * System fingerprint (optional, for caching).
     */
    private String system_fingerprint;

    /**
     * Create a simple response with a single choice.
     */
    public static ChatCompletionResponse of(String content, String model) {
        return ChatCompletionResponse.builder()
            .id("chatcmpl-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24))
            .object("chat.completion")
            .created(System.currentTimeMillis() / 1000)
            .model(model)
            .choices(Collections.singletonList(
                ChatCompletionChoice.of(0, content, "stop")
            ))
            .build();
    }

    /**
     * Create a response with usage information.
     */
    public static ChatCompletionResponse of(String content, String model, String prompt) {
        ChatCompletionResponse response = of(content, model);
        response.setUsage(UsageInfo.estimate(prompt, content));
        return response;
    }
}
