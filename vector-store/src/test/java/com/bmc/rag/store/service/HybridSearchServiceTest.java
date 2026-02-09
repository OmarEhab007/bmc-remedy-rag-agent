package com.bmc.rag.store.service;

import com.bmc.rag.store.service.HybridSearchService.RecallLevel;
import com.bmc.rag.vectorization.embedding.LocalEmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HybridSearchService.
 */
@ExtendWith(MockitoExtension.class)
class HybridSearchServiceTest {

    @Mock(lenient = true)
    private JdbcTemplate jdbcTemplate;

    @Mock(lenient = true)
    private LocalEmbeddingService embeddingService;

    @InjectMocks
    private HybridSearchService hybridSearchService;

    private float[] mockEmbedding;

    @BeforeEach
    void setUp() {
        // Create a 384-dimensional mock embedding
        mockEmbedding = new float[384];
        Arrays.fill(mockEmbedding, 0.1f);

        // Set the @Value injected fields
        ReflectionTestUtils.setField(hybridSearchService, "vectorWeight", 0.7f);
        ReflectionTestUtils.setField(hybridSearchService, "textWeight", 0.3f);
        ReflectionTestUtils.setField(hybridSearchService, "rrfK", 60);
    }

    @Test
    void setEfSearch_validValue_executesSuccessfully() {
        // Given
        int efSearch = 100;

        // When
        hybridSearchService.setEfSearch(efSearch);

        // Then
        verify(jdbcTemplate).execute("SET hnsw.ef_search = 100");
    }

    @Test
    void setEfSearch_minimumBoundary_executesSuccessfully() {
        // Given
        int efSearch = 1;

        // When
        hybridSearchService.setEfSearch(efSearch);

        // Then
        verify(jdbcTemplate).execute("SET hnsw.ef_search = 1");
    }

    @Test
    void setEfSearch_maximumBoundary_executesSuccessfully() {
        // Given
        int efSearch = 1000;

        // When
        hybridSearchService.setEfSearch(efSearch);

        // Then
        verify(jdbcTemplate).execute("SET hnsw.ef_search = 1000");
    }

    @Test
    void setEfSearch_belowMinimum_throwsException() {
        // Given
        int efSearch = 0;

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> hybridSearchService.setEfSearch(efSearch)
        );

        assertTrue(exception.getMessage().contains("ef_search must be between 1 and 1000"));
        assertTrue(exception.getMessage().contains("got: 0"));
        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    void setEfSearch_aboveMaximum_throwsException() {
        // Given
        int efSearch = 1001;

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> hybridSearchService.setEfSearch(efSearch)
        );

        assertTrue(exception.getMessage().contains("ef_search must be between 1 and 1000"));
        assertTrue(exception.getMessage().contains("got: 1001"));
        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    void setEfSearch_negativeValue_throwsException() {
        // Given
        int efSearch = -1;

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> hybridSearchService.setEfSearch(efSearch)
        );

        assertTrue(exception.getMessage().contains("ef_search must be between 1 and 1000"));
        assertTrue(exception.getMessage().contains("got: -1"));
        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    void search_validQuery_returnsResults() {
        // Given
        String query = "VPN connection issue";
        int maxResults = 10;
        float minScore = 0.5f;

        when(embeddingService.embed(query)).thenReturn(mockEmbedding);

        // Mock database response
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> mockDbResults = createMockSearchResults();

        // Use doReturn to avoid strict stubbing issues with argument matchers
        doReturn(mockDbResults).when(jdbcTemplate)
            .queryForList(anyString(), anyString(), anyString(), anyInt(), anyFloat(), anyFloat(), anyFloat(), anyInt());

        // When
        List<HybridSearchService.HybridSearchResult> results = hybridSearchService.search(query, maxResults, minScore);

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());

        // Verify first result
        HybridSearchService.HybridSearchResult firstResult = results.get(0);
        assertEquals("INC000123", firstResult.getSourceId());
        assertEquals("Incident", firstResult.getSourceType());
        assertEquals("VPN authentication failed", firstResult.getTextSegment());
        assertEquals(0.95f, firstResult.getHybridScore(), 0.001f);

        // Verify embedding service was called
        verify(embeddingService).embed(query);

        // Verify JDBC template was called with correct parameters
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForList(
            sqlCaptor.capture(),
            eq(query),
            anyString(), // embedding string
            eq(maxResults),
            eq(minScore),
            anyFloat(), // vector weight
            anyFloat(), // text weight
            anyInt()    // rrf k
        );

        assertTrue(sqlCaptor.getValue().contains("hybrid_search"));
    }

    @Test
    void getRecommendedEfSearch_allLevels_returnsExpectedValues() {
        // Test LOW recall level
        assertEquals(40, hybridSearchService.getRecommendedEfSearch(RecallLevel.LOW));

        // Test MEDIUM recall level
        assertEquals(64, hybridSearchService.getRecommendedEfSearch(RecallLevel.MEDIUM));

        // Test HIGH recall level
        assertEquals(100, hybridSearchService.getRecommendedEfSearch(RecallLevel.HIGH));

        // Test MAXIMUM recall level
        assertEquals(200, hybridSearchService.getRecommendedEfSearch(RecallLevel.MAXIMUM));
    }

    /**
     * Create mock search results from database.
     */
    private List<Map<String, Object>> createMockSearchResults() {
        List<Map<String, Object>> results = new ArrayList<>();

        // First result
        Map<String, Object> result1 = new HashMap<>();
        result1.put("id", UUID.randomUUID());
        result1.put("chunk_id", "chunk-1");
        result1.put("text_segment", "VPN authentication failed");
        result1.put("source_type", "Incident");
        result1.put("source_id", "INC000123");
        result1.put("entry_id", "entry-1");
        result1.put("chunk_type", "RESOLUTION");
        result1.put("sequence_number", 1);
        result1.put("metadata", new HashMap<String, String>());
        result1.put("vector_score", 0.92f);
        result1.put("text_score", 0.98f);
        result1.put("hybrid_score", 0.95f);
        results.add(result1);

        // Second result
        Map<String, Object> result2 = new HashMap<>();
        result2.put("id", UUID.randomUUID());
        result2.put("chunk_id", "chunk-2");
        result2.put("text_segment", "Network connectivity problem");
        result2.put("source_type", "Incident");
        result2.put("source_id", "INC000124");
        result2.put("entry_id", "entry-2");
        result2.put("chunk_type", "DESCRIPTION");
        result2.put("sequence_number", 1);
        result2.put("metadata", new HashMap<String, String>());
        result2.put("vector_score", 0.85f);
        result2.put("text_score", 0.80f);
        result2.put("hybrid_score", 0.83f);
        results.add(result2);

        return results;
    }

    // ========== Edge case tests for enhanced coverage ==========

    @Test
    void search_emptyQuery_returnsResults() {
        // Given
        String query = "";
        when(embeddingService.embed(query)).thenReturn(mockEmbedding);
        when(jdbcTemplate.queryForList(anyString(), anyString(), anyString(), anyInt(), anyFloat(), anyFloat(), anyFloat(), anyInt()))
            .thenReturn(Collections.emptyList());

        // When
        var results = hybridSearchService.search(query, 10, 0.5f);

        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void search_keywordOnlyScenario_usesTextScoring() {
        // Given - mock result with high text score, low vector score
        String query = "INC000123";
        when(embeddingService.embed(query)).thenReturn(mockEmbedding);

        List<Map<String, Object>> mockResults = new ArrayList<>();
        Map<String, Object> result = new HashMap<>();
        result.put("id", UUID.randomUUID());
        result.put("chunk_id", "chunk-1");
        result.put("text_segment", "Incident INC000123");
        result.put("source_type", "Incident");
        result.put("source_id", "INC000123");
        result.put("entry_id", "entry-1");
        result.put("chunk_type", "DESCRIPTION");
        result.put("sequence_number", 1);
        result.put("metadata", new HashMap<String, String>());
        result.put("vector_score", 0.3f);  // Low vector score
        result.put("text_score", 0.95f);   // High text score
        result.put("hybrid_score", 0.75f); // Combined via RRF
        mockResults.add(result);

        doReturn(mockResults).when(jdbcTemplate)
            .queryForList(anyString(), anyString(), anyString(), anyInt(), anyFloat(), anyFloat(), anyFloat(), anyInt());

        // When
        var results = hybridSearchService.search(query, 10, 0.5f);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(0.95f, results.get(0).getTextScore(), 0.001f);
        assertEquals(0.3f, results.get(0).getVectorScore(), 0.001f);
    }

    @Test
    void search_semanticOnlyScenario_usesVectorScoring() {
        // Given - mock result with high vector score, low text score
        String query = "network connectivity problems";
        when(embeddingService.embed(query)).thenReturn(mockEmbedding);

        List<Map<String, Object>> mockResults = new ArrayList<>();
        Map<String, Object> result = new HashMap<>();
        result.put("id", UUID.randomUUID());
        result.put("chunk_id", "chunk-1");
        result.put("text_segment", "VPN authentication issue");
        result.put("source_type", "Incident");
        result.put("source_id", "INC000123");
        result.put("entry_id", "entry-1");
        result.put("chunk_type", "DESCRIPTION");
        result.put("sequence_number", 1);
        result.put("metadata", new HashMap<String, String>());
        result.put("vector_score", 0.92f);  // High vector score (semantic match)
        result.put("text_score", 0.4f);     // Low text score (no keyword match)
        result.put("hybrid_score", 0.85f);  // Combined via RRF
        mockResults.add(result);

        doReturn(mockResults).when(jdbcTemplate)
            .queryForList(anyString(), anyString(), anyString(), anyInt(), anyFloat(), anyFloat(), anyFloat(), anyInt());

        // When
        var results = hybridSearchService.search(query, 10, 0.5f);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(0.92f, results.get(0).getVectorScore(), 0.001f);
        assertEquals(0.4f, results.get(0).getTextScore(), 0.001f);
    }

    @Test
    void search_combinedScoring_balancesBothScores() {
        // Given - mock result with balanced scores
        String query = "VPN connection issue";
        when(embeddingService.embed(query)).thenReturn(mockEmbedding);

        List<Map<String, Object>> mockResults = new ArrayList<>();
        Map<String, Object> result = new HashMap<>();
        result.put("id", UUID.randomUUID());
        result.put("chunk_id", "chunk-1");
        result.put("text_segment", "VPN connection failure");
        result.put("source_type", "Incident");
        result.put("source_id", "INC000123");
        result.put("entry_id", "entry-1");
        result.put("chunk_type", "DESCRIPTION");
        result.put("sequence_number", 1);
        result.put("metadata", new HashMap<String, String>());
        result.put("vector_score", 0.85f);  // Good semantic match
        result.put("text_score", 0.80f);    // Good keyword match
        result.put("hybrid_score", 0.90f);  // High combined score
        mockResults.add(result);

        doReturn(mockResults).when(jdbcTemplate)
            .queryForList(anyString(), anyString(), anyString(), anyInt(), anyFloat(), anyFloat(), anyFloat(), anyInt());

        // When
        var results = hybridSearchService.search(query, 10, 0.5f);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(0.85f, results.get(0).getVectorScore(), 0.001f);
        assertEquals(0.80f, results.get(0).getTextScore(), 0.001f);
        assertEquals(0.90f, results.get(0).getHybridScore(), 0.001f);
    }

    @Test
    void searchWithGroups_emptyGroups_filtersCorrectly() {
        // Given
        String query = "test query";
        when(embeddingService.embed(query)).thenReturn(mockEmbedding);
        when(jdbcTemplate.queryForList(anyString(), anyString(), anyString(), eq("{}"), anyInt(), anyFloat(), anyFloat(), anyFloat(), anyInt()))
            .thenReturn(Collections.emptyList());

        // When
        var results = hybridSearchService.searchWithGroups(query, 10, 0.5f, Collections.emptyList());

        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(jdbcTemplate).queryForList(anyString(), anyString(), anyString(), eq("{}"), anyInt(), anyFloat(), anyFloat(), anyFloat(), anyInt());
    }

    @Test
    void searchWithGroups_multipleGroups_formatsArrayCorrectly() {
        // Given
        String query = "test query";
        List<String> groups = List.of("Network Support", "Application Support");
        when(embeddingService.embed(query)).thenReturn(mockEmbedding);
        when(jdbcTemplate.queryForList(anyString(), anyString(), anyString(), anyString(), anyInt(), anyFloat(), anyFloat(), anyFloat(), anyInt()))
            .thenReturn(Collections.emptyList());

        // When
        hybridSearchService.searchWithGroups(query, 10, 0.5f, groups);

        // Then
        ArgumentCaptor<String> groupsCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForList(anyString(), anyString(), anyString(), groupsCaptor.capture(), anyInt(), anyFloat(), anyFloat(), anyFloat(), anyInt());
        assertEquals("{\"Network Support\",\"Application Support\"}", groupsCaptor.getValue());
    }

    @Test
    void searchExact_findsExactMatch_returnsWithoutFallback() {
        // Given
        String query = "INC000123";
        List<Map<String, Object>> exactMatches = new ArrayList<>();
        Map<String, Object> match = new HashMap<>();
        match.put("id", UUID.randomUUID());
        match.put("chunk_id", "chunk-1");
        match.put("text_segment", "Incident INC000123");
        match.put("source_type", "Incident");
        match.put("source_id", "INC000123");
        match.put("entry_id", "entry-1");
        match.put("chunk_type", "DESCRIPTION");
        match.put("sequence_number", 1);
        match.put("metadata", new HashMap<String, String>());
        match.put("vector_score", 1.0f);
        match.put("text_score", 1.0f);
        match.put("hybrid_score", 1.0f);
        exactMatches.add(match);

        when(jdbcTemplate.queryForList(contains("ILIKE"), anyString(), anyInt()))
            .thenReturn(exactMatches);

        // When
        var results = hybridSearchService.searchExact(query, 10);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("INC000123", results.get(0).getSourceId());
        verify(embeddingService, never()).embed(anyString()); // Should not fall back to hybrid search
    }

    @Test
    void searchExact_noExactMatch_fallsBackToHybridSearch() {
        // Given
        String query = "nonexistent";
        when(jdbcTemplate.queryForList(contains("ILIKE"), anyString(), anyInt()))
            .thenReturn(Collections.emptyList());
        when(embeddingService.embed(query)).thenReturn(mockEmbedding);
        when(jdbcTemplate.queryForList(contains("hybrid_search"), anyString(), anyString(), anyInt(), anyFloat(), anyFloat(), anyFloat(), anyInt()))
            .thenReturn(Collections.emptyList());

        // When
        var results = hybridSearchService.searchExact(query, 10);

        // Then
        assertNotNull(results);
        verify(embeddingService).embed(query); // Should fall back to hybrid search
    }

    @Test
    void search_nullMetadata_handlesGracefully() {
        // Given
        String query = "test";
        when(embeddingService.embed(query)).thenReturn(mockEmbedding);

        List<Map<String, Object>> mockResults = new ArrayList<>();
        Map<String, Object> result = new HashMap<>();
        result.put("id", UUID.randomUUID());
        result.put("chunk_id", "chunk-1");
        result.put("text_segment", "test content");
        result.put("source_type", "Incident");
        result.put("source_id", "INC000123");
        result.put("entry_id", "entry-1");
        result.put("chunk_type", "DESCRIPTION");
        result.put("sequence_number", 1);
        result.put("metadata", null);  // Null metadata
        result.put("vector_score", 0.8f);
        result.put("text_score", 0.7f);
        result.put("hybrid_score", 0.75f);
        mockResults.add(result);

        doReturn(mockResults).when(jdbcTemplate)
            .queryForList(anyString(), anyString(), anyString(), anyInt(), anyFloat(), anyFloat(), anyFloat(), anyInt());

        // When
        var results = hybridSearchService.search(query, 10, 0.5f);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertNotNull(results.get(0).getMetadata());
        assertTrue(results.get(0).getMetadata().isEmpty());
    }

    @Test
    void formatEmbedding_correctlyFormatsFloatArray() {
        // Given
        float[] embedding = new float[]{0.1f, 0.2f, 0.3f};
        when(embeddingService.embed(anyString())).thenReturn(embedding);
        when(jdbcTemplate.queryForList(anyString(), anyString(), anyString(), anyInt(), anyFloat(), anyFloat(), anyFloat(), anyInt()))
            .thenReturn(Collections.emptyList());

        // When
        hybridSearchService.search("test", 10, 0.5f);

        // Then
        ArgumentCaptor<String> embeddingCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForList(anyString(), anyString(), embeddingCaptor.capture(), anyInt(), anyFloat(), anyFloat(), anyFloat(), anyInt());
        assertEquals("[0.1,0.2,0.3]", embeddingCaptor.getValue());
    }

    @Test
    void hybridSearchResult_getSourceReference_formatsCorrectly() {
        // Given
        var result = HybridSearchService.HybridSearchResult.builder()
            .sourceType("WorkOrder")
            .sourceId("WO000456")
            .build();

        // When
        String reference = result.getSourceReference();

        // Then
        assertEquals("WorkOrder WO000456", reference);
    }
}
