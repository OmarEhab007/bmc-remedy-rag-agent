package com.bmc.rag.vectorization.chunking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * ITSM-aware semantic chunking utility.
 * Splits text at natural boundaries (sentences, paragraphs) while respecting size limits.
 */
@Slf4j
@Component
public class SemanticChunker {

    // Default chunk configuration
    private static final int DEFAULT_MAX_CHUNK_SIZE = 1000;  // ~250 tokens
    private static final int DEFAULT_OVERLAP_SIZE = 100;     // ~25 tokens

    // Sentence boundary patterns
    private static final Pattern SENTENCE_END = Pattern.compile(
        "(?<=[.!?])\\s+(?=[A-Z])|(?<=\\n)\\s*(?=\\n)|(?<=:)\\s*(?=\\n)"
    );

    // Paragraph boundary pattern
    private static final Pattern PARAGRAPH_BREAK = Pattern.compile("\\n\\s*\\n");

    /**
     * Split text into semantic chunks with context injection.
     *
     * @param text The text to chunk
     * @param contextPrefix Optional context to prepend to each chunk (e.g., incident summary)
     * @param maxChunkSize Maximum chunk size in characters
     * @param overlapSize Overlap between chunks
     * @return List of text chunks
     */
    public List<String> splitText(String text, String contextPrefix, int maxChunkSize, int overlapSize) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        // Clean and normalize text
        String cleanedText = cleanText(text);

        if (cleanedText.length() <= maxChunkSize) {
            // Text fits in single chunk
            return List.of(prependContext(cleanedText, contextPrefix, maxChunkSize));
        }

        List<String> chunks = new ArrayList<>();

        // Calculate effective max size accounting for context
        int effectiveMaxSize = maxChunkSize;
        if (contextPrefix != null && !contextPrefix.isEmpty()) {
            effectiveMaxSize = maxChunkSize - contextPrefix.length() - 4; // 4 for "\n\n" separator
        }

        // First try to split by paragraphs
        String[] paragraphs = PARAGRAPH_BREAK.split(cleanedText);

        StringBuilder currentChunk = new StringBuilder();
        int startOffset = 0;

        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) {
                continue;
            }

            // If paragraph itself is too large, split by sentences
            if (paragraph.length() > effectiveMaxSize) {
                // Flush current chunk first
                if (currentChunk.length() > 0) {
                    chunks.add(prependContext(currentChunk.toString().trim(), contextPrefix, maxChunkSize));
                    currentChunk = new StringBuilder();
                }

                // Split paragraph by sentences
                List<String> sentenceChunks = splitBySentences(paragraph, effectiveMaxSize, overlapSize);
                for (String sentenceChunk : sentenceChunks) {
                    chunks.add(prependContext(sentenceChunk, contextPrefix, maxChunkSize));
                }
            } else if (currentChunk.length() + paragraph.length() + 2 <= effectiveMaxSize) {
                // Paragraph fits in current chunk
                if (currentChunk.length() > 0) {
                    currentChunk.append("\n\n");
                }
                currentChunk.append(paragraph);
            } else {
                // Start new chunk with overlap
                if (currentChunk.length() > 0) {
                    chunks.add(prependContext(currentChunk.toString().trim(), contextPrefix, maxChunkSize));
                }

                // Create overlap from end of previous chunk
                String overlap = getOverlapText(currentChunk.toString(), overlapSize);
                currentChunk = new StringBuilder();
                if (!overlap.isEmpty()) {
                    currentChunk.append(overlap).append(" ");
                }
                currentChunk.append(paragraph);
            }
        }

        // Add final chunk
        if (currentChunk.length() > 0) {
            chunks.add(prependContext(currentChunk.toString().trim(), contextPrefix, maxChunkSize));
        }

        log.debug("Split text into {} chunks", chunks.size());
        return chunks;
    }

    /**
     * Split text using default parameters.
     */
    public List<String> splitText(String text) {
        return splitText(text, null, DEFAULT_MAX_CHUNK_SIZE, DEFAULT_OVERLAP_SIZE);
    }

    /**
     * Split text with context prefix.
     */
    public List<String> splitTextWithContext(String text, String contextPrefix) {
        return splitText(text, contextPrefix, DEFAULT_MAX_CHUNK_SIZE, DEFAULT_OVERLAP_SIZE);
    }

    /**
     * Split text by sentences when paragraph is too large.
     */
    private List<String> splitBySentences(String text, int maxSize, int overlapSize) {
        List<String> chunks = new ArrayList<>();
        String[] sentences = SENTENCE_END.split(text);

        StringBuilder currentChunk = new StringBuilder();

        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.isEmpty()) {
                continue;
            }

            // If single sentence is too large, use hard split
            if (sentence.length() > maxSize) {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                }
                chunks.addAll(hardSplit(sentence, maxSize, overlapSize));
            } else if (currentChunk.length() + sentence.length() + 1 <= maxSize) {
                if (currentChunk.length() > 0) {
                    currentChunk.append(" ");
                }
                currentChunk.append(sentence);
            } else {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                }
                String overlap = getOverlapText(currentChunk.toString(), overlapSize);
                currentChunk = new StringBuilder();
                if (!overlap.isEmpty()) {
                    currentChunk.append(overlap).append(" ");
                }
                currentChunk.append(sentence);
            }
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    /**
     * Hard split when text cannot be split semantically.
     */
    private List<String> hardSplit(String text, int maxSize, int overlapSize) {
        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + maxSize, text.length());

            // Try to find a word boundary
            if (end < text.length()) {
                int lastSpace = text.lastIndexOf(' ', end);
                if (lastSpace > start + maxSize / 2) {
                    end = lastSpace;
                }
            }

            chunks.add(text.substring(start, end).trim());

            // Move start with overlap
            start = end - overlapSize;
            if (start < 0) {
                start = end;
            }
        }

        return chunks;
    }

    /**
     * Get overlap text from end of previous chunk.
     */
    private String getOverlapText(String text, int overlapSize) {
        if (text == null || text.length() <= overlapSize) {
            return "";
        }

        String overlap = text.substring(text.length() - overlapSize);

        // Try to start at a word boundary
        int firstSpace = overlap.indexOf(' ');
        if (firstSpace > 0 && firstSpace < overlapSize / 2) {
            overlap = overlap.substring(firstSpace + 1);
        }

        return overlap.trim();
    }

    /**
     * Prepend context to chunk if provided.
     */
    private String prependContext(String chunk, String contextPrefix, int maxSize) {
        if (contextPrefix == null || contextPrefix.isEmpty()) {
            return chunk;
        }

        String result = contextPrefix + "\n\n" + chunk;

        // Truncate if too long
        if (result.length() > maxSize) {
            result = result.substring(0, maxSize);
            // Try to end at word boundary
            int lastSpace = result.lastIndexOf(' ');
            if (lastSpace > maxSize * 0.8) {
                result = result.substring(0, lastSpace);
            }
        }

        return result;
    }

    /**
     * Clean and normalize text.
     */
    private String cleanText(String text) {
        if (text == null) {
            return "";
        }

        return text
            // Normalize line endings
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            // Remove excessive whitespace
            .replaceAll("[ \\t]+", " ")
            // Remove excessive newlines (more than 2)
            .replaceAll("\\n{3,}", "\n\n")
            // Trim
            .trim();
    }

    /**
     * Estimate token count (rough approximation).
     * English text averages ~4 characters per token.
     */
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.length() / 4;
    }
}
