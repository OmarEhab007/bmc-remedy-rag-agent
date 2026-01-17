package com.bmc.rag.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Chat request DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    /**
     * Session ID for conversation continuity.
     * If not provided, a new session will be created.
     */
    private String sessionId;

    /**
     * The user's question or message.
     * Maximum length: 10000 characters (consistent with controller and retriever limits).
     */
    @NotBlank(message = "Question is required")
    @Size(max = 10000, message = "Question must not exceed 10000 characters")
    private String question;

    /**
     * User ID for access control (optional).
     */
    private String userId;

    /**
     * User's group memberships for ReBAC filtering (optional).
     */
    private Set<String> userGroups;

    /**
     * Whether to skip RAG and use direct LLM query (optional).
     */
    @Builder.Default
    private boolean skipContext = false;

    /**
     * Source types to search (optional, defaults to all).
     */
    private Set<String> sourceTypes;
}
