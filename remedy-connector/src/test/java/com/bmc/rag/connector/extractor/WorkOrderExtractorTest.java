package com.bmc.rag.connector.extractor;

import com.bmc.arsys.api.Entry;
import com.bmc.arsys.api.Timestamp;
import com.bmc.arsys.api.Value;
import com.bmc.rag.connector.config.RemedyConnectionConfig;
import com.bmc.rag.connector.connection.ThreadLocalARContext;
import com.bmc.rag.connector.model.WorkOrderRecord;
import com.bmc.rag.connector.util.FieldIdConstants;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WorkOrderExtractor.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkOrderExtractorTest {

    @Mock
    private ThreadLocalARContext mockArContext;

    @Mock
    private RemedyConnectionConfig mockConfig;

    @InjectMocks
    private WorkOrderExtractor workOrderExtractor;

    @BeforeEach
    void setUp() {
        when(mockConfig.getChunkSize()).thenReturn(500);
    }

    @Test
    void extractModifiedSince_validTimestamp_returnsWorkOrders() {
        // Given
        long timestamp = 1672531200L;
        WorkOrderRecord mockRecord = WorkOrderRecord.builder()
            .workOrderId("WO0000001")
            .summary("Install new server")
            .build();
        List<WorkOrderRecord> expectedList = List.of(mockRecord);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<WorkOrderRecord> results = workOrderExtractor.extractModifiedSince(timestamp);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getWorkOrderId()).isEqualTo("WO0000001");
        assertThat(results.get(0).getSummary()).isEqualTo("Install new server");
    }

    @Test
    void extractWithQualification_nullQualification_returnsAllWorkOrders() {
        // Given
        WorkOrderRecord record1 = WorkOrderRecord.builder().workOrderId("WO0000001").summary("Task 1").build();
        WorkOrderRecord record2 = WorkOrderRecord.builder().workOrderId("WO0000002").summary("Task 2").build();
        List<WorkOrderRecord> expectedList = List.of(record1, record2);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<WorkOrderRecord> results = workOrderExtractor.extractWithQualification(null);

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void extractWithQualification_validQualification_returnsFilteredWorkOrders() {
        // Given
        String qualification = "'1000000217' = \"Network Support\"";
        WorkOrderRecord mockRecord = WorkOrderRecord.builder()
            .workOrderId("WO0000001")
            .summary("Network maintenance")
            .assignedGroup("Network Support")
            .build();
        List<WorkOrderRecord> expectedList = List.of(mockRecord);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<WorkOrderRecord> results = workOrderExtractor.extractWithQualification(qualification);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAssignedGroup()).isEqualTo("Network Support");
    }

    @Test
    void extractWithQualification_pagination_retrievesAllChunks() {
        // Given
        when(mockConfig.getChunkSize()).thenReturn(2);

        WorkOrderRecord record1 = WorkOrderRecord.builder().workOrderId("WO0000001").summary("Task 1").build();
        WorkOrderRecord record2 = WorkOrderRecord.builder().workOrderId("WO0000002").summary("Task 2").build();
        WorkOrderRecord record3 = WorkOrderRecord.builder().workOrderId("WO0000003").summary("Task 3").build();
        List<WorkOrderRecord> expectedList = List.of(record1, record2, record3);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<WorkOrderRecord> results = workOrderExtractor.extractWithQualification(null);

        // Then
        assertThat(results).hasSize(3);
    }

    @Test
    void extractWithQualification_emptyResult_returnsEmptyList() {
        // Given
        when(mockArContext.executeWithRetry(any())).thenReturn(Collections.emptyList());

        // When
        List<WorkOrderRecord> results = workOrderExtractor.extractWithQualification("'7' = 5");

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void extractByWorkOrderId_found_returnsWorkOrder() {
        // Given
        WorkOrderRecord mockRecord = WorkOrderRecord.builder()
            .workOrderId("WO0000001")
            .summary("Server installation")
            .build();
        List<WorkOrderRecord> expectedList = List.of(mockRecord);
        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        Optional<WorkOrderRecord> result = workOrderExtractor.extractByWorkOrderId("WO0000001");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getWorkOrderId()).isEqualTo("WO0000001");
    }

    @Test
    void extractByWorkOrderId_notFound_returnsEmpty() {
        // Given
        when(mockArContext.executeWithRetry(any())).thenReturn(Collections.emptyList());

        // When
        Optional<WorkOrderRecord> result = workOrderExtractor.extractByWorkOrderId("WO9999999");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void extractByAssignedGroup_withTimestamp_returnsFilteredWorkOrders() {
        // Given
        String assignedGroup = "Server Team";
        long timestamp = 1672531200L;

        WorkOrderRecord mockRecord = WorkOrderRecord.builder()
            .workOrderId("WO0000001")
            .summary("Server maintenance")
            .assignedGroup(assignedGroup)
            .build();
        List<WorkOrderRecord> expectedList = List.of(mockRecord);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<WorkOrderRecord> results = workOrderExtractor.extractByAssignedGroup(assignedGroup, timestamp);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAssignedGroup()).isEqualTo(assignedGroup);
    }

    @Test
    void extractByAssignedGroup_withoutTimestamp_returnsAllForGroup() {
        // Given
        String assignedGroup = "Server Team";

        WorkOrderRecord record1 = WorkOrderRecord.builder().workOrderId("WO0000001").assignedGroup(assignedGroup).build();
        WorkOrderRecord record2 = WorkOrderRecord.builder().workOrderId("WO0000002").assignedGroup(assignedGroup).build();
        List<WorkOrderRecord> expectedList = List.of(record1, record2);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<WorkOrderRecord> results = workOrderExtractor.extractByAssignedGroup(assignedGroup, 0L);

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void checkExistence_validList_returnsExistingWorkOrders() {
        // Given
        List<String> workOrderIds = Arrays.asList("WO0000001", "WO0000002", "WO9999999");
        Set<String> expectedSet = Set.of("WO0000001", "WO0000002");

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedSet);

        // When
        Set<String> results = workOrderExtractor.checkExistence(workOrderIds);

        // Then
        assertThat(results).contains("WO0000001", "WO0000002");
        assertThat(results).doesNotContain("WO9999999");
    }

    @Test
    void checkExistence_emptyList_returnsEmptySet() {
        // When
        Set<String> results = workOrderExtractor.checkExistence(Collections.emptyList());

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void checkExistence_nullList_returnsEmptySet() {
        // When
        Set<String> results = workOrderExtractor.checkExistence(null);

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void extractWithQualification_nullFieldValues_handlesGracefully() {
        // Given
        WorkOrderRecord recordWithNulls = WorkOrderRecord.builder()
            .workOrderId("WO0000001")
            .summary(null)
            .description(null)
            .status(null)
            .build();
        List<WorkOrderRecord> expectedList = List.of(recordWithNulls);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<WorkOrderRecord> results = workOrderExtractor.extractWithQualification(null);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSummary()).isNull();
    }

    @Test
    void extractModifiedSince_zeroTimestamp_returnsAll() {
        // Given
        WorkOrderRecord mockRecord = WorkOrderRecord.builder()
            .workOrderId("WO0000001")
            .summary("Task")
            .build();
        List<WorkOrderRecord> expectedList = List.of(mockRecord);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<WorkOrderRecord> results = workOrderExtractor.extractModifiedSince(0L);

        // Then
        assertThat(results).hasSize(1);
    }

}
