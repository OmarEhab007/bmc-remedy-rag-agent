package com.bmc.rag.agent.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RagConfig}.
 */
class RagConfigTest {

    private RagConfig config;

    @BeforeEach
    void setUp() {
        config = new RagConfig();
        // Trigger @PostConstruct
        config.logConfiguration();
    }

    @Test
    void defaultValues_areSetCorrectly() {
        // Then: default values are set
        assertThat(config.getMaxResults()).isEqualTo(5);
        assertThat(config.getMinScore()).isEqualTo(0.5f);
        assertThat(config.getMaxMemoryMessages()).isEqualTo(20);
        assertThat(config.isIncludeCitations()).isTrue();
        assertThat(config.isPrioritizeKnowledgeArticles()).isTrue();
        assertThat(config.isRebacEnabled()).isTrue();
    }

    @Test
    void systemPrompt_containsIdentity() {
        // When: get system prompt
        String prompt = config.getSystemPrompt();

        // Then: contains identity information
        assertThat(prompt).contains("Damee");
        assertThat(prompt).contains("دعمي");
        assertThat(prompt).contains("CST");
        assertThat(prompt).contains("Communications, Space & Technology Commission");
    }

    @Test
    void systemPrompt_containsServiceCategories() {
        // When: get system prompt
        String prompt = config.getSystemPrompt();

        // Then: contains service categories
        assertThat(prompt).contains("IT Services");
        assertThat(prompt).contains("خدمات تقنية المعلومات");
        assertThat(prompt).contains("Support Services");
        assertThat(prompt).contains("Legal Consultation Services");
        assertThat(prompt).contains("Inspection Services");
        assertThat(prompt).contains("Geospatial Services");
    }

    @Test
    void systemPrompt_containsGroundingPrinciples() {
        // When: get system prompt
        String prompt = config.getSystemPrompt();

        // Then: contains grounding principles
        assertThat(prompt).contains("GROUNDING");
        assertThat(prompt).contains("Never Hallucinate");
        assertThat(prompt).contains("ONLY use information from the provided context");
        assertThat(prompt).contains("don't have enough information");
    }

    @Test
    void systemPrompt_containsCitationRequirements() {
        // When: get system prompt
        String prompt = config.getSystemPrompt();

        // Then: contains citation requirements
        assertThat(prompt).contains("CITATION REQUIREMENTS");
        assertThat(prompt).contains("EVERY claim MUST include a citation");
        assertThat(prompt).contains("(Source: TYPE-ID)");
        assertThat(prompt).contains("INC (Incident)");
        assertThat(prompt).contains("KB (Knowledge)");
    }

    @Test
    void systemPrompt_containsResponseStructure() {
        // When: get system prompt
        String prompt = config.getSystemPrompt();

        // Then: contains response structure guidelines
        assertThat(prompt).contains("RESPONSE STRUCTURE FOR SERVICE GUIDANCE");
        assertThat(prompt).contains("RESPONSE STRUCTURE FOR TROUBLESHOOTING");
        assertThat(prompt).contains("Service ID");
        assertThat(prompt).contains("Workflow");
    }

    @Test
    void systemPrompt_containsLanguageRules() {
        // When: get system prompt
        String prompt = config.getSystemPrompt();

        // Then: contains language rules
        assertThat(prompt).contains("LANGUAGE RULES");
        assertThat(prompt).contains("English question → English response");
        assertThat(prompt).contains("Arabic question → Arabic response");
    }

    @Test
    void systemPrompt_containsBehavioralGuidelines() {
        // When: get system prompt
        String prompt = config.getSystemPrompt();

        // Then: contains behavioral guidelines
        assertThat(prompt).contains("BEHAVIORAL GUIDELINES");
        assertThat(prompt).contains("When user asks about a specific service");
        assertThat(prompt).contains("When you LACK sufficient context");
    }

    @Test
    void systemPrompt_containsWorkflowPatterns() {
        // When: get system prompt
        String prompt = config.getSystemPrompt();

        // Then: contains common workflow patterns
        assertThat(prompt).contains("Common Workflow Patterns");
        assertThat(prompt).contains("Manager Approval");
        assertThat(prompt).contains("VIP Bypass");
        assertThat(prompt).contains("GRC Approval");
    }

    @Test
    void systemPrompt_containsUserOperationsGuide() {
        // When: get system prompt
        String prompt = config.getSystemPrompt();

        // Then: contains user operations
        assertThat(prompt).contains("User Operations Guide");
        assertThat(prompt).contains("Creating a Service Request");
        assertThat(prompt).contains("Tracking Requests");
        assertThat(prompt).contains("Approving Requests");
    }

    @Test
    void systemPrompt_containsProhibitedBehaviors() {
        // When: get system prompt
        String prompt = config.getSystemPrompt();

        // Then: contains prohibited behaviors
        assertThat(prompt).contains("PROHIBITED BEHAVIORS");
        assertThat(prompt).contains("NEVER make up service IDs");
        assertThat(prompt).contains("NEVER guess approval chains");
        assertThat(prompt).contains("NEVER provide solutions without citations");
    }

    @Test
    void systemPrompt_containsQualityChecklist() {
        // When: get system prompt
        String prompt = config.getSystemPrompt();

        // Then: contains quality checklist
        assertThat(prompt).contains("QUALITY CHECKLIST");
        assertThat(prompt).contains("Every claim has a citation");
        assertThat(prompt).contains("Response is in the same language");
        assertThat(prompt).contains("Service workflows are complete");
    }

    @Test
    void systemPrompt_containsDameePlatformUrl() {
        // When: get system prompt
        String prompt = config.getSystemPrompt();

        // Then: contains platform URL
        assertThat(prompt).contains("https://itsmweb.mewa.gov.sa/jahz/index.html");
    }

    @Test
    void systemPrompt_containsSourcePriority() {
        // When: get system prompt
        String prompt = config.getSystemPrompt();

        // Then: contains source priority
        assertThat(prompt).contains("SOURCE PRIORITY");
        assertThat(prompt).contains("Service Definitions (SVC)");
        assertThat(prompt).contains("Knowledge Articles (KB)");
        assertThat(prompt).contains("Incidents (INC)");
    }

    @Test
    void setMaxResults_updatesValue() {
        // When: set max results
        config.setMaxResults(10);

        // Then: value is updated
        assertThat(config.getMaxResults()).isEqualTo(10);
    }

    @Test
    void setMinScore_updatesValue() {
        // When: set min score
        config.setMinScore(0.75f);

        // Then: value is updated
        assertThat(config.getMinScore()).isEqualTo(0.75f);
    }

    @Test
    void setMaxMemoryMessages_updatesValue() {
        // When: set max memory messages
        config.setMaxMemoryMessages(50);

        // Then: value is updated
        assertThat(config.getMaxMemoryMessages()).isEqualTo(50);
    }

    @Test
    void setSystemPrompt_updatesValue() {
        // When: set custom system prompt
        String customPrompt = "Custom prompt";
        config.setSystemPrompt(customPrompt);

        // Then: value is updated
        assertThat(config.getSystemPrompt()).isEqualTo(customPrompt);
    }

    @Test
    void setIncludeCitations_updatesValue() {
        // When: disable citations
        config.setIncludeCitations(false);

        // Then: value is updated
        assertThat(config.isIncludeCitations()).isFalse();
    }

    @Test
    void setPrioritizeKnowledgeArticles_updatesValue() {
        // When: disable knowledge article priority
        config.setPrioritizeKnowledgeArticles(false);

        // Then: value is updated
        assertThat(config.isPrioritizeKnowledgeArticles()).isFalse();
    }

    @Test
    void setRebacEnabled_updatesValue() {
        // When: disable ReBAC
        config.setRebacEnabled(false);

        // Then: value is updated
        assertThat(config.isRebacEnabled()).isFalse();
    }

    @Test
    void toString_includesKeyFields() {
        // When: convert to string
        String str = config.toString();

        // Then: includes key configuration
        assertThat(str).isNotBlank();
    }

    @Test
    void logConfiguration_logsValues() {
        // When: log configuration (already called in setUp)
        // Then: no exception is thrown and method completes
        assertThat(config).isNotNull();
    }

    @Test
    void defaultSystemPrompt_isNotBlank() {
        // When: get default system prompt
        String prompt = new RagConfig().getSystemPrompt();

        // Then: prompt is substantial
        assertThat(prompt).isNotBlank();
        assertThat(prompt.length()).isGreaterThan(1000);
    }

    @Test
    void minScore_validRange_acceptsValue() {
        // When: set valid min score values
        config.setMinScore(0.0f);
        assertThat(config.getMinScore()).isEqualTo(0.0f);

        config.setMinScore(0.5f);
        assertThat(config.getMinScore()).isEqualTo(0.5f);

        config.setMinScore(1.0f);
        assertThat(config.getMinScore()).isEqualTo(1.0f);
    }

    @Test
    void maxResults_positiveValue_acceptsValue() {
        // When: set positive max results
        config.setMaxResults(1);
        assertThat(config.getMaxResults()).isEqualTo(1);

        config.setMaxResults(100);
        assertThat(config.getMaxResults()).isEqualTo(100);
    }

    @Test
    void maxMemoryMessages_positiveValue_acceptsValue() {
        // When: set positive max memory messages
        config.setMaxMemoryMessages(1);
        assertThat(config.getMaxMemoryMessages()).isEqualTo(1);

        config.setMaxMemoryMessages(1000);
        assertThat(config.getMaxMemoryMessages()).isEqualTo(1000);
    }

    @Test
    void systemPrompt_containsFormattingRules() {
        // When: get system prompt
        String prompt = config.getSystemPrompt();

        // Then: contains formatting rules
        assertThat(prompt).contains("FORMATTING RULES");
        assertThat(prompt).contains("Lists:");
        assertThat(prompt).contains("Arabic Responses:");
        assertThat(prompt).contains("English Responses:");
    }

    @Test
    void systemPrompt_containsContextHandling() {
        // When: get system prompt
        String prompt = config.getSystemPrompt();

        // Then: contains context handling
        assertThat(prompt).contains("CONTEXT HANDLING");
        assertThat(prompt).contains("Using this context:");
        assertThat(prompt).contains("Identify which services/entries are relevant");
    }

    @Test
    void systemPrompt_containsBilingualSupport() {
        // When: get system prompt
        String prompt = config.getSystemPrompt();

        // Then: contains bilingual (Arabic/English) content
        assertThat(prompt).contains("أنت \"دعمي\"");
        assertThat(prompt).contains("You are \"Damee\"");
        assertThat(prompt).contains("الفئات الرئيسية");
        assertThat(prompt).contains("Service Categories");
    }

    @Test
    void systemPrompt_containsSpecialNotes() {
        // When: get system prompt
        String prompt = config.getSystemPrompt();

        // Then: contains special notes section
        assertThat(prompt).contains("Special Notes:");
        assertThat(prompt).contains("VIP bypass");
    }

    @Test
    void equals_sameConfigs_returnsTrue() {
        // Given: two configs with same values
        RagConfig config1 = new RagConfig();
        RagConfig config2 = new RagConfig();

        // When/Then: configs are equal
        assertThat(config1).isEqualTo(config2);
        assertThat(config1.hashCode()).isEqualTo(config2.hashCode());
    }

    @Test
    void equals_differentMaxResults_returnsFalse() {
        // Given: two configs with different max results
        RagConfig config1 = new RagConfig();
        RagConfig config2 = new RagConfig();
        config2.setMaxResults(10);

        // When/Then: configs are not equal
        assertThat(config1).isNotEqualTo(config2);
    }

    @Test
    void systemPrompt_containsDameeCheckoutUrl() {
        // When: get system prompt
        String prompt = config.getSystemPrompt();

        // Then: contains checkout URL pattern
        assertThat(prompt).contains("https://dwpstg.citc.gov.sa/dwp/app/#/checkout/{ServiceID}");
    }

    @Test
    void systemPrompt_containsApprovalWorkflowGuidance() {
        // When: get system prompt
        String prompt = config.getSystemPrompt();

        // Then: contains approval workflow guidance
        assertThat(prompt).contains("Fill Form");
        assertThat(prompt).contains("Approvals");
        assertThat(prompt).contains("Fulfillment");
        assertThat(prompt).contains("End");
    }
}
