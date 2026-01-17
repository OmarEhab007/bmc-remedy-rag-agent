# Implementation Plan: BMC Remedy Integration

**Spec Reference**: 002-remedy-integration
**Status**: ✅ Complete
**Author**: BMC RAG Team
**Created**: 2024-01-15

---

## 1. Technical Context

### 1.1 Current State
The project has the following components:
- `FieldIdConstants.java` with complete field mappings
- Basic extractor classes (IncidentExtractor, etc.)
- ThreadLocalARContext for connection management
- Model classes (IncidentRecord, ChangeRequestRecord, etc.)

### 1.2 Affected Components

| Component | Path | Change Type |
|-----------|------|-------------|
| IncidentExtractor | `remedy-connector/src/.../extractor/IncidentExtractor.java` | Modified |
| ChangeRequestExtractor | `remedy-connector/src/.../extractor/ChangeRequestExtractor.java` | Modified |
| WorkOrderExtractor | `remedy-connector/src/.../extractor/WorkOrderExtractor.java` | Modified |
| KnowledgeExtractor | `remedy-connector/src/.../extractor/KnowledgeExtractor.java` | Modified |
| WorkLogExtractor | `remedy-connector/src/.../extractor/WorkLogExtractor.java` | Modified |
| AttachmentExtractor | `remedy-connector/src/.../extractor/AttachmentExtractor.java` | Modified |
| SyncService | `remedy-connector/src/.../service/SyncService.java` | New |
| BulkExtractorService | `remedy-connector/src/.../service/BulkExtractorService.java` | New |

### 1.3 Dependencies
- BMC AR API (arAPI-91.9.jar)
- Apache Tika for attachment processing
- LangChain4j for embedding generation

---

## 2. Constitution Compliance

| Principle | Implementation Approach |
|-----------|------------------------|
| I. Air-Gap Mandate | Direct Java RPC to on-premise Remedy server |
| II. Field ID Supremacy | All queries use FieldIdConstants, never field names |
| III. Thread-Safe Connections | ThreadLocalARContext for all operations |
| IV. Security (ReBAC) | Extract assigned_group (1000000217) into metadata |
| V. Semantic Chunking | Resolution (1000000156) as standalone high-value chunk |
| VI. Date Handling | Use Field ID 6, compare as Unix epoch integers |

---

## 3. Architecture Decisions

### 3.1 Decision: Pagination Strategy
**Context**: Remedy has server-side limits on query results (~2000)
**Options Considered**:
1. Max chunk size (2000) - Faster, risk of timeouts
2. Conservative chunk size (500) - Slower, reliable
3. Adaptive sizing - Complex, adjusts on errors

**Decision**: Start with 500, reduce to 100 on ARERR 93

### 3.2 Decision: Work Log Join Strategy
**Context**: Work logs are in separate forms, need to be joined to parents
**Options Considered**:
1. N+1 queries - Fetch work logs per parent
2. Batch query - Fetch work logs for batch of parents

**Decision**: Batch query with IN clause on parent IDs (max 100 per batch)

### 3.3 Decision: Attachment Storage
**Context**: Binary attachments need temporary storage for Tika processing
**Options Considered**:
1. Memory buffer - Fast, limited by heap
2. Temp file - Slower, no memory limit
3. Streaming - Complex, optimal for large files

**Decision**: Temp file with 10MB limit, skip larger attachments

---

## 4. Implementation Phases

### Phase 1: Core Extractors ✅ COMPLETE
**Goal**: Extract basic records without work logs or attachments
**Status**: Complete
**Implemented in**: `remedy-connector/src/main/java/com/bmc/rag/connector/extractor/`

#### Tasks
- [x] Implement IncidentExtractor with pagination (`getListEntryObjects()`, configurable chunk size)
- [x] Implement ChangeRequestExtractor with full field mapping
- [x] Implement WorkOrderExtractor with field mapping
- [x] Implement KnowledgeExtractor (published articles filter)
- [x] Add error handling with retry logic (`executeWithRetry()` in ThreadLocalARContext)
- [x] Implement QualifierBuilder for safe query construction

#### Deliverables
- Working extractors for all four record types ✅
- Pagination handling with configurable chunk size ✅
- Retry logic for ARERR 92/93 ✅

### Phase 2: Work Log Integration ✅ COMPLETE
**Goal**: Extract and join work logs to parent records
**Status**: Complete
**Implemented in**: `remedy-connector/src/main/java/com/bmc/rag/connector/extractor/WorkLogExtractor.java`

#### Tasks
- [x] Implement WorkLogExtractor for HPD:WorkLog
- [x] Implement WorkLogExtractor for CHG:WorkLog
- [x] Implement WorkLogExtractor for WOI:WorkInfo
- [x] Add batch fetching with `batchExtractIncidentWorkLogs()`, `batchExtractChangeWorkLogs()`
- [x] Join work logs to parent records in IncrementalSyncService

#### Deliverables
- Complete work log extraction ✅
- Efficient batch queries ✅
- Parent-child relationships preserved ✅

### Phase 3: Attachment Processing ✅ COMPLETE
**Goal**: Extract and process document attachments
**Status**: Complete
**Implemented in**:
- `remedy-connector/src/main/java/com/bmc/rag/connector/extractor/AttachmentExtractor.java`
- `vectorization-engine/src/main/java/com/bmc/rag/vectorization/tika/AttachmentParser.java`

#### Tasks
- [x] Implement AttachmentExtractor using `getEntryBlob()` (extracts to temp file)
- [x] Integrate Apache Tika for text extraction
- [x] Handle PDF, DOCX, DOC, TXT, RTF, XLS, XLSX, PPT, PPTX, HTML, XML, CSV formats
- [x] Implement size limits (50MB max) and error handling
- [x] Add attachment metadata to records (filename, size, contentType, extractedText)

#### Deliverables
- Binary attachment retrieval ✅
- Text extraction pipeline with Tika ✅
- Format-specific handlers with MIME type detection ✅

### Phase 4: Incremental Sync ✅ COMPLETE
**Goal**: CDC-based synchronization
**Status**: Complete
**Implemented in**: `vector-store/src/main/java/com/bmc/rag/store/sync/IncrementalSyncService.java`

#### Tasks
- [x] Implement SyncService with scheduler (`@Scheduled` every 15 minutes)
- [x] Store LAST_SYNC_TIMESTAMP in database via SyncStateRepository
- [x] Query modified records using Field ID 6 (Last Modified Date)
- [x] Delete and re-insert embeddings for modified records
- [x] Add sync status tracking with atomic locking (prevents concurrent syncs)
- [x] Implement stale lock detection and recovery (60 minute timeout)

#### Deliverables
- Scheduled incremental sync ✅
- Persistent sync state with lock management ✅
- Force full sync API (`forceFullSync()`) ✅

---

## 5. Technical Design

### 5.1 Class Design

```java
public interface RecordExtractor<T extends ITSMRecord> {
    List<T> extractBatch(int offset, int limit);
    List<T> extractModifiedSince(long timestamp);
    int getTotalCount();
}

public class IncidentExtractor implements RecordExtractor<IncidentRecord> {
    private static final int[] FIELD_IDS = {
        FieldIdConstants.Incident.INCIDENT_NUMBER,
        FieldIdConstants.Incident.SUMMARY,
        FieldIdConstants.Incident.DESCRIPTION,
        FieldIdConstants.Incident.RESOLUTION,
        FieldIdConstants.Incident.ASSIGNED_GROUP,
        FieldIdConstants.LAST_MODIFIED_DATE
    };

    @Override
    public List<IncidentRecord> extractBatch(int offset, int limit) {
        ARServerUser ctx = ThreadLocalARContext.get();
        ctx.verifyUser();

        OutputInteger totalMatches = new OutputInteger();
        List<EntryListFieldInfo> entries = ctx.getListEntryObjects(
            FieldIdConstants.Incident.FORM_NAME,
            null, // qualification
            offset,
            limit,
            null, // sortList
            FIELD_IDS,
            false, // useLocale
            totalMatches
        );

        return entries.stream()
            .map(this::mapToRecord)
            .collect(Collectors.toList());
    }
}
```

### 5.2 Query Construction

```java
// Correct: Using Field ID and epoch
String qualification = "'6' > " + lastSyncTimestamp;

// Incorrect: Using field name (NEVER DO THIS)
// String qualification = "'Last Modified Date' > \"01/01/2024\"";
```

### 5.3 Error Handling

```java
public List<T> extractWithRetry(int offset, int limit) {
    int attempts = 0;
    int currentChunkSize = limit;

    while (attempts < 5) {
        try {
            return extractBatch(offset, currentChunkSize);
        } catch (ARException e) {
            if (e.getMessageNum() == 93) { // ARERR 93
                currentChunkSize = Math.max(100, currentChunkSize / 2);
                log.warn("Reducing chunk size to {} after ARERR 93", currentChunkSize);
            } else if (e.getMessageNum() == 92) { // ARERR 92
                log.warn("RPC timeout, retrying...");
            } else {
                throw e;
            }
            attempts++;
            Thread.sleep((long) Math.pow(2, attempts) * 1000);
        }
    }
    throw new ExtractionException("Max retries exceeded");
}
```

---

## 6. Testing Strategy

### 6.1 Unit Tests
| Component | Test Class | Coverage Target |
|-----------|------------|-----------------|
| IncidentExtractor | `IncidentExtractorTest.java` | 80% |
| QualifierBuilder | `QualifierBuilderTest.java` | 90% |
| FieldIdConstants | `FieldIdConstantsTest.java` | 100% |

### 6.2 Integration Tests
| Scenario | Test Class | Environment |
|----------|------------|-------------|
| Bulk extraction | `BulkExtractionIT.java` | Test Remedy server |
| Incremental sync | `IncrementalSyncIT.java` | Test Remedy server |
| Error recovery | `ErrorRecoveryIT.java` | Mock server |

### 6.3 Performance Tests
| Metric | Test Method | Target |
|--------|-------------|--------|
| Extraction rate | Load test | 7+ records/sec |
| Memory usage | Profiling | <2GB |
| Sync time | Timing | <15 min for 1000 records |

---

## 7. Rollout Plan

### 7.1 Feature Flags
- `remedy.extraction.enabled` - Enable extraction (default: true)
- `remedy.sync.enabled` - Enable scheduled sync (default: true)
- `remedy.attachments.enabled` - Process attachments (default: true)

### 7.2 Migration Steps
1. Deploy extractor service
2. Run initial bulk extraction
3. Verify record counts match Remedy
4. Enable scheduled sync
5. Monitor for errors

### 7.3 Rollback Plan
1. Disable sync scheduler
2. Clear embedding tables if corrupted
3. Re-run bulk extraction from scratch

---

## 8. Risks and Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Remedy server overload | Medium | High | Rate limiting, off-hours extraction |
| License exhaustion | Low | High | Use Fixed license, monitor usage |
| Network instability | Medium | Medium | Retry logic, checkpointing |
| Schema changes after upgrade | Low | Medium | Field ID stability, version check |

---

## 9. Monitoring and Observability

### 9.1 Metrics
| Metric | Type | Alert Threshold |
|--------|------|-----------------|
| `remedy.extraction.records` | Counter | N/A |
| `remedy.extraction.errors` | Counter | >10/hour |
| `remedy.extraction.latency` | Histogram | p99 >30s per batch |
| `remedy.sync.lag` | Gauge | >1 hour |

### 9.2 Logs
| Log Event | Level | Context |
|-----------|-------|---------|
| Batch extracted | INFO | record count, duration |
| ARERR received | WARN | error number, message |
| Sync completed | INFO | records processed, duration |
| Attachment skipped | WARN | size, reason |

---

## 10. Sign-Off

| Role | Name | Date | Approved |
|------|------|------|----------|
| Tech Lead | | | [ ] |
| Architect | | | [ ] |
| DBA | | | [ ] |
