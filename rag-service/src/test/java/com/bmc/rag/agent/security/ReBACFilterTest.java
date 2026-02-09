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

    @Test
    @DisplayName("filterAndPrioritize without prioritization")
    void testFilterAndPrioritizeWithoutPrioritization() {
        List<SearchResult> results = List.of(
            createSearchResult("1", "Service Desk"),
            createSearchResult("2", "Network Team")
        );

        Set<String> userGroups = Set.of("Service Desk");

        List<SearchResult> filtered = rebacFilter.filterAndPrioritize(results, userGroups, false);

        assertEquals(1, filtered.size());
        assertEquals("1", filtered.get(0).getChunkId());
    }

    @Test
    @DisplayName("filterAndPrioritize with knowledge article prioritization")
    void testFilterAndPrioritizeWithKnowledgeArticles() {
        SearchResult incident = SearchResult.builder()
            .chunkId("1")
            .sourceType("Incident")
            .sourceId("INC001")
            .textSegment("content")
            .score(0.9f)
            .build();

        SearchResult kb = SearchResult.builder()
            .chunkId("2")
            .sourceType("KnowledgeArticle")
            .sourceId("KB001")
            .textSegment("content")
            .score(0.7f)
            .build();

        List<SearchResult> results = List.of(incident, kb);

        List<SearchResult> filtered = rebacFilter.filterAndPrioritize(
            results, Collections.emptySet(), true);

        assertEquals(2, filtered.size());
        // Knowledge article should be first
        assertEquals("KnowledgeArticle", filtered.get(0).getSourceType());
        assertEquals("Incident", filtered.get(1).getSourceType());
    }

    @Test
    @DisplayName("filterAndPrioritize with empty results")
    void testFilterAndPrioritizeEmptyResults() {
        List<SearchResult> filtered = rebacFilter.filterAndPrioritize(
            Collections.emptyList(), Set.of("Admin"), true);

        assertTrue(filtered.isEmpty());
    }

    @Test
    @DisplayName("prioritizeHighValueChunks prioritizes resolution chunks")
    void testPrioritizeHighValueChunksResolution() {
        SearchResult normal = SearchResult.builder()
            .chunkId("1")
            .sourceType("Incident")
            .sourceId("INC001")
            .chunkType("SUMMARY")
            .textSegment("content")
            .score(0.8f)
            .build();

        SearchResult resolution = SearchResult.builder()
            .chunkId("2")
            .sourceType("Incident")
            .sourceId("INC002")
            .chunkType("RESOLUTION")
            .textSegment("resolution content")
            .score(0.7f)
            .build();

        List<SearchResult> results = List.of(normal, resolution);
        List<SearchResult> prioritized = rebacFilter.prioritizeHighValueChunks(results);

        assertEquals(2, prioritized.size());
        // Resolution should be first despite lower score
        assertEquals("RESOLUTION", prioritized.get(0).getChunkType());
        assertEquals("SUMMARY", prioritized.get(1).getChunkType());
    }

    @Test
    @DisplayName("prioritizeHighValueChunks handles implementation and article content")
    void testPrioritizeHighValueChunksMultipleTypes() {
        SearchResult normal = SearchResult.builder()
            .chunkId("1")
            .sourceType("Incident")
            .sourceId("INC001")
            .chunkType("NOTES")
            .score(0.9f)
            .build();

        SearchResult implementation = SearchResult.builder()
            .chunkId("2")
            .sourceType("Change")
            .sourceId("CHG001")
            .chunkType("IMPLEMENTATION")
            .score(0.8f)
            .build();

        SearchResult article = SearchResult.builder()
            .chunkId("3")
            .sourceType("KnowledgeArticle")
            .sourceId("KB001")
            .chunkType("ARTICLE_CONTENT")
            .score(0.7f)
            .build();

        List<SearchResult> results = List.of(normal, implementation, article);
        List<SearchResult> prioritized = rebacFilter.prioritizeHighValueChunks(results);

        assertEquals(3, prioritized.size());
        // High-value chunks first
        assertTrue(prioritized.get(0).getChunkType().equals("IMPLEMENTATION") ||
                   prioritized.get(0).getChunkType().equals("ARTICLE_CONTENT"));
        assertEquals("NOTES", prioritized.get(2).getChunkType());
    }

    @Test
    @DisplayName("prioritizeHighValueChunks handles null chunk types")
    void testPrioritizeHighValueChunksNullChunkType() {
        SearchResult withNull = SearchResult.builder()
            .chunkId("1")
            .sourceType("Incident")
            .sourceId("INC001")
            .chunkType(null)
            .score(0.8f)
            .build();

        List<SearchResult> results = List.of(withNull);
        List<SearchResult> prioritized = rebacFilter.prioritizeHighValueChunks(results);

        assertEquals(1, prioritized.size());
        assertNull(prioritized.get(0).getChunkType());
    }

    @Test
    @DisplayName("prioritizeHighValueChunks handles single result")
    void testPrioritizeHighValueChunksSingleResult() {
        SearchResult single = SearchResult.builder()
            .chunkId("1")
            .sourceType("Incident")
            .sourceId("INC001")
            .score(0.8f)
            .build();

        List<SearchResult> results = List.of(single);
        List<SearchResult> prioritized = rebacFilter.prioritizeHighValueChunks(results);

        assertEquals(results, prioritized);
    }

    @Test
    @DisplayName("prioritizeHighValueChunks handles null input")
    void testPrioritizeHighValueChunksNullInput() {
        List<SearchResult> prioritized = rebacFilter.prioritizeHighValueChunks(null);
        assertNull(prioritized);
    }

    @Test
    @DisplayName("deduplicateBySource handles null and empty")
    void testDeduplicateBySourceHandlesNullAndEmpty() {
        assertNull(rebacFilter.deduplicateBySource(null));
        assertTrue(rebacFilter.deduplicateBySource(Collections.emptyList()).isEmpty());
    }

    @Test
    @DisplayName("applyAllFilters combines all operations")
    void testApplyAllFilters() {
        SearchResult kb1 = SearchResult.builder()
            .chunkId("1")
            .sourceType("KnowledgeArticle")
            .sourceId("KB001")
            .chunkType("ARTICLE_CONTENT")
            .score(0.9f)
            .metadata(Map.of("assigned_group", "Service Desk"))
            .build();

        SearchResult incident1 = SearchResult.builder()
            .chunkId("2")
            .sourceType("Incident")
            .sourceId("INC001")
            .chunkType("SUMMARY")
            .score(0.8f)
            .metadata(Map.of("assigned_group", "Service Desk"))
            .build();

        SearchResult incident2 = SearchResult.builder()
            .chunkId("3")
            .sourceType("Incident")
            .sourceId("INC001")
            .chunkType("RESOLUTION")
            .score(0.7f)
            .metadata(Map.of("assigned_group", "Service Desk"))
            .build();

        List<SearchResult> results = List.of(kb1, incident1, incident2);

        List<SearchResult> filtered = rebacFilter.applyAllFilters(
            results,
            Set.of("Service Desk"),
            true,   // prioritize knowledge
            true    // deduplicate
        );

        // Should have KB article first, then only one incident (deduplicated)
        assertTrue(filtered.size() <= 2);
        assertEquals("KnowledgeArticle", filtered.get(0).getSourceType());
    }

    @Test
    @DisplayName("applyAllFilters without deduplication")
    void testApplyAllFiltersWithoutDedup() {
        SearchResult r1 = SearchResult.builder()
            .chunkId("1")
            .sourceType("Incident")
            .sourceId("INC001")
            .chunkType("SUMMARY")
            .score(0.8f)
            .build();

        SearchResult r2 = SearchResult.builder()
            .chunkId("2")
            .sourceType("Incident")
            .sourceId("INC001")
            .chunkType("RESOLUTION")
            .score(0.7f)
            .build();

        List<SearchResult> results = List.of(r1, r2);

        List<SearchResult> filtered = rebacFilter.applyAllFilters(
            results,
            Collections.emptySet(),
            false,  // no prioritization
            false   // no deduplication
        );

        // Both results should remain
        assertEquals(2, filtered.size());
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
