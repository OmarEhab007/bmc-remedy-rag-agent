package com.bmc.rag.api.health;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator that reports the availability of the LLM chat model.
 * <p>
 * Exposes status under {@code /actuator/health/llm}. When the
 * {@link ChatLanguageModel} bean is present and reachable the indicator
 * returns UP; otherwise it returns DOWN with diagnostic details.
 */
@Component
public class LlmHealthIndicator implements HealthIndicator {

    private final ChatLanguageModel chatModel;

    public LlmHealthIndicator(ChatLanguageModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public Health health() {
        try {
            if (chatModel != null) {
                return Health.up()
                        .withDetail("provider", "configured")
                        .build();
            }
            return Health.down()
                    .withDetail("reason", "ChatLanguageModel bean is null")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
