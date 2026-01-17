# BMC Remedy RAG Agent Constitution

The following principles govern all development on this project. Violations require explicit approval and documented justification.

---

## I. Air-Gap Mandate

**All components must operate in a fully air-gapped, on-premise environment.**

- No external cloud dependencies (AWS, Azure, GCP, OpenAI, etc.)
- No outbound network calls for inference, embeddings, or storage
- Local models only: Ollama (Llama 3 8B or Mistral 7B) for LLM, ONNX runtime for embeddings
- PostgreSQL with pgvector for all vector storage

**Rationale**: Enterprise security requirements mandate complete network isolation.

---

## II. Field ID Supremacy

**Always use Field IDs (integers), never Field Names for BMC Remedy API calls.**

- Field IDs are immutable across localization and upgrades
- Field Names change based on language packs and customizations
- Reference `FieldIdConstants.java` for all field mappings

**Example**:
```java
// CORRECT
int incidentNumber = entry.get(FieldIdConstants.Incident.INCIDENT_NUMBER);

// INCORRECT - will break on localized servers
String incidentNumber = entry.get("Incident Number");
```

---

## III. Thread-Safe Connections

**`ARServerUser` is NOT thread-safe. Always use ThreadLocal pattern.**

- Never share `ARServerUser` instances across threads
- Use `ThreadLocalARContext` for connection management
- Use "Fixed" license type for background processes (not Floating)
- Always call `verifyUser()` before data operations

**Connection Template**:
```java
ThreadLocalARContext.get().verifyUser();
// perform operations
ThreadLocalARContext.clear(); // cleanup in finally block
```

---

## IV. Security at the Source (ReBAC)

**Relationship-Based Access Control must filter at vector retrieval time.**

- Store `assigned_group` in vector metadata during ingestion
- Query user's group memberships at runtime
- Filter vector searches using `metadata.assigned_group IN (user_groups)`
- Content without `assigned_group` is considered public

**Never** return data the requesting user is not authorized to see.

---

## V. Semantic Chunking Strategy

**High-value content must be preserved and contextualized.**

1. **Resolution fields** (Field ID 1000000156): Always treat as standalone high-value chunk
2. **Implementation/Rollback plans**: Standalone high-value chunks
3. **Work logs**: Group by submitter or timestamp
4. **All chunks**: Inject parent record Summary into metadata for context

**Chunk Priority Order**:
1. RESOLUTION
2. IMPLEMENTATION
3. ROLLBACK
4. ARTICLE_CONTENT
5. Standard content

---

## VI. Date Handling

**Remedy stores dates as Unix epoch integers (seconds since 1970-01-01).**

- **Correct**: `'Last Modified Date' > 1672531200`
- **Incorrect**: `'Last Modified Date' > "01/01/2023"`

Always use `FieldIdConstants.LAST_MODIFIED_DATE` (Field ID 6) for CDC queries.

---

## VII. Error Handling Patterns

**Handle Remedy-specific errors with appropriate retry strategies.**

| Error | Cause | Solution |
|-------|-------|----------|
| ARERR 93 | Server-side query timeout | Reduce chunk size (100-500 records) |
| ARERR 92 | Network RPC timeout | Increase `setSocketTimeOut()` |

- Default chunk size: 500 records (not 1000)
- Default socket timeout: 60000ms
- Retry with exponential backoff for transient errors

---

## VIII. Pagination Requirements

**Use proper pagination to avoid memory issues and timeouts.**

- Use `getListEntryObjects()` instead of `getListEntry()` to avoid N+1 queries
- Implement pagination with `firstRetrieve`/`maxRetrieve` parameters
- Server limit is typically 2000 records per query
- Safe chunk size: 100-500 records

---

## Constitution Checklist

Before implementing any feature, verify:

- [ ] No external cloud dependencies introduced
- [ ] All Remedy field references use Field IDs
- [ ] Connection management follows ThreadLocal pattern
- [ ] ReBAC filtering implemented for user-facing queries
- [ ] Chunking strategy preserves high-value content
- [ ] Date comparisons use Unix epoch integers
- [ ] Error handling includes retry strategies
- [ ] Pagination implemented for bulk operations
