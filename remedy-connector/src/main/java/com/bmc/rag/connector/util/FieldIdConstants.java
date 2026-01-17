package com.bmc.rag.connector.util;

/**
 * Immutable field ID mappings for BMC Remedy forms.
 * Field IDs are used instead of names for stability across localization and upgrades.
 */
public final class FieldIdConstants {

    private FieldIdConstants() {
        // Utility class - prevent instantiation
    }

    // ==========================================================================
    // COMMON FIELDS (Shared across multiple forms)
    // ==========================================================================
    public static final int REQUEST_ID = 1;           // Entry ID
    public static final int SUBMITTER = 2;            // Submitter
    public static final int CREATE_DATE = 3;          // Create Date (Unix epoch)
    public static final int ASSIGNED_TO = 4;          // Assigned To
    public static final int LAST_MODIFIED_BY = 5;     // Last Modified By
    public static final int LAST_MODIFIED_DATE = 6;   // Last Modified Date (Unix epoch)
    public static final int STATUS = 7;               // Status (Enum)
    public static final int SHORT_DESCRIPTION = 8;    // Short Description

    // ==========================================================================
    // HPD:Help Desk (Incidents)
    // ==========================================================================
    public static final class Incident {
        public static final String FORM_NAME = "HPD:Help Desk";

        public static final int INCIDENT_NUMBER = 1000000161;      // Incident Number (e.g., INC000000000001)
        public static final int SUMMARY = 1000000000;              // Summary
        public static final int DESCRIPTION = 1000000151;          // Notes/Description
        public static final int RESOLUTION = 1000000156;           // Resolution
        public static final int ASSIGNED_GROUP = 1000000217;       // Assigned Group
        public static final int ASSIGNED_SUPPORT_COMPANY = 1000000082; // Assigned Support Company
        public static final int ASSIGNED_SUPPORT_ORG = 1000000010;     // Assigned Support Organization
        public static final int URGENCY = 1000000162;              // Urgency (Enum)
        public static final int IMPACT = 1000000163;               // Impact (Enum)
        public static final int PRIORITY = 1000000164;             // Priority (Enum)
        public static final int SERVICE_TYPE = 1000000099;         // Service Type
        public static final int REPORTED_SOURCE = 1000000215;      // Reported Source
        public static final int CATEGORY_TIER_1 = 1000000063;      // Categorization Tier 1
        public static final int CATEGORY_TIER_2 = 1000000064;      // Categorization Tier 2
        public static final int CATEGORY_TIER_3 = 1000000065;      // Categorization Tier 3
        public static final int PRODUCT_TIER_1 = 200000003;        // Product Categorization Tier 1
        public static final int PRODUCT_TIER_2 = 200000004;        // Product Categorization Tier 2
        public static final int PRODUCT_TIER_3 = 200000005;        // Product Categorization Tier 3
        public static final int RESOLUTION_CATEGORY_TIER_1 = 1000000150; // Resolution Categorization Tier 1
        public static final int RESOLUTION_CATEGORY_TIER_2 = 1000000152; // Resolution Categorization Tier 2
        public static final int RESOLUTION_CATEGORY_TIER_3 = 1000000153; // Resolution Categorization Tier 3
        public static final int CUSTOMER_FIRST_NAME = 1000000018;  // First Name
        public static final int CUSTOMER_LAST_NAME = 1000000019;   // Last Name
        public static final int CUSTOMER_COMPANY = 1000000001;     // Company

        private Incident() {}
    }

    // ==========================================================================
    // HPD:WorkLog (Incident Work Logs)
    // ==========================================================================
    public static final class IncidentWorkLog {
        public static final String FORM_NAME = "HPD:WorkLog";

        public static final int WORK_LOG_ID = 1;                   // Work Log ID
        public static final int INCIDENT_NUMBER = 1000000161;      // Parent Incident Number
        public static final int WORK_LOG_TYPE = 1000000156;        // Work Log Type (Enum)
        public static final int DETAILED_DESCRIPTION = 1000000151; // Detailed Description
        public static final int SUBMITTER = 1000000217;            // Work Log Submitter
        public static final int SUBMIT_DATE = 1000000560;          // Submit Date
        public static final int VIEW_ACCESS = 1000000761;          // View Access

        private IncidentWorkLog() {}
    }

    // ==========================================================================
    // WOI:WorkOrder (Work Orders)
    // ==========================================================================
    public static final class WorkOrder {
        public static final String FORM_NAME = "WOI:WorkOrder";

        public static final int WORK_ORDER_ID = 1000000182;        // Work Order ID
        public static final int SUMMARY = 1000000000;              // Summary
        public static final int DESCRIPTION = 1000000151;          // Detailed Description
        public static final int ASSIGNED_GROUP = 1000000217;       // Assigned Group
        public static final int ASSIGNED_SUPPORT_COMPANY = 1000000082; // Support Company
        public static final int PRIORITY = 1000000164;             // Priority (Enum)
        public static final int LOCATION_COMPANY = 1000000001;     // Location Company
        public static final int REQUESTER_FIRST_NAME = 1000000018; // Requester First Name
        public static final int REQUESTER_LAST_NAME = 1000000019;  // Requester Last Name
        public static final int CATEGORY_TIER_1 = 1000000063;      // Categorization Tier 1
        public static final int CATEGORY_TIER_2 = 1000000064;      // Categorization Tier 2
        public static final int CATEGORY_TIER_3 = 1000000065;      // Categorization Tier 3
        public static final int SCHEDULED_START_DATE = 1000000411; // Scheduled Start Date
        public static final int SCHEDULED_END_DATE = 1000000412;   // Scheduled End Date

        private WorkOrder() {}
    }

    // ==========================================================================
    // WOI:WorkInfo (Work Order Work Logs)
    // ==========================================================================
    public static final class WorkOrderInfo {
        public static final String FORM_NAME = "WOI:WorkInfo";

        public static final int WORK_INFO_ID = 1;                  // Work Info ID
        public static final int WORK_ORDER_ID = 1000000182;        // Parent Work Order ID
        public static final int WORK_INFO_TYPE = 1000000156;       // Work Info Type (Enum)
        public static final int DETAILED_DESCRIPTION = 1000000151; // Detailed Description
        public static final int SUBMITTER = 1000000217;            // Submitter
        public static final int SUBMIT_DATE = 1000000560;          // Submit Date

        private WorkOrderInfo() {}
    }

    // ==========================================================================
    // RKM:KnowledgeArticleManager (Knowledge Articles)
    // ==========================================================================
    public static final class KnowledgeArticle {
        public static final String FORM_NAME = "RKM:KnowledgeArticleManager";

        public static final int ARTICLE_ID = 302300500;            // Article ID (Doc ID)
        public static final int ARTICLE_TITLE = 302300502;         // Article Title
        public static final int ARTICLE_CONTENT = 302311200;       // Article Content (HTML/Text)
        public static final int ARTICLE_SUMMARY = 302300507;       // Article Summary
        public static final int ARTICLE_KEYWORDS = 302300510;      // Keywords
        public static final int ARTICLE_TYPE = 302300503;          // Article Type
        public static final int CATEGORY_TIER_1 = 302300504;       // Category Tier 1
        public static final int CATEGORY_TIER_2 = 302300505;       // Category Tier 2
        public static final int CATEGORY_TIER_3 = 302300506;       // Category Tier 3
        public static final int ASSIGNED_GROUP = 1000000217;       // Assigned Group
        public static final int AUTHOR = 302300508;                // Author
        public static final int VERSION_NUMBER = 302300520;        // Version Number
        public static final int PUBLISHED_DATE = 302300530;        // Published Date
        public static final int EXPIRATION_DATE = 302300531;       // Expiration Date
        public static final int VIEW_COUNT = 302300540;            // View Count

        private KnowledgeArticle() {}
    }

    // ==========================================================================
    // CHG:ChangeManagement (Change Requests)
    // ==========================================================================
    public static final class ChangeRequest {
        public static final String FORM_NAME = "CHG:Infrastructure Change";

        public static final int CHANGE_ID = 1000000182;            // Change ID (Infrastructure Change ID)
        public static final int SUMMARY = 1000000000;              // Summary
        public static final int DESCRIPTION = 1000000151;          // Description
        public static final int CHANGE_REASON = 1000000153;        // Reason for Change
        public static final int IMPLEMENTATION_PLAN = 1000000885;  // Implementation Plan
        public static final int ROLLBACK_PLAN = 1000000886;        // Backout/Rollback Plan
        public static final int RISK_LEVEL = 1000000180;           // Risk Level (Enum)
        public static final int IMPACT = 1000000163;               // Impact (Enum)
        public static final int URGENCY = 1000000162;              // Urgency (Enum)
        public static final int CHANGE_TYPE = 1000000171;          // Change Type
        public static final int CHANGE_CLASS = 1000000012;         // Change Class
        public static final int ASSIGNED_GROUP = 1000000217;       // Assigned Group
        public static final int ASSIGNED_SUPPORT_COMPANY = 1000000082; // Support Company
        public static final int CATEGORY_TIER_1 = 1000000063;      // Category Tier 1
        public static final int CATEGORY_TIER_2 = 1000000064;      // Category Tier 2
        public static final int CATEGORY_TIER_3 = 1000000065;      // Category Tier 3
        public static final int SCHEDULED_START_DATE = 1000000411; // Scheduled Start Date
        public static final int SCHEDULED_END_DATE = 1000000412;   // Scheduled End Date
        public static final int ACTUAL_START_DATE = 1000000413;    // Actual Start Date
        public static final int ACTUAL_END_DATE = 1000000414;      // Actual End Date

        private ChangeRequest() {}
    }

    // ==========================================================================
    // CHG:WorkLog (Change Request Work Logs)
    // ==========================================================================
    public static final class ChangeWorkLog {
        public static final String FORM_NAME = "CHG:WorkLog";

        public static final int WORK_LOG_ID = 1;                   // Work Log ID
        public static final int CHANGE_ID = 1000000182;            // Parent Change ID
        public static final int WORK_LOG_TYPE = 1000000156;        // Work Log Type (Enum)
        public static final int DETAILED_DESCRIPTION = 1000000151; // Detailed Description
        public static final int SUBMITTER = 1000000217;            // Submitter
        public static final int SUBMIT_DATE = 1000000560;          // Submit Date

        private ChangeWorkLog() {}
    }

    // ==========================================================================
    // Attachment Fields (Common across forms)
    // ==========================================================================
    public static final class Attachment {
        public static final int ATTACHMENT_1 = 1000000600;         // Attachment 1 (in main forms)
        public static final int ATTACHMENT_2 = 1000000601;         // Attachment 2
        public static final int ATTACHMENT_3 = 1000000602;         // Attachment 3

        // Work Log attachments
        public static final int WORK_LOG_ATTACHMENT_1 = 1000000568; // Work Log Attachment 1
        public static final int WORK_LOG_ATTACHMENT_2 = 1000000569; // Work Log Attachment 2
        public static final int WORK_LOG_ATTACHMENT_3 = 1000000570; // Work Log Attachment 3

        private Attachment() {}
    }

    // ==========================================================================
    // Status Values (Common Enum Values)
    // ==========================================================================
    public static final class StatusValues {
        // Incident Status
        public static final int INCIDENT_NEW = 0;
        public static final int INCIDENT_ASSIGNED = 1;
        public static final int INCIDENT_IN_PROGRESS = 2;
        public static final int INCIDENT_PENDING = 3;
        public static final int INCIDENT_RESOLVED = 4;
        public static final int INCIDENT_CLOSED = 5;
        public static final int INCIDENT_CANCELLED = 6;

        // Work Order Status
        public static final int WO_NEW = 0;
        public static final int WO_ASSIGNED = 1;
        public static final int WO_PLANNING = 2;
        public static final int WO_WAITING_APPROVAL = 3;
        public static final int WO_IN_PROGRESS = 4;
        public static final int WO_COMPLETED = 5;
        public static final int WO_REJECTED = 6;
        public static final int WO_CANCELLED = 7;
        public static final int WO_CLOSED = 8;

        // Change Request Status
        public static final int CHG_DRAFT = 0;
        public static final int CHG_REQUEST_FOR_AUTHORIZATION = 1;
        public static final int CHG_REQUEST_FOR_CHANGE = 2;
        public static final int CHG_PLANNING_IN_PROGRESS = 3;
        public static final int CHG_SCHEDULED_FOR_REVIEW = 4;
        public static final int CHG_SCHEDULED_FOR_APPROVAL = 5;
        public static final int CHG_SCHEDULED = 6;
        public static final int CHG_IMPLEMENTATION_IN_PROGRESS = 7;
        public static final int CHG_PENDING = 8;
        public static final int CHG_REJECTED = 9;
        public static final int CHG_COMPLETED = 10;
        public static final int CHG_CLOSED = 11;
        public static final int CHG_CANCELLED = 12;

        // Knowledge Article Status
        public static final int KA_DRAFT = 0;
        public static final int KA_IN_REVIEW = 1;
        public static final int KA_PUBLISHED = 2;
        public static final int KA_RETIRED = 3;
        public static final int KA_EXPIRED = 4;

        private StatusValues() {}
    }
}
