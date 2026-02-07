package com.bmc.rag.store.service;

import com.bmc.rag.vectorization.embedding.LocalEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for hybrid search combining vector similarity and full-text search (P2.1).
 * Uses Reciprocal Rank Fusion (RRF) to combine results from both methods.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridSearchService {

    private final JdbcTemplate jdbcTemplate;
    private final LocalEmbeddingService embeddingService;

    @Value("${hybrid-search.vector-weight:0.7}")
    private float vectorWeight;

    @Value("${hybrid-search.text-weight:0.3}")
    private float textWeight;

    @Value("${hybrid-search.rrf-k:60}")
    private int rrfK;

    /**
     * Perform hybrid search combining vector and keyword search.
     *
     * @param query Search query text
     * @param maxResults Maximum results to return
     * @param minScore Minimum score threshold
     * @return List of search results
     */
    public List<HybridSearchResult> search(String query, int maxResults, float minScore) {
        log.debug("Hybrid search: query='{}', maxResults={}, minScore={}",
            truncateForLog(query), maxResults, minScore);

        float[] queryEmbedding = embeddingService.embed(query);
        String embeddingStr = formatEmbedding(queryEmbedding);

        String sql = """
            SELECT * FROM hybrid_search(
                ?, ?, ?, ?, ?, ?, ?
            )
            """;

        List<Map<String, Object>> results = jdbcTemplate.queryForList(
            sql, query, embeddingStr, maxResults, minScore, vectorWeight, textWeight, rrfK
        );

        List<HybridSearchResult> searchResults = results.stream()
            .map(this::mapToSearchResult)
            .collect(Collectors.toList());

        log.debug("Hybrid search returned {} results", searchResults.size());
        return searchResults;
    }

    /**
     * Perform hybrid search with ReBAC filtering.
     *
     * @param query Search query text
     * @param maxResults Maximum results to return
     * @param minScore Minimum score threshold
     * @param allowedGroups List of groups user belongs to
     * @return List of search results
     */
    public List<HybridSearchResult> searchWithGroups(
            String query,
            int maxResults,
            float minScore,
            List<String> allowedGroups) {

        log.debug("Hybrid search with groups: query='{}', groups={}",
            truncateForLog(query), allowedGroups);

        float[] queryEmbedding = embeddingService.embed(query);
        String embeddingStr = formatEmbedding(queryEmbedding);
        String groupsArray = formatPostgresArray(allowedGroups);

        String sql = """
            SELECT * FROM hybrid_search_with_groups(
                ?, ?, ?, ?, ?, ?, ?, ?
            )
            """;

        // Note: Parameter order matches hybrid_search_with_groups function:
        // query_text, query_embedding, allowed_groups, max_results, min_score, vector_weight, text_weight, k_rrf
        List<Map<String, Object>> results = jdbcTemplate.queryForList(
            sql, query, embeddingStr, groupsArray, maxResults, minScore, vectorWeight, textWeight, rrfK
        );

        return results.stream()
            .map(this::mapToSearchResult)
            .collect(Collectors.toList());
    }

    /**
     * Search for exact matches (like incident numbers) using keyword search.
     * Falls back to hybrid search if no exact match found.
     *
     * @param query Search query (e.g., "INC000012345")
     * @param maxResults Maximum results
     * @return List of results
     */
    public List<HybridSearchResult> searchExact(String query, int maxResults) {
        log.debug("Exact search for: {}", query);

        // First try exact source_id match
        String exactSql = """
            SELECT id, chunk_id, text_segment, source_type, source_id,
                   entry_id, chunk_type, sequence_number, metadata,
                   1.0::float as vector_score, 1.0::float as text_score, 1.0::float as hybrid_score
            FROM embedding_store
            WHERE source_id ILIKE ?
            LIMIT ?
            """;

        List<Map<String, Object>> exactResults = jdbcTemplate.queryForList(
            exactSql, "%" + query + "%", maxResults
        );

        if (!exactResults.isEmpty()) {
            log.debug("Found {} exact matches", exactResults.size());
            return exactResults.stream()
                .map(this::mapToSearchResult)
                .collect(Collectors.toList());
        }

        // Fall back to hybrid search
        log.debug("No exact matches, falling back to hybrid search");
        return search(query, maxResults, 0.3f);
    }

    /**
     * Set the HNSW ef_search parameter for the current session.
     * Higher values = better recall, slower search.
     */
    public void setEfSearch(int efSearch) {
        if (efSearch < 1 || efSearch > 1000) {
            throw new IllegalArgumentException("ef_search must be between 1 and 1000, got: " + efSearch);
        }
        jdbcTemplate.execute("SET hnsw.ef_search = " + efSearch);
        log.debug("Set hnsw.ef_search = {}", efSearch);
    }

    /**
     * Get recommended ef_search value based on recall requirements.
     */
    public int getRecommendedEfSearch(RecallLevel level) {
        return switch (level) {
            case LOW -> 40;       // ~90% recall, fastest
            case MEDIUM -> 64;    // ~95% recall, balanced
            case HIGH -> 100;     // ~98% recall, slower
            case MAXIMUM -> 200;  // ~99% recall, slowest
        };
    }

    private HybridSearchResult mapToSearchResult(Map<String, Object> row) {
        return HybridSearchResult.builder()
            .id((UUID) row.get("id"))
            .chunkId((String) row.get("chunk_id"))
            .textSegment((String) row.get("text_segment"))
            .sourceType((String) row.get("source_type"))
            .sourceId((String) row.get("source_id"))
            .entryId((String) row.get("entry_id"))
            .chunkType((String) row.get("chunk_type"))
            .sequenceNumber(row.get("sequence_number") != null ?
                ((Number) row.get("sequence_number")).intValue() : 0)
            .metadata(parseMetadata(row.get("metadata")))
            .vectorScore(row.get("vector_score") != null ?
                ((Number) row.get("vector_score")).floatValue() : 0f)
            .textScore(row.get("text_score") != null ?
                ((Number) row.get("text_score")).floatValue() : 0f)
            .hybridScore(row.get("hybrid_score") != null ?
                ((Number) row.get("hybrid_score")).floatValue() : 0f)
            .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseMetadata(Object metadataObj) {
        if (metadataObj == null) return new HashMap<>();
        if (metadataObj instanceof Map) return (Map<String, String>) metadataObj;
        return new HashMap<>();
    }

    private String formatEmbedding(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private String formatPostgresArray(List<String> values) {
        if (values == null || values.isEmpty()) return "{}";
        return "{" + values.stream()
            .map(v -> "\"" + v.replace("\"", "\"\"") + "\"")
            .collect(Collectors.joining(",")) + "}";
    }

    private String truncateForLog(String text) {
        if (text == null) return "";
        return text.length() > 100 ? text.substring(0, 100) + "..." : text;
    }

    /**
     * Recall level for HNSW search.
     */
    public enum RecallLevel {
        LOW,     // ~90% recall, fastest
        MEDIUM,  // ~95% recall, balanced
        HIGH,    // ~98% recall, slower
        MAXIMUM  // ~99% recall, slowest
    }

    /**
     * Hybrid search result DTO.
     */
    @lombok.Data
    @lombok.Builder
    public static class HybridSearchResult {
        private UUID id;
        private String chunkId;
        private String textSegment;
        private String sourceType;
        private String sourceId;
        private String entryId;
        private String chunkType;
        private Integer sequenceNumber;
        private Map<String, String> metadata;
        private float vectorScore;
        private float textScore;
        private float hybridScore;

        public String getSourceReference() {
            return String.format("%s %s", sourceType, sourceId);
        }
    }
}
