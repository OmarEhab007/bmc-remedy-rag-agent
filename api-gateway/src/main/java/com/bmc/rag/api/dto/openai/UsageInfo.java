package com.bmc.rag.api.dto.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAI-compatible token usage information DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UsageInfo {

    /**
     * Number of tokens in the prompt.
     */
    @JsonProperty("prompt_tokens")
    private int promptTokens;

    /**
     * Number of tokens in the generated completion.
     */
    @JsonProperty("completion_tokens")
    private int completionTokens;

    /**
     * Total tokens (prompt + completion).
     */
    @JsonProperty("total_tokens")
    private int totalTokens;

    /**
     * Estimate token count based on character count.
     * This is an approximation (roughly 4 characters per token for English).
     */
    public static UsageInfo estimate(String prompt, String completion) {
        int promptTokens = estimateTokens(prompt);
        int completionTokens = estimateTokens(completion);
        return UsageInfo.builder()
            .promptTokens(promptTokens)
            .completionTokens(completionTokens)
            .totalTokens(promptTokens + completionTokens)
            .build();
    }

    /**
     * Estimate tokens from character count.
     * Rough approximation: ~4 chars per token for English, ~2 chars for CJK/Arabic.
     */
    private static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // Simple estimation based on character count
        // This is a rough approximation - actual tokenization varies by model
        return Math.max(1, text.length() / 4);
    }
}
