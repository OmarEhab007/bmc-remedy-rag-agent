package com.bmc.rag.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * BMC Remedy RAG Agent - Main Application Entry Point.
 *
 * An on-premise RAG (Retrieval-Augmented Generation) agent for BMC Remedy AR System
 * that extracts ITSM data, vectorizes it, and enables semantic search for IT support workflows.
 */
@SpringBootApplication(scanBasePackages = "com.bmc.rag")
@EnableJpaRepositories(basePackages = "com.bmc.rag.store.repository")
@EntityScan(basePackages = "com.bmc.rag.store.entity")
@EnableConfigurationProperties
@EnableScheduling
public class BmcRemedyRagApplication {

    public static void main(String[] args) {
        SpringApplication.run(BmcRemedyRagApplication.class, args);
    }
}
