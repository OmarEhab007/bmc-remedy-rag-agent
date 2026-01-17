package com.bmc.rag.agent.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Configuration properties for RAG service.
 */
@Data
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "rag")
@Validated
public class RagConfig {

    @PostConstruct
    public void logConfiguration() {
        log.info("RAG Config: maxResults={}, minScore={}, rebacEnabled={}, prioritizeKnowledge={}",
            maxResults, minScore, rebacEnabled, prioritizeKnowledgeArticles);
    }

    /**
     * Maximum number of results to retrieve from vector search.
     */
    @Positive
    private int maxResults = 5;

    /**
     * Minimum similarity score for retrieved results (0.0 to 1.0).
     * Lower values allow more results; 0.5 is a reasonable default for general queries.
     */
    @Min(0)
    @Max(1)
    private float minScore = 0.5f;

    /**
     * Maximum number of messages to keep in chat memory.
     */
    @Positive
    private int maxMemoryMessages = 20;

    /**
     * System prompt for the RAG assistant.
     */
    private String systemPrompt = """
        You are an IT support assistant for BMC Remedy ITSM. Your role is to help users find solutions from the knowledge base.

        RESPONSE GUIDELINES:
        1. ALWAYS cite sources using [SOURCE: type id] format (e.g., [SOURCE: Incident INC000001234])
        2. If context is provided, synthesize information from multiple sources when relevant
        3. If no relevant context is found, clearly state this - never fabricate information
        4. For ambiguous questions, ask for clarification
        5. Provide step-by-step instructions when troubleshooting
        6. Be concise but complete - prefer bullet points for complex procedures

        PRIORITY ORDER:
        1. Knowledge Articles (authoritative solutions)
        2. Resolved Incidents with matching symptoms
        3. Work Orders with relevant procedures
        4. Change Requests for context on system modifications

        OUTPUT FORMAT:
        - Lead with the most relevant solution
        - Include alternative approaches if multiple exist
        - End with source citations
        """;

    /**
     * Whether to include source citations in responses.
     */
    private boolean includeCitations = true;

    /**
     * Whether to prioritize knowledge articles over incidents.
     */
    private boolean prioritizeKnowledgeArticles = true;

    /**
     * Enable ReBAC (Relationship-Based Access Control) filtering.
     */
    private boolean rebacEnabled = true;
}
