package com.bmc.rag.api.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects user intent for tool invocation.
 * Analyzes user messages to determine if a tool should be called.
 */
@Slf4j
@Service
public class ToolIntentDetector {

    // Patterns for incident creation intent - more flexible to catch natural language
    private static final Pattern CREATE_INCIDENT_PATTERN = Pattern.compile(
        "(?i)(i\\s+want\\s+to\\s+|i\\s+need\\s+to\\s+|can\\s+you\\s+|please\\s+|i\\s+would\\s+like\\s+to\\s+)?" +
        "(create|open|submit|raise|log|file|report)\\s+(an?\\s+)?(new\\s+)?" +
        "(technical\\s+)?(incident|ticket|issue|case)[:.]?\\s*(.+)?",
        Pattern.DOTALL
    );

    // Pattern for explicit tool invocation
    private static final Pattern EXPLICIT_CREATE_PATTERN = Pattern.compile(
        "(?i)use\\s+(the\\s+)?create_incident\\s+tool",
        Pattern.DOTALL
    );

    // Patterns for confirmation
    private static final Pattern CONFIRM_PATTERN = Pattern.compile(
        "(?i)^\\s*(confirm|yes|proceed|approve|go ahead|do it|execute|submit it)\\s*$"
    );

    // Pattern for cancel
    private static final Pattern CANCEL_PATTERN = Pattern.compile(
        "(?i)^\\s*(cancel|no|stop|abort|nevermind|forget it)\\s*$"
    );

    // Pattern for incident search
    private static final Pattern SEARCH_PATTERN = Pattern.compile(
        "(?i)(search|find|look\\s*up|show me|list)\\s+(for\\s+)?(similar\\s+)?(incidents?|tickets?|issues?|cases?)\\s*(about|for|with|related to|matching)?\\s*(.+)?",
        Pattern.DOTALL
    );

    // Pattern for viewing incident details
    private static final Pattern GET_INCIDENT_PATTERN = Pattern.compile(
        "(?i)(show|get|view|display|what('s| is| are))\\s+(the\\s+)?(details?|info(rmation)?|status)\\s+(of|for|about)\\s+(incident\\s+)?([A-Z]{2,4}\\d+)",
        Pattern.DOTALL
    );

    // Patterns for service requests (Damee services)
    private static final Pattern SERVICE_REQUEST_PATTERN = Pattern.compile(
        "(?i)(request|need|want|طلب|أحتاج|أريد)\\s+(a\\s+|an\\s+)?(vpn|software|email|server|database|permission|phone|meeting|car|shipping|access|service|خدمة|برنامج|سيارة|هاتف)",
        Pattern.DOTALL
    );

    // Patterns for specific Damee services
    private static final Pattern VPN_PATTERN = Pattern.compile(
        "(?i)(vpn|remote access|work from home|العمل عن بعد|شبكة افتراضية)"
    );
    private static final Pattern SOFTWARE_PATTERN = Pattern.compile(
        "(?i)(install|software|program|application|برنامج|تثبيت)"
    );
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "(?i)(email|mailbox|outlook|storage|بريد|البريد)"
    );
    private static final Pattern SERVER_PATTERN = Pattern.compile(
        "(?i)(server|خادم|سيرفر)"
    );
    private static final Pattern DATABASE_PATTERN = Pattern.compile(
        "(?i)(database|db|oracle|sql|قاعدة بيانات)"
    );
    private static final Pattern PERMISSION_PATTERN = Pattern.compile(
        "(?i)(permission|access|grant|صلاحية|صلاحيات)"
    );
    private static final Pattern CAR_PATTERN = Pattern.compile(
        "(?i)(car|vehicle|سيارة)"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "(?i)(phone|telephony|extension|هاتف)"
    );
    private static final Pattern MEETING_PATTERN = Pattern.compile(
        "(?i)(meeting|webex|teams|zoom|اجتماع)"
    );
    private static final Pattern TECHNICAL_ISSUE_PATTERN = Pattern.compile(
        "(?i)(technical issue|technical incident|incident|problem|not working|broken|مشكلة|عطل|لا يعمل|بلاغ)"
    );

    /**
     * Detected intent result.
     */
    @Data
    public static class IntentResult {
        private final Intent intent;
        private final Map<String, String> parameters;

        public static IntentResult none() {
            return new IntentResult(Intent.NONE, Map.of());
        }

        public static IntentResult createIncident(String summary, String description) {
            return new IntentResult(Intent.CREATE_INCIDENT, Map.of(
                "summary", summary != null ? summary.trim() : "",
                "description", description != null ? description.trim() : ""
            ));
        }

        public static IntentResult confirm() {
            return new IntentResult(Intent.CONFIRM, Map.of());
        }

        public static IntentResult cancel() {
            return new IntentResult(Intent.CANCEL, Map.of());
        }

        public static IntentResult searchIncidents(String query) {
            return new IntentResult(Intent.SEARCH_INCIDENTS, Map.of(
                "query", query != null ? query.trim() : ""
            ));
        }

        public static IntentResult getIncident(String incidentId) {
            return new IntentResult(Intent.GET_INCIDENT, Map.of(
                "incident_id", incidentId.toUpperCase()
            ));
        }

        public static IntentResult serviceRequest(String query) {
            return new IntentResult(Intent.SERVICE_REQUEST, Map.of(
                "query", query != null ? query.trim() : ""
            ));
        }
    }

    /**
     * Intent types.
     */
    public enum Intent {
        NONE,
        CREATE_INCIDENT,
        SEARCH_INCIDENTS,
        GET_INCIDENT,
        SERVICE_REQUEST,
        CONFIRM,
        CANCEL
    }

    /**
     * Detect intent from user message.
     *
     * @param message User's message
     * @param hasTools Whether tools are enabled in the request
     * @return Detected intent with parameters
     */
    public IntentResult detectIntent(String message, boolean hasTools) {
        if (message == null || message.isBlank()) {
            return IntentResult.none();
        }

        // Only detect tool intents if tools are enabled
        if (!hasTools) {
            return IntentResult.none();
        }

        String trimmed = message.trim();

        // Check for confirmation first (simple pattern)
        if (CONFIRM_PATTERN.matcher(trimmed).matches()) {
            log.info("Detected CONFIRM intent");
            return IntentResult.confirm();
        }

        // Check for cancel
        if (CANCEL_PATTERN.matcher(trimmed).matches()) {
            log.info("Detected CANCEL intent");
            return IntentResult.cancel();
        }

        // Check for explicit tool invocation
        if (EXPLICIT_CREATE_PATTERN.matcher(trimmed).find()) {
            log.info("Detected explicit CREATE_INCIDENT tool request");
            // Extract summary and description from the rest of the message
            String remaining = trimmed.replaceFirst("(?i)use\\s+(the\\s+)?create_incident\\s+tool\\s+(to\\s+)?", "").trim();
            return parseCreateIncidentParams(remaining);
        }

        // Check for create incident intent
        Matcher createMatcher = CREATE_INCIDENT_PATTERN.matcher(trimmed);
        if (createMatcher.find()) {
            String afterKeyword = createMatcher.group(7); // Group index updated for new pattern
            if (afterKeyword != null && !afterKeyword.isBlank()) {
                log.info("Detected CREATE_INCIDENT intent: {}", afterKeyword.substring(0, Math.min(50, afterKeyword.length())));
                return parseCreateIncidentFromDescription(afterKeyword);
            }
            // If no description after keywords, still treat as create incident intent
            log.info("Detected CREATE_INCIDENT intent (no description provided)");
            return IntentResult.createIncident("New incident", "User requested to create a new incident");
        }

        // Check for incident search
        Matcher searchMatcher = SEARCH_PATTERN.matcher(trimmed);
        if (searchMatcher.find()) {
            String query = searchMatcher.group(6);
            if (query != null && !query.isBlank()) {
                log.info("Detected SEARCH_INCIDENTS intent: {}", query.substring(0, Math.min(50, query.length())));
                return IntentResult.searchIncidents(query.trim());
            }
        }

        // Check for get incident details
        Matcher getMatcher = GET_INCIDENT_PATTERN.matcher(trimmed);
        if (getMatcher.find()) {
            String incidentId = getMatcher.group(7);
            log.info("Detected GET_INCIDENT intent: {}", incidentId);
            return IntentResult.getIncident(incidentId);
        }

        // Check for Damee service request patterns
        if (isServiceRequestIntent(trimmed)) {
            log.info("Detected SERVICE_REQUEST intent: {}", trimmed.substring(0, Math.min(50, trimmed.length())));
            return IntentResult.serviceRequest(trimmed);
        }

        return IntentResult.none();
    }

    // Pattern to detect problem/issue language (user reporting a problem, not requesting service)
    private static final Pattern PROBLEM_LANGUAGE_PATTERN = Pattern.compile(
        "(?i)(can't|cannot|couldn't|unable to|not working|doesn't work|won't|failed|failing|error|issue|problem|broken|stuck|crash|freeze|slow|timeout|denied|rejected|invalid|incorrect)"
    );

    /**
     * Check if the message matches Damee service request patterns.
     * IMPORTANT: Does NOT match if the user is reporting a problem (should go to troubleshooting/RAG instead)
     */
    private boolean isServiceRequestIntent(String message) {
        // FIRST: Check if user is reporting a PROBLEM (not a service request)
        // If they mention problem language, this should go to troubleshooting/RAG, not service request
        if (PROBLEM_LANGUAGE_PATTERN.matcher(message).find()) {
            log.debug("Problem language detected - not a service request: {}", message.substring(0, Math.min(50, message.length())));
            return false;
        }

        // Check explicit service request pattern (need, want, request)
        if (SERVICE_REQUEST_PATTERN.matcher(message).find()) {
            return true;
        }

        // For service-specific patterns, only match if there's also request language
        // Don't match just "VPN" alone - need context like "I need VPN" or "request VPN"
        boolean hasRequestLanguage = Pattern.compile("(?i)(need|want|request|get|setup|configure|install|create|new|add|grant)").matcher(message).find();

        if (!hasRequestLanguage) {
            return false;
        }

        // Check specific service patterns (only if request language is present)
        return VPN_PATTERN.matcher(message).find() ||
               SOFTWARE_PATTERN.matcher(message).find() ||
               EMAIL_PATTERN.matcher(message).find() ||
               SERVER_PATTERN.matcher(message).find() ||
               DATABASE_PATTERN.matcher(message).find() ||
               PERMISSION_PATTERN.matcher(message).find() ||
               CAR_PATTERN.matcher(message).find() ||
               PHONE_PATTERN.matcher(message).find() ||
               MEETING_PATTERN.matcher(message).find();
    }

    /**
     * Parse create incident parameters from explicit invocation.
     */
    private IntentResult parseCreateIncidentParams(String text) {
        // Look for summary and description in various formats
        // Format 1: summary "..." description "..."
        Pattern paramPattern = Pattern.compile(
            "(?i)(?:with\\s+)?summary\\s+[\"']([^\"']+)[\"']\\s+(?:and\\s+)?description\\s+[\"']([^\"']+)[\"']"
        );
        Matcher m = paramPattern.matcher(text);
        if (m.find()) {
            return IntentResult.createIncident(m.group(1), m.group(2));
        }

        // Format 2: create incident with summary "..." and description "..."
        Pattern paramPattern2 = Pattern.compile(
            "(?i)create\\s+(?:an?\\s+)?incident\\s+with\\s+summary\\s+[\"']([^\"']+)[\"']\\s+and\\s+description\\s+[\"']([^\"']+)[\"']"
        );
        Matcher m2 = paramPattern2.matcher(text);
        if (m2.find()) {
            return IntentResult.createIncident(m2.group(1), m2.group(2));
        }

        // Fall back to using the whole text as both summary and description
        String summary = text.length() > 100 ? text.substring(0, 100) : text;
        return IntentResult.createIncident(summary, text);
    }

    /**
     * Parse create incident from natural language description.
     */
    private IntentResult parseCreateIncidentFromDescription(String description) {
        // Clean up the description
        String cleaned = description.trim();

        // Generate a summary (first sentence or first 100 chars)
        String summary;
        int dotIndex = cleaned.indexOf('.');
        if (dotIndex > 0 && dotIndex < 100) {
            summary = cleaned.substring(0, dotIndex);
        } else {
            summary = cleaned.length() > 100 ? cleaned.substring(0, 100) : cleaned;
        }

        // Capitalize first letter
        if (!summary.isEmpty()) {
            summary = Character.toUpperCase(summary.charAt(0)) + summary.substring(1);
        }

        return IntentResult.createIncident(summary, cleaned);
    }
}
