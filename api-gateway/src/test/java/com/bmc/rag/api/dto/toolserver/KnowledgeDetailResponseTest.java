package com.bmc.rag.api.dto.toolserver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for KnowledgeDetailResponse and nested classes.
 */
@DisplayName("KnowledgeDetailResponse Tests")
class KnowledgeDetailResponseTest {

    @Test
    @DisplayName("builder_shouldCreateInstanceWithAllFields")
    void builder_shouldCreateInstanceWithAllFields() {
        KnowledgeDetailResponse.AttachmentItem attachment = KnowledgeDetailResponse.AttachmentItem.builder()
            .name("guide.pdf")
            .sizeBytes(5120L)
            .contentType("application/pdf")
            .build();

        KnowledgeDetailResponse response = KnowledgeDetailResponse.builder()
            .articleId("KB000000001")
            .title("How to reset password")
            .summary("Password reset guide")
            .content("Full content here")
            .keywords(List.of("password", "reset", "security"))
            .articleType("Solution")
            .status("Published")
            .categoryPath("Security > Authentication")
            .author("John Doe")
            .versionNumber(2)
            .viewCount(150)
            .createDate(Instant.now())
            .publishedDate(Instant.now())
            .expirationDate(Instant.now().plusSeconds(86400 * 365))
            .lastModifiedDate(Instant.now())
            .lastModifiedBy("Jane Smith")
            .assignedGroup("KB Team")
            .relatedArticles(List.of("KB000000002", "KB000000003"))
            .attachments(List.of(attachment))
            .found(true)
            .build();

        assertThat(response.getArticleId()).isEqualTo("KB000000001");
        assertThat(response.getTitle()).isEqualTo("How to reset password");
        assertThat(response.getKeywords()).hasSize(3);
        assertThat(response.getRelatedArticles()).hasSize(2);
        assertThat(response.getAttachments()).hasSize(1);
    }

    @Test
    @DisplayName("notFound_shouldCreateNotFoundResponse")
    void notFound_shouldCreateNotFoundResponse() {
        KnowledgeDetailResponse response = KnowledgeDetailResponse.notFound("KB000000001");

        assertThat(response.getArticleId()).isEqualTo("KB000000001");
        assertThat(response.getFound()).isFalse();
        assertThat(response.getErrorMessage()).isEqualTo("Knowledge article KB000000001 not found");
    }

    @Test
    @DisplayName("attachmentItem_builder_shouldWork")
    void attachmentItem_builder_shouldWork() {
        KnowledgeDetailResponse.AttachmentItem attachment = KnowledgeDetailResponse.AttachmentItem.builder()
            .name("document.docx")
            .sizeBytes(10240L)
            .contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            .build();

        assertThat(attachment.getName()).isEqualTo("document.docx");
        assertThat(attachment.getSizeBytes()).isEqualTo(10240L);
        assertThat(attachment.getContentType()).contains("wordprocessingml");
    }

    @Test
    @DisplayName("attachmentItem_noArgsConstructor_shouldWork")
    void attachmentItem_noArgsConstructor_shouldWork() {
        KnowledgeDetailResponse.AttachmentItem attachment = new KnowledgeDetailResponse.AttachmentItem();
        assertThat(attachment).isNotNull();
    }

    @Test
    @DisplayName("attachmentItem_allArgsConstructor_shouldWork")
    void attachmentItem_allArgsConstructor_shouldWork() {
        KnowledgeDetailResponse.AttachmentItem attachment = new KnowledgeDetailResponse.AttachmentItem(
            "image.png", 4096L, "image/png"
        );

        assertThat(attachment.getName()).isEqualTo("image.png");
        assertThat(attachment.getSizeBytes()).isEqualTo(4096L);
        assertThat(attachment.getContentType()).isEqualTo("image/png");
    }

    @Test
    @DisplayName("defaultBuilderValues_shouldCreateEmptyLists")
    void defaultBuilderValues_shouldCreateEmptyLists() {
        KnowledgeDetailResponse response = KnowledgeDetailResponse.builder()
            .articleId("KB001")
            .build();

        assertThat(response.getRelatedArticles()).isNotNull().isEmpty();
        assertThat(response.getAttachments()).isNotNull().isEmpty();
        assertThat(response.getFound()).isTrue();
    }
}
