-- BMC Remedy RAG Agent - BMC Remedy System Guides and How-To Articles
-- Comprehensive knowledge base for guiding users through ITSM processes
-- Run after seed_dummy_data.sql

\echo 'Starting BMC Remedy guides seed...'

-- Ensure embedding generator exists
CREATE OR REPLACE FUNCTION generate_random_embedding()
RETURNS vector(384) AS $$
DECLARE
    arr float8[384];
    magnitude float8 := 0;
    i int;
BEGIN
    FOR i IN 1..384 LOOP
        arr[i] := random() * 2 - 1;
        magnitude := magnitude + arr[i] * arr[i];
    END LOOP;
    magnitude := sqrt(magnitude);
    FOR i IN 1..384 LOOP
        arr[i] := arr[i] / magnitude;
    END LOOP;
    RETURN arr::vector(384);
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- INCIDENT MANAGEMENT GUIDES
-- =============================================================================
\echo 'Inserting Incident Management guides...'

-- KB0010: BMC Remedy Overview
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
('KB0010-CONTENT-0', generate_random_embedding(),
'Knowledge Article KB0010: Introduction to BMC Remedy IT Service Management

BMC Remedy is our enterprise IT Service Management (ITSM) platform used to manage all IT support activities. This article provides an overview of the system and its key modules.

WHAT IS BMC REMEDY?
BMC Remedy AR System (Action Request System) is an industry-leading ITSM solution that helps organizations deliver IT services efficiently. It follows ITIL (Information Technology Infrastructure Library) best practices.

KEY MODULES:
1. INCIDENT MANAGEMENT - Report and track IT issues affecting your work
2. WORK ORDER MANAGEMENT - Request IT services and equipment
3. CHANGE MANAGEMENT - Request and track infrastructure changes
4. KNOWLEDGE MANAGEMENT - Search for solutions and documentation
5. ASSET MANAGEMENT - Track IT assets and configurations

ACCESSING BMC REMEDY:
- Web Portal: https://remedy.acme.com (recommended)
- Direct Client: BMC Remedy User application (IT staff only)
- Mobile App: BMC MyIT app (iOS/Android)
- Email: servicedesk@acme.com (creates incident automatically)

WHEN TO USE EACH MODULE:
- Something is BROKEN → Create an INCIDENT
- You NEED something → Create a WORK ORDER
- Infrastructure CHANGE needed → Create a CHANGE REQUEST
- Looking for ANSWERS → Search KNOWLEDGE BASE',
'KnowledgeArticle', 'KB0010', 'ENT000000200010', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Service Desk", "title": "Introduction to BMC Remedy ITSM", "category": "BMC Remedy/Overview/Introduction", "status": "Published", "keywords": "BMC Remedy, ITSM, IT Service Management, overview, introduction"}'),

('KB0010-CONTENT-1', generate_random_embedding(),
'BMC REMEDY NAVIGATION BASICS:

HOME CONSOLE:
When you log in, you see your personalized console showing:
- My Open Requests: Your submitted incidents and work orders
- My Approvals: Items waiting for your approval
- Announcements: IT service announcements and planned maintenance
- Quick Links: Common actions and searches

TOP NAVIGATION:
- Applications menu: Access different modules
- Search bar: Global search across all records
- Create New: Quick create for incidents, work orders
- Profile: Your settings and preferences

SEARCHING FOR TICKETS:
1. Use the global search bar for quick searches
2. Enter ticket number (INC000001, WO000001, CHG000001)
3. Or search by keywords from the summary
4. Use Advanced Search for complex queries

TICKET STATUSES (color coded):
- NEW (Blue): Just created, not yet assigned
- ASSIGNED (Yellow): Assigned to a technician
- IN PROGRESS (Orange): Being actively worked
- PENDING (Gray): Waiting for information or vendor
- RESOLVED (Green): Solution provided, awaiting confirmation
- CLOSED (Dark Gray): Completed and verified

NOTIFICATIONS:
- Email notifications for status changes
- In-app notifications in the bell icon
- Configure preferences in your profile',
'KnowledgeArticle', 'KB0010', 'ENT000000200010', 'ARTICLE_CONTENT', 1,
'{"assigned_group": "Service Desk", "title": "Introduction to BMC Remedy ITSM", "category": "BMC Remedy/Overview/Introduction", "status": "Published"}');

-- KB0011: How to Create an Incident
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
('KB0011-CONTENT-0', generate_random_embedding(),
'Knowledge Article KB0011: How to Create an Incident in BMC Remedy

An incident is a report of an unplanned interruption to an IT service or reduction in quality. Use this guide to submit incidents effectively.

WHEN TO CREATE AN INCIDENT:
- Your computer, application, or service is not working
- You are experiencing errors or performance issues
- Something that was working has stopped working
- You need urgent IT support for a broken service

DO NOT create an incident for:
- Requesting new equipment → Use Work Order
- Requesting new software → Use Work Order
- Requesting access/permissions → Use Work Order
- Planned infrastructure changes → Use Change Request

STEP-BY-STEP: CREATING AN INCIDENT

Step 1: Access the Portal
- Go to https://remedy.acme.com
- Log in with your network credentials
- Click "Create New" → "Incident" (or use the Incident tile on home page)

Step 2: Describe Your Issue (Summary Field)
Write a clear, concise summary (max 100 characters):
GOOD: "Outlook crashes when opening attachments"
GOOD: "Cannot access shared drive S: - permission denied"
BAD: "Computer not working" (too vague)
BAD: "HELP!!!" (not descriptive)

Step 3: Provide Details (Description Field)
Include ALL relevant information:
- What were you trying to do?
- What happened instead?
- When did it start?
- Error messages (exact text or screenshot)
- Is anyone else affected?
- What have you already tried?',
'KnowledgeArticle', 'KB0011', 'ENT000000200011', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Service Desk", "title": "How to Create an Incident", "category": "BMC Remedy/Incident/Create", "status": "Published", "keywords": "incident, create incident, submit ticket, report issue, BMC Remedy"}'),

('KB0011-CONTENT-1', generate_random_embedding(),
'Step 4: Select Categorization
Choose the category that best matches your issue:

HARDWARE
├── Desktop/Laptop → Computer hardware issues
├── Printer → Printing problems
├── Monitor/Display → Screen issues
├── Peripheral → Keyboard, mouse, docking station
└── Mobile Device → Phone, tablet issues

SOFTWARE
├── Operating System → Windows, macOS issues
├── Microsoft Office → Word, Excel, PowerPoint, Outlook
├── Email → Outlook, webmail, calendar
├── Browser → Chrome, Edge, Firefox issues
└── Business Application → SAP, CRM, custom apps

NETWORK
├── Connectivity → Cannot connect to network
├── VPN → Remote access issues
├── WiFi → Wireless connection problems
├── Internet → External website access
└── Performance → Slow network

SECURITY
├── Account/Password → Login issues, locked accounts
├── Permissions → Access denied errors
├── Virus/Malware → Suspected infection
└── Phishing → Suspicious emails

Step 5: Set Impact and Urgency
IMPACT - Who is affected?
- Individual (1 user)
- Department (5-25 users)
- Multiple Departments (25-100 users)
- Enterprise (100+ users or critical service)

URGENCY - How quickly is resolution needed?
- Low: Workaround available, can wait
- Medium: Work affected but can continue
- High: Cannot perform primary job function
- Critical: Business-critical service down

The system automatically calculates PRIORITY based on Impact + Urgency.',
'KnowledgeArticle', 'KB0011', 'ENT000000200011', 'ARTICLE_CONTENT', 1,
'{"assigned_group": "Service Desk", "title": "How to Create an Incident", "category": "BMC Remedy/Incident/Create", "status": "Published"}'),

('KB0011-CONTENT-2', generate_random_embedding(),
'Step 6: Provide Contact Information
- Verify your contact phone number is correct
- Add alternate contact if you will be unavailable
- Specify preferred contact method (phone/email)
- Include your location (building, floor, desk number)

Step 7: Attach Supporting Information
Click "Add Attachment" to include:
- Screenshots of error messages
- Log files if requested
- Documents showing the issue
- Maximum file size: 10MB per attachment
- Supported formats: PNG, JPG, PDF, DOCX, XLSX, TXT, LOG

Step 8: Review and Submit
- Review all information for accuracy
- Click "Submit" button
- Note your Incident Number (INC000XXXXX)
- You will receive email confirmation

AFTER SUBMISSION:
1. You receive email confirmation with incident number
2. Incident is triaged by Service Desk (usually within 15 minutes)
3. Assigned to appropriate support team
4. Technician may contact you for more information
5. You can track progress in the portal
6. You receive notification when resolved
7. Please confirm resolution or reopen if issue persists

TIPS FOR FASTER RESOLUTION:
- Be specific and detailed in your description
- Include error messages verbatim
- Attach screenshots when possible
- Respond promptly to technician questions
- Keep your contact information updated
- Check your email (including spam) for updates',
'KnowledgeArticle', 'KB0011', 'ENT000000200011', 'ARTICLE_CONTENT', 2,
'{"assigned_group": "Service Desk", "title": "How to Create an Incident", "category": "BMC Remedy/Incident/Create", "status": "Published"}');

-- KB0012: Incident Lifecycle and Statuses
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
('KB0012-CONTENT-0', generate_random_embedding(),
'Knowledge Article KB0012: Understanding the Incident Lifecycle

This article explains how incidents flow through the system from creation to closure.

INCIDENT LIFECYCLE STAGES:

1. IDENTIFICATION & LOGGING
   └─ User reports issue via portal, phone, email, or chat
   └─ Incident created in BMC Remedy
   └─ Automatic categorization suggested
   └─ Initial priority calculated
   Status: NEW

2. CATEGORIZATION & PRIORITIZATION
   └─ Service Desk reviews and validates category
   └─ Priority adjusted if needed based on business impact
   └─ Related incidents/problems identified
   Status: NEW → ASSIGNED

3. INVESTIGATION & DIAGNOSIS
   └─ Technical team investigates root cause
   └─ Knowledge base searched for known solutions
   └─ Additional information gathered from user
   └─ May escalate to specialized teams
   Status: ASSIGNED → IN PROGRESS

4. RESOLUTION & RECOVERY
   └─ Solution implemented
   └─ Service restored to normal operation
   └─ Resolution documented
   └─ User notified of resolution
   Status: IN PROGRESS → RESOLVED

5. CLOSURE
   └─ User confirms issue is resolved
   └─ Or auto-closes after 5 business days with no response
   └─ Final categorization verified
   └─ Knowledge article created if needed
   Status: RESOLVED → CLOSED',
'KnowledgeArticle', 'KB0012', 'ENT000000200012', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Service Desk", "title": "Incident Lifecycle and Statuses", "category": "BMC Remedy/Incident/Process", "status": "Published", "keywords": "incident lifecycle, status, workflow, process, ticket status"}'),

('KB0012-CONTENT-1', generate_random_embedding(),
'DETAILED STATUS DESCRIPTIONS:

NEW (Status Code: 0)
- Incident just created
- Not yet reviewed by Service Desk
- Awaiting initial triage
- Expected duration: < 15 minutes during business hours

ASSIGNED (Status Code: 1)
- Reviewed and categorized
- Assigned to support group or individual
- Waiting to be picked up by technician
- SLA clock is running

IN PROGRESS (Status Code: 2)
- Technician actively working on issue
- Investigation or troubleshooting underway
- May require user interaction
- Updates posted to work log

PENDING (Status Code: 3)
- Work temporarily paused
- Requires external action
Pending reasons include:
  - Pending User: Waiting for information from you
  - Pending Vendor: Waiting for third-party response
  - Pending Change: Waiting for approved change
  - Pending Problem: Linked to problem investigation
  - Scheduled: Planned for future date
Note: SLA clock may pause during Pending status

RESOLVED (Status Code: 4)
- Solution has been implemented
- Service should be restored
- Awaiting user confirmation
- Auto-closes in 5 business days if no response
- You can REOPEN if issue persists

CLOSED (Status Code: 5)
- Incident fully completed
- User confirmed resolution OR auto-closed
- Cannot be modified
- New incident required for recurring issues

CANCELLED (Status Code: 6)
- Incident withdrawn before resolution
- Duplicate of another incident
- Created in error
- No longer needed',
'KnowledgeArticle', 'KB0012', 'ENT000000200012', 'ARTICLE_CONTENT', 1,
'{"assigned_group": "Service Desk", "title": "Incident Lifecycle and Statuses", "category": "BMC Remedy/Incident/Process", "status": "Published"}'),

('KB0012-CONTENT-2', generate_random_embedding(),
'HOW TO CHECK YOUR INCIDENT STATUS:

Via Web Portal:
1. Log in to https://remedy.acme.com
2. Click "My Open Requests" on home page
3. Find your incident in the list
4. Click to open and view full details
5. Check "Status" field and "Work Log" for updates

Via Email:
- You receive automatic emails when status changes
- Each email includes current status and recent notes
- Reply to email to add information (creates work log entry)

Via Phone:
- Call Service Desk: 1-800-555-HELP
- Provide your incident number
- Agent can provide status update

COMMON QUESTIONS:

Q: Why is my incident still "New"?
A: During business hours, incidents are typically triaged within 15 minutes. After hours, it may take longer. High-priority incidents are addressed first.

Q: What does "Pending User" mean?
A: The technician needs information from you. Check the work log for their question and respond promptly to avoid delays.

Q: My incident was resolved but the issue is back. What do I do?
A: If within 5 days and still in "Resolved" status, you can reopen it by adding a work note explaining the recurrence. If already closed, create a new incident and reference the old one.

Q: Why was my incident cancelled?
A: Common reasons: duplicate report, issue resolved itself, or requested by submitter. Check the resolution notes for details.

Q: How do I escalate an urgent incident?
A: Call the Service Desk and request escalation. Provide business impact justification. Manager can also request escalation via the portal.',
'KnowledgeArticle', 'KB0012', 'ENT000000200012', 'ARTICLE_CONTENT', 2,
'{"assigned_group": "Service Desk", "title": "Incident Lifecycle and Statuses", "category": "BMC Remedy/Incident/Process", "status": "Published"}');

-- KB0013: Priority Matrix and SLAs
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
('KB0013-CONTENT-0', generate_random_embedding(),
'Knowledge Article KB0013: Understanding Incident Priority and SLA Targets

This article explains how incident priority is calculated and what Service Level Agreement (SLA) targets apply to each priority level.

PRIORITY CALCULATION:
Priority is automatically calculated based on two factors:
1. IMPACT - The scope of the issue (who is affected)
2. URGENCY - How quickly resolution is needed

PRIORITY MATRIX:

                    U R G E N C Y
                 Critical | High | Medium | Low
    I   Critical     1       1       2       3
    M   High         1       2       2       3
    P   Medium       2       2       3       4
    A   Low          3       3       4       4
    C
    T

PRIORITY DEFINITIONS:

PRIORITY 1 - CRITICAL
- Business-critical service completely unavailable
- Affecting entire organization or critical business function
- No workaround available
- Examples: Email server down, ERP system unavailable, network outage

PRIORITY 2 - HIGH
- Significant impact on business operations
- Major system degraded or unavailable for department
- Limited or difficult workaround
- Examples: Department application down, VPN issues for remote team

PRIORITY 3 - MEDIUM
- Moderate impact on business operations
- Single user or small group affected
- Workaround available
- Examples: Individual software crash, printer issues

PRIORITY 4 - LOW
- Minimal impact on business operations
- Issue is inconvenient but not blocking
- Easy workaround available
- Examples: Cosmetic issues, feature requests misclassified as incidents',
'KnowledgeArticle', 'KB0013', 'ENT000000200013', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Service Desk", "title": "Incident Priority and SLA Targets", "category": "BMC Remedy/Incident/SLA", "status": "Published", "keywords": "priority, SLA, urgency, impact, response time, resolution time"}'),

('KB0013-CONTENT-1', generate_random_embedding(),
'SERVICE LEVEL AGREEMENT (SLA) TARGETS:

Priority 1 - Critical:
├── Initial Response: 15 minutes
├── Status Update: Every 30 minutes
├── Resolution Target: 4 hours
├── Escalation: Automatic after 2 hours
└── Support: 24x7 (including weekends/holidays)

Priority 2 - High:
├── Initial Response: 30 minutes
├── Status Update: Every 2 hours
├── Resolution Target: 8 hours
├── Escalation: Automatic after 4 hours
└── Support: Extended hours (7 AM - 10 PM)

Priority 3 - Medium:
├── Initial Response: 4 hours
├── Status Update: Daily
├── Resolution Target: 24 hours
├── Escalation: On request
└── Support: Business hours (8 AM - 6 PM M-F)

Priority 4 - Low:
├── Initial Response: 8 hours
├── Status Update: Every 3 days
├── Resolution Target: 72 hours
├── Escalation: On request
└── Support: Business hours (8 AM - 6 PM M-F)

IMPORTANT NOTES:
- Response time = Time until first meaningful response from IT
- Resolution time = Time until service is restored
- Times are in BUSINESS HOURS unless noted otherwise
- SLA clock pauses when status is "Pending User"
- SLA clock continues during "Pending Vendor"
- Holidays follow the corporate calendar',
'KnowledgeArticle', 'KB0013', 'ENT000000200013', 'ARTICLE_CONTENT', 1,
'{"assigned_group": "Service Desk", "title": "Incident Priority and SLA Targets", "category": "BMC Remedy/Incident/SLA", "status": "Published"}'),

('KB0013-CONTENT-2', generate_random_embedding(),
'ESCALATION PROCESS:

Automatic Escalation Triggers:
- Priority 1: No response in 15 min → Escalate to on-call manager
- Priority 1: Not resolved in 2 hours → Escalate to IT Director
- Priority 2: No response in 30 min → Escalate to team lead
- SLA breach imminent → Notification to management

Manual Escalation:
If you feel your incident needs higher priority attention:
1. Add a work note explaining business impact
2. Contact Service Desk and request escalation
3. Have your manager contact IT management
4. For P1/P2: Use the "Request Escalation" button

ESCALATION LEVELS:
Level 1: Service Desk (initial triage and common issues)
Level 2: Technical Support Teams (specialized troubleshooting)
Level 3: Engineering/Development (complex technical issues)
Level 4: Vendor Support (third-party product issues)
Management: IT Management involvement for business impact

WHAT AFFECTS SLA:
Starts SLA Clock:
- Incident creation
- Status changed from Pending to Active

Pauses SLA Clock:
- Status = Pending User (waiting for your response)
- Status = Pending Scheduled (planned future work)

Does NOT Pause:
- Pending Vendor
- Pending Change
- Weekends (for P3/P4 only)

TIPS TO AVOID SLA ISSUES:
- Respond quickly when contacted by IT
- Provide complete information upfront
- Be available at your listed contact number
- Update the ticket if situation changes',
'KnowledgeArticle', 'KB0013', 'ENT000000200013', 'ARTICLE_CONTENT', 2,
'{"assigned_group": "Service Desk", "title": "Incident Priority and SLA Targets", "category": "BMC Remedy/Incident/SLA", "status": "Published"}');

-- KB0014: Adding Notes and Attachments to Incidents
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
('KB0014-CONTENT-0', generate_random_embedding(),
'Knowledge Article KB0014: How to Add Notes and Attachments to Your Incident

Learn how to communicate with IT support and provide additional information for your incident.

ADDING WORK NOTES:

Via Web Portal:
1. Log in to https://remedy.acme.com
2. Open your incident from "My Open Requests"
3. Scroll to "Work Info" or "Activity Log" section
4. Click "Add Work Note" or the note icon
5. Type your message in the text field
6. Select note type:
   - General Information: Standard updates
   - Customer Communication: Visible to all parties
   - Customer Follow-up: Your questions to IT
7. Click "Save" or "Submit"

Via Email Reply:
1. Find the notification email for your incident
2. Reply to the email
3. Write your message above the quoted text
4. Your reply is automatically added to the incident
5. Note: Keep the incident number in the subject line

WHAT TO INCLUDE IN NOTES:
- New information about the issue
- Results of troubleshooting steps you tried
- Changes in the situation (better/worse)
- Questions for the technician
- Availability for calls or remote sessions
- Confirmation that issue is resolved or still occurring',
'KnowledgeArticle', 'KB0014', 'ENT000000200014', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Service Desk", "title": "Adding Notes and Attachments", "category": "BMC Remedy/Incident/Update", "status": "Published", "keywords": "work note, attachment, update incident, add information, communicate"}'),

('KB0014-CONTENT-1', generate_random_embedding(),
'ADDING ATTACHMENTS:

Supported File Types:
- Images: PNG, JPG, JPEG, GIF, BMP (screenshots)
- Documents: PDF, DOC, DOCX, TXT, RTF
- Spreadsheets: XLS, XLSX, CSV
- Logs: LOG, TXT, XML
- Archives: ZIP (for multiple files)

Size Limits:
- Maximum per file: 10 MB
- Maximum total per incident: 50 MB
- For larger files: Contact Service Desk for file share link

How to Attach Files:
1. Open your incident in the portal
2. Click "Attachments" tab or "Add Attachment" button
3. Click "Choose File" or drag and drop
4. Select file from your computer
5. Add description (optional but helpful)
6. Click "Upload" or "Attach"

SCREENSHOTS - BEST PRACTICES:

Capturing Screenshots:
- Windows: Press Windows + Shift + S (Snipping Tool)
- Mac: Press Cmd + Shift + 4
- Full screen: Print Screen key (Windows) or Cmd + Shift + 3 (Mac)

What to Capture:
- Error message dialogs (include full text)
- Application state when issue occurs
- System information if relevant
- Browser console for web issues (F12 → Console tab)

Tips:
- Capture the entire error message, not just part
- Include the title bar showing application name
- Circle or highlight the relevant area
- If multiple screens, number them in order
- Save as PNG for best quality',
'KnowledgeArticle', 'KB0014', 'ENT000000200014', 'ARTICLE_CONTENT', 1,
'{"assigned_group": "Service Desk", "title": "Adding Notes and Attachments", "category": "BMC Remedy/Incident/Update", "status": "Published"}'),

('KB0014-CONTENT-2', generate_random_embedding(),
'COLLECTING DIAGNOSTIC INFORMATION:

Windows System Information:
1. Press Windows + R
2. Type "msinfo32" and press Enter
3. File → Export → Save as text file
4. Attach to incident

Windows Event Logs:
1. Press Windows + R
2. Type "eventvwr" and press Enter
3. Navigate to Windows Logs → Application or System
4. Look for Error entries around the time of issue
5. Right-click the error → Copy → Details as Text
6. Paste into a text file and attach

Application Log Files:
Common locations:
- %AppData%\\[Application Name]\\logs
- C:\\ProgramData\\[Application Name]\\logs
- Check application Help → About for log location

Network Information:
1. Open Command Prompt (Windows + R, type "cmd")
2. Type: ipconfig /all > network_info.txt
3. Attach the file to incident

Browser Information (for web issues):
1. Open browser DevTools (F12)
2. Go to Console tab - screenshot any red errors
3. Go to Network tab - look for failed requests (red)
4. Include browser name and version (Help → About)

RESPONDING TO IT REQUESTS:
When IT asks for information:
1. Check the work log for their specific request
2. Gather the requested information
3. Add a work note with your response
4. Attach any requested files
5. Update your availability if needed
6. Prompt response helps resolve issues faster',
'KnowledgeArticle', 'KB0014', 'ENT000000200014', 'ARTICLE_CONTENT', 2,
'{"assigned_group": "Service Desk", "title": "Adding Notes and Attachments", "category": "BMC Remedy/Incident/Update", "status": "Published"}');

-- =============================================================================
-- WORK ORDER MANAGEMENT GUIDES
-- =============================================================================
\echo 'Inserting Work Order Management guides...'

-- KB0020: Understanding Work Orders
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
('KB0020-CONTENT-0', generate_random_embedding(),
'Knowledge Article KB0020: Understanding Work Orders vs Incidents

Learn the difference between Work Orders and Incidents and when to use each.

WHAT IS A WORK ORDER?
A Work Order is a request for IT services that are:
- Planned and predictable
- Standard or routine
- Not related to a broken service
- Part of normal IT service delivery

INCIDENT vs WORK ORDER:

INCIDENT (Something is BROKEN):
- My computer crashed
- I cannot access my email
- Application is showing errors
- Network is slow or unavailable
- Printer is not working

WORK ORDER (I NEED something):
- I need a new laptop
- I need software installed
- I need access to a system
- I need my equipment moved
- I need a new user account created

SIMPLE RULE:
"If it was working and now it is not → INCIDENT"
"If you need something new or changed → WORK ORDER"

COMMON WORK ORDER TYPES:

HARDWARE REQUESTS:
- New computer or laptop
- Additional monitor
- Keyboard, mouse, headset
- Docking station
- Mobile device (phone, tablet)

SOFTWARE REQUESTS:
- Application installation
- Software license request
- Software upgrade
- Development tools

ACCESS REQUESTS:
- System access / permissions
- Shared drive access
- Application access
- VPN access for contractor
- Building/badge access',
'KnowledgeArticle', 'KB0020', 'ENT000000200020', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Service Desk", "title": "Understanding Work Orders", "category": "BMC Remedy/WorkOrder/Overview", "status": "Published", "keywords": "work order, service request, incident vs work order, request something"}'),

('KB0020-CONTENT-1', generate_random_embedding(),
'ACCOUNT REQUESTS:
- New employee setup
- Contractor account
- Password reset (if locked out)
- Account modification
- Account termination

MOVE/ADD/CHANGE:
- Desk relocation
- Office move
- Equipment relocation
- Phone number change
- Display name change

TRAINING REQUESTS:
- Software training
- Security awareness training
- System-specific training

WORK ORDER PROCESS OVERVIEW:

1. SUBMISSION
   └─ User submits request via portal
   └─ Automatic routing based on request type
   └─ Approval workflow triggered if required
   Status: SUBMITTED

2. APPROVAL (if required)
   └─ Manager approval for cost items
   └─ Security approval for access requests
   └─ May require multiple approvers
   Status: PENDING APPROVAL

3. PLANNING
   └─ Work scheduled
   └─ Resources allocated
   └─ Parts ordered if needed
   Status: ASSIGNED

4. EXECUTION
   └─ Work performed
   └─ User contacted for delivery/installation
   Status: IN PROGRESS

5. COMPLETION
   └─ Work verified
   └─ User confirms satisfaction
   Status: COMPLETED → CLOSED',
'KnowledgeArticle', 'KB0020', 'ENT000000200020', 'ARTICLE_CONTENT', 1,
'{"assigned_group": "Service Desk", "title": "Understanding Work Orders", "category": "BMC Remedy/WorkOrder/Overview", "status": "Published"}');

-- KB0021: How to Submit a Work Order
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
('KB0021-CONTENT-0', generate_random_embedding(),
'Knowledge Article KB0021: How to Submit a Work Order

Step-by-step guide to requesting IT services through the Work Order system.

BEFORE YOU START:
1. Verify this is a request, not a break/fix issue (use Incident for that)
2. Check if approval is needed (software > $500, hardware > $1000)
3. Have justification ready for non-standard requests
4. Know the timeline - when do you need this?

STEP-BY-STEP: CREATING A WORK ORDER

Step 1: Access the Service Catalog
- Log in to https://remedy.acme.com
- Click "Service Catalog" or "Request Something"
- Browse categories or use search

Step 2: Find Your Request Type
Popular categories:
├── Hardware
│   ├── Desktop/Laptop Request
│   ├── Monitor Request
│   ├── Peripheral Request
│   └── Mobile Device Request
├── Software
│   ├── Software Installation
│   ├── License Request
│   └── Software Removal
├── Access
│   ├── System Access Request
│   ├── Shared Drive Access
│   ├── VPN Access
│   └── Application Access
├── Accounts
│   ├── New Employee Setup
│   ├── Contractor Account
│   └── Account Modification
└── Other Services
    ├── Equipment Move
    ├── Training Request
    └── General IT Request

Step 3: Click on the Request Type
- Review the service description
- Note any prerequisites or requirements
- Check expected fulfillment time
- Click "Request" or "Add to Cart"',
'KnowledgeArticle', 'KB0021', 'ENT000000200021', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Service Desk", "title": "How to Submit a Work Order", "category": "BMC Remedy/WorkOrder/Create", "status": "Published", "keywords": "submit work order, service request, request equipment, request software, request access"}'),

('KB0021-CONTENT-1', generate_random_embedding(),
'Step 4: Complete the Request Form

FOR HARDWARE REQUESTS:
- Select specific model if known
- Indicate if replacing existing equipment
- Provide business justification
- Specify delivery location (building, floor, desk)
- Note any special requirements

FOR SOFTWARE REQUESTS:
- Specify exact software name and version
- Provide business justification
- Confirm you have budget approval if > $500
- Indicate if this is for project use
- Note any urgency

FOR ACCESS REQUESTS:
- Specify exact system/resource name
- Select access level needed (read, write, admin)
- Provide business justification
- List approving manager if not your direct manager
- Indicate duration (permanent or temporary)

FOR NEW EMPLOYEE:
- Employee full name
- Start date
- Department and manager
- Job title and role
- Equipment needs (standard or specific)
- Software requirements
- System access needed
- Desk location

Step 5: Verify Request Details
- Review all fields for accuracy
- Add any additional notes
- Attach supporting documents if needed
- Confirm cost center/budget code if applicable

Step 6: Submit
- Click "Submit" or "Checkout"
- Note your Work Order number (WO000XXXXX)
- Review confirmation email',
'KnowledgeArticle', 'KB0021', 'ENT000000200021', 'ARTICLE_CONTENT', 1,
'{"assigned_group": "Service Desk", "title": "How to Submit a Work Order", "category": "BMC Remedy/WorkOrder/Create", "status": "Published"}'),

('KB0021-CONTENT-2', generate_random_embedding(),
'AFTER SUBMISSION:

Approval Process:
- Some requests require manager approval
- Approver receives email notification
- You can track approval status in the portal
- Reminder sent after 2 business days
- Escalate to your manager if urgent

Tracking Your Request:
1. Log in to portal
2. Click "My Requests" or "My Work Orders"
3. Find your request in the list
4. View current status and work log
5. Add notes if you have updates

Expected Fulfillment Times:
- Standard hardware: 3-5 business days
- Software installation: 1-2 business days
- Access requests: 1-2 business days
- New employee setup: Ready by start date (submit 5+ days early)
- Equipment moves: 3-5 business days

TIPS FOR FASTER PROCESSING:
- Submit requests during business hours
- Provide complete information upfront
- Respond quickly to approval requests
- Be specific about requirements
- Include business justification

BULK REQUESTS:
For multiple items or users:
- Use the bulk request form in Service Catalog
- Attach spreadsheet for large requests
- Contact Service Desk for guidance
- Plan extra time for processing

CANCELLING A WORK ORDER:
If you no longer need the request:
1. Open the work order in portal
2. Click "Cancel Request"
3. Provide reason for cancellation
4. Confirm cancellation
Note: Cannot cancel if work has already begun',
'KnowledgeArticle', 'KB0021', 'ENT000000200021', 'ARTICLE_CONTENT', 2,
'{"assigned_group": "Service Desk", "title": "How to Submit a Work Order", "category": "BMC Remedy/WorkOrder/Create", "status": "Published"}');

-- KB0022: Work Order Approval Process
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
('KB0022-CONTENT-0', generate_random_embedding(),
'Knowledge Article KB0022: Work Order Approval Process

Understanding when approvals are required and how to manage them.

WHEN IS APPROVAL REQUIRED?

Financial Thresholds:
- Software > $500: Manager approval required
- Hardware > $1,000: Manager approval required
- Any request > $5,000: Director approval required
- Any request > $25,000: VP approval required

Access Requests:
- Production system access: Manager + Security approval
- Financial system access: Manager + Finance approval
- Admin/elevated access: Manager + Security + System Owner
- Contractor access: Sponsor manager + Security

Special Categories:
- Non-standard equipment: Manager + IT approval
- Executive equipment: Manager approval
- Development tools: Manager approval
- Cloud services: Manager + Security + Architecture

WHO APPROVES?

Level 1 - Direct Manager:
- Reviews business justification
- Confirms budget availability
- Approves standard requests

Level 2 - Department Head/Director:
- Reviews high-cost items
- Approves non-standard requests
- Budget authority confirmation

Level 3 - Security Team:
- Reviews access requests
- Validates least-privilege principle
- Confirms compliance requirements

Level 4 - IT Leadership:
- Architecture compliance
- Standards exceptions
- Strategic alignment',
'KnowledgeArticle', 'KB0022', 'ENT000000200022', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Service Desk", "title": "Work Order Approval Process", "category": "BMC Remedy/WorkOrder/Approval", "status": "Published", "keywords": "approval, work order approval, manager approval, authorization"}'),

('KB0022-CONTENT-1', generate_random_embedding(),
'APPROVAL WORKFLOW:

Step 1: Request Submitted
└─ System identifies required approvers
└─ Notification sent to first approver
└─ Status: PENDING APPROVAL

Step 2: Approval In Progress
└─ Approver reviews request details
└─ May add comments or questions
└─ Options: Approve, Reject, or Request More Info
└─ Sequential or parallel depending on request type

Step 3: All Approvals Received
└─ Request moves to fulfillment queue
└─ Requestor notified of approval
└─ Status: APPROVED → ASSIGNED

If Rejected:
└─ Requestor notified with reason
└─ Can modify and resubmit
└─ Or accept rejection and close
└─ Status: REJECTED

APPROVING A REQUEST (For Managers):

Via Email:
1. Click approval link in notification email
2. Review request details
3. Click "Approve" or "Reject"
4. Add comments if rejecting
5. Confirmation displayed

Via Portal:
1. Log in to https://remedy.acme.com
2. Click "My Approvals" on home page
3. Open the pending approval
4. Review all details and justification
5. Click "Approve" or "Reject"
6. Add comments (required for rejection)
7. Click Submit

APPROVAL TIPS FOR MANAGERS:
- Set up email rules to highlight approval requests
- Check approvals daily to avoid delays
- Ask questions via work notes before rejecting
- Delegate approval authority when on PTO
- Review past approvals in "My Approval History"',
'KnowledgeArticle', 'KB0022', 'ENT000000200022', 'ARTICLE_CONTENT', 1,
'{"assigned_group": "Service Desk", "title": "Work Order Approval Process", "category": "BMC Remedy/WorkOrder/Approval", "status": "Published"}');

-- =============================================================================
-- CHANGE REQUEST MANAGEMENT GUIDES
-- =============================================================================
\echo 'Inserting Change Request Management guides...'

-- KB0030: Understanding Change Management
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
('KB0030-CONTENT-0', generate_random_embedding(),
'Knowledge Article KB0030: Understanding IT Change Management

Learn about the Change Management process and when to submit a Change Request.

WHAT IS CHANGE MANAGEMENT?
Change Management is the ITIL process for controlling modifications to IT infrastructure, applications, and services. It ensures changes are:
- Properly assessed for risk
- Approved by appropriate stakeholders
- Scheduled to minimize disruption
- Documented for audit and compliance
- Reversible if problems occur

WHEN IS A CHANGE REQUEST REQUIRED?

ALWAYS Required:
- Production server modifications
- Network infrastructure changes
- Database schema changes
- Application deployments to production
- Security configuration changes
- Active Directory structure changes
- Firewall rule modifications
- DNS changes
- Load balancer configurations

USUALLY Required:
- Development/Test environment changes affecting shared resources
- Software version upgrades
- Hardware replacements
- Backup configuration changes
- Monitoring system changes

NOT Required (Standard Operations):
- Password resets
- User account creation (covered by Work Order)
- Workstation software installs (covered by Work Order)
- Scheduled maintenance (pre-approved)
- Standard patching (pre-approved window)',
'KnowledgeArticle', 'KB0030', 'ENT000000200030', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Change Management", "title": "Understanding Change Management", "category": "BMC Remedy/Change/Overview", "status": "Published", "keywords": "change management, change request, RFC, infrastructure change"}'),

('KB0030-CONTENT-1', generate_random_embedding(),
'TYPES OF CHANGES:

STANDARD CHANGE
- Pre-approved, low-risk, routine
- Follows documented procedure
- No CAB approval needed
- Examples:
  └─ Standard patching
  └─ Scheduled restarts
  └─ Pre-approved software deployments
  └─ Routine certificate renewals

NORMAL CHANGE
- Requires full assessment
- CAB review and approval
- Scheduled maintenance window
- Most common type
- Examples:
  └─ Application upgrades
  └─ Server migrations
  └─ Network modifications
  └─ New system implementations

EMERGENCY CHANGE
- Urgent fix for critical issue
- Expedited approval process
- Post-implementation review required
- Examples:
  └─ Security vulnerability patch
  └─ Critical bug fix
  └─ Service restoration
  └─ Emergency failover

CHANGE RISK LEVELS:

LOW RISK:
- Single system, no dependencies
- Easy rollback
- Non-business hours
- Precedent exists

MEDIUM RISK:
- Multiple related systems
- Standard rollback plan
- May affect business operations
- Tested procedure

HIGH RISK:
- Critical systems
- Complex rollback
- Business hours impact possible
- First-time implementation

EMERGENCY:
- Production outage in progress
- Security breach response
- Critical vulnerability exploitation',
'KnowledgeArticle', 'KB0030', 'ENT000000200030', 'ARTICLE_CONTENT', 1,
'{"assigned_group": "Change Management", "title": "Understanding Change Management", "category": "BMC Remedy/Change/Overview", "status": "Published"}');

-- KB0031: How to Submit a Change Request
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
('KB0031-CONTENT-0', generate_random_embedding(),
'Knowledge Article KB0031: How to Submit a Change Request

Step-by-step guide for submitting infrastructure and application changes.

BEFORE SUBMITTING:
1. Confirm a change request is actually needed
2. Complete testing in non-production environment
3. Document implementation and rollback procedures
4. Identify all affected systems and stakeholders
5. Plan your preferred maintenance window
6. Gather technical details and diagrams if applicable

STEP-BY-STEP: CREATING A CHANGE REQUEST

Step 1: Access Change Management Module
- Log in to https://remedy.acme.com
- Click "Applications" → "Change Management"
- Or click "Create New" → "Change Request"

Step 2: Select Change Type
- Standard: For pre-approved routine changes
- Normal: For most infrastructure changes
- Emergency: Only for urgent production issues

Step 3: Complete Required Information

SUMMARY (required):
Write a clear, concise description:
GOOD: "Upgrade Oracle database from 19c to 21c on PROD-DB-01"
GOOD: "Deploy CRM v4.2 hotfix for payment processing bug"
BAD: "Database upgrade" (too vague)
BAD: "Fix stuff" (not professional)

DESCRIPTION (required):
Include comprehensive details:
- What exactly will be changed
- Why this change is needed
- Expected outcome
- Any dependencies or prerequisites

REASON FOR CHANGE:
- Bug fix
- Enhancement
- Security update
- Performance improvement
- Compliance requirement
- New feature
- Maintenance',
'KnowledgeArticle', 'KB0031', 'ENT000000200031', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Change Management", "title": "How to Submit a Change Request", "category": "BMC Remedy/Change/Create", "status": "Published", "keywords": "submit change, RFC, change request, create change"}'),

('KB0031-CONTENT-1', generate_random_embedding(),
'Step 4: Document Implementation Plan

The implementation plan should include:

PRE-IMPLEMENTATION:
- [ ] Backup current configuration
- [ ] Verify rollback procedure tested
- [ ] Notify stakeholders of maintenance
- [ ] Confirm support team availability
- [ ] Verify monitoring is in place

IMPLEMENTATION STEPS:
1. [Step 1 - specific action and expected result]
2. [Step 2 - specific action and expected result]
3. [Step 3 - specific action and expected result]
... continue with detailed steps

VERIFICATION:
- [ ] System comes back online
- [ ] Application functionality verified
- [ ] Performance metrics normal
- [ ] No error alerts
- [ ] User acceptance confirmed

Step 5: Document Rollback Plan

The rollback plan should include:

ROLLBACK DECISION TRIGGERS:
- Define specific criteria for invoking rollback
- Example: "If application does not respond within 15 minutes"
- Example: "If more than 5% of transactions fail"

ROLLBACK STEPS:
1. [Step 1 - specific rollback action]
2. [Step 2 - specific rollback action]
3. [Step 3 - specific rollback action]

ROLLBACK TIME ESTIMATE:
- How long to fully restore service
- Must be within maintenance window

Step 6: Specify Scheduling

REQUESTED START: When you want to implement
REQUESTED END: When you will be finished

Consider:
- Business impact during the window
- Global team time zones
- Dependencies on other changes
- Blackout periods (month-end, holidays)',
'KnowledgeArticle', 'KB0031', 'ENT000000200031', 'ARTICLE_CONTENT', 1,
'{"assigned_group": "Change Management", "title": "How to Submit a Change Request", "category": "BMC Remedy/Change/Create", "status": "Published"}'),

('KB0031-CONTENT-2', generate_random_embedding(),
'Step 7: Complete Risk Assessment

Answer these questions honestly:
- Is there a tested rollback plan? (Yes/No)
- Has this change been done before? (Yes/No)
- Is the change being done during business hours? (Yes/No)
- Are there dependent systems? (Yes/No)
- Is this a complex change? (Yes/No)
- Is there adequate testing? (Yes/No)

Risk is automatically calculated based on answers.

Step 8: Identify Stakeholders

CHANGE IMPLEMENTER: Person performing the change
CHANGE COORDINATOR: Person managing the process (may be same)
AFFECTED GROUPS: Teams whose systems are impacted
NOTIFICATION LIST: People to notify before/during/after

Step 9: Attach Supporting Documents
- Architecture diagrams
- Test results
- Approval emails
- Vendor documentation
- Previous change records for reference

Step 10: Submit for Review
- Click "Submit" or "Request Approval"
- Change enters review queue
- You receive confirmation email
- Track via change number (CHG000XXXXX)

AFTER SUBMISSION:

CAB Review (for Normal/High-Risk changes):
- Change reviewed in weekly CAB meeting
- Or emergency CAB for urgent changes
- You may be asked to present

Questions from Reviewers:
- Monitor the change for questions
- Respond promptly to comments
- Update documentation as needed

Approval Received:
- Schedule confirmed
- Proceed with implementation
- Follow the approved plan exactly',
'KnowledgeArticle', 'KB0031', 'ENT000000200031', 'ARTICLE_CONTENT', 2,
'{"assigned_group": "Change Management", "title": "How to Submit a Change Request", "category": "BMC Remedy/Change/Create", "status": "Published"}');

-- KB0032: Change Advisory Board (CAB)
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
('KB0032-CONTENT-0', generate_random_embedding(),
'Knowledge Article KB0032: Change Advisory Board (CAB) Process

Understanding the CAB review and approval process for IT changes.

WHAT IS CAB?
The Change Advisory Board (CAB) is a group of stakeholders who review and authorize IT changes. The CAB ensures changes are properly assessed, scheduled, and coordinated.

CAB MEMBERSHIP:
- Change Manager (Chair)
- IT Operations representatives
- Application owners
- Security representative
- Network representative
- Database representative
- Service Desk representative
- Business representatives (as needed)

CAB MEETING SCHEDULE:
- Regular CAB: Tuesdays at 10:00 AM (for standard normal changes)
- High-Risk CAB: Thursdays at 2:00 PM (for high-risk changes)
- Emergency CAB: On-demand (for emergency changes, via conference call)

CHANGE SUBMISSION DEADLINES:
- Regular CAB: Submit by Monday 5:00 PM for Tuesday review
- High-Risk CAB: Submit by Wednesday 5:00 PM for Thursday review
- Emergency: Contact Change Manager immediately

WHAT HAPPENS IN CAB:

1. AGENDA REVIEW
   - List of changes to be reviewed
   - Priority ordering

2. FOR EACH CHANGE:
   - Change owner presents summary
   - Risk assessment reviewed
   - Implementation plan reviewed
   - Rollback plan verified
   - Scheduling conflicts checked
   - Questions and concerns addressed

3. DECISION:
   - APPROVED: Proceed as planned
   - APPROVED WITH CONDITIONS: Proceed after meeting conditions
   - MORE INFO NEEDED: Return with requested information
   - REJECTED: Not approved (with documented reasons)
   - DEFERRED: Postponed to future date',
'KnowledgeArticle', 'KB0032', 'ENT000000200032', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Change Management", "title": "Change Advisory Board Process", "category": "BMC Remedy/Change/CAB", "status": "Published", "keywords": "CAB, Change Advisory Board, change approval, change review"}'),

('KB0032-CONTENT-1', generate_random_embedding(),
'PREPARING FOR CAB:

Before the Meeting:
- Ensure change request is complete
- All sections filled out
- Risk assessment accurate
- Implementation plan detailed
- Rollback plan tested
- Attachments uploaded

Presenting Your Change:
1. Brief summary (30 seconds)
2. Business justification
3. Technical approach
4. Risk mitigation
5. Schedule and duration
6. Rollback capability

Common CAB Questions:
- "What is the business impact if this fails?"
- "Has this been tested in a non-production environment?"
- "What is your rollback plan if issues occur?"
- "Who will be on-call during implementation?"
- "Are there any dependencies on other systems?"
- "What monitoring will be in place?"

Tips for CAB Success:
- Be prepared and concise
- Know your change details thoroughly
- Have answers ready for common questions
- Be honest about risks
- Dont minimize potential issues
- Accept feedback constructively
- Follow up on action items promptly

AFTER CAB APPROVAL:

Approved Changes:
1. Verify final schedule in the system
2. Send stakeholder notifications
3. Confirm team availability
4. Proceed with implementation
5. Update change status as you progress
6. Complete post-implementation review

Rejected/Deferred Changes:
1. Review CAB comments
2. Address identified issues
3. Resubmit when ready
4. Request expedited review if urgent',
'KnowledgeArticle', 'KB0032', 'ENT000000200032', 'ARTICLE_CONTENT', 1,
'{"assigned_group": "Change Management", "title": "Change Advisory Board Process", "category": "BMC Remedy/Change/CAB", "status": "Published"}');

-- KB0033: Emergency Change Process
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
('KB0033-CONTENT-0', generate_random_embedding(),
'Knowledge Article KB0033: Emergency Change Process

When and how to use the Emergency Change process for urgent situations.

WHAT QUALIFIES AS EMERGENCY?

Emergency changes are ONLY for:
- Production service outage requiring immediate fix
- Critical security vulnerability being actively exploited
- Regulatory compliance breach requiring immediate action
- Safety-related system failure
- Data loss prevention

NOT Emergency (use Normal Change):
- "Urgent" business requests
- Missed deadlines for normal changes
- Changes that could have been planned
- Non-critical bugs, even if annoying
- Performance issues that are not causing outage

EMERGENCY CHANGE PROCESS:

Step 1: Assess the Situation
- Confirm this is a true emergency
- Document the business impact
- Identify the minimum necessary change
- Consider temporary workarounds

Step 2: Contact Change Manager
- Phone: 555-0100 (24/7 on-call)
- Email: emergency-change@acme.com
- Page: Use PagerDuty for after-hours

Step 3: Obtain Verbal Approval
- Change Manager assesses request
- Contacts Emergency CAB members
- Verbal approval given for immediate action
- You may proceed once verbally approved

Step 4: Submit Emergency Change Request
- Create change request marked "Emergency"
- Document what was approved verbally
- Include current incident number
- Attach any supporting evidence

Step 5: Implement the Change
- Follow minimal change needed
- Document each step taken
- Monitor results
- Report status to Change Manager

Step 6: Post-Implementation
- Complete the change record
- Document actual steps taken
- Record any deviations from plan
- Update related incident',
'KnowledgeArticle', 'KB0033', 'ENT000000200033', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Change Management", "title": "Emergency Change Process", "category": "BMC Remedy/Change/Emergency", "status": "Published", "keywords": "emergency change, urgent change, critical fix, emergency CAB"}'),

('KB0033-CONTENT-1', generate_random_embedding(),
'EMERGENCY CAB:

When Convened:
- 24/7 availability via on-call rotation
- Conference bridge always available
- Decision within 30 minutes maximum
- Minimum: Change Manager + affected area owner

Emergency CAB Dial-in:
- Phone: 1-800-555-ECAB (3222)
- Conference ID: 999-EMERGENCY
- Available 24/7/365

Emergency Approval Criteria:
- Is this truly an emergency?
- What is the immediate business impact?
- What is the minimum change needed?
- Is there a rollback plan?
- Who will implement?
- Who will verify?

DOCUMENTATION REQUIREMENTS:

During Emergency:
- Keep detailed notes of all actions
- Screenshot before and after states
- Note times of all activities
- Record who was involved

Within 24 Hours:
- Complete formal change record
- Attach all documentation
- Link to related incident
- Document lessons learned

Within 1 Week:
- Post-Implementation Review (PIR)
- Root cause analysis
- Prevention recommendations
- Process improvement suggestions

IMPORTANT RULES:

DO:
- Contact Change Manager first
- Document everything
- Follow minimum change principle
- Report status regularly
- Complete documentation promptly

DO NOT:
- Make changes without approval
- Expand scope beyond emergency fix
- Skip documentation
- Forget post-implementation review
- Abuse emergency process for non-emergencies

Abuse of Emergency Process:
- Emergency changes are audited
- Pattern of abuse is escalated to management
- May result in restricted change privileges
- Impacts team metrics and reporting',
'KnowledgeArticle', 'KB0033', 'ENT000000200033', 'ARTICLE_CONTENT', 1,
'{"assigned_group": "Change Management", "title": "Emergency Change Process", "category": "BMC Remedy/Change/Emergency", "status": "Published"}');

-- =============================================================================
-- SELF-SERVICE PORTAL GUIDES
-- =============================================================================
\echo 'Inserting Self-Service Portal guides...'

-- KB0040: Self-Service Portal Overview
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
('KB0040-CONTENT-0', generate_random_embedding(),
'Knowledge Article KB0040: BMC Remedy Self-Service Portal Guide

Complete guide to using the IT Self-Service Portal for all your IT needs.

ACCESSING THE PORTAL:

URL: https://remedy.acme.com
Mobile: Download "BMC MyIT" app from App Store or Play Store
Supported Browsers: Chrome (recommended), Firefox, Edge, Safari

LOGIN:
- Use your network credentials (same as Windows login)
- Complete MFA verification if prompted
- Select "Remember me" for faster access (on trusted devices only)

PORTAL HOME PAGE:

Top Navigation Bar:
[Search] [Create New ▼] [My Items ▼] [Notifications] [Profile]

Main Sections:
┌─────────────────────────────────────────────────────────┐
│ ANNOUNCEMENTS                                           │
│ Important IT notifications and planned maintenance      │
├─────────────────────────────────────────────────────────┤
│ QUICK ACTIONS                                          │
│ [Report Issue] [Request Something] [Search KB]         │
├─────────────────────────────────────────────────────────┤
│ MY OPEN ITEMS                     │ SERVICE HEALTH     │
│ • INC000123 - Outlook issue       │ ✓ Email: Normal    │
│ • WO000456 - Software request     │ ✓ VPN: Normal      │
│                                   │ ⚠ SAP: Degraded   │
├─────────────────────────────────────────────────────────┤
│ POPULAR SERVICES              │ KNOWLEDGE ARTICLES     │
│ • Password Reset              │ • VPN Setup Guide      │
│ • Software Request            │ • Outlook Config       │
│ • New Hire Setup              │ • Printer Setup        │
└─────────────────────────────────────────────────────────┘',
'KnowledgeArticle', 'KB0040', 'ENT000000200040', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Service Desk", "title": "Self-Service Portal Guide", "category": "BMC Remedy/Portal/Overview", "status": "Published", "keywords": "self-service, portal, BMC Remedy portal, IT portal"}'),

('KB0040-CONTENT-1', generate_random_embedding(),
'KEY PORTAL FEATURES:

1. GLOBAL SEARCH
- Search bar at top of every page
- Searches across all content types
- Type ticket number for direct access
- Use keywords for knowledge articles
- Filter results by type

2. CREATE NEW MENU
Click "Create New" for quick access:
├── Incident: Report an IT issue
├── Work Order: Request a service
├── Change Request: Request infrastructure change
└── Knowledge Feedback: Suggest article improvements

3. MY ITEMS
Track all your tickets and requests:
├── My Incidents: Issues you reported
├── My Work Orders: Services you requested
├── My Changes: Changes you submitted
├── My Approvals: Items awaiting your approval
└── Watched Items: Items you are following

4. SERVICE CATALOG
Browse available IT services:
├── Categories: Hardware, Software, Access, etc.
├── Popular Services: Frequently requested items
├── Search: Find specific services
└── Cart: Queue multiple requests

5. KNOWLEDGE BASE
Self-help resources:
├── Browse by category
├── Search by keyword
├── View related articles
├── Rate article helpfulness
└── Request new articles

6. CHAT SUPPORT
Live chat with Service Desk:
├── Click chat icon (bottom right)
├── Available during business hours
├── Creates ticket automatically
└── Chat transcript saved',
'KnowledgeArticle', 'KB0040', 'ENT000000200040', 'ARTICLE_CONTENT', 1,
'{"assigned_group": "Service Desk", "title": "Self-Service Portal Guide", "category": "BMC Remedy/Portal/Overview", "status": "Published"}'),

('KB0040-CONTENT-2', generate_random_embedding(),
'PROFILE SETTINGS:

Access via profile icon (top right) → Settings

Contact Information:
- Verify phone number
- Add alternate contact
- Set preferred contact method
- Update location information

Notification Preferences:
- Email notifications: On/Off for each event type
- SMS alerts: For critical updates (optional)
- In-app notifications: Configure what appears

Delegate Settings (for managers):
- Set approval delegate when on PTO
- Delegate can approve requests on your behalf
- Delegate period with start/end dates

Display Preferences:
- Language selection
- Time zone
- Date format
- Items per page

MOBILE APP FEATURES:

Download "BMC MyIT" from your app store

Mobile-Specific Features:
- Push notifications for updates
- Camera integration for attachments
- Barcode scanning for asset lookup
- Location services for desk assignment
- Touch ID / Face ID login

ACCESSIBILITY:

The portal supports:
- Screen readers (JAWS, NVDA)
- Keyboard navigation
- High contrast mode
- Text resizing
- Alternative text for images

Enable accessibility mode in Profile → Settings → Accessibility

SUPPORT:

If you need help with the portal:
- Click the "?" icon for context help
- Search knowledge base for guides
- Chat with Service Desk
- Call: 1-800-555-HELP
- Email: servicedesk@acme.com',
'KnowledgeArticle', 'KB0040', 'ENT000000200040', 'ARTICLE_CONTENT', 2,
'{"assigned_group": "Service Desk", "title": "Self-Service Portal Guide", "category": "BMC Remedy/Portal/Overview", "status": "Published"}');

-- KB0041: Searching and Tracking Tickets
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
('KB0041-CONTENT-0', generate_random_embedding(),
'Knowledge Article KB0041: How to Search and Track Your Tickets

Learn how to find your tickets and monitor their progress.

FINDING YOUR TICKETS:

Method 1: My Items Menu
1. Click "My Items" in top navigation
2. Select the appropriate category:
   - My Incidents
   - My Work Orders
   - My Changes
3. View list sorted by last updated
4. Click any ticket to open details

Method 2: Global Search
1. Click the search bar (or press /)
2. Enter ticket number (e.g., INC000123)
3. Press Enter or click search
4. Click result to open ticket

Method 3: Home Page
- Your open items appear on home page
- Click "View All" to see complete list
- Click any item to open details

SEARCH TIPS:

Search by Ticket Number:
- Enter exact number: INC000123
- Partial number works: INC123

Search by Keywords:
- Enter words from summary: "outlook crash"
- Use quotes for phrases: "password reset"
- Multiple keywords: outlook email sync

Search Filters:
After searching, filter by:
├── Type: Incident, Work Order, Change
├── Status: Open, Resolved, Closed
├── Date: Created, Updated, Resolved
├── Priority: Critical, High, Medium, Low
└── Group: Assigned support team',
'KnowledgeArticle', 'KB0041', 'ENT000000200041', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Service Desk", "title": "Searching and Tracking Tickets", "category": "BMC Remedy/Portal/Search", "status": "Published", "keywords": "search ticket, find incident, track request, ticket status"}'),

('KB0041-CONTENT-1', generate_random_embedding(),
'UNDERSTANDING TICKET DETAILS:

When you open a ticket, you see:

HEADER SECTION:
- Ticket number and status (color coded)
- Summary/title
- Priority indicator
- Created and updated dates

DETAILS TAB:
- Full description
- Categorization
- Impact and urgency
- Assigned group and individual
- Contact information

WORK INFO TAB:
- Chronological activity log
- Notes from technicians
- Your responses
- Status change history
- Attachment list

RELATIONSHIPS TAB:
- Related incidents
- Parent problems
- Associated changes
- Linked knowledge articles

TRACKING STATUS CHANGES:

Via Email:
- Automatic email on every status change
- Email includes new status and recent notes
- Links directly to ticket in portal

Via Portal:
- Open ticket and view "Work Info" tab
- Status history shows all transitions
- Timestamps for each change

Via Mobile App:
- Push notifications enabled by default
- Open app to view current status
- Swipe for quick status overview

NOTIFICATION SETTINGS:

Customize what notifications you receive:
1. Go to Profile → Notification Preferences
2. Toggle events on/off:
   - Status changes
   - Assignment changes
   - New work notes
   - Resolution notification
   - Closure notification
3. Choose delivery method:
   - Email
   - SMS (for critical only)
   - Push notification (mobile)',
'KnowledgeArticle', 'KB0041', 'ENT000000200041', 'ARTICLE_CONTENT', 1,
'{"assigned_group": "Service Desk", "title": "Searching and Tracking Tickets", "category": "BMC Remedy/Portal/Search", "status": "Published"}');

-- =============================================================================
-- BEST PRACTICES AND TIPS
-- =============================================================================
\echo 'Inserting Best Practices guides...'

-- KB0050: Best Practices for Submitting Tickets
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
('KB0050-CONTENT-0', generate_random_embedding(),
'Knowledge Article KB0050: Best Practices for Submitting IT Tickets

Follow these guidelines to get faster, better support.

WRITING EFFECTIVE SUMMARIES:

The summary is the first thing IT sees. Make it count!

DO:
✓ Be specific: "Outlook crashes when opening PDF attachments"
✓ Include application name: "SAP showing error 500 on login"
✓ State the problem: "Cannot print to HP LaserJet Floor 3"
✓ Mention scope: "WiFi dropping for all users in Room 301"

DONT:
✗ Too vague: "Computer not working"
✗ No context: "Help!"
✗ Too long: Keep under 100 characters
✗ All caps: "MY EMAIL IS BROKEN!!!"

EXAMPLES OF GOOD SUMMARIES:
- "Excel freezes when saving files larger than 5MB"
- "VPN Error 691 - unable to connect from home"
- "Password expired - unable to login to workstation"
- "Shared drive S: showing access denied for accounting team"

PROVIDING USEFUL DESCRIPTIONS:

Answer these questions in your description:
1. What were you trying to do?
2. What happened instead?
3. When did it start happening?
4. Is it consistent or intermittent?
5. What error message appears (exact text)?
6. Have you tried anything to fix it?
7. Is anyone else affected?
8. Can you work around it?

DESCRIPTION TEMPLATE:
---
What I was doing: [action]
What happened: [result/error]
When it started: [date/time]
Error message: "[exact message]"
Frequency: [always/sometimes/once]
Others affected: [yes/no/unknown]
Workaround: [available/not available]
Already tried: [steps taken]
---',
'KnowledgeArticle', 'KB0050', 'ENT000000200050', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Service Desk", "title": "Best Practices for Submitting Tickets", "category": "BMC Remedy/Tips/BestPractices", "status": "Published", "keywords": "best practices, tips, submit ticket, write summary, good description"}'),

('KB0050-CONTENT-1', generate_random_embedding(),
'SELECTING THE RIGHT CATEGORY:

Why it matters:
- Tickets route to the right team faster
- Correct priority calculation
- Better reporting and trending
- Faster resolution

Category selection tips:
- Start broad, then narrow down
- Use the category that matches the SYMPTOM
- Not sure? Service Desk will recategorize
- Search existing tickets for similar issues

Common category mistakes:
- Wrong: "Network" for internet browser issues (use Software > Browser)
- Wrong: "Hardware" for slow computer (could be Software)
- Wrong: "Software" for new install request (use Work Order)

SETTING APPROPRIATE PRIORITY:

Be honest about impact and urgency:
- Impact: How many people are affected RIGHT NOW?
- Urgency: How business-critical is this REALLY?

Signs youre over-prioritizing:
- Everything is "Critical"
- Using urgency to jump the queue
- Impact on "future" work, not current

Signs youre under-prioritizing:
- Multiple users affected but marked as "Individual"
- Production system down but marked "Medium"
- Compliance violation not marked "High"

RESPONDING TO TECHNICIANS:

When IT reaches out:
- Respond within 4 business hours if possible
- Answer all questions asked
- Provide additional screenshots if requested
- Indicate your availability for calls/remote sessions

If going on PTO:
- Add a note with your return date
- Provide alternate contact if possible
- Update the ticket when you return

CLOSING THE LOOP:

When resolved:
- Confirm the fix works completely
- Report any remaining issues
- Say thank you (it helps morale!)

If issue returns:
- Reopen the ticket if still within 5 days
- Create new ticket referencing old one
- Describe what changed since resolution',
'KnowledgeArticle', 'KB0050', 'ENT000000200050', 'ARTICLE_CONTENT', 1,
'{"assigned_group": "Service Desk", "title": "Best Practices for Submitting Tickets", "category": "BMC Remedy/Tips/BestPractices", "status": "Published"}');

-- =============================================================================
-- Update sync_state
-- =============================================================================
\echo 'Updating sync_state...'

UPDATE sync_state SET
    last_sync_timestamp = EXTRACT(EPOCH FROM CURRENT_TIMESTAMP)::BIGINT,
    last_sync_at = CURRENT_TIMESTAMP,
    records_synced = (SELECT COUNT(DISTINCT source_id) FROM embedding_store WHERE source_type = 'KnowledgeArticle'),
    status = 'completed'
WHERE source_type = 'KnowledgeArticle';

-- =============================================================================
-- Summary
-- =============================================================================
\echo ''
\echo '=============================================='
\echo 'BMC Remedy guides seed completed!'
\echo '=============================================='

SELECT
    source_type,
    COUNT(DISTINCT source_id) as articles,
    COUNT(*) as total_chunks
FROM embedding_store
WHERE source_type = 'KnowledgeArticle'
GROUP BY source_type;

SELECT
    source_id as article_id,
    (metadata->>'title')::varchar(50) as title
FROM embedding_store
WHERE source_type = 'KnowledgeArticle'
  AND chunk_type = 'ARTICLE_CONTENT'
  AND sequence_number = 0
ORDER BY source_id;

\echo ''
\echo 'Knowledge articles added successfully!'
