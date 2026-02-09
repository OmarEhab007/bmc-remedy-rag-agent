package com.bmc.rag.agent.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AgenticConfig}.
 */
class AgenticConfigTest {

    private AgenticConfig config;

    @BeforeEach
    void setUp() {
        config = new AgenticConfig();
    }

    @Nested
    @DisplayName("Default Values")
    class DefaultValues {

        @Test
        void enabled_defaultsFalse() {
            assertThat(config.isEnabled()).isFalse();
        }

        @Test
        void confirmation_defaultsNotNull() {
            assertThat(config.getConfirmation()).isNotNull();
        }

        @Test
        void rateLimit_defaultsNotNull() {
            assertThat(config.getRateLimit()).isNotNull();
        }

        @Test
        void duplicateDetection_defaultsNotNull() {
            assertThat(config.getDuplicateDetection()).isNotNull();
        }

        @Test
        void audit_defaultsNotNull() {
            assertThat(config.getAudit()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Confirmation Defaults")
    class ConfirmationDefaults {

        @Test
        void timeoutMinutes_defaults5() {
            assertThat(config.getConfirmation().getTimeoutMinutes()).isEqualTo(5);
        }

        @Test
        void requireConfirmation_defaultsTrue() {
            assertThat(config.getConfirmation().isRequireConfirmation()).isTrue();
        }

        @Test
        void maxPendingPerSession_defaults5() {
            assertThat(config.getConfirmation().getMaxPendingPerSession()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("RateLimit Defaults")
    class RateLimitDefaults {

        @Test
        void maxCreationsPerHour_defaults10() {
            assertThat(config.getRateLimit().getMaxCreationsPerHour()).isEqualTo(10);
        }

        @Test
        void enabled_defaultsTrue() {
            assertThat(config.getRateLimit().isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("DuplicateDetection Defaults")
    class DuplicateDetectionDefaults {

        @Test
        void similarityThreshold_defaults085() {
            assertThat(config.getDuplicateDetection().getSimilarityThreshold()).isEqualTo(0.85);
        }

        @Test
        void blockOnDuplicate_defaultsFalse() {
            assertThat(config.getDuplicateDetection().isBlockOnDuplicate()).isFalse();
        }

        @Test
        void maxDuplicatesToCheck_defaults5() {
            assertThat(config.getDuplicateDetection().getMaxDuplicatesToCheck()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("Audit Defaults")
    class AuditDefaults {

        @Test
        void enabled_defaultsTrue() {
            assertThat(config.getAudit().isEnabled()).isTrue();
        }

        @Test
        void includePayloads_defaultsFalse() {
            assertThat(config.getAudit().isIncludePayloads()).isFalse();
        }

        @Test
        void retentionDays_defaults90() {
            assertThat(config.getAudit().getRetentionDays()).isEqualTo(90);
        }
    }

    @Nested
    @DisplayName("isOperational")
    class IsOperational {

        @Test
        void disabled_returnsFalse() {
            config.setEnabled(false);
            assertThat(config.isOperational()).isFalse();
        }

        @Test
        void enabledWithConfirmation_returnsTrue() {
            config.setEnabled(true);
            config.getConfirmation().setRequireConfirmation(true);
            assertThat(config.isOperational()).isTrue();
        }

        @Test
        void enabledWithoutConfirmation_returnsFalse() {
            config.setEnabled(true);
            config.getConfirmation().setRequireConfirmation(false);
            assertThat(config.isOperational()).isFalse();
        }
    }

    @Nested
    @DisplayName("Setter/Getter")
    class SetterGetter {

        @Test
        void setEnabled_updatesValue() {
            config.setEnabled(true);
            assertThat(config.isEnabled()).isTrue();
        }

        @Test
        void confirmationTimeout_canBeModified() {
            config.getConfirmation().setTimeoutMinutes(10);
            assertThat(config.getConfirmation().getTimeoutMinutes()).isEqualTo(10);
        }

        @Test
        void rateLimitMax_canBeModified() {
            config.getRateLimit().setMaxCreationsPerHour(20);
            assertThat(config.getRateLimit().getMaxCreationsPerHour()).isEqualTo(20);
        }

        @Test
        void duplicateThreshold_canBeModified() {
            config.getDuplicateDetection().setSimilarityThreshold(0.90);
            assertThat(config.getDuplicateDetection().getSimilarityThreshold()).isEqualTo(0.90);
        }

        @Test
        void auditRetention_canBeModified() {
            config.getAudit().setRetentionDays(180);
            assertThat(config.getAudit().getRetentionDays()).isEqualTo(180);
        }
    }
}
