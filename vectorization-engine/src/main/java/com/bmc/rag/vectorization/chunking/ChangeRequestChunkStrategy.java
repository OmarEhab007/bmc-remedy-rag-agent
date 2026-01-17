package com.bmc.rag.vectorization.chunking;

import com.bmc.rag.connector.model.ChangeRequestRecord;
import com.bmc.rag.connector.model.WorkLogEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Chunking strategy for Change Request records.
 * Creates separate chunks for implementation and rollback plans.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChangeRequestChunkStrategy implements ChunkStrategy<ChangeRequestRecord> {

    private final SemanticChunker semanticChunker;

    @Override
    public String getRecordType() {
        return "ChangeRequest";
    }

    @Override
    public List<TextChunk> chunk(ChangeRequestRecord change) {
        List<TextChunk> chunks = new ArrayList<>();
        int sequence = 0;

        String sourceId = change.getChangeId();
        Map<String, String> baseMetadata = TextChunk.buildITSMMetadata(
            getRecordType(),
            sourceId,
            change.getSummary(),
            change.getAssignedGroup(),
            change.getCategoryPath(),
            change.getStatusDisplayValue()
        );

        // Add change-specific metadata
        if (change.getChangeType() != null) {
            baseMetadata.put("change_type", change.getChangeType());
        }
        if (change.getChangeClass() != null) {
            baseMetadata.put("change_class", change.getChangeClass());
        }
        if (change.getRiskLevel() != null) {
            baseMetadata.put("risk_level", change.getRiskLevel().toString());
        }

        // Context prefix
        String contextPrefix = buildContextPrefix(change);

        // 1. Summary chunk
        StringBuilder summaryContent = new StringBuilder();
        summaryContent.append(contextPrefix);

        if (change.getChangeReason() != null && !change.getChangeReason().isEmpty()) {
            summaryContent.append("\n\nReason: ").append(change.getChangeReason());
        }

        TextChunk summaryChunk = TextChunk.builder()
            .chunkId(TextChunk.generateChunkId(getRecordType(), sourceId, TextChunk.ChunkType.SUMMARY, sequence++))
            .content(summaryContent.toString())
            .chunkType(TextChunk.ChunkType.SUMMARY)
            .sourceType(getRecordType())
            .sourceId(sourceId)
            .entryId(change.getEntryId())
            .metadata(new HashMap<>(baseMetadata))
            .sequenceNumber(sequence - 1)
            .build();
        chunks.add(summaryChunk);

        // 2. Description chunks
        if (change.getDescription() != null && !change.getDescription().isEmpty()) {
            List<String> descChunks = semanticChunker.splitTextWithContext(
                change.getDescription(),
                contextPrefix
            );

            for (String text : descChunks) {
                TextChunk chunk = TextChunk.builder()
                    .chunkId(TextChunk.generateChunkId(getRecordType(), sourceId, TextChunk.ChunkType.DESCRIPTION, sequence++))
                    .content(text)
                    .chunkType(TextChunk.ChunkType.DESCRIPTION)
                    .sourceType(getRecordType())
                    .sourceId(sourceId)
                    .entryId(change.getEntryId())
                    .metadata(new HashMap<>(baseMetadata))
                    .sequenceNumber(sequence - 1)
                    .build();
                chunks.add(chunk);
            }
        }

        // 3. Implementation Plan chunks (HIGH VALUE)
        if (change.hasImplementationPlan()) {
            Map<String, String> implMetadata = new HashMap<>(baseMetadata);
            implMetadata.put("chunk_priority", "high");

            List<String> implChunks = semanticChunker.splitTextWithContext(
                "Implementation Plan:\n" + change.getImplementationPlan(),
                contextPrefix
            );

            for (String text : implChunks) {
                TextChunk chunk = TextChunk.builder()
                    .chunkId(TextChunk.generateChunkId(getRecordType(), sourceId, TextChunk.ChunkType.IMPLEMENTATION, sequence++))
                    .content(text)
                    .chunkType(TextChunk.ChunkType.IMPLEMENTATION)
                    .sourceType(getRecordType())
                    .sourceId(sourceId)
                    .entryId(change.getEntryId())
                    .metadata(implMetadata)
                    .sequenceNumber(sequence - 1)
                    .build();
                chunks.add(chunk);
            }
        }

        // 4. Rollback Plan chunks (HIGH VALUE)
        if (change.hasRollbackPlan()) {
            Map<String, String> rollbackMetadata = new HashMap<>(baseMetadata);
            rollbackMetadata.put("chunk_priority", "high");

            List<String> rollbackChunks = semanticChunker.splitTextWithContext(
                "Rollback/Backout Plan:\n" + change.getRollbackPlan(),
                contextPrefix
            );

            for (String text : rollbackChunks) {
                TextChunk chunk = TextChunk.builder()
                    .chunkId(TextChunk.generateChunkId(getRecordType(), sourceId, TextChunk.ChunkType.ROLLBACK, sequence++))
                    .content(text)
                    .chunkType(TextChunk.ChunkType.ROLLBACK)
                    .sourceType(getRecordType())
                    .sourceId(sourceId)
                    .entryId(change.getEntryId())
                    .metadata(rollbackMetadata)
                    .sequenceNumber(sequence - 1)
                    .build();
                chunks.add(chunk);
            }
        }

        // 5. Work log chunks
        if (change.getWorkLogs() != null && !change.getWorkLogs().isEmpty()) {
            List<TextChunk> workLogChunks = chunkWorkLogs(
                change.getWorkLogs(),
                contextPrefix,
                sourceId,
                change.getEntryId(),
                baseMetadata,
                sequence
            );
            chunks.addAll(workLogChunks);
        }

        log.debug("Created {} chunks for change request {}", chunks.size(), sourceId);
        return chunks;
    }

    private String buildContextPrefix(ChangeRequestRecord change) {
        StringBuilder sb = new StringBuilder();
        sb.append("Change Request ").append(change.getChangeId());

        if (change.getSummary() != null) {
            sb.append(": ").append(change.getSummary());
        }

        if (change.getChangeType() != null) {
            sb.append(" [Type: ").append(change.getChangeType()).append("]");
        }

        if (change.getCategoryPath() != null && !change.getCategoryPath().isEmpty()) {
            sb.append(" [").append(change.getCategoryPath()).append("]");
        }

        return sb.toString();
    }

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

        for (Map.Entry<LocalDate, List<WorkLogEntry>> entry : byDay.entrySet()) {
            LocalDate date = entry.getKey();
            List<WorkLogEntry> dayLogs = entry.getValue();

            StringBuilder dayContent = new StringBuilder();
            dayContent.append("Change Work Log - ").append(date).append(":\n\n");

            for (WorkLogEntry log : dayLogs) {
                dayContent.append(log.getFormattedContent()).append("\n\n---\n\n");
            }

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
