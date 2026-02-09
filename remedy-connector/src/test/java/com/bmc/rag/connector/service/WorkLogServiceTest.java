package com.bmc.rag.connector.service;

import com.bmc.rag.connector.connection.ThreadLocalARContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WorkLogService.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkLogServiceTest {

    @Mock
    private ThreadLocalARContext mockArContext;

    @InjectMocks
    private WorkLogService workLogService;

    @Test
    void getWorkLogsForIncident_validIncident_returnsWorkLogs() {
        // Given
        String incidentNumber = "INC000001";
        Instant now = Instant.now();
        WorkLogService.WorkLogEntry expectedEntry = new WorkLogService.WorkLogEntry(
            "WL-001",
            incidentNumber,
            1,
            "Working Log",
            "Work log text",
            "john.doe",
            now,
            "Internal"
        );
        List<WorkLogService.WorkLogEntry> expectedList = List.of(expectedEntry);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<WorkLogService.WorkLogEntry> workLogs = workLogService.getWorkLogsForIncident(incidentNumber);

        // Then
        assertThat(workLogs).hasSize(1);
        assertThat(workLogs.get(0).workLogId()).isEqualTo("WL-001");
        assertThat(workLogs.get(0).incidentNumber()).isEqualTo(incidentNumber);
        assertThat(workLogs.get(0).description()).isEqualTo("Work log text");
        assertThat(workLogs.get(0).submitter()).isEqualTo("john.doe");
    }

    @Test
    void getWorkLogsForIncident_nullIncidentNumber_returnsEmptyList() {
        // When
        List<WorkLogService.WorkLogEntry> workLogs = workLogService.getWorkLogsForIncident(null);

        // Then
        assertThat(workLogs).isEmpty();
    }

    @Test
    void getWorkLogsForIncident_blankIncidentNumber_returnsEmptyList() {
        // When
        List<WorkLogService.WorkLogEntry> workLogs = workLogService.getWorkLogsForIncident("  ");

        // Then
        assertThat(workLogs).isEmpty();
    }

    @Test
    void getWorkLogsForIncident_noWorkLogs_returnsEmptyList() {
        // Given
        String incidentNumber = "INC000001";
        when(mockArContext.executeWithRetry(any())).thenReturn(Collections.emptyList());

        // When
        List<WorkLogService.WorkLogEntry> workLogs = workLogService.getWorkLogsForIncident(incidentNumber);

        // Then
        assertThat(workLogs).isEmpty();
    }

    @Test
    void workLogEntry_getFormattedDate_formatsCorrectly() {
        // Given
        Instant now = Instant.now();
        WorkLogService.WorkLogEntry entry = new WorkLogService.WorkLogEntry(
            "WL-001", "INC000001", 1, "Working Log", "Description", "user", now, "Internal"
        );

        // When
        String formatted = entry.getFormattedDate();

        // Then
        assertThat(formatted).isNotEmpty();
        assertThat(formatted).isEqualTo(now.toString());
    }

    @Test
    void workLogEntry_getPreview_truncatesLongText() {
        // Given
        String longDescription = "A".repeat(150);
        WorkLogService.WorkLogEntry entry = new WorkLogService.WorkLogEntry(
            "WL-001", "INC000001", 1, "Working Log", longDescription, "user", Instant.now(), "Internal"
        );

        // When
        String preview = entry.getPreview(100);

        // Then
        assertThat(preview).hasSize(103); // 100 + "..."
        assertThat(preview).endsWith("...");
    }

    @Test
    void workLogEntry_getPreview_shortText_doesNotTruncate() {
        // Given
        String shortDescription = "Short text";
        WorkLogService.WorkLogEntry entry = new WorkLogService.WorkLogEntry(
            "WL-001", "INC000001", 1, "Working Log", shortDescription, "user", Instant.now(), "Internal"
        );

        // When
        String preview = entry.getPreview(100);

        // Then
        assertThat(preview).isEqualTo(shortDescription);
    }

    @Test
    void workLogEntry_getFormattedDate_nullDate_returnsEmpty() {
        WorkLogService.WorkLogEntry entry = new WorkLogService.WorkLogEntry(
            "WL-001", "INC000001", 1, "Working Log", "Desc", "user", null, "Internal"
        );
        assertThat(entry.getFormattedDate()).isEmpty();
    }

    @Test
    void getWorkLogsForIncident_exceptionDuringQuery_returnsEmptyList() {
        // Given
        when(mockArContext.executeWithRetry(any()))
            .thenThrow(new RuntimeException("Query failed"));

        // When
        List<WorkLogService.WorkLogEntry> result = workLogService.getWorkLogsForIncident("INC000001");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void workLogEntry_getPreview_nullDescription_returnsEmpty() {
        WorkLogService.WorkLogEntry entry = new WorkLogService.WorkLogEntry(
            "WL-001", "INC000001", 1, "Working Log", null, "user", Instant.now(), "Internal"
        );
        assertThat(entry.getPreview(100)).isEmpty();
    }

    @Test
    void workLogEntry_getPreview_exactLength_doesNotTruncate() {
        String exactText = "A".repeat(100);
        WorkLogService.WorkLogEntry entry = new WorkLogService.WorkLogEntry(
            "WL-001", "INC000001", 1, "Working Log", exactText, "user", Instant.now(), "Internal"
        );
        assertThat(entry.getPreview(100)).isEqualTo(exactText);
    }

    @Test
    void getWorkLogsForIncident_exception_returnsEmptyList() {
        when(mockArContext.executeWithRetry(any())).thenThrow(new RuntimeException("Connection error"));
        List<WorkLogService.WorkLogEntry> workLogs = workLogService.getWorkLogsForIncident("INC000001");
        assertThat(workLogs).isEmpty();
    }

    @Test
    void getWorkLogsForIncident_multipleWorkLogs_returnsAll() {
        Instant now = Instant.now();
        List<WorkLogService.WorkLogEntry> expectedList = List.of(
            new WorkLogService.WorkLogEntry("WL-001", "INC000001", 1, "Working Log", "First entry", "john.doe", now, "Internal"),
            new WorkLogService.WorkLogEntry("WL-002", "INC000001", 3, "Customer Communication", "Second entry", "jane.doe", now.minusSeconds(3600), "Public")
        );
        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        List<WorkLogService.WorkLogEntry> workLogs = workLogService.getWorkLogsForIncident("INC000001");
        assertThat(workLogs).hasSize(2);
        assertThat(workLogs.get(0).workLogTypeName()).isEqualTo("Working Log");
        assertThat(workLogs.get(1).viewAccess()).isEqualTo("Public");
    }

    @Test
    void workLogEntry_recordFields_accessible() {
        Instant now = Instant.now();
        WorkLogService.WorkLogEntry entry = new WorkLogService.WorkLogEntry(
            "WL-001", "INC000001", 3, "Customer Communication", "Test", "user", now, "Public"
        );
        assertThat(entry.workLogId()).isEqualTo("WL-001");
        assertThat(entry.incidentNumber()).isEqualTo("INC000001");
        assertThat(entry.workLogType()).isEqualTo(3);
        assertThat(entry.workLogTypeName()).isEqualTo("Customer Communication");
        assertThat(entry.description()).isEqualTo("Test");
        assertThat(entry.submitter()).isEqualTo("user");
        assertThat(entry.submitDate()).isEqualTo(now);
        assertThat(entry.viewAccess()).isEqualTo("Public");
    }

}
