package com.bmc.rag.store.service;

import com.bmc.rag.store.service.VectorStoreService.SearchResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Additional unit tests for VectorStoreService.SearchResult nested class.
 * Covers Lombok-generated methods and edge cases.
 */
@DisplayName("SearchResult Nested Class Tests")
class SearchResultNestedClassTest {

    @Test
    @DisplayName("builder_shouldCreateInstanceWithAllFields")
    void builder_shouldCreateInstanceWithAllFields() {
        UUID id = UUID.randomUUID();
        Map<String, String> metadata = new HashMap<>();
        metadata.put("category", "Hardware");
        metadata.put("assigned_group", "IT Support");

        SearchResult result = SearchResult.builder()
            .id(id)
            .chunkId("chunk-123")
            .textSegment("This is a test segment")
            .sourceType("Incident")
            .sourceId("INC000001")
            .entryId("entry-456")
            .chunkType("RESOLUTION")
            .sequenceNumber(1)
            .metadata(metadata)
            .score(0.95f)
            .build();

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getChunkId()).isEqualTo("chunk-123");
        assertThat(result.getTextSegment()).isEqualTo("This is a test segment");
        assertThat(result.getSourceType()).isEqualTo("Incident");
        assertThat(result.getSourceId()).isEqualTo("INC000001");
        assertThat(result.getEntryId()).isEqualTo("entry-456");
        assertThat(result.getChunkType()).isEqualTo("RESOLUTION");
        assertThat(result.getSequenceNumber()).isEqualTo(1);
        assertThat(result.getMetadata()).hasSize(2);
        assertThat(result.getScore()).isEqualTo(0.95f);
    }

    @Test
    @DisplayName("getSourceReference_shouldFormatCorrectly")
    void getSourceReference_shouldFormatCorrectly() {
        SearchResult result = SearchResult.builder()
            .sourceType("KnowledgeArticle")
            .sourceId("KB000123")
            .build();

        assertThat(result.getSourceReference()).isEqualTo("KnowledgeArticle KB000123");
    }

    @Test
    @DisplayName("setters_shouldUpdateValues")
    void setters_shouldUpdateValues() {
        SearchResult result = SearchResult.builder().build();

        UUID newId = UUID.randomUUID();
        result.setId(newId);
        result.setChunkId("new-chunk");
        result.setTextSegment("new text");
        result.setSourceType("WorkOrder");
        result.setSourceId("WO001");
        result.setEntryId("entry-789");
        result.setChunkType("DESCRIPTION");
        result.setSequenceNumber(5);
        result.setScore(0.85f);

        assertThat(result.getId()).isEqualTo(newId);
        assertThat(result.getChunkId()).isEqualTo("new-chunk");
        assertThat(result.getSourceType()).isEqualTo("WorkOrder");
        assertThat(result.getScore()).isEqualTo(0.85f);
    }

    @Test
    @DisplayName("equalsAndHashCode_shouldWork")
    void equalsAndHashCode_shouldWork() {
        UUID id = UUID.randomUUID();

        SearchResult result1 = SearchResult.builder()
            .id(id)
            .chunkId("chunk-1")
            .score(0.9f)
            .build();

        SearchResult result2 = SearchResult.builder()
            .id(id)
            .chunkId("chunk-1")
            .score(0.9f)
            .build();

        assertThat(result1).isEqualTo(result2);
        assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
    }

    @Test
    @DisplayName("toString_shouldContainFields")
    void toString_shouldContainFields() {
        SearchResult result = SearchResult.builder()
            .chunkId("chunk-123")
            .sourceId("INC000001")
            .score(0.95f)
            .build();

        String toString = result.toString();

        assertThat(toString).contains("chunk-123");
        assertThat(toString).contains("INC000001");
        assertThat(toString).contains("0.95");
    }

    @Test
    @DisplayName("metadata_shouldBeNullable")
    void metadata_shouldBeNullable() {
        SearchResult result = SearchResult.builder()
            .metadata(null)
            .build();

        assertThat(result.getMetadata()).isNull();
    }
}
