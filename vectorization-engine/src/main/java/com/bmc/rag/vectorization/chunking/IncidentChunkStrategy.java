package com.bmc.rag.vectorization.chunking;

import com.bmc.rag.connector.model.IncidentRecord;
import com.bmc.rag.connector.model.WorkLogEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Chunking strategy for Incident records.
 * Creates high-value chunks from resolutions and groups work logs by day.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentChunkStrategy implements ChunkStrategy<IncidentRecord> {

    private final SemanticChunker semanticChunker;

    @Override
    public String getRecordType() {
        return "Incident";
    }

    @Override
    public List<TextChunk> chunk(IncidentRecord incident) {
        List<TextChunk> chunks = new ArrayList<>();
        int sequence = 0;

        String sourceId = incident.getIncidentNumber();
        Map<String, String> baseMetadata = TextChunk.buildITSMMetadata(
            getRecordType(),
            sourceId,
            incident.getSummary(),
            incident.getAssignedGroup(),
            incident.getCategoryPath(),
            incident.getStatusDisplayValue()
        );

        // Add customer info to metadata
        if (incident.getCustomerCompany() != null) {
            baseMetadata.put("customer_company", incident.getCustomerCompany());
        }

        // Context prefix for all chunks (incident summary)
        String contextPrefix = buildContextPrefix(incident);

        // 1. Summary chunk (always create)
        if (incident.getSummary() != null && !incident.getSummary().isEmpty()) {
            TextChunk summaryChunk = TextChunk.builder()
                .chunkId(TextChunk.generateChunkId(getRecordType(), sourceId, TextChunk.ChunkType.SUMMARY, sequence++))
                .content(contextPrefix)
                .chunkType(TextChunk.ChunkType.SUMMARY)
                .sourceType(getRecordType())
                .sourceId(sourceId)
                .entryId(incident.getEntryId())
                .metadata(new HashMap<>(baseMetadata))
                .sequenceNumber(sequence - 1)
                .build();
            chunks.add(summaryChunk);
        }

        // 2. Description chunks
        if (incident.getDescription() != null && !incident.getDescription().isEmpty()) {
            List<String> descChunks = semanticChunker.splitTextWithContext(
                incident.getDescription(),
                contextPrefix
            );

            for (String text : descChunks) {
                TextChunk chunk = TextChunk.builder()
                    .chunkId(TextChunk.generateChunkId(getRecordType(), sourceId, TextChunk.ChunkType.DESCRIPTION, sequence++))
                    .content(text)
                    .chunkType(TextChunk.ChunkType.DESCRIPTION)
                    .sourceType(getRecordType())
                    .sourceId(sourceId)
                    .entryId(incident.getEntryId())
                    .metadata(new HashMap<>(baseMetadata))
                    .sequenceNumber(sequence - 1)
                    .build();
                chunks.add(chunk);
            }
        }

        // 3. Resolution chunk (HIGH VALUE - standalone)
        if (incident.hasResolution()) {
            List<String> resChunks = semanticChunker.splitTextWithContext(
                incident.getResolution(),
                "Resolution for: " + contextPrefix
            );

            for (String text : resChunks) {
                Map<String, String> resMetadata = new HashMap<>(baseMetadata);
                resMetadata.put("chunk_priority", "high");  // Mark as high value

                TextChunk chunk = TextChunk.builder()
                    .chunkId(TextChunk.generateChunkId(getRecordType(), sourceId, TextChunk.ChunkType.RESOLUTION, sequence++))
                    .content(text)
                    .chunkType(TextChunk.ChunkType.RESOLUTION)
                    .sourceType(getRecordType())
                    .sourceId(sourceId)
                    .entryId(incident.getEntryId())
                    .metadata(resMetadata)
                    .sequenceNumber(sequence - 1)
                    .build();
                chunks.add(chunk);
            }
        }

        // 4. Work log chunks (grouped by day)
        if (incident.getWorkLogs() != null && !incident.getWorkLogs().isEmpty()) {
            List<TextChunk> workLogChunks = chunkWorkLogs(
                incident.getWorkLogs(),
                contextPrefix,
                sourceId,
                incident.getEntryId(),
                baseMetadata,
                sequence
            );
            chunks.addAll(workLogChunks);
        }

        log.debug("Created {} chunks for incident {}", chunks.size(), sourceId);
        return chunks;
    }

    /**
     * Build context prefix containing incident summary and key metadata.
     */
    private String buildContextPrefix(IncidentRecord incident) {
        StringBuilder sb = new StringBuilder();
        sb.append("Incident ").append(incident.getIncidentNumber());

        if (incident.getSummary() != null) {
            sb.append(": ").append(incident.getSummary());
        }

        if (incident.getCategoryPath() != null && !incident.getCategoryPath().isEmpty()) {
            sb.append(" [").append(incident.getCategoryPath()).append("]");
        }

        return sb.toString();
    }

    /**
     * Chunk work logs, grouped by day.
     */
    private List<TextChunk> chunkWorkLogs(
            List<WorkLogEntry> workLogs,
            String contextPrefix,
            String sourceId,
            String entryId,
            Map<String, String> baseMetadata,
            int startSequence) {

        List<TextChunk> chunks = new ArrayList<>();
        int sequence = startSequence;

        // Group work logs by day
        Map<LocalDate, List<WorkLogEntry>> byDay = new LinkedHashMap<>();

        for (WorkLogEntry log : workLogs) {
            if (log.hasContent()) {
                LocalDate date = log.getSubmitDate() != null
                    ? log.getSubmitDate().atZone(ZoneId.systemDefault()).toLocalDate()
                    : LocalDate.now();

                byDay.computeIfAbsent(date, k -> new ArrayList<>()).add(log);
            }
        }

        // Create chunks for each day's work logs
        for (Map.Entry<LocalDate, List<WorkLogEntry>> entry : byDay.entrySet()) {
            LocalDate date = entry.getKey();
            List<WorkLogEntry> dayLogs = entry.getValue();

            // Combine day's work logs
            StringBuilder dayContent = new StringBuilder();
            dayContent.append("Work Log - ").append(date).append(":\n\n");

            for (WorkLogEntry log : dayLogs) {
                dayContent.append(log.getFormattedContent()).append("\n\n---\n\n");
            }

            // Split if necessary
            List<String> logChunks = semanticChunker.splitTextWithContext(
                dayContent.toString(),
                contextPrefix
            );

            for (String text : logChunks) {
                Map<String, String> logMetadata = new HashMap<>(baseMetadata);
                logMetadata.put("work_log_date", date.toString());

                TextChunk chunk = TextChunk.builder()
                    .chunkId(TextChunk.generateChunkId(getRecordType(), sourceId, TextChunk.ChunkType.WORK_LOG, sequence++))
                    .content(text)
                    .chunkType(TextChunk.ChunkType.WORK_LOG)
                    .sourceType(getRecordType())
                    .sourceId(sourceId)
                    .entryId(entryId)
                    .metadata(logMetadata)
                    .sequenceNumber(sequence - 1)
                    .build();
                chunks.add(chunk);
            }
        }

        return chunks;
    }
}
