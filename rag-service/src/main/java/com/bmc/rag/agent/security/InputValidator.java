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
     */
    public ValidationResult validateSummary(String input) {
        return validateField(input, "Summary", MAX_SUMMARY_LENGTH, true);
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
