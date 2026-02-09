package com.bmc.rag.connector.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link WorkOrderCreationRequest}.
 * Tests validation methods, label methods, and preview generation.
 */
@DisplayName("WorkOrderCreationRequest")
class WorkOrderCreationRequestTest {

    @Nested
    @DisplayName("isValidWorkOrderType")
    class IsValidWorkOrderType {

        @Test
        @DisplayName("isValidWorkOrderType_withType0_returnsTrue")
        void isValidWorkOrderType_withType0_returnsTrue() {
            // Given
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test work order")
                    .description("Test description")
                    .workOrderType(0)
                    .priority(2)
                    .build();

            // When
            boolean result = request.isValidWorkOrderType();

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("isValidWorkOrderType_withType4_returnsTrue")
        void isValidWorkOrderType_withType4_returnsTrue() {
            // Given
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test work order")
                    .description("Test description")
                    .workOrderType(4)
                    .priority(2)
                    .build();

            // When
            boolean result = request.isValidWorkOrderType();

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("isValidWorkOrderType_withTypeNegative1_returnsFalse")
        void isValidWorkOrderType_withTypeNegative1_returnsFalse() {
            // Given
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test work order")
                    .description("Test description")
                    .workOrderType(-1)
                    .priority(2)
                    .build();

            // When
            boolean result = request.isValidWorkOrderType();

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("isValidWorkOrderType_withType5_returnsFalse")
        void isValidWorkOrderType_withType5_returnsFalse() {
            // Given
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test work order")
                    .description("Test description")
                    .workOrderType(5)
                    .priority(2)
                    .build();

            // When
            boolean result = request.isValidWorkOrderType();

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("isValidWorkOrderType_withNullType_returnsFalse")
        void isValidWorkOrderType_withNullType_returnsFalse() {
            // Given
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test work order")
                    .description("Test description")
                    .workOrderType(null)
                    .priority(2)
                    .build();

            // When
            boolean result = request.isValidWorkOrderType();

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("isValidPriority")
    class IsValidPriority {

        @Test
        @DisplayName("isValidPriority_withPriority0_returnsTrue")
        void isValidPriority_withPriority0_returnsTrue() {
            // Given
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test work order")
                    .description("Test description")
                    .workOrderType(0)
                    .priority(0)
                    .build();

            // When
            boolean result = request.isValidPriority();

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("isValidPriority_withPriority3_returnsTrue")
        void isValidPriority_withPriority3_returnsTrue() {
            // Given
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test work order")
                    .description("Test description")
                    .workOrderType(0)
                    .priority(3)
                    .build();

            // When
            boolean result = request.isValidPriority();

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("isValidPriority_withPriorityNegative1_returnsFalse")
        void isValidPriority_withPriorityNegative1_returnsFalse() {
            // Given
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test work order")
                    .description("Test description")
                    .workOrderType(0)
                    .priority(-1)
                    .build();

            // When
            boolean result = request.isValidPriority();

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("isValidPriority_withPriority4_returnsFalse")
        void isValidPriority_withPriority4_returnsFalse() {
            // Given
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test work order")
                    .description("Test description")
                    .workOrderType(0)
                    .priority(4)
                    .build();

            // When
            boolean result = request.isValidPriority();

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("isValidPriority_withNullPriority_returnsFalse")
        void isValidPriority_withNullPriority_returnsFalse() {
            // Given
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test work order")
                    .description("Test description")
                    .workOrderType(0)
                    .priority(null)
                    .build();

            // When
            boolean result = request.isValidPriority();

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getWorkOrderTypeLabel")
    class GetWorkOrderTypeLabel {

        @Test
        @DisplayName("getWorkOrderTypeLabel_withType0_returnsGeneral")
        void getWorkOrderTypeLabel_withType0_returnsGeneral() {
            // Given
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test work order")
                    .description("Test description")
                    .workOrderType(0)
                    .priority(2)
                    .build();

            // When
            String label = request.getWorkOrderTypeLabel();

            // Then
            assertThat(label).isEqualTo("General");
        }

        @Test
        @DisplayName("getWorkOrderTypeLabel_withType1_returnsProjectWork")
        void getWorkOrderTypeLabel_withType1_returnsProjectWork() {
            // Given
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test work order")
                    .description("Test description")
                    .workOrderType(1)
                    .priority(2)
                    .build();

            // When
            String label = request.getWorkOrderTypeLabel();

            // Then
            assertThat(label).isEqualTo("Project Work");
        }

        @Test
        @DisplayName("getWorkOrderTypeLabel_withType2_returnsBreakFix")
        void getWorkOrderTypeLabel_withType2_returnsBreakFix() {
            // Given
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test work order")
                    .description("Test description")
                    .workOrderType(2)
                    .priority(2)
                    .build();

            // When
            String label = request.getWorkOrderTypeLabel();

            // Then
            assertThat(label).isEqualTo("Break/Fix");
        }

        @Test
        @DisplayName("getWorkOrderTypeLabel_withType3_returnsMoveAddChange")
        void getWorkOrderTypeLabel_withType3_returnsMoveAddChange() {
            // Given
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test work order")
                    .description("Test description")
                    .workOrderType(3)
                    .priority(2)
                    .build();

            // When
            String label = request.getWorkOrderTypeLabel();

            // Then
            assertThat(label).isEqualTo("Move/Add/Change");
        }

        @Test
        @DisplayName("getWorkOrderTypeLabel_withType4_returnsReleaseActivity")
        void getWorkOrderTypeLabel_withType4_returnsReleaseActivity() {
            // Given
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test work order")
                    .description("Test description")
                    .workOrderType(4)
                    .priority(2)
                    .build();

            // When
            String label = request.getWorkOrderTypeLabel();

            // Then
            assertThat(label).isEqualTo("Release Activity");
        }

        @Test
        @DisplayName("getWorkOrderTypeLabel_withNullType_returnsUnknown")
        void getWorkOrderTypeLabel_withNullType_returnsUnknown() {
            // Given
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test work order")
                    .description("Test description")
                    .workOrderType(null)
                    .priority(2)
                    .build();

            // When
            String label = request.getWorkOrderTypeLabel();

            // Then
            assertThat(label).isEqualTo("Unknown");
        }

        @Test
        @DisplayName("getWorkOrderTypeLabel_withInvalidType_returnsUnknown")
        void getWorkOrderTypeLabel_withInvalidType_returnsUnknown() {
            // Given
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test work order")
                    .description("Test description")
                    .workOrderType(99)
                    .priority(2)
                    .build();

            // When
            String label = request.getWorkOrderTypeLabel();

            // Then
            assertThat(label).isEqualTo("Unknown");
        }
    }

    @Nested
    @DisplayName("getPriorityLabel")
    class GetPriorityLabel {

        @Test
        @DisplayName("getPriorityLabel_withPriority0_returnsCritical")
        void getPriorityLabel_withPriority0_returnsCritical() {
            // Given
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test work order")
                    .description("Test description")
                    .workOrderType(0)
                    .priority(0)
                    .build();

            // When
            String label = request.getPriorityLabel();

            // Then
            assertThat(label).isEqualTo("Critical");
        }

        @Test
        @DisplayName("getPriorityLabel_withPriority1_returnsHigh")
        void getPriorityLabel_withPriority1_returnsHigh() {
            // Given
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test work order")
                    .description("Test description")
                    .workOrderType(0)
                    .priority(1)
                    .build();

            // When
            String label = request.getPriorityLabel();

            // Then
            assertThat(label).isEqualTo("High");
        }

        @Test
        @DisplayName("getPriorityLabel_withPriority2_returnsMedium")
        void getPriorityLabel_withPriority2_returnsMedium() {
            // Given
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test work order")
                    .description("Test description")
                    .workOrderType(0)
                    .priority(2)
                    .build();

            // When
            String label = request.getPriorityLabel();

            // Then
            assertThat(label).isEqualTo("Medium");
        }

        @Test
        @DisplayName("getPriorityLabel_withPriority3_returnsLow")
        void getPriorityLabel_withPriority3_returnsLow() {
            // Given
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test work order")
                    .description("Test description")
                    .workOrderType(0)
                    .priority(3)
                    .build();

            // When
            String label = request.getPriorityLabel();

            // Then
            assertThat(label).isEqualTo("Low");
        }

        @Test
        @DisplayName("getPriorityLabel_withNullPriority_returnsUnknown")
        void getPriorityLabel_withNullPriority_returnsUnknown() {
            // Given
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test work order")
                    .description("Test description")
                    .workOrderType(0)
                    .priority(null)
                    .build();

            // When
            String label = request.getPriorityLabel();

            // Then
            assertThat(label).isEqualTo("Unknown");
        }

        @Test
        @DisplayName("getPriorityLabel_withInvalidPriority_returnsUnknown")
        void getPriorityLabel_withInvalidPriority_returnsUnknown() {
            // Given
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test work order")
                    .description("Test description")
                    .workOrderType(0)
                    .priority(99)
                    .build();

            // When
            String label = request.getPriorityLabel();

            // Then
            assertThat(label).isEqualTo("Unknown");
        }
    }

    @Nested
    @DisplayName("toPreviewString")
    class ToPreviewString {

        @Test
        @DisplayName("toPreviewString_withMinimalFields_returnsCorrectPreview")
        void toPreviewString_withMinimalFields_returnsCorrectPreview() {
            // Given
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Server maintenance")
                    .description("Perform routine server maintenance")
                    .workOrderType(0)
                    .priority(2)
                    .build();

            // When
            String preview = request.toPreviewString();

            // Then
            assertThat(preview)
                    .contains("**New Work Order Preview**")
                    .contains("**Summary:** Server maintenance")
                    .contains("**Description:** Perform routine server maintenance")
                    .contains("**Type:** General")
                    .contains("**Priority:** Medium")
                    .doesNotContain("**Requester:**")
                    .doesNotContain("**Category:**")
                    .doesNotContain("**Assigned Group:**");
        }

        @Test
        @DisplayName("toPreviewString_withLongDescription_truncatesAt200Characters")
        void toPreviewString_withLongDescription_truncatesAt200Characters() {
            // Given
            String longDescription = "B".repeat(250);
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test work order")
                    .description(longDescription)
                    .workOrderType(1)
                    .priority(1)
                    .build();

            // When
            String preview = request.toPreviewString();

            // Then
            assertThat(preview)
                    .contains("**Description:** " + "B".repeat(200) + "...");
        }

        @Test
        @DisplayName("toPreviewString_withShortDescription_doesNotTruncate")
        void toPreviewString_withShortDescription_doesNotTruncate() {
            // Given
            String shortDescription = "Short work order description";
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test work order")
                    .description(shortDescription)
                    .workOrderType(1)
                    .priority(1)
                    .build();

            // When
            String preview = request.toPreviewString();

            // Then
            assertThat(preview)
                    .contains("**Description:** " + shortDescription)
                    .doesNotContain("...");
        }

        @Test
        @DisplayName("toPreviewString_withRequesterFirstNameOnly_includesRequester")
        void toPreviewString_withRequesterFirstNameOnly_includesRequester() {
            // Given
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test work order")
                    .description("Test description")
                    .workOrderType(0)
                    .priority(2)
                    .requesterFirstName("Alice")
                    .build();

            // When
            String preview = request.toPreviewString();

            // Then
            assertThat(preview)
                    .contains("**Requester:** Alice");
        }

        @Test
        @DisplayName("toPreviewString_withRequesterLastNameOnly_includesRequester")
        void toPreviewString_withRequesterLastNameOnly_includesRequester() {
            // Given
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test work order")
                    .description("Test description")
                    .workOrderType(0)
                    .priority(2)
                    .requesterLastName("Smith")
                    .build();

            // When
            String preview = request.toPreviewString();

            // Then
            assertThat(preview)
                    .contains("**Requester:** Smith");
        }

        @Test
        @DisplayName("toPreviewString_withFullRequesterName_includesRequester")
        void toPreviewString_withFullRequesterName_includesRequester() {
            // Given
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test work order")
                    .description("Test description")
                    .workOrderType(0)
                    .priority(2)
                    .requesterFirstName("Alice")
                    .requesterLastName("Smith")
                    .build();

            // When
            String preview = request.toPreviewString();

            // Then
            assertThat(preview)
                    .contains("**Requester:** Alice Smith");
        }

        @Test
        @DisplayName("toPreviewString_withCategoryTier1Only_includesCategory")
        void toPreviewString_withCategoryTier1Only_includesCategory() {
            // Given
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test work order")
                    .description("Test description")
                    .workOrderType(0)
                    .priority(2)
                    .categoryTier1("Infrastructure")
                    .build();

            // When
            String preview = request.toPreviewString();

            // Then
            assertThat(preview)
                    .contains("**Category:** Infrastructure")
                    .doesNotContain(" > ");
        }

        @Test
        @DisplayName("toPreviewString_withFullCategoryHierarchy_includesAllTiers")
        void toPreviewString_withFullCategoryHierarchy_includesAllTiers() {
            // Given
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test work order")
                    .description("Test description")
                    .workOrderType(0)
                    .priority(2)
                    .categoryTier1("Infrastructure")
                    .categoryTier2("Network")
                    .categoryTier3("Switch Upgrade")
                    .build();

            // When
            String preview = request.toPreviewString();

            // Then
            assertThat(preview)
                    .contains("**Category:** Infrastructure > Network > Switch Upgrade");
        }

        @Test
        @DisplayName("toPreviewString_withAssignedGroup_includesAssignedGroup")
        void toPreviewString_withAssignedGroup_includesAssignedGroup() {
            // Given
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test work order")
                    .description("Test description")
                    .workOrderType(0)
                    .priority(2)
                    .assignedGroup("Network Operations")
                    .build();

            // When
            String preview = request.toPreviewString();

            // Then
            assertThat(preview)
                    .contains("**Assigned Group:** Network Operations");
        }

        @Test
        @DisplayName("toPreviewString_withScheduledStartDate_includesScheduledStart")
        void toPreviewString_withScheduledStartDate_includesScheduledStart() {
            // Given
            Instant startDate = Instant.parse("2026-03-01T10:00:00Z");
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test work order")
                    .description("Test description")
                    .workOrderType(0)
                    .priority(2)
                    .scheduledStartDate(startDate)
                    .build();

            // When
            String preview = request.toPreviewString();

            // Then
            assertThat(preview)
                    .contains("**Scheduled Start:** " + startDate.toString());
        }

        @Test
        @DisplayName("toPreviewString_withScheduledEndDate_includesScheduledEnd")
        void toPreviewString_withScheduledEndDate_includesScheduledEnd() {
            // Given
            Instant endDate = Instant.parse("2026-03-01T18:00:00Z");
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test work order")
                    .description("Test description")
                    .workOrderType(0)
                    .priority(2)
                    .scheduledEndDate(endDate)
                    .build();

            // When
            String preview = request.toPreviewString();

            // Then
            assertThat(preview)
                    .contains("**Scheduled End:** " + endDate.toString());
        }

        @Test
        @DisplayName("toPreviewString_withAllOptionalFields_includesAllFields")
        void toPreviewString_withAllOptionalFields_includesAllFields() {
            // Given
            Instant startDate = Instant.parse("2026-03-01T10:00:00Z");
            Instant endDate = Instant.parse("2026-03-01T18:00:00Z");

            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Critical server upgrade")
                    .description("Upgrade production database server to version 19c")
                    .workOrderType(2)
                    .priority(0)
                    .requesterFirstName("Alice")
                    .requesterLastName("Smith")
                    .locationCompany("Acme Corp")
                    .categoryTier1("Infrastructure")
                    .categoryTier2("Database")
                    .categoryTier3("Upgrade")
                    .assignedGroup("Database Operations")
                    .scheduledStartDate(startDate)
                    .scheduledEndDate(endDate)
                    .createdBy("admin")
                    .sessionId("session-456")
                    .build();

            // When
            String preview = request.toPreviewString();

            // Then
            assertThat(preview)
                    .contains("**New Work Order Preview**")
                    .contains("**Summary:** Critical server upgrade")
                    .contains("**Description:** Upgrade production database server to version 19c")
                    .contains("**Type:** Break/Fix")
                    .contains("**Priority:** Critical")
                    .contains("**Requester:** Alice Smith")
                    .contains("**Category:** Infrastructure > Database > Upgrade")
                    .contains("**Assigned Group:** Database Operations")
                    .contains("**Scheduled Start:** " + startDate.toString())
                    .contains("**Scheduled End:** " + endDate.toString());
        }

        @Test
        @DisplayName("toPreviewString_withAllWorkOrderTypes_displaysCorrectLabels")
        void toPreviewString_withAllWorkOrderTypes_displaysCorrectLabels() {
            // Test all work order types
            for (int type = 0; type <= 4; type++) {
                WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                        .summary("Test WO")
                        .description("Test")
                        .workOrderType(type)
                        .priority(1)
                        .build();

                String preview = request.toPreviewString();
                String expectedLabel = request.getWorkOrderTypeLabel();

                assertThat(preview).contains("**Type:** " + expectedLabel);
            }
        }

        @Test
        @DisplayName("toPreviewString_withAllPriorities_displaysCorrectLabels")
        void toPreviewString_withAllPriorities_displaysCorrectLabels() {
            // Test all priority levels
            for (int priority = 0; priority <= 3; priority++) {
                WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                        .summary("Test WO")
                        .description("Test")
                        .workOrderType(0)
                        .priority(priority)
                        .build();

                String preview = request.toPreviewString();
                String expectedLabel = request.getPriorityLabel();

                assertThat(preview).contains("**Priority:** " + expectedLabel);
            }
        }
    }

    @Nested
    @DisplayName("Lombok Generated Methods")
    class LombokGeneratedMethodsTests {

        @Test
        @DisplayName("equals returns true for same object")
        void equals_sameObject_returnsTrue() {
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test")
                    .description("Test")
                    .workOrderType(0)
                    .priority(2)
                    .build();

            assertThat(request).isEqualTo(request);
        }

        @Test
        @DisplayName("equals returns true for same values")
        void equals_sameValues_returnsTrue() {
            WorkOrderCreationRequest req1 = WorkOrderCreationRequest.builder()
                    .summary("Test")
                    .description("Test")
                    .workOrderType(0)
                    .priority(2)
                    .build();

            WorkOrderCreationRequest req2 = WorkOrderCreationRequest.builder()
                    .summary("Test")
                    .description("Test")
                    .workOrderType(0)
                    .priority(2)
                    .build();

            assertThat(req1).isEqualTo(req2);
            assertThat(req1.hashCode()).isEqualTo(req2.hashCode());
        }

        @Test
        @DisplayName("equals returns false for different values")
        void equals_differentSummary_returnsFalse() {
            WorkOrderCreationRequest req1 = WorkOrderCreationRequest.builder()
                    .summary("Test1")
                    .description("Test")
                    .workOrderType(0)
                    .priority(2)
                    .build();

            WorkOrderCreationRequest req2 = WorkOrderCreationRequest.builder()
                    .summary("Test2")
                    .description("Test")
                    .workOrderType(0)
                    .priority(2)
                    .build();

            assertThat(req1).isNotEqualTo(req2);
        }

        @Test
        @DisplayName("toString includes key fields")
        void toString_includesKeyFields() {
            WorkOrderCreationRequest request = WorkOrderCreationRequest.builder()
                    .summary("Test summary")
                    .description("Test description")
                    .workOrderType(2)
                    .priority(1)
                    .build();

            String result = request.toString();

            assertThat(result).contains("Test summary");
            assertThat(result).contains("Test description");
            assertThat(result).contains("workOrderType=2");
            assertThat(result).contains("priority=1");
        }

        @Test
        @DisplayName("no-args constructor creates object")
        void noArgsConstructor_createsObject() {
            WorkOrderCreationRequest request = new WorkOrderCreationRequest();
            assertThat(request).isNotNull();
        }
    }
}
