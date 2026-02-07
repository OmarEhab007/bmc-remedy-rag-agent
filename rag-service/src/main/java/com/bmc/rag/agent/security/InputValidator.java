package com.bmc.rag.agent.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Security-focused input validation for agentic operations.
 * Provides sanitization, prompt injection detection, and input constraints.
 */
@Slf4j
@Component
public class InputValidator {

    // Maximum field lengths per BMC Remedy constraints
    private static final int MAX_SUMMARY_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 32000;
    private static final int MAX_CATEGORY_LENGTH = 120;
    private static final int MAX_NAME_LENGTH = 50;

    // Prompt injection detection patterns
    private static final List<Pattern> INJECTION_PATTERNS = List.of(
        // Direct instruction override attempts
        Pattern.compile("(?i)(ignore|disregard|forget)\\s+(all\\s+)?(previous|prior|above)\\s+(instructions?|rules?|prompts?|context)"),
        Pattern.compile("(?i)(new|override|replace)\\s+(instructions?|rules?|system\\s+prompt)"),
        Pattern.compile("(?i)you\\s+are\\s+now\\s+a"),
        Pattern.compile("(?i)pretend\\s+(to\\s+be|you\\s+are)"),
        Pattern.compile("(?i)act\\s+as\\s+(if|a)"),
        Pattern.compile("(?i)roleplay\\s+as"),

        // System prompt extraction attempts
        Pattern.compile("(?i)(show|reveal|display|print|output)\\s+(your|the)\\s+(system\\s+)?prompt"),
        Pattern.compile("(?i)what\\s+(is|are)\\s+your\\s+(instructions?|rules?|guidelines?)"),
        Pattern.compile("(?i)repeat\\s+(your|the)\\s+(system\\s+)?prompt"),

        // Delimiter injection
        Pattern.compile("(?i)```\\s*(system|assistant|user)"),
        Pattern.compile("(?i)<\\|?(system|im_start|im_end)\\|?>"),
        Pattern.compile("(?i)\\[\\[?(SYSTEM|INST)\\]?\\]?"),

        // Code execution attempts
        Pattern.compile("(?i)(execute|run|eval)\\s*(\\(|:)"),
        Pattern.compile("(?i)(import|require|include)\\s+['\"]"),
        Pattern.compile("(?i)<script[^>]*>"),

        // SQL injection patterns (for fields that might reach DB)
        Pattern.compile("(?i)(union|select|insert|update|delete|drop|truncate)\\s+"),
        Pattern.compile("(?i)'\\s*(or|and)\\s+'?\\d"),
        Pattern.compile("(?i);\\s*(drop|delete|truncate)"),

        // Command injection
        Pattern.compile("(?i)[;&|]\\s*(rm|del|format|shutdown|reboot)"),
        Pattern.compile("(?i)\\$\\([^)]+\\)"),
        Pattern.compile("(?i)`[^`]+`")
    );

    // Suspicious content patterns (lower severity)
    private static final List<Pattern> SUSPICIOUS_PATTERNS = List.of(
        Pattern.compile("(?i)bypass\\s+(security|filter|validation)"),
        Pattern.compile("(?i)jailbreak"),
        Pattern.compile("(?i)do\\s+anything\\s+now"),
        Pattern.compile("(?i)dan\\s+mode"),
        Pattern.compile("(?i)developer\\s+mode")
    );

    // Vague summary patterns that indicate LLM failed to extract actual issue from context
    private static final List<Pattern> VAGUE_SUMMARY_PATTERNS = List.of(
        // Generic "this/the/my issue" patterns
        Pattern.compile("(?i)^\\s*(this|the|my|their|an?)\\s+(issue|problem|error|request|ticket)\\s*$"),
        Pattern.compile("(?i)^\\s*with\\s+(this|the|my)?\\s*(issue|problem)\\s*$"),
        // Vague "with X issue" patterns (e.g., "with email issue", "with login issue")
        Pattern.compile("(?i)^\\s*with\\s+\\w+\\s+(issue|problem)\\s*$"),
        // Generic service + issue patterns without specifics (e.g., "email issue", "login problem")
        Pattern.compile("(?i)^\\s*(email|login|access|network|computer|system|application|app|software|password)\\s+(issue|problem|error)\\s*$"),
        // Patterns starting with action words
        Pattern.compile("(?i)^\\s*(create|open|new|log|raise|file|submit)\\s+(an?\\s+)?(incident|ticket|request)\\s*$"),
        // Just the type word
        Pattern.compile("(?i)^\\s*(issue|problem|error)\\s*$"),
        // User reported patterns
        Pattern.compile("(?i)^\\s*user\\s+(has|have|reported|reports|experiencing|is having)\\s+(an?\\s+)?(issue|problem)\\s*$"),
        // Having/experiencing issue patterns
        Pattern.compile("(?i)^\\s*(having|experiencing)\\s+(an?\\s+)?(issue|problem|trouble)\\s+(with\\s+\\w+)?\\s*$"),
        // Help/assist patterns
        Pattern.compile("(?i)^\\s*(help|assist|support)\\s+(with|for)\\s+(this|the)?\\s*(issue|problem|user)?\\s*$"),
        // Need patterns without specifics
        Pattern.compile("(?i)^\\s*(need|needs|require|requires)\\s+(help|assistance|support)\\s*$"),
        // Incident for/about patterns without technical detail
        Pattern.compile("(?i)^\\s*(incident|ticket)\\s+(for|about|regarding)\\s+(this|the|my|an?)\\s*(issue|problem)?\\s*$")
    );

    // Keywords that indicate a summary has technical specificity (should NOT be flagged as vague)
    private static final List<String> TECHNICAL_SPECIFICITY_KEYWORDS = List.of(
        "error code", "cannot", "can't", "won't", "doesn't", "failed", "failing", "crash",
        "timeout", "connection", "unable to", "not working", "stopped", "not syncing",
        "not loading", "not responding", "freezing", "slow", "missing", "blank",
        "vpn", "outlook", "teams", "sharepoint", "sap", "oracle", "cisco", "citrix"
    );

    /**
     * Result of input validation.
     */
    public record ValidationResult(
        boolean valid,
        List<String> errors,
        List<String> warnings,
        String sanitizedInput
    ) {
        public static ValidationResult valid(String sanitizedInput) {
            return new ValidationResult(true, List.of(), List.of(), sanitizedInput);
        }

        public static ValidationResult valid(String sanitizedInput, List<String> warnings) {
            return new ValidationResult(true, List.of(), warnings, sanitizedInput);
        }

        public static ValidationResult invalid(List<String> errors) {
            return new ValidationResult(false, errors, List.of(), null);
        }
    }

    /**
     * Validate and sanitize a summary field.
     * Also checks for vague summaries that don't describe the actual technical issue.
     */
    public ValidationResult validateSummary(String input) {
        // First, check for vague summaries that indicate LLM failed to extract context
        if (input != null && !input.isBlank()) {
            // Check if summary has technical specificity - if so, it's likely good
            if (!hasTechnicalSpecificity(input)) {
                // No technical specificity found - check against vague patterns
                for (Pattern pattern : VAGUE_SUMMARY_PATTERNS) {
                    if (pattern.matcher(input).matches()) {
                        log.warn("Vague summary detected: '{}' - LLM likely failed to extract issue from context", input);
                        return ValidationResult.invalid(List.of(
                            "Summary is too vague. Please provide a specific description of the technical issue " +
                            "(e.g., 'Outlook email not syncing' instead of 'this issue')."
                        ));
                    }
                }

                // Additional check: if summary is too short and lacks technical detail
                if (input.length() < 15 && !containsTechnicalKeyword(input)) {
                    log.warn("Short vague summary detected: '{}' - likely needs more detail", input);
                    return ValidationResult.invalid(List.of(
                        "Summary is too vague. Please describe the specific technical issue " +
                        "(e.g., 'Cannot login to Outlook with username/password')."
                    ));
                }
            }
        }
        return validateField(input, "Summary", MAX_SUMMARY_LENGTH, true);
    }

    /**
     * Check if the summary has technical specificity (contains specific technical keywords).
     */
    private boolean hasTechnicalSpecificity(String input) {
        if (input == null) return false;
        String lower = input.toLowerCase();
        return TECHNICAL_SPECIFICITY_KEYWORDS.stream()
            .anyMatch(keyword -> lower.contains(keyword.toLowerCase()));
    }

    /**
     * Check if summary contains any technical keyword indicating real issue content.
     */
    private boolean containsTechnicalKeyword(String input) {
        if (input == null) return false;
        String lower = input.toLowerCase();
        // Check for basic technical indicators
        return lower.contains("error") ||
               lower.contains("fail") ||
               lower.contains("not ") ||
               lower.contains("can't") ||
               lower.contains("cannot") ||
               lower.contains("won't") ||
               lower.contains("sync") ||
               lower.contains("crash") ||
               lower.contains("slow") ||
               lower.contains("timeout") ||
               lower.contains("down") ||
               lower.contains("login") && (lower.contains("fail") || lower.contains("not ") || lower.contains("unable"));
    }

    /**
     * Validate and sanitize a description field.
     */
    public ValidationResult validateDescription(String input) {
        return validateField(input, "Description", MAX_DESCRIPTION_LENGTH, true);
    }

    /**
     * Validate and sanitize a category field.
     */
    public ValidationResult validateCategory(String input) {
        return validateField(input, "Category", MAX_CATEGORY_LENGTH, false);
    }

    /**
     * Validate and sanitize a name field.
     */
    public ValidationResult validateName(String input) {
        return validateField(input, "Name", MAX_NAME_LENGTH, false);
    }

    /**
     * General field validation.
     */
    public ValidationResult validateField(String input, String fieldName, int maxLength, boolean checkInjection) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (input == null || input.isBlank()) {
            errors.add(fieldName + " is required");
            return ValidationResult.invalid(errors);
        }

        // Check length
        if (input.length() > maxLength) {
            errors.add(fieldName + " exceeds maximum length of " + maxLength + " characters");
            return ValidationResult.invalid(errors);
        }

        // Check for prompt injection (if enabled for this field)
        if (checkInjection) {
            var injectionResult = detectPromptInjection(input);
            if (injectionResult.injectionDetected) {
                log.warn("Prompt injection detected in {}: {}", fieldName, injectionResult.matchedPattern);
                errors.add(fieldName + " contains potentially malicious content");
                return ValidationResult.invalid(errors);
            }
            if (injectionResult.suspiciousContent) {
                log.info("Suspicious content detected in {}: {}", fieldName, injectionResult.matchedPattern);
                warnings.add(fieldName + " contains suspicious content that will be reviewed");
            }
        }

        // Sanitize the input
        String sanitized = sanitizeInput(input);

        if (warnings.isEmpty()) {
            return ValidationResult.valid(sanitized);
        } else {
            return ValidationResult.valid(sanitized, warnings);
        }
    }

    /**
     * Detect prompt injection attempts.
     */
    public InjectionDetectionResult detectPromptInjection(String input) {
        if (input == null || input.isEmpty()) {
            return new InjectionDetectionResult(false, false, null);
        }

        // Check for injection patterns
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                return new InjectionDetectionResult(true, false, pattern.pattern());
            }
        }

        // Check for suspicious patterns (lower severity)
        for (Pattern pattern : SUSPICIOUS_PATTERNS) {
            if (pattern.matcher(input).find()) {
                return new InjectionDetectionResult(false, true, pattern.pattern());
            }
        }

        return new InjectionDetectionResult(false, false, null);
    }

    /**
     * Result of prompt injection detection.
     */
    public record InjectionDetectionResult(
        boolean injectionDetected,
        boolean suspiciousContent,
        String matchedPattern
    ) {}

    /**
     * Sanitize input by removing or escaping potentially dangerous content.
     */
    public String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }

        String sanitized = input;

        // Remove null bytes
        sanitized = sanitized.replace("\u0000", "");

        // Normalize whitespace (but preserve line breaks)
        sanitized = sanitized.replaceAll("[\\p{Zs}&&[^ \\t]]", " ");

        // Remove control characters except newline and tab
        sanitized = sanitized.replaceAll("[\\p{Cc}&&[^\\n\\t\\r]]", "");

        // Trim excessive whitespace
        sanitized = sanitized.replaceAll("[ \\t]+", " ");
        sanitized = sanitized.replaceAll("\\n{3,}", "\n\n");

        return sanitized.trim();
    }

    /**
     * Validate an impact value.
     */
    public boolean isValidImpact(Integer impact) {
        return impact != null && impact >= 1 && impact <= 4;
    }

    /**
     * Validate an urgency value.
     */
    public boolean isValidUrgency(Integer urgency) {
        return urgency != null && urgency >= 1 && urgency <= 4;
    }

    /**
     * Validate a priority value (0-based for work orders).
     */
    public boolean isValidPriority(Integer priority) {
        return priority != null && priority >= 0 && priority <= 3;
    }

    /**
     * Validate a work order type value.
     */
    public boolean isValidWorkOrderType(Integer type) {
        return type != null && type >= 0 && type <= 4;
    }

    /**
     * Check if input contains any HTML tags.
     */
    public boolean containsHtml(String input) {
        if (input == null) {
            return false;
        }
        return Pattern.compile("<[a-zA-Z][^>]*>").matcher(input).find();
    }

    /**
     * Strip HTML tags from input.
     */
    public String stripHtml(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("<[^>]+>", "");
    }
}
