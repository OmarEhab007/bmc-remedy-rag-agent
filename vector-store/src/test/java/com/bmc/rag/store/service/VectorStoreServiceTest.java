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
}
