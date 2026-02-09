package com.bmc.rag.api.dto.toolserver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SearchResultItem.
 */
@DisplayName("SearchResultItem Tests")
class SearchResultItemTest {

    @Test
    @DisplayName("builder_shouldCreateInstanceWithAllFields")
    void builder_shouldCreateInstanceWithAllFields() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("category", "Hardware");
        metadata.put("priority", "High");

        SearchResultItem item = SearchResultItem.builder()
            .id("INC000001")
            .type("Incident")
            .title("Laptop not booting")
            .snippet("User reports that laptop shows black screen...")
            .score(0.95)
            .scorePercent(95)
            .status("In Progress")
            .metadata(metadata)
            .build();

        assertThat(item.getId()).isEqualTo("INC000001");
        assertThat(item.getType()).isEqualTo("Incident");
        assertThat(item.getTitle()).isEqualTo("Laptop not booting");
        assertThat(item.getSnippet()).contains("black screen");
        assertThat(item.getScore()).isEqualTo(0.95);
        assertThat(item.getScorePercent()).isEqualTo(95);
        assertThat(item.getStatus()).isEqualTo("In Progress");
        assertThat(item.getMetadata()).hasSize(2);
    }

    @Test
    @DisplayName("getScorePercent_withScore_shouldCalculateFromScore")
    void getScorePercent_withScore_shouldCalculateFromScore() {
        SearchResultItem item = SearchResultItem.builder()
            .score(0.856)
            .build();

        assertThat(item.getScorePercent()).isEqualTo(86);
    }

    @Test
    @DisplayName("getScorePercent_withScoreRounding_shouldRoundCorrectly")
    void getScorePercent_withScoreRounding_shouldRoundCorrectly() {
        SearchResultItem item1 = SearchResultItem.builder()
            .score(0.855)
            .build();

        SearchResultItem item2 = SearchResultItem.builder()
            .score(0.844)
            .build();

        assertThat(item1.getScorePercent()).isEqualTo(86); // rounds up
        assertThat(item2.getScorePercent()).isEqualTo(84); // rounds down
    }

    @Test
    @DisplayName("getScorePercent_withExplicitScorePercent_shouldReturnExplicitValue")
    void getScorePercent_withExplicitScorePercent_shouldReturnExplicitValue() {
        SearchResultItem item = SearchResultItem.builder()
            .scorePercent(75)
            .build();

        assertThat(item.getScorePercent()).isEqualTo(75);
    }

    @Test
    @DisplayName("getScorePercent_withNullScore_shouldReturnScorePercent")
    void getScorePercent_withNullScore_shouldReturnScorePercent() {
        SearchResultItem item = SearchResultItem.builder()
            .score(null)
            .scorePercent(80)
            .build();

        assertThat(item.getScorePercent()).isEqualTo(80);
    }

    @Test
    @DisplayName("noArgsConstructor_shouldWork")
    void noArgsConstructor_shouldWork() {
        SearchResultItem item = new SearchResultItem();
        assertThat(item).isNotNull();
    }

    @Test
    @DisplayName("allArgsConstructor_shouldWork")
    void allArgsConstructor_shouldWork() {
        Map<String, String> metadata = new HashMap<>();
        SearchResultItem item = new SearchResultItem(
            "KB001", "KnowledgeArticle", "Test Title", "Test Snippet",
            0.90, 90, "Published", metadata
        );

        assertThat(item.getId()).isEqualTo("KB001");
        assertThat(item.getType()).isEqualTo("KnowledgeArticle");
        assertThat(item.getScore()).isEqualTo(0.90);
    }

    @Test
    @DisplayName("setters_shouldUpdateValues")
    void setters_shouldUpdateValues() {
        SearchResultItem item = new SearchResultItem();
        item.setId("WO001");
        item.setType("WorkOrder");
        item.setTitle("New Title");
        item.setSnippet("New Snippet");
        item.setScore(0.75);
        item.setScorePercent(75);
        item.setStatus("Completed");

        assertThat(item.getId()).isEqualTo("WO001");
        assertThat(item.getType()).isEqualTo("WorkOrder");
        assertThat(item.getScore()).isEqualTo(0.75);
    }

    @Test
    @DisplayName("equalsAndHashCode_shouldWork")
    void equalsAndHashCode_shouldWork() {
        SearchResultItem item1 = SearchResultItem.builder()
            .id("INC001")
            .type("Incident")
            .score(0.95)
            .build();

        SearchResultItem item2 = SearchResultItem.builder()
            .id("INC001")
            .type("Incident")
            .score(0.95)
            .build();

        assertThat(item1).isEqualTo(item2);
        assertThat(item1.hashCode()).isEqualTo(item2.hashCode());
    }

    @Test
    @DisplayName("toString_shouldContainFields")
    void toString_shouldContainFields() {
        SearchResultItem item = SearchResultItem.builder()
            .id("INC001")
            .title("Test")
            .score(0.95)
            .build();

        String toString = item.toString();

        assertThat(toString).contains("INC001");
        assertThat(toString).contains("Test");
        assertThat(toString).contains("0.95");
    }
}
