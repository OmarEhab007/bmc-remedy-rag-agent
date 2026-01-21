package com.bmc.rag.api.dto.toolserver;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic search response DTO for Tool Server endpoints.
 * Contains matched results with relevance scores.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolSearchResponse {

    /**
     * The original search query.
     */
    private String query;

    /**
     * Total number of results found.
     */
    private Integer totalResults;

    /**
     * Number of results in this response (may be limited).
     */
    private Integer returnedResults;

    /**
     * List of search results.
     */
    @Builder.Default
    private List<SearchResultItem> results = new ArrayList<>();

    /**
     * Message about the search (e.g., warnings about duplicates).
     */
    private String message;

    /**
     * Whether potential duplicates were found (for incident creation workflows).
     */
    @Builder.Default
    private Boolean hasPotentialDuplicates = false;

    /**
     * Search execution time in milliseconds.
     */
    private Long executionTimeMs;

    /**
     * Create an empty response.
     */
    public static ToolSearchResponse empty(String query) {
        return ToolSearchResponse.builder()
            .query(query)
            .totalResults(0)
            .returnedResults(0)
            .results(new ArrayList<>())
            .message("No results found matching your query.")
            .build();
    }

    /**
     * Create a response from a list of results.
     */
    public static ToolSearchResponse of(String query, List<SearchResultItem> results) {
        return ToolSearchResponse.builder()
            .query(query)
            .totalResults(results.size())
            .returnedResults(results.size())
            .results(results)
            .build();
    }
}
