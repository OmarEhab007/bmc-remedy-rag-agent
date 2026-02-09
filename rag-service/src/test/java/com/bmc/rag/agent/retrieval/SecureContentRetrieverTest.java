package com.bmc.rag.agent.retrieval;

import com.bmc.rag.agent.config.RagConfig;
import com.bmc.rag.agent.security.ReBACFilter;
import com.bmc.rag.agent.retrieval.SecureContentRetriever.RetrievalResult;
import com.bmc.rag.agent.retrieval.SecureContentRetriever.RetrievedDocument;
import com.bmc.rag.agent.retrieval.SecureContentRetriever.UserContext;
import com.bmc.rag.agent.util.ArabicTextProcessor;
import com.bmc.rag.store.service.VectorStoreService;
import com.bmc.rag.store.service.VectorStoreService.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SecureContentRetrieverTest {

    @Mock
    private VectorStoreService vectorStoreService;

    @Mock
    private ReBACFilter rebacFilter;

    @Mock
    private RagConfig ragConfig;

    @Mock
    private QueryRewriter queryRewriter;

    @Mock
    private ArabicTextProcessor arabicTextProcessor;

    private SecureContentRetriever retriever;

    @BeforeEach
    void setUp() {
        retriever = new SecureContentRetriever(
            vectorStoreService, rebacFilter, ragConfig, queryRewriter, arabicTextProcessor
        );

        // Default: no query rewriting
        lenient().when(queryRewriter.rewrite(anyString())).thenAnswer(inv -> {
            String q = inv.getArgument(0);
            return new QueryRewriter.RewriteResult(q, q, false, List.of());
        });

        // Default: no Arabic detected
        lenient().when(arabicTextProcessor.containsArabic(anyString())).thenReturn(false);

        // Default config
        lenient().when(ragConfig.getMaxResults()).thenReturn(5);
        lenient().when(ragConfig.getMinScore()).thenReturn(0.5f);
        lenient().when(ragConfig.isRebacEnabled()).thenReturn(false);
        lenient().when(ragConfig.isPrioritizeKnowledgeArticles()).thenReturn(false);
    }

    @Nested
    @DisplayName("Query Validation")
    class QueryValidation {

        @Test
        void retrieve_nullQuery_throwsIllegalArgument() {
            assertThatThrownBy(() -> retriever.retrieve(null, UserContext.anonymous()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or empty");
        }

        @Test
        void retrieve_emptyQuery_throwsIllegalArgument() {
            assertThatThrownBy(() -> retriever.retrieve("", UserContext.anonymous()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or empty");
        }

        @Test
        void retrieve_blankQuery_throwsIllegalArgument() {
            assertThatThrownBy(() -> retriever.retrieve("   ", UserContext.anonymous()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or empty");
        }

        @Test
        void retrieve_queryExceedsMaxLength_throwsIllegalArgument() {
            String longQuery = "a".repeat(10001);
            assertThatThrownBy(() -> retriever.retrieve(longQuery, UserContext.anonymous()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds maximum length");
        }

        @Test
        void retrieve_queryAtMaxLength_doesNotThrow() {
            String maxQuery = "a".repeat(10000);
            when(vectorStoreService.search(anyString(), anyInt(), anyFloat()))
                .thenReturn(Collections.emptyList());

            var result = retriever.retrieve(maxQuery, UserContext.anonymous());
            assertThat(result.isEmpty()).isTrue();
        }
    }

    @Nested
    @DisplayName("UserContext")
    class UserContextTests {

        @Test
        void anonymous_hasNoGroups() {
            UserContext anon = UserContext.anonymous();
            assertThat(anon.userId()).isNull();
            assertThat(anon.groups()).isEmpty();
            assertThat(anon.hasGroups()).isFalse();
        }

        @Test
        void withGroups_setsGroupsCorrectly() {
            UserContext ctx = UserContext.withGroups("user1", "Group A", "Group B");
            assertThat(ctx.userId()).isEqualTo("user1");
            assertThat(ctx.groups()).containsExactlyInAnyOrder("Group A", "Group B");
            assertThat(ctx.hasGroups()).isTrue();
        }

        @Test
        void getGroupsAsList_convertsSetToList() {
            UserContext ctx = new UserContext("user1", Set.of("G1", "G2"));
            assertThat(ctx.getGroupsAsList()).containsExactlyInAnyOrder("G1", "G2");
        }

        @Test
        void getGroupsAsList_nullGroups_returnsEmptyList() {
            UserContext ctx = new UserContext("user1", null);
            assertThat(ctx.getGroupsAsList()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Retrieval Flow - No ReBAC")
    class RetrievalFlowNoRebac {

        @Test
        void retrieve_noResults_returnsEmpty() {
            when(vectorStoreService.search("VPN issue", 5, 0.5f))
                .thenReturn(Collections.emptyList());

            var result = retriever.retrieve("VPN issue", UserContext.anonymous());

            assertThat(result.isEmpty()).isTrue();
            assertThat(result.formattedContext()).isEmpty();
        }

        @Test
        void retrieve_withResults_buildsFormattedContext() {
            SearchResult sr = SearchResult.builder()
                .sourceType("Incident").sourceId("INC000001")
                .chunkType("resolution").textSegment("Reset password via AD")
                .metadata(Map.of("title", "Password Reset", "category", "IT"))
                .score(0.85f).build();

            when(vectorStoreService.search("password reset", 5, 0.5f))
                .thenReturn(List.of(sr));
            when(rebacFilter.prioritizeHighValueChunks(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));
            when(rebacFilter.deduplicateBySource(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));

            var result = retriever.retrieve("password reset", UserContext.anonymous());

            assertThat(result.isEmpty()).isFalse();
            assertThat(result.size()).isEqualTo(1);
            assertThat(result.formattedContext()).contains("Incident");
            assertThat(result.formattedContext()).contains("INC000001");
            assertThat(result.formattedContext()).contains("Password Reset");
            assertThat(result.formattedContext()).contains("Reset password via AD");
        }

        @Test
        void retrieve_appliesPrioritizationAndDedup() {
            SearchResult sr1 = SearchResult.builder()
                .sourceType("Incident").sourceId("INC001")
                .textSegment("content1").score(0.9f).build();
            SearchResult sr2 = SearchResult.builder()
                .sourceType("Incident").sourceId("INC001")
                .textSegment("content2").score(0.8f).build();

            when(vectorStoreService.search(anyString(), anyInt(), anyFloat()))
                .thenReturn(List.of(sr1, sr2));
            when(rebacFilter.prioritizeHighValueChunks(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));
            when(rebacFilter.deduplicateBySource(anyList()))
                .thenReturn(List.of(sr1)); // Dedup removes sr2

            var result = retriever.retrieve("test", UserContext.anonymous());

            assertThat(result.size()).isEqualTo(1);
            verify(rebacFilter).prioritizeHighValueChunks(anyList());
            verify(rebacFilter).deduplicateBySource(anyList());
        }

        @Test
        void retrieve_limitsToMaxResults() {
            when(ragConfig.getMaxResults()).thenReturn(2);

            List<SearchResult> manyResults = List.of(
                SearchResult.builder().sourceType("INC").sourceId("INC001")
                    .textSegment("c1").score(0.9f).build(),
                SearchResult.builder().sourceType("INC").sourceId("INC002")
                    .textSegment("c2").score(0.8f).build(),
                SearchResult.builder().sourceType("INC").sourceId("INC003")
                    .textSegment("c3").score(0.7f).build()
            );

            when(vectorStoreService.search(anyString(), anyInt(), anyFloat()))
                .thenReturn(manyResults);
            when(rebacFilter.prioritizeHighValueChunks(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));
            when(rebacFilter.deduplicateBySource(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));

            var result = retriever.retrieve("test", UserContext.anonymous());

            assertThat(result.size()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Retrieval Flow - With ReBAC")
    class RetrievalFlowWithRebac {

        @Test
        void retrieve_rebacEnabled_usesSearchWithGroups() {
            when(ragConfig.isRebacEnabled()).thenReturn(true);
            UserContext ctx = UserContext.withGroups("user1", "Service Desk", "IT");

            SearchResult sr = SearchResult.builder()
                .sourceType("Incident").sourceId("INC001")
                .textSegment("content").score(0.85f)
                .metadata(Map.of("assigned_group", "Service Desk"))
                .build();

            when(vectorStoreService.searchWithGroups("VPN issue", 5, 0.5f,
                List.of("Service Desk", "IT")))
                .thenReturn(List.of(sr));
            // Also mock the alternative ordering
            when(vectorStoreService.searchWithGroups(eq("VPN issue"), eq(5), eq(0.5f), anyList()))
                .thenReturn(List.of(sr));

            when(rebacFilter.prioritizeHighValueChunks(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));
            when(rebacFilter.deduplicateBySource(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));

            var result = retriever.retrieve("VPN issue", ctx);

            assertThat(result.isEmpty()).isFalse();
            verify(vectorStoreService).searchWithGroups(eq("VPN issue"), eq(5), eq(0.5f), anyList());
            verify(vectorStoreService, never()).search(anyString(), anyInt(), anyFloat());
        }

        @Test
        void retrieve_rebacEnabled_noGroups_fallsBackToRegularSearch() {
            when(ragConfig.isRebacEnabled()).thenReturn(true);
            UserContext ctx = UserContext.anonymous(); // no groups

            when(vectorStoreService.search("VPN issue", 5, 0.5f))
                .thenReturn(Collections.emptyList());

            retriever.retrieve("VPN issue", ctx);

            verify(vectorStoreService).search("VPN issue", 5, 0.5f);
            verify(vectorStoreService, never()).searchWithGroups(anyString(), anyInt(), anyFloat(), anyList());
        }

        @Test
        void retrieve_rebacDisabled_usesRegularSearch() {
            when(ragConfig.isRebacEnabled()).thenReturn(false);
            UserContext ctx = UserContext.withGroups("user1", "IT");

            when(vectorStoreService.search("test", 5, 0.5f))
                .thenReturn(Collections.emptyList());

            retriever.retrieve("test", ctx);

            verify(vectorStoreService).search("test", 5, 0.5f);
            verify(vectorStoreService, never()).searchWithGroups(anyString(), anyInt(), anyFloat(), anyList());
        }
    }

    @Nested
    @DisplayName("Knowledge Article Prioritization")
    class KnowledgeArticlePrioritization {

        @Test
        void retrieve_prioritizeKA_kaResultsFirst() {
            when(ragConfig.isPrioritizeKnowledgeArticles()).thenReturn(true);

            SearchResult incident = SearchResult.builder()
                .sourceType("Incident").sourceId("INC001")
                .textSegment("incident content").score(0.95f).build();
            SearchResult ka = SearchResult.builder()
                .sourceType("KnowledgeArticle").sourceId("KA001")
                .textSegment("ka content").score(0.85f).build();

            when(vectorStoreService.search(anyString(), anyInt(), anyFloat()))
                .thenReturn(List.of(incident, ka));
            when(rebacFilter.prioritizeHighValueChunks(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));
            when(rebacFilter.deduplicateBySource(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));

            var result = retriever.retrieve("test", UserContext.anonymous());

            // KA should come first even though incident has higher score
            assertThat(result.documents().get(0).sourceType()).isEqualTo("KnowledgeArticle");
            assertThat(result.documents().get(1).sourceType()).isEqualTo("Incident");
        }

        @Test
        void retrieve_dontPrioritizeKA_originalOrder() {
            when(ragConfig.isPrioritizeKnowledgeArticles()).thenReturn(false);

            SearchResult incident = SearchResult.builder()
                .sourceType("Incident").sourceId("INC001")
                .textSegment("incident content").score(0.95f).build();
            SearchResult ka = SearchResult.builder()
                .sourceType("KnowledgeArticle").sourceId("KA001")
                .textSegment("ka content").score(0.85f).build();

            when(vectorStoreService.search(anyString(), anyInt(), anyFloat()))
                .thenReturn(List.of(incident, ka));
            when(rebacFilter.prioritizeHighValueChunks(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));
            when(rebacFilter.deduplicateBySource(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));

            var result = retriever.retrieve("test", UserContext.anonymous());

            // Original order preserved
            assertThat(result.documents().get(0).sourceType()).isEqualTo("Incident");
        }
    }

    @Nested
    @DisplayName("Arabic Query Preprocessing")
    class ArabicQueryPreprocessing {

        @Test
        void retrieve_arabicQuery_appliesArabicProcessing() {
            String arabicQuery = "مشكلة في الشبكة";
            when(arabicTextProcessor.containsArabic(arabicQuery)).thenReturn(true);
            var arabicResult = new ArabicTextProcessor.ProcessedText(
                arabicQuery, "مشكلة في الشبكة المحلية",
                ArabicTextProcessor.Language.ARABIC, true);
            when(arabicTextProcessor.processArabicQuery(arabicQuery)).thenReturn(arabicResult);

            when(vectorStoreService.search(eq("مشكلة في الشبكة المحلية"), anyInt(), anyFloat()))
                .thenReturn(Collections.emptyList());

            retriever.retrieve(arabicQuery, UserContext.anonymous());

            verify(arabicTextProcessor).processArabicQuery(arabicQuery);
            verify(vectorStoreService).search(eq("مشكلة في الشبكة المحلية"), anyInt(), anyFloat());
        }

        @Test
        void retrieve_englishQuery_skipsArabicProcessing() {
            when(arabicTextProcessor.containsArabic("VPN issue")).thenReturn(false);
            when(vectorStoreService.search(anyString(), anyInt(), anyFloat()))
                .thenReturn(Collections.emptyList());

            retriever.retrieve("VPN issue", UserContext.anonymous());

            verify(arabicTextProcessor, never()).processArabicQuery(anyString());
        }
    }

    @Nested
    @DisplayName("Query Rewriting")
    class QueryRewriting {

        @Test
        void retrieve_queryRewriterApplied_usesRewrittenQuery() {
            when(queryRewriter.rewrite("vpn")).thenReturn(
                new QueryRewriter.RewriteResult("vpn", "vpn virtual private network", true,
                    List.of("expanded VPN")));

            when(vectorStoreService.search(eq("vpn virtual private network"), anyInt(), anyFloat()))
                .thenReturn(Collections.emptyList());

            retriever.retrieve("vpn", UserContext.anonymous());

            verify(vectorStoreService).search(eq("vpn virtual private network"), anyInt(), anyFloat());
        }
    }

    @Nested
    @DisplayName("RetrievedDocument record")
    class RetrievedDocumentTests {

        @Test
        void getSourceReference_formatsCorrectly() {
            var doc = new RetrievedDocument("Incident", "INC000001", "resolution",
                "content", "title", "IT", "Service Desk", 0.85f);

            assertThat(doc.getSourceReference()).isEqualTo("Incident INC000001");
        }

        @Test
        void recordFields_accessible() {
            var doc = new RetrievedDocument("KB", "KB001", "article",
                "Some content", "Article Title", "IT", "Group A", 0.92f);

            assertThat(doc.sourceType()).isEqualTo("KB");
            assertThat(doc.sourceId()).isEqualTo("KB001");
            assertThat(doc.chunkType()).isEqualTo("article");
            assertThat(doc.content()).isEqualTo("Some content");
            assertThat(doc.title()).isEqualTo("Article Title");
            assertThat(doc.category()).isEqualTo("IT");
            assertThat(doc.assignedGroup()).isEqualTo("Group A");
            assertThat(doc.score()).isEqualTo(0.92f);
        }
    }

    @Nested
    @DisplayName("RetrievalResult record")
    class RetrievalResultTests {

        @Test
        void empty_returnsEmptyResult() {
            var result = RetrievalResult.empty();
            assertThat(result.isEmpty()).isTrue();
            assertThat(result.size()).isZero();
            assertThat(result.formattedContext()).isEmpty();
        }

        @Test
        void getSourceReferences_deduplicates() {
            var doc1 = new RetrievedDocument("INC", "INC001", "summary", "c1",
                null, null, null, 0.9f);
            var doc2 = new RetrievedDocument("INC", "INC001", "resolution", "c2",
                null, null, null, 0.8f);

            var result = new RetrievalResult(List.of(doc1, doc2), "context");

            assertThat(result.getSourceReferences()).hasSize(1);
            assertThat(result.getSourceReferences()).contains("INC INC001");
        }

        @Test
        void getDocumentsForCitations_deduplicatesBySourceId_keepsHigherScore() {
            var doc1 = new RetrievedDocument("INC", "INC001", "summary", "c1",
                null, null, null, 0.9f);
            var doc2 = new RetrievedDocument("INC", "INC001", "resolution", "c2",
                null, null, null, 0.8f);

            var result = new RetrievalResult(List.of(doc1, doc2), "context");

            List<RetrievedDocument> citations = result.getDocumentsForCitations();
            assertThat(citations).hasSize(1);
            assertThat(citations.get(0).score()).isEqualTo(0.9f);
        }

        @Test
        void getDocumentsForCitations_sortsByScoreDescending() {
            var doc1 = new RetrievedDocument("INC", "INC001", "summary", "c1",
                null, null, null, 0.7f);
            var doc2 = new RetrievedDocument("KB", "KB001", "article", "c2",
                null, null, null, 0.95f);

            var result = new RetrievalResult(List.of(doc1, doc2), "context");

            List<RetrievedDocument> citations = result.getDocumentsForCitations();
            assertThat(citations.get(0).score()).isGreaterThan(citations.get(1).score());
        }

        @Test
        void getSourceReferences_nullDocuments_returnsEmpty() {
            var result = new RetrievalResult(null, "");
            assertThat(result.getSourceReferences()).isEmpty();
        }

        @Test
        void getDocumentsForCitations_nullDocuments_returnsEmpty() {
            var result = new RetrievalResult(null, "");
            assertThat(result.getDocumentsForCitations()).isEmpty();
        }
    }

    @Nested
    @DisplayName("retrieveBySourceTypes()")
    class RetrieveBySourceTypes {

        @Test
        void retrieveBySourceTypes_filtersAndReturns() {
            SearchResult sr = SearchResult.builder()
                .sourceType("KnowledgeArticle").sourceId("KA001")
                .textSegment("KA content").score(0.8f).build();

            when(vectorStoreService.searchBySourceTypes(
                anyString(), anyInt(), anyFloat(), eq(List.of("KnowledgeArticle"))))
                .thenReturn(List.of(sr));
            when(rebacFilter.filterByGroups(anyList(), anySet()))
                .thenAnswer(inv -> inv.getArgument(0));

            var result = retriever.retrieveBySourceTypes(
                "test", List.of("KnowledgeArticle"), UserContext.anonymous());

            assertThat(result.isEmpty()).isFalse();
            assertThat(result.documents().get(0).sourceType()).isEqualTo("KnowledgeArticle");
        }

        @Test
        void retrieveKnowledgeArticles_delegatesCorrectly() {
            when(vectorStoreService.searchBySourceTypes(
                anyString(), anyInt(), anyFloat(), eq(List.of("KnowledgeArticle"))))
                .thenReturn(Collections.emptyList());

            var result = retriever.retrieveKnowledgeArticles("test", UserContext.anonymous());

            assertThat(result.isEmpty()).isTrue();
            verify(vectorStoreService).searchBySourceTypes(
                anyString(), anyInt(), anyFloat(), eq(List.of("KnowledgeArticle")));
        }

        @Test
        void retrieveIncidents_delegatesCorrectly() {
            when(vectorStoreService.searchBySourceTypes(
                anyString(), anyInt(), anyFloat(), eq(List.of("Incident"))))
                .thenReturn(Collections.emptyList());

            var result = retriever.retrieveIncidents("test", UserContext.anonymous());

            assertThat(result.isEmpty()).isTrue();
            verify(vectorStoreService).searchBySourceTypes(
                anyString(), anyInt(), anyFloat(), eq(List.of("Incident")));
        }
    }
}
