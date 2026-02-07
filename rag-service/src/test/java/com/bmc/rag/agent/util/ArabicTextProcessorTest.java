package com.bmc.rag.agent.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ArabicTextProcessor.
 * Tests Arabic language detection, dialect normalization, numeral conversion,
 * and text preprocessing for the Damee ITSM system.
 */
@DisplayName("ArabicTextProcessor Tests")
class ArabicTextProcessorTest {

    private ArabicTextProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new ArabicTextProcessor();
    }

    @Nested
    @DisplayName("Language Detection Tests")
    class LanguageDetectionTests {

        @Test
        @DisplayName("Should detect pure Arabic text")
        void detectPureArabic() {
            String arabicText = "أريد إعادة تعيين كلمة المرور";
            assertThat(processor.detectLanguage(arabicText))
                .isEqualTo(ArabicTextProcessor.Language.ARABIC);
        }

        @Test
        @DisplayName("Should detect pure English text")
        void detectPureEnglish() {
            String englishText = "I need to reset my password";
            assertThat(processor.detectLanguage(englishText))
                .isEqualTo(ArabicTextProcessor.Language.ENGLISH);
        }

        @Test
        @DisplayName("Should detect mixed Arabic-English text")
        void detectMixedText() {
            String mixedText = "أحتاج access للـ VPN";
            assertThat(processor.detectLanguage(mixedText))
                .isEqualTo(ArabicTextProcessor.Language.MIXED);
        }

        @Test
        @DisplayName("Should return UNKNOWN for null text")
        void detectNullText() {
            assertThat(processor.detectLanguage(null))
                .isEqualTo(ArabicTextProcessor.Language.UNKNOWN);
        }

        @Test
        @DisplayName("Should return UNKNOWN for empty text")
        void detectEmptyText() {
            assertThat(processor.detectLanguage(""))
                .isEqualTo(ArabicTextProcessor.Language.UNKNOWN);
        }

        @Test
        @DisplayName("Should return UNKNOWN for numbers only")
        void detectNumbersOnly() {
            assertThat(processor.detectLanguage("12345"))
                .isEqualTo(ArabicTextProcessor.Language.UNKNOWN);
        }
    }

    @Nested
    @DisplayName("Contains Arabic Tests")
    class ContainsArabicTests {

        @Test
        @DisplayName("Should return true for Arabic text")
        void containsArabicTrue() {
            assertThat(processor.containsArabic("مرحبا")).isTrue();
        }

        @Test
        @DisplayName("Should return false for English text")
        void containsArabicFalse() {
            assertThat(processor.containsArabic("hello")).isFalse();
        }

        @Test
        @DisplayName("Should return true for mixed text with Arabic")
        void containsArabicMixed() {
            assertThat(processor.containsArabic("VPN مشكلة في")).isTrue();
        }

        @Test
        @DisplayName("Should return false for null text")
        void containsArabicNull() {
            assertThat(processor.containsArabic(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("Gulf Dialect Normalization Tests")
    class GulfDialectTests {

        @Test
        @DisplayName("Should normalize 'وش' to 'ما' (what)")
        void normalizeWash() {
            String input = "وش المشكلة";
            String result = processor.normalizeDialectToMSA(input);
            assertThat(result).isEqualTo("ما المشكلة");
        }

        @Test
        @DisplayName("Should normalize 'ليش' to 'لماذا' (why)")
        void normalizeLeish() {
            String input = "ليش الجهاز بطيء";
            String result = processor.normalizeDialectToMSA(input);
            assertThat(result).isEqualTo("لماذا الجهاز بطيء");
        }

        @Test
        @DisplayName("Should normalize 'أبي' to 'أريد' (I want)")
        void normalizeAbi() {
            String input = "أبي مساعدة";
            String result = processor.normalizeDialectToMSA(input);
            assertThat(result).isEqualTo("أريد مساعدة");
        }

        @Test
        @DisplayName("Should normalize 'مو' to 'ليس' (not) - whole word only")
        void normalizeMo() {
            // "مو" as standalone word should be replaced
            // Note: "شغال" (Gulf) is also normalized to "يعمل" (MSA)
            String input = "الجهاز مو شغال";
            String result = processor.normalizeDialectToMSA(input);
            assertThat(result).isEqualTo("الجهاز ليس يعمل");

            // "مو" as part of "مشكلة" should NOT be replaced
            String input2 = "مشكلة في الشبكة";
            String result2 = processor.normalizeDialectToMSA(input2);
            assertThat(result2).isEqualTo("مشكلة في الشبكة");
        }

        @Test
        @DisplayName("Should handle 'شلون' (how in Gulf)")
        void normalizeShlon() {
            String input = "شلون أسوي هذا";
            String result = processor.normalizeDialectToMSA(input);
            assertThat(result).contains("كيف");
        }
    }

    @Nested
    @DisplayName("Egyptian Dialect Normalization Tests")
    class EgyptianDialectTests {

        @Test
        @DisplayName("Should normalize 'إيه' to 'ماذا' (what)")
        void normalizeEih() {
            String input = "إيه المشكلة";
            String result = processor.normalizeDialectToMSA(input);
            assertThat(result).isEqualTo("ماذا المشكلة");
        }

        @Test
        @DisplayName("Should normalize 'إزاي' to 'كيف' (how)")
        void normalizeIzay() {
            String input = "إزاي أحل المشكلة";
            String result = processor.normalizeDialectToMSA(input);
            assertThat(result).isEqualTo("كيف أحل المشكلة");
        }

        @Test
        @DisplayName("Should normalize 'فين' to 'أين' (where)")
        void normalizeFein() {
            String input = "فين الملف";
            String result = processor.normalizeDialectToMSA(input);
            assertThat(result).isEqualTo("أين الملف");
        }

        @Test
        @DisplayName("Should normalize 'مش' to 'ليس' (not)")
        void normalizeMesh() {
            // Note: "شغال" may also be normalized depending on dialect mapping
            String input = "مش شغال";
            String result = processor.normalizeDialectToMSA(input);
            // Both "مش" and "شغال" are normalized
            assertThat(result).isEqualTo("ليس يعمل");
        }
    }

    @Nested
    @DisplayName("Arabic Numeral Conversion Tests")
    class ArabicNumeralTests {

        @Test
        @DisplayName("Should convert Arabic-Indic numerals to Western")
        void convertArabicIndic() {
            String input = "رقم التذكرة ١٢٣٤٥";
            String expected = "رقم التذكرة 12345";
            assertThat(processor.convertArabicNumerals(input)).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should handle Persian numerals")
        void convertPersianNumerals() {
            String input = "۱۲۳۴۵۶۷۸۹۰";
            String expected = "1234567890";
            assertThat(processor.convertArabicNumerals(input)).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should preserve Western numerals")
        void preserveWesternNumerals() {
            String input = "INC12345";
            assertThat(processor.convertArabicNumerals(input)).isEqualTo(input);
        }

        @Test
        @DisplayName("Should handle mixed numerals")
        void convertMixedNumerals() {
            String input = "الطلب رقم ١٢٣ و 456";
            String expected = "الطلب رقم 123 و 456";
            assertThat(processor.convertArabicNumerals(input)).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should handle null input")
        void handleNullInput() {
            assertThat(processor.convertArabicNumerals(null)).isNull();
        }
    }

    @Nested
    @DisplayName("Ticket Number Extraction Tests")
    class TicketNumberTests {

        @ParameterizedTest
        @ValueSource(strings = {
            "رقم التذكرة ١٢٣٤٥",
            "INC12345",
            "تذكرة 12345",
            "Incident number: 12345"
        })
        @DisplayName("Should extract ticket numbers from various formats")
        void extractTicketNumber(String input) {
            String result = processor.extractTicketNumber(input);
            assertThat(result).isEqualTo("12345");
        }

        @Test
        @DisplayName("Should return null when no ticket number found")
        void noTicketNumber() {
            assertThat(processor.extractTicketNumber("مرحبا كيف حالك")).isNull();
        }
    }

    @Nested
    @DisplayName("Diacritics Removal Tests")
    class DiacriticsTests {

        @Test
        @DisplayName("Should remove Arabic diacritics (tashkeel)")
        void removeDiacritics() {
            String input = "كَلِمَةُ السِّرِّ";  // With tashkeel
            String expected = "كلمة السر";       // Without tashkeel
            assertThat(processor.removeDiacritics(input)).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should handle text without diacritics")
        void noDiacritics() {
            String input = "كلمة المرور";
            assertThat(processor.removeDiacritics(input)).isEqualTo(input);
        }
    }

    @Nested
    @DisplayName("Search Normalization Tests")
    class SearchNormalizationTests {

        @Test
        @DisplayName("Should normalize Alef forms")
        void normalizeAlef() {
            String input = "أحمد إبراهيم آدم";
            String result = processor.normalizeForSearch(input);
            // All Alef forms should be normalized to plain Alef
            assertThat(result).doesNotContain("أ", "إ", "آ");
        }

        @Test
        @DisplayName("Should normalize Yeh forms")
        void normalizeYeh() {
            String input = "على";  // With Alef Maqsura
            String result = processor.normalizeForSearch(input);
            assertThat(result).isEqualTo("علي");  // Normalized Yeh
        }

        @Test
        @DisplayName("Should remove tatweel")
        void removeTatweel() {
            String input = "كلـــمة";  // With tatweel for decoration
            String result = processor.removeTatweel(input);
            assertThat(result).isEqualTo("كلمة");
        }
    }

    @Nested
    @DisplayName("Text Direction Tests")
    class TextDirectionTests {

        @Test
        @DisplayName("Should return RTL for Arabic text")
        void arabicTextDirection() {
            assertThat(processor.getTextDirection("مرحبا"))
                .isEqualTo(ArabicTextProcessor.TextDirection.RTL);
        }

        @Test
        @DisplayName("Should return LTR for English text")
        void englishTextDirection() {
            assertThat(processor.getTextDirection("Hello"))
                .isEqualTo(ArabicTextProcessor.TextDirection.LTR);
        }

        @Test
        @DisplayName("Should return LTR for null/empty text")
        void nullTextDirection() {
            assertThat(processor.getTextDirection(null))
                .isEqualTo(ArabicTextProcessor.TextDirection.LTR);
            assertThat(processor.getTextDirection(""))
                .isEqualTo(ArabicTextProcessor.TextDirection.LTR);
        }
    }

    @Nested
    @DisplayName("Full Pipeline Tests")
    class FullPipelineTests {

        @Test
        @DisplayName("Should process Arabic query with dialect and numerals")
        void processFullArabicQuery() {
            String input = "وش رقم التذكرة ١٢٣٤٥";
            var result = processor.processArabicQuery(input);

            assertThat(result.wasModified()).isTrue();
            assertThat(result.language()).isEqualTo(ArabicTextProcessor.Language.ARABIC);
            assertThat(result.processed()).contains("12345");  // Numerals converted
            assertThat(result.processed()).contains("ما");     // Dialect normalized
        }

        @Test
        @DisplayName("Should not modify pure English query")
        void processEnglishQuery() {
            String input = "What is the ticket number 12345";
            var result = processor.processArabicQuery(input);

            assertThat(result.wasModified()).isFalse();
            assertThat(result.language()).isEqualTo(ArabicTextProcessor.Language.ENGLISH);
            assertThat(result.processed()).isEqualTo(input);
        }

        @Test
        @DisplayName("Should handle mixed language query")
        void processMixedQuery() {
            String input = "أريد reset للـ password";
            var result = processor.processArabicQuery(input);

            assertThat(result.language()).isIn(
                ArabicTextProcessor.Language.ARABIC,
                ArabicTextProcessor.Language.MIXED
            );
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle empty string")
        void handleEmptyString() {
            var result = processor.processArabicQuery("");
            assertThat(result.original()).isEmpty();
            assertThat(result.processed()).isEmpty();
        }

        @Test
        @DisplayName("Should handle whitespace only")
        void handleWhitespaceOnly() {
            var result = processor.processArabicQuery("   ");
            assertThat(result.wasModified()).isFalse();
        }

        @Test
        @DisplayName("Should handle very long text")
        void handleLongText() {
            String longText = "مرحبا ".repeat(1000);
            var result = processor.processArabicQuery(longText);
            assertThat(result.processed()).isNotNull();
        }

        @Test
        @DisplayName("Should handle special characters")
        void handleSpecialCharacters() {
            String input = "مشكلة في @#$%^& البريد";
            var result = processor.processArabicQuery(input);
            assertThat(result.processed()).isNotNull();
        }
    }
}
