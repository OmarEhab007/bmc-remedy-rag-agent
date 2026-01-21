package com.bmc.rag.agent.damee;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Definition of a service field for dynamic form building.
 * Defines prompts, validation, and options for each field.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceFieldDefinition {

    /**
     * Field types for different input handling.
     */
    public enum FieldType {
        TEXT,           // Free text input
        SELECT,         // Single selection from options
        MULTI_SELECT,   // Multiple selection from options
        DATE,           // Date input
        ATTACHMENT,     // File attachment
        EMAIL,          // Email address
        PHONE,          // Phone number
        NUMBER          // Numeric input
    }

    /**
     * Internal field name (used as key).
     */
    private String fieldName;

    /**
     * Display prompt in English.
     */
    private String promptEn;

    /**
     * Display prompt in Arabic.
     */
    private String promptAr;

    /**
     * Field type for validation and UI.
     */
    private FieldType type;

    /**
     * Whether this field is required.
     */
    private boolean required;

    /**
     * Options for SELECT/MULTI_SELECT types.
     */
    private List<String> options;

    /**
     * Regex pattern for validation.
     */
    private String validationPattern;

    /**
     * Error message for validation failure.
     */
    private String validationErrorMessage;

    /**
     * Default value.
     */
    private String defaultValue;

    /**
     * Placeholder text for input.
     */
    private String placeholder;

    /**
     * Maximum length for text fields.
     */
    private Integer maxLength;

    /**
     * Minimum length for text fields.
     */
    private Integer minLength;

    /**
     * Get the appropriate prompt based on language preference.
     */
    public String getPrompt(String language) {
        if ("ar".equalsIgnoreCase(language) && promptAr != null) {
            return promptAr;
        }
        return promptEn;
    }

    /**
     * Validate a value against this field's rules.
     */
    public ValidationResult validate(String value) {
        // Check required
        if (required && (value == null || value.isBlank())) {
            return ValidationResult.invalid("This field is required.");
        }

        if (value == null || value.isBlank()) {
            return ValidationResult.valid();
        }

        // Check length constraints
        if (minLength != null && value.length() < minLength) {
            return ValidationResult.invalid(
                    String.format("Minimum length is %d characters.", minLength));
        }

        if (maxLength != null && value.length() > maxLength) {
            return ValidationResult.invalid(
                    String.format("Maximum length is %d characters.", maxLength));
        }

        // Check pattern
        if (validationPattern != null && !value.matches(validationPattern)) {
            return ValidationResult.invalid(
                    validationErrorMessage != null ? validationErrorMessage : "Invalid format.");
        }

        // Check options for SELECT type
        if (type == FieldType.SELECT && options != null && !options.isEmpty()) {
            // Check if value is a valid option (by index or by value)
            try {
                int index = Integer.parseInt(value);
                if (index >= 1 && index <= options.size()) {
                    return ValidationResult.validWithValue(options.get(index - 1));
                }
            } catch (NumberFormatException ignored) {
            }

            // Check if value matches an option
            for (String option : options) {
                if (option.equalsIgnoreCase(value) || option.toLowerCase().contains(value.toLowerCase())) {
                    return ValidationResult.validWithValue(option);
                }
            }

            return ValidationResult.invalid(
                    "Please select a valid option (enter number or text).");
        }

        // Type-specific validation
        switch (type) {
            case EMAIL:
                if (!value.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                    return ValidationResult.invalid("Please enter a valid email address.");
                }
                break;
            case PHONE:
                if (!value.matches("^[+]?[0-9\\s-]{8,15}$")) {
                    return ValidationResult.invalid("Please enter a valid phone number.");
                }
                break;
            case NUMBER:
                try {
                    Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    return ValidationResult.invalid("Please enter a valid number.");
                }
                break;
            default:
                break;
        }

        return ValidationResult.valid();
    }

    /**
     * Get a formatted prompt with options if applicable.
     */
    public String getFormattedPrompt(String language) {
        StringBuilder sb = new StringBuilder(getPrompt(language));

        if (type == FieldType.SELECT && options != null && !options.isEmpty()) {
            sb.append("\n");
            for (int i = 0; i < options.size(); i++) {
                sb.append(String.format("%d. %s\n", i + 1, options.get(i)));
            }
        }

        if (placeholder != null) {
            sb.append("\n_").append(placeholder).append("_");
        }

        return sb.toString();
    }

    /**
     * Validation result.
     */
    @Data
    @Builder
    public static class ValidationResult {
        private boolean valid;
        private String errorMessage;
        private String normalizedValue;

        public static ValidationResult valid() {
            return ValidationResult.builder().valid(true).build();
        }

        public static ValidationResult validWithValue(String normalizedValue) {
            return ValidationResult.builder()
                    .valid(true)
                    .normalizedValue(normalizedValue)
                    .build();
        }

        public static ValidationResult invalid(String errorMessage) {
            return ValidationResult.builder()
                    .valid(false)
                    .errorMessage(errorMessage)
                    .build();
        }

        public String getValue(String originalValue) {
            return normalizedValue != null ? normalizedValue : originalValue;
        }
    }

    // ========================
    // Builder Helpers
    // ========================

    public static ServiceFieldDefinition text(String name, String promptEn, String promptAr, boolean required) {
        return ServiceFieldDefinition.builder()
                .fieldName(name)
                .promptEn(promptEn)
                .promptAr(promptAr)
                .type(FieldType.TEXT)
                .required(required)
                .build();
    }

    public static ServiceFieldDefinition select(String name, String promptEn, String promptAr,
                                                  boolean required, List<String> options) {
        return ServiceFieldDefinition.builder()
                .fieldName(name)
                .promptEn(promptEn)
                .promptAr(promptAr)
                .type(FieldType.SELECT)
                .required(required)
                .options(options)
                .build();
    }
}
