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
 * OpenAI-compatible streaming chat completion chunk DTO.
 * Used in SSE streaming responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StreamingChatCompletionResponse {

    /**
     * A unique identifier for this completion (same across all chunks).
     */
    private String id;

    /**
     * The object type, always "chat.completion.chunk" for streaming.
     */
    @Builder.Default
    private String object = "chat.completion.chunk";

    /**
     * Unix timestamp of when the completion was created.
     */
    private long created;

    /**
     * The model used for the completion.
     */
    private String model;

    /**
     * A list of streaming choices.
     */
    private List<StreamChoice> choices;

    /**
     * System fingerprint (optional).
     */
    private String system_fingerprint;

    /**
     * Create a streaming chunk with content.
     */
    public static StreamingChatCompletionResponse chunk(String id, String model, String content) {
        return StreamingChatCompletionResponse.builder()
            .id(id)
            .object("chat.completion.chunk")
            .created(System.currentTimeMillis() / 1000)
            .model(model)
            .choices(Collections.singletonList(StreamChoice.content(0, content)))
            .build();
    }

    /**
     * Create the first streaming chunk with role.
     */
    public static StreamingChatCompletionResponse first(String model) {
        String id = "chatcmpl-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        return StreamingChatCompletionResponse.builder()
            .id(id)
            .object("chat.completion.chunk")
            .created(System.currentTimeMillis() / 1000)
            .model(model)
            .choices(Collections.singletonList(StreamChoice.role(0, "assistant")))
            .build();
    }

    /**
     * Create a finish chunk.
     */
    public static StreamingChatCompletionResponse finish(String id, String model) {
        return StreamingChatCompletionResponse.builder()
            .id(id)
            .object("chat.completion.chunk")
            .created(System.currentTimeMillis() / 1000)
            .model(model)
            .choices(Collections.singletonList(StreamChoice.finish(0, "stop")))
            .build();
    }
}
