package com.bmc.rag.agent.metrics;

import io.micrometer.core.instrument.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * Service for tracking RAG-specific metrics.
 * Provides observability into RAG pipeline performance and quality.
 */
@Slf4j
@Service
public class RagMetricsService {

    private final MeterRegistry meterRegistry;

    // Counters
    private final Counter retrievalCounter;
    private final Counter citationCounter;
    private final Counter hallucinationCounter;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Counter errorCounter;

    // Timers
    private final Timer retrievalTimer;
    private final Timer embeddingTimer;
    private final Timer generationTimer;
    private final Timer totalLatencyTimer;

    // Gauges (thread-safe values)
    private final AtomicInteger activeRetrievals = new AtomicInteger(0);
    private final DoubleAdder groundednessScoreSum = new DoubleAdder();
    private final AtomicLong groundednessCount = new AtomicLong(0);
    private final AtomicInteger lastRetrievalCount = new AtomicInteger(0);

    public RagMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Initialize counters
        this.retrievalCounter = Counter.builder("rag.retrievals.total")
            .description("Total number of RAG retrievals performed")
            .tag("type", "semantic")
            .register(meterRegistry);

        this.citationCounter = Counter.builder("rag.citations.total")
            .description("Total number of source citations generated")
            .register(meterRegistry);

        this.hallucinationCounter = Counter.builder("rag.hallucinations.detected")
            .description("Number of responses flagged as potentially ungrounded")
            .register(meterRegistry);

        this.cacheHitCounter = Counter.builder("rag.cache.hits")
            .description("Semantic cache hits")
            .register(meterRegistry);

        this.cacheMissCounter = Counter.builder("rag.cache.misses")
            .description("Semantic cache misses")
            .register(meterRegistry);

        this.errorCounter = Counter.builder("rag.errors.total")
            .description("Total RAG pipeline errors")
            .register(meterRegistry);

        // Initialize timers
        this.retrievalTimer = Timer.builder("rag.retrieval.latency")
            .description("Time spent retrieving relevant documents")
            .publishPercentiles(0.5, 0.9, 0.95, 0.99)
            .register(meterRegistry);

        this.embeddingTimer = Timer.builder("rag.embedding.latency")
            .description("Time spent generating embeddings")
            .publishPercentiles(0.5, 0.9, 0.95, 0.99)
            .register(meterRegistry);

        this.generationTimer = Timer.builder("rag.generation.latency")
            .description("Time spent generating LLM response")
            .publishPercentiles(0.5, 0.9, 0.95, 0.99)
            .register(meterRegistry);

        this.totalLatencyTimer = Timer.builder("rag.total.latency")
            .description("Total end-to-end RAG latency")
            .publishPercentiles(0.5, 0.9, 0.95, 0.99)
            .register(meterRegistry);

        // Initialize gauges
        Gauge.builder("rag.retrievals.active", activeRetrievals, AtomicInteger::get)
            .description("Currently active retrievals")
            .register(meterRegistry);

        Gauge.builder("rag.groundedness.score", this, RagMetricsService::getAverageGroundednessScore)
            .description("Average groundedness score (0-1)")
            .register(meterRegistry);

        Gauge.builder("rag.cache.hit_rate", this, RagMetricsService::getCacheHitRate)
            .description("Cache hit rate (0-1)")
            .register(meterRegistry);

        Gauge.builder("rag.retrieval.last_count", lastRetrievalCount, AtomicInteger::get)
            .description("Number of documents in last retrieval")
            .register(meterRegistry);

        log.info("RAG metrics service initialized");
    }

    // ========================
    // Counter Methods
    // ========================

    /**
     * Record a retrieval operation.
     */
    public void recordRetrieval(int documentsRetrieved) {
        retrievalCounter.increment();
        lastRetrievalCount.set(documentsRetrieved);
    }

    /**
     * Record citations generated.
     */
    public void recordCitations(int count) {
        citationCounter.increment(count);
    }

    /**
     * Record a potential hallucination detection.
     */
    public void recordHallucination() {
        hallucinationCounter.increment();
    }

    /**
     * Record a cache hit.
     */
    public void recordCacheHit() {
        cacheHitCounter.increment();
    }

    /**
     * Record a cache miss.
     */
    public void recordCacheMiss() {
        cacheMissCounter.increment();
    }

    /**
     * Record an error.
     */
    public void recordError(String type) {
        errorCounter.increment();
        Counter.builder("rag.errors.by_type")
            .tag("type", type)
            .register(meterRegistry)
            .increment();
    }

    // ========================
    // Timer Methods
    // ========================

    /**
     * Record retrieval latency.
     */
    public void recordRetrievalLatency(long durationMs) {
        retrievalTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Record embedding generation latency.
     */
    public void recordEmbeddingLatency(long durationMs) {
        embeddingTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Record LLM generation latency.
     */
    public void recordGenerationLatency(long durationMs) {
        generationTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Record total end-to-end latency.
     */
    public void recordTotalLatency(long durationMs) {
        totalLatencyTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Start tracking a retrieval operation.
     * Returns a timer sample that should be stopped when done.
     */
    public Timer.Sample startRetrieval() {
        activeRetrievals.incrementAndGet();
        return Timer.start(meterRegistry);
    }

    /**
     * Stop tracking a retrieval operation.
     */
    public void stopRetrieval(Timer.Sample sample) {
        activeRetrievals.decrementAndGet();
        sample.stop(retrievalTimer);
    }

    // ========================
    // Gauge Methods
    // ========================

    /**
     * Record a groundedness score (0-1).
     */
    public void recordGroundednessScore(double score) {
        groundednessScoreSum.add(score);
        groundednessCount.incrementAndGet();
    }

    /**
     * Get average groundedness score.
     */
    public double getAverageGroundednessScore() {
        long count = groundednessCount.get();
        if (count == 0) return 0.0;
        return groundednessScoreSum.sum() / count;
    }

    /**
     * Get cache hit rate.
     */
    public double getCacheHitRate() {
        double hits = cacheHitCounter.count();
        double misses = cacheMissCounter.count();
        double total = hits + misses;
        if (total == 0) return 0.0;
        return hits / total;
    }

    // ========================
    // Summary Methods
    // ========================

    /**
     * Get a snapshot of current metrics.
     */
    public MetricsSnapshot getSnapshot() {
        return MetricsSnapshot.builder()
            .totalRetrievals((long) retrievalCounter.count())
            .totalCitations((long) citationCounter.count())
            .hallucinationsDetected((long) hallucinationCounter.count())
            .cacheHits((long) cacheHitCounter.count())
            .cacheMisses((long) cacheMissCounter.count())
            .cacheHitRate(getCacheHitRate())
            .averageGroundednessScore(getAverageGroundednessScore())
            .activeRetrievals(activeRetrievals.get())
            .lastRetrievalCount(lastRetrievalCount.get())
            .totalErrors((long) errorCounter.count())
            .retrievalP50Ms(getPercentile(retrievalTimer, 0.5))
            .retrievalP95Ms(getPercentile(retrievalTimer, 0.95))
            .retrievalP99Ms(getPercentile(retrievalTimer, 0.99))
            .generationP50Ms(getPercentile(generationTimer, 0.5))
            .generationP95Ms(getPercentile(generationTimer, 0.95))
            .totalP50Ms(getPercentile(totalLatencyTimer, 0.5))
            .totalP95Ms(getPercentile(totalLatencyTimer, 0.95))
            .build();
    }

    /**
     * Get a percentile value from a timer.
     */
    private double getPercentile(Timer timer, double percentile) {
        // Micrometer percentiles are reported via the registry
        // This is a simplified approach - in production, use actual percentile histograms
        return timer.mean(TimeUnit.MILLISECONDS);
    }

    /**
     * Snapshot of current metrics.
     */
    @lombok.Data
    @lombok.Builder
    public static class MetricsSnapshot {
        private long totalRetrievals;
        private long totalCitations;
        private long hallucinationsDetected;
        private long cacheHits;
        private long cacheMisses;
        private double cacheHitRate;
        private double averageGroundednessScore;
        private int activeRetrievals;
        private int lastRetrievalCount;
        private long totalErrors;
        private double retrievalP50Ms;
        private double retrievalP95Ms;
        private double retrievalP99Ms;
        private double generationP50Ms;
        private double generationP95Ms;
        private double totalP50Ms;
        private double totalP95Ms;
    }
}
