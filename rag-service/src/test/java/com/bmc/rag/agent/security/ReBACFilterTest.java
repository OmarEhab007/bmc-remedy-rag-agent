package com.bmc.rag.agent.security;

import com.bmc.rag.store.service.VectorStoreService.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ReBACFilter to verify access control logic.
 */
class ReBACFilterTest {

    private ReBACFilter rebacFilter;

    @BeforeEach
    void setUp() {
        rebacFilter = new ReBACFilter();
    }

    @Test
    @DisplayName("Case-insensitive group matching should work")
    void testCaseInsensitiveGroupMatching() {
        // Create test results with different case groups
        List<SearchResult> results = List.of(
            createSearchResult("1", "Service Desk"),
            createSearchResult("2", "service desk"),
            createSearchResult("3", "SERVICE DESK"),
            createSearchResult("4", "Network Team")
        );

        // User groups with mixed case
        Set<String> userGroups = Set.of("Service Desk");

        List<SearchResult> filtered = rebacFilter.filterByGroups(results, userGroups);

        // Should match all Service Desk variations (case-insensitive)
        assertEquals(3, filtered.size());
        assertTrue(filtered.stream().anyMatch(r -> r.getChunkId().equals("1")));
        assertTrue(filtered.stream().anyMatch(r -> r.getChunkId().equals("2")));
        assertTrue(filtered.stream().anyMatch(r -> r.getChunkId().equals("3")));
    }

    @Test
    @DisplayName("Results without assigned_group should be visible to all")
    void testResultsWithoutGroupVisibleToAll() {
        List<SearchResult> results = List.of(
            createSearchResult("1", null),
            createSearchResult("2", ""),
            createSearchResult("3", "Service Desk")
        );

        Set<String> userGroups = Set.of("Other Team");

        List<SearchResult> filtered = rebacFilter.filterByGroups(results, userGroups);

        // Should include results without group assignment
        assertEquals(2, filtered.size());
        assertTrue(filtered.stream().anyMatch(r -> r.getChunkId().equals("1")));
        assertTrue(filtered.stream().anyMatch(r -> r.getChunkId().equals("2")));
    }

    @Test
    @DisplayName("Users without groups should only see public results")
    void testUsersWithoutGroupsSeeOnlyPublic() {
        List<SearchResult> results = List.of(
            createSearchResult("1", null),
            createSearchResult("2", "Service Desk"),
            createSearchResult("3", "")
        );

        Set<String> userGroups = Collections.emptySet();

        List<SearchResult> filtered = rebacFilter.filterByGroups(results, userGroups);

        // Should only include results without group assignment
        assertEquals(2, filtered.size());
        assertFalse(filtered.stream().anyMatch(r -> r.getChunkId().equals("2")));
    }

    @Test
    @DisplayName("Null results should return empty list")
    void testNullResultsReturnEmpty() {
        List<SearchResult> filtered = rebacFilter.filterByGroups(null, Set.of("Admin"));
        assertTrue(filtered.isEmpty());
    }

    @Test
    @DisplayName("Empty results should return empty list")
    void testEmptyResultsReturnEmpty() {
        List<SearchResult> filtered = rebacFilter.filterByGroups(Collections.emptyList(), Set.of("Admin"));
        assertTrue(filtered.isEmpty());
    }

    @Test
    @DisplayName("Deduplication should keep highest score per source")
    void testDeduplicationKeepsHighestScore() {
        SearchResult result1 = SearchResult.builder()
            .chunkId("chunk1")
            .sourceType("Incident")
            .sourceId("INC001")
            .score(0.7f)
            .build();

        SearchResult result2 = SearchResult.builder()
            .chunkId("chunk2")
            .sourceType("Incident")
            .sourceId("INC001")
            .score(0.9f)
            .build();

        SearchResult result3 = SearchResult.builder()
            .chunkId("chunk3")
            .sourceType("Incident")
            .sourceId("INC002")
            .score(0.8f)
            .build();

        List<SearchResult> results = List.of(result1, result2, result3);
        List<SearchResult> deduplicated = rebacFilter.deduplicateBySource(results);

        assertEquals(2, deduplicated.size());
        // Should keep the higher score for INC001
        assertTrue(deduplicated.stream()
            .filter(r -> r.getSourceId().equals("INC001"))
            .allMatch(r -> r.getScore() == 0.9f));
    }

    private SearchResult createSearchResult(String chunkId, String assignedGroup) {
        Map<String, String> metadata = new HashMap<>();
        if (assignedGroup != null) {
            metadata.put("assigned_group", assignedGroup);
        }

        return SearchResult.builder()
            .chunkId(chunkId)
            .sourceType("Incident")
            .sourceId("INC" + chunkId)
            .textSegment("Test content")
            .score(0.8f)
            .metadata(metadata)
            .build();
    }
}
