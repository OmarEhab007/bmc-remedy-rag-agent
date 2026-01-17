package com.bmc.rag.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Search response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {

    /**
     * Number of results found.
     */
    private int resultCount;

    /**
     * Search results.
     */
    private List<SearchResultItem> results;

    /**
     * Individual search result item.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResultItem {
        private String sourceType;
        private String sourceId;
        private String chunkType;
        private String content;
        private String title;
        private String category;
        private float score;
        private Map<String, String> metadata;
    }
}
