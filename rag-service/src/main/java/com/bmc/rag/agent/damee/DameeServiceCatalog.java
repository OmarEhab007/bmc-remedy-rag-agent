package com.bmc.rag.agent.damee;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service catalog for Damee services.
 * Loads and indexes all CST ITSM Damee services from the knowledge base.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DameeServiceCatalog {

    private static final String DAMEE_KB_PATH = "damee/Damee.md";

    @Getter
    private final Map<String, DameeService> servicesById = new LinkedHashMap<>();

    @Getter
    private final List<DameeService> allServices = new ArrayList<>();

    private final Map<String, List<DameeService>> servicesByCategory = new LinkedHashMap<>();

    // Patterns for parsing the markdown
    private static final Pattern SERVICE_HEADER_PATTERN = Pattern.compile("^####\\s+\\d+\\.\\s+(.+?)\\s+\\((.+?)\\)$");
    private static final Pattern SERVICE_ID_PATTERN = Pattern.compile("\\*\\*Service ID:\\*\\*\\s*(.+)");
    private static final Pattern URL_PATTERN = Pattern.compile("\\*\\*URL:\\*\\*\\s*(.+)");
    private static final Pattern DESC_EN_PATTERN = Pattern.compile("\\*\\*Description EN:\\*\\*\\s*(.+)");
    private static final Pattern DESC_AR_PATTERN = Pattern.compile("\\*\\*Description AR:\\*\\*\\s*(.+)");

    @PostConstruct
    public void loadFromMarkdown() {
        log.info("Loading Damee service catalog from {}", DAMEE_KB_PATH);

        try {
            String content = loadMarkdownContent();
            parseServices(content);

            log.info("Loaded {} Damee services across {} categories",
                    allServices.size(), servicesByCategory.size());

            // Log category breakdown
            servicesByCategory.forEach((category, services) ->
                    log.debug("Category '{}': {} services", category, services.size()));

        } catch (IOException e) {
            log.error("Failed to load Damee service catalog: {}", e.getMessage(), e);
            loadHardcodedServices();
        }
    }

    private String loadMarkdownContent() throws IOException {
        ClassPathResource resource = new ClassPathResource(DAMEE_KB_PATH);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private void parseServices(String content) {
        String[] lines = content.split("\n");
        String currentCategory = null;
        String currentSubcategory = null;
        DameeService.DameeServiceBuilder currentBuilder = null;
        List<String> workflowLines = new ArrayList<>();
        boolean inWorkflow = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // Detect category headers (## Category Name)
            if (line.startsWith("## ") && !line.startsWith("## Overview") && !line.startsWith("## User Manual") &&
                    !line.startsWith("## Workflow") && !line.startsWith("## Keywords") && !line.startsWith("## Service Categories")) {
                currentCategory = extractCategoryName(line);
                currentSubcategory = null;
                continue;
            }

            // Detect subcategory headers (### Subcategory Name)
            if (line.startsWith("### ")) {
                currentSubcategory = extractSubcategoryName(line);
                continue;
            }

            // Detect service headers (#### N. Service Name (Arabic Name))
            Matcher headerMatcher = SERVICE_HEADER_PATTERN.matcher(line);
            if (headerMatcher.matches()) {
                // Save previous service if exists
                if (currentBuilder != null) {
                    saveService(currentBuilder, workflowLines);
                }

                // Start new service
                String nameEn = headerMatcher.group(1);
                String nameAr = headerMatcher.group(2);

                currentBuilder = DameeService.builder()
                        .nameEn(nameEn)
                        .nameAr(nameAr)
                        .category(currentCategory)
                        .subcategory(currentSubcategory)
                        .keywords(generateKeywords(nameEn, nameAr))
                        .requiresManagerApproval(true)
                        .vipBypass(false);

                workflowLines = new ArrayList<>();
                inWorkflow = false;
                continue;
            }

            if (currentBuilder == null) continue;

            // Parse service attributes
            Matcher idMatcher = SERVICE_ID_PATTERN.matcher(line);
            if (idMatcher.find()) {
                String serviceId = idMatcher.group(1).trim();
                if (!serviceId.equals("Not specified") && !serviceId.isEmpty()) {
                    currentBuilder.serviceId(serviceId);
                }
                continue;
            }

            Matcher urlMatcher = URL_PATTERN.matcher(line);
            if (urlMatcher.find()) {
                currentBuilder.url(urlMatcher.group(1).trim());
                continue;
            }

            Matcher descEnMatcher = DESC_EN_PATTERN.matcher(line);
            if (descEnMatcher.find()) {
                currentBuilder.descriptionEn(descEnMatcher.group(1).trim());
                continue;
            }

            Matcher descArMatcher = DESC_AR_PATTERN.matcher(line);
            if (descArMatcher.find()) {
                currentBuilder.descriptionAr(descArMatcher.group(1).trim());
                continue;
            }

            // Detect workflow start
            if (line.startsWith("- **Workflow:**") || line.equals("**Workflow:**")) {
                inWorkflow = true;
                continue;
            }

            // Collect workflow lines
            if (inWorkflow && line.startsWith("-")) {
                workflowLines.add(line.substring(1).trim());

                // Check for VIP bypass
                if (line.contains("VIP bypasses") || line.contains("VIP Bypass")) {
                    currentBuilder.vipBypass(true);
                }
            }

            // Detect service options
            if (line.startsWith("- **Option:") || line.startsWith("**Option:")) {
                // This is a multi-option service - parse options later
                continue;
            }
        }

        // Save last service
        if (currentBuilder != null) {
            saveService(currentBuilder, workflowLines);
        }
    }

    private void saveService(DameeService.DameeServiceBuilder builder, List<String> workflowLines) {
        // Parse workflow steps
        List<DameeService.WorkflowStep> workflow = parseWorkflow(workflowLines);
        builder.workflow(workflow);

        // Generate required fields based on service type
        builder.requiredFields(generateRequiredFields(builder.build()));

        DameeService service = builder.build();

        // Only add services with valid IDs
        if (service.getServiceId() != null && !service.getServiceId().isEmpty()) {
            servicesById.put(service.getServiceId(), service);
            allServices.add(service);

            // Index by category
            String category = service.getCategory() != null ? service.getCategory() : "Other";
            servicesByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(service);
        }
    }

    private List<DameeService.WorkflowStep> parseWorkflow(List<String> workflowLines) {
        List<DameeService.WorkflowStep> steps = new ArrayList<>();

        for (int i = 0; i < workflowLines.size(); i++) {
            String line = workflowLines.get(i);

            // Parse workflow steps separated by →
            String[] parts = line.split("→");
            for (int j = 0; j < parts.length; j++) {
                String step = parts[j].trim();
                if (step.isEmpty() || step.equals("End")) continue;

                DameeService.WorkflowStep.WorkflowStepBuilder stepBuilder = DameeService.WorkflowStep.builder()
                        .order(steps.size() + 1)
                        .description(step)
                        .requiresApproval(step.toLowerCase().contains("approval"));

                // Extract team from step description
                if (step.contains("Service Desk")) {
                    stepBuilder.team("Service Desk");
                } else if (step.contains("Network")) {
                    stepBuilder.team("Network");
                } else if (step.contains("GRC")) {
                    stepBuilder.team("GRC");
                } else if (step.contains("Manager")) {
                    stepBuilder.team("Manager");
                } else if (step.contains("Database")) {
                    stepBuilder.team("Database Ops");
                } else if (step.contains("Infrastructure")) {
                    stepBuilder.team("Infrastructure Operation");
                } else if (step.contains("IT Security") || step.contains("SOC")) {
                    stepBuilder.team("IT Security");
                } else if (step.contains("Application")) {
                    stepBuilder.team("Application Operation");
                } else if (step.contains("Geospatial")) {
                    stepBuilder.team("Geospatial Data Center");
                }

                steps.add(stepBuilder.build());
            }
        }

        return steps;
    }

    private String extractCategoryName(String line) {
        // Extract "IT Services" from "## IT Services (خدمات تقنية المعلومات)"
        String name = line.substring(3).trim();
        if (name.contains("(")) {
            name = name.substring(0, name.indexOf("(")).trim();
        }
        return name;
    }

    private String extractSubcategoryName(String line) {
        String name = line.substring(4).trim();
        if (name.contains("(")) {
            name = name.substring(0, name.indexOf("(")).trim();
        }
        return name;
    }

    private List<String> generateKeywords(String nameEn, String nameAr) {
        List<String> keywords = new ArrayList<>();

        // Add words from English name
        if (nameEn != null) {
            String[] words = nameEn.toLowerCase().split("\\s+");
            for (String word : words) {
                if (word.length() > 2 && !isStopWord(word)) {
                    keywords.add(word);
                }
            }
        }

        // Add Arabic name
        if (nameAr != null) {
            keywords.add(nameAr);
        }

        return keywords;
    }

    private boolean isStopWord(String word) {
        return Set.of("the", "a", "an", "for", "to", "and", "or", "of", "in", "on", "with").contains(word);
    }

    private List<String> generateRequiredFields(DameeService service) {
        List<String> fields = new ArrayList<>();

        // Common required fields
        fields.add("description");

        // Service-specific fields based on category
        String category = service.getCategory();
        if (category != null) {
            if (category.contains("IT Services")) {
                fields.add("justification");
            } else if (category.contains("Support Services")) {
                fields.add("requestDate");
            } else if (category.contains("Legal")) {
                fields.add("caseDetails");
            } else if (category.contains("Geospatial")) {
                fields.add("dataRequirements");
            }
        }

        return fields;
    }

    /**
     * Fallback method to load hardcoded services if markdown parsing fails.
     */
    private void loadHardcodedServices() {
        log.warn("Loading hardcoded Damee services as fallback");

        // IT Services
        addService("10504", "Applications Permission Management", "إدارة صلاحيات التطبيقات",
                "Service to manage users' CST application permissions (grant, modify, stop)",
                "خدمة لإدارة حسابات المستفيدين المتعلقة بتطبيقات الهيئة (إنشاء، تعديل، إيقاف)",
                "IT Services", "Accounts Services", "https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10504",
                List.of("permission", "access", "application", "grant", "revoke", "صلاحية", "تطبيق"));

        addService("10503", "Database Access Management", "إدارة حسابات قاعدة البيانات",
                "Database Access Management",
                "خدمة لإدارة حسابات قواعد البيانات (إنشاء، تعديل، إيقاف)",
                "IT Services", "Accounts Services", "https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10503",
                List.of("database", "db", "access", "sql", "oracle", "قاعدة بيانات"));

        addService("10242", "Personal Email Management", "إدارة البريد الإلكتروني الشخصي",
                "This feature offers options to increase email storage or perform archiving",
                "خدمة تقدم لإدارة البريد الالكتروني من حيث زيادة مساحة البريد الالكتروني أو عمل أرشفة",
                "IT Services", "Accounts Services", "https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10242",
                List.of("email", "mailbox", "storage", "archive", "outlook", "بريد"));

        addService("10209", "Server Access Management", "إدارة حساب الخادم",
                "Through this form, you can request for permission on one server",
                "خدمة لإدارة حسابات الخوادم (إنشاء، تعديل، إيقاف)",
                "IT Services", "Accounts Services", "https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10209",
                List.of("server", "access", "create server", "new server", "خادم"));

        addService("10247", "Software Installation", "تثبيت برنامج",
                "This feature provides the option to install approved or unapproved software",
                "خدمة تتاح لتثبيت البرامج المعتمدة أو الغير معتمدة",
                "IT Services", "Technical Services", "https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10247",
                List.of("software", "install", "program", "application", "برنامج", "تثبيت"));

        addService("10513", "Virtual Private Network Request", "طلب الشبكة الخاصة الافتراضية",
                "Virtual Private Network Request for remote access",
                "طلب الشبكة الخاصة الافتراضية",
                "IT Services", "Technical Services", "https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10513",
                List.of("vpn", "remote access", "work from home", "remote", "شبكة افتراضية"));

        addService("10101", "Technical Incident", "بلاغ عن مشكلة تقنية",
                "This service is for submitting requests to resolve technical issues",
                "خدمة ترفع لحل مشكلة تقنية للأحداث التي تعطل التشغيل الطبيعي لنظام أو خدمة أو تطبيق",
                "IT Services", "Report Issue", "https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10101",
                List.of("incident", "problem", "issue", "technical", "مشكلة", "عطل"));

        addService("10254", "IP Telephony Management", "إدارة الهاتف الشبكي",
                "You can submit a request for an extension to your network phone",
                "خدمة لإدارة الهاتف الشبكي الأساسي والافتراضي",
                "IT Services", "Storage and Communication", "https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10254",
                List.of("phone", "telephony", "extension", "هاتف"));

        addService("10257", "Meeting Applications Management", "إدارة تطبيقات الإجتماعات الإفتراضية",
                "This feature relates to managing virtual meetings (Webex, Teams, Zoom)",
                "خدمة تخص إدراة برامج الاجتماعات الافتراضية",
                "IT Services", "Storage and Communication", "https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10257",
                List.of("meeting", "webex", "teams", "zoom", "اجتماع"));

        // Support Services
        addService("10113", "Request To Use An Existing Car", "طلب استخدام سيارة",
                "Using a temporary or permanent car",
                "استخدام سيارة مؤقته او دائمة تكون عهدة على الموظف",
                "Support Services", "Document Management", "https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10113",
                List.of("car", "vehicle", "سيارة"));

        addService("10114", "Request A New Car", "طلب تأمين سيارة جديدة",
                "Insuring a new car through rent or purchase",
                "تأمين سيارة عن طريق الايجار او الشراء",
                "Support Services", "Document Management", "https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10114",
                List.of("new car", "purchase car", "rent car", "سيارة جديدة"));

        addService("10116", "SMSA Shipping", "طلب بوليصة شحن سمسا",
                "For work shipments for the Authority and personal shipments via express shipping",
                "لإرساليات العمل الخاصة بالهيئة والارساليات الشخصية عن طريق الشحن السريع",
                "Support Services", "Document Management", "https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10116",
                List.of("shipping", "smsa", "delivery", "شحن"));

        addService("10120", "Identity Card Request", "إصدار بطاقة",
                "Identity Card Request",
                "إصدار بطاقة",
                "Support Services", "Security Services", "https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10120",
                List.of("id card", "identity", "badge", "بطاقة"));

        // Legal Services
        addService("10106", "Contract Draft", "مسودة عقد",
                "Contract Draft",
                "مسودة عقد",
                "Legal Consultation Services", null, "https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10106",
                List.of("contract", "draft", "legal", "عقد"));

        addService("10109", "Letter Draft", "مسودة خطاب",
                "Letter Draft",
                "مسودة خطاب",
                "Legal Consultation Services", null, "https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10109",
                List.of("letter", "draft", "خطاب"));

        // Inspection Services
        addService("10401", "Inspection Request Service", "خدمة طلبات التفتيش",
                "Inspection Request service",
                "خدمة طلبات التفتيش",
                "Inspection Services", null, "https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10401",
                List.of("inspection", "audit", "تفتيش"));

        // Geospatial Services
        addService("10601", "Operational Dashboard Request", "طلب انشاء لوحة معلومات تشغيلية",
                "Request a dashboard to display key performance indicators",
                "طلب إنشاء لوحة لعرض مؤشرات الأداء والبيانات التشغيلية المهمة",
                "Geospatial Services", null, "https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10601",
                List.of("dashboard", "gis", "map", "لوحة معلومات"));

        addService("10605", "Account And Permission On GIS", "الحساب والصلاحيات على نظام المعلومات الجغرافية",
                "Request access permissions for geospatial systems",
                "طلب صلاحيات الوصول للأنظمة الجيومكانية",
                "Geospatial Services", null, "https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10605",
                List.of("gis", "geospatial", "map", "نظام معلومات جغرافية"));

        log.info("Loaded {} hardcoded services", allServices.size());
    }

    private void addService(String id, String nameEn, String nameAr, String descEn, String descAr,
                            String category, String subcategory, String url, List<String> keywords) {
        DameeService service = DameeService.builder()
                .serviceId(id)
                .nameEn(nameEn)
                .nameAr(nameAr)
                .descriptionEn(descEn)
                .descriptionAr(descAr)
                .category(category)
                .subcategory(subcategory)
                .url(url)
                .keywords(new ArrayList<>(keywords))
                .workflow(generateDefaultWorkflow())
                .requiredFields(List.of("description", "justification"))
                .requiresManagerApproval(true)
                .vipBypass(false)
                .build();

        servicesById.put(id, service);
        allServices.add(service);
        servicesByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(service);
    }

    private List<DameeService.WorkflowStep> generateDefaultWorkflow() {
        return List.of(
                DameeService.WorkflowStep.builder().order(1).description("Fill Form").team("Requester").build(),
                DameeService.WorkflowStep.builder().order(2).description("Manager Approval").team("Manager").requiresApproval(true).build(),
                DameeService.WorkflowStep.builder().order(3).description("Service Desk Processing").team("Service Desk").build()
        );
    }

    // ========================
    // Query Methods
    // ========================

    /**
     * Get a service by its ID.
     */
    public DameeService getById(String serviceId) {
        return servicesById.get(serviceId);
    }

    /**
     * Get all services in a category.
     */
    public List<DameeService> getByCategory(String category) {
        return servicesByCategory.getOrDefault(category, Collections.emptyList());
    }

    /**
     * Get all available categories.
     */
    public Set<String> getCategories() {
        return servicesByCategory.keySet();
    }

    /**
     * Search services by keyword matching.
     */
    public List<DameeService> searchByKeyword(String query, int limit) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        String lowerQuery = query.toLowerCase();

        return allServices.stream()
                .map(service -> new ScoredService(service, calculateKeywordScore(service, lowerQuery)))
                .filter(scored -> scored.score > 0)
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(limit)
                .map(scored -> {
                    scored.service.setScore(scored.score);
                    return scored.service;
                })
                .toList();
    }

    private double calculateKeywordScore(DameeService service, String query) {
        double score = 0;

        // Check service name (highest weight)
        if (service.getNameEn() != null && service.getNameEn().toLowerCase().contains(query)) {
            score += 10;
        }
        if (service.getNameAr() != null && service.getNameAr().contains(query)) {
            score += 10;
        }

        // Check description
        if (service.getDescriptionEn() != null && service.getDescriptionEn().toLowerCase().contains(query)) {
            score += 5;
        }
        if (service.getDescriptionAr() != null && service.getDescriptionAr().contains(query)) {
            score += 5;
        }

        // Check keywords
        if (service.getKeywords() != null) {
            for (String keyword : service.getKeywords()) {
                if (keyword.toLowerCase().contains(query) || query.contains(keyword.toLowerCase())) {
                    score += 3;
                }
            }
        }

        // Check category
        if (service.getCategory() != null && service.getCategory().toLowerCase().contains(query)) {
            score += 2;
        }

        return score;
    }

    /**
     * Search services by category filter.
     */
    public List<DameeService> search(String query, String categoryFilter, int limit) {
        List<DameeService> candidates;

        if (categoryFilter != null && !categoryFilter.isBlank()) {
            candidates = getByCategory(categoryFilter);
        } else {
            candidates = allServices;
        }

        if (query == null || query.isBlank()) {
            return candidates.stream().limit(limit).toList();
        }

        String lowerQuery = query.toLowerCase();

        return candidates.stream()
                .map(service -> new ScoredService(service, calculateKeywordScore(service, lowerQuery)))
                .filter(scored -> scored.score > 0)
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(limit)
                .map(scored -> {
                    scored.service.setScore(scored.score);
                    return scored.service;
                })
                .toList();
    }

    /**
     * Get service count.
     */
    public int getServiceCount() {
        return allServices.size();
    }

    /**
     * Get category summary for display.
     */
    public String getCategorySummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Available service categories:\n\n");

        int index = 1;
        for (Map.Entry<String, List<DameeService>> entry : servicesByCategory.entrySet()) {
            sb.append(String.format("%d. **%s** (%d services)\n",
                    index++, entry.getKey(), entry.getValue().size()));
        }

        return sb.toString();
    }

    private record ScoredService(DameeService service, double score) {}
}
