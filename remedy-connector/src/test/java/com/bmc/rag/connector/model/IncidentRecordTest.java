package com.bmc.rag.connector.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for IncidentRecord.
 */
class IncidentRecordTest {

    @Test
    void getRecordType_returnsIncident() {
        // Given
        IncidentRecord record = IncidentRecord.builder().build();

        // Then
        assertThat(record.getRecordType()).isEqualTo("Incident");
    }

    @Test
    void getRecordId_returnsIncidentNumber() {
        // Given
        IncidentRecord record = IncidentRecord.builder()
            .incidentNumber("INC000001")
            .build();

        // Then
        assertThat(record.getRecordId()).isEqualTo("INC000001");
    }

    @Test
    void getTitle_returnsSummary() {
        // Given
        IncidentRecord record = IncidentRecord.builder()
            .summary("VPN connection issue")
            .build();

        // Then
        assertThat(record.getTitle()).isEqualTo("VPN connection issue");
    }

    @Test
    void getContent_returnsDescription() {
        // Given
        IncidentRecord record = IncidentRecord.builder()
            .description("User cannot connect to VPN")
            .build();

        // Then
        assertThat(record.getContent()).isEqualTo("User cannot connect to VPN");
    }

    @Test
    void getCategoryPath_allTiers_formatsCategoryPath() {
        // Given
        IncidentRecord record = IncidentRecord.builder()
            .categoryTier1("Infrastructure")
            .categoryTier2("Network")
            .categoryTier3("VPN")
            .build();

        // When
        String path = record.getCategoryPath();

        // Then
        assertThat(path).isEqualTo("Infrastructure > Network > VPN");
    }

    @Test
    void getCategoryPath_onlyTier1_returnsSingleTier() {
        // Given
        IncidentRecord record = IncidentRecord.builder()
            .categoryTier1("Infrastructure")
            .build();

        // When
        String path = record.getCategoryPath();

        // Then
        assertThat(path).isEqualTo("Infrastructure");
    }

    @Test
    void getCategoryPath_tier1And2_returnsPartialPath() {
        // Given
        IncidentRecord record = IncidentRecord.builder()
            .categoryTier1("Infrastructure")
            .categoryTier2("Network")
            .build();

        // When
        String path = record.getCategoryPath();

        // Then
        assertThat(path).isEqualTo("Infrastructure > Network");
    }

    @Test
    void getCategoryPath_noTiers_returnsEmptyString() {
        // Given
        IncidentRecord record = IncidentRecord.builder().build();

        // When
        String path = record.getCategoryPath();

        // Then
        assertThat(path).isEmpty();
    }

    @Test
    void getCustomerFullName_firstAndLast_combinesNames() {
        // Given
        IncidentRecord record = IncidentRecord.builder()
            .customerFirstName("John")
            .customerLastName("Doe")
            .build();

        // When
        String fullName = record.getCustomerFullName();

        // Then
        assertThat(fullName).isEqualTo("John Doe");
    }

    @Test
    void getCustomerFullName_onlyFirst_returnsFirstName() {
        // Given
        IncidentRecord record = IncidentRecord.builder()
            .customerFirstName("John")
            .build();

        // When
        String fullName = record.getCustomerFullName();

        // Then
        assertThat(fullName).isEqualTo("John");
    }

    @Test
    void getCustomerFullName_onlyLast_returnsLastName() {
        // Given
        IncidentRecord record = IncidentRecord.builder()
            .customerLastName("Doe")
            .build();

        // When
        String fullName = record.getCustomerFullName();

        // Then
        assertThat(fullName).isEqualTo("Doe");
    }

    @Test
    void getCustomerFullName_noNames_returnsNull() {
        // Given
        IncidentRecord record = IncidentRecord.builder().build();

        // When
        String fullName = record.getCustomerFullName();

        // Then
        assertThat(fullName).isNull();
    }

    @Test
    void hasResolution_withResolution_returnsTrue() {
        // Given
        IncidentRecord record = IncidentRecord.builder()
            .resolution("Reset VPN credentials")
            .build();

        // Then
        assertThat(record.hasResolution()).isTrue();
    }

    @Test
    void hasResolution_withEmptyResolution_returnsFalse() {
        // Given
        IncidentRecord record = IncidentRecord.builder()
            .resolution("")
            .build();

        // Then
        assertThat(record.hasResolution()).isFalse();
    }

    @Test
    void hasResolution_withWhitespaceResolution_returnsFalse() {
        // Given
        IncidentRecord record = IncidentRecord.builder()
            .resolution("   ")
            .build();

        // Then
        assertThat(record.hasResolution()).isFalse();
    }

    @Test
    void hasResolution_nullResolution_returnsFalse() {
        // Given
        IncidentRecord record = IncidentRecord.builder()
            .resolution(null)
            .build();

        // Then
        assertThat(record.hasResolution()).isFalse();
    }

    @Test
    void isClosed_resolvedStatus_returnsTrue() {
        // Given - Status 4 = Resolved
        IncidentRecord record = IncidentRecord.builder()
            .status(4)
            .build();

        // Then
        assertThat(record.isClosed()).isTrue();
    }

    @Test
    void isClosed_closedStatus_returnsTrue() {
        // Given - Status 5 = Closed
        IncidentRecord record = IncidentRecord.builder()
            .status(5)
            .build();

        // Then
        assertThat(record.isClosed()).isTrue();
    }

    @Test
    void isClosed_cancelledStatus_returnsTrue() {
        // Given - Status 6 = Cancelled
        IncidentRecord record = IncidentRecord.builder()
            .status(6)
            .build();

        // Then
        assertThat(record.isClosed()).isTrue();
    }

    @Test
    void isClosed_newStatus_returnsFalse() {
        // Given - Status 0 = New
        IncidentRecord record = IncidentRecord.builder()
            .status(0)
            .build();

        // Then
        assertThat(record.isClosed()).isFalse();
    }

    @Test
    void isClosed_inProgressStatus_returnsFalse() {
        // Given - Status 2 = In Progress
        IncidentRecord record = IncidentRecord.builder()
            .status(2)
            .build();

        // Then
        assertThat(record.isClosed()).isFalse();
    }

    @Test
    void isClosed_nullStatus_returnsFalse() {
        // Given
        IncidentRecord record = IncidentRecord.builder()
            .status(null)
            .build();

        // Then
        assertThat(record.isClosed()).isFalse();
    }

    @Test
    void builder_allFields_buildsCorrectly() {
        // Given
        Instant now = Instant.now();
        List<WorkLogEntry> workLogs = new ArrayList<>();
        List<AttachmentInfo> attachments = new ArrayList<>();

        // When
        IncidentRecord record = IncidentRecord.builder()
            .entryId("entry-123")
            .incidentNumber("INC000001")
            .summary("VPN connection issue")
            .description("User cannot connect to VPN")
            .resolution("Reset credentials")
            .status(4)
            .statusDisplayValue("Resolved")
            .urgency(3)
            .impact(2)
            .priority(3)
            .assignedGroup("Network Support")
            .assignedTo("john.doe")
            .assignedSupportCompany("Acme Corp")
            .assignedSupportOrg("IT Support")
            .submitter("jane.smith")
            .createDate(now)
            .lastModifiedDate(now)
            .lastModifiedBy("system")
            .categoryTier1("Infrastructure")
            .categoryTier2("Network")
            .categoryTier3("VPN")
            .productTier1("Software")
            .productTier2("VPN Client")
            .productTier3("Version 2.0")
            .resolutionCategoryTier1("Password")
            .resolutionCategoryTier2("Reset")
            .resolutionCategoryTier3("Credentials")
            .customerFirstName("John")
            .customerLastName("Doe")
            .customerCompany("Acme Corp")
            .reportedSource("Self-Service")
            .serviceType("User Service Request")
            .workLogs(workLogs)
            .attachments(attachments)
            .build();

        // Then
        assertThat(record.getEntryId()).isEqualTo("entry-123");
        assertThat(record.getIncidentNumber()).isEqualTo("INC000001");
        assertThat(record.getSummary()).isEqualTo("VPN connection issue");
        assertThat(record.getDescription()).isEqualTo("User cannot connect to VPN");
        assertThat(record.getResolution()).isEqualTo("Reset credentials");
        assertThat(record.getStatus()).isEqualTo(4);
        assertThat(record.getUrgency()).isEqualTo(3);
        assertThat(record.getImpact()).isEqualTo(2);
        assertThat(record.getAssignedGroup()).isEqualTo("Network Support");
        assertThat(record.getCustomerFullName()).isEqualTo("John Doe");
        assertThat(record.getCategoryPath()).isEqualTo("Infrastructure > Network > VPN");
        assertThat(record.hasResolution()).isTrue();
        assertThat(record.isClosed()).isTrue();
    }

    @Test
    void builder_defaultCollections_initializesEmptyLists() {
        // When
        IncidentRecord record = IncidentRecord.builder().build();

        // Then
        assertThat(record.getWorkLogs()).isNotNull();
        assertThat(record.getWorkLogs()).isEmpty();
        assertThat(record.getAttachments()).isNotNull();
        assertThat(record.getAttachments()).isEmpty();
    }

    @Test
    void equals_sameObject_returnsTrue() {
        // Given
        IncidentRecord record = IncidentRecord.builder()
            .incidentNumber("INC000001")
            .summary("Test")
            .build();

        // Then
        assertThat(record).isEqualTo(record);
    }

    @Test
    void equals_sameValues_returnsTrue() {
        // Given
        IncidentRecord record1 = IncidentRecord.builder()
            .incidentNumber("INC000001")
            .summary("Test")
            .description("Test description")
            .status(1)
            .build();

        IncidentRecord record2 = IncidentRecord.builder()
            .incidentNumber("INC000001")
            .summary("Test")
            .description("Test description")
            .status(1)
            .build();

        // Then
        assertThat(record1).isEqualTo(record2);
        assertThat(record1.hashCode()).isEqualTo(record2.hashCode());
    }

    @Test
    void equals_differentIncidentNumber_returnsFalse() {
        // Given
        IncidentRecord record1 = IncidentRecord.builder()
            .incidentNumber("INC000001")
            .summary("Test")
            .build();

        IncidentRecord record2 = IncidentRecord.builder()
            .incidentNumber("INC000002")
            .summary("Test")
            .build();

        // Then
        assertThat(record1).isNotEqualTo(record2);
    }

    @Test
    void equals_differentSummary_returnsFalse() {
        // Given
        IncidentRecord record1 = IncidentRecord.builder()
            .incidentNumber("INC000001")
            .summary("Test 1")
            .build();

        IncidentRecord record2 = IncidentRecord.builder()
            .incidentNumber("INC000001")
            .summary("Test 2")
            .build();

        // Then
        assertThat(record1).isNotEqualTo(record2);
    }

    @Test
    void equals_nullObject_returnsFalse() {
        // Given
        IncidentRecord record = IncidentRecord.builder()
            .incidentNumber("INC000001")
            .build();

        // Then
        assertThat(record).isNotEqualTo(null);
    }

    @Test
    void equals_differentType_returnsFalse() {
        // Given
        IncidentRecord record = IncidentRecord.builder()
            .incidentNumber("INC000001")
            .build();

        // Then
        assertThat(record).isNotEqualTo("INC000001");
    }

    @Test
    void hashCode_sameValues_returnsSameHashCode() {
        // Given
        IncidentRecord record1 = IncidentRecord.builder()
            .incidentNumber("INC000001")
            .summary("Test")
            .build();

        IncidentRecord record2 = IncidentRecord.builder()
            .incidentNumber("INC000001")
            .summary("Test")
            .build();

        // Then
        assertThat(record1.hashCode()).isEqualTo(record2.hashCode());
    }

    @Test
    void toString_includesKeyFields() {
        // Given
        IncidentRecord record = IncidentRecord.builder()
            .incidentNumber("INC000001")
            .summary("VPN issue")
            .status(2)
            .build();

        // When
        String result = record.toString();

        // Then
        assertThat(result).contains("INC000001");
        assertThat(result).contains("VPN issue");
        assertThat(result).contains("status=2");
    }

    @Test
    void categoryPath_tier1WithTier3ButNoTier2_ignoresTier3() {
        // Given
        IncidentRecord record = IncidentRecord.builder()
            .categoryTier1("Infrastructure")
            .categoryTier3("VPN")
            .build();

        // When
        String path = record.getCategoryPath();

        // Then
        assertThat(path).isEqualTo("Infrastructure");
        assertThat(path).doesNotContain("VPN");
    }
}
