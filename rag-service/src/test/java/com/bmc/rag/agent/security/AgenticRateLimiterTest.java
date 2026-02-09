package com.bmc.rag.agent.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticRateLimiterTest {

    private AgenticRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new AgenticRateLimiter(3); // Low limit for testing
    }

    @Nested
    @DisplayName("isRateLimited")
    class IsRateLimited {

        @Test
        void isRateLimited_noActions_returnsFalse() {
            assertThat(rateLimiter.isRateLimited("user-1")).isFalse();
        }

        @Test
        void isRateLimited_underLimit_returnsFalse() {
            rateLimiter.recordAction("user-1");
            rateLimiter.recordAction("user-1");
            assertThat(rateLimiter.isRateLimited("user-1")).isFalse();
        }

        @Test
        void isRateLimited_atLimit_returnsTrue() {
            rateLimiter.recordAction("user-1");
            rateLimiter.recordAction("user-1");
            rateLimiter.recordAction("user-1");
            assertThat(rateLimiter.isRateLimited("user-1")).isTrue();
        }

        @Test
        void isRateLimited_overLimit_returnsTrue() {
            for (int i = 0; i < 5; i++) {
                rateLimiter.recordAction("user-1");
            }
            assertThat(rateLimiter.isRateLimited("user-1")).isTrue();
        }

        @Test
        void isRateLimited_nullUserId_returnsTrue() {
            assertThat(rateLimiter.isRateLimited(null)).isTrue();
        }

        @Test
        void isRateLimited_blankUserId_returnsTrue() {
            assertThat(rateLimiter.isRateLimited("  ")).isTrue();
        }

        @Test
        void isRateLimited_emptyUserId_returnsTrue() {
            assertThat(rateLimiter.isRateLimited("")).isTrue();
        }

        @Test
        void isRateLimited_differentUsers_trackedSeparately() {
            rateLimiter.recordAction("user-1");
            rateLimiter.recordAction("user-1");
            rateLimiter.recordAction("user-1");

            assertThat(rateLimiter.isRateLimited("user-1")).isTrue();
            assertThat(rateLimiter.isRateLimited("user-2")).isFalse();
        }
    }

    @Nested
    @DisplayName("recordAction")
    class RecordAction {

        @Test
        void recordAction_incrementsCounter() {
            rateLimiter.recordAction("user-1");
            assertThat(rateLimiter.getRemainingTokens("user-1")).isEqualTo(2);
        }

        @Test
        void recordAction_nullUserId_doesNotThrow() {
            rateLimiter.recordAction(null); // Should not throw
            assertThat(rateLimiter.getRemainingTokens(null)).isEqualTo(0);
        }

        @Test
        void recordAction_blankUserId_doesNotThrow() {
            rateLimiter.recordAction("  "); // Should not throw
        }
    }

    @Nested
    @DisplayName("getRemainingTokens")
    class GetRemainingTokens {

        @Test
        void getRemainingTokens_noActions_returnsMax() {
            assertThat(rateLimiter.getRemainingTokens("user-1")).isEqualTo(3);
        }

        @Test
        void getRemainingTokens_afterActions_returnsCorrectCount() {
            rateLimiter.recordAction("user-1");
            assertThat(rateLimiter.getRemainingTokens("user-1")).isEqualTo(2);

            rateLimiter.recordAction("user-1");
            assertThat(rateLimiter.getRemainingTokens("user-1")).isEqualTo(1);
        }

        @Test
        void getRemainingTokens_atLimit_returnsZero() {
            for (int i = 0; i < 3; i++) {
                rateLimiter.recordAction("user-1");
            }
            assertThat(rateLimiter.getRemainingTokens("user-1")).isEqualTo(0);
        }

        @Test
        void getRemainingTokens_overLimit_returnsZero() {
            for (int i = 0; i < 5; i++) {
                rateLimiter.recordAction("user-1");
            }
            assertThat(rateLimiter.getRemainingTokens("user-1")).isEqualTo(0);
        }

        @Test
        void getRemainingTokens_nullUserId_returnsZero() {
            assertThat(rateLimiter.getRemainingTokens(null)).isEqualTo(0);
        }

        @Test
        void getRemainingTokens_blankUserId_returnsZero() {
            assertThat(rateLimiter.getRemainingTokens("")).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("getStatus")
    class GetStatus {

        @Test
        void getStatus_newUser_showsFullTokens() {
            var status = rateLimiter.getStatus("user-1");
            assertThat(status.maxPerHour()).isEqualTo(3);
            assertThat(status.remaining()).isEqualTo(3);
            assertThat(status.isLimited()).isFalse();
        }

        @Test
        void getStatus_atLimit_showsLimited() {
            for (int i = 0; i < 3; i++) {
                rateLimiter.recordAction("user-1");
            }
            var status = rateLimiter.getStatus("user-1");
            assertThat(status.remaining()).isEqualTo(0);
            assertThat(status.isLimited()).isTrue();
        }
    }

    @Nested
    @DisplayName("clearRateLimitForUser")
    class ClearRateLimitForUser {

        @Test
        void clearRateLimitForUser_resetsCounter() {
            rateLimiter.recordAction("user-1");
            rateLimiter.recordAction("user-1");
            rateLimiter.recordAction("user-1");
            assertThat(rateLimiter.isRateLimited("user-1")).isTrue();

            rateLimiter.clearRateLimitForUser("user-1");
            assertThat(rateLimiter.isRateLimited("user-1")).isFalse();
            assertThat(rateLimiter.getRemainingTokens("user-1")).isEqualTo(3);
        }

        @Test
        void clearRateLimitForUser_doesNotAffectOtherUsers() {
            rateLimiter.recordAction("user-1");
            rateLimiter.recordAction("user-2");

            rateLimiter.clearRateLimitForUser("user-1");
            assertThat(rateLimiter.getRemainingTokens("user-1")).isEqualTo(3);
            assertThat(rateLimiter.getRemainingTokens("user-2")).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("getStats")
    class GetStats {

        @Test
        void getStats_noUsers_showsEmpty() {
            var stats = rateLimiter.getStats();
            assertThat(stats.maxCreationsPerHour()).isEqualTo(3);
            assertThat(stats.cachedCounters()).isEqualTo(0);
        }

        @Test
        void getStats_withUsers_showsCount() {
            rateLimiter.recordAction("user-1");
            rateLimiter.recordAction("user-2");
            var stats = rateLimiter.getStats();
            assertThat(stats.cachedCounters()).isEqualTo(2);
        }
    }
}
