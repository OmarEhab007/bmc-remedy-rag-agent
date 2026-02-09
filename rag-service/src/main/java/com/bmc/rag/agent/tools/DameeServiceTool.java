package com.bmc.rag.agent.tools;

import com.bmc.rag.agent.damee.*;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * LangChain4j tool for searching and retrieving Damee service catalog information.
 * Enables the agent to help users find the right service for their needs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DameeServiceTool {

    private final DameeServiceCatalog catalog;
    private final ServiceIntentMatcher intentMatcher;
    private final GuidedServiceCreator guidedCreator;
    private final WorkflowPreviewBuilder workflowBuilder;

    @Tool(name = "search_damee_services",
            value = "Search for available Damee services based on user needs. " +
                    "Use this when user asks about available services or needs help " +
                    "finding the right service for their request. Returns matching services " +
                    "with descriptions and URLs.")
    public String searchServices(
            @P("Keywords or description of what the user needs") String query,
            @P("Category filter (optional): IT Services, Support Services, Legal Consultation Services, " +
                    "Inspection Services, Geospatial Services. Leave empty for all categories.")
            String category) {

        log.info("Searching Damee services: query='{}', category='{}'", query, category);

        List<DameeService> matches;

        if (category != null && !category.isBlank()) {
            matches = catalog.search(query, category, 5);
        } else {
            matches = catalog.searchByKeyword(query, 5);
        }

        if (matches.isEmpty()) {
            return "No matching services found for: " + query + "\n\n" +
                    "Try describing your need differently, or choose from these categories:\n" +
                    catalog.getCategorySummary();
        }

        StringBuilder result = new StringBuilder("Found these services:\n\n");

        for (DameeService s : matches) {
            result.append(String.format("**%s** (ID: %s)\n", s.getNameEn(), s.getServiceId()));

            if (s.getNameAr() != null) {
                result.append(String.format("_%s_\n", s.getNameAr()));
            }

            if (s.getDescriptionEn() != null) {
                String desc = s.getDescriptionEn();
                if (desc.length() > 150) {
                    desc = desc.substring(0, 147) + "...";
                }
                result.append("- ").append(desc).append("\n");
            }

            result.append(String.format("- Category: %s\n", s.getCategory()));

            if (s.getUrl() != null) {
                result.append(String.format("- [Open in Damee](%s)\n", s.getUrl()));
            }

            result.append("\n");
        }

        result.append("To get full details about a service, use `get_service_details` with the service ID.");

        return result.toString();
    }

    @Tool(name = "get_service_details",
            value = "Get full details about a specific Damee service including " +
                    "description, workflow, required approvals, and how to submit. " +
                    "Use this after searching to provide detailed service information.")
    public String getServiceDetails(
            @P("Service ID from Damee catalog (e.g., 10504, 10513)") String serviceId) {

        log.info("Getting service details for: {}", serviceId);

        DameeService service = catalog.getById(serviceId);

        if (service == null) {
            return "Service not found with ID: " + serviceId + "\n\n" +
                    "Use `search_damee_services` to find available services.";
        }

        StringBuilder details = new StringBuilder();

        // Header
        details.append(String.format("## %s\n", service.getNameEn()));
        if (service.getNameAr() != null) {
            details.append(String.format("### %s\n\n", service.getNameAr()));
        }

        // Basic info
        details.append(String.format("**Service ID:** %s\n", service.getServiceId()));
        details.append(String.format("**Category:** %s\n", service.getCategory()));

        if (service.getSubcategory() != null) {
            details.append(String.format("**Subcategory:** %s\n", service.getSubcategory()));
        }

        details.append("\n");

        // Description
        if (service.getDescriptionEn() != null) {
            details.append("**Description:**\n");
            details.append(service.getDescriptionEn()).append("\n\n");
        }

        if (service.getDescriptionAr() != null) {
            details.append("**الوصف:**\n");
            details.append(service.getDescriptionAr()).append("\n\n");
        }

        // Workflow
        details.append("**Workflow:**\n");
        details.append(workflowBuilder.buildCompactWorkflow(service)).append("\n\n");

        // Approval info
        details.append("**Approval Requirements:**\n");
        details.append(String.format("- Manager Approval Required: %s\n",
                service.isRequiresManagerApproval() ? "Yes" : "No"));
        details.append(String.format("- VIP Bypass Available: %s\n",
                service.isVipBypass() ? "Yes" : "No"));
        details.append("\n");

        // URL
        if (service.getUrl() != null) {
            details.append(String.format("**[Submit Request in Damee](%s)**\n\n", service.getUrl()));
        }

        // Keywords
        if (service.getKeywords() != null && !service.getKeywords().isEmpty()) {
            details.append("**Related Keywords:** ");
            details.append(String.join(", ", service.getKeywords()));
            details.append("\n");
        }

        return details.toString();
    }

    @Tool(name = "list_service_categories",
            value = "List all available Damee service categories with service counts. " +
                    "Use this when user wants to browse available services or doesn't know " +
                    "what service they need.")
    public String listCategories() {
        log.info("Listing Damee service categories");

        StringBuilder sb = new StringBuilder();
        sb.append("## Damee Service Categories\n\n");
        sb.append(catalog.getCategorySummary()).append("\n");

        sb.append("Total services available: ").append(catalog.getServiceCount()).append("\n\n");

        sb.append("To see services in a category, use `search_damee_services` with the category name.\n");
        sb.append("Example: search_damee_services(query=\"access\", category=\"IT Services\")");

        return sb.toString();
    }

    @Tool(name = "get_services_by_category",
            value = "Get all services in a specific category. " +
                    "Use this when user wants to see all services in IT Services, " +
                    "Support Services, Legal, Inspection, or Geospatial categories.")
    public String getServicesByCategory(
            @P("Category name: 'IT Services', 'Support Services', 'Legal Consultation Services', " +
                    "'Inspection Services', or 'Geospatial Services'") String category) {

        log.info("Getting services for category: {}", category);

        List<DameeService> services = catalog.getByCategory(category);

        if (services.isEmpty()) {
            return "No services found in category: " + category + "\n\n" +
                    "Available categories:\n" + catalog.getCategorySummary();
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("## %s (%d %s)\n\n", category, services.size(), services.size() == 1 ? "service" : "services"));

        for (DameeService s : services) {
            sb.append(String.format("- **%s** (ID: %s)\n", s.getNameEn(), s.getServiceId()));

            if (s.getDescriptionEn() != null) {
                String desc = s.getDescriptionEn();
                if (desc.length() > 80) {
                    desc = desc.substring(0, 77) + "...";
                }
                sb.append(String.format("  _%s_\n", desc));
            }
        }

        sb.append("\nFor full details, use `get_service_details` with the service ID.");

        return sb.toString();
    }

    @Tool(name = "match_user_intent",
            value = "Match a user's natural language request to the most appropriate Damee service. " +
                    "Use this to help users find the right service based on their description of what they need. " +
                    "Returns high-confidence match, multiple options, or category suggestions.")
    public String matchUserIntent(
            @P("The user's natural language description of what they need") String userQuery) {

        log.info("Matching intent for: {}", userQuery);

        ServiceIntentMatcher.ServiceMatchResult result = intentMatcher.matchService(userQuery);

        return result.getDisplayMessage();
    }

    @Tool(name = "start_guided_request",
            value = "Start a guided service request flow for a specific Damee service. " +
                    "This will begin collecting required information from the user. " +
                    "Use after user confirms which service they want.")
    public String startGuidedRequest(
            @P("Service ID to create request for") String serviceId,
            @P("Session ID for the current conversation") String sessionId,
            @P("User ID of the requester") String userId) {

        log.info("Starting guided request for service {} in session {}", serviceId, sessionId);

        DameeService service = catalog.getById(serviceId);

        if (service == null) {
            return "Service not found with ID: " + serviceId + "\n" +
                    "Use `search_damee_services` to find the correct service.";
        }

        // Start guided flow
        GuidedServiceCreator.GuidedResponse response = guidedCreator.processMessage(
                sessionId, userId, "start " + serviceId);

        if (response.isError()) {
            return "Error starting request: " + response.getErrorMessage();
        }

        return String.format(
                "Starting request for: **%s**\n\n" +
                        "I'll guide you through the required information.\n\n" +
                        "%s",
                service.getNameEn(),
                response.getMessage());
    }

    @Tool(name = "get_service_workflow",
            value = "Get the detailed approval workflow for a Damee service. " +
                    "Shows all steps, approvers, and estimated timeline. " +
                    "Use when user asks about the approval process or how long something will take.")
    public String getServiceWorkflow(
            @P("Service ID from Damee catalog") String serviceId) {

        log.info("Getting workflow for service: {}", serviceId);

        DameeService service = catalog.getById(serviceId);

        if (service == null) {
            return "Service not found with ID: " + serviceId;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("## Workflow for: %s\n\n", service.getNameEn()));

        sb.append(workflowBuilder.buildDetailedWorkflow(service, null));

        sb.append("\n**Notes:**\n");
        if (service.isVipBypass()) {
            sb.append("- VIP users may bypass manager approval\n");
        }
        if (service.isRequiresManagerApproval()) {
            sb.append("- Manager approval is required\n");
        }

        return sb.toString();
    }
}
