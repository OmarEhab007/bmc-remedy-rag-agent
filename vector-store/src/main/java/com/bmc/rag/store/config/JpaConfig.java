package com.bmc.rag.store.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA configuration for the vector-store module.
 *
 * Enables JPA repositories and entity scanning for the store package.
 * This configuration is automatically picked up via component scanning
 * (scanBasePackages = "com.bmc.rag") from the main application class,
 * keeping JPA concerns in the data module where they belong.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.bmc.rag.store.repository")
@EntityScan(basePackages = "com.bmc.rag.store.entity")
public class JpaConfig {
}
