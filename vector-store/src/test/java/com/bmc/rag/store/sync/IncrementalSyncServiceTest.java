package com.bmc.rag.store.sync;

import com.bmc.rag.connector.config.RemedyConnectionConfig;
import com.bmc.rag.connector.extractor.*;
import com.bmc.rag.connector.model.*;
import com.bmc.rag.store.repository.SyncStateRepository;
import com.bmc.rag.store.service.VectorStoreService;
import com.bmc.rag.store.sync.IncrementalSyncService.SyncResult;
import com.bmc.rag.vectorization.chunking.*;
import com.bmc.rag.vectorization.embedding.LocalEmbeddingService;
import com.bmc.rag.vectorization.embedding.LocalEmbeddingService.EmbeddedChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for IncrementalSyncService.
 * Tests sync cycle, cursor management, and error handling.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IncrementalSyncServiceTest {

    @Mock
    private RemedyConnectionConfig remedyConfig;

    @Mock
    private SyncStateRepository syncStateRepository;

    @Mock
    private VectorStoreService vectorStoreService;

    @Mock
    private LocalEmbeddingService embeddingService;

    @Mock
    private IncidentExtractor incidentExtractor;

    @Mock
    private WorkOrderExtractor workOrderExtractor;

    @Mock
    private KnowledgeExtractor knowledgeExtractor;

    @Mock
    private ChangeRequestExtractor changeRequestExtractor;

    @Mock
    private WorkLogExtractor workLogExtractor;

    @Mock
    private IncidentChunkStrategy incidentChunkStrategy;

    @Mock
    private WorkOrderChunkStrategy workOrderChunkStrategy;

    @Mock
    private KnowledgeChunkStrategy knowledgeChunkStrategy;

    @Mock
    private ChangeRequestChunkStrategy changeRequestChunkStrategy;

    @InjectMocks
    private IncrementalSyncService syncService;

    private float[] mockEmbedding;

    @BeforeEach
    void setUp() {
        mockEmbedding = new float[384];
        Arrays.fill(mockEmbedding, 0.1f);

        // Default: Remedy is enabled
        when(remedyConfig.isEnabled()).thenReturn(true);
    }

    @Test
    void isRemedyEnabled_configEnabled_returnsTrue() {
        // Given
        when(remedyConfig.isEnabled()).thenReturn(true);

        // When
        boolean result = syncService.isRemedyEnabled();

        // Then
        assertTrue(result);
    }

    @Test
    void isRemedyEnabled_configDisabled_returnsFalse() {
        // Given
        when(remedyConfig.isEnabled()).thenReturn(false);

        // When
        boolean result = syncService.isRemedyEnabled();

        // Then
        assertFalse(result);
    }

    @Test
    void syncIncidents_remedyDisabled_returnsDisabledResult() {
        // Given
        when(remedyConfig.isEnabled()).thenReturn(false);

        // When
        SyncResult result = syncService.syncIncidents();

        // Then
        assertNotNull(result);
        assertEquals(0, result.recordsProcessed());
        assertEquals(0, result.chunksCreated());
        assertEquals("Remedy connection disabled", result.errorMessage());
        assertFalse(result.isSuccess());
    }

    @Test
    void syncIncidents_noModifiedRecords_returnsZero() {
        // Given
        when(syncStateRepository.tryAcquireLock("Incident")).thenReturn(1);
        when(syncStateRepository.getLastSyncTimestamp("Incident")).thenReturn(Optional.of(1000L));
        when(incidentExtractor.extractModifiedSince(1000L)).thenReturn(Collections.emptyList());

        // When
        SyncResult result = syncService.syncIncidents();

        // Then
        assertNotNull(result);
        assertEquals(0, result.recordsProcessed());
        assertEquals(0, result.chunksCreated());
        assertTrue(result.isSuccess());
        verify(syncStateRepository).releaseLock("Incident");
    }

    @Test
    void syncIncidents_withModifiedRecords_processesSuccessfully() {
        // Given
        when(syncStateRepository.tryAcquireLock("Incident")).thenReturn(1);
        when(syncStateRepository.getLastSyncTimestamp("Incident")).thenReturn(Optional.of(1000L));

        IncidentRecord incident = createMockIncident("INC000123");
        when(incidentExtractor.extractModifiedSince(1000L)).thenReturn(List.of(incident));
        when(workLogExtractor.batchExtractIncidentWorkLogs(anyList())).thenReturn(Map.of());

        TextChunk chunk = createMockChunk("chunk-1", "Content");
        when(incidentChunkStrategy.chunk(incident)).thenReturn(List.of(chunk));

        EmbeddedChunk embeddedChunk = new EmbeddedChunk(chunk, mockEmbedding);
        when(embeddingService.embedChunks(anyList())).thenReturn(List.of(embeddedChunk));

        // When
        SyncResult result = syncService.syncIncidents();

        // Then
        assertNotNull(result);
        assertEquals(1, result.recordsProcessed());
        assertEquals(1, result.chunksCreated());
        assertTrue(result.isSuccess());
        verify(vectorStoreService).deleteBySourceRecord("Incident", "INC000123");
        verify(vectorStoreService).storeBatch(anyList());
        verify(syncStateRepository).updateSyncCompleted(eq("Incident"), anyLong(), eq(1));
        verify(syncStateRepository).releaseLock("Incident");
    }

    @Test
    void syncIncidents_withWorkLogs_attachesWorkLogs() {
        // Given
        when(syncStateRepository.tryAcquireLock("Incident")).thenReturn(1);
        when(syncStateRepository.getLastSyncTimestamp("Incident")).thenReturn(Optional.of(1000L));

        IncidentRecord incident = createMockIncident("INC000123");
        when(incidentExtractor.extractModifiedSince(1000L)).thenReturn(List.of(incident));

        WorkLogEntry workLog = WorkLogEntry.builder()
            .workLogId("WL001")
            .detailedDescription("Updated resolution")
            .build();
        when(workLogExtractor.batchExtractIncidentWorkLogs(anyList()))
            .thenReturn(Map.of("INC000123", List.of(workLog)));

        TextChunk chunk = createMockChunk("chunk-1", "Content");
        when(incidentChunkStrategy.chunk(incident)).thenReturn(List.of(chunk));

        EmbeddedChunk embeddedChunk = new EmbeddedChunk(chunk, mockEmbedding);
        when(embeddingService.embedChunks(anyList())).thenReturn(List.of(embeddedChunk));

        // When
        syncService.syncIncidents();

        // Then
        verify(workLogExtractor).batchExtractIncidentWorkLogs(List.of("INC000123"));
        assertNotNull(incident.getWorkLogs());
        assertEquals(1, incident.getWorkLogs().size());
        assertEquals("WL001", incident.getWorkLogs().get(0).getWorkLogId());
    }

    @Test
    void syncIncidents_lockAlreadyHeld_skipsSync() {
        // Given
        when(syncStateRepository.tryAcquireLock("Incident")).thenReturn(0);  // Lock not acquired

        // When
        SyncResult result = syncService.syncIncidents();

        // Then
        assertNotNull(result);
        assertEquals(0, result.recordsProcessed());
        assertEquals("Sync already in progress for Incident", result.errorMessage());
        verify(incidentExtractor, never()).extractModifiedSince(anyLong());
        verify(syncStateRepository, never()).releaseLock("Incident");
    }

    @Test
    void syncIncidents_staleLock_releasesAndAcquires() {
        // Given
        when(syncStateRepository.hasStalelock("Incident", 60)).thenReturn(true);
        when(syncStateRepository.tryAcquireLock("Incident")).thenReturn(1);
        when(syncStateRepository.getLastSyncTimestamp("Incident")).thenReturn(Optional.of(1000L));
        when(incidentExtractor.extractModifiedSince(anyLong())).thenReturn(Collections.emptyList());

        // When
        syncService.syncIncidents();

        // Then
        verify(syncStateRepository).releaseStaleLocksNative(60);
        verify(syncStateRepository).tryAcquireLock("Incident");
    }

    @Test
    void syncIncidents_extractorThrowsException_marksAsFailed() {
        // Given
        when(syncStateRepository.tryAcquireLock("Incident")).thenReturn(1);
        when(syncStateRepository.getLastSyncTimestamp("Incident")).thenReturn(Optional.of(1000L));
        when(incidentExtractor.extractModifiedSince(anyLong()))
            .thenThrow(new RuntimeException("ARERR 93: Timeout"));

        // When
        SyncResult result = syncService.syncIncidents();

        // Then
        assertNotNull(result);
        assertEquals(0, result.recordsProcessed());
        assertFalse(result.isSuccess());
        assertEquals("ARERR 93: Timeout", result.errorMessage());
        verify(syncStateRepository).markSyncFailed("Incident", "ARERR 93: Timeout");
        verify(syncStateRepository).releaseLock("Incident");
    }

    @Test
    void syncIncidents_updatesCursorWithLatestTimestamp() {
        // Given
        when(syncStateRepository.tryAcquireLock("Incident")).thenReturn(1);
        when(syncStateRepository.getLastSyncTimestamp("Incident")).thenReturn(Optional.of(1000L));

        IncidentRecord incident1 = createMockIncidentWithTimestamp("INC001", 2000L);
        IncidentRecord incident2 = createMockIncidentWithTimestamp("INC002", 3000L);
        when(incidentExtractor.extractModifiedSince(1000L)).thenReturn(List.of(incident1, incident2));
        when(workLogExtractor.batchExtractIncidentWorkLogs(anyList())).thenReturn(Map.of());

        TextChunk chunk = createMockChunk("chunk-1", "Content");
        when(incidentChunkStrategy.chunk(any())).thenReturn(List.of(chunk));

        EmbeddedChunk embeddedChunk = new EmbeddedChunk(chunk, mockEmbedding);
        when(embeddingService.embedChunks(anyList())).thenReturn(List.of(embeddedChunk));

        // When
        syncService.syncIncidents();

        // Then
        verify(syncStateRepository).updateSyncCompleted("Incident", 3000L, 2);
    }

    @Test
    void syncWorkOrders_processesSuccessfully() {
        // Given
        when(syncStateRepository.tryAcquireLock("WorkOrder")).thenReturn(1);
        when(syncStateRepository.getLastSyncTimestamp("WorkOrder")).thenReturn(Optional.of(1000L));

        WorkOrderRecord workOrder = createMockWorkOrder("WO000123");
        when(workOrderExtractor.extractModifiedSince(1000L)).thenReturn(List.of(workOrder));
        when(workLogExtractor.batchExtractWorkOrderWorkLogs(anyList())).thenReturn(Map.of());

        TextChunk chunk = createMockChunk("chunk-1", "Work order content");
        when(workOrderChunkStrategy.chunk(workOrder)).thenReturn(List.of(chunk));

        EmbeddedChunk embeddedChunk = new EmbeddedChunk(chunk, mockEmbedding);
        when(embeddingService.embedChunks(anyList())).thenReturn(List.of(embeddedChunk));

        // When
        SyncResult result = syncService.syncWorkOrders();

        // Then
        assertEquals(1, result.recordsProcessed());
        assertEquals(1, result.chunksCreated());
        assertTrue(result.isSuccess());
        verify(vectorStoreService).deleteBySourceRecord("WorkOrder", "WO000123");
    }

    @Test
    void syncKnowledgeArticles_processesOnlyPublished() {
        // Given
        when(syncStateRepository.tryAcquireLock("KnowledgeArticle")).thenReturn(1);
        when(syncStateRepository.getLastSyncTimestamp("KnowledgeArticle")).thenReturn(Optional.of(1000L));

        KnowledgeArticle article = createMockKnowledgeArticle("KA000123");
        when(knowledgeExtractor.extractPublishedArticles(1000L)).thenReturn(List.of(article));

        TextChunk chunk = createMockChunk("chunk-1", "Article content");
        when(knowledgeChunkStrategy.chunk(article)).thenReturn(List.of(chunk));

        EmbeddedChunk embeddedChunk = new EmbeddedChunk(chunk, mockEmbedding);
        when(embeddingService.embedChunks(anyList())).thenReturn(List.of(embeddedChunk));

        // When
        SyncResult result = syncService.syncKnowledgeArticles();

        // Then
        assertEquals(1, result.recordsProcessed());
        assertTrue(result.isSuccess());
        verify(knowledgeExtractor).extractPublishedArticles(1000L);
    }

    @Test
    void syncChangeRequests_processesSuccessfully() {
        // Given
        when(syncStateRepository.tryAcquireLock("ChangeRequest")).thenReturn(1);
        when(syncStateRepository.getLastSyncTimestamp("ChangeRequest")).thenReturn(Optional.of(1000L));

        ChangeRequestRecord changeRequest = createMockChangeRequest("CHG000123");
        when(changeRequestExtractor.extractModifiedSince(1000L)).thenReturn(List.of(changeRequest));
        when(workLogExtractor.batchExtractChangeWorkLogs(anyList())).thenReturn(Map.of());

        TextChunk chunk = createMockChunk("chunk-1", "Change request content");
        when(changeRequestChunkStrategy.chunk(changeRequest)).thenReturn(List.of(chunk));

        EmbeddedChunk embeddedChunk = new EmbeddedChunk(chunk, mockEmbedding);
        when(embeddingService.embedChunks(anyList())).thenReturn(List.of(embeddedChunk));

        // When
        SyncResult result = syncService.syncChangeRequests();

        // Then
        assertEquals(1, result.recordsProcessed());
        assertTrue(result.isSuccess());
    }

    @Test
    void runIncrementalSync_remedyDisabled_skipsSync() {
        // Given
        when(remedyConfig.isEnabled()).thenReturn(false);

        // When
        syncService.runIncrementalSync();

        // Then
        verify(syncStateRepository, never()).isAnySyncRunning();
        verify(incidentExtractor, never()).extractModifiedSince(anyLong());
    }

    @Test
    void runIncrementalSync_syncAlreadyRunning_skipsSync() {
        // Given
        when(syncStateRepository.isAnySyncRunning()).thenReturn(true);

        // When
        syncService.runIncrementalSync();

        // Then
        verify(incidentExtractor, never()).extractModifiedSince(anyLong());
    }

    @Test
    void forceFullSync_deletesExistingData() {
        // Given
        when(syncStateRepository.tryAcquireLock("Incident")).thenReturn(1);
        when(syncStateRepository.getLastSyncTimestamp("Incident")).thenReturn(Optional.of(0L));
        when(incidentExtractor.extractModifiedSince(0L)).thenReturn(Collections.emptyList());

        // When
        syncService.forceFullSync("Incident");

        // Then
        verify(syncStateRepository).updateSyncCompleted("Incident", 0L, 0);
        verify(vectorStoreService).deleteBySourceType("Incident");
    }

    @Test
    void forceFullSync_unknownSourceType_throwsException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            syncService.forceFullSync("InvalidType");
        });
    }

    @Test
    void handleHardDeletes_remedyDisabled_returnsDisabledResult() {
        // Given
        when(remedyConfig.isEnabled()).thenReturn(false);

        // When
        SyncResult result = syncService.handleHardDeletes();

        // Then
        assertFalse(result.isSuccess());
        assertEquals("Remedy connection disabled", result.errorMessage());
    }

    @Test
    void handleHardDeletes_detectsDeletedRecords() {
        // Given
        Map<String, Set<String>> vectorStoreIds = Map.of(
            "Incident", Set.of("INC000001", "INC000002", "INC000003")
        );
        when(vectorStoreService.getAllSourceIdsByType()).thenReturn(vectorStoreIds);

        // Only INC000001 and INC000003 still exist in Remedy
        when(incidentExtractor.checkExistence(anyList()))
            .thenReturn(Set.of("INC000001", "INC000003"));

        // When
        SyncResult result = syncService.handleHardDeletes();

        // Then
        assertEquals(1, result.recordsProcessed());  // INC000002 was deleted
        assertTrue(result.isSuccess());
        verify(vectorStoreService).deleteBySourceRecord("Incident", "INC000002");
    }

    @Test
    void syncResult_isSuccess_trueWhenNoError() {
        // Given
        SyncResult result = new SyncResult(10, 50);

        // Then
        assertTrue(result.isSuccess());
    }

    @Test
    void syncResult_isSuccess_falseWhenHasError() {
        // Given
        SyncResult result = new SyncResult(0, 0, "Error occurred");

        // Then
        assertFalse(result.isSuccess());
    }

    /**
     * Helper: Create mock incident.
     */
    private IncidentRecord createMockIncident(String incidentNumber) {
        IncidentRecord incident = new IncidentRecord();
        incident.setIncidentNumber(incidentNumber);
        incident.setSummary("Test incident");
        incident.setLastModifiedDate(Instant.ofEpochSecond(2000L));
        incident.setWorkLogs(new ArrayList<>());
        return incident;
    }

    /**
     * Helper: Create mock incident with specific timestamp.
     */
    private IncidentRecord createMockIncidentWithTimestamp(String incidentNumber, long timestamp) {
        IncidentRecord incident = createMockIncident(incidentNumber);
        incident.setLastModifiedDate(Instant.ofEpochSecond(timestamp));
        return incident;
    }

    /**
     * Helper: Create mock work order.
     */
    private WorkOrderRecord createMockWorkOrder(String workOrderId) {
        WorkOrderRecord wo = new WorkOrderRecord();
        wo.setWorkOrderId(workOrderId);
        wo.setSummary("Test work order");
        wo.setLastModifiedDate(Instant.ofEpochSecond(2000L));
        wo.setWorkLogs(new ArrayList<>());
        return wo;
    }

    /**
     * Helper: Create mock knowledge article.
     */
    private KnowledgeArticle createMockKnowledgeArticle(String articleId) {
        KnowledgeArticle article = new KnowledgeArticle();
        article.setArticleId(articleId);
        article.setTitle("Test article");
        article.setLastModifiedDate(Instant.ofEpochSecond(2000L));
        return article;
    }

    /**
     * Helper: Create mock change request.
     */
    private ChangeRequestRecord createMockChangeRequest(String changeId) {
        ChangeRequestRecord change = new ChangeRequestRecord();
        change.setChangeId(changeId);
        change.setSummary("Test change");
        change.setLastModifiedDate(Instant.ofEpochSecond(2000L));
        change.setWorkLogs(new ArrayList<>());
        return change;
    }

    /**
     * Helper: Create mock text chunk.
     */
    private TextChunk createMockChunk(String chunkId, String content) {
        return TextChunk.builder()
            .chunkId(chunkId)
            .content(content)
            .sourceType("Incident")
            .sourceId("INC000123")
            .chunkType(TextChunk.ChunkType.DESCRIPTION)
            .sequenceNumber(1)
            .metadata(new HashMap<>())
            .build();
    }
}
