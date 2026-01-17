package com.bmc.rag.connector.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.validation.constraints.Positive;

/**
 * Configuration properties for BMC Remedy AR System connection.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "remedy")
public class RemedyConnectionConfig {

    /**
     * Enable Remedy connection (default: true).
     * Set to false for testing without a Remedy server.
     */
    private boolean enabled = true;

    /**
     * Remedy AR Server hostname or IP address.
     */
    private String server = "localhost";

    /**
     * Remedy AR Server port (default: 7100 for RPC).
     */
    @Positive
    private int port = 7100;

    /**
     * Username for Remedy authentication.
     * Should be a "Fixed" license account for background processes.
     */
    private String username = "Demo";

    /**
     * Password for Remedy authentication.
     */
    private String password = "";

    /**
     * Socket timeout in milliseconds for RPC calls (default: 60 seconds).
     * Increase for slow networks or complex queries.
     */
    @Positive
    private int socketTimeout = 60000;

    /**
     * Chunk size for paginated queries (default: 500).
     * Reduce to 100 if experiencing ARERR 93 (timeout) errors.
     */
    @Positive
    private int chunkSize = 500;

    /**
     * Maximum records to retrieve in a single query (default: 2000).
     * Server typically limits to 2000-5000.
     */
    @Positive
    private int maxRetrieve = 2000;

    /**
     * Authentication string (optional, for multi-server setups).
     */
    private String authString;

    /**
     * Locale for Remedy API (default: en_US).
     */
    private String locale = "en_US";

    /**
     * Enable SSL/TLS for connection (default: false).
     */
    private boolean sslEnabled = false;

    /**
     * Number of retry attempts for failed connections (default: 3).
     */
    @Positive
    private int retryAttempts = 3;

    /**
     * Delay between retry attempts in milliseconds (default: 5000).
     */
    @Positive
    private long retryDelayMs = 5000;

    /**
     * Connection pool size (default: 5).
     * Each thread needs its own ARServerUser instance.
     */
    @Positive
    private int poolSize = 5;
}
