package com.bmc.rag.api.dto.toolserver;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Individual search result item.
 * Contains the matched record summary and relevance score.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultItem {

    /**
     * The record ID (e.g., INC000001, KB000001).
     */
    private String id;

    /**
     * The source type (Incident, KnowledgeArticle, WorkOrder).
     */
    private String type;

    /**
     * Title or summary of the record.
     */
    private String title;

    /**
     * Snippet of the matched content.
     */
    private String snippet;

    /**
     * Relevance score (0.0 to 1.0).
     */
    private Double score;

    /**
     * Score as percentage for display.
     */
    private Integer scorePercent;

    /**
     * Status of the record (if applicable).
     */
    private String status;

    /**
     * Additional metadata about the result.
     */
    private Map<String, String> metadata;

    /**
     * Get score as a display percentage.
     */
    public Integer getScorePercent() {
        if (score != null) {
            return (int) Math.round(score * 100);
        }
        return scorePercent;
    }
}
