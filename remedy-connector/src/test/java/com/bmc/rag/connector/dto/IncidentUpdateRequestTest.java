package com.bmc.rag.connector.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for IncidentUpdateRequest.
 */
class IncidentUpdateRequestTest {

    @Test
    void isValidImpact_validValues_returnsTrue() {
        // Given
        IncidentUpdateRequest request1 = IncidentUpdateRequest.builder().impact(1).build();
        IncidentUpdateRequest request2 = IncidentUpdateRequest.builder().impact(2).build();
        IncidentUpdateRequest request3 = IncidentUpdateRequest.builder().impact(3).build();
        IncidentUpdateRequest request4 = IncidentUpdateRequest.builder().impact(4).build();

        // Then
        assertThat(request1.isValidImpact()).isTrue();
        assertThat(request2.isValidImpact()).isTrue();
        assertThat(request3.isValidImpact()).isTrue();
        assertThat(request4.isValidImpact()).isTrue();
    }

    @Test
    void isValidImpact_nullValue_returnsTrue() {
        // Given
        IncidentUpdateRequest request = IncidentUpdateRequest.builder().impact(null).build();

        // Then
        assertThat(request.isValidImpact()).isTrue();
    }

    @Test
    void isValidImpact_invalidValues_returnsFalse() {
        // Given
        IncidentUpdateRequest request1 = IncidentUpdateRequest.builder().impact(0).build();
        IncidentUpdateRequest request2 = IncidentUpdateRequest.builder().impact(5).build();
        IncidentUpdateRequest request3 = IncidentUpdateRequest.builder().impact(-1).build();

        // Then
        assertThat(request1.isValidImpact()).isFalse();
        assertThat(request2.isValidImpact()).isFalse();
        assertThat(request3.isValidImpact()).isFalse();
    }

    @Test
    void isValidUrgency_validValues_returnsTrue() {
        // Given
        IncidentUpdateRequest request1 = IncidentUpdateRequest.builder().urgency(1).build();
        IncidentUpdateRequest request2 = IncidentUpdateRequest.builder().urgency(2).build();
        IncidentUpdateRequest request3 = IncidentUpdateRequest.builder().urgency(3).build();
        IncidentUpdateRequest request4 = IncidentUpdateRequest.builder().urgency(4).build();

        // Then
        assertThat(request1.isValidUrgency()).isTrue();
        assertThat(request2.isValidUrgency()).isTrue();
        assertThat(request3.isValidUrgency()).isTrue();
        assertThat(request4.isValidUrgency()).isTrue();
    }

    @Test
    void isValidUrgency_nullValue_returnsTrue() {
        // Given
        IncidentUpdateRequest request = IncidentUpdateRequest.builder().urgency(null).build();

        // Then
        assertThat(request.isValidUrgency()).isTrue();
    }

    @Test
    void isValidUrgency_invalidValues_returnsFalse() {
        // Given
        IncidentUpdateRequest request1 = IncidentUpdateRequest.builder().urgency(0).build();
        IncidentUpdateRequest request2 = IncidentUpdateRequest.builder().urgency(5).build();

        // Then
        assertThat(request1.isValidUrgency()).isFalse();
        assertThat(request2.isValidUrgency()).isFalse();
    }

    @Test
    void isValidStatus_validValues_returnsTrue() {
        // Given - Status 0-6 are valid
        for (int status = 0; status <= 6; status++) {
            IncidentUpdateRequest request = IncidentUpdateRequest.builder().status(status).build();
            assertThat(request.isValidStatus()).isTrue();
        }
    }

    @Test
    void isValidStatus_nullValue_returnsTrue() {
        // Given
        IncidentUpdateRequest request = IncidentUpdateRequest.builder().status(null).build();

        // Then
        assertThat(request.isValidStatus()).isTrue();
    }

    @Test
    void isValidStatus_invalidValues_returnsFalse() {
        // Given
        IncidentUpdateRequest request1 = IncidentUpdateRequest.builder().status(-1).build();
        IncidentUpdateRequest request2 = IncidentUpdateRequest.builder().status(7).build();

        // Then
        assertThat(request1.isValidStatus()).isFalse();
        assertThat(request2.isValidStatus()).isFalse();
    }

    @Test
    void hasUpdates_withSummary_returnsTrue() {
        // Given
        IncidentUpdateRequest request = IncidentUpdateRequest.builder()
            .summary("Updated summary")
            .build();

        // Then
        assertThat(request.hasUpdates()).isTrue();
    }

    @Test
    void hasUpdates_withMultipleFields_returnsTrue() {
        // Given
        IncidentUpdateRequest request = IncidentUpdateRequest.builder()
            .summary("Updated summary")
            .description("Updated description")
            .impact(2)
            .urgency(3)
            .build();

        // Then
        assertThat(request.hasUpdates()).isTrue();
    }

    @Test
    void hasUpdates_noFields_returnsFalse() {
        // Given
        IncidentUpdateRequest request = IncidentUpdateRequest.builder()
            .incidentNumber("INC000001")
            .build();

        // Then
        assertThat(request.hasUpdates()).isFalse();
    }

    @Test
    void getStatusLabel_allStatuses_returnsCorrectLabels() {
        // Given/When/Then
        assertThat(IncidentUpdateRequest.builder().status(0).build().getStatusLabel()).isEqualTo("New");
        assertThat(IncidentUpdateRequest.builder().status(1).build().getStatusLabel()).isEqualTo("Assigned");
        assertThat(IncidentUpdateRequest.builder().status(2).build().getStatusLabel()).isEqualTo("In Progress");
        assertThat(IncidentUpdateRequest.builder().status(3).build().getStatusLabel()).isEqualTo("Pending");
        assertThat(IncidentUpdateRequest.builder().status(4).build().getStatusLabel()).isEqualTo("Resolved");
        assertThat(IncidentUpdateRequest.builder().status(5).build().getStatusLabel()).isEqualTo("Closed");
        assertThat(IncidentUpdateRequest.builder().status(6).build().getStatusLabel()).isEqualTo("Cancelled");
    }

    @Test
    void getStatusLabel_nullStatus_returnsNull() {
        // Given
        IncidentUpdateRequest request = IncidentUpdateRequest.builder().status(null).build();

        // Then
        assertThat(request.getStatusLabel()).isNull();
    }

    @Test
    void getStatusLabel_unknownStatus_returnsUnknown() {
        // Given
        IncidentUpdateRequest request = IncidentUpdateRequest.builder().status(99).build();

        // Then
        assertThat(request.getStatusLabel()).isEqualTo("Unknown");
    }

    @Test
    void getImpactLabel_allImpacts_returnsCorrectLabels() {
        // Given/When/Then
        assertThat(IncidentUpdateRequest.builder().impact(1).build().getImpactLabel()).isEqualTo("Extensive/Widespread");
        assertThat(IncidentUpdateRequest.builder().impact(2).build().getImpactLabel()).isEqualTo("Significant/Large");
        assertThat(IncidentUpdateRequest.builder().impact(3).build().getImpactLabel()).isEqualTo("Moderate/Limited");
        assertThat(IncidentUpdateRequest.builder().impact(4).build().getImpactLabel()).isEqualTo("Minor/Localized");
    }

    @Test
    void getImpactLabel_nullImpact_returnsNull() {
        // Given
        IncidentUpdateRequest request = IncidentUpdateRequest.builder().impact(null).build();

        // Then
        assertThat(request.getImpactLabel()).isNull();
    }

    @Test
    void getUrgencyLabel_allUrgencies_returnsCorrectLabels() {
        // Given/When/Then
        assertThat(IncidentUpdateRequest.builder().urgency(1).build().getUrgencyLabel()).isEqualTo("Critical");
        assertThat(IncidentUpdateRequest.builder().urgency(2).build().getUrgencyLabel()).isEqualTo("High");
        assertThat(IncidentUpdateRequest.builder().urgency(3).build().getUrgencyLabel()).isEqualTo("Medium");
        assertThat(IncidentUpdateRequest.builder().urgency(4).build().getUrgencyLabel()).isEqualTo("Low");
    }

    @Test
    void getUrgencyLabel_nullUrgency_returnsNull() {
        // Given
        IncidentUpdateRequest request = IncidentUpdateRequest.builder().urgency(null).build();

        // Then
        assertThat(request.getUrgencyLabel()).isNull();
    }

    @Test
    void toPreviewString_allFields_formatsCorrectly() {
        // Given
        IncidentUpdateRequest request = IncidentUpdateRequest.builder()
            .incidentNumber("INC000001")
            .summary("VPN connection issue")
            .description("User cannot connect to VPN")
            .impact(2)
            .urgency(3)
            .status(4)
            .resolution("Reset VPN credentials")
            .workLog("Contacted user and resolved issue")
            .assignedGroup("Network Support")
            .categoryTier1("Infrastructure")
            .categoryTier2("Network")
            .categoryTier3("VPN")
            .build();

        // When
        String preview = request.toPreviewString();

        // Then
        assertThat(preview).contains("**Incident Update Preview**");
        assertThat(preview).contains("**Incident:** INC000001");
        assertThat(preview).contains("**Summary:** VPN connection issue");
        assertThat(preview).contains("**Description:**");
        assertThat(preview).contains("**Impact:** Significant/Large");
        assertThat(preview).contains("**Urgency:** Medium");
        assertThat(preview).contains("**Status:** Resolved");
        assertThat(preview).contains("**Resolution:**");
        assertThat(preview).contains("**Work Log:**");
        assertThat(preview).contains("**Assigned Group:** Network Support");
        assertThat(preview).contains("**Category:** Infrastructure > Network > VPN");
    }

    @Test
    void toPreviewString_longDescription_truncates() {
        // Given
        String longDescription = "A".repeat(250);
        IncidentUpdateRequest request = IncidentUpdateRequest.builder()
            .incidentNumber("INC000001")
            .description(longDescription)
            .build();

        // When
        String preview = request.toPreviewString();

        // Then
        assertThat(preview).contains("...");
        assertThat(preview).doesNotContain(longDescription);
    }

    @Test
    void getUpdateSummary_multipleFields_summarizesCorrectly() {
        // Given
        IncidentUpdateRequest request = IncidentUpdateRequest.builder()
            .summary("Updated")
            .status(4)
            .resolution("Fixed")
            .assignedGroup("Network")
            .build();

        // When
        String summary = request.getUpdateSummary();

        // Then
        assertThat(summary).isEqualTo("Updating: summary, status → Resolved, resolution, assignment");
    }

    @Test
    void getUpdateSummary_noUpdates_returnsNoChanges() {
        // Given
        IncidentUpdateRequest request = IncidentUpdateRequest.builder()
            .incidentNumber("INC000001")
            .build();

        // When
        String summary = request.getUpdateSummary();

        // Then
        assertThat(summary).isEqualTo("No changes");
    }

    @Test
    void getUpdateSummary_singleField_formatsWithoutComma() {
        // Given
        IncidentUpdateRequest request = IncidentUpdateRequest.builder()
            .workLog("Added work log")
            .build();

        // When
        String summary = request.getUpdateSummary();

        // Then
        assertThat(summary).isEqualTo("Updating: work log");
    }

    @Test
    void equals_sameObject_returnsTrue() {
        // Given
        IncidentUpdateRequest request = IncidentUpdateRequest.builder()
            .incidentNumber("INC000001")
            .summary("Test")
            .build();

        // Then
        assertThat(request).isEqualTo(request);
    }

    @Test
    void equals_sameValues_returnsTrue() {
        // Given
        IncidentUpdateRequest req1 = IncidentUpdateRequest.builder()
            .incidentNumber("INC000001")
            .summary("Test")
            .status(4)
            .build();

        IncidentUpdateRequest req2 = IncidentUpdateRequest.builder()
            .incidentNumber("INC000001")
            .summary("Test")
            .status(4)
            .build();

        // Then
        assertThat(req1).isEqualTo(req2);
        assertThat(req1.hashCode()).isEqualTo(req2.hashCode());
    }

    @Test
    void equals_differentIncidentNumber_returnsFalse() {
        // Given
        IncidentUpdateRequest req1 = IncidentUpdateRequest.builder()
            .incidentNumber("INC000001")
            .build();

        IncidentUpdateRequest req2 = IncidentUpdateRequest.builder()
            .incidentNumber("INC000002")
            .build();

        // Then
        assertThat(req1).isNotEqualTo(req2);
    }

    @Test
    void toString_includesKeyFields() {
        // Given
        IncidentUpdateRequest request = IncidentUpdateRequest.builder()
            .incidentNumber("INC000001")
            .summary("Test summary")
            .status(4)
            .build();

        // When
        String result = request.toString();

        // Then
        assertThat(result).contains("INC000001");
        assertThat(result).contains("Test summary");
        assertThat(result).contains("status=4");
    }

    @Test
    void getImpactLabel_unknownImpact_returnsUnknown() {
        // Given
        IncidentUpdateRequest request = IncidentUpdateRequest.builder()
            .impact(99)
            .build();

        // Then
        assertThat(request.getImpactLabel()).isEqualTo("Unknown");
    }

    @Test
    void getUrgencyLabel_unknownUrgency_returnsUnknown() {
        // Given
        IncidentUpdateRequest request = IncidentUpdateRequest.builder()
            .urgency(99)
            .build();

        // Then
        assertThat(request.getUrgencyLabel()).isEqualTo("Unknown");
    }

    @Test
    void hasUpdates_withImpact_returnsTrue() {
        // Given
        IncidentUpdateRequest request = IncidentUpdateRequest.builder()
            .impact(2)
            .build();

        // Then
        assertThat(request.hasUpdates()).isTrue();
    }

    @Test
    void hasUpdates_withUrgency_returnsTrue() {
        // Given
        IncidentUpdateRequest request = IncidentUpdateRequest.builder()
            .urgency(3)
            .build();

        // Then
        assertThat(request.hasUpdates()).isTrue();
    }

    @Test
    void hasUpdates_withStatus_returnsTrue() {
        // Given
        IncidentUpdateRequest request = IncidentUpdateRequest.builder()
            .status(4)
            .build();

        // Then
        assertThat(request.hasUpdates()).isTrue();
    }

    @Test
    void hasUpdates_withResolution_returnsTrue() {
        // Given
        IncidentUpdateRequest request = IncidentUpdateRequest.builder()
            .resolution("Fixed")
            .build();

        // Then
        assertThat(request.hasUpdates()).isTrue();
    }

    @Test
    void toPreviewString_withCategoryTier2_includesInPreview() {
        // Given
        IncidentUpdateRequest request = IncidentUpdateRequest.builder()
            .incidentNumber("INC000001")
            .categoryTier1("Infrastructure")
            .categoryTier2("Network")
            .build();

        // When
        String preview = request.toPreviewString();

        // Then
        assertThat(preview).contains("**Category:** Infrastructure > Network");
    }

    @Test
    void getUpdateSummary_withDescription_includesDescription() {
        // Given
        IncidentUpdateRequest request = IncidentUpdateRequest.builder()
            .description("Updated description")
            .build();

        // When
        String summary = request.getUpdateSummary();

        // Then
        assertThat(summary).contains("description");
    }

    @Test
    void getUpdateSummary_withImpactAndUrgency_includesFields() {
        // Given
        IncidentUpdateRequest request = IncidentUpdateRequest.builder()
            .summary("Updated")
            .description("New desc")
            .build();

        // When
        String summary = request.getUpdateSummary();

        // Then
        assertThat(summary).contains("summary");
        assertThat(summary).contains("description");
    }
}
