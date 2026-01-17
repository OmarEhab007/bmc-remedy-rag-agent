package com.bmc.rag.connector.extractor;

import com.bmc.arsys.api.*;
import com.bmc.rag.connector.connection.ThreadLocalARContext;
import com.bmc.rag.connector.model.AttachmentInfo;
import com.bmc.rag.connector.util.FieldIdConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Extracts attachment binary content from Remedy forms.
 * Note: Standard queries return only attachment metadata (filename, size).
 * Use getEntryBlob() to retrieve actual binary content.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttachmentExtractor {

    private final ThreadLocalARContext arContext;
    private final Tika tika = new Tika();

    // Maximum attachment size to process (50MB)
    private static final long MAX_ATTACHMENT_SIZE = 50 * 1024 * 1024;

    // Supported file extensions for text extraction
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
        "pdf", "doc", "docx", "txt", "rtf", "xls", "xlsx",
        "ppt", "pptx", "html", "htm", "xml", "csv", "odt", "ods"
    );

    /**
     * Extract attachment content by entry ID and field ID.
     *
     * @param formName The Remedy form name
     * @param entryId The entry ID
     * @param fieldId The attachment field ID
     * @return AttachmentInfo with extracted text, or empty optional if not found
     */
    public Optional<AttachmentInfo> extractAttachment(String formName, String entryId, int fieldId) {
        return arContext.executeWithRetry(ctx -> {
            try {
                // Get attachment metadata first
                Entry entry = ctx.getEntry(formName, entryId, null);
                if (entry == null) {
                    log.warn("Entry not found: {} in {}", entryId, formName);
                    return Optional.empty();
                }

                Value attachmentValue = entry.get(fieldId);
                if (attachmentValue == null || attachmentValue.getValue() == null) {
                    return Optional.empty();
                }

                // Attachment value contains metadata
                AttachmentValue attValue = (AttachmentValue) attachmentValue.getValue();
                String filename = attValue.getName();
                long size = attValue.getOriginalSize();

                log.debug("Found attachment: {} ({} bytes)", filename, size);

                AttachmentInfo info = AttachmentInfo.builder()
                    .entryId(entryId)
                    .fieldId(fieldId)
                    .filename(filename)
                    .sizeBytes(size)
                    .build();

                // Check if we should extract text
                if (!info.isTextParseable()) {
                    log.debug("Attachment {} is not text-parseable", filename);
                    return Optional.of(info);
                }

                if (size > MAX_ATTACHMENT_SIZE) {
                    log.warn("Attachment {} exceeds size limit ({} > {} bytes)",
                        filename, size, MAX_ATTACHMENT_SIZE);
                    return Optional.of(info);
                }

                // Extract binary content to temp file
                Path tempFile = extractToTempFile(ctx, formName, entryId, fieldId, filename);
                if (tempFile != null) {
                    try {
                        String extractedText = parseWithTika(tempFile);
                        info.setExtractedText(extractedText);
                        info.setContentType(tika.detect(tempFile));
                        info.setParsed(true);
                        log.debug("Successfully extracted text from {} ({} chars)",
                            filename, extractedText != null ? extractedText.length() : 0);
                    } finally {
                        // Clean up temp file
                        Files.deleteIfExists(tempFile);
                    }
                }

                return Optional.of(info);
            } catch (ARException e) {
                log.error("Failed to extract attachment from {} entry {}: {}",
                    formName, entryId, e.getMessage());
                return Optional.empty();
            } catch (IOException e) {
                log.error("IO error extracting attachment: {}", e.getMessage());
                return Optional.empty();
            }
        });
    }

    /**
     * Extract attachments for an incident.
     *
     * @param entryId The incident entry ID
     * @return List of attachment info with extracted text
     */
    public List<AttachmentInfo> extractIncidentAttachments(String entryId) {
        return extractFormAttachments(
            FieldIdConstants.Incident.FORM_NAME,
            entryId,
            new int[]{
                FieldIdConstants.Attachment.ATTACHMENT_1,
                FieldIdConstants.Attachment.ATTACHMENT_2,
                FieldIdConstants.Attachment.ATTACHMENT_3
            },
            AttachmentInfo.AttachmentSource.INCIDENT
        );
    }

    /**
     * Extract attachments for a work order.
     *
     * @param entryId The work order entry ID
     * @return List of attachment info with extracted text
     */
    public List<AttachmentInfo> extractWorkOrderAttachments(String entryId) {
        return extractFormAttachments(
            FieldIdConstants.WorkOrder.FORM_NAME,
            entryId,
            new int[]{
                FieldIdConstants.Attachment.ATTACHMENT_1,
                FieldIdConstants.Attachment.ATTACHMENT_2,
                FieldIdConstants.Attachment.ATTACHMENT_3
            },
            AttachmentInfo.AttachmentSource.WORK_ORDER
        );
    }

    /**
     * Extract attachments for a change request.
     *
     * @param entryId The change request entry ID
     * @return List of attachment info with extracted text
     */
    public List<AttachmentInfo> extractChangeRequestAttachments(String entryId) {
        return extractFormAttachments(
            FieldIdConstants.ChangeRequest.FORM_NAME,
            entryId,
            new int[]{
                FieldIdConstants.Attachment.ATTACHMENT_1,
                FieldIdConstants.Attachment.ATTACHMENT_2,
                FieldIdConstants.Attachment.ATTACHMENT_3
            },
            AttachmentInfo.AttachmentSource.CHANGE_REQUEST
        );
    }

    /**
     * Extract attachments for a work log entry.
     *
     * @param formName The work log form name
     * @param entryId The work log entry ID
     * @param source The attachment source type
     * @return List of attachment info with extracted text
     */
    public List<AttachmentInfo> extractWorkLogAttachments(
            String formName,
            String entryId,
            AttachmentInfo.AttachmentSource source) {

        return extractFormAttachments(
            formName,
            entryId,
            new int[]{
                FieldIdConstants.Attachment.WORK_LOG_ATTACHMENT_1,
                FieldIdConstants.Attachment.WORK_LOG_ATTACHMENT_2,
                FieldIdConstants.Attachment.WORK_LOG_ATTACHMENT_3
            },
            source
        );
    }

    /**
     * Internal method to extract attachments from a form.
     */
    private List<AttachmentInfo> extractFormAttachments(
            String formName,
            String entryId,
            int[] fieldIds,
            AttachmentInfo.AttachmentSource source) {

        List<AttachmentInfo> attachments = new ArrayList<>();

        for (int fieldId : fieldIds) {
            Optional<AttachmentInfo> attachment = extractAttachment(formName, entryId, fieldId);
            attachment.ifPresent(info -> {
                info.setSource(source);
                attachments.add(info);
            });
        }

        return attachments;
    }

    /**
     * Extract attachment binary content to a temporary file.
     *
     * @return Path to temp file, or null if extraction failed
     */
    private Path extractToTempFile(
            ARServerUser ctx,
            String formName,
            String entryId,
            int fieldId,
            String filename) throws ARException, IOException {

        // Create temp file with appropriate extension
        String extension = getExtension(filename);
        Path tempFile = Files.createTempFile("remedy_attachment_", "." + extension);

        try {
            // Use getEntryBlob to retrieve binary content
            ctx.getEntryBlob(formName, entryId, fieldId, tempFile.toString());
            return tempFile;
        } catch (ARException e) {
            // Clean up on failure
            Files.deleteIfExists(tempFile);
            throw e;
        }
    }

    /**
     * Parse file content using Apache Tika.
     */
    private String parseWithTika(Path filePath) {
        try {
            String content = tika.parseToString(filePath);
            if (content != null) {
                // Clean up the text
                content = content.trim();
                // Remove excessive whitespace
                content = content.replaceAll("\\s+", " ");
            }
            return content;
        } catch (IOException | TikaException e) {
            log.warn("Failed to parse file with Tika: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get file extension from filename.
     */
    private String getExtension(String filename) {
        if (filename == null) {
            return "bin";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1).toLowerCase();
        }
        return "bin";
    }

    /**
     * Check if a file extension is supported for text extraction.
     */
    public boolean isSupported(String filename) {
        String extension = getExtension(filename);
        return SUPPORTED_EXTENSIONS.contains(extension);
    }
}
