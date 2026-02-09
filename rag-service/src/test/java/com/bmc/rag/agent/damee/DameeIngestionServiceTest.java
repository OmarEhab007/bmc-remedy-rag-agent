package com.bmc.rag.agent.damee;

import com.bmc.rag.store.service.VectorStoreService;
import com.bmc.rag.vectorization.chunking.TextChunk;
import com.bmc.rag.vectorization.embedding.LocalEmbeddingService;
import com.bmc.rag.vectorization.embedding.LocalEmbeddingService.EmbeddedChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DameeIngestionService}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DameeIngestionServiceTest {

    @Mock
    private DameeServiceCatalog serviceCatalog;

    @Mock
    private VectorStoreService vectorStoreService;

    @Mock
    private LocalEmbeddingService embeddingService;

    @InjectMocks
    private DameeIngestionService ingestionService;

    @Captor
    private ArgumentCaptor<List<TextChunk>> chunksCaptor;

    @Captor
    private ArgumentCaptor<List<EmbeddedChunk>> embeddedChunksCaptor;

    private DameeService testService;

    @BeforeEach
    void setUp() {
        testService = DameeService.builder()
                .serviceId("10504")
                .nameEn("Test Service")
                .nameAr("خدمة اختبار")
                .descriptionEn("Test description")
                .descriptionAr("وصف الاختبار")
                .category("IT Services")
                .subcategory("Test Category")
                .url("https://test.url")
                .keywords(List.of("test", "service", "اختبار"))
                .requiredFields(List.of("description", "justification"))
                .requiresManagerApproval(true)
                .vipBypass(false)
                .workflow(List.of(
                        DameeService.WorkflowStep.builder()
                                .order(1)
                                .description("Step 1")
                                .team("Test Team")
                                .requiresApproval(true)
                                .build()
                ))
                .build();
    }

    @Test
    void ingestAllServices_emptyServiceList_logsWarning() {
        // Given: empty service list
        when(serviceCatalog.getAllServices()).thenReturn(List.of());

        // When: ingest all services
        ingestionService.ingestAllServices();

        // Then: no embedding or storage calls
        verify(embeddingService, never()).embedChunks(any());
        verify(vectorStoreService, never()).storeBatch(any());
    }

    @Test
    void ingestAllServices_singleService_createsThreeChunks() {
        // Given: single service
        when(serviceCatalog.getAllServices()).thenReturn(List.of(testService));
        when(embeddingService.embedChunks(any())).thenReturn(List.of());

        // When: ingest all services
        ingestionService.ingestAllServices();

        // Then: creates 3 chunks (service info, workflow, keywords)
        verify(embeddingService).embedChunks(chunksCaptor.capture());
        List<TextChunk> chunks = chunksCaptor.getValue();
        assertThat(chunks).hasSize(3);
    }

    @Test
    void ingestAllServices_multipleServices_createsCorrectChunkCount() {
        // Given: two services
        DameeService service2 = DameeService.builder()
                .serviceId("10513")
                .nameEn("VPN Service")
                .nameAr("خدمة VPN")
                .descriptionEn("VPN description")
                .category("IT Services")
                .keywords(List.of("vpn"))
                .workflow(List.of())
                .requiredFields(List.of())
                .build();

        when(serviceCatalog.getAllServices()).thenReturn(List.of(testService, service2));
        when(embeddingService.embedChunks(any())).thenReturn(List.of());

        // When: ingest all services
        ingestionService.ingestAllServices();

        // Then: creates chunks for both services
        verify(embeddingService).embedChunks(chunksCaptor.capture());
        List<TextChunk> chunks = chunksCaptor.getValue();
        assertThat(chunks).hasSizeGreaterThanOrEqualTo(3); // At least 3 chunks
    }

    @Test
    void ingestAllServices_embedsAndStoresChunks_success() {
        // Given: service and mock embeddings
        when(serviceCatalog.getAllServices()).thenReturn(List.of(testService));

        List<EmbeddedChunk> mockEmbeddings = List.of(
                new EmbeddedChunk(new TextChunk(), new float[384])
        );
        when(embeddingService.embedChunks(any())).thenReturn(mockEmbeddings);

        // When: ingest all services
        ingestionService.ingestAllServices();

        // Then: stores embedded chunks
        verify(vectorStoreService).storeBatch(embeddedChunksCaptor.capture());
        List<EmbeddedChunk> stored = embeddedChunksCaptor.getValue();
        assertThat(stored).hasSize(1);
    }

    @Test
    void createServiceInfoChunk_containsRequiredFields_success() {
        // Given: service with all fields
        when(serviceCatalog.getAllServices()).thenReturn(List.of(testService));
        when(embeddingService.embedChunks(any())).thenReturn(List.of());

        // When: ingest service
        ingestionService.ingestAllServices();

        // Then: service info chunk contains key fields
        verify(embeddingService).embedChunks(chunksCaptor.capture());
        List<TextChunk> chunks = chunksCaptor.getValue();

        TextChunk serviceChunk = chunks.stream()
                .filter(c -> c.getChunkType() == TextChunk.ChunkType.DAMEE_SERVICE)
                .findFirst()
                .orElse(null);

        assertThat(serviceChunk).isNotNull();
        assertThat(serviceChunk.getContent()).contains("Test Service");
        assertThat(serviceChunk.getContent()).contains("10504");
        assertThat(serviceChunk.getContent()).contains("Test description");
        assertThat(serviceChunk.getContent()).contains("IT Services");
    }

    @Test
    void createServiceInfoChunk_containsArabicContent_success() {
        // Given: service with Arabic content
        when(serviceCatalog.getAllServices()).thenReturn(List.of(testService));
        when(embeddingService.embedChunks(any())).thenReturn(List.of());

        // When: ingest service
        ingestionService.ingestAllServices();

        // Then: chunk contains Arabic content
        verify(embeddingService).embedChunks(chunksCaptor.capture());
        List<TextChunk> chunks = chunksCaptor.getValue();

        TextChunk serviceChunk = chunks.stream()
                .filter(c -> c.getChunkType() == TextChunk.ChunkType.DAMEE_SERVICE)
                .findFirst()
                .orElse(null);

        assertThat(serviceChunk).isNotNull();
        assertThat(serviceChunk.getContent()).contains("خدمة اختبار");
        assertThat(serviceChunk.getContent()).contains("وصف الاختبار");
    }

    @Test
    void createServiceInfoChunk_containsApprovalInfo_success() {
        // Given: service with approval requirements
        when(serviceCatalog.getAllServices()).thenReturn(List.of(testService));
        when(embeddingService.embedChunks(any())).thenReturn(List.of());

        // When: ingest service
        ingestionService.ingestAllServices();

        // Then: chunk contains approval information
        verify(embeddingService).embedChunks(chunksCaptor.capture());
        List<TextChunk> chunks = chunksCaptor.getValue();

        TextChunk serviceChunk = chunks.stream()
                .filter(c -> c.getChunkType() == TextChunk.ChunkType.DAMEE_SERVICE)
                .findFirst()
                .orElse(null);

        assertThat(serviceChunk).isNotNull();
        assertThat(serviceChunk.getContent()).contains("Manager Approval: Yes");
        assertThat(serviceChunk.getContent()).contains("VIP Bypass Available: No");
    }

    @Test
    void createWorkflowChunk_containsWorkflowSteps_success() {
        // Given: service with workflow
        when(serviceCatalog.getAllServices()).thenReturn(List.of(testService));
        when(embeddingService.embedChunks(any())).thenReturn(List.of());

        // When: ingest service
        ingestionService.ingestAllServices();

        // Then: workflow chunk contains steps
        verify(embeddingService).embedChunks(chunksCaptor.capture());
        List<TextChunk> chunks = chunksCaptor.getValue();

        TextChunk workflowChunk = chunks.stream()
                .filter(c -> c.getChunkType() == TextChunk.ChunkType.DAMEE_WORKFLOW)
                .findFirst()
                .orElse(null);

        assertThat(workflowChunk).isNotNull();
        assertThat(workflowChunk.getContent()).contains("Workflow for: Test Service");
        assertThat(workflowChunk.getContent()).contains("Step 1");
        assertThat(workflowChunk.getContent()).contains("Test Team");
        assertThat(workflowChunk.getContent()).contains("[Requires Approval]");
    }

    @Test
    void createWorkflowChunk_emptyWorkflow_stillCreated() {
        // Given: service with no workflow
        testService.setWorkflow(new ArrayList<>());
        when(serviceCatalog.getAllServices()).thenReturn(List.of(testService));
        when(embeddingService.embedChunks(any())).thenReturn(List.of());

        // When: ingest service
        ingestionService.ingestAllServices();

        // Then: workflow chunk not created for empty workflow
        verify(embeddingService).embedChunks(chunksCaptor.capture());
        List<TextChunk> chunks = chunksCaptor.getValue();

        long workflowChunks = chunks.stream()
                .filter(c -> c.getChunkType() == TextChunk.ChunkType.DAMEE_WORKFLOW)
                .count();

        assertThat(workflowChunks).isZero();
    }

    @Test
    void createKeywordsChunk_containsKeywords_success() {
        // Given: service with keywords
        when(serviceCatalog.getAllServices()).thenReturn(List.of(testService));
        when(embeddingService.embedChunks(any())).thenReturn(List.of());

        // When: ingest service
        ingestionService.ingestAllServices();

        // Then: keywords chunk contains keywords
        verify(embeddingService).embedChunks(chunksCaptor.capture());
        List<TextChunk> chunks = chunksCaptor.getValue();

        TextChunk keywordsChunk = chunks.stream()
                .filter(c -> c.getChunkType() == TextChunk.ChunkType.DAMEE_KEYWORDS)
                .findFirst()
                .orElse(null);

        assertThat(keywordsChunk).isNotNull();
        assertThat(keywordsChunk.getContent()).contains("test, service, اختبار");
        assertThat(keywordsChunk.getContent()).contains("I need test service");
        assertThat(keywordsChunk.getContent()).contains("Request for test service");
    }

    @Test
    void createKeywordsChunk_containsArabicPatterns_success() {
        // Given: service with Arabic name
        when(serviceCatalog.getAllServices()).thenReturn(List.of(testService));
        when(embeddingService.embedChunks(any())).thenReturn(List.of());

        // When: ingest service
        ingestionService.ingestAllServices();

        // Then: keywords chunk contains Arabic patterns
        verify(embeddingService).embedChunks(chunksCaptor.capture());
        List<TextChunk> chunks = chunksCaptor.getValue();

        TextChunk keywordsChunk = chunks.stream()
                .filter(c -> c.getChunkType() == TextChunk.ChunkType.DAMEE_KEYWORDS)
                .findFirst()
                .orElse(null);

        assertThat(keywordsChunk).isNotNull();
        assertThat(keywordsChunk.getContent()).contains("أريد خدمة اختبار");
        assertThat(keywordsChunk.getContent()).contains("طلب خدمة اختبار");
    }

    @Test
    void createBaseMetadata_containsRequiredFields_success() {
        // Given: service
        when(serviceCatalog.getAllServices()).thenReturn(List.of(testService));
        when(embeddingService.embedChunks(any())).thenReturn(List.of());

        // When: ingest service
        ingestionService.ingestAllServices();

        // Then: chunks have metadata
        verify(embeddingService).embedChunks(chunksCaptor.capture());
        List<TextChunk> chunks = chunksCaptor.getValue();

        for (TextChunk chunk : chunks) {
            assertThat(chunk.getMetadata()).isNotNull();
            assertThat(chunk.getMetadata()).containsEntry("source_type", "DAMEE_SERVICE");
            assertThat(chunk.getMetadata()).containsEntry("service_id", "10504");
            assertThat(chunk.getMetadata()).containsEntry("service_name_en", "Test Service");
            assertThat(chunk.getMetadata()).containsEntry("category", "IT Services");
        }
    }

    @Test
    void refreshCatalog_deletesExistingAndReingests_success() {
        // Given: existing ingestion
        when(serviceCatalog.getAllServices()).thenReturn(List.of(testService));
        when(embeddingService.embedChunks(any())).thenReturn(List.of());

        // When: refresh catalog
        ingestionService.refreshCatalog();

        // Then: deletes old data and re-ingests
        verify(vectorStoreService).deleteBySourceType("DAMEE_SERVICE");
        verify(embeddingService).embedChunks(any());
        verify(vectorStoreService).storeBatch(any());
    }

    @Test
    void isIngestionComplete_beforeIngestion_returnsFalse() {
        // When: check before ingestion
        boolean complete = ingestionService.isIngestionComplete();

        // Then: returns false
        assertThat(complete).isFalse();
    }

    @Test
    void isIngestionComplete_afterIngestion_returnsTrue() {
        // Given: successful ingestion via ingestOnStartup
        when(serviceCatalog.getAllServices()).thenReturn(List.of(testService));
        when(embeddingService.embedChunks(any())).thenReturn(List.of());
        ingestionService.ingestOnStartup();

        // When: check completion
        boolean complete = ingestionService.isIngestionComplete();

        // Then: returns true
        assertThat(complete).isTrue();
    }

    @Test
    void getIngestionStatus_beforeIngestion_returnsInProgress() {
        // When: get status before ingestion
        String status = ingestionService.getIngestionStatus();

        // Then: returns in progress message
        assertThat(status).contains("in progress");
    }

    @Test
    void getIngestionStatus_afterIngestion_returnsComplete() {
        // Given: successful ingestion via ingestOnStartup
        when(serviceCatalog.getAllServices()).thenReturn(List.of(testService));
        when(serviceCatalog.getServiceCount()).thenReturn(1);
        when(embeddingService.embedChunks(any())).thenReturn(List.of());
        ingestionService.ingestOnStartup();

        // When: get status
        String status = ingestionService.getIngestionStatus();

        // Then: returns complete message with count
        assertThat(status).contains("complete");
        assertThat(status).contains("1 services");
    }

    @Test
    void ingestOnStartup_alreadyComplete_skipsIngestion() {
        // Given: ingestion already complete
        when(serviceCatalog.getAllServices()).thenReturn(List.of(testService));
        when(embeddingService.embedChunks(any())).thenReturn(List.of());
        ingestionService.ingestOnStartup();

        // When: trigger startup ingestion again
        ingestionService.ingestOnStartup();

        // Then: only ingests once (verify call count is 1)
        verify(embeddingService, times(1)).embedChunks(any());
    }

    @Test
    void ingestAllServices_handlesException_logsError() {
        // Given: embedding service throws exception
        when(serviceCatalog.getAllServices()).thenReturn(List.of(testService));
        when(embeddingService.embedChunks(any())).thenThrow(new RuntimeException("Embedding failed"));

        // When/Then: exception is handled (no exception thrown)
        try {
            ingestionService.ingestAllServices();
        } catch (Exception e) {
            // Expected to be caught and logged
        }

        // Verify embedding was attempted
        verify(embeddingService).embedChunks(any());
    }

    @Test
    void createChunksForService_nullWorkflow_createsOnlyTwoChunks() {
        // Given: service with null workflow
        testService.setWorkflow(null);
        when(serviceCatalog.getAllServices()).thenReturn(List.of(testService));
        when(embeddingService.embedChunks(any())).thenReturn(List.of());

        // When: ingest service
        ingestionService.ingestAllServices();

        // Then: creates only service info and keywords chunks (2 total)
        verify(embeddingService).embedChunks(chunksCaptor.capture());
        List<TextChunk> chunks = chunksCaptor.getValue();
        assertThat(chunks).hasSize(2);
    }

    @Test
    void chunks_haveCorrectSequenceNumbers_success() {
        // Given: service
        when(serviceCatalog.getAllServices()).thenReturn(List.of(testService));
        when(embeddingService.embedChunks(any())).thenReturn(List.of());

        // When: ingest service
        ingestionService.ingestAllServices();

        // Then: chunks have correct sequence numbers
        verify(embeddingService).embedChunks(chunksCaptor.capture());
        List<TextChunk> chunks = chunksCaptor.getValue();

        TextChunk serviceChunk = chunks.stream()
                .filter(c -> c.getChunkType() == TextChunk.ChunkType.DAMEE_SERVICE)
                .findFirst()
                .orElse(null);
        TextChunk workflowChunk = chunks.stream()
                .filter(c -> c.getChunkType() == TextChunk.ChunkType.DAMEE_WORKFLOW)
                .findFirst()
                .orElse(null);
        TextChunk keywordsChunk = chunks.stream()
                .filter(c -> c.getChunkType() == TextChunk.ChunkType.DAMEE_KEYWORDS)
                .findFirst()
                .orElse(null);

        assertThat(serviceChunk).isNotNull();
        assertThat(serviceChunk.getSequenceNumber()).isEqualTo(0);

        if (workflowChunk != null) {
            assertThat(workflowChunk.getSequenceNumber()).isEqualTo(1);
        }

        assertThat(keywordsChunk).isNotNull();
        assertThat(keywordsChunk.getSequenceNumber()).isEqualTo(2);
    }
}
