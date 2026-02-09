package com.bmc.rag.api.dto.toolserver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ToolSearchRequest.
 */
@DisplayName("ToolSearchRequest Tests")
class ToolSearchRequestTest {

    @Test
    @DisplayName("builder_shouldCreateInstanceWithAllFields")
    void builder_shouldCreateInstanceWithAllFields() {
        Map<String, String> filters = new HashMap<>();
        filters.put("status", "Open");
        filters.put("priority", "High");

        List<String> sourceTypes = List.of("Incident", "KnowledgeArticle");

        ToolSearchRequest request = ToolSearchRequest.builder()
            .query("test query")
            .limit(20)
            .minScore(0.7)
            .filters(filters)
            .sourceTypes(sourceTypes)
            .build();

        assertThat(request.getQuery()).isEqualTo("test query");
        assertThat(request.getLimit()).isEqualTo(20);
        assertThat(request.getMinScore()).isEqualTo(0.7);
        assertThat(request.getFilters()).hasSize(2);
        assertThat(request.getSourceTypes()).hasSize(2);
    }

    @Test
    @DisplayName("builder_shouldUseDefaultValues")
    void builder_shouldUseDefaultValues() {
        ToolSearchRequest request = ToolSearchRequest.builder()
            .query("test")
            .build();

        assertThat(request.getLimit()).isEqualTo(10);
        assertThat(request.getMinScore()).isEqualTo(0.3);
    }

    @Test
    @DisplayName("noArgsConstructor_shouldWork")
    void noArgsConstructor_shouldWork() {
        ToolSearchRequest request = new ToolSearchRequest();
        assertThat(request).isNotNull();
    }

    @Test
    @DisplayName("allArgsConstructor_shouldWork")
    void allArgsConstructor_shouldWork() {
        Map<String, String> filters = new HashMap<>();
        List<String> sourceTypes = List.of("Incident");

        ToolSearchRequest request = new ToolSearchRequest(
            "query", 15, 0.5, filters, sourceTypes
        );

        assertThat(request.getQuery()).isEqualTo("query");
        assertThat(request.getLimit()).isEqualTo(15);
        assertThat(request.getMinScore()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("setters_shouldUpdateValues")
    void setters_shouldUpdateValues() {
        ToolSearchRequest request = new ToolSearchRequest();
        request.setQuery("new query");
        request.setLimit(25);
        request.setMinScore(0.8);

        assertThat(request.getQuery()).isEqualTo("new query");
        assertThat(request.getLimit()).isEqualTo(25);
        assertThat(request.getMinScore()).isEqualTo(0.8);
    }
}
