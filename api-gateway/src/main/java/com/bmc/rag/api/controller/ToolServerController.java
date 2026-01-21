package com.bmc.rag.api.controller;

import com.bmc.rag.agent.confirmation.ConfirmationService;
import com.bmc.rag.agent.confirmation.PendingAction;
import com.bmc.rag.agent.security.AgenticRateLimiter;
import com.bmc.rag.agent.security.InputValidator;
import com.bmc.rag.api.dto.toolserver.*;
import com.bmc.rag.connector.dto.IncidentCreationRequest;
import com.bmc.rag.store.service.VectorStoreService;
import com.bmc.rag.store.service.VectorStoreService.SearchResult;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tool Server Controller for Open WebUI integration.
 *
 * Provides REST endpoints that Open WebUI tools can call to interact
 * with BMC Remedy data. Implements OpenAPI-compatible interface for
 * tool server connections.
 *
 * Endpoints:
 * - GET  /tool-server/openapi.json         - OpenAPI specification
 * - POST /tool-server/incidents/search     - Semantic incident search
 * - GET  /tool-server/incidents/{id}       - Get incident details
 * - POST /tool-server/incidents            - Create incident (staged)
 * - PUT  /tool-server/incidents/{id}       - Update incident
 * - GET  /tool-server/incidents/{id}/worklogs - Get incident work logs
 * - POST /tool-server/knowledge/search     - Search knowledge base
 * - GET  /tool-server/knowledge/{id}       - Get KB article details
 * - POST /tool-server/actions/confirm      - Confirm staged action
 * - POST /tool-server/actions/cancel       - Cancel staged action
 * - GET  /tool-server/actions/pending      - List pending actions
 */
@Slf4j
@RestController
@RequestMapping("/tool-server")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "agentic.enabled", havingValue = "true")
public class ToolServerController {

    private final VectorStoreService vectorStoreService;
    private final ConfirmationService confirmationService;
    private final InputValidator inputValidator;
    private final AgenticRateLimiter rateLimiter;
    private final com.bmc.rag.connector.service.WorkLogService workLogService;

    @Value("${agentic.duplicate-detection.similarity-threshold:0.85}")
    private double duplicateThreshold;

    // =========================================================================
    // OpenAPI Specification
    // =========================================================================

    /**
     * Serve the OpenAPI specification for tool discovery.
     */
    @GetMapping(value = "/openapi.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getOpenApiSpec() {
        try {
            ClassPathResource resource = new ClassPathResource("static/tool-server-openapi.json");
            String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            return ResponseEntity.ok(content);
        } catch (IOException e) {
            log.error("Failed to load OpenAPI spec: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("{\"error\": \"Failed to load OpenAPI specification\"}");
        }
    }

    // =========================================================================
    // Incident Search Operations
    // =========================================================================

    /**
     * Search for incidents using semantic search.
     *
     * @param request The search request
     * @return Search results with similarity scores
     */
    @PostMapping("/incidents/search")
    @RateLimiter(name = "chat")
    public ResponseEntity<ToolSearchResponse> searchIncidents(
            @Valid @RequestBody ToolSearchRequest request) {

        log.info("Tool Server: Searching incidents with query: '{}'",
            truncate(request.getQuery(), 100));

        long startTime = System.currentTimeMillis();

        try {
            List<SearchResult> results = vectorStoreService.searchByType(
                request.getQuery(),
                "Incident",
                request.getLimit() != null ? request.getLimit() : 10,
                request.getMinScore() != null ? request.getMinScore() : 0.3
            );

            if (results.isEmpty()) {
                return ResponseEntity.ok(ToolSearchResponse.empty(request.getQuery()));
            }

            List<SearchResultItem> items = results.stream()
                .map(this::mapToSearchResultItem)
                .collect(Collectors.toList());

            // Check for potential duplicates (high similarity)
            long duplicateCount = results.stream()
                .filter(r -> r.getScore() >= duplicateThreshold)
                .count();

            ToolSearchResponse response = ToolSearchResponse.builder()
                .query(request.getQuery())
                .totalResults(items.size())
                .returnedResults(items.size())
                .results(items)
                .hasPotentialDuplicates(duplicateCount > 0)
                .executionTimeMs(System.currentTimeMillis() - startTime)
                .build();

            if (duplicateCount > 0) {
                response.setMessage(String.format(
                    "Found %d highly similar incident(s) that may be duplicates (>%d%% similar).",
                    duplicateCount, (int)(duplicateThreshold * 100)));
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error searching incidents: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ToolSearchResponse.builder()
                    .query(request.getQuery())
                    .message("Error searching incidents: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Get details of a specific incident.
     *
     * @param incidentId The incident number (e.g., INC000000001)
     * @return Full incident details
     */
    @GetMapping("/incidents/{incidentId}")
    @RateLimiter(name = "chat")
    public ResponseEntity<IncidentDetailResponse> getIncidentDetails(
            @PathVariable String incidentId) {

        log.info("Tool Server: Getting details for incident: {}", incidentId);

        try {
            // Search for the specific incident by ID
            List<SearchResult> results = vectorStoreService.searchByType(
                incidentId, "Incident", 10, 0.0);

            // Filter to exact match
            SearchResult exactMatch = results.stream()
                .filter(r -> incidentId.equalsIgnoreCase(r.getSourceId()))
                .findFirst()
                .orElse(null);

            if (exactMatch == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(IncidentDetailResponse.notFound(incidentId));
            }

            // Build response from search result metadata
            IncidentDetailResponse response = buildIncidentDetailFromSearchResult(exactMatch);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting incident details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(IncidentDetailResponse.builder()
                    .incidentNumber(incidentId)
                    .found(false)
                    .errorMessage("Error retrieving incident: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Get work logs for an incident.
     *
     * @param incidentId The incident number (e.g., INC000000001)
     * @return List of work log entries
     */
    @GetMapping("/incidents/{incidentId}/worklogs")
    @RateLimiter(name = "chat")
    public ResponseEntity<WorkLogResponse> getIncidentWorkLogs(
            @PathVariable String incidentId) {

        log.info("Tool Server: Getting work logs for incident: {}", incidentId);

        try {
            List<com.bmc.rag.connector.service.WorkLogService.WorkLogEntry> workLogs =
                workLogService.getWorkLogsForIncident(incidentId);

            List<WorkLogItem> items = workLogs.stream()
                .map(wl -> WorkLogItem.builder()
                    .workLogId(wl.workLogId())
                    .type(wl.workLogTypeName())
                    .description(wl.description())
                    .submitter(wl.submitter())
                    .submitDate(wl.submitDate() != null ? wl.submitDate().toString() : null)
                    .viewAccess(wl.viewAccess())
                    .build())
                .toList();

            return ResponseEntity.ok(WorkLogResponse.builder()
                .incidentNumber(incidentId)
                .totalCount(items.size())
                .workLogs(items)
                .build());

        } catch (Exception e) {
            log.error("Error getting work logs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(WorkLogResponse.builder()
                    .incidentNumber(incidentId)
                    .errorMessage("Error retrieving work logs: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Create a new incident (staged for confirmation).
     *
     * @param request The creation request
     * @param jwt The authenticated user
     * @return Creation response with action ID for confirmation
     */
    @PostMapping("/incidents")
    @RateLimiter(name = "chat")
    public ResponseEntity<CreateIncidentResponse> createIncident(
            @Valid @RequestBody CreateIncidentRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = extractUserId(jwt, request.getSessionId());
        log.info("Tool Server: Creating incident for user: {}", userId);

        // Check rate limit
        if (rateLimiter.isRateLimited(userId)) {
            var status = rateLimiter.getStatus(userId);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(CreateIncidentResponse.rateLimited(status.maxPerHour()));
        }

        // Validate inputs
        var summaryValidation = inputValidator.validateSummary(request.getSummary());
        if (!summaryValidation.valid()) {
            return ResponseEntity.badRequest()
                .body(CreateIncidentResponse.validationError(
                    "Summary: " + String.join(", ", summaryValidation.errors())));
        }

        var descValidation = inputValidator.validateDescription(request.getDescription());
        if (!descValidation.valid()) {
            return ResponseEntity.badRequest()
                .body(CreateIncidentResponse.validationError(
                    "Description: " + String.join(", ", descValidation.errors())));
        }

        try {
            // Check for duplicates unless skipped
            List<SearchResultItem> similarIncidents = null;
            if (!Boolean.TRUE.equals(request.getSkipDuplicateCheck())) {
                String searchQuery = request.getSummary() + " " + request.getDescription();
                List<SearchResult> duplicates = vectorStoreService.searchByType(
                    searchQuery, "Incident", 5, duplicateThreshold);

                if (!duplicates.isEmpty()) {
                    similarIncidents = duplicates.stream()
                        .map(this::mapToSearchResultItem)
                        .collect(Collectors.toList());
                }
            }

            // Build internal creation request
            IncidentCreationRequest internalRequest = IncidentCreationRequest.builder()
                .summary(summaryValidation.sanitizedInput())
                .description(descValidation.sanitizedInput())
                .impact(request.getImpact())
                .urgency(request.getUrgency())
                .requesterFirstName(request.getRequesterFirstName())
                .requesterLastName(request.getRequesterLastName())
                .requesterCompany(request.getRequesterCompany())
                .categoryTier1(request.getCategory())
                .categoryTier2(request.getSubCategory())
                .categoryTier3(request.getItem())
                .assignedGroup(request.getAssignedGroup())
                .serviceType(request.getServiceType())
                .configurationItem(request.getConfigurationItem())
                .location(request.getLocation())
                .createdBy(userId)
                .sessionId(request.getSessionId())
                .build();

            // Stage the action
            String sessionId = request.getSessionId() != null
                ? request.getSessionId()
                : "tool-server-" + System.currentTimeMillis();

            PendingAction action = confirmationService.stageIncidentCreation(
                sessionId, userId, internalRequest);

            // Return response
            if (similarIncidents != null && !similarIncidents.isEmpty()) {
                return ResponseEntity.ok(CreateIncidentResponse.stagedWithDuplicates(
                    action.getActionId(),
                    action.getPreview(),
                    action.getExpiresAt(),
                    similarIncidents
                ));
            } else {
                return ResponseEntity.ok(CreateIncidentResponse.staged(
                    action.getActionId(),
                    action.getPreview(),
                    action.getExpiresAt()
                ));
            }

        } catch (Exception e) {
            log.error("Error creating incident: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CreateIncidentResponse.failed("Failed to stage incident creation", e.getMessage()));
        }
    }

    /**
     * Update an existing incident (staged for confirmation).
     *
     * @param incidentId The incident number to update
     * @param request The update request
     * @param jwt The authenticated user
     * @return Update response
     */
    @PutMapping("/incidents/{incidentId}")
    @RateLimiter(name = "chat")
    public ResponseEntity<UpdateIncidentResponse> updateIncident(
            @PathVariable String incidentId,
            @Valid @RequestBody UpdateIncidentRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = extractUserId(jwt, request.getSessionId());
        log.info("Tool Server: Updating incident {} for user: {}", incidentId, userId);

        if (!request.hasUpdates()) {
            return ResponseEntity.badRequest()
                .body(UpdateIncidentResponse.failed(incidentId,
                    "No updates specified", "At least one field must be provided"));
        }

        // Check rate limit
        if (rateLimiter.isRateLimited(userId)) {
            var status = rateLimiter.getStatus(userId);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(UpdateIncidentResponse.failed(incidentId,
                    "Rate limit exceeded", "Max " + status.maxPerHour() + " operations per hour"));
        }

        try {
            // Convert API request to connector request
            com.bmc.rag.connector.dto.IncidentUpdateRequest updateRequest =
                com.bmc.rag.connector.dto.IncidentUpdateRequest.builder()
                    .incidentNumber(incidentId)
                    .status(parseStatus(request.getStatus()))
                    .resolution(request.getResolution())
                    .resolutionCategoryTier1(request.getResolutionCategory())
                    .resolutionCategoryTier2(request.getResolutionSubCategory())
                    .resolutionCategoryTier3(request.getResolutionItem())
                    .workLog(request.getWorkLogNotes())
                    .workLogType(parseWorkLogType(request.getWorkLogType()))
                    .impact(request.getImpact())
                    .urgency(request.getUrgency())
                    .assignedGroup(request.getAssignedGroup())
                    .updatedBy(userId)
                    .sessionId(request.getSessionId())
                    .build();

            String sessionId = request.getSessionId() != null
                ? request.getSessionId()
                : "tool-server-" + System.currentTimeMillis();

            // Stage the update action
            PendingAction action = confirmationService.stageIncidentUpdate(
                sessionId, userId, updateRequest);

            return ResponseEntity.ok(UpdateIncidentResponse.staged(
                incidentId,
                action.getActionId(),
                action.getPreview(),
                action.getExpiresAt()
            ));

        } catch (Exception e) {
            log.error("Error staging incident update: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(UpdateIncidentResponse.failed(incidentId,
                    "Failed to stage update", e.getMessage()));
        }
    }

    /**
     * Parse status string to integer value.
     */
    private Integer parseStatus(String status) {
        if (status == null) return null;
        return switch (status.toLowerCase().replace(" ", "")) {
            case "new" -> 0;
            case "assigned" -> 1;
            case "inprogress" -> 2;
            case "pending" -> 3;
            case "resolved" -> 4;
            case "closed" -> 5;
            case "cancelled" -> 6;
            default -> null;
        };
    }

    /**
     * Parse work log type string to integer value.
     */
    private Integer parseWorkLogType(String workLogType) {
        if (workLogType == null) return null;
        return switch (workLogType.toLowerCase().replace(" ", "")) {
            case "generalinformation" -> 0;
            case "workinglog" -> 1;
            case "emailsystem" -> 2;
            case "customercommunication" -> 3;
            case "customerinbound" -> 4;
            case "customeroutbound" -> 5;
            case "resolutioncommunications" -> 6;
            default -> 1; // Default to Working Log
        };
    }

    // =========================================================================
    // Knowledge Base Operations
    // =========================================================================

    /**
     * Search the knowledge base using semantic search.
     *
     * @param request The search request
     * @return Search results with similarity scores
     */
    @PostMapping("/knowledge/search")
    @RateLimiter(name = "chat")
    public ResponseEntity<ToolSearchResponse> searchKnowledge(
            @Valid @RequestBody ToolSearchRequest request) {

        log.info("Tool Server: Searching knowledge with query: '{}'",
            truncate(request.getQuery(), 100));

        long startTime = System.currentTimeMillis();

        try {
            List<SearchResult> results = vectorStoreService.searchByType(
                request.getQuery(),
                "KnowledgeArticle",
                request.getLimit() != null ? request.getLimit() : 10,
                request.getMinScore() != null ? request.getMinScore() : 0.3
            );

            if (results.isEmpty()) {
                return ResponseEntity.ok(ToolSearchResponse.empty(request.getQuery()));
            }

            List<SearchResultItem> items = results.stream()
                .map(this::mapToSearchResultItem)
                .collect(Collectors.toList());

            ToolSearchResponse response = ToolSearchResponse.builder()
                .query(request.getQuery())
                .totalResults(items.size())
                .returnedResults(items.size())
                .results(items)
                .executionTimeMs(System.currentTimeMillis() - startTime)
                .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error searching knowledge: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ToolSearchResponse.builder()
                    .query(request.getQuery())
                    .message("Error searching knowledge base: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Get details of a specific knowledge article.
     *
     * @param articleId The article ID (e.g., KB000000001)
     * @return Full article details
     */
    @GetMapping("/knowledge/{articleId}")
    @RateLimiter(name = "chat")
    public ResponseEntity<KnowledgeDetailResponse> getKnowledgeArticle(
            @PathVariable String articleId) {

        log.info("Tool Server: Getting details for knowledge article: {}", articleId);

        try {
            // Search for the specific article by ID
            List<SearchResult> results = vectorStoreService.searchByType(
                articleId, "KnowledgeArticle", 10, 0.0);

            // Filter to exact match
            SearchResult exactMatch = results.stream()
                .filter(r -> articleId.equalsIgnoreCase(r.getSourceId()))
                .findFirst()
                .orElse(null);

            if (exactMatch == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(KnowledgeDetailResponse.notFound(articleId));
            }

            // Build response from search result
            KnowledgeDetailResponse response = buildKnowledgeDetailFromSearchResult(exactMatch);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting knowledge article: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(KnowledgeDetailResponse.builder()
                    .articleId(articleId)
                    .found(false)
                    .errorMessage("Error retrieving article: " + e.getMessage())
                    .build());
        }
    }

    // =========================================================================
    // Action Management Operations
    // =========================================================================

    /**
     * Confirm a staged action.
     *
     * @param actionId The action ID to confirm
     * @param sessionId The session ID for ownership verification
     * @param jwt The authenticated user
     * @return Confirmation response
     */
    @PostMapping("/actions/confirm")
    @RateLimiter(name = "chat")
    public ResponseEntity<ConfirmActionResponse> confirmAction(
            @RequestParam String actionId,
            @RequestParam String sessionId,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = extractUserId(jwt, sessionId);
        log.info("Tool Server: Confirming action {} for session {}", actionId, sessionId);

        // Check rate limit
        if (rateLimiter.isRateLimited(userId)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ConfirmActionResponse.failed(actionId,
                    "Rate limit exceeded", "Please wait before confirming more actions"));
        }

        try {
            var result = confirmationService.confirm(actionId, sessionId, userId);

            if (result.success()) {
                // Determine record type from the record ID pattern
                String recordType = determineRecordType(result.recordId());
                return ResponseEntity.ok(ConfirmActionResponse.executed(
                    actionId, result.recordId(), recordType));
            } else if (result.message() != null && result.message().toLowerCase().contains("expired")) {
                return ResponseEntity.ok(ConfirmActionResponse.expired(actionId));
            } else {
                return ResponseEntity.badRequest()
                    .body(ConfirmActionResponse.failed(actionId, result.message(), null));
            }

        } catch (Exception e) {
            log.error("Error confirming action: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ConfirmActionResponse.failed(actionId,
                    "Failed to confirm action", e.getMessage()));
        }
    }

    /**
     * Cancel a staged action.
     *
     * @param actionId The action ID to cancel
     * @param sessionId The session ID for ownership verification
     * @param jwt The authenticated user
     * @return Cancellation response
     */
    @PostMapping("/actions/cancel")
    public ResponseEntity<ConfirmActionResponse> cancelAction(
            @RequestParam String actionId,
            @RequestParam String sessionId,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = extractUserId(jwt, sessionId);
        log.info("Tool Server: Cancelling action {} for session {}", actionId, sessionId);

        try {
            var result = confirmationService.cancel(actionId, sessionId, userId);

            if (result.cancelled()) {
                return ResponseEntity.ok(ConfirmActionResponse.cancelled(actionId));
            } else {
                return ResponseEntity.badRequest()
                    .body(ConfirmActionResponse.notFound(actionId));
            }

        } catch (Exception e) {
            log.error("Error cancelling action: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ConfirmActionResponse.failed(actionId,
                    "Failed to cancel action", e.getMessage()));
        }
    }

    /**
     * List pending actions for a session.
     *
     * @param sessionId The session ID
     * @param jwt The authenticated user
     * @return List of pending actions
     */
    @GetMapping("/actions/pending")
    public ResponseEntity<List<Map<String, Object>>> getPendingActions(
            @RequestParam String sessionId,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = extractUserId(jwt, sessionId);
        log.info("Tool Server: Getting pending actions for session {}", sessionId);

        List<PendingAction> pending = confirmationService.getPendingActionsForSession(sessionId);

        List<Map<String, Object>> result = pending.stream()
            .filter(a -> a.belongsTo(sessionId, userId))
            .map(a -> Map.<String, Object>of(
                "actionId", a.getActionId(),
                "actionType", a.getActionType().name(),
                "preview", a.getPreview(),
                "stagedAt", a.getStagedAt().toString(),
                "expiresAt", a.getExpiresAt().toString(),
                "status", a.getStatus().name()
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // =========================================================================
    // Work Order Operations (placeholder)
    // =========================================================================

    /**
     * Search for work orders using semantic search.
     */
    @PostMapping("/workorders/search")
    @RateLimiter(name = "chat")
    public ResponseEntity<ToolSearchResponse> searchWorkOrders(
            @Valid @RequestBody ToolSearchRequest request) {

        log.info("Tool Server: Searching work orders with query: '{}'",
            truncate(request.getQuery(), 100));

        long startTime = System.currentTimeMillis();

        try {
            List<SearchResult> results = vectorStoreService.searchByType(
                request.getQuery(),
                "WorkOrder",
                request.getLimit() != null ? request.getLimit() : 10,
                request.getMinScore() != null ? request.getMinScore() : 0.3
            );

            if (results.isEmpty()) {
                return ResponseEntity.ok(ToolSearchResponse.empty(request.getQuery()));
            }

            List<SearchResultItem> items = results.stream()
                .map(this::mapToSearchResultItem)
                .collect(Collectors.toList());

            ToolSearchResponse response = ToolSearchResponse.builder()
                .query(request.getQuery())
                .totalResults(items.size())
                .returnedResults(items.size())
                .results(items)
                .executionTimeMs(System.currentTimeMillis() - startTime)
                .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error searching work orders: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ToolSearchResponse.builder()
                    .query(request.getQuery())
                    .message("Error searching work orders: " + e.getMessage())
                    .build());
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    /**
     * Map a VectorStoreService.SearchResult to a SearchResultItem.
     */
    private SearchResultItem mapToSearchResultItem(SearchResult result) {
        Map<String, String> metadata = result.getMetadata();

        return SearchResultItem.builder()
            .id(result.getSourceId())
            .type(result.getSourceType())
            .title(metadata != null ? metadata.get("title") : null)
            .snippet(truncate(result.getTextSegment(), 300))
            .score((double) result.getScore())
            .status(metadata != null ? metadata.get("status") : null)
            .metadata(metadata)
            .build();
    }

    /**
     * Build IncidentDetailResponse from a SearchResult.
     */
    private IncidentDetailResponse buildIncidentDetailFromSearchResult(SearchResult result) {
        Map<String, String> metadata = result.getMetadata();

        return IncidentDetailResponse.builder()
            .incidentNumber(result.getSourceId())
            .summary(metadata != null ? metadata.get("title") : null)
            .description(result.getTextSegment())
            .status(metadata != null ? metadata.get("status") : null)
            .assignedGroup(metadata != null ? metadata.get("assigned_group") : null)
            .categoryPath(metadata != null ? metadata.get("category") : null)
            .found(true)
            .build();
    }

    /**
     * Build KnowledgeDetailResponse from a SearchResult.
     */
    private KnowledgeDetailResponse buildKnowledgeDetailFromSearchResult(SearchResult result) {
        Map<String, String> metadata = result.getMetadata();

        return KnowledgeDetailResponse.builder()
            .articleId(result.getSourceId())
            .title(metadata != null ? metadata.get("title") : null)
            .content(result.getTextSegment())
            .status(metadata != null ? metadata.get("status") : null)
            .categoryPath(metadata != null ? metadata.get("category") : null)
            .assignedGroup(metadata != null ? metadata.get("assigned_group") : null)
            .found(true)
            .build();
    }

    /**
     * Extract user ID from JWT or fallback to session-based ID.
     */
    private String extractUserId(Jwt jwt, String sessionId) {
        if (jwt != null) {
            String sub = jwt.getClaimAsString("sub");
            if (sub != null) return sub;

            String email = jwt.getClaimAsString("email");
            if (email != null) return email;
        }
        return "session:" + (sessionId != null ? sessionId : "anonymous");
    }

    /**
     * Truncate text for logging/display.
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    /**
     * Determine record type from record ID pattern.
     * INC* -> Incident, WO* -> WorkOrder, KB* -> KnowledgeArticle
     */
    private String determineRecordType(String recordId) {
        if (recordId == null) return "Record";
        String upper = recordId.toUpperCase();
        if (upper.startsWith("INC")) return "Incident";
        if (upper.startsWith("WO")) return "WorkOrder";
        if (upper.startsWith("KB")) return "KnowledgeArticle";
        if (upper.startsWith("CR")) return "ChangeRequest";
        return "Record";
    }
}
