package com.bmc.rag.vectorization.chunking;

import com.bmc.rag.connector.model.WorkLogEntry;
import com.bmc.rag.connector.model.WorkOrderRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Tests for WorkOrderChunkStrategy.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkOrderChunkStrategyTest {

    @Mock
    private SemanticChunker semanticChunker;

    private WorkOrderChunkStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new WorkOrderChunkStrategy(semanticChunker);

        // Default mock behavior
        when(semanticChunker.splitTextWithContext(anyString(), anyString()))
            .thenAnswer(invocation -> {
                String text = invocation.getArgument(0);
                String context = invocation.getArgument(1);
                return List.of(context + "\n\n" + text);
            });
    }

    @Test
    void getRecordType_returnsWorkOrder() {
        // When
        String recordType = strategy.getRecordType();

        // Then
        assertThat(recordType).isEqualTo("WorkOrder");
    }

    @Test
    void chunk_minimalWorkOrder_createsSummaryChunk() {
        // Given
        WorkOrderRecord workOrder = WorkOrderRecord.builder()
            .entryId("000000000000001")
            .workOrderId("WO000000000001")
            .summary("Install new printer")
            .statusDisplayValue("New")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(workOrder);

        // Then
        assertThat(chunks).hasSize(1);
        TextChunk summaryChunk = chunks.get(0);
        assertThat(summaryChunk.getChunkType()).isEqualTo(TextChunk.ChunkType.SUMMARY);
        assertThat(summaryChunk.getSourceType()).isEqualTo("WorkOrder");
        assertThat(summaryChunk.getSourceId()).isEqualTo("WO000000000001");
        assertThat(summaryChunk.getEntryId()).isEqualTo("000000000000001");
    }

    @Test
    void chunk_withDescription_createsDescriptionChunks() {
        // Given
        WorkOrderRecord workOrder = WorkOrderRecord.builder()
            .entryId("000000000000001")
            .workOrderId("WO000000000001")
            .summary("Install new printer")
            .description("Install HP LaserJet printer in conference room 3B. Connect to network and test printing.")
            .statusDisplayValue("Assigned")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(workOrder);

        // Then
        assertThat(chunks).hasSizeGreaterThanOrEqualTo(2);
        assertThat(chunks.stream().anyMatch(c -> c.getChunkType() == TextChunk.ChunkType.SUMMARY)).isTrue();
        assertThat(chunks.stream().anyMatch(c -> c.getChunkType() == TextChunk.ChunkType.DESCRIPTION)).isTrue();
    }

    @Test
    void chunk_populatesWorkOrderMetadata() {
        // Given
        WorkOrderRecord workOrder = WorkOrderRecord.builder()
            .entryId("000000000000001")
            .workOrderId("WO000000000001")
            .summary("Install new printer")
            .assignedGroup("Facilities")
            .categoryTier1("Hardware")
            .categoryTier2("Printer")
            .statusDisplayValue("In Progress")
            .requesterFirstName("John")
            .requesterLastName("Smith")
            .locationCompany("Acme Corp HQ")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(workOrder);

        // Then
        assertThat(chunks).isNotEmpty();
        TextChunk firstChunk = chunks.get(0);
        assertThat(firstChunk.getMetadata()).containsEntry("source_type", "WorkOrder");
        assertThat(firstChunk.getMetadata()).containsEntry("source_id", "WO000000000001");
        assertThat(firstChunk.getMetadata()).containsEntry("title", "Install new printer");
        assertThat(firstChunk.getMetadata()).containsEntry("assigned_group", "Facilities");
        assertThat(firstChunk.getMetadata()).containsEntry("category", "Hardware > Printer");
        assertThat(firstChunk.getMetadata()).containsEntry("status", "In Progress");
        assertThat(firstChunk.getMetadata()).containsEntry("requester", "John Smith");
        assertThat(firstChunk.getMetadata()).containsEntry("location_company", "Acme Corp HQ");
    }

    @Test
    void chunk_withWorkLogs_createsWorkLogChunks() {
        // Given
        WorkLogEntry log = WorkLogEntry.builder()
            .workLogId("WL001")
            .detailedDescription("Printer installed and tested successfully.")
            .submitDate(Instant.now())
            .build();

        WorkOrderRecord workOrder = WorkOrderRecord.builder()
            .entryId("000000000000001")
            .workOrderId("WO000000000001")
            .summary("Install new printer")
            .workLogs(List.of(log))
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(workOrder);

        // Then
        assertThat(chunks.stream().anyMatch(c -> c.getChunkType() == TextChunk.ChunkType.WORK_LOG)).isTrue();
    }

    @Test
    void chunk_nullDescription_doesNotCreateDescriptionChunks() {
        // Given
        WorkOrderRecord workOrder = WorkOrderRecord.builder()
            .entryId("000000000000001")
            .workOrderId("WO000000000001")
            .summary("Install new printer")
            .description(null)
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(workOrder);

        // Then
        assertThat(chunks.stream().filter(c -> c.getChunkType() == TextChunk.ChunkType.DESCRIPTION))
            .isEmpty();
    }

    @Test
    void chunk_emptyDescription_doesNotCreateDescriptionChunks() {
        // Given
        WorkOrderRecord workOrder = WorkOrderRecord.builder()
            .entryId("000000000000001")
            .workOrderId("WO000000000001")
            .summary("Install new printer")
            .description("")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(workOrder);

        // Then
        assertThat(chunks.stream().filter(c -> c.getChunkType() == TextChunk.ChunkType.DESCRIPTION))
            .isEmpty();
    }

    @Test
    void chunk_emptyWorkLogs_doesNotCreateWorkLogChunks() {
        // Given
        WorkOrderRecord workOrder = WorkOrderRecord.builder()
            .entryId("000000000000001")
            .workOrderId("WO000000000001")
            .summary("Install new printer")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(workOrder);

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

        WorkOrderRecord workOrder = WorkOrderRecord.builder()
            .entryId("000000000000001")
            .workOrderId("WO000000000001")
            .summary("Install new printer")
            .workLogs(List.of(emptyLog))
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(workOrder);

        // Then
        assertThat(chunks.stream().filter(c -> c.getChunkType() == TextChunk.ChunkType.WORK_LOG))
            .isEmpty();
    }

    @Test
    void chunk_sequenceNumbersIncrement_correctly() {
        // Given
        WorkOrderRecord workOrder = WorkOrderRecord.builder()
            .entryId("000000000000001")
            .workOrderId("WO000000000001")
            .summary("Install new printer")
            .description("Install and configure printer")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(workOrder);

        // Then
        assertThat(chunks).hasSizeGreaterThanOrEqualTo(2);
        for (int i = 0; i < chunks.size(); i++) {
            assertThat(chunks.get(i).getSequenceNumber()).isEqualTo(i);
        }
    }

    @Test
    void chunk_contextPrefix_includesWorkOrderIdAndSummary() {
        // Given
        WorkOrderRecord workOrder = WorkOrderRecord.builder()
            .entryId("000000000000001")
            .workOrderId("WO000000000001")
            .summary("Install new printer")
            .description("Installation details")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(workOrder);

        // Then
        TextChunk summaryChunk = chunks.stream()
            .filter(c -> c.getChunkType() == TextChunk.ChunkType.SUMMARY)
            .findFirst()
            .orElseThrow();

        assertThat(summaryChunk.getContent()).contains("Work Order WO000000000001");
        assertThat(summaryChunk.getContent()).contains("Install new printer");
    }

    @Test
    void chunk_categoryPath_includesAllTiers() {
        // Given
        WorkOrderRecord workOrder = WorkOrderRecord.builder()
            .entryId("000000000000001")
            .workOrderId("WO000000000001")
            .summary("Install new printer")
            .categoryTier1("Hardware")
            .categoryTier2("Printer")
            .categoryTier3("Laser")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(workOrder);

        // Then
        assertThat(chunks).isNotEmpty();
        TextChunk firstChunk = chunks.get(0);
        assertThat(firstChunk.getMetadata()).containsEntry("category", "Hardware > Printer > Laser");
    }

    @Test
    void chunk_fullWorkOrder_createsAllChunks() {
        // Given
        WorkLogEntry workLog = WorkLogEntry.builder()
            .workLogId("WL001")
            .detailedDescription("Printer installed successfully. Tested color and BW printing.")
            .submitDate(Instant.now())
            .build();

        WorkOrderRecord workOrder = WorkOrderRecord.builder()
            .entryId("000000000000001")
            .workOrderId("WO000000000001")
            .summary("Install HP LaserJet printer")
            .description("Install HP LaserJet Pro M404dn in conference room 3B. Connect to corporate network. Configure duplex printing.")
            .statusDisplayValue("Completed")
            .assignedGroup("IT Hardware")
            .categoryTier1("Hardware")
            .categoryTier2("Printer")
            .requesterFirstName("Jane")
            .requesterLastName("Doe")
            .locationCompany("Acme Corp - Building 5")
            .workLogs(List.of(workLog))
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(workOrder);

        // Then
        assertThat(chunks).hasSizeGreaterThanOrEqualTo(3);
        assertThat(chunks.stream().anyMatch(c -> c.getChunkType() == TextChunk.ChunkType.SUMMARY)).isTrue();
        assertThat(chunks.stream().anyMatch(c -> c.getChunkType() == TextChunk.ChunkType.DESCRIPTION)).isTrue();
        assertThat(chunks.stream().anyMatch(c -> c.getChunkType() == TextChunk.ChunkType.WORK_LOG)).isTrue();

        // Verify metadata
        TextChunk firstChunk = chunks.get(0);
        assertThat(firstChunk.getMetadata()).containsEntry("requester", "Jane Doe");
        assertThat(firstChunk.getMetadata()).containsEntry("location_company", "Acme Corp - Building 5");
        assertThat(firstChunk.getMetadata()).containsEntry("assigned_group", "IT Hardware");
    }

    @Test
    void chunk_nullMetadataFields_doesNotIncludeInMetadata() {
        // Given
        WorkOrderRecord workOrder = WorkOrderRecord.builder()
            .entryId("000000000000001")
            .workOrderId("WO000000000001")
            .summary("Install new printer")
            .requesterFirstName(null)
            .requesterLastName(null)
            .locationCompany(null)
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(workOrder);

        // Then
        assertThat(chunks).isNotEmpty();
        TextChunk firstChunk = chunks.get(0);
        assertThat(firstChunk.getMetadata()).doesNotContainKey("requester");
        assertThat(firstChunk.getMetadata()).doesNotContainKey("location_company");
    }

    @Test
    void chunk_multipleWorkLogs_groupsByDay() {
        // Given
        Instant now = Instant.now();
        WorkLogEntry log1 = WorkLogEntry.builder()
            .workLogId("WL001")
            .detailedDescription("Started installation")
            .submitDate(now)
            .build();

        WorkLogEntry log2 = WorkLogEntry.builder()
            .workLogId("WL002")
            .detailedDescription("Completed installation")
            .submitDate(now.plusSeconds(3600))
            .build();

        WorkOrderRecord workOrder = WorkOrderRecord.builder()
            .entryId("000000000000001")
            .workOrderId("WO000000000001")
            .summary("Install new printer")
            .workLogs(List.of(log1, log2))
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(workOrder);

        // Then
        List<TextChunk> workLogChunks = chunks.stream()
            .filter(c -> c.getChunkType() == TextChunk.ChunkType.WORK_LOG)
            .toList();

        assertThat(workLogChunks).hasSizeGreaterThanOrEqualTo(1);
        assertThat(workLogChunks.get(0).getMetadata()).containsKey("work_log_date");
    }

    @Test
    void chunk_chunkIds_followCorrectFormat() {
        // Given
        WorkOrderRecord workOrder = WorkOrderRecord.builder()
            .entryId("000000000000001")
            .workOrderId("WO000000000001")
            .summary("Install new printer")
            .description("Installation details")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(workOrder);

        // Then
        for (TextChunk chunk : chunks) {
            assertThat(chunk.getChunkId()).matches("workorder:WO000000000001:(summary|description):\\d+");
        }
    }
}
