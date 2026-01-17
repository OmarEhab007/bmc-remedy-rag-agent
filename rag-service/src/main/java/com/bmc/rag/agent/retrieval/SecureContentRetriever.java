package com.bmc.rag.agent.retrieval;

import com.bmc.rag.agent.config.RagConfig;
import com.bmc.rag.agent.security.ReBACFilter;
import com.bmc.rag.store.service.VectorStoreService;
import com.bmc.rag.store.service.VectorStoreService.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Secure content retriever with ReBAC (Relationship-Based Access Control).
 * Retrieves relevant content from the vector store while respecting access controls.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecureContentRetriever {

    private static final int MAX_QUERY_LENGTH = 10000;

    private final VectorStoreService vectorStoreService;
    private final ReBACFilter rebacFilter;
    private final RagConfig ragConfig;

    /**
     * Retrieve relevant content for a query with security filtering.
     *
     * @param query The user's query
     * @param userContext User context containing group memberships
     * @return Retrieved content ready for LLM context
     */
    public RetrievalResult retrieve(String query, UserContext userContext) {
        // Validate query to prevent DoS via large queries
        validateQuery(query);

        log.info("Retrieving content for query: '{}' with maxResults={}, minScore={}",
            truncateForLog(query), ragConfig.getMaxResults(), ragConfig.getMinScore());

        // Perform vector search
        List<SearchResult> rawResults;

        if (ragConfig.isRebacEnabled() && userContext != null && userContext.hasGroups()) {
            // Use database-level group filtering for efficiency
            rawResults = vectorStoreService.searchWithGroups(
                query,
                ragConfig.getMaxResults() * 2,  // Fetch more for post-filtering
                ragConfig.getMinScore(),
                userContext.getGroupsAsList()
            );
        } else {
            // No ReBAC filtering
            rawResults = vectorStoreService.search(
                query,
                ragConfig.getMaxResults(),
                ragConfig.getMinScore()
            );
        }

        log.info("Vector search returned {} raw results (rebacEnabled={})", rawResults.size(), ragConfig.isRebacEnabled());

        // Log details of each result for debugging
        for (SearchResult r : rawResults) {
            log.debug("Result: chunkId={}, sourceType={}, sourceId={}, score={}",
                r.getChunkId(), r.getSourceType(), r.getSourceId(), r.getScore());
        }

        if (rawResults.isEmpty()) {
            log.info("No results found for query - check if embeddings are properly generated");
            return RetrievalResult.empty();
        }

        // Apply additional filtering and prioritization
        List<SearchResult> filtered;
        if (ragConfig.isRebacEnabled()) {
            // Apply ReBAC filtering when enabled
            filtered = rebacFilter.applyAllFilters(
                rawResults,
                userContext != null ? userContext.groups() : Collections.emptySet(),
                ragConfig.isPrioritizeKnowledgeArticles(),
                true  // deduplicate
            );
        } else {
            // Skip ReBAC filtering - only apply prioritization and deduplication
            log.debug("ReBAC disabled - skipping group filtering");
            filtered = rebacFilter.prioritizeHighValueChunks(rawResults);
            filtered = rebacFilter.deduplicateBySource(filtered);
            if (ragConfig.isPrioritizeKnowledgeArticles()) {
                // Manually prioritize knowledge articles
                List<SearchResult> ka = new ArrayList<>();
                List<SearchResult> other = new ArrayList<>();
                for (SearchResult r : filtered) {
                    if ("KnowledgeArticle".equals(r.getSourceType())) {
                        ka.add(r);
                    } else {
                        other.add(r);
                    }
                }
                filtered = new ArrayList<>();
                filtered.addAll(ka);
                filtered.addAll(other);
            }
        }

        // Limit to max results
        if (filtered.size() > ragConfig.getMaxResults()) {
            filtered = filtered.subList(0, ragConfig.getMaxResults());
        }

        log.info("Returning {} results after filtering", filtered.size());

        return buildRetrievalResult(filtered);
    }

    /**
     * Retrieve content filtered by specific source types.
     *
     * @param query The user's query
     * @param sourceTypes The source types to include
     * @param userContext User context
     * @return Retrieved content
     */
    public RetrievalResult retrieveBySourceTypes(
            String query,
            List<String> sourceTypes,
            UserContext userContext) {

        // Validate query to prevent DoS
        validateQuery(query);

        log.info("Retrieving content for query: '{}', source types: {}",
            truncateForLog(query), sourceTypes);

        List<SearchResult> rawResults = vectorStoreService.searchBySourceTypes(
            query,
            ragConfig.getMaxResults() * 2,
            ragConfig.getMinScore(),
            sourceTypes
        );

        if (rawResults.isEmpty()) {
            return RetrievalResult.empty();
        }

        // Apply ReBAC filtering
        List<SearchResult> filtered = rebacFilter.filterByGroups(
            rawResults,
            userContext != null ? userContext.groups() : Collections.emptySet()
        );

        if (filtered.size() > ragConfig.getMaxResults()) {
            filtered = filtered.subList(0, ragConfig.getMaxResults());
        }

        return buildRetrievalResult(filtered);
    }

    /**
     * Search only knowledge articles.
     */
    public RetrievalResult retrieveKnowledgeArticles(String query, UserContext userContext) {
        return retrieveBySourceTypes(query, List.of("KnowledgeArticle"), userContext);
    }

    /**
     * Search only incidents.
     */
    public RetrievalResult retrieveIncidents(String query, UserContext userContext) {
        return retrieveBySourceTypes(query, List.of("Incident"), userContext);
    }

    /**
     * Build the retrieval result with formatted context.
     */
    private RetrievalResult buildRetrievalResult(List<SearchResult> results) {
        List<RetrievedDocument> documents = results.stream()
            .map(this::toRetrievedDocument)
            .collect(Collectors.toList());

        // Build context string for LLM
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("## Relevant Information from ITSM Knowledge Base\n\n");

        for (int i = 0; i < documents.size(); i++) {
            RetrievedDocument doc = documents.get(i);
            contextBuilder.append(String.format("### Source %d: %s %s (Score: %.2f)\n",
                i + 1, doc.sourceType(), doc.sourceId(), doc.score()));

            if (doc.title() != null) {
                contextBuilder.append("**Title:** ").append(doc.title()).append("\n");
            }

            if (doc.category() != null) {
                contextBuilder.append("**Category:** ").append(doc.category()).append("\n");
            }

            contextBuilder.append("\n").append(doc.content()).append("\n\n---\n\n");
        }

        return new RetrievalResult(documents, contextBuilder.toString());
    }

    /**
     * Convert SearchResult to RetrievedDocument.
     */
    private RetrievedDocument toRetrievedDocument(SearchResult result) {
        Map<String, String> metadata = result.getMetadata();

        return new RetrievedDocument(
            result.getSourceType(),
            result.getSourceId(),
            result.getChunkType(),
            result.getTextSegment(),
            metadata != null ? metadata.get("title") : null,
            metadata != null ? metadata.get("category") : null,
            metadata != null ? metadata.get("assigned_group") : null,
            result.getScore()
        );
    }

    /**
     * Validate query to prevent DoS attacks via large queries.
     */
    private void validateQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query cannot be null or empty");
        }
        if (query.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException("Query exceeds maximum length of " + MAX_QUERY_LENGTH + " characters");
        }
    }

    /**
     * Truncate text for logging.
     */
    private String truncateForLog(String text) {
        if (text == null) return "";
        return text.length() > 100 ? text.substring(0, 100) + "..." : text;
    }

    /**
     * User context containing group memberships.
     */
    public record UserContext(String userId, Set<String> groups) {
        public boolean hasGroups() {
            return groups != null && !groups.isEmpty();
        }

        public List<String> getGroupsAsList() {
            return groups != null ? new ArrayList<>(groups) : Collections.emptyList();
        }

        public static UserContext anonymous() {
            return new UserContext(null, Collections.emptySet());
        }

        public static UserContext withGroups(String userId, String... groups) {
            return new UserContext(userId, Set.of(groups));
        }
    }

    /**
     * Retrieved document record.
     */
    public record RetrievedDocument(
        String sourceType,
        String sourceId,
        String chunkType,
        String content,
        String title,
        String category,
        String assignedGroup,
        float score
    ) {
        /**
         * Get formatted source reference for citations.
         */
        public String getSourceReference() {
            return String.format("%s %s", sourceType, sourceId);
        }
    }

    /**
     * Retrieval result containing documents and formatted context.
     */
    public record RetrievalResult(
        List<RetrievedDocument> documents,
        String formattedContext
    ) {
        public static RetrievalResult empty() {
            return new RetrievalResult(Collections.emptyList(), "");
        }

        public boolean isEmpty() {
            return documents == null || documents.isEmpty();
        }

        public int size() {
            return documents != null ? documents.size() : 0;
        }

        /**
         * Get all source references for citations.
         */
        public List<String> getSourceReferences() {
            if (documents == null) return Collections.emptyList();
            return documents.stream()
                .map(RetrievedDocument::getSourceReference)
                .distinct()
                .collect(Collectors.toList());
        }
    }
}
