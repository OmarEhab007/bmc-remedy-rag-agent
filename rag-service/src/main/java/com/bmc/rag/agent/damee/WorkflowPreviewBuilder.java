package com.bmc.rag.agent.damee;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Builds workflow previews for Damee service requests.
 * Generates formatted summaries showing approval chain and estimated timelines.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowPreviewBuilder {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    /**
     * Build a complete preview of the service request before submission.
     */
    public String buildPreview(DameeService service, Map<String, String> fields, UserContext user) {
        StringBuilder preview = new StringBuilder();

        // Header
        preview.append("📋 **Service Request Summary**\n\n");

        // Service info
        appendServiceInfo(preview, service);

        // User-provided fields
        appendUserFields(preview, fields);

        // Workflow preview
        appendWorkflowPreview(preview, service, user);

        // VIP status
        appendVipStatus(preview, service, user);

        // Estimated timeline
        appendTimeline(preview, service);

        // Confirmation prompt
        preview.append("\n**Ready to submit?** Reply `confirm` or `cancel`");

        return preview.toString();
    }

    /**
     * Build a compact workflow summary for inline display.
     */
    public String buildCompactWorkflow(DameeService service) {
        if (service.getWorkflow() == null || service.getWorkflow().isEmpty()) {
            return "Standard approval workflow";
        }

        StringBuilder sb = new StringBuilder();
        List<DameeService.WorkflowStep> steps = service.getWorkflow();

        for (int i = 0; i < steps.size(); i++) {
            if (i > 0) {
                sb.append(" → ");
            }
            sb.append(steps.get(i).getDescription());
        }

        return sb.toString();
    }

    /**
     * Build a detailed workflow with estimated dates.
     */
    public String buildDetailedWorkflow(DameeService service, UserContext user) {
        if (service.getWorkflow() == null || service.getWorkflow().isEmpty()) {
            return buildDefaultWorkflow(service);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("**Approval Workflow:**\n\n");

        List<DameeService.WorkflowStep> steps = service.getWorkflow();
        LocalDate currentDate = LocalDate.now();
        int daysOffset = 0;

        for (int i = 0; i < steps.size(); i++) {
            DameeService.WorkflowStep step = steps.get(i);
            String status = (i == 0) ? "🔵" : "⚪";

            sb.append(String.format("%s **Step %d:** %s\n", status, i + 1, step.getDescription()));

            if (step.getTeam() != null) {
                sb.append(String.format("   _Assigned to:_ %s\n", step.getTeam()));
            }

            if (step.isRequiresApproval()) {
                // Check for VIP bypass
                if (step.getDescription().toLowerCase().contains("manager") &&
                        service.isVipBypass() && user != null && user.isVip()) {
                    sb.append("   ✅ _VIP bypass available_\n");
                } else {
                    daysOffset += estimateApprovalDays(step);
                    LocalDate estimatedDate = currentDate.plusDays(daysOffset);
                    sb.append(String.format("   ⏱️ _Est. completion:_ %s\n",
                            estimatedDate.format(DATE_FORMATTER)));
                }
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    // ========================
    // Helper Methods
    // ========================

    private void appendServiceInfo(StringBuilder sb, DameeService service) {
        sb.append(String.format("**Service:** %s\n", service.getNameEn()));

        if (service.getNameAr() != null) {
            sb.append(String.format("**الخدمة:** %s\n", service.getNameAr()));
        }

        sb.append(String.format("**Service ID:** %s\n", service.getServiceId()));

        if (service.getCategory() != null) {
            sb.append(String.format("**Category:** %s\n", service.getCategory()));
        }

        sb.append("\n");
    }

    private void appendUserFields(StringBuilder sb, Map<String, String> fields) {
        if (fields == null || fields.isEmpty()) {
            return;
        }

        sb.append("**Your Request Details:**\n");
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String fieldName = formatFieldName(entry.getKey());
            String value = entry.getValue();

            // Truncate long values
            if (value.length() > 100) {
                value = value.substring(0, 97) + "...";
            }

            sb.append(String.format("- **%s:** %s\n", fieldName, value));
        }
        sb.append("\n");
    }

    private void appendWorkflowPreview(StringBuilder sb, DameeService service, UserContext user) {
        sb.append("**Approval Workflow:**\n");

        if (service.getWorkflow() == null || service.getWorkflow().isEmpty()) {
            sb.append("1. 🔵 Submit Request (You)\n");
            sb.append("2. ⚪ Manager Approval\n");
            sb.append("3. ⚪ Service Desk Processing\n");
        } else {
            for (int i = 0; i < service.getWorkflow().size(); i++) {
                DameeService.WorkflowStep step = service.getWorkflow().get(i);
                String icon = (i == 0) ? "🔵" : "⚪";

                String description = step.getDescription();

                // Add VIP bypass indicator
                if (step.getDescription().toLowerCase().contains("manager") &&
                        service.isVipBypass() && user != null && user.isVip()) {
                    description += " _(VIP bypass)_";
                }

                sb.append(String.format("%d. %s %s\n", i + 1, icon, description));
            }
        }

        sb.append("\n");
    }

    private void appendVipStatus(StringBuilder sb, DameeService service, UserContext user) {
        if (service.isVipBypass() && user != null && user.isVip()) {
            sb.append("✅ **VIP Status:** Manager approval will be bypassed.\n\n");
        }
    }

    private void appendTimeline(StringBuilder sb, DameeService service) {
        int estimatedDays = estimateTotalDays(service);

        sb.append(String.format("⏱️ **Estimated Completion:** %d-%d business days\n",
                estimatedDays, estimatedDays + 2));

        LocalDate estimatedDate = LocalDate.now().plusDays(estimatedDays);
        sb.append(String.format("📅 **Est. Completion Date:** %s\n\n",
                estimatedDate.format(DATE_FORMATTER)));
    }

    private String buildDefaultWorkflow(DameeService service) {
        StringBuilder sb = new StringBuilder();
        sb.append("**Approval Workflow:**\n\n");
        sb.append("🔵 **Step 1:** Submit Request\n");
        sb.append("   _You_\n\n");
        sb.append("⚪ **Step 2:** Manager Approval\n");
        sb.append("   _Your Direct Manager_\n\n");
        sb.append("⚪ **Step 3:** Service Desk Processing\n");
        sb.append("   _IT Service Desk_\n\n");
        return sb.toString();
    }

    private int estimateApprovalDays(DameeService.WorkflowStep step) {
        String description = step.getDescription().toLowerCase();

        if (description.contains("fill form") || description.contains("submit")) {
            return 0;
        }

        if (description.contains("manager")) {
            return 1;
        }

        if (description.contains("grc") || description.contains("security")) {
            return 2;
        }

        if (description.contains("governance") || description.contains("approval")) {
            return 2;
        }

        // Default processing time
        return 1;
    }

    private int estimateTotalDays(DameeService service) {
        if (service.getWorkflow() == null || service.getWorkflow().isEmpty()) {
            return 3; // Default estimate
        }

        int totalDays = 0;
        for (DameeService.WorkflowStep step : service.getWorkflow()) {
            if (step.isRequiresApproval()) {
                totalDays += estimateApprovalDays(step);
            }
        }

        // Add processing time
        return Math.max(totalDays + 1, 2);
    }

    private String formatFieldName(String fieldName) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < fieldName.length(); i++) {
            char c = fieldName.charAt(i);
            if (i == 0) {
                result.append(Character.toUpperCase(c));
            } else if (Character.isUpperCase(c)) {
                result.append(' ').append(c);
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    // ========================
    // User Context
    // ========================

    /**
     * User context for workflow preview customization.
     */
    @Data
    @Builder
    public static class UserContext {
        private String userId;
        private String displayName;
        private String department;
        private String managerName;
        private boolean vip;
        private List<String> groups;

        public static UserContext anonymous() {
            return UserContext.builder()
                    .userId("anonymous")
                    .vip(false)
                    .build();
        }
    }
}
