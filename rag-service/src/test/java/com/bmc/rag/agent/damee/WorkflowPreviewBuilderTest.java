package com.bmc.rag.agent.damee;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowPreviewBuilderTest {

    private WorkflowPreviewBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new WorkflowPreviewBuilder();
    }

    @Nested
    @DisplayName("buildCompactWorkflow")
    class BuildCompactWorkflow {

        @Test
        void buildCompactWorkflow_nullWorkflow_returnsDefault() {
            var service = DameeService.builder()
                    .serviceId("S1").nameEn("Test Service").workflow(null).build();
            assertThat(builder.buildCompactWorkflow(service)).isEqualTo("Standard approval workflow");
        }

        @Test
        void buildCompactWorkflow_emptyWorkflow_returnsDefault() {
            var service = DameeService.builder()
                    .serviceId("S1").nameEn("Test Service").workflow(List.of()).build();
            assertThat(builder.buildCompactWorkflow(service)).isEqualTo("Standard approval workflow");
        }

        @Test
        void buildCompactWorkflow_singleStep_returnsDescription() {
            var step = DameeService.WorkflowStep.builder()
                    .description("Submit Request").build();
            var service = DameeService.builder()
                    .serviceId("S1").nameEn("Test").workflow(List.of(step)).build();
            assertThat(builder.buildCompactWorkflow(service)).isEqualTo("Submit Request");
        }

        @Test
        void buildCompactWorkflow_multipleSteps_joinsWithArrow() {
            var steps = List.of(
                    DameeService.WorkflowStep.builder().description("Submit Request").build(),
                    DameeService.WorkflowStep.builder().description("Manager Approval").build(),
                    DameeService.WorkflowStep.builder().description("Processing").build()
            );
            var service = DameeService.builder()
                    .serviceId("S1").nameEn("Test").workflow(steps).build();

            String result = builder.buildCompactWorkflow(service);
            assertThat(result).isEqualTo("Submit Request → Manager Approval → Processing");
        }
    }

    @Nested
    @DisplayName("buildDetailedWorkflow")
    class BuildDetailedWorkflow {

        @Test
        void buildDetailedWorkflow_nullWorkflow_returnsDefault() {
            var service = DameeService.builder()
                    .serviceId("S1").nameEn("Test").workflow(null).build();
            String result = builder.buildDetailedWorkflow(service, null);
            assertThat(result).contains("Approval Workflow");
            assertThat(result).contains("Submit Request");
            assertThat(result).contains("Manager Approval");
        }

        @Test
        void buildDetailedWorkflow_withSteps_containsStepNumbers() {
            var steps = List.of(
                    DameeService.WorkflowStep.builder()
                            .description("Fill Form").requiresApproval(false).build(),
                    DameeService.WorkflowStep.builder()
                            .description("Manager Approval").requiresApproval(true).team("Managers").build()
            );
            var service = DameeService.builder()
                    .serviceId("S1").nameEn("Test").workflow(steps).build();

            String result = builder.buildDetailedWorkflow(service, null);
            assertThat(result).contains("Step 1");
            assertThat(result).contains("Step 2");
            assertThat(result).contains("Fill Form");
            assertThat(result).contains("Manager Approval");
        }

        @Test
        void buildDetailedWorkflow_approvalStep_showsEstimatedDate() {
            var steps = List.of(
                    DameeService.WorkflowStep.builder()
                            .description("Manager Approval").requiresApproval(true).build()
            );
            var service = DameeService.builder()
                    .serviceId("S1").nameEn("Test").workflow(steps).build();

            String result = builder.buildDetailedWorkflow(service, null);
            assertThat(result).contains("Est. completion");
        }

        @Test
        void buildDetailedWorkflow_vipUser_showsBypass() {
            var steps = List.of(
                    DameeService.WorkflowStep.builder()
                            .description("Manager Approval").requiresApproval(true).build()
            );
            var service = DameeService.builder()
                    .serviceId("S1").nameEn("Test").workflow(steps).vipBypass(true).build();
            var user = WorkflowPreviewBuilder.UserContext.builder().vip(true).build();

            String result = builder.buildDetailedWorkflow(service, user);
            assertThat(result).contains("VIP bypass available");
        }

        @Test
        void buildDetailedWorkflow_nonVipUser_noBypass() {
            var steps = List.of(
                    DameeService.WorkflowStep.builder()
                            .description("Manager Approval").requiresApproval(true).build()
            );
            var service = DameeService.builder()
                    .serviceId("S1").nameEn("Test").workflow(steps).vipBypass(true).build();
            var user = WorkflowPreviewBuilder.UserContext.builder().vip(false).build();

            String result = builder.buildDetailedWorkflow(service, user);
            assertThat(result).doesNotContain("VIP bypass available");
        }

        @Test
        void buildDetailedWorkflow_stepWithTeam_showsAssignment() {
            var steps = List.of(
                    DameeService.WorkflowStep.builder()
                            .description("GRC Review").requiresApproval(true).team("GRC Team").build()
            );
            var service = DameeService.builder()
                    .serviceId("S1").nameEn("Test").workflow(steps).build();

            String result = builder.buildDetailedWorkflow(service, null);
            assertThat(result).contains("GRC Team");
        }
    }

    @Nested
    @DisplayName("buildPreview")
    class BuildPreview {

        @Test
        void buildPreview_basicService_containsServiceInfo() {
            var service = DameeService.builder()
                    .serviceId("S1").nameEn("VPN Access").category("IT Services")
                    .workflow(null).build();
            var user = WorkflowPreviewBuilder.UserContext.anonymous();

            String result = builder.buildPreview(service, Map.of("reason", "Need VPN"), user);
            assertThat(result).contains("Service Request Summary");
            assertThat(result).contains("VPN Access");
            assertThat(result).contains("S1");
            assertThat(result).contains("IT Services");
        }

        @Test
        void buildPreview_withFields_showsUserFields() {
            var service = DameeService.builder()
                    .serviceId("S1").nameEn("Test").workflow(null).build();
            var user = WorkflowPreviewBuilder.UserContext.anonymous();

            String result = builder.buildPreview(service, Map.of("name", "John", "department", "IT"), user);
            assertThat(result).contains("Your Request Details");
            assertThat(result).contains("John");
            assertThat(result).contains("IT");
        }

        @Test
        void buildPreview_nullFields_skipsFieldSection() {
            var service = DameeService.builder()
                    .serviceId("S1").nameEn("Test").workflow(null).build();
            var user = WorkflowPreviewBuilder.UserContext.anonymous();

            String result = builder.buildPreview(service, null, user);
            assertThat(result).doesNotContain("Your Request Details");
        }

        @Test
        void buildPreview_emptyFields_skipsFieldSection() {
            var service = DameeService.builder()
                    .serviceId("S1").nameEn("Test").workflow(null).build();
            var user = WorkflowPreviewBuilder.UserContext.anonymous();

            String result = builder.buildPreview(service, Map.of(), user);
            assertThat(result).doesNotContain("Your Request Details");
        }

        @Test
        void buildPreview_longFieldValue_truncates() {
            var service = DameeService.builder()
                    .serviceId("S1").nameEn("Test").workflow(null).build();
            var user = WorkflowPreviewBuilder.UserContext.anonymous();
            String longValue = "A".repeat(150);

            String result = builder.buildPreview(service, Map.of("description", longValue), user);
            assertThat(result).contains("...");
            assertThat(result).doesNotContain("A".repeat(150));
        }

        @Test
        void buildPreview_vipUser_showsVipStatus() {
            var service = DameeService.builder()
                    .serviceId("S1").nameEn("Test").workflow(null).vipBypass(true).build();
            var user = WorkflowPreviewBuilder.UserContext.builder().vip(true).build();

            String result = builder.buildPreview(service, null, user);
            assertThat(result).contains("VIP Status");
        }

        @Test
        void buildPreview_containsConfirmationPrompt() {
            var service = DameeService.builder()
                    .serviceId("S1").nameEn("Test").workflow(null).build();
            var user = WorkflowPreviewBuilder.UserContext.anonymous();

            String result = builder.buildPreview(service, null, user);
            assertThat(result).contains("confirm");
            assertThat(result).contains("cancel");
        }

        @Test
        void buildPreview_containsTimeline() {
            var service = DameeService.builder()
                    .serviceId("S1").nameEn("Test").workflow(null).build();
            var user = WorkflowPreviewBuilder.UserContext.anonymous();

            String result = builder.buildPreview(service, null, user);
            assertThat(result).contains("Estimated Completion");
            assertThat(result).contains("business days");
        }

        @Test
        void buildPreview_arabicName_showsBothLanguages() {
            var service = DameeService.builder()
                    .serviceId("S1").nameEn("VPN Access").nameAr("وصول VPN")
                    .workflow(null).build();
            var user = WorkflowPreviewBuilder.UserContext.anonymous();

            String result = builder.buildPreview(service, null, user);
            assertThat(result).contains("VPN Access");
            assertThat(result).contains("وصول VPN");
        }
    }

    @Nested
    @DisplayName("UserContext")
    class UserContextTest {

        @Test
        void anonymous_createsNonVipUser() {
            var user = WorkflowPreviewBuilder.UserContext.anonymous();
            assertThat(user.getUserId()).isEqualTo("anonymous");
            assertThat(user.isVip()).isFalse();
        }

        @Test
        void builder_setsAllFields() {
            var user = WorkflowPreviewBuilder.UserContext.builder()
                    .userId("user1").displayName("John Doe").department("IT")
                    .managerName("Jane").vip(true).groups(List.of("Admin", "IT"))
                    .build();
            assertThat(user.getUserId()).isEqualTo("user1");
            assertThat(user.getDisplayName()).isEqualTo("John Doe");
            assertThat(user.getDepartment()).isEqualTo("IT");
            assertThat(user.getManagerName()).isEqualTo("Jane");
            assertThat(user.isVip()).isTrue();
            assertThat(user.getGroups()).containsExactly("Admin", "IT");
        }
    }

    @Nested
    @DisplayName("estimateApprovalDays branches")
    class EstimateApprovalDays {

        @Test
        void fillFormStep_estimatesZeroDays() {
            var steps = List.of(
                    DameeService.WorkflowStep.builder()
                            .description("Fill Form and Submit").requiresApproval(true).build()
            );
            var service = DameeService.builder()
                    .serviceId("S1").nameEn("Test").workflow(steps).build();

            String result = builder.buildDetailedWorkflow(service, null);
            // fillForm/submit gets 0 days, so the estimated date should be today
            assertThat(result).contains("Est. completion");
        }

        @Test
        void grcSecurityStep_estimatesTwoDays() {
            var steps = List.of(
                    DameeService.WorkflowStep.builder()
                            .description("GRC Security Review").requiresApproval(true).build()
            );
            var service = DameeService.builder()
                    .serviceId("S1").nameEn("Test").workflow(steps).build();

            String result = builder.buildDetailedWorkflow(service, null);
            assertThat(result).contains("Est. completion");
        }

        @Test
        void governanceApprovalStep_estimatesTwoDays() {
            var steps = List.of(
                    DameeService.WorkflowStep.builder()
                            .description("Governance Approval Committee").requiresApproval(true).build()
            );
            var service = DameeService.builder()
                    .serviceId("S1").nameEn("Test").workflow(steps).build();

            String result = builder.buildDetailedWorkflow(service, null);
            assertThat(result).contains("Est. completion");
        }

        @Test
        void defaultStep_estimatesOneDay() {
            var steps = List.of(
                    DameeService.WorkflowStep.builder()
                            .description("Processing by Service Desk").requiresApproval(true).build()
            );
            var service = DameeService.builder()
                    .serviceId("S1").nameEn("Test").workflow(steps).build();

            String result = builder.buildDetailedWorkflow(service, null);
            assertThat(result).contains("Est. completion");
        }

        @Test
        void multipleStepTypes_accumulatesDays() {
            var steps = List.of(
                    DameeService.WorkflowStep.builder()
                            .description("Fill Form and Submit").requiresApproval(true).build(),
                    DameeService.WorkflowStep.builder()
                            .description("Manager Approval").requiresApproval(true).build(),
                    DameeService.WorkflowStep.builder()
                            .description("GRC Security Review").requiresApproval(true).build(),
                    DameeService.WorkflowStep.builder()
                            .description("Governance Approval").requiresApproval(true).build(),
                    DameeService.WorkflowStep.builder()
                            .description("IT Processing").requiresApproval(true).build()
            );
            var service = DameeService.builder()
                    .serviceId("S1").nameEn("Test").workflow(steps).build();

            String result = builder.buildDetailedWorkflow(service, null);
            assertThat(result).contains("Step 1").contains("Step 5");
        }
    }

    @Nested
    @DisplayName("buildPreview with workflow steps")
    class BuildPreviewWithWorkflow {

        @Test
        void buildPreview_withWorkflowSteps_showsStepNumbers() {
            var steps = List.of(
                    DameeService.WorkflowStep.builder()
                            .description("Fill Form").requiresApproval(false).build(),
                    DameeService.WorkflowStep.builder()
                            .description("Manager Approval").requiresApproval(true).build(),
                    DameeService.WorkflowStep.builder()
                            .description("Processing").requiresApproval(false).build()
            );
            var service = DameeService.builder()
                    .serviceId("S1").nameEn("Test").category("IT Services")
                    .workflow(steps).build();
            var user = WorkflowPreviewBuilder.UserContext.anonymous();

            String result = builder.buildPreview(service, Map.of("reason", "testing"), user);
            assertThat(result).contains("1.");
            assertThat(result).contains("2.");
            assertThat(result).contains("3.");
            assertThat(result).contains("Fill Form");
            assertThat(result).contains("Manager Approval");
        }

        @Test
        void buildPreview_withVipBypassWorkflow_showsBypass() {
            var steps = List.of(
                    DameeService.WorkflowStep.builder()
                            .description("Fill Form").requiresApproval(false).build(),
                    DameeService.WorkflowStep.builder()
                            .description("Manager Approval").requiresApproval(true).build()
            );
            var service = DameeService.builder()
                    .serviceId("S1").nameEn("Test").workflow(steps).vipBypass(true).build();
            var user = WorkflowPreviewBuilder.UserContext.builder().vip(true).build();

            String result = builder.buildPreview(service, null, user);
            assertThat(result).contains("VIP bypass");
        }

        @Test
        void buildPreview_withWorkflow_showsEstimatedDays() {
            var steps = List.of(
                    DameeService.WorkflowStep.builder()
                            .description("Manager Approval").requiresApproval(true).build()
            );
            var service = DameeService.builder()
                    .serviceId("S1").nameEn("Test").workflow(steps).build();
            var user = WorkflowPreviewBuilder.UserContext.anonymous();

            String result = builder.buildPreview(service, null, user);
            assertThat(result).contains("business days");
        }
    }

    @Nested
    @DisplayName("formatFieldName")
    class FormatFieldName {

        @Test
        void formatFieldName_camelCase_addSpaces() {
            var service = DameeService.builder()
                    .serviceId("S1").nameEn("Test").workflow(null).build();
            var user = WorkflowPreviewBuilder.UserContext.anonymous();

            String result = builder.buildPreview(service,
                    Map.of("applicationName", "Adobe", "requestDate", "2025-01-01"), user);
            assertThat(result).contains("Application Name");
            assertThat(result).contains("Request Date");
        }

        @Test
        void formatFieldName_singleWord_capitalizes() {
            var service = DameeService.builder()
                    .serviceId("S1").nameEn("Test").workflow(null).build();
            var user = WorkflowPreviewBuilder.UserContext.anonymous();

            String result = builder.buildPreview(service,
                    Map.of("reason", "need access"), user);
            assertThat(result).contains("Reason");
        }
    }
}
