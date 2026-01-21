package com.bmc.rag.api.dto.toolserver;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Generic search request DTO for Tool Server endpoints.
 * Used for semantic search across incidents, knowledge articles, and work orders.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolSearchRequest {

    /**
     * The search query text for semantic search.
     */
    @NotBlank(message = "Query is required")
    @Size(max = 2000, message = "Query must not exceed 2000 characters")
    private String query;

    /**
     * Maximum number of results to return.
     */
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 50, message = "Limit must not exceed 50")
    @Builder.Default
    private Integer limit = 10;

    /**
     * Minimum similarity score threshold (0.0 to 1.0).
     */
    @Min(value = 0, message = "minScore must be at least 0")
    @Max(value = 1, message = "minScore must not exceed 1")
    @Builder.Default
    private Double minScore = 0.3;

    /**
     * Optional filters to apply to the search.
     * Keys: status, assignedGroup, category, priority, dateFrom, dateTo
     */
    private Map<String, String> filters;

    /**
     * Optional list of specific source types to search.
     * If null, searches default type for the endpoint.
     */
    private List<String> sourceTypes;
}
