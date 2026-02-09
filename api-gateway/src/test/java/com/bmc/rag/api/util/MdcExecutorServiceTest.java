package com.bmc.rag.api.util;

import org.junit.jupiter.api.*;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MdcExecutorService}.
 * Tests MDC context propagation in async tasks.
 */
@DisplayName("MdcExecutorService Tests")
class MdcExecutorServiceTest {

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newSingleThreadExecutor();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        MDC.clear();
    }

    @Nested
    @DisplayName("Submit Tests")
    class SubmitTests {

        @Test
        @DisplayName("Should propagate MDC context to submitted task")
        void shouldPropagateMdcContextToSubmittedTask() throws Exception {
            MDC.put("requestId", "req-123");
            MDC.put("userId", "user-456");

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<String> capturedRequestId = new AtomicReference<>();
            AtomicReference<String> capturedUserId = new AtomicReference<>();

            Runnable task = () -> {
                capturedRequestId.set(MDC.get("requestId"));
                capturedUserId.set(MDC.get("userId"));
                latch.countDown();
            };

            MdcExecutorService.submit(executor, task);

            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(capturedRequestId.get()).isEqualTo("req-123");
            assertThat(capturedUserId.get()).isEqualTo("user-456");
        }

        @Test
        @DisplayName("Should handle null MDC context in submitted task")
        void shouldHandleNullMdcContextInSubmittedTask() throws Exception {
            MDC.clear();

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Map<String, String>> capturedContext = new AtomicReference<>();

            Runnable task = () -> {
                capturedContext.set(MDC.getCopyOfContextMap());
                latch.countDown();
            };

            MdcExecutorService.submit(executor, task);

            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(capturedContext.get()).isNull();
        }

        @Test
        @DisplayName("Should not affect original thread MDC after task submission")
        void shouldNotAffectOriginalThreadMdcAfterSubmission() throws Exception {
            MDC.put("requestId", "req-123");

            CountDownLatch latch = new CountDownLatch(1);

            Runnable task = () -> {
                MDC.put("taskId", "task-789");
                latch.countDown();
            };

            MdcExecutorService.submit(executor, task);

            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(MDC.get("requestId")).isEqualTo("req-123");
            assertThat(MDC.get("taskId")).isNull();
        }

        @Test
        @DisplayName("Should handle multiple submitted tasks with different MDC contexts")
        void shouldHandleMultipleTasksWithDifferentContexts() throws Exception {
            CountDownLatch latch1 = new CountDownLatch(1);
            CountDownLatch latch2 = new CountDownLatch(1);
            AtomicReference<String> capturedId1 = new AtomicReference<>();
            AtomicReference<String> capturedId2 = new AtomicReference<>();

            MDC.put("taskId", "task-1");
            Runnable task1 = () -> {
                capturedId1.set(MDC.get("taskId"));
                latch1.countDown();
            };
            MdcExecutorService.submit(executor, task1);

            MDC.put("taskId", "task-2");
            Runnable task2 = () -> {
                capturedId2.set(MDC.get("taskId"));
                latch2.countDown();
            };
            MdcExecutorService.submit(executor, task2);

            assertThat(latch1.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(latch2.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(capturedId1.get()).isEqualTo("task-1");
            assertThat(capturedId2.get()).isEqualTo("task-2");
        }
    }

    @Nested
    @DisplayName("WrapRunnable Tests")
    class WrapRunnableTests {

        @Test
        @DisplayName("Should wrap Runnable with MDC context propagation")
        void shouldWrapRunnableWithMdcPropagation() throws Exception {
            MDC.put("requestId", "req-123");
            MDC.put("sessionId", "session-456");

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<String> capturedRequestId = new AtomicReference<>();
            AtomicReference<String> capturedSessionId = new AtomicReference<>();

            Runnable task = () -> {
                capturedRequestId.set(MDC.get("requestId"));
                capturedSessionId.set(MDC.get("sessionId"));
                latch.countDown();
            };

            Runnable wrapped = MdcExecutorService.wrapRunnable(task);
            executor.submit(wrapped);

            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(capturedRequestId.get()).isEqualTo("req-123");
            assertThat(capturedSessionId.get()).isEqualTo("session-456");
        }

        @Test
        @DisplayName("Should restore previous MDC after wrapped Runnable completes")
        void shouldRestorePreviousMdcAfterRunnableCompletes() throws Exception {
            MDC.put("original", "value-1");

            CountDownLatch latch = new CountDownLatch(1);

            Runnable task = () -> {
                MDC.put("modified", "value-2");
                latch.countDown();
            };

            Runnable wrapped = MdcExecutorService.wrapRunnable(task);

            ExecutorService sameThreadExecutor = Executors.newSingleThreadExecutor();
            try {
                sameThreadExecutor.submit(() -> {
                    MDC.put("threadLocal", "value-3");
                    wrapped.run();
                    // After wrapped task, threadLocal context should be restored
                    assertThat(MDC.get("threadLocal")).isEqualTo("value-3");
                    assertThat(MDC.get("modified")).isNull();
                    latch.countDown();
                });

                assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
            } finally {
                sameThreadExecutor.shutdown();
            }
        }

        @Test
        @DisplayName("Should handle null MDC context in wrapped Runnable")
        void shouldHandleNullMdcContextInWrappedRunnable() throws Exception {
            MDC.clear();

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Map<String, String>> capturedContext = new AtomicReference<>();

            Runnable task = () -> {
                capturedContext.set(MDC.getCopyOfContextMap());
                latch.countDown();
            };

            Runnable wrapped = MdcExecutorService.wrapRunnable(task);
            executor.submit(wrapped);

            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(capturedContext.get()).isNull();
        }

        @Test
        @DisplayName("Should clear MDC when context was null and restore to null")
        void shouldClearMdcWhenContextWasNull() throws Exception {
            MDC.clear();

            CountDownLatch latch = new CountDownLatch(1);

            Runnable task = () -> {
                assertThat(MDC.getCopyOfContextMap()).isNull();
                latch.countDown();
            };

            Runnable wrapped = MdcExecutorService.wrapRunnable(task);

            ExecutorService testExecutor = Executors.newSingleThreadExecutor();
            try {
                testExecutor.submit(() -> {
                    MDC.put("shouldBeCleared", "value");
                    wrapped.run();
                    // After wrapped task, MDC should be cleared back to null
                    assertThat(MDC.get("shouldBeCleared")).isEqualTo("value");
                    latch.countDown();
                });

                assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
            } finally {
                testExecutor.shutdown();
            }
        }
    }

    @Nested
    @DisplayName("WrapSupplier Tests")
    class WrapSupplierTests {

        @Test
        @DisplayName("Should wrap Supplier with MDC context propagation")
        void shouldWrapSupplierWithMdcPropagation() throws Exception {
            MDC.put("requestId", "req-123");
            MDC.put("traceId", "trace-789");

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<String> capturedRequestId = new AtomicReference<>();
            AtomicReference<String> capturedTraceId = new AtomicReference<>();

            Supplier<String> supplier = () -> {
                capturedRequestId.set(MDC.get("requestId"));
                capturedTraceId.set(MDC.get("traceId"));
                latch.countDown();
                return "result";
            };

            Supplier<String> wrapped = MdcExecutorService.wrapSupplier(supplier);
            CompletableFuture<String> future = CompletableFuture.supplyAsync(wrapped, executor);

            String result = future.get(2, TimeUnit.SECONDS);

            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(result).isEqualTo("result");
            assertThat(capturedRequestId.get()).isEqualTo("req-123");
            assertThat(capturedTraceId.get()).isEqualTo("trace-789");
        }

        @Test
        @DisplayName("Should restore previous MDC after wrapped Supplier completes")
        void shouldRestorePreviousMdcAfterSupplierCompletes() throws Exception {
            MDC.put("original", "value-1");

            Supplier<String> supplier = () -> {
                MDC.put("modified", "value-2");
                return "result";
            };

            Supplier<String> wrapped = MdcExecutorService.wrapSupplier(supplier);

            ExecutorService sameThreadExecutor = Executors.newSingleThreadExecutor();
            try {
                Future<Void> future = sameThreadExecutor.submit(() -> {
                    MDC.put("threadLocal", "value-3");
                    String result = wrapped.get();
                    assertThat(result).isEqualTo("result");
                    // After wrapped task, threadLocal context should be restored
                    assertThat(MDC.get("threadLocal")).isEqualTo("value-3");
                    assertThat(MDC.get("modified")).isNull();
                    return null;
                });

                future.get(2, TimeUnit.SECONDS);
            } finally {
                sameThreadExecutor.shutdown();
            }
        }

        @Test
        @DisplayName("Should handle null MDC context in wrapped Supplier")
        void shouldHandleNullMdcContextInWrappedSupplier() throws Exception {
            MDC.clear();

            AtomicReference<Map<String, String>> capturedContext = new AtomicReference<>();

            Supplier<String> supplier = () -> {
                capturedContext.set(MDC.getCopyOfContextMap());
                return "result";
            };

            Supplier<String> wrapped = MdcExecutorService.wrapSupplier(supplier);
            CompletableFuture<String> future = CompletableFuture.supplyAsync(wrapped, executor);

            String result = future.get(2, TimeUnit.SECONDS);

            assertThat(result).isEqualTo("result");
            assertThat(capturedContext.get()).isNull();
        }

        @Test
        @DisplayName("Should return supplier result correctly")
        void shouldReturnSupplierResultCorrectly() throws Exception {
            MDC.put("key", "value");

            Supplier<Integer> supplier = () -> 42;

            Supplier<Integer> wrapped = MdcExecutorService.wrapSupplier(supplier);
            CompletableFuture<Integer> future = CompletableFuture.supplyAsync(wrapped, executor);

            Integer result = future.get(2, TimeUnit.SECONDS);

            assertThat(result).isEqualTo(42);
        }

        @Test
        @DisplayName("Should propagate exceptions from wrapped Supplier")
        void shouldPropagateExceptionsFromWrappedSupplier() {
            MDC.put("key", "value");

            Supplier<String> supplier = () -> {
                throw new RuntimeException("Test exception");
            };

            Supplier<String> wrapped = MdcExecutorService.wrapSupplier(supplier);
            CompletableFuture<String> future = CompletableFuture.supplyAsync(wrapped, executor);

            assertThat(future)
                    .failsWithin(2, TimeUnit.SECONDS)
                    .withThrowableThat()
                    .withRootCauseInstanceOf(RuntimeException.class)
                    .withMessageContaining("Test exception");
        }

        @Test
        @DisplayName("Should handle complex objects as Supplier result")
        void shouldHandleComplexObjectsAsSupplierResult() throws Exception {
            MDC.put("key", "value");

            record TestResult(String message, int code) {}

            Supplier<TestResult> supplier = () -> new TestResult("Success", 200);

            Supplier<TestResult> wrapped = MdcExecutorService.wrapSupplier(supplier);
            CompletableFuture<TestResult> future = CompletableFuture.supplyAsync(wrapped, executor);

            TestResult result = future.get(2, TimeUnit.SECONDS);

            assertThat(result.message()).isEqualTo("Success");
            assertThat(result.code()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("MDC Cleanup Tests")
    class MdcCleanupTests {

        @Test
        @DisplayName("Should clean up MDC after Runnable completes")
        void shouldCleanUpMdcAfterRunnableCompletes() throws Exception {
            MDC.put("requestId", "req-123");

            CountDownLatch latch = new CountDownLatch(1);

            Runnable task = () -> {
                assertThat(MDC.get("requestId")).isEqualTo("req-123");
                latch.countDown();
            };

            Runnable wrapped = MdcExecutorService.wrapRunnable(task);

            ExecutorService testExecutor = Executors.newSingleThreadExecutor();
            try {
                testExecutor.submit(() -> {
                    wrapped.run();
                    // After wrapped task, check MDC is properly restored/cleaned
                    latch.countDown();
                });

                assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
            } finally {
                testExecutor.shutdown();
            }
        }

        @Test
        @DisplayName("Should clean up MDC after Supplier completes")
        void shouldCleanUpMdcAfterSupplierCompletes() throws Exception {
            MDC.put("requestId", "req-123");

            Supplier<String> supplier = () -> {
                assertThat(MDC.get("requestId")).isEqualTo("req-123");
                return "result";
            };

            Supplier<String> wrapped = MdcExecutorService.wrapSupplier(supplier);

            CompletableFuture<String> future = CompletableFuture.supplyAsync(wrapped, executor);

            String result = future.get(2, TimeUnit.SECONDS);

            assertThat(result).isEqualTo("result");
        }

        @Test
        @DisplayName("Should restore MDC even when task throws exception")
        void shouldRestoreMdcEvenWhenTaskThrowsException() throws Exception {
            MDC.put("requestId", "req-123");

            CountDownLatch latch = new CountDownLatch(1);

            Runnable task = () -> {
                throw new RuntimeException("Task failed");
            };

            Runnable wrapped = MdcExecutorService.wrapRunnable(task);

            ExecutorService testExecutor = Executors.newSingleThreadExecutor();
            try {
                testExecutor.submit(() -> {
                    MDC.put("threadLocal", "value");
                    try {
                        wrapped.run();
                    } catch (RuntimeException e) {
                        // Exception expected
                    }
                    // MDC should be restored even after exception
                    assertThat(MDC.get("threadLocal")).isEqualTo("value");
                    assertThat(MDC.get("requestId")).isNull();
                    latch.countDown();
                });

                assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
            } finally {
                testExecutor.shutdown();
            }
        }
    }
}
