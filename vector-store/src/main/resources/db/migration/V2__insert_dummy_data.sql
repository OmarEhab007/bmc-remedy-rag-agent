-- BMC Remedy RAG Agent - Dummy IT Service Desk Data
-- This script generates realistic test data for the RAG agent
-- Run AFTER V1__create_embedding_tables.sql

-- =============================================================================
-- Helper function to generate random normalized 384-dimensional vectors
-- These simulate embeddings from all-minilm-l6-v2 model
-- =============================================================================
CREATE OR REPLACE FUNCTION generate_random_embedding()
RETURNS vector(384) AS $$
DECLARE
    arr float8[384];
    magnitude float8 := 0;
    i int;
BEGIN
    -- Generate random components
    FOR i IN 1..384 LOOP
        arr[i] := random() * 2 - 1;  -- Values between -1 and 1
        magnitude := magnitude + arr[i] * arr[i];
    END LOOP;

    -- Normalize to unit vector (like real embeddings)
    magnitude := sqrt(magnitude);
    FOR i IN 1..384 LOOP
        arr[i] := arr[i] / magnitude;
    END LOOP;

    RETURN arr::vector(384);
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- INCIDENT DATA
-- Realistic IT Service Desk incidents with common issues
-- =============================================================================

-- INC000001: Password Reset Issue
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('INC000001-SUMMARY-0', generate_random_embedding(),
'Incident INC000001: User unable to login - password expired. Category: Infrastructure/Authentication/Password. Priority: Medium. User John Smith from Finance department reported they cannot access their workstation after returning from vacation. The system shows "Your password has expired" message.',
'Incident', 'INC000001', 'ENT000000000001', 'SUMMARY', 0,
'{"assigned_group": "Service Desk", "title": "User unable to login - password expired", "category": "Infrastructure/Authentication/Password", "status": "Closed", "priority": "Medium", "customer_company": "ACME Corp", "urgency": "Medium", "impact": "Individual"}'),

('INC000001-RESOLUTION-0', generate_random_embedding(),
'Resolution for INC000001: Password reset completed successfully. Steps taken: 1) Verified user identity through security questions 2) Reset password in Active Directory using ADUC 3) Forced password change at next login 4) Confirmed user can access workstation and all applications. User education provided on password expiration policy (90 days). Ticket resolved and closed.',
'Incident', 'INC000001', 'ENT000000000001', 'RESOLUTION', 0,
'{"assigned_group": "Service Desk", "title": "User unable to login - password expired", "category": "Infrastructure/Authentication/Password", "status": "Closed", "priority": "Medium"}'),

('INC000001-WORKLOG-0', generate_random_embedding(),
'Work Log Entry [2024-01-15 09:30 - Service Desk Agent]: Received call from user. Verified identity using employee ID and manager name. User confirmed they were on vacation for 2 weeks. Password expired during absence. Initiating password reset procedure.',
'Incident', 'INC000001', 'ENT000000000001', 'WORK_LOG', 1,
'{"assigned_group": "Service Desk", "title": "User unable to login - password expired", "submitter": "jdoe@acme.com"}');

-- INC000002: VPN Connection Issue
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('INC000002-SUMMARY-0', generate_random_embedding(),
'Incident INC000002: VPN connection failing with error 691. Category: Network/VPN/Connectivity. Priority: High. Multiple remote users reporting inability to connect to corporate VPN. Error message: "The remote connection was denied because the user name and password combination you provided is not recognized". Affecting approximately 50 remote workers.',
'Incident', 'INC000002', 'ENT000000000002', 'SUMMARY', 0,
'{"assigned_group": "Network Operations", "title": "VPN connection failing with error 691", "category": "Network/VPN/Connectivity", "status": "Closed", "priority": "High", "urgency": "High", "impact": "Multiple Users"}'),

('INC000002-DESCRIPTION-0', generate_random_embedding(),
'Users experiencing VPN authentication failures started around 8:00 AM EST. The Cisco AnyConnect client shows error 691 immediately after entering credentials. Issue affects users connecting from home offices and mobile devices. Corporate office users on internal network are not affected. VPN server logs show RADIUS authentication timeouts.',
'Incident', 'INC000002', 'ENT000000000002', 'DESCRIPTION', 0,
'{"assigned_group": "Network Operations", "title": "VPN connection failing with error 691", "category": "Network/VPN/Connectivity", "status": "Closed"}'),

('INC000002-RESOLUTION-0', generate_random_embedding(),
'Resolution for INC000002: RADIUS server connectivity restored. Root cause: Network switch firmware update last night caused VLAN tagging issue affecting RADIUS server communication. Fix applied: 1) Rolled back switch firmware to previous stable version 2) Verified RADIUS server connectivity from all VPN concentrators 3) Tested VPN connections from multiple locations 4) Monitoring enabled for 24 hours. All affected users can now connect. Post-incident review scheduled.',
'Incident', 'INC000002', 'ENT000000000002', 'RESOLUTION', 0,
'{"assigned_group": "Network Operations", "title": "VPN connection failing with error 691", "category": "Network/VPN/Connectivity", "status": "Closed"}'),

('INC000002-WORKLOG-0', generate_random_embedding(),
'Work Log Entry [2024-01-16 08:15 - Network Team]: Initial triage completed. Confirmed issue is widespread. VPN concentrator logs show authentication requests being sent to RADIUS but timing out. Escalating to Network Operations for investigation.',
'Incident', 'INC000002', 'ENT000000000002', 'WORK_LOG', 1,
'{"assigned_group": "Network Operations", "submitter": "netops@acme.com"}'),

('INC000002-WORKLOG-1', generate_random_embedding(),
'Work Log Entry [2024-01-16 09:45 - Network Operations]: Root cause identified. Last night maintenance window included switch firmware updates. The update changed VLAN tagging behavior affecting RADIUS server subnet. Rolling back firmware now.',
'Incident', 'INC000002', 'ENT000000000002', 'WORK_LOG', 2,
'{"assigned_group": "Network Operations", "submitter": "jsmith@acme.com"}');

-- INC000003: Email Sync Issue - Outlook
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('INC000003-SUMMARY-0', generate_random_embedding(),
'Incident INC000003: Outlook not syncing emails - stuck on "Updating inbox". Category: Software/Email/Outlook. Priority: Medium. User reports Microsoft Outlook 365 has been stuck on "Updating inbox" for 3 hours. Send/receive shows "0 of 0" but webmail works fine. User is in Sales department and needs email for client communications.',
'Incident', 'INC000003', 'ENT000000000003', 'SUMMARY', 0,
'{"assigned_group": "Desktop Support", "title": "Outlook not syncing emails", "category": "Software/Email/Outlook", "status": "Closed", "priority": "Medium", "customer_company": "ACME Corp"}'),

('INC000003-RESOLUTION-0', generate_random_embedding(),
'Resolution for INC000003: Outlook profile corrupted - recreated profile. Troubleshooting steps performed: 1) Verified network connectivity - OK 2) Checked Outlook in Safe Mode - same issue 3) Ran Outlook diagnostic tool - found OST file corruption 4) Created new Outlook profile 5) Removed old OST file and allowed resync 6) Email now syncing correctly. User advised to wait for full mailbox sync (approximately 30 minutes for 5GB mailbox).',
'Incident', 'INC000003', 'ENT000000000003', 'RESOLUTION', 0,
'{"assigned_group": "Desktop Support", "title": "Outlook not syncing emails", "category": "Software/Email/Outlook", "status": "Closed"}');

-- INC000004: Printer Not Working
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('INC000004-SUMMARY-0', generate_random_embedding(),
'Incident INC000004: Shared printer HP LaserJet on 3rd floor not printing. Category: Hardware/Printer/Network Printer. Priority: Medium. Multiple users on 3rd floor Finance department unable to print. Print jobs go to queue but never print. Printer display shows "Ready" status. Affects approximately 25 users.',
'Incident', 'INC000004', 'ENT000000000004', 'SUMMARY', 0,
'{"assigned_group": "Desktop Support", "title": "Shared printer not printing", "category": "Hardware/Printer/Network Printer", "status": "Closed", "priority": "Medium", "impact": "Department"}'),

('INC000004-RESOLUTION-0', generate_random_embedding(),
'Resolution for INC000004: Print spooler service restart and queue clear resolved the issue. Steps taken: 1) Connected to print server PRTSRV01 2) Found 847 stuck print jobs in queue 3) Stopped Print Spooler service 4) Cleared all jobs from C:\\Windows\\System32\\spool\\PRINTERS 5) Restarted Print Spooler service 6) Test printed successfully from multiple workstations. Investigating why jobs got stuck - may need to increase spool folder disk space.',
'Incident', 'INC000004', 'ENT000000000004', 'RESOLUTION', 0,
'{"assigned_group": "Desktop Support", "title": "Shared printer not printing", "category": "Hardware/Printer/Network Printer", "status": "Closed"}');

-- INC000005: Blue Screen of Death
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('INC000005-SUMMARY-0', generate_random_embedding(),
'Incident INC000005: Workstation experiencing frequent BSOD crashes. Category: Hardware/Desktop/System Crash. Priority: High. User workstation (Dell OptiPlex 7090) crashing with blue screen error KERNEL_DATA_INPAGE_ERROR multiple times per day. User loses unsaved work. Critical for HR department payroll processing. Asset tag: WS-HR-2341.',
'Incident', 'INC000005', 'ENT000000000005', 'SUMMARY', 0,
'{"assigned_group": "Desktop Support", "title": "Frequent BSOD - KERNEL_DATA_INPAGE_ERROR", "category": "Hardware/Desktop/System Crash", "status": "Closed", "priority": "High", "urgency": "High", "impact": "Individual"}'),

('INC000005-DESCRIPTION-0', generate_random_embedding(),
'Blue screen errors occurring 3-5 times daily over the past week. Stop code: KERNEL_DATA_INPAGE_ERROR (0x0000007A). User reports crashes happen randomly but more frequently when working with large Excel files. System is 2 years old, recently had Windows updates installed. Memory diagnostic passed. Event viewer shows disk errors.',
'Incident', 'INC000005', 'ENT000000000005', 'DESCRIPTION', 0,
'{"assigned_group": "Desktop Support", "title": "Frequent BSOD - KERNEL_DATA_INPAGE_ERROR", "category": "Hardware/Desktop/System Crash", "status": "Closed"}'),

('INC000005-RESOLUTION-0', generate_random_embedding(),
'Resolution for INC000005: Failing hard drive replaced. Diagnostics performed: 1) Analyzed memory dump - pointed to disk subsystem 2) Ran Dell diagnostics - HDD SMART status showing pending sector count warning 3) CrystalDiskInfo confirmed drive health at 23% 4) Backed up user data to network share 5) Replaced 500GB HDD with new 512GB SSD 6) Restored Windows image and user data 7) System running stable for 48 hours post-replacement. User reports significant performance improvement with SSD.',
'Incident', 'INC000005', 'ENT000000000005', 'RESOLUTION', 0,
'{"assigned_group": "Desktop Support", "title": "Frequent BSOD - KERNEL_DATA_INPAGE_ERROR", "category": "Hardware/Desktop/System Crash", "status": "Closed"}'),

('INC000005-WORKLOG-0', generate_random_embedding(),
'Work Log Entry [2024-01-18 14:00 - Desktop Support]: Visited user desk for diagnosis. Collected crash dump files. Initial analysis suggests storage subsystem issue. Will run hardware diagnostics during lunch break when user can leave machine.',
'Incident', 'INC000005', 'ENT000000000005', 'WORK_LOG', 1,
'{"assigned_group": "Desktop Support", "submitter": "techsupport@acme.com"}');

-- INC000006: Software Installation Request
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('INC000006-SUMMARY-0', generate_random_embedding(),
'Incident INC000006: Request to install Adobe Acrobat Pro DC. Category: Software/Installation/Adobe. Priority: Low. Marketing department user needs Adobe Acrobat Pro DC for editing PDF files and creating fillable forms. Current Adobe Reader cannot edit PDFs. Manager approval attached.',
'Incident', 'INC000006', 'ENT000000000006', 'SUMMARY', 0,
'{"assigned_group": "Desktop Support", "title": "Install Adobe Acrobat Pro DC", "category": "Software/Installation/Adobe", "status": "Closed", "priority": "Low"}'),

('INC000006-RESOLUTION-0', generate_random_embedding(),
'Resolution for INC000006: Adobe Acrobat Pro DC installed successfully. Steps: 1) Verified manager approval and license availability 2) Connected remotely via SCCM 3) Deployed Adobe Acrobat Pro DC 2024 from software catalog 4) Removed Adobe Reader to avoid conflicts 5) Activated license using enterprise licensing server 6) Verified user can edit PDFs and create forms. User trained on basic features.',
'Incident', 'INC000006', 'ENT000000000006', 'RESOLUTION', 0,
'{"assigned_group": "Desktop Support", "title": "Install Adobe Acrobat Pro DC", "category": "Software/Installation/Adobe", "status": "Closed"}');

-- INC000007: Network Drive Not Accessible
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('INC000007-SUMMARY-0', generate_random_embedding(),
'Incident INC000007: Cannot access shared network drive S:. Category: Infrastructure/Storage/Network Share. Priority: High. Entire Accounting team (15 users) cannot access the S: drive mapped to \\\\FILESRV01\\Accounting. Error: "Windows cannot access \\\\FILESRV01\\Accounting. You do not have permission." Was working yesterday.',
'Incident', 'INC000007', 'ENT000000000007', 'SUMMARY', 0,
'{"assigned_group": "Server Team", "title": "Network drive S: not accessible", "category": "Infrastructure/Storage/Network Share", "status": "Closed", "priority": "High", "impact": "Department"}'),

('INC000007-RESOLUTION-0', generate_random_embedding(),
'Resolution for INC000007: AD group membership restored after accidental deletion. Root cause: During AD cleanup activity, the "Accounting-FileShare-RW" security group was accidentally deleted. This group was nested in the share permissions. Fix: 1) Recreated "Accounting-FileShare-RW" group with same GID 2) Re-added all Accounting department users 3) Verified share and NTFS permissions unchanged 4) Users can now access drive after logging off/on to refresh tokens. Added group to protected objects list to prevent future deletion.',
'Incident', 'INC000007', 'ENT000000000007', 'RESOLUTION', 0,
'{"assigned_group": "Server Team", "title": "Network drive S: not accessible", "category": "Infrastructure/Storage/Network Share", "status": "Closed"}');

-- INC000008: Two-Factor Authentication Not Working
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('INC000008-SUMMARY-0', generate_random_embedding(),
'Incident INC000008: MFA codes not being accepted for Microsoft 365 login. Category: Security/Authentication/MFA. Priority: High. Executive user unable to login to M365 applications. Microsoft Authenticator app shows codes but they are rejected as invalid. User recently got new phone and restored from backup.',
'Incident', 'INC000008', 'ENT000000000008', 'SUMMARY', 0,
'{"assigned_group": "Identity Management", "title": "MFA codes not working after phone change", "category": "Security/Authentication/MFA", "status": "Closed", "priority": "High"}'),

('INC000008-RESOLUTION-0', generate_random_embedding(),
'Resolution for INC000008: MFA registration reset and re-enrolled. Issue: When user restored backup to new phone, the Authenticator app copied over but the cryptographic keys were not valid. TOTP authentication requires the secret key to be properly enrolled, not just app data. Steps: 1) Verified user identity through video call (executive protocol) 2) Reset MFA registration in Azure AD admin portal 3) Guided user through fresh Authenticator app setup 4) Added phone number as backup MFA method 5) Tested all M365 applications successfully. Provided documentation on proper phone migration procedure.',
'Incident', 'INC000008', 'ENT000000000008', 'RESOLUTION', 0,
'{"assigned_group": "Identity Management", "title": "MFA codes not working after phone change", "category": "Security/Authentication/MFA", "status": "Closed"}');

-- INC000009: Slow Computer Performance
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('INC000009-SUMMARY-0', generate_random_embedding(),
'Incident INC000009: Computer extremely slow - takes 15 minutes to boot. Category: Hardware/Desktop/Performance. Priority: Medium. User reports workstation has become progressively slower over past month. Boot time increased from 2 minutes to 15 minutes. Applications freeze frequently. Machine is Dell Latitude 5520 laptop, 3 years old.',
'Incident', 'INC000009', 'ENT000000000009', 'SUMMARY', 0,
'{"assigned_group": "Desktop Support", "title": "Slow computer - long boot time", "category": "Hardware/Desktop/Performance", "status": "Closed", "priority": "Medium"}'),

('INC000009-RESOLUTION-0', generate_random_embedding(),
'Resolution for INC000009: Multiple issues found and resolved. Diagnostics: 1) Disk 98% full - cleaned up 45GB of temp files and old downloads 2) 47 Chrome extensions installed - removed unnecessary ones 3) 15 programs in startup - disabled non-essential 4) Antivirus scan found 3 PUPs (potentially unwanted programs) - removed 5) Windows updates pending for 6 months - installed all updates 6) Defragmented HDD and optimized page file. Boot time now under 3 minutes. Recommended SSD upgrade for further improvement - user to discuss with manager.',
'Incident', 'INC000009', 'ENT000000000009', 'RESOLUTION', 0,
'{"assigned_group": "Desktop Support", "title": "Slow computer - long boot time", "category": "Hardware/Desktop/Performance", "status": "Closed"}');

-- INC000010: Database Connection Error in Application
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('INC000010-SUMMARY-0', generate_random_embedding(),
'Incident INC000010: SAP application showing database connection error. Category: Application/SAP/Database. Priority: Critical. SAP ERP system inaccessible for all users. Error: "Database connection failed - ORA-12541: TNS:no listener". Finance team cannot process month-end closing. Approximately 200 users affected.',
'Incident', 'INC000010', 'ENT000000000010', 'SUMMARY', 0,
'{"assigned_group": "Database Administration", "title": "SAP database connection failure", "category": "Application/SAP/Database", "status": "Closed", "priority": "Critical", "impact": "Enterprise"}'),

('INC000010-RESOLUTION-0', generate_random_embedding(),
'Resolution for INC000010: Oracle listener service restarted after memory exhaustion. Root cause: Overnight batch job consumed all available memory on database server causing Oracle listener process to be killed by OOM killer. Steps: 1) Verified database server ORCL-PROD01 responsive via SSH 2) Checked listener status - not running 3) Reviewed /var/log/messages - OOM killer terminated tnslsnr 4) Started listener: lsnrctl start 5) Verified all SAP services reconnected 6) Tested transactions with Finance team. Prevention: Increased server RAM from 64GB to 128GB approved, scheduled for next maintenance window. Added memory monitoring alerts.',
'Incident', 'INC000010', 'ENT000000000010', 'RESOLUTION', 0,
'{"assigned_group": "Database Administration", "title": "SAP database connection failure", "category": "Application/SAP/Database", "status": "Closed"}'),

('INC000010-WORKLOG-0', generate_random_embedding(),
'Work Log Entry [2024-01-20 06:00 - NOC]: P1 alert received. SAP monitoring showing all application servers unable to connect to database. Paging DBA team.',
'Incident', 'INC000010', 'ENT000000000010', 'WORK_LOG', 1,
'{"assigned_group": "Database Administration", "submitter": "noc@acme.com"}'),

('INC000010-WORKLOG-1', generate_random_embedding(),
'Work Log Entry [2024-01-20 06:15 - DBA Team]: Confirmed listener process not running. Server responsive. Investigating cause before restart to understand root cause.',
'Incident', 'INC000010', 'ENT000000000010', 'WORK_LOG', 2,
'{"assigned_group": "Database Administration", "submitter": "dba@acme.com"}');

-- INC000011: Zoom Meeting Audio Issues
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('INC000011-SUMMARY-0', generate_random_embedding(),
'Incident INC000011: No audio in Zoom meetings - cannot hear or be heard. Category: Software/Collaboration/Zoom. Priority: Medium. User reports audio not working in Zoom meetings for past 2 days. Other participants cannot hear user, and user cannot hear them. Headset works fine in other applications like Spotify and Teams.',
'Incident', 'INC000011', 'ENT000000000011', 'SUMMARY', 0,
'{"assigned_group": "Desktop Support", "title": "Zoom audio not working", "category": "Software/Collaboration/Zoom", "status": "Closed", "priority": "Medium"}'),

('INC000011-RESOLUTION-0', generate_random_embedding(),
'Resolution for INC000011: Zoom audio device settings corrected. Issue: Zoom was configured to use a disconnected Bluetooth headset that was previously paired. Steps: 1) Opened Zoom Settings > Audio 2) Found microphone set to "Jabra Evolve2" (device not connected) 3) Changed microphone and speaker to "Logitech USB Headset" (current device) 4) Tested audio - working both directions 5) Enabled "Automatically join audio" with correct device. User advised to check audio settings if switching between headsets.',
'Incident', 'INC000011', 'ENT000000000011', 'RESOLUTION', 0,
'{"assigned_group": "Desktop Support", "title": "Zoom audio not working", "category": "Software/Collaboration/Zoom", "status": "Closed"}');

-- INC000012: Account Locked Out Repeatedly
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('INC000012-SUMMARY-0', generate_random_embedding(),
'Incident INC000012: User account keeps getting locked out every few hours. Category: Security/Account/Lockout. Priority: High. User reports AD account locks every 2-3 hours even after password reset. Productivity severely impacted. User has to call helpdesk multiple times daily for unlock.',
'Incident', 'INC000012', 'ENT000000000012', 'SUMMARY', 0,
'{"assigned_group": "Identity Management", "title": "Repeated account lockouts", "category": "Security/Account/Lockout", "status": "Closed", "priority": "High"}'),

('INC000012-RESOLUTION-0', generate_random_embedding(),
'Resolution for INC000012: Stale credentials on mobile device causing lockouts. Investigation: 1) Used Lockout Status tool to trace lockout source 2) Lockouts originating from Exchange ActiveSync 3) User has old iPhone configured with corporate email 4) Phone was in drawer using old password, attempting sync every 15 minutes 5) After 5 failed attempts, account locks. Fix: Removed old device from Exchange ActiveSync in EAC, performed remote wipe authorization. Current phone re-enrolled with correct password. No lockouts for 48 hours. User reminded to remove corporate accounts before disposing of devices.',
'Incident', 'INC000012', 'ENT000000000012', 'RESOLUTION', 0,
'{"assigned_group": "Identity Management", "title": "Repeated account lockouts", "category": "Security/Account/Lockout", "status": "Closed"}');

-- INC000013: Wi-Fi Connection Dropping
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('INC000013-SUMMARY-0', generate_random_embedding(),
'Incident INC000013: Laptop WiFi disconnects every 30 minutes. Category: Network/Wireless/Connectivity. Priority: Medium. User laptop keeps dropping WiFi connection randomly throughout the day. Has to manually reconnect. Happens in conference rooms and at desk. Other users on same network not affected.',
'Incident', 'INC000013', 'ENT000000000013', 'SUMMARY', 0,
'{"assigned_group": "Network Operations", "title": "WiFi dropping intermittently", "category": "Network/Wireless/Connectivity", "status": "Closed", "priority": "Medium"}'),

('INC000013-RESOLUTION-0', generate_random_embedding(),
'Resolution for INC000013: WiFi adapter power management disabled. Root cause: Windows power settings were turning off WiFi adapter to save power. Steps: 1) Device Manager > Network Adapters > Intel WiFi 6 AX201 2) Properties > Power Management 3) Unchecked "Allow the computer to turn off this device to save power" 4) Also adjusted Advanced settings: Roaming Aggressiveness to Highest 5) Updated WiFi driver to latest version from Intel. Connection stable for 3 days after fix. Added to standard laptop build configuration.',
'Incident', 'INC000013', 'ENT000000000013', 'RESOLUTION', 0,
'{"assigned_group": "Network Operations", "title": "WiFi dropping intermittently", "category": "Network/Wireless/Connectivity", "status": "Closed"}');

-- INC000014: Unable to Access Internet
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('INC000014-SUMMARY-0', generate_random_embedding(),
'Incident INC000014: No internet access on workstation - internal sites work. Category: Network/Internet/Proxy. Priority: Medium. User can access internal SharePoint and intranet but cannot browse external websites. Chrome shows "ERR_PROXY_CONNECTION_FAILED". New employee who started this week.',
'Incident', 'INC000014', 'ENT000000000014', 'SUMMARY', 0,
'{"assigned_group": "Network Operations", "title": "No internet - proxy error", "category": "Network/Internet/Proxy", "status": "Closed", "priority": "Medium"}'),

('INC000014-RESOLUTION-0', generate_random_embedding(),
'Resolution for INC000014: User added to proxy authentication group. Issue: New employees are not automatically added to the BlueCoat proxy authentication group. HR onboarding ticket was created but AD provisioning step was missed. Steps: 1) Confirmed user not in "Internet-Users-Proxy" group 2) Added user to group in AD 3) User logged off and on to refresh group membership 4) Cleared browser cache 5) Internet now working. Process improvement: Added automatic group membership to new employee provisioning script.',
'Incident', 'INC000014', 'ENT000000000014', 'RESOLUTION', 0,
'{"assigned_group": "Network Operations", "title": "No internet - proxy error", "category": "Network/Internet/Proxy", "status": "Closed"}');

-- INC000015: Laptop Docking Station Not Working
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('INC000015-SUMMARY-0', generate_random_embedding(),
'Incident INC000015: Dell docking station not recognizing monitors. Category: Hardware/Peripheral/Docking Station. Priority: Medium. User laptop not outputting to dual monitors when connected to Dell WD19 docking station. Charging works, but no display. Monitors show "No Signal". Was working until Windows update yesterday.',
'Incident', 'INC000015', 'ENT000000000015', 'SUMMARY', 0,
'{"assigned_group": "Desktop Support", "title": "Docking station monitors not working", "category": "Hardware/Peripheral/Docking Station", "status": "Closed", "priority": "Medium"}'),

('INC000015-RESOLUTION-0', generate_random_embedding(),
'Resolution for INC000015: DisplayLink driver update resolved the issue. Investigation: 1) Docking station firmware up to date 2) Checked Windows Update history - KB5034441 installed yesterday 3) This update known to cause DisplayLink compatibility issues 4) Downloaded latest DisplayLink driver (11.4) from Dell support 5) Installed driver and rebooted 6) Both monitors now detected and working at correct resolution 7) Extended display confirmed working. Added to known issues knowledge base for recent Windows update.',
'Incident', 'INC000015', 'ENT000000000015', 'RESOLUTION', 0,
'{"assigned_group": "Desktop Support", "title": "Docking station monitors not working", "category": "Hardware/Peripheral/Docking Station", "status": "Closed"}');

-- =============================================================================
-- WORK ORDER DATA
-- IT Service requests and standard changes
-- =============================================================================

-- WO000001: New Employee Setup
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('WO000001-SUMMARY-0', generate_random_embedding(),
'Work Order WO000001: New employee onboarding - IT setup required. Category: Access Management/New Hire/Standard. Priority: Medium. New hire starting 2024-02-01 in Marketing department. Manager: Sarah Johnson. Position: Marketing Analyst. Requires standard laptop, Microsoft 365, Adobe Creative Suite, Salesforce access, and building badge.',
'WorkOrder', 'WO000001', 'ENT000000100001', 'SUMMARY', 0,
'{"assigned_group": "Desktop Support", "title": "New hire IT setup - Marketing Analyst", "category": "Access Management/New Hire/Standard", "status": "Closed", "priority": "Medium"}'),

('WO000001-DESCRIPTION-0', generate_random_embedding(),
'New employee information: Name: Michael Chen. Start Date: February 1, 2024. Department: Marketing. Location: HQ Building A, Floor 3. Manager: Sarah Johnson. Equipment needs: Dell Latitude 5540 laptop, 27-inch monitor, wireless keyboard and mouse. Software: Microsoft 365, Adobe Creative Suite (Photoshop, Illustrator, InDesign), Salesforce CRM. Access: Building badge, parking, VPN, department shared drives.',
'WorkOrder', 'WO000001', 'ENT000000100001', 'DESCRIPTION', 0,
'{"assigned_group": "Desktop Support", "title": "New hire IT setup - Marketing Analyst", "status": "Closed"}'),

('WO000001-WORKLOG-0', generate_random_embedding(),
'Work Log Entry [2024-01-25 - Desktop Support]: AD account created: mchen@acme.com. Added to Marketing-Users and VPN-Users groups. Laptop WS-MKT-0892 imaged and configured. Software installed per request. Ready for pickup from IT window.',
'WorkOrder', 'WO000001', 'ENT000000100001', 'WORK_LOG', 1,
'{"assigned_group": "Desktop Support", "submitter": "itsetup@acme.com"}');

-- WO000002: Software License Request
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('WO000002-SUMMARY-0', generate_random_embedding(),
'Work Order WO000002: Request for Microsoft Visio Professional license. Category: Software/License/Microsoft. Priority: Low. Solution Architect needs Visio Professional for creating technical architecture diagrams. Standard Visio Viewer insufficient for editing. Business justification and manager approval provided.',
'WorkOrder', 'WO000002', 'ENT000000100002', 'SUMMARY', 0,
'{"assigned_group": "Software Asset Management", "title": "Visio Professional license request", "category": "Software/License/Microsoft", "status": "Closed", "priority": "Low"}'),

('WO000002-WORKLOG-0', generate_random_embedding(),
'Work Log Entry [2024-01-22 - SAM Team]: License availability confirmed. Assigned license from volume agreement pool. Installed via Software Center push. User confirmed Visio Professional 2021 working. License tracked in ServiceNow CMDB.',
'WorkOrder', 'WO000002', 'ENT000000100002', 'WORK_LOG', 1,
'{"assigned_group": "Software Asset Management", "submitter": "sam@acme.com"}');

-- WO000003: Meeting Room AV Setup
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('WO000003-SUMMARY-0', generate_random_embedding(),
'Work Order WO000003: Configure video conferencing in new meeting room. Category: Infrastructure/AV/Conference Room. Priority: Medium. New meeting room "Innovation Lab" on Floor 5 needs full video conferencing setup. Room capacity: 20 people. Required: 85-inch display, Zoom Room configuration, wireless presentation, microphone array.',
'WorkOrder', 'WO000003', 'ENT000000100003', 'SUMMARY', 0,
'{"assigned_group": "AV Support", "title": "Meeting room AV configuration", "category": "Infrastructure/AV/Conference Room", "status": "Closed", "priority": "Medium"}'),

('WO000003-DESCRIPTION-0', generate_random_embedding(),
'Innovation Lab meeting room requirements: Primary display - 85-inch Samsung commercial display. Zoom Room controller - iPad with scheduling panel. Camera - Poly Studio X70 with speaker tracking. Audio - ceiling microphone array for full room coverage. Wireless presentation - Barco ClickShare. Cable management and wall mounting. Integration with room booking system.',
'WorkOrder', 'WO000003', 'ENT000000100003', 'DESCRIPTION', 0,
'{"assigned_group": "AV Support", "title": "Meeting room AV configuration", "status": "Closed"}');

-- WO000004: Security Access Request
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('WO000004-SUMMARY-0', generate_random_embedding(),
'Work Order WO000004: Request access to Production Database environment. Category: Access Management/Database/Production. Priority: Medium. DBA team member requires read-only access to Production Oracle databases for performance monitoring and troubleshooting. Security approval form attached.',
'WorkOrder', 'WO000004', 'ENT000000100004', 'SUMMARY', 0,
'{"assigned_group": "Database Administration", "title": "Production database access request", "category": "Access Management/Database/Production", "status": "Closed", "priority": "Medium"}'),

('WO000004-WORKLOG-0', generate_random_embedding(),
'Work Log Entry [2024-01-18 - DBA Team]: Security approval verified. Created Oracle account with SELECT_CATALOG_ROLE and monitoring privileges only. No DML/DDL access. Account: JSMITH_READONLY. Password provided via encrypted channel. Access logged and audited per policy.',
'WorkOrder', 'WO000004', 'ENT000000100004', 'WORK_LOG', 1,
'{"assigned_group": "Database Administration", "submitter": "dba@acme.com"}');

-- WO000005: Laptop Refresh
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('WO000005-SUMMARY-0', generate_random_embedding(),
'Work Order WO000005: Standard laptop refresh - 4 year old device. Category: Hardware/Laptop/Refresh. Priority: Low. User current laptop (Dell Latitude 5410) is 4 years old and eligible for standard refresh. Performance degradation reported. Requesting new Dell Latitude 5550 per department standard.',
'WorkOrder', 'WO000005', 'ENT000000100005', 'SUMMARY', 0,
'{"assigned_group": "Desktop Support", "title": "Laptop refresh request", "category": "Hardware/Laptop/Refresh", "status": "Closed", "priority": "Low"}'),

('WO000005-WORKLOG-0', generate_random_embedding(),
'Work Log Entry [2024-01-28 - Desktop Support]: Refresh approved based on 4-year policy. New Dell Latitude 5550 (16GB RAM, 512GB SSD) assigned. Data migrated using USMT. Old device wiped and sent to e-waste recycling. Asset records updated in CMDB.',
'WorkOrder', 'WO000005', 'ENT000000100005', 'WORK_LOG', 1,
'{"assigned_group": "Desktop Support", "submitter": "itsetup@acme.com"}');

-- =============================================================================
-- KNOWLEDGE ARTICLE DATA
-- IT Support knowledge base articles
-- =============================================================================

-- KB0001: Password Reset Procedure
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('KB0001-CONTENT-0', generate_random_embedding(),
'Knowledge Article KB0001: How to Reset Your Windows Password. This article explains the self-service password reset process for ACME Corp employees.

PREREQUISITES:
- You must have registered for Self-Service Password Reset (SSPR) at https://aka.ms/ssprsetup
- You need access to your registered phone or authentication app

STEPS TO RESET YOUR PASSWORD:
1. Go to https://passwordreset.microsoftonline.com
2. Enter your work email address (username@acme.com)
3. Complete the CAPTCHA verification
4. Choose your verification method (text message, phone call, or authenticator app)
5. Enter the verification code received
6. Create a new password following these requirements:
   - Minimum 12 characters
   - At least one uppercase letter
   - At least one lowercase letter
   - At least one number
   - At least one special character (!@#$%^&*)
   - Cannot contain your name or username
   - Cannot be one of your last 12 passwords
7. Confirm your new password
8. Click Submit

AFTER RESETTING:
- Your new password will work immediately for web applications
- You may need to update saved passwords in Outlook, Teams, and other desktop apps
- If using VPN, disconnect and reconnect with new credentials',
'KnowledgeArticle', 'KB0001', 'ENT000000200001', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Service Desk", "title": "How to Reset Your Windows Password", "category": "Security/Password/Self-Service", "status": "Published", "keywords": "password reset, forgot password, change password, SSPR"}'),

('KB0001-CONTENT-1', generate_random_embedding(),
'TROUBLESHOOTING PASSWORD RESET ISSUES:

If you cannot complete self-service reset:
- "Account not found": Verify you are using your full email address
- "SSPR not enabled": Contact Service Desk to register for SSPR
- "Verification failed": Ensure your phone number is current in your profile
- "Password does not meet requirements": Review the complexity rules above

IF ACCOUNT IS LOCKED:
Account lockout occurs after 5 failed password attempts. Wait 30 minutes for automatic unlock, or contact Service Desk for immediate unlock.

CONTACT INFORMATION:
Service Desk: 1-800-555-HELP (4357)
Email: servicedesk@acme.com
Chat: Available on intranet homepage
Hours: 24/7 for password resets

Related Articles:
- KB0002: How to Register for Self-Service Password Reset
- KB0015: Account Lockout Policy Explained
- KB0023: Setting Up Microsoft Authenticator',
'KnowledgeArticle', 'KB0001', 'ENT000000200001', 'ARTICLE_CONTENT', 1,
'{"assigned_group": "Service Desk", "title": "How to Reset Your Windows Password", "category": "Security/Password/Self-Service", "status": "Published"}');

-- KB0002: VPN Setup Guide
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('KB0002-CONTENT-0', generate_random_embedding(),
'Knowledge Article KB0002: Setting Up Cisco AnyConnect VPN for Remote Work. This guide walks you through installing and configuring VPN for secure remote access to corporate resources.

REQUIREMENTS:
- ACME Corp laptop with Windows 10/11 or macOS
- Active network/WiFi connection
- Your network credentials (same as Windows login)
- MFA configured (Microsoft Authenticator)

INSTALLATION (if not pre-installed):
1. Go to https://vpn.acme.com from any browser
2. Login with your ACME credentials
3. Click "Download for Windows" or "Download for macOS"
4. Run the installer and accept default settings
5. Restart your computer when prompted

CONNECTING TO VPN:
1. Open Cisco AnyConnect Secure Mobility Client
2. Enter server address: vpn.acme.com
3. Click Connect
4. Enter your username (without @acme.com)
5. Enter your password
6. Approve MFA push notification on your phone
7. Wait for "Connected" status

SPLIT TUNNEL VS FULL TUNNEL:
- Default is Split Tunnel: Only corporate traffic goes through VPN
- Select "Full Tunnel" profile for accessing restricted resources
- Full Tunnel routes all internet traffic through corporate network',
'KnowledgeArticle', 'KB0002', 'ENT000000200002', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Network Operations", "title": "Setting Up Cisco AnyConnect VPN", "category": "Network/VPN/Setup", "status": "Published", "keywords": "VPN, remote access, AnyConnect, work from home"}'),

('KB0002-CONTENT-1', generate_random_embedding(),
'VPN TROUBLESHOOTING:

Error 691 - Authentication Failed:
- Verify username and password are correct
- Check if account is locked (try password reset)
- Ensure MFA is properly configured

Connection Drops Frequently:
- Check your internet connection stability
- Try Full Tunnel profile
- Disable VPN power-saving in AnyConnect preferences
- Update to latest AnyConnect version

Slow VPN Performance:
- Use Split Tunnel when possible
- Close unnecessary applications
- Check for Windows updates downloading in background
- Try connecting to alternate VPN gateway: vpn2.acme.com

Cannot Access Internal Resources:
- Verify you are connected (green checkmark in AnyConnect)
- Try disconnecting and reconnecting
- Clear DNS cache: ipconfig /flushdns
- Restart browser

FOR MACOS USERS:
- Grant Full Disk Access to AnyConnect in System Preferences > Security
- Allow kernel extension if prompted
- Use vpn.acme.com (not IP address)

SUPPORT:
Network Operations: netops@acme.com
After hours: Service Desk 1-800-555-HELP',
'KnowledgeArticle', 'KB0002', 'ENT000000200002', 'ARTICLE_CONTENT', 1,
'{"assigned_group": "Network Operations", "title": "Setting Up Cisco AnyConnect VPN", "category": "Network/VPN/Setup", "status": "Published"}');

-- KB0003: Outlook Configuration
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('KB0003-CONTENT-0', generate_random_embedding(),
'Knowledge Article KB0003: Configuring Microsoft Outlook for ACME Corp Email. This article covers setting up Outlook on Windows, Mac, and mobile devices.

OUTLOOK ON WINDOWS:
1. Open Outlook (pre-installed on corporate laptops)
2. If prompted, enter your email: username@acme.com
3. Click Connect
4. Sign in with your ACME credentials
5. Complete MFA verification
6. Outlook will auto-configure Exchange Online settings
7. Wait for initial sync (may take 15-30 minutes for large mailboxes)

OUTLOOK ON MAC:
1. Download Outlook from Self Service or App Store
2. Open Outlook and click "Add Account"
3. Enter your ACME email address
4. Select "Microsoft 365" when prompted
5. Sign in and complete MFA
6. Allow Outlook access in System Preferences if prompted

MOBILE (iOS/Android):
1. Download "Outlook" app from App Store/Play Store
2. Open app and tap "Add Account"
3. Enter your ACME email
4. Sign in via Microsoft login page
5. Complete MFA
6. Allow notifications for new email alerts

SHARED MAILBOXES:
To add a shared mailbox:
1. File > Account Settings > Account Settings
2. Double-click your account
3. Click "More Settings" > "Advanced"
4. Click "Add" under "Open these additional mailboxes"
5. Enter the shared mailbox name
6. Click OK and restart Outlook',
'KnowledgeArticle', 'KB0003', 'ENT000000200003', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Desktop Support", "title": "Configuring Microsoft Outlook", "category": "Software/Email/Outlook", "status": "Published", "keywords": "Outlook, email, Exchange, configuration, setup"}');

-- KB0004: Printer Installation
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('KB0004-CONTENT-0', generate_random_embedding(),
'Knowledge Article KB0004: How to Add Network Printers. This guide explains how to connect to department printers at ACME Corp.

FINDING YOUR PRINTER:
Printers are named by location: [Building]-[Floor]-[Type]-[Number]
Example: HQ-3-COLOR-01 = Headquarters, Floor 3, Color printer, Unit 1

ADDING A PRINTER (WINDOWS):
1. Click Start > Settings > Devices > Printers & scanners
2. Click "Add a printer or scanner"
3. Wait for scan to complete
4. If printer not found, click "The printer I want isnt listed"
5. Select "Select a shared printer by name"
6. Enter: \\\\printserver.acme.local\\[PrinterName]
7. Click Next and wait for drivers to install
8. Print a test page to verify

COMMON PRINTER LOCATIONS:
- HQ-3-BW-01: Headquarters 3rd floor, black/white (near elevator)
- HQ-3-COLOR-01: Headquarters 3rd floor, color (near kitchen)
- HQ-4-MFP-01: Headquarters 4th floor, multifunction (copy/scan/fax)

SECURE PRINT (FOLLOW-ME PRINTING):
1. Print document and select "Secure Print" printer
2. Walk to any MFP printer
3. Badge in with your employee card
4. Select your documents to print
5. Documents print and are automatically deleted from queue

TROUBLESHOOTING:
- "Access Denied": Contact Service Desk for printer permissions
- "Driver not found": Try Windows Update or contact Desktop Support
- Print jobs stuck: See KB0022 for clearing print queue',
'KnowledgeArticle', 'KB0004', 'ENT000000200004', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Desktop Support", "title": "How to Add Network Printers", "category": "Hardware/Printer/Setup", "status": "Published", "keywords": "printer, printing, network printer, add printer"}');

-- KB0005: Teams Meeting Best Practices
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('KB0005-CONTENT-0', generate_random_embedding(),
'Knowledge Article KB0005: Microsoft Teams Meeting Best Practices and Troubleshooting. Tips for successful virtual meetings at ACME Corp.

BEFORE YOUR MEETING:
- Test audio/video: Click your profile > Settings > Devices
- Check internet connection (minimum 2 Mbps for video)
- Close unnecessary applications to free up bandwidth
- Use headset for best audio quality
- Find a quiet location with good lighting

STARTING/JOINING A MEETING:
- Join 2-3 minutes early to resolve any technical issues
- Use "Join" button in calendar invitation or Teams calendar
- Select audio/video settings before joining
- Share meeting links only with intended participants

DURING THE MEETING:
- Mute when not speaking to reduce background noise
- Use "Raise Hand" feature instead of interrupting
- Share specific window, not entire screen, when presenting
- Use meeting chat for questions and links
- Record only with participant consent

SCREEN SHARING TIPS:
- Close sensitive applications before sharing
- Use "Window" sharing instead of "Desktop" when possible
- Enable "Include computer sound" for video playback
- Use PowerPoint Live for interactive presentations

COMMON ISSUES AND FIXES:
- No audio: Check Teams device settings, ensure correct device selected
- Echo/feedback: Use headphones, only one device per room
- Video freezing: Reduce video quality in settings, close other apps
- Cannot share screen: Update Teams, check if admin has restricted sharing
- "Meeting full": Maximum 1000 participants for standard meetings',
'KnowledgeArticle', 'KB0005', 'ENT000000200005', 'ARTICLE_CONTENT', 0,
'{"assigned_group": "Desktop Support", "title": "Teams Meeting Best Practices", "category": "Software/Collaboration/Teams", "status": "Published", "keywords": "Teams, meeting, video conference, screen share"}');

-- =============================================================================
-- CHANGE REQUEST DATA
-- Infrastructure and application changes
-- =============================================================================

-- CHG000001: Server Patching
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('CHG000001-SUMMARY-0', generate_random_embedding(),
'Change Request CHG000001: Monthly Windows Server Security Patching - February 2024. Category: Infrastructure/Server/Patching. Risk: Medium. Schedule: February 10, 2024 02:00-06:00 EST. Affects: All Windows servers in Production environment (47 servers). Expected downtime: Rolling restarts, 5-10 minutes per server.',
'ChangeRequest', 'CHG000001', 'ENT000000300001', 'SUMMARY', 0,
'{"assigned_group": "Server Team", "title": "Monthly Windows Server Patching", "category": "Infrastructure/Server/Patching", "status": "Closed", "risk_level": "Medium"}'),

('CHG000001-IMPLEMENTATION-0', generate_random_embedding(),
'Implementation Plan for CHG000001:

PRE-CHANGE:
1. Verify all server backups completed successfully (previous night)
2. Confirm change window with application owners
3. Notify NOC and Service Desk of maintenance window
4. Test patches in Dev/UAT environment (completed 02/03)

CHANGE EXECUTION:
1. Begin with non-critical servers (file servers, print servers)
2. Apply patches using WSUS targeting by server groups
3. Monitor each group for 15 minutes before proceeding
4. Patch database servers with application team standby
5. Patch domain controllers last, one at a time
6. Verify AD replication after each DC restart

POST-CHANGE:
1. Run server health check script on all patched servers
2. Verify critical services running (SQL, IIS, Exchange)
3. Check Windows Event logs for errors
4. Confirm application availability with monitoring tools
5. Send completion notification to stakeholders',
'ChangeRequest', 'CHG000001', 'ENT000000300001', 'IMPLEMENTATION', 0,
'{"assigned_group": "Server Team", "title": "Monthly Windows Server Patching", "status": "Closed"}'),

('CHG000001-ROLLBACK-0', generate_random_embedding(),
'Rollback Plan for CHG000001:

ROLLBACK TRIGGERS:
- Critical application failure after patching
- Active Directory replication failure
- More than 5 servers failing health checks
- Change manager decision based on business impact

ROLLBACK PROCEDURE:
1. Stop further patch deployment immediately
2. Identify affected servers from monitoring alerts
3. For individual servers:
   a. Boot to Safe Mode
   b. Uninstall recent Windows Updates via DISM
   c. Or restore from most recent backup snapshot
4. For widespread issues:
   a. Invoke disaster recovery procedures
   b. Restore critical servers from backup in priority order
5. Verify services restored and functioning
6. Root cause analysis and incident creation

ROLLBACK TIME ESTIMATE:
- Single server: 30-45 minutes
- Full environment: 4-6 hours

CONTACTS:
- Server Team Lead: John Smith (555-0101)
- Change Manager: Mary Johnson (555-0102)
- NOC: noc@acme.com / 555-0199',
'ChangeRequest', 'CHG000001', 'ENT000000300001', 'ROLLBACK', 0,
'{"assigned_group": "Server Team", "title": "Monthly Windows Server Patching", "status": "Closed"}');

-- CHG000002: Network Switch Upgrade
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('CHG000002-SUMMARY-0', generate_random_embedding(),
'Change Request CHG000002: Replace core switch stack in Data Center A. Category: Infrastructure/Network/Hardware. Risk: High. Schedule: February 24, 2024 23:00 - February 25, 2024 03:00 EST. Impact: Complete network outage for Data Center A during cutover (estimated 30 minutes). New Cisco Catalyst 9500 stack replacing aging Catalyst 6500.',
'ChangeRequest', 'CHG000002', 'ENT000000300002', 'SUMMARY', 0,
'{"assigned_group": "Network Operations", "title": "Core Switch Replacement - DC-A", "category": "Infrastructure/Network/Hardware", "status": "Closed", "risk_level": "High"}'),

('CHG000002-IMPLEMENTATION-0', generate_random_embedding(),
'Implementation Plan for CHG000002:

PRE-CHANGE (Week before):
1. Stage new Cisco 9500 switches in lab
2. Pre-configure all VLANs, routing, ACLs from existing config
3. Test configuration with sample traffic
4. Coordinate with facilities for rack space and power
5. Schedule applications for planned failover to DR

CHANGE EXECUTION:
Phase 1 - Preparation (23:00-23:30):
1. Final backup of existing switch configuration
2. Notify all stakeholders of change start
3. Failover critical applications to DR site
4. Verify DR operations stable

Phase 2 - Hardware Swap (23:30-01:00):
1. Gracefully shut down existing switch ports
2. Power down old Catalyst 6500
3. Physically remove old switches
4. Install new Catalyst 9500 stack
5. Connect fiber uplinks and server ports
6. Power on new switches

Phase 3 - Validation (01:00-02:30):
1. Verify spanning tree convergence
2. Test VLAN connectivity from each access switch
3. Validate routing adjacencies (OSPF, BGP)
4. Test server connectivity and application access
5. Verify monitoring systems see new devices

Phase 4 - Completion (02:30-03:00):
1. Failback applications from DR
2. Final validation with application owners
3. Send completion notification',
'ChangeRequest', 'CHG000002', 'ENT000000300002', 'IMPLEMENTATION', 0,
'{"assigned_group": "Network Operations", "title": "Core Switch Replacement - DC-A", "status": "Closed"}'),

('CHG000002-ROLLBACK-0', generate_random_embedding(),
'Rollback Plan for CHG000002:

ROLLBACK DECISION POINT:
If new switches not operational within 2 hours of installation, rollback will be invoked.

ROLLBACK PROCEDURE:
1. Power down new Catalyst 9500 stack
2. Remove new switches from rack
3. Reinstall old Catalyst 6500 (kept on-site during change)
4. Reconnect original cabling (labeled and documented)
5. Power on old switches
6. Verify configuration and connectivity restored
7. Failback applications from DR

ROLLBACK CONSIDERATIONS:
- Old switches must remain in data center during change window
- Original cables labeled and organized for quick reconnection
- Configuration backups accessible offline
- DR site must remain operational throughout change window

TIME TO ROLLBACK: Approximately 90 minutes

ESCALATION:
- Network Director: Bob Wilson (555-0201)
- CTO (for business decision): Jane Adams (555-0301)',
'ChangeRequest', 'CHG000002', 'ENT000000300002', 'ROLLBACK', 0,
'{"assigned_group": "Network Operations", "title": "Core Switch Replacement - DC-A", "status": "Closed"}');

-- CHG000003: Application Deployment
INSERT INTO embedding_store (chunk_id, embedding, text_segment, source_type, source_id, entry_id, chunk_type, sequence_number, metadata)
VALUES
('CHG000003-SUMMARY-0', generate_random_embedding(),
'Change Request CHG000003: Deploy CRM Application version 4.2.0 to Production. Category: Application/CRM/Deployment. Risk: Medium. Schedule: February 15, 2024 18:00-22:00 EST. Impact: CRM unavailable during deployment (estimated 2 hours). New features: Enhanced reporting, Salesforce integration, mobile improvements.',
'ChangeRequest', 'CHG000003', 'ENT000000300003', 'SUMMARY', 0,
'{"assigned_group": "Application Support", "title": "CRM v4.2.0 Production Deployment", "category": "Application/CRM/Deployment", "status": "Closed", "risk_level": "Medium"}'),

('CHG000003-IMPLEMENTATION-0', generate_random_embedding(),
'Implementation Plan for CHG000003:

PRE-DEPLOYMENT:
1. UAT sign-off completed and documented
2. Database backup scheduled for 17:30
3. Application team on standby
4. Rollback package prepared and tested

DEPLOYMENT STEPS:
1. 18:00 - Post maintenance banner on CRM login page
2. 18:15 - Stop CRM application servers (4 nodes)
3. 18:30 - Run database migration scripts (reviewed by DBA)
4. 19:00 - Deploy application WAR files to all nodes
5. 19:30 - Update application configuration files
6. 19:45 - Start application servers sequentially
7. 20:00 - Smoke test core functionality
8. 20:30 - Load test with synthetic transactions
9. 21:00 - UAT validation by business users
10. 21:30 - Remove maintenance banner
11. 22:00 - Monitor for 30 minutes before close',
'ChangeRequest', 'CHG000003', 'ENT000000300003', 'IMPLEMENTATION', 0,
'{"assigned_group": "Application Support", "title": "CRM v4.2.0 Production Deployment", "status": "Closed"}');

-- =============================================================================
-- Update sync_state to reflect dummy data has been loaded
-- =============================================================================
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
-- Create some chat memory entries for testing
-- =============================================================================
INSERT INTO chat_memory (session_id, message_type, content, metadata)
VALUES
('test-session-001', 'USER', 'How do I reset my password?', '{"user_id": "testuser1", "groups": ["Service Desk"]}'),
('test-session-001', 'AI', 'You can reset your password using the self-service portal at https://passwordreset.microsoftonline.com. You will need to verify your identity through MFA. If you need help, contact the Service Desk at 1-800-555-HELP.', '{"sources": ["KB0001"], "confidence": 0.92}'),
('test-session-002', 'USER', 'VPN is not connecting, error 691', '{"user_id": "testuser2", "groups": ["Network Operations"]}'),
('test-session-002', 'AI', 'Error 691 indicates authentication failure. This is commonly caused by: 1) Incorrect username or password, 2) Account lockout, 3) RADIUS server issues. Based on recent incident INC000002, we had a widespread issue with VPN authentication due to RADIUS connectivity problems. Is your issue happening now or was it earlier today?', '{"sources": ["KB0002", "INC000002"], "confidence": 0.88}');

-- =============================================================================
-- Create sample ingestion job records
-- =============================================================================
INSERT INTO ingestion_job (job_type, source_type, status, started_at, completed_at, records_processed, chunks_created)
VALUES
('FULL', 'Incident', 'completed', CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '1 hour', 15, 35),
('FULL', 'WorkOrder', 'completed', CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '90 minutes', 5, 8),
('FULL', 'KnowledgeArticle', 'completed', CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '85 minutes', 5, 9),
('FULL', 'ChangeRequest', 'completed', CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '80 minutes', 3, 8);

-- =============================================================================
-- Verify data load
-- =============================================================================
DO $$
DECLARE
    embedding_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO embedding_count FROM embedding_store;
    RAISE NOTICE 'Dummy data loaded successfully!';
    RAISE NOTICE 'Total embeddings created: %', embedding_count;
END $$;
