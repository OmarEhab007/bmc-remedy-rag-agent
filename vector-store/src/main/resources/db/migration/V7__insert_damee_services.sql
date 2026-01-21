-- =============================================================================
-- CST ITSM Damee Services Knowledge Base
-- V7 Migration - Insert all Damee platform services for RAG retrieval
-- Source: Damee.md (January 2026)
-- =============================================================================

-- =============================================================================
-- Update sync_state constraint to include DameeService
-- =============================================================================
ALTER TABLE sync_state DROP CONSTRAINT IF EXISTS valid_sync_source;
ALTER TABLE sync_state ADD CONSTRAINT valid_sync_source CHECK (
    source_type IN ('Incident', 'WorkOrder', 'KnowledgeArticle', 'ChangeRequest', 'DameeService')
);

-- =============================================================================
-- PLATFORM OVERVIEW
-- =============================================================================
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('DAMEE-OVERVIEW-0', generate_random_embedding(),
'CST ITSM Damee Digital Work Platform Overview (نظرة عامة على منصة دعمي للعمل الرقمي)

Platform URL: https://itsmweb.mewa.gov.sa/jahz/index.html

Service Categories (الفئات الرئيسية):
1. IT Services (خدمات تقنية المعلومات) - Technical support, accounts, permissions, software, network
2. Support Services (الخدمات المساندة) - Cars, shipping, security badges, facilities
3. Legal Consultation Services (خدمات الاستشارات القانونية) - Contracts, legal opinions, agreements
4. Inspection Services (خدمات الإدارة العامة للتفتيش) - Inspection requests
5. Geospatial Services (الخدمات الجيومكانية) - GIS, dashboards, mapping, spatial analysis

The Damee platform organizes IT Service Management for CST (Communications, Space & Technology Commission) employees.',
'KnowledgeArticle', 'DAMEE-OVERVIEW', 'ENT-DAMEE-000', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Service Desk", "title": "Damee Platform Overview", "title_ar": "نظرة عامة على منصة دعمي", "category": "Platform/Overview", "status": "Published", "keywords": "Damee, platform, ITSM, services, categories, overview"}');

-- =============================================================================
-- USER MANUAL - PLATFORM OPERATIONS
-- =============================================================================
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('DAMEE-MANUAL-CREATE-0', generate_random_embedding(),
'How to Create a Service Request on Damee Platform (كيفية إنشاء طلب خدمة)

Steps to create a service request:
1. Access the Damee platform at https://itsmweb.mewa.gov.sa/jahz/index.html
2. Click on "Request Service" (طلب خدمة)
3. Select the desired service from the available list
4. Fill in the required data (fields marked with "*" are mandatory)
5. Attach any necessary files (optional)
6. Click "Submit Request" (إرسال طلب)
7. A confirmation message will appear upon successful submission

Note: Mandatory fields are marked with an asterisk (*). Ensure all required information is provided before submission.',
'KnowledgeArticle', 'DAMEE-MANUAL-CREATE', 'ENT-DAMEE-M01', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Service Desk", "title": "How to Create a Service Request", "title_ar": "كيفية إنشاء طلب خدمة", "category": "Platform/UserManual", "status": "Published", "keywords": "create request, submit, new request, طلب خدمة"}'),

('DAMEE-MANUAL-TRACK-0', generate_random_embedding(),
'How to Track and Search Service Requests on Damee (كيفية متابعة الطلبات)

Searching for a Service Request:
1. Click on "Track My Requests" (متابعة طلباتي)
2. Enter the service request number in the search bar
3. Click to view details to see the progress bar and request status

Request Statuses:
- Submitted (تم التقديم): Request has been submitted
- Pending Approval (في انتظار الموافقة): Waiting for approver action
- Approved (تمت الموافقة): Request has been approved
- Rejected (مرفوض): Request has been rejected
- In Progress (قيد التنفيذ): Request is being processed
- Completed (مكتمل): Request has been fulfilled
- Closed (مغلق): Request has been closed',
'KnowledgeArticle', 'DAMEE-MANUAL-TRACK', 'ENT-DAMEE-M02', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Service Desk", "title": "How to Track Service Requests", "title_ar": "كيفية متابعة الطلبات", "category": "Platform/UserManual", "status": "Published", "keywords": "track, search, status, متابعة, بحث"}'),

('DAMEE-MANUAL-COMMENT-0', generate_random_embedding(),
'How to Add Comments to Service Requests on Damee (كيفية إضافة تعليقات)

Adding Comments to a Request:
1. Search for the service request using the request number
2. View request details by clicking on the request
3. Click "Add Comment" (إضافة تعليق) at the bottom of the page
4. Enter your comment in the text field
5. Attach any files if needed
6. Click "Send" (إرسال)

Comments are useful for providing additional information, asking questions, or updating the status of your request.',
'KnowledgeArticle', 'DAMEE-MANUAL-COMMENT', 'ENT-DAMEE-M03', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Service Desk", "title": "How to Add Comments", "title_ar": "كيفية إضافة تعليقات", "category": "Platform/UserManual", "status": "Published", "keywords": "comment, add, تعليق, إضافة"}'),

('DAMEE-MANUAL-APPROVE-0', generate_random_embedding(),
'How to Approve Requests on Damee Platform (كيفية الموافقة على الطلبات)

Approving Requests:
1. Click on "My Activity" (نشاطي) menu
2. View pending approval requests in the list
3. Click on a request to review details
4. Approve or reject based on work requirements
5. Add comments if needed before approving/rejecting

Alternative method:
- Access approval requests through your email notifications
- Click the approval link in the notification email
- Review and approve directly from the notification',
'KnowledgeArticle', 'DAMEE-MANUAL-APPROVE', 'ENT-DAMEE-M04', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Service Desk", "title": "How to Approve Requests", "title_ar": "كيفية الموافقة على الطلبات", "category": "Platform/UserManual", "status": "Published", "keywords": "approve, reject, approval, موافقة, رفض"}'),

('DAMEE-MANUAL-REOPEN-0', generate_random_embedding(),
'How to Reopen a Service Request on Damee (كيفية إعادة تقديم طلب)

Reopening a Service Request:
1. Search for the completed service request
2. Select the request from the search results
3. Click "Resubmit Request" (إعادة تقديم الطلب)
4. Fields will be auto-populated with previous data
5. Make any necessary modifications to the form
6. Submit the request

Note: This creates a new request based on the previous one. The original request remains closed.',
'KnowledgeArticle', 'DAMEE-MANUAL-REOPEN', 'ENT-DAMEE-M05', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Service Desk", "title": "How to Reopen a Request", "title_ar": "كيفية إعادة تقديم طلب", "category": "Platform/UserManual", "status": "Published", "keywords": "reopen, resubmit, إعادة تقديم"}'),

('DAMEE-MANUAL-DELEGATE-0', generate_random_embedding(),
'How to Manage Approval Delegates on Damee (كيفية إدارة المفوضين للموافقات)

Managing Approval Delegates:
1. From the main page, click the preferences icon (التفضيلات)
2. Navigate to "Approval Settings" (إعداد الموافقة)
3. Click "Manage Approvers" (إدارة الموافقين)
4. Click "Add Approver" (إضافة موافق)
5. Search for the substitute person by name
6. Set the time period for delegation (start and end dates)
7. Click "Save" (حفظ)

To modify existing delegates:
- Select the three dots on the left side of the delegate entry
- Choose: Edit Approver, Edit Date/Time, or Cancel Substitute Approver

This is useful when you are on vacation or unavailable to approve requests.',
'KnowledgeArticle', 'DAMEE-MANUAL-DELEGATE', 'ENT-DAMEE-M06', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Service Desk", "title": "How to Manage Approval Delegates", "title_ar": "كيفية إدارة المفوضين للموافقات", "category": "Platform/UserManual", "status": "Published", "keywords": "delegate, substitute, approval, تفويض, بديل"}'),

('DAMEE-MANUAL-NOTIFY-0', generate_random_embedding(),
'How to Change Notification Settings on Damee (كيفية تغيير إعدادات الإشعارات)

Changing Notification Settings:
1. From the main page, click the preferences icon (التفضيلات)
2. Navigate to notification settings at the bottom of the page
3. Manage notifications for:
   - Approval requests (طلبات الموافقة)
   - Service requests updates (تحديثات طلبات الخدمة)
   - Social activities (النشاطات الاجتماعية)
4. Toggle notifications on/off as needed
5. Save your preferences

You can choose to receive notifications via email, in-app notifications, or both.',
'KnowledgeArticle', 'DAMEE-MANUAL-NOTIFY', 'ENT-DAMEE-M07', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Service Desk", "title": "How to Change Notification Settings", "title_ar": "كيفية تغيير إعدادات الإشعارات", "category": "Platform/UserManual", "status": "Published", "keywords": "notification, settings, إشعارات, إعدادات"}');

-- =============================================================================
-- IT SERVICES - ACCOUNTS AND PERMISSIONS
-- =============================================================================

-- SVC-10504: Applications Permission Management
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10504-0', generate_random_embedding(),
'Service: Applications Permission Management (إدارة صلاحيات التطبيقات)

Service ID: 10504
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10504
Category: IT Services > Accounts Services and Technical Privilege

Description (EN): Service to manage users CST application permissions (grant, modify, stop)
Description (AR): خدمة لإدارة حسابات المستفيدين المتعلقة بتطبيقات الهيئة (إنشاء، تعديل، إيقاف)

Workflow Options:

Option 1 - Business Intelligence (BI-Grant Permission):
1. Fill Form
2. Dashboard Section Approval
3. End

Option 2 - Add/Revoke ERP Permission:
1. Fill Form
2. Manager Approval (VIP users bypass this step)
3. ERP Team fulfillment
4. End

Option 3 - Access to Damee System:
1. Fill Form
2. Manager Approval
3. App Owner Approval
4. Application Operation team
5. End

Option 4 - Add or Revoke Applications Permissions:
1. Fill Form
2. Manager Approval (VIP users bypass this step)
3. App Owner Approval
4. Application Operation team
5. End',
'KnowledgeArticle', 'SVC-10504', 'ENT-SVC-10504', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Application Operation", "title": "Applications Permission Management", "title_ar": "إدارة صلاحيات التطبيقات", "category": "IT Services/Accounts/Permissions", "status": "Published", "service_id": "10504", "keywords": "application permission, access, ERP, BI, Damee, صلاحيات, تطبيقات"}');

-- SVC-10503: Database Access Management
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10503-0', generate_random_embedding(),
'Service: Database Access Management (إدارة حسابات قاعدة البيانات)

Service ID: 10503
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10503
Category: IT Services > Accounts Services and Technical Privilege

Description (EN): Database Access Management - manage database accounts (create, modify, stop)
Description (AR): خدمة لإدارة حسابات قواعد البيانات (إنشاء، تعديل، إيقاف)

Workflow:
1. Fill Form - Specify database, access type, and justification
2. Manager Approval - Direct supervisor approves the request
3. GRC Approval - Governance, Risk & Compliance team reviews
4. Database Ops - Database team implements the access
5. End

Use this service to request:
- New database account creation
- Modify existing database permissions
- Revoke/stop database access',
'KnowledgeArticle', 'SVC-10503', 'ENT-SVC-10503', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Database Administration", "title": "Database Access Management", "title_ar": "إدارة حسابات قاعدة البيانات", "category": "IT Services/Accounts/Database", "status": "Published", "service_id": "10503", "keywords": "database, access, account, قاعدة بيانات, حساب"}');

-- SVC-10501: IRP Service
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10501-0', generate_random_embedding(),
'Service: IRP Service (خدمات IRP)

Service ID: 10501
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10501
Category: IT Services > Accounts Services and Technical Privilege

Description (EN): IRP Service - Identity and Role Provisioning requests
Description (AR): خدمة IRP

Workflow:
1. Fill Form - Submit IRP request details
2. GRC Approval - Governance, Risk & Compliance reviews
3. IRP Approval - IRP team approves the request
4. IRP Team - Implementation by IRP team
5. End

This service handles identity and role provisioning within the organization.',
'KnowledgeArticle', 'SVC-10501', 'ENT-SVC-10501', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "IRP Team", "title": "IRP Service", "title_ar": "خدمات IRP", "category": "IT Services/Accounts/IRP", "status": "Published", "service_id": "10501", "keywords": "IRP, identity, role, provisioning"}');

-- SVC-10242: Personal Email Management
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10242-0', generate_random_embedding(),
'Service: Personal Email Management (إدارة البريد الإلكتروني الشخصي)

Service ID: 10242
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10242
Category: IT Services > Accounts Services and Technical Privilege

Description (EN): This feature offers options to increase email storage or perform archiving
Description (AR): خدمة تقدم لإدارة البريد الالكتروني من حيث زيادة مساحة البريد الالكتروني أو عمل أرشفة

Workflow:
1. Fill Form - Select storage increase or archiving option
2. Manager Approval (VIP users bypass this step)
3. Service Desk fulfillment
4. End

Use this service for:
- Increasing email mailbox storage capacity
- Email archiving requests
- Email management issues',
'KnowledgeArticle', 'SVC-10242', 'ENT-SVC-10242', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Service Desk", "title": "Personal Email Management", "title_ar": "إدارة البريد الإلكتروني الشخصي", "category": "IT Services/Accounts/Email", "status": "Published", "service_id": "10242", "keywords": "email, storage, archive, بريد, مساحة, أرشفة"}');

-- SVC-10209: Server Access Management
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10209-0', generate_random_embedding(),
'Service: Server Access Management (إدارة حساب الخادم)

Service ID: 10209
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10209
Category: IT Services > Accounts Services and Technical Privilege

Description (EN): Through this form, you can request for permission on one server
Description (AR): خدمة لإدارة حسابات الخوادم (إنشاء، تعديل، إيقاف)

Workflow:
1. Fill Form - Specify server and access requirements
2. Manager Approval (VIP users bypass this step)
3. GRC Approval - Security review
4. Infrastructure Operation - Implementation
5. End

IMPORTANT: Additional approval required if:
- Server Environment = "Production"
- Need Copy/Paste Functionality = "Yes"

These conditions trigger additional security review steps.',
'KnowledgeArticle', 'SVC-10209', 'ENT-SVC-10209', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Infrastructure Operation", "title": "Server Access Management", "title_ar": "إدارة حساب الخادم", "category": "IT Services/Accounts/Server", "status": "Published", "service_id": "10209", "keywords": "server, access, production, خادم, صلاحية"}');

-- SVC-10505: User Account Management (Contractor)
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10505-0', generate_random_embedding(),
'Service: User Account Management - Contracted Employees (خدمات الموظف المتعاقد)

Service ID: 10505
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10505
Category: IT Services > Accounts Services and Technical Privilege

Description (EN): This option assists contracted employees in updating their information, extending their account validity, or suspending all electronic services
Description (AR): يخدم الموظف المتعاقد لتحديث بياناته وتمديد صلاحية حسابه أو إيقاف جميع الخدمات الإلكترونية

Service Options & Workflows:

1. Create Account Request:
   Fill Form → Manager Approval → End

2. Add Contractor on Attendance Leave System:
   Fill Form → Manager Approval → Application Ops → End

3. Edit Contractor Account:
   Fill Form → Manager Approval → Routes to appropriate team (Application Operation, Moamalat, ERP, SD) → End

4. Stop all Electronic Services:
   Fill Form → Manager Approval → Routes to ERP, SD, Moamalat → End

5. Extend Account Validity:
   Fill Form → Manager Approval → Service Desk → End

6. Reactivate Disabled Account:
   Fill Form → Manager Approval → End',
'KnowledgeArticle', 'SVC-10505', 'ENT-SVC-10505', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Service Desk", "title": "User Account Management - Contractors", "title_ar": "خدمات الموظف المتعاقد", "category": "IT Services/Accounts/Contractor", "status": "Published", "service_id": "10505", "keywords": "contractor, account, extend, create, stop, متعاقد, حساب, تمديد"}');

-- SVC-10229: Release Blocked Email
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10229-0', generate_random_embedding(),
'Service: Release Blocked Email (تمرير بريد إلكتروني محجوب)

Service ID: 10229
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10229
Category: IT Services > Accounts Services and Technical Privilege

Description (EN): Through this form, you can submit your request to pass the pending mail that did not reach you
Description (AR): مجموعة بريدية، ايميل، بريد الكتروني، تمرير، فك، سماح

Workflow:
1. Fill Form - Provide email details and reason for release
2. IT Security Approval - Security team reviews the blocked email
3. Network team - Implements the release
4. End

Use this service when an email has been blocked by security filters and you need it released.',
'KnowledgeArticle', 'SVC-10229', 'ENT-SVC-10229', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Network", "title": "Release Blocked Email", "title_ar": "تمرير بريد إلكتروني محجوب", "category": "IT Services/Accounts/Email", "status": "Published", "service_id": "10229", "keywords": "email, blocked, release, بريد, محجوب, تمرير"}');

-- SVC-10502: Service Access Management
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10502-0', generate_random_embedding(),
'Service: Service Access Management (إدارة حساب الخدمة)

Service ID: 10502
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10502
Category: IT Services > Accounts Services and Technical Privilege

Description (EN): Service Account Management - manage technical service accounts (create, modify, stop)
Description (AR): خدمة لإدارة حسابات الخدمات التقنية (إنشاء، تعديل، إيقاف)

Workflow:
1. Fill Form - Specify service account requirements
2. Manager Approval
3. Service Desk fulfillment
4. End

Use this service for technical service accounts used by applications and systems.',
'KnowledgeArticle', 'SVC-10502', 'ENT-SVC-10502', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Service Desk", "title": "Service Access Management", "title_ar": "إدارة حساب الخدمة", "category": "IT Services/Accounts/ServiceAccount", "status": "Published", "service_id": "10502", "keywords": "service account, technical, خدمة, حساب تقني"}');

-- =============================================================================
-- IT SERVICES - TECHNICAL SERVICES
-- =============================================================================

-- SVC-10515: IT Demand Management
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10515-0', generate_random_embedding(),
'Service: IT Demand Management (إدارة طلبات التحسينات)

Service ID: 10515
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10515
Category: IT Services > Technical Services - Application, PC, Network

Description (EN): Demand is request for any new software, system, or solution implementation/development such as developing new service or application, mobile application, or integration. Any new infrastructure solution implementation or Demand for new licenses. Includes major enhancements/updates to existing service/system.

Description (AR): أي حل جديد للبنية التحتية أو طلب الحصول على تراخيص جديدة ويشمل التحسينات/التحديثات الكبرى لخدمة قائمة أو نظام قائم

Workflow:
1. Fill Form - Describe the new system/enhancement needed
2. EA Approval - Enterprise Architecture review
3. Business Development Approval
4. Sector Business Approval
5. GRC Approval - Governance, Risk & Compliance review
6. If Require Business Process Design = Yes → BPM (Business Development, DT, EA, IT Governance, GRC)
7. End

Use this for new software development, system implementation, major upgrades, or new license requests.',
'KnowledgeArticle', 'SVC-10515', 'ENT-SVC-10515', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "IT Governance", "title": "IT Demand Management", "title_ar": "إدارة طلبات التحسينات", "category": "IT Services/Technical/Demand", "status": "Published", "service_id": "10515", "keywords": "demand, new system, development, enhancement, طلب, تطوير, تحسين"}');

-- SVC-10511: Request To Upgrade Access To Internal Network ISE
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10511-0', generate_random_embedding(),
'Service: Request To Upgrade Access To Internal Network ISE (طلب ترقية صلاحية الوصول للشبكة الداخلية ISE)

Service ID: 10511
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10511
Category: IT Services > Technical Services - Application, PC, Network

Description (EN): Request To Upgrade Access To The Internal Network ISE
Description (AR): طلب ترقية صلاحية الوصول للشبكة الداخلية ISE

Workflow:
1. Fill Form - Specify ISE access requirements
2. Manager Approval
3. Service Desk fulfillment
4. End

ISE (Identity Services Engine) is used for network access control.',
'KnowledgeArticle', 'SVC-10511', 'ENT-SVC-10511', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Service Desk", "title": "Upgrade ISE Network Access", "title_ar": "طلب ترقية صلاحية الوصول للشبكة الداخلية ISE", "category": "IT Services/Technical/Network", "status": "Published", "service_id": "10511", "keywords": "ISE, network, access, شبكة, صلاحية"}');

-- SVC-10509: End-User Devices Services
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10509-0', generate_random_embedding(),
'Service: End-User Devices Services (خدمات أجهزة العميل)

Service ID: 10509
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10509
Category: IT Services > Technical Services - Application, PC, Network

Description (EN): Service which allows users to request the configuration or setup of a new device, add a device to VPNPC, or add a user to the VPN Client
Description (AR): خدمة تتيح بطلب تهيئة أو إعداد جهاز جديد أو خدمة إضافة جهاز على VPNPC أو خدمة إضافة مستخدم على VPN Client

Workflow:
1. Fill Form - Select device type and configuration needed
2. Service Desk fulfillment
3. End

No manager approval required for this service. Use for:
- New device setup/configuration
- Adding device to VPN PC
- Adding user to VPN Client',
'KnowledgeArticle', 'SVC-10509', 'ENT-SVC-10509', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Service Desk", "title": "End-User Devices Services", "title_ar": "خدمات أجهزة العميل", "category": "IT Services/Technical/Devices", "status": "Published", "service_id": "10509", "keywords": "device, setup, configuration, VPN, جهاز, إعداد, تهيئة"}');

-- SVC-10247: Software Installation
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10247-0', generate_random_embedding(),
'Service: Software Installation (تثبيت برنامج)

Service ID: 10247
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10247
Category: IT Services > Technical Services - Application, PC, Network

Description (EN): This feature provides the option to install approved or unapproved software, with approved software including a predefined list of specific programs
Description (AR): خدمة تتاح لتثبيت البرامج المعتمدة أو الغير معتمدة، حيث أن البرامج المعتمدة تضم عدة برامج محددة مسبقا

Workflow:
1. Fill Form - Select software to install

If Non-Certified Software (برنامج غير معتمد):
2. GRC Approval - Security review required
3. SOC Approval - Security Operations Center review
4. Service Desk - Installation
5. End

If Certified Software (برنامج معتمد):
2. Service Desk - Direct installation
3. End

Certified software has faster approval as it is pre-approved by security.',
'KnowledgeArticle', 'SVC-10247', 'ENT-SVC-10247', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Service Desk", "title": "Software Installation", "title_ar": "تثبيت برنامج", "category": "IT Services/Technical/Software", "status": "Published", "service_id": "10247", "keywords": "software, install, program, برنامج, تثبيت"}');

-- SVC-10216: Static IP Address Management
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10216-0', generate_random_embedding(),
'Service: Static IP Address Management (إدارة عنوان بروتوكول إنترنت ثابت)

Service ID: 10216
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10216
Category: IT Services > Technical Services - Application, PC, Network

Description (EN): Through this form, you can submit a request to request a static IP address
Description (AR): الخاص بجهازك IP يمكنك من خلال هذا النموذج تقديم طلب بتثبيت عنوان

Workflow:
1. Fill Form - Specify device and IP requirements
2. Manager Approval
3. Systems team - IP assignment
4. End

Use this service when you need a fixed/static IP address for your device.',
'KnowledgeArticle', 'SVC-10216', 'ENT-SVC-10216', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Systems", "title": "Static IP Address Management", "title_ar": "إدارة عنوان بروتوكول إنترنت ثابت", "category": "IT Services/Technical/Network", "status": "Published", "service_id": "10216", "keywords": "IP, static, address, عنوان, ثابت"}');

-- SVC-10516: Add New IT Contract / IT License
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10516-0', generate_random_embedding(),
'Service: Add New IT Contract / IT License (إضافة، تحديث، حذف رخصة تقنية)

Service ID: 10516
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10516
Category: IT Services > Technical Services - Application, PC, Network

Description (EN): This option is related to managing technical licenses, including (adding, updating, or deleting) licenses
Description (AR): خدمة تخص الرخص التقنية من: (إضافة - تحديث - حذف) الرخص

Workflow:
1. Fill Form - Specify license details and action needed
2. IT Governance team review and processing
3. End

Use this service for:
- Adding new software licenses
- Updating existing license information
- Removing/deleting licenses',
'KnowledgeArticle', 'SVC-10516', 'ENT-SVC-10516', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "IT Governance", "title": "IT License Management", "title_ar": "إضافة، تحديث، حذف رخصة تقنية", "category": "IT Services/Technical/License", "status": "Published", "service_id": "10516", "keywords": "license, contract, add, update, delete, رخصة, عقد"}');

-- SVC-10301: Services Publication Request
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10301-0', generate_random_embedding(),
'Service: Services Publication Request (طلب نشر خدمات)

Service ID: 10301
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10301
Category: IT Services > Technical Services

Description (EN): Services Publication Request
Description (AR): طلب نشر خدمات

Workflow - If Urgent:
1. Fill Form - Select Urgent
2. Approving content (Sector and Department Management)
3. Publication approval (Governor Office)
4. End

Workflow - If Not Urgent (Standard):
1. Fill Form
2. Review numbers (Studies & Performance)
3. Approving content (Sector and Department Management)
4. Preparing communication plan (Digital communication)
5. Design communication plan (General Department of Institutional Communication)
6. Material design (Marketing)
7. Proofreading
8. Initial approval (Digital communication Management)
9. Approval of produced materials (Corporate Communication Management)
10. Review and approval of publication content (Sector and Department Management)
11. Publication of media material (Digital communication)
12. End',
'KnowledgeArticle', 'SVC-10301', 'ENT-SVC-10301', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Digital Communication", "title": "Services Publication Request", "title_ar": "طلب نشر خدمات", "category": "IT Services/Technical/Publication", "status": "Published", "service_id": "10301", "keywords": "publication, services, نشر, خدمات"}');

-- =============================================================================
-- IT SERVICES - STORAGE AND COMMUNICATION
-- =============================================================================

-- SVC-10249: Personal Folder Management
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10249-0', generate_random_embedding(),
'Service: Personal Folder Management (إدارة المجلد الشخصي)

Service ID: 10249
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10249
Category: IT Services > Storage and Communication Services

Description (EN): This service offers the creation of a personal folder or an increase in the size of the personal folder, with the desired size specified
Description (AR): خدمة تقدم لإنشاء مجلد شخصي أو زيادة سعة المجلد الشخصي مع تحديد حجم السعة المرغوبة

Workflow - If Create Personal Folder:
1. Fill Form - Request new personal folder
2. Service Desk - Creates folder
3. End

Workflow - If Increase Folder Size:
1. Fill Form - Specify desired size
2. Systems team - Processes increase
3. End',
'KnowledgeArticle', 'SVC-10249', 'ENT-SVC-10249', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Service Desk", "title": "Personal Folder Management", "title_ar": "إدارة المجلد الشخصي", "category": "IT Services/Storage/Personal", "status": "Published", "service_id": "10249", "keywords": "folder, personal, storage, مجلد, شخصي, سعة"}');

-- SVC-10259: Shared Folder Management
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10259-0', generate_random_embedding(),
'Service: Shared Folder Management (إدارة المجلد المشترك)

Service ID: 10259
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10259
Category: IT Services > Storage and Communication Services

Description (EN): This option covers the creation of a shared folder, granting access to a specific path, modifying or deleting access rights, or restoring files in the shared folder
Description (AR): خدمة تخص المجلدات المشتركة ابتداء من انشاء المجلد المشترك أو منح صلاحية لمسار معين أو تعديل أو حذف الصلاحية أو إستعادة ملفات على المجلد المشترك

Workflow:
1. Fill Form - Specify folder action needed
2. Manager Approval (VIP users bypass this step)
3. Service Desk fulfillment
4. End

Use this service for:
- Creating new shared folders
- Granting access to specific paths
- Modifying or deleting access rights
- Restoring files in shared folders',
'KnowledgeArticle', 'SVC-10259', 'ENT-SVC-10259', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Service Desk", "title": "Shared Folder Management", "title_ar": "إدارة المجلد المشترك", "category": "IT Services/Storage/Shared", "status": "Published", "service_id": "10259", "keywords": "folder, shared, access, restore, مجلد, مشترك, صلاحية"}');

-- SVC-10254: IP Telephony Management
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10254-0', generate_random_embedding(),
'Service: IP Telephony Management (إدارة الهاتف الشبكي)

Service ID: 10254
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10254
Category: IT Services > Storage and Communication Services

Description (EN): You can submit a request for an extension to your network phone or add a new extension or other features
Description (AR): خدمة لإدارة الهاتف الشبكي الأساسي والافتراضي (تفعيل هاتف، تفعيل خدمات الاتصال السريع، وغيرها)

Workflow - For: New IP Phone, International Call Advantage, Forward Call:
1. Fill Form
2. Manager Approval
3. Service Desk fulfillment
4. End

Workflow - For other phone requests:
1. Fill Form
2. Service Desk fulfillment
3. End

Use this service for phone activation, speed dial, call forwarding, and other telephony features.',
'KnowledgeArticle', 'SVC-10254', 'ENT-SVC-10254', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Service Desk", "title": "IP Telephony Management", "title_ar": "إدارة الهاتف الشبكي", "category": "IT Services/Communication/Phone", "status": "Published", "service_id": "10254", "keywords": "phone, IP, telephony, extension, هاتف, شبكي, تحويلة"}');

-- SVC-10257: Meeting Applications Management
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10257-0', generate_random_embedding(),
'Service: Meeting Applications Management (إدارة تطبيقات الإجتماعات الإفتراضية)

Service ID: 10257
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10257
Category: IT Services > Storage and Communication Services

Description (EN): This feature relates to managing virtual meetings (Webex, Teams, Zoom), as well as providing add-ons in Outlook and addressing related requests
Description (AR): خدمة تخص إدراة برامج الاجتماعات الافتراضية (ويبكس، تيمز، زوم)، بالإضافة إلى خدمة الإضافات في الأوتلوك، وحل الطلبات المتعلقة بها

Workflow:
1. Fill Form - Select meeting application and issue type
2. Collaboration team fulfillment
3. End

Use this service for:
- Webex, Teams, Zoom issues and requests
- Outlook add-ons for meeting applications
- Virtual meeting troubleshooting',
'KnowledgeArticle', 'SVC-10257', 'ENT-SVC-10257', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Collaboration", "title": "Meeting Applications Management", "title_ar": "إدارة تطبيقات الإجتماعات الإفتراضية", "category": "IT Services/Communication/Meetings", "status": "Published", "service_id": "10257", "keywords": "meeting, Webex, Teams, Zoom, اجتماعات, تيمز, زوم"}');

-- =============================================================================
-- IT SERVICES - GENERAL TECHNICAL
-- =============================================================================

-- SVC-10205: Moamalat System Requests
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10205-0', generate_random_embedding(),
'Service: Moamalat System Requests (طلبات نظام معاملات)

Service ID: 10205
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10205
Category: IT Services > General Technical Services

Description (EN): Through this form, you can submit this request for Moamalat System
Description (AR): يمكنك من خلال هذا النموذج تقديم طلب على نظام معاملات

Workflow:
1. Fill Form - Submit Moamalat request
2. Moamalat team processing
3. End

Use this service for all Moamalat (document management) system related requests.',
'KnowledgeArticle', 'SVC-10205', 'ENT-SVC-10205', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Moamalat", "title": "Moamalat System Requests", "title_ar": "طلبات نظام معاملات", "category": "IT Services/Technical/Moamalat", "status": "Published", "service_id": "10205", "keywords": "Moamalat, documents, system, معاملات, نظام"}');

-- SVC-10507: Mobile Devices Management (MDM)
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10507-0', generate_random_embedding(),
'Service: Mobile Devices Management - MDM (إدارة الأجهزة المحمولة)

Service ID: 10507
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10507
Category: IT Services > General Technical Services

Description (EN): This service allows for the management of mobile devices (MDM) on computers, laptops, or mobile phones, enabling users to access organizational services, request technical support, manage data, and enhance security. It also includes Multi-Factor Authentication (MFA) for all operating systems on mobile devices.

Description (AR): خدمة يمكن عن طريقها إدارة الأجهزة المحمولة (MDM) في أجهزة الكمبيوتر أو اللابتوب أو الهواتف المحمولة حيث تمكن المستفيد من الوصول الى خدمات الهيئة و طلب الدعم الفني وإدارة البيانات وتعزيز الأمان، بالإضافة إلى المصادقة بالتحقق الثنائي (MFA) لكافة أنظمة التشغيل للهواتف المتنقلة.

Workflow:
1. Fill Form - Select MDM service type
2. Service Desk fulfillment
3. End

Use this service for mobile device enrollment, MFA setup, and device management.',
'KnowledgeArticle', 'SVC-10507', 'ENT-SVC-10507', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Service Desk", "title": "Mobile Devices Management (MDM)", "title_ar": "إدارة الأجهزة المحمولة", "category": "IT Services/Technical/MDM", "status": "Published", "service_id": "10507", "keywords": "MDM, mobile, MFA, device, أجهزة, محمولة, هاتف"}');

-- SVC-10513: Virtual Private Network Request
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10513-0', generate_random_embedding(),
'Service: Virtual Private Network Request (طلب الشبكة الخاصة الافتراضية)

Service ID: 10513
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10513
Category: IT Services > General Technical Services

Description (EN): Virtual Private Network Request
Description (AR): طلب الشبكة الخاصة الافتراضية

Service Options & Workflows:

Option 1 - Add User to VPN Client:
1. Fill Form
2. Manager Approval (VIP users bypass this step)
3. GRC Approval
4. Service Desk
5. Network team
6. End

Option 2 - Add Device On VPNPC:
1. Fill Form
2. Manager Approval (VIP users bypass this step)
3. GRC Approval
4. IT Security
5. End

VPN access enables secure remote work connectivity.',
'KnowledgeArticle', 'SVC-10513', 'ENT-SVC-10513', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Network", "title": "Virtual Private Network Request", "title_ar": "طلب الشبكة الخاصة الافتراضية", "category": "IT Services/Technical/VPN", "status": "Published", "service_id": "10513", "keywords": "VPN, remote, access, شبكة, افتراضية"}');

-- =============================================================================
-- IT SERVICES - SECURITY TECHNOLOGY
-- =============================================================================

-- SVC-10228: Allow/Block Access to Websites
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10228-0', generate_random_embedding(),
'Service: Allow/Block Access to Websites & Addresses (السماح/حظر الوصول إلى مواقع الويب والعناوين)

Service ID: 10228
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10228
Category: IT Services > Security Technology Services

Description (EN): Through this form, you can submit your request to allow access to a dedicated external link to which access was previously blocked. It requires the approval of the SOC team
Description (AR): يمكنك من خلال هذا النموذج تقديم طلبك بالسماح للوصول الى رابط خارجي مخصص قد تم منع الوصول اليه سابقا. ويتطلب موافقة إدارة الأمن السيبراني التشغيلي

Workflow:
1. Fill Form - Provide website URL and justification
2. Manager Approval (VIP users bypass this step)
3. GRC Approval - Security review
4. Network team - Implements access change
5. End

Use this service to request access to blocked websites or to block specific websites.',
'KnowledgeArticle', 'SVC-10228', 'ENT-SVC-10228', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Network", "title": "Allow/Block Website Access", "title_ar": "السماح/حظر الوصول إلى مواقع الويب والعناوين", "category": "IT Services/Security/WebAccess", "status": "Published", "service_id": "10228", "keywords": "website, block, allow, access, موقع, حظر, سماح"}');

-- SVC-10101: Technical Incident
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10101-0', generate_random_embedding(),
'Service: Technical Incident (بلاغ عن مشكلة تقنية)

Service ID: 10101
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10101
Category: IT Services > Report Issue

Description (EN): This service is for submitting requests to resolve technical issues that disrupt the normal operation of a system, service, or application
Description (AR): خدمة ترفع لحل مشكلة تقنية للأحداث التي تعطل التشغيل الطبيعي لنظام أو خدمة أو تطبيق

Workflow:
1. Fill Form - Describe the technical issue
2. Assignment Based on the issue - Routed to appropriate team
3. End

Use this service to report:
- System malfunctions
- Application errors
- Service disruptions
- Technical problems',
'KnowledgeArticle', 'SVC-10101', 'ENT-SVC-10101', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Service Desk", "title": "Technical Incident Report", "title_ar": "بلاغ عن مشكلة تقنية", "category": "IT Services/Incident", "status": "Published", "service_id": "10101", "keywords": "incident, problem, issue, technical, مشكلة, تقنية, بلاغ"}');

-- SVC-10512: Antivirus And Adobe License
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10512-0', generate_random_embedding(),
'Service: Antivirus And Adobe License (رخص Adobe ومكافح الفيروسات)

Service ID: 10512
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10512
Category: IT Services > License Services

Description (EN): This option is available for requesting Adobe or antivirus licenses
Description (AR): خدمة تقدم لطلب رخص أدوبي أو مكافح الفيروسات

Workflow - If Adobe License:
1. Fill Form
2. Manager Approval
3. Eid El Rashed (License Manager) Approval
4. Service Desk - License assignment
5. End

Workflow - If Antivirus License:
1. Fill Form
2. Service Desk - Direct fulfillment
3. End

Antivirus licenses have faster processing as they are standard security requirements.',
'KnowledgeArticle', 'SVC-10512', 'ENT-SVC-10512', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Service Desk", "title": "Antivirus And Adobe License", "title_ar": "رخص Adobe ومكافح الفيروسات", "category": "IT Services/License", "status": "Published", "service_id": "10512", "keywords": "Adobe, antivirus, license, رخصة, أدوبي, مكافح"}');

-- =============================================================================
-- SUPPORT SERVICES
-- =============================================================================

-- SVC-10114: Request A New Car
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10114-0', generate_random_embedding(),
'Service: Request A New Car (طلب تأمين سيارة جديدة)

Service ID: 10114
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10114
Category: Support Services > Document Management and Administrative Communications

Description (EN): Insuring a new car through rent or purchase in accordance with car insurance and rental regulations
Description (AR): تأمين سيارة عن طريق الايجار او الشراء وفق ضوابط تأمين واستئجار السيارات بالهيئة

Workflow:
1. Fill Form - Specify car requirements
2. Abdullah AlQaoud Approval
3. Ahmed AlJarboa Approval
4. Azzam AlJibali Approval
5. Service team - Processes request
6. End

Use this service for new car acquisition through rental or purchase.',
'KnowledgeArticle', 'SVC-10114', 'ENT-SVC-10114', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Services", "title": "Request A New Car", "title_ar": "طلب تأمين سيارة جديدة", "category": "Support Services/Cars", "status": "Published", "service_id": "10114", "keywords": "car, new, rent, purchase, سيارة, جديدة, شراء, ايجار"}');

-- SVC-10115: Fuel Supply For Sectors and Branches
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10115-0', generate_random_embedding(),
'Service: Fuel Supply For Sectors and Branches (طلب استلام الوقود لفروع وقطاعات الهيئة)

Service ID: 10115
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10115
Category: Support Services > Document Management and Administrative Communications

Description (EN): Receiving the fuel card for cars designated for carrying out official business and tasks
Description (AR): استلام بطاقة الوقود للسيارات المخصصة لتأدية الاعمال والمهام الرسمية

Workflow:
1. Fill Form - Request fuel card
2. Abdullah Alqaoud Approval
3. Service team - Issues fuel card
4. End

Use this service to request fuel cards for official vehicles.',
'KnowledgeArticle', 'SVC-10115', 'ENT-SVC-10115', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Services", "title": "Fuel Supply Request", "title_ar": "طلب استلام الوقود لفروع وقطاعات الهيئة", "category": "Support Services/Cars", "status": "Published", "service_id": "10115", "keywords": "fuel, card, car, وقود, بطاقة, سيارة"}');

-- SVC-10117: Request A Car With A Driver
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10117-0', generate_random_embedding(),
'Service: Request A Car With A Driver (طلب سيارة بسائق)

Service ID: 10117
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10117
Category: Support Services > Document Management and Administrative Communications

Description (EN): A car with a driver to transport the authority''s leaders to perform official work inside Riyadh during official working hours
Description (AR): سيارة بسائق لنقل قيادات الهيئة لتأدية المهام الموكله اليهم داخل الرياض خلال أوقات العمل الرسمية

Workflow:
1. Fill Form - Specify date, time, and destination
2. Abdullah Alqaoud Approval
3. Services Approval
4. Service team - Assigns car and driver
5. End

This service is for leadership transportation within Riyadh during working hours.',
'KnowledgeArticle', 'SVC-10117', 'ENT-SVC-10117', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Services", "title": "Request A Car With A Driver", "title_ar": "طلب سيارة بسائق", "category": "Support Services/Cars", "status": "Published", "service_id": "10117", "keywords": "car, driver, transport, سيارة, سائق, نقل"}');

-- SVC-10113: Request To Use An Existing Car
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10113-0', generate_random_embedding(),
'Service: Request To Use An Existing Car (طلب استخدام سيارة)

Service ID: 10113
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10113
Category: Support Services > Document Management and Administrative Communications

Description (EN): Using a temporary or permanent car that is entrusted to the employee in accordance with car insurance and rental regulations
Description (AR): استخدام سيارة مؤقته او دائمة تكون عهدة على الموظف وفق ضوابط تأمين و استئجار السيارات

Workflow:
1. Fill Form - Specify car usage requirements
2. Abdullah Alqaoud Approval
3. Service team - Assigns car
4. End

Use this for temporary or permanent car assignment to an employee.',
'KnowledgeArticle', 'SVC-10113', 'ENT-SVC-10113', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Services", "title": "Request To Use An Existing Car", "title_ar": "طلب استخدام سيارة", "category": "Support Services/Cars", "status": "Published", "service_id": "10113", "keywords": "car, use, existing, سيارة, استخدام, عهدة"}');

-- SVC-10116: SMSA Shipping
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10116-0', generate_random_embedding(),
'Service: SMSA Shipping (طلب بوليصة شحن سمسا)

Service ID: 10116
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10116
Category: Support Services > Document Management and Administrative Communications

Description (EN): For work shipments for the Authority and personal shipments via express shipping
Description (AR): لإرساليات العمل الخاصة بالهيئة والارساليات الشخصية عن طريق الشحن السريع

Workflow:
1. Fill Form - Provide shipping details
2. Abdullah Alqaoud Approval
3. Service team - Processes shipment
4. End

Use this service for official and personal SMSA express shipping requests.',
'KnowledgeArticle', 'SVC-10116', 'ENT-SVC-10116', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Services", "title": "SMSA Shipping Request", "title_ar": "طلب بوليصة شحن سمسا", "category": "Support Services/Shipping", "status": "Published", "service_id": "10116", "keywords": "SMSA, shipping, express, شحن, سمسا, ارسالية"}');

-- =============================================================================
-- SUPPORT SERVICES - SECURITY, HEALTH AND SAFETY
-- =============================================================================

-- SVC-10120: Identity Card Request
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10120-0', generate_random_embedding(),
'Service: Identity Card Request (إصدار بطاقة)

Service ID: 10120
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10120
Category: Support Services > Security, Health and Safety Services

Description (EN): Identity Card Request - Request for employee identification card
Description (AR): إصدار بطاقة

Workflow:
1. Fill Form - Provide employee details
2. Manager Approval
3. SecFac01 Approval
4. SecFac02 - Issues card
5. End

Use this service to request new or replacement employee identity cards.',
'KnowledgeArticle', 'SVC-10120', 'ENT-SVC-10120', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Facilities", "title": "Identity Card Request", "title_ar": "إصدار بطاقة", "category": "Support Services/Security", "status": "Published", "service_id": "10120", "keywords": "identity, card, badge, بطاقة, هوية"}');

-- SVC-10119: Input / Output Materials
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10119-0', generate_random_embedding(),
'Service: Input / Output Materials (إدخال / إخراج مواد)

Service ID: 10119
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10119
Category: Support Services > Security, Health and Safety Services

Description (EN): Input / Output Materials - Permission to bring in or take out materials
Description (AR): إدخال / إخراج مواد

Workflow:
1. Fill Form - List materials and purpose
2. SecFac05 Approval
3. SecFac06 Approval
4. SecFac07 - Processes request
5. End

Use this service to request permission to bring materials into or take out of the building.',
'KnowledgeArticle', 'SVC-10119', 'ENT-SVC-10119', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Facilities", "title": "Input/Output Materials", "title_ar": "إدخال / إخراج مواد", "category": "Support Services/Security", "status": "Published", "service_id": "10119", "keywords": "materials, input, output, مواد, إدخال, إخراج"}');

-- SVC-10118: Issuance of Car Poster
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10118-0', generate_random_embedding(),
'Service: Issuance of Car Poster (إصدار ملصق)

Service ID: 10118
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10118
Category: Support Services > Security, Health and Safety Services

Description (EN): Issuance of Car Poster - Request for vehicle parking sticker/poster
Description (AR): إصدار ملصق

Workflow:
1. Fill Form - Provide vehicle details
2. SecFac03 Approval
3. SecFac04 - Issues sticker
4. End

Use this service to request a parking sticker for your vehicle.',
'KnowledgeArticle', 'SVC-10118', 'ENT-SVC-10118', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Facilities", "title": "Issuance of Car Poster", "title_ar": "إصدار ملصق", "category": "Support Services/Security", "status": "Published", "service_id": "10118", "keywords": "car, poster, sticker, parking, ملصق, سيارة, موقف"}');

-- SVC-10512-WORK: Request For Work Authorization
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-WORK-AUTH-0', generate_random_embedding(),
'Service: Request For Work Authorization (تصريح عمل)

Service ID: 10512
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10512
Category: Support Services > Security, Health and Safety Services

Description (EN): Request For Work Authorization - Permission for maintenance or construction work
Description (AR): تصريح عمل

Workflow - If Location = Riyadh:
1. Fill Form
2. Designated Approvers (based on work type)
3. SecFac06
4. Secfac7
5. Facilities team - Issues authorization
6. End

Workflow - If Location = Jeddah:
1. Fill Form
2. Designated Approvers
3. Secfac10 - Issues authorization
4. End

Use this service to request authorization for maintenance, construction, or other work activities.',
'KnowledgeArticle', 'SVC-WORK-AUTH', 'ENT-SVC-WORK', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Facilities", "title": "Request For Work Authorization", "title_ar": "تصريح عمل", "category": "Support Services/Security", "status": "Published", "service_id": "10512", "keywords": "work, authorization, permit, تصريح, عمل, صيانة"}');

-- SVC-10124: Visitation Request
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10124-0', generate_random_embedding(),
'Service: Visitation Request (طلب زيارة)

Service ID: 10124
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10124
Category: Support Services > Security, Health and Safety Services

Description (EN): Request for visitor access to the building
Description (AR): طلب زيارة

Workflow:
1. Fill Form - Provide visitor details and visit purpose
2. Sec.Sup Approval - Security supervisor approves
3. Sec.rec - Reception processes the visit
4. End

Use this service to request access for external visitors to the building.',
'KnowledgeArticle', 'SVC-10124', 'ENT-SVC-10124', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Security", "title": "Visitation Request", "title_ar": "طلب زيارة", "category": "Support Services/Security", "status": "Published", "service_id": "10124", "keywords": "visit, visitor, access, زيارة, زائر, دخول"}');

-- SVC-10122: Visitation Request For Female Section
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10122-0', generate_random_embedding(),
'Service: Visitation Request For Female Section (طلب زيارة للقسم النسائي)

Service ID: 10122
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10122
Category: Support Services > Security, Health and Safety Services

Description (EN): Request for visitor access to the female section of the building
Description (AR): طلب زيارة للقسم النسائي

Workflow:
1. Fill Form - Provide visitor details and visit purpose
2. Sec.Sup Approval - Security supervisor approves
3. Sec.Female - Female section reception processes
4. End

Use this service specifically for visits to the female section of the building.',
'KnowledgeArticle', 'SVC-10122', 'ENT-SVC-10122', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Security", "title": "Visitation Request - Female Section", "title_ar": "طلب زيارة للقسم النسائي", "category": "Support Services/Security", "status": "Published", "service_id": "10122", "keywords": "visit, female, section, زيارة, نسائي, قسم"}');

-- SVC-10123: VIP-Visitation Request
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10123-0', generate_random_embedding(),
'Service: VIP-Visitation Request (طلب زيارة VIP)

Service ID: 10123
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10123
Category: Support Services > Security, Health and Safety Services

Description (EN): Request for VIP visitor access to the building
Description (AR): طلب زيارة VIP

Workflow:
1. Fill Form - Provide VIP visitor details
2. Sec.Sup Approval - Security supervisor approves
3. Sec.rec - Reception prepares for VIP arrival
4. End

Use this service for high-profile or VIP visitor arrangements.',
'KnowledgeArticle', 'SVC-10123', 'ENT-SVC-10123', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Security", "title": "VIP Visitation Request", "title_ar": "طلب زيارة VIP", "category": "Support Services/Security", "status": "Published", "service_id": "10123", "keywords": "VIP, visit, visitor, زيارة, شخصية مهمة"}');

-- =============================================================================
-- LEGAL CONSULTATION SERVICES
-- =============================================================================

-- SVC-10105: Board Theme
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10105-0', generate_random_embedding(),
'Service: Board Theme (موضوع مجلس إدارة)

Service ID: 10105
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10105
Category: Legal Consultation Services

Description (EN): Board Theme - Request related to board of directors topics
Description (AR): موضوع مجلس إدارة

Workflow:
1. Fill Form - Submit board topic details
2. Saad Alghamdi Approval
3. Team ZE (Legal) - Processes request
4. End

Use this service for matters related to the board of directors.',
'KnowledgeArticle', 'SVC-10105', 'ENT-SVC-10105', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Team ZE", "title": "Board Theme", "title_ar": "موضوع مجلس إدارة", "category": "Legal Services/Board", "status": "Published", "service_id": "10105", "keywords": "board, directors, theme, مجلس, إدارة, موضوع"}');

-- SVC-10106: Contract Draft
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10106-0', generate_random_embedding(),
'Service: Contract Draft (مسودة عقد)

Service ID: 10106
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10106
Category: Legal Consultation Services

Description (EN): Contract Draft - Request for contract drafting or review
Description (AR): مسودة عقد

Workflow:
1. Fill Form - Submit contract requirements
2. Saad Alghamdi Approval
3. Sultan Alruwais Approval
4. Team ZE (Legal) - Drafts/reviews contract
5. End

Use this service to request contract drafting or review by the legal team.',
'KnowledgeArticle', 'SVC-10106', 'ENT-SVC-10106', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Team ZE", "title": "Contract Draft", "title_ar": "مسودة عقد", "category": "Legal Services/Contract", "status": "Published", "service_id": "10106", "keywords": "contract, draft, agreement, عقد, مسودة, اتفاقية"}');

-- SVC-10108: Draft Agreement/Note of Understanding
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10108-0', generate_random_embedding(),
'Service: Draft Agreement/Note of Understanding or Cooperation (مسودة اتفاقية/مذكرة تفاهم أو تعاون)

Service ID: 10108
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10108
Category: Legal Consultation Services

Description (EN): Draft agreement/Note of understanding or cooperation
Description (AR): مسودة اتفاقية/مذكرة تفاهم أو تعاون

Workflow:
1. Fill Form - Submit agreement/MOU requirements
2. Saad Alghamdi Approval
3. Team ZE (Legal) - Drafts document
4. End

Use this service for MOU or cooperation agreement drafting.',
'KnowledgeArticle', 'SVC-10108', 'ENT-SVC-10108', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Team ZE", "title": "Draft Agreement/MOU", "title_ar": "مسودة اتفاقية/مذكرة تفاهم أو تعاون", "category": "Legal Services/Agreement", "status": "Published", "service_id": "10108", "keywords": "agreement, MOU, cooperation, understanding, اتفاقية, مذكرة, تفاهم, تعاون"}');

-- SVC-10107: Draft Minutes
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10107-0', generate_random_embedding(),
'Service: Draft Minutes (مسودة محضر)

Service ID: 10107
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10107
Category: Legal Consultation Services

Description (EN): Draft Minutes - Request for meeting minutes drafting
Description (AR): مسودة محضر

Workflow:
1. Fill Form - Submit meeting details
2. Saad Alghamdi Approval
3. Team ZE (Legal) - Drafts minutes
4. End

Use this service for official meeting minutes drafting.',
'KnowledgeArticle', 'SVC-10107', 'ENT-SVC-10107', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Team ZE", "title": "Draft Minutes", "title_ar": "مسودة محضر", "category": "Legal Services/Minutes", "status": "Published", "service_id": "10107", "keywords": "minutes, meeting, draft, محضر, اجتماع, مسودة"}');

-- SVC-10110: Draft Regulatory Document
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10110-0', generate_random_embedding(),
'Service: Draft Regulatory Document (مسودة وثيقة تنظيمية)

Service ID: 10110
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10110
Category: Legal Consultation Services

Description (EN): Draft regulatory document
Description (AR): مسودة وثيقة تنظيمية

Workflow:
1. Fill Form - Submit regulatory document requirements
2. Saad Alghamdi Approval
3. Sultan Alruwais Approval
4. Team ZE (Legal) - Drafts document
5. End

Use this service for regulatory document drafting.',
'KnowledgeArticle', 'SVC-10110', 'ENT-SVC-10110', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Team ZE", "title": "Draft Regulatory Document", "title_ar": "مسودة وثيقة تنظيمية", "category": "Legal Services/Regulatory", "status": "Published", "service_id": "10110", "keywords": "regulatory, document, policy, وثيقة, تنظيمية, سياسة"}');

-- SVC-10109: Letter Draft
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10109-0', generate_random_embedding(),
'Service: Letter Draft (مسودة خطاب)

Service ID: 10109
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10109
Category: Legal Consultation Services

Description (EN): Letter Draft - Request for official letter drafting
Description (AR): مسودة خطاب

Workflow:
1. Fill Form - Submit letter requirements
2. Saad Alghamdi Approval
3. Team ZE (Legal) - Drafts letter
4. End

Use this service for official letter drafting by the legal team.',
'KnowledgeArticle', 'SVC-10109', 'ENT-SVC-10109', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Team ZE", "title": "Letter Draft", "title_ar": "مسودة خطاب", "category": "Legal Services/Letter", "status": "Published", "service_id": "10109", "keywords": "letter, draft, official, خطاب, مسودة, رسمي"}');

-- SVC-LEGAL-OPINION: Express a Legal Opinion
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-LEGAL-OPINION-0', generate_random_embedding(),
'Service: Express a Legal Opinion (ابداء رأي قانوني)

Service ID: SRGAA5V0HHDDCARDQUMARCRQN3C20M
URL: https://dwpstg.citc.gov.sa/dwp/app/#/srm/profile/SRGAA5V0HHDDCARDQUMARCRQN3C20M/srm
Category: Legal Consultation Services

Description (EN): Express a legal opinion - Request for legal opinion on a matter
Description (AR): ابداء رأي قانوني

Workflow:
1. Fill Form - Submit the matter requiring legal opinion
2. Saad Alghamdi Approval
3. Team ZE (Legal) - Provides legal opinion
4. End

Use this service to request a formal legal opinion on any matter.',
'KnowledgeArticle', 'SVC-LEGAL-OPINION', 'ENT-SVC-LEGAL-OP', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Team ZE", "title": "Express a Legal Opinion", "title_ar": "ابداء رأي قانوني", "category": "Legal Services/Opinion", "status": "Published", "keywords": "legal, opinion, advice, رأي, قانوني, استشارة"}');

-- SVC-10111: Submit a Legal Study
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10111-0', generate_random_embedding(),
'Service: Submit a Legal Study (تقديم دراسة قانونية)

Service ID: 10111
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10111
Category: Legal Consultation Services

Description (EN): Submit a legal study - Request for comprehensive legal study
Description (AR): تقديم دراسة قانونية

Workflow:
1. Fill Form - Submit study requirements and scope
2. Saad Alghamdi Approval
3. Team ZE (Legal) - Conducts legal study
4. End

Use this service to request a comprehensive legal study on a topic.',
'KnowledgeArticle', 'SVC-10111', 'ENT-SVC-10111', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Team ZE", "title": "Submit a Legal Study", "title_ar": "تقديم دراسة قانونية", "category": "Legal Services/Study", "status": "Published", "service_id": "10111", "keywords": "legal, study, research, دراسة, قانونية, بحث"}');

-- =============================================================================
-- INSPECTION SERVICES
-- =============================================================================

-- SVC-10401: Inspection Request Service
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10401-0', generate_random_embedding(),
'Service: Inspection Request Service (خدمة طلبات التفتيش)

Service ID: 10401
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10401
Category: Inspection Services > General Directorate of Inspection

Description (EN): Inspection Request service - Request for inspection activities
Description (AR): خدمة طلبات التفتيش

Workflow:
1. Fill Form - Submit inspection request details
2. Planning Inspection Team Approval
3. Inspection reviewers - Process and assign inspection
4. End

Use this service to request inspection activities.',
'KnowledgeArticle', 'SVC-10401', 'ENT-SVC-10401', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Inspection", "title": "Inspection Request Service", "title_ar": "خدمة طلبات التفتيش", "category": "Inspection Services", "status": "Published", "service_id": "10401", "keywords": "inspection, request, تفتيش, طلب"}');

-- =============================================================================
-- GEOSPATIAL SERVICES
-- =============================================================================

-- SVC-10605: Account And Permission On GIS
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10605-0', generate_random_embedding(),
'Service: Account And Permission On GIS (الحساب والصلاحيات على نظام المعلومات الجغرافية)

Service ID: 10605
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10605
Category: Geospatial Services

Description (EN): Request access permissions for geospatial systems and specify the intended use
Description (AR): طلب صلاحيات الوصول للأنظمة الجيومكانية وتحديد الغرض من استخدامها

Workflow:
1. Fill Form - Specify GIS access requirements and intended use
2. Ahmed AlDamegh Approval
3. ADHOC Approval (Based on Application Name)
4. Geospatial Data Center - Grants access
5. End

Use this service to request access to GIS systems and applications.',
'KnowledgeArticle', 'SVC-10605', 'ENT-SVC-10605', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Geospatial Data Center", "title": "GIS Account and Permission", "title_ar": "الحساب والصلاحيات على نظام المعلومات الجغرافية", "category": "Geospatial Services/Access", "status": "Published", "service_id": "10605", "keywords": "GIS, geospatial, access, permission, جيومكاني, صلاحية, نظام معلومات جغرافية"}');

-- SVC-10603: Geospatial Analysis Request
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10603-0', generate_random_embedding(),
'Service: Geospatial Analysis Request (طلب تحليل جيومكاني)

Service ID: 10603
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10603
Category: Geospatial Services

Description (EN): Request customized geospatial analyses based on available or required data
Description (AR): طلب تحليلات جيومكانية مخصصة بناءً على البيانات المتاحة والمطلوبة

Workflow:
1. Fill Form - Describe analysis requirements and data needs
2. Ahmed AlDamegh Approval
3. Geospatial Data Center - Performs analysis
4. End

Use this service for custom geospatial analysis and mapping.',
'KnowledgeArticle', 'SVC-10603', 'ENT-SVC-10603', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Geospatial Data Center", "title": "Geospatial Analysis Request", "title_ar": "طلب تحليل جيومكاني", "category": "Geospatial Services/Analysis", "status": "Published", "service_id": "10603", "keywords": "geospatial, analysis, mapping, تحليل, جيومكاني, خرائط"}');

-- SVC-10602: Geospatial Consultation Request
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10602-0', generate_random_embedding(),
'Service: Geospatial Consultation Request (طلب استشارة جيومكانية)

Service ID: 10602
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10602
Category: Geospatial Services

Description (EN): Request consultation to solve a problem or effectively use geospatial data
Description (AR): طلب استشارة لحل مشكلة أو استخدام البيانات الجيومكانية بشكل فعّال

Workflow:
1. Fill Form - Describe consultation needs
2. Ahmed AlDamegh Approval
3. Geospatial Data Center - Provides consultation
4. End

Use this service for geospatial guidance and consultation.',
'KnowledgeArticle', 'SVC-10602', 'ENT-SVC-10602', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Geospatial Data Center", "title": "Geospatial Consultation", "title_ar": "طلب استشارة جيومكانية", "category": "Geospatial Services/Consultation", "status": "Published", "service_id": "10602", "keywords": "geospatial, consultation, advice, استشارة, جيومكانية"}');

-- SVC-10604: GIS Portal Technical Issue
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10604-0', generate_random_embedding(),
'Service: GIS Portal Technical Issue (مشكلة تقنية في بوابة نظام المعلومات الجغرافية)

Service ID: 10604
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10604
Category: Geospatial Services

Description (EN): Support for resolving account, login, or geospatial system usage issues
Description (AR): دعم لحل مشكلات الحسابات أو الدخول أو استخدام الأنظمة الجيومكانية

Workflow:
1. Fill Form - Describe the technical issue
2. Geospatial Data Center - Resolves issue
3. End

No approval required for technical support. Use this for GIS technical problems.',
'KnowledgeArticle', 'SVC-10604', 'ENT-SVC-10604', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Geospatial Data Center", "title": "GIS Portal Technical Issue", "title_ar": "مشكلة تقنية في بوابة نظام المعلومات الجغرافية", "category": "Geospatial Services/Support", "status": "Published", "service_id": "10604", "keywords": "GIS, technical, issue, problem, مشكلة, تقنية, جيومكاني"}');

-- SVC-10601: Operational Dashboard Request
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('SVC-10601-0', generate_random_embedding(),
'Service: Operational Dashboard Request (طلب انشاء لوحة معلومات تشغيلية)

Service ID: 10601
URL: https://dwpstg.citc.gov.sa/dwp/app/#/checkout/10601
Category: Geospatial Services

Description (EN): Request a dashboard to display key performance indicators and operational data
Description (AR): طلب إنشاء لوحة لعرض مؤشرات الأداء والبيانات التشغيلية المهمة

Workflow:
1. Fill Form - Describe dashboard requirements and KPIs needed
2. Ahmed AlDamegh Approval
3. Geospatial Data Center - Creates dashboard
4. End

Use this service to request operational dashboards with KPIs and data visualizations.',
'KnowledgeArticle', 'SVC-10601', 'ENT-SVC-10601', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Geospatial Data Center", "title": "Operational Dashboard Request", "title_ar": "طلب انشاء لوحة معلومات تشغيلية", "category": "Geospatial Services/Dashboard", "status": "Published", "service_id": "10601", "keywords": "dashboard, KPI, operational, لوحة, معلومات, تشغيلية, مؤشرات"}');

-- =============================================================================
-- WORKFLOW REFERENCE GUIDE
-- =============================================================================
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('DAMEE-WORKFLOW-REF-0', generate_random_embedding(),
'Damee Platform Workflow Components Reference (مرجع مكونات سير العمل)

APPROVAL TYPES (أنواع الموافقات):
- Manager Approval (موافقة المدير): Direct supervisor approval
- VIP Bypass (تجاوز VIP): Some services allow VIP users to skip manager approval
- GRC Approval (موافقة GRC): Governance, Risk, and Compliance approval for security-sensitive requests
- App Owner Approval (موافقة مالك التطبيق): Application owner approval for app access
- IT Security Approval (موافقة أمن المعلومات): Security team approval
- SOC Approval (موافقة SOC): Security Operations Center approval

FULFILLMENT TEAMS (فرق التنفيذ):
- Service Desk (مكتب الخدمة): General IT support
- Infrastructure Operation (عمليات البنية التحتية): Server and infrastructure management
- Database Ops (عمليات قواعد البيانات): Database operations team
- Network (الشبكات): Network operations team
- Application Operation (عمليات التطبيقات): Application support team
- Moamalat (معاملات): Document management system team
- ERP: Enterprise Resource Planning team
- Collaboration (التعاون): Meeting and collaboration tools team
- Geospatial Data Center (مركز البيانات الجيومكانية): GIS and mapping services team
- IT Governance (حوكمة تقنية المعلومات): IT policy and standards team
- Team ZE: Legal team',
'KnowledgeArticle', 'DAMEE-WORKFLOW-REF', 'ENT-DAMEE-WF', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Service Desk", "title": "Workflow Components Reference", "title_ar": "مرجع مكونات سير العمل", "category": "Platform/Reference", "status": "Published", "keywords": "workflow, approval, fulfillment, teams, سير عمل, موافقة, فرق"}');

-- =============================================================================
-- Update sync_state to reflect Damee services data has been loaded
-- =============================================================================
INSERT INTO sync_state (source_type, last_sync_timestamp, last_sync_at, records_synced, status)
VALUES ('DameeService', EXTRACT(EPOCH FROM CURRENT_TIMESTAMP)::BIGINT, CURRENT_TIMESTAMP, 56, 'completed')
ON CONFLICT (source_type) DO UPDATE SET
    last_sync_timestamp = EXCLUDED.last_sync_timestamp,
    last_sync_at = EXCLUDED.last_sync_at,
    records_synced = EXCLUDED.records_synced,
    status = EXCLUDED.status;

-- =============================================================================
-- Verify data load
-- =============================================================================
DO $$
DECLARE
    damee_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO damee_count FROM embedding_store WHERE source_id LIKE 'DAMEE%' OR source_id LIKE 'SVC-%';
    RAISE NOTICE 'Damee services data loaded successfully!';
    RAISE NOTICE 'Total Damee service entries created: %', damee_count;
END $$;
