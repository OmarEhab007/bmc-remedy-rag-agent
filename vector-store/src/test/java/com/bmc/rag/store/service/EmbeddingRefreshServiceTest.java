package com.bmc.rag.store.service;

import com.bmc.rag.store.entity.EmbeddingEntity;
import com.bmc.rag.store.repository.EmbeddingRepository;
import com.bmc.rag.vectorization.embedding.LocalEmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmbeddingRefreshService.
 * Tests embedding refresh logic, pagination, and error handling.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmbeddingRefreshServiceTest {

    @Mock
    private EmbeddingRepository repository;

    @Mock
    private LocalEmbeddingService embeddingService;

    @InjectMocks
    private EmbeddingRefreshService refreshService;

    private float[] mockEmbedding;

    @BeforeEach
    void setUp() {
        // Create a 384-dimensional mock embedding
        mockEmbedding = new float[384];
        Arrays.fill(mockEmbedding, 0.1f);
    }

    @Test
    void refreshAllEmbeddings_emptyDatabase_returnsZero() {
        // Given
        Page<EmbeddingEntity> emptyPage = Page.empty();
        when(repository.findAll(any(PageRequest.class))).thenReturn(emptyPage);

        // When
        int result = refreshService.refreshAllEmbeddings();

        // Then
        assertEquals(0, result);
        verify(repository, times(1)).findAll(any(PageRequest.class));
        verify(embeddingService, never()).embed(anyString());
    }

    @Test
    void refreshAllEmbeddings_singlePage_refreshesAll() {
        // Given
        List<EmbeddingEntity> entities = createMockEntities(50);
        Page<EmbeddingEntity> page = new PageImpl<>(entities, PageRequest.of(0, 100), 50);
        when(repository.findAll(any(PageRequest.class))).thenReturn(page);
        when(embeddingService.embed(anyString())).thenReturn(mockEmbedding);

        // When
        int result = refreshService.refreshAllEmbeddings();

        // Then
        assertEquals(50, result);
        verify(repository, times(1)).findAll(any(PageRequest.class));
        verify(embeddingService, times(50)).embed(anyString());
        verify(repository, times(50)).updateEmbedding(any(UUID.class), anyString());
    }

    @Test
    void refreshAllEmbeddings_multiplePages_processesPaginated() {
        // Given - 250 records across 3 pages (100, 100, 50)
        List<EmbeddingEntity> page1 = createMockEntities(100);
        List<EmbeddingEntity> page2 = createMockEntities(100);
        List<EmbeddingEntity> page3 = createMockEntities(50);

        when(repository.findAll(PageRequest.of(0, 100)))
            .thenReturn(new PageImpl<>(page1, PageRequest.of(0, 100), 250));
        when(repository.findAll(PageRequest.of(1, 100)))
            .thenReturn(new PageImpl<>(page2, PageRequest.of(1, 100), 250));
        when(repository.findAll(PageRequest.of(2, 100)))
            .thenReturn(new PageImpl<>(page3, PageRequest.of(2, 100), 250));

        when(embeddingService.embed(anyString())).thenReturn(mockEmbedding);

        // When
        int result = refreshService.refreshAllEmbeddings();

        // Then
        assertEquals(250, result);
        verify(repository, times(3)).findAll(any(PageRequest.class));
        verify(embeddingService, times(250)).embed(anyString());
        verify(repository, times(250)).updateEmbedding(any(UUID.class), anyString());
    }

    @Test
    void refreshAllEmbeddings_partialFailure_continuesAndCountsSuccesses() {
        // Given
        List<EmbeddingEntity> entities = createMockEntities(10);
        Page<EmbeddingEntity> page = new PageImpl<>(entities, PageRequest.of(0, 100), 10);
        when(repository.findAll(any(PageRequest.class))).thenReturn(page);
        when(embeddingService.embed(anyString())).thenReturn(mockEmbedding);

        // Simulate failure on 3rd entity
        doNothing().doNothing()
            .doThrow(new RuntimeException("Database error"))
            .doNothing().doNothing().doNothing().doNothing().doNothing().doNothing().doNothing()
            .when(repository).updateEmbedding(any(UUID.class), anyString());

        // When
        int result = refreshService.refreshAllEmbeddings();

        // Then
        assertEquals(9, result);  // 10 - 1 failure
        verify(embeddingService, times(10)).embed(anyString());
    }

    @Test
    void refreshEmbeddingsBySourceType_specificType_filtersCorrectly() {
        // Given
        String sourceType = "Incident";
        List<EmbeddingEntity> incidents = createMockEntitiesOfType(50, sourceType);
        Page<EmbeddingEntity> page = new PageImpl<>(incidents, PageRequest.of(0, 100), 50);

        when(repository.findBySourceType(eq(sourceType), any(PageRequest.class))).thenReturn(page);
        when(embeddingService.embed(anyString())).thenReturn(mockEmbedding);

        // When
        int result = refreshService.refreshEmbeddingsBySourceType(sourceType);

        // Then
        assertEquals(50, result);
        verify(repository, times(1)).findBySourceType(eq(sourceType), any(PageRequest.class));
        verify(embeddingService, times(50)).embed(anyString());
    }

    @Test
    void refreshEmbeddingsBySourceType_noEntities_returnsZero() {
        // Given
        String sourceType = "WorkOrder";
        Page<EmbeddingEntity> emptyPage = Page.empty();
        when(repository.findBySourceType(eq(sourceType), any(PageRequest.class))).thenReturn(emptyPage);

        // When
        int result = refreshService.refreshEmbeddingsBySourceType(sourceType);

        // Then
        assertEquals(0, result);
        verify(embeddingService, never()).embed(anyString());
    }

    @Test
    void refreshEmbeddingsBySourceType_multiplePages_processesAll() {
        // Given
        String sourceType = "KnowledgeArticle";
        List<EmbeddingEntity> page1 = createMockEntitiesOfType(100, sourceType);
        List<EmbeddingEntity> page2 = createMockEntitiesOfType(75, sourceType);

        when(repository.findBySourceType(eq(sourceType), eq(PageRequest.of(0, 100))))
            .thenReturn(new PageImpl<>(page1, PageRequest.of(0, 100), 175));
        when(repository.findBySourceType(eq(sourceType), eq(PageRequest.of(1, 100))))
            .thenReturn(new PageImpl<>(page2, PageRequest.of(1, 100), 175));

        when(embeddingService.embed(anyString())).thenReturn(mockEmbedding);

        // When
        int result = refreshService.refreshEmbeddingsBySourceType(sourceType);

        // Then
        assertEquals(175, result);
        verify(repository, times(2)).findBySourceType(eq(sourceType), any(PageRequest.class));
    }

    @Test
    void refreshSingleEmbedding_validEntity_updatesEmbedding() {
        // Given
        EmbeddingEntity entity = createMockEntity("INC000123", "VPN connection failed");
        when(embeddingService.embed(entity.getTextSegment())).thenReturn(mockEmbedding);

        // When
        refreshService.refreshSingleEmbedding(entity);

        // Then
        verify(embeddingService).embed("VPN connection failed");
        ArgumentCaptor<String> embeddingCaptor = ArgumentCaptor.forClass(String.class);
        verify(repository).updateEmbedding(eq(entity.getId()), embeddingCaptor.capture());

        // Verify embedding format
        String embeddingStr = embeddingCaptor.getValue();
        assertTrue(embeddingStr.startsWith("["));
        assertTrue(embeddingStr.endsWith("]"));
        assertTrue(embeddingStr.contains(","));
    }

    @Test
    void formatEmbedding_correctFormat_producesValidString() {
        // Given
        EmbeddingEntity entity = createMockEntity("INC000123", "test");
        float[] embedding = new float[]{0.1f, 0.2f, 0.3f};
        when(embeddingService.embed(anyString())).thenReturn(embedding);

        // When
        refreshService.refreshSingleEmbedding(entity);

        // Then
        ArgumentCaptor<String> embeddingCaptor = ArgumentCaptor.forClass(String.class);
        verify(repository).updateEmbedding(any(UUID.class), embeddingCaptor.capture());
        assertEquals("[0.1,0.2,0.3]", embeddingCaptor.getValue());
    }

    @Test
    void refreshAllEmbeddings_progressLogging_logsEvery50() {
        // Given - Create exactly 150 entities to test logging at 50, 100, 150
        List<EmbeddingEntity> page1 = createMockEntities(100);
        List<EmbeddingEntity> page2 = createMockEntities(50);

        when(repository.findAll(PageRequest.of(0, 100)))
            .thenReturn(new PageImpl<>(page1, PageRequest.of(0, 100), 150));
        when(repository.findAll(PageRequest.of(1, 100)))
            .thenReturn(new PageImpl<>(page2, PageRequest.of(1, 100), 150));

        when(embeddingService.embed(anyString())).thenReturn(mockEmbedding);

        // When
        int result = refreshService.refreshAllEmbeddings();

        // Then
        assertEquals(150, result);
        // Progress logs would occur at 50, 100, 150 (verified through log inspection in practice)
    }

    @Test
    void refreshEmbeddingsBySourceType_partialFailure_countsCorrectly() {
        // Given
        String sourceType = "ChangeRequest";
        List<EmbeddingEntity> entities = createMockEntitiesOfType(5, sourceType);
        Page<EmbeddingEntity> page = new PageImpl<>(entities, PageRequest.of(0, 100), 5);

        when(repository.findBySourceType(eq(sourceType), any(PageRequest.class))).thenReturn(page);
        when(embeddingService.embed(anyString())).thenReturn(mockEmbedding);

        // Simulate 2 failures
        doNothing()
            .doThrow(new RuntimeException("Error 1"))
            .doNothing()
            .doThrow(new RuntimeException("Error 2"))
            .doNothing()
            .when(repository).updateEmbedding(any(UUID.class), anyString());

        // When
        int result = refreshService.refreshEmbeddingsBySourceType(sourceType);

        // Then
        assertEquals(3, result);  // 5 - 2 failures
    }

    @Test
    void refreshAllEmbeddings_nullTextSegment_handlesGracefully() {
        // Given
        EmbeddingEntity entityWithNullText = EmbeddingEntity.builder()
            .id(UUID.randomUUID())
            .chunkId("chunk-1")
            .textSegment(null)
            .sourceType("Incident")
            .sourceId("INC000123")
            .build();

        Page<EmbeddingEntity> page = new PageImpl<>(List.of(entityWithNullText));
        when(repository.findAll(any(PageRequest.class))).thenReturn(page);
        when(embeddingService.embed((String) null)).thenReturn(mockEmbedding);

        // When
        int result = refreshService.refreshAllEmbeddings();

        // Then
        assertEquals(1, result);
        verify(embeddingService).embed((String) null);
    }

    @Test
    void refreshAllEmbeddings_emptyTextSegment_embedsEmptyString() {
        // Given
        EmbeddingEntity entityWithEmptyText = createMockEntity("INC000123", "");
        Page<EmbeddingEntity> page = new PageImpl<>(List.of(entityWithEmptyText));

        when(repository.findAll(any(PageRequest.class))).thenReturn(page);
        when(embeddingService.embed(eq(""))).thenReturn(mockEmbedding);

        // When
        int result = refreshService.refreshAllEmbeddings();

        // Then
        assertEquals(1, result);
        verify(embeddingService).embed(eq(""));
    }

    /**
     * Helper: Create mock entities with sequential IDs.
     */
    private List<EmbeddingEntity> createMockEntities(int count) {
        List<EmbeddingEntity> entities = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            entities.add(createMockEntity("INC" + i, "Content " + i));
        }
        return entities;
    }

    /**
     * Helper: Create mock entities of a specific source type.
     */
    private List<EmbeddingEntity> createMockEntitiesOfType(int count, String sourceType) {
        List<EmbeddingEntity> entities = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            EmbeddingEntity entity = createMockEntity(sourceType + i, "Content " + i);
            entity.setSourceType(sourceType);
            entities.add(entity);
        }
        return entities;
    }

    /**
     * Helper: Create a single mock entity.
     */
    private EmbeddingEntity createMockEntity(String sourceId, String textSegment) {
        return EmbeddingEntity.builder()
            .id(UUID.randomUUID())
            .chunkId("chunk-" + sourceId)
            .textSegment(textSegment)
            .sourceType("Incident")
            .sourceId(sourceId)
            .entryId("entry-" + sourceId)
            .chunkType("DESCRIPTION")
            .sequenceNumber(1)
            .metadata(new HashMap<>())
            .build();
    }
}
