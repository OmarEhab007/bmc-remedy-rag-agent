package com.bmc.rag.api.dto.toolserver;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Full knowledge article details response for Tool Server.
 * Contains all relevant KB article information for AI tool consumption.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDetailResponse {

    /**
     * Article ID (e.g., KB000000001).
     */
    private String articleId;

    /**
     * Article title.
     */
    private String title;

    /**
     * Article summary/abstract.
     */
    private String summary;

    /**
     * Full article content (HTML stripped).
     */
    private String content;

    /**
     * Article keywords.
     */
    private List<String> keywords;

    /**
     * Article type.
     */
    private String articleType;

    /**
     * Current status.
     */
    private String status;

    /**
     * Category path (e.g., "Software > Microsoft > Office 365").
     */
    private String categoryPath;

    /**
     * Article author.
     */
    private String author;

    /**
     * Version number.
     */
    private Integer versionNumber;

    /**
     * View count.
     */
    private Integer viewCount;

    /**
     * When the article was created.
     */
    private Instant createDate;

    /**
     * When the article was published.
     */
    private Instant publishedDate;

    /**
     * When the article expires.
     */
    private Instant expirationDate;

    /**
     * Last modification timestamp.
     */
    private Instant lastModifiedDate;

    /**
     * Who last modified the article.
     */
    private String lastModifiedBy;

    /**
     * Assigned support group.
     */
    private String assignedGroup;

    /**
     * Related article IDs.
     */
    @Builder.Default
    private List<String> relatedArticles = new ArrayList<>();

    /**
     * Attachment information.
     */
    @Builder.Default
    private List<AttachmentItem> attachments = new ArrayList<>();

    /**
     * Whether the record was found.
     */
    @Builder.Default
    private Boolean found = true;

    /**
     * Error message if not found.
     */
    private String errorMessage;

    /**
     * Attachment information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentItem {
        private String name;
        private Long sizeBytes;
        private String contentType;
    }

    /**
     * Create a not-found response.
     */
    public static KnowledgeDetailResponse notFound(String articleId) {
        return KnowledgeDetailResponse.builder()
            .articleId(articleId)
            .found(false)
            .errorMessage("Knowledge article " + articleId + " not found")
            .build();
    }
}
