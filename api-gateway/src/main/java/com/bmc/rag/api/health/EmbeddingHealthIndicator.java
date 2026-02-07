package com.bmc.rag.api.health;

import com.bmc.rag.vectorization.embedding.LocalEmbeddingService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator that reports the availability of the local
 * embedding model used for vectorization.
 * <p>
 * Exposes status under {@code /actuator/health/embedding}. When the
 * {@link LocalEmbeddingService} bean is present the indicator returns UP
 * with model metadata; otherwise it returns DOWN with diagnostic details.
 */
@Component
public class EmbeddingHealthIndicator implements HealthIndicator {

    private final LocalEmbeddingService embeddingService;

    public EmbeddingHealthIndicator(LocalEmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @Override
    public Health health() {
        try {
            float[] testEmbedding = embeddingService.embed("health check");
            if (testEmbedding != null && testEmbedding.length > 0) {
                return Health.up()
                        .withDetail("model", "all-minilm-l6-v2")
                        .withDetail("dimensions", embeddingService.getDimension())
                        .build();
            }
            return Health.down()
                    .withDetail("reason", "Embedding service returned empty result")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
