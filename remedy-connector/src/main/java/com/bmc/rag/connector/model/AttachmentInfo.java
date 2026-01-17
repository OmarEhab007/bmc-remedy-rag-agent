package com.bmc.rag.connector.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Represents attachment metadata from Remedy forms.
 * Note: Actual binary content is retrieved separately via getEntryBlob().
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentInfo {

    public enum AttachmentSource {
        INCIDENT,
        INCIDENT_WORK_LOG,
        WORK_ORDER,
        WORK_ORDER_WORK_LOG,
        CHANGE_REQUEST,
        CHANGE_WORK_LOG,
        KNOWLEDGE_ARTICLE
    }

    private String entryId;          // Parent entry ID
    private int fieldId;             // Field ID of the attachment field
    private String filename;         // Original filename
    private long sizeBytes;          // Size in bytes
    private Instant createDate;
    private AttachmentSource source;
    private String parentRecordId;   // Parent record ID (Incident Number, WO ID, etc.)

    // Parsed content (populated after extraction)
    private String extractedText;
    private String contentType;
    private boolean parsed;

    /**
     * Check if this is a text-parseable document.
     */
    public boolean isTextParseable() {
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
               lowerName.endsWith(".csv");
    }

    /**
     * Check if this is an image file.
     */
    public boolean isImage() {
        if (filename == null) {
            return false;
        }
        String lowerName = filename.toLowerCase();
        return lowerName.endsWith(".png") ||
               lowerName.endsWith(".jpg") ||
               lowerName.endsWith(".jpeg") ||
               lowerName.endsWith(".gif") ||
               lowerName.endsWith(".bmp") ||
               lowerName.endsWith(".tiff");
    }

    /**
     * Get file extension.
     */
    public String getExtension() {
        if (filename == null) {
            return null;
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            return filename.substring(dotIndex + 1).toLowerCase();
        }
        return null;
    }

    /**
     * Get size in human-readable format.
     */
    public String getHumanReadableSize() {
        if (sizeBytes < 1024) {
            return sizeBytes + " B";
        } else if (sizeBytes < 1024 * 1024) {
            return String.format("%.1f KB", sizeBytes / 1024.0);
        } else if (sizeBytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", sizeBytes / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", sizeBytes / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * Check if the attachment size is within a reasonable limit for parsing.
     * Default limit is 50MB.
     */
    public boolean isWithinSizeLimit() {
        return isWithinSizeLimit(50 * 1024 * 1024); // 50MB
    }

    /**
     * Check if the attachment size is within a specified limit.
     */
    public boolean isWithinSizeLimit(long maxBytes) {
        return sizeBytes <= maxBytes;
    }
}
