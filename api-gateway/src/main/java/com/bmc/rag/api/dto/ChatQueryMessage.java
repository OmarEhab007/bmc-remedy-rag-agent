package com.bmc.rag.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * WebSocket inbound message for chat queries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatQueryMessage {

    /**
     * Unique message ID for tracking request/response correlation.
     */
    private String messageId;

    /**
     * The user's question or message text.
     */
    private String text;

    /**
     * Session ID for conversation continuity.
     */
    private String sessionId;

    /**
     * User ID for access control.
     */
    private String userId;

    /**
     * User's group memberships for ReBAC filtering.
     */
    private Set<String> userGroups;

    /**
     * ISO 8601 timestamp of when the message was sent.
     */
    private String timestamp;

    /**
     * Whether to skip RAG context retrieval.
     */
    @Builder.Default
    private boolean skipContext = false;
}
