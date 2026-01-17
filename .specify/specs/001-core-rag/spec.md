# Feature Specification: Core RAG Functionality

**Spec ID**: 001-core-rag
**Status**: Approved
**Owner**: BMC RAG Team
**Created**: 2024-01-15
**Last Updated**: 2024-01-15

---

## 1. Overview

### 1.1 Problem Statement
IT support analysts spend significant time searching through historical incidents, knowledge articles, and change records to find relevant solutions. Manual search across multiple systems is inefficient and often misses relevant context from past resolutions.

### 1.2 Goals
- Enable semantic search across ITSM data (incidents, changes, work orders, knowledge articles)
- Provide conversational AI interface for natural language queries
- Maintain conversation context for multi-turn interactions
- Enforce citation of sources in all responses

### 1.3 Non-Goals
- Real-time incident creation or modification
- Integration with ticketing workflows (future spec)
- Multi-language support (English only for v1)

---

## 2. User Stories

### 2.1 Primary User Stories

#### US-001: Semantic Search
**As a** IT support analyst
**I want to** search for similar past incidents using natural language
**So that** I can quickly find relevant resolutions and reduce ticket handling time

**Acceptance Criteria**:
- [ ] Search returns results ranked by semantic similarity
- [ ] Results include source type, ID, title, and relevance score
- [ ] Results respect ReBAC access controls
- [ ] Search latency is under 500ms for 95th percentile

#### US-002: Conversational Chat
**As a** IT support analyst
**I want to** ask follow-up questions in a conversation
**So that** I can refine my search without repeating context

**Acceptance Criteria**:
- [ ] System maintains conversation history within a session
- [ ] Follow-up questions reference previous context
- [ ] Sessions persist for at least 30 minutes of inactivity
- [ ] User can start a new session explicitly

#### US-003: Citation Enforcement
**As a** IT support analyst
**I want to** see the sources for every AI response
**So that** I can verify the information and access the original records

**Acceptance Criteria**:
- [ ] Every response includes at least one source citation
- [ ] Citations include source type, record ID, and title
- [ ] Citations include relevance score (0.0-1.0)
- [ ] User can access original record from citation

#### US-004: Streaming Responses
**As a** IT support analyst
**I want to** see response text as it's generated
**So that** I get faster perceived response time

**Acceptance Criteria**:
- [ ] Response tokens stream via WebSocket
- [ ] UI renders tokens as they arrive
- [ ] Final message includes complete citations
- [ ] Error states are communicated clearly

---

## 3. Requirements

### 3.1 Functional Requirements

| ID | Requirement | Priority | Notes |
|----|-------------|----------|-------|
| FR-001 | Semantic vector search across all ITSM record types | P0 | |
| FR-002 | Session-based conversation memory | P0 | |
| FR-003 | WebSocket streaming for responses | P0 | |
| FR-004 | REST API for synchronous search | P1 | |
| FR-005 | Source citation in all responses | P0 | |
| FR-006 | Knowledge article prioritization | P1 | |
| FR-007 | High-value chunk prioritization (resolutions) | P1 | |
| FR-008 | Query rewriting for context injection | P2 | |

### 3.2 Non-Functional Requirements

| ID | Requirement | Target | Notes |
|----|-------------|--------|-------|
| NFR-001 | Search latency (p95) | <500ms | Vector search + retrieval |
| NFR-002 | First token latency | <1s | Time to first streamed token |
| NFR-003 | Full response latency | <10s | For typical queries |
| NFR-004 | Concurrent sessions | 100 | Active WebSocket connections |
| NFR-005 | Session memory | 30 min | Inactivity timeout |
| NFR-006 | Embedding dimensions | 384 | all-minilm-l6-v2 |

---

## 4. Constitution Check

| Principle | Compliant | Notes |
|-----------|-----------|-------|
| I. Air-Gap Mandate | Yes | Ollama local LLM, ONNX embeddings, local PostgreSQL |
| II. Field ID Supremacy | N/A | Core RAG doesn't access Remedy directly |
| III. Thread-Safe Connections | N/A | No direct Remedy connections |
| IV. Security (ReBAC) | Yes | All queries filtered by user groups |
| V. Semantic Chunking | Yes | Respects chunk types from vectorization |
| VI. Date Handling | N/A | Dates handled by vectorization layer |

---

## 5. Data Model

### 5.1 Entities

**Session**
```
- sessionId: UUID
- userId: String
- userGroups: Set<String>
- messages: List<Message>
- createdAt: Timestamp
- lastActivityAt: Timestamp
```

**Message**
```
- messageId: UUID
- role: USER | ASSISTANT
- content: String
- citations: List<Citation>
- timestamp: Timestamp
```

**Citation**
```
- sourceType: String (INCIDENT, KNOWLEDGE, CHANGE, WORKORDER)
- sourceId: String
- title: String
- score: Double
```

### 5.2 Database Changes
Session state stored in-memory with optional Redis persistence for HA deployments.

---

## 6. API Contracts

See `contracts/api-spec.md` for detailed API specifications.

### 6.1 REST Endpoints
- `POST /api/v1/chat` - Synchronous chat request
- `POST /api/v1/chat/search` - Semantic search only

### 6.2 WebSocket Messages
- `CONNECT /ws/chat` - WebSocket connection
- `ChatQueryMessage` - Inbound query
- `ChatResponseChunk` - Outbound streaming response

---

## 7. Security Considerations

### 7.1 Authentication
- JWT-based authentication for all endpoints
- Token validation at API gateway
- User ID and groups extracted from JWT claims

### 7.2 Authorization
- ReBAC filtering on all vector search results
- User groups passed with each query
- No cross-tenant data exposure

### 7.3 Data Protection
- No PII logged in application logs
- Session data encrypted at rest (when persisted)
- WebSocket connections secured with TLS

---

## 8. Success Criteria

| Metric | Target | Measurement Method |
|--------|--------|-------------------|
| Search latency p95 | <500ms | Application metrics |
| First token latency | <1s | Client-side timing |
| Citation accuracy | 100% | Manual verification |
| Concurrent sessions | 100 | Load testing |
| Session persistence | 30 min | Functional testing |

---

## 9. Open Questions

1. Should we support cross-session history retrieval? (deferred to v2)
2. What is the maximum message length for user queries? (proposed: 4000 chars)
3. Should we implement rate limiting per user? (proposed: 10 req/min)

---

## 10. References

- LangChain4j Documentation
- Ollama API Reference
- pgvector Documentation
- WebSocket RFC 6455
