package com.bmc.rag.vectorization.chunking;

import com.bmc.rag.connector.model.KnowledgeArticle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Tests for KnowledgeChunkStrategy.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KnowledgeChunkStrategyTest {

    @Mock
    private SemanticChunker semanticChunker;

    private KnowledgeChunkStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new KnowledgeChunkStrategy(semanticChunker);

        // Default mock behavior
        when(semanticChunker.splitTextWithContext(anyString(), anyString()))
            .thenAnswer(invocation -> {
                String text = invocation.getArgument(0);
                String context = invocation.getArgument(1);
                return List.of(context + "\n\n" + text);
            });
    }

    @Test
    void getRecordType_returnsKnowledgeArticle() {
        // When
        String recordType = strategy.getRecordType();

        // Then
        assertThat(recordType).isEqualTo("KnowledgeArticle");
    }

    @Test
    void chunk_minimalArticle_createsSummaryChunk() {
        // Given
        KnowledgeArticle article = KnowledgeArticle.builder()
            .entryId("000000000000001")
            .articleId("KA000000000001")
            .title("How to reset password")
            .statusDisplayValue("Published")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(article);

        // Then
        assertThat(chunks).hasSize(1);
        TextChunk summaryChunk = chunks.get(0);
        assertThat(summaryChunk.getChunkType()).isEqualTo(TextChunk.ChunkType.SUMMARY);
        assertThat(summaryChunk.getSourceType()).isEqualTo("KnowledgeArticle");
        assertThat(summaryChunk.getSourceId()).isEqualTo("KA000000000001");
    }

    @Test
    void chunk_withContent_createsHighPriorityContentChunks() {
        // Given
        KnowledgeArticle article = KnowledgeArticle.builder()
            .entryId("000000000000001")
            .articleId("KA000000000001")
            .title("Password Reset Guide")
            .content("Step 1: Click Forgot Password. Step 2: Enter your email. Step 3: Check email for reset link.")
            .statusDisplayValue("Published")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(article);

        // Then
        List<TextChunk> contentChunks = chunks.stream()
            .filter(c -> c.getChunkType() == TextChunk.ChunkType.ARTICLE_CONTENT)
            .toList();

        assertThat(contentChunks).hasSizeGreaterThanOrEqualTo(1);
        TextChunk contentChunk = contentChunks.get(0);
        assertThat(contentChunk.getMetadata()).containsEntry("chunk_priority", "high");
    }

    @Test
    void chunk_htmlContent_cleansHtmlTags() {
        // Given
        String htmlContent = "<p>This is a <strong>bold</strong> paragraph.</p><br/><div>Another section.</div>";
        KnowledgeArticle article = KnowledgeArticle.builder()
            .entryId("000000000000001")
            .articleId("KA000000000001")
            .title("Test Article")
            .content(htmlContent)
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(article);

        // Then
        List<TextChunk> contentChunks = chunks.stream()
            .filter(c -> c.getChunkType() == TextChunk.ChunkType.ARTICLE_CONTENT)
            .toList();

        assertThat(contentChunks).isNotEmpty();
        String cleanedContent = contentChunks.get(0).getContent();
        assertThat(cleanedContent).doesNotContain("<p>");
        assertThat(cleanedContent).doesNotContain("</p>");
        assertThat(cleanedContent).doesNotContain("<strong>");
        assertThat(cleanedContent).doesNotContain("<br/>");
        assertThat(cleanedContent).doesNotContain("<div>");
    }

    @Test
    void chunk_htmlBrTag_convertsToNewline() {
        // Given
        String htmlContent = "Line one<br/>Line two<BR>Line three";
        KnowledgeArticle article = KnowledgeArticle.builder()
            .entryId("000000000000001")
            .articleId("KA000000000001")
            .title("Test Article")
            .content(htmlContent)
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(article);

        // Then
        List<TextChunk> contentChunks = chunks.stream()
            .filter(c -> c.getChunkType() == TextChunk.ChunkType.ARTICLE_CONTENT)
            .toList();

        assertThat(contentChunks).isNotEmpty();
        String cleanedContent = contentChunks.get(0).getContent();
        assertThat(cleanedContent).contains("Line one");
        assertThat(cleanedContent).contains("Line two");
        assertThat(cleanedContent).contains("Line three");
    }

    @Test
    void chunk_htmlParagraphs_convertsToDoubleNewlines() {
        // Given
        String htmlContent = "<p>First paragraph.</p><p>Second paragraph.</p>";
        KnowledgeArticle article = KnowledgeArticle.builder()
            .entryId("000000000000001")
            .articleId("KA000000000001")
            .title("Test Article")
            .content(htmlContent)
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(article);

        // Then
        List<TextChunk> contentChunks = chunks.stream()
            .filter(c -> c.getChunkType() == TextChunk.ChunkType.ARTICLE_CONTENT)
            .toList();

        assertThat(contentChunks).isNotEmpty();
        String cleanedContent = contentChunks.get(0).getContent();
        assertThat(cleanedContent).contains("First paragraph");
        assertThat(cleanedContent).contains("Second paragraph");
    }

    @Test
    void chunk_htmlEntities_decodesCorrectly() {
        // Given
        String htmlContent = "Text with &nbsp; spaces &amp; ampersand &lt;tag&gt; &quot;quotes&quot; &apos;apostrophe&apos;";
        KnowledgeArticle article = KnowledgeArticle.builder()
            .entryId("000000000000001")
            .articleId("KA000000000001")
            .title("Test Article")
            .content(htmlContent)
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(article);

        // Then
        List<TextChunk> contentChunks = chunks.stream()
            .filter(c -> c.getChunkType() == TextChunk.ChunkType.ARTICLE_CONTENT)
            .toList();

        assertThat(contentChunks).isNotEmpty();
        String cleanedContent = contentChunks.get(0).getContent();
        assertThat(cleanedContent).contains("&");
        assertThat(cleanedContent).contains("<");
        assertThat(cleanedContent).contains(">");
        assertThat(cleanedContent).contains("\"");
        assertThat(cleanedContent).contains("'");
        assertThat(cleanedContent).doesNotContain("&nbsp;");
        assertThat(cleanedContent).doesNotContain("&amp;");
        assertThat(cleanedContent).doesNotContain("&lt;");
        assertThat(cleanedContent).doesNotContain("&gt;");
    }

    @Test
    void chunk_populatesArticleMetadata() {
        // Given
        KnowledgeArticle article = KnowledgeArticle.builder()
            .entryId("000000000000001")
            .articleId("KA000000000001")
            .title("Password Reset Guide")
            .author("John Doe")
            .keywords("password, reset, security")
            .articleType("How-To")
            .viewCount(250)
            .assignedGroup("Knowledge Management")
            .categoryTier1("Security")
            .statusDisplayValue("Published")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(article);

        // Then
        assertThat(chunks).isNotEmpty();
        TextChunk firstChunk = chunks.get(0);
        assertThat(firstChunk.getMetadata()).containsEntry("source_type", "KnowledgeArticle");
        assertThat(firstChunk.getMetadata()).containsEntry("author", "John Doe");
        assertThat(firstChunk.getMetadata()).containsEntry("keywords", "password, reset, security");
        assertThat(firstChunk.getMetadata()).containsEntry("article_type", "How-To");
        assertThat(firstChunk.getMetadata()).containsEntry("view_count", "250");
        assertThat(firstChunk.getMetadata()).containsEntry("assigned_group", "Knowledge Management");
    }

    @Test
    void chunk_withArticleSummary_includesInSummaryChunk() {
        // Given
        KnowledgeArticle article = KnowledgeArticle.builder()
            .entryId("000000000000001")
            .articleId("KA000000000001")
            .title("Password Reset Guide")
            .articleSummary("This article explains how to reset your password in 3 simple steps.")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(article);

        // Then
        TextChunk summaryChunk = chunks.stream()
            .filter(c -> c.getChunkType() == TextChunk.ChunkType.SUMMARY)
            .findFirst()
            .orElseThrow();

        assertThat(summaryChunk.getContent()).contains("Summary:");
        assertThat(summaryChunk.getContent()).contains("This article explains how to reset your password");
    }

    @Test
    void chunk_withKeywords_includesInSummaryChunk() {
        // Given
        KnowledgeArticle article = KnowledgeArticle.builder()
            .entryId("000000000000001")
            .articleId("KA000000000001")
            .title("Password Reset Guide")
            .keywords("password, reset, authentication, security")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(article);

        // Then
        TextChunk summaryChunk = chunks.stream()
            .filter(c -> c.getChunkType() == TextChunk.ChunkType.SUMMARY)
            .findFirst()
            .orElseThrow();

        assertThat(summaryChunk.getContent()).contains("Keywords:");
        assertThat(summaryChunk.getContent()).contains("password, reset, authentication, security");
    }

    @Test
    void chunk_nullContent_doesNotCreateContentChunks() {
        // Given
        KnowledgeArticle article = KnowledgeArticle.builder()
            .entryId("000000000000001")
            .articleId("KA000000000001")
            .title("Empty Article")
            .content(null)
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(article);

        // Then
        assertThat(chunks.stream().filter(c -> c.getChunkType() == TextChunk.ChunkType.ARTICLE_CONTENT))
            .isEmpty();
    }

    @Test
    void chunk_emptyContent_doesNotCreateContentChunks() {
        // Given
        KnowledgeArticle article = KnowledgeArticle.builder()
            .entryId("000000000000001")
            .articleId("KA000000000001")
            .title("Empty Article")
            .content("")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(article);

        // Then
        assertThat(chunks.stream().filter(c -> c.getChunkType() == TextChunk.ChunkType.ARTICLE_CONTENT))
            .isEmpty();
    }

    @Test
    void chunk_onlyHtmlTags_doesNotCreateContentChunks() {
        // Given
        KnowledgeArticle article = KnowledgeArticle.builder()
            .entryId("000000000000001")
            .articleId("KA000000000001")
            .title("Empty Article")
            .content("<p></p><div></div>")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(article);

        // Then
        assertThat(chunks.stream().filter(c -> c.getChunkType() == TextChunk.ChunkType.ARTICLE_CONTENT))
            .isEmpty();
    }

    @Test
    void chunk_sequenceNumbersIncrement_correctly() {
        // Given
        KnowledgeArticle article = KnowledgeArticle.builder()
            .entryId("000000000000001")
            .articleId("KA000000000001")
            .title("Test Article")
            .content("Article content goes here.")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(article);

        // Then
        assertThat(chunks).hasSizeGreaterThanOrEqualTo(2);
        for (int i = 0; i < chunks.size(); i++) {
            assertThat(chunks.get(i).getSequenceNumber()).isEqualTo(i);
        }
    }

    @Test
    void chunk_contextPrefix_includesArticleIdAndTitle() {
        // Given
        KnowledgeArticle article = KnowledgeArticle.builder()
            .entryId("000000000000001")
            .articleId("KA000000000001")
            .title("Password Reset Guide")
            .content("Article content")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(article);

        // Then
        TextChunk summaryChunk = chunks.stream()
            .filter(c -> c.getChunkType() == TextChunk.ChunkType.SUMMARY)
            .findFirst()
            .orElseThrow();

        assertThat(summaryChunk.getContent()).contains("Knowledge Article: Password Reset Guide");
    }

    @Test
    void chunk_fullKnowledgeArticle_createsAllChunks() {
        // Given
        KnowledgeArticle article = KnowledgeArticle.builder()
            .entryId("000000000000001")
            .articleId("KA000000000001")
            .title("Password Reset Guide")
            .articleSummary("Complete guide to resetting passwords")
            .keywords("password, reset, security")
            .content("<h1>How to Reset Password</h1><p>Follow these steps:</p><ol><li>Click Forgot Password</li><li>Enter email</li><li>Check inbox</li></ol>")
            .author("Jane Smith")
            .articleType("How-To")
            .viewCount(500)
            .statusDisplayValue("Published")
            .assignedGroup("Knowledge Management")
            .categoryTier1("Security")
            .categoryTier2("Access")
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(article);

        // Then
        assertThat(chunks).hasSizeGreaterThanOrEqualTo(2);
        assertThat(chunks.stream().anyMatch(c -> c.getChunkType() == TextChunk.ChunkType.SUMMARY)).isTrue();
        assertThat(chunks.stream().anyMatch(c -> c.getChunkType() == TextChunk.ChunkType.ARTICLE_CONTENT)).isTrue();

        // Verify metadata
        TextChunk firstChunk = chunks.get(0);
        assertThat(firstChunk.getMetadata()).containsEntry("author", "Jane Smith");
        assertThat(firstChunk.getMetadata()).containsEntry("keywords", "password, reset, security");
        assertThat(firstChunk.getMetadata()).containsEntry("view_count", "500");
    }

    @Test
    void chunk_complexHtml_cleansCorrectly() {
        // Given
        String complexHtml = "<html><body><h1>Title</h1><p>Paragraph with <strong>bold</strong> and <em>italic</em>.</p>" +
                            "<ul><li>Item 1</li><li>Item 2</li></ul>" +
                            "<table><tr><td>Cell</td></tr></table></body></html>";
        KnowledgeArticle article = KnowledgeArticle.builder()
            .entryId("000000000000001")
            .articleId("KA000000000001")
            .title("Complex HTML Article")
            .content(complexHtml)
            .build();

        // When
        List<TextChunk> chunks = strategy.chunk(article);

        // Then
        List<TextChunk> contentChunks = chunks.stream()
            .filter(c -> c.getChunkType() == TextChunk.ChunkType.ARTICLE_CONTENT)
            .toList();

        assertThat(contentChunks).isNotEmpty();
        String cleanedContent = contentChunks.get(0).getContent();
        assertThat(cleanedContent).contains("Title");
        assertThat(cleanedContent).contains("Paragraph");
        assertThat(cleanedContent).contains("bold");
        assertThat(cleanedContent).contains("italic");
        assertThat(cleanedContent).doesNotContain("<");
        assertThat(cleanedContent).doesNotContain(">");
    }
}
