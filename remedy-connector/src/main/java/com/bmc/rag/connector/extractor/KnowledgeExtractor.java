package com.bmc.rag.connector.extractor;

import com.bmc.arsys.api.*;
import com.bmc.rag.connector.connection.ThreadLocalARContext;
import com.bmc.rag.connector.config.RemedyConnectionConfig;
import com.bmc.rag.connector.model.KnowledgeArticle;
import com.bmc.rag.connector.util.FieldIdConstants;
import com.bmc.rag.connector.util.QualifierBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

/**
 * Extracts Knowledge Articles from RKM:KnowledgeArticleManager form.
 * Uses bulk retrieval with pagination to avoid timeouts.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeExtractor {

    private final ThreadLocalARContext arContext;
    private final RemedyConnectionConfig config;

    private static final String FORM_NAME = FieldIdConstants.KnowledgeArticle.FORM_NAME;

    // Fields to retrieve
    private static final int[] FIELD_IDS = {
        FieldIdConstants.REQUEST_ID,
        FieldIdConstants.KnowledgeArticle.ARTICLE_ID,
        FieldIdConstants.KnowledgeArticle.ARTICLE_TITLE,
        FieldIdConstants.KnowledgeArticle.ARTICLE_CONTENT,
        FieldIdConstants.KnowledgeArticle.ARTICLE_SUMMARY,
        FieldIdConstants.KnowledgeArticle.ARTICLE_KEYWORDS,
        FieldIdConstants.KnowledgeArticle.ARTICLE_TYPE,
        FieldIdConstants.STATUS,
        FieldIdConstants.KnowledgeArticle.ASSIGNED_GROUP,
        FieldIdConstants.KnowledgeArticle.AUTHOR,
        FieldIdConstants.KnowledgeArticle.VERSION_NUMBER,
        FieldIdConstants.CREATE_DATE,
        FieldIdConstants.LAST_MODIFIED_DATE,
        FieldIdConstants.LAST_MODIFIED_BY,
        FieldIdConstants.KnowledgeArticle.PUBLISHED_DATE,
        FieldIdConstants.KnowledgeArticle.EXPIRATION_DATE,
        FieldIdConstants.KnowledgeArticle.VIEW_COUNT,
        FieldIdConstants.KnowledgeArticle.CATEGORY_TIER_1,
        FieldIdConstants.KnowledgeArticle.CATEGORY_TIER_2,
        FieldIdConstants.KnowledgeArticle.CATEGORY_TIER_3
    };

    /**
     * Extract all knowledge articles modified since the given timestamp.
     *
     * @param lastSyncTimestamp Unix epoch timestamp (seconds)
     * @return List of knowledge articles
     */
    public List<KnowledgeArticle> extractModifiedSince(long lastSyncTimestamp) {
        String qualification = QualifierBuilder.incrementalSyncQualifier(lastSyncTimestamp);
        return extractWithQualification(qualification);
    }

    /**
     * Extract only published knowledge articles.
     *
     * @param lastSyncTimestamp Unix epoch timestamp (seconds)
     * @return List of published knowledge articles
     */
    public List<KnowledgeArticle> extractPublishedArticles(long lastSyncTimestamp) {
        QualifierBuilder builder = new QualifierBuilder()
            .equals(FieldIdConstants.STATUS, FieldIdConstants.StatusValues.KA_PUBLISHED);

        if (lastSyncTimestamp > 0) {
            builder.dateAfter(FieldIdConstants.LAST_MODIFIED_DATE, lastSyncTimestamp);
        }

        return extractWithQualification(builder.build());
    }

    /**
     * Extract knowledge articles matching a custom qualification.
     *
     * @param qualification Remedy qualification string (null for all records)
     * @return List of knowledge articles
     */
    public List<KnowledgeArticle> extractWithQualification(String qualification) {
        return arContext.executeWithRetry(ctx -> {
            List<KnowledgeArticle> allArticles = new ArrayList<>();
            int chunkSize = config.getChunkSize();
            int firstRetrieve = 0;
            boolean hasMore = true;



            OutputInteger numMatches = new OutputInteger();

            log.info("Starting knowledge article extraction with qualification: {}",
                qualification != null ? qualification : "(all records)");

            // Parse qualification string to QualifierInfo (BMC AR API requires this)
            QualifierInfo qualifierInfo = QualifierBuilder.parseQualification(ctx, FORM_NAME, qualification);

            while (hasMore) {
                log.debug("Retrieving articles: firstRetrieve={}, maxRetrieve={}",
                    firstRetrieve, chunkSize);

                List<Entry> entries = ctx.getListEntryObjects(
                    FORM_NAME,
                    qualifierInfo,
                    firstRetrieve,
                    chunkSize,
                    null,
                    FIELD_IDS,
                    false,
                    numMatches
                );

                if (entries == null || entries.isEmpty()) {
                    hasMore = false;
                } else {
                    log.debug("Retrieved {} entries", entries.size());

                    for (Entry entry : entries) {
                        try {
                            KnowledgeArticle article = mapEntryToArticle(entry);
                            allArticles.add(article);
                        } catch (Exception e) {
                            log.warn("Failed to map knowledge article entry: {}", e.getMessage());
                        }
                    }

                    if (entries.size() < chunkSize) {
                        hasMore = false;
                    } else {
                        firstRetrieve += chunkSize;
                    }
                }
            }

            log.info("Extracted {} total knowledge articles", allArticles.size());
            return allArticles;
        });
    }

    /**
     * Extract a single knowledge article by ID.
     *
     * @param articleId The article ID
     * @return The article, or empty optional if not found
     */
    public Optional<KnowledgeArticle> extractByArticleId(String articleId) {
        String qualification = new QualifierBuilder()
            .equals(FieldIdConstants.KnowledgeArticle.ARTICLE_ID, articleId)
            .build();

        List<KnowledgeArticle> results = extractWithQualification(qualification);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Search knowledge articles by keywords.
     *
     * @param searchTerm The search term
     * @return List of matching articles
     */
    public List<KnowledgeArticle> searchByKeywords(String searchTerm) {
        // Search in title, keywords, and content
        String qualification = new QualifierBuilder()
            .equals(FieldIdConstants.STATUS, FieldIdConstants.StatusValues.KA_PUBLISHED)
            .raw(String.format(
                "('%d' LIKE \"%%%s%%\" OR '%d' LIKE \"%%%s%%\" OR '%d' LIKE \"%%%s%%\")",
                FieldIdConstants.KnowledgeArticle.ARTICLE_TITLE, searchTerm,
                FieldIdConstants.KnowledgeArticle.ARTICLE_KEYWORDS, searchTerm,
                FieldIdConstants.KnowledgeArticle.ARTICLE_CONTENT, searchTerm
            ))
            .build();

        return extractWithQualification(qualification);
    }

    /**
     * Check which article IDs from the provided list still exist in Remedy.
     *
     * @param articleIds List of article IDs to check
     * @return Set of article IDs that still exist
     */
    public Set<String> checkExistence(List<String> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return Collections.emptySet();
        }

        return arContext.executeWithRetry(ctx -> {
            Set<String> existingIds = new HashSet<>();

            // Build OR qualification for all article IDs
            StringBuilder orQualification = new StringBuilder();
            for (int i = 0; i < articleIds.size(); i++) {
                if (i > 0) orQualification.append(" OR ");
                orQualification.append("'")
                    .append(FieldIdConstants.KnowledgeArticle.ARTICLE_ID)
                    .append("' = \"")
                    .append(articleIds.get(i).replace("\"", "\\\""))
                    .append("\"");
            }

            QualifierInfo qualifierInfo = QualifierBuilder.parseQualification(
                ctx, FORM_NAME, orQualification.toString());
            OutputInteger numMatches = new OutputInteger();

            // Only fetch the article ID field
            int[] fieldIds = { FieldIdConstants.KnowledgeArticle.ARTICLE_ID };

            List<Entry> entries = ctx.getListEntryObjects(
                FORM_NAME,
                qualifierInfo,
                0,
                articleIds.size(),
                null,
                fieldIds,
                false,
                numMatches
            );

            if (entries != null) {
                for (Entry entry : entries) {
                    Value value = entry.get(FieldIdConstants.KnowledgeArticle.ARTICLE_ID);
                    if (value != null && value.getValue() != null) {
                        existingIds.add(value.getValue().toString());
                    }
                }
            }

            return existingIds;
        });
    }

    /**
     * Map a Remedy Entry to a KnowledgeArticle.
     */
    private KnowledgeArticle mapEntryToArticle(Entry entry) {
        Map<Integer, Value> fieldValues = new HashMap<>();
        for (Map.Entry<Integer, Value> e : entry.entrySet()) {
            fieldValues.put(e.getKey(), e.getValue());
        }

        return KnowledgeArticle.builder()
            .entryId(entry.getEntryId())
            .articleId(getStringValue(fieldValues, FieldIdConstants.KnowledgeArticle.ARTICLE_ID))
            .title(getStringValue(fieldValues, FieldIdConstants.KnowledgeArticle.ARTICLE_TITLE))
            .content(getStringValue(fieldValues, FieldIdConstants.KnowledgeArticle.ARTICLE_CONTENT))
            .articleSummary(getStringValue(fieldValues, FieldIdConstants.KnowledgeArticle.ARTICLE_SUMMARY))
            .keywords(getStringValue(fieldValues, FieldIdConstants.KnowledgeArticle.ARTICLE_KEYWORDS))
            .articleType(getStringValue(fieldValues, FieldIdConstants.KnowledgeArticle.ARTICLE_TYPE))
            .status(getIntValue(fieldValues, FieldIdConstants.STATUS))
            .assignedGroup(getStringValue(fieldValues, FieldIdConstants.KnowledgeArticle.ASSIGNED_GROUP))
            .author(getStringValue(fieldValues, FieldIdConstants.KnowledgeArticle.AUTHOR))
            .versionNumber(getIntValue(fieldValues, FieldIdConstants.KnowledgeArticle.VERSION_NUMBER))
            .createDate(getInstantValue(fieldValues, FieldIdConstants.CREATE_DATE))
            .lastModifiedDate(getInstantValue(fieldValues, FieldIdConstants.LAST_MODIFIED_DATE))
            .lastModifiedBy(getStringValue(fieldValues, FieldIdConstants.LAST_MODIFIED_BY))
            .publishedDate(getInstantValue(fieldValues, FieldIdConstants.KnowledgeArticle.PUBLISHED_DATE))
            .expirationDate(getInstantValue(fieldValues, FieldIdConstants.KnowledgeArticle.EXPIRATION_DATE))
            .viewCount(getIntValue(fieldValues, FieldIdConstants.KnowledgeArticle.VIEW_COUNT))
            .categoryTier1(getStringValue(fieldValues, FieldIdConstants.KnowledgeArticle.CATEGORY_TIER_1))
            .categoryTier2(getStringValue(fieldValues, FieldIdConstants.KnowledgeArticle.CATEGORY_TIER_2))
            .categoryTier3(getStringValue(fieldValues, FieldIdConstants.KnowledgeArticle.CATEGORY_TIER_3))
            .build();
    }

    private String getStringValue(Map<Integer, Value> fields, int fieldId) {
        Value value = fields.get(fieldId);
        if (value == null || value.getValue() == null) {
            return null;
        }
        return value.getValue().toString();
    }

    private Integer getIntValue(Map<Integer, Value> fields, int fieldId) {
        Value value = fields.get(fieldId);
        if (value == null || value.getValue() == null) {
            return null;
        }
        try {
            if (value.getValue() instanceof Integer) {
                return (Integer) value.getValue();
            }
            return Integer.parseInt(value.getValue().toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Instant getInstantValue(Map<Integer, Value> fields, int fieldId) {
        Value value = fields.get(fieldId);
        if (value == null || value.getValue() == null) {
            return null;
        }
        try {
            if (value.getValue() instanceof Timestamp) {
                return Instant.ofEpochSecond(((Timestamp) value.getValue()).getValue());
            }
            long epochSeconds = Long.parseLong(value.getValue().toString());
            return Instant.ofEpochSecond(epochSeconds);
        } catch (Exception e) {
            return null;
        }
    }
}
