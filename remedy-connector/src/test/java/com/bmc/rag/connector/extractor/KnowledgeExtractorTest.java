package com.bmc.rag.connector.extractor;

import com.bmc.arsys.api.Entry;
import com.bmc.arsys.api.Timestamp;
import com.bmc.arsys.api.Value;
import com.bmc.rag.connector.config.RemedyConnectionConfig;
import com.bmc.rag.connector.connection.ThreadLocalARContext;
import com.bmc.rag.connector.model.KnowledgeArticle;
import com.bmc.rag.connector.util.FieldIdConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for KnowledgeExtractor.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KnowledgeExtractorTest {

    @Mock
    private ThreadLocalARContext mockArContext;

    @Mock
    private RemedyConnectionConfig mockConfig;

    @InjectMocks
    private KnowledgeExtractor knowledgeExtractor;

    @BeforeEach
    void setUp() {
        when(mockConfig.getChunkSize()).thenReturn(500);
    }

    @Test
    void extractModifiedSince_validTimestamp_returnsArticles() {
        // Given
        long timestamp = 1672531200L;
        KnowledgeArticle mockArticle = KnowledgeArticle.builder()
            .articleId("KA000001")
            .title("How to reset password")
            .build();
        List<KnowledgeArticle> expectedList = List.of(mockArticle);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<KnowledgeArticle> results = knowledgeExtractor.extractModifiedSince(timestamp);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getArticleId()).isEqualTo("KA000001");
        assertThat(results.get(0).getTitle()).isEqualTo("How to reset password");
    }

    @Test
    void extractPublishedArticles_withTimestamp_returnsPublishedOnly() {
        // Given
        long timestamp = 1672531200L;
        KnowledgeArticle publishedArticle = KnowledgeArticle.builder()
            .articleId("KA000001")
            .title("Published article")
            .status(2) // KA_PUBLISHED
            .build();
        List<KnowledgeArticle> expectedList = List.of(publishedArticle);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<KnowledgeArticle> results = knowledgeExtractor.extractPublishedArticles(timestamp);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(2);
    }

    @Test
    void extractPublishedArticles_zeroTimestamp_returnsAllPublished() {
        // Given
        KnowledgeArticle article1 = KnowledgeArticle.builder().articleId("KA000001").title("Article 1").status(2).build();
        KnowledgeArticle article2 = KnowledgeArticle.builder().articleId("KA000002").title("Article 2").status(2).build();
        List<KnowledgeArticle> expectedList = List.of(article1, article2);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<KnowledgeArticle> results = knowledgeExtractor.extractPublishedArticles(0L);

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void extractWithQualification_nullQualification_returnsAllArticles() {
        // Given
        KnowledgeArticle article1 = KnowledgeArticle.builder().articleId("KA000001").title("Article 1").build();
        KnowledgeArticle article2 = KnowledgeArticle.builder().articleId("KA000002").title("Article 2").build();
        List<KnowledgeArticle> expectedList = List.of(article1, article2);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<KnowledgeArticle> results = knowledgeExtractor.extractWithQualification(null);

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void extractWithQualification_validQualification_returnsFilteredArticles() {
        // Given
        String qualification = "'302300504' = \"Network\"";
        KnowledgeArticle mockArticle = KnowledgeArticle.builder()
            .articleId("KA000001")
            .title("Network troubleshooting")
            .categoryTier1("Network")
            .build();
        List<KnowledgeArticle> expectedList = List.of(mockArticle);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<KnowledgeArticle> results = knowledgeExtractor.extractWithQualification(qualification);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCategoryTier1()).isEqualTo("Network");
    }

    @Test
    void extractWithQualification_multipleResults_retrievesAll() {
        // Given
        when(mockConfig.getChunkSize()).thenReturn(2);

        KnowledgeArticle article1 = KnowledgeArticle.builder().articleId("KA000001").title("Article 1").build();
        KnowledgeArticle article2 = KnowledgeArticle.builder().articleId("KA000002").title("Article 2").build();
        KnowledgeArticle article3 = KnowledgeArticle.builder().articleId("KA000003").title("Article 3").build();
        List<KnowledgeArticle> expectedList = List.of(article1, article2, article3);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<KnowledgeArticle> results = knowledgeExtractor.extractWithQualification(null);

        // Then
        assertThat(results).hasSize(3);
    }

    @Test
    void extractWithQualification_emptyResult_returnsEmptyList() {
        // Given
        when(mockArContext.executeWithRetry(any())).thenReturn(Collections.emptyList());

        // When
        List<KnowledgeArticle> results = knowledgeExtractor.extractWithQualification("'7' = 99");

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void extractByArticleId_found_returnsArticle() {
        // Given
        KnowledgeArticle mockArticle = KnowledgeArticle.builder()
            .articleId("KA000001")
            .title("VPN Setup Guide")
            .build();
        List<KnowledgeArticle> expectedList = List.of(mockArticle);
        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        Optional<KnowledgeArticle> result = knowledgeExtractor.extractByArticleId("KA000001");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getArticleId()).isEqualTo("KA000001");
    }

    @Test
    void extractByArticleId_notFound_returnsEmpty() {
        // Given
        when(mockArContext.executeWithRetry(any())).thenReturn(Collections.emptyList());

        // When
        Optional<KnowledgeArticle> result = knowledgeExtractor.extractByArticleId("KA999999");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void searchByKeywords_validSearchTerm_returnsMatchingArticles() {
        // Given
        String searchTerm = "VPN";
        KnowledgeArticle matchingArticle = KnowledgeArticle.builder()
            .articleId("KA000001")
            .title("VPN Configuration")
            .keywords("VPN, network, access")
            .build();
        List<KnowledgeArticle> expectedList = List.of(matchingArticle);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<KnowledgeArticle> results = knowledgeExtractor.searchByKeywords(searchTerm);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).contains("VPN");
    }

    @Test
    void searchByKeywords_noMatches_returnsEmptyList() {
        // Given
        when(mockArContext.executeWithRetry(any())).thenReturn(Collections.emptyList());

        // When
        List<KnowledgeArticle> results = knowledgeExtractor.searchByKeywords("nonexistent");

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void checkExistence_validList_returnsExistingArticles() {
        // Given
        List<String> articleIds = Arrays.asList("KA000001", "KA000002", "KA999999");
        Set<String> expectedSet = Set.of("KA000001", "KA000002");

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedSet);

        // When
        Set<String> results = knowledgeExtractor.checkExistence(articleIds);

        // Then
        assertThat(results).contains("KA000001", "KA000002");
        assertThat(results).doesNotContain("KA999999");
    }

    @Test
    void checkExistence_emptyList_returnsEmptySet() {
        // When
        Set<String> results = knowledgeExtractor.checkExistence(Collections.emptyList());

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void checkExistence_nullList_returnsEmptySet() {
        // When
        Set<String> results = knowledgeExtractor.checkExistence(null);

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void extractWithQualification_nullFieldValues_handlesGracefully() {
        // Given
        KnowledgeArticle articleWithNulls = KnowledgeArticle.builder()
            .articleId("KA000001")
            .title(null)
            .content(null)
            .status(null)
            .build();
        List<KnowledgeArticle> expectedList = List.of(articleWithNulls);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<KnowledgeArticle> results = knowledgeExtractor.extractWithQualification(null);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isNull();
    }

    @Test
    void extractPublishedArticles_emptyResult_returnsEmptyList() {
        // Given
        when(mockArContext.executeWithRetry(any())).thenReturn(Collections.emptyList());

        // When
        List<KnowledgeArticle> results = knowledgeExtractor.extractPublishedArticles(1672531200L);

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void extractModifiedSince_zeroTimestamp_returnsAll() {
        // Given
        KnowledgeArticle mockArticle = KnowledgeArticle.builder()
            .articleId("KA000001")
            .title("Article")
            .build();
        List<KnowledgeArticle> expectedList = List.of(mockArticle);

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedList);

        // When
        List<KnowledgeArticle> results = knowledgeExtractor.extractModifiedSince(0L);

        // Then
        assertThat(results).hasSize(1);
    }

}
