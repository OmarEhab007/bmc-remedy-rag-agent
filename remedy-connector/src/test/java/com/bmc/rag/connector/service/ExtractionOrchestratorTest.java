package com.bmc.rag.connector.service;

import com.bmc.rag.connector.config.RemedyConnectionConfig;
import com.bmc.rag.connector.connection.ThreadLocalARContext;
import com.bmc.rag.connector.extractor.*;
import com.bmc.rag.connector.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExtractionOrchestrator.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExtractionOrchestratorTest {

    @Mock
    private RemedyConnectionConfig mockConfig;

    @Mock
    private ThreadLocalARContext mockArContext;

    @Mock
    private IncidentExtractor mockIncidentExtractor;

    @Mock
    private WorkOrderExtractor mockWorkOrderExtractor;

    @Mock
    private KnowledgeExtractor mockKnowledgeExtractor;

    @Mock
    private ChangeRequestExtractor mockChangeRequestExtractor;

    @Mock
    private WorkLogExtractor mockWorkLogExtractor;

    @Mock
    private AttachmentExtractor mockAttachmentExtractor;

    @InjectMocks
    private ExtractionOrchestrator extractionOrchestrator;

    @BeforeEach
    void setUp() {
        when(mockConfig.isEnabled()).thenReturn(true);
    }

    @Test
    void isEnabled_configEnabled_returnsTrue() {
        // Given
        when(mockConfig.isEnabled()).thenReturn(true);

        // Then
        assertThat(extractionOrchestrator.isEnabled()).isTrue();
    }

    @Test
    void isEnabled_configDisabled_returnsFalse() {
        // Given
        when(mockConfig.isEnabled()).thenReturn(false);

        // Then
        assertThat(extractionOrchestrator.isEnabled()).isFalse();
    }

    @Test
    void isExtractionInProgress_notRunning_returnsFalse() {
        // Then
        assertThat(extractionOrchestrator.isExtractionInProgress()).isFalse();
    }

    @Test
    void extractAll_configDisabled_returnsDisabledResult() {
        // Given
        when(mockConfig.isEnabled()).thenReturn(false);

        // When
        ExtractionOrchestrator.ExtractionResult result = extractionOrchestrator.extractAll(null);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("disabled");
    }

    @Test
    void extractAll_validData_extractsAllSources() {
        // Given
        when(mockIncidentExtractor.extractWithQualification(any())).thenReturn(Collections.emptyList());
        when(mockWorkOrderExtractor.extractWithQualification(any())).thenReturn(Collections.emptyList());
        when(mockKnowledgeExtractor.extractPublishedArticles(anyLong())).thenReturn(Collections.emptyList());
        when(mockChangeRequestExtractor.extractWithQualification(any())).thenReturn(Collections.emptyList());
        when(mockWorkLogExtractor.batchExtractIncidentWorkLogs(any())).thenReturn(Collections.emptyMap());
        when(mockWorkLogExtractor.batchExtractWorkOrderWorkLogs(any())).thenReturn(Collections.emptyMap());
        when(mockWorkLogExtractor.batchExtractChangeWorkLogs(any())).thenReturn(Collections.emptyMap());

        // When
        ExtractionOrchestrator.ExtractionResult result = extractionOrchestrator.extractAll(null);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDuration()).isNotNull();
        assertThat(result.getIncidents()).isNotNull();
        assertThat(result.getWorkOrders()).isNotNull();
        assertThat(result.getKnowledgeArticles()).isNotNull();
        assertThat(result.getChangeRequests()).isNotNull();
    }

    @Test
    void extractAll_withProgressCallback_invokesCallback() {
        // Given
        List<ExtractionOrchestrator.ExtractionProgress> progressUpdates = new ArrayList<>();
        when(mockIncidentExtractor.extractWithQualification(any())).thenReturn(Collections.emptyList());
        when(mockWorkOrderExtractor.extractWithQualification(any())).thenReturn(Collections.emptyList());
        when(mockKnowledgeExtractor.extractPublishedArticles(anyLong())).thenReturn(Collections.emptyList());
        when(mockChangeRequestExtractor.extractWithQualification(any())).thenReturn(Collections.emptyList());
        when(mockWorkLogExtractor.batchExtractIncidentWorkLogs(any())).thenReturn(Collections.emptyMap());
        when(mockWorkLogExtractor.batchExtractWorkOrderWorkLogs(any())).thenReturn(Collections.emptyMap());
        when(mockWorkLogExtractor.batchExtractChangeWorkLogs(any())).thenReturn(Collections.emptyMap());

        // When
        ExtractionOrchestrator.ExtractionResult result = extractionOrchestrator.extractAll(progressUpdates::add);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(progressUpdates).isNotEmpty();
    }

    @Test
    void extractIncidents_validData_returnsResult() {
        // Given
        IncidentRecord incident = IncidentRecord.builder().incidentNumber("INC000001").build();
        when(mockIncidentExtractor.extractWithQualification(any())).thenReturn(List.of(incident));
        when(mockWorkLogExtractor.batchExtractIncidentWorkLogs(any())).thenReturn(Collections.emptyMap());

        // When
        ExtractionOrchestrator.SourceExtractionResult result = extractionOrchestrator.extractIncidents(null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.sourceType()).isEqualTo("Incident");
        assertThat(result.recordCount()).isEqualTo(1);
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void extractIncidents_withWorkLogs_attachesWorkLogs() {
        // Given
        IncidentRecord incident = IncidentRecord.builder().incidentNumber("INC000001").build();
        WorkLogEntry workLog = WorkLogEntry.builder().workLogId("WL001").parentId("INC000001").build();

        when(mockIncidentExtractor.extractWithQualification(any())).thenReturn(List.of(incident));
        when(mockWorkLogExtractor.batchExtractIncidentWorkLogs(any())).thenReturn(
            Map.of("INC000001", List.of(workLog))
        );

        // When
        ExtractionOrchestrator.SourceExtractionResult result = extractionOrchestrator.extractIncidents(null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        verify(mockWorkLogExtractor).batchExtractIncidentWorkLogs(any());
    }

    @Test
    void extractWorkOrders_validData_returnsResult() {
        // Given
        WorkOrderRecord workOrder = WorkOrderRecord.builder().workOrderId("WO0000001").build();
        when(mockWorkOrderExtractor.extractWithQualification(any())).thenReturn(List.of(workOrder));
        when(mockWorkLogExtractor.batchExtractWorkOrderWorkLogs(any())).thenReturn(Collections.emptyMap());

        // When
        ExtractionOrchestrator.SourceExtractionResult result = extractionOrchestrator.extractWorkOrders(null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.sourceType()).isEqualTo("WorkOrder");
        assertThat(result.recordCount()).isEqualTo(1);
    }

    @Test
    void extractKnowledgeArticles_validData_returnsResult() {
        // Given
        KnowledgeArticle article = KnowledgeArticle.builder().articleId("KA000001").build();
        when(mockKnowledgeExtractor.extractPublishedArticles(anyLong())).thenReturn(List.of(article));

        // When
        ExtractionOrchestrator.SourceExtractionResult result = extractionOrchestrator.extractKnowledgeArticles(null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.sourceType()).isEqualTo("KnowledgeArticle");
        assertThat(result.recordCount()).isEqualTo(1);
    }

    @Test
    void extractChangeRequests_validData_returnsResult() {
        // Given
        ChangeRequestRecord change = ChangeRequestRecord.builder().changeId("CHG0000001").build();
        when(mockChangeRequestExtractor.extractWithQualification(any())).thenReturn(List.of(change));
        when(mockWorkLogExtractor.batchExtractChangeWorkLogs(any())).thenReturn(Collections.emptyMap());

        // When
        ExtractionOrchestrator.SourceExtractionResult result = extractionOrchestrator.extractChangeRequests(null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.sourceType()).isEqualTo("ChangeRequest");
        assertThat(result.recordCount()).isEqualTo(1);
    }

    @Test
    void extractModifiedSince_validTimestamp_extractsModifiedRecords() {
        // Given
        long timestamp = 1672531200L;
        when(mockIncidentExtractor.extractModifiedSince(timestamp)).thenReturn(Collections.emptyList());
        when(mockWorkOrderExtractor.extractModifiedSince(timestamp)).thenReturn(Collections.emptyList());
        when(mockKnowledgeExtractor.extractPublishedArticles(timestamp)).thenReturn(Collections.emptyList());
        when(mockChangeRequestExtractor.extractModifiedSince(timestamp)).thenReturn(Collections.emptyList());

        // When
        ExtractionOrchestrator.ExtractionResult result = extractionOrchestrator.extractModifiedSince(timestamp, null);

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(mockIncidentExtractor).extractModifiedSince(timestamp);
        verify(mockWorkOrderExtractor).extractModifiedSince(timestamp);
        verify(mockKnowledgeExtractor).extractPublishedArticles(timestamp);
        verify(mockChangeRequestExtractor).extractModifiedSince(timestamp);
    }

    @Test
    void extractModifiedSince_configDisabled_returnsDisabledResult() {
        // Given
        when(mockConfig.isEnabled()).thenReturn(false);

        // When
        ExtractionOrchestrator.ExtractionResult result = extractionOrchestrator.extractModifiedSince(0L, null);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("disabled");
    }

    @Test
    void extractAll_extractorException_continuesWithOtherSources() {
        // Given - One extractor throws exception
        when(mockIncidentExtractor.extractWithQualification(any()))
            .thenThrow(new RuntimeException("Extraction failed"));
        when(mockWorkOrderExtractor.extractWithQualification(any())).thenReturn(Collections.emptyList());
        when(mockKnowledgeExtractor.extractPublishedArticles(anyLong())).thenReturn(Collections.emptyList());
        when(mockChangeRequestExtractor.extractWithQualification(any())).thenReturn(Collections.emptyList());
        when(mockWorkLogExtractor.batchExtractIncidentWorkLogs(any())).thenReturn(Collections.emptyMap());
        when(mockWorkLogExtractor.batchExtractWorkOrderWorkLogs(any())).thenReturn(Collections.emptyMap());
        when(mockWorkLogExtractor.batchExtractChangeWorkLogs(any())).thenReturn(Collections.emptyMap());

        // When
        ExtractionOrchestrator.ExtractionResult result = extractionOrchestrator.extractAll(null);

        // Then - Orchestrator continues and succeeds, but incident extraction failed
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getIncidents()).isNotNull();
        assertThat(result.getIncidents().isSuccess()).isFalse();
        assertThat(result.getIncidents().errorMessage()).contains("Extraction failed");
    }

    @Test
    void extractIncidents_extractorFailure_returnsFailureResult() {
        // Given
        when(mockIncidentExtractor.extractWithQualification(any()))
            .thenThrow(new RuntimeException("Database connection failed"));

        // When
        ExtractionOrchestrator.SourceExtractionResult result = extractionOrchestrator.extractIncidents(null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.errorMessage()).contains("Database connection failed");
    }

    @Test
    void extractionResult_getTotalRecords_sumsAllSources() {
        // Given
        ExtractionOrchestrator.ExtractionResult result = ExtractionOrchestrator.ExtractionResult.builder()
            .incidents(new ExtractionOrchestrator.SourceExtractionResult("Incident", 10, null))
            .workOrders(new ExtractionOrchestrator.SourceExtractionResult("WorkOrder", 5, null))
            .knowledgeArticles(new ExtractionOrchestrator.SourceExtractionResult("KnowledgeArticle", 8, null))
            .changeRequests(new ExtractionOrchestrator.SourceExtractionResult("ChangeRequest", 3, null))
            .build();

        // When
        int total = result.getTotalRecords();

        // Then
        assertThat(total).isEqualTo(26);
    }

    @Test
    void extractionResult_nullSources_getTotalRecordsReturnsZero() {
        // Given
        ExtractionOrchestrator.ExtractionResult result = ExtractionOrchestrator.ExtractionResult.builder()
            .incidents(null)
            .workOrders(null)
            .knowledgeArticles(null)
            .changeRequests(null)
            .build();

        // When
        int total = result.getTotalRecords();

        // Then
        assertThat(total).isEqualTo(0);
    }

    @Test
    void sourceExtractionResult_isSuccess_trueWhenNoError() {
        // Given
        ExtractionOrchestrator.SourceExtractionResult result =
            new ExtractionOrchestrator.SourceExtractionResult("Test", 5, null);

        // Then
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void sourceExtractionResult_isSuccess_falseWhenError() {
        // Given
        ExtractionOrchestrator.SourceExtractionResult result =
            new ExtractionOrchestrator.SourceExtractionResult("Test", 0, "Error occurred");

        // Then
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void extractionResult_disabled_createsDisabledResult() {
        // When
        ExtractionOrchestrator.ExtractionResult result = ExtractionOrchestrator.ExtractionResult.disabled();

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("disabled");
    }

    @Test
    void extractionResult_alreadyRunning_createsAlreadyRunningResult() {
        // When
        ExtractionOrchestrator.ExtractionResult result = ExtractionOrchestrator.ExtractionResult.alreadyRunning();

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("already in progress");
    }

    @Test
    void extractionResult_circuitOpen_createsCircuitOpenResult() {
        // When
        ExtractionOrchestrator.ExtractionResult result = ExtractionOrchestrator.ExtractionResult.circuitOpen();

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Circuit breaker");
    }

    @Test
    void extractIncidents_emptyResult_returnsZeroCount() {
        // Given
        when(mockIncidentExtractor.extractWithQualification(any())).thenReturn(Collections.emptyList());
        when(mockWorkLogExtractor.batchExtractIncidentWorkLogs(any())).thenReturn(Collections.emptyMap());

        // When
        ExtractionOrchestrator.SourceExtractionResult result = extractionOrchestrator.extractIncidents(null);

        // Then
        assertThat(result.recordCount()).isEqualTo(0);
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void extractAll_withProgressCallback_reportsFailed() {
        // Given
        List<ExtractionOrchestrator.ExtractionProgress> progressUpdates = new ArrayList<>();
        when(mockIncidentExtractor.extractWithQualification(any()))
            .thenThrow(new RuntimeException("Extraction failed"));
        when(mockWorkOrderExtractor.extractWithQualification(any())).thenReturn(Collections.emptyList());
        when(mockKnowledgeExtractor.extractPublishedArticles(anyLong())).thenReturn(Collections.emptyList());
        when(mockChangeRequestExtractor.extractWithQualification(any())).thenReturn(Collections.emptyList());
        when(mockWorkLogExtractor.batchExtractWorkOrderWorkLogs(any())).thenReturn(Collections.emptyMap());
        when(mockWorkLogExtractor.batchExtractChangeWorkLogs(any())).thenReturn(Collections.emptyMap());

        // When
        ExtractionOrchestrator.ExtractionResult result = extractionOrchestrator.extractAll(progressUpdates::add);

        // Then
        assertThat(progressUpdates).isNotEmpty();
        assertThat(progressUpdates.stream().anyMatch(p -> "FAILED".equals(p.status()))).isTrue();
    }

    @Test
    void extractWorkOrders_withProgressCallback_invokesCallback() {
        // Given
        List<ExtractionOrchestrator.ExtractionProgress> progressUpdates = new ArrayList<>();
        WorkOrderRecord workOrder = WorkOrderRecord.builder().workOrderId("WO0000001").build();
        when(mockWorkOrderExtractor.extractWithQualification(any())).thenReturn(List.of(workOrder));
        when(mockWorkLogExtractor.batchExtractWorkOrderWorkLogs(any())).thenReturn(Collections.emptyMap());

        // When
        ExtractionOrchestrator.SourceExtractionResult result = extractionOrchestrator.extractWorkOrders(progressUpdates::add);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(progressUpdates).hasSize(2); // STARTED and COMPLETED
        assertThat(progressUpdates.get(0).status()).isEqualTo("STARTED");
        assertThat(progressUpdates.get(1).status()).isEqualTo("COMPLETED");
    }

    @Test
    void extractKnowledgeArticles_withProgressCallback_invokesCallback() {
        // Given
        List<ExtractionOrchestrator.ExtractionProgress> progressUpdates = new ArrayList<>();
        KnowledgeArticle article = KnowledgeArticle.builder().articleId("KA000001").build();
        when(mockKnowledgeExtractor.extractPublishedArticles(anyLong())).thenReturn(List.of(article));

        // When
        ExtractionOrchestrator.SourceExtractionResult result = extractionOrchestrator.extractKnowledgeArticles(progressUpdates::add);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(progressUpdates).hasSize(2);
    }

    @Test
    void extractChangeRequests_withProgressCallback_invokesCallback() {
        // Given
        List<ExtractionOrchestrator.ExtractionProgress> progressUpdates = new ArrayList<>();
        ChangeRequestRecord change = ChangeRequestRecord.builder().changeId("CHG0000001").build();
        when(mockChangeRequestExtractor.extractWithQualification(any())).thenReturn(List.of(change));
        when(mockWorkLogExtractor.batchExtractChangeWorkLogs(any())).thenReturn(Collections.emptyMap());

        // When
        ExtractionOrchestrator.SourceExtractionResult result = extractionOrchestrator.extractChangeRequests(progressUpdates::add);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(progressUpdates).hasSize(2);
    }

    @Test
    void extractWorkOrders_exceptionThrown_returnsFailureResult() {
        // Given
        when(mockWorkOrderExtractor.extractWithQualification(any()))
            .thenThrow(new RuntimeException("Database error"));

        // When
        ExtractionOrchestrator.SourceExtractionResult result = extractionOrchestrator.extractWorkOrders(null);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.recordCount()).isEqualTo(0);
        assertThat(result.errorMessage()).contains("Database error");
    }

    @Test
    void extractKnowledgeArticles_exceptionThrown_returnsFailureResult() {
        // Given
        when(mockKnowledgeExtractor.extractPublishedArticles(anyLong()))
            .thenThrow(new RuntimeException("Query failed"));

        // When
        ExtractionOrchestrator.SourceExtractionResult result = extractionOrchestrator.extractKnowledgeArticles(null);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.errorMessage()).contains("Query failed");
    }

    @Test
    void extractChangeRequests_exceptionThrown_returnsFailureResult() {
        // Given
        when(mockChangeRequestExtractor.extractWithQualification(any()))
            .thenThrow(new RuntimeException("Connection lost"));

        // When
        ExtractionOrchestrator.SourceExtractionResult result = extractionOrchestrator.extractChangeRequests(null);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.errorMessage()).contains("Connection lost");
    }

    @Test
    void extractModifiedSince_exceptionInExtractor_returnsFailure() {
        // Given
        long timestamp = 1672531200L;
        when(mockIncidentExtractor.extractModifiedSince(timestamp))
            .thenThrow(new RuntimeException("Extraction failed"));

        // When
        ExtractionOrchestrator.ExtractionResult result = extractionOrchestrator.extractModifiedSince(timestamp, null);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Extraction failed");
    }

    @Test
    void extractAll_individualExtractorFailure_continuesWithOthers() {
        // Given - One extractor fails, others succeed
        when(mockIncidentExtractor.extractWithQualification(any()))
            .thenThrow(new RuntimeException("Connection failed"));
        when(mockWorkOrderExtractor.extractWithQualification(any())).thenReturn(Collections.emptyList());
        when(mockKnowledgeExtractor.extractPublishedArticles(anyLong())).thenReturn(Collections.emptyList());
        when(mockChangeRequestExtractor.extractWithQualification(any())).thenReturn(Collections.emptyList());
        when(mockWorkLogExtractor.batchExtractWorkOrderWorkLogs(any())).thenReturn(Collections.emptyMap());
        when(mockWorkLogExtractor.batchExtractChangeWorkLogs(any())).thenReturn(Collections.emptyMap());

        // When - Execute extraction
        ExtractionOrchestrator.ExtractionResult result = extractionOrchestrator.extractAll(null);

        // Then - Overall extraction succeeds but incident extraction failed
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getIncidents()).isNotNull();
        assertThat(result.getIncidents().isSuccess()).isFalse();
    }

    @Test
    void extractModifiedSince_withProgressCallback_invokesCallback() {
        // Given
        long timestamp = 1672531200L;
        List<ExtractionOrchestrator.ExtractionProgress> progressUpdates = new ArrayList<>();
        when(mockIncidentExtractor.extractModifiedSince(timestamp)).thenReturn(Collections.emptyList());
        when(mockWorkOrderExtractor.extractModifiedSince(timestamp)).thenReturn(Collections.emptyList());
        when(mockKnowledgeExtractor.extractPublishedArticles(timestamp)).thenReturn(Collections.emptyList());
        when(mockChangeRequestExtractor.extractModifiedSince(timestamp)).thenReturn(Collections.emptyList());

        // When
        ExtractionOrchestrator.ExtractionResult result = extractionOrchestrator.extractModifiedSince(
            timestamp, progressUpdates::add);

        // Then
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void extractionProgress_recordFields_accessible() {
        // Given
        ExtractionOrchestrator.ExtractionProgress progress =
            new ExtractionOrchestrator.ExtractionProgress("Incident", "STARTED", 0, 100);

        // Then
        assertThat(progress.sourceType()).isEqualTo("Incident");
        assertThat(progress.status()).isEqualTo("STARTED");
        assertThat(progress.current()).isEqualTo(0);
        assertThat(progress.total()).isEqualTo(100);
    }

    @Test
    void sourceExtractionResult_recordFields_accessible() {
        // Given
        ExtractionOrchestrator.SourceExtractionResult result =
            new ExtractionOrchestrator.SourceExtractionResult("WorkOrder", 42, null);

        // Then
        assertThat(result.sourceType()).isEqualTo("WorkOrder");
        assertThat(result.recordCount()).isEqualTo(42);
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    void extractionResult_builderSetters_workCorrectly() {
        // Given
        ExtractionOrchestrator.ExtractionResult result = ExtractionOrchestrator.ExtractionResult.builder()
            .success(true)
            .errorMessage(null)
            .duration(Duration.ofSeconds(5))
            .build();

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getDuration()).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void extractIncidents_withProgressCallback_reportsProgress() {
        // Given
        List<ExtractionOrchestrator.ExtractionProgress> progressUpdates = new ArrayList<>();
        IncidentRecord incident = IncidentRecord.builder().incidentNumber("INC000001").build();
        when(mockIncidentExtractor.extractWithQualification(any())).thenReturn(List.of(incident));
        when(mockWorkLogExtractor.batchExtractIncidentWorkLogs(any())).thenReturn(Collections.emptyMap());

        // When
        ExtractionOrchestrator.SourceExtractionResult result = extractionOrchestrator.extractIncidents(progressUpdates::add);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(progressUpdates).hasSize(2);
        assertThat(progressUpdates.get(0).sourceType()).isEqualTo("Incident");
        assertThat(progressUpdates.get(0).status()).isEqualTo("STARTED");
        assertThat(progressUpdates.get(1).status()).isEqualTo("COMPLETED");
    }

    @Nested
    @DisplayName("Concurrent Extraction Tests")
    class ConcurrentExtractionTests {

        @Test
        @DisplayName("Extraction already running returns error")
        void extractionAlreadyRunningReturnsError() throws InterruptedException {
            // Given - Block first extraction with slow extractor
            when(mockIncidentExtractor.extractWithQualification(any())).thenAnswer(inv -> {
                Thread.sleep(500);
                return Collections.emptyList();
            });
            when(mockWorkOrderExtractor.extractWithQualification(any())).thenReturn(Collections.emptyList());
            when(mockKnowledgeExtractor.extractPublishedArticles(anyLong())).thenReturn(Collections.emptyList());
            when(mockChangeRequestExtractor.extractWithQualification(any())).thenReturn(Collections.emptyList());
            when(mockWorkLogExtractor.batchExtractWorkOrderWorkLogs(any())).thenReturn(Collections.emptyMap());
            when(mockWorkLogExtractor.batchExtractChangeWorkLogs(any())).thenReturn(Collections.emptyMap());

            // When - Start extraction in background thread
            Thread t1 = new Thread(() -> extractionOrchestrator.extractAll(null));
            t1.start();

            // Wait for extraction to start
            Thread.sleep(100);

            // Try to start second extraction
            ExtractionOrchestrator.ExtractionResult result = extractionOrchestrator.extractAll(null);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("already in progress");

            // Cleanup
            t1.join();
        }

        @Test
        @DisplayName("extractModifiedSince respects already running state")
        void extractModifiedSince_alreadyRunningReturnsError() throws InterruptedException {
            // Given - Block first extraction
            when(mockIncidentExtractor.extractWithQualification(any())).thenAnswer(inv -> {
                Thread.sleep(500);
                return Collections.emptyList();
            });
            when(mockWorkOrderExtractor.extractWithQualification(any())).thenReturn(Collections.emptyList());
            when(mockKnowledgeExtractor.extractPublishedArticles(anyLong())).thenReturn(Collections.emptyList());
            when(mockChangeRequestExtractor.extractWithQualification(any())).thenReturn(Collections.emptyList());
            when(mockWorkLogExtractor.batchExtractWorkOrderWorkLogs(any())).thenReturn(Collections.emptyMap());
            when(mockWorkLogExtractor.batchExtractChangeWorkLogs(any())).thenReturn(Collections.emptyMap());

            // When - Start full extraction in background
            Thread t1 = new Thread(() -> extractionOrchestrator.extractAll(null));
            t1.start();

            // Wait for extraction to start
            Thread.sleep(100);

            // Try to start incremental extraction
            ExtractionOrchestrator.ExtractionResult result = extractionOrchestrator.extractModifiedSince(1672531200L, null);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("already in progress");

            // Cleanup
            t1.join();
        }
    }

    @Nested
    @DisplayName("Work Log Attachment Tests")
    class WorkLogTests {

        @Test
        @DisplayName("extractWorkOrders with work logs attaches them correctly")
        void extractWorkOrders_withWorkLogs_attachesWorkLogs() {
            // Given
            WorkOrderRecord workOrder1 = WorkOrderRecord.builder().workOrderId("WO001").build();
            WorkOrderRecord workOrder2 = WorkOrderRecord.builder().workOrderId("WO002").build();

            WorkLogEntry log1 = WorkLogEntry.builder().workLogId("WL001").parentId("WO001").build();
            WorkLogEntry log2 = WorkLogEntry.builder().workLogId("WL002").parentId("WO002").build();

            when(mockWorkOrderExtractor.extractWithQualification(any())).thenReturn(List.of(workOrder1, workOrder2));
            when(mockWorkLogExtractor.batchExtractWorkOrderWorkLogs(any())).thenReturn(
                Map.of("WO001", List.of(log1), "WO002", List.of(log2))
            );

            // When
            ExtractionOrchestrator.SourceExtractionResult result = extractionOrchestrator.extractWorkOrders(null);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.recordCount()).isEqualTo(2);
            verify(mockWorkLogExtractor).batchExtractWorkOrderWorkLogs(any());
        }

        @Test
        @DisplayName("extractChangeRequests with work logs attaches them correctly")
        void extractChangeRequests_withWorkLogs_attachesWorkLogs() {
            // Given
            ChangeRequestRecord change1 = ChangeRequestRecord.builder().changeId("CHG001").build();
            ChangeRequestRecord change2 = ChangeRequestRecord.builder().changeId("CHG002").build();

            WorkLogEntry log1 = WorkLogEntry.builder().workLogId("WL001").parentId("CHG001").build();

            when(mockChangeRequestExtractor.extractWithQualification(any())).thenReturn(List.of(change1, change2));
            when(mockWorkLogExtractor.batchExtractChangeWorkLogs(any())).thenReturn(
                Map.of("CHG001", List.of(log1))
            );

            // When
            ExtractionOrchestrator.SourceExtractionResult result = extractionOrchestrator.extractChangeRequests(null);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.recordCount()).isEqualTo(2);
            verify(mockWorkLogExtractor).batchExtractChangeWorkLogs(any());
        }
    }
}
