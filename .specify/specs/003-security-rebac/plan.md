# Implementation Plan: Relationship-Based Access Control (ReBAC)

**Spec Reference**: 003-security-rebac
**Status**: ✅ Complete
**Author**: BMC RAG Team
**Created**: 2024-01-15

---

## 1. Technical Context

### 1.1 Current State
The ReBACFilter component is implemented with the following capabilities:
- Group-based filtering
- Knowledge article prioritization
- High-value chunk prioritization
- Deduplication by source

### 1.2 Affected Components

| Component | Path | Change Type |
|-----------|------|-------------|
| ReBACFilter | `rag-service/src/.../security/ReBACFilter.java` | Implemented |
| SecureContentRetriever | `rag-service/src/.../retrieval/SecureContentRetriever.java` | Uses ReBACFilter |
| EmbeddingEntity | `vector-store/src/.../entity/EmbeddingEntity.java` | Has metadata field |
| ChatQueryMessage | `api-gateway/src/.../dto/ChatQueryMessage.java` | Has userGroups field |

### 1.3 Dependencies
- Spring Security for JWT validation
- pgvector JSONB for metadata storage
- LangChain4j retriever integration

---

## 2. Constitution Compliance

| Principle | Implementation Approach |
|-----------|------------------------|
| I. Air-Gap Mandate | JWT validation local, no external IdP calls |
| II. Field ID Supremacy | Field ID 1000000217 used for assigned_group extraction |
| III. Thread-Safe Connections | Filter is stateless, thread-safe |
| IV. Security (ReBAC) | Core implementation of this principle |
| V. Semantic Chunking | Uses chunk_type metadata for prioritization |
| VI. Date Handling | N/A |

---

## 3. Architecture Decisions

### 3.1 Decision: Filter Location
**Context**: Where in the pipeline should filtering occur?
**Options Considered**:
1. Database level (WHERE clause) - Efficient but complex SQL
2. Application level (post-retrieval) - Simple, flexible
3. Hybrid - Database pre-filter, app post-filter

**Decision**: Application level. Vector search is already efficient, and application filter provides flexibility for prioritization logic.

### 3.2 Decision: Group Comparison
**Context**: How to handle case sensitivity in group names?
**Options Considered**:
1. Case-sensitive - Simple, but error-prone
2. Case-insensitive - More forgiving

**Decision**: Case-insensitive. Group names may vary in case across systems.

### 3.3 Decision: Empty Groups Handling
**Context**: What happens when user has no groups?
**Options Considered**:
1. Return nothing - Strictest
2. Return public only - Balanced

**Decision**: Return public content only. Allows basic access while protecting sensitive data.

---

## 4. Implementation Phases

### Phase 1: Core Filter ✅ COMPLETE
**Goal**: Basic group-based filtering
**Status**: Complete
**Implemented in**: `rag-service/src/main/java/com/bmc/rag/agent/security/ReBACFilter.java`

#### Tasks
- [x] Implement ReBACFilter.filterByGroups() with case-insensitive matching
- [x] Add case-insensitive comparison via toLowerCase() normalization
- [x] Handle null/empty groups (returns public content only)
- [x] Handle null/empty assigned_group (public content visible to all)
- [x] Unit tests for filter logic in ReBACFilterTest.java

#### Deliverables
- Working ReBACFilter component ✅
- Comprehensive test coverage ✅

### Phase 2: Prioritization ✅ COMPLETE
**Goal**: Knowledge article and high-value chunk prioritization
**Status**: Complete
**Implemented in**: `rag-service/src/main/java/com/bmc/rag/agent/security/ReBACFilter.java`

#### Tasks
- [x] Implement filterAndPrioritize() - knowledge articles first
- [x] Implement prioritizeHighValueChunks() - RESOLUTION, IMPLEMENTATION, ROLLBACK, ARTICLE_CONTENT
- [x] Implement deduplicateBySource() - keeps highest scoring result per source
- [x] Implement applyAllFilters() convenience method
- [x] Unit tests for prioritization

#### Deliverables
- Prioritization methods ✅
- Configurable prioritization ✅

### Phase 3: Integration ✅ COMPLETE
**Goal**: Wire filter into retrieval pipeline
**Status**: Complete
**Implemented in**: `rag-service/src/main/java/com/bmc/rag/agent/retrieval/SecureContentRetriever.java`

#### Tasks
- [x] Integrate ReBACFilter with SecureContentRetriever
- [x] Extract userGroups from ChatQueryMessage and ChatRequest
- [x] Pass groups through pipeline to ChatController and WebSocketChatController
- [x] Integration tests in SecureContentRetrieverTest.java

#### Deliverables
- End-to-end filtering ✅
- Integration test suite ✅

### Phase 4: Metadata Schema ✅ COMPLETE
**Goal**: Ensure metadata stored correctly at ingestion
**Status**: Complete
**Implemented in**:
- `vector-store/src/main/resources/db/migration/V1__create_embedding_tables.sql`
- `vectorization-engine/src/main/java/com/bmc/rag/vectorization/chunking/`

#### Tasks
- [x] Add assigned_group to chunk metadata (extracted from Field ID 1000000217)
- [x] Create JSONB metadata column in embedding_store table
- [x] Verify ingestion pipeline stores group via chunk strategies
- [x] Test with sample data

#### Deliverables
- Indexed metadata ✅
- Verified ingestion ✅

---

## 5. Technical Design

### 5.1 Class Design

```java
@Component
public class ReBACFilter {

    /**
     * Filter by user groups. Public content (null group) always allowed.
     */
    public List<SearchResult> filterByGroups(
            List<SearchResult> results,
            Set<String> userGroups);

    /**
     * Filter and optionally prioritize knowledge articles.
     */
    public List<SearchResult> filterAndPrioritize(
            List<SearchResult> results,
            Set<String> userGroups,
            boolean prioritizeKnowledgeArticles);

    /**
     * Prioritize high-value chunk types.
     */
    public List<SearchResult> prioritizeHighValueChunks(
            List<SearchResult> results);

    /**
     * Deduplicate results by source record.
     */
    public List<SearchResult> deduplicateBySource(
            List<SearchResult> results);

    /**
     * Apply all filters and prioritization.
     */
    public List<SearchResult> applyAllFilters(
            List<SearchResult> results,
            Set<String> userGroups,
            boolean prioritizeKnowledge,
            boolean deduplicate);
}
```

### 5.2 Integration Point

```java
@Component
public class SecureContentRetriever {

    private final VectorStoreService vectorStore;
    private final ReBACFilter rebacFilter;

    public List<SearchResult> retrieve(
            String query,
            Set<String> userGroups,
            int maxResults) {

        // Get more results to account for filtering
        int fetchSize = maxResults * 3;

        List<SearchResult> raw = vectorStore.search(query, fetchSize);

        // Apply ReBAC filter
        List<SearchResult> filtered = rebacFilter.applyAllFilters(
            raw,
            userGroups,
            true,  // prioritize knowledge
            true   // deduplicate
        );

        // Limit to requested size
        return filtered.stream()
            .limit(maxResults)
            .collect(Collectors.toList());
    }
}
```

### 5.3 Metadata Storage

```java
// During ingestion
Map<String, String> metadata = new HashMap<>();
metadata.put("source_type", "INCIDENT");
metadata.put("source_id", incident.getIncidentNumber());
metadata.put("chunk_type", "RESOLUTION");
metadata.put("assigned_group", incident.getAssignedGroup()); // Field ID 1000000217
metadata.put("category", incident.getCategory());

EmbeddingEntity entity = EmbeddingEntity.builder()
    .id(UUID.randomUUID())
    .embedding(embeddingVector)
    .textSegment(chunkText)
    .metadata(metadata)
    .build();
```

---

## 6. Testing Strategy

### 6.1 Unit Tests
| Component | Test Class | Coverage Target |
|-----------|------------|-----------------|
| ReBACFilter | `ReBACFilterTest.java` | 95% |
| SecureContentRetriever | `SecureContentRetrieverTest.java` | 85% |

### 6.2 Security Tests
| Scenario | Test | Expected |
|----------|------|----------|
| Authorized access | User in group | Results returned |
| Unauthorized access | User NOT in group | Results filtered |
| No groups | Empty userGroups | Public only |
| Public content | Null assigned_group | Visible to all |
| Case mismatch | "ServiceDesk" vs "servicedesk" | Match found |

### 6.3 Integration Tests
| Scenario | Test Class | Environment |
|----------|------------|-------------|
| End-to-end filter | `ReBACIntegrationTest.java` | Docker Compose |
| JWT extraction | `JwtExtractionTest.java` | Mock server |

---

## 7. Rollout Plan

### 7.1 Feature Flags
- `security.rebac.enabled` - Enable ReBAC filtering (default: true)
- `security.rebac.prioritize-knowledge` - Prioritize KA (default: true)
- `security.rebac.deduplicate` - Deduplicate results (default: true)

### 7.2 Migration Steps
1. Deploy with ReBAC enabled
2. Verify filtering in staging
3. Run security audit
4. Enable in production
5. Monitor access patterns

### 7.3 Rollback Plan
1. Disable ReBAC (returns all results - security risk!)
2. Investigate root cause
3. Fix and re-enable
4. Never leave disabled in production

---

## 8. Risks and Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Filter bypass | Low | Critical | Code review, security testing |
| Performance degradation | Low | Medium | Profiling, caching |
| Group sync issues | Medium | Medium | Clear documentation |
| Over-filtering | Low | Medium | Test coverage |

---

## 9. Monitoring and Observability

### 9.1 Metrics
| Metric | Type | Alert Threshold |
|--------|------|-----------------|
| `rebac.filter.latency` | Histogram | p99 >50ms |
| `rebac.results.filtered` | Counter | N/A (audit) |
| `rebac.results.total` | Counter | N/A (audit) |
| `rebac.errors` | Counter | >0 |

### 9.2 Logs
| Log Event | Level | Context |
|-----------|-------|---------|
| Filter applied | DEBUG | userId, groupCount, resultsBefore, resultsAfter |
| Zero results after filter | INFO | userId, query (no groups logged) |
| Filter error | ERROR | error message |

### 9.3 Audit Trail
```json
{
  "event": "VECTOR_SEARCH",
  "timestamp": "2024-01-15T10:30:00Z",
  "userId": "user123",
  "groupCount": 3,
  "resultsBefore": 50,
  "resultsAfter": 12,
  "latencyMs": 8
}
```

---

## 10. Security Verification

### 10.1 Pre-Deployment Checklist
- [x] All unit tests passing
- [x] Integration tests passing
- [x] Security review complete
- [x] No group information in logs (only groupCount logged)
- [x] Error messages reveal nothing sensitive
- [x] Audit logging enabled (DEBUG level)
- [x] Filter cannot be bypassed

### 10.2 Post-Deployment Verification
- [x] Query with authorized groups returns expected results
- [x] Query with unauthorized groups returns nothing
- [x] Public content accessible to all
- [x] Latency within SLA (<10ms filter overhead)
- [x] Audit logs capturing events

---

## 11. Sign-Off

| Role | Name | Date | Approved |
|------|------|------|----------|
| Tech Lead | | | [x] |
| Security Lead | | | [x] |
| Architect | | | [x] |
