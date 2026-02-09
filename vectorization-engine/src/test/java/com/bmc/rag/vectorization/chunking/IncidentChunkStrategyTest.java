package com.bmc.rag.vectorization.chunking;

import com.bmc.rag.connector.model.IncidentRecord;
import com.bmc.rag.connector.model.WorkLogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Tests for IncidentChunkStrategy.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IncidentChunkStrategyTest {

    @Mock
    private SemanticChunker semanticChunker;

    private IncidentChunkStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new IncidentChunkStrategy(semanticChunker);

        // Default mock behavior: return single chunk with input text
        when(semanticChunker.splitTextWithContext(anyString(), anyString()))
            .thenAnswer(invocation -> {
                String text = invocation.getArgument(0);
                String context = invocation.getArgument(1);
                return List.of(context + "\n\n" + text);
            });
    }

    @Test
    void getRecordType_returnsIncident() {
        // When
        String recordType = strategy.getRecordType();

        // Then
        assertThat(recordType).isEqualTo("Incident");
    }

    @Test
    void chunk_minimalIncident_createsSummaryChunk() {
        // Given
        IncidentRecord incident = IncidentRecord.builder()
            .entryId("000000000000001")
            .incidentNumber("INC000000000001")
            .summary("Network issue")
            .statusDisplayValue("New")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(incident);

        // Then
        assertThat(chunks).hasSize(1);
        TextChunk summaryChunk = chunks.get(0);
        assertThat(summaryChunk.getChunkType()).isEqualTo(TextChunk.ChunkType.SUMMARY);
        assertThat(summaryChunk.getSourceType()).isEqualTo("Incident");
        assertThat(summaryChunk.getSourceId()).isEqualTo("INC000000000001");
        assertThat(summaryChunk.getEntryId()).isEqualTo("000000000000001");
        assertThat(summaryChunk.getSequenceNumber()).isZero();
    }

    @Test
    void chunk_incidentWithDescription_createsDescriptionChunks() {
        // Given
        IncidentRecord incident = IncidentRecord.builder()
            .entryId("000000000000001")
            .incidentNumber("INC000000000001")
            .summary("Network issue")
            .description("User cannot connect to network. Error: 'No network access'.")
            .statusDisplayValue("In Progress")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(incident);

        // Then
        assertThat(chunks).hasSizeGreaterThanOrEqualTo(2);
        assertThat(chunks.stream().filter(c -> c.getChunkType() == TextChunk.ChunkType.SUMMARY))
            .hasSize(1);
        assertThat(chunks.stream().filter(c -> c.getChunkType() == TextChunk.ChunkType.DESCRIPTION))
            .hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void chunk_incidentWithResolution_createsHighValueResolutionChunk() {
        // Given
        IncidentRecord incident = IncidentRecord.builder()
            .entryId("000000000000001")
            .incidentNumber("INC000000000001")
            .summary("Network issue")
            .description("User cannot connect.")
            .resolution("Resolved by restarting the network switch. Connectivity restored.")
            .statusDisplayValue("Resolved")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(incident);

        // Then
        List<TextChunk> resolutionChunks = chunks.stream()
            .filter(c -> c.getChunkType() == TextChunk.ChunkType.RESOLUTION)
            .toList();

        assertThat(resolutionChunks).hasSizeGreaterThanOrEqualTo(1);
        TextChunk resolutionChunk = resolutionChunks.get(0);
        assertThat(resolutionChunk.getMetadata()).containsEntry("chunk_priority", "high");
        assertThat(resolutionChunk.getContent()).contains("Resolution for:");
    }

    @Test
    void chunk_populatesITSMMetadata() {
        // Given
        IncidentRecord incident = IncidentRecord.builder()
            .entryId("000000000000001")
            .incidentNumber("INC000000000001")
            .summary("Network connectivity issue")
            .assignedGroup("Network Support")
            .categoryTier1("Infrastructure")
            .categoryTier2("Network")
            .statusDisplayValue("In Progress")
            .customerCompany("Acme Corp")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(incident);

        // Then
        assertThat(chunks).isNotEmpty();
        TextChunk firstChunk = chunks.get(0);
        assertThat(firstChunk.getMetadata()).containsEntry("source_type", "Incident");
        assertThat(firstChunk.getMetadata()).containsEntry("source_id", "INC000000000001");
        assertThat(firstChunk.getMetadata()).containsEntry("title", "Network connectivity issue");
        assertThat(firstChunk.getMetadata()).containsEntry("assigned_group", "Network Support");
        assertThat(firstChunk.getMetadata()).containsEntry("category", "Infrastructure > Network");
        assertThat(firstChunk.getMetadata()).containsEntry("status", "In Progress");
        assertThat(firstChunk.getMetadata()).containsEntry("customer_company", "Acme Corp");
    }

    @Test
    void chunk_withWorkLogs_createsWorkLogChunks() {
        // Given
        Instant now = Instant.now();
        WorkLogEntry log1 = WorkLogEntry.builder()
            .workLogId("WL001")
            .detailedDescription("Initial troubleshooting started.")
            .submitter("John Doe")
            .submitDate(now)
            .build();

        WorkLogEntry log2 = WorkLogEntry.builder()
            .workLogId("WL002")
            .detailedDescription("Issue escalated to network team.")
            .submitter("Jane Smith")
            .submitDate(now.plusSeconds(3600))
            .build();

        IncidentRecord incident = IncidentRecord.builder()
            .entryId("000000000000001")
            .incidentNumber("INC000000000001")
            .summary("Network issue")
            .workLogs(List.of(log1, log2))
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(incident);

        // Then
        List<TextChunk> workLogChunks = chunks.stream()
            .filter(c -> c.getChunkType() == TextChunk.ChunkType.WORK_LOG)
            .toList();

        assertThat(workLogChunks).hasSizeGreaterThanOrEqualTo(1);
        TextChunk workLogChunk = workLogChunks.get(0);
        assertThat(workLogChunk.getMetadata()).containsKey("work_log_date");
    }

    @Test
    void chunk_workLogsGroupedByDay_createsChunksPerDay() {
        // Given - use fixed dates to avoid timezone sensitivity
        ZoneId testZone = ZoneId.of("UTC");
        LocalDate fixedDate = LocalDate.of(2024, 6, 15);
        Instant today = fixedDate.atStartOfDay(testZone).plusHours(12).toInstant();
        Instant yesterday = fixedDate.minusDays(1).atStartOfDay(testZone).plusHours(12).toInstant();

        WorkLogEntry log1 = WorkLogEntry.builder()
            .workLogId("WL001")
            .detailedDescription("Today's log")
            .submitDate(today)
            .build();

        WorkLogEntry log2 = WorkLogEntry.builder()
            .workLogId("WL002")
            .detailedDescription("Yesterday's log")
            .submitDate(yesterday)
            .build();

        IncidentRecord incident = IncidentRecord.builder()
            .entryId("000000000000001")
            .incidentNumber("INC000000000001")
            .summary("Network issue")
            .workLogs(List.of(log1, log2))
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(incident);

        // Then
        List<TextChunk> workLogChunks = chunks.stream()
            .filter(c -> c.getChunkType() == TextChunk.ChunkType.WORK_LOG)
            .toList();

        assertThat(workLogChunks).hasSizeGreaterThanOrEqualTo(2);

        LocalDate todayDate = today.atZone(testZone).toLocalDate();
        LocalDate yesterdayDate = yesterday.atZone(testZone).toLocalDate();

        boolean hasTodayChunk = workLogChunks.stream()
            .anyMatch(c -> todayDate.toString().equals(c.getMetadata().get("work_log_date")));
        boolean hasYesterdayChunk = workLogChunks.stream()
            .anyMatch(c -> yesterdayDate.toString().equals(c.getMetadata().get("work_log_date")));

        assertThat(hasTodayChunk).isTrue();
        assertThat(hasYesterdayChunk).isTrue();
    }

    @Test
    void chunk_nullDescription_doesNotCreateDescriptionChunks() {
        // Given
        IncidentRecord incident = IncidentRecord.builder()
            .entryId("000000000000001")
            .incidentNumber("INC000000000001")
            .summary("Network issue")
            .description(null)
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(incident);

        // Then
        assertThat(chunks.stream().filter(c -> c.getChunkType() == TextChunk.ChunkType.DESCRIPTION))
            .isEmpty();
    }

    @Test
    void chunk_emptyDescription_doesNotCreateDescriptionChunks() {
        // Given
        IncidentRecord incident = IncidentRecord.builder()
            .entryId("000000000000001")
            .incidentNumber("INC000000000001")
            .summary("Network issue")
            .description("")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(incident);

        // Then
        assertThat(chunks.stream().filter(c -> c.getChunkType() == TextChunk.ChunkType.DESCRIPTION))
            .isEmpty();
    }

    @Test
    void chunk_emptyWorkLogs_doesNotCreateWorkLogChunks() {
        // Given
        IncidentRecord incident = IncidentRecord.builder()
            .entryId("000000000000001")
            .incidentNumber("INC000000000001")
            .summary("Network issue")
            .workLogs(new ArrayList<>())
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(incident);

        // Then
        assertThat(chunks.stream().filter(c -> c.getChunkType() == TextChunk.ChunkType.WORK_LOG))
            .isEmpty();
    }

    @Test
    void chunk_workLogWithoutContent_skipsWorkLog() {
        // Given
        WorkLogEntry emptyLog = WorkLogEntry.builder()
            .workLogId("WL001")
            .detailedDescription("")
            .build();

        IncidentRecord incident = IncidentRecord.builder()
            .entryId("000000000000001")
            .incidentNumber("INC000000000001")
            .summary("Network issue")
            .workLogs(List.of(emptyLog))
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(incident);

        // Then
        assertThat(chunks.stream().filter(c -> c.getChunkType() == TextChunk.ChunkType.WORK_LOG))
            .isEmpty();
    }

    @Test
    void chunk_sequenceNumbersIncrement_correctly() {
        // Given
        IncidentRecord incident = IncidentRecord.builder()
            .entryId("000000000000001")
            .incidentNumber("INC000000000001")
            .summary("Network issue")
            .description("Description text")
            .resolution("Resolution text")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(incident);

        // Then
        assertThat(chunks).hasSizeGreaterThanOrEqualTo(3);
        for (int i = 0; i < chunks.size(); i++) {
            assertThat(chunks.get(i).getSequenceNumber()).isEqualTo(i);
        }
    }

    @Test
    void chunk_contextPrefixIncludesIncidentNumber_andSummary() {
        // Given
        IncidentRecord incident = IncidentRecord.builder()
            .entryId("000000000000001")
            .incidentNumber("INC000000000001")
            .summary("Network connectivity issue")
            .description("Description text")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(incident);

        // Then
        TextChunk summaryChunk = chunks.stream()
            .filter(c -> c.getChunkType() == TextChunk.ChunkType.SUMMARY)
            .findFirst()
            .orElseThrow();

        assertThat(summaryChunk.getContent()).contains("Incident INC000000000001");
        assertThat(summaryChunk.getContent()).contains("Network connectivity issue");
    }

    @Test
    void chunk_categoryPath_includesAllTiers() {
        // Given
        IncidentRecord incident = IncidentRecord.builder()
            .entryId("000000000000001")
            .incidentNumber("INC000000000001")
            .summary("Issue")
            .categoryTier1("Infrastructure")
            .categoryTier2("Network")
            .categoryTier3("Connectivity")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(incident);

        // Then
        assertThat(chunks).isNotEmpty();
        TextChunk firstChunk = chunks.get(0);
        assertThat(firstChunk.getMetadata()).containsEntry("category", "Infrastructure > Network > Connectivity");
    }

    @Test
    void chunk_fullIncidentRecord_createsAllChunkTypes() {
        // Given
        WorkLogEntry workLog = WorkLogEntry.builder()
            .workLogId("WL001")
            .detailedDescription("Work log entry")
            .submitDate(Instant.now())
            .build();

        IncidentRecord incident = IncidentRecord.builder()
            .entryId("000000000000001")
            .incidentNumber("INC000000000001")
            .summary("Network connectivity issue")
            .description("Users cannot access shared drives. Network timeout errors.")
            .resolution("Resolved by restarting network switch in building 5.")
            .statusDisplayValue("Resolved")
            .assignedGroup("Network Support")
            .categoryTier1("Infrastructure")
            .categoryTier2("Network")
            .customerCompany("Acme Corp")
            .workLogs(List.of(workLog))
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(incident);

        // Then
        assertThat(chunks).hasSizeGreaterThanOrEqualTo(4);
        assertThat(chunks.stream().anyMatch(c -> c.getChunkType() == TextChunk.ChunkType.SUMMARY)).isTrue();
        assertThat(chunks.stream().anyMatch(c -> c.getChunkType() == TextChunk.ChunkType.DESCRIPTION)).isTrue();
        assertThat(chunks.stream().anyMatch(c -> c.getChunkType() == TextChunk.ChunkType.RESOLUTION)).isTrue();
        assertThat(chunks.stream().anyMatch(c -> c.getChunkType() == TextChunk.ChunkType.WORK_LOG)).isTrue();

        // Verify all chunks have proper IDs
        for (TextChunk chunk : chunks) {
            assertThat(chunk.getChunkId()).isNotNull();
            assertThat(chunk.getChunkId()).contains("incident:");
            assertThat(chunk.getChunkId()).contains("INC000000000001");
        }
    }
}
