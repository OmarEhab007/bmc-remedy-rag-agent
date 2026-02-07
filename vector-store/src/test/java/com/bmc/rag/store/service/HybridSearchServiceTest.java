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
        assertEquals(0.95f, firstResult.getHybridScore());

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
}
