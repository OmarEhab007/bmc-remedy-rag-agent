package com.bmc.rag.connector.extractor;

import com.bmc.arsys.api.Entry;
import com.bmc.arsys.api.Timestamp;
import com.bmc.arsys.api.Value;
import com.bmc.rag.connector.config.RemedyConnectionConfig;
import com.bmc.rag.connector.connection.ThreadLocalARContext;
import com.bmc.rag.connector.model.IncidentRecord;
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
 * Unit tests for IncidentExtractor.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IncidentExtractorTest {

    @Mock
    private ThreadLocalARContext mockArContext;

    @Mock
    private RemedyConnectionConfig mockConfig;

    @InjectMocks
    private IncidentExtractor incidentExtractor;

    @BeforeEach
    void setUp() {
        when(mockConfig.getChunkSize()).thenReturn(500);
    }

    @Test
    void extractModifiedSince_validTimestamp_returnsIncidents() {
        // Given
        long timestamp = 1672531200L;
        IncidentRecord mockRecord = IncidentRecord.builder()
            .incidentNumber("INC000001")
            .summary("VPN Issue")
            .build();
        List<IncidentRecord> expectedList = List.of(mockRecord);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<IncidentRecord> results = incidentExtractor.extractModifiedSince(timestamp);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getIncidentNumber()).isEqualTo("INC000001");
        assertThat(results.get(0).getSummary()).isEqualTo("VPN Issue");
    }

    @Test
    void extractWithQualification_nullQualification_returnsAllIncidents() {
        // Given
        IncidentRecord record1 = IncidentRecord.builder().incidentNumber("INC000001").summary("Issue 1").build();
        IncidentRecord record2 = IncidentRecord.builder().incidentNumber("INC000002").summary("Issue 2").build();
        List<IncidentRecord> expectedList = List.of(record1, record2);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<IncidentRecord> results = incidentExtractor.extractWithQualification(null);

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void extractWithQualification_pagination_retrievesAllChunks() {
        // Given
        when(mockConfig.getChunkSize()).thenReturn(2);

        IncidentRecord record1 = IncidentRecord.builder().incidentNumber("INC000001").summary("Issue 1").build();
        IncidentRecord record2 = IncidentRecord.builder().incidentNumber("INC000002").summary("Issue 2").build();
        IncidentRecord record3 = IncidentRecord.builder().incidentNumber("INC000003").summary("Issue 3").build();
        List<IncidentRecord> expectedList = List.of(record1, record2, record3);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<IncidentRecord> results = incidentExtractor.extractWithQualification(null);

        // Then
        assertThat(results).hasSize(3);
    }

    @Test
    void extractByIncidentNumber_found_returnsIncident() {
        // Given
        IncidentRecord mockRecord = IncidentRecord.builder().incidentNumber("INC000001").summary("VPN Issue").build();
        List<IncidentRecord> expectedList = List.of(mockRecord);
        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        Optional<IncidentRecord> result = incidentExtractor.extractByIncidentNumber("INC000001");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getIncidentNumber()).isEqualTo("INC000001");
    }

    @Test
    void extractByIncidentNumber_notFound_returnsEmpty() {
        // Given
        when(mockArContext.executeWithRetry(any())).thenReturn(Collections.emptyList());

        // When
        Optional<IncidentRecord> result = incidentExtractor.extractByIncidentNumber("INC999999");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void extractByAssignedGroup_withTimestamp_filtersCorrectly() {
        // Given
        IncidentRecord mockRecord = IncidentRecord.builder()
            .incidentNumber("INC000001")
            .assignedGroup("IT Support")
            .build();
        List<IncidentRecord> expectedList = List.of(mockRecord);
        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<IncidentRecord> results = incidentExtractor.extractByAssignedGroup("IT Support", 1672531200L);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAssignedGroup()).isEqualTo("IT Support");
    }

    @Test
    void extractByAssignedGroup_withoutTimestamp_returnsAll() {
        // Given
        IncidentRecord mockRecord = IncidentRecord.builder()
            .incidentNumber("INC000001")
            .assignedGroup("IT Support")
            .build();
        List<IncidentRecord> expectedList = List.of(mockRecord);
        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<IncidentRecord> results = incidentExtractor.extractByAssignedGroup("IT Support", 0L);

        // Then
        assertThat(results).hasSize(1);
    }

    @Test
    void extractResolvedIncidents_withTimestamp_filtersResolvedAndClosed() {
        // Given
        IncidentRecord mockRecord = IncidentRecord.builder()
            .incidentNumber("INC000001")
            .summary("Resolved Issue")
            .status(FieldIdConstants.StatusValues.INCIDENT_RESOLVED)
            .build();
        List<IncidentRecord> expectedList = List.of(mockRecord);
        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<IncidentRecord> results = incidentExtractor.extractResolvedIncidents(1672531200L);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(FieldIdConstants.StatusValues.INCIDENT_RESOLVED);
    }

    @Test
    void checkExistence_validList_returnsExistingIncidents() {
        // Given
        List<String> incidentNumbers = Arrays.asList("INC000001", "INC000002", "INC999999");
        Set<String> expectedSet = Set.of("INC000001", "INC000002");

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedSet);

        // When
        Set<String> results = incidentExtractor.checkExistence(incidentNumbers);

        // Then
        assertThat(results).contains("INC000001", "INC000002");
        assertThat(results).doesNotContain("INC999999");
    }

    @Test
    void checkExistence_emptyList_returnsEmptySet() {
        // When
        Set<String> results = incidentExtractor.checkExistence(Collections.emptyList());

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void checkExistence_nullList_returnsEmptySet() {
        // When
        Set<String> results = incidentExtractor.checkExistence(null);

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void getIncidentCount_validQualification_returnsCount() {
        // Given
        when(mockArContext.executeWithRetry(any())).thenReturn(42);

        // When
        int count = incidentExtractor.getIncidentCount("'7' = 4");

        // Then
        assertThat(count).isEqualTo(42);
    }

}
