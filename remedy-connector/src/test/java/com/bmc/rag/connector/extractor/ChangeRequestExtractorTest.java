package com.bmc.rag.connector.extractor;

import com.bmc.arsys.api.Entry;
import com.bmc.arsys.api.Timestamp;
import com.bmc.arsys.api.Value;
import com.bmc.rag.connector.config.RemedyConnectionConfig;
import com.bmc.rag.connector.connection.ThreadLocalARContext;
import com.bmc.rag.connector.model.ChangeRequestRecord;
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
 * Unit tests for ChangeRequestExtractor.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChangeRequestExtractorTest {

    @Mock
    private ThreadLocalARContext mockArContext;

    @Mock
    private RemedyConnectionConfig mockConfig;

    @InjectMocks
    private ChangeRequestExtractor changeRequestExtractor;

    @BeforeEach
    void setUp() {
        when(mockConfig.getChunkSize()).thenReturn(500);
    }

    @Test
    void extractModifiedSince_validTimestamp_returnsChangeRequests() {
        // Given
        long timestamp = 1672531200L;
        ChangeRequestRecord mockRecord = ChangeRequestRecord.builder()
            .changeId("CHG0000001")
            .summary("Database upgrade")
            .build();
        List<ChangeRequestRecord> expectedList = List.of(mockRecord);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<ChangeRequestRecord> results = changeRequestExtractor.extractModifiedSince(timestamp);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getChangeId()).isEqualTo("CHG0000001");
        assertThat(results.get(0).getSummary()).isEqualTo("Database upgrade");
    }

    @Test
    void extractWithQualification_nullQualification_returnsAllChangeRequests() {
        // Given
        ChangeRequestRecord record1 = ChangeRequestRecord.builder().changeId("CHG0000001").summary("Change 1").build();
        ChangeRequestRecord record2 = ChangeRequestRecord.builder().changeId("CHG0000002").summary("Change 2").build();
        List<ChangeRequestRecord> expectedList = List.of(record1, record2);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<ChangeRequestRecord> results = changeRequestExtractor.extractWithQualification(null);

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void extractWithQualification_validQualification_returnsFilteredChangeRequests() {
        // Given
        String qualification = "'1000000217' = \"Change Management\"";
        ChangeRequestRecord mockRecord = ChangeRequestRecord.builder()
            .changeId("CHG0000001")
            .summary("Critical infrastructure change")
            .assignedGroup("Change Management")
            .build();
        List<ChangeRequestRecord> expectedList = List.of(mockRecord);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<ChangeRequestRecord> results = changeRequestExtractor.extractWithQualification(qualification);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAssignedGroup()).isEqualTo("Change Management");
    }

    @Test
    void extractWithQualification_pagination_retrievesAllChunks() {
        // Given
        when(mockConfig.getChunkSize()).thenReturn(2);

        ChangeRequestRecord record1 = ChangeRequestRecord.builder().changeId("CHG0000001").summary("Change 1").build();
        ChangeRequestRecord record2 = ChangeRequestRecord.builder().changeId("CHG0000002").summary("Change 2").build();
        ChangeRequestRecord record3 = ChangeRequestRecord.builder().changeId("CHG0000003").summary("Change 3").build();
        List<ChangeRequestRecord> expectedList = List.of(record1, record2, record3);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<ChangeRequestRecord> results = changeRequestExtractor.extractWithQualification(null);

        // Then
        assertThat(results).hasSize(3);
    }

    @Test
    void extractWithQualification_emptyResult_returnsEmptyList() {
        // Given
        when(mockArContext.executeWithRetry(any())).thenReturn(Collections.emptyList());

        // When
        List<ChangeRequestRecord> results = changeRequestExtractor.extractWithQualification("'7' = 99");

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void extractByChangeId_found_returnsChangeRequest() {
        // Given
        ChangeRequestRecord mockRecord = ChangeRequestRecord.builder()
            .changeId("CHG0000001")
            .summary("Network infrastructure update")
            .build();
        List<ChangeRequestRecord> expectedList = List.of(mockRecord);
        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        Optional<ChangeRequestRecord> result = changeRequestExtractor.extractByChangeId("CHG0000001");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getChangeId()).isEqualTo("CHG0000001");
    }

    @Test
    void extractByChangeId_notFound_returnsEmpty() {
        // Given
        when(mockArContext.executeWithRetry(any())).thenReturn(Collections.emptyList());

        // When
        Optional<ChangeRequestRecord> result = changeRequestExtractor.extractByChangeId("CHG9999999");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void extractByAssignedGroup_withTimestamp_returnsFilteredChangeRequests() {
        // Given
        String assignedGroup = "Change Management";
        long timestamp = 1672531200L;

        ChangeRequestRecord mockRecord = ChangeRequestRecord.builder()
            .changeId("CHG0000001")
            .summary("Server upgrade")
            .assignedGroup(assignedGroup)
            .build();
        List<ChangeRequestRecord> expectedList = List.of(mockRecord);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<ChangeRequestRecord> results = changeRequestExtractor.extractByAssignedGroup(assignedGroup, timestamp);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAssignedGroup()).isEqualTo(assignedGroup);
    }

    @Test
    void extractByAssignedGroup_withoutTimestamp_returnsAllForGroup() {
        // Given
        String assignedGroup = "Change Management";

        ChangeRequestRecord record1 = ChangeRequestRecord.builder().changeId("CHG0000001").assignedGroup(assignedGroup).build();
        ChangeRequestRecord record2 = ChangeRequestRecord.builder().changeId("CHG0000002").assignedGroup(assignedGroup).build();
        List<ChangeRequestRecord> expectedList = List.of(record1, record2);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<ChangeRequestRecord> results = changeRequestExtractor.extractByAssignedGroup(assignedGroup, 0L);

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void extractCompletedChanges_withTimestamp_returnsCompletedOnly() {
        // Given
        long timestamp = 1672531200L;

        ChangeRequestRecord completedRecord = ChangeRequestRecord.builder()
            .changeId("CHG0000001")
            .summary("Completed change")
            .status(10) // CHG_COMPLETED
            .build();
        List<ChangeRequestRecord> expectedList = List.of(completedRecord);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<ChangeRequestRecord> results = changeRequestExtractor.extractCompletedChanges(timestamp);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(10);
    }

    @Test
    void checkExistence_validList_returnsExistingChangeRequests() {
        // Given
        List<String> changeIds = Arrays.asList("CHG0000001", "CHG0000002", "CHG9999999");
        Set<String> expectedSet = Set.of("CHG0000001", "CHG0000002");

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedSet);

        // When
        Set<String> results = changeRequestExtractor.checkExistence(changeIds);

        // Then
        assertThat(results).contains("CHG0000001", "CHG0000002");
        assertThat(results).doesNotContain("CHG9999999");
    }

    @Test
    void checkExistence_emptyList_returnsEmptySet() {
        // When
        Set<String> results = changeRequestExtractor.checkExistence(Collections.emptyList());

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void checkExistence_nullList_returnsEmptySet() {
        // When
        Set<String> results = changeRequestExtractor.checkExistence(null);

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void extractWithQualification_nullFieldValues_handlesGracefully() {
        // Given
        ChangeRequestRecord recordWithNulls = ChangeRequestRecord.builder()
            .changeId("CHG0000001")
            .summary(null)
            .description(null)
            .status(null)
            .build();
        List<ChangeRequestRecord> expectedList = List.of(recordWithNulls);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<ChangeRequestRecord> results = changeRequestExtractor.extractWithQualification(null);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSummary()).isNull();
    }

    @Test
    void extractModifiedSince_zeroTimestamp_returnsAll() {
        // Given
        ChangeRequestRecord mockRecord = ChangeRequestRecord.builder()
            .changeId("CHG0000001")
            .summary("Change")
            .build();
        List<ChangeRequestRecord> expectedList = List.of(mockRecord);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<ChangeRequestRecord> results = changeRequestExtractor.extractModifiedSince(0L);

        // Then
        assertThat(results).hasSize(1);
    }

    @Test
    void extractCompletedChanges_emptyResult_returnsEmptyList() {
        // Given
        when(mockArContext.executeWithRetry(any())).thenReturn(Collections.emptyList());

        // When
        List<ChangeRequestRecord> results = changeRequestExtractor.extractCompletedChanges(1672531200L);

        // Then
        assertThat(results).isEmpty();
    }

}
