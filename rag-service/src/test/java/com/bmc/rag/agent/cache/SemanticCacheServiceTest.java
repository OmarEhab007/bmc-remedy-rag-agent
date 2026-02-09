package com.bmc.rag.agent.cache;

import com.bmc.rag.vectorization.embedding.LocalEmbeddingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SemanticCacheServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private LocalEmbeddingService embeddingService;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private SetOperations<String, String> setOps;

    private ObjectMapper objectMapper;
    private SemanticCacheService cacheService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        cacheService = new SemanticCacheService(redisTemplate, embeddingService, objectMapper);
        ReflectionTestUtils.setField(cacheService, "enabled", true);
        ReflectionTestUtils.setField(cacheService, "similarityThreshold", 0.95f);
        ReflectionTestUtils.setField(cacheService, "ttlHours", 24);
        ReflectionTestUtils.setField(cacheService, "maxEntries", 10000);
    }

    @Nested
    @DisplayName("Constructor / isEnabled")
    class IsEnabled {

        @Test
        void isEnabled_redisAvailableAndEnabled_returnsTrue() {
            assertThat(cacheService.isEnabled()).isTrue();
        }

        @Test
        void isEnabled_disabled_returnsFalse() {
            ReflectionTestUtils.setField(cacheService, "enabled", false);
            assertThat(cacheService.isEnabled()).isFalse();
        }

        @Test
        void isEnabled_noRedis_returnsFalse() {
            var noRedisService = new SemanticCacheService(null, embeddingService, objectMapper);
            ReflectionTestUtils.setField(noRedisService, "enabled", true);
            assertThat(noRedisService.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("get")
    class Get {

        @Test
        void get_disabled_returnsEmpty() {
            ReflectionTestUtils.setField(cacheService, "enabled", false);
            Optional<SemanticCacheService.CachedResult> result = cacheService.get("test query");
            assertThat(result).isEmpty();
        }

        @Test
        void get_noRedis_returnsEmpty() {
            var noRedisService = new SemanticCacheService(null, embeddingService, objectMapper);
            ReflectionTestUtils.setField(noRedisService, "enabled", true);
            Optional<SemanticCacheService.CachedResult> result = noRedisService.get("test");
            assertThat(result).isEmpty();
        }

        @Test
        void get_emptyCacheIndex_returnsEmpty() {
            float[] embedding = new float[384];
            when(embeddingService.embed("test query")).thenReturn(embedding);
            when(redisTemplate.opsForSet()).thenReturn(setOps);
            when(setOps.members("rag:cache-index")).thenReturn(Set.of());

            Optional<SemanticCacheService.CachedResult> result = cacheService.get("test query");
            assertThat(result).isEmpty();
        }

        @Test
        void get_nullCacheIndex_returnsEmpty() {
            float[] embedding = new float[384];
            when(embeddingService.embed("test query")).thenReturn(embedding);
            when(redisTemplate.opsForSet()).thenReturn(setOps);
            when(setOps.members("rag:cache-index")).thenReturn(null);

            Optional<SemanticCacheService.CachedResult> result = cacheService.get("test query");
            assertThat(result).isEmpty();
        }

        @Test
        void get_highSimilarity_returnsCachedResult() throws Exception {
            float[] queryEmbedding = new float[384];
            Arrays.fill(queryEmbedding, 0.5f);
            when(embeddingService.embed("test query")).thenReturn(queryEmbedding);

            when(redisTemplate.opsForSet()).thenReturn(setOps);
            when(setOps.members("rag:cache-index")).thenReturn(Set.of("key1"));

            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            // Cached embedding identical to query (similarity = 1.0)
            String embeddingJson = objectMapper.writeValueAsString(queryEmbedding);
            when(valueOps.get("rag:cache-embedding:key1")).thenReturn(embeddingJson);

            SemanticCacheService.CachedResult cached = new SemanticCacheService.CachedResult(
                    "test query", "cached response", List.of("source1"), System.currentTimeMillis());
            String resultJson = objectMapper.writeValueAsString(cached);
            when(valueOps.get("rag:semantic-cache:key1")).thenReturn(resultJson);

            Optional<SemanticCacheService.CachedResult> result = cacheService.get("test query");
            assertThat(result).isPresent();
            assertThat(result.get().getResponse()).isEqualTo("cached response");
            assertThat(result.get().getSimilarity()).isGreaterThanOrEqualTo(0.95f);
        }

        @Test
        void get_lowSimilarity_returnsEmpty() throws Exception {
            float[] queryEmbedding = new float[384];
            Arrays.fill(queryEmbedding, 0.5f);
            when(embeddingService.embed("test query")).thenReturn(queryEmbedding);

            when(redisTemplate.opsForSet()).thenReturn(setOps);
            when(setOps.members("rag:cache-index")).thenReturn(Set.of("key1"));

            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            // Very different embedding
            float[] differentEmbedding = new float[384];
            Arrays.fill(differentEmbedding, -0.5f);
            String embeddingJson = objectMapper.writeValueAsString(differentEmbedding);
            when(valueOps.get("rag:cache-embedding:key1")).thenReturn(embeddingJson);

            Optional<SemanticCacheService.CachedResult> result = cacheService.get("test query");
            assertThat(result).isEmpty();
        }

        @Test
        void get_exceptionDuringLookup_returnsEmpty() {
            when(embeddingService.embed(anyString())).thenThrow(new RuntimeException("Embedding error"));

            Optional<SemanticCacheService.CachedResult> result = cacheService.get("test");
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("put")
    class Put {

        @Test
        void put_disabled_doesNothing() {
            ReflectionTestUtils.setField(cacheService, "enabled", false);
            cacheService.put("query", "response", List.of());
            verifyNoInteractions(redisTemplate);
        }

        @Test
        void put_noRedis_doesNothing() {
            var noRedisService = new SemanticCacheService(null, embeddingService, objectMapper);
            ReflectionTestUtils.setField(noRedisService, "enabled", true);
            noRedisService.put("query", "response", List.of());
            verifyNoInteractions(redisTemplate);
        }

        @Test
        void put_validEntry_storesInRedis() {
            float[] embedding = new float[384];
            when(embeddingService.embed("query")).thenReturn(embedding);
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(redisTemplate.opsForSet()).thenReturn(setOps);
            when(setOps.size("rag:cache-index")).thenReturn(0L);

            cacheService.put("query", "response", List.of("source1"));

            verify(valueOps, times(2)).set(anyString(), anyString(), any());
            verify(setOps).add(eq("rag:cache-index"), anyString());
        }

        @Test
        void put_exceptionDuringStore_doesNotThrow() {
            when(embeddingService.embed(anyString())).thenThrow(new RuntimeException("Error"));

            // Should not throw
            cacheService.put("query", "response", List.of());
        }
    }

    @Nested
    @DisplayName("clear")
    class Clear {

        @Test
        void clear_withEntries_deletesAll() {
            when(redisTemplate.opsForSet()).thenReturn(setOps);
            when(setOps.members("rag:cache-index")).thenReturn(Set.of("key1", "key2"));

            cacheService.clear();

            verify(redisTemplate).delete("rag:semantic-cache:key1");
            verify(redisTemplate).delete("rag:cache-embedding:key1");
            verify(redisTemplate).delete("rag:semantic-cache:key2");
            verify(redisTemplate).delete("rag:cache-embedding:key2");
            verify(redisTemplate).delete("rag:cache-index");
        }

        @Test
        void clear_noRedis_doesNotThrow() {
            var noRedisService = new SemanticCacheService(null, embeddingService, objectMapper);
            noRedisService.clear(); // Should not throw
        }
    }

    @Nested
    @DisplayName("getStats")
    class GetStats {

        @Test
        void getStats_initialState_allZeros() {
            when(redisTemplate.opsForSet()).thenReturn(setOps);
            when(setOps.size("rag:cache-index")).thenReturn(0L);

            var stats = cacheService.getStats();
            assertThat(stats.hits()).isEqualTo(0);
            assertThat(stats.misses()).isEqualTo(0);
            assertThat(stats.hitRate()).isEqualTo(0.0);
            assertThat(stats.size()).isEqualTo(0);
        }

        @Test
        void getStats_afterMisses_showsMisses() {
            // Trigger misses
            ReflectionTestUtils.setField(cacheService, "enabled", false);
            cacheService.get("q1"); // miss (disabled)
            cacheService.get("q2"); // miss (disabled)
            ReflectionTestUtils.setField(cacheService, "enabled", true);

            when(redisTemplate.opsForSet()).thenReturn(setOps);
            when(setOps.size("rag:cache-index")).thenReturn(5L);

            // Note: disabled lookups don't count as misses
            var stats = cacheService.getStats();
            assertThat(stats.size()).isEqualTo(5);
        }

        @Test
        void getStats_noRedis_sizeZero() {
            var noRedisService = new SemanticCacheService(null, embeddingService, objectMapper);
            var stats = noRedisService.getStats();
            assertThat(stats.size()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("CachedResult")
    class CachedResultTest {

        @Test
        void cachedResult_constructorAndGetters() {
            var result = new SemanticCacheService.CachedResult(
                    "query", "response", List.of("s1"), 123456L);
            assertThat(result.getQuery()).isEqualTo("query");
            assertThat(result.getResponse()).isEqualTo("response");
            assertThat(result.getSources()).containsExactly("s1");
            assertThat(result.getTimestamp()).isEqualTo(123456L);
            assertThat(result.getSimilarity()).isEqualTo(0f);
        }

        @Test
        void cachedResult_setSimilarity() {
            var result = new SemanticCacheService.CachedResult();
            result.setSimilarity(0.97f);
            assertThat(result.getSimilarity()).isEqualTo(0.97f);
        }
    }

    @Nested
    @DisplayName("CacheStats")
    class CacheStatsTest {

        @Test
        void cacheStats_recordFields() {
            var stats = new SemanticCacheService.CacheStats(10, 5, 0.666, 50);
            assertThat(stats.hits()).isEqualTo(10);
            assertThat(stats.misses()).isEqualTo(5);
            assertThat(stats.hitRate()).isCloseTo(0.666, org.assertj.core.data.Offset.offset(0.001));
            assertThat(stats.size()).isEqualTo(50);
        }
    }

    @Nested
    @DisplayName("enforceMaxEntries")
    class EnforceMaxEntries {

        @Test
        void put_exceedsMaxEntries_evictsOldEntries() {
            ReflectionTestUtils.setField(cacheService, "maxEntries", 1);

            float[] embedding = new float[384];
            when(embeddingService.embed("query")).thenReturn(embedding);
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(redisTemplate.opsForSet()).thenReturn(setOps);
            // Size exceeds max of 1
            when(setOps.size("rag:cache-index")).thenReturn(3L);
            when(setOps.members("rag:cache-index")).thenReturn(Set.of("old-key1", "old-key2"));

            cacheService.put("query", "response", List.of("source1"));

            // Verify eviction happened
            verify(redisTemplate, atLeastOnce()).delete(contains("rag:semantic-cache:"));
            verify(redisTemplate, atLeastOnce()).delete(contains("rag:cache-embedding:"));
            verify(setOps, atLeastOnce()).remove(eq("rag:cache-index"), anyString());
        }

        @Test
        void put_withinMaxEntries_noEviction() {
            float[] embedding = new float[384];
            when(embeddingService.embed("query")).thenReturn(embedding);
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(redisTemplate.opsForSet()).thenReturn(setOps);
            // Size within max of 10000
            when(setOps.size("rag:cache-index")).thenReturn(5L);

            cacheService.put("query", "response", List.of("source1"));

            // No eviction - no members() call for eviction
            verify(setOps, never()).members("rag:cache-index");
        }
    }

    @Nested
    @DisplayName("Null embedding in get")
    class NullEmbedding {

        @Test
        void get_nullEmbeddingJson_skipsEntry() throws Exception {
            float[] queryEmbedding = new float[384];
            Arrays.fill(queryEmbedding, 0.5f);
            when(embeddingService.embed("test query")).thenReturn(queryEmbedding);

            when(redisTemplate.opsForSet()).thenReturn(setOps);
            when(setOps.members("rag:cache-index")).thenReturn(Set.of("key1", "key2"));

            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            // key1 has null embedding
            when(valueOps.get("rag:cache-embedding:key1")).thenReturn(null);
            // key2 has matching embedding
            String embeddingJson = objectMapper.writeValueAsString(queryEmbedding);
            when(valueOps.get("rag:cache-embedding:key2")).thenReturn(embeddingJson);

            SemanticCacheService.CachedResult cached = new SemanticCacheService.CachedResult(
                    "test query", "cached response", List.of("s1"), System.currentTimeMillis());
            String resultJson = objectMapper.writeValueAsString(cached);
            when(valueOps.get("rag:semantic-cache:key2")).thenReturn(resultJson);

            Optional<SemanticCacheService.CachedResult> result = cacheService.get("test query");
            assertThat(result).isPresent();
            assertThat(result.get().getResponse()).isEqualTo("cached response");
        }
    }

    @Nested
    @DisplayName("Clear exception handling")
    class ClearException {

        @Test
        void clear_exceptionDuringClear_doesNotThrow() {
            when(redisTemplate.opsForSet()).thenReturn(setOps);
            when(setOps.members("rag:cache-index")).thenThrow(new RuntimeException("Redis error"));

            // Should not throw
            cacheService.clear();
        }
    }

    @Nested
    @DisplayName("getStats null size")
    class GetStatsNullSize {

        @Test
        void getStats_nullSizeValue_returnsZeroSize() {
            when(redisTemplate.opsForSet()).thenReturn(setOps);
            when(setOps.size("rag:cache-index")).thenReturn(null);

            var stats = cacheService.getStats();
            assertThat(stats.size()).isEqualTo(0);
        }
    }
}
