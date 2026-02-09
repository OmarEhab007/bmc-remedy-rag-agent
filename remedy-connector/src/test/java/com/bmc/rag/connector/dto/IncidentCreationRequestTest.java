package com.bmc.rag.connector.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link IncidentCreationRequest}.
 * Tests validation methods, label methods, and preview generation.
 */
@DisplayName("IncidentCreationRequest")
class IncidentCreationRequestTest {

    @Nested
    @DisplayName("isValidImpact")
    class IsValidImpact {

        @Test
        @DisplayName("isValidImpact_withImpact1_returnsTrue")
        void isValidImpact_withImpact1_returnsTrue() {
            // Given
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Test incident")
                    .description("Test description")
                    .impact(1)
                    .urgency(2)
                    .build();

            // When
            boolean result = request.isValidImpact();

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("isValidImpact_withImpact4_returnsTrue")
        void isValidImpact_withImpact4_returnsTrue() {
            // Given
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Test incident")
                    .description("Test description")
                    .impact(4)
                    .urgency(2)
                    .build();

            // When
            boolean result = request.isValidImpact();

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("isValidImpact_withImpact0_returnsFalse")
        void isValidImpact_withImpact0_returnsFalse() {
            // Given
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Test incident")
                    .description("Test description")
                    .impact(0)
                    .urgency(2)
                    .build();

            // When
            boolean result = request.isValidImpact();

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("isValidImpact_withImpact5_returnsFalse")
        void isValidImpact_withImpact5_returnsFalse() {
            // Given
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Test incident")
                    .description("Test description")
                    .impact(5)
                    .urgency(2)
                    .build();

            // When
            boolean result = request.isValidImpact();

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("isValidImpact_withNullImpact_returnsFalse")
        void isValidImpact_withNullImpact_returnsFalse() {
            // Given
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Test incident")
                    .description("Test description")
                    .impact(null)
                    .urgency(2)
                    .build();

            // When
            boolean result = request.isValidImpact();

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("isValidUrgency")
    class IsValidUrgency {

        @Test
        @DisplayName("isValidUrgency_withUrgency1_returnsTrue")
        void isValidUrgency_withUrgency1_returnsTrue() {
            // Given
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Test incident")
                    .description("Test description")
                    .impact(2)
                    .urgency(1)
                    .build();

            // When
            boolean result = request.isValidUrgency();

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("isValidUrgency_withUrgency4_returnsTrue")
        void isValidUrgency_withUrgency4_returnsTrue() {
            // Given
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Test incident")
                    .description("Test description")
                    .impact(2)
                    .urgency(4)
                    .build();

            // When
            boolean result = request.isValidUrgency();

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("isValidUrgency_withUrgency0_returnsFalse")
        void isValidUrgency_withUrgency0_returnsFalse() {
            // Given
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Test incident")
                    .description("Test description")
                    .impact(2)
                    .urgency(0)
                    .build();

            // When
            boolean result = request.isValidUrgency();

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("isValidUrgency_withUrgency5_returnsFalse")
        void isValidUrgency_withUrgency5_returnsFalse() {
            // Given
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Test incident")
                    .description("Test description")
                    .impact(2)
                    .urgency(5)
                    .build();

            // When
            boolean result = request.isValidUrgency();

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("isValidUrgency_withNullUrgency_returnsFalse")
        void isValidUrgency_withNullUrgency_returnsFalse() {
            // Given
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Test incident")
                    .description("Test description")
                    .impact(2)
                    .urgency(null)
                    .build();

            // When
            boolean result = request.isValidUrgency();

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getImpactLabel")
    class GetImpactLabel {

        @Test
        @DisplayName("getImpactLabel_withImpact1_returnsExtensiveWidespread")
        void getImpactLabel_withImpact1_returnsExtensiveWidespread() {
            // Given
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Test incident")
                    .description("Test description")
                    .impact(1)
                    .urgency(2)
                    .build();

            // When
            String label = request.getImpactLabel();

            // Then
            assertThat(label).isEqualTo("Extensive/Widespread");
        }

        @Test
        @DisplayName("getImpactLabel_withImpact2_returnsSignificantLarge")
        void getImpactLabel_withImpact2_returnsSignificantLarge() {
            // Given
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Test incident")
                    .description("Test description")
                    .impact(2)
                    .urgency(2)
                    .build();

            // When
            String label = request.getImpactLabel();

            // Then
            assertThat(label).isEqualTo("Significant/Large");
        }

        @Test
        @DisplayName("getImpactLabel_withImpact3_returnsModerateLimited")
        void getImpactLabel_withImpact3_returnsModerateLimited() {
            // Given
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Test incident")
                    .description("Test description")
                    .impact(3)
                    .urgency(2)
                    .build();

            // When
            String label = request.getImpactLabel();

            // Then
            assertThat(label).isEqualTo("Moderate/Limited");
        }

        @Test
        @DisplayName("getImpactLabel_withImpact4_returnsMinorLocalized")
        void getImpactLabel_withImpact4_returnsMinorLocalized() {
            // Given
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Test incident")
                    .description("Test description")
                    .impact(4)
                    .urgency(2)
                    .build();

            // When
            String label = request.getImpactLabel();

            // Then
            assertThat(label).isEqualTo("Minor/Localized");
        }

        @Test
        @DisplayName("getImpactLabel_withNullImpact_returnsUnknown")
        void getImpactLabel_withNullImpact_returnsUnknown() {
            // Given
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Test incident")
                    .description("Test description")
                    .impact(null)
                    .urgency(2)
                    .build();

            // When
            String label = request.getImpactLabel();

            // Then
            assertThat(label).isEqualTo("Unknown");
        }

        @Test
        @DisplayName("getImpactLabel_withInvalidImpact_returnsUnknown")
        void getImpactLabel_withInvalidImpact_returnsUnknown() {
            // Given
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Test incident")
                    .description("Test description")
                    .impact(99)
                    .urgency(2)
                    .build();

            // When
            String label = request.getImpactLabel();

            // Then
            assertThat(label).isEqualTo("Unknown");
        }
    }

    @Nested
    @DisplayName("getUrgencyLabel")
    class GetUrgencyLabel {

        @Test
        @DisplayName("getUrgencyLabel_withUrgency1_returnsCritical")
        void getUrgencyLabel_withUrgency1_returnsCritical() {
            // Given
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Test incident")
                    .description("Test description")
                    .impact(2)
                    .urgency(1)
                    .build();

            // When
            String label = request.getUrgencyLabel();

            // Then
            assertThat(label).isEqualTo("Critical");
        }

        @Test
        @DisplayName("getUrgencyLabel_withUrgency2_returnsHigh")
        void getUrgencyLabel_withUrgency2_returnsHigh() {
            // Given
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Test incident")
                    .description("Test description")
                    .impact(2)
                    .urgency(2)
                    .build();

            // When
            String label = request.getUrgencyLabel();

            // Then
            assertThat(label).isEqualTo("High");
        }

        @Test
        @DisplayName("getUrgencyLabel_withUrgency3_returnsMedium")
        void getUrgencyLabel_withUrgency3_returnsMedium() {
            // Given
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Test incident")
                    .description("Test description")
                    .impact(2)
                    .urgency(3)
                    .build();

            // When
            String label = request.getUrgencyLabel();

            // Then
            assertThat(label).isEqualTo("Medium");
        }

        @Test
        @DisplayName("getUrgencyLabel_withUrgency4_returnsLow")
        void getUrgencyLabel_withUrgency4_returnsLow() {
            // Given
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Test incident")
                    .description("Test description")
                    .impact(2)
                    .urgency(4)
                    .build();

            // When
            String label = request.getUrgencyLabel();

            // Then
            assertThat(label).isEqualTo("Low");
        }

        @Test
        @DisplayName("getUrgencyLabel_withNullUrgency_returnsUnknown")
        void getUrgencyLabel_withNullUrgency_returnsUnknown() {
            // Given
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Test incident")
                    .description("Test description")
                    .impact(2)
                    .urgency(null)
                    .build();

            // When
            String label = request.getUrgencyLabel();

            // Then
            assertThat(label).isEqualTo("Unknown");
        }

        @Test
        @DisplayName("getUrgencyLabel_withInvalidUrgency_returnsUnknown")
        void getUrgencyLabel_withInvalidUrgency_returnsUnknown() {
            // Given
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Test incident")
                    .description("Test description")
                    .impact(2)
                    .urgency(99)
                    .build();

            // When
            String label = request.getUrgencyLabel();

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
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Database connection failed")
                    .description("Unable to connect to production database")
                    .impact(1)
                    .urgency(2)
                    .build();

            // When
            String preview = request.toPreviewString();

            // Then
            assertThat(preview)
                    .contains("**New Incident Preview**")
                    .contains("**Summary:** Database connection failed")
                    .contains("**Description:** Unable to connect to production database")
                    .contains("**Impact:** Extensive/Widespread")
                    .contains("**Urgency:** High")
                    .doesNotContain("**Requester:**")
                    .doesNotContain("**Category:**")
                    .doesNotContain("**Assigned Group:**");
        }

        @Test
        @DisplayName("toPreviewString_withLongDescription_truncatesAt200Characters")
        void toPreviewString_withLongDescription_truncatesAt200Characters() {
            // Given
            String longDescription = "A".repeat(250);
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Test incident")
                    .description(longDescription)
                    .impact(3)
                    .urgency(3)
                    .build();

            // When
            String preview = request.toPreviewString();

            // Then
            assertThat(preview)
                    .contains("**Description:** " + "A".repeat(200) + "...");
        }

        @Test
        @DisplayName("toPreviewString_withShortDescription_doesNotTruncate")
        void toPreviewString_withShortDescription_doesNotTruncate() {
            // Given
            String shortDescription = "Short description";
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Test incident")
                    .description(shortDescription)
                    .impact(3)
                    .urgency(3)
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
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Test incident")
                    .description("Test description")
                    .impact(2)
                    .urgency(2)
                    .requesterFirstName("John")
                    .build();

            // When
            String preview = request.toPreviewString();

            // Then
            assertThat(preview)
                    .contains("**Requester:** John");
        }

        @Test
        @DisplayName("toPreviewString_withRequesterLastNameOnly_includesRequester")
        void toPreviewString_withRequesterLastNameOnly_includesRequester() {
            // Given
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Test incident")
                    .description("Test description")
                    .impact(2)
                    .urgency(2)
                    .requesterLastName("Doe")
                    .build();

            // When
            String preview = request.toPreviewString();

            // Then
            assertThat(preview)
                    .contains("**Requester:** Doe");
        }

        @Test
        @DisplayName("toPreviewString_withFullRequesterName_includesRequester")
        void toPreviewString_withFullRequesterName_includesRequester() {
            // Given
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Test incident")
                    .description("Test description")
                    .impact(2)
                    .urgency(2)
                    .requesterFirstName("John")
                    .requesterLastName("Doe")
                    .build();

            // When
            String preview = request.toPreviewString();

            // Then
            assertThat(preview)
                    .contains("**Requester:** John Doe");
        }

        @Test
        @DisplayName("toPreviewString_withCategoryTier1Only_includesCategory")
        void toPreviewString_withCategoryTier1Only_includesCategory() {
            // Given
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Test incident")
                    .description("Test description")
                    .impact(2)
                    .urgency(2)
                    .categoryTier1("Hardware")
                    .build();

            // When
            String preview = request.toPreviewString();

            // Then
            assertThat(preview)
                    .contains("**Category:** Hardware")
                    .doesNotContain(" > ");
        }

        @Test
        @DisplayName("toPreviewString_withFullCategoryHierarchy_includesAllTiers")
        void toPreviewString_withFullCategoryHierarchy_includesAllTiers() {
            // Given
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Test incident")
                    .description("Test description")
                    .impact(2)
                    .urgency(2)
                    .categoryTier1("Hardware")
                    .categoryTier2("Server")
                    .categoryTier3("Database")
                    .build();

            // When
            String preview = request.toPreviewString();

            // Then
            assertThat(preview)
                    .contains("**Category:** Hardware > Server > Database");
        }

        @Test
        @DisplayName("toPreviewString_withAssignedGroup_includesAssignedGroup")
        void toPreviewString_withAssignedGroup_includesAssignedGroup() {
            // Given
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Test incident")
                    .description("Test description")
                    .impact(2)
                    .urgency(2)
                    .assignedGroup("Database Support")
                    .build();

            // When
            String preview = request.toPreviewString();

            // Then
            assertThat(preview)
                    .contains("**Assigned Group:** Database Support");
        }

        @Test
        @DisplayName("toPreviewString_withConfigurationItem_includesConfigurationItem")
        void toPreviewString_withConfigurationItem_includesConfigurationItem() {
            // Given
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Test incident")
                    .description("Test description")
                    .impact(2)
                    .urgency(2)
                    .configurationItem("PROD-DB-01")
                    .build();

            // When
            String preview = request.toPreviewString();

            // Then
            assertThat(preview)
                    .contains("**Configuration Item:** PROD-DB-01");
        }

        @Test
        @DisplayName("toPreviewString_withLocation_includesLocation")
        void toPreviewString_withLocation_includesLocation() {
            // Given
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Test incident")
                    .description("Test description")
                    .impact(2)
                    .urgency(2)
                    .location("Building A, Floor 3")
                    .build();

            // When
            String preview = request.toPreviewString();

            // Then
            assertThat(preview)
                    .contains("**Location:** Building A, Floor 3");
        }

        @Test
        @DisplayName("toPreviewString_withAllOptionalFields_includesAllFields")
        void toPreviewString_withAllOptionalFields_includesAllFields() {
            // Given
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Database connection failed")
                    .description("Unable to connect to production database server")
                    .impact(1)
                    .urgency(1)
                    .requesterFirstName("John")
                    .requesterLastName("Doe")
                    .requesterCompany("Acme Corp")
                    .categoryTier1("Hardware")
                    .categoryTier2("Server")
                    .categoryTier3("Database")
                    .assignedGroup("Database Support")
                    .serviceType("User Service Restoration")
                    .configurationItem("PROD-DB-01")
                    .location("Building A, Floor 3")
                    .createdBy("admin")
                    .sessionId("session-123")
                    .build();

            // When
            String preview = request.toPreviewString();

            // Then
            assertThat(preview)
                    .contains("**New Incident Preview**")
                    .contains("**Summary:** Database connection failed")
                    .contains("**Description:** Unable to connect to production database server")
                    .contains("**Impact:** Extensive/Widespread")
                    .contains("**Urgency:** Critical")
                    .contains("**Requester:** John Doe")
                    .contains("**Category:** Hardware > Server > Database")
                    .contains("**Assigned Group:** Database Support")
                    .contains("**Configuration Item:** PROD-DB-01")
                    .contains("**Location:** Building A, Floor 3");
        }
    }

    @Nested
    @DisplayName("Lombok Generated Methods")
    class LombokGeneratedMethodsTests {

        @Test
        @DisplayName("equals returns true for same object")
        void equals_sameObject_returnsTrue() {
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Test")
                    .description("Test")
                    .impact(2)
                    .urgency(2)
                    .build();

            assertThat(request).isEqualTo(request);
        }

        @Test
        @DisplayName("equals returns true for same values")
        void equals_sameValues_returnsTrue() {
            IncidentCreationRequest req1 = IncidentCreationRequest.builder()
                    .summary("Test")
                    .description("Test")
                    .impact(2)
                    .urgency(2)
                    .build();

            IncidentCreationRequest req2 = IncidentCreationRequest.builder()
                    .summary("Test")
                    .description("Test")
                    .impact(2)
                    .urgency(2)
                    .build();

            assertThat(req1).isEqualTo(req2);
            assertThat(req1.hashCode()).isEqualTo(req2.hashCode());
        }

        @Test
        @DisplayName("equals returns false for different values")
        void equals_differentSummary_returnsFalse() {
            IncidentCreationRequest req1 = IncidentCreationRequest.builder()
                    .summary("Test1")
                    .description("Test")
                    .impact(2)
                    .urgency(2)
                    .build();

            IncidentCreationRequest req2 = IncidentCreationRequest.builder()
                    .summary("Test2")
                    .description("Test")
                    .impact(2)
                    .urgency(2)
                    .build();

            assertThat(req1).isNotEqualTo(req2);
        }

        @Test
        @DisplayName("toString includes key fields")
        void toString_includesKeyFields() {
            IncidentCreationRequest request = IncidentCreationRequest.builder()
                    .summary("Test summary")
                    .description("Test description")
                    .impact(2)
                    .urgency(3)
                    .build();

            String result = request.toString();

            assertThat(result).contains("Test summary");
            assertThat(result).contains("Test description");
            assertThat(result).contains("impact=2");
            assertThat(result).contains("urgency=3");
        }

        @Test
        @DisplayName("no-args constructor creates object")
        void noArgsConstructor_createsObject() {
            IncidentCreationRequest request = new IncidentCreationRequest();
            assertThat(request).isNotNull();
        }
    }
}
