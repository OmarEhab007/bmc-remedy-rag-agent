package com.bmc.rag.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * WebSocket outbound message for streaming chat responses.
 * Each chunk contains either a token or completion metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseChunk {

    /**
     * Message ID correlating to the original query.
     */
    private String messageId;

    /**
     * Session ID for conversation context.
     */
    private String sessionId;

    /**
     * The token/text chunk for streaming (may be partial).
     */
    private String token;

    /**
     * Whether this is the final chunk of the response.
     */
    @Builder.Default
    private boolean isComplete = false;

    /**
     * Source citations (only included in completion message).
     */
    private List<Citation> citations;

    /**
     * Confidence score of the response (0.0 to 1.0).
     */
    private Double confidenceScore;

    /**
     * Error message if processing failed.
     */
    private String error;

    /**
     * Chunk type indicator.
     */
    @Builder.Default
    private ChunkType type = ChunkType.TOKEN;

    /**
     * Source citation details.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Citation {
        /**
         * Source type (INCIDENT, KNOWLEDGE, CHANGE, WORKORDER).
         */
        private String sourceType;

        /**
         * Source record ID (e.g., INC000001234).
         */
        private String sourceId;

        /**
         * Brief title or summary of the source.
         */
        private String title;

        /**
         * Relevance score (0.0 to 1.0).
         */
        private Double score;
    }

    /**
     * Type of response chunk.
     */
    public enum ChunkType {
        /**
         * Streaming token chunk.
         */
        TOKEN,

        /**
         * Connection acknowledgment.
         */
        CONNECTED,

        /**
         * Processing started indicator.
         */
        THINKING,

        /**
         * Final completion with metadata.
         */
        COMPLETE,

        /**
         * Error message.
         */
        ERROR
    }
}
