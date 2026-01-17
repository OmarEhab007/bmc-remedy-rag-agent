package com.bmc.rag.agent.cache;

import com.bmc.rag.vectorization.embedding.LocalEmbeddingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Semantic cache service for caching query results based on embedding similarity (P2.2).
 * Uses Redis for distributed caching with cosine similarity matching.
 */
@Slf4j
@Service
public class SemanticCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final LocalEmbeddingService embeddingService;
    private final ObjectMapper objectMapper;
    private final boolean redisAvailable;

    @Value("${semantic-cache.enabled:true}")
    private boolean enabled;

    @Value("${semantic-cache.similarity-threshold:0.95}")
    private float similarityThreshold;

    @Value("${semantic-cache.ttl-hours:24}")
    private int ttlHours;

    @Value("${semantic-cache.max-entries:10000}")
    private int maxEntries;

    private static final String CACHE_KEY_PREFIX = "rag:semantic-cache:";
    private static final String EMBEDDING_KEY_PREFIX = "rag:cache-embedding:";
    private static final String INDEX_KEY = "rag:cache-index";

    // Statistics
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);

    public SemanticCacheService(
            @org.springframework.beans.factory.annotation.Autowired(required = false) RedisTemplate<String, String> redisTemplate,
            LocalEmbeddingService embeddingService,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.embeddingService = embeddingService;
        this.objectMapper = objectMapper;
        this.redisAvailable = redisTemplate != null;

        if (!redisAvailable) {
            log.warn("Redis not available - semantic caching disabled. Set up Redis for production use.");
        }
    }

    /**
     * Check if a semantically similar query result exists in cache.
     *
     * @param query The user's query
     * @return Cached result if found, empty optional otherwise
     */
    public Optional<CachedResult> get(String query) {
        if (!enabled || !redisAvailable) {
            return Optional.empty();
        }

        try {
            // Generate embedding for the query
            float[] queryEmbedding = embeddingService.embed(query);

            // Get all cached embeddings (in production, use vector index)
            Set<String> cacheKeys = redisTemplate.opsForSet().members(INDEX_KEY);
            if (cacheKeys == null || cacheKeys.isEmpty()) {
                misses.incrementAndGet();
                return Optional.empty();
            }

            // Find most similar cached query
            String bestMatch = null;
            float bestSimilarity = 0;

            for (String key : cacheKeys) {
                String embeddingJson = redisTemplate.opsForValue().get(EMBEDDING_KEY_PREFIX + key);
                if (embeddingJson == null) continue;

                float[] cachedEmbedding = parseEmbedding(embeddingJson);
                float similarity = cosineSimilarity(queryEmbedding, cachedEmbedding);

                if (similarity > bestSimilarity) {
                    bestSimilarity = similarity;
                    bestMatch = key;
                }
            }

            // Check if best match exceeds threshold
            if (bestMatch != null && bestSimilarity >= similarityThreshold) {
                String cachedJson = redisTemplate.opsForValue().get(CACHE_KEY_PREFIX + bestMatch);
                if (cachedJson != null) {
                    CachedResult result = objectMapper.readValue(cachedJson, CachedResult.class);
                    result.setSimilarity(bestSimilarity);
                    hits.incrementAndGet();
                    log.debug("Cache hit: similarity={:.4f} for query: {}", bestSimilarity, truncate(query));
                    return Optional.of(result);
                }
            }

            misses.incrementAndGet();
            return Optional.empty();

        } catch (Exception e) {
            log.warn("Cache lookup failed: {}", e.getMessage());
            misses.incrementAndGet();
            return Optional.empty();
        }
    }

    /**
     * Store a query result in the cache.
     *
     * @param query The original query
     * @param response The response to cache
     * @param sources The source references
     */
    public void put(String query, String response, List<String> sources) {
        if (!enabled || !redisAvailable) return;

        try {
            // Generate unique key
            String key = UUID.randomUUID().toString();

            // Generate and store embedding
            float[] embedding = embeddingService.embed(query);
            String embeddingJson = objectMapper.writeValueAsString(embedding);
            redisTemplate.opsForValue().set(
                EMBEDDING_KEY_PREFIX + key,
                embeddingJson,
                Duration.ofHours(ttlHours)
            );

            // Store cached result
            CachedResult result = new CachedResult(query, response, sources, System.currentTimeMillis());
            String resultJson = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(
                CACHE_KEY_PREFIX + key,
                resultJson,
                Duration.ofHours(ttlHours)
            );

            // Add to index
            redisTemplate.opsForSet().add(INDEX_KEY, key);

            // Enforce max entries limit
            enforceMaxEntries();

            log.debug("Cached response for query: {}", truncate(query));

        } catch (Exception e) {
            log.warn("Cache storage failed: {}", e.getMessage());
        }
    }

    /**
     * Clear all cached entries.
     */
    public void clear() {
        if (!redisAvailable) return;

        try {
            Set<String> keys = redisTemplate.opsForSet().members(INDEX_KEY);
            if (keys != null) {
                for (String key : keys) {
                    redisTemplate.delete(CACHE_KEY_PREFIX + key);
                    redisTemplate.delete(EMBEDDING_KEY_PREFIX + key);
                }
            }
            redisTemplate.delete(INDEX_KEY);
            log.info("Semantic cache cleared");
        } catch (Exception e) {
            log.warn("Cache clear failed: {}", e.getMessage());
        }
    }

    /**
     * Get cache statistics.
     */
    public CacheStats getStats() {
        long hitCount = hits.get();
        long missCount = misses.get();
        long total = hitCount + missCount;
        double hitRate = total > 0 ? (double) hitCount / total : 0;

        int size = 0;
        if (redisAvailable) {
            Long sizeValue = redisTemplate.opsForSet().size(INDEX_KEY);
            size = sizeValue != null ? sizeValue.intValue() : 0;
        }

        return new CacheStats(
            hitCount,
            missCount,
            hitRate,
            size
        );
    }

    /**
     * Check if cache is enabled and Redis is available.
     */
    public boolean isEnabled() {
        return enabled && redisAvailable;
    }

    private void enforceMaxEntries() {
        if (!redisAvailable) return;

        try {
            Long size = redisTemplate.opsForSet().size(INDEX_KEY);
            if (size != null && size > maxEntries) {
                // Remove random entries (in production, use LRU)
                int toRemove = (int) (size - maxEntries);
                Set<String> keys = redisTemplate.opsForSet().members(INDEX_KEY);
                if (keys != null) {
                    int removed = 0;
                    for (String key : keys) {
                        if (removed >= toRemove) break;
                        redisTemplate.delete(CACHE_KEY_PREFIX + key);
                        redisTemplate.delete(EMBEDDING_KEY_PREFIX + key);
                        redisTemplate.opsForSet().remove(INDEX_KEY, key);
                        removed++;
                    }
                    log.debug("Evicted {} cache entries", removed);
                }
            }
        } catch (Exception e) {
            log.warn("Cache eviction failed: {}", e.getMessage());
        }
    }

    private float cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0;

        float dotProduct = 0;
        float normA = 0;
        float normB = 0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0 || normB == 0) return 0;
        return dotProduct / (float) (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private float[] parseEmbedding(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, float[].class);
    }

    private String truncate(String text) {
        if (text == null) return "";
        return text.length() > 50 ? text.substring(0, 50) + "..." : text;
    }

    /**
     * Cached result record.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CachedResult {
        private String query;
        private String response;
        private List<String> sources;
        private long timestamp;
        private float similarity;

        public CachedResult(String query, String response, List<String> sources, long timestamp) {
            this.query = query;
            this.response = response;
            this.sources = sources;
            this.timestamp = timestamp;
        }
    }

    /**
     * Cache statistics record.
     */
    public record CacheStats(
        long hits,
        long misses,
        double hitRate,
        int size
    ) {}
}
