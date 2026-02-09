package com.bmc.rag.connector.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for WorkLogEntry model class.
 */
@DisplayName("WorkLogEntry")
class WorkLogEntryTest {

    @Nested
    @DisplayName("getFormattedContent")
    class GetFormattedContentTests {

        @Test
        @DisplayName("formats complete work log entry")
        void getFormattedContent_completeEntry() {
            Instant submitDate = Instant.parse("2024-01-15T10:30:00Z");

            WorkLogEntry workLog = WorkLogEntry.builder()
                    .submitDate(submitDate)
                    .submitter("john.doe@example.com")
                    .workLogTypeDisplayValue("General Information")
                    .detailedDescription("Updated printer driver successfully")
                    .build();

            String formatted = workLog.getFormattedContent();

            assertThat(formatted)
                    .contains("[2024-01-15T10:30:00Z]")
                    .contains("By: john.doe@example.com")
                    .contains("Type: General Information")
                    .contains("Updated printer driver successfully");
        }

        @Test
        @DisplayName("handles null submit date")
        void getFormattedContent_nullSubmitDate() {
            WorkLogEntry workLog = WorkLogEntry.builder()
                    .submitter("john.doe@example.com")
                    .workLogTypeDisplayValue("General Information")
                    .detailedDescription("Work completed")
                    .build();

            String formatted = workLog.getFormattedContent();

            assertThat(formatted)
                    .doesNotContain("[")
                    .doesNotContain("]")
                    .contains("By: john.doe@example.com")
                    .contains("Type: General Information")
                    .contains("Work completed");
        }

        @Test
        @DisplayName("handles null submitter")
        void getFormattedContent_nullSubmitter() {
            Instant submitDate = Instant.parse("2024-01-15T10:30:00Z");

            WorkLogEntry workLog = WorkLogEntry.builder()
                    .submitDate(submitDate)
                    .workLogTypeDisplayValue("General Information")
                    .detailedDescription("Work completed")
                    .build();

            String formatted = workLog.getFormattedContent();

            assertThat(formatted)
                    .contains("[2024-01-15T10:30:00Z]")
                    .doesNotContain("By:")
                    .contains("Type: General Information")
                    .contains("Work completed");
        }

        @Test
        @DisplayName("handles null work log type")
        void getFormattedContent_nullWorkLogType() {
            Instant submitDate = Instant.parse("2024-01-15T10:30:00Z");

            WorkLogEntry workLog = WorkLogEntry.builder()
                    .submitDate(submitDate)
                    .submitter("john.doe@example.com")
                    .detailedDescription("Work completed")
                    .build();

            String formatted = workLog.getFormattedContent();

            assertThat(formatted)
                    .contains("[2024-01-15T10:30:00Z]")
                    .contains("By: john.doe@example.com")
                    .doesNotContain("Type:")
                    .contains("Work completed");
        }

        @Test
        @DisplayName("handles null detailed description")
        void getFormattedContent_nullDescription() {
            Instant submitDate = Instant.parse("2024-01-15T10:30:00Z");

            WorkLogEntry workLog = WorkLogEntry.builder()
                    .submitDate(submitDate)
                    .submitter("john.doe@example.com")
                    .workLogTypeDisplayValue("General Information")
                    .build();

            String formatted = workLog.getFormattedContent();

            assertThat(formatted)
                    .contains("[2024-01-15T10:30:00Z]")
                    .contains("By: john.doe@example.com")
                    .contains("Type: General Information");
        }

        @Test
        @DisplayName("handles all fields null")
        void getFormattedContent_allNull() {
            WorkLogEntry workLog = WorkLogEntry.builder()
                    .build();

            String formatted = workLog.getFormattedContent();

            assertThat(formatted).isEmpty();
        }
    }

    @Nested
    @DisplayName("isPublic")
    class IsPublicTests {

        @Test
        @DisplayName("returns true when viewAccess is null")
        void isPublic_viewAccessNull() {
            WorkLogEntry workLog = WorkLogEntry.builder()
                    .build();

            assertThat(workLog.isPublic()).isTrue();
        }

        @Test
        @DisplayName("returns true when viewAccess is 0")
        void isPublic_viewAccess0() {
            WorkLogEntry workLog = WorkLogEntry.builder()
                    .viewAccess(0)
                    .build();

            assertThat(workLog.isPublic()).isTrue();
        }

        @Test
        @DisplayName("returns false when viewAccess is 1 (Internal)")
        void isPublic_viewAccess1() {
            WorkLogEntry workLog = WorkLogEntry.builder()
                    .viewAccess(1)
                    .build();

            assertThat(workLog.isPublic()).isFalse();
        }

        @Test
        @DisplayName("returns false when viewAccess is other value")
        void isPublic_viewAccessOther() {
            WorkLogEntry workLog = WorkLogEntry.builder()
                    .viewAccess(2)
                    .build();

            assertThat(workLog.isPublic()).isFalse();
        }
    }

    @Nested
    @DisplayName("hasContent")
    class HasContentTests {

        @Test
        @DisplayName("returns true when detailed description has content")
        void hasContent_withContent() {
            WorkLogEntry workLog = WorkLogEntry.builder()
                    .detailedDescription("This is a work log entry")
                    .build();

            assertThat(workLog.hasContent()).isTrue();
        }

        @Test
        @DisplayName("returns false when detailed description is null")
        void hasContent_null() {
            WorkLogEntry workLog = WorkLogEntry.builder()
                    .build();

            assertThat(workLog.hasContent()).isFalse();
        }

        @Test
        @DisplayName("returns false when detailed description is empty")
        void hasContent_empty() {
            WorkLogEntry workLog = WorkLogEntry.builder()
                    .detailedDescription("")
                    .build();

            assertThat(workLog.hasContent()).isFalse();
        }

        @Test
        @DisplayName("returns false when detailed description is whitespace only")
        void hasContent_whitespaceOnly() {
            WorkLogEntry workLog = WorkLogEntry.builder()
                    .detailedDescription("   \n\t  ")
                    .build();

            assertThat(workLog.hasContent()).isFalse();
        }
    }

    @Nested
    @DisplayName("getContentLength")
    class GetContentLengthTests {

        @Test
        @DisplayName("returns length of detailed description")
        void getContentLength_withContent() {
            WorkLogEntry workLog = WorkLogEntry.builder()
                    .detailedDescription("This is a test")
                    .build();

            assertThat(workLog.getContentLength()).isEqualTo(14);
        }

        @Test
        @DisplayName("returns 0 when detailed description is null")
        void getContentLength_null() {
            WorkLogEntry workLog = WorkLogEntry.builder()
                    .build();

            assertThat(workLog.getContentLength()).isEqualTo(0);
        }

        @Test
        @DisplayName("returns 0 when detailed description is empty")
        void getContentLength_empty() {
            WorkLogEntry workLog = WorkLogEntry.builder()
                    .detailedDescription("")
                    .build();

            assertThat(workLog.getContentLength()).isEqualTo(0);
        }

        @Test
        @DisplayName("returns correct length for long content")
        void getContentLength_longContent() {
            String longDescription = "a".repeat(5000);
            WorkLogEntry workLog = WorkLogEntry.builder()
                    .detailedDescription(longDescription)
                    .build();

            assertThat(workLog.getContentLength()).isEqualTo(5000);
        }
    }

    @Nested
    @DisplayName("WorkLogSource Enum")
    class WorkLogSourceTests {

        @Test
        @DisplayName("INCIDENT has correct form name")
        void workLogSource_incident() {
            assertThat(WorkLogEntry.WorkLogSource.INCIDENT.getFormName())
                    .isEqualTo("HPD:WorkLog");
        }

        @Test
        @DisplayName("WORK_ORDER has correct form name")
        void workLogSource_workOrder() {
            assertThat(WorkLogEntry.WorkLogSource.WORK_ORDER.getFormName())
                    .isEqualTo("WOI:WorkInfo");
        }

        @Test
        @DisplayName("CHANGE_REQUEST has correct form name")
        void workLogSource_changeRequest() {
            assertThat(WorkLogEntry.WorkLogSource.CHANGE_REQUEST.getFormName())
                    .isEqualTo("CHG:WorkLog");
        }

        @Test
        @DisplayName("all enum values are accessible")
        void workLogSource_allValues() {
            WorkLogEntry.WorkLogSource[] values = WorkLogEntry.WorkLogSource.values();

            assertThat(values)
                    .hasSize(3)
                    .contains(
                            WorkLogEntry.WorkLogSource.INCIDENT,
                            WorkLogEntry.WorkLogSource.WORK_ORDER,
                            WorkLogEntry.WorkLogSource.CHANGE_REQUEST
                    );
        }
    }

    @Nested
    @DisplayName("Builder Defaults")
    class BuilderDefaultsTests {

        @Test
        @DisplayName("attachments defaults to empty ArrayList")
        void builder_attachmentsDefaultsToEmptyList() {
            WorkLogEntry workLog = WorkLogEntry.builder()
                    .workLogId("WLG000001")
                    .build();

            assertThat(workLog.getAttachments())
                    .isNotNull()
                    .isEmpty();
            assertThat(workLog.getAttachments()).isInstanceOf(ArrayList.class);
        }

        @Test
        @DisplayName("can add attachments to default list")
        void builder_canAddToDefaultAttachmentsList() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .filename("screenshot.png")
                    .build();

            WorkLogEntry workLog = WorkLogEntry.builder()
                    .workLogId("WLG000001")
                    .build();

            workLog.getAttachments().add(attachment);

            assertThat(workLog.getAttachments()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Complete Object Creation")
    class CompleteObjectTests {

        @Test
        @DisplayName("can create fully populated work log entry")
        void builder_createFullyPopulatedWorkLog() {
            Instant now = Instant.now();

            WorkLogEntry workLog = WorkLogEntry.builder()
                    .entryId("000000000000100")
                    .workLogId("WLG000001")
                    .parentId("INC000000000001")
                    .source(WorkLogEntry.WorkLogSource.INCIDENT)
                    .workLogType(1000)
                    .workLogTypeDisplayValue("General Information")
                    .detailedDescription("Troubleshooting steps completed successfully")
                    .submitter("john.doe@example.com")
                    .submitDate(now.minusSeconds(3600))
                    .viewAccess(0)
                    .createDate(now.minusSeconds(3600))
                    .lastModifiedDate(now.minusSeconds(1800))
                    .build();

            assertThat(workLog.getWorkLogId()).isEqualTo("WLG000001");
            assertThat(workLog.getParentId()).isEqualTo("INC000000000001");
            assertThat(workLog.getSource()).isEqualTo(WorkLogEntry.WorkLogSource.INCIDENT);
            assertThat(workLog.hasContent()).isTrue();
            assertThat(workLog.isPublic()).isTrue();
            assertThat(workLog.getContentLength()).isGreaterThan(0);
            assertThat(workLog.getFormattedContent())
                    .contains("john.doe@example.com")
                    .contains("General Information")
                    .contains("Troubleshooting steps completed successfully");
        }
    }

    @Nested
    @DisplayName("Lombok Generated Methods")
    class LombokGeneratedMethodsTests {

        @Test
        @DisplayName("equals returns true for same object")
        void equals_sameObject_returnsTrue() {
            WorkLogEntry workLog = WorkLogEntry.builder()
                    .workLogId("WLG000001")
                    .detailedDescription("Test")
                    .build();

            assertThat(workLog).isEqualTo(workLog);
        }

        @Test
        @DisplayName("equals returns true for same values")
        void equals_sameValues_returnsTrue() {
            WorkLogEntry wl1 = WorkLogEntry.builder()
                    .workLogId("WLG000001")
                    .detailedDescription("Test")
                    .viewAccess(0)
                    .build();

            WorkLogEntry wl2 = WorkLogEntry.builder()
                    .workLogId("WLG000001")
                    .detailedDescription("Test")
                    .viewAccess(0)
                    .build();

            assertThat(wl1).isEqualTo(wl2);
            assertThat(wl1.hashCode()).isEqualTo(wl2.hashCode());
        }

        @Test
        @DisplayName("equals returns false for different values")
        void equals_differentWorkLogId_returnsFalse() {
            WorkLogEntry wl1 = WorkLogEntry.builder()
                    .workLogId("WLG000001")
                    .build();

            WorkLogEntry wl2 = WorkLogEntry.builder()
                    .workLogId("WLG000002")
                    .build();

            assertThat(wl1).isNotEqualTo(wl2);
        }

        @Test
        @DisplayName("toString includes key fields")
        void toString_includesKeyFields() {
            WorkLogEntry workLog = WorkLogEntry.builder()
                    .workLogId("WLG000001")
                    .detailedDescription("Work completed")
                    .viewAccess(0)
                    .build();

            String result = workLog.toString();

            assertThat(result).contains("WLG000001");
            assertThat(result).contains("Work completed");
            assertThat(result).contains("viewAccess=0");
        }

        @Test
        @DisplayName("no-args constructor creates object")
        void noArgsConstructor_createsObject() {
            WorkLogEntry workLog = new WorkLogEntry();
            assertThat(workLog).isNotNull();
        }

        @Test
        @DisplayName("all-args constructor sets all fields")
        void allArgsConstructor_setsAllFields() {
            Instant now = Instant.now();
            WorkLogEntry wl = new WorkLogEntry(
                "entry1", "WLG001", "INC001", WorkLogEntry.WorkLogSource.INCIDENT,
                1000, "General", "Description", "submitter", now, 0, now, now,
                new ArrayList<>()
            );

            assertThat(wl.getEntryId()).isEqualTo("entry1");
            assertThat(wl.getWorkLogId()).isEqualTo("WLG001");
        }

        @Test
        @DisplayName("enum valueOf works correctly")
        void enumValueOf_worksCorrectly() {
            WorkLogEntry.WorkLogSource source = WorkLogEntry.WorkLogSource.valueOf("INCIDENT");
            assertThat(source).isEqualTo(WorkLogEntry.WorkLogSource.INCIDENT);
        }
    }
}
