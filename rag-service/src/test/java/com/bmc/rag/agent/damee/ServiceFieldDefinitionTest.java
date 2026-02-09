package com.bmc.rag.agent.damee;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceFieldDefinitionTest {

    @Nested
    @DisplayName("getPrompt")
    class GetPrompt {

        @Test
        void getPrompt_englishLanguage_returnsEnglish() {
            var field = ServiceFieldDefinition.text("name", "Enter your name", "أدخل اسمك", true);
            assertThat(field.getPrompt("en")).isEqualTo("Enter your name");
        }

        @Test
        void getPrompt_arabicLanguage_returnsArabic() {
            var field = ServiceFieldDefinition.text("name", "Enter your name", "أدخل اسمك", true);
            assertThat(field.getPrompt("ar")).isEqualTo("أدخل اسمك");
        }

        @Test
        void getPrompt_arabicLanguageUppercase_returnsArabic() {
            var field = ServiceFieldDefinition.text("name", "Enter your name", "أدخل اسمك", true);
            assertThat(field.getPrompt("AR")).isEqualTo("أدخل اسمك");
        }

        @Test
        void getPrompt_arabicNull_returnsEnglish() {
            var field = ServiceFieldDefinition.text("name", "Enter your name", null, true);
            assertThat(field.getPrompt("ar")).isEqualTo("Enter your name");
        }

        @Test
        void getPrompt_unknownLanguage_returnsEnglish() {
            var field = ServiceFieldDefinition.text("name", "Enter your name", "أدخل اسمك", true);
            assertThat(field.getPrompt("fr")).isEqualTo("Enter your name");
        }
    }

    @Nested
    @DisplayName("validate - required fields")
    class ValidateRequired {

        @Test
        void validate_requiredFieldNull_returnsInvalid() {
            var field = ServiceFieldDefinition.text("name", "Name", null, true);
            var result = field.validate(null);
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).contains("required");
        }

        @Test
        void validate_requiredFieldBlank_returnsInvalid() {
            var field = ServiceFieldDefinition.text("name", "Name", null, true);
            var result = field.validate("   ");
            assertThat(result.isValid()).isFalse();
        }

        @Test
        void validate_requiredFieldPresent_returnsValid() {
            var field = ServiceFieldDefinition.text("name", "Name", null, true);
            var result = field.validate("John");
            assertThat(result.isValid()).isTrue();
        }

        @Test
        void validate_optionalFieldNull_returnsValid() {
            var field = ServiceFieldDefinition.text("name", "Name", null, false);
            var result = field.validate(null);
            assertThat(result.isValid()).isTrue();
        }

        @Test
        void validate_optionalFieldBlank_returnsValid() {
            var field = ServiceFieldDefinition.text("name", "Name", null, false);
            var result = field.validate("   ");
            assertThat(result.isValid()).isTrue();
        }
    }

    @Nested
    @DisplayName("validate - length constraints")
    class ValidateLength {

        @Test
        void validate_underMinLength_returnsInvalid() {
            var field = ServiceFieldDefinition.builder()
                    .fieldName("desc").promptEn("Description").type(ServiceFieldDefinition.FieldType.TEXT)
                    .required(false).minLength(5).build();
            var result = field.validate("Hi");
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).contains("Minimum length");
        }

        @Test
        void validate_overMaxLength_returnsInvalid() {
            var field = ServiceFieldDefinition.builder()
                    .fieldName("code").promptEn("Code").type(ServiceFieldDefinition.FieldType.TEXT)
                    .required(false).maxLength(5).build();
            var result = field.validate("toolong");
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).contains("Maximum length");
        }

        @Test
        void validate_withinLengthBounds_returnsValid() {
            var field = ServiceFieldDefinition.builder()
                    .fieldName("code").promptEn("Code").type(ServiceFieldDefinition.FieldType.TEXT)
                    .required(false).minLength(2).maxLength(10).build();
            var result = field.validate("Hello");
            assertThat(result.isValid()).isTrue();
        }
    }

    @Nested
    @DisplayName("validate - pattern matching")
    class ValidatePattern {

        @Test
        void validate_matchesPattern_returnsValid() {
            var field = ServiceFieldDefinition.builder()
                    .fieldName("id").promptEn("ID").type(ServiceFieldDefinition.FieldType.TEXT)
                    .required(false).validationPattern("^[A-Z]{3}\\d{3}$").build();
            var result = field.validate("ABC123");
            assertThat(result.isValid()).isTrue();
        }

        @Test
        void validate_noMatchPattern_returnsInvalid() {
            var field = ServiceFieldDefinition.builder()
                    .fieldName("id").promptEn("ID").type(ServiceFieldDefinition.FieldType.TEXT)
                    .required(false).validationPattern("^[A-Z]{3}\\d{3}$")
                    .validationErrorMessage("Must be 3 letters + 3 digits").build();
            var result = field.validate("abc");
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).isEqualTo("Must be 3 letters + 3 digits");
        }

        @Test
        void validate_noMatchPatternNoMessage_returnsDefaultError() {
            var field = ServiceFieldDefinition.builder()
                    .fieldName("id").promptEn("ID").type(ServiceFieldDefinition.FieldType.TEXT)
                    .required(false).validationPattern("^[0-9]+$").build();
            var result = field.validate("abc");
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).isEqualTo("Invalid format.");
        }
    }

    @Nested
    @DisplayName("validate - SELECT type")
    class ValidateSelect {

        @Test
        void validate_selectByIndex_returnsNormalizedValue() {
            var field = ServiceFieldDefinition.select("priority", "Priority", null, true,
                    List.of("Low", "Medium", "High"));
            var result = field.validate("2");
            assertThat(result.isValid()).isTrue();
            assertThat(result.getNormalizedValue()).isEqualTo("Medium");
        }

        @Test
        void validate_selectByName_returnsNormalizedValue() {
            var field = ServiceFieldDefinition.select("priority", "Priority", null, true,
                    List.of("Low", "Medium", "High"));
            var result = field.validate("high");
            assertThat(result.isValid()).isTrue();
            assertThat(result.getNormalizedValue()).isEqualTo("High");
        }

        @Test
        void validate_selectByPartialMatch_returnsNormalizedValue() {
            var field = ServiceFieldDefinition.select("priority", "Priority", null, true,
                    List.of("Low", "Medium", "High"));
            var result = field.validate("med");
            assertThat(result.isValid()).isTrue();
            assertThat(result.getNormalizedValue()).isEqualTo("Medium");
        }

        @Test
        void validate_selectInvalidOption_returnsInvalid() {
            var field = ServiceFieldDefinition.select("priority", "Priority", null, true,
                    List.of("Low", "Medium", "High"));
            var result = field.validate("urgent");
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).contains("valid option");
        }

        @Test
        void validate_selectIndexOutOfRange_fallsThrough() {
            var field = ServiceFieldDefinition.select("priority", "Priority", null, true,
                    List.of("Low", "Medium", "High"));
            var result = field.validate("5");
            assertThat(result.isValid()).isFalse();
        }

        @Test
        void validate_selectIndexZero_fallsThrough() {
            var field = ServiceFieldDefinition.select("priority", "Priority", null, true,
                    List.of("Low", "Medium", "High"));
            var result = field.validate("0");
            assertThat(result.isValid()).isFalse();
        }
    }

    @Nested
    @DisplayName("validate - EMAIL type")
    class ValidateEmail {

        @Test
        void validate_validEmail_returnsValid() {
            var field = ServiceFieldDefinition.builder()
                    .fieldName("email").promptEn("Email").type(ServiceFieldDefinition.FieldType.EMAIL)
                    .required(false).build();
            var result = field.validate("user@example.com");
            assertThat(result.isValid()).isTrue();
        }

        @Test
        void validate_invalidEmail_returnsInvalid() {
            var field = ServiceFieldDefinition.builder()
                    .fieldName("email").promptEn("Email").type(ServiceFieldDefinition.FieldType.EMAIL)
                    .required(false).build();
            var result = field.validate("not-an-email");
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).contains("email");
        }
    }

    @Nested
    @DisplayName("validate - PHONE type")
    class ValidatePhone {

        @Test
        void validate_validPhone_returnsValid() {
            var field = ServiceFieldDefinition.builder()
                    .fieldName("phone").promptEn("Phone").type(ServiceFieldDefinition.FieldType.PHONE)
                    .required(false).build();
            var result = field.validate("+966 50 123 4567");
            assertThat(result.isValid()).isTrue();
        }

        @Test
        void validate_invalidPhone_returnsInvalid() {
            var field = ServiceFieldDefinition.builder()
                    .fieldName("phone").promptEn("Phone").type(ServiceFieldDefinition.FieldType.PHONE)
                    .required(false).build();
            var result = field.validate("abc");
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).contains("phone");
        }
    }

    @Nested
    @DisplayName("validate - NUMBER type")
    class ValidateNumber {

        @Test
        void validate_validNumber_returnsValid() {
            var field = ServiceFieldDefinition.builder()
                    .fieldName("qty").promptEn("Quantity").type(ServiceFieldDefinition.FieldType.NUMBER)
                    .required(false).build();
            var result = field.validate("42");
            assertThat(result.isValid()).isTrue();
        }

        @Test
        void validate_validDecimal_returnsValid() {
            var field = ServiceFieldDefinition.builder()
                    .fieldName("amount").promptEn("Amount").type(ServiceFieldDefinition.FieldType.NUMBER)
                    .required(false).build();
            var result = field.validate("3.14");
            assertThat(result.isValid()).isTrue();
        }

        @Test
        void validate_invalidNumber_returnsInvalid() {
            var field = ServiceFieldDefinition.builder()
                    .fieldName("qty").promptEn("Quantity").type(ServiceFieldDefinition.FieldType.NUMBER)
                    .required(false).build();
            var result = field.validate("abc");
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).contains("number");
        }
    }

    @Nested
    @DisplayName("getFormattedPrompt")
    class GetFormattedPrompt {

        @Test
        void getFormattedPrompt_textField_returnsPromptOnly() {
            var field = ServiceFieldDefinition.text("name", "Enter your name", null, true);
            assertThat(field.getFormattedPrompt("en")).isEqualTo("Enter your name");
        }

        @Test
        void getFormattedPrompt_selectField_includesOptions() {
            var field = ServiceFieldDefinition.select("priority", "Select priority:", null, true,
                    List.of("Low", "Medium", "High"));
            String formatted = field.getFormattedPrompt("en");
            assertThat(formatted).contains("Select priority:");
            assertThat(formatted).contains("1. Low");
            assertThat(formatted).contains("2. Medium");
            assertThat(formatted).contains("3. High");
        }

        @Test
        void getFormattedPrompt_withPlaceholder_includesPlaceholder() {
            var field = ServiceFieldDefinition.builder()
                    .fieldName("name").promptEn("Name").type(ServiceFieldDefinition.FieldType.TEXT)
                    .required(false).placeholder("e.g., John Doe").build();
            String formatted = field.getFormattedPrompt("en");
            assertThat(formatted).contains("e.g., John Doe");
        }
    }

    @Nested
    @DisplayName("ValidationResult")
    class ValidationResultTest {

        @Test
        void valid_createsValidResult() {
            var result = ServiceFieldDefinition.ValidationResult.valid();
            assertThat(result.isValid()).isTrue();
            assertThat(result.getErrorMessage()).isNull();
        }

        @Test
        void validWithValue_createsValidResultWithNormalizedValue() {
            var result = ServiceFieldDefinition.ValidationResult.validWithValue("Normalized");
            assertThat(result.isValid()).isTrue();
            assertThat(result.getNormalizedValue()).isEqualTo("Normalized");
        }

        @Test
        void invalid_createsInvalidResultWithMessage() {
            var result = ServiceFieldDefinition.ValidationResult.invalid("Error occurred");
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).isEqualTo("Error occurred");
        }

        @Test
        void getValue_withNormalizedValue_returnsNormalized() {
            var result = ServiceFieldDefinition.ValidationResult.validWithValue("Normalized");
            assertThat(result.getValue("original")).isEqualTo("Normalized");
        }

        @Test
        void getValue_withoutNormalizedValue_returnsOriginal() {
            var result = ServiceFieldDefinition.ValidationResult.valid();
            assertThat(result.getValue("original")).isEqualTo("original");
        }
    }

    @Nested
    @DisplayName("Builder Helpers")
    class BuilderHelpers {

        @Test
        void text_createsTextFieldDefinition() {
            var field = ServiceFieldDefinition.text("name", "Name", "الاسم", true);
            assertThat(field.getFieldName()).isEqualTo("name");
            assertThat(field.getPromptEn()).isEqualTo("Name");
            assertThat(field.getPromptAr()).isEqualTo("الاسم");
            assertThat(field.getType()).isEqualTo(ServiceFieldDefinition.FieldType.TEXT);
            assertThat(field.isRequired()).isTrue();
        }

        @Test
        void select_createsSelectFieldDefinition() {
            var field = ServiceFieldDefinition.select("type", "Type", "النوع", false,
                    List.of("A", "B", "C"));
            assertThat(field.getFieldName()).isEqualTo("type");
            assertThat(field.getType()).isEqualTo(ServiceFieldDefinition.FieldType.SELECT);
            assertThat(field.getOptions()).containsExactly("A", "B", "C");
            assertThat(field.isRequired()).isFalse();
        }
    }
}
