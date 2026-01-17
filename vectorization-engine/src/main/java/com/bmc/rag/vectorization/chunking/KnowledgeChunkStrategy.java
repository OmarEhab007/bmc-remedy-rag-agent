package com.bmc.rag.vectorization.chunking;

import com.bmc.rag.connector.model.KnowledgeArticle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Chunking strategy for Knowledge Articles.
 * Handles HTML content and preserves article structure.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeChunkStrategy implements ChunkStrategy<KnowledgeArticle> {

    private final SemanticChunker semanticChunker;

    // Pattern to strip HTML tags
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    // Pattern to decode common HTML entities
    private static final Pattern HTML_ENTITY_PATTERN = Pattern.compile("&[a-zA-Z]+;");

    @Override
    public String getRecordType() {
        return "KnowledgeArticle";
    }

    @Override
    public List<TextChunk> chunk(KnowledgeArticle article) {
        List<TextChunk> chunks = new ArrayList<>();
        int sequence = 0;

        String sourceId = article.getArticleId();
        Map<String, String> baseMetadata = TextChunk.buildITSMMetadata(
            getRecordType(),
            sourceId,
            article.getTitle(),
            article.getAssignedGroup(),
            article.getCategoryPath(),
            article.getStatusDisplayValue()
        );

        // Add article-specific metadata
        if (article.getAuthor() != null) {
            baseMetadata.put("author", article.getAuthor());
        }
        if (article.getKeywords() != null) {
            baseMetadata.put("keywords", article.getKeywords());
        }
        if (article.getArticleType() != null) {
            baseMetadata.put("article_type", article.getArticleType());
        }
        if (article.getViewCount() != null) {
            baseMetadata.put("view_count", article.getViewCount().toString());
        }

        // Context prefix
        String contextPrefix = buildContextPrefix(article);

        // 1. Summary chunk (title + summary)
        StringBuilder summaryContent = new StringBuilder();
        summaryContent.append("Knowledge Article: ").append(article.getTitle());

        if (article.getArticleSummary() != null && !article.getArticleSummary().isEmpty()) {
            summaryContent.append("\n\nSummary: ").append(article.getArticleSummary());
        }

        if (article.getKeywords() != null && !article.getKeywords().isEmpty()) {
            summaryContent.append("\n\nKeywords: ").append(article.getKeywords());
        }

        TextChunk summaryChunk = TextChunk.builder()
            .chunkId(TextChunk.generateChunkId(getRecordType(), sourceId, TextChunk.ChunkType.SUMMARY, sequence++))
            .content(summaryContent.toString())
            .chunkType(TextChunk.ChunkType.SUMMARY)
            .sourceType(getRecordType())
            .sourceId(sourceId)
            .entryId(article.getEntryId())
            .metadata(new HashMap<>(baseMetadata))
            .sequenceNumber(sequence - 1)
            .build();
        chunks.add(summaryChunk);

        // 2. Article content chunks
        if (article.getContent() != null && !article.getContent().isEmpty()) {
            String cleanContent = cleanHtmlContent(article.getContent());

            if (!cleanContent.isEmpty()) {
                // Mark as high priority since knowledge articles are authoritative
                Map<String, String> contentMetadata = new HashMap<>(baseMetadata);
                contentMetadata.put("chunk_priority", "high");

                List<String> contentChunks = semanticChunker.splitTextWithContext(
                    cleanContent,
                    contextPrefix
                );

                for (String text : contentChunks) {
                    TextChunk chunk = TextChunk.builder()
                        .chunkId(TextChunk.generateChunkId(getRecordType(), sourceId, TextChunk.ChunkType.ARTICLE_CONTENT, sequence++))
                        .content(text)
                        .chunkType(TextChunk.ChunkType.ARTICLE_CONTENT)
                        .sourceType(getRecordType())
                        .sourceId(sourceId)
                        .entryId(article.getEntryId())
                        .metadata(contentMetadata)
                        .sequenceNumber(sequence - 1)
                        .build();
                    chunks.add(chunk);
                }
            }
        }

        log.debug("Created {} chunks for knowledge article {}", chunks.size(), sourceId);
        return chunks;
    }

    private String buildContextPrefix(KnowledgeArticle article) {
        StringBuilder sb = new StringBuilder();
        sb.append("Knowledge Article ").append(article.getArticleId());

        if (article.getTitle() != null) {
            sb.append(": ").append(article.getTitle());
        }

        if (article.getCategoryPath() != null && !article.getCategoryPath().isEmpty()) {
            sb.append(" [").append(article.getCategoryPath()).append("]");
        }

        return sb.toString();
    }

    /**
     * Clean HTML content to plain text.
     */
    private String cleanHtmlContent(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }

        String text = html;

        // Replace common block elements with newlines
        text = text.replaceAll("(?i)<br\\s*/?>", "\n");
        text = text.replaceAll("(?i)</p>", "\n\n");
        text = text.replaceAll("(?i)</div>", "\n");
        text = text.replaceAll("(?i)</li>", "\n");
        text = text.replaceAll("(?i)</tr>", "\n");
        text = text.replaceAll("(?i)<h[1-6][^>]*>", "\n\n");
        text = text.replaceAll("(?i)</h[1-6]>", "\n\n");

        // Strip remaining HTML tags
        text = HTML_TAG_PATTERN.matcher(text).replaceAll("");

        // Decode common HTML entities
        text = text.replace("&nbsp;", " ");
        text = text.replace("&amp;", "&");
        text = text.replace("&lt;", "<");
        text = text.replace("&gt;", ">");
        text = text.replace("&quot;", "\"");
        text = text.replace("&#39;", "'");
        text = text.replace("&apos;", "'");

        // Clean up whitespace
        text = text.replaceAll("[ \\t]+", " ");
        text = text.replaceAll("\\n[ \\t]+", "\n");
        text = text.replaceAll("\\n{3,}", "\n\n");
        text = text.trim();

        return text;
    }
}
