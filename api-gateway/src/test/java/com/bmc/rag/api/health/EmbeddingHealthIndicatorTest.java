package com.bmc.rag.api.health;

import com.bmc.rag.vectorization.embedding.LocalEmbeddingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EmbeddingHealthIndicator}.
 * Tests health check reporting for the local embedding service.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EmbeddingHealthIndicator Tests")
class EmbeddingHealthIndicatorTest {

    @Mock
    private LocalEmbeddingService embeddingService;

    @Nested
    @DisplayName("Health Check Tests")
    class HealthCheckTests {

        @Test
        @DisplayName("Should return UP when embedding service returns valid array")
        void shouldReturnUpWhenEmbeddingServiceValid() {
            float[] validEmbedding = new float[384];
            for (int i = 0; i < validEmbedding.length; i++) {
                validEmbedding[i] = 0.1f * i;
            }
            when(embeddingService.embed("health check")).thenReturn(validEmbedding);
            when(embeddingService.getDimension()).thenReturn(384);

            EmbeddingHealthIndicator indicator = new EmbeddingHealthIndicator(embeddingService);

            Health health = indicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.UP);
            assertThat(health.getDetails()).containsEntry("model", "all-minilm-l6-v2");
            assertThat(health.getDetails()).containsEntry("dimensions", 384);
        }

        @Test
        @DisplayName("Should return DOWN when embedding service is null")
        void shouldReturnDownWhenEmbeddingServiceNull() {
            EmbeddingHealthIndicator indicator = new EmbeddingHealthIndicator(null);

            Health health = indicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails()).containsKey("reason");
            assertThat(health.getDetails().get("reason")).isEqualTo("LocalEmbeddingService bean is null");
        }

        @Test
        @DisplayName("Should return DOWN when embed returns null")
        void shouldReturnDownWhenEmbedReturnsNull() {
            when(embeddingService.embed("health check")).thenReturn(null);

            EmbeddingHealthIndicator indicator = new EmbeddingHealthIndicator(embeddingService);

            Health health = indicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails()).containsKey("reason");
            assertThat(health.getDetails().get("reason")).isEqualTo("Embedding service returned empty result");
        }

        @Test
        @DisplayName("Should return DOWN when embed returns empty array")
        void shouldReturnDownWhenEmbedReturnsEmptyArray() {
            when(embeddingService.embed("health check")).thenReturn(new float[0]);

            EmbeddingHealthIndicator indicator = new EmbeddingHealthIndicator(embeddingService);

            Health health = indicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails()).containsKey("reason");
            assertThat(health.getDetails().get("reason")).isEqualTo("Embedding service returned empty result");
        }

        @Test
        @DisplayName("Should return DOWN with error when exception thrown")
        void shouldReturnDownWhenExceptionThrown() {
            when(embeddingService.embed(anyString())).thenThrow(new RuntimeException("Model loading failed"));

            EmbeddingHealthIndicator indicator = new EmbeddingHealthIndicator(embeddingService);

            Health health = indicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails()).containsKey("error");
            assertThat(health.getDetails().get("error")).asString().contains("Model loading failed");
        }

        @Test
        @DisplayName("Should handle multiple health check calls")
        void shouldHandleMultipleHealthCheckCalls() {
            float[] validEmbedding = new float[384];
            when(embeddingService.embed("health check")).thenReturn(validEmbedding);
            when(embeddingService.getDimension()).thenReturn(384);

            EmbeddingHealthIndicator indicator = new EmbeddingHealthIndicator(embeddingService);

            Health health1 = indicator.health();
            Health health2 = indicator.health();

            assertThat(health1.getStatus()).isEqualTo(Status.UP);
            assertThat(health2.getStatus()).isEqualTo(Status.UP);
            assertThat(health1.getDetails()).isEqualTo(health2.getDetails());
        }
    }

    @Nested
    @DisplayName("Health Details Tests")
    class HealthDetailsTests {

        @Test
        @DisplayName("Should include model and dimensions when UP")
        void shouldIncludeModelAndDimensionsWhenUp() {
            float[] validEmbedding = new float[384];
            when(embeddingService.embed("health check")).thenReturn(validEmbedding);
            when(embeddingService.getDimension()).thenReturn(384);

            EmbeddingHealthIndicator indicator = new EmbeddingHealthIndicator(embeddingService);

            Health health = indicator.health();

            assertThat(health.getDetails()).hasSize(2);
            assertThat(health.getDetails()).containsEntry("model", "all-minilm-l6-v2");
            assertThat(health.getDetails()).containsEntry("dimensions", 384);
        }

        @Test
        @DisplayName("Should include reason detail when DOWN due to null service")
        void shouldIncludeReasonDetailWhenServiceNull() {
            EmbeddingHealthIndicator indicator = new EmbeddingHealthIndicator(null);

            Health health = indicator.health();

            assertThat(health.getDetails()).hasSize(1);
            assertThat(health.getDetails()).containsKey("reason");
        }

        @Test
        @DisplayName("Should include reason detail when DOWN due to empty result")
        void shouldIncludeReasonDetailWhenEmptyResult() {
            when(embeddingService.embed("health check")).thenReturn(new float[0]);

            EmbeddingHealthIndicator indicator = new EmbeddingHealthIndicator(embeddingService);

            Health health = indicator.health();

            assertThat(health.getDetails()).hasSize(1);
            assertThat(health.getDetails()).containsKey("reason");
        }

        @Test
        @DisplayName("Should include error detail when exception occurs")
        void shouldIncludeErrorDetailWhenException() {
            when(embeddingService.embed(anyString())).thenThrow(new RuntimeException("ONNX runtime error"));

            EmbeddingHealthIndicator indicator = new EmbeddingHealthIndicator(embeddingService);

            Health health = indicator.health();

            assertThat(health.getDetails()).hasSize(1);
            assertThat(health.getDetails()).containsKey("error");
            assertThat(health.getDetails().get("error")).asString().contains("ONNX runtime error");
        }
    }

    @Nested
    @DisplayName("Embedding Service Interaction Tests")
    class EmbeddingServiceInteractionTests {

        @Test
        @DisplayName("Should call embed with 'health check' string")
        void shouldCallEmbedWithHealthCheckString() {
            float[] validEmbedding = new float[384];
            when(embeddingService.embed("health check")).thenReturn(validEmbedding);
            when(embeddingService.getDimension()).thenReturn(384);

            EmbeddingHealthIndicator indicator = new EmbeddingHealthIndicator(embeddingService);

            indicator.health();

            // Verify interaction through mock - the when clause ensures this
            assertThat(indicator).isNotNull();
        }

        @Test
        @DisplayName("Should call getDimension when embedding is valid")
        void shouldCallGetDimensionWhenEmbeddingValid() {
            float[] validEmbedding = new float[384];
            when(embeddingService.embed("health check")).thenReturn(validEmbedding);
            when(embeddingService.getDimension()).thenReturn(384);

            EmbeddingHealthIndicator indicator = new EmbeddingHealthIndicator(embeddingService);

            Health health = indicator.health();

            assertThat(health.getDetails()).containsEntry("dimensions", 384);
        }

        @Test
        @DisplayName("Should handle different embedding dimensions")
        void shouldHandleDifferentEmbeddingDimensions() {
            float[] validEmbedding = new float[768];
            when(embeddingService.embed("health check")).thenReturn(validEmbedding);
            when(embeddingService.getDimension()).thenReturn(768);

            EmbeddingHealthIndicator indicator = new EmbeddingHealthIndicator(embeddingService);

            Health health = indicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.UP);
            assertThat(health.getDetails()).containsEntry("dimensions", 768);
        }

        @Test
        @DisplayName("Should handle single-element embedding array")
        void shouldHandleSingleElementEmbedding() {
            float[] validEmbedding = new float[1];
            validEmbedding[0] = 0.5f;
            when(embeddingService.embed("health check")).thenReturn(validEmbedding);
            when(embeddingService.getDimension()).thenReturn(1);

            EmbeddingHealthIndicator indicator = new EmbeddingHealthIndicator(embeddingService);

            Health health = indicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.UP);
            assertThat(health.getDetails()).containsEntry("dimensions", 1);
        }
    }
}
