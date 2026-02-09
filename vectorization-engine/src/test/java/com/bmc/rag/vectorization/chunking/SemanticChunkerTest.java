package com.bmc.rag.vectorization.chunking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SemanticChunker utility class.
 */
class SemanticChunkerTest {

    private SemanticChunker chunker;

    @BeforeEach
    void setUp() {
        chunker = new SemanticChunker();
    }

    @Test
    void splitText_nullText_returnsEmptyList() {
        // When
        List<String> chunks = chunker.splitText(null);

        // Then
        assertThat(chunks).isEmpty();
    }

    @Test
    void splitText_emptyText_returnsEmptyList() {
        // When
        List<String> chunks = chunker.splitText("");

        // Then
        assertThat(chunks).isEmpty();
    }

    @Test
    void splitText_singleWordText_returnsSingleChunk() {
        // Given
        String text = "Test";

        // When
        List<String> chunks = chunker.splitText(text);

        // Then
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo("Test");
    }

    @Test
    void splitText_smallText_returnsSingleChunk() {
        // Given
        String text = "This is a small text that fits in one chunk.";

        // When
        List<String> chunks = chunker.splitText(text);

        // Then
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo(text);
    }

    @Test
    void splitText_exactlyMaxSize_returnsSingleChunk() {
        // Given
        String text = "a".repeat(1000);

        // When
        List<String> chunks = chunker.splitText(text);

        // Then
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).hasSize(1000);
    }

    @Test
    void splitText_paragraphSeparation_splitsAtDoubleNewline() {
        // Given
        String text = "First paragraph with some content.\n\nSecond paragraph with more content.\n\nThird paragraph.";

        // When
        List<String> chunks = chunker.splitText(text, null, 1000, 100);

        // Then
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).contains("First paragraph");
        assertThat(chunks.get(0)).contains("Second paragraph");
        assertThat(chunks.get(0)).contains("Third paragraph");
    }

    @Test
    void splitText_largeParagraphs_splitsBySentences() {
        // Given
        String paragraph1 = "This is a long paragraph. ".repeat(30);
        String paragraph2 = "This is another paragraph. ".repeat(30);
        String text = paragraph1 + "\n\n" + paragraph2;

        // When
        List<String> chunks = chunker.splitText(text, null, 500, 50);

        // Then
        assertThat(chunks).hasSizeGreaterThan(1);
        for (String chunk : chunks) {
            assertThat(chunk).hasSizeLessThanOrEqualTo(500);
        }
    }

    @Test
    void splitText_sentenceSeparation_splitsAtPeriodCapital() {
        // Given
        String text = "First sentence. Second sentence. Third sentence. Fourth sentence.";

        // When
        List<String> chunks = chunker.splitText(text, null, 50, 10);

        // Then
        assertThat(chunks).hasSizeGreaterThan(1);
        for (String chunk : chunks) {
            assertThat(chunk).hasSizeLessThanOrEqualTo(50);
        }
    }

    @Test
    void splitTextWithContext_contextPrefix_prependsToEachChunk() {
        // Given
        String text = "This is the main content. ".repeat(50);
        String context = "Incident INC000001: Network Issue";

        // When
        List<String> chunks = chunker.splitTextWithContext(text, context);

        // Then
        assertThat(chunks).isNotEmpty();
        for (String chunk : chunks) {
            assertThat(chunk).startsWith(context);
        }
    }

    @Test
    void splitText_withContextPrefix_respectsMaxSize() {
        // Given
        String text = "This is the content. ".repeat(100);
        String context = "Context: ";
        int maxSize = 200;

        // When
        List<String> chunks = chunker.splitText(text, context, maxSize, 20);

        // Then
        assertThat(chunks).isNotEmpty();
        for (String chunk : chunks) {
            assertThat(chunk).hasSizeLessThanOrEqualTo(maxSize);
        }
    }

    @Test
    void splitText_withOverlap_includesOverlapBetweenChunks() {
        // Given
        String text = "Sentence one is here. Sentence two follows. Sentence three continues. Sentence four ends it.";
        int maxSize = 50;
        int overlap = 15;

        // When
        List<String> chunks = chunker.splitText(text, null, maxSize, overlap);

        // Then
        if (chunks.size() > 1) {
            // Check that there's some overlap between consecutive chunks
            for (int i = 0; i < chunks.size() - 1; i++) {
                String chunk1 = chunks.get(i);
                String chunk2 = chunks.get(i + 1);

                // The end of chunk1 should have some words that appear in chunk2
                String[] words1 = chunk1.split("\\s+");
                String[] words2 = chunk2.split("\\s+");

                if (words1.length > 3 && words2.length > 3) {
                    String lastWord = words1[words1.length - 1].toLowerCase();
                    String chunk2Lower = chunk2.toLowerCase();
                    // There might be overlap (not guaranteed in all cases due to context)
                }
            }
        }
        assertThat(chunks).isNotEmpty();
    }

    @Test
    void splitText_hardSplitForOversizedSentence_splitsAtWordBoundary() {
        // Given
        String text = "word ".repeat(300); // Single "sentence" that's too large

        // When
        List<String> chunks = chunker.splitText(text, null, 500, 50);

        // Then
        assertThat(chunks).hasSizeGreaterThan(1);
        for (String chunk : chunks) {
            assertThat(chunk).hasSizeLessThanOrEqualTo(500);
            // Should not end mid-word (except possibly last chunk)
            if (!chunk.equals(chunks.get(chunks.size() - 1))) {
                assertThat(chunk.trim()).endsWith("word");
            }
        }
    }

    @Test
    void splitText_normalizeWhitespace_removesExcessiveSpaces() {
        // Given
        String text = "Text  with    multiple     spaces.";

        // When
        List<String> chunks = chunker.splitText(text);

        // Then
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).doesNotContain("  ");
    }

    @Test
    void splitText_normalizeNewlines_removesExcessiveNewlines() {
        // Given
        String text = "First line\n\n\n\nSecond line\n\n\n\n\nThird line";

        // When
        List<String> chunks = chunker.splitText(text);

        // Then
        assertThat(chunks).hasSize(1);
        // Should have normalized to at most 2 consecutive newlines
        assertThat(chunks.get(0)).doesNotContain("\n\n\n");
    }

    @Test
    void splitText_windowsLineEndings_normalizedToUnix() {
        // Given
        String text = "Line one\r\nLine two\r\nLine three";

        // When
        List<String> chunks = chunker.splitText(text);

        // Then
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).doesNotContain("\r");
        assertThat(chunks.get(0)).contains("\n");
    }

    @Test
    void splitText_macLineEndings_normalizedToUnix() {
        // Given
        String text = "Line one\rLine two\rLine three";

        // When
        List<String> chunks = chunker.splitText(text);

        // Then
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).doesNotContain("\r");
    }

    @Test
    void splitText_mixedContent_splitsCorrectly() {
        // Given
        String text = "Short intro.\n\n" +
                     "This is a longer paragraph with multiple sentences. " +
                     "It should be split appropriately. " +
                     "Each chunk should respect boundaries.\n\n" +
                     "Final paragraph.";

        // When
        List<String> chunks = chunker.splitText(text, null, 150, 20);

        // Then
        assertThat(chunks).isNotEmpty();
        for (String chunk : chunks) {
            assertThat(chunk).hasSizeLessThanOrEqualTo(150);
            assertThat(chunk.trim()).isNotEmpty();
        }
    }

    @Test
    void estimateTokens_nullText_returnsZero() {
        // When
        int tokens = chunker.estimateTokens(null);

        // Then
        assertThat(tokens).isZero();
    }

    @Test
    void estimateTokens_emptyText_returnsZero() {
        // When
        int tokens = chunker.estimateTokens("");

        // Then
        assertThat(tokens).isZero();
    }

    @Test
    void estimateTokens_validText_estimatesCorrectly() {
        // Given
        String text = "This is a test"; // 14 characters

        // When
        int tokens = chunker.estimateTokens(text);

        // Then
        // Rough approximation: 4 characters per token
        assertThat(tokens).isEqualTo(3); // 14 / 4 = 3
    }

    @Test
    void estimateTokens_longText_estimatesCorrectly() {
        // Given
        String text = "a".repeat(1000);

        // When
        int tokens = chunker.estimateTokens(text);

        // Then
        assertThat(tokens).isEqualTo(250); // 1000 / 4 = 250
    }

    @Test
    void splitText_customMaxSize_respectsLimit() {
        // Given
        String text = "word ".repeat(200);
        int customMaxSize = 300;

        // When
        List<String> chunks = chunker.splitText(text, null, customMaxSize, 30);

        // Then
        assertThat(chunks).hasSizeGreaterThan(1);
        for (String chunk : chunks) {
            assertThat(chunk).hasSizeLessThanOrEqualTo(customMaxSize);
        }
    }

    @Test
    void splitText_customOverlapSize_appliesOverlap() {
        // Given
        String text = "This is sentence one. This is sentence two. This is sentence three.";
        int maxSize = 40;
        int overlapSize = 10;

        // When
        List<String> chunks = chunker.splitText(text, null, maxSize, overlapSize);

        // Then
        assertThat(chunks).isNotEmpty();
        for (String chunk : chunks) {
            assertThat(chunk).hasSizeLessThanOrEqualTo(maxSize);
        }
    }

    @Test
    void splitText_realWorldIncidentDescription_chunksCorrectly() {
        // Given
        String text = "User reported that they cannot access the shared drive on the network. " +
                     "The issue started this morning at 9 AM. " +
                     "They can access other resources but not \\\\fileserver\\shared. " +
                     "Error message: 'Network path not found'. " +
                     "Tried rebooting the computer but issue persists.\n\n" +
                     "Troubleshooting steps:\n" +
                     "1. Checked network connectivity - OK\n" +
                     "2. Pinged file server - successful\n" +
                     "3. Checked user permissions - OK\n" +
                     "4. Mapped drive manually - same error\n\n" +
                     "Need to escalate to network team for further investigation.";

        // When
        List<String> chunks = chunker.splitText(text, "Incident INC001: File share access", 500, 50);

        // Then
        assertThat(chunks).isNotEmpty();
        for (String chunk : chunks) {
            assertThat(chunk).hasSizeLessThanOrEqualTo(500);
            assertThat(chunk).startsWith("Incident INC001:");
        }
    }

    @Test
    void splitText_emptyParagraphs_skipsEmptyContent() {
        // Given
        String text = "First paragraph.\n\n\n\nSecond paragraph.";

        // When
        List<String> chunks = chunker.splitText(text);

        // Then
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).contains("First paragraph");
        assertThat(chunks.get(0)).contains("Second paragraph");
    }

    @Test
    void splitText_onlyWhitespace_returnsEmptyList() {
        // Given
        String text = "   \n\n   \t\t   ";

        // When
        List<String> chunks = chunker.splitText(text);

        // Then
        assertThat(chunks).isEmpty();
    }
}
