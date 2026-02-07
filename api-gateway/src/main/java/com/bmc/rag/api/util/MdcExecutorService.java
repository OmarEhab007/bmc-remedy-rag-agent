package com.bmc.rag.api.util;

import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * Utility for wrapping async tasks with MDC context propagation.
 * MDC is thread-local and does not automatically propagate to async threads.
 * These wrappers capture the MDC context from the submitting thread and restore
 * it in the executing thread.
 */
public final class MdcExecutorService {

    private MdcExecutorService() {}

    /**
     * Submit a Runnable to the executor with MDC context propagation.
     */
    public static void submit(ExecutorService executor, Runnable task) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        executor.submit(wrapWithMdc(task, contextMap));
    }

    /**
     * Wrap a Runnable with MDC context capture/restore.
     * Use with CompletableFuture.runAsync(wrapRunnable(task), executor).
     */
    public static Runnable wrapRunnable(Runnable task) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return wrapWithMdc(task, contextMap);
    }

    /**
     * Wrap a Supplier with MDC context capture/restore.
     * Use with CompletableFuture.supplyAsync(wrapSupplier(supplier), executor).
     */
    public static <T> Supplier<T> wrapSupplier(Supplier<T> supplier) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            } else {
                MDC.clear();
            }
            try {
                return supplier.get();
            } finally {
                if (previous != null) {
                    MDC.setContextMap(previous);
                } else {
                    MDC.clear();
                }
            }
        };
    }

    private static Runnable wrapWithMdc(Runnable task, Map<String, String> contextMap) {
        return () -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            } else {
                MDC.clear();
            }
            try {
                task.run();
            } finally {
                if (previous != null) {
                    MDC.setContextMap(previous);
                } else {
                    MDC.clear();
                }
            }
        };
    }
}
