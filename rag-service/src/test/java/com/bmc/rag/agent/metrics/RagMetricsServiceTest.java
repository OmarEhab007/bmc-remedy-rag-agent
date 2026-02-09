package com.bmc.rag.agent.metrics;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RagMetricsServiceTest {

    private RagMetricsService metricsService;

    @BeforeEach
    void setUp() {
        metricsService = new RagMetricsService(new SimpleMeterRegistry());
    }

    @Nested
    @DisplayName("Counter Methods")
    class CounterMethods {

        @Test
        void recordRetrieval_incrementsCounter() {
            metricsService.recordRetrieval(5);
            metricsService.recordRetrieval(3);

            var snapshot = metricsService.getSnapshot();
            assertThat(snapshot.getTotalRetrievals()).isEqualTo(2);
            assertThat(snapshot.getLastRetrievalCount()).isEqualTo(3);
        }

        @Test
        void recordCitations_incrementsByCount() {
            metricsService.recordCitations(3);
            metricsService.recordCitations(2);

            var snapshot = metricsService.getSnapshot();
            assertThat(snapshot.getTotalCitations()).isEqualTo(5);
        }

        @Test
        void recordHallucination_incrementsCounter() {
            metricsService.recordHallucination();
            metricsService.recordHallucination();

            var snapshot = metricsService.getSnapshot();
            assertThat(snapshot.getHallucinationsDetected()).isEqualTo(2);
        }

        @Test
        void recordCacheHit_incrementsCounter() {
            metricsService.recordCacheHit();

            var snapshot = metricsService.getSnapshot();
            assertThat(snapshot.getCacheHits()).isEqualTo(1);
        }

        @Test
        void recordCacheMiss_incrementsCounter() {
            metricsService.recordCacheMiss();

            var snapshot = metricsService.getSnapshot();
            assertThat(snapshot.getCacheMisses()).isEqualTo(1);
        }

        @Test
        void recordError_incrementsCounter() {
            metricsService.recordError("retrieval");
            metricsService.recordError("generation");

            var snapshot = metricsService.getSnapshot();
            assertThat(snapshot.getTotalErrors()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Timer Methods")
    class TimerMethods {

        @Test
        void recordRetrievalLatency_recordsValue() {
            metricsService.recordRetrievalLatency(150);
            var snapshot = metricsService.getSnapshot();
            assertThat(snapshot.getRetrievalP50Ms()).isGreaterThan(0);
        }

        @Test
        void recordEmbeddingLatency_recordsValue() {
            metricsService.recordEmbeddingLatency(50);
            // No exception means success
        }

        @Test
        void recordGenerationLatency_recordsValue() {
            metricsService.recordGenerationLatency(500);
            var snapshot = metricsService.getSnapshot();
            assertThat(snapshot.getGenerationP50Ms()).isGreaterThan(0);
        }

        @Test
        void recordTotalLatency_recordsValue() {
            metricsService.recordTotalLatency(700);
            var snapshot = metricsService.getSnapshot();
            assertThat(snapshot.getTotalP50Ms()).isGreaterThan(0);
        }

        @Test
        void startRetrieval_stopRetrieval_tracksActiveCount() {
            Timer.Sample sample1 = metricsService.startRetrieval();
            Timer.Sample sample2 = metricsService.startRetrieval();

            var snapshot1 = metricsService.getSnapshot();
            assertThat(snapshot1.getActiveRetrievals()).isEqualTo(2);

            metricsService.stopRetrieval(sample1);
            var snapshot2 = metricsService.getSnapshot();
            assertThat(snapshot2.getActiveRetrievals()).isEqualTo(1);

            metricsService.stopRetrieval(sample2);
            var snapshot3 = metricsService.getSnapshot();
            assertThat(snapshot3.getActiveRetrievals()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Gauge Methods")
    class GaugeMethods {

        @Test
        void getAverageGroundednessScore_noScores_returnsZero() {
            assertThat(metricsService.getAverageGroundednessScore()).isEqualTo(0.0);
        }

        @Test
        void getAverageGroundednessScore_withScores_returnsAverage() {
            metricsService.recordGroundednessScore(0.8);
            metricsService.recordGroundednessScore(0.9);
            metricsService.recordGroundednessScore(1.0);

            assertThat(metricsService.getAverageGroundednessScore()).isCloseTo(0.9, org.assertj.core.data.Offset.offset(0.01));
        }

        @Test
        void getCacheHitRate_noActivity_returnsZero() {
            assertThat(metricsService.getCacheHitRate()).isEqualTo(0.0);
        }

        @Test
        void getCacheHitRate_withActivity_returnsRate() {
            metricsService.recordCacheHit();
            metricsService.recordCacheHit();
            metricsService.recordCacheMiss();

            assertThat(metricsService.getCacheHitRate()).isCloseTo(0.666, org.assertj.core.data.Offset.offset(0.01));
        }

        @Test
        void getCacheHitRate_allHits_returnsOne() {
            metricsService.recordCacheHit();
            metricsService.recordCacheHit();

            assertThat(metricsService.getCacheHitRate()).isEqualTo(1.0);
        }

        @Test
        void getCacheHitRate_allMisses_returnsZero() {
            metricsService.recordCacheMiss();
            metricsService.recordCacheMiss();

            assertThat(metricsService.getCacheHitRate()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Snapshot")
    class Snapshot {

        @Test
        void getSnapshot_initialState_allZeros() {
            var snapshot = metricsService.getSnapshot();
            assertThat(snapshot.getTotalRetrievals()).isEqualTo(0);
            assertThat(snapshot.getTotalCitations()).isEqualTo(0);
            assertThat(snapshot.getHallucinationsDetected()).isEqualTo(0);
            assertThat(snapshot.getCacheHits()).isEqualTo(0);
            assertThat(snapshot.getCacheMisses()).isEqualTo(0);
            assertThat(snapshot.getCacheHitRate()).isEqualTo(0.0);
            assertThat(snapshot.getAverageGroundednessScore()).isEqualTo(0.0);
            assertThat(snapshot.getActiveRetrievals()).isEqualTo(0);
            assertThat(snapshot.getLastRetrievalCount()).isEqualTo(0);
            assertThat(snapshot.getTotalErrors()).isEqualTo(0);
        }

        @Test
        void getSnapshot_afterActivity_reflectsAllMetrics() {
            metricsService.recordRetrieval(5);
            metricsService.recordCitations(3);
            metricsService.recordHallucination();
            metricsService.recordCacheHit();
            metricsService.recordCacheMiss();
            metricsService.recordError("test");
            metricsService.recordGroundednessScore(0.85);
            metricsService.recordRetrievalLatency(100);
            metricsService.recordGenerationLatency(200);
            metricsService.recordTotalLatency(300);

            var snapshot = metricsService.getSnapshot();
            assertThat(snapshot.getTotalRetrievals()).isEqualTo(1);
            assertThat(snapshot.getTotalCitations()).isEqualTo(3);
            assertThat(snapshot.getHallucinationsDetected()).isEqualTo(1);
            assertThat(snapshot.getCacheHits()).isEqualTo(1);
            assertThat(snapshot.getCacheMisses()).isEqualTo(1);
            assertThat(snapshot.getCacheHitRate()).isCloseTo(0.5, org.assertj.core.data.Offset.offset(0.01));
            assertThat(snapshot.getAverageGroundednessScore()).isCloseTo(0.85, org.assertj.core.data.Offset.offset(0.01));
            assertThat(snapshot.getLastRetrievalCount()).isEqualTo(5);
            assertThat(snapshot.getTotalErrors()).isEqualTo(1);
        }
    }
}
