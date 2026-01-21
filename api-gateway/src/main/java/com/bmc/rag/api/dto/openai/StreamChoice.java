package com.bmc.rag.api.dto.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAI-compatible streaming choice DTO.
 * Used in SSE streaming responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StreamChoice {

    /**
     * The index of this choice.
     */
    private int index;

    /**
     * The delta containing the new content.
     */
    private Delta delta;

    /**
     * The reason the model stopped generating (null until complete).
     */
    @JsonProperty("finish_reason")
    private String finishReason;

    /**
     * Delta object for streaming content.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Delta {
        /**
         * The role (only sent in first chunk).
         */
        private String role;

        /**
         * The content fragment.
         */
        private String content;
    }

    /**
     * Create a content delta chunk.
     */
    public static StreamChoice content(int index, String content) {
        return StreamChoice.builder()
            .index(index)
            .delta(Delta.builder().content(content).build())
            .build();
    }

    /**
     * Create a role delta chunk (first chunk).
     */
    public static StreamChoice role(int index, String role) {
        return StreamChoice.builder()
            .index(index)
            .delta(Delta.builder().role(role).build())
            .build();
    }

    /**
     * Create a finish chunk.
     */
    public static StreamChoice finish(int index, String finishReason) {
        return StreamChoice.builder()
            .index(index)
            .delta(Delta.builder().build())
            .finishReason(finishReason)
            .build();
    }
}
