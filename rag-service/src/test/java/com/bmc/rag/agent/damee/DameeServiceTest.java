package com.bmc.rag.agent.damee;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DameeService} model class.
 */
class DameeServiceTest {

    @Test
    void builder_createsService_success() {
        // When: build service
        DameeService service = DameeService.builder()
                .serviceId("10513")
                .nameEn("VPN Request")
                .nameAr("طلب VPN")
                .descriptionEn("VPN access")
                .descriptionAr("وصول VPN")
                .category("IT Services")
                .subcategory("Network")
                .url("https://test.url")
                .keywords(List.of("vpn", "remote"))
                .requiredFields(List.of("justification"))
                .requiresManagerApproval(true)
                .vipBypass(false)
                .build();

        // Then: service has all properties
        assertThat(service.getServiceId()).isEqualTo("10513");
        assertThat(service.getNameEn()).isEqualTo("VPN Request");
        assertThat(service.getNameAr()).isEqualTo("طلب VPN");
        assertThat(service.getDescriptionEn()).isEqualTo("VPN access");
        assertThat(service.getDescriptionAr()).isEqualTo("وصول VPN");
        assertThat(service.getCategory()).isEqualTo("IT Services");
        assertThat(service.getSubcategory()).isEqualTo("Network");
        assertThat(service.getUrl()).isEqualTo("https://test.url");
        assertThat(service.getKeywords()).containsExactly("vpn", "remote");
        assertThat(service.getRequiredFields()).containsExactly("justification");
        assertThat(service.isRequiresManagerApproval()).isTrue();
        assertThat(service.isVipBypass()).isFalse();
    }

    @Test
    void builder_defaultValues_success() {
        // When: build service with minimal fields
        DameeService service = DameeService.builder()
                .serviceId("10101")
                .nameEn("Test Service")
                .build();

        // Then: has default values
        assertThat(service.isRequiresManagerApproval()).isTrue();
        assertThat(service.isVipBypass()).isFalse();
    }

    @Test
    void getSummary_fullService_formatsCorrectly() {
        // Given: service with all fields
        DameeService service = DameeService.builder()
                .serviceId("10513")
                .nameEn("VPN Request")
                .nameAr("طلب VPN")
                .descriptionEn("Request VPN access for remote work")
                .url("https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10513")
                .build();

        // When: get summary
        String summary = service.getSummary();

        // Then: contains all key information
        assertThat(summary).contains("**VPN Request**");
        assertThat(summary).contains("(10513)");
        assertThat(summary).contains("طلب VPN");
        assertThat(summary).contains("Request VPN access for remote work");
        assertThat(summary).contains("[Open in Damee](https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10513)");
    }

    @Test
    void getSummary_minimalService_formatsCorrectly() {
        // Given: service with minimal fields
        DameeService service = DameeService.builder()
                .serviceId("10101")
                .nameEn("Test Service")
                .descriptionEn("Test description")
                .build();

        // When: get summary
        String summary = service.getSummary();

        // Then: contains available information
        assertThat(summary).contains("**Test Service**");
        assertThat(summary).contains("(10101)");
        assertThat(summary).contains("Test description");
    }

    @Test
    void getWorkflowSummary_withWorkflow_formatsSteps() {
        // Given: service with workflow
        DameeService service = DameeService.builder()
                .serviceId("10513")
                .nameEn("VPN Request")
                .workflow(List.of(
                        DameeService.WorkflowStep.builder()
                                .order(1)
                                .description("Fill Form")
                                .build(),
                        DameeService.WorkflowStep.builder()
                                .order(2)
                                .description("Manager Approval")
                                .build(),
                        DameeService.WorkflowStep.builder()
                                .order(3)
                                .description("Network Team")
                                .build()
                ))
                .build();

        // When: get workflow summary
        String summary = service.getWorkflowSummary();

        // Then: formats as arrow-separated steps
        assertThat(summary).isEqualTo("Fill Form → Manager Approval → Network Team");
    }

    @Test
    void getWorkflowSummary_nullWorkflow_returnsDefault() {
        // Given: service without workflow
        DameeService service = DameeService.builder()
                .serviceId("10513")
                .nameEn("VPN Request")
                .workflow(null)
                .build();

        // When: get workflow summary
        String summary = service.getWorkflowSummary();

        // Then: returns default message
        assertThat(summary).isEqualTo("Standard workflow");
    }

    @Test
    void getWorkflowSummary_emptyWorkflow_returnsDefault() {
        // Given: service with empty workflow
        DameeService service = DameeService.builder()
                .serviceId("10513")
                .nameEn("VPN Request")
                .workflow(List.of())
                .build();

        // When: get workflow summary
        String summary = service.getWorkflowSummary();

        // Then: returns default message
        assertThat(summary).isEqualTo("Standard workflow");
    }

    @Test
    void getFirstFieldPrompt_withRequiredFields_returnsFirstField() {
        // Given: service with required fields
        DameeService service = DameeService.builder()
                .serviceId("10513")
                .nameEn("VPN Request")
                .requiredFields(List.of("justification", "vpnType"))
                .build();

        // When: get first field prompt
        String prompt = service.getFirstFieldPrompt();

        // Then: returns first field
        assertThat(prompt).contains("justification");
    }

    @Test
    void getFirstFieldPrompt_noRequiredFields_returnsDefault() {
        // Given: service without required fields
        DameeService service = DameeService.builder()
                .serviceId("10513")
                .nameEn("VPN Request")
                .requiredFields(null)
                .build();

        // When: get first field prompt
        String prompt = service.getFirstFieldPrompt();

        // Then: returns default prompt
        assertThat(prompt).contains("describe your request");
    }

    @Test
    void getFirstFieldPrompt_emptyRequiredFields_returnsDefault() {
        // Given: service with empty required fields
        DameeService service = DameeService.builder()
                .serviceId("10513")
                .nameEn("VPN Request")
                .requiredFields(List.of())
                .build();

        // When: get first field prompt
        String prompt = service.getFirstFieldPrompt();

        // Then: returns default prompt
        assertThat(prompt).contains("describe your request");
    }

    @Test
    void setScore_updatesScore_success() {
        // Given: service
        DameeService service = DameeService.builder()
                .serviceId("10513")
                .nameEn("VPN Request")
                .build();

        // When: set score
        service.setScore(0.88);

        // Then: score is updated
        assertThat(service.getScore()).isEqualTo(0.88);
    }

    @Test
    void workflowStep_builder_createsStep() {
        // When: build workflow step
        DameeService.WorkflowStep step = DameeService.WorkflowStep.builder()
                .order(1)
                .description("Manager Approval")
                .team("Manager")
                .requiresApproval(true)
                .condition("if urgent")
                .build();

        // Then: step has all properties
        assertThat(step.getOrder()).isEqualTo(1);
        assertThat(step.getDescription()).isEqualTo("Manager Approval");
        assertThat(step.getTeam()).isEqualTo("Manager");
        assertThat(step.isRequiresApproval()).isTrue();
        assertThat(step.getCondition()).isEqualTo("if urgent");
    }

    @Test
    void serviceOption_builder_createsOption() {
        // When: build service option
        DameeService.ServiceOption option = DameeService.ServiceOption.builder()
                .optionId("opt1")
                .nameEn("Option 1")
                .nameAr("خيار 1")
                .description("First option")
                .workflow(List.of())
                .build();

        // Then: option has all properties
        assertThat(option.getOptionId()).isEqualTo("opt1");
        assertThat(option.getNameEn()).isEqualTo("Option 1");
        assertThat(option.getNameAr()).isEqualTo("خيار 1");
        assertThat(option.getDescription()).isEqualTo("First option");
        assertThat(option.getWorkflow()).isEmpty();
    }

    @Test
    void noArgsConstructor_createsEmptyService() {
        // When: create with no-args constructor
        DameeService service = new DameeService();

        // Then: service is created
        assertThat(service).isNotNull();
        assertThat(service.getServiceId()).isNull();
    }

    @Test
    void allArgsConstructor_createsService() {
        // When: create with all-args constructor
        DameeService service = new DameeService(
                "10513",
                "VPN Request",
                "طلب VPN",
                "VPN access",
                "وصول VPN",
                "IT Services",
                "Network",
                "https://test.url",
                List.of(),
                List.of("vpn"),
                List.of("justification"),
                List.of("notes"),
                true,
                false,
                List.of(),
                0.88
        );

        // Then: service has all properties
        assertThat(service.getServiceId()).isEqualTo("10513");
        assertThat(service.getNameEn()).isEqualTo("VPN Request");
        assertThat(service.getScore()).isEqualTo(0.88);
    }

    @Test
    void equals_sameServices_returnsTrue() {
        // Given: two identical services
        DameeService service1 = DameeService.builder()
                .serviceId("10513")
                .nameEn("VPN Request")
                .build();
        DameeService service2 = DameeService.builder()
                .serviceId("10513")
                .nameEn("VPN Request")
                .build();

        // When/Then: services are equal
        assertThat(service1).isEqualTo(service2);
        assertThat(service1.hashCode()).isEqualTo(service2.hashCode());
    }

    @Test
    void toString_includesKeyFields() {
        // Given: service
        DameeService service = DameeService.builder()
                .serviceId("10513")
                .nameEn("VPN Request")
                .category("IT Services")
                .build();

        // When: convert to string
        String str = service.toString();

        // Then: includes key fields
        assertThat(str).contains("10513");
        assertThat(str).contains("VPN Request");
    }

    @Test
    void setters_updateFields_success() {
        // Given: service
        DameeService service = new DameeService();

        // When: use setters
        service.setServiceId("10513");
        service.setNameEn("VPN Request");
        service.setCategory("IT Services");
        service.setRequiresManagerApproval(false);

        // Then: fields are updated
        assertThat(service.getServiceId()).isEqualTo("10513");
        assertThat(service.getNameEn()).isEqualTo("VPN Request");
        assertThat(service.getCategory()).isEqualTo("IT Services");
        assertThat(service.isRequiresManagerApproval()).isFalse();
    }

    @Test
    void workflowStep_noArgsConstructor_createsStep() {
        // When: create step with no-args constructor
        DameeService.WorkflowStep step = new DameeService.WorkflowStep();

        // Then: step is created
        assertThat(step).isNotNull();
    }

    @Test
    void workflowStep_allArgsConstructor_createsStep() {
        // When: create step with all-args constructor
        DameeService.WorkflowStep step = new DameeService.WorkflowStep(
                1,
                "Manager Approval",
                "Manager",
                true,
                "if urgent"
        );

        // Then: step has all properties
        assertThat(step.getOrder()).isEqualTo(1);
        assertThat(step.getDescription()).isEqualTo("Manager Approval");
        assertThat(step.getTeam()).isEqualTo("Manager");
        assertThat(step.isRequiresApproval()).isTrue();
        assertThat(step.getCondition()).isEqualTo("if urgent");
    }

    @Test
    void serviceOption_noArgsConstructor_createsOption() {
        // When: create option with no-args constructor
        DameeService.ServiceOption option = new DameeService.ServiceOption();

        // Then: option is created
        assertThat(option).isNotNull();
    }

    @Test
    void serviceOption_allArgsConstructor_createsOption() {
        // When: create option with all-args constructor
        DameeService.ServiceOption option = new DameeService.ServiceOption(
                "opt1",
                "Option 1",
                "خيار 1",
                "First option",
                List.of()
        );

        // Then: option has all properties
        assertThat(option.getOptionId()).isEqualTo("opt1");
        assertThat(option.getNameEn()).isEqualTo("Option 1");
        assertThat(option.getNameAr()).isEqualTo("خيار 1");
        assertThat(option.getDescription()).isEqualTo("First option");
    }
}
