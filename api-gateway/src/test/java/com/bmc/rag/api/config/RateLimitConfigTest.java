package com.bmc.rag.api.config;

import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for RateLimitConfig.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RateLimitConfig Tests")
class RateLimitConfigTest {

    private RateLimitConfig rateLimitConfig;

    @BeforeEach
    void setUp() {
        rateLimitConfig = new RateLimitConfig();

        // Set default values using reflection (as they are @Value injected in real usage)
        ReflectionTestUtils.setField(rateLimitConfig, "chatRequestsPerMinute", 50);
        ReflectionTestUtils.setField(rateLimitConfig, "searchRequestsPerMinute", 100);
        ReflectionTestUtils.setField(rateLimitConfig, "adminRequestsPerMinute", 20);
        ReflectionTestUtils.setField(rateLimitConfig, "feedbackRequestsPerMinute", 30);
        ReflectionTestUtils.setField(rateLimitConfig, "actionRequestsPerHour", 10);
    }

    @Nested
    @DisplayName("Configuration Values Tests")
    class ConfigurationValuesTests {

        @Test
        @DisplayName("getChatBucket_firstCall_createsBucketWithCorrectCapacity")
        void getChatBucket_firstCall_createsBucketWithCorrectCapacity() {
            String userId = "user1";

            Bucket bucket = rateLimitConfig.getChatBucket(userId);

            assertThat(bucket).isNotNull();
            assertThat(bucket.getAvailableTokens()).isEqualTo(50);
        }

        @Test
        @DisplayName("getSearchBucket_firstCall_createsBucketWithCorrectCapacity")
        void getSearchBucket_firstCall_createsBucketWithCorrectCapacity() {
            String userId = "user1";

            Bucket bucket = rateLimitConfig.getSearchBucket(userId);

            assertThat(bucket).isNotNull();
            assertThat(bucket.getAvailableTokens()).isEqualTo(100);
        }

        @Test
        @DisplayName("getAdminBucket_firstCall_createsBucketWithCorrectCapacity")
        void getAdminBucket_firstCall_createsBucketWithCorrectCapacity() {
            String userId = "admin1";

            Bucket bucket = rateLimitConfig.getAdminBucket(userId);

            assertThat(bucket).isNotNull();
            assertThat(bucket.getAvailableTokens()).isEqualTo(20);
        }

        @Test
        @DisplayName("getFeedbackBucket_firstCall_createsBucketWithCorrectCapacity")
        void getFeedbackBucket_firstCall_createsBucketWithCorrectCapacity() {
            String userId = "user1";

            Bucket bucket = rateLimitConfig.getFeedbackBucket(userId);

            assertThat(bucket).isNotNull();
            assertThat(bucket.getAvailableTokens()).isEqualTo(30);
        }

        @Test
        @DisplayName("getActionBucket_firstCall_createsBucketWithCorrectCapacity")
        void getActionBucket_firstCall_createsBucketWithCorrectCapacity() {
            String userId = "user1";

            Bucket bucket = rateLimitConfig.getActionBucket(userId);

            assertThat(bucket).isNotNull();
            assertThat(bucket.getAvailableTokens()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("Bucket Caching Tests")
    class BucketCachingTests {

        @Test
        @DisplayName("getChatBucket_sameUser_returnsSameBucket")
        void getChatBucket_sameUser_returnsSameBucket() {
            String userId = "user1";

            Bucket bucket1 = rateLimitConfig.getChatBucket(userId);
            Bucket bucket2 = rateLimitConfig.getChatBucket(userId);

            assertThat(bucket1).isSameAs(bucket2);
        }

        @Test
        @DisplayName("getChatBucket_differentUsers_returnsDifferentBuckets")
        void getChatBucket_differentUsers_returnsDifferentBuckets() {
            String userId1 = "user1";
            String userId2 = "user2";

            Bucket bucket1 = rateLimitConfig.getChatBucket(userId1);
            Bucket bucket2 = rateLimitConfig.getChatBucket(userId2);

            assertThat(bucket1).isNotSameAs(bucket2);
        }
    }

    @Nested
    @DisplayName("Rate Limiting Logic Tests")
    class RateLimitingLogicTests {

        @Test
        @DisplayName("isRateLimitedChat_withinLimit_returnsFalse")
        void isRateLimitedChat_withinLimit_returnsFalse() {
            String userId = "user1";

            boolean isLimited = rateLimitConfig.isRateLimitedChat(userId);

            assertThat(isLimited).isFalse();
        }

        @Test
        @DisplayName("isRateLimitedChat_exceedsLimit_returnsTrue")
        void isRateLimitedChat_exceedsLimit_returnsTrue() {
            String userId = "user1";

            // Consume all 50 tokens
            for (int i = 0; i < 50; i++) {
                rateLimitConfig.isRateLimitedChat(userId);
            }

            // Next call should be rate limited
            boolean isLimited = rateLimitConfig.isRateLimitedChat(userId);

            assertThat(isLimited).isTrue();
        }

        @Test
        @DisplayName("isRateLimitedSearch_withinLimit_returnsFalse")
        void isRateLimitedSearch_withinLimit_returnsFalse() {
            String userId = "user1";

            boolean isLimited = rateLimitConfig.isRateLimitedSearch(userId);

            assertThat(isLimited).isFalse();
        }

        @Test
        @DisplayName("isRateLimitedSearch_exceedsLimit_returnsTrue")
        void isRateLimitedSearch_exceedsLimit_returnsTrue() {
            String userId = "user1";

            // Consume all 100 tokens
            for (int i = 0; i < 100; i++) {
                rateLimitConfig.isRateLimitedSearch(userId);
            }

            // Next call should be rate limited
            boolean isLimited = rateLimitConfig.isRateLimitedSearch(userId);

            assertThat(isLimited).isTrue();
        }

        @Test
        @DisplayName("isRateLimitedAdmin_withinLimit_returnsFalse")
        void isRateLimitedAdmin_withinLimit_returnsFalse() {
            String userId = "admin1";

            boolean isLimited = rateLimitConfig.isRateLimitedAdmin(userId);

            assertThat(isLimited).isFalse();
        }

        @Test
        @DisplayName("isRateLimitedAdmin_exceedsLimit_returnsTrue")
        void isRateLimitedAdmin_exceedsLimit_returnsTrue() {
            String userId = "admin1";

            // Consume all 20 tokens
            for (int i = 0; i < 20; i++) {
                rateLimitConfig.isRateLimitedAdmin(userId);
            }

            // Next call should be rate limited
            boolean isLimited = rateLimitConfig.isRateLimitedAdmin(userId);

            assertThat(isLimited).isTrue();
        }

        @Test
        @DisplayName("isRateLimitedFeedback_withinLimit_returnsFalse")
        void isRateLimitedFeedback_withinLimit_returnsFalse() {
            String userId = "user1";

            boolean isLimited = rateLimitConfig.isRateLimitedFeedback(userId);

            assertThat(isLimited).isFalse();
        }

        @Test
        @DisplayName("isRateLimitedFeedback_exceedsLimit_returnsTrue")
        void isRateLimitedFeedback_exceedsLimit_returnsTrue() {
            String userId = "user1";

            // Consume all 30 tokens
            for (int i = 0; i < 30; i++) {
                rateLimitConfig.isRateLimitedFeedback(userId);
            }

            // Next call should be rate limited
            boolean isLimited = rateLimitConfig.isRateLimitedFeedback(userId);

            assertThat(isLimited).isTrue();
        }

        @Test
        @DisplayName("isRateLimitedAction_withinLimit_returnsFalse")
        void isRateLimitedAction_withinLimit_returnsFalse() {
            String userId = "user1";

            boolean isLimited = rateLimitConfig.isRateLimitedAction(userId);

            assertThat(isLimited).isFalse();
        }

        @Test
        @DisplayName("isRateLimitedAction_exceedsLimit_returnsTrue")
        void isRateLimitedAction_exceedsLimit_returnsTrue() {
            String userId = "user1";

            // Consume all 10 tokens
            for (int i = 0; i < 10; i++) {
                rateLimitConfig.isRateLimitedAction(userId);
            }

            // Next call should be rate limited
            boolean isLimited = rateLimitConfig.isRateLimitedAction(userId);

            assertThat(isLimited).isTrue();
        }
    }

    @Nested
    @DisplayName("User Isolation Tests")
    class UserIsolationTests {

        @Test
        @DisplayName("isRateLimitedChat_user1Limited_user2NotAffected")
        void isRateLimitedChat_user1Limited_user2NotAffected() {
            String user1 = "user1";
            String user2 = "user2";

            // Consume all tokens for user1
            for (int i = 0; i < 50; i++) {
                rateLimitConfig.isRateLimitedChat(user1);
            }

            // User1 should be rate limited
            assertThat(rateLimitConfig.isRateLimitedChat(user1)).isTrue();

            // User2 should not be affected
            assertThat(rateLimitConfig.isRateLimitedChat(user2)).isFalse();
        }
    }

    @Nested
    @DisplayName("Cleanup Tests")
    class CleanupTests {

        @Test
        @DisplayName("cleanupOldBuckets_belowMaxBuckets_doesNotClear")
        void cleanupOldBuckets_belowMaxBuckets_doesNotClear() {
            // Create a few buckets
            for (int i = 0; i < 10; i++) {
                rateLimitConfig.getChatBucket("user" + i);
            }

            Map<String, Integer> statsBefore = rateLimitConfig.getStatistics();

            rateLimitConfig.cleanupOldBuckets();

            Map<String, Integer> statsAfter = rateLimitConfig.getStatistics();

            assertThat(statsAfter.get("chatBuckets")).isEqualTo(statsBefore.get("chatBuckets"));
        }

        @Test
        @DisplayName("cleanupOldBuckets_exceedsMaxBuckets_clearsBuckets")
        void cleanupOldBuckets_exceedsMaxBuckets_clearsBuckets() {
            // Create more than MAX_BUCKETS (10000)
            for (int i = 0; i < 10001; i++) {
                rateLimitConfig.getChatBucket("user" + i);
            }

            rateLimitConfig.cleanupOldBuckets();

            Map<String, Integer> stats = rateLimitConfig.getStatistics();

            assertThat(stats.get("chatBuckets")).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Statistics Tests")
    class StatisticsTests {

        @Test
        @DisplayName("getStatistics_noBuckets_returnsZeros")
        void getStatistics_noBuckets_returnsZeros() {
            Map<String, Integer> stats = rateLimitConfig.getStatistics();

            assertThat(stats).containsEntry("chatBuckets", 0);
            assertThat(stats).containsEntry("searchBuckets", 0);
            assertThat(stats).containsEntry("adminBuckets", 0);
            assertThat(stats).containsEntry("feedbackBuckets", 0);
            assertThat(stats).containsEntry("actionBuckets", 0);
        }

        @Test
        @DisplayName("getStatistics_withBuckets_returnsCorrectCounts")
        void getStatistics_withBuckets_returnsCorrectCounts() {
            // Create buckets of different types
            rateLimitConfig.getChatBucket("user1");
            rateLimitConfig.getChatBucket("user2");
            rateLimitConfig.getSearchBucket("user1");
            rateLimitConfig.getAdminBucket("admin1");
            rateLimitConfig.getFeedbackBucket("user1");
            rateLimitConfig.getActionBucket("user1");

            Map<String, Integer> stats = rateLimitConfig.getStatistics();

            assertThat(stats).containsEntry("chatBuckets", 2);
            assertThat(stats).containsEntry("searchBuckets", 1);
            assertThat(stats).containsEntry("adminBuckets", 1);
            assertThat(stats).containsEntry("feedbackBuckets", 1);
            assertThat(stats).containsEntry("actionBuckets", 1);
        }
    }

    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("getChatBucket_concurrentAccess_threadsafe")
        void getChatBucket_concurrentAccess_threadsafe() throws InterruptedException {
            int threadCount = 10;
            CountDownLatch latch = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            for (int i = 0; i < threadCount; i++) {
                final int userId = i;
                executor.submit(() -> {
                    try {
                        rateLimitConfig.getChatBucket("user" + userId);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean completed = latch.await(5, TimeUnit.SECONDS);
            executor.shutdown();

            assertThat(completed).isTrue();
            assertThat(rateLimitConfig.getStatistics().get("chatBuckets")).isEqualTo(threadCount);
        }
    }
}
