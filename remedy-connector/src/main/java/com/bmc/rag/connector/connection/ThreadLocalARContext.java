package com.bmc.rag.connector.connection;

import com.bmc.arsys.api.ARException;
import com.bmc.arsys.api.ARServerUser;
import com.bmc.rag.connector.config.RemedyConnectionConfig;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Thread-safe management of BMC ARServerUser instances.
 * ARServerUser is NOT thread-safe, so each thread must have its own instance.
 * Uses ThreadLocal storage pattern for connection management.
 */
@Slf4j
@Component
public class ThreadLocalARContext {

    private final RemedyConnectionConfig config;
    private final ThreadLocal<ARServerUser> contextHolder = new ThreadLocal<>();

    public ThreadLocalARContext(RemedyConnectionConfig config) {
        this.config = config;
    }

    /**
     * Check if Remedy connection is enabled.
     */
    public boolean isEnabled() {
        return config.isEnabled();
    }

    /**
     * Get or create an ARServerUser for the current thread.
     * Creates a new connection if none exists for this thread.
     *
     * @return ARServerUser instance for the current thread
     * @throws ARConnectionException if connection fails or Remedy is disabled
     */
    public ARServerUser getContext() {
        if (!config.isEnabled()) {
            throw new ARConnectionException("Remedy connection is disabled. Set remedy.enabled=true to enable.");
        }
        ARServerUser ctx = contextHolder.get();
        if (ctx == null) {
            ctx = createConnection();
            contextHolder.set(ctx);
        }
        return ctx;
    }

    /**
     * Create a new ARServerUser connection with configured settings.
     *
     * @return New ARServerUser instance
     * @throws ARConnectionException if connection fails
     */
    private ARServerUser createConnection() {
        log.debug("Creating new ARServerUser connection for thread: {}", Thread.currentThread().getName());

        ARServerUser ctx = new ARServerUser();
        ctx.setServer(config.getServer());
        ctx.setPort(config.getPort());
        ctx.setUser(config.getUsername());
        ctx.setPassword(config.getPassword());

        // Set timeouts to handle RPC timeouts (ARERR 92, 93)
        // BMC AR API uses setTimeoutNormal/setTimeoutLong/setTimeoutXLong
        int socketTimeout = config.getSocketTimeout();
        ctx.setTimeoutNormal(socketTimeout);
        ctx.setTimeoutLong(socketTimeout * 2);
        ctx.setTimeoutXLong(socketTimeout * 4);

        // Set locale if specified
        if (config.getLocale() != null) {
            ctx.setLocale(config.getLocale());
        }

        // Set auth string for multi-server setups
        if (config.getAuthString() != null && !config.getAuthString().isEmpty()) {
            ctx.setAuthentication(config.getAuthString());
        }

        // Verify connection
        try {
            ctx.verifyUser();
            log.info("Successfully connected to Remedy server: {}:{}", config.getServer(), config.getPort());
        } catch (ARException e) {
            log.error("Failed to verify user connection to Remedy server: {}", e.getMessage());
            throw new ARConnectionException("Failed to connect to Remedy server", e);
        }

        return ctx;
    }

    /**
     * Verify the current thread's connection is still valid.
     *
     * @return true if connection is valid, false otherwise
     */
    public boolean verifyConnection() {
        ARServerUser ctx = contextHolder.get();
        if (ctx == null) {
            return false;
        }
        try {
            ctx.verifyUser();
            return true;
        } catch (ARException e) {
            log.warn("Connection verification failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Refresh the connection for the current thread.
     * Closes existing connection and creates a new one.
     *
     * @return New ARServerUser instance
     */
    public ARServerUser refreshConnection() {
        closeContext();
        return getContext();
    }

    /**
     * Close the connection for the current thread.
     * Should be called when the thread is done with Remedy operations.
     */
    public void closeContext() {
        ARServerUser ctx = contextHolder.get();
        if (ctx != null) {
            try {
                ctx.logout();
                log.debug("Closed ARServerUser connection for thread: {}", Thread.currentThread().getName());
            } catch (Exception e) {
                log.warn("Error closing ARServerUser connection: {}", e.getMessage());
            } finally {
                contextHolder.remove();
            }
        }
    }

    /**
     * Execute an operation with automatic retry on connection failure.
     *
     * @param operation The operation to execute
     * @param <T> Return type of the operation
     * @return Result of the operation
     * @throws ARConnectionException if all retries fail or Remedy is disabled
     */
    public <T> T executeWithRetry(AROperation<T> operation) {
        if (!config.isEnabled()) {
            throw new ARConnectionException("Remedy connection is disabled. Set remedy.enabled=true to enable.");
        }

        int attempts = 0;
        Exception lastException = null;

        while (attempts < config.getRetryAttempts()) {
            try {
                ARServerUser ctx = getContext();
                return operation.execute(ctx);
            } catch (ARException e) {
                lastException = e;
                attempts++;
                log.warn("Remedy operation failed (attempt {}/{}): {}",
                    attempts, config.getRetryAttempts(), e.getMessage());

                // Check if it's a connection error (ARERR 92, 93)
                if (isConnectionError(e)) {
                    log.info("Refreshing connection due to connection error");
                    closeContext();

                    if (attempts < config.getRetryAttempts()) {
                        try {
                            Thread.sleep(config.getRetryDelayMs());
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new ARConnectionException("Interrupted during retry delay", ie);
                        }
                    }
                } else {
                    // Non-connection error, don't retry
                    throw new ARConnectionException("Remedy operation failed", e);
                }
            }
        }

        throw new ARConnectionException("Remedy operation failed after " + attempts + " attempts", lastException);
    }

    /**
     * Check if an ARException is a connection-related error.
     */
    private boolean isConnectionError(ARException e) {
        // ARERR 92: Network-level RPC timeout
        // ARERR 93: Server-side query timeout
        // ARERR 90: Server not available
        int[] connectionErrors = {90, 92, 93, 9251, 9252};
        for (int errorCode : connectionErrors) {
            if (e.getMessage() != null && e.getMessage().contains("ARERR " + errorCode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Cleanup when the Spring context is destroyed.
     */
    @PreDestroy
    public void cleanup() {
        log.info("Cleaning up ThreadLocalARContext");
        closeContext();
    }

    /**
     * Functional interface for Remedy operations.
     */
    @FunctionalInterface
    public interface AROperation<T> {
        T execute(ARServerUser ctx) throws ARException;
    }

    /**
     * Custom exception for Remedy connection errors.
     */
    public static class ARConnectionException extends RuntimeException {
        public ARConnectionException(String message) {
            super(message);
        }

        public ARConnectionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
