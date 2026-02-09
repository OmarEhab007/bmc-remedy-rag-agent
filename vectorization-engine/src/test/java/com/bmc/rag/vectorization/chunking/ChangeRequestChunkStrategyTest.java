package com.bmc.rag.vectorization.chunking;

import com.bmc.rag.connector.model.ChangeRequestRecord;
import com.bmc.rag.connector.model.WorkLogEntry;
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
 * Tests for ChangeRequestChunkStrategy.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChangeRequestChunkStrategyTest {

    @Mock
    private SemanticChunker semanticChunker;

    private ChangeRequestChunkStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new ChangeRequestChunkStrategy(semanticChunker);

        // Default mock behavior
        when(semanticChunker.splitTextWithContext(anyString(), anyString()))
            .thenAnswer(invocation -> {
                String text = invocation.getArgument(0);
                String context = invocation.getArgument(1);
                return List.of(context + "\n\n" + text);
            });
    }

    @Test
    void getRecordType_returnsChangeRequest() {
        // When
        String recordType = strategy.getRecordType();

        // Then
        assertThat(recordType).isEqualTo("ChangeRequest");
    }

    @Test
    void chunk_minimalChangeRequest_createsSummaryChunk() {
        // Given
        ChangeRequestRecord change = ChangeRequestRecord.builder()
            .entryId("000000000000001")
            .changeId("CRQ000000000001")
            .summary("Database upgrade")
            .statusDisplayValue("Draft")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(change);

        // Then
        assertThat(chunks).hasSize(1);
        TextChunk summaryChunk = chunks.get(0);
        assertThat(summaryChunk.getChunkType()).isEqualTo(TextChunk.ChunkType.SUMMARY);
        assertThat(summaryChunk.getSourceType()).isEqualTo("ChangeRequest");
        assertThat(summaryChunk.getSourceId()).isEqualTo("CRQ000000000001");
    }

    @Test
    void chunk_withImplementationPlan_createsHighValueChunk() {
        // Given
        ChangeRequestRecord change = ChangeRequestRecord.builder()
            .entryId("000000000000001")
            .changeId("CRQ000000000001")
            .summary("Database upgrade")
            .implementationPlan("1. Backup database\n2. Stop application\n3. Run upgrade scripts\n4. Start application")
            .statusDisplayValue("Planning")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(change);

        // Then
        List<TextChunk> implChunks = chunks.stream()
            .filter(c -> c.getChunkType() == TextChunk.ChunkType.IMPLEMENTATION)
            .toList();

        assertThat(implChunks).hasSizeGreaterThanOrEqualTo(1);
        TextChunk implChunk = implChunks.get(0);
        assertThat(implChunk.getMetadata()).containsEntry("chunk_priority", "high");
        assertThat(implChunk.getContent()).contains("Implementation Plan:");
    }

    @Test
    void chunk_withRollbackPlan_createsHighValueChunk() {
        // Given
        ChangeRequestRecord change = ChangeRequestRecord.builder()
            .entryId("000000000000001")
            .changeId("CRQ000000000001")
            .summary("Database upgrade")
            .rollbackPlan("1. Restore database from backup\n2. Restart application with old version")
            .statusDisplayValue("Planning")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(change);

        // Then
        List<TextChunk> rollbackChunks = chunks.stream()
            .filter(c -> c.getChunkType() == TextChunk.ChunkType.ROLLBACK)
            .toList();

        assertThat(rollbackChunks).hasSizeGreaterThanOrEqualTo(1);
        TextChunk rollbackChunk = rollbackChunks.get(0);
        assertThat(rollbackChunk.getMetadata()).containsEntry("chunk_priority", "high");
        assertThat(rollbackChunk.getContent()).contains("Rollback/Backout Plan:");
    }

    @Test
    void chunk_populatesChangeSpecificMetadata() {
        // Given
        ChangeRequestRecord change = ChangeRequestRecord.builder()
            .entryId("000000000000001")
            .changeId("CRQ000000000001")
            .summary("Database upgrade")
            .changeType("Standard")
            .changeClass("Infrastructure")
            .riskLevel(2)
            .assignedGroup("Database Team")
            .categoryTier1("Systems")
            .statusDisplayValue("Scheduled")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(change);

        // Then
        assertThat(chunks).isNotEmpty();
        TextChunk firstChunk = chunks.get(0);
        assertThat(firstChunk.getMetadata()).containsEntry("source_type", "ChangeRequest");
        assertThat(firstChunk.getMetadata()).containsEntry("change_type", "Standard");
        assertThat(firstChunk.getMetadata()).containsEntry("change_class", "Infrastructure");
        assertThat(firstChunk.getMetadata()).containsEntry("risk_level", "2");
        assertThat(firstChunk.getMetadata()).containsEntry("assigned_group", "Database Team");
    }

    @Test
    void chunk_withChangeReason_includesInSummary() {
        // Given
        ChangeRequestRecord change = ChangeRequestRecord.builder()
            .entryId("000000000000001")
            .changeId("CRQ000000000001")
            .summary("Database upgrade")
            .changeReason("Required for compliance with new security standards")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(change);

        // Then
        TextChunk summaryChunk = chunks.stream()
            .filter(c -> c.getChunkType() == TextChunk.ChunkType.SUMMARY)
            .findFirst()
            .orElseThrow();

        assertThat(summaryChunk.getContent()).contains("Reason:");
        assertThat(summaryChunk.getContent()).contains("compliance with new security standards");
    }

    @Test
    void chunk_withDescription_createsDescriptionChunks() {
        // Given
        ChangeRequestRecord change = ChangeRequestRecord.builder()
            .entryId("000000000000001")
            .changeId("CRQ000000000001")
            .summary("Database upgrade")
            .description("Upgrading production database from version 12 to version 14. This includes schema changes and data migration.")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(change);

        // Then
        assertThat(chunks.stream().anyMatch(c -> c.getChunkType() == TextChunk.ChunkType.DESCRIPTION)).isTrue();
    }

    @Test
    void chunk_withWorkLogs_createsWorkLogChunks() {
        // Given
        WorkLogEntry log = WorkLogEntry.builder()
            .workLogId("WL001")
            .detailedDescription("Change approved by CAB")
            .submitDate(Instant.now())
            .build();

        ChangeRequestRecord change = ChangeRequestRecord.builder()
            .entryId("000000000000001")
            .changeId("CRQ000000000001")
            .summary("Database upgrade")
            .workLogs(List.of(log))
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(change);

        // Then
        assertThat(chunks.stream().anyMatch(c -> c.getChunkType() == TextChunk.ChunkType.WORK_LOG)).isTrue();
    }

    @Test
    void chunk_nullImplementationPlan_doesNotCreateImplementationChunk() {
        // Given
        ChangeRequestRecord change = ChangeRequestRecord.builder()
            .entryId("000000000000001")
            .changeId("CRQ000000000001")
            .summary("Database upgrade")
            .implementationPlan(null)
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(change);

        // Then
        assertThat(chunks.stream().filter(c -> c.getChunkType() == TextChunk.ChunkType.IMPLEMENTATION))
            .isEmpty();
    }

    @Test
    void chunk_emptyRollbackPlan_doesNotCreateRollbackChunk() {
        // Given
        ChangeRequestRecord change = ChangeRequestRecord.builder()
            .entryId("000000000000001")
            .changeId("CRQ000000000001")
            .summary("Database upgrade")
            .rollbackPlan("")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(change);

        // Then
        assertThat(chunks.stream().filter(c -> c.getChunkType() == TextChunk.ChunkType.ROLLBACK))
            .isEmpty();
    }

    @Test
    void chunk_contextPrefix_includesChangeTypeAndCategory() {
        // Given
        ChangeRequestRecord change = ChangeRequestRecord.builder()
            .entryId("000000000000001")
            .changeId("CRQ000000000001")
            .summary("Database upgrade")
            .changeType("Standard")
            .categoryTier1("Infrastructure")
            .categoryTier2("Database")
            .description("Description text")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(change);

        // Then
        TextChunk summaryChunk = chunks.stream()
            .filter(c -> c.getChunkType() == TextChunk.ChunkType.SUMMARY)
            .findFirst()
            .orElseThrow();

        assertThat(summaryChunk.getContent()).contains("Change Request CRQ000000000001");
        assertThat(summaryChunk.getContent()).contains("Database upgrade");
    }

    @Test
    void chunk_sequenceNumbersIncrement_correctly() {
        // Given
        ChangeRequestRecord change = ChangeRequestRecord.builder()
            .entryId("000000000000001")
            .changeId("CRQ000000000001")
            .summary("Database upgrade")
            .description("Description")
            .implementationPlan("Implementation steps")
            .rollbackPlan("Rollback steps")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(change);

        // Then
        assertThat(chunks).hasSizeGreaterThanOrEqualTo(4);
        for (int i = 0; i < chunks.size(); i++) {
            assertThat(chunks.get(i).getSequenceNumber()).isEqualTo(i);
        }
    }

    @Test
    void chunk_fullChangeRequest_createsAllChunkTypes() {
        // Given
        WorkLogEntry workLog = WorkLogEntry.builder()
            .workLogId("WL001")
            .detailedDescription("CAB approval received")
            .submitDate(Instant.now())
            .build();

        ChangeRequestRecord change = ChangeRequestRecord.builder()
            .entryId("000000000000001")
            .changeId("CRQ000000000001")
            .summary("Database upgrade to version 14")
            .description("Comprehensive database upgrade including schema changes.")
            .changeReason("Security and compliance requirements")
            .implementationPlan("1. Backup\n2. Upgrade\n3. Test\n4. Cutover")
            .rollbackPlan("Restore from backup and restart")
            .changeType("Standard")
            .changeClass("Infrastructure")
            .riskLevel(3)
            .statusDisplayValue("Scheduled")
            .assignedGroup("Database Team")
            .categoryTier1("Infrastructure")
            .categoryTier2("Database")
            .workLogs(List.of(workLog))
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(change);

        // Then
        assertThat(chunks).hasSizeGreaterThanOrEqualTo(5);
        assertThat(chunks.stream().anyMatch(c -> c.getChunkType() == TextChunk.ChunkType.SUMMARY)).isTrue();
        assertThat(chunks.stream().anyMatch(c -> c.getChunkType() == TextChunk.ChunkType.DESCRIPTION)).isTrue();
        assertThat(chunks.stream().anyMatch(c -> c.getChunkType() == TextChunk.ChunkType.IMPLEMENTATION)).isTrue();
        assertThat(chunks.stream().anyMatch(c -> c.getChunkType() == TextChunk.ChunkType.ROLLBACK)).isTrue();
        assertThat(chunks.stream().anyMatch(c -> c.getChunkType() == TextChunk.ChunkType.WORK_LOG)).isTrue();

        // Verify metadata
        TextChunk firstChunk = chunks.get(0);
        assertThat(firstChunk.getMetadata()).containsEntry("change_type", "Standard");
        assertThat(firstChunk.getMetadata()).containsEntry("risk_level", "3");
    }

    @Test
    void chunk_nullMetadataFields_doesNotIncludeInMetadata() {
        // Given
        ChangeRequestRecord change = ChangeRequestRecord.builder()
            .entryId("000000000000001")
            .changeId("CRQ000000000001")
            .summary("Database upgrade")
            .changeType(null)
            .changeClass(null)
            .riskLevel(null)
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(change);

        // Then
        assertThat(chunks).isNotEmpty();
        TextChunk firstChunk = chunks.get(0);
        assertThat(firstChunk.getMetadata()).doesNotContainKey("change_type");
        assertThat(firstChunk.getMetadata()).doesNotContainKey("change_class");
        assertThat(firstChunk.getMetadata()).doesNotContainKey("risk_level");
    }
}
