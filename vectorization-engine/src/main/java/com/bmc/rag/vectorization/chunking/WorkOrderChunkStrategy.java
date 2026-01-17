package com.bmc.rag.vectorization.chunking;

import com.bmc.rag.connector.model.WorkLogEntry;
import com.bmc.rag.connector.model.WorkOrderRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Chunking strategy for Work Order records.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkOrderChunkStrategy implements ChunkStrategy<WorkOrderRecord> {

    private final SemanticChunker semanticChunker;

    @Override
    public String getRecordType() {
        return "WorkOrder";
    }

    @Override
    public List<TextChunk> chunk(WorkOrderRecord workOrder) {
        List<TextChunk> chunks = new ArrayList<>();
        int sequence = 0;

        String sourceId = workOrder.getWorkOrderId();
        Map<String, String> baseMetadata = TextChunk.buildITSMMetadata(
            getRecordType(),
            sourceId,
            workOrder.getSummary(),
            workOrder.getAssignedGroup(),
            workOrder.getCategoryPath(),
            workOrder.getStatusDisplayValue()
        );

        // Add requester info to metadata
        if (workOrder.getRequesterFullName() != null) {
            baseMetadata.put("requester", workOrder.getRequesterFullName());
        }
        if (workOrder.getLocationCompany() != null) {
            baseMetadata.put("location_company", workOrder.getLocationCompany());
        }

        // Context prefix
        String contextPrefix = buildContextPrefix(workOrder);

        // 1. Summary chunk
        if (workOrder.getSummary() != null && !workOrder.getSummary().isEmpty()) {
            TextChunk summaryChunk = TextChunk.builder()
                .chunkId(TextChunk.generateChunkId(getRecordType(), sourceId, TextChunk.ChunkType.SUMMARY, sequence++))
                .content(contextPrefix)
                .chunkType(TextChunk.ChunkType.SUMMARY)
                .sourceType(getRecordType())
                .sourceId(sourceId)
                .entryId(workOrder.getEntryId())
                .metadata(new HashMap<>(baseMetadata))
                .sequenceNumber(sequence - 1)
                .build();
            chunks.add(summaryChunk);
        }

        // 2. Description chunks
        if (workOrder.getDescription() != null && !workOrder.getDescription().isEmpty()) {
            List<String> descChunks = semanticChunker.splitTextWithContext(
                workOrder.getDescription(),
                contextPrefix
            );

            for (String text : descChunks) {
                TextChunk chunk = TextChunk.builder()
                    .chunkId(TextChunk.generateChunkId(getRecordType(), sourceId, TextChunk.ChunkType.DESCRIPTION, sequence++))
                    .content(text)
                    .chunkType(TextChunk.ChunkType.DESCRIPTION)
                    .sourceType(getRecordType())
                    .sourceId(sourceId)
                    .entryId(workOrder.getEntryId())
                    .metadata(new HashMap<>(baseMetadata))
                    .sequenceNumber(sequence - 1)
                    .build();
                chunks.add(chunk);
            }
        }

        // 3. Work log chunks (grouped by day)
        if (workOrder.getWorkLogs() != null && !workOrder.getWorkLogs().isEmpty()) {
            List<TextChunk> workLogChunks = chunkWorkLogs(
                workOrder.getWorkLogs(),
                contextPrefix,
                sourceId,
                workOrder.getEntryId(),
                baseMetadata,
                sequence
            );
            chunks.addAll(workLogChunks);
        }

        log.debug("Created {} chunks for work order {}", chunks.size(), sourceId);
        return chunks;
    }

    private String buildContextPrefix(WorkOrderRecord workOrder) {
        StringBuilder sb = new StringBuilder();
        sb.append("Work Order ").append(workOrder.getWorkOrderId());

        if (workOrder.getSummary() != null) {
            sb.append(": ").append(workOrder.getSummary());
        }

        if (workOrder.getCategoryPath() != null && !workOrder.getCategoryPath().isEmpty()) {
            sb.append(" [").append(workOrder.getCategoryPath()).append("]");
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
            dayContent.append("Work Info - ").append(date).append(":\n\n");

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
