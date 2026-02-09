package com.bmc.rag.vectorization.embedding;

import com.bmc.rag.vectorization.chunking.TextChunk;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests for LocalEmbeddingService using real ONNX model.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LocalEmbeddingServiceTest {

    private LocalEmbeddingService embeddingService;

    @BeforeAll
    void setUp() {
        embeddingService = new LocalEmbeddingService();
        embeddingService.init();
    }

    @Test
    void init_initializesModel_successfully() {
        // When/Then
        assertThat(embeddingService).isNotNull();
    }

    @Test
    void getDimension_returnsCorrectDimension() {
        // When
        int dimension = embeddingService.getDimension();

        // Then
        assertThat(dimension).isEqualTo(384);
    }

    @Test
    void embed_validText_returnsEmbeddingWith384Dimensions() {
        // Given
        String text = "This is a test sentence for embedding.";

        // When
        float[] embedding = embeddingService.embed(text);

        // Then
        assertThat(embedding).hasSize(384);
        assertThat(embedding).isNotNull();
    }

    @Test
    void embed_differentTexts_produceDifferentEmbeddings() {
        // Given
        String text1 = "Network connectivity issue";
        String text2 = "Database performance problem";

        // When
        float[] embedding1 = embeddingService.embed(text1);
        float[] embedding2 = embeddingService.embed(text2);

        // Then
        assertThat(embedding1).hasSize(384);
        assertThat(embedding2).hasSize(384);
        assertThat(embedding1).isNotEqualTo(embedding2);

        // Calculate similarity - should be low since texts are different
        double similarity = LocalEmbeddingService.cosineSimilarity(embedding1, embedding2);
        assertThat(similarity).isLessThan(0.9); // Not too similar
    }

    @Test
    void embed_similarTexts_producesSimilarEmbeddings() {
        // Given
        String text1 = "The network is not working";
        String text2 = "Network connectivity failure";

        // When
        float[] embedding1 = embeddingService.embed(text1);
        float[] embedding2 = embeddingService.embed(text2);

        // Then
        double similarity = LocalEmbeddingService.cosineSimilarity(embedding1, embedding2);
        assertThat(similarity).isGreaterThan(0.5); // Should be somewhat similar
    }

    @Test
    void embed_nullText_returnsZeroVector() {
        // When
        float[] embedding = embeddingService.embed((String) null);

        // Then
        assertThat(embedding).hasSize(384);
        for (float value : embedding) {
            assertThat(value).isZero();
        }
    }

    @Test
    void embed_emptyText_returnsZeroVector() {
        // When
        float[] embedding = embeddingService.embed("");

        // Then
        assertThat(embedding).hasSize(384);
        for (float value : embedding) {
            assertThat(value).isZero();
        }
    }

    @Test
    void embed_textChunk_returnsEmbeddedChunk() {
        // Given
        TextChunk chunk = TextChunk.builder()
            .chunkId("test:001:summary:0")
            .content("Network connectivity issue")
            .chunkType(TextChunk.ChunkType.SUMMARY)
            .sourceType("Incident")
            .sourceId("INC001")
            .build();

        // When
        LocalEmbeddingService.EmbeddedChunk embeddedChunk = embeddingService.embed(chunk);

        // Then
        assertThat(embeddedChunk).isNotNull();
        assertThat(embeddedChunk.chunk()).isEqualTo(chunk);
        assertThat(embeddedChunk.embedding()).hasSize(384);
    }

    @Test
    void embedBatch_multipleTexts_returnsCorrectNumberOfEmbeddings() {
        // Given
        List<String> texts = Arrays.asList(
            "First text",
            "Second text",
            "Third text"
        );

        // When
        List<float[]> embeddings = embeddingService.embedBatch(texts);

        // Then
        assertThat(embeddings).hasSize(3);
        for (float[] embedding : embeddings) {
            assertThat(embedding).hasSize(384);
        }
    }

    @Test
    void embedBatch_emptyList_returnsEmptyList() {
        // When
        List<float[]> embeddings = embeddingService.embedBatch(List.of());

        // Then
        assertThat(embeddings).isEmpty();
    }

    @Test
    void embedBatch_nullList_returnsEmptyList() {
        // When
        List<float[]> embeddings = embeddingService.embedBatch(null);

        // Then
        assertThat(embeddings).isEmpty();
    }

    @Test
    void embedBatch_largeBatch_handlesCorrectly() {
        // Given
        List<String> texts = Arrays.asList(
            "Text 1", "Text 2", "Text 3", "Text 4", "Text 5",
            "Text 6", "Text 7", "Text 8", "Text 9", "Text 10",
            "Text 11", "Text 12", "Text 13", "Text 14", "Text 15",
            "Text 16", "Text 17", "Text 18", "Text 19", "Text 20",
            "Text 21", "Text 22", "Text 23", "Text 24", "Text 25",
            "Text 26", "Text 27", "Text 28", "Text 29", "Text 30",
            "Text 31", "Text 32", "Text 33", "Text 34", "Text 35"
        );

        // When
        List<float[]> embeddings = embeddingService.embedBatch(texts);

        // Then
        assertThat(embeddings).hasSize(35);
        for (float[] embedding : embeddings) {
            assertThat(embedding).hasSize(384);
        }
    }

    @Test
    void embedChunks_multipleChunks_returnsEmbeddedChunks() {
        // Given
        List<TextChunk> chunks = Arrays.asList(
            TextChunk.builder()
                .chunkId("test:001:summary:0")
                .content("Network issue")
                .chunkType(TextChunk.ChunkType.SUMMARY)
                .build(),
            TextChunk.builder()
                .chunkId("test:001:description:1")
                .content("Cannot connect to server")
                .chunkType(TextChunk.ChunkType.DESCRIPTION)
                .build()
        );

        // When
        List<LocalEmbeddingService.EmbeddedChunk> embeddedChunks = embeddingService.embedChunks(chunks);

        // Then
        assertThat(embeddedChunks).hasSize(2);
        for (int i = 0; i < chunks.size(); i++) {
            assertThat(embeddedChunks.get(i).chunk()).isEqualTo(chunks.get(i));
            assertThat(embeddedChunks.get(i).embedding()).hasSize(384);
        }
    }

    @Test
    void embedChunks_emptyList_returnsEmptyList() {
        // When
        List<LocalEmbeddingService.EmbeddedChunk> embeddedChunks = embeddingService.embedChunks(List.of());

        // Then
        assertThat(embeddedChunks).isEmpty();
    }

    @Test
    void embedChunks_nullList_returnsEmptyList() {
        // When
        List<LocalEmbeddingService.EmbeddedChunk> embeddedChunks = embeddingService.embedChunks(null);

        // Then
        assertThat(embeddedChunks).isEmpty();
    }

    @Test
    void cosineSimilarity_identicalVectors_returnsOne() {
        // Given
        float[] vector = embeddingService.embed("Network connectivity issue");

        // When
        double similarity = LocalEmbeddingService.cosineSimilarity(vector, vector);

        // Then
        assertThat(similarity).isCloseTo(1.0, within(0.0001));
    }

    @Test
    void cosineSimilarity_orthogonalVectors_returnsZero() {
        // Given
        float[] vector1 = new float[384];
        float[] vector2 = new float[384];
        vector1[0] = 1.0f;
        vector2[1] = 1.0f;

        // When
        double similarity = LocalEmbeddingService.cosineSimilarity(vector1, vector2);

        // Then
        assertThat(similarity).isCloseTo(0.0, within(0.0001));
    }

    @Test
    void cosineSimilarity_oppositeVectors_returnsNegativeOne() {
        // Given
        float[] vector1 = new float[384];
        float[] vector2 = new float[384];
        Arrays.fill(vector1, 1.0f);
        Arrays.fill(vector2, -1.0f);

        // When
        double similarity = LocalEmbeddingService.cosineSimilarity(vector1, vector2);

        // Then
        assertThat(similarity).isCloseTo(-1.0, within(0.0001));
    }

    @Test
    void cosineSimilarity_zeroVector_returnsZero() {
        // Given
        float[] vector1 = embeddingService.embed("Some text");
        float[] vector2 = new float[384]; // Zero vector

        // When
        double similarity = LocalEmbeddingService.cosineSimilarity(vector1, vector2);

        // Then
        assertThat(similarity).isZero();
    }

    @Test
    void cosineSimilarity_bothZeroVectors_returnsZero() {
        // Given
        float[] vector1 = new float[384];
        float[] vector2 = new float[384];

        // When
        double similarity = LocalEmbeddingService.cosineSimilarity(vector1, vector2);

        // Then
        assertThat(similarity).isZero();
    }

    @Test
    void cosineSimilarity_differentDimensions_throwsException() {
        // Given
        float[] vector1 = new float[384];
        float[] vector2 = new float[256];

        // When/Then
        assertThatThrownBy(() -> LocalEmbeddingService.cosineSimilarity(vector1, vector2))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Embedding dimensions must match");
    }

    @Test
    void embeddedChunk_embeddingAsList_convertsCorrectly() {
        // Given
        TextChunk chunk = TextChunk.builder()
            .chunkId("test:001:summary:0")
            .content("Test content")
            .build();

        LocalEmbeddingService.EmbeddedChunk embeddedChunk = embeddingService.embed(chunk);

        // When
        List<Float> embeddingList = embeddedChunk.embeddingAsList();

        // Then
        assertThat(embeddingList).hasSize(384);
        for (int i = 0; i < embeddedChunk.embedding().length; i++) {
            assertThat(embeddingList.get(i)).isEqualTo(embeddedChunk.embedding()[i]);
        }
    }

    @Test
    void embed_longText_handlesCorrectly() {
        // Given
        String longText = "This is a very long text. ".repeat(100);

        // When
        float[] embedding = embeddingService.embed(longText);

        // Then
        assertThat(embedding).hasSize(384);
        // Verify it's not a zero vector
        double sum = 0;
        for (float value : embedding) {
            sum += Math.abs(value);
        }
        assertThat(sum).isGreaterThan(0);
    }

    @Test
    void embed_specialCharacters_handlesCorrectly() {
        // Given
        String text = "Network issue: can't connect to server @192.168.1.1 #urgent!";

        // When
        float[] embedding = embeddingService.embed(text);

        // Then
        assertThat(embedding).hasSize(384);
        double sum = 0;
        for (float value : embedding) {
            sum += Math.abs(value);
        }
        assertThat(sum).isGreaterThan(0);
    }

    @Test
    void embed_nonEnglishCharacters_handlesCorrectly() {
        // Given
        String text = "Problème de réseau avec café ñoño";

        // When
        float[] embedding = embeddingService.embed(text);

        // Then
        assertThat(embedding).hasSize(384);
        double sum = 0;
        for (float value : embedding) {
            sum += Math.abs(value);
        }
        assertThat(sum).isGreaterThan(0);
    }

    @Test
    void embed_sameTextTwice_producesSameEmbedding() {
        // Given
        String text = "Network connectivity issue";

        // When
        float[] embedding1 = embeddingService.embed(text);
        float[] embedding2 = embeddingService.embed(text);

        // Then
        assertThat(embedding1).hasSize(384);
        assertThat(embedding2).hasSize(384);

        double similarity = LocalEmbeddingService.cosineSimilarity(embedding1, embedding2);
        assertThat(similarity).isCloseTo(1.0, within(0.0001));
    }

    @Test
    void embedBatch_maintainsOrder_correctly() {
        // Given
        List<String> texts = Arrays.asList("First", "Second", "Third");

        // When
        List<float[]> embeddings = embeddingService.embedBatch(texts);

        // Then
        assertThat(embeddings).hasSize(3);

        // Verify order by comparing with individual embeddings
        float[] firstIndividual = embeddingService.embed("First");
        double similarity = LocalEmbeddingService.cosineSimilarity(embeddings.get(0), firstIndividual);
        assertThat(similarity).isCloseTo(1.0, within(0.0001));
    }
}
