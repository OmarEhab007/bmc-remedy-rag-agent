package com.bmc.rag.connector.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Knowledge Article from RKM:KnowledgeArticleManager form.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeArticle implements ITSMRecord {

    private String entryId;
    private String articleId;
    private String title;
    private String content;
    private String articleSummary;
    private String keywords;
    private String articleType;
    private Integer status;
    private String statusDisplayValue;
    private String assignedGroup;
    private String author;
    private Integer versionNumber;
    private Instant createDate;
    private Instant lastModifiedDate;
    private String lastModifiedBy;
    private Instant publishedDate;
    private Instant expirationDate;
    private Integer viewCount;

    // Categorization
    private String categoryTier1;
    private String categoryTier2;
    private String categoryTier3;

    // Related attachments
    @Builder.Default
    private List<AttachmentInfo> attachments = new ArrayList<>();

    @Override
    public String getRecordType() {
        return "KnowledgeArticle";
    }

    @Override
    public String getRecordId() {
        return articleId;
    }

    @Override
    public String getTitle() {
        return title;
    }

    /**
     * Get full categorization as a path string.
     */
    public String getCategoryPath() {
        StringBuilder sb = new StringBuilder();
        if (categoryTier1 != null) {
            sb.append(categoryTier1);
            if (categoryTier2 != null) {
                sb.append(" > ").append(categoryTier2);
                if (categoryTier3 != null) {
                    sb.append(" > ").append(categoryTier3);
                }
            }
        }
        return sb.toString();
    }

    /**
     * Parse keywords into a list.
     */
    public List<String> getKeywordList() {
        List<String> keywordList = new ArrayList<>();
        if (keywords != null && !keywords.isEmpty()) {
            String[] parts = keywords.split("[,;\\s]+");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    keywordList.add(trimmed);
                }
            }
        }
        return keywordList;
    }

    /**
     * Check if the article is published.
     */
    public boolean isPublished() {
        return status != null && status == 2; // Published
    }

    /**
     * Check if the article is expired.
     */
    public boolean isExpired() {
        if (expirationDate == null) {
            return false;
        }
        return expirationDate.isBefore(Instant.now());
    }

    /**
     * Check if the article is active (published and not expired).
     */
    public boolean isActive() {
        return isPublished() && !isExpired();
    }

    /**
     * Get the combined text content for vectorization.
     * Includes title, summary, content, and keywords.
     */
    public String getCombinedContent() {
        StringBuilder sb = new StringBuilder();

        if (title != null) {
            sb.append("Title: ").append(title).append("\n\n");
        }

        if (articleSummary != null) {
            sb.append("Summary: ").append(articleSummary).append("\n\n");
        }

        if (content != null) {
            // Strip HTML tags if present
            String cleanContent = content.replaceAll("<[^>]*>", " ");
            cleanContent = cleanContent.replaceAll("\\s+", " ").trim();
            sb.append(cleanContent);
        }

        if (keywords != null) {
            sb.append("\n\nKeywords: ").append(keywords);
        }

        return sb.toString();
    }
}
