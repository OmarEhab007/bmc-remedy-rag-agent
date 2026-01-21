package com.bmc.rag.agent.damee;

import com.bmc.rag.agent.damee.ServiceFieldDefinition.FieldType;
import com.bmc.rag.agent.damee.ServiceFieldDefinition.ValidationResult;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for collecting and validating fields for Damee service requests.
 * Manages field definitions and collection logic for different services.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FieldCollector {

    /**
     * Service-specific field definitions.
     * Maps service ID to list of required fields.
     */
    private static final Map<String, List<ServiceFieldDefinition>> SERVICE_FIELDS = new HashMap<>();

    static {
        // 10504 - Applications Permission Management
        SERVICE_FIELDS.put("10504", List.of(
                ServiceFieldDefinition.select("serviceOption",
                        "What type of permission do you need?",
                        "ما نوع الصلاحية المطلوبة؟",
                        true,
                        List.of("BI-Grant Permission", "Add/Revoke ERP Permission",
                                "Access to Damee System", "Add or Revoke Applications Permissions")),
                ServiceFieldDefinition.text("applicationName",
                        "Which application?",
                        "أي تطبيق؟",
                        true),
                ServiceFieldDefinition.text("justification",
                        "Why do you need this access?",
                        "سبب الطلب؟",
                        true)
        ));

        // 10513 - VPN Request
        SERVICE_FIELDS.put("10513", List.of(
                ServiceFieldDefinition.select("vpnType",
                        "What type of VPN access?",
                        "ما نوع الوصول؟",
                        true,
                        List.of("Add User to VPN Client", "Add Device On VPNPC")),
                ServiceFieldDefinition.select("deviceType",
                        "Device type?",
                        "نوع الجهاز؟",
                        false,
                        List.of("Laptop", "Desktop", "Mobile")),
                ServiceFieldDefinition.text("justification",
                        "Why do you need VPN access?",
                        "سبب طلب VPN؟",
                        true)
        ));

        // 10247 - Software Installation
        SERVICE_FIELDS.put("10247", List.of(
                ServiceFieldDefinition.text("softwareName",
                        "Which software do you need installed?",
                        "ما البرنامج المطلوب؟",
                        true),
                ServiceFieldDefinition.select("isCertified",
                        "Is this software on the approved list?",
                        "هل البرنامج معتمد؟",
                        true,
                        List.of("Yes - Approved Software", "No - Needs Security Review")),
                ServiceFieldDefinition.text("justification",
                        "Why do you need this software?",
                        "سبب الحاجة للبرنامج؟",
                        true)
        ));

        // 10209 - Server Access Management
        SERVICE_FIELDS.put("10209", List.of(
                ServiceFieldDefinition.text("serverName",
                        "Which server do you need access to?",
                        "أي خادم تحتاج الوصول إليه؟",
                        true),
                ServiceFieldDefinition.select("environment",
                        "Server environment?",
                        "بيئة الخادم؟",
                        true,
                        List.of("Development", "Testing", "Staging", "Production")),
                ServiceFieldDefinition.select("accessType",
                        "What type of access do you need?",
                        "نوع الوصول المطلوب؟",
                        true,
                        List.of("Read Only", "Read/Write", "Administrator")),
                ServiceFieldDefinition.text("justification",
                        "Why do you need this server access?",
                        "سبب طلب الوصول للخادم؟",
                        true)
        ));

        // 10503 - Database Access Management
        SERVICE_FIELDS.put("10503", List.of(
                ServiceFieldDefinition.text("databaseName",
                        "Which database do you need access to?",
                        "أي قاعدة بيانات تحتاج الوصول إليها؟",
                        true),
                ServiceFieldDefinition.select("accessLevel",
                        "What access level do you need?",
                        "مستوى الوصول المطلوب؟",
                        true,
                        List.of("Read Only", "Read/Write", "DBA")),
                ServiceFieldDefinition.text("justification",
                        "Why do you need database access?",
                        "سبب طلب الوصول لقاعدة البيانات؟",
                        true)
        ));

        // 10242 - Personal Email Management
        SERVICE_FIELDS.put("10242", List.of(
                ServiceFieldDefinition.select("requestType",
                        "What do you need for your email?",
                        "ماذا تحتاج لبريدك الإلكتروني؟",
                        true,
                        List.of("Increase Storage", "Archive Setup", "Other")),
                ServiceFieldDefinition.text("details",
                        "Please provide additional details",
                        "يرجى تقديم تفاصيل إضافية",
                        true)
        ));

        // 10101 - Technical Incident
        SERVICE_FIELDS.put("10101", List.of(
                ServiceFieldDefinition.text("summary",
                        "Briefly describe the issue",
                        "صف المشكلة باختصار",
                        true),
                ServiceFieldDefinition.text("description",
                        "Provide detailed description of the problem",
                        "قدم وصفاً مفصلاً للمشكلة",
                        true),
                ServiceFieldDefinition.select("urgency",
                        "How urgent is this issue?",
                        "ما مدى إلحاح هذه المشكلة؟",
                        true,
                        List.of("Low - Can wait", "Medium - Affects work", "High - Critical blocker"))
        ));

        // 10113 - Request To Use An Existing Car
        SERVICE_FIELDS.put("10113", List.of(
                ServiceFieldDefinition.select("usageType",
                        "What type of car usage?",
                        "نوع استخدام السيارة؟",
                        true,
                        List.of("Temporary", "Permanent")),
                ServiceFieldDefinition.text("purpose",
                        "Purpose of car usage",
                        "الغرض من استخدام السيارة",
                        true),
                ServiceFieldDefinition.text("duration",
                        "Expected duration (if temporary)",
                        "المدة المتوقعة (إذا كان مؤقتاً)",
                        false)
        ));

        // 10116 - SMSA Shipping
        SERVICE_FIELDS.put("10116", List.of(
                ServiceFieldDefinition.select("shipmentType",
                        "Type of shipment?",
                        "نوع الشحنة؟",
                        true,
                        List.of("Work Shipment", "Personal Shipment")),
                ServiceFieldDefinition.text("destination",
                        "Destination address",
                        "عنوان الوجهة",
                        true),
                ServiceFieldDefinition.text("contents",
                        "What are you shipping?",
                        "ما الذي تريد شحنه؟",
                        true)
        ));

        // 10601 - Operational Dashboard Request
        SERVICE_FIELDS.put("10601", List.of(
                ServiceFieldDefinition.text("dashboardName",
                        "What should the dashboard be named?",
                        "ما اسم لوحة المعلومات؟",
                        true),
                ServiceFieldDefinition.text("dataSource",
                        "What data should be displayed?",
                        "ما البيانات المطلوب عرضها؟",
                        true),
                ServiceFieldDefinition.text("kpis",
                        "What KPIs or metrics should be shown?",
                        "ما المؤشرات المطلوب إظهارها؟",
                        true)
        ));
    }

    /**
     * Default fields for services without specific definitions.
     */
    private static final List<ServiceFieldDefinition> DEFAULT_FIELDS = List.of(
            ServiceFieldDefinition.text("description",
                    "Please describe your request in detail",
                    "يرجى وصف طلبك بالتفصيل",
                    true),
            ServiceFieldDefinition.text("justification",
                    "What is the business justification?",
                    "ما هو المبرر التجاري؟",
                    true)
    );

    /**
     * Get field definitions for a service.
     */
    public List<ServiceFieldDefinition> getFieldsForService(String serviceId) {
        return SERVICE_FIELDS.getOrDefault(serviceId, DEFAULT_FIELDS);
    }

    /**
     * Get the next uncollected field.
     */
    public FieldCollectionResult collectNextField(String serviceId, Map<String, String> collectedFields) {
        List<ServiceFieldDefinition> fields = getFieldsForService(serviceId);

        for (ServiceFieldDefinition field : fields) {
            if (field.isRequired() && !collectedFields.containsKey(field.getFieldName())) {
                return FieldCollectionResult.needsField(field);
            }
        }

        // All required fields collected, check optional fields
        for (ServiceFieldDefinition field : fields) {
            if (!field.isRequired() && !collectedFields.containsKey(field.getFieldName())) {
                // Optional field - can skip
                return FieldCollectionResult.optionalField(field);
            }
        }

        return FieldCollectionResult.complete(collectedFields);
    }

    /**
     * Validate a field value.
     */
    public ValidationResult validateField(String serviceId, String fieldName, String value) {
        List<ServiceFieldDefinition> fields = getFieldsForService(serviceId);

        for (ServiceFieldDefinition field : fields) {
            if (field.getFieldName().equals(fieldName)) {
                return field.validate(value);
            }
        }

        // Field not found - accept any value
        return ValidationResult.valid();
    }

    /**
     * Get the prompt for a specific field.
     */
    public String getFieldPrompt(String serviceId, String fieldName, String language) {
        List<ServiceFieldDefinition> fields = getFieldsForService(serviceId);

        for (ServiceFieldDefinition field : fields) {
            if (field.getFieldName().equals(fieldName)) {
                return field.getFormattedPrompt(language);
            }
        }

        return "Please provide: " + fieldName;
    }

    /**
     * Get total number of fields for a service.
     */
    public int getTotalFieldCount(String serviceId) {
        return getFieldsForService(serviceId).size();
    }

    /**
     * Get number of required fields for a service.
     */
    public int getRequiredFieldCount(String serviceId) {
        return (int) getFieldsForService(serviceId).stream()
                .filter(ServiceFieldDefinition::isRequired)
                .count();
    }

    /**
     * Result of field collection check.
     */
    @Data
    @Builder
    public static class FieldCollectionResult {
        private boolean complete;
        private boolean needsInput;
        private boolean optional;
        private ServiceFieldDefinition field;
        private Map<String, String> collectedFields;

        public static FieldCollectionResult needsField(ServiceFieldDefinition field) {
            return FieldCollectionResult.builder()
                    .complete(false)
                    .needsInput(true)
                    .optional(false)
                    .field(field)
                    .build();
        }

        public static FieldCollectionResult optionalField(ServiceFieldDefinition field) {
            return FieldCollectionResult.builder()
                    .complete(false)
                    .needsInput(true)
                    .optional(true)
                    .field(field)
                    .build();
        }

        public static FieldCollectionResult complete(Map<String, String> fields) {
            return FieldCollectionResult.builder()
                    .complete(true)
                    .needsInput(false)
                    .collectedFields(fields)
                    .build();
        }
    }
}
