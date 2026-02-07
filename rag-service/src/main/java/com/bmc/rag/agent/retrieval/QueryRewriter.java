package com.bmc.rag.agent.retrieval;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Query rewriter for improving retrieval quality (P2.3).
 * Expands abbreviations, adds synonyms, corrects common typos,
 * and provides bilingual Arabic/English support for CST ITSM queries.
 */
@Slf4j
@Component
public class QueryRewriter {

    private final ChatLanguageModel chatModel;

    @Value("${query-rewrite.enabled:true}")
    private boolean enabled;

    @Value("${query-rewrite.use-llm:false}")
    private boolean useLlm;

    @Value("${query-rewrite.arabic-expansion:true}")
    private boolean arabicExpansionEnabled;

    // Arabic IT terms mapped to English equivalents for bilingual search expansion
    // These help improve retrieval for Arabic queries by adding English search terms
    private static final Map<String, String> ARABIC_IT_TERMS = Map.ofEntries(
        // Core ITSM concepts
        Map.entry("تذكرة", "ticket incident request"),
        Map.entry("طلب", "request service ticket"),
        Map.entry("مشكلة", "problem issue incident"),
        Map.entry("حادثة", "incident ticket issue"),
        Map.entry("تغيير", "change request CR"),
        Map.entry("أمر عمل", "work order WO task"),
        Map.entry("قاعدة المعرفة", "knowledge base KB article"),
        Map.entry("خدمة", "service request catalog"),

        // Authentication & Access
        Map.entry("إعادة تعيين", "reset password unlock"),
        Map.entry("كلمة المرور", "password credential secret"),
        Map.entry("صلاحيات", "permissions access rights authorization"),
        Map.entry("دخول", "login sign-in access authentication"),
        Map.entry("تسجيل", "login register sign-in"),
        Map.entry("حساب", "account user profile"),

        // Network & Connectivity
        Map.entry("شبكة", "network VPN WiFi LAN connectivity"),
        Map.entry("اتصال", "connection connectivity network link"),
        Map.entry("انترنت", "internet web network connectivity"),
        Map.entry("واي فاي", "WiFi wireless network"),

        // Hardware & Devices
        Map.entry("جهاز", "device computer laptop workstation"),
        Map.entry("حاسب", "computer PC laptop desktop"),
        Map.entry("طابعة", "printer printing document"),
        Map.entry("شاشة", "screen monitor display"),
        Map.entry("لوحة مفاتيح", "keyboard input device"),
        Map.entry("فأرة", "mouse input device"),

        // Software & Applications
        Map.entry("برنامج", "software application program install"),
        Map.entry("تطبيق", "application app software"),
        Map.entry("تثبيت", "install setup deploy configure"),
        Map.entry("تحديث", "update upgrade patch"),
        Map.entry("إزالة", "uninstall remove delete"),

        // Email & Communication
        Map.entry("بريد", "email mail outlook message"),
        Map.entry("رسالة", "message email notification"),
        Map.entry("مرفق", "attachment file document"),

        // Status & Issues
        Map.entry("عطل", "failure error malfunction down"),
        Map.entry("خطأ", "error issue problem failure exception"),
        Map.entry("بطيء", "slow performance lag delay"),
        Map.entry("توقف", "crash freeze hang stopped"),
        Map.entry("لا يعمل", "not working down broken failure"),

        // Actions
        Map.entry("موافقة", "approval approve authorization"),
        Map.entry("رفض", "reject decline deny"),
        Map.entry("إلغاء", "cancel abort terminate"),
        Map.entry("استعادة", "restore recover backup"),
        Map.entry("نسخ احتياطي", "backup restore recovery"),

        // Support & Help
        Map.entry("دعم", "support help assistance"),
        Map.entry("مساعدة", "help support assistance"),
        Map.entry("استفسار", "inquiry question query"),

        // Organization specific (CST/CITC)
        Map.entry("هيئة", "commission CST organization"),
        Map.entry("إدارة", "department administration management"),
        Map.entry("موظف", "employee staff user"),
        Map.entry("مدير", "manager supervisor approver")
    );

    // Arabic transliterated terms (Arabizi) - common code-switching patterns
    private static final Map<String, String> ARABIZI_TERMS = Map.ofEntries(
        Map.entry("ريست", "reset restore"),
        Map.entry("باسوورد", "password credential"),
        Map.entry("لوقن", "login sign-in"),
        Map.entry("لوج ان", "login sign-in"),
        Map.entry("اكسس", "access permission"),
        Map.entry("ايميل", "email mail"),
        Map.entry("سيرفر", "server system"),
        Map.entry("نتورك", "network connectivity"),
        Map.entry("سوفت وير", "software application"),
        Map.entry("هارد وير", "hardware device"),
        Map.entry("اب ديت", "update upgrade"),
        Map.entry("داون لود", "download install"),
        Map.entry("اب لود", "upload send"),
        Map.entry("فايل", "file document"),
        Map.entry("فولدر", "folder directory"),
        Map.entry("لينك", "link URL"),
        Map.entry("كليك", "click select"),
        Map.entry("سكرين", "screen display"),
        Map.entry("برنت", "print printer")
    );

    // Common IT abbreviations and their expansions
    private static final Map<String, String> ABBREVIATIONS = Map.ofEntries(
        Map.entry("vpn", "VPN Virtual Private Network"),
        Map.entry("ad", "AD Active Directory"),
        Map.entry("dns", "DNS Domain Name System"),
        Map.entry("dhcp", "DHCP Dynamic Host Configuration Protocol"),
        Map.entry("smtp", "SMTP Simple Mail Transfer Protocol"),
        Map.entry("imap", "IMAP Internet Message Access Protocol"),
        Map.entry("pop3", "POP3 Post Office Protocol"),
        Map.entry("ssl", "SSL TLS Secure Sockets Layer Transport Layer Security"),
        Map.entry("https", "HTTPS HTTP Secure"),
        Map.entry("ftp", "FTP File Transfer Protocol"),
        Map.entry("ssh", "SSH Secure Shell"),
        Map.entry("rdp", "RDP Remote Desktop Protocol"),
        Map.entry("sql", "SQL database query"),
        Map.entry("api", "API Application Programming Interface"),
        Map.entry("sso", "SSO Single Sign-On"),
        Map.entry("mfa", "MFA Multi-Factor Authentication 2FA"),
        Map.entry("2fa", "2FA Two-Factor Authentication MFA"),
        Map.entry("ldap", "LDAP Lightweight Directory Access Protocol"),
        Map.entry("saml", "SAML Security Assertion Markup Language"),
        Map.entry("oauth", "OAuth authorization authentication"),
        Map.entry("pc", "PC computer workstation desktop"),
        Map.entry("vm", "VM virtual machine VMware Hyper-V"),
        Map.entry("os", "OS operating system Windows Linux"),
        Map.entry("cpu", "CPU processor performance slow"),
        Map.entry("ram", "RAM memory"),
        Map.entry("hdd", "HDD hard drive disk storage"),
        Map.entry("ssd", "SSD solid state drive storage"),
        Map.entry("lan", "LAN local area network"),
        Map.entry("wan", "WAN wide area network"),
        Map.entry("wifi", "WiFi wireless network connection"),
        Map.entry("ip", "IP address network"),
        Map.entry("mac", "MAC address network"),
        Map.entry("usb", "USB port device"),
        Map.entry("bsod", "BSOD Blue Screen of Death crash"),
        Map.entry("oom", "OOM Out of Memory"),
        Map.entry("kb", "KB knowledge base article"),
        Map.entry("inc", "INC incident ticket"),
        Map.entry("wo", "WO work order"),
        Map.entry("cr", "CR change request"),
        Map.entry("sla", "SLA Service Level Agreement"),
        Map.entry("itsm", "ITSM IT Service Management"),
        Map.entry("itil", "ITIL IT Infrastructure Library")
    );

    // Common synonyms for IT concepts
    private static final Map<String, List<String>> SYNONYMS = Map.ofEntries(
        Map.entry("slow", List.of("performance", "lag", "freeze", "unresponsive")),
        Map.entry("crash", List.of("freeze", "hang", "not responding", "blue screen")),
        Map.entry("login", List.of("sign in", "logon", "authenticate", "access")),
        Map.entry("password", List.of("credential", "passcode", "secret")),
        Map.entry("reset", List.of("restore", "reinitialize", "clear")),
        Map.entry("error", List.of("issue", "problem", "failure", "exception")),
        Map.entry("update", List.of("upgrade", "patch", "install")),
        Map.entry("connect", List.of("access", "link", "join", "network")),
        Map.entry("print", List.of("printer", "printing", "document")),
        Map.entry("email", List.of("mail", "outlook", "message")),
        Map.entry("install", List.of("setup", "deploy", "configure")),
        Map.entry("delete", List.of("remove", "uninstall", "clear"))
    );

    // Common typos and corrections
    private static final Map<String, String> TYPO_CORRECTIONS = Map.ofEntries(
        Map.entry("outlok", "outlook"),
        Map.entry("outllook", "outlook"),
        Map.entry("pasword", "password"),
        Map.entry("passowrd", "password"),
        Map.entry("erorr", "error"),
        Map.entry("eroor", "error"),
        Map.entry("conect", "connect"),
        Map.entry("conection", "connection"),
        Map.entry("conectivity", "connectivity"),
        Map.entry("acess", "access"),
        Map.entry("acces", "access"),
        Map.entry("instalation", "installation"),
        Map.entry("intall", "install"),
        Map.entry("netwrok", "network"),
        Map.entry("netowrk", "network"),
        Map.entry("pritn", "print"),
        Map.entry("printe", "printer"),
        Map.entry("screeen", "screen"),
        Map.entry("computre", "computer"),
        Map.entry("compter", "computer")
    );

    public QueryRewriter(ChatLanguageModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * Rewrite query to improve retrieval quality.
     *
     * @param query Original user query
     * @return Rewritten query with expanded terms
     */
    public RewriteResult rewrite(String query) {
        if (!enabled || query == null || query.isBlank()) {
            return new RewriteResult(query, query, false, List.of());
        }

        log.debug("Rewriting query: {}", truncate(query));

        List<String> modifications = new ArrayList<>();
        String rewritten = query;

        // Step 1: Correct typos
        rewritten = correctTypos(rewritten, modifications);

        // Step 2: Expand Arabic IT terms (bilingual support)
        if (arabicExpansionEnabled && containsArabic(query)) {
            rewritten = expandArabicTerms(rewritten, modifications);
            rewritten = expandArabiziTerms(rewritten, modifications);
            rewritten = normalizeArabicNumerals(rewritten, modifications);
        }

        // Step 3: Expand abbreviations
        rewritten = expandAbbreviations(rewritten, modifications);

        // Step 4: Add synonyms for key terms
        rewritten = addSynonyms(rewritten, modifications);

        // Step 5: Optionally use LLM for more sophisticated rewriting
        if (useLlm && !modifications.isEmpty()) {
            rewritten = llmRewrite(query, rewritten, modifications);
        }

        boolean wasModified = !rewritten.equals(query);
        log.debug("Rewritten query: {} (modified: {})", truncate(rewritten), wasModified);

        return new RewriteResult(query, rewritten, wasModified, modifications);
    }

    private String correctTypos(String query, List<String> modifications) {
        String result = query.toLowerCase();

        for (Map.Entry<String, String> entry : TYPO_CORRECTIONS.entrySet()) {
            if (result.contains(entry.getKey())) {
                result = result.replace(entry.getKey(), entry.getValue());
                modifications.add("Corrected typo: " + entry.getKey() + " → " + entry.getValue());
            }
        }

        return result;
    }

    private String expandAbbreviations(String query, List<String> modifications) {
        StringBuilder result = new StringBuilder(query);

        // Pattern to find word boundaries
        for (Map.Entry<String, String> entry : ABBREVIATIONS.entrySet()) {
            String abbr = entry.getKey();
            String expansion = entry.getValue();

            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(abbr) + "\\b", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(result);

            if (matcher.find()) {
                // Add expansion as additional context, don't replace
                result.append(" ").append(expansion);
                modifications.add("Expanded abbreviation: " + abbr);
            }
        }

        return result.toString();
    }

    private String addSynonyms(String query, List<String> modifications) {
        StringBuilder result = new StringBuilder(query);
        Set<String> addedSynonyms = new HashSet<>();

        for (Map.Entry<String, List<String>> entry : SYNONYMS.entrySet()) {
            String word = entry.getKey();
            List<String> synonyms = entry.getValue();

            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE);
            if (pattern.matcher(query).find()) {
                // Add first synonym as additional context
                String synonym = synonyms.get(0);
                if (!addedSynonyms.contains(synonym) && !query.toLowerCase().contains(synonym.toLowerCase())) {
                    result.append(" ").append(synonym);
                    addedSynonyms.add(synonym);
                    modifications.add("Added synonym for " + word + ": " + synonym);
                }
            }
        }

        return result.toString();
    }

    private String llmRewrite(String original, String expanded, List<String> modifications) {
        try {
            String prompt = String.format("""
                You are a search query optimizer for an IT support knowledge base.

                Original query: %s
                Expanded query: %s

                Rewrite the query to improve search results. Keep it concise but add relevant technical terms.
                Only output the rewritten query, nothing else.
                """, original, expanded);

            ChatRequest chatRequest = ChatRequest.builder()
                .messages(List.of(UserMessage.from(prompt)))
                .build();
            ChatResponse response = chatModel.chat(chatRequest);
            String llmResult = response.aiMessage().text();
            modifications.add("LLM enhanced query");
            return llmResult.trim();

        } catch (Exception e) {
            log.warn("LLM query rewrite failed: {}", e.getMessage());
            return expanded;
        }
    }

    private String truncate(String text) {
        if (text == null) return "";
        return text.length() > 100 ? text.substring(0, 100) + "..." : text;
    }

    /**
     * Check if query contains Arabic characters.
     */
    private boolean containsArabic(String text) {
        if (text == null || text.isEmpty()) return false;
        for (char c : text.toCharArray()) {
            if (c >= 0x0600 && c <= 0x06FF) {
                return true;
            }
        }
        return false;
    }

    /**
     * Expand Arabic IT terms by adding English equivalents.
     * This enables bilingual retrieval - Arabic queries can match English content.
     */
    private String expandArabicTerms(String query, List<String> modifications) {
        StringBuilder result = new StringBuilder(query);
        Set<String> addedTerms = new HashSet<>();

        for (Map.Entry<String, String> entry : ARABIC_IT_TERMS.entrySet()) {
            String arabicTerm = entry.getKey();
            String englishExpansion = entry.getValue();

            if (query.contains(arabicTerm) && !addedTerms.contains(englishExpansion)) {
                result.append(" ").append(englishExpansion);
                addedTerms.add(englishExpansion);
                modifications.add("Expanded Arabic term: " + arabicTerm + " → " + englishExpansion);
                log.debug("Arabic expansion: '{}' → '{}'", arabicTerm, englishExpansion);
            }
        }

        return result.toString();
    }

    /**
     * Expand Arabizi (transliterated) terms.
     * Handles common code-switching patterns like "ريست" → "reset".
     */
    private String expandArabiziTerms(String query, List<String> modifications) {
        StringBuilder result = new StringBuilder(query);
        Set<String> addedTerms = new HashSet<>();

        for (Map.Entry<String, String> entry : ARABIZI_TERMS.entrySet()) {
            String arabiziTerm = entry.getKey();
            String expansion = entry.getValue();

            if (query.contains(arabiziTerm) && !addedTerms.contains(expansion)) {
                result.append(" ").append(expansion);
                addedTerms.add(expansion);
                modifications.add("Expanded Arabizi: " + arabiziTerm + " → " + expansion);
                log.debug("Arabizi expansion: '{}' → '{}'", arabiziTerm, expansion);
            }
        }

        return result.toString();
    }

    /**
     * Normalize Arabic-Indic numerals (٠١٢٣٤٥٦٧٨٩) to Western Arabic numerals (0123456789).
     * Essential for ticket number parsing: "رقم التذكرة ١٢٣٤٥" → "رقم التذكرة 12345"
     */
    private String normalizeArabicNumerals(String query, List<String> modifications) {
        StringBuilder result = new StringBuilder();
        boolean hasArabicNumerals = false;

        for (char c : query.toCharArray()) {
            if (c >= 0x0660 && c <= 0x0669) {
                // Arabic-Indic digits (٠-٩)
                result.append((char) (c - 0x0660 + '0'));
                hasArabicNumerals = true;
            } else if (c >= 0x06F0 && c <= 0x06F9) {
                // Extended Arabic-Indic digits (Persian/Urdu)
                result.append((char) (c - 0x06F0 + '0'));
                hasArabicNumerals = true;
            } else {
                result.append(c);
            }
        }

        if (hasArabicNumerals) {
            modifications.add("Normalized Arabic numerals to Western");
            log.debug("Numeral normalization: '{}' → '{}'", query, result);
        }

        return result.toString();
    }

    /**
     * Query rewrite result.
     */
    public record RewriteResult(
        String originalQuery,
        String rewrittenQuery,
        boolean wasModified,
        List<String> modifications
    ) {
        public String getQueryForSearch() {
            return rewrittenQuery;
        }
    }
}
