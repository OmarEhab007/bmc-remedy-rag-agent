package com.bmc.rag.store.service;

import com.bmc.rag.store.service.HybridSearchService.HybridSearchResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Additional unit tests for HybridSearchService.HybridSearchResult nested class.
 * Covers Lombok-generated methods and edge cases.
 */
@DisplayName("HybridSearchResult Nested Class Tests")
class HybridSearchResultNestedClassTest {

    @Test
    @DisplayName("builder_shouldCreateInstanceWithAllFields")
    void builder_shouldCreateInstanceWithAllFields() {
        UUID id = UUID.randomUUID();
        Map<String, String> metadata = new HashMap<>();
        metadata.put("priority", "High");
        metadata.put("urgency", "Critical");

        HybridSearchResult result = HybridSearchResult.builder()
            .id(id)
            .chunkId("hybrid-chunk-123")
            .textSegment("Hybrid search test segment")
            .sourceType("Incident")
            .sourceId("INC000002")
            .entryId("hybrid-entry-456")
            .chunkType("NOTES")
            .sequenceNumber(2)
            .metadata(metadata)
            .vectorScore(0.92f)
            .textScore(0.88f)
            .hybridScore(0.90f)
            .build();

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getChunkId()).isEqualTo("hybrid-chunk-123");
        assertThat(result.getTextSegment()).isEqualTo("Hybrid search test segment");
        assertThat(result.getSourceType()).isEqualTo("Incident");
        assertThat(result.getSourceId()).isEqualTo("INC000002");
        assertThat(result.getEntryId()).isEqualTo("hybrid-entry-456");
        assertThat(result.getChunkType()).isEqualTo("NOTES");
        assertThat(result.getSequenceNumber()).isEqualTo(2);
        assertThat(result.getMetadata()).hasSize(2);
        assertThat(result.getVectorScore()).isEqualTo(0.92f);
        assertThat(result.getTextScore()).isEqualTo(0.88f);
        assertThat(result.getHybridScore()).isEqualTo(0.90f);
    }

    @Test
    @DisplayName("getSourceReference_shouldFormatCorrectly")
    void getSourceReference_shouldFormatCorrectly() {
        HybridSearchResult result = HybridSearchResult.builder()
            .sourceType("WorkOrder")
            .sourceId("WO000789")
            .build();

        assertThat(result.getSourceReference()).isEqualTo("WorkOrder WO000789");
    }

    @Test
    @DisplayName("setters_shouldUpdateValues")
    void setters_shouldUpdateValues() {
        HybridSearchResult result = HybridSearchResult.builder().build();

        UUID newId = UUID.randomUUID();
        result.setId(newId);
        result.setChunkId("new-hybrid-chunk");
        result.setTextSegment("new hybrid text");
        result.setSourceType("KnowledgeArticle");
        result.setSourceId("KB005");
        result.setEntryId("hybrid-entry-999");
        result.setChunkType("SOLUTION");
        result.setSequenceNumber(10);
        result.setVectorScore(0.75f);
        result.setTextScore(0.80f);
        result.setHybridScore(0.78f);

        assertThat(result.getId()).isEqualTo(newId);
        assertThat(result.getChunkId()).isEqualTo("new-hybrid-chunk");
        assertThat(result.getSourceType()).isEqualTo("KnowledgeArticle");
        assertThat(result.getVectorScore()).isEqualTo(0.75f);
        assertThat(result.getTextScore()).isEqualTo(0.80f);
        assertThat(result.getHybridScore()).isEqualTo(0.78f);
    }

    @Test
    @DisplayName("equalsAndHashCode_shouldWork")
    void equalsAndHashCode_shouldWork() {
        UUID id = UUID.randomUUID();

        HybridSearchResult result1 = HybridSearchResult.builder()
            .id(id)
            .chunkId("hybrid-1")
            .vectorScore(0.9f)
            .textScore(0.85f)
            .hybridScore(0.88f)
            .build();

        HybridSearchResult result2 = HybridSearchResult.builder()
            .id(id)
            .chunkId("hybrid-1")
            .vectorScore(0.9f)
            .textScore(0.85f)
            .hybridScore(0.88f)
            .build();

        assertThat(result1).isEqualTo(result2);
        assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
    }

    @Test
    @DisplayName("toString_shouldContainFields")
    void toString_shouldContainFields() {
        HybridSearchResult result = HybridSearchResult.builder()
            .chunkId("hybrid-456")
            .sourceId("WO000123")
            .vectorScore(0.92f)
            .textScore(0.88f)
            .hybridScore(0.90f)
            .build();

        String toString = result.toString();

        assertThat(toString).contains("hybrid-456");
        assertThat(toString).contains("WO000123");
        assertThat(toString).contains("0.92");
        assertThat(toString).contains("0.88");
        assertThat(toString).contains("0.9"); // Float 0.90 prints as 0.9
    }

    @Test
    @DisplayName("metadata_shouldBeNullable")
    void metadata_shouldBeNullable() {
        HybridSearchResult result = HybridSearchResult.builder()
            .metadata(null)
            .build();

        assertThat(result.getMetadata()).isNull();
    }

    @Test
    @DisplayName("scoreComparison_shouldWorkCorrectly")
    void scoreComparison_shouldWorkCorrectly() {
        HybridSearchResult result1 = HybridSearchResult.builder()
            .hybridScore(0.95f)
            .build();

        HybridSearchResult result2 = HybridSearchResult.builder()
            .hybridScore(0.85f)
            .build();

        assertThat(result1.getHybridScore()).isGreaterThan(result2.getHybridScore());
    }
}
