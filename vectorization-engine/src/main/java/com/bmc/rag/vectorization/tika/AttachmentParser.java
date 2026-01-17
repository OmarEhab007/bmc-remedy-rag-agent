package com.bmc.rag.vectorization.tika;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Apache Tika-based attachment parser for extracting text from various document formats.
 * Supports PDF, Word, Excel, PowerPoint, HTML, and plain text files.
 */
@Slf4j
@Component
public class AttachmentParser {

    private final Tika tika;
    private final Parser parser;
    private final ExecutorService executorService;

    // Maximum content length to extract (10MB of text)
    private static final int MAX_CONTENT_LENGTH = 10 * 1024 * 1024;

    // Maximum file size to process (50MB) - P1.3
    private static final long MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024;

    // Parsing timeout in seconds - P1.3
    @Value("${tika.timeout-seconds:60}")
    private int timeoutSeconds = 60;

    // Supported MIME types for text extraction
    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "text/plain",
        "text/html",
        "text/xml",
        "text/csv",
        "text/rtf",
        "application/rtf",
        "application/xml",
        "application/json"
    );

    public AttachmentParser() {
        this.tika = new Tika();
        this.parser = new AutoDetectParser();
        // Single-threaded executor for timeout control - P1.3
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "tika-parser");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Cleanup executor service on shutdown.
     */
    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Parse a file and extract text content with timeout protection.
     *
     * @param filePath Path to the file
     * @return Extracted text, or empty optional if parsing fails or times out
     */
    public Optional<ParsedContent> parse(Path filePath) {
        try {
            // Check file size before processing - P1.3
            long fileSize = Files.size(filePath);
            if (fileSize > MAX_FILE_SIZE_BYTES) {
                log.warn("File {} exceeds maximum size limit ({} bytes > {} bytes)",
                    filePath.getFileName(), fileSize, MAX_FILE_SIZE_BYTES);
                return Optional.empty();
            }

            // Detect MIME type
            String mimeType = tika.detect(filePath);
            log.debug("Detected MIME type for {}: {}", filePath.getFileName(), mimeType);

            if (!isSupportedMimeType(mimeType)) {
                log.debug("Unsupported MIME type: {}", mimeType);
                return Optional.empty();
            }

            // Parse content with timeout - P1.3
            try (InputStream inputStream = Files.newInputStream(filePath)) {
                return parseWithTimeout(inputStream, filePath.getFileName().toString(), mimeType);
            }
        } catch (IOException e) {
            log.error("Failed to read file {}: {}", filePath, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Parse with timeout protection to handle corrupt files - P1.3.
     */
    private Optional<ParsedContent> parseWithTimeout(InputStream inputStream, String filename, String mimeType) {
        Future<Optional<ParsedContent>> future = executorService.submit(() ->
            parseInternal(inputStream, filename, mimeType)
        );

        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.error("Parsing timed out after {} seconds for file: {}", timeoutSeconds, filename);
            return Optional.empty();
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            log.error("Parsing interrupted for file: {}", filename);
            return Optional.empty();
        } catch (ExecutionException e) {
            log.error("Parsing failed for file {}: {}", filename, e.getCause().getMessage());
            return Optional.empty();
        }
    }

    /**
     * Parse an input stream and extract text content with timeout.
     *
     * @param inputStream The input stream
     * @param filename Original filename (for metadata)
     * @param mimeType MIME type (optional, will be detected if null)
     * @return Extracted text, or empty optional if parsing fails
     */
    public Optional<ParsedContent> parse(InputStream inputStream, String filename, String mimeType) {
        return parseWithTimeout(inputStream, filename, mimeType);
    }

    /**
     * Internal parse method called within executor service.
     */
    private Optional<ParsedContent> parseInternal(InputStream inputStream, String filename, String mimeType) {
        try {
            // Detect MIME type if not provided
            String detectedMimeType = mimeType;
            if (detectedMimeType == null) {
                detectedMimeType = tika.detect(inputStream, filename);
            }

            if (!isSupportedMimeType(detectedMimeType)) {
                log.debug("Unsupported MIME type for {}: {}", filename, detectedMimeType);
                return Optional.empty();
            }

            // Set up handler with max content length
            BodyContentHandler handler = new BodyContentHandler(MAX_CONTENT_LENGTH);
            Metadata metadata = new Metadata();
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, filename);

            ParseContext context = new ParseContext();
            context.set(Parser.class, parser);

            // Parse the document
            parser.parse(inputStream, handler, metadata, context);

            String text = handler.toString();

            // Clean up extracted text
            text = cleanExtractedText(text);

            if (text.isEmpty()) {
                log.debug("No text content extracted from {}", filename);
                return Optional.empty();
            }

            ParsedContent content = new ParsedContent(
                text,
                detectedMimeType,
                filename,
                metadata.get("dc:title"),
                metadata.get("dc:creator"),
                extractPageCount(metadata)
            );

            log.debug("Extracted {} characters from {}", text.length(), filename);
            return Optional.of(content);

        } catch (IOException | SAXException | TikaException e) {
            log.error("Failed to parse {}: {}", filename, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Parse a byte array and extract text content.
     *
     * @param data The byte array
     * @param filename Original filename
     * @return Extracted text, or empty optional if parsing fails
     */
    public Optional<ParsedContent> parse(byte[] data, String filename) {
        try (InputStream inputStream = new ByteArrayInputStream(data)) {
            String mimeType = tika.detect(data, filename);
            return parse(inputStream, filename, mimeType);
        } catch (IOException e) {
            log.error("Failed to parse byte array for {}: {}", filename, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Quick text extraction without metadata.
     *
     * @param filePath Path to the file
     * @return Extracted text, or empty string if parsing fails
     */
    public String extractText(Path filePath) {
        try {
            return tika.parseToString(filePath);
        } catch (IOException | TikaException e) {
            log.error("Failed to extract text from {}: {}", filePath, e.getMessage());
            return "";
        }
    }

    /**
     * Check if a MIME type is supported for text extraction.
     */
    public boolean isSupportedMimeType(String mimeType) {
        if (mimeType == null) {
            return false;
        }

        // Check exact match
        if (SUPPORTED_MIME_TYPES.contains(mimeType)) {
            return true;
        }

        // Check for text/* types
        if (mimeType.startsWith("text/")) {
            return true;
        }

        // Check for common variations
        String baseMime = mimeType.split(";")[0].trim().toLowerCase();
        return SUPPORTED_MIME_TYPES.contains(baseMime);
    }

    /**
     * Check if a file extension is supported.
     */
    public boolean isSupportedExtension(String filename) {
        if (filename == null) {
            return false;
        }

        String lowerName = filename.toLowerCase();
        return lowerName.endsWith(".pdf") ||
               lowerName.endsWith(".doc") ||
               lowerName.endsWith(".docx") ||
               lowerName.endsWith(".txt") ||
               lowerName.endsWith(".rtf") ||
               lowerName.endsWith(".xls") ||
               lowerName.endsWith(".xlsx") ||
               lowerName.endsWith(".ppt") ||
               lowerName.endsWith(".pptx") ||
               lowerName.endsWith(".html") ||
               lowerName.endsWith(".htm") ||
               lowerName.endsWith(".xml") ||
               lowerName.endsWith(".csv") ||
               lowerName.endsWith(".json") ||
               lowerName.endsWith(".odt") ||
               lowerName.endsWith(".ods");
    }

    /**
     * Clean up extracted text.
     */
    private String cleanExtractedText(String text) {
        if (text == null) {
            return "";
        }

        return text
            // Normalize line endings
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            // Remove excessive whitespace
            .replaceAll("[ \\t]+", " ")
            // Remove excessive newlines
            .replaceAll("\\n{3,}", "\n\n")
            // Remove leading/trailing whitespace from lines
            .replaceAll("(?m)^[ \\t]+|[ \\t]+$", "")
            // Trim overall
            .trim();
    }

    /**
     * Extract page count from metadata.
     */
    private Integer extractPageCount(Metadata metadata) {
        String pageCount = metadata.get("xmpTPg:NPages");
        if (pageCount == null) {
            pageCount = metadata.get("meta:page-count");
        }
        if (pageCount == null) {
            pageCount = metadata.get("Page-Count");
        }

        if (pageCount != null) {
            try {
                return Integer.parseInt(pageCount);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Parsed content with metadata.
     */
    public record ParsedContent(
        String text,
        String mimeType,
        String filename,
        String title,
        String author,
        Integer pageCount
    ) {
        /**
         * Get text length.
         */
        public int length() {
            return text != null ? text.length() : 0;
        }

        /**
         * Check if content is empty.
         */
        public boolean isEmpty() {
            return text == null || text.isEmpty();
        }
    }
}
