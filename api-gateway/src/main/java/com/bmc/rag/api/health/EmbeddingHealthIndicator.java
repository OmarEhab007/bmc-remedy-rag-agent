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
            if (embeddingService != null) {
                return Health.up()
                        .withDetail("model", "all-minilm-l6-v2")
                        .withDetail("dimensions", 384)
                        .build();
            }
            return Health.down()
                    .withDetail("reason", "LocalEmbeddingService bean is null")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
