package com.bmc.rag.connector.extractor;

import com.bmc.rag.connector.config.RemedyConnectionConfig;
import com.bmc.rag.connector.connection.ThreadLocalARContext;
import com.bmc.rag.connector.model.WorkLogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WorkLogExtractor.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkLogExtractorTest {

    @Mock
    private ThreadLocalARContext mockArContext;

    @Mock
    private RemedyConnectionConfig mockConfig;

    @InjectMocks
    private WorkLogExtractor workLogExtractor;

    @BeforeEach
    void setUp() {
        when(mockConfig.getChunkSize()).thenReturn(500);
    }

    @Test
    void extractIncidentWorkLogs_validIncidentNumber_returnsWorkLogs() {
        // Given
        String incidentNumber = "INC000001";
        WorkLogEntry mockLog = WorkLogEntry.builder()
            .workLogId("WL001")
            .parentId(incidentNumber)
            .source(WorkLogEntry.WorkLogSource.INCIDENT)
            .detailedDescription("User called to report issue")
            .build();
        List<WorkLogEntry> expectedList = List.of(mockLog);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<WorkLogEntry> results = workLogExtractor.extractIncidentWorkLogs(incidentNumber);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getWorkLogId()).isEqualTo("WL001");
        assertThat(results.get(0).getSource()).isEqualTo(WorkLogEntry.WorkLogSource.INCIDENT);
    }

    @Test
    void extractIncidentWorkLogs_noWorkLogs_returnsEmptyList() {
        // Given
        String incidentNumber = "INC000001";
        when(mockArContext.executeWithRetry(any())).thenReturn(Collections.emptyList());

        // When
        List<WorkLogEntry> results = workLogExtractor.extractIncidentWorkLogs(incidentNumber);

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void extractWorkOrderWorkLogs_validWorkOrderId_returnsWorkLogs() {
        // Given
        String workOrderId = "WO0000001";
        WorkLogEntry mockLog = WorkLogEntry.builder()
            .workLogId("WL001")
            .parentId(workOrderId)
            .source(WorkLogEntry.WorkLogSource.WORK_ORDER)
            .detailedDescription("Work started on server installation")
            .build();
        List<WorkLogEntry> expectedList = List.of(mockLog);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<WorkLogEntry> results = workLogExtractor.extractWorkOrderWorkLogs(workOrderId);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getParentId()).isEqualTo(workOrderId);
        assertThat(results.get(0).getSource()).isEqualTo(WorkLogEntry.WorkLogSource.WORK_ORDER);
    }

    @Test
    void extractChangeWorkLogs_validChangeId_returnsWorkLogs() {
        // Given
        String changeId = "CHG0000001";
        WorkLogEntry mockLog = WorkLogEntry.builder()
            .workLogId("WL001")
            .parentId(changeId)
            .source(WorkLogEntry.WorkLogSource.CHANGE_REQUEST)
            .detailedDescription("Change implementation notes")
            .build();
        List<WorkLogEntry> expectedList = List.of(mockLog);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<WorkLogEntry> results = workLogExtractor.extractChangeWorkLogs(changeId);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getParentId()).isEqualTo(changeId);
        assertThat(results.get(0).getSource()).isEqualTo(WorkLogEntry.WorkLogSource.CHANGE_REQUEST);
    }

    @Test
    void batchExtractIncidentWorkLogs_multipleIncidents_returnsGroupedLogs() {
        // Given
        List<String> incidentNumbers = Arrays.asList("INC000001", "INC000002");

        WorkLogEntry log1 = WorkLogEntry.builder()
            .workLogId("WL001")
            .parentId("INC000001")
            .source(WorkLogEntry.WorkLogSource.INCIDENT)
            .build();

        WorkLogEntry log2 = WorkLogEntry.builder()
            .workLogId("WL002")
            .parentId("INC000002")
            .source(WorkLogEntry.WorkLogSource.INCIDENT)
            .build();

        List<WorkLogEntry> allLogs = Arrays.asList(log1, log2);
        when(mockArContext.executeWithRetry(any())).thenReturn(allLogs);

        // When
        Map<String, List<WorkLogEntry>> results = workLogExtractor.batchExtractIncidentWorkLogs(incidentNumbers);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get("INC000001")).hasSize(1);
        assertThat(results.get("INC000002")).hasSize(1);
    }

    @Test
    void batchExtractIncidentWorkLogs_emptyList_returnsEmptyMap() {
        // When
        Map<String, List<WorkLogEntry>> results = workLogExtractor.batchExtractIncidentWorkLogs(Collections.emptyList());

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void batchExtractIncidentWorkLogs_nullList_returnsEmptyMap() {
        // When
        Map<String, List<WorkLogEntry>> results = workLogExtractor.batchExtractIncidentWorkLogs(null);

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void batchExtractWorkOrderWorkLogs_multipleWorkOrders_returnsGroupedLogs() {
        // Given
        List<String> workOrderIds = Arrays.asList("WO0000001", "WO0000002");

        WorkLogEntry log1 = WorkLogEntry.builder()
            .workLogId("WL001")
            .parentId("WO0000001")
            .source(WorkLogEntry.WorkLogSource.WORK_ORDER)
            .build();

        WorkLogEntry log2 = WorkLogEntry.builder()
            .workLogId("WL002")
            .parentId("WO0000002")
            .source(WorkLogEntry.WorkLogSource.WORK_ORDER)
            .build();

        List<WorkLogEntry> allLogs = Arrays.asList(log1, log2);
        when(mockArContext.executeWithRetry(any())).thenReturn(allLogs);

        // When
        Map<String, List<WorkLogEntry>> results = workLogExtractor.batchExtractWorkOrderWorkLogs(workOrderIds);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get("WO0000001")).hasSize(1);
        assertThat(results.get("WO0000002")).hasSize(1);
    }

    @Test
    void batchExtractChangeWorkLogs_multipleChanges_returnsGroupedLogs() {
        // Given
        List<String> changeIds = Arrays.asList("CHG0000001", "CHG0000002");

        WorkLogEntry log1 = WorkLogEntry.builder()
            .workLogId("WL001")
            .parentId("CHG0000001")
            .source(WorkLogEntry.WorkLogSource.CHANGE_REQUEST)
            .build();

        WorkLogEntry log2 = WorkLogEntry.builder()
            .workLogId("WL002")
            .parentId("CHG0000002")
            .source(WorkLogEntry.WorkLogSource.CHANGE_REQUEST)
            .build();

        List<WorkLogEntry> allLogs = Arrays.asList(log1, log2);
        when(mockArContext.executeWithRetry(any())).thenReturn(allLogs);

        // When
        Map<String, List<WorkLogEntry>> results = workLogExtractor.batchExtractChangeWorkLogs(changeIds);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get("CHG0000001")).hasSize(1);
        assertThat(results.get("CHG0000002")).hasSize(1);
    }

    @Test
    void extractIncidentWorkLogs_multipleLogsForSingleIncident_returnsAllLogs() {
        // Given
        String incidentNumber = "INC000001";
        WorkLogEntry log1 = WorkLogEntry.builder()
            .workLogId("WL001")
            .parentId(incidentNumber)
            .source(WorkLogEntry.WorkLogSource.INCIDENT)
            .build();

        WorkLogEntry log2 = WorkLogEntry.builder()
            .workLogId("WL002")
            .parentId(incidentNumber)
            .source(WorkLogEntry.WorkLogSource.INCIDENT)
            .build();

        List<WorkLogEntry> expectedList = Arrays.asList(log1, log2);
        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<WorkLogEntry> results = workLogExtractor.extractIncidentWorkLogs(incidentNumber);

        // Then
        assertThat(results).hasSize(2);
        verify(mockArContext).executeWithRetry(any());
    }

    @Test
    void extractIncidentWorkLogs_multipleResults_retrievesAll() {
        // Given
        when(mockConfig.getChunkSize()).thenReturn(2);

        WorkLogEntry log1 = WorkLogEntry.builder().workLogId("WL001").parentId("INC000001").source(WorkLogEntry.WorkLogSource.INCIDENT).build();
        WorkLogEntry log2 = WorkLogEntry.builder().workLogId("WL002").parentId("INC000001").source(WorkLogEntry.WorkLogSource.INCIDENT).build();
        WorkLogEntry log3 = WorkLogEntry.builder().workLogId("WL003").parentId("INC000001").source(WorkLogEntry.WorkLogSource.INCIDENT).build();

        List<WorkLogEntry> expectedList = Arrays.asList(log1, log2, log3);
        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<WorkLogEntry> results = workLogExtractor.extractIncidentWorkLogs("INC000001");

        // Then
        assertThat(results).hasSize(3);
    }

    @Test
    void constructor_createsInstance() {
        // Then
        assertThat(workLogExtractor).isNotNull();
    }

    @Test
    void batchExtractWorkOrderWorkLogs_emptyList_returnsEmptyMap() {
        // When
        Map<String, List<WorkLogEntry>> results = workLogExtractor.batchExtractWorkOrderWorkLogs(Collections.emptyList());

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void batchExtractWorkOrderWorkLogs_nullList_returnsEmptyMap() {
        // When
        Map<String, List<WorkLogEntry>> results = workLogExtractor.batchExtractWorkOrderWorkLogs(null);

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void batchExtractChangeWorkLogs_emptyList_returnsEmptyMap() {
        // When
        Map<String, List<WorkLogEntry>> results = workLogExtractor.batchExtractChangeWorkLogs(Collections.emptyList());

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void batchExtractChangeWorkLogs_nullList_returnsEmptyMap() {
        // When
        Map<String, List<WorkLogEntry>> results = workLogExtractor.batchExtractChangeWorkLogs(null);

        // Then
        assertThat(results).isEmpty();
    }

}
