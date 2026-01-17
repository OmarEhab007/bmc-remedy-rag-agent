-- BMC Remedy RAG Agent - Dummy Data Seed Script
-- Run this script manually to populate test data
-- Prerequisites: V1__create_embedding_tables.sql must be applied first
--
-- Usage: psql -d bmc_rag -f seed_dummy_data.sql
--        or via pgAdmin/DBeaver

-- Clean existing data before seeding (optional - uncomment if needed)
-- TRUNCATE embedding_store, chat_memory, ingestion_job RESTART IDENTITY CASCADE;

\echo 'Starting dummy data seed...'

-- =============================================================================
-- Helper function for random embeddings (if not exists)
-- =============================================================================
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

\echo 'Created embedding generator function'

-- =============================================================================
-- INCIDENTS - Common IT Service Desk Issues
-- =============================================================================
\echo 'Inserting Incident data...'

-- Clear existing incidents
DELETE FROM embedding_store WHERE source_type = 'Incident';

-- INC000001: Password Reset
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
('INC000001-SUMMARY-0', generate_random_embedding(), 'Incident INC000001: User unable to login - password expired. Category: Infrastructure/Authentication/Password. Priority: Medium. User John Smith from Finance department reported they cannot access their workstation after returning from vacation. The system shows "Your password has expired" message.', 'Incident', 'INC000001', 'ENT000000000001', 'SUMMARY', 0, '{"assigned_group": "Service Desk", "title": "User unable to login - password expired", "category": "Infrastructure/Authentication/Password", "status": "Closed", "priority": "Medium", "customer_company": "ACME Corp"}'),
('INC000001-RESOLUTION-0', generate_random_embedding(), 'Resolution for INC000001: Password reset completed successfully. Steps taken: 1) Verified user identity through security questions 2) Reset password in Active Directory using ADUC 3) Forced password change at next login 4) Confirmed user can access workstation and all applications. User education provided on password expiration policy (90 days). Ticket resolved and closed.', 'Incident', 'INC000001', 'ENT000000000001', 'RESOLUTION', 0, '{"assigned_group": "Service Desk", "title": "User unable to login - password expired", "status": "Closed"}');

-- INC000002: VPN Connection Issue
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
('INC000002-SUMMARY-0', generate_random_embedding(), 'Incident INC000002: VPN connection failing with error 691. Category: Network/VPN/Connectivity. Priority: High. Multiple remote users reporting inability to connect to corporate VPN. Error message: "The remote connection was denied because the user name and password combination you provided is not recognized". Affecting approximately 50 remote workers.', 'Incident', 'INC000002', 'ENT000000000002', 'SUMMARY', 0, '{"assigned_group": "Network Operations", "title": "VPN connection failing with error 691", "category": "Network/VPN/Connectivity", "status": "Closed", "priority": "High", "impact": "Multiple Users"}'),
('INC000002-DESCRIPTION-0', generate_random_embedding(), 'Users experiencing VPN authentication failures started around 8:00 AM EST. The Cisco AnyConnect client shows error 691 immediately after entering credentials. Issue affects users connecting from home offices and mobile devices. Corporate office users on internal network are not affected. VPN server logs show RADIUS authentication timeouts.', 'Incident', 'INC000002', 'ENT000000000002', 'DESCRIPTION', 0, '{"assigned_group": "Network Operations", "title": "VPN connection failing with error 691", "status": "Closed"}'),
('INC000002-RESOLUTION-0', generate_random_embedding(), 'Resolution for INC000002: RADIUS server connectivity restored. Root cause: Network switch firmware update last night caused VLAN tagging issue affecting RADIUS server communication. Fix applied: 1) Rolled back switch firmware to previous stable version 2) Verified RADIUS server connectivity from all VPN concentrators 3) Tested VPN connections from multiple locations 4) Monitoring enabled for 24 hours. All affected users can now connect. Post-incident review scheduled.', 'Incident', 'INC000002', 'ENT000000000002', 'RESOLUTION', 0, '{"assigned_group": "Network Operations", "title": "VPN connection failing with error 691", "status": "Closed"}'),
('INC000002-WORKLOG-0', generate_random_embedding(), 'Work Log Entry [2024-01-16 08:15 - Network Team]: Initial triage completed. Confirmed issue is widespread. VPN concentrator logs show authentication requests being sent to RADIUS but timing out. Escalating to Network Operations for investigation.', 'Incident', 'INC000002', 'ENT000000000002', 'WORK_LOG', 1, '{"assigned_group": "Network Operations", "submitter": "netops@acme.com"}');

-- INC000003: Email Sync Issue
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
('INC000003-SUMMARY-0', generate_random_embedding(), 'Incident INC000003: Outlook not syncing emails - stuck on "Updating inbox". Category: Software/Email/Outlook. Priority: Medium. User reports Microsoft Outlook 365 has been stuck on "Updating inbox" for 3 hours. Send/receive shows "0 of 0" but webmail works fine.', 'Incident', 'INC000003', 'ENT000000000003', 'SUMMARY', 0, '{"assigned_group": "Desktop Support", "title": "Outlook not syncing emails", "category": "Software/Email/Outlook", "status": "Closed", "priority": "Medium"}'),
('INC000003-RESOLUTION-0', generate_random_embedding(), 'Resolution for INC000003: Outlook profile corrupted - recreated profile. Troubleshooting steps: 1) Verified network connectivity 2) Checked Outlook in Safe Mode 3) Ran Outlook diagnostic tool - found OST file corruption 4) Created new Outlook profile 5) Removed old OST file and allowed resync 6) Email now syncing correctly.', 'Incident', 'INC000003', 'ENT000000000003', 'RESOLUTION', 0, '{"assigned_group": "Desktop Support", "title": "Outlook not syncing emails", "status": "Closed"}');

-- INC000004: Printer Not Working
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
('INC000004-SUMMARY-0', generate_random_embedding(), 'Incident INC000004: Shared printer HP LaserJet on 3rd floor not printing. Category: Hardware/Printer/Network Printer. Priority: Medium. Multiple users on 3rd floor Finance department unable to print. Print jobs go to queue but never print. Printer display shows "Ready" status. Affects approximately 25 users.', 'Incident', 'INC000004', 'ENT000000000004', 'SUMMARY', 0, '{"assigned_group": "Desktop Support", "title": "Shared printer not printing", "category": "Hardware/Printer/Network Printer", "status": "Closed", "priority": "Medium", "impact": "Department"}'),
('INC000004-RESOLUTION-0', generate_random_embedding(), 'Resolution for INC000004: Print spooler service restart and queue clear resolved the issue. Steps: 1) Connected to print server PRTSRV01 2) Found 847 stuck print jobs in queue 3) Stopped Print Spooler service 4) Cleared all jobs from spool folder 5) Restarted Print Spooler service 6) Test printed successfully.', 'Incident', 'INC000004', 'ENT000000000004', 'RESOLUTION', 0, '{"assigned_group": "Desktop Support", "title": "Shared printer not printing", "status": "Closed"}');

-- INC000005: BSOD
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
('INC000005-SUMMARY-0', generate_random_embedding(), 'Incident INC000005: Workstation experiencing frequent BSOD crashes. Category: Hardware/Desktop/System Crash. Priority: High. User workstation (Dell OptiPlex 7090) crashing with blue screen error KERNEL_DATA_INPAGE_ERROR multiple times per day. Critical for HR department payroll processing.', 'Incident', 'INC000005', 'ENT000000000005', 'SUMMARY', 0, '{"assigned_group": "Desktop Support", "title": "Frequent BSOD - KERNEL_DATA_INPAGE_ERROR", "category": "Hardware/Desktop/System Crash", "status": "Closed", "priority": "High"}'),
('INC000005-RESOLUTION-0', generate_random_embedding(), 'Resolution for INC000005: Failing hard drive replaced. Diagnostics: 1) Analyzed memory dump - pointed to disk subsystem 2) Ran Dell diagnostics - HDD SMART status showing pending sector count warning 3) CrystalDiskInfo confirmed drive health at 23% 4) Backed up user data 5) Replaced HDD with new SSD 6) Restored Windows image and user data.', 'Incident', 'INC000005', 'ENT000000000005', 'RESOLUTION', 0, '{"assigned_group": "Desktop Support", "title": "Frequent BSOD", "status": "Closed"}');

-- INC000006: Software Installation
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
('INC000006-SUMMARY-0', generate_random_embedding(), 'Incident INC000006: Request to install Adobe Acrobat Pro DC. Category: Software/Installation/Adobe. Priority: Low. Marketing department user needs Adobe Acrobat Pro DC for editing PDF files and creating fillable forms.', 'Incident', 'INC000006', 'ENT000000000006', 'SUMMARY', 0, '{"assigned_group": "Desktop Support", "title": "Install Adobe Acrobat Pro DC", "category": "Software/Installation/Adobe", "status": "Closed", "priority": "Low"}'),
('INC000006-RESOLUTION-0', generate_random_embedding(), 'Resolution for INC000006: Adobe Acrobat Pro DC installed successfully. Steps: 1) Verified manager approval 2) Deployed via SCCM 3) Removed Adobe Reader 4) Activated enterprise license 5) Verified functionality.', 'Incident', 'INC000006', 'ENT000000000006', 'RESOLUTION', 0, '{"assigned_group": "Desktop Support", "title": "Install Adobe Acrobat Pro DC", "status": "Closed"}');

-- INC000007: Network Drive
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
('INC000007-SUMMARY-0', generate_random_embedding(), 'Incident INC000007: Cannot access shared network drive S:. Category: Infrastructure/Storage/Network Share. Priority: High. Entire Accounting team (15 users) cannot access the S: drive. Error: "Windows cannot access. You do not have permission."', 'Incident', 'INC000007', 'ENT000000000007', 'SUMMARY', 0, '{"assigned_group": "Server Team", "title": "Network drive S: not accessible", "category": "Infrastructure/Storage/Network Share", "status": "Closed", "priority": "High", "impact": "Department"}'),
('INC000007-RESOLUTION-0', generate_random_embedding(), 'Resolution for INC000007: AD group membership restored after accidental deletion. Root cause: During AD cleanup, the security group was accidentally deleted. Fix: Recreated group, re-added users, verified permissions. Added group to protected objects.', 'Incident', 'INC000007', 'ENT000000000007', 'RESOLUTION', 0, '{"assigned_group": "Server Team", "title": "Network drive S: not accessible", "status": "Closed"}');

-- INC000008: MFA Issue
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
('INC000008-SUMMARY-0', generate_random_embedding(), 'Incident INC000008: MFA codes not being accepted for Microsoft 365 login. Category: Security/Authentication/MFA. Priority: High. Executive user unable to login to M365 applications. Microsoft Authenticator app shows codes but they are rejected. User recently got new phone.', 'Incident', 'INC000008', 'ENT000000000008', 'SUMMARY', 0, '{"assigned_group": "Identity Management", "title": "MFA codes not working after phone change", "category": "Security/Authentication/MFA", "status": "Closed", "priority": "High"}'),
('INC000008-RESOLUTION-0', generate_random_embedding(), 'Resolution for INC000008: MFA registration reset and re-enrolled. Issue: Backup restore did not transfer cryptographic keys. Steps: 1) Verified identity 2) Reset MFA in Azure AD 3) Fresh Authenticator setup 4) Added backup phone method 5) Tested successfully.', 'Incident', 'INC000008', 'ENT000000000008', 'RESOLUTION', 0, '{"assigned_group": "Identity Management", "title": "MFA codes not working", "status": "Closed"}');

-- INC000009: Slow Computer
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
('INC000009-SUMMARY-0', generate_random_embedding(), 'Incident INC000009: Computer extremely slow - takes 15 minutes to boot. Category: Hardware/Desktop/Performance. Priority: Medium. User reports workstation has become progressively slower. Boot time increased from 2 minutes to 15 minutes.', 'Incident', 'INC000009', 'ENT000000000009', 'SUMMARY', 0, '{"assigned_group": "Desktop Support", "title": "Slow computer - long boot time", "category": "Hardware/Desktop/Performance", "status": "Closed", "priority": "Medium"}'),
('INC000009-RESOLUTION-0', generate_random_embedding(), 'Resolution for INC000009: Multiple issues resolved. 1) Disk 98% full - cleaned 45GB 2) 47 Chrome extensions removed 3) 15 startup programs disabled 4) 3 PUPs removed 5) Windows updates installed 6) HDD defragmented. Boot time now under 3 minutes.', 'Incident', 'INC000009', 'ENT000000000009', 'RESOLUTION', 0, '{"assigned_group": "Desktop Support", "title": "Slow computer", "status": "Closed"}');

-- INC000010: Database Connection Error
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
('INC000010-SUMMARY-0', generate_random_embedding(), 'Incident INC000010: SAP application showing database connection error. Category: Application/SAP/Database. Priority: Critical. SAP ERP system inaccessible. Error: "ORA-12541: TNS:no listener". Finance team cannot process month-end closing. 200 users affected.', 'Incident', 'INC000010', 'ENT000000000010', 'SUMMARY', 0, '{"assigned_group": "Database Administration", "title": "SAP database connection failure", "category": "Application/SAP/Database", "status": "Closed", "priority": "Critical", "impact": "Enterprise"}'),
('INC000010-RESOLUTION-0', generate_random_embedding(), 'Resolution for INC000010: Oracle listener service restarted after memory exhaustion. Root cause: Batch job consumed all memory, OOM killer terminated listener. Steps: 1) Verified server access 2) Started listener 3) Verified SAP reconnected. Prevention: RAM upgrade approved.', 'Incident', 'INC000010', 'ENT000000000010', 'RESOLUTION', 0, '{"assigned_group": "Database Administration", "title": "SAP database connection failure", "status": "Closed"}');

-- INC000011-015: Additional incidents (abbreviated for brevity)
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
('INC000011-SUMMARY-0', generate_random_embedding(), 'Incident INC000011: No audio in Zoom meetings - cannot hear or be heard. Category: Software/Collaboration/Zoom. User reports audio not working in Zoom meetings. Headset works in other apps.', 'Incident', 'INC000011', 'ENT000000000011', 'SUMMARY', 0, '{"assigned_group": "Desktop Support", "title": "Zoom audio not working", "status": "Closed", "priority": "Medium"}'),
('INC000011-RESOLUTION-0', generate_random_embedding(), 'Resolution for INC000011: Zoom audio device settings corrected. Zoom was configured to use disconnected Bluetooth headset. Changed to current USB headset. User advised on device selection.', 'Incident', 'INC000011', 'ENT000000000011', 'RESOLUTION', 0, '{"assigned_group": "Desktop Support", "title": "Zoom audio not working", "status": "Closed"}'),
('INC000012-SUMMARY-0', generate_random_embedding(), 'Incident INC000012: User account keeps getting locked out every few hours. Category: Security/Account/Lockout. Priority: High. User reports AD account locks every 2-3 hours even after password reset.', 'Incident', 'INC000012', 'ENT000000000012', 'SUMMARY', 0, '{"assigned_group": "Identity Management", "title": "Repeated account lockouts", "status": "Closed", "priority": "High"}'),
('INC000012-RESOLUTION-0', generate_random_embedding(), 'Resolution for INC000012: Stale credentials on old mobile device causing lockouts. Old phone in drawer attempting sync with expired password. Removed device from Exchange ActiveSync, performed remote wipe.', 'Incident', 'INC000012', 'ENT000000000012', 'RESOLUTION', 0, '{"assigned_group": "Identity Management", "title": "Repeated account lockouts", "status": "Closed"}'),
('INC000013-SUMMARY-0', generate_random_embedding(), 'Incident INC000013: Laptop WiFi disconnects every 30 minutes. Category: Network/Wireless/Connectivity. Priority: Medium. User laptop keeps dropping WiFi connection randomly throughout the day.', 'Incident', 'INC000013', 'ENT000000000013', 'SUMMARY', 0, '{"assigned_group": "Network Operations", "title": "WiFi dropping intermittently", "status": "Closed", "priority": "Medium"}'),
('INC000013-RESOLUTION-0', generate_random_embedding(), 'Resolution for INC000013: WiFi adapter power management disabled. Windows was turning off adapter to save power. Unchecked power management option, updated driver, set roaming to Highest.', 'Incident', 'INC000013', 'ENT000000000013', 'RESOLUTION', 0, '{"assigned_group": "Network Operations", "title": "WiFi dropping intermittently", "status": "Closed"}'),
('INC000014-SUMMARY-0', generate_random_embedding(), 'Incident INC000014: No internet access - internal sites work. Category: Network/Internet/Proxy. Priority: Medium. User can access internal SharePoint but not external websites. Chrome shows proxy error. New employee.', 'Incident', 'INC000014', 'ENT000000000014', 'SUMMARY', 0, '{"assigned_group": "Network Operations", "title": "No internet - proxy error", "status": "Closed", "priority": "Medium"}'),
('INC000014-RESOLUTION-0', generate_random_embedding(), 'Resolution for INC000014: User added to proxy authentication group. New employees not automatically added to BlueCoat proxy group. Added to group, cleared browser cache. Process improvement submitted.', 'Incident', 'INC000014', 'ENT000000000014', 'RESOLUTION', 0, '{"assigned_group": "Network Operations", "title": "No internet - proxy error", "status": "Closed"}'),
('INC000015-SUMMARY-0', generate_random_embedding(), 'Incident INC000015: Dell docking station not recognizing monitors. Category: Hardware/Peripheral/Docking Station. Priority: Medium. Laptop not outputting to dual monitors via Dell WD19 dock. Charging works but no display. Happened after Windows update.', 'Incident', 'INC000015', 'ENT000000000015', 'SUMMARY', 0, '{"assigned_group": "Desktop Support", "title": "Docking station monitors not working", "status": "Closed", "priority": "Medium"}'),
('INC000015-RESOLUTION-0', generate_random_embedding(), 'Resolution for INC000015: DisplayLink driver update resolved compatibility issue with recent Windows update KB5034441. Downloaded and installed latest DisplayLink driver from Dell support. Both monitors now working.', 'Incident', 'INC000015', 'ENT000000000015', 'RESOLUTION', 0, '{"assigned_group": "Desktop Support", "title": "Docking station monitors not working", "status": "Closed"}');

\echo 'Incidents inserted: 15 records'

-- =============================================================================
-- WORK ORDERS
-- =============================================================================
\echo 'Inserting Work Order data...'

DELETE FROM embedding_store WHERE source_type = 'WorkOrder';

INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
('WO000001-SUMMARY-0', generate_random_embedding(), 'Work Order WO000001: New employee onboarding - IT setup required. Category: Access Management/New Hire. New hire starting in Marketing department. Requires standard laptop, Microsoft 365, Adobe Creative Suite, Salesforce access.', 'WorkOrder', 'WO000001', 'ENT000000100001', 'SUMMARY', 0, '{"assigned_group": "Desktop Support", "title": "New hire IT setup - Marketing", "status": "Closed", "priority": "Medium"}'),
('WO000001-DESCRIPTION-0', generate_random_embedding(), 'New employee: Michael Chen. Start Date: February 1, 2024. Department: Marketing. Equipment: Dell Latitude 5540, 27-inch monitor, wireless peripherals. Software: Microsoft 365, Adobe Creative Suite, Salesforce. Access: Building badge, VPN, department drives.', 'WorkOrder', 'WO000001', 'ENT000000100001', 'DESCRIPTION', 0, '{"assigned_group": "Desktop Support", "title": "New hire IT setup", "status": "Closed"}'),
('WO000002-SUMMARY-0', generate_random_embedding(), 'Work Order WO000002: Request for Microsoft Visio Professional license. Category: Software/License/Microsoft. Solution Architect needs Visio for technical architecture diagrams. Manager approval provided.', 'WorkOrder', 'WO000002', 'ENT000000100002', 'SUMMARY', 0, '{"assigned_group": "Software Asset Management", "title": "Visio Pro license request", "status": "Closed", "priority": "Low"}'),
('WO000003-SUMMARY-0', generate_random_embedding(), 'Work Order WO000003: Configure video conferencing in new meeting room. Category: Infrastructure/AV/Conference Room. Room "Innovation Lab" Floor 5 needs Zoom Room setup with 85-inch display, camera, and microphone array.', 'WorkOrder', 'WO000003', 'ENT000000100003', 'SUMMARY', 0, '{"assigned_group": "AV Support", "title": "Meeting room AV configuration", "status": "Closed", "priority": "Medium"}'),
('WO000004-SUMMARY-0', generate_random_embedding(), 'Work Order WO000004: Request access to Production Database environment. Category: Access Management/Database/Production. DBA needs read-only access to Production Oracle databases for performance monitoring.', 'WorkOrder', 'WO000004', 'ENT000000100004', 'SUMMARY', 0, '{"assigned_group": "Database Administration", "title": "Production database access request", "status": "Closed", "priority": "Medium"}'),
('WO000005-SUMMARY-0', generate_random_embedding(), 'Work Order WO000005: Standard laptop refresh - 4 year old device. Category: Hardware/Laptop/Refresh. Current laptop eligible for standard refresh due to age and performance issues.', 'WorkOrder', 'WO000005', 'ENT000000100005', 'SUMMARY', 0, '{"assigned_group": "Desktop Support", "title": "Laptop refresh request", "status": "Closed", "priority": "Low"}');

\echo 'Work Orders inserted: 5 records'

-- =============================================================================
-- KNOWLEDGE ARTICLES
-- =============================================================================
\echo 'Inserting Knowledge Article data...'

DELETE FROM embedding_store WHERE source_type = 'KnowledgeArticle';

INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
('KB0001-CONTENT-0', generate_random_embedding(), 'Knowledge Article KB0001: How to Reset Your Windows Password. Self-service password reset using https://passwordreset.microsoftonline.com. Prerequisites: SSPR registration, MFA access. Steps: Enter email, verify identity, create new password meeting complexity requirements (12+ chars, upper, lower, number, special character).', 'KnowledgeArticle', 'KB0001', 'ENT000000200001', 'ARTICLE_CONTENT', 0, '{"assigned_group": "Service Desk", "title": "How to Reset Your Windows Password", "status": "Published", "keywords": "password reset, forgot password, SSPR"}'),
('KB0001-CONTENT-1', generate_random_embedding(), 'Password Reset Troubleshooting: Account not found - verify full email. SSPR not enabled - contact Service Desk to register. Verification failed - check phone number in profile. Account locked - wait 30 minutes or call Service Desk. Contact: 1-800-555-HELP, servicedesk@acme.com.', 'KnowledgeArticle', 'KB0001', 'ENT000000200001', 'ARTICLE_CONTENT', 1, '{"assigned_group": "Service Desk", "title": "How to Reset Your Windows Password", "status": "Published"}'),
('KB0002-CONTENT-0', generate_random_embedding(), 'Knowledge Article KB0002: Setting Up Cisco AnyConnect VPN. Install from https://vpn.acme.com or pre-installed on corporate laptops. Connect: Open AnyConnect, enter vpn.acme.com, login with credentials, approve MFA. Split Tunnel (default) or Full Tunnel available.', 'KnowledgeArticle', 'KB0002', 'ENT000000200002', 'ARTICLE_CONTENT', 0, '{"assigned_group": "Network Operations", "title": "Setting Up Cisco AnyConnect VPN", "status": "Published", "keywords": "VPN, remote access, AnyConnect, work from home"}'),
('KB0002-CONTENT-1', generate_random_embedding(), 'VPN Troubleshooting: Error 691 - verify credentials, check if locked. Connection drops - disable power-saving, try Full Tunnel. Slow performance - use Split Tunnel, close unnecessary apps. Cannot access resources - verify connection, clear DNS cache, restart browser.', 'KnowledgeArticle', 'KB0002', 'ENT000000200002', 'ARTICLE_CONTENT', 1, '{"assigned_group": "Network Operations", "title": "Setting Up Cisco AnyConnect VPN", "status": "Published"}'),
('KB0003-CONTENT-0', generate_random_embedding(), 'Knowledge Article KB0003: Configuring Microsoft Outlook. Windows: Enter email when prompted, sign in with MFA. Mac: Add Account, select Microsoft 365. Mobile: Download Outlook app, add ACME email. Shared mailboxes: Account Settings > More Settings > Advanced > Add mailbox name.', 'KnowledgeArticle', 'KB0003', 'ENT000000200003', 'ARTICLE_CONTENT', 0, '{"assigned_group": "Desktop Support", "title": "Configuring Microsoft Outlook", "status": "Published", "keywords": "Outlook, email, Exchange, configuration"}'),
('KB0004-CONTENT-0', generate_random_embedding(), 'Knowledge Article KB0004: How to Add Network Printers. Printer naming: [Building]-[Floor]-[Type]-[Number]. Add via Settings > Printers or enter \\\\printserver.acme.local\\[PrinterName]. Secure Print: Select "Secure Print" printer, badge in at any MFP to release job.', 'KnowledgeArticle', 'KB0004', 'ENT000000200004', 'ARTICLE_CONTENT', 0, '{"assigned_group": "Desktop Support", "title": "How to Add Network Printers", "status": "Published", "keywords": "printer, printing, network printer"}'),
('KB0005-CONTENT-0', generate_random_embedding(), 'Knowledge Article KB0005: Microsoft Teams Meeting Best Practices. Before: Test audio/video in Settings, use headset, find quiet location. During: Mute when not speaking, use Raise Hand, share specific window not desktop. Issues: Check device settings, use headphones to prevent echo.', 'KnowledgeArticle', 'KB0005', 'ENT000000200005', 'ARTICLE_CONTENT', 0, '{"assigned_group": "Desktop Support", "title": "Teams Meeting Best Practices", "status": "Published", "keywords": "Teams, meeting, video conference"}');

\echo 'Knowledge Articles inserted: 5 records'

-- =============================================================================
-- CHANGE REQUESTS
-- =============================================================================
\echo 'Inserting Change Request data...'

DELETE FROM embedding_store WHERE source_type = 'ChangeRequest';

INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
('CHG000001-SUMMARY-0', generate_random_embedding(), 'Change Request CHG000001: Monthly Windows Server Security Patching - February 2024. Risk: Medium. Schedule: February 10, 2024 02:00-06:00 EST. Affects all Windows servers (47 servers). Rolling restarts, 5-10 minutes downtime per server.', 'ChangeRequest', 'CHG000001', 'ENT000000300001', 'SUMMARY', 0, '{"assigned_group": "Server Team", "title": "Monthly Windows Server Patching", "status": "Closed", "risk_level": "Medium"}'),
('CHG000001-IMPLEMENTATION-0', generate_random_embedding(), 'Implementation Plan: Pre-change - verify backups, confirm window with owners. Execution - patch non-critical servers first, then database servers with app team standby, domain controllers last. Post-change - run health checks, verify services, check event logs.', 'ChangeRequest', 'CHG000001', 'ENT000000300001', 'IMPLEMENTATION', 0, '{"assigned_group": "Server Team", "title": "Monthly Windows Server Patching", "status": "Closed"}'),
('CHG000001-ROLLBACK-0', generate_random_embedding(), 'Rollback Plan: Triggers - critical app failure, AD replication failure, more than 5 servers failing. Procedure - boot to Safe Mode, uninstall updates via DISM, or restore from backup. Rollback time: Single server 30-45 min, full environment 4-6 hours.', 'ChangeRequest', 'CHG000001', 'ENT000000300001', 'ROLLBACK', 0, '{"assigned_group": "Server Team", "title": "Monthly Windows Server Patching", "status": "Closed"}'),
('CHG000002-SUMMARY-0', generate_random_embedding(), 'Change Request CHG000002: Replace core switch stack in Data Center A. Risk: High. Schedule: February 24-25, 2024 23:00-03:00 EST. Complete network outage during cutover (30 minutes). New Cisco Catalyst 9500 replacing Catalyst 6500.', 'ChangeRequest', 'CHG000002', 'ENT000000300002', 'SUMMARY', 0, '{"assigned_group": "Network Operations", "title": "Core Switch Replacement - DC-A", "status": "Closed", "risk_level": "High"}'),
('CHG000002-IMPLEMENTATION-0', generate_random_embedding(), 'Implementation: Pre-stage new switches in lab, pre-configure VLANs/routing. Phase 1 - backup config, failover to DR. Phase 2 - hardware swap (23:30-01:00). Phase 3 - validate spanning tree, routing, connectivity. Phase 4 - failback applications, final validation.', 'ChangeRequest', 'CHG000002', 'ENT000000300002', 'IMPLEMENTATION', 0, '{"assigned_group": "Network Operations", "title": "Core Switch Replacement - DC-A", "status": "Closed"}'),
('CHG000002-ROLLBACK-0', generate_random_embedding(), 'Rollback: If new switches not operational within 2 hours, reinstall old Catalyst 6500 kept on-site. Original cables labeled. Configuration backups accessible offline. DR site remains operational. Time to rollback: 90 minutes.', 'ChangeRequest', 'CHG000002', 'ENT000000300002', 'ROLLBACK', 0, '{"assigned_group": "Network Operations", "title": "Core Switch Replacement - DC-A", "status": "Closed"}'),
('CHG000003-SUMMARY-0', generate_random_embedding(), 'Change Request CHG000003: Deploy CRM Application version 4.2.0 to Production. Risk: Medium. Schedule: February 15, 2024 18:00-22:00 EST. CRM unavailable 2 hours. New features: Enhanced reporting, Salesforce integration.', 'ChangeRequest', 'CHG000003', 'ENT000000300003', 'SUMMARY', 0, '{"assigned_group": "Application Support", "title": "CRM v4.2.0 Production Deployment", "status": "Closed", "risk_level": "Medium"}'),
('CHG000003-IMPLEMENTATION-0', generate_random_embedding(), 'Deployment: 18:00 maintenance banner, 18:15 stop app servers, 18:30 run DB migrations, 19:00 deploy WAR files, 19:45 start servers, 20:00 smoke test, 20:30 load test, 21:00 UAT validation, 21:30 remove banner.', 'ChangeRequest', 'CHG000003', 'ENT000000300003', 'IMPLEMENTATION', 0, '{"assigned_group": "Application Support", "title": "CRM v4.2.0 Production Deployment", "status": "Closed"}');

\echo 'Change Requests inserted: 3 records'

-- =============================================================================
-- Update sync_state
-- =============================================================================
\echo 'Updating sync_state...'

UPDATE sync_state SET
    last_sync_timestamp = EXTRACT(EPOCH FROM CURRENT_TIMESTAMP)::BIGINT,
    last_sync_at = CURRENT_TIMESTAMP,
    records_synced = CASE source_type
        WHEN 'Incident' THEN 15
        WHEN 'WorkOrder' THEN 5
        WHEN 'KnowledgeArticle' THEN 5
        WHEN 'ChangeRequest' THEN 3
    END,
    status = 'completed'
WHERE source_type IN ('Incident', 'WorkOrder', 'KnowledgeArticle', 'ChangeRequest');

-- =============================================================================
-- Sample chat memory
-- =============================================================================
\echo 'Inserting sample chat memory...'

DELETE FROM chat_memory;

INSERT INTO chat_memory (session_id, message_type, content, metadata) VALUES
('test-session-001', 'USER', 'How do I reset my password?', '{"user_id": "testuser1", "groups": ["Service Desk"]}'),
('test-session-001', 'AI', 'You can reset your password using the self-service portal at https://passwordreset.microsoftonline.com. You will need to verify your identity through MFA.', '{"sources": ["KB0001"], "confidence": 0.92}'),
('test-session-002', 'USER', 'VPN is not connecting, error 691', '{"user_id": "testuser2", "groups": ["Network Operations"]}'),
('test-session-002', 'AI', 'Error 691 indicates authentication failure. This could be caused by incorrect credentials, account lockout, or RADIUS server issues. Please verify your username and password are correct.', '{"sources": ["KB0002", "INC000002"], "confidence": 0.88}');

-- =============================================================================
-- Sample ingestion jobs
-- =============================================================================
\echo 'Inserting sample ingestion jobs...'

DELETE FROM ingestion_job;

INSERT INTO ingestion_job (job_type, source_type, status, started_at, completed_at, records_processed, chunks_created) VALUES
('FULL', 'Incident', 'completed', CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '1 hour', 15, 35),
('FULL', 'WorkOrder', 'completed', CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '90 minutes', 5, 8),
('FULL', 'KnowledgeArticle', 'completed', CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '85 minutes', 5, 9),
('FULL', 'ChangeRequest', 'completed', CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '80 minutes', 3, 8);

-- =============================================================================
-- Summary
-- =============================================================================
\echo ''
\echo '=============================================='
\echo 'Dummy data seed completed successfully!'
\echo '=============================================='

SELECT
    'Embeddings' as table_name,
    source_type,
    COUNT(*) as record_count
FROM embedding_store
GROUP BY source_type
UNION ALL
SELECT
    'Chat Memory' as table_name,
    'Messages' as source_type,
    COUNT(*) as record_count
FROM chat_memory
UNION ALL
SELECT
    'Ingestion Jobs' as table_name,
    'Jobs' as source_type,
    COUNT(*) as record_count
FROM ingestion_job
ORDER BY table_name, source_type;

\echo ''
\echo 'To test semantic search, run:'
\echo 'SELECT * FROM search_embeddings(generate_random_embedding(), 5, 0.0);'
