package com.bmc.rag.vectorization.chunking;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for TextChunk model class.
 */
class TextChunkTest {

    @Test
    void buildITSMMetadata_allFieldsProvided_returnsCompleteMetadata() {
        // Given
        String sourceType = "Incident";
        String sourceId = "INC000000000001";
        String title = "Network connectivity issue";
        String assignedGroup = "Network Support";
        String category = "Infrastructure > Network";
        String status = "In Progress";

        // When
        Map<String, String> metadata = TextChunk.buildITSMMetadata(
            sourceType, sourceId, title, assignedGroup, category, status
        );

        // Then
        assertThat(metadata).containsEntry("source_type", "Incident");
        assertThat(metadata).containsEntry("source_id", "INC000000000001");
        assertThat(metadata).containsEntry("title", "Network connectivity issue");
        assertThat(metadata).containsEntry("assigned_group", "Network Support");
        assertThat(metadata).containsEntry("category", "Infrastructure > Network");
        assertThat(metadata).containsEntry("status", "In Progress");
        assertThat(metadata).hasSize(6);
    }

    @Test
    void buildITSMMetadata_onlyRequiredFields_returnsMinimalMetadata() {
        // Given
        String sourceType = "Incident";
        String sourceId = "INC000000000001";

        // When
        Map<String, String> metadata = TextChunk.buildITSMMetadata(
            sourceType, sourceId, null, null, null, null
        );

        // Then
        assertThat(metadata).containsEntry("source_type", "Incident");
        assertThat(metadata).containsEntry("source_id", "INC000000000001");
        assertThat(metadata).hasSize(2);
        assertThat(metadata).doesNotContainKey("title");
        assertThat(metadata).doesNotContainKey("assigned_group");
    }

    @Test
    void buildITSMMetadata_partialFields_includesOnlyProvidedFields() {
        // Given
        String sourceType = "KnowledgeArticle";
        String sourceId = "KA000000000001";
        String title = "How to reset password";

        // When
        Map<String, String> metadata = TextChunk.buildITSMMetadata(
            sourceType, sourceId, title, null, "Security", null
        );

        // Then
        assertThat(metadata).containsEntry("source_type", "KnowledgeArticle");
        assertThat(metadata).containsEntry("source_id", "KA000000000001");
        assertThat(metadata).containsEntry("title", "How to reset password");
        assertThat(metadata).containsEntry("category", "Security");
        assertThat(metadata).hasSize(4);
        assertThat(metadata).doesNotContainKey("assigned_group");
        assertThat(metadata).doesNotContainKey("status");
    }

    @Test
    void generateChunkId_validInputs_returnsCorrectFormat() {
        // Given
        String sourceType = "Incident";
        String sourceId = "INC000000000001";
        TextChunk.ChunkType chunkType = TextChunk.ChunkType.RESOLUTION;
        int sequence = 0;

        // When
        String chunkId = TextChunk.generateChunkId(sourceType, sourceId, chunkType, sequence);

        // Then
        assertThat(chunkId).isEqualTo("incident:INC000000000001:resolution:0");
    }

    @Test
    void generateChunkId_differentSequence_includesSequenceNumber() {
        // Given
        String sourceType = "WorkOrder";
        String sourceId = "WO000000000005";
        TextChunk.ChunkType chunkType = TextChunk.ChunkType.WORK_LOG;
        int sequence = 3;

        // When
        String chunkId = TextChunk.generateChunkId(sourceType, sourceId, chunkType, sequence);

        // Then
        assertThat(chunkId).isEqualTo("workorder:WO000000000005:work_log:3");
    }

    @Test
    void generateChunkId_summaryChunkType_formatsCorrectly() {
        // Given
        String sourceType = "ChangeRequest";
        String sourceId = "CRQ000000000001";
        TextChunk.ChunkType chunkType = TextChunk.ChunkType.SUMMARY;
        int sequence = 0;

        // When
        String chunkId = TextChunk.generateChunkId(sourceType, sourceId, chunkType, sequence);

        // Then
        assertThat(chunkId).isEqualTo("changerequest:CRQ000000000001:summary:0");
    }

    @Test
    void addMetadata_validKeyValue_addsToMetadata() {
        // Given
        TextChunk chunk = TextChunk.builder()
            .content("Test content")
            .metadata(new HashMap<>())
            .build();

        // When
        TextChunk result = chunk.addMetadata("custom_key", "custom_value");

        // Then
        assertThat(result).isSameAs(chunk); // Fluent API
        assertThat(chunk.getMetadata()).containsEntry("custom_key", "custom_value");
    }

    @Test
    void addMetadata_nullValue_doesNotAddToMetadata() {
        // Given
        TextChunk chunk = TextChunk.builder()
            .content("Test content")
            .metadata(new HashMap<>())
            .build();

        // When
        chunk.addMetadata("custom_key", null);

        // Then
        assertThat(chunk.getMetadata()).doesNotContainKey("custom_key");
        assertThat(chunk.getMetadata()).isEmpty();
    }

    @Test
    void addMetadata_emptyValue_doesNotAddToMetadata() {
        // Given
        TextChunk chunk = TextChunk.builder()
            .content("Test content")
            .metadata(new HashMap<>())
            .build();

        // When
        chunk.addMetadata("custom_key", "");

        // Then
        assertThat(chunk.getMetadata()).doesNotContainKey("custom_key");
        assertThat(chunk.getMetadata()).isEmpty();
    }

    @Test
    void addMetadata_multipleKeys_addsAllToMetadata() {
        // Given
        TextChunk chunk = TextChunk.builder()
            .content("Test content")
            .metadata(new HashMap<>())
            .build();

        // When
        chunk.addMetadata("key1", "value1")
             .addMetadata("key2", "value2")
             .addMetadata("key3", "value3");

        // Then
        assertThat(chunk.getMetadata()).hasSize(3);
        assertThat(chunk.getMetadata()).containsEntry("key1", "value1");
        assertThat(chunk.getMetadata()).containsEntry("key2", "value2");
        assertThat(chunk.getMetadata()).containsEntry("key3", "value3");
    }

    @Test
    void getContentLength_withContent_returnsCorrectLength() {
        // Given
        String content = "This is a test content with 42 chars.";
        TextChunk chunk = TextChunk.builder()
            .content(content)
            .build();

        // When
        int length = chunk.getContentLength();

        // Then
        assertThat(length).isEqualTo(content.length());
        assertThat(length).isGreaterThan(0);
    }

    @Test
    void getContentLength_nullContent_returnsZero() {
        // Given
        TextChunk chunk = TextChunk.builder()
            .content(null)
            .build();

        // When
        int length = chunk.getContentLength();

        // Then
        assertThat(length).isZero();
    }

    @Test
    void getContentLength_emptyContent_returnsZero() {
        // Given
        TextChunk chunk = TextChunk.builder()
            .content("")
            .build();

        // When
        int length = chunk.getContentLength();

        // Then
        assertThat(length).isZero();
    }

    @Test
    void builder_fullChunk_createsCorrectly() {
        // Given/When
        TextChunk chunk = TextChunk.builder()
            .chunkId("incident:INC000000000001:summary:0")
            .content("Network connectivity issue in building 5")
            .chunkType(TextChunk.ChunkType.SUMMARY)
            .sourceType("Incident")
            .sourceId("INC000000000001")
            .entryId("000000000000001")
            .metadata(new HashMap<>())
            .sequenceNumber(0)
            .startOffset(0)
            .build();

        // Then
        assertThat(chunk.getChunkId()).isEqualTo("incident:INC000000000001:summary:0");
        assertThat(chunk.getContent()).isEqualTo("Network connectivity issue in building 5");
        assertThat(chunk.getChunkType()).isEqualTo(TextChunk.ChunkType.SUMMARY);
        assertThat(chunk.getSourceType()).isEqualTo("Incident");
        assertThat(chunk.getSourceId()).isEqualTo("INC000000000001");
        assertThat(chunk.getEntryId()).isEqualTo("000000000000001");
        assertThat(chunk.getSequenceNumber()).isZero();
        assertThat(chunk.getStartOffset()).isZero();
        assertThat(chunk.getMetadata()).isEmpty();
    }

    @Test
    void builder_defaultMetadata_initializesAsEmptyMap() {
        // Given/When
        TextChunk chunk = TextChunk.builder()
            .content("Test")
            .build();

        // Then
        assertThat(chunk.getMetadata()).isNotNull();
        assertThat(chunk.getMetadata()).isEmpty();
    }
}
