package com.bmc.rag.agent.security;

import com.bmc.rag.store.service.VectorStoreService.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Relationship-Based Access Control (ReBAC) filter for vector search results.
 * Filters results based on user's group memberships.
 */
@Slf4j
@Component
public class ReBACFilter {

    /**
     * Filter search results based on user's allowed groups.
     * Records without an assigned_group are visible to all users.
     *
     * @param results The search results to filter
     * @param userGroups The groups the user belongs to
     * @return Filtered list of search results
     */
    public List<SearchResult> filterByGroups(List<SearchResult> results, Set<String> userGroups) {
        if (results == null || results.isEmpty()) {
            return Collections.emptyList();
        }

        if (userGroups == null || userGroups.isEmpty()) {
            // If no groups provided, only return results without assigned_group
            log.debug("No user groups provided, filtering to public results only");
            return results.stream()
                .filter(result -> {
                    String assignedGroup = getAssignedGroup(result);
                    return assignedGroup == null || assignedGroup.isEmpty();
                })
                .collect(Collectors.toList());
        }

        log.debug("Filtering {} results for user groups: {}", results.size(), userGroups);

        // Create case-insensitive set for comparison
        Set<String> userGroupsLower = userGroups.stream()
            .map(String::toLowerCase)
            .collect(Collectors.toSet());

        List<SearchResult> filtered = results.stream()
            .filter(result -> {
                String assignedGroup = getAssignedGroup(result);
                // Allow if no assigned_group or if user is in the assigned group (case-insensitive)
                return assignedGroup == null ||
                       assignedGroup.isEmpty() ||
                       userGroupsLower.contains(assignedGroup.toLowerCase());
            })
            .collect(Collectors.toList());

        log.debug("Filtered to {} results after ReBAC check", filtered.size());
        return filtered;
    }

    /**
     * Filter and prioritize results.
     * Knowledge articles are prioritized if configured.
     *
     * @param results The search results
     * @param userGroups User's groups
     * @param prioritizeKnowledgeArticles Whether to prioritize knowledge articles
     * @return Filtered and prioritized results
     */
    public List<SearchResult> filterAndPrioritize(
            List<SearchResult> results,
            Set<String> userGroups,
            boolean prioritizeKnowledgeArticles) {

        List<SearchResult> filtered = filterByGroups(results, userGroups);

        if (!prioritizeKnowledgeArticles || filtered.isEmpty()) {
            return filtered;
        }

        // Separate knowledge articles from other results
        List<SearchResult> knowledgeArticles = new ArrayList<>();
        List<SearchResult> otherResults = new ArrayList<>();

        for (SearchResult result : filtered) {
            if ("KnowledgeArticle".equals(result.getSourceType())) {
                knowledgeArticles.add(result);
            } else {
                otherResults.add(result);
            }
        }

        // Combine with knowledge articles first
        List<SearchResult> prioritized = new ArrayList<>();
        prioritized.addAll(knowledgeArticles);
        prioritized.addAll(otherResults);

        return prioritized;
    }

    /**
     * Prioritize high-value chunks (resolutions, implementation plans).
     *
     * @param results The search results
     * @return Results with high-value chunks prioritized
     */
    public List<SearchResult> prioritizeHighValueChunks(List<SearchResult> results) {
        if (results == null || results.size() <= 1) {
            return results;
        }

        // Define high-value chunk types
        Set<String> highValueTypes = Set.of("RESOLUTION", "IMPLEMENTATION", "ROLLBACK", "ARTICLE_CONTENT");

        // Separate high-value and normal results
        List<SearchResult> highValue = new ArrayList<>();
        List<SearchResult> normal = new ArrayList<>();

        for (SearchResult result : results) {
            String chunkType = result.getChunkType();
            if (chunkType != null && highValueTypes.contains(chunkType)) {
                highValue.add(result);
            } else {
                normal.add(result);
            }
        }

        // Combine with high-value first
        List<SearchResult> prioritized = new ArrayList<>();
        prioritized.addAll(highValue);
        prioritized.addAll(normal);

        return prioritized;
    }

    /**
     * Remove duplicate content from search results.
     * Keeps the highest-scoring result for each source record.
     *
     * @param results The search results
     * @return Deduplicated results
     */
    public List<SearchResult> deduplicateBySource(List<SearchResult> results) {
        if (results == null || results.isEmpty()) {
            return results;
        }

        // Track best result per source record
        Map<String, SearchResult> bestBySource = new LinkedHashMap<>();

        for (SearchResult result : results) {
            String sourceKey = result.getSourceType() + ":" + result.getSourceId();
            SearchResult existing = bestBySource.get(sourceKey);

            if (existing == null || result.getScore() > existing.getScore()) {
                bestBySource.put(sourceKey, result);
            }
        }

        return new ArrayList<>(bestBySource.values());
    }

    /**
     * Apply all filters and prioritization.
     *
     * @param results The raw search results
     * @param userGroups User's groups
     * @param prioritizeKnowledge Whether to prioritize knowledge articles
     * @param deduplicate Whether to deduplicate by source
     * @return Processed results
     */
    public List<SearchResult> applyAllFilters(
            List<SearchResult> results,
            Set<String> userGroups,
            boolean prioritizeKnowledge,
            boolean deduplicate) {

        // Apply ReBAC filter
        List<SearchResult> filtered = filterAndPrioritize(results, userGroups, prioritizeKnowledge);

        // Prioritize high-value chunks
        filtered = prioritizeHighValueChunks(filtered);

        // Deduplicate if requested
        if (deduplicate) {
            filtered = deduplicateBySource(filtered);
        }

        return filtered;
    }

    /**
     * Get assigned_group from result metadata.
     */
    private String getAssignedGroup(SearchResult result) {
        if (result.getMetadata() == null) {
            return null;
        }
        return result.getMetadata().get("assigned_group");
    }
}
