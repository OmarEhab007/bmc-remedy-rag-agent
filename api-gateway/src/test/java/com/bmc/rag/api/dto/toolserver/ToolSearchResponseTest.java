package com.bmc.rag.api.dto.toolserver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ToolSearchResponse.
 */
@DisplayName("ToolSearchResponse Tests")
class ToolSearchResponseTest {

    @Test
    @DisplayName("builder_shouldCreateInstanceWithAllFields")
    void builder_shouldCreateInstanceWithAllFields() {
        List<SearchResultItem> results = List.of(
            SearchResultItem.builder()
                .id("INC001")
                .title("Test incident")
                .score(0.95)
                .build()
        );

        ToolSearchResponse response = ToolSearchResponse.builder()
            .query("test query")
            .totalResults(1)
            .returnedResults(1)
            .results(results)
            .message("Search completed successfully")
            .hasPotentialDuplicates(false)
            .executionTimeMs(150L)
            .build();

        assertThat(response.getQuery()).isEqualTo("test query");
        assertThat(response.getTotalResults()).isEqualTo(1);
        assertThat(response.getReturnedResults()).isEqualTo(1);
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getMessage()).isEqualTo("Search completed successfully");
        assertThat(response.getHasPotentialDuplicates()).isFalse();
        assertThat(response.getExecutionTimeMs()).isEqualTo(150L);
    }

    @Test
    @DisplayName("empty_shouldCreateEmptyResponse")
    void empty_shouldCreateEmptyResponse() {
        ToolSearchResponse response = ToolSearchResponse.empty("test query");

        assertThat(response.getQuery()).isEqualTo("test query");
        assertThat(response.getTotalResults()).isEqualTo(0);
        assertThat(response.getReturnedResults()).isEqualTo(0);
        assertThat(response.getResults()).isEmpty();
        assertThat(response.getMessage()).isEqualTo("No results found matching your query.");
    }

    @Test
    @DisplayName("of_shouldCreateResponseFromResults")
    void of_shouldCreateResponseFromResults() {
        List<SearchResultItem> results = List.of(
            SearchResultItem.builder().id("INC001").title("Test 1").score(0.95).build(),
            SearchResultItem.builder().id("INC002").title("Test 2").score(0.85).build()
        );

        ToolSearchResponse response = ToolSearchResponse.of("test query", results);

        assertThat(response.getQuery()).isEqualTo("test query");
        assertThat(response.getTotalResults()).isEqualTo(2);
        assertThat(response.getReturnedResults()).isEqualTo(2);
        assertThat(response.getResults()).hasSize(2);
    }

    @Test
    @DisplayName("defaultBuilderValues_shouldSetDefaults")
    void defaultBuilderValues_shouldSetDefaults() {
        ToolSearchResponse response = ToolSearchResponse.builder()
            .query("test")
            .build();

        assertThat(response.getResults()).isNotNull().isEmpty();
        assertThat(response.getHasPotentialDuplicates()).isFalse();
    }

    @Test
    @DisplayName("noArgsConstructor_shouldWork")
    void noArgsConstructor_shouldWork() {
        ToolSearchResponse response = new ToolSearchResponse();
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("allArgsConstructor_shouldWork")
    void allArgsConstructor_shouldWork() {
        List<SearchResultItem> results = new ArrayList<>();
        ToolSearchResponse response = new ToolSearchResponse(
            "query", 5, 3, results, "message", true, 200L
        );

        assertThat(response.getQuery()).isEqualTo("query");
        assertThat(response.getTotalResults()).isEqualTo(5);
        assertThat(response.getReturnedResults()).isEqualTo(3);
        assertThat(response.getMessage()).isEqualTo("message");
        assertThat(response.getHasPotentialDuplicates()).isTrue();
        assertThat(response.getExecutionTimeMs()).isEqualTo(200L);
    }

    @Test
    @DisplayName("setters_shouldUpdateValues")
    void setters_shouldUpdateValues() {
        ToolSearchResponse response = new ToolSearchResponse();
        response.setQuery("new query");
        response.setTotalResults(10);
        response.setReturnedResults(5);
        response.setMessage("test message");
        response.setHasPotentialDuplicates(true);
        response.setExecutionTimeMs(500L);

        assertThat(response.getQuery()).isEqualTo("new query");
        assertThat(response.getTotalResults()).isEqualTo(10);
        assertThat(response.getReturnedResults()).isEqualTo(5);
        assertThat(response.getMessage()).isEqualTo("test message");
        assertThat(response.getHasPotentialDuplicates()).isTrue();
        assertThat(response.getExecutionTimeMs()).isEqualTo(500L);
    }
}
