package com.bmc.rag.api.dto.toolserver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for IncidentDetailResponse and nested classes.
 */
@DisplayName("IncidentDetailResponse Tests")
class IncidentDetailResponseTest {

    @Test
    @DisplayName("builder_shouldCreateInstanceWithAllFields")
    void builder_shouldCreateInstanceWithAllFields() {
        IncidentDetailResponse.WorkLogItem workLog = IncidentDetailResponse.WorkLogItem.builder()
            .id("WL001")
            .type("General")
            .summary("Work log summary")
            .notes("Work log notes")
            .submitter("John Doe")
            .submitDate(Instant.now())
            .build();

        IncidentDetailResponse.AttachmentItem attachment = IncidentDetailResponse.AttachmentItem.builder()
            .name("screenshot.png")
            .sizeBytes(1024L)
            .contentType("image/png")
            .build();

        IncidentDetailResponse response = IncidentDetailResponse.builder()
            .incidentNumber("INC000000001")
            .summary("Test incident")
            .description("Test description")
            .resolution("Test resolution")
            .status("Resolved")
            .statusCode(5)
            .impact(3)
            .urgency(3)
            .priority(3)
            .assignedGroup("IT Support")
            .assignedTo("Jane Smith")
            .submitter("John Doe")
            .customerFirstName("John")
            .customerLastName("Doe")
            .customerCompany("Acme Corp")
            .categoryPath("Hardware > Laptop")
            .resolutionCategoryPath("Hardware > Laptop > Fixed")
            .createDate(Instant.now())
            .lastModifiedDate(Instant.now())
            .lastModifiedBy("Jane Smith")
            .workLogs(List.of(workLog))
            .attachments(List.of(attachment))
            .found(true)
            .build();

        assertThat(response.getIncidentNumber()).isEqualTo("INC000000001");
        assertThat(response.getSummary()).isEqualTo("Test incident");
        assertThat(response.getWorkLogs()).hasSize(1);
        assertThat(response.getAttachments()).hasSize(1);
    }

    @Test
    @DisplayName("notFound_shouldCreateNotFoundResponse")
    void notFound_shouldCreateNotFoundResponse() {
        IncidentDetailResponse response = IncidentDetailResponse.notFound("INC000000001");

        assertThat(response.getIncidentNumber()).isEqualTo("INC000000001");
        assertThat(response.getFound()).isFalse();
        assertThat(response.getErrorMessage()).isEqualTo("Incident INC000000001 not found");
    }

    @Test
    @DisplayName("getImpactLabel_withImpact1_shouldReturnExtensive")
    void getImpactLabel_withImpact1_shouldReturnExtensive() {
        IncidentDetailResponse response = IncidentDetailResponse.builder()
            .impact(1)
            .build();

        assertThat(response.getImpactLabel()).isEqualTo("Extensive/Widespread");
    }

    @Test
    @DisplayName("getImpactLabel_withImpact2_shouldReturnSignificant")
    void getImpactLabel_withImpact2_shouldReturnSignificant() {
        IncidentDetailResponse response = IncidentDetailResponse.builder()
            .impact(2)
            .build();

        assertThat(response.getImpactLabel()).isEqualTo("Significant/Large");
    }

    @Test
    @DisplayName("getImpactLabel_withImpact3_shouldReturnModerate")
    void getImpactLabel_withImpact3_shouldReturnModerate() {
        IncidentDetailResponse response = IncidentDetailResponse.builder()
            .impact(3)
            .build();

        assertThat(response.getImpactLabel()).isEqualTo("Moderate/Limited");
    }

    @Test
    @DisplayName("getImpactLabel_withImpact4_shouldReturnMinor")
    void getImpactLabel_withImpact4_shouldReturnMinor() {
        IncidentDetailResponse response = IncidentDetailResponse.builder()
            .impact(4)
            .build();

        assertThat(response.getImpactLabel()).isEqualTo("Minor/Localized");
    }

    @Test
    @DisplayName("getImpactLabel_withInvalidImpact_shouldReturnUnknown")
    void getImpactLabel_withInvalidImpact_shouldReturnUnknown() {
        IncidentDetailResponse response = IncidentDetailResponse.builder()
            .impact(999)
            .build();

        assertThat(response.getImpactLabel()).isEqualTo("Unknown");
    }

    @Test
    @DisplayName("getImpactLabel_withNullImpact_shouldReturnUnknown")
    void getImpactLabel_withNullImpact_shouldReturnUnknown() {
        IncidentDetailResponse response = IncidentDetailResponse.builder().build();

        assertThat(response.getImpactLabel()).isEqualTo("Unknown");
    }

    @Test
    @DisplayName("getImpactLabel_withExistingLabel_shouldReturnLabel")
    void getImpactLabel_withExistingLabel_shouldReturnLabel() {
        IncidentDetailResponse response = IncidentDetailResponse.builder()
            .impactLabel("Custom Label")
            .build();

        assertThat(response.getImpactLabel()).isEqualTo("Custom Label");
    }

    @Test
    @DisplayName("getUrgencyLabel_withUrgency1_shouldReturnCritical")
    void getUrgencyLabel_withUrgency1_shouldReturnCritical() {
        IncidentDetailResponse response = IncidentDetailResponse.builder()
            .urgency(1)
            .build();

        assertThat(response.getUrgencyLabel()).isEqualTo("Critical");
    }

    @Test
    @DisplayName("getUrgencyLabel_withUrgency2_shouldReturnHigh")
    void getUrgencyLabel_withUrgency2_shouldReturnHigh() {
        IncidentDetailResponse response = IncidentDetailResponse.builder()
            .urgency(2)
            .build();

        assertThat(response.getUrgencyLabel()).isEqualTo("High");
    }

    @Test
    @DisplayName("getUrgencyLabel_withUrgency3_shouldReturnMedium")
    void getUrgencyLabel_withUrgency3_shouldReturnMedium() {
        IncidentDetailResponse response = IncidentDetailResponse.builder()
            .urgency(3)
            .build();

        assertThat(response.getUrgencyLabel()).isEqualTo("Medium");
    }

    @Test
    @DisplayName("getUrgencyLabel_withUrgency4_shouldReturnLow")
    void getUrgencyLabel_withUrgency4_shouldReturnLow() {
        IncidentDetailResponse response = IncidentDetailResponse.builder()
            .urgency(4)
            .build();

        assertThat(response.getUrgencyLabel()).isEqualTo("Low");
    }

    @Test
    @DisplayName("getUrgencyLabel_withInvalidUrgency_shouldReturnUnknown")
    void getUrgencyLabel_withInvalidUrgency_shouldReturnUnknown() {
        IncidentDetailResponse response = IncidentDetailResponse.builder()
            .urgency(999)
            .build();

        assertThat(response.getUrgencyLabel()).isEqualTo("Unknown");
    }

    @Test
    @DisplayName("getUrgencyLabel_withNullUrgency_shouldReturnUnknown")
    void getUrgencyLabel_withNullUrgency_shouldReturnUnknown() {
        IncidentDetailResponse response = IncidentDetailResponse.builder().build();

        assertThat(response.getUrgencyLabel()).isEqualTo("Unknown");
    }

    @Test
    @DisplayName("getUrgencyLabel_withExistingLabel_shouldReturnLabel")
    void getUrgencyLabel_withExistingLabel_shouldReturnLabel() {
        IncidentDetailResponse response = IncidentDetailResponse.builder()
            .urgencyLabel("Custom Urgency")
            .build();

        assertThat(response.getUrgencyLabel()).isEqualTo("Custom Urgency");
    }

    @Test
    @DisplayName("getPriorityLabel_withPriority1_shouldReturnCritical")
    void getPriorityLabel_withPriority1_shouldReturnCritical() {
        IncidentDetailResponse response = IncidentDetailResponse.builder()
            .priority(1)
            .build();

        assertThat(response.getPriorityLabel()).isEqualTo("Critical");
    }

    @Test
    @DisplayName("getPriorityLabel_withPriority2_shouldReturnHigh")
    void getPriorityLabel_withPriority2_shouldReturnHigh() {
        IncidentDetailResponse response = IncidentDetailResponse.builder()
            .priority(2)
            .build();

        assertThat(response.getPriorityLabel()).isEqualTo("High");
    }

    @Test
    @DisplayName("getPriorityLabel_withPriority3_shouldReturnMedium")
    void getPriorityLabel_withPriority3_shouldReturnMedium() {
        IncidentDetailResponse response = IncidentDetailResponse.builder()
            .priority(3)
            .build();

        assertThat(response.getPriorityLabel()).isEqualTo("Medium");
    }

    @Test
    @DisplayName("getPriorityLabel_withPriority4_shouldReturnLow")
    void getPriorityLabel_withPriority4_shouldReturnLow() {
        IncidentDetailResponse response = IncidentDetailResponse.builder()
            .priority(4)
            .build();

        assertThat(response.getPriorityLabel()).isEqualTo("Low");
    }

    @Test
    @DisplayName("getPriorityLabel_withInvalidPriority_shouldReturnUnknown")
    void getPriorityLabel_withInvalidPriority_shouldReturnUnknown() {
        IncidentDetailResponse response = IncidentDetailResponse.builder()
            .priority(999)
            .build();

        assertThat(response.getPriorityLabel()).isEqualTo("Unknown");
    }

    @Test
    @DisplayName("getPriorityLabel_withNullPriority_shouldReturnUnknown")
    void getPriorityLabel_withNullPriority_shouldReturnUnknown() {
        IncidentDetailResponse response = IncidentDetailResponse.builder().build();

        assertThat(response.getPriorityLabel()).isEqualTo("Unknown");
    }

    @Test
    @DisplayName("getPriorityLabel_withExistingLabel_shouldReturnLabel")
    void getPriorityLabel_withExistingLabel_shouldReturnLabel() {
        IncidentDetailResponse response = IncidentDetailResponse.builder()
            .priorityLabel("Custom Priority")
            .build();

        assertThat(response.getPriorityLabel()).isEqualTo("Custom Priority");
    }

    @Test
    @DisplayName("workLogItem_builder_shouldWork")
    void workLogItem_builder_shouldWork() {
        Instant now = Instant.now();
        IncidentDetailResponse.WorkLogItem workLog = IncidentDetailResponse.WorkLogItem.builder()
            .id("WL001")
            .type("General")
            .summary("Summary")
            .notes("Notes")
            .submitter("John")
            .submitDate(now)
            .build();

        assertThat(workLog.getId()).isEqualTo("WL001");
        assertThat(workLog.getType()).isEqualTo("General");
        assertThat(workLog.getSummary()).isEqualTo("Summary");
        assertThat(workLog.getNotes()).isEqualTo("Notes");
        assertThat(workLog.getSubmitter()).isEqualTo("John");
        assertThat(workLog.getSubmitDate()).isEqualTo(now);
    }

    @Test
    @DisplayName("workLogItem_noArgsConstructor_shouldWork")
    void workLogItem_noArgsConstructor_shouldWork() {
        IncidentDetailResponse.WorkLogItem workLog = new IncidentDetailResponse.WorkLogItem();
        assertThat(workLog).isNotNull();
    }

    @Test
    @DisplayName("workLogItem_allArgsConstructor_shouldWork")
    void workLogItem_allArgsConstructor_shouldWork() {
        Instant now = Instant.now();
        IncidentDetailResponse.WorkLogItem workLog = new IncidentDetailResponse.WorkLogItem(
            "WL001", "General", "Summary", "Notes", "John", now
        );

        assertThat(workLog.getId()).isEqualTo("WL001");
        assertThat(workLog.getType()).isEqualTo("General");
    }

    @Test
    @DisplayName("attachmentItem_builder_shouldWork")
    void attachmentItem_builder_shouldWork() {
        IncidentDetailResponse.AttachmentItem attachment = IncidentDetailResponse.AttachmentItem.builder()
            .name("file.pdf")
            .sizeBytes(2048L)
            .contentType("application/pdf")
            .build();

        assertThat(attachment.getName()).isEqualTo("file.pdf");
        assertThat(attachment.getSizeBytes()).isEqualTo(2048L);
        assertThat(attachment.getContentType()).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("attachmentItem_noArgsConstructor_shouldWork")
    void attachmentItem_noArgsConstructor_shouldWork() {
        IncidentDetailResponse.AttachmentItem attachment = new IncidentDetailResponse.AttachmentItem();
        assertThat(attachment).isNotNull();
    }

    @Test
    @DisplayName("attachmentItem_allArgsConstructor_shouldWork")
    void attachmentItem_allArgsConstructor_shouldWork() {
        IncidentDetailResponse.AttachmentItem attachment = new IncidentDetailResponse.AttachmentItem(
            "file.pdf", 2048L, "application/pdf"
        );

        assertThat(attachment.getName()).isEqualTo("file.pdf");
        assertThat(attachment.getSizeBytes()).isEqualTo(2048L);
    }

    @Test
    @DisplayName("defaultBuilderValues_shouldCreateEmptyLists")
    void defaultBuilderValues_shouldCreateEmptyLists() {
        IncidentDetailResponse response = IncidentDetailResponse.builder()
            .incidentNumber("INC001")
            .build();

        assertThat(response.getWorkLogs()).isNotNull().isEmpty();
        assertThat(response.getAttachments()).isNotNull().isEmpty();
        assertThat(response.getFound()).isTrue();
    }
}
