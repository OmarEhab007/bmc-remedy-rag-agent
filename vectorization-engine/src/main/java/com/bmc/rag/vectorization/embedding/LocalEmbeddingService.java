package com.bmc.rag.vectorization.embedding;

import com.bmc.rag.vectorization.chunking.TextChunk;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.output.Response;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Local embedding service using all-minilm-l6-v2 model.
 * Runs entirely in-process via ONNX runtime (no external API calls).
 * Produces 384-dimensional embeddings.
 */
@Slf4j
@Service
public class LocalEmbeddingService {

    private EmbeddingModel embeddingModel;

    // Embedding dimensions for all-minilm-l6-v2
    public static final int EMBEDDING_DIMENSION = 384;

    // Batch size for embedding multiple texts
    private static final int BATCH_SIZE = 32;

    @PostConstruct
    public void init() {
        log.info("Initializing local embedding model (all-minilm-l6-v2)...");
        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        log.info("Embedding model initialized. Dimension: {}", EMBEDDING_DIMENSION);
    }

    /**
     * Embed a single text.
     *
     * @param text The text to embed
     * @return Embedding vector (384 dimensions)
     */
    public float[] embed(String text) {
        if (text == null || text.isEmpty()) {
            return new float[EMBEDDING_DIMENSION];
        }

        Response<Embedding> response = embeddingModel.embed(text);
        return response.content().vector();
    }

    /**
     * Embed a text chunk.
     *
     * @param chunk The text chunk to embed
     * @return EmbeddedChunk containing the chunk and its embedding
     */
    public EmbeddedChunk embed(TextChunk chunk) {
        float[] embedding = embed(chunk.getContent());
        return new EmbeddedChunk(chunk, embedding);
    }

    /**
     * Embed multiple texts in batches.
     *
     * @param texts The texts to embed
     * @return List of embedding vectors
     */
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return new ArrayList<>();
        }

        List<float[]> embeddings = new ArrayList<>(texts.size());
        AtomicInteger processed = new AtomicInteger(0);

        // Process in batches
        for (int i = 0; i < texts.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, texts.size());
            List<String> batch = texts.subList(i, end);

            List<TextSegment> segments = batch.stream()
                .map(TextSegment::from)
                .toList();

            Response<List<Embedding>> response = embeddingModel.embedAll(segments);

            for (Embedding embedding : response.content()) {
                embeddings.add(embedding.vector());
            }

            processed.addAndGet(batch.size());
            log.debug("Embedded {}/{} texts", processed.get(), texts.size());
        }

        return embeddings;
    }

    /**
     * Embed multiple text chunks in batches.
     *
     * @param chunks The chunks to embed
     * @return List of EmbeddedChunk objects
     */
    public List<EmbeddedChunk> embedChunks(List<TextChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return new ArrayList<>();
        }

        log.info("Embedding {} chunks...", chunks.size());

        List<String> texts = chunks.stream()
            .map(TextChunk::getContent)
            .toList();

        List<float[]> embeddings = embedBatch(texts);

        List<EmbeddedChunk> embeddedChunks = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            embeddedChunks.add(new EmbeddedChunk(chunks.get(i), embeddings.get(i)));
        }

        log.info("Completed embedding {} chunks", chunks.size());
        return embeddedChunks;
    }

    /**
     * Get the embedding dimension.
     */
    public int getDimension() {
        return EMBEDDING_DIMENSION;
    }

    /**
     * Calculate cosine similarity between two embeddings.
     * Returns 0.0 if either embedding is a zero vector to avoid division by zero.
     *
     * @param embedding1 First embedding
     * @param embedding2 Second embedding
     * @return Cosine similarity (0-1), or 0.0 if either vector is zero
     */
    public static double cosineSimilarity(float[] embedding1, float[] embedding2) {
        if (embedding1.length != embedding2.length) {
            throw new IllegalArgumentException("Embedding dimensions must match");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < embedding1.length; i++) {
            dotProduct += embedding1[i] * embedding2[i];
            norm1 += embedding1[i] * embedding1[i];
            norm2 += embedding2[i] * embedding2[i];
        }

        // Handle zero vectors to avoid division by zero
        double denominator = Math.sqrt(norm1) * Math.sqrt(norm2);
        if (denominator == 0.0) {
            // If either vector is zero, similarity is undefined - return 0
            return 0.0;
        }

        return dotProduct / denominator;
    }

    /**
     * Container for a chunk and its embedding.
     */
    public record EmbeddedChunk(TextChunk chunk, float[] embedding) {

        /**
         * Get embedding as a list (for database compatibility).
         */
        public List<Float> embeddingAsList() {
            List<Float> list = new ArrayList<>(embedding.length);
            for (float f : embedding) {
                list.add(f);
            }
            return list;
        }
    }
}
