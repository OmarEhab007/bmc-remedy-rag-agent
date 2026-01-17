# Feature Specification: Relationship-Based Access Control (ReBAC)

**Spec ID**: 003-security-rebac
**Status**: Approved
**Owner**: BMC RAG Team
**Created**: 2024-01-15
**Last Updated**: 2024-01-15

---

## 1. Overview

### 1.1 Problem Statement
ITSM data contains sensitive information that must be protected based on organizational access policies. Users should only see search results for records assigned to their support groups, preventing unauthorized data exposure.

### 1.2 Goals
- Implement group-based filtering at vector retrieval time
- Ensure zero unauthorized data exposure
- Maintain low latency overhead (<10ms)
- Support public content (no group restriction)
- Prioritize knowledge articles for broader visibility

### 1.3 Non-Goals
- Fine-grained field-level access control
- Dynamic permission changes (requires re-sync)
- Integration with external identity providers (uses JWT claims)

---

## 2. User Stories

### 2.1 Primary User Stories

#### US-001: Group-Based Filtering
**As a** IT support analyst
**I want to** see only records assigned to my support groups
**So that** I don't access data I'm not authorized to see

**Acceptance Criteria**:
- [ ] Search results filtered by user's group memberships
- [ ] Group comparison is case-insensitive
- [ ] Filter applied before results returned to user
- [ ] Filter latency under 10ms for 1000 results

#### US-002: Public Content Access
**As a** IT support analyst
**I want to** see knowledge articles and records without group restrictions
**So that** I can access broadly applicable information

**Acceptance Criteria**:
- [ ] Records without `assigned_group` visible to all users
- [ ] Knowledge articles with null group visible to all
- [ ] Public content clearly identified in results

#### US-003: Knowledge Article Prioritization
**As a** IT support analyst
**I want to** see knowledge articles prioritized in results
**So that** I get authoritative answers first

**Acceptance Criteria**:
- [ ] Knowledge articles sorted before other record types
- [ ] Prioritization configurable per request
- [ ] Original relevance scores preserved

#### US-004: High-Value Chunk Prioritization
**As a** IT support analyst
**I want to** see resolutions and implementation plans prioritized
**So that** I get actionable information first

**Acceptance Criteria**:
- [ ] RESOLUTION, IMPLEMENTATION, ROLLBACK chunks prioritized
- [ ] ARTICLE_CONTENT chunks prioritized
- [ ] Prioritization applied after ReBAC filter

---

## 3. Requirements

### 3.1 Functional Requirements

| ID | Requirement | Priority | Notes |
|----|-------------|----------|-------|
| FR-001 | Filter results by user's group memberships | P0 | Core security |
| FR-002 | Allow public content (null assigned_group) | P0 | |
| FR-003 | Case-insensitive group comparison | P0 | |
| FR-004 | Prioritize knowledge articles | P1 | Configurable |
| FR-005 | Prioritize high-value chunk types | P1 | |
| FR-006 | Deduplicate results by source record | P2 | |
| FR-007 | Store assigned_group in vector metadata | P0 | Ingestion requirement |
| FR-008 | Extract user groups from JWT claims | P0 | |

### 3.2 Non-Functional Requirements

| ID | Requirement | Target | Notes |
|----|-------------|--------|-------|
| NFR-001 | Filter latency | <10ms | For 1000 results |
| NFR-002 | Zero unauthorized access | 100% | Security critical |
| NFR-003 | Memory overhead | <50MB | Per filter operation |
| NFR-004 | Concurrency | 100 concurrent | Stateless filter |

---

## 4. Constitution Check

| Principle | Compliant | Notes |
|-----------|-----------|-------|
| I. Air-Gap Mandate | Yes | No external auth calls |
| II. Field ID Supremacy | Yes | Uses Field ID 1000000217 for assigned_group |
| III. Thread-Safe Connections | N/A | Filter is stateless |
| IV. Security (ReBAC) | Yes | This IS the ReBAC implementation |
| V. Semantic Chunking | N/A | Uses chunk metadata |
| VI. Date Handling | N/A | No date operations |

---

## 5. Data Flow

### 5.1 Ingestion Flow (Store Group in Metadata)

```
Remedy Record
    |
    v
Extract assigned_group (Field ID 1000000217)
    |
    v
Include in chunk metadata:
{
  "source_type": "INCIDENT",
  "source_id": "INC000001234",
  "chunk_type": "RESOLUTION",
  "assigned_group": "ServiceDesk"  <-- Key field
}
    |
    v
Store in pgvector with JSONB metadata
```

### 5.2 Query Flow (Filter by Group)

```
User Query
    |
    v
Extract userGroups from JWT claims
    |
    v
Vector Search (returns all matching results)
    |
    v
ReBACFilter.filterByGroups(results, userGroups)
    |
    +--> For each result:
    |    - Get assigned_group from metadata
    |    - If null/empty: ALLOW (public content)
    |    - If in userGroups (case-insensitive): ALLOW
    |    - Otherwise: DENY
    |
    v
Filtered Results
    |
    v
Optional: Prioritize knowledge articles
    |
    v
Optional: Prioritize high-value chunks
    |
    v
Return to user
```

---

## 6. Security Considerations

### 6.1 Authentication
- JWT tokens validated at API gateway
- User ID extracted from `sub` claim
- Group memberships from `groups` claim

### 6.2 Authorization Flow
```
1. User authenticates (external IdP)
2. JWT issued with groups claim
3. API gateway validates JWT signature
4. ChatController extracts userGroups
5. ReBACFilter applies group filter
6. Only authorized results returned
```

### 6.3 Defense in Depth
- Filter applied at retrieval layer (before LLM)
- Filter applied even if context is cached
- No group information in error messages
- Audit logging for access patterns

### 6.4 Edge Cases
| Scenario | Behavior |
|----------|----------|
| Empty userGroups | Only public content returned |
| Missing groups claim | Treated as empty groups |
| Invalid JWT | Request rejected (401) |
| Null assigned_group | Visible to all users |
| Group case mismatch | Case-insensitive comparison |

---

## 7. API Contract

### 7.1 Request with Groups

```json
{
  "userId": "user123",
  "userGroups": ["ServiceDesk", "NetworkOps", "Level2Support"],
  "text": "How do I reset a password?",
  "sessionId": "uuid"
}
```

### 7.2 JWT Claims

```json
{
  "sub": "user123",
  "email": "user@company.com",
  "groups": ["ServiceDesk", "NetworkOps", "Level2Support"],
  "iat": 1705334400,
  "exp": 1705420800
}
```

### 7.3 Metadata Schema

```sql
CREATE TABLE embedding_store (
    id UUID PRIMARY KEY,
    embedding vector(384),
    text_segment TEXT,
    metadata JSONB
);

-- Example metadata
{
    "source_type": "INCIDENT",
    "source_id": "INC000001234",
    "chunk_type": "RESOLUTION",
    "assigned_group": "ServiceDesk",
    "category": "Password Reset",
    "created_date": 1705248000
}

-- Index for group filtering
CREATE INDEX idx_metadata_group ON embedding_store
    USING gin ((metadata->'assigned_group'));
```

---

## 8. Filter Implementation

### 8.1 Core Filter Logic

```java
public List<SearchResult> filterByGroups(
        List<SearchResult> results,
        Set<String> userGroups) {

    if (results == null || results.isEmpty()) {
        return Collections.emptyList();
    }

    // No groups = only public content
    if (userGroups == null || userGroups.isEmpty()) {
        return results.stream()
            .filter(r -> {
                String group = r.getMetadata().get("assigned_group");
                return group == null || group.isEmpty();
            })
            .collect(Collectors.toList());
    }

    // Case-insensitive group matching
    Set<String> groupsLower = userGroups.stream()
        .map(String::toLowerCase)
        .collect(Collectors.toSet());

    return results.stream()
        .filter(r -> {
            String group = r.getMetadata().get("assigned_group");
            return group == null ||
                   group.isEmpty() ||
                   groupsLower.contains(group.toLowerCase());
        })
        .collect(Collectors.toList());
}
```

### 8.2 Prioritization Logic

```java
public List<SearchResult> filterAndPrioritize(
        List<SearchResult> results,
        Set<String> userGroups,
        boolean prioritizeKnowledge) {

    List<SearchResult> filtered = filterByGroups(results, userGroups);

    if (!prioritizeKnowledge) {
        return filtered;
    }

    // Separate knowledge articles
    List<SearchResult> knowledge = new ArrayList<>();
    List<SearchResult> other = new ArrayList<>();

    for (SearchResult r : filtered) {
        if ("KnowledgeArticle".equals(r.getSourceType())) {
            knowledge.add(r);
        } else {
            other.add(r);
        }
    }

    // Knowledge first, then others
    List<SearchResult> prioritized = new ArrayList<>();
    prioritized.addAll(knowledge);
    prioritized.addAll(other);
    return prioritized;
}
```

---

## 9. Success Criteria

| Metric | Target | Measurement Method |
|--------|--------|-------------------|
| Zero unauthorized access | 100% | Security testing, audit |
| Filter latency | <10ms | Performance profiling |
| Public content access | Works correctly | Functional testing |
| Case sensitivity | Case-insensitive | Unit tests |
| Concurrent operations | 100+ | Load testing |

---

## 10. Test Scenarios

### 10.1 Security Tests
| Test | Expected Result |
|------|-----------------|
| User in matching group | Results returned |
| User NOT in matching group | Results filtered out |
| User with no groups | Only public results |
| Public content (null group) | Visible to all |
| Group case mismatch | Still matches |
| Empty results | Empty list (no error) |

### 10.2 Prioritization Tests
| Test | Expected Result |
|------|-----------------|
| Knowledge prioritization ON | KA before incidents |
| Knowledge prioritization OFF | Original order |
| High-value chunks | RESOLUTION first |
| Deduplication | One result per source |

---

## 11. Open Questions

1. Should we support hierarchical groups (group inheritance)?
   - **Decision**: Defer to v2, flat groups for v1

2. How do we handle group membership changes?
   - **Decision**: Changes reflected on next query (JWT refresh)

3. Should we log filtered-out results for audit?
   - **Decision**: No, only log access attempts and counts

---

## 12. References

- JWT RFC 7519
- OWASP Access Control Cheat Sheet
- BMC Remedy Group Management
- `ReBACFilter.java` implementation
