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
 * Expands abbreviations, adds synonyms, and corrects common typos.
 */
@Slf4j
@Component
public class QueryRewriter {

    private final ChatLanguageModel chatModel;

    @Value("${query-rewrite.enabled:true}")
    private boolean enabled;

    @Value("${query-rewrite.use-llm:false}")
    private boolean useLlm;

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

        // Step 2: Expand abbreviations
        rewritten = expandAbbreviations(rewritten, modifications);

        // Step 3: Add synonyms for key terms
        rewritten = addSynonyms(rewritten, modifications);

        // Step 4: Optionally use LLM for more sophisticated rewriting
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
