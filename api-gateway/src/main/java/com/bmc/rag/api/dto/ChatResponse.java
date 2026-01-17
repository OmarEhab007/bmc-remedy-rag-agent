package com.bmc.rag.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Chat response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /**
     * Session ID for conversation continuity.
     */
    private String sessionId;

    /**
     * The assistant's response.
     */
    private String response;

    /**
     * Source references cited in the response.
     */
    private List<String> sources;

    /**
     * Whether context was used from the knowledge base.
     */
    private boolean hasContext;

    /**
     * Timestamp of the response.
     */
    private long timestamp;
}
