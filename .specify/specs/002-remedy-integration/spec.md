# Feature Specification: BMC Remedy Integration

**Spec ID**: 002-remedy-integration
**Status**: Approved
**Owner**: BMC RAG Team
**Created**: 2024-01-15
**Last Updated**: 2024-01-15

---

## 1. Overview

### 1.1 Problem Statement
The RAG agent requires access to ITSM data stored in BMC Remedy AR System. Data must be extracted, transformed, and vectorized while respecting Remedy's unique API patterns and constraints.

### 1.2 Goals
- Extract ITSM records from Remedy (Incidents, Changes, Work Orders, Knowledge Articles)
- Support both bulk (initial) and incremental (CDC) extraction
- Handle attachments and work logs with proper joins
- Maintain data consistency during synchronization

### 1.3 Non-Goals
- Write operations back to Remedy (read-only integration)
- Real-time event streaming (polling-based CDC)
- Supporting Remedy versions prior to 9.x

---

## 2. User Stories

### 2.1 Primary User Stories

#### US-001: Bulk Extraction
**As a** system administrator
**I want to** perform initial bulk extraction of all ITSM records
**So that** the RAG agent has a complete knowledge base to search

**Acceptance Criteria**:
- [ ] Extract 100,000+ incidents in under 4 hours
- [ ] Handle server-side pagination correctly
- [ ] Resume extraction from checkpoint on failure
- [ ] No ARERR 93 (timeout) errors with default settings

#### US-002: Incremental Sync (CDC)
**As a** system administrator
**I want to** sync only modified records since last extraction
**So that** the knowledge base stays current without full re-extraction

**Acceptance Criteria**:
- [ ] Query uses `Last Modified Date > $LAST_SYNC_TIMESTAMP`
- [ ] Sync state persisted across restarts
- [ ] Modified records re-vectorized (delete + insert)
- [ ] Sync completes in under 15 minutes for typical daily changes

#### US-003: Work Log Extraction
**As a** system administrator
**I want to** extract work logs associated with parent records
**So that** resolution details and troubleshooting steps are searchable

**Acceptance Criteria**:
- [ ] Work logs joined to parent records at application level
- [ ] Batch fetching to avoid N+1 queries
- [ ] Work log submitter and timestamp preserved
- [ ] Work log attachments processed

#### US-004: Attachment Processing
**As a** system administrator
**I want to** extract and process document attachments
**So that** PDF/Word content is searchable

**Acceptance Criteria**:
- [ ] Binary content retrieved via `getEntryBlob()`
- [ ] Text extraction using Apache Tika
- [ ] Supported formats: PDF, DOCX, DOC, TXT
- [ ] Max attachment size: 10MB

---

## 3. Requirements

### 3.1 Functional Requirements

| ID | Requirement | Priority | Notes |
|----|-------------|----------|-------|
| FR-001 | Extract Incident records from HPD:Help Desk | P0 | Core functionality |
| FR-002 | Extract Change records from CHG:Infrastructure Change | P0 | |
| FR-003 | Extract Work Order records from WOI:WorkOrder | P1 | |
| FR-004 | Extract Knowledge Articles from RKM:KnowledgeArticleManager | P0 | High-value content |
| FR-005 | Extract Work Logs (HPD:WorkLog, CHG:WorkLog, WOI:WorkInfo) | P0 | Resolution details |
| FR-006 | Process attachments with Tika | P1 | |
| FR-007 | Incremental sync using Last Modified Date | P0 | |
| FR-008 | Checkpoint/resume for bulk extraction | P1 | |
| FR-009 | Connection pooling with ThreadLocal pattern | P0 | Constitution requirement |

### 3.2 Non-Functional Requirements

| ID | Requirement | Target | Notes |
|----|-------------|--------|-------|
| NFR-001 | Bulk extraction throughput | 100k records / 4 hours | ~7 records/sec |
| NFR-002 | Incremental sync time | <15 minutes | For ~1000 modified records |
| NFR-003 | Memory usage | <2GB | Per extraction thread |
| NFR-004 | Connection timeout | 60 seconds | Socket timeout |
| NFR-005 | Chunk size | 500 records | Per query batch |

---

## 4. Constitution Check

| Principle | Compliant | Notes |
|-----------|-----------|-------|
| I. Air-Gap Mandate | Yes | Direct RPC connection, no cloud |
| II. Field ID Supremacy | Yes | All queries use FieldIdConstants |
| III. Thread-Safe Connections | Yes | ThreadLocalARContext pattern |
| IV. Security (ReBAC) | Yes | assigned_group stored in metadata |
| V. Semantic Chunking | Yes | Resolution as standalone chunk |
| VI. Date Handling | Yes | Unix epoch for all date comparisons |

---

## 5. Field ID Reference

### 5.1 Common Fields

| Field | ID | Type | Notes |
|-------|-----|------|-------|
| Entry ID | 1 | Character | Unique record ID |
| Create Date | 3 | Integer | Unix epoch |
| Last Modified Date | 6 | Integer | Unix epoch, CDC cursor |
| Status | 7 | Enum | Record status |

### 5.2 Incident Fields (HPD:Help Desk)

| Field | ID | Type | Notes |
|-------|-----|------|-------|
| Incident Number | 1000000161 | Character | e.g., INC000000001 |
| Summary | 1000000000 | Character | Short description |
| Description | 1000000151 | Character | Full description |
| Resolution | 1000000156 | Character | **High-value chunk** |
| Assigned Group | 1000000217 | Character | ReBAC filter |
| Urgency | 1000000162 | Enum | 1-4 |
| Impact | 1000000163 | Enum | 1-4 |
| Priority | 1000000164 | Enum | Calculated |
| Category Tier 1 | 1000000063 | Character | Classification |
| Category Tier 2 | 1000000064 | Character | Classification |
| Category Tier 3 | 1000000065 | Character | Classification |

### 5.3 Change Request Fields (CHG:Infrastructure Change)

| Field | ID | Type | Notes |
|-------|-----|------|-------|
| Change ID | 1000000182 | Character | e.g., CHG000001 |
| Summary | 1000000000 | Character | Short description |
| Description | 1000000151 | Character | Full description |
| Reason for Change | 1000000153 | Character | Business justification |
| Implementation Plan | 1000000885 | Character | **High-value chunk** |
| Rollback Plan | 1000000886 | Character | **High-value chunk** |
| Risk Level | 1000000180 | Enum | 1-5 |
| Assigned Group | 1000000217 | Character | ReBAC filter |

### 5.4 Work Order Fields (WOI:WorkOrder)

| Field | ID | Type | Notes |
|-------|-----|------|-------|
| Work Order ID | 1000000182 | Character | e.g., WO0000001 |
| Summary | 1000000000 | Character | Short description |
| Description | 1000000151 | Character | Full description |
| Assigned Group | 1000000217 | Character | ReBAC filter |
| Priority | 1000000164 | Enum | 1-4 |

### 5.5 Knowledge Article Fields (RKM:KnowledgeArticleManager)

| Field | ID | Type | Notes |
|-------|-----|------|-------|
| Article ID | 302300500 | Character | Doc ID |
| Title | 302300502 | Character | Article title |
| Content | 302311200 | Character | **High-value chunk** |
| Summary | 302300507 | Character | Abstract |
| Keywords | 302300510 | Character | Search keywords |
| Assigned Group | 1000000217 | Character | ReBAC filter |
| View Count | 302300540 | Integer | Popularity metric |

---

## 6. Error Handling

### 6.1 Remedy Error Codes

| Error | Code | Cause | Mitigation |
|-------|------|-------|------------|
| ARERR 92 | 92 | Network RPC timeout | Increase setSocketTimeOut() |
| ARERR 93 | 93 | Server query timeout | Reduce chunk size to 100-500 |
| ARERR 88 | 88 | Authentication failure | Verify credentials |
| ARERR 91 | 91 | License not available | Use Fixed license type |

### 6.2 Retry Strategy

```
Attempt 1: Immediate
Attempt 2: Wait 1 second
Attempt 3: Wait 2 seconds
Attempt 4: Wait 4 seconds
Attempt 5: Fail with error
```

On ARERR 93:
- Reduce chunk size by 50%
- Retry with smaller batch
- Log warning for admin review

---

## 7. Data Flow

### 7.1 Bulk Extraction

```
RemedyConnector
    |
    v
ARServerUser (ThreadLocal)
    |
    v
getListEntryObjects() with pagination
    |
    v
IncidentExtractor / ChangeExtractor / etc.
    |
    v
IncidentRecord / ChangeRequestRecord / etc.
    |
    v
Work Log Extractor (batch fetch by parent IDs)
    |
    v
Attachment Extractor (getEntryBlob)
    |
    v
SemanticChunker (IncidentChunkStrategy, etc.)
    |
    v
EmbeddingEngine (ONNX)
    |
    v
VectorStore (pgvector)
```

### 7.2 Incremental Sync

```
SyncScheduler (every 15 minutes)
    |
    v
Load LAST_SYNC_TIMESTAMP from DB
    |
    v
Query: 'Last Modified Date' > $LAST_SYNC_TIMESTAMP
    |
    v
For each modified record:
    1. Delete existing embeddings (by source_id)
    2. Re-extract record
    3. Re-chunk and re-embed
    4. Insert new embeddings
    |
    v
Update LAST_SYNC_TIMESTAMP
```

---

## 8. Success Criteria

| Metric | Target | Measurement Method |
|--------|--------|-------------------|
| Bulk extraction rate | 100k records / 4 hours | Timing measurement |
| Incremental sync time | <15 minutes | Timing measurement |
| Zero ARERR 93 | 0 errors | Error log analysis |
| Data completeness | 100% | Record count comparison |
| Field accuracy | 100% | Spot check verification |

---

## 9. Open Questions

1. Should we support Remedy REST API (v20.x+) in addition to Java RPC?
   - **Decision**: Defer to v2, Java RPC has broader version support

2. How do we handle deleted records in Remedy?
   - **Decision**: Weekly reconciliation scan comparing active IDs

3. Should work logs with `View Access = Internal` be excluded?
   - **Decision**: Include all work logs, let ReBAC filter at query time

---

## 10. References

- BMC AR System Java API Documentation
- `FieldIdConstants.java` for complete field mappings
- BMC Remedy Form definitions
