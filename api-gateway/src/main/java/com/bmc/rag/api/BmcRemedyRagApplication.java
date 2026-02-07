package com.bmc.rag.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * BMC Remedy RAG Agent - Main Application Entry Point.
 *
 * An on-premise RAG (Retrieval-Augmented Generation) agent for BMC Remedy AR System
 * that extracts ITSM data, vectorizes it, and enables semantic search for IT support workflows.
 *
 * JPA repositories and entity scanning are configured in
 * {@link com.bmc.rag.store.config.JpaConfig} within the vector-store module.
 */
@SpringBootApplication(scanBasePackages = "com.bmc.rag")
@EnableConfigurationProperties
@EnableScheduling
public class BmcRemedyRagApplication {

    public static void main(String[] args) {
        SpringApplication.run(BmcRemedyRagApplication.class, args);
    }
}
