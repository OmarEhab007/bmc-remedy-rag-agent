package com.bmc.rag.store.service;

import com.bmc.rag.store.repository.EmbeddingRepository;
import com.bmc.rag.store.service.VectorStoreService.SearchResult;
import com.bmc.rag.vectorization.chunking.TextChunk;
import com.bmc.rag.vectorization.embedding.LocalEmbeddingService;
import com.bmc.rag.vectorization.embedding.LocalEmbeddingService.EmbeddedChunk;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Unit tests for VectorStoreService.
 */
@ExtendWith(MockitoExtension.class)
class VectorStoreServiceTest {

    @Mock(lenient = true)
    private EmbeddingRepository embeddingRepository;

    @Mock(lenient = true)
    private LocalEmbeddingService embeddingService;

    @Mock(lenient = true)
    private ObjectMapper objectMapper;

    @Mock(lenient = true)
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private VectorStoreService vectorStoreService;

    private float[] mockEmbedding;
    private TextChunk mockChunk;

    @BeforeEach
    void setUp() {
        // Create a 384-dimensional mock embedding
        mockEmbedding = new float[384];
        Arrays.fill(mockEmbedding, 0.1f);

        // Create a mock text chunk
        mockChunk = TextChunk.builder()
            .chunkId("chunk-123")
            .content("VPN authentication failed for user John")
            .sourceType("Incident")
            .sourceId("INC000123")
            .entryId("entry-1")
            .chunkType(TextChunk.ChunkType.DESCRIPTION)
            .sequenceNumber(1)
            .metadata(Map.of("assigned_group", "Network Support"))
            .build();
    }

    @Test
    void search_validQuery_returnsResults() throws Exception {
        // Given
        String query = "VPN connection issue";
        int maxResults = 10;
        float minScore = 0.7f;

        when(embeddingService.embed(query)).thenReturn(mockEmbedding);

        // Mock repository response
        List<Object[]> mockDbResults = createMockDbResults();
        when(embeddingRepository.searchSimilar(anyString(), eq(maxResults), eq(minScore)))
            .thenReturn(mockDbResults);

        // When
        List<SearchResult> results = vectorStoreService.search(query, maxResults, minScore);

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());

        // Verify first result
        SearchResult firstResult = results.get(0);
        assertEquals("INC000123", firstResult.getSourceId());
        assertEquals("Incident", firstResult.getSourceType());
        assertEquals("VPN authentication failed", firstResult.getTextSegment());
        assertEquals(0.92f, firstResult.getScore(), 0.001f);

        // Verify embedding service was called
        verify(embeddingService).embed(query);

        // Verify repository was called
        ArgumentCaptor<String> embeddingCaptor = ArgumentCaptor.forClass(String.class);
        verify(embeddingRepository).searchSimilar(embeddingCaptor.capture(), eq(maxResults), eq(minScore));

        // Verify embedding format
        String embeddingStr = embeddingCaptor.getValue();
        assertTrue(embeddingStr.startsWith("["));
        assertTrue(embeddingStr.endsWith("]"));
        assertTrue(embeddingStr.contains(","));
    }

    @Test
    void formatEmbedding_producesCorrectVectorString() {
        // Given - use reflection to test private method
        float[] embedding = new float[]{0.1f, 0.2f, 0.3f};

        // When - call through a public method that uses formatEmbedding
        when(embeddingService.embed(anyString())).thenReturn(embedding);
        when(embeddingRepository.searchSimilar(anyString(), anyInt(), anyFloat()))
            .thenReturn(Collections.emptyList());

        vectorStoreService.search("test", 1, 0.5f);

        // Then - capture the formatted embedding
        ArgumentCaptor<String> embeddingCaptor = ArgumentCaptor.forClass(String.class);
        verify(embeddingRepository).searchSimilar(embeddingCaptor.capture(), anyInt(), anyFloat());

        String formattedEmbedding = embeddingCaptor.getValue();
        assertEquals("[0.1,0.2,0.3]", formattedEmbedding);
    }

    @Test
    void formatPostgresArray_emptyList_returnsEmptyArray() {
        // Given
        List<String> emptyList = Collections.emptyList();

        // When - test through searchWithGroups
        when(embeddingService.embed(anyString())).thenReturn(mockEmbedding);
        when(embeddingRepository.searchSimilarWithGroups(anyString(), anyInt(), anyFloat(), anyString()))
            .thenReturn(Collections.emptyList());

        vectorStoreService.searchWithGroups("test", 10, 0.5f, emptyList);

        // Then - verify format
        ArgumentCaptor<String> arrayCaptor = ArgumentCaptor.forClass(String.class);
        verify(embeddingRepository).searchSimilarWithGroups(anyString(), anyInt(), anyFloat(), arrayCaptor.capture());

        assertEquals("{}", arrayCaptor.getValue());
    }

    @Test
    void formatPostgresArray_singleElement_returnsFormattedArray() {
        // Given
        List<String> singleList = List.of("Network Support");

        // When
        when(embeddingService.embed(anyString())).thenReturn(mockEmbedding);
        when(embeddingRepository.searchSimilarWithGroups(anyString(), anyInt(), anyFloat(), anyString()))
            .thenReturn(Collections.emptyList());

        vectorStoreService.searchWithGroups("test", 10, 0.5f, singleList);

        // Then
        ArgumentCaptor<String> arrayCaptor = ArgumentCaptor.forClass(String.class);
        verify(embeddingRepository).searchSimilarWithGroups(anyString(), anyInt(), anyFloat(), arrayCaptor.capture());

        assertEquals("{\"Network Support\"}", arrayCaptor.getValue());
    }

    @Test
    void formatPostgresArray_multipleElements_returnsFormattedArray() {
        // Given
        List<String> multipleList = List.of("Network Support", "Application Support");

        // When
        when(embeddingService.embed(anyString())).thenReturn(mockEmbedding);
        when(embeddingRepository.searchSimilarWithGroups(anyString(), anyInt(), anyFloat(), anyString()))
            .thenReturn(Collections.emptyList());

        vectorStoreService.searchWithGroups("test", 10, 0.5f, multipleList);

        // Then
        ArgumentCaptor<String> arrayCaptor = ArgumentCaptor.forClass(String.class);
        verify(embeddingRepository).searchSimilarWithGroups(anyString(), anyInt(), anyFloat(), arrayCaptor.capture());

        assertEquals("{\"Network Support\",\"Application Support\"}", arrayCaptor.getValue());
    }

    @Test
    void formatPostgresArray_elementWithQuotes_escapesQuotes() {
        // Given
        List<String> quotedList = List.of("Support \"VIP\" Team");

        // When
        when(embeddingService.embed(anyString())).thenReturn(mockEmbedding);
        when(embeddingRepository.searchSimilarWithGroups(anyString(), anyInt(), anyFloat(), anyString()))
            .thenReturn(Collections.emptyList());

        vectorStoreService.searchWithGroups("test", 10, 0.5f, quotedList);

        // Then
        ArgumentCaptor<String> arrayCaptor = ArgumentCaptor.forClass(String.class);
        verify(embeddingRepository).searchSimilarWithGroups(anyString(), anyInt(), anyFloat(), arrayCaptor.capture());

        // PostgreSQL escapes quotes by doubling them
        assertEquals("{\"Support \"\"VIP\"\" Team\"}", arrayCaptor.getValue());
    }

    @Test
    void formatPostgresArray_nullList_returnsEmptyArray() {
        // Given - null list
        List<String> nullList = null;

        // When
        when(embeddingService.embed(anyString())).thenReturn(mockEmbedding);
        when(embeddingRepository.searchSimilarWithGroups(anyString(), anyInt(), anyFloat(), anyString()))
            .thenReturn(Collections.emptyList());

        vectorStoreService.searchWithGroups("test", 10, 0.5f, nullList);

        // Then
        ArgumentCaptor<String> arrayCaptor = ArgumentCaptor.forClass(String.class);
        verify(embeddingRepository).searchSimilarWithGroups(anyString(), anyInt(), anyFloat(), arrayCaptor.capture());

        assertEquals("{}", arrayCaptor.getValue());
    }

    /**
     * Create mock database results.
     */
    private List<Object[]> createMockDbResults() {
        List<Object[]> results = new ArrayList<>();

        // First result - matching repository query structure
        // Based on searchSimilar query: id, chunk_id, text_segment, source_type, source_id,
        // entry_id, chunk_type, sequence_number, metadata, created_at, updated_at, score
        Object[] result1 = new Object[]{
            UUID.randomUUID(),              // 0: id
            "chunk-1",                      // 1: chunk_id
            "VPN authentication failed",    // 2: text_segment
            "Incident",                     // 3: source_type
            "INC000123",                    // 4: source_id
            "entry-1",                      // 5: entry_id
            "RESOLUTION",                   // 6: chunk_type
            1,                              // 7: sequence_number
            Map.of("assigned_group", "Network Support"), // 8: metadata
            null,                           // 9: created_at
            null,                           // 10: updated_at
            0.92f                           // 11: score
        };
        results.add(result1);

        // Second result
        Object[] result2 = new Object[]{
            UUID.randomUUID(),
            "chunk-2",
            "Network connectivity problem",
            "Incident",
            "INC000124",
            "entry-2",
            "DESCRIPTION",
            1,
            Map.of("assigned_group", "Network Support"),
            null,
            null,
            0.85f
        };
        results.add(result2);

        return results;
    }

    // ========== detectLanguage tests ==========

    @Test
    void detectLanguage_pureEnglish_returnsEn() {
        String result = ReflectionTestUtils.invokeMethod(vectorStoreService, "detectLanguage",
            "This is a test incident with VPN connection failure");
        assertEquals("en", result);
    }

    @Test
    void detectLanguage_pureArabic_returnsAr() {
        String result = ReflectionTestUtils.invokeMethod(vectorStoreService, "detectLanguage",
            "مشكلة في الاتصال بالشبكة الافتراضية");
        assertEquals("ar", result);
    }

    @Test
    void detectLanguage_mixedContent_returnsMixed() {
        // Arabic > 10% but < 50% → mixed
        String result = ReflectionTestUtils.invokeMethod(vectorStoreService, "detectLanguage",
            "VPN connection issue الشبكة الافتراضية problem");
        assertEquals("mixed", result);
    }

    @Test
    void detectLanguage_nullInput_returnsEn() {
        String result = ReflectionTestUtils.invokeMethod(vectorStoreService, "detectLanguage",
            (String) null);
        assertEquals("en", result);
    }

    @Test
    void detectLanguage_blankInput_returnsEn() {
        String result = ReflectionTestUtils.invokeMethod(vectorStoreService, "detectLanguage",
            "   ");
        assertEquals("en", result);
    }

    @Test
    void detectLanguage_numbersOnly_returnsEn() {
        String result = ReflectionTestUtils.invokeMethod(vectorStoreService, "detectLanguage",
            "12345 67890");
        assertEquals("en", result);
    }

    // ========== Edge case tests for enhanced coverage ==========

    @Test
    void search_emptyResults_returnsEmptyList() {
        // Given
        String query = "nonexistent query";
        when(embeddingService.embed(query)).thenReturn(mockEmbedding);
        when(embeddingRepository.searchSimilar(anyString(), anyInt(), anyFloat()))
            .thenReturn(Collections.emptyList());
        when(embeddingRepository.count()).thenReturn(100L);

        // When
        List<SearchResult> results = vectorStoreService.search(query, 10, 0.7f);

        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(embeddingRepository).count(); // Should log warning about no results
    }

    @Test
    void search_nullEmbedding_handlesGracefully() {
        // Given
        String query = "test query";
        when(embeddingService.embed(query)).thenReturn(null);

        // When & Then - should throw NullPointerException when trying to format null embedding
        assertThrows(NullPointerException.class, () -> {
            vectorStoreService.search(query, 10, 0.5f);
        });
    }

    @Test
    void storeBatch_emptyList_returnsImmediately() {
        // Given
        List<EmbeddedChunk> emptyList = Collections.emptyList();

        // When
        vectorStoreService.storeBatch(emptyList);

        // Then
        verify(jdbcTemplate, never()).batchUpdate(anyString(), anyList(), anyInt(), any());
    }

    @Test
    void storeBatch_nullList_returnsImmediately() {
        // Given
        List<EmbeddedChunk> nullList = null;

        // When
        vectorStoreService.storeBatch(nullList);

        // Then
        verify(jdbcTemplate, never()).batchUpdate(anyString(), anyList(), anyInt(), any());
    }

    @Test
    void storeBatch_largeBatch_processesinBatches() throws Exception {
        // Given - create 250 chunks (should be processed in 3 batches: 100, 100, 50)
        List<EmbeddedChunk> largeList = new ArrayList<>();
        for (int i = 0; i < 250; i++) {
            TextChunk chunk = TextChunk.builder()
                .chunkId("chunk-" + i)
                .content("Content " + i)
                .sourceType("Incident")
                .sourceId("INC" + i)
                .entryId("entry-" + i)
                .chunkType(TextChunk.ChunkType.DESCRIPTION)
                .sequenceNumber(i)
                .metadata(Map.of())
                .build();
            largeList.add(new EmbeddedChunk(chunk, mockEmbedding));
        }

        // Mock ObjectMapper for metadata serialization
        when(objectMapper.writeValueAsString(anyMap())).thenReturn("{}");

        // When
        vectorStoreService.storeBatch(largeList);

        // Then - should call batchUpdate 3 times (batches of 100, 100, 50)
        verify(jdbcTemplate, times(3)).batchUpdate(anyString(), anyList(), anyInt(), any());
    }

    @Test
    void deleteBySourceRecord_callsRepository() {
        // Given
        String sourceType = "Incident";
        String sourceId = "INC000123";

        // When
        vectorStoreService.deleteBySourceRecord(sourceType, sourceId);

        // Then
        verify(embeddingRepository).deleteBySourceTypeAndSourceId(sourceType, sourceId);
    }

    @Test
    void deleteBySourceType_callsRepository() {
        // Given
        String sourceType = "Incident";

        // When
        vectorStoreService.deleteBySourceType(sourceType);

        // Then
        verify(embeddingRepository).deleteBySourceType(sourceType);
    }

    @Test
    void searchWithGroups_emptyGroups_returnsFilteredResults() {
        // Given
        when(embeddingService.embed(anyString())).thenReturn(mockEmbedding);
        when(embeddingRepository.searchSimilarWithGroups(anyString(), anyInt(), anyFloat(), eq("{}")))
            .thenReturn(Collections.emptyList());

        // When
        List<SearchResult> results = vectorStoreService.searchWithGroups("test", 10, 0.5f, Collections.emptyList());

        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(embeddingRepository).searchSimilarWithGroups(anyString(), anyInt(), anyFloat(), eq("{}"));
    }

    @Test
    void searchBySourceTypes_multipleTypes_callsRepository() {
        // Given
        List<String> sourceTypes = List.of("Incident", "WorkOrder");
        when(embeddingService.embed(anyString())).thenReturn(mockEmbedding);
        when(embeddingRepository.searchSimilarBySourceTypes(anyString(), anyInt(), anyFloat(), anyString()))
            .thenReturn(Collections.emptyList());

        // When
        List<SearchResult> results = vectorStoreService.searchBySourceTypes("test", 10, 0.5f, sourceTypes);

        // Then
        assertNotNull(results);
        ArgumentCaptor<String> typesCaptor = ArgumentCaptor.forClass(String.class);
        verify(embeddingRepository).searchSimilarBySourceTypes(anyString(), anyInt(), anyFloat(), typesCaptor.capture());
        assertEquals("{\"Incident\",\"WorkOrder\"}", typesCaptor.getValue());
    }

    @Test
    void searchByType_singleType_callsSearchBySourceTypes() {
        // Given
        when(embeddingService.embed(anyString())).thenReturn(mockEmbedding);
        when(embeddingRepository.searchSimilarBySourceTypes(anyString(), anyInt(), anyFloat(), anyString()))
            .thenReturn(Collections.emptyList());

        // When
        List<SearchResult> results = vectorStoreService.searchByType("test", "Incident", 10, 0.8);

        // Then
        assertNotNull(results);
        verify(embeddingRepository).searchSimilarBySourceTypes(anyString(), eq(10), eq(0.8f), eq("{\"Incident\"}"));
    }

    @Test
    void getStatistics_returnsCorrectCounts() {
        // Given
        when(embeddingRepository.count()).thenReturn(1000L);
        when(embeddingRepository.countBySourceType("Incident")).thenReturn(500L);
        when(embeddingRepository.countBySourceType("WorkOrder")).thenReturn(300L);
        when(embeddingRepository.countBySourceType("KnowledgeArticle")).thenReturn(150L);
        when(embeddingRepository.countBySourceType("ChangeRequest")).thenReturn(50L);

        // When
        Map<String, Long> stats = vectorStoreService.getStatistics();

        // Then
        assertNotNull(stats);
        assertEquals(1000L, stats.get("total"));
        assertEquals(500L, stats.get("incidents"));
        assertEquals(300L, stats.get("workOrders"));
        assertEquals(150L, stats.get("knowledgeArticles"));
        assertEquals(50L, stats.get("changeRequests"));
    }

    @Test
    void getAllSourceIdsByType_returnsGroupedIds() {
        // Given
        when(embeddingRepository.findDistinctSourceIdsBySourceType("Incident"))
            .thenReturn(List.of("INC000001", "INC000002"));
        when(embeddingRepository.findDistinctSourceIdsBySourceType("WorkOrder"))
            .thenReturn(List.of("WO000001"));
        when(embeddingRepository.findDistinctSourceIdsBySourceType("KnowledgeArticle"))
            .thenReturn(Collections.emptyList());
        when(embeddingRepository.findDistinctSourceIdsBySourceType("ChangeRequest"))
            .thenReturn(Collections.emptyList());

        // When
        Map<String, Set<String>> result = vectorStoreService.getAllSourceIdsByType();

        // Then
        assertNotNull(result);
        assertEquals(4, result.size());
        assertEquals(2, result.get("Incident").size());
        assertTrue(result.get("Incident").contains("INC000001"));
        assertTrue(result.get("Incident").contains("INC000002"));
        assertEquals(1, result.get("WorkOrder").size());
        assertTrue(result.get("WorkOrder").contains("WO000001"));
    }

    @Test
    void existsForSourceRecord_existingRecord_returnsTrue() {
        // Given
        when(embeddingRepository.findBySourceTypeAndSourceId("Incident", "INC000123"))
            .thenReturn(List.of(new com.bmc.rag.store.entity.EmbeddingEntity()));

        // When
        boolean exists = vectorStoreService.existsForSourceRecord("Incident", "INC000123");

        // Then
        assertTrue(exists);
    }

    @Test
    void existsForSourceRecord_nonExistingRecord_returnsFalse() {
        // Given
        when(embeddingRepository.findBySourceTypeAndSourceId("Incident", "INC999999"))
            .thenReturn(Collections.emptyList());

        // When
        boolean exists = vectorStoreService.existsForSourceRecord("Incident", "INC999999");

        // Then
        assertFalse(exists);
    }

    @Test
    void formatMetadata_nullMetadata_returnsEmptyJson() throws Exception {
        // Given
        when(objectMapper.writeValueAsString(anyMap())).thenReturn("{}");

        // When - test through store method
        EmbeddedChunk chunk = new EmbeddedChunk(
            TextChunk.builder()
                .chunkId("test")
                .content("test")
                .sourceType("Incident")
                .sourceId("INC000001")
                .metadata(null)
                .build(),
            mockEmbedding
        );

        vectorStoreService.store(chunk);

        // Then
        verify(objectMapper).writeValueAsString(Map.of());
    }

    @Test
    void searchResult_getSourceReference_formatsCorrectly() {
        // Given
        SearchResult result = SearchResult.builder()
            .sourceType("Incident")
            .sourceId("INC000123")
            .build();

        // When
        String reference = result.getSourceReference();

        // Then
        assertEquals("Incident INC000123", reference);
    }
}
