package com.bmc.rag.agent.damee;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Entity representing a Damee service from the CST ITSM Service Catalog.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DameeService {

    /**
     * Unique service ID (e.g., "10504").
     */
    private String serviceId;

    /**
     * Service name in English.
     */
    private String nameEn;

    /**
     * Service name in Arabic.
     */
    private String nameAr;

    /**
     * Service description in English.
     */
    private String descriptionEn;

    /**
     * Service description in Arabic.
     */
    private String descriptionAr;

    /**
     * Main category (e.g., "IT Services", "Support Services").
     */
    private String category;

    /**
     * Sub-category (e.g., "Accounts Services", "Technical Services").
     */
    private String subcategory;

    /**
     * URL to the Damee checkout page.
     */
    private String url;

    /**
     * Workflow steps for this service.
     */
    private List<WorkflowStep> workflow;

    /**
     * Keywords for intent matching.
     */
    private List<String> keywords;

    /**
     * Required fields for this service.
     */
    private List<String> requiredFields;

    /**
     * Optional fields for this service.
     */
    private List<String> optionalFields;

    /**
     * Whether this service requires manager approval.
     */
    @Builder.Default
    private boolean requiresManagerApproval = true;

    /**
     * Whether VIP users can bypass manager approval.
     */
    @Builder.Default
    private boolean vipBypass = false;

    /**
     * Service options (if multi-option service).
     */
    private List<ServiceOption> options;

    /**
     * Similarity score when retrieved from search.
     */
    private transient double score;

    /**
     * Get a formatted summary for display.
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("**").append(nameEn).append("** (").append(serviceId).append(")\n");
        if (nameAr != null) {
            sb.append("_").append(nameAr).append("_\n");
        }
        sb.append("\n").append(descriptionEn).append("\n");
        if (url != null) {
            sb.append("\n[Open in Damee](").append(url).append(")");
        }
        return sb.toString();
    }

    /**
     * Get the workflow summary as a string.
     */
    public String getWorkflowSummary() {
        if (workflow == null || workflow.isEmpty()) {
            return "Standard workflow";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < workflow.size(); i++) {
            if (i > 0) sb.append(" → ");
            sb.append(workflow.get(i).getDescription());
        }
        return sb.toString();
    }

    /**
     * Get the first field prompt for guided creation.
     */
    public String getFirstFieldPrompt() {
        if (requiredFields != null && !requiredFields.isEmpty()) {
            return "Please provide: " + requiredFields.get(0);
        }
        return "Please describe your request in detail.";
    }

    /**
     * Workflow step definition.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowStep {
        private int order;
        private String description;
        private String team;
        private boolean requiresApproval;
        private String condition;
    }

    /**
     * Service option for multi-option services.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceOption {
        private String optionId;
        private String nameEn;
        private String nameAr;
        private String description;
        private List<WorkflowStep> workflow;
    }
}
