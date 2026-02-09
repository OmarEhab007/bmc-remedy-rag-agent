package com.bmc.rag.agent.damee;

import com.bmc.rag.agent.damee.ServiceIntentMatcher.ServiceMatchResult;
import com.bmc.rag.agent.damee.ServiceIntentMatcher.ServiceMatchResult.MatchType;
import com.bmc.rag.store.service.VectorStoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ServiceIntentMatcher}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ServiceIntentMatcherTest {

    @Mock
    private VectorStoreService vectorStoreService;

    @Mock
    private DameeServiceCatalog catalog;

    @InjectMocks
    private ServiceIntentMatcher matcher;

    private DameeService vpnService;
    private DameeService emailService;
    private DameeService incidentService;

    @BeforeEach
    void setUp() {
        vpnService = DameeService.builder()
                .serviceId("10513")
                .nameEn("Virtual Private Network Request")
                .nameAr("طلب الشبكة الخاصة الافتراضية")
                .descriptionEn("VPN Request for remote access")
                .category("IT Services")
                .keywords(List.of("vpn", "remote", "شبكة افتراضية"))
                .workflow(List.of(DameeService.WorkflowStep.builder()
                        .order(1)
                        .description("Fill Form → Manager Approval → Network Team → End")
                        .build()))
                .build();

        emailService = DameeService.builder()
                .serviceId("10242")
                .nameEn("Personal Email Management")
                .nameAr("إدارة البريد الإلكتروني الشخصي")
                .descriptionEn("Manage email storage")
                .category("IT Services")
                .keywords(List.of("email", "mailbox", "بريد"))
                .workflow(List.of())
                .build();

        incidentService = DameeService.builder()
                .serviceId("10101")
                .nameEn("Technical Incident")
                .nameAr("بلاغ عن مشكلة تقنية")
                .descriptionEn("Report technical issues")
                .category("IT Services")
                .keywords(List.of("incident", "problem", "مشكلة"))
                .workflow(List.of())
                .build();
    }

    @Test
    void matchService_nullQuery_returnsNoMatch() {
        // When: match with null query
        ServiceMatchResult result = matcher.matchService(null);

        // Then: returns no match
        assertThat(result.getType()).isEqualTo(MatchType.NO_MATCH);
        assertThat(result.getMessage()).contains("Empty query");
    }

    @Test
    void matchService_blankQuery_returnsNoMatch() {
        // When: match with blank query
        ServiceMatchResult result = matcher.matchService("   ");

        // Then: returns no match
        assertThat(result.getType()).isEqualTo(MatchType.NO_MATCH);
    }

    @Test
    void matchService_problemLanguage_returnsNoMatch() {
        // When: user reports a problem
        ServiceMatchResult result = matcher.matchService("I can't access my email");

        // Then: returns no match (problem language detected)
        assertThat(result.getType()).isEqualTo(MatchType.NO_MATCH);
        assertThat(result.getMessage()).contains("problem");
    }

    @Test
    void matchService_problemLanguageVariations_returnsNoMatch() {
        // Test various problem phrases
        String[] problemPhrases = {
                "not working",
                "doesn't work",
                "unable to login",
                "failed to connect",
                "error message",
                "help me fix this",
                "broken system",
                "crashed server"
        };

        for (String phrase : problemPhrases) {
            // When: match with problem phrase
            ServiceMatchResult result = matcher.matchService(phrase);

            // Then: returns no match
            assertThat(result.getType()).isEqualTo(MatchType.NO_MATCH);
        }
    }

    @Test
    void matchService_vpnPattern_returnsExactMatch() {
        // Given: VPN service exists
        when(catalog.getById("10513")).thenReturn(vpnService);

        // When: match VPN query
        ServiceMatchResult result = matcher.matchService("I need VPN access");

        // Then: returns exact match
        assertThat(result.getType()).isEqualTo(MatchType.EXACT);
        assertThat(result.getService()).isEqualTo(vpnService);
        assertThat(result.getConfidence()).isEqualTo(1.0);
    }

    @Test
    void matchService_vpnPatternArabic_returnsExactMatch() {
        // Given: VPN service exists
        when(catalog.getById("10513")).thenReturn(vpnService);

        // When: match Arabic VPN query
        ServiceMatchResult result = matcher.matchService("أريد شبكة افتراضية");

        // Then: returns exact match
        assertThat(result.getType()).isEqualTo(MatchType.EXACT);
    }

    @Test
    void matchService_emailPattern_returnsExactMatch() {
        // Given: Email service exists
        when(catalog.getById("10242")).thenReturn(emailService);

        // When: match email query
        ServiceMatchResult result = matcher.matchService("I need more email storage");

        // Then: returns exact match
        assertThat(result.getType()).isEqualTo(MatchType.EXACT);
        assertThat(result.getService()).isEqualTo(emailService);
    }

    @Test
    void matchService_softwarePattern_returnsExactMatch() {
        // Given: Software installation service exists
        DameeService softwareService = DameeService.builder()
                .serviceId("10247")
                .nameEn("Software Installation")
                .build();
        when(catalog.getById("10247")).thenReturn(softwareService);

        // When: match software query
        ServiceMatchResult result = matcher.matchService("Install software");

        // Then: returns exact match
        assertThat(result.getType()).isEqualTo(MatchType.EXACT);
    }

    @Test
    void matchService_databasePattern_returnsExactMatch() {
        // Given: Database service exists
        DameeService dbService = DameeService.builder()
                .serviceId("10503")
                .nameEn("Database Access")
                .build();
        when(catalog.getById("10503")).thenReturn(dbService);

        // When: match database query
        ServiceMatchResult result = matcher.matchService("Need Oracle database access");

        // Then: returns exact match
        assertThat(result.getType()).isEqualTo(MatchType.EXACT);
    }

    @Test
    void matchService_highConfidenceKeywordMatch_returnsHighConfidence() {
        // Given: high-confidence keyword match
        vpnService.setScore(9.0);
        when(catalog.searchByKeyword(anyString(), anyInt())).thenReturn(List.of(vpnService));

        // When: match query
        ServiceMatchResult result = matcher.matchService("remote access request");

        // Then: returns high confidence
        assertThat(result.getType()).isEqualTo(MatchType.HIGH_CONFIDENCE);
        assertThat(result.getService()).isEqualTo(vpnService);
    }

    @Test
    void matchService_lowKeywordScore_fallsBackToSemanticSearch() {
        // Given: low keyword score, semantic match available
        vpnService.setScore(5.0);
        when(catalog.searchByKeyword(anyString(), anyInt())).thenReturn(List.of(vpnService));

        Map<String, String> metadata = new HashMap<>();
        metadata.put("service_id", "10513");

        VectorStoreService.SearchResult semanticResult = VectorStoreService.SearchResult.builder()
                .score(0.88f)
                .metadata(metadata)
                .build();

        when(vectorStoreService.searchByType(anyString(), eq("DAMEE_SERVICE"), anyInt(), anyDouble()))
                .thenReturn(List.of(semanticResult));
        when(catalog.getById("10513")).thenReturn(vpnService);

        // When: match query with neutral text
        ServiceMatchResult result = matcher.matchService("need configuration assistance");

        // Then: uses semantic search
        assertThat(result.getType()).isEqualTo(MatchType.HIGH_CONFIDENCE);
    }

    @Test
    void matchService_semanticSearchHighScore_returnsHighConfidence() {
        // Given: no keyword match, high semantic score
        when(catalog.searchByKeyword(anyString(), anyInt())).thenReturn(List.of());

        Map<String, String> metadata = new HashMap<>();
        metadata.put("service_id", "10242");

        VectorStoreService.SearchResult semanticResult = VectorStoreService.SearchResult.builder()
                .score(0.90f)
                .metadata(metadata)
                .build();

        when(vectorStoreService.searchByType(anyString(), eq("DAMEE_SERVICE"), anyInt(), anyDouble()))
                .thenReturn(List.of(semanticResult));

        emailService.setScore(0.90);
        when(catalog.getById("10242")).thenReturn(emailService);

        // When: match query with generic text
        ServiceMatchResult result = matcher.matchService("require additional allocation");

        // Then: returns high confidence
        assertThat(result.getType()).isEqualTo(MatchType.HIGH_CONFIDENCE);
        assertThat(result.getService().getScore()).isGreaterThan(0.85);
    }

    @Test
    void matchService_multipleSemanticMatches_needsClarification() {
        // Given: multiple semantic matches
        when(catalog.searchByKeyword(anyString(), anyInt())).thenReturn(List.of());

        Map<String, String> metadata1 = new HashMap<>();
        metadata1.put("service_id", "10513");
        Map<String, String> metadata2 = new HashMap<>();
        metadata2.put("service_id", "10242");

        VectorStoreService.SearchResult result1 = VectorStoreService.SearchResult.builder()
                .score(0.70f)
                .metadata(metadata1)
                .build();
        VectorStoreService.SearchResult result2 = VectorStoreService.SearchResult.builder()
                .score(0.68f)
                .metadata(metadata2)
                .build();

        when(vectorStoreService.searchByType(anyString(), eq("DAMEE_SERVICE"), anyInt(), anyDouble()))
                .thenReturn(List.of(result1, result2));

        vpnService.setScore(0.70);
        emailService.setScore(0.68);
        when(catalog.getById("10513")).thenReturn(vpnService);
        when(catalog.getById("10242")).thenReturn(emailService);

        // When: match query with vague neutral text
        ServiceMatchResult result = matcher.matchService("need general assistance");

        // Then: needs clarification
        assertThat(result.getType()).isEqualTo(MatchType.CLARIFICATION);
        assertThat(result.getMatches()).hasSize(2);
    }

    @Test
    void matchService_noSemanticMatches_returnsNoMatch() {
        // Given: no matches at all
        when(catalog.searchByKeyword(anyString(), anyInt())).thenReturn(List.of());
        when(vectorStoreService.searchByType(anyString(), eq("DAMEE_SERVICE"), anyInt(), anyDouble()))
                .thenReturn(List.of());

        // When: match query
        ServiceMatchResult result = matcher.matchService("completely unrelated query");

        // Then: returns no match
        assertThat(result.getType()).isEqualTo(MatchType.NO_MATCH);
    }

    @Test
    void matchService_deduplicatesByServiceId_success() {
        // Given: same service appears in multiple chunks
        when(catalog.searchByKeyword(anyString(), anyInt())).thenReturn(List.of());

        Map<String, String> metadata1 = new HashMap<>();
        metadata1.put("service_id", "10513");
        Map<String, String> metadata2 = new HashMap<>();
        metadata2.put("service_id", "10513");

        VectorStoreService.SearchResult result1 = VectorStoreService.SearchResult.builder()
                .score(0.90f)
                .metadata(metadata1)
                .build();
        VectorStoreService.SearchResult result2 = VectorStoreService.SearchResult.builder()
                .score(0.85f)
                .metadata(metadata2)
                .build();

        when(vectorStoreService.searchByType(anyString(), eq("DAMEE_SERVICE"), anyInt(), anyDouble()))
                .thenReturn(List.of(result1, result2));

        vpnService.setScore(0.90);
        when(catalog.getById("10513")).thenReturn(vpnService);

        // When: match query
        ServiceMatchResult result = matcher.matchService("vpn access");

        // Then: deduplicates and keeps highest score
        assertThat(result.getService().getScore()).isEqualTo(0.90);
    }

    @Test
    void matchService_singleModerateSemanticMatch_returnsHighConfidence() {
        // Given: single semantic match with moderate score
        when(catalog.searchByKeyword(anyString(), anyInt())).thenReturn(List.of());

        Map<String, String> metadata = new HashMap<>();
        metadata.put("service_id", "10513");

        VectorStoreService.SearchResult semanticResult = VectorStoreService.SearchResult.builder()
                .score(0.75f)
                .metadata(metadata)
                .build();

        when(vectorStoreService.searchByType(anyString(), eq("DAMEE_SERVICE"), anyInt(), anyDouble()))
                .thenReturn(List.of(semanticResult));

        vpnService.setScore(0.75);
        when(catalog.getById("10513")).thenReturn(vpnService);

        // When: match query with neutral text
        ServiceMatchResult result = matcher.matchService("need to configure link");

        // Then: returns high confidence (single match)
        assertThat(result.getType()).isEqualTo(MatchType.HIGH_CONFIDENCE);
    }

    @Test
    void matchService_nullServiceInCatalog_skipsIt() {
        // Given: semantic search returns ID not in catalog
        when(catalog.searchByKeyword(anyString(), anyInt())).thenReturn(List.of());

        Map<String, String> metadata = new HashMap<>();
        metadata.put("service_id", "99999");

        VectorStoreService.SearchResult semanticResult = VectorStoreService.SearchResult.builder()
                .score(0.90f)
                .metadata(metadata)
                .build();

        when(vectorStoreService.searchByType(anyString(), eq("DAMEE_SERVICE"), anyInt(), anyDouble()))
                .thenReturn(List.of(semanticResult));
        when(catalog.getById("99999")).thenReturn(null);

        // When: match query
        ServiceMatchResult result = matcher.matchService("test query");

        // Then: returns no match
        assertThat(result.getType()).isEqualTo(MatchType.NO_MATCH);
    }

    @Test
    void getServiceCategoriesDisplay_returnsCategorySummary_success() {
        // Given: catalog with categories
        when(catalog.getCategorySummary()).thenReturn("Available categories:\n1. IT Services");

        // When: get categories display
        String display = matcher.getServiceCategoriesDisplay();

        // Then: returns formatted summary
        assertThat(display).contains("Available categories");
        assertThat(display).contains("IT Services");
    }

    @Test
    void serviceMatchResult_exact_hasCorrectProperties() {
        // When: create exact match result
        ServiceMatchResult result = ServiceMatchResult.exact(vpnService);

        // Then: has correct properties
        assertThat(result.getType()).isEqualTo(MatchType.EXACT);
        assertThat(result.getService()).isEqualTo(vpnService);
        assertThat(result.getConfidence()).isEqualTo(1.0);
        assertThat(result.isHighConfidence()).isTrue();
        assertThat(result.needsClarification()).isFalse();
        assertThat(result.isNoMatch()).isFalse();
    }

    @Test
    void serviceMatchResult_highConfidence_hasCorrectProperties() {
        // Given: service with score
        vpnService.setScore(0.88);

        // When: create high confidence result
        ServiceMatchResult result = ServiceMatchResult.highConfidence(vpnService);

        // Then: has correct properties
        assertThat(result.getType()).isEqualTo(MatchType.HIGH_CONFIDENCE);
        assertThat(result.getConfidence()).isEqualTo(0.88);
        assertThat(result.isHighConfidence()).isTrue();
    }

    @Test
    void serviceMatchResult_clarification_hasCorrectProperties() {
        // When: create clarification result
        ServiceMatchResult result = ServiceMatchResult.clarificationNeeded(List.of(vpnService, emailService));

        // Then: has correct properties
        assertThat(result.getType()).isEqualTo(MatchType.CLARIFICATION);
        assertThat(result.getMatches()).hasSize(2);
        assertThat(result.needsClarification()).isTrue();
        assertThat(result.isHighConfidence()).isFalse();
    }

    @Test
    void serviceMatchResult_noMatch_hasCorrectProperties() {
        // When: create no match result
        ServiceMatchResult result = ServiceMatchResult.noMatch("No services found");

        // Then: has correct properties
        assertThat(result.getType()).isEqualTo(MatchType.NO_MATCH);
        assertThat(result.getMessage()).isEqualTo("No services found");
        assertThat(result.isNoMatch()).isTrue();
        assertThat(result.getConfidence()).isEqualTo(0);
    }

    @Test
    void serviceMatchResult_getDisplayMessage_exactMatch_formatsCorrectly() {
        // Given: exact match with workflow
        vpnService.setWorkflow(List.of(
                DameeService.WorkflowStep.builder()
                        .order(1)
                        .description("Fill Form → Manager → Network → End")
                        .build()
        ));
        ServiceMatchResult result = ServiceMatchResult.exact(vpnService);

        // When: get display message
        String message = result.getDisplayMessage();

        // Then: contains service details
        assertThat(message).contains("Virtual Private Network Request");
        assertThat(message).contains("10513");
        assertThat(message).contains("VPN Request for remote access");
        assertThat(message).contains("Is this the service you need?");
    }

    @Test
    void serviceMatchResult_getDisplayMessage_clarification_listsOptions() {
        // Given: clarification needed
        ServiceMatchResult result = ServiceMatchResult.clarificationNeeded(List.of(vpnService, emailService));

        // When: get display message
        String message = result.getDisplayMessage();

        // Then: lists services
        assertThat(message).contains("multiple services");
        assertThat(message).contains("1. ");
        assertThat(message).contains("2. ");
        assertThat(message).contains("Which service do you need?");
    }

    @Test
    void serviceMatchResult_getDisplayMessage_noMatch_showsCategories() {
        // Given: no match
        ServiceMatchResult result = ServiceMatchResult.noMatch("Not found");

        // When: get display message
        String message = result.getDisplayMessage();

        // Then: shows categories
        assertThat(message).contains("service categories");
        assertThat(message).contains("IT Services");
        assertThat(message).contains("Support Services");
    }
}
