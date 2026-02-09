package com.bmc.rag.store.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EmbeddingEntity")
class EmbeddingEntityTest {

    @Nested
    @DisplayName("Builder Defaults")
    class BuilderDefaults {

        @Test
        @DisplayName("should set sequenceNumber to 0 by default")
        void shouldSetSequenceNumberToZero() {
            EmbeddingEntity entity = EmbeddingEntity.builder()
                .chunkId("chunk1")
                .textSegment("Sample text")
                .sourceType("incident")
                .sourceId("INC000123")
                .build();

            assertThat(entity.getSequenceNumber()).isEqualTo(0);
        }

        @Test
        @DisplayName("should initialize metadata as empty HashMap by default")
        void shouldInitializeMetadataAsEmptyHashMap() {
            EmbeddingEntity entity = EmbeddingEntity.builder()
                .chunkId("chunk1")
                .textSegment("Sample text")
                .sourceType("incident")
                .sourceId("INC000123")
                .build();

            assertThat(entity.getMetadata())
                .isNotNull()
                .isInstanceOf(HashMap.class)
                .isEmpty();
        }

        @Test
        @DisplayName("should allow metadata to be mutable after initialization")
        void shouldAllowMutableMetadata() {
            EmbeddingEntity entity = EmbeddingEntity.builder()
                .chunkId("chunk1")
                .textSegment("Sample text")
                .sourceType("incident")
                .sourceId("INC000123")
                .build();

            entity.getMetadata().put("key1", "value1");
            entity.getMetadata().put("key2", "value2");

            assertThat(entity.getMetadata())
                .hasSize(2)
                .containsEntry("key1", "value1")
                .containsEntry("key2", "value2");
        }

        @Test
        @DisplayName("should allow overriding default sequenceNumber")
        void shouldAllowOverridingSequenceNumber() {
            EmbeddingEntity entity = EmbeddingEntity.builder()
                .chunkId("chunk1")
                .textSegment("Sample text")
                .sourceType("incident")
                .sourceId("INC000123")
                .sequenceNumber(5)
                .build();

            assertThat(entity.getSequenceNumber()).isEqualTo(5);
        }

        @Test
        @DisplayName("should allow overriding default metadata")
        void shouldAllowOverridingMetadata() {
            Map<String, String> customMetadata = Map.of("custom", "data");
            EmbeddingEntity entity = EmbeddingEntity.builder()
                .chunkId("chunk1")
                .textSegment("Sample text")
                .sourceType("incident")
                .sourceId("INC000123")
                .metadata(customMetadata)
                .build();

            assertThat(entity.getMetadata()).isEqualTo(customMetadata);
        }
    }

    @Nested
    @DisplayName("onCreate() Lifecycle Hook")
    class OnCreateLifecycleHook {

        @Test
        @DisplayName("should set both createdAt and updatedAt to current time")
        void shouldSetBothTimestampsToNow() {
            EmbeddingEntity entity = EmbeddingEntity.builder()
                .chunkId("chunk1")
                .textSegment("Sample text")
                .sourceType("incident")
                .sourceId("INC000123")
                .build();

            Instant before = Instant.now();
            entity.onCreate();
            Instant after = Instant.now();

            assertThat(entity.getCreatedAt())
                .isNotNull()
                .isBetween(before, after);
            assertThat(entity.getUpdatedAt())
                .isNotNull()
                .isBetween(before, after);
        }

        @Test
        @DisplayName("should set createdAt and updatedAt to approximately same value")
        void shouldSetCreatedAtAndUpdatedAtToApproximatelySameValue() {
            EmbeddingEntity entity = EmbeddingEntity.builder()
                .chunkId("chunk1")
                .textSegment("Sample text")
                .sourceType("incident")
                .sourceId("INC000123")
                .build();

            entity.onCreate();

            // Both are set to Instant.now() in sequence, so they should be within 10ms of each other
            assertThat(entity.getCreatedAt()).isNotNull();
            assertThat(entity.getUpdatedAt()).isNotNull();

            long millisDiff = Math.abs(
                entity.getUpdatedAt().toEpochMilli() - entity.getCreatedAt().toEpochMilli()
            );
            assertThat(millisDiff).isLessThan(10);
        }

        @Test
        @DisplayName("should overwrite existing createdAt if called again")
        void shouldOverwriteExistingCreatedAt() {
            EmbeddingEntity entity = EmbeddingEntity.builder()
                .chunkId("chunk1")
                .textSegment("Sample text")
                .sourceType("incident")
                .sourceId("INC000123")
                .build();

            Instant firstCreation = Instant.parse("2025-01-01T10:00:00Z");
            entity.setCreatedAt(firstCreation);
            entity.setUpdatedAt(firstCreation);

            entity.onCreate();

            assertThat(entity.getCreatedAt()).isNotEqualTo(firstCreation);
            assertThat(entity.getUpdatedAt()).isNotEqualTo(firstCreation);
        }
    }

    @Nested
    @DisplayName("onUpdate() Lifecycle Hook")
    class OnUpdateLifecycleHook {

        @Test
        @DisplayName("should update only updatedAt timestamp")
        void shouldUpdateOnlyUpdatedAt() {
            EmbeddingEntity entity = EmbeddingEntity.builder()
                .chunkId("chunk1")
                .textSegment("Sample text")
                .sourceType("incident")
                .sourceId("INC000123")
                .build();

            Instant originalCreated = Instant.parse("2025-01-01T10:00:00Z");
            entity.setCreatedAt(originalCreated);
            entity.setUpdatedAt(originalCreated);

            Instant before = Instant.now();
            entity.onUpdate();
            Instant after = Instant.now();

            assertThat(entity.getCreatedAt()).isEqualTo(originalCreated);
            assertThat(entity.getUpdatedAt())
                .isNotNull()
                .isBetween(before, after);
        }

        @Test
        @DisplayName("should set updatedAt to current time on each call")
        void shouldSetUpdatedAtToCurrentTimeOnEachCall() throws InterruptedException {
            EmbeddingEntity entity = EmbeddingEntity.builder()
                .chunkId("chunk1")
                .textSegment("Sample text")
                .sourceType("incident")
                .sourceId("INC000123")
                .build();

            entity.onCreate();
            Instant firstUpdate = entity.getUpdatedAt();

            Thread.sleep(10); // Ensure time difference

            entity.onUpdate();
            Instant secondUpdate = entity.getUpdatedAt();

            assertThat(secondUpdate).isAfter(firstUpdate);
        }

        @Test
        @DisplayName("should work correctly when createdAt is null")
        void shouldWorkWhenCreatedAtIsNull() {
            EmbeddingEntity entity = EmbeddingEntity.builder()
                .chunkId("chunk1")
                .textSegment("Sample text")
                .sourceType("incident")
                .sourceId("INC000123")
                .build();

            Instant before = Instant.now();
            entity.onUpdate();
            Instant after = Instant.now();

            assertThat(entity.getCreatedAt()).isNull();
            assertThat(entity.getUpdatedAt())
                .isNotNull()
                .isBetween(before, after);
        }
    }

    @Nested
    @DisplayName("Builder with All Fields")
    class BuilderWithAllFields {

        @Test
        @DisplayName("should build entity with all required fields")
        void shouldBuildEntityWithAllRequiredFields() {
            UUID id = UUID.randomUUID();
            float[] embedding = new float[]{0.1f, 0.2f, 0.3f};
            Instant createdAt = Instant.parse("2025-01-01T10:00:00Z");
            Instant updatedAt = Instant.parse("2025-01-02T10:00:00Z");

            EmbeddingEntity entity = EmbeddingEntity.builder()
                .id(id)
                .chunkId("chunk123")
                .embedding(embedding)
                .textSegment("This is a sample text segment")
                .sourceType("incident")
                .sourceId("INC000123")
                .entryId("entry456")
                .chunkType("resolution")
                .sequenceNumber(3)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

            assertThat(entity.getId()).isEqualTo(id);
            assertThat(entity.getChunkId()).isEqualTo("chunk123");
            assertThat(entity.getEmbedding()).isEqualTo(embedding);
            assertThat(entity.getTextSegment()).isEqualTo("This is a sample text segment");
            assertThat(entity.getSourceType()).isEqualTo("incident");
            assertThat(entity.getSourceId()).isEqualTo("INC000123");
            assertThat(entity.getEntryId()).isEqualTo("entry456");
            assertThat(entity.getChunkType()).isEqualTo("resolution");
            assertThat(entity.getSequenceNumber()).isEqualTo(3);
            assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
            assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
        }

        @Test
        @DisplayName("should build entity with metadata map")
        void shouldBuildEntityWithMetadataMap() {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("assignedGroup", "Network Team");
            metadata.put("priority", "High");
            metadata.put("category", "Hardware");

            EmbeddingEntity entity = EmbeddingEntity.builder()
                .chunkId("chunk1")
                .textSegment("Sample text")
                .sourceType("incident")
                .sourceId("INC000123")
                .metadata(metadata)
                .build();

            assertThat(entity.getMetadata())
                .hasSize(3)
                .containsEntry("assignedGroup", "Network Team")
                .containsEntry("priority", "High")
                .containsEntry("category", "Hardware");
        }

        @Test
        @DisplayName("should handle null optional fields")
        void shouldHandleNullOptionalFields() {
            EmbeddingEntity entity = EmbeddingEntity.builder()
                .chunkId("chunk1")
                .textSegment("Sample text")
                .sourceType("incident")
                .sourceId("INC000123")
                .id(null)
                .embedding(null)
                .entryId(null)
                .chunkType(null)
                .createdAt(null)
                .updatedAt(null)
                .build();

            assertThat(entity.getId()).isNull();
            assertThat(entity.getEmbedding()).isNull();
            assertThat(entity.getEntryId()).isNull();
            assertThat(entity.getChunkType()).isNull();
            assertThat(entity.getCreatedAt()).isNull();
            assertThat(entity.getUpdatedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("Embedding Field")
    class EmbeddingField {

        @Test
        @DisplayName("should store and retrieve embedding array")
        void shouldStoreAndRetrieveEmbeddingArray() {
            float[] embedding = new float[]{0.1f, 0.2f, 0.3f, 0.4f, 0.5f};
            EmbeddingEntity entity = EmbeddingEntity.builder()
                .chunkId("chunk1")
                .textSegment("Sample text")
                .sourceType("incident")
                .sourceId("INC000123")
                .embedding(embedding)
                .build();

            assertThat(entity.getEmbedding()).isEqualTo(embedding);
        }

        @Test
        @DisplayName("should handle 384-dimensional embedding vector")
        void shouldHandle384DimensionalVector() {
            float[] embedding = new float[384];
            for (int i = 0; i < 384; i++) {
                embedding[i] = (float) (Math.random() - 0.5);
            }

            EmbeddingEntity entity = EmbeddingEntity.builder()
                .chunkId("chunk1")
                .textSegment("Sample text")
                .sourceType("incident")
                .sourceId("INC000123")
                .embedding(embedding)
                .build();

            assertThat(entity.getEmbedding())
                .hasSize(384)
                .isEqualTo(embedding);
        }

        @Test
        @DisplayName("should be transient field but Lombok @Data includes it in equals/hashCode")
        void shouldBeTransientFieldButIncludedInEquals() {
            float[] embedding1 = new float[]{0.1f, 0.2f, 0.3f};
            float[] embedding2 = new float[]{0.4f, 0.5f, 0.6f};

            EmbeddingEntity entity1 = EmbeddingEntity.builder()
                .id(UUID.randomUUID())
                .chunkId("chunk1")
                .textSegment("Sample text")
                .sourceType("incident")
                .sourceId("INC000123")
                .embedding(embedding1)
                .build();

            EmbeddingEntity entity2 = EmbeddingEntity.builder()
                .id(entity1.getId())
                .chunkId("chunk1")
                .textSegment("Sample text")
                .sourceType("incident")
                .sourceId("INC000123")
                .embedding(embedding2)
                .build();

            // Note: @Transient is for JPA persistence, but Lombok @Data still includes the field in equals/hashCode
            // This is expected Lombok behavior - @Transient only affects JPA, not Lombok
            assertThat(entity1).isNotEqualTo(entity2);
        }
    }

    @Nested
    @DisplayName("Metadata Operations")
    class MetadataOperations {

        @Test
        @DisplayName("should support put and get operations")
        void shouldSupportPutAndGetOperations() {
            EmbeddingEntity entity = EmbeddingEntity.builder()
                .chunkId("chunk1")
                .textSegment("Sample text")
                .sourceType("incident")
                .sourceId("INC000123")
                .build();

            entity.getMetadata().put("key1", "value1");
            entity.getMetadata().put("key2", "value2");

            assertThat(entity.getMetadata().get("key1")).isEqualTo("value1");
            assertThat(entity.getMetadata().get("key2")).isEqualTo("value2");
        }

        @Test
        @DisplayName("should support removing metadata entries")
        void shouldSupportRemovingMetadataEntries() {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("key1", "value1");
            metadata.put("key2", "value2");

            EmbeddingEntity entity = EmbeddingEntity.builder()
                .chunkId("chunk1")
                .textSegment("Sample text")
                .sourceType("incident")
                .sourceId("INC000123")
                .metadata(metadata)
                .build();

            entity.getMetadata().remove("key1");

            assertThat(entity.getMetadata())
                .hasSize(1)
                .containsEntry("key2", "value2")
                .doesNotContainKey("key1");
        }

        @Test
        @DisplayName("should support clearing all metadata")
        void shouldSupportClearingAllMetadata() {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("key1", "value1");
            metadata.put("key2", "value2");

            EmbeddingEntity entity = EmbeddingEntity.builder()
                .chunkId("chunk1")
                .textSegment("Sample text")
                .sourceType("incident")
                .sourceId("INC000123")
                .metadata(metadata)
                .build();

            entity.getMetadata().clear();

            assertThat(entity.getMetadata()).isEmpty();
        }

        @Test
        @DisplayName("should preserve metadata after onCreate")
        void shouldPreserveMetadataAfterOnCreate() {
            EmbeddingEntity entity = EmbeddingEntity.builder()
                .chunkId("chunk1")
                .textSegment("Sample text")
                .sourceType("incident")
                .sourceId("INC000123")
                .build();

            entity.getMetadata().put("key1", "value1");
            entity.onCreate();

            assertThat(entity.getMetadata())
                .hasSize(1)
                .containsEntry("key1", "value1");
        }

        @Test
        @DisplayName("should preserve metadata after onUpdate")
        void shouldPreserveMetadataAfterOnUpdate() {
            EmbeddingEntity entity = EmbeddingEntity.builder()
                .chunkId("chunk1")
                .textSegment("Sample text")
                .sourceType("incident")
                .sourceId("INC000123")
                .build();

            entity.getMetadata().put("key1", "value1");
            entity.onCreate();
            entity.getMetadata().put("key2", "value2");
            entity.onUpdate();

            assertThat(entity.getMetadata())
                .hasSize(2)
                .containsEntry("key1", "value1")
                .containsEntry("key2", "value2");
        }
    }

    @Nested
    @DisplayName("NoArgsConstructor and AllArgsConstructor")
    class Constructors {

        @Test
        @DisplayName("should create instance with no-args constructor and initialize field defaults")
        void shouldCreateInstanceWithNoArgsConstructor() {
            EmbeddingEntity entity = new EmbeddingEntity();

            assertThat(entity).isNotNull();
            // Field initializers are applied even in no-args constructor
            assertThat(entity.getSequenceNumber()).isEqualTo(0);
            assertThat(entity.getMetadata()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("should allow setting fields after no-args construction")
        void shouldAllowSettingFieldsAfterNoArgsConstruction() {
            EmbeddingEntity entity = new EmbeddingEntity();

            entity.setChunkId("chunk1");
            entity.setTextSegment("Sample text");
            entity.setSourceType("incident");
            entity.setSourceId("INC000123");
            entity.setSequenceNumber(5);
            entity.setMetadata(new HashMap<>());

            assertThat(entity.getChunkId()).isEqualTo("chunk1");
            assertThat(entity.getTextSegment()).isEqualTo("Sample text");
            assertThat(entity.getSourceType()).isEqualTo("incident");
            assertThat(entity.getSourceId()).isEqualTo("INC000123");
            assertThat(entity.getSequenceNumber()).isEqualTo(5);
            assertThat(entity.getMetadata()).isEmpty();
        }

        @Test
        @DisplayName("should create instance with all-args constructor")
        void shouldCreateInstanceWithAllArgsConstructor() {
            UUID id = UUID.randomUUID();
            float[] embedding = new float[]{0.1f, 0.2f, 0.3f};
            Map<String, String> metadata = Map.of("key", "value");
            Instant createdAt = Instant.now();
            Instant updatedAt = Instant.now();

            EmbeddingEntity entity = new EmbeddingEntity(
                id,
                "chunk1",
                embedding,
                "Sample text",
                "incident",
                "INC000123",
                "entry1",
                "resolution",
                3,
                metadata,
                createdAt,
                updatedAt
            );

            assertThat(entity.getId()).isEqualTo(id);
            assertThat(entity.getChunkId()).isEqualTo("chunk1");
            assertThat(entity.getEmbedding()).isEqualTo(embedding);
            assertThat(entity.getTextSegment()).isEqualTo("Sample text");
            assertThat(entity.getSourceType()).isEqualTo("incident");
            assertThat(entity.getSourceId()).isEqualTo("INC000123");
            assertThat(entity.getEntryId()).isEqualTo("entry1");
            assertThat(entity.getChunkType()).isEqualTo("resolution");
            assertThat(entity.getSequenceNumber()).isEqualTo(3);
            assertThat(entity.getMetadata()).isEqualTo(metadata);
            assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
            assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
        }
    }

    @Nested
    @DisplayName("Source Type and ID")
    class SourceTypeAndId {

        @Test
        @DisplayName("should store incident as source type")
        void shouldStoreIncidentAsSourceType() {
            EmbeddingEntity entity = EmbeddingEntity.builder()
                .chunkId("chunk1")
                .textSegment("Sample text")
                .sourceType("incident")
                .sourceId("INC000123")
                .build();

            assertThat(entity.getSourceType()).isEqualTo("incident");
            assertThat(entity.getSourceId()).isEqualTo("INC000123");
        }

        @Test
        @DisplayName("should store work order as source type")
        void shouldStoreWorkOrderAsSourceType() {
            EmbeddingEntity entity = EmbeddingEntity.builder()
                .chunkId("chunk1")
                .textSegment("Sample text")
                .sourceType("work_order")
                .sourceId("WO000456")
                .build();

            assertThat(entity.getSourceType()).isEqualTo("work_order");
            assertThat(entity.getSourceId()).isEqualTo("WO000456");
        }

        @Test
        @DisplayName("should store knowledge article as source type")
        void shouldStoreKnowledgeArticleAsSourceType() {
            EmbeddingEntity entity = EmbeddingEntity.builder()
                .chunkId("chunk1")
                .textSegment("Sample text")
                .sourceType("knowledge_article")
                .sourceId("KB000789")
                .build();

            assertThat(entity.getSourceType()).isEqualTo("knowledge_article");
            assertThat(entity.getSourceId()).isEqualTo("KB000789");
        }
    }

    @Nested
    @DisplayName("Chunk Type")
    class ChunkType {

        @Test
        @DisplayName("should store resolution chunk type")
        void shouldStoreResolutionChunkType() {
            EmbeddingEntity entity = EmbeddingEntity.builder()
                .chunkId("chunk1")
                .textSegment("Resolution text")
                .sourceType("incident")
                .sourceId("INC000123")
                .chunkType("resolution")
                .build();

            assertThat(entity.getChunkType()).isEqualTo("resolution");
        }

        @Test
        @DisplayName("should store description chunk type")
        void shouldStoreDescriptionChunkType() {
            EmbeddingEntity entity = EmbeddingEntity.builder()
                .chunkId("chunk1")
                .textSegment("Description text")
                .sourceType("incident")
                .sourceId("INC000123")
                .chunkType("description")
                .build();

            assertThat(entity.getChunkType()).isEqualTo("description");
        }

        @Test
        @DisplayName("should store work log chunk type")
        void shouldStoreWorkLogChunkType() {
            EmbeddingEntity entity = EmbeddingEntity.builder()
                .chunkId("chunk1")
                .textSegment("Work log entry")
                .sourceType("incident")
                .sourceId("INC000123")
                .chunkType("work_log")
                .build();

            assertThat(entity.getChunkType()).isEqualTo("work_log");
        }

        @Test
        @DisplayName("should allow null chunk type")
        void shouldAllowNullChunkType() {
            EmbeddingEntity entity = EmbeddingEntity.builder()
                .chunkId("chunk1")
                .textSegment("Sample text")
                .sourceType("incident")
                .sourceId("INC000123")
                .chunkType(null)
                .build();

            assertThat(entity.getChunkType()).isNull();
        }
    }
}
