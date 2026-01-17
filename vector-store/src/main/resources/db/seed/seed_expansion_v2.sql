-- BMC Remedy RAG Agent - Knowledge Base Expansion V2
-- Additional comprehensive IT Service Desk content
-- Covers: Security, Networking, Hardware, Software, Mobile, Cloud Services
-- ================================================================================

\echo 'Starting knowledge base expansion V2...'

-- Ensure embedding generator exists (correct version)
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

-- ================================================================================
-- SECTION 1: SECURITY INCIDENTS (Critical)
-- ================================================================================
\echo 'Inserting Security Incidents...'

INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
-- Phishing Attack Incident
('INC2024001-SUMMARY', generate_random_embedding(),
'Security Incident: Phishing Attack - Multiple users reported receiving sophisticated phishing emails appearing to be from executive leadership. The emails requested urgent wire transfers and contained malicious attachments. IT Security team issued immediate alert to all staff to delete such emails and report any similar communications.',
'Incident', 'INC2024001', 'INC2024001', 'SUMMARY', 0,
'{"assigned_group": "IT Security", "status": "Closed", "priority": "Critical", "category": "Security"}'),

('INC2024001-DESC-0', generate_random_embedding(),
'Affected users: john.doe@company.com, jane.smith@company.com, mike.wilson@company.com. All users clicked on the malicious link before reporting. Immediate actions taken: 1) Forced password reset for all affected accounts, 2) Disabled compromised email accounts temporarily, 3) Scanned all devices for malware using endpoint protection, 4) Blocked sender domain in email gateway.',
'Incident', 'INC2024001', 'INC2024001', 'DESCRIPTION', 1,
'{"assigned_group": "IT Security", "status": "Closed", "priority": "Critical", "category": "Security"}'),

('INC2024001-RESOL-0', generate_random_embedding(),
'Resolution: No data breach detected. All user passwords were reset within 2 hours of incident report. Email filters updated to block similar phishing patterns. All affected users completed mandatory security awareness training. Communication sent to entire company about phishing awareness. Incident escalated to CISO for review. Prevention measures implemented: SPF/DKIM/DMARC records tightened, additional email gateway rules added.',
'Incident', 'INC2024001', 'INC2024001', 'RESOLUTION', 2,
'{"assigned_group": "IT Security", "status": "Closed", "priority": "Critical", "category": "Security"}'),

-- Malware Outbreak
('INC2024002-SUMMARY', generate_random_embedding(),
'Security Incident: Malware Detection - Ransomware variant detected on Finance department file server. Antivirus alerted on suspicious encryption activity. Server immediately isolated from network to prevent spread. No ransom demand received yet. Critical finance data potentially affected.',
'Incident', 'INC2024002', 'INC2024002', 'SUMMARY', 0,
'{"assigned_group": "IT Security", "status": "Closed", "priority": "Critical", "category": "Security"}'),

('INC2024002-WORKLOG-0', generate_random_embedding(),
'Incident Timeline: 09:15 - AV alert triggered on FS-FINANCE-01, 09:20 - Server isolated by automated response, 09:25 - IT Security team engaged, 09:30 - Incident declared, 10:00 - Backups identified for recovery, 10:30 - Decision made to wipe and rebuild server, 14:00 - Server restored from clean backup (24 hours prior), 14:30 - File-by-file review completed, no active malware found.',
'Incident', 'INC2024002', 'INC2024002', 'WORK_LOG', 1,
'{"assigned_group": "IT Security", "status": "Closed", "priority": "Critical", "category": "Security"}'),

-- Unauthorized Access Attempt
('INC2024003-SUMMARY', generate_random_embedding(),
'Security Incident: Brute Force Attack - Multiple failed login attempts detected from external IP address 185.220.101.1. Over 5000 failed authentication attempts against VPN gateway in 2-hour period. Account lockout policies prevented unauthorized access. No successful breaches detected. IP address blocked at firewall level.',
'Incident', 'INC2024003', 'INC2024003', 'SUMMARY', 0,
'{"assigned_group": "IT Security", "status": "Closed", "priority": "High", "category": "Security"}'),

-- Data Loss Prevention Alert
('INC2024004-SUMMARY', generate_random_embedding(),
'Security Incident: DLP Alert - Sensitive data detected in outgoing email to personal Gmail account. Employee attempted to send customer list containing PII (names, addresses, phone numbers). Email automatically blocked by DLP system. Employee claimed this was for working from home over weekend. Manager notified for HR action.',
'Incident', 'INC2024004', 'INC2024004', 'SUMMARY', 0,
'{"assigned_group": "IT Security", "status": "Closed", "priority": "High", "category": "Security"}'),

-- Account Takeover
('INC2024005-SUMMARY', generate_random_embedding(),
'Security Incident: Account Takeover - Executive email account compromised due to password spray attack. Attacker sent fraudulent wire transfer requests to finance team. $50,000 transferred before fraud detected. Bank notified and freeze initiated. Account credentials reset, MFA enforced. Incident under investigation by authorities.',
'Incident', 'INC2024005', 'INC2024005', 'SUMMARY', 0,
'{"assigned_group": "IT Security", "status": "Closed", "priority": "Critical", "category": "Security"}'),

('INC2024005-RESOL-0', generate_random_embedding(),
'Resolution: Account fully secured. All executive accounts now require hardware security keys for MFA. New wire transfer procedures implemented requiring dual approval and phone verification. Security awareness training scheduled for all finance staff. Insurance claim filed for lost funds. Dark web monitoring initiated for leaked credentials.',
'Incident', 'INC2024005', 'INC2024005', 'RESOLUTION', 1,
'{"assigned_group": "IT Security", "status": "Closed", "priority": "Critical", "category": "Security"}');

-- ================================================================================
-- SECTION 2: NETWORKING INCIDENTS
-- ================================================================================
\echo 'Inserting Networking Incidents...'

INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
-- Complete Network Outage
('INC2024010-SUMMARY', generate_random_embedding(),
'Network Outage: Complete network connectivity loss at main office. All users unable to access internet, cloud applications, or internal servers. Root cause: Core switch power supply failure. Redundant PSU failed to take over due to firmware bug. Impact: All 500+ users affected for 45 minutes. VOIP phones down, critical business applications inaccessible.',
'Incident', 'INC2024010', 'INC2024010', 'SUMMARY', 0,
'{"assigned_group": "Network Engineering", "status": "Closed", "priority": "Critical", "category": "Network"}'),

('INC2024010-RESOL-0', generate_random_embedding(),
'Resolution: Temporary workaround - Bypassed failed core switch by redistributing critical VLANs to access layer switches. Replacement switch procured and configured within 4 hours. Full network restoration completed during maintenance window (2AM-4AM). Firmware updated on all core switches to prevent PSU failover bug. Configuration backups verified.',
'Incident', 'INC2024010', 'INC2024010', 'RESOLUTION', 1,
'{"assigned_group": "Network Engineering", "status": "Closed", "priority": "Critical", "category": "Network"}'),

-- VPN Performance Issues
('INC2024011-SUMMARY', generate_random_embedding(),
'Network Issue: VPN Performance Degradation - Remote users experiencing extremely slow VPN connections (less than 1Mbps on 100Mbps home connections). Intermittent disconnects throughout the day. Issue started after firmware update on VPN concentrator. Affects approximately 200 remote workers. Productivity significantly impacted.',
'Incident', 'INC2024011', 'INC2024011', 'SUMMARY', 0,
'{"assigned_group": "Network Engineering", "status": "Closed", "priority": "High", "category": "Network"}'),

('INC2024011-RESOL-0', generate_random_embedding(),
'Troubleshooting Steps Taken: 1) Verified internet bandwidth at VPN concentrator - normal, 2) Checked CPU utilization on VPN device - 95% (abnormal), 3) Reviewed logs - SSL renegotiation happening excessively, 4) Identified firmware bug causing unnecessary re-handshakes, 5) Rolled back to previous firmware version, 6) Performance immediately improved to normal levels.',
'Incident', 'INC2024011', 'INC2024011', 'RESOLUTION', 1,
'{"assigned_group": "Network Engineering", "status": "Closed", "priority": "High", "category": "Network"}'),

-- WiFi Dead Zones
('INC2024012-SUMMARY', generate_random_embedding(),
'Network Issue: WiFi Coverage Problems - Conference rooms A, B, and C report poor WiFi signal and frequent disconnections during meetings. Users unable to participate in video calls or access presentations. Site survey revealed inadequate AP placement and interference from neighboring buildings. Affects executive floor and client meeting areas - high visibility issue.',
'Incident', 'INC2024012', 'INC2024012', 'SUMMARY', 0,
'{"assigned_group": "Network Engineering", "status": "Closed", "priority": "Medium", "category": "Network"}'),

('INC2024012-RESOL-0', generate_random_embedding(),
'Resolution: Additional WiFi access points installed in conference rooms. APs configured on 5GHz band only to reduce interference. Channel planning optimized to avoid conflicts with neighboring networks. Signal strength verified at -65dBm or better throughout affected areas. User feedback collected showing 100% improvement in connection stability.',
'Incident', 'INC2024012', 'INC2024012', 'RESOLUTION', 1,
'{"assigned_group": "Network Engineering", "status": "Closed", "priority": "Medium", "category": "Network"}'),

-- DNS Resolution Issues
('INC2024013-SUMMARY', generate_random_embedding(),
'Network Issue: Intermittent DNS Failures - Users experiencing random "page not found" errors when accessing internal and external websites. Issue affects multiple sites. DNS servers showing high query latency and timeouts. Some applications unable to connect to databases due to name resolution failures. Root cause: DNS cache poisoning attempt from external source.',
'Incident', 'INC2024013', 'INC2024013', 'SUMMARY', 0,
'{"assigned_group": "Network Engineering", "status": "Closed", "priority": "High", "category": "Network"}'),

('INC2024013-RESOL-0', generate_random_embedding(),
'Resolution: Implemented DNS Response Rate Limiting (RRL) to prevent cache poisoning attacks. Added DNSSEC validation for external domains. Configured split-horizon DNS to separate internal and external resolution. Added secondary DNS servers for redundancy. Monitoring configured to alert on anomalous query patterns. No successful breach detected.',
'Incident', 'INC2024013', 'INC2024013', 'RESOLUTION', 1,
'{"assigned_group": "Network Engineering", "status": "Closed", "priority": "High", "category": "Network"}');

-- ================================================================================
-- SECTION 3: HARDWARE INCIDENTS
-- ================================================================================
\echo 'Inserting Hardware Incidents...'

INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
-- Server Hardware Failure
('INC2024020-SUMMARY', generate_random_embedding(),
'Server Hardware Failure: Database server DB-PROD-01 experienced catastrophic disk failure in RAID array. Multiple drives failed simultaneously causing complete data loss. Hot spare drives engaged but RAID rebuild stuck at 17%. Critical ERP system down. Production database inaccessible. Disaster recovery plan initiated.',
'Incident', 'INC2024020', 'INC2024020', 'SUMMARY', 0,
'{"assigned_group": "Server Engineering", "status": "Closed", "priority": "Critical", "category": "Hardware"}'),

-- Laptop Battery Swelling
('INC2024021-SUMMARY', generate_random_embedding(),
'Hardware Issue: Laptop Battery Safety Hazard - User reported laptop trackpad not clicking and case bulging. Investigation revealed swollen lithium-ion battery posing fire risk. Model: Dell Latitude 7420, purchased 18 months ago. Immediate safety recall issued for all laptops from same batch (45 units). Replacement batteries ordered.',
'Incident', 'INC2024021', 'INC2024021', 'SUMMARY', 0,
'{"assigned_group": "IT Support", "status": "Closed", "priority": "High", "category": "Hardware"}'),

-- Printer Maintenance
('INC2024022-SUMMARY', generate_random_embedding(),
'Hardware Issue: Multifunction Printer Failure - Main floor MFP (HP LaserJet Enterprise) displaying "59.F0 Error" - developing heater failure. Printer completely inoperative. Queue of 15 users waiting for documents. Firmware reset attempted multiple times without success. Hardware service technician dispatched. Temporary loaner unit installed.',
'Incident', 'INC2024022', 'INC2024022', 'SUMMARY', 0,
'{"assigned_group": "IT Support", "status": "Closed", "priority": "Medium", "category": "Hardware"}'),

-- Monitor Flickering
('INC2024023-SUMMARY', generate_random_embedding(),
'Hardware Issue: Monitor Flickering - Multiple users (12+) reporting monitor flickering and eye strain after recent display driver update. Affected models: Dell UltraSharp U2722D. Issue correlates with Windows update KB5034441. Rollback of display driver resolves issue temporarily. Escalated to Dell support as hardware-level compatibility issue.',
'Incident', 'INC2024023', 'INC2024023', 'SUMMARY', 0,
'{"assigned_group": "IT Support", "status": "Closed", "priority": "Low", "category": "Hardware"}');

-- ================================================================================
-- SECTION 4: SOFTWARE INCIDENTS
-- ================================================================================
\echo 'Inserting Software Incidents...'

INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
-- Application Crash
('INC2024030-SUMMARY', generate_random_embedding(),
'Software Issue: CRM Application Crashes - Salesforce CRM application crashing immediately upon launch for all users. Error message: "Unexpected token < in JSON at position 0". Issue began after scheduled maintenance window. 100% of users affected. Sales team unable to access customer records or log activities. Business impact: Critical.',
'Incident', 'INC2024030', 'INC2024030', 'SUMMARY', 0,
'{"assigned_group": "Applications", "status": "Closed", "priority": "Critical", "category": "Software"}'),

('INC2024030-RESOL-0', generate_random_embedding(),
'Root Cause Analysis: During maintenance, API gateway configuration was incorrectly updated, causing all API requests to return HTML error pages instead of JSON. The CRM application''s JSON parser failed when encountering HTML, causing immediate crashes. Resolution: Reverted API gateway configuration to previous working version. Implemented additional validation to prevent similar misconfigurations.',
'Incident', 'INC2024030', 'INC2024030', 'RESOLUTION', 1,
'{"assigned_group": "Applications", "status": "Closed", "priority": "Critical", "category": "Software"}'),

-- Office 365 Activation Issues
('INC2024031-SUMMARY', generate_random_embedding(),
'Software Issue: Office 365 Activation Failures - Users reporting "Product Activation Failed" errors when opening Microsoft Office applications. Affects approximately 30% of Office users. KMS hostname appears correct but license requests timing out. Recent Windows Defender update blocking KMS communication. Issue resolved by adding KMS server to allowed list.',
'Incident', 'INC2024031', 'INC2024031', 'SUMMARY', 0,
'{"assigned_group": "IT Support", "status": "Closed", "priority": "High", "category": "Software"}'),

-- Browser Compatibility
('INC2024032-SUMMARY', generate_random_embedding(),
'Software Issue: Web Application Not Loading - Internal web application (HR portal) not loading in Chrome browser. Displays blank page with no error messages. Works fine in Firefox and Edge. Issue affects all Chrome users (browser version 120+). Console shows: "Uncaught TypeError: Cannot read properties of undefined". Development team deployed fix within 2 hours.',
'Incident', 'INC2024032', 'INC2024032', 'SUMMARY', 0,
'{"assigned_group": "Applications", "status": "Closed", "priority": "Medium", "category": "Software"}'),

-- Java Application Performance
('INC2024033-SUMMARY', generate_random_embedding(),
'Software Issue: Java Application Sluggishness - Legacy Java-based ERP application experiencing severe performance degradation. Simple transactions taking 30+ seconds. Application server CPU at 100%. Memory leak identified in custom reporting module. Heap dumps analyzed showing large object retention. Temporary fix: nightly application restarts. Permanent fix in development.',
'Incident', 'INC2024033', 'INC2024033', 'SUMMARY', 0,
'{"assigned_group": "Applications", "status": "In Progress", "priority": "High", "category": "Software"}');

-- ================================================================================
-- SECTION 5: MOBILE DEVICE INCIDENTS
-- ================================================================================
\echo 'Inserting Mobile Device Incidents...'

INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
-- MDM Enrollment Issues
('INC2024040-SUMMARY', generate_random_embedding(),
'Mobile Issue: MDM Enrollment Failure - New employees unable to enroll personal iOS devices in Mobile Device Management (MDM). Enrollment fails at "Installing MDM Profile" step. Error: "Profile Installation Failed - The server could not communicate with your device". Intune/Azure AD integration broken. Issue affecting all new iOS enrollments for 48 hours.',
'Incident', 'INC2024040', 'INC2024040', 'SUMMARY', 0,
'{"assigned_group": "Mobile Services", "status": "Closed", "priority": "High", "category": "Mobile"}'),

-- Email Sync on Mobile
('INC2024041-SUMMARY', generate_random_embedding(),
'Mobile Issue: Exchange Email Not Syncing - Android users reporting email not syncing automatically. Manual refresh works but push notifications not arriving. Affected devices: Samsung Galaxy S23, Pixel 7. Issue started after Android security patch. Workaround: Users can set sync interval to 15 minutes. Microsoft investigating Exchange ActiveSync protocol issue.',
'Incident', 'INC2024041', 'INC2024041', 'SUMMARY', 0,
'{"assigned_group": "Mobile Services", "status": "Closed", "priority": "Medium", "category": "Mobile"}'),

-- VPN Profile Issues
('INC2024042-SUMMARY', generate_random_embedding(),
'Mobile Issue: iOS VPN Connection Drops - iPhone users experiencing frequent VPN disconnections. Connection drops after 5-10 minutes of use. Requires manual reconnection. Issue affects iOS 17 users exclusively. Root cause: VPN app incompatible with iOS 17 power management features. App update released and users prompted to update.',
'Incident', 'INC2024042', 'INC2024042', 'SUMMARY', 0,
'{"assigned_group": "Mobile Services", "status": "Closed", "priority": "Medium", "category": "Mobile"}');

-- ================================================================================
-- SECTION 6: KNOWLEDGE ARTICLES - TROUBLESHOOTING GUIDES
-- ================================================================================
\echo 'Inserting Troubleshooting Knowledge Articles...'

INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
-- VPN Troubleshooting Guide
('KB1001-CONTENT-0', generate_random_embedding(),
'Knowledge Article: VPN Connection Troubleshooting Guide - This article helps users troubleshoot common VPN connection issues. Symptoms addressed: Unable to connect, frequent disconnections, slow performance, authentication errors. Prerequisites: Company-issued device, valid network connection, VPN client installed.',
'KnowledgeArticle', 'KB1001', 'ENT-KB1001', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Network Engineering", "category": "Troubleshooting", "keywords": "vpn,remote,connection,troubleshooting"}'),

('KB1001-CONTENT-1', generate_random_embedding(),
'Step 1 - Verify Internet Connection: Before troubleshooting VPN, ensure you have a working internet connection. Open a web browser and try accessing https://www.google.com. If this fails, your internet connection is the problem, not VPN. Try: Restarting your router, connecting via ethernet cable instead of WiFi, contacting your ISP if issue persists.',
'KnowledgeArticle', 'KB1001', 'ENT-KB1001', 'ARTICLE_CONTENT', 1,
'{"assigned_group": "Network Engineering", "category": "Troubleshooting"}'),

('KB1001-CONTENT-2', generate_random_embedding(),
'Step 2 - Check VPN Client Status: Open your VPN client (GlobalProtect, Cisco AnyConnect, or similar). Check the status indicator: Green/Connected = VPN working properly, Red/Disconnected = not connected, Yellow/Connecting = attempting connection. If stuck on "Connecting" for more than 30 seconds, click Disconnect and try connecting again.',
'KnowledgeArticle', 'KB1001', 'ENT-KB1001', 'ARTICLE_CONTENT', 2,
'{"assigned_group": "Network Engineering", "category": "Troubleshooting"}'),

('KB1001-CONTENT-3', generate_random_embedding(),
'Step 3 - Authentication Errors: If you receive "Authentication Failed" or "Invalid Credentials": Verify you are using correct username (usually your email address), Check if password expired (try logging into company portal), Clear stored credentials in VPN client and re-enter, If using MFA, ensure you approve the push notification or enter the correct code.',
'KnowledgeArticle', 'KB1001', 'ENT-KB1001', 'ARTICLE_CONTENT', 3,
'{"assigned_group": "Network Engineering", "category": "Troubleshooting"}'),

('KB1001-CONTENT-4', generate_random_embedding(),
'Step 4 - VPN Error Codes: Error 691 (Authentication Failed) - Wrong username/password or account locked. Wait 15 minutes and retry, or contact IT Service Desk. Error 720 (No VPN Controllers) - VPN gateway temporarily unavailable. Wait 5 minutes and retry. Error 809 (Network Timeout) - Firewall blocking VPN. Try: Disable 3rd party antivirus temporarily, Connect from different network, Contact IT if issue persists.',
'KnowledgeArticle', 'KB1001', 'ENT-KB1001', 'ARTICLE_CONTENT', 4,
'{"assigned_group": "Network Engineering", "category": "Troubleshooting"}'),

-- Email Troubleshooting
('KB1002-CONTENT-0', generate_random_embedding(),
'Knowledge Article: Outlook Not Sending/Receiving Emails - When Outlook stops sending or receiving emails, follow these steps. This applies to Outlook 2016, 2019, 2021, and Microsoft 365. Common causes: Offline mode enabled, connection issues, corrupted profile, large attachments blocking send/receive.',
'KnowledgeArticle', 'KB1002', 'ENT-KB1002', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "IT Support", "category": "Troubleshooting", "keywords": "outlook,email,exchange"}'),

('KB1002-CONTENT-1', generate_random_embedding(),
'Quick Fixes for Outlook: 1) Check Work Offline - Look at "Work Offline" button in Outlook ribbon. If highlighted, click it to go back online. 2) Restart Outlook - Close and reopen Outlook. If it won''t close, use Task Manager to end process. 3) Check Internet - Open a browser and verify internet works. 4) Test Web Mail - Log into OWA to see if emails arrive there.',
'KnowledgeArticle', 'KB1002', 'ENT-KB1002', 'ARTICLE_CONTENT', 1,
'{"assigned_group": "IT Support", "category": "Troubleshooting"}'),

-- Printer Troubleshooting
('KB1003-CONTENT-0', generate_random_embedding(),
'Knowledge Article: Printer Troubleshooting Guide - Steps to resolve common printer issues: Printer not printing, Print jobs stuck in queue, Poor print quality, Paper jams. Covers both local and network printers. Before troubleshooting: Verify printer is powered on, Check for error messages on printer display, Ensure paper is loaded and ink/toner not empty.',
'KnowledgeArticle', 'KB1003', 'ENT-KB1003', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "IT Support", "category": "Troubleshooting", "keywords": "printer,printing,hardware"}'),

('KB1003-CONTENT-1', generate_random_embedding(),
'Printer Shows as "Offline": In Windows: Go to Settings > Devices > Printers & scanners. Select your printer and click "Open queue". Click "Printer" menu and ensure "Use Printer Offline" is unchecked. Click "Set as default printer" if not already. If still offline: Restart Print Spooler service, Power cycle the printer, Update printer driver, Remove and re-add printer.',
'KnowledgeArticle', 'KB1003', 'ENT-KB1003', 'ARTICLE_CONTENT', 1,
'{"assigned_group": "IT Support", "category": "Troubleshooting"}'),

-- Password Reset Guide
('KB1004-CONTENT-0', generate_random_embedding(),
'Knowledge Article: Self-Service Password Reset Guide - Users can reset their own passwords without calling IT Service Desk. Prerequisites: You must have enrolled in MFA and registered alternative contact methods. Available at: https://password.company.com or via Ctrl+Alt+Del > Change Password on Windows domain-joined devices.',
'KnowledgeArticle', 'KB1004', 'ENT-KB1004', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "IT Support", "category": "Self-Service", "keywords": "password,reset,self-service"}'),

('KB1004-CONTENT-1', generate_random_embedding(),
'Password Reset Process: 1) Navigate to password reset portal, 2) Enter your username (employee ID or email), 3) Verify your identity via MFA (authentication app or SMS), 4) Choose a new password meeting requirements: Minimum 12 characters, Contains uppercase, lowercase, numbers, and symbols, Not used in previous 10 passwords. 5) Confirm new password. You will receive confirmation email.',
'KnowledgeArticle', 'KB1004', 'ENT-KB1004', 'ARTICLE_CONTENT', 1,
'{"assigned_group": "IT Support", "category": "Self-Service"}'),

-- WiFi Troubleshooting
('KB1005-CONTENT-0', generate_random_embedding(),
'Knowledge Article: WiFi Connection Problems - Troubleshooting guide for wireless connectivity issues. Symptoms: No WiFi networks visible, Cannot connect to WiFi, Frequent disconnections, Slow WiFi speeds. This guide covers both company-issued laptops and personal mobile devices.',
'KnowledgeArticle', 'KB1005', 'ENT-KB1005', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Network Engineering", "category": "Troubleshooting", "keywords": "wifi,wireless,network"}'),

('KB1005-CONTENT-1', generate_random_embedding(),
'Basic WiFi Troubleshooting: 1) Toggle WiFi Off/On - Open Network settings, turn WiFi off, wait 10 seconds, turn back on. 2) Forget Network - Remove saved network and reconnect. Windows: Settings > Network > Manage known networks > Forget. Mac: Network preferences > Advanced > WiFi > Remove. 3) Check Airplane Mode - Ensure airplane mode is disabled. 4) Restart Device - Full restart often clears connection issues.',
'KnowledgeArticle', 'KB1005', 'ENT-KB1005', 'ARTICLE_CONTENT', 1,
'{"assigned_group": "Network Engineering", "category": "Troubleshooting"}'),

('KB1005-CONTENT-2', generate_random_embedding(),
'WiFi Authentication Issues: If prompted for password repeatedly: Verify correct password (case-sensitive), Check if using correct network (Company vs Company-Guest), Ensure device time is correct (authentication fails with wrong time), Try different authentication method if available (certificate vs password). For company devices, ensure you''re logged in with domain credentials.',
'KnowledgeArticle', 'KB1005', 'ENT-KB1005', 'ARTICLE_CONTENT', 2,
'{"assigned_group": "Network Engineering", "category": "Troubleshooting"}'),

-- Blue Screen Troubleshooting
('KB1006-CONTENT-0', generate_random_embedding(),
'Knowledge Article: Windows Blue Screen of Death (BSOD) - Critical system error requiring immediate attention. When BSOD occurs: Note the error code and bug check string (e.g., DRIVER_IRQL_NOT_LESS_OR_EQUAL), Wait for memory dump creation (may take several minutes), System will restart automatically. If BSOD repeats, the issue requires investigation.',
'KnowledgeArticle', 'KB1006', 'ENT-KB1006', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "IT Support", "category": "Troubleshooting", "keywords": "bsod,blue,screen,crash"}'),

('KB1006-CONTENT-1', generate_random_embedding(),
'Common BSOD Error Codes: DRIVER_IRQL_NOT_LESS_OR_EQUAL (0x000000D1) - Usually driver issue, update display or network drivers. SYSTEM_SERVICE_EXCEPTION (0x0000003B) - Often caused by antivirus or system file corruption. PAGE_FAULT_IN_NONPAGED_AREA (0x00000050) - Faulty RAM or disk issues. CRITICAL_PROCESS_DIED (0x000000EF) - Critical system file damaged, may need OS repair. UNMOUNTABLE_BOOT_VOLUME (0x000000ED) - Disk corruption or bad sectors.',
'KnowledgeArticle', 'KB1006', 'ENT-KB1006', 'ARTICLE_CONTENT', 1,
'{"assigned_group": "IT Support", "category": "Troubleshooting"}'),

-- Remote Desktop Issues
('KB1007-CONTENT-0', generate_random_embedding(),
'Knowledge Article: Remote Desktop Connection Problems - Guide for troubleshooting RDP and remote access issues. Symptoms: Cannot connect to remote computer, Connection drops frequently, Black screen after connection, Audio/clipboard not working. Applies to: Microsoft Remote Desktop, VPN RDP, TeamViewer, AnyDesk.',
'KnowledgeArticle', 'KB1007', 'ENT-KB1007', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "IT Support", "category": "Troubleshooting", "keywords": "rdp,remote,desktop"}'),

('KB1007-CONTENT-1', generate_random_embedding(),
'RDP Cannot Connect - Checklist: 1) Verify remote computer is turned on and connected to network, 2) Ensure Remote Desktop is enabled on target PC, 3) Check if user has permission for RDP, 4) Verify correct computer name/IP address, 5) Check firewall settings (port 3389 must be open), 6) Confirm network connectivity (ping target computer), 7) Try via VPN if outside company network.',
'KnowledgeArticle', 'KB1007', 'ENT-KB1007', 'ARTICLE_CONTENT', 1,
'{"assigned_group": "IT Support", "category": "Troubleshooting"}'),

-- File Share Access Issues
('KB1008-CONTENT-0', generate_random_embedding(),
'Knowledge Article: Cannot Access Network Shared Drives - Troubleshooting file share connectivity. Symptoms: Mapped drives disconnected, "Access Denied" errors, Drives not appearing in File Explorer, Cannot open files on share. Applies to: Windows file shares, SharePoint, OneDrive, network drives.',
'KnowledgeArticle', 'KB1008', 'ENT-KB1008', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "IT Support", "category": "Troubleshooting", "keywords": "file,share,network,drive"}'),

('KB1008-CONTENT-1', generate_random_embedding(),
'Regain Access to Network Shares: 1) Confirm VPN is connected (if working remotely), 2) Use IP address instead of name: \\192.168.1.100\share, 3) Use Fully Qualified Domain Name: \\server.company.local\share, 4) Map drive with alternate credentials (specify username/password), 5) Clear saved credentials: Credential Manager > Windows Credentials > remove entries, 6) Restart "Workstation" service in services.msc.',
'KnowledgeArticle', 'KB1008', 'ENT-KB1008', 'ARTICLE_CONTENT', 1,
'{"assigned_group": "IT Support", "category": "Troubleshooting"}');

-- ================================================================================
-- SECTION 7: WORK ORDERS - SERVICE REQUESTS
-- ================================================================================
\echo 'Inserting Work Orders...'

INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
-- New Employee Equipment Setup
('WO2024001-DESC-0', generate_random_embedding(),
'Work Order: New Employee Equipment Provisioning - Request to provision IT equipment for new hire starting next Monday. Standard package requested: Laptop (Dell Latitude 5440), 24" Monitor, Docking station, Keyboard and mouse, Phone handset. User requires software: Microsoft Office, Adobe Creative Cloud, VPN access, ERP system access. Due date: Before employee start date.',
'WorkOrder', 'WO2024001', 'WO2024001', 'DESCRIPTION', 0,
'{"assigned_group": "IT Procurement", "status": "Approved", "priority": "Normal", "category": "New Hire"}'),

-- Software License Request
('WO2024002-DESC-0', generate_random_embedding(),
'Work Order: Software License Request - Marketing department requesting 5 additional licenses for Adobe Creative Cloud. Current licenses fully utilized. New designer position starting next month requires access to Photoshop, Illustrator, InDesign. Business justification provided: Increased workload for upcoming product launch. Cost center approved budget.',
'WorkOrder', 'WO2024002', 'WO2024002', 'DESCRIPTION', 0,
'{"assigned_group": "IT Procurement", "status": "Pending Approval", "priority": "Normal", "category": "Software"}'),

-- Access Rights Request
('WO2024003-DESC-0', generate_random_embedding(),
'Work Order: System Access Request - Manager requesting elevated access for team member. Employee requires: Read-only access to Finance folder on shared drive, Contribute access to Marketing SharePoint site, ERP system "Approver" role, CRM system "Sales Manager" license. Manager approval attached. Employee completed required security training.',
'WorkOrder', 'WO2024003', 'WO2024003', 'DESCRIPTION', 0,
'{"assigned_group": "Access Management", "status": "In Progress", "priority": "Normal", "category": "Access"}'),

-- Hardware Upgrade Request
('WO2024004-DESC-0', generate_random_embedding(),
'Work Order: Hardware Upgrade Request - Senior Developer requesting upgrade from 16GB RAM to 32GB RAM. Current system insufficient for development tasks: Docker containers, local database instances, IDE with multiple projects. Current hardware causes performance issues impacting productivity. Business case: 30 minutes saved daily = 2.5 hours/week.',
'WorkOrder', 'WO2024004', 'WO2024004', 'DESCRIPTION', 0,
'{"assigned_group": "IT Support", "status": "Approved", "priority": "Low", "category": "Hardware"}'),

-- VPN Access for Contractor
('WO2024005-DESC-0', generate_random_embedding(),
'Work Order: External Access Request - Third-party contractor requiring VPN access for 3-month project. Contractor background check completed. NDA signed. Sponsor: Director of Engineering. Access scope: Limited to specific project servers. Network segmentation: Contractor VLAN with restricted access. Account expires automatically on project end date.',
'WorkOrder', 'WO2024005', 'WO2024005', 'DESCRIPTION', 0,
'{"assigned_group": "Access Management", "status": "Pending Approval", "priority": "Normal", "category": "External Access"}'),

-- Meeting Room Setup
('WO2024006-DESC-0', generate_random_embedding(),
'Work Order: AV Equipment Setup - Boardroom requires equipment setup for executive meeting next week. Requirements: Video conferencing system (Zoom Rooms integration), Wireless presentation system (share content from any device), Additional microphones for all attendees, Recording capability, Technical support on standby during meeting. Attendees: 15 on-site, 5 remote.',
'WorkOrder', 'WO2024006', 'WO2024006', 'DESCRIPTION', 0,
'{"assigned_group": "AV Services", "status": "In Progress", "priority": "High", "category": "Meeting Support"}');

-- ================================================================================
-- SECTION 8: CHANGE REQUESTS
-- ================================================================================
\echo 'Inserting Change Requests...'

INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata) VALUES
-- Server Migration
('CR2024005-IMPL-0', generate_random_embedding(),
'Change Request: Physical Server Migration to Virtual - Migrating legacy physical server (APP-OLD-01) to virtual environment. Physical server approaching end of life (hardware 7+ years old). Virtualization will improve: Disaster recovery capabilities, Performance scalability, Maintenance efficiency. Planned downtime: 4 hours during weekend maintenance window.',
'ChangeRequest', 'CR2024005', 'CR2024005', 'IMPLEMENTATION_PLAN', 0,
'{"assigned_group": "Server Engineering", "status": "Approved", "priority": "Normal", "category": "Infrastructure"}'),

('CR2024005-ROLLBACK-0', generate_random_embedding(),
'Rollback Plan - Server Migration CR2024005: If critical issues arise post-migration: 1) Shutdown virtual server, 2) Boot physical server from original disks, 3) Update DNS to point back to physical server IP, 4) Verify application connectivity, 5) Notify stakeholders of rollback. Rollback decision criteria: Application unavailable for >30 minutes, Data corruption detected.',
'ChangeRequest', 'CR2024005', 'CR2024005', 'ROLLBACK_PLAN', 1,
'{"assigned_group": "Server Engineering", "status": "Approved", "priority": "Normal", "category": "Infrastructure"}'),

-- Firewall Rule Update
('CR2024006-IMPL-0', generate_random_embedding(),
'Change Request: Firewall Rules Cleanup - Removing obsolete firewall rules from perimeter firewall. Audit identified 200+ rules with no traffic in 12+ months. Removing stale rules reduces: Attack surface, Rule processing overhead, Configuration complexity. Change type: Standard (pre-authorized). Risk: Low. Testing: Rules backed up before deletion.',
'ChangeRequest', 'CR2024006', 'CR2024006', 'IMPLEMENTATION_PLAN', 0,
'{"assigned_group": "Network Engineering", "status": "Approved", "priority": "Low", "category": "Security"}'),

-- Database Index Optimization
('CR2024007-IMPL-0', generate_random_embedding(),
'Change Request: Database Index Optimization - Adding composite indexes to improve query performance on ERP database. Several reports taking 5+ minutes to execute. Analysis shows missing indexes on frequently joined columns. Expected improvement: 80% reduction in query time. Risk: Very low. Testing: Applied to test environment, verified no negative impact.',
'ChangeRequest', 'CR2024007', 'CR2024007', 'IMPLEMENTATION_PLAN', 0,
'{"assigned_group": "Database Administration", "status": "Approved", "priority": "Normal", "category": "Performance"}'),

-- Security Patch Deployment
('CR2024008-IMPL-0', generate_random_embedding(),
'Change Request: Critical Security Patch Deployment - Deploying Microsoft security updates for January 2025 Patch Tuesday. Updates address 2 critical vulnerabilities (CVSS 9.0+) and 5 important vulnerabilities. Scope: All Windows servers (45 servers) and all Windows workstations (500+ workstations). Risk: Medium. Deployment timeline: Servers - Saturday 2AM-6AM, Workstations - staggered over 3 days.',
'ChangeRequest', 'CR2024008', 'CR2024008', 'IMPLEMENTATION_PLAN', 0,
'{"assigned_group": "Windows Engineering", "status": "Approved", "priority": "High", "category": "Security"}'),

('CR2024008-ROLLBACK-0', generate_random_embedding(),
'Rollback Plan - Security Patch CR2024008: If issues arise after patch deployment: 1) Pause workstation deployment immediately, 2) Identify problematic patch via Windows Update logs, 3) Uninstall specific problematic patch: wusa /uninstall /kb:XXXXXXX, 4) Document affected systems and patch, 5) Report to vendor and await guidance. Known issues monitored via Microsoft KB articles.',
'ChangeRequest', 'CR2024008', 'CR2024008', 'ROLLBACK_PLAN', 1,
'{"assigned_group": "Windows Engineering", "status": "Approved", "priority": "High", "category": "Security"}');

-- ================================================================================
-- SUMMARY
-- ================================================================================
\echo ''
\echo '========================================'
\echo 'Knowledge Base Expansion V2 Complete!'
\echo '========================================'
\echo 'Added:'
\echo '  - 9 Security Incidents'
\echo '  - 6 Networking Incidents'
\echo '  - 4 Hardware Incidents'
\echo '  - 5 Software Incidents'
\echo '  - 3 Mobile Device Incidents'
\echo '  - 18 Knowledge Articles'
\echo '  - 6 Work Orders'
\echo '  - 6 Change Requests'
\echo 'Total: ~57 new records with chunks'
\echo ''
