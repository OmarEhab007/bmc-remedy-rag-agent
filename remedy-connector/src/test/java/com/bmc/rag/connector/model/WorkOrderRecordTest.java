package com.bmc.rag.connector.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for WorkOrderRecord model class.
 */
@DisplayName("WorkOrderRecord")
class WorkOrderRecordTest {

    @Nested
    @DisplayName("ITSMRecord Interface Methods")
    class ITSMRecordInterfaceTests {

        @Test
        @DisplayName("getRecordType returns 'WorkOrder'")
        void getRecordType_returnsWorkOrder() {
            WorkOrderRecord workOrder = WorkOrderRecord.builder()
                    .workOrderId("WO0000000000001")
                    .build();

            assertThat(workOrder.getRecordType()).isEqualTo("WorkOrder");
        }

        @Test
        @DisplayName("getRecordId returns work order ID")
        void getRecordId_returnsWorkOrderId() {
            WorkOrderRecord workOrder = WorkOrderRecord.builder()
                    .workOrderId("WO0000000000001")
                    .build();

            assertThat(workOrder.getRecordId()).isEqualTo("WO0000000000001");
        }

        @Test
        @DisplayName("getTitle returns summary")
        void getTitle_returnsSummary() {
            WorkOrderRecord workOrder = WorkOrderRecord.builder()
                    .summary("Replace server fan")
                    .build();

            assertThat(workOrder.getTitle()).isEqualTo("Replace server fan");
        }

        @Test
        @DisplayName("getContent returns description")
        void getContent_returnsDescription() {
            WorkOrderRecord workOrder = WorkOrderRecord.builder()
                    .description("Server room fan replacement scheduled for maintenance window")
                    .build();

            assertThat(workOrder.getContent()).isEqualTo("Server room fan replacement scheduled for maintenance window");
        }
    }

    @Nested
    @DisplayName("getCategoryPath")
    class GetCategoryPathTests {

        @Test
        @DisplayName("returns tier1 only when tier2 and tier3 are null")
        void getCategoryPath_tier1Only() {
            WorkOrderRecord workOrder = WorkOrderRecord.builder()
                    .categoryTier1("Facilities")
                    .build();

            assertThat(workOrder.getCategoryPath()).isEqualTo("Facilities");
        }

        @Test
        @DisplayName("returns tier1 > tier2 when tier3 is null")
        void getCategoryPath_tier1AndTier2() {
            WorkOrderRecord workOrder = WorkOrderRecord.builder()
                    .categoryTier1("Facilities")
                    .categoryTier2("HVAC")
                    .build();

            assertThat(workOrder.getCategoryPath()).isEqualTo("Facilities > HVAC");
        }

        @Test
        @DisplayName("returns full path tier1 > tier2 > tier3")
        void getCategoryPath_allThreeTiers() {
            WorkOrderRecord workOrder = WorkOrderRecord.builder()
                    .categoryTier1("Facilities")
                    .categoryTier2("HVAC")
                    .categoryTier3("Cooling")
                    .build();

            assertThat(workOrder.getCategoryPath()).isEqualTo("Facilities > HVAC > Cooling");
        }

        @Test
        @DisplayName("returns empty string when tier1 is null")
        void getCategoryPath_allNull() {
            WorkOrderRecord workOrder = WorkOrderRecord.builder()
                    .build();

            assertThat(workOrder.getCategoryPath()).isEmpty();
        }

        @Test
        @DisplayName("returns tier1 only when tier2 is null even if tier3 is present")
        void getCategoryPath_tier1WithTier3ButNoTier2() {
            WorkOrderRecord workOrder = WorkOrderRecord.builder()
                    .categoryTier1("Facilities")
                    .categoryTier3("Cooling")
                    .build();

            assertThat(workOrder.getCategoryPath()).isEqualTo("Facilities");
        }
    }

    @Nested
    @DisplayName("getRequesterFullName")
    class GetRequesterFullNameTests {

        @Test
        @DisplayName("returns full name when both first and last names present")
        void getRequesterFullName_bothNames() {
            WorkOrderRecord workOrder = WorkOrderRecord.builder()
                    .requesterFirstName("Alice")
                    .requesterLastName("Johnson")
                    .build();

            assertThat(workOrder.getRequesterFullName()).isEqualTo("Alice Johnson");
        }

        @Test
        @DisplayName("returns first name only when last name is null")
        void getRequesterFullName_firstNameOnly() {
            WorkOrderRecord workOrder = WorkOrderRecord.builder()
                    .requesterFirstName("Alice")
                    .build();

            assertThat(workOrder.getRequesterFullName()).isEqualTo("Alice");
        }

        @Test
        @DisplayName("returns last name only when first name is null")
        void getRequesterFullName_lastNameOnly() {
            WorkOrderRecord workOrder = WorkOrderRecord.builder()
                    .requesterLastName("Johnson")
                    .build();

            assertThat(workOrder.getRequesterFullName()).isEqualTo("Johnson");
        }

        @Test
        @DisplayName("returns null when both names are null")
        void getRequesterFullName_bothNull() {
            WorkOrderRecord workOrder = WorkOrderRecord.builder()
                    .build();

            assertThat(workOrder.getRequesterFullName()).isNull();
        }
    }

    @Nested
    @DisplayName("isClosed")
    class IsClosedTests {

        @Test
        @DisplayName("returns true when status is 5 (Completed)")
        void isClosed_statusCompleted() {
            WorkOrderRecord workOrder = WorkOrderRecord.builder()
                    .status(5)
                    .build();

            assertThat(workOrder.isClosed()).isTrue();
        }

        @Test
        @DisplayName("returns true when status is 7 (Cancelled)")
        void isClosed_statusCancelled() {
            WorkOrderRecord workOrder = WorkOrderRecord.builder()
                    .status(7)
                    .build();

            assertThat(workOrder.isClosed()).isTrue();
        }

        @Test
        @DisplayName("returns true when status is 8 (Closed)")
        void isClosed_statusClosed() {
            WorkOrderRecord workOrder = WorkOrderRecord.builder()
                    .status(8)
                    .build();

            assertThat(workOrder.isClosed()).isTrue();
        }

        @Test
        @DisplayName("returns false when status is 0 (Draft)")
        void isClosed_statusDraft() {
            WorkOrderRecord workOrder = WorkOrderRecord.builder()
                    .status(0)
                    .build();

            assertThat(workOrder.isClosed()).isFalse();
        }

        @Test
        @DisplayName("returns false when status is 1 (Pending)")
        void isClosed_statusPending() {
            WorkOrderRecord workOrder = WorkOrderRecord.builder()
                    .status(1)
                    .build();

            assertThat(workOrder.isClosed()).isFalse();
        }

        @Test
        @DisplayName("returns false when status is 2 (Assigned)")
        void isClosed_statusAssigned() {
            WorkOrderRecord workOrder = WorkOrderRecord.builder()
                    .status(2)
                    .build();

            assertThat(workOrder.isClosed()).isFalse();
        }

        @Test
        @DisplayName("returns false when status is 3 (In Progress)")
        void isClosed_statusInProgress() {
            WorkOrderRecord workOrder = WorkOrderRecord.builder()
                    .status(3)
                    .build();

            assertThat(workOrder.isClosed()).isFalse();
        }

        @Test
        @DisplayName("returns false when status is 4 (Waiting)")
        void isClosed_statusWaiting() {
            WorkOrderRecord workOrder = WorkOrderRecord.builder()
                    .status(4)
                    .build();

            assertThat(workOrder.isClosed()).isFalse();
        }

        @Test
        @DisplayName("returns false when status is 6 (other value)")
        void isClosed_statusOther() {
            WorkOrderRecord workOrder = WorkOrderRecord.builder()
                    .status(6)
                    .build();

            assertThat(workOrder.isClosed()).isFalse();
        }

        @Test
        @DisplayName("returns false when status is null")
        void isClosed_statusNull() {
            WorkOrderRecord workOrder = WorkOrderRecord.builder()
                    .build();

            assertThat(workOrder.isClosed()).isFalse();
        }
    }

    @Nested
    @DisplayName("Builder Defaults")
    class BuilderDefaultsTests {

        @Test
        @DisplayName("workLogs defaults to empty ArrayList")
        void builder_workLogsDefaultsToEmptyList() {
            WorkOrderRecord workOrder = WorkOrderRecord.builder()
                    .workOrderId("WO0000000000001")
                    .build();

            assertThat(workOrder.getWorkLogs())
                    .isNotNull()
                    .isEmpty();
            assertThat(workOrder.getWorkLogs()).isInstanceOf(ArrayList.class);
        }

        @Test
        @DisplayName("attachments defaults to empty ArrayList")
        void builder_attachmentsDefaultsToEmptyList() {
            WorkOrderRecord workOrder = WorkOrderRecord.builder()
                    .workOrderId("WO0000000000001")
                    .build();

            assertThat(workOrder.getAttachments())
                    .isNotNull()
                    .isEmpty();
            assertThat(workOrder.getAttachments()).isInstanceOf(ArrayList.class);
        }

        @Test
        @DisplayName("can add work logs to default list")
        void builder_canAddToDefaultWorkLogsList() {
            WorkLogEntry workLog = WorkLogEntry.builder()
                    .workLogId("WLG000001")
                    .detailedDescription("Maintenance started")
                    .build();

            WorkOrderRecord workOrder = WorkOrderRecord.builder()
                    .workOrderId("WO0000000000001")
                    .build();

            workOrder.getWorkLogs().add(workLog);

            assertThat(workOrder.getWorkLogs()).hasSize(1);
        }

        @Test
        @DisplayName("can provide custom attachments list")
        void builder_canProvideCustomAttachmentsList() {
            AttachmentInfo attachment1 = AttachmentInfo.builder()
                    .filename("checklist.pdf")
                    .build();
            AttachmentInfo attachment2 = AttachmentInfo.builder()
                    .filename("diagram.png")
                    .build();

            List<AttachmentInfo> customList = new ArrayList<>();
            customList.add(attachment1);
            customList.add(attachment2);

            WorkOrderRecord workOrder = WorkOrderRecord.builder()
                    .workOrderId("WO0000000000001")
                    .attachments(customList)
                    .build();

            assertThat(workOrder.getAttachments())
                    .hasSize(2)
                    .containsExactly(attachment1, attachment2);
        }
    }

    @Nested
    @DisplayName("Complete Object Creation")
    class CompleteObjectTests {

        @Test
        @DisplayName("can create fully populated work order")
        void builder_createFullyPopulatedWorkOrder() {
            Instant now = Instant.now();

            WorkOrderRecord workOrder = WorkOrderRecord.builder()
                    .entryId("000000000000002")
                    .workOrderId("WO0000000000001")
                    .summary("Replace server fan")
                    .description("Server room fan replacement scheduled for maintenance window")
                    .status(5)
                    .statusDisplayValue("Completed")
                    .priority(2)
                    .assignedGroup("Facilities")
                    .assignedTo("Bob Smith")
                    .assignedSupportCompany("Facilities Management Inc")
                    .submitter("alice.johnson@example.com")
                    .createDate(now.minusSeconds(7200))
                    .lastModifiedDate(now)
                    .lastModifiedBy("bob.smith@example.com")
                    .categoryTier1("Facilities")
                    .categoryTier2("HVAC")
                    .categoryTier3("Cooling")
                    .requesterFirstName("Alice")
                    .requesterLastName("Johnson")
                    .locationCompany("Acme Corp")
                    .scheduledStartDate(now.minusSeconds(3600))
                    .scheduledEndDate(now.plusSeconds(3600))
                    .build();

            assertThat(workOrder.getRecordType()).isEqualTo("WorkOrder");
            assertThat(workOrder.getRecordId()).isEqualTo("WO0000000000001");
            assertThat(workOrder.getTitle()).isEqualTo("Replace server fan");
            assertThat(workOrder.getContent()).isEqualTo("Server room fan replacement scheduled for maintenance window");
            assertThat(workOrder.getCategoryPath()).isEqualTo("Facilities > HVAC > Cooling");
            assertThat(workOrder.getRequesterFullName()).isEqualTo("Alice Johnson");
            assertThat(workOrder.isClosed()).isTrue();
        }
    }

    @Nested
    @DisplayName("Lombok Generated Methods")
    class LombokGeneratedMethodsTests {

        @Test
        @DisplayName("equals returns true for same object")
        void equals_sameObject_returnsTrue() {
            WorkOrderRecord workOrder = WorkOrderRecord.builder()
                    .workOrderId("WO0000000000001")
                    .summary("Test")
                    .build();

            assertThat(workOrder).isEqualTo(workOrder);
        }

        @Test
        @DisplayName("equals returns true for same values")
        void equals_sameValues_returnsTrue() {
            WorkOrderRecord wo1 = WorkOrderRecord.builder()
                    .workOrderId("WO0000000000001")
                    .summary("Test")
                    .status(5)
                    .build();

            WorkOrderRecord wo2 = WorkOrderRecord.builder()
                    .workOrderId("WO0000000000001")
                    .summary("Test")
                    .status(5)
                    .build();

            assertThat(wo1).isEqualTo(wo2);
            assertThat(wo1.hashCode()).isEqualTo(wo2.hashCode());
        }

        @Test
        @DisplayName("equals returns false for different values")
        void equals_differentWorkOrderId_returnsFalse() {
            WorkOrderRecord wo1 = WorkOrderRecord.builder()
                    .workOrderId("WO0000000000001")
                    .build();

            WorkOrderRecord wo2 = WorkOrderRecord.builder()
                    .workOrderId("WO0000000000002")
                    .build();

            assertThat(wo1).isNotEqualTo(wo2);
        }

        @Test
        @DisplayName("toString includes key fields")
        void toString_includesKeyFields() {
            WorkOrderRecord workOrder = WorkOrderRecord.builder()
                    .workOrderId("WO0000000000001")
                    .summary("Replace fan")
                    .status(5)
                    .priority(2)
                    .build();

            String result = workOrder.toString();

            assertThat(result).contains("WO0000000000001");
            assertThat(result).contains("Replace fan");
            assertThat(result).contains("status=5");
        }

        @Test
        @DisplayName("no-args constructor creates object")
        void noArgsConstructor_createsObject() {
            WorkOrderRecord workOrder = new WorkOrderRecord();
            assertThat(workOrder).isNotNull();
        }

        @Test
        @DisplayName("all-args constructor sets all fields")
        void allArgsConstructor_setsAllFields() {
            Instant now = Instant.now();
            WorkOrderRecord wo = new WorkOrderRecord(
                "entry1", "WO001", "summary", "desc", 5, "Completed",
                2, "group", "assignee", "company", "submitter", now, now, "modifier",
                "tier1", "tier2", "tier3", "Alice", "Johnson", "Acme Corp",
                now, now, new ArrayList<>(), new ArrayList<>()
            );

            assertThat(wo.getEntryId()).isEqualTo("entry1");
            assertThat(wo.getWorkOrderId()).isEqualTo("WO001");
        }
    }
}
