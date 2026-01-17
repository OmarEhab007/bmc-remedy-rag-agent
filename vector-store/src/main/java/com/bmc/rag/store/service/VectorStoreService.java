package com.bmc.rag.store.service;

import com.bmc.rag.store.repository.EmbeddingRepository;
import com.bmc.rag.vectorization.chunking.TextChunk;
import com.bmc.rag.vectorization.embedding.LocalEmbeddingService;
import com.bmc.rag.vectorization.embedding.LocalEmbeddingService.EmbeddedChunk;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for vector storage operations.
 * Handles embedding storage, retrieval, and semantic search.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreService {

    private static final int BATCH_SIZE = 100;

    private final EmbeddingRepository embeddingRepository;
    private final LocalEmbeddingService embeddingService;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Store a single embedded chunk.
     */
    @Transactional
    public void store(EmbeddedChunk embeddedChunk) {
        TextChunk chunk = embeddedChunk.chunk();

        String embeddingStr = formatEmbedding(embeddedChunk.embedding());
        String metadataJson = formatMetadata(chunk.getMetadata());

        embeddingRepository.upsertWithEmbedding(
            UUID.randomUUID(),
            chunk.getChunkId(),
            embeddingStr,
            chunk.getContent(),
            chunk.getSourceType(),
            chunk.getSourceId(),
            chunk.getEntryId(),
            chunk.getChunkType() != null ? chunk.getChunkType().name() : null,
            chunk.getSequenceNumber(),
            metadataJson
        );

        log.debug("Stored embedding for chunk: {}", chunk.getChunkId());
    }

    /**
     * Store multiple embedded chunks in batch using true JDBC batch insert.
     * Uses ON CONFLICT for upsert semantics.
     */
    @Transactional
    public void storeBatch(List<EmbeddedChunk> embeddedChunks) {
        if (embeddedChunks == null || embeddedChunks.isEmpty()) {
            return;
        }

        log.info("Storing {} embedded chunks in batches of {}", embeddedChunks.size(), BATCH_SIZE);

        String sql = """
            INSERT INTO embedding_store (id, chunk_id, embedding, text_segment, source_type,
                source_id, entry_id, chunk_type, sequence_number, metadata)
            VALUES (?, ?, ?::vector, ?, ?, ?, ?, ?, ?, ?::jsonb)
            ON CONFLICT (chunk_id) DO UPDATE SET
                embedding = EXCLUDED.embedding,
                text_segment = EXCLUDED.text_segment,
                metadata = EXCLUDED.metadata,
                updated_at = NOW()
            """;

        // Process in batches
        int totalBatches = (embeddedChunks.size() + BATCH_SIZE - 1) / BATCH_SIZE;
        int processed = 0;

        for (int batch = 0; batch < totalBatches; batch++) {
            int start = batch * BATCH_SIZE;
            int end = Math.min(start + BATCH_SIZE, embeddedChunks.size());
            List<EmbeddedChunk> batchChunks = embeddedChunks.subList(start, end);

            jdbcTemplate.batchUpdate(sql, batchChunks, batchChunks.size(),
                (PreparedStatement ps, EmbeddedChunk ec) -> {
                    TextChunk chunk = ec.chunk();
                    ps.setObject(1, UUID.randomUUID());
                    ps.setString(2, chunk.getChunkId());
                    ps.setString(3, formatEmbedding(ec.embedding()));
                    ps.setString(4, chunk.getContent());
                    ps.setString(5, chunk.getSourceType());
                    ps.setString(6, chunk.getSourceId());
                    ps.setString(7, chunk.getEntryId());
                    if (chunk.getChunkType() != null) {
                        ps.setString(8, chunk.getChunkType().name());
                    } else {
                        ps.setNull(8, Types.VARCHAR);
                    }
                    ps.setInt(9, chunk.getSequenceNumber());
                    ps.setString(10, formatMetadata(chunk.getMetadata()));
                });

            processed += batchChunks.size();
            log.debug("Batch {}/{}: stored {} chunks (total: {})",
                batch + 1, totalBatches, batchChunks.size(), processed);
        }

        log.info("Stored {} chunks successfully in {} batches", embeddedChunks.size(), totalBatches);
    }

    /**
     * Delete all embeddings for a source record.
     */
    @Transactional
    public void deleteBySourceRecord(String sourceType, String sourceId) {
        embeddingRepository.deleteBySourceTypeAndSourceId(sourceType, sourceId);
        log.debug("Deleted embeddings for {} {}", sourceType, sourceId);
    }

    /**
     * Delete all embeddings for a source type.
     */
    @Transactional
    public void deleteBySourceType(String sourceType) {
        embeddingRepository.deleteBySourceType(sourceType);
        log.info("Deleted all embeddings for source type: {}", sourceType);
    }

    /**
     * Perform semantic search.
     *
     * @param query The search query
     * @param maxResults Maximum number of results
     * @param minScore Minimum similarity score (0-1)
     * @return List of search results
     */
    public List<SearchResult> search(String query, int maxResults, float minScore) {
        log.debug("Searching for query: '{}' with maxResults={}, minScore={}",
            query.length() > 100 ? query.substring(0, 100) + "..." : query, maxResults, minScore);

        float[] queryEmbedding = embeddingService.embed(query);
        String embeddingStr = formatEmbedding(queryEmbedding);

        List<Object[]> results = embeddingRepository.searchSimilar(embeddingStr, maxResults, minScore);
        List<SearchResult> searchResults = mapSearchResults(results);

        log.debug("Search returned {} results", searchResults.size());
        if (searchResults.isEmpty()) {
            long totalEmbeddings = embeddingRepository.count();
            log.warn("No search results found. Total embeddings in store: {}. " +
                    "If total is 0, run data sync first. If total > 0, query may not match any content above minScore={}",
                totalEmbeddings, minScore);
        }

        return searchResults;
    }

    /**
     * Perform semantic search with ReBAC filtering.
     *
     * @param query The search query
     * @param maxResults Maximum number of results
     * @param minScore Minimum similarity score
     * @param allowedGroups List of groups the user belongs to
     * @return List of search results
     */
    public List<SearchResult> searchWithGroups(
            String query,
            int maxResults,
            float minScore,
            List<String> allowedGroups) {

        log.debug("Searching with groups filter: query='{}', maxResults={}, minScore={}, groups={}",
            query.length() > 100 ? query.substring(0, 100) + "..." : query,
            maxResults, minScore, allowedGroups);

        float[] queryEmbedding = embeddingService.embed(query);
        String embeddingStr = formatEmbedding(queryEmbedding);
        String groupsArray = formatPostgresArray(allowedGroups);

        List<Object[]> results = embeddingRepository.searchSimilarWithGroups(
            embeddingStr, maxResults, minScore, groupsArray
        );
        List<SearchResult> searchResults = mapSearchResults(results);

        log.debug("Search with groups returned {} results", searchResults.size());
        if (searchResults.isEmpty()) {
            long totalEmbeddings = embeddingRepository.count();
            log.warn("No results with group filter. Total embeddings: {}. Groups: {}. " +
                    "Check if embeddings have assigned_group metadata matching user groups.",
                totalEmbeddings, allowedGroups);
        }

        return searchResults;
    }

    /**
     * Perform semantic search filtered by source types.
     *
     * @param query The search query
     * @param maxResults Maximum number of results
     * @param minScore Minimum similarity score
     * @param sourceTypes List of source types to include
     * @return List of search results
     */
    public List<SearchResult> searchBySourceTypes(
            String query,
            int maxResults,
            float minScore,
            List<String> sourceTypes) {

        float[] queryEmbedding = embeddingService.embed(query);
        String embeddingStr = formatEmbedding(queryEmbedding);
        String typesArray = formatPostgresArray(sourceTypes);

        List<Object[]> results = embeddingRepository.searchSimilarBySourceTypes(
            embeddingStr, maxResults, minScore, typesArray
        );
        return mapSearchResults(results);
    }

    /**
     * Perform semantic search filtered by a single source type.
     * Convenience method for agentic operations (Section 12).
     *
     * @param query The search query
     * @param sourceType Single source type to filter (e.g., "Incident", "WorkOrder")
     * @param maxResults Maximum number of results
     * @param minScore Minimum similarity score (0-1)
     * @return List of search results
     */
    public List<SearchResult> searchByType(String query, String sourceType, int maxResults, double minScore) {
        return searchBySourceTypes(query, maxResults, (float) minScore, List.of(sourceType));
    }

    /**
     * Get statistics about stored embeddings.
     */
    public Map<String, Long> getStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", embeddingRepository.count());
        stats.put("incidents", embeddingRepository.countBySourceType("Incident"));
        stats.put("workOrders", embeddingRepository.countBySourceType("WorkOrder"));
        stats.put("knowledgeArticles", embeddingRepository.countBySourceType("KnowledgeArticle"));
        stats.put("changeRequests", embeddingRepository.countBySourceType("ChangeRequest"));
        return stats;
    }

    /**
     * Get all source IDs grouped by source type.
     * Used for hard-delete detection (P1.2).
     */
    public Map<String, Set<String>> getAllSourceIdsByType() {
        Map<String, Set<String>> result = new HashMap<>();

        List<String> sourceTypes = List.of("Incident", "WorkOrder", "KnowledgeArticle", "ChangeRequest");

        for (String sourceType : sourceTypes) {
            List<String> ids = embeddingRepository.findDistinctSourceIdsBySourceType(sourceType);
            result.put(sourceType, new HashSet<>(ids));
        }

        return result;
    }

    /**
     * Check if embeddings exist for a source record.
     */
    public boolean existsForSourceRecord(String sourceType, String sourceId) {
        return !embeddingRepository.findBySourceTypeAndSourceId(sourceType, sourceId).isEmpty();
    }

    /**
     * Format embedding array as PostgreSQL vector string.
     */
    private String formatEmbedding(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Format metadata map as JSON string.
     */
    private String formatMetadata(Map<String, String> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata != null ? metadata : Map.of());
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize metadata: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * Format list as PostgreSQL array string.
     * PostgreSQL array literals use doubled double-quotes for escaping: {"value with ""quotes"""}
     */
    private String formatPostgresArray(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "{}";
        }
        return "{" + values.stream()
            .map(v -> "\"" + v.replace("\"", "\"\"") + "\"")
            .collect(Collectors.joining(",")) + "}";
    }

    /**
     * Map native query results to SearchResult objects.
     */
    @SuppressWarnings("unchecked")
    private List<SearchResult> mapSearchResults(List<Object[]> results) {
        return results.stream()
            .map(row -> SearchResult.builder()
                .id((UUID) row[0])
                .chunkId((String) row[1])
                .textSegment((String) row[2])
                .sourceType((String) row[3])
                .sourceId((String) row[4])
                .entryId((String) row[5])
                .chunkType((String) row[6])
                .sequenceNumber(row[7] != null ? ((Number) row[7]).intValue() : 0)
                .metadata(parseMetadata(row[8]))
                .score(row[11] != null ? ((Number) row[11]).floatValue() : 0f)
                .build())
            .toList();
    }

    /**
     * Parse JSONB metadata from database.
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> parseMetadata(Object metadataObj) {
        if (metadataObj == null) {
            return new HashMap<>();
        }
        if (metadataObj instanceof Map) {
            return (Map<String, String>) metadataObj;
        }
        try {
            return objectMapper.readValue(metadataObj.toString(), Map.class);
        } catch (Exception e) {
            log.warn("Failed to parse metadata: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Search result DTO.
     */
    @lombok.Data
    @lombok.Builder
    public static class SearchResult {
        private UUID id;
        private String chunkId;
        private String textSegment;
        private String sourceType;
        private String sourceId;
        private String entryId;
        private String chunkType;
        private Integer sequenceNumber;
        private Map<String, String> metadata;
        private float score;

        /**
         * Get a formatted source reference.
         */
        public String getSourceReference() {
            return String.format("%s %s", sourceType, sourceId);
        }
    }
}
