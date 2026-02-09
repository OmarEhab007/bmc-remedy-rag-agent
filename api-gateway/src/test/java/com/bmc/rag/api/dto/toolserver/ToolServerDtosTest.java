package com.bmc.rag.api.dto.toolserver;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive unit tests for Tool Server DTOs.
 * Groups related tests for better organization.
 */
class ToolServerDtosTest {

    @Nested
    class UpdateIncidentRequestTests {

        @Test
        void builder_shouldCreateInstanceWithAllFields() {
            // When
            UpdateIncidentRequest request = UpdateIncidentRequest.builder()
                .status("Resolved")
                .statusReason("Fixed")
                .resolution("Issue resolved")
                .resolutionCategory("Software")
                .resolutionSubCategory("Application")
                .resolutionItem("Bug Fix")
                .workLogNotes("Updated incident")
                .workLogType("Working Log")
                .impact(2)
                .urgency(3)
                .assignedGroup("Support Team")
                .assignedTo("John Doe")
                .requireConfirmation(true)
                .sessionId("session-123")
                .build();

            // Then
            assertThat(request.getStatus()).isEqualTo("Resolved");
            assertThat(request.getResolution()).isEqualTo("Issue resolved");
            assertThat(request.getImpact()).isEqualTo(2);
            assertThat(request.getUrgency()).isEqualTo(3);
            assertThat(request.getRequireConfirmation()).isTrue();
        }

        @Test
        void hasUpdates_shouldReturnTrueWhenFieldsAreSet() {
            // Given
            UpdateIncidentRequest request = UpdateIncidentRequest.builder()
                .status("Resolved")
                .build();

            // Then
            assertThat(request.hasUpdates()).isTrue();
        }

        @Test
        void hasUpdates_shouldReturnFalseWhenNoFieldsSet() {
            // Given
            UpdateIncidentRequest request = UpdateIncidentRequest.builder()
                .requireConfirmation(true)
                .build();

            // Then
            assertThat(request.hasUpdates()).isFalse();
        }

        @Test
        void getUpdateSummary_shouldFormatAllUpdates() {
            // Given
            UpdateIncidentRequest request = UpdateIncidentRequest.builder()
                .status("Resolved")
                .resolution("Fixed")
                .workLogNotes("Note added")
                .impact(2)
                .urgency(3)
                .assignedGroup("Team A")
                .assignedTo("User B")
                .build();

            // When
            String summary = request.getUpdateSummary();

            // Then
            assertThat(summary).contains("Status → Resolved");
            assertThat(summary).contains("Resolution added");
            assertThat(summary).contains("Work log added");
            assertThat(summary).contains("Impact → 2");
            assertThat(summary).contains("Urgency → 3");
            assertThat(summary).contains("Assigned to Team A");
            assertThat(summary).contains("Assigned to User B");
        }

        @Test
        void getUpdateSummary_shouldReturnMessageWhenNoUpdates() {
            // Given
            UpdateIncidentRequest request = UpdateIncidentRequest.builder().build();

            // When
            String summary = request.getUpdateSummary();

            // Then
            assertThat(summary).isEqualTo("No updates specified");
        }
    }

    @Nested
    class UpdateIncidentResponseTests {

        @Test
        void staged_shouldCreateStagedResponse() {
            // Given
            Instant expiresAt = Instant.now().plusSeconds(300);

            // When
            UpdateIncidentResponse response = UpdateIncidentResponse.staged(
                "INC000001",
                "action-123",
                "Update status to Resolved",
                expiresAt
            );

            // Then
            assertThat(response.getSuccess()).isTrue();
            assertThat(response.getStatus()).isEqualTo("STAGED");
            assertThat(response.getIncidentNumber()).isEqualTo("INC000001");
            assertThat(response.getActionId()).isEqualTo("action-123");
            assertThat(response.getPreview()).isEqualTo("Update status to Resolved");
            assertThat(response.getExpiresAt()).isEqualTo(expiresAt);
            assertThat(response.getMessage()).contains("staged");
        }

        @Test
        void updated_shouldCreateSuccessResponse() {
            // When
            UpdateIncidentResponse response = UpdateIncidentResponse.updated("INC000001");

            // Then
            assertThat(response.getSuccess()).isTrue();
            assertThat(response.getStatus()).isEqualTo("UPDATED");
            assertThat(response.getIncidentNumber()).isEqualTo("INC000001");
            assertThat(response.getMessage()).contains("updated successfully");
        }

        @Test
        void failed_shouldCreateFailureResponse() {
            // When
            UpdateIncidentResponse response = UpdateIncidentResponse.failed(
                "INC000001",
                "Update failed",
                "Database error"
            );

            // Then
            assertThat(response.getSuccess()).isFalse();
            assertThat(response.getStatus()).isEqualTo("FAILED");
            assertThat(response.getIncidentNumber()).isEqualTo("INC000001");
            assertThat(response.getMessage()).isEqualTo("Update failed");
            assertThat(response.getErrorDetail()).isEqualTo("Database error");
        }

        @Test
        void notFound_shouldCreateNotFoundResponse() {
            // When
            UpdateIncidentResponse response = UpdateIncidentResponse.notFound("INC000999");

            // Then
            assertThat(response.getSuccess()).isFalse();
            assertThat(response.getStatus()).isEqualTo("NOT_FOUND");
            assertThat(response.getIncidentNumber()).isEqualTo("INC000999");
            assertThat(response.getMessage()).contains("not found");
        }
    }

    @Nested
    class CreateIncidentRequestTests {

        @Test
        void builder_shouldCreateInstanceWithAllFields() {
            // When
            CreateIncidentRequest request = CreateIncidentRequest.builder()
                .summary("Test incident")
                .description("Test description")
                .impact(2)
                .urgency(3)
                .requesterFirstName("John")
                .requesterLastName("Doe")
                .requesterCompany("ACME Corp")
                .category("Software")
                .subCategory("Application")
                .item("Bug")
                .assignedGroup("Support")
                .serviceType("User Service")
                .configurationItem("LAPTOP-001")
                .location("Building A")
                .skipDuplicateCheck(false)
                .requireConfirmation(true)
                .sessionId("session-123")
                .build();

            // Then
            assertThat(request.getSummary()).isEqualTo("Test incident");
            assertThat(request.getImpact()).isEqualTo(2);
            assertThat(request.getUrgency()).isEqualTo(3);
            assertThat(request.getRequireConfirmation()).isTrue();
        }

        @Test
        void getImpactLabel_shouldReturnCorrectLabels() {
            // Given
            CreateIncidentRequest request = CreateIncidentRequest.builder()
                .summary("Test")
                .description("Test")
                .impact(1)
                .urgency(1)
                .build();

            // Then
            assertThat(request.getImpactLabel()).isEqualTo("Extensive/Widespread");

            request.setImpact(2);
            assertThat(request.getImpactLabel()).isEqualTo("Significant/Large");

            request.setImpact(3);
            assertThat(request.getImpactLabel()).isEqualTo("Moderate/Limited");

            request.setImpact(4);
            assertThat(request.getImpactLabel()).isEqualTo("Minor/Localized");

            request.setImpact(null);
            assertThat(request.getImpactLabel()).isEqualTo("Unknown");
        }

        @Test
        void getUrgencyLabel_shouldReturnCorrectLabels() {
            // Given
            CreateIncidentRequest request = CreateIncidentRequest.builder()
                .summary("Test")
                .description("Test")
                .impact(1)
                .urgency(1)
                .build();

            // Then
            assertThat(request.getUrgencyLabel()).isEqualTo("Critical");

            request.setUrgency(2);
            assertThat(request.getUrgencyLabel()).isEqualTo("High");

            request.setUrgency(3);
            assertThat(request.getUrgencyLabel()).isEqualTo("Medium");

            request.setUrgency(4);
            assertThat(request.getUrgencyLabel()).isEqualTo("Low");

            request.setUrgency(null);
            assertThat(request.getUrgencyLabel()).isEqualTo("Unknown");
        }
    }

    @Nested
    class CreateIncidentResponseTests {

        @Test
        void staged_shouldCreateStagedResponse() {
            // Given
            Instant expiresAt = Instant.now().plusSeconds(300);

            // When
            CreateIncidentResponse response = CreateIncidentResponse.staged(
                "action-123",
                "Create incident with summary...",
                expiresAt
            );

            // Then
            assertThat(response.getSuccess()).isTrue();
            assertThat(response.getStatus()).isEqualTo("STAGED");
            assertThat(response.getActionId()).isEqualTo("action-123");
            assertThat(response.getPreview()).isEqualTo("Create incident with summary...");
            assertThat(response.getExpiresAt()).isEqualTo(expiresAt);
        }

        @Test
        void stagedWithDuplicates_shouldCreateDuplicateWarningResponse() {
            // Given
            Instant expiresAt = Instant.now().plusSeconds(300);
            SearchResultItem similar = SearchResultItem.builder()
                .id("INC000001")
                .title("Similar incident")
                .build();

            // When
            CreateIncidentResponse response = CreateIncidentResponse.stagedWithDuplicates(
                "action-123",
                "Create incident",
                expiresAt,
                Collections.singletonList(similar)
            );

            // Then
            assertThat(response.getSuccess()).isTrue();
            assertThat(response.getStatus()).isEqualTo("DUPLICATE_WARNING");
            assertThat(response.getHasDuplicates()).isTrue();
            assertThat(response.getSimilarIncidents()).hasSize(1);
            assertThat(response.getMessage()).contains("duplicates");
        }

        @Test
        void created_shouldCreateSuccessResponse() {
            // When
            CreateIncidentResponse response = CreateIncidentResponse.created("INC000123");

            // Then
            assertThat(response.getSuccess()).isTrue();
            assertThat(response.getStatus()).isEqualTo("CREATED");
            assertThat(response.getIncidentNumber()).isEqualTo("INC000123");
            assertThat(response.getMessage()).contains("created successfully");
        }

        @Test
        void failed_shouldCreateFailureResponse() {
            // When
            CreateIncidentResponse response = CreateIncidentResponse.failed(
                "Creation failed",
                "Validation error"
            );

            // Then
            assertThat(response.getSuccess()).isFalse();
            assertThat(response.getStatus()).isEqualTo("FAILED");
            assertThat(response.getMessage()).isEqualTo("Creation failed");
            assertThat(response.getErrorDetail()).isEqualTo("Validation error");
        }

        @Test
        void validationError_shouldCreateValidationErrorResponse() {
            // When
            CreateIncidentResponse response = CreateIncidentResponse.validationError(
                "Summary is required"
            );

            // Then
            assertThat(response.getSuccess()).isFalse();
            assertThat(response.getStatus()).isEqualTo("VALIDATION_ERROR");
            assertThat(response.getMessage()).isEqualTo("Summary is required");
        }

        @Test
        void rateLimited_shouldCreateRateLimitResponse() {
            // When
            CreateIncidentResponse response = CreateIncidentResponse.rateLimited(10);

            // Then
            assertThat(response.getSuccess()).isFalse();
            assertThat(response.getStatus()).isEqualTo("RATE_LIMITED");
            assertThat(response.getMessage()).contains("10");
        }
    }

    @Nested
    class ConfirmActionResponseTests {

        @Test
        void executed_shouldCreateExecutedResponse() {
            // When
            ConfirmActionResponse response = ConfirmActionResponse.executed(
                "action-123",
                "INC000001",
                "Incident"
            );

            // Then
            assertThat(response.getSuccess()).isTrue();
            assertThat(response.getStatus()).isEqualTo("EXECUTED");
            assertThat(response.getActionId()).isEqualTo("action-123");
            assertThat(response.getRecordId()).isEqualTo("INC000001");
            assertThat(response.getRecordType()).isEqualTo("Incident");
            assertThat(response.getMessage()).contains("created successfully");
        }

        @Test
        void cancelled_shouldCreateCancelledResponse() {
            // When
            ConfirmActionResponse response = ConfirmActionResponse.cancelled("action-123");

            // Then
            assertThat(response.getSuccess()).isTrue();
            assertThat(response.getStatus()).isEqualTo("CANCELLED");
            assertThat(response.getActionId()).isEqualTo("action-123");
            assertThat(response.getMessage()).contains("cancelled");
        }

        @Test
        void expired_shouldCreateExpiredResponse() {
            // When
            ConfirmActionResponse response = ConfirmActionResponse.expired("action-123");

            // Then
            assertThat(response.getSuccess()).isFalse();
            assertThat(response.getStatus()).isEqualTo("EXPIRED");
            assertThat(response.getActionId()).isEqualTo("action-123");
            assertThat(response.getMessage()).contains("expired");
        }

        @Test
        void notFound_shouldCreateNotFoundResponse() {
            // When
            ConfirmActionResponse response = ConfirmActionResponse.notFound("action-999");

            // Then
            assertThat(response.getSuccess()).isFalse();
            assertThat(response.getStatus()).isEqualTo("NOT_FOUND");
            assertThat(response.getActionId()).isEqualTo("action-999");
            assertThat(response.getMessage()).contains("not found");
        }

        @Test
        void failed_shouldCreateFailedResponse() {
            // When
            ConfirmActionResponse response = ConfirmActionResponse.failed(
                "action-123",
                "Execution failed",
                "Database error"
            );

            // Then
            assertThat(response.getSuccess()).isFalse();
            assertThat(response.getStatus()).isEqualTo("FAILED");
            assertThat(response.getActionId()).isEqualTo("action-123");
            assertThat(response.getMessage()).isEqualTo("Execution failed");
            assertThat(response.getErrorDetail()).isEqualTo("Database error");
        }
    }

    @Nested
    class IncidentDetailResponseTests {

        @Test
        void builder_shouldCreateInstanceWithAllFields() {
            // Given
            Instant now = Instant.now();
            IncidentDetailResponse.WorkLogItem workLog = IncidentDetailResponse.WorkLogItem.builder()
                .id("WL001")
                .type("Working Log")
                .summary("Work log summary")
                .notes("Work log notes")
                .submitter("User")
                .submitDate(now)
                .build();

            IncidentDetailResponse.AttachmentItem attachment = IncidentDetailResponse.AttachmentItem.builder()
                .name("file.pdf")
                .sizeBytes(1024L)
                .contentType("application/pdf")
                .build();

            // When
            IncidentDetailResponse response = IncidentDetailResponse.builder()
                .incidentNumber("INC000001")
                .summary("Test incident")
                .description("Test description")
                .resolution("Resolved")
                .status("Resolved")
                .statusCode(5)
                .impact(2)
                .impactLabel("Moderate")
                .urgency(3)
                .urgencyLabel("Medium")
                .priority(3)
                .priorityLabel("Medium")
                .assignedGroup("Support")
                .assignedTo("John")
                .submitter("User")
                .customerFirstName("Jane")
                .customerLastName("Doe")
                .customerCompany("ACME")
                .categoryPath("Software > Application")
                .resolutionCategoryPath("Software > Bug Fix")
                .createDate(now)
                .lastModifiedDate(now)
                .lastModifiedBy("Admin")
                .workLogs(Collections.singletonList(workLog))
                .attachments(Collections.singletonList(attachment))
                .found(true)
                .build();

            // Then
            assertThat(response.getIncidentNumber()).isEqualTo("INC000001");
            assertThat(response.getImpact()).isEqualTo(2);
            assertThat(response.getWorkLogs()).hasSize(1);
            assertThat(response.getAttachments()).hasSize(1);
            assertThat(response.getFound()).isTrue();
        }

        @Test
        void notFound_shouldCreateNotFoundResponse() {
            // When
            IncidentDetailResponse response = IncidentDetailResponse.notFound("INC000999");

            // Then
            assertThat(response.getIncidentNumber()).isEqualTo("INC000999");
            assertThat(response.getFound()).isFalse();
            assertThat(response.getErrorMessage()).contains("not found");
        }

        @Test
        void getImpactLabel_shouldReturnCorrectLabels() {
            // Given
            IncidentDetailResponse response = IncidentDetailResponse.builder()
                .impact(1)
                .build();

            // Then
            assertThat(response.getImpactLabel()).isEqualTo("Extensive/Widespread");

            response.setImpact(2);
            assertThat(response.getImpactLabel()).isEqualTo("Significant/Large");

            response.setImpact(3);
            assertThat(response.getImpactLabel()).isEqualTo("Moderate/Limited");

            response.setImpact(4);
            assertThat(response.getImpactLabel()).isEqualTo("Minor/Localized");

            response.setImpact(null);
            assertThat(response.getImpactLabel()).isEqualTo("Unknown");
        }

        @Test
        void getUrgencyLabel_shouldReturnCorrectLabels() {
            // Given
            IncidentDetailResponse response = IncidentDetailResponse.builder()
                .urgency(1)
                .build();

            // Then
            assertThat(response.getUrgencyLabel()).isEqualTo("Critical");

            response.setUrgency(2);
            assertThat(response.getUrgencyLabel()).isEqualTo("High");

            response.setUrgency(3);
            assertThat(response.getUrgencyLabel()).isEqualTo("Medium");

            response.setUrgency(4);
            assertThat(response.getUrgencyLabel()).isEqualTo("Low");
        }

        @Test
        void getPriorityLabel_shouldReturnCorrectLabels() {
            // Given
            IncidentDetailResponse response = IncidentDetailResponse.builder()
                .priority(1)
                .build();

            // Then
            assertThat(response.getPriorityLabel()).isEqualTo("Critical");

            response.setPriority(2);
            assertThat(response.getPriorityLabel()).isEqualTo("High");

            response.setPriority(3);
            assertThat(response.getPriorityLabel()).isEqualTo("Medium");

            response.setPriority(4);
            assertThat(response.getPriorityLabel()).isEqualTo("Low");
        }

        @Test
        void getLabelMethods_shouldUseExistingLabelWhenPresent() {
            // Given
            IncidentDetailResponse response = IncidentDetailResponse.builder()
                .impact(1)
                .impactLabel("Custom Impact")
                .urgency(1)
                .urgencyLabel("Custom Urgency")
                .priority(1)
                .priorityLabel("Custom Priority")
                .build();

            // Then
            assertThat(response.getImpactLabel()).isEqualTo("Custom Impact");
            assertThat(response.getUrgencyLabel()).isEqualTo("Custom Urgency");
            assertThat(response.getPriorityLabel()).isEqualTo("Custom Priority");
        }
    }

    @Nested
    class KnowledgeDetailResponseTests {

        @Test
        void builder_shouldCreateInstanceWithAllFields() {
            // Given
            Instant now = Instant.now();
            KnowledgeDetailResponse.AttachmentItem attachment = KnowledgeDetailResponse.AttachmentItem.builder()
                .name("doc.pdf")
                .sizeBytes(2048L)
                .contentType("application/pdf")
                .build();

            // When
            KnowledgeDetailResponse response = KnowledgeDetailResponse.builder()
                .articleId("KB000001")
                .title("How to reset password")
                .summary("Password reset guide")
                .content("Full article content")
                .keywords(Arrays.asList("password", "reset", "account"))
                .articleType("How-To")
                .status("Published")
                .categoryPath("User Management > Password")
                .author("Admin")
                .versionNumber(2)
                .viewCount(100)
                .createDate(now)
                .publishedDate(now)
                .expirationDate(now.plusSeconds(86400))
                .lastModifiedDate(now)
                .lastModifiedBy("Editor")
                .assignedGroup("Support")
                .relatedArticles(Arrays.asList("KB000002", "KB000003"))
                .attachments(Collections.singletonList(attachment))
                .found(true)
                .build();

            // Then
            assertThat(response.getArticleId()).isEqualTo("KB000001");
            assertThat(response.getKeywords()).hasSize(3);
            assertThat(response.getRelatedArticles()).hasSize(2);
            assertThat(response.getAttachments()).hasSize(1);
            assertThat(response.getFound()).isTrue();
        }

        @Test
        void notFound_shouldCreateNotFoundResponse() {
            // When
            KnowledgeDetailResponse response = KnowledgeDetailResponse.notFound("KB000999");

            // Then
            assertThat(response.getArticleId()).isEqualTo("KB000999");
            assertThat(response.getFound()).isFalse();
            assertThat(response.getErrorMessage()).contains("not found");
        }

        @Test
        void builder_shouldUseDefaultEmptyLists() {
            // When
            KnowledgeDetailResponse response = KnowledgeDetailResponse.builder()
                .articleId("KB001")
                .build();

            // Then
            assertThat(response.getRelatedArticles()).isEmpty();
            assertThat(response.getAttachments()).isEmpty();
            assertThat(response.getFound()).isTrue();
        }
    }

    @Nested
    class WorkLogItemTests {

        @Test
        void builder_shouldCreateInstanceWithAllFields() {
            // When
            WorkLogItem item = WorkLogItem.builder()
                .workLogId("WL001")
                .type("Working Log")
                .description("Detailed work log description")
                .submitter("User")
                .submitDate("2024-01-01T10:00:00Z")
                .viewAccess("Internal")
                .build();

            // Then
            assertThat(item.getWorkLogId()).isEqualTo("WL001");
            assertThat(item.getType()).isEqualTo("Working Log");
            assertThat(item.getDescription()).isEqualTo("Detailed work log description");
            assertThat(item.getSubmitter()).isEqualTo("User");
            assertThat(item.getViewAccess()).isEqualTo("Internal");
        }

        @Test
        void getPreview_shouldReturnFullDescriptionWhenShort() {
            // Given
            WorkLogItem item = WorkLogItem.builder()
                .description("Short description")
                .build();

            // When
            String preview = item.getPreview();

            // Then
            assertThat(preview).isEqualTo("Short description");
        }

        @Test
        void getPreview_shouldTruncateLongDescription() {
            // Given
            String longDescription = "a".repeat(250);
            WorkLogItem item = WorkLogItem.builder()
                .description(longDescription)
                .build();

            // When
            String preview = item.getPreview();

            // Then
            assertThat(preview).hasSize(203); // 200 + "..."
            assertThat(preview).endsWith("...");
        }

        @Test
        void getPreview_shouldHandleNullDescription() {
            // Given
            WorkLogItem item = WorkLogItem.builder()
                .description(null)
                .build();

            // When
            String preview = item.getPreview();

            // Then
            assertThat(preview).isEmpty();
        }
    }
}
