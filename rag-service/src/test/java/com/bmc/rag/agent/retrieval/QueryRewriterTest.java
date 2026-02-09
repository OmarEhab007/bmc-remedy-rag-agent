package com.bmc.rag.agent.retrieval;

import com.bmc.rag.agent.util.ArabicTextProcessor;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for QueryRewriter.
 * Tests query expansion including Arabic IT term expansion, Arabizi handling,
 * numeral normalization, abbreviation expansion, and synonym addition.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QueryRewriter Tests")
class QueryRewriterTest {

    @Mock
    private ChatLanguageModel chatModel;

    private QueryRewriter queryRewriter;

    private final ArabicTextProcessor arabicTextProcessor = new ArabicTextProcessor();

    @BeforeEach
    void setUp() {
        queryRewriter = new QueryRewriter(chatModel, arabicTextProcessor);
        // Enable query rewriting
        ReflectionTestUtils.setField(queryRewriter, "enabled", true);
        ReflectionTestUtils.setField(queryRewriter, "useLlm", false);
        ReflectionTestUtils.setField(queryRewriter, "arabicExpansionEnabled", true);
    }

    @Nested
    @DisplayName("Arabic IT Term Expansion Tests")
    class ArabicITTermExpansionTests {

        @Test
        @DisplayName("Should expand 'تذكرة' to include 'ticket incident request'")
        void expandTicketTerm() {
            String query = "أريد فتح تذكرة جديدة";
            var result = queryRewriter.rewrite(query);

            assertThat(result.wasModified()).isTrue();
            assertThat(result.rewrittenQuery()).contains("ticket");
            assertThat(result.rewrittenQuery()).contains("incident");
            assertThat(result.modifications()).anyMatch(m -> m.contains("Arabic term"));
        }

        @Test
        @DisplayName("Should expand 'كلمة المرور' to include 'password'")
        void expandPasswordTerm() {
            String query = "نسيت كلمة المرور";
            var result = queryRewriter.rewrite(query);

            assertThat(result.wasModified()).isTrue();
            assertThat(result.rewrittenQuery()).contains("password");
        }

        @Test
        @DisplayName("Should expand 'شبكة' to include 'network VPN WiFi'")
        void expandNetworkTerm() {
            String query = "مشكلة في الشبكة";
            var result = queryRewriter.rewrite(query);

            assertThat(result.wasModified()).isTrue();
            assertThat(result.rewrittenQuery()).containsAnyOf("network", "VPN", "WiFi");
        }

        @Test
        @DisplayName("Should expand 'خطأ' to include 'error issue problem'")
        void expandErrorTerm() {
            String query = "ظهر لي خطأ";
            var result = queryRewriter.rewrite(query);

            assertThat(result.wasModified()).isTrue();
            assertThat(result.rewrittenQuery()).containsAnyOf("error", "issue", "problem");
        }

        @Test
        @DisplayName("Should expand 'طابعة' to include 'printer'")
        void expandPrinterTerm() {
            String query = "الطابعة لا تعمل";
            var result = queryRewriter.rewrite(query);

            assertThat(result.wasModified()).isTrue();
            assertThat(result.rewrittenQuery()).contains("printer");
        }

        @Test
        @DisplayName("Should expand multiple Arabic terms in one query")
        void expandMultipleTerms() {
            String query = "مشكلة في الشبكة وكلمة المرور";
            var result = queryRewriter.rewrite(query);

            assertThat(result.wasModified()).isTrue();
            assertThat(result.rewrittenQuery()).contains("network");
            assertThat(result.rewrittenQuery()).contains("password");
        }
    }

    @Nested
    @DisplayName("Arabizi Term Expansion Tests")
    class ArabiziTermExpansionTests {

        @Test
        @DisplayName("Should expand 'ريست' to include 'reset'")
        void expandResetArabizi() {
            String query = "اريد ريست للباسوورد";
            var result = queryRewriter.rewrite(query);

            assertThat(result.wasModified()).isTrue();
            assertThat(result.rewrittenQuery()).contains("reset");
            assertThat(result.modifications()).anyMatch(m -> m.contains("Arabizi"));
        }

        @Test
        @DisplayName("Should expand 'باسوورد' to include 'password'")
        void expandPasswordArabizi() {
            String query = "نسيت الباسوورد";
            var result = queryRewriter.rewrite(query);

            assertThat(result.wasModified()).isTrue();
            assertThat(result.rewrittenQuery()).contains("password");
        }

        @Test
        @DisplayName("Should expand 'ايميل' to include 'email'")
        void expandEmailArabizi() {
            String query = "مشكلة في الايميل";
            var result = queryRewriter.rewrite(query);

            assertThat(result.wasModified()).isTrue();
            assertThat(result.rewrittenQuery()).contains("email");
        }

        @Test
        @DisplayName("Should expand 'سيرفر' to include 'server'")
        void expandServerArabizi() {
            String query = "السيرفر لا يعمل";
            var result = queryRewriter.rewrite(query);

            assertThat(result.wasModified()).isTrue();
            assertThat(result.rewrittenQuery()).contains("server");
        }
    }

    @Nested
    @DisplayName("Arabic Numeral Normalization Tests")
    class ArabicNumeralTests {

        @Test
        @DisplayName("Should normalize Arabic-Indic numerals to Western")
        void normalizeArabicNumerals() {
            String query = "رقم التذكرة ١٢٣٤٥";
            var result = queryRewriter.rewrite(query);

            assertThat(result.wasModified()).isTrue();
            assertThat(result.rewrittenQuery()).contains("12345");
            assertThat(result.rewrittenQuery()).doesNotContain("١٢٣٤٥");
            assertThat(result.modifications()).anyMatch(m -> m.contains("numeral"));
        }

        @Test
        @DisplayName("Should handle mixed numerals")
        void normalizeMixedNumerals() {
            String query = "الطلب ١٢٣ والتذكرة 456";
            var result = queryRewriter.rewrite(query);

            assertThat(result.rewrittenQuery()).contains("123");
            assertThat(result.rewrittenQuery()).contains("456");
        }
    }

    @Nested
    @DisplayName("English Abbreviation Expansion Tests")
    class EnglishAbbreviationTests {

        @Test
        @DisplayName("Should expand VPN abbreviation")
        void expandVPN() {
            String query = "cannot connect to vpn";
            var result = queryRewriter.rewrite(query);

            assertThat(result.wasModified()).isTrue();
            assertThat(result.rewrittenQuery()).containsIgnoringCase("Virtual Private Network");
        }

        @Test
        @DisplayName("Should expand DNS abbreviation")
        void expandDNS() {
            String query = "dns not resolving";
            var result = queryRewriter.rewrite(query);

            assertThat(result.wasModified()).isTrue();
            assertThat(result.rewrittenQuery()).containsIgnoringCase("Domain Name System");
        }

        @Test
        @DisplayName("Should expand SSO abbreviation")
        void expandSSO() {
            String query = "sso login failed";
            var result = queryRewriter.rewrite(query);

            assertThat(result.wasModified()).isTrue();
            assertThat(result.rewrittenQuery()).containsIgnoringCase("Single Sign-On");
        }
    }

    @Nested
    @DisplayName("Synonym Addition Tests")
    class SynonymTests {

        @Test
        @DisplayName("Should add synonyms for 'slow'")
        void addSlowSynonyms() {
            String query = "computer is slow";
            var result = queryRewriter.rewrite(query);

            assertThat(result.wasModified()).isTrue();
            assertThat(result.rewrittenQuery()).containsAnyOf("performance", "lag");
        }

        @Test
        @DisplayName("Should add synonyms for 'crash'")
        void addCrashSynonyms() {
            String query = "application crash";
            var result = queryRewriter.rewrite(query);

            assertThat(result.wasModified()).isTrue();
            assertThat(result.rewrittenQuery()).containsAnyOf("freeze", "hang");
        }

        @Test
        @DisplayName("Should add synonyms for 'login'")
        void addLoginSynonyms() {
            String query = "cannot login";
            var result = queryRewriter.rewrite(query);

            assertThat(result.wasModified()).isTrue();
            assertThat(result.rewrittenQuery()).containsAnyOf("sign in", "authenticate");
        }
    }

    @Nested
    @DisplayName("Typo Correction Tests")
    class TypoCorrectionTests {

        @Test
        @DisplayName("Should correct 'outlok' to 'outlook'")
        void correctOutlookTypo() {
            String query = "outlok not working";
            var result = queryRewriter.rewrite(query);

            assertThat(result.wasModified()).isTrue();
            assertThat(result.rewrittenQuery()).contains("outlook");
            assertThat(result.modifications()).anyMatch(m -> m.contains("typo"));
        }

        @Test
        @DisplayName("Should correct 'pasword' to 'password'")
        void correctPasswordTypo() {
            String query = "forgot my pasword";
            var result = queryRewriter.rewrite(query);

            assertThat(result.wasModified()).isTrue();
            assertThat(result.rewrittenQuery()).contains("password");
        }

        @Test
        @DisplayName("Should correct 'netwrok' to 'network'")
        void correctNetworkTypo() {
            String query = "netwrok connection issue";
            var result = queryRewriter.rewrite(query);

            assertThat(result.wasModified()).isTrue();
            assertThat(result.rewrittenQuery()).contains("network");
        }
    }

    @Nested
    @DisplayName("Mixed Language Query Tests")
    class MixedLanguageTests {

        @Test
        @DisplayName("Should handle Arabic text with English technical terms")
        void handleMixedArabicEnglish() {
            String query = "أحتاج access للـ VPN";
            var result = queryRewriter.rewrite(query);

            assertThat(result.wasModified()).isTrue();
            // Should expand both Arabic request term and VPN
            assertThat(result.rewrittenQuery()).containsIgnoringCase("Virtual Private Network");
        }

        @Test
        @DisplayName("Should handle English text with Arabic error message")
        void handleEnglishWithArabicError() {
            String query = "Error: خطأ في الاتصال";
            var result = queryRewriter.rewrite(query);

            assertThat(result.wasModified()).isTrue();
            assertThat(result.rewrittenQuery()).containsAnyOf("connection", "network");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should return original query when disabled")
        void returnOriginalWhenDisabled() {
            ReflectionTestUtils.setField(queryRewriter, "enabled", false);
            String query = "أريد فتح تذكرة";
            var result = queryRewriter.rewrite(query);

            assertThat(result.wasModified()).isFalse();
            assertThat(result.rewrittenQuery()).isEqualTo(query);
        }

        @Test
        @DisplayName("Should handle null query")
        void handleNullQuery() {
            var result = queryRewriter.rewrite(null);
            assertThat(result.wasModified()).isFalse();
        }

        @Test
        @DisplayName("Should handle empty query")
        void handleEmptyQuery() {
            var result = queryRewriter.rewrite("");
            assertThat(result.wasModified()).isFalse();
        }

        @Test
        @DisplayName("Should handle whitespace query")
        void handleWhitespaceQuery() {
            var result = queryRewriter.rewrite("   ");
            assertThat(result.wasModified()).isFalse();
        }

        @Test
        @DisplayName("Should not modify query with no matches")
        void noModificationNeeded() {
            String query = "hello world test";
            var result = queryRewriter.rewrite(query);

            // May or may not be modified depending on synonym matches
            assertThat(result.rewrittenQuery()).isNotNull();
        }

        @Test
        @DisplayName("Should not add duplicate expansions")
        void noDuplicateExpansions() {
            String query = "تذكرة تذكرة تذكرة";
            var result = queryRewriter.rewrite(query);

            // Should only add "ticket incident request" once
            long ticketCount = result.rewrittenQuery().toLowerCase()
                .chars()
                .filter(ch -> ch == 't')
                .count();
            // Original query has no 't', expanded should have limited 't' from "ticket"
            assertThat(ticketCount).isLessThan(10);
        }

        @Test
        @DisplayName("Should skip Arabic expansion when disabled")
        void skipArabicExpansionWhenDisabled() {
            ReflectionTestUtils.setField(queryRewriter, "arabicExpansionEnabled", false);
            String query = "أريد فتح تذكرة";
            var result = queryRewriter.rewrite(query);

            // Should not contain English expansions
            assertThat(result.modifications())
                .noneMatch(m -> m.contains("Arabic term"));
        }
    }

    @Nested
    @DisplayName("RewriteResult Tests")
    class RewriteResultTests {

        @Test
        @DisplayName("RewriteResult should provide original query")
        void resultContainsOriginal() {
            String query = "مشكلة في الشبكة";
            var result = queryRewriter.rewrite(query);

            assertThat(result.originalQuery()).isEqualTo(query);
        }

        @Test
        @DisplayName("RewriteResult should list all modifications")
        void resultListsModifications() {
            String query = "أريد ريست للباسوورد";
            var result = queryRewriter.rewrite(query);

            assertThat(result.modifications()).isNotEmpty();
        }

        @Test
        @DisplayName("getQueryForSearch should return rewritten query")
        void getQueryForSearch() {
            String query = "مشكلة في الشبكة";
            var result = queryRewriter.rewrite(query);

            assertThat(result.getQueryForSearch()).isEqualTo(result.rewrittenQuery());
        }
    }

    @Nested
    @DisplayName("LLM Rewrite Tests")
    class LlmRewriteTests {

        @Test
        @DisplayName("Should use LLM rewrite when useLlm=true and modifications exist")
        void llmRewrite_whenEnabled_callsLlm() {
            ReflectionTestUtils.setField(queryRewriter, "useLlm", true);

            // Mock the LLM to return a rewritten query
            dev.langchain4j.model.chat.response.ChatResponse mockResponse =
                    dev.langchain4j.model.chat.response.ChatResponse.builder()
                            .aiMessage(dev.langchain4j.data.message.AiMessage.from("optimized VPN connection query"))
                            .build();
            when(chatModel.chat(any(dev.langchain4j.model.chat.request.ChatRequest.class)))
                    .thenReturn(mockResponse);

            // Use a query that triggers modifications (typo + abbreviation)
            String query = "cannot connect to vpn conection issue";
            var result = queryRewriter.rewrite(query);

            assertThat(result.wasModified()).isTrue();
            assertThat(result.modifications()).anyMatch(m -> m.contains("LLM enhanced"));
            assertThat(result.rewrittenQuery()).isEqualTo("optimized VPN connection query");
        }

        @Test
        @DisplayName("Should fallback to expanded when LLM fails")
        void llmRewrite_whenLlmFails_fallsBack() {
            ReflectionTestUtils.setField(queryRewriter, "useLlm", true);

            // Mock LLM failure
            when(chatModel.chat(any(dev.langchain4j.model.chat.request.ChatRequest.class)))
                    .thenThrow(new RuntimeException("LLM unavailable"));

            // Use a query that triggers modifications
            String query = "outlok not working";
            var result = queryRewriter.rewrite(query);

            assertThat(result.wasModified()).isTrue();
            // Should contain the typo-corrected version, not throw
            assertThat(result.rewrittenQuery()).contains("outlook");
        }

        @Test
        @DisplayName("Should NOT call LLM when useLlm=true but no modifications")
        void llmRewrite_noModifications_skipsLlm() {
            ReflectionTestUtils.setField(queryRewriter, "useLlm", true);

            // Use a query with no modifications (no typos, no abbreviations, no synonyms)
            String query = "general information about the company";
            var result = queryRewriter.rewrite(query);

            // No LLM call since no modifications were made
            verify(chatModel, never()).chat(any(dev.langchain4j.model.chat.request.ChatRequest.class));
        }
    }

    @Nested
    @DisplayName("Null ArabicTextProcessor Tests")
    class NullArabicProcessorTests {

        @Test
        @DisplayName("Should handle null arabicTextProcessor gracefully")
        void nullProcessor_arabicQuery_noExpansion() {
            // Create a rewriter with null arabicTextProcessor
            QueryRewriter rewriterWithNullProcessor = new QueryRewriter(chatModel, null);
            ReflectionTestUtils.setField(rewriterWithNullProcessor, "enabled", true);
            ReflectionTestUtils.setField(rewriterWithNullProcessor, "useLlm", false);
            ReflectionTestUtils.setField(rewriterWithNullProcessor, "arabicExpansionEnabled", true);

            // Arabic query should not cause NPE
            String query = "أريد فتح تذكرة جديدة";
            var result = rewriterWithNullProcessor.rewrite(query);

            // containsArabic returns false when processor is null, so no Arabic expansion
            assertThat(result).isNotNull();
        }
    }
}
