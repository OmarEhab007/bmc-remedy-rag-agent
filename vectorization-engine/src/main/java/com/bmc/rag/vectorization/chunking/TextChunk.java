package com.bmc.rag.vectorization.chunking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a chunk of text prepared for embedding.
 * Contains the text content and associated metadata for retrieval.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextChunk {

    public enum ChunkType {
        SUMMARY,           // Record summary/title
        DESCRIPTION,       // Main description
        RESOLUTION,        // Incident resolution (high value)
        WORK_LOG,          // Work log entry
        IMPLEMENTATION,    // Change implementation plan
        ROLLBACK,          // Change rollback plan
        ARTICLE_CONTENT,   // Knowledge article content
        ATTACHMENT         // Extracted attachment text
    }

    /**
     * Unique identifier for this chunk.
     */
    private String chunkId;

    /**
     * The actual text content of this chunk.
     */
    private String content;

    /**
     * Type of chunk for prioritization and filtering.
     */
    private ChunkType chunkType;

    /**
     * Source record type (Incident, WorkOrder, KnowledgeArticle, ChangeRequest).
     */
    private String sourceType;

    /**
     * Source record ID (e.g., INC000000000001).
     */
    private String sourceId;

    /**
     * Entry ID in Remedy system.
     */
    private String entryId;

    /**
     * Additional metadata for filtering and context.
     */
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();

    /**
     * Chunk sequence number within the source record.
     */
    private int sequenceNumber;

    /**
     * Character offset in original text (for reference).
     */
    private int startOffset;

    /**
     * Get the content length.
     */
    public int getContentLength() {
        return content != null ? content.length() : 0;
    }

    /**
     * Add metadata key-value pair.
     */
    public TextChunk addMetadata(String key, String value) {
        if (value != null && !value.isEmpty()) {
            metadata.put(key, value);
        }
        return this;
    }

    /**
     * Build standard metadata for ITSM records.
     */
    public static Map<String, String> buildITSMMetadata(
            String sourceType,
            String sourceId,
            String title,
            String assignedGroup,
            String category,
            String status) {

        Map<String, String> metadata = new HashMap<>();
        metadata.put("source_type", sourceType);
        metadata.put("source_id", sourceId);

        if (title != null) {
            metadata.put("title", title);
        }
        if (assignedGroup != null) {
            metadata.put("assigned_group", assignedGroup);
        }
        if (category != null) {
            metadata.put("category", category);
        }
        if (status != null) {
            metadata.put("status", status);
        }

        return metadata;
    }

    /**
     * Create a chunk ID based on source and sequence.
     */
    public static String generateChunkId(String sourceType, String sourceId, ChunkType chunkType, int sequence) {
        return String.format("%s:%s:%s:%d",
            sourceType.toLowerCase(),
            sourceId,
            chunkType.name().toLowerCase(),
            sequence);
    }
}
