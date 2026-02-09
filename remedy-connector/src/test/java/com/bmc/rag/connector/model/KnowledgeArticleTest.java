package com.bmc.rag.connector.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for KnowledgeArticle model class.
 */
@DisplayName("KnowledgeArticle")
class KnowledgeArticleTest {

    @Nested
    @DisplayName("ITSMRecord Interface Methods")
    class ITSMRecordInterfaceTests {

        @Test
        @DisplayName("getRecordType returns 'KnowledgeArticle'")
        void getRecordType_returnsKnowledgeArticle() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .articleId("KA000001")
                    .build();

            assertThat(article.getRecordType()).isEqualTo("KnowledgeArticle");
        }

        @Test
        @DisplayName("getRecordId returns article ID")
        void getRecordId_returnsArticleId() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .articleId("KA000001")
                    .build();

            assertThat(article.getRecordId()).isEqualTo("KA000001");
        }

        @Test
        @DisplayName("getTitle returns title field")
        void getTitle_returnsTitle() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .title("How to reset password")
                    .build();

            assertThat(article.getTitle()).isEqualTo("How to reset password");
        }
    }

    @Nested
    @DisplayName("getCategoryPath")
    class GetCategoryPathTests {

        @Test
        @DisplayName("returns tier1 only when tier2 and tier3 are null")
        void getCategoryPath_tier1Only() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .categoryTier1("Security")
                    .build();

            assertThat(article.getCategoryPath()).isEqualTo("Security");
        }

        @Test
        @DisplayName("returns tier1 > tier2 when tier3 is null")
        void getCategoryPath_tier1AndTier2() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .categoryTier1("Security")
                    .categoryTier2("Authentication")
                    .build();

            assertThat(article.getCategoryPath()).isEqualTo("Security > Authentication");
        }

        @Test
        @DisplayName("returns full path tier1 > tier2 > tier3")
        void getCategoryPath_allThreeTiers() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .categoryTier1("Security")
                    .categoryTier2("Authentication")
                    .categoryTier3("Password Reset")
                    .build();

            assertThat(article.getCategoryPath()).isEqualTo("Security > Authentication > Password Reset");
        }

        @Test
        @DisplayName("returns empty string when tier1 is null")
        void getCategoryPath_allNull() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .build();

            assertThat(article.getCategoryPath()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getKeywordList")
    class GetKeywordListTests {

        @Test
        @DisplayName("splits keywords by comma")
        void getKeywordList_splitsByComma() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .keywords("password,reset,authentication")
                    .build();

            assertThat(article.getKeywordList())
                    .containsExactly("password", "reset", "authentication");
        }

        @Test
        @DisplayName("splits keywords by semicolon")
        void getKeywordList_splitsBySemicolon() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .keywords("password;reset;authentication")
                    .build();

            assertThat(article.getKeywordList())
                    .containsExactly("password", "reset", "authentication");
        }

        @Test
        @DisplayName("splits keywords by whitespace")
        void getKeywordList_splitsByWhitespace() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .keywords("password reset authentication")
                    .build();

            assertThat(article.getKeywordList())
                    .containsExactly("password", "reset", "authentication");
        }

        @Test
        @DisplayName("splits keywords by mixed delimiters")
        void getKeywordList_splitsByMixedDelimiters() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .keywords("password, reset; authentication login")
                    .build();

            assertThat(article.getKeywordList())
                    .containsExactly("password", "reset", "authentication", "login");
        }

        @Test
        @DisplayName("trims whitespace from keywords")
        void getKeywordList_trimsWhitespace() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .keywords("  password  ,  reset  ,  authentication  ")
                    .build();

            assertThat(article.getKeywordList())
                    .containsExactly("password", "reset", "authentication");
        }

        @Test
        @DisplayName("filters out empty strings")
        void getKeywordList_filtersEmptyStrings() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .keywords("password,,reset,,,authentication")
                    .build();

            assertThat(article.getKeywordList())
                    .containsExactly("password", "reset", "authentication");
        }

        @Test
        @DisplayName("returns empty list when keywords is null")
        void getKeywordList_nullKeywords() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .build();

            assertThat(article.getKeywordList()).isEmpty();
        }

        @Test
        @DisplayName("returns empty list when keywords is empty string")
        void getKeywordList_emptyString() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .keywords("")
                    .build();

            assertThat(article.getKeywordList()).isEmpty();
        }
    }

    @Nested
    @DisplayName("isPublished")
    class IsPublishedTests {

        @Test
        @DisplayName("returns true when status is 2")
        void isPublished_status2() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .status(2)
                    .build();

            assertThat(article.isPublished()).isTrue();
        }

        @Test
        @DisplayName("returns false when status is 0")
        void isPublished_status0() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .status(0)
                    .build();

            assertThat(article.isPublished()).isFalse();
        }

        @Test
        @DisplayName("returns false when status is 1")
        void isPublished_status1() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .status(1)
                    .build();

            assertThat(article.isPublished()).isFalse();
        }

        @Test
        @DisplayName("returns false when status is null")
        void isPublished_statusNull() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .build();

            assertThat(article.isPublished()).isFalse();
        }
    }

    @Nested
    @DisplayName("isExpired")
    class IsExpiredTests {

        @Test
        @DisplayName("returns true when expiration date is in the past")
        void isExpired_pastDate() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .expirationDate(Instant.now().minus(1, ChronoUnit.DAYS))
                    .build();

            assertThat(article.isExpired()).isTrue();
        }

        @Test
        @DisplayName("returns false when expiration date is in the future")
        void isExpired_futureDate() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .expirationDate(Instant.now().plus(1, ChronoUnit.DAYS))
                    .build();

            assertThat(article.isExpired()).isFalse();
        }

        @Test
        @DisplayName("returns false when expiration date is null")
        void isExpired_nullDate() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .build();

            assertThat(article.isExpired()).isFalse();
        }
    }

    @Nested
    @DisplayName("isActive")
    class IsActiveTests {

        @Test
        @DisplayName("returns true when published and not expired")
        void isActive_publishedAndNotExpired() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .status(2)
                    .expirationDate(Instant.now().plus(30, ChronoUnit.DAYS))
                    .build();

            assertThat(article.isActive()).isTrue();
        }

        @Test
        @DisplayName("returns true when published and expiration date is null")
        void isActive_publishedAndNoExpirationDate() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .status(2)
                    .build();

            assertThat(article.isActive()).isTrue();
        }

        @Test
        @DisplayName("returns false when published but expired")
        void isActive_publishedButExpired() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .status(2)
                    .expirationDate(Instant.now().minus(1, ChronoUnit.DAYS))
                    .build();

            assertThat(article.isActive()).isFalse();
        }

        @Test
        @DisplayName("returns false when not published and not expired")
        void isActive_notPublishedButNotExpired() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .status(1)
                    .expirationDate(Instant.now().plus(30, ChronoUnit.DAYS))
                    .build();

            assertThat(article.isActive()).isFalse();
        }

        @Test
        @DisplayName("returns false when not published and expired")
        void isActive_notPublishedAndExpired() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .status(1)
                    .expirationDate(Instant.now().minus(1, ChronoUnit.DAYS))
                    .build();

            assertThat(article.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("getCombinedContent")
    class GetCombinedContentTests {

        @Test
        @DisplayName("combines all fields with labels")
        void getCombinedContent_allFields() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .title("How to reset password")
                    .articleSummary("Step-by-step guide for password reset")
                    .content("1. Click Forgot Password\n2. Enter email\n3. Check inbox")
                    .keywords("password, reset, authentication")
                    .build();

            String combined = article.getCombinedContent();

            assertThat(combined)
                    .contains("Title: How to reset password")
                    .contains("Summary: Step-by-step guide for password reset")
                    .contains("1. Click Forgot Password")
                    .contains("Keywords: password, reset, authentication");
        }

        @Test
        @DisplayName("strips HTML tags from content")
        void getCombinedContent_stripsHtmlTags() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .content("<p>This is <strong>bold</strong> text</p><div>More content</div>")
                    .build();

            String combined = article.getCombinedContent();

            assertThat(combined)
                    .doesNotContain("<p>", "</p>", "<strong>", "</strong>", "<div>", "</div>")
                    .contains("This is")
                    .contains("bold")
                    .contains("text")
                    .contains("More content");
        }

        @Test
        @DisplayName("normalizes whitespace")
        void getCombinedContent_normalizesWhitespace() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .content("Text   with    multiple     spaces\n\n\nand\n\nnewlines")
                    .build();

            String combined = article.getCombinedContent();

            assertThat(combined)
                    .doesNotContain("   ")
                    .contains("Text with multiple spaces and newlines");
        }

        @Test
        @DisplayName("handles null title gracefully")
        void getCombinedContent_nullTitle() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .articleSummary("Summary")
                    .content("Content")
                    .build();

            String combined = article.getCombinedContent();

            assertThat(combined)
                    .doesNotContain("Title:")
                    .contains("Summary: Summary")
                    .contains("Content");
        }

        @Test
        @DisplayName("handles null summary gracefully")
        void getCombinedContent_nullSummary() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .title("Title")
                    .content("Content")
                    .build();

            String combined = article.getCombinedContent();

            assertThat(combined)
                    .contains("Title: Title")
                    .doesNotContain("Summary:")
                    .contains("Content");
        }

        @Test
        @DisplayName("handles null content gracefully")
        void getCombinedContent_nullContent() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .title("Title")
                    .articleSummary("Summary")
                    .keywords("keyword1, keyword2")
                    .build();

            String combined = article.getCombinedContent();

            assertThat(combined)
                    .contains("Title: Title")
                    .contains("Summary: Summary")
                    .contains("Keywords: keyword1, keyword2");
        }

        @Test
        @DisplayName("handles null keywords gracefully")
        void getCombinedContent_nullKeywords() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .title("Title")
                    .content("Content")
                    .build();

            String combined = article.getCombinedContent();

            assertThat(combined)
                    .contains("Title: Title")
                    .contains("Content")
                    .doesNotContain("Keywords:");
        }

        @Test
        @DisplayName("returns empty string when all fields are null")
        void getCombinedContent_allNull() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .build();

            String combined = article.getCombinedContent();

            assertThat(combined).isEmpty();
        }
    }

    @Nested
    @DisplayName("Builder Defaults")
    class BuilderDefaultsTests {

        @Test
        @DisplayName("attachments defaults to empty ArrayList")
        void builder_attachmentsDefaultsToEmptyList() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .articleId("KA000001")
                    .build();

            assertThat(article.getAttachments())
                    .isNotNull()
                    .isEmpty();
            assertThat(article.getAttachments()).isInstanceOf(ArrayList.class);
        }

        @Test
        @DisplayName("can add attachments to default list")
        void builder_canAddToDefaultAttachmentsList() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .filename("diagram.pdf")
                    .build();

            KnowledgeArticle article = KnowledgeArticle.builder()
                    .articleId("KA000001")
                    .build();

            article.getAttachments().add(attachment);

            assertThat(article.getAttachments()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Complete Object Creation")
    class CompleteObjectTests {

        @Test
        @DisplayName("can create fully populated article")
        void builder_createFullyPopulatedArticle() {
            Instant now = Instant.now();

            KnowledgeArticle article = KnowledgeArticle.builder()
                    .entryId("000000000000003")
                    .articleId("KA000001")
                    .title("How to reset password")
                    .content("<p>Step-by-step instructions</p>")
                    .articleSummary("Password reset guide")
                    .keywords("password, reset, authentication")
                    .articleType("How-To")
                    .status(2)
                    .statusDisplayValue("Published")
                    .assignedGroup("Knowledge Management")
                    .author("knowledge.admin@example.com")
                    .versionNumber(3)
                    .createDate(now.minus(90, ChronoUnit.DAYS))
                    .lastModifiedDate(now.minus(5, ChronoUnit.DAYS))
                    .lastModifiedBy("editor@example.com")
                    .publishedDate(now.minus(30, ChronoUnit.DAYS))
                    .expirationDate(now.plus(180, ChronoUnit.DAYS))
                    .viewCount(1250)
                    .categoryTier1("Security")
                    .categoryTier2("Authentication")
                    .categoryTier3("Password Reset")
                    .build();

            assertThat(article.getRecordType()).isEqualTo("KnowledgeArticle");
            assertThat(article.getRecordId()).isEqualTo("KA000001");
            assertThat(article.getTitle()).isEqualTo("How to reset password");
            assertThat(article.getCategoryPath()).isEqualTo("Security > Authentication > Password Reset");
            assertThat(article.getKeywordList()).containsExactly("password", "reset", "authentication");
            assertThat(article.isPublished()).isTrue();
            assertThat(article.isExpired()).isFalse();
            assertThat(article.isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("Lombok Generated Methods")
    class LombokGeneratedMethodsTests {

        @Test
        @DisplayName("equals returns true for same object")
        void equals_sameObject_returnsTrue() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .articleId("KA000001")
                    .title("Test")
                    .build();

            assertThat(article).isEqualTo(article);
        }

        @Test
        @DisplayName("equals returns true for same values")
        void equals_sameValues_returnsTrue() {
            KnowledgeArticle art1 = KnowledgeArticle.builder()
                    .articleId("KA000001")
                    .title("Test")
                    .status(2)
                    .build();

            KnowledgeArticle art2 = KnowledgeArticle.builder()
                    .articleId("KA000001")
                    .title("Test")
                    .status(2)
                    .build();

            assertThat(art1).isEqualTo(art2);
            assertThat(art1.hashCode()).isEqualTo(art2.hashCode());
        }

        @Test
        @DisplayName("equals returns false for different values")
        void equals_differentArticleId_returnsFalse() {
            KnowledgeArticle art1 = KnowledgeArticle.builder()
                    .articleId("KA000001")
                    .build();

            KnowledgeArticle art2 = KnowledgeArticle.builder()
                    .articleId("KA000002")
                    .build();

            assertThat(art1).isNotEqualTo(art2);
        }

        @Test
        @DisplayName("toString includes key fields")
        void toString_includesKeyFields() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .articleId("KA000001")
                    .title("Password Reset")
                    .status(2)
                    .viewCount(100)
                    .build();

            String result = article.toString();

            assertThat(result).contains("KA000001");
            assertThat(result).contains("Password Reset");
            assertThat(result).contains("status=2");
        }

        @Test
        @DisplayName("no-args constructor creates object")
        void noArgsConstructor_createsObject() {
            KnowledgeArticle article = new KnowledgeArticle();
            assertThat(article).isNotNull();
        }

        @Test
        @DisplayName("getContent interface method returns content field")
        void getContent_returnsContentField() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .content("Article content here")
                    .build();

            assertThat(article.getContent()).isEqualTo("Article content here");
        }

        @Test
        @DisplayName("all-args constructor sets all fields")
        void allArgsConstructor_setsAllFields() {
            Instant now = Instant.now();
            KnowledgeArticle art = new KnowledgeArticle(
                "entry1", "KA001", "title", "content", "summary", "keywords",
                "How-To", 2, "Published", "group", "author", 1, now, now, "modifier",
                now, now, 100, "tier1", "tier2", "tier3", new ArrayList<>()
            );

            assertThat(art.getEntryId()).isEqualTo("entry1");
            assertThat(art.getArticleId()).isEqualTo("KA001");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("getCategoryPath with tier2 null and tier3 set returns only tier1")
        void getCategoryPath_tier2NullWithTier3_returnsTier1Only() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .categoryTier1("Security")
                    .categoryTier3("Password Reset")
                    .build();

            assertThat(article.getCategoryPath()).isEqualTo("Security");
        }

        @Test
        @DisplayName("isPublished with status 3 returns false")
        void isPublished_status3_returnsFalse() {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .status(3)
                    .build();

            assertThat(article.isPublished()).isFalse();
        }
    }
}
