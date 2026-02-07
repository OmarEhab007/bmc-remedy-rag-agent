package com.bmc.rag.agent.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Utility class for Arabic text processing in the Damee ITSM system.
 * Provides:
 * - Language detection (Arabic, English, Mixed)
 * - Dialect normalization (Gulf/Egyptian → MSA)
 * - Arabic numeral conversion
 * - Diacritics handling
 * - Text direction awareness
 */
@Slf4j
@Component
public class ArabicTextProcessor {

    // Unicode ranges for Arabic script
    private static final int ARABIC_START = 0x0600;
    private static final int ARABIC_END = 0x06FF;
    private static final int ARABIC_EXTENDED_A_START = 0x08A0;
    private static final int ARABIC_EXTENDED_A_END = 0x08FF;
    private static final int ARABIC_PRESENTATION_A_START = 0xFB50;
    private static final int ARABIC_PRESENTATION_A_END = 0xFDFF;
    private static final int ARABIC_PRESENTATION_B_START = 0xFE70;
    private static final int ARABIC_PRESENTATION_B_END = 0xFEFF;

    // Arabic diacritics (tashkeel) - often need to be removed for search
    private static final Pattern ARABIC_DIACRITICS = Pattern.compile(
        "[\u064B-\u065F\u0670]"  // Fatha, Damma, Kasra, Shadda, Sukun, etc.
    );

    // Arabic tatweel (kashida) - decorative elongation
    private static final char TATWEEL = '\u0640';

    // Common Gulf dialect words mapped to MSA equivalents
    private static final Map<String, String> GULF_TO_MSA = Map.ofEntries(
        // Question words
        Map.entry("وش", "ما"),           // What (Gulf) → What (MSA)
        Map.entry("ليش", "لماذا"),         // Why (Gulf) → Why (MSA)
        Map.entry("شلون", "كيف"),         // How (Gulf) → How (MSA)
        Map.entry("وين", "أين"),          // Where (Gulf) → Where (MSA)
        Map.entry("متى", "متى"),          // When (same)
        Map.entry("شنو", "ماذا"),         // What (Iraqi) → What (MSA)

        // Common verbs/expressions
        Map.entry("أبي", "أريد"),         // I want (Gulf) → I want (MSA)
        Map.entry("ابي", "أريد"),         // I want (Gulf without hamza)
        Map.entry("أبغى", "أريد"),        // I want (Gulf variant)
        Map.entry("ابغى", "أريد"),        // I want (Gulf variant)
        Map.entry("مو", "ليس"),           // Not (Gulf) → Not (MSA)
        Map.entry("مب", "ليس"),           // Not (UAE) → Not (MSA)
        Map.entry("ما يشتغل", "لا يعمل"), // Doesn't work (Gulf)
        Map.entry("شغال", "يعمل"),        // Working (Gulf)
        Map.entry("خربان", "معطل"),       // Broken (Gulf)
        Map.entry("زين", "حسناً"),        // Good/OK (Gulf)
        Map.entry("تمام", "حسناً"),       // OK (common)
        Map.entry("اوكي", "حسناً"),       // OK (transliterated)

        // Pronouns and particles
        Map.entry("أنته", "أنت"),         // You (Gulf m.) → You (MSA)
        Map.entry("أنتي", "أنتِ"),        // You (Gulf f.) → You (MSA f.)
        Map.entry("هذا", "هذا"),          // This (same)
        Map.entry("ذا", "هذا"),           // This (abbreviated)
        Map.entry("هاذا", "هذا"),         // This (Gulf variant)
        Map.entry("إحنا", "نحن"),         // We (Gulf) → We (MSA)
        Map.entry("احنا", "نحن"),         // We (Gulf variant)

        // IT-specific Gulf terms
        Map.entry("يهنق", "يتوقف"),       // Hangs/freezes (Gulf)
        Map.entry("يعلق", "يتوقف"),       // Hangs (Gulf)
        Map.entry("طافي", "مغلق"),        // Off/closed (Gulf)
        Map.entry("فاتح", "مفتوح")        // Open (Gulf)
    );

    // Common Egyptian dialect words mapped to MSA equivalents
    private static final Map<String, String> EGYPTIAN_TO_MSA = Map.ofEntries(
        // Question words
        Map.entry("إيه", "ماذا"),         // What (Egyptian)
        Map.entry("ايه", "ماذا"),         // What (Egyptian without hamza)
        Map.entry("إزاي", "كيف"),         // How (Egyptian)
        Map.entry("ازاي", "كيف"),         // How (Egyptian variant)
        Map.entry("فين", "أين"),          // Where (Egyptian)

        // Common verbs
        Map.entry("عايز", "أريد"),        // I want (Egyptian m.)
        Map.entry("عاوز", "أريد"),        // I want (Egyptian variant)
        Map.entry("عايزة", "أريد"),       // I want (Egyptian f.)
        Map.entry("مش", "ليس"),           // Not (Egyptian)
        Map.entry("بيشتغل", "يعمل"),      // Works (Egyptian present)
        Map.entry("مبيشتغلش", "لا يعمل"), // Doesn't work (Egyptian)
        Map.entry("باظ", "معطل"),         // Broken (Egyptian slang)
        Map.entry("كويس", "جيد"),         // Good (Egyptian)

        // Pronouns
        Map.entry("انا", "أنا"),          // I (same, variant spelling)
        Map.entry("إنت", "أنت"),          // You (Egyptian m.)
        Map.entry("إنتي", "أنتِ"),        // You (Egyptian f.)
        Map.entry("ده", "هذا"),           // This (Egyptian m.)
        Map.entry("دي", "هذه"),           // This (Egyptian f.)
        Map.entry("دول", "هؤلاء"),        // These (Egyptian)
        Map.entry("احنا", "نحن"),         // We (Egyptian)
        Map.entry("هما", "هم")            // They (Egyptian)
    );

    /**
     * Detected language of text.
     */
    public enum Language {
        ARABIC,
        ENGLISH,
        MIXED,
        UNKNOWN
    }

    /**
     * Detect the primary language of the text.
     *
     * @param text The text to analyze
     * @return The detected language
     */
    public Language detectLanguage(String text) {
        if (text == null || text.isBlank()) {
            return Language.UNKNOWN;
        }

        int arabicCount = 0;
        int latinCount = 0;
        int totalLetters = 0;

        for (char c : text.toCharArray()) {
            if (isArabicLetter(c)) {
                arabicCount++;
                totalLetters++;
            } else if (Character.isLetter(c) && c < 0x0100) {
                latinCount++;
                totalLetters++;
            }
        }

        if (totalLetters == 0) {
            return Language.UNKNOWN;
        }

        double arabicRatio = (double) arabicCount / totalLetters;
        double latinRatio = (double) latinCount / totalLetters;

        if (arabicRatio > 0.8) {
            return Language.ARABIC;
        } else if (latinRatio > 0.8) {
            return Language.ENGLISH;
        } else if (arabicRatio > 0.2 && latinRatio > 0.2) {
            return Language.MIXED;
        } else if (arabicRatio > latinRatio) {
            return Language.ARABIC;
        } else {
            return Language.ENGLISH;
        }
    }

    /**
     * Check if a character is an Arabic letter.
     */
    public boolean isArabicLetter(char c) {
        return (c >= ARABIC_START && c <= ARABIC_END) ||
               (c >= ARABIC_EXTENDED_A_START && c <= ARABIC_EXTENDED_A_END) ||
               (c >= ARABIC_PRESENTATION_A_START && c <= ARABIC_PRESENTATION_A_END) ||
               (c >= ARABIC_PRESENTATION_B_START && c <= ARABIC_PRESENTATION_B_END);
    }

    /**
     * Check if text contains any Arabic characters.
     */
    public boolean containsArabic(String text) {
        if (text == null) return false;
        for (char c : text.toCharArray()) {
            if (isArabicLetter(c)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Normalize Arabic dialect text to Modern Standard Arabic (MSA).
     * Handles Gulf and Egyptian dialects commonly used in the region.
     * Uses word boundary matching to avoid substring replacement issues.
     *
     * @param text The dialect text
     * @return Normalized MSA text
     */
    public String normalizeDialectToMSA(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String normalized = text;

        // Apply Gulf dialect normalization with word boundaries
        for (Map.Entry<String, String> entry : GULF_TO_MSA.entrySet()) {
            normalized = replaceWholeWord(normalized, entry.getKey(), entry.getValue());
        }

        // Apply Egyptian dialect normalization with word boundaries
        for (Map.Entry<String, String> entry : EGYPTIAN_TO_MSA.entrySet()) {
            normalized = replaceWholeWord(normalized, entry.getKey(), entry.getValue());
        }

        if (!normalized.equals(text)) {
            log.debug("Dialect normalized: '{}' → '{}'", truncate(text), truncate(normalized));
        }

        return normalized;
    }

    /**
     * Replace whole words only, respecting Arabic word boundaries.
     * Prevents substring replacement issues (e.g., "مو" in "مشكلة").
     */
    private String replaceWholeWord(String text, String word, String replacement) {
        // Arabic word boundaries: spaces, punctuation, or start/end of string
        // Using regex with Unicode word boundaries for Arabic
        String pattern = "(?<=^|\\s|[،؟!.,:;])" + Pattern.quote(word) + "(?=$|\\s|[،؟!.,:;])";
        return text.replaceAll(pattern, replacement);
    }

    /**
     * Remove Arabic diacritics (tashkeel) from text.
     * Useful for search normalization.
     */
    public String removeDiacritics(String text) {
        if (text == null) return null;
        return ARABIC_DIACRITICS.matcher(text).replaceAll("");
    }

    /**
     * Remove tatweel (kashida) characters from text.
     * Tatweel is decorative elongation that doesn't affect meaning.
     */
    public String removeTatweel(String text) {
        if (text == null) return null;
        return text.replace(String.valueOf(TATWEEL), "");
    }

    /**
     * Normalize Arabic text for search.
     * Removes diacritics, tatweel, and normalizes character forms.
     */
    public String normalizeForSearch(String text) {
        if (text == null) return null;

        String normalized = text;

        // Remove diacritics
        normalized = removeDiacritics(normalized);

        // Remove tatweel
        normalized = removeTatweel(normalized);

        // Normalize Alef forms (أ إ آ ا → ا)
        normalized = normalizeAlef(normalized);

        // Normalize Yeh forms (ي ى → ي)
        normalized = normalizeYeh(normalized);

        // Normalize Heh forms (ه ة → ه)
        normalized = normalizeHeh(normalized);

        return normalized;
    }

    /**
     * Normalize various Alef forms to plain Alef.
     */
    private String normalizeAlef(String text) {
        return text
            .replace('\u0623', '\u0627')  // أ → ا
            .replace('\u0625', '\u0627')  // إ → ا
            .replace('\u0622', '\u0627')  // آ → ا
            .replace('\u0671', '\u0627'); // ٱ → ا
    }

    /**
     * Normalize Yeh forms.
     */
    private String normalizeYeh(String text) {
        return text
            .replace('\u0649', '\u064A')  // ى → ي
            .replace('\u06CC', '\u064A'); // ی → ي (Persian Yeh)
    }

    /**
     * Normalize Heh forms.
     */
    private String normalizeHeh(String text) {
        return text
            .replace('\u0629', '\u0647'); // ة → ه (Tah Marbuta → Heh)
    }

    /**
     * Convert Arabic-Indic numerals to Western Arabic numerals.
     * ٠١٢٣٤٥٦٧٨٩ → 0123456789
     */
    public String convertArabicNumerals(String text) {
        if (text == null) return null;

        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c >= 0x0660 && c <= 0x0669) {
                // Arabic-Indic digits
                result.append((char) (c - 0x0660 + '0'));
            } else if (c >= 0x06F0 && c <= 0x06F9) {
                // Extended Arabic-Indic digits (Persian)
                result.append((char) (c - 0x06F0 + '0'));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Extract ticket/incident numbers from Arabic text.
     * Handles both Arabic and Western numerals.
     * Example: "تذكرة رقم ١٢٣٤٥" → "12345"
     */
    public String extractTicketNumber(String text) {
        if (text == null) return null;

        // First normalize numerals
        String normalized = convertArabicNumerals(text);

        // Pattern to find ticket numbers (digits, optionally with prefix)
        Pattern ticketPattern = Pattern.compile("(?:INC|WO|CR|KB)?\\s*(\\d{5,})");
        var matcher = ticketPattern.matcher(normalized);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * Determine the dominant text direction.
     */
    public TextDirection getTextDirection(String text) {
        if (text == null || text.isBlank()) {
            return TextDirection.LTR;
        }

        Language lang = detectLanguage(text);
        return lang == Language.ARABIC ? TextDirection.RTL : TextDirection.LTR;
    }

    /**
     * Text direction enum.
     */
    public enum TextDirection {
        LTR,  // Left-to-right (English)
        RTL   // Right-to-left (Arabic)
    }

    /**
     * Full preprocessing pipeline for Arabic queries.
     * Combines all normalization steps.
     */
    public ProcessedText processArabicQuery(String text) {
        if (text == null || text.isBlank()) {
            return new ProcessedText(text, text, Language.UNKNOWN, false);
        }

        Language originalLanguage = detectLanguage(text);
        String processed = text;
        boolean wasModified = false;

        if (containsArabic(text)) {
            // Normalize dialect to MSA
            String dialectNormalized = normalizeDialectToMSA(processed);
            if (!dialectNormalized.equals(processed)) {
                processed = dialectNormalized;
                wasModified = true;
            }

            // Convert Arabic numerals
            String numeralsConverted = convertArabicNumerals(processed);
            if (!numeralsConverted.equals(processed)) {
                processed = numeralsConverted;
                wasModified = true;
            }
        }

        return new ProcessedText(text, processed, originalLanguage, wasModified);
    }

    /**
     * Result of Arabic text processing.
     */
    public record ProcessedText(
        String original,
        String processed,
        Language language,
        boolean wasModified
    ) {}

    private String truncate(String text) {
        if (text == null) return "";
        return text.length() > 50 ? text.substring(0, 50) + "..." : text;
    }
}
