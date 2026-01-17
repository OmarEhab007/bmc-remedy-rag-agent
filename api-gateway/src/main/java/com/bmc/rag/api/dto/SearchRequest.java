package com.bmc.rag.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Search request DTO for direct vector search.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {

    /**
     * The search query.
     * Maximum length: 10000 characters (consistent with controller limits).
     */
    @NotBlank(message = "Query is required")
    @Size(max = 10000, message = "Query must not exceed 10000 characters")
    private String query;

    /**
     * Maximum number of results to return.
     */
    @Positive(message = "maxResults must be positive")
    @Max(value = 100, message = "maxResults must not exceed 100")
    @Builder.Default
    private int maxResults = 5;

    /**
     * Minimum similarity score (0.0 to 1.0).
     */
    @Min(value = 0, message = "minScore must be at least 0")
    @Max(value = 1, message = "minScore must not exceed 1")
    @Builder.Default
    private float minScore = 0.7f;

    /**
     * Source types to search (optional, defaults to all).
     */
    private Set<String> sourceTypes;

    /**
     * User ID for access control (optional).
     */
    private String userId;

    /**
     * User's group memberships for ReBAC filtering (optional).
     */
    private Set<String> userGroups;
}
