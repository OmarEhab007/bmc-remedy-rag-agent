package com.bmc.rag.connector.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ChangeRequestRecord model class.
 */
@DisplayName("ChangeRequestRecord")
class ChangeRequestRecordTest {

    @Nested
    @DisplayName("ITSMRecord Interface Methods")
    class ITSMRecordInterfaceTests {

        @Test
        @DisplayName("getRecordType returns 'ChangeRequest'")
        void getRecordType_returnsChangeRequest() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .changeId("CHG000000000001")
                    .build();

            assertThat(changeRequest.getRecordType()).isEqualTo("ChangeRequest");
        }

        @Test
        @DisplayName("getRecordId returns change ID")
        void getRecordId_returnsChangeId() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .changeId("CHG000000000001")
                    .build();

            assertThat(changeRequest.getRecordId()).isEqualTo("CHG000000000001");
        }

        @Test
        @DisplayName("getTitle returns summary")
        void getTitle_returnsSummary() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .summary("Upgrade database server")
                    .build();

            assertThat(changeRequest.getTitle()).isEqualTo("Upgrade database server");
        }

        @Test
        @DisplayName("getContent returns description")
        void getContent_returnsDescription() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .description("Upgrade database server from version 11 to version 12")
                    .build();

            assertThat(changeRequest.getContent()).isEqualTo("Upgrade database server from version 11 to version 12");
        }
    }

    @Nested
    @DisplayName("getCategoryPath")
    class GetCategoryPathTests {

        @Test
        @DisplayName("returns tier1 only when tier2 and tier3 are null")
        void getCategoryPath_tier1Only() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .categoryTier1("Infrastructure")
                    .build();

            assertThat(changeRequest.getCategoryPath()).isEqualTo("Infrastructure");
        }

        @Test
        @DisplayName("returns tier1 > tier2 when tier3 is null")
        void getCategoryPath_tier1AndTier2() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .categoryTier1("Infrastructure")
                    .categoryTier2("Database")
                    .build();

            assertThat(changeRequest.getCategoryPath()).isEqualTo("Infrastructure > Database");
        }

        @Test
        @DisplayName("returns full path tier1 > tier2 > tier3")
        void getCategoryPath_allThreeTiers() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .categoryTier1("Infrastructure")
                    .categoryTier2("Database")
                    .categoryTier3("SQL Server")
                    .build();

            assertThat(changeRequest.getCategoryPath()).isEqualTo("Infrastructure > Database > SQL Server");
        }

        @Test
        @DisplayName("returns empty string when tier1 is null")
        void getCategoryPath_allNull() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .build();

            assertThat(changeRequest.getCategoryPath()).isEmpty();
        }

        @Test
        @DisplayName("returns tier1 only when tier2 is null even if tier3 is present")
        void getCategoryPath_tier1WithTier3ButNoTier2() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .categoryTier1("Infrastructure")
                    .categoryTier3("SQL Server")
                    .build();

            assertThat(changeRequest.getCategoryPath()).isEqualTo("Infrastructure");
        }
    }

    @Nested
    @DisplayName("hasImplementationPlan")
    class HasImplementationPlanTests {

        @Test
        @DisplayName("returns true when implementation plan is present")
        void hasImplementationPlan_withContent() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .implementationPlan("1. Backup current database\n2. Run upgrade script\n3. Verify")
                    .build();

            assertThat(changeRequest.hasImplementationPlan()).isTrue();
        }

        @Test
        @DisplayName("returns false when implementation plan is null")
        void hasImplementationPlan_null() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .build();

            assertThat(changeRequest.hasImplementationPlan()).isFalse();
        }

        @Test
        @DisplayName("returns false when implementation plan is empty string")
        void hasImplementationPlan_emptyString() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .implementationPlan("")
                    .build();

            assertThat(changeRequest.hasImplementationPlan()).isFalse();
        }

        @Test
        @DisplayName("returns false when implementation plan is whitespace only")
        void hasImplementationPlan_whitespaceOnly() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .implementationPlan("   \n\t  ")
                    .build();

            assertThat(changeRequest.hasImplementationPlan()).isFalse();
        }
    }

    @Nested
    @DisplayName("hasRollbackPlan")
    class HasRollbackPlanTests {

        @Test
        @DisplayName("returns true when rollback plan is present")
        void hasRollbackPlan_withContent() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .rollbackPlan("1. Restore from backup\n2. Restart services\n3. Verify")
                    .build();

            assertThat(changeRequest.hasRollbackPlan()).isTrue();
        }

        @Test
        @DisplayName("returns false when rollback plan is null")
        void hasRollbackPlan_null() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .build();

            assertThat(changeRequest.hasRollbackPlan()).isFalse();
        }

        @Test
        @DisplayName("returns false when rollback plan is empty string")
        void hasRollbackPlan_emptyString() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .rollbackPlan("")
                    .build();

            assertThat(changeRequest.hasRollbackPlan()).isFalse();
        }

        @Test
        @DisplayName("returns false when rollback plan is whitespace only")
        void hasRollbackPlan_whitespaceOnly() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .rollbackPlan("   \n\t  ")
                    .build();

            assertThat(changeRequest.hasRollbackPlan()).isFalse();
        }
    }

    @Nested
    @DisplayName("isClosed")
    class IsClosedTests {

        @Test
        @DisplayName("returns true when status is 10 (Completed)")
        void isClosed_statusCompleted() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .status(10)
                    .build();

            assertThat(changeRequest.isClosed()).isTrue();
        }

        @Test
        @DisplayName("returns true when status is 11 (Closed)")
        void isClosed_statusClosed() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .status(11)
                    .build();

            assertThat(changeRequest.isClosed()).isTrue();
        }

        @Test
        @DisplayName("returns true when status is 12 (Cancelled)")
        void isClosed_statusCancelled() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .status(12)
                    .build();

            assertThat(changeRequest.isClosed()).isTrue();
        }

        @Test
        @DisplayName("returns false when status is 0 (Draft)")
        void isClosed_statusDraft() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .status(0)
                    .build();

            assertThat(changeRequest.isClosed()).isFalse();
        }

        @Test
        @DisplayName("returns false when status is 1 (Planning)")
        void isClosed_statusPlanning() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .status(1)
                    .build();

            assertThat(changeRequest.isClosed()).isFalse();
        }

        @Test
        @DisplayName("returns false when status is 5 (Scheduled)")
        void isClosed_statusScheduled() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .status(5)
                    .build();

            assertThat(changeRequest.isClosed()).isFalse();
        }

        @Test
        @DisplayName("returns false when status is 9 (In Progress)")
        void isClosed_statusInProgress() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .status(9)
                    .build();

            assertThat(changeRequest.isClosed()).isFalse();
        }

        @Test
        @DisplayName("returns false when status is null")
        void isClosed_statusNull() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .build();

            assertThat(changeRequest.isClosed()).isFalse();
        }
    }

    @Nested
    @DisplayName("isHighRisk")
    class IsHighRiskTests {

        @Test
        @DisplayName("returns true when risk level is 3")
        void isHighRisk_level3() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .riskLevel(3)
                    .build();

            assertThat(changeRequest.isHighRisk()).isTrue();
        }

        @Test
        @DisplayName("returns true when risk level is 4")
        void isHighRisk_level4() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .riskLevel(4)
                    .build();

            assertThat(changeRequest.isHighRisk()).isTrue();
        }

        @Test
        @DisplayName("returns false when risk level is 1")
        void isHighRisk_level1() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .riskLevel(1)
                    .build();

            assertThat(changeRequest.isHighRisk()).isFalse();
        }

        @Test
        @DisplayName("returns false when risk level is 2")
        void isHighRisk_level2() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .riskLevel(2)
                    .build();

            assertThat(changeRequest.isHighRisk()).isFalse();
        }

        @Test
        @DisplayName("returns false when risk level is null")
        void isHighRisk_nullRiskLevel() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .build();

            assertThat(changeRequest.isHighRisk()).isFalse();
        }
    }

    @Nested
    @DisplayName("getCombinedContent")
    class GetCombinedContentTests {

        @Test
        @DisplayName("combines all fields with labels")
        void getCombinedContent_allFields() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .summary("Upgrade database server")
                    .description("Upgrade database server from version 11 to version 12")
                    .changeReason("End of life for version 11")
                    .implementationPlan("1. Backup\n2. Upgrade\n3. Verify")
                    .rollbackPlan("1. Restore backup\n2. Verify")
                    .build();

            String combined = changeRequest.getCombinedContent();

            assertThat(combined)
                    .contains("Summary: Upgrade database server")
                    .contains("Description: Upgrade database server from version 11 to version 12")
                    .contains("Reason for Change: End of life for version 11")
                    .contains("Implementation Plan: 1. Backup")
                    .contains("Rollback Plan: 1. Restore backup");
        }

        @Test
        @DisplayName("handles null summary gracefully")
        void getCombinedContent_nullSummary() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .description("Description text")
                    .build();

            String combined = changeRequest.getCombinedContent();

            assertThat(combined)
                    .doesNotContain("Summary:")
                    .contains("Description: Description text");
        }

        @Test
        @DisplayName("handles null description gracefully")
        void getCombinedContent_nullDescription() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .summary("Summary text")
                    .changeReason("Change reason")
                    .build();

            String combined = changeRequest.getCombinedContent();

            assertThat(combined)
                    .contains("Summary: Summary text")
                    .doesNotContain("Description:")
                    .contains("Reason for Change: Change reason");
        }

        @Test
        @DisplayName("handles null change reason gracefully")
        void getCombinedContent_nullChangeReason() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .summary("Summary text")
                    .description("Description text")
                    .implementationPlan("Implementation")
                    .build();

            String combined = changeRequest.getCombinedContent();

            assertThat(combined)
                    .contains("Summary: Summary text")
                    .contains("Description: Description text")
                    .doesNotContain("Reason for Change:")
                    .contains("Implementation Plan: Implementation");
        }

        @Test
        @DisplayName("handles null implementation plan gracefully")
        void getCombinedContent_nullImplementationPlan() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .summary("Summary text")
                    .description("Description text")
                    .rollbackPlan("Rollback steps")
                    .build();

            String combined = changeRequest.getCombinedContent();

            assertThat(combined)
                    .contains("Summary: Summary text")
                    .contains("Description: Description text")
                    .doesNotContain("Implementation Plan:")
                    .contains("Rollback Plan: Rollback steps");
        }

        @Test
        @DisplayName("handles null rollback plan gracefully")
        void getCombinedContent_nullRollbackPlan() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .summary("Summary text")
                    .description("Description text")
                    .implementationPlan("Implementation steps")
                    .build();

            String combined = changeRequest.getCombinedContent();

            assertThat(combined)
                    .contains("Summary: Summary text")
                    .contains("Description: Description text")
                    .contains("Implementation Plan: Implementation steps")
                    .doesNotContain("Rollback Plan:");
        }

        @Test
        @DisplayName("trims result when last field is null")
        void getCombinedContent_trimsResult() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .summary("Summary text")
                    .build();

            String combined = changeRequest.getCombinedContent();

            assertThat(combined)
                    .isEqualTo("Summary: Summary text")
                    .doesNotEndWith("\n\n");
        }

        @Test
        @DisplayName("returns empty string when all fields are null")
        void getCombinedContent_allNull() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .build();

            String combined = changeRequest.getCombinedContent();

            assertThat(combined).isEmpty();
        }
    }

    @Nested
    @DisplayName("Builder Defaults")
    class BuilderDefaultsTests {

        @Test
        @DisplayName("workLogs defaults to empty ArrayList")
        void builder_workLogsDefaultsToEmptyList() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .changeId("CHG000000000001")
                    .build();

            assertThat(changeRequest.getWorkLogs())
                    .isNotNull()
                    .isEmpty();
            assertThat(changeRequest.getWorkLogs()).isInstanceOf(ArrayList.class);
        }

        @Test
        @DisplayName("attachments defaults to empty ArrayList")
        void builder_attachmentsDefaultsToEmptyList() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .changeId("CHG000000000001")
                    .build();

            assertThat(changeRequest.getAttachments())
                    .isNotNull()
                    .isEmpty();
            assertThat(changeRequest.getAttachments()).isInstanceOf(ArrayList.class);
        }

        @Test
        @DisplayName("can add work logs to default list")
        void builder_canAddToDefaultWorkLogsList() {
            WorkLogEntry workLog = WorkLogEntry.builder()
                    .workLogId("WLG000001")
                    .detailedDescription("Change implementation started")
                    .build();

            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .changeId("CHG000000000001")
                    .build();

            changeRequest.getWorkLogs().add(workLog);

            assertThat(changeRequest.getWorkLogs()).hasSize(1);
        }

        @Test
        @DisplayName("can provide custom attachments list")
        void builder_canProvideCustomAttachmentsList() {
            AttachmentInfo attachment1 = AttachmentInfo.builder()
                    .filename("implementation_plan.pdf")
                    .build();
            AttachmentInfo attachment2 = AttachmentInfo.builder()
                    .filename("rollback_procedure.docx")
                    .build();

            List<AttachmentInfo> customList = new ArrayList<>();
            customList.add(attachment1);
            customList.add(attachment2);

            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .changeId("CHG000000000001")
                    .attachments(customList)
                    .build();

            assertThat(changeRequest.getAttachments())
                    .hasSize(2)
                    .containsExactly(attachment1, attachment2);
        }
    }

    @Nested
    @DisplayName("Complete Object Creation")
    class CompleteObjectTests {

        @Test
        @DisplayName("can create fully populated change request")
        void builder_createFullyPopulatedChangeRequest() {
            Instant now = Instant.now();

            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .entryId("000000000000004")
                    .changeId("CHG000000000001")
                    .summary("Upgrade database server")
                    .description("Upgrade database server from version 11 to version 12")
                    .changeReason("End of life for version 11")
                    .implementationPlan("1. Backup database\n2. Run upgrade script\n3. Verify functionality")
                    .rollbackPlan("1. Restore from backup\n2. Restart services\n3. Verify")
                    .status(10)
                    .statusDisplayValue("Completed")
                    .riskLevel(3)
                    .impact(2)
                    .urgency(2)
                    .changeType("Standard")
                    .changeClass("Infrastructure")
                    .assignedGroup("Database Team")
                    .assignedTo("alice.db@example.com")
                    .assignedSupportCompany("IT Services Inc")
                    .submitter("bob.manager@example.com")
                    .createDate(now.minusSeconds(604800)) // 7 days ago
                    .lastModifiedDate(now)
                    .lastModifiedBy("alice.db@example.com")
                    .categoryTier1("Infrastructure")
                    .categoryTier2("Database")
                    .categoryTier3("SQL Server")
                    .scheduledStartDate(now.minusSeconds(86400)) // 1 day ago
                    .scheduledEndDate(now.minusSeconds(82800)) // 23 hours ago
                    .actualStartDate(now.minusSeconds(86400))
                    .actualEndDate(now.minusSeconds(82800))
                    .build();

            assertThat(changeRequest.getRecordType()).isEqualTo("ChangeRequest");
            assertThat(changeRequest.getRecordId()).isEqualTo("CHG000000000001");
            assertThat(changeRequest.getTitle()).isEqualTo("Upgrade database server");
            assertThat(changeRequest.getContent()).isEqualTo("Upgrade database server from version 11 to version 12");
            assertThat(changeRequest.getCategoryPath()).isEqualTo("Infrastructure > Database > SQL Server");
            assertThat(changeRequest.hasImplementationPlan()).isTrue();
            assertThat(changeRequest.hasRollbackPlan()).isTrue();
            assertThat(changeRequest.isClosed()).isTrue();
            assertThat(changeRequest.isHighRisk()).isTrue();
        }
    }

    @Nested
    @DisplayName("Lombok Generated Methods")
    class LombokGeneratedMethodsTests {

        @Test
        @DisplayName("equals returns true for same object")
        void equals_sameObject_returnsTrue() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .changeId("CHG000000000001")
                    .summary("Test")
                    .build();

            assertThat(changeRequest).isEqualTo(changeRequest);
        }

        @Test
        @DisplayName("equals returns true for objects with same values")
        void equals_sameValues_returnsTrue() {
            ChangeRequestRecord cr1 = ChangeRequestRecord.builder()
                    .changeId("CHG000000000001")
                    .summary("Upgrade server")
                    .status(10)
                    .build();

            ChangeRequestRecord cr2 = ChangeRequestRecord.builder()
                    .changeId("CHG000000000001")
                    .summary("Upgrade server")
                    .status(10)
                    .build();

            assertThat(cr1).isEqualTo(cr2);
            assertThat(cr1.hashCode()).isEqualTo(cr2.hashCode());
        }

        @Test
        @DisplayName("equals returns false for different changeId")
        void equals_differentChangeId_returnsFalse() {
            ChangeRequestRecord cr1 = ChangeRequestRecord.builder()
                    .changeId("CHG000000000001")
                    .summary("Test")
                    .build();

            ChangeRequestRecord cr2 = ChangeRequestRecord.builder()
                    .changeId("CHG000000000002")
                    .summary("Test")
                    .build();

            assertThat(cr1).isNotEqualTo(cr2);
        }

        @Test
        @DisplayName("equals returns false for null")
        void equals_null_returnsFalse() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .changeId("CHG000000000001")
                    .build();

            assertThat(changeRequest).isNotEqualTo(null);
        }

        @Test
        @DisplayName("equals returns false for different type")
        void equals_differentType_returnsFalse() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .changeId("CHG000000000001")
                    .build();

            assertThat(changeRequest).isNotEqualTo("CHG000000000001");
        }

        @Test
        @DisplayName("hashCode returns same value for same objects")
        void hashCode_sameValues_returnsSameHash() {
            ChangeRequestRecord cr1 = ChangeRequestRecord.builder()
                    .changeId("CHG000000000001")
                    .summary("Test")
                    .build();

            ChangeRequestRecord cr2 = ChangeRequestRecord.builder()
                    .changeId("CHG000000000001")
                    .summary("Test")
                    .build();

            assertThat(cr1.hashCode()).isEqualTo(cr2.hashCode());
        }

        @Test
        @DisplayName("toString includes key fields")
        void toString_includesKeyFields() {
            ChangeRequestRecord changeRequest = ChangeRequestRecord.builder()
                    .changeId("CHG000000000001")
                    .summary("Upgrade server")
                    .status(10)
                    .riskLevel(3)
                    .build();

            String result = changeRequest.toString();

            assertThat(result).contains("CHG000000000001");
            assertThat(result).contains("Upgrade server");
            assertThat(result).contains("status=10");
            assertThat(result).contains("riskLevel=3");
        }

        @Test
        @DisplayName("no-args constructor creates object")
        void noArgsConstructor_createsObject() {
            ChangeRequestRecord changeRequest = new ChangeRequestRecord();
            assertThat(changeRequest).isNotNull();
            assertThat(changeRequest.getChangeId()).isNull();
        }

        @Test
        @DisplayName("all-args constructor sets all fields")
        void allArgsConstructor_setsAllFields() {
            Instant now = Instant.now();
            ChangeRequestRecord cr = new ChangeRequestRecord(
                "entry1", "CHG001", "summary", "desc", "reason", "impl", "rollback",
                10, "Completed", 3, 2, 2, "Standard", "Infrastructure",
                "group", "assignee", "company", "submitter", now, now, "modifier",
                "tier1", "tier2", "tier3", now, now, now, now,
                new ArrayList<>(), new ArrayList<>()
            );

            assertThat(cr.getEntryId()).isEqualTo("entry1");
            assertThat(cr.getChangeId()).isEqualTo("CHG001");
            assertThat(cr.getSummary()).isEqualTo("summary");
        }
    }
}
