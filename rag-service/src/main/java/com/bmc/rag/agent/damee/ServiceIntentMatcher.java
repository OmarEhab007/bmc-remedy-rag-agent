package com.bmc.rag.agent.damee;

import com.bmc.rag.store.service.VectorStoreService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Service for matching user queries to Damee services.
 * Uses a combination of pattern matching and semantic search.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceIntentMatcher {

    private final VectorStoreService vectorStoreService;
    private final DameeServiceCatalog catalog;

    private static final String SOURCE_TYPE = "DAMEE_SERVICE";
    private static final float MIN_SCORE = 0.5f;
    private static final float HIGH_CONFIDENCE_THRESHOLD = 0.85f;

    /**
     * Intent patterns for common service requests.
     * Maps regex patterns to service IDs for fast matching.
     */
    private static final List<IntentPattern> INTENT_PATTERNS = List.of(
            // VPN and remote access
            new IntentPattern("(?i)(vpn|remote access|work from home|العمل عن بعد|شبكة افتراضية)", "10513"),

            // Email services
            new IntentPattern("(?i)(email|outlook|mailbox|storage|بريد|البريد الالكتروني)", "10242"),

            // Software installation
            new IntentPattern("(?i)(software|install|program|برنامج|تثبيت)", "10247"),

            // Server access
            new IntentPattern("(?i)(server|create server|new server|خادم)", "10209"),

            // Database access
            new IntentPattern("(?i)(database|db access|oracle|sql|قاعدة بيانات)", "10503"),

            // Application permissions
            new IntentPattern("(?i)(permission|access|grant|صلاحية|صلاحيات)", "10504"),

            // Telephony
            new IntentPattern("(?i)(phone|telephony|extension|هاتف)", "10254"),

            // Meetings
            new IntentPattern("(?i)(meeting|webex|teams|zoom|اجتماع)", "10257"),

            // Technical incident
            new IntentPattern("(?i)(technical|issue|problem|مشكلة|عطل)", "10101"),

            // Vehicle/Car services
            new IntentPattern("(?i)(car|vehicle|سيارة)", "10113"),

            // Shipping
            new IntentPattern("(?i)(shipping|smsa|delivery|شحن)", "10116"),

            // ID Card
            new IntentPattern("(?i)(id card|identity|badge|بطاقة)", "10120"),

            // Legal services
            new IntentPattern("(?i)(contract|legal|عقد|قانوني)", "10106"),

            // GIS/Geospatial
            new IntentPattern("(?i)(gis|geospatial|map|dashboard|جيومكاني|خريطة)", "10601")
    );

    // Pattern to detect problem/issue language (user reporting a problem, not requesting service)
    private static final Pattern PROBLEM_LANGUAGE_PATTERN = Pattern.compile(
        "(?i)(can't|cannot|couldn't|unable to|not working|doesn't work|won't|failed|failing|error|issue with|problem with|broken|stuck|crash|freeze|slow|timeout|denied|rejected|invalid|incorrect|help me fix|troubleshoot)"
    );

    /**
     * Match a user query to the most appropriate Damee service.
     *
     * @param userQuery The user's natural language query
     * @return Match result containing the matched service(s) and confidence
     */
    public ServiceMatchResult matchService(String userQuery) {
        if (userQuery == null || userQuery.isBlank()) {
            return ServiceMatchResult.noMatch("Empty query");
        }

        log.debug("Matching service for query: '{}'", userQuery);

        // IMPORTANT: Check if user is reporting a PROBLEM (should go to troubleshooting, not service request)
        if (PROBLEM_LANGUAGE_PATTERN.matcher(userQuery).find()) {
            log.debug("Problem language detected - not matching to service: '{}'", userQuery.substring(0, Math.min(50, userQuery.length())));
            return ServiceMatchResult.noMatch("User is reporting a problem - use RAG for troubleshooting");
        }

        // Step 1: Check explicit patterns first (fastest)
        Optional<DameeService> patternMatch = matchByPattern(userQuery);
        if (patternMatch.isPresent()) {
            DameeService service = patternMatch.get();
            log.debug("Pattern match found: {} ({})", service.getNameEn(), service.getServiceId());
            return ServiceMatchResult.exact(service);
        }

        // Step 2: Try keyword-based search in catalog
        List<DameeService> keywordMatches = catalog.searchByKeyword(userQuery, 5);
        if (!keywordMatches.isEmpty() && keywordMatches.get(0).getScore() > 8.0) {
            // High confidence keyword match
            DameeService service = keywordMatches.get(0);
            log.debug("High-confidence keyword match: {} (score: {})",
                    service.getNameEn(), service.getScore());
            return ServiceMatchResult.highConfidence(service);
        }

        // Step 3: Fall back to semantic search in vector store
        List<VectorStoreService.SearchResult> semanticResults = vectorStoreService.searchByType(
                userQuery, SOURCE_TYPE, 5, MIN_SCORE);

        if (semanticResults.isEmpty()) {
            log.debug("No semantic matches found for query: '{}'", userQuery);
            return ServiceMatchResult.noMatch("No matching services found");
        }

        // Process semantic results
        List<DameeService> matchedServices = new ArrayList<>();
        for (VectorStoreService.SearchResult result : semanticResults) {
            String serviceId = result.getMetadata().get("service_id");
            if (serviceId != null) {
                DameeService service = catalog.getById(serviceId);
                if (service != null) {
                    service.setScore(result.getScore());
                    matchedServices.add(service);
                }
            }
        }

        // Deduplicate (same service may have multiple chunks matched)
        matchedServices = deduplicateByServiceId(matchedServices);

        if (matchedServices.isEmpty()) {
            return ServiceMatchResult.noMatch("No matching services found");
        }

        DameeService topMatch = matchedServices.get(0);

        // Check if top match has high confidence
        if (topMatch.getScore() > HIGH_CONFIDENCE_THRESHOLD) {
            log.debug("High-confidence semantic match: {} (score: {})",
                    topMatch.getNameEn(), topMatch.getScore());
            return ServiceMatchResult.highConfidence(topMatch);
        }

        // Multiple possible matches - need clarification
        if (matchedServices.size() > 1) {
            log.debug("Multiple matches found, requesting clarification");
            return ServiceMatchResult.clarificationNeeded(matchedServices);
        }

        // Single match with moderate confidence
        return ServiceMatchResult.highConfidence(topMatch);
    }

    /**
     * Match by explicit regex patterns.
     */
    private Optional<DameeService> matchByPattern(String query) {
        for (IntentPattern pattern : INTENT_PATTERNS) {
            if (pattern.pattern.matcher(query).find()) {
                DameeService service = catalog.getById(pattern.serviceId);
                if (service != null) {
                    return Optional.of(service);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Remove duplicate services (keep highest score).
     */
    private List<DameeService> deduplicateByServiceId(List<DameeService> services) {
        Map<String, DameeService> seen = new LinkedHashMap<>();
        for (DameeService service : services) {
            String id = service.getServiceId();
            if (!seen.containsKey(id) || service.getScore() > seen.get(id).getScore()) {
                seen.put(id, service);
            }
        }
        return new ArrayList<>(seen.values());
    }

    /**
     * Get service categories for display when no match is found.
     */
    public String getServiceCategoriesDisplay() {
        return catalog.getCategorySummary();
    }

    // ========================
    // Inner Classes
    // ========================

    /**
     * Intent pattern for fast matching.
     */
    private record IntentPattern(Pattern pattern, String serviceId) {
        IntentPattern(String regex, String serviceId) {
            this(Pattern.compile(regex), serviceId);
        }
    }

    /**
     * Result of service matching.
     */
    @Data
    @Builder
    public static class ServiceMatchResult {
        private MatchType type;
        private DameeService service;
        private List<DameeService> matches;
        private String message;
        private double confidence;

        public enum MatchType {
            EXACT,              // Pattern matched exactly
            HIGH_CONFIDENCE,    // Semantic match > 85%
            CLARIFICATION,      // Multiple matches, need user input
            NO_MATCH            // No suitable match found
        }

        public static ServiceMatchResult exact(DameeService service) {
            return ServiceMatchResult.builder()
                    .type(MatchType.EXACT)
                    .service(service)
                    .matches(List.of(service))
                    .confidence(1.0)
                    .build();
        }

        public static ServiceMatchResult highConfidence(DameeService service) {
            return ServiceMatchResult.builder()
                    .type(MatchType.HIGH_CONFIDENCE)
                    .service(service)
                    .matches(List.of(service))
                    .confidence(service.getScore())
                    .build();
        }

        public static ServiceMatchResult clarificationNeeded(List<DameeService> matches) {
            return ServiceMatchResult.builder()
                    .type(MatchType.CLARIFICATION)
                    .matches(matches)
                    .confidence(matches.isEmpty() ? 0 : matches.get(0).getScore())
                    .build();
        }

        public static ServiceMatchResult noMatch(String message) {
            return ServiceMatchResult.builder()
                    .type(MatchType.NO_MATCH)
                    .message(message)
                    .matches(Collections.emptyList())
                    .confidence(0)
                    .build();
        }

        public boolean isHighConfidence() {
            return type == MatchType.EXACT || type == MatchType.HIGH_CONFIDENCE;
        }

        public boolean needsClarification() {
            return type == MatchType.CLARIFICATION;
        }

        public boolean isNoMatch() {
            return type == MatchType.NO_MATCH;
        }

        /**
         * Get a formatted message for the match result.
         */
        public String getDisplayMessage() {
            return switch (type) {
                case EXACT, HIGH_CONFIDENCE -> String.format(
                        "I found a matching service:\n\n" +
                                "**%s** (ID: %s)\n" +
                                "_%s_\n\n" +
                                "%s\n\n" +
                                "📋 **Workflow:** %s\n\n" +
                                "Is this the service you need?",
                        service.getNameEn(),
                        service.getServiceId(),
                        service.getNameAr() != null ? service.getNameAr() : "",
                        service.getDescriptionEn() != null ? service.getDescriptionEn() : "",
                        service.getWorkflowSummary());

                case CLARIFICATION -> {
                    StringBuilder sb = new StringBuilder("I found multiple services that might help:\n\n");
                    for (int i = 0; i < Math.min(matches.size(), 5); i++) {
                        DameeService s = matches.get(i);
                        String desc = s.getDescriptionEn() != null ? s.getDescriptionEn() : "";
                        String preview = desc.length() > 80 ? desc.substring(0, 80) + "..." : desc;
                        sb.append(String.format("%d. **%s** - %s\n", i + 1, s.getNameEn(), preview));
                    }
                    sb.append("\nWhich service do you need? (Enter number or describe more)");
                    yield sb.toString();
                }

                case NO_MATCH -> "I can help you with these service categories:\n\n" +
                        "1️⃣ **IT Services** - Accounts, permissions, software, network\n" +
                        "2️⃣ **Support Services** - Cars, shipping, security badges\n" +
                        "3️⃣ **Legal Services** - Contracts, legal opinions\n" +
                        "4️⃣ **Inspection Services** - Inspection requests\n" +
                        "5️⃣ **Geospatial Services** - GIS, dashboards, maps\n\n" +
                        "Which category, or describe what you need?";
            };
        }
    }
}
