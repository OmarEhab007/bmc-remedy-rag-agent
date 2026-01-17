# Implementation Plan: Core RAG Functionality

**Spec Reference**: 001-core-rag
**Status**: ✅ Complete
**Author**: BMC RAG Team
**Created**: 2024-01-15

---

## 1. Technical Context

### 1.1 Current State
The project has the following components partially implemented:
- API Gateway with controller stubs
- Vector store with pgvector integration
- DTOs for request/response handling

### 1.2 Affected Components

| Component | Path | Change Type |
|-----------|------|-------------|
| ChatController | `api-gateway/src/main/java/.../controller/ChatController.java` | Modified |
| WebSocketChatController | `api-gateway/src/main/java/.../controller/WebSocketChatController.java` | Modified |
| RagService | `rag-service/src/main/java/.../service/RagService.java` | New |
| SessionManager | `rag-service/src/main/java/.../session/SessionManager.java` | New |
| SecureContentRetriever | `rag-service/src/main/java/.../retrieval/SecureContentRetriever.java` | Modified |

### 1.3 Dependencies
- LangChain4j for orchestration
- Ollama for LLM inference
- pgvector for vector search
- Spring WebSocket for streaming

---

## 2. Constitution Compliance

| Principle | Implementation Approach |
|-----------|------------------------|
| I. Air-Gap Mandate | OllamaChatModel with local endpoint (localhost:11434), ONNX embeddings |
| II. Field ID Supremacy | N/A - RAG layer doesn't access Remedy directly |
| III. Thread-Safe Connections | N/A - No Remedy connections |
| IV. Security (ReBAC) | ReBACFilter applied to all vector search results before LLM context |
| V. Semantic Chunking | High-value chunks (RESOLUTION, IMPLEMENTATION) prioritized in retrieval |
| VI. Date Handling | N/A - Dates handled at vectorization layer |

---

## 3. Architecture Decisions

### 3.1 Decision: Session Storage
**Context**: Need to store conversation history for multi-turn interactions
**Options Considered**:
1. In-memory ConcurrentHashMap - Simple, fast, lost on restart
2. Redis - Distributed, survives restarts, additional infra
3. PostgreSQL - Persistent, slower, no additional infra

**Decision**: In-memory with optional Redis for HA. Initial deployment uses in-memory; Redis added for clustered deployments.

### 3.2 Decision: LLM Temperature
**Context**: Need consistent, deterministic responses for IT support
**Options Considered**:
1. temperature=0.0 - Fully deterministic
2. temperature=0.3 - Slight variation

**Decision**: temperature=0.0 for deterministic responses. IT support requires consistent answers.

### 3.3 Decision: Streaming Protocol
**Context**: Need real-time token streaming
**Options Considered**:
1. Server-Sent Events (SSE) - Unidirectional, simpler
2. WebSocket - Bidirectional, more complex

**Decision**: WebSocket for bidirectional communication (user can cancel, send metadata).

---

## 4. Implementation Phases

### Phase 1: Core RAG Pipeline ✅ COMPLETE
**Goal**: Basic question-answering with citations
**Status**: Complete
**Implemented in**: `rag-service/src/main/java/com/bmc/rag/agent/service/RagAssistantService.java`

#### Tasks
- [x] Implement RagService with LangChain4j orchestration
- [x] Configure OllamaChatModel with temperature=0.0
- [x] Implement context builder with citation tracking
- [x] Create prompt templates for IT support domain
- [x] Wire SecureContentRetriever with ReBACFilter

#### Deliverables
- Working synchronous `/api/v1/chat` endpoint ✅
- Responses include source citations ✅

### Phase 2: Session Management ✅ COMPLETE
**Goal**: Multi-turn conversation support
**Status**: Complete
**Implemented in**: `RagAssistantService.java`, `PostgresChatMemoryStore.java`

#### Tasks
- [x] Implement SessionManager with Caffeine cache (24hr eviction, 10k max sessions)
- [x] Add session timeout handling (configurable)
- [x] Implement conversation memory injection via MessageWindowChatMemory
- [x] Add session creation/retrieval endpoints (`GET /sessions`, `GET /sessions/{id}/history`)
- [x] Handle session cleanup on timeout (Caffeine eviction)

#### Deliverables
- Session-aware chat endpoint ✅
- Conversation context maintained across turns ✅

### Phase 3: WebSocket Streaming ✅ COMPLETE
**Goal**: Real-time token streaming
**Status**: Complete
**Implemented in**: `api-gateway/src/main/java/com/bmc/rag/api/controller/WebSocketChatController.java`

#### Tasks
- [x] Implement WebSocket handler for chat (`@MessageMapping("/chat.query")`)
- [x] Add token streaming with ChatResponseChunk (TOKEN, THINKING, COMPLETE, ERROR types)
- [x] Implement connection lifecycle management (dedicated ExecutorService with 8 threads)
- [x] Add error handling for stream interruptions (fallback responses, graceful degradation)
- [x] Implement streaming timeout (120 seconds) and max response size (50k chars)

#### Deliverables
- Working `/ws/chat` WebSocket endpoint ✅
- Real-time token streaming to clients ✅

### Phase 4: Search API ✅ COMPLETE
**Goal**: Semantic search without LLM generation
**Status**: Complete
**Implemented in**: `api-gateway/src/main/java/com/bmc/rag/api/controller/ChatController.java`

#### Tasks
- [x] Implement `/api/v1/chat/search` endpoint with rate limiting
- [x] Add result formatting and ranking via SecureContentRetriever
- [x] Implement ReBAC filtering on search results
- [x] Add metadata enrichment for results (sourceType, chunkType, category, score)

#### Deliverables
- Working search-only endpoint ✅
- Filtered, ranked results with metadata ✅

---

## 5. Technical Design

### 5.1 Class Design

```java
public interface RagService {
    ChatResponse chat(ChatRequest request);
    Flux<ChatResponseChunk> chatStream(ChatQueryMessage message);
    SearchResponse search(SearchRequest request);
}

public interface SessionManager {
    Session getOrCreate(String sessionId, String userId, Set<String> groups);
    void addMessage(String sessionId, Message message);
    List<Message> getHistory(String sessionId);
    void cleanup();
}
```

### 5.2 Sequence Diagram

```
User -> WebSocket -> ChatController
                          |
                          v
                    SessionManager
                          |
                          v
                    SecureContentRetriever
                          |
                          v
                    VectorStore -> pgvector
                          |
                          v
                    ReBACFilter
                          |
                          v
                    RagService -> OllamaChatModel
                          |
                          v
                    [Stream tokens]
                          |
                          v
User <- WebSocket <- ChatController
```

### 5.3 Prompt Template

```text
You are an IT support assistant. Answer questions using ONLY the provided context.
Always cite your sources using [Source: TYPE-ID] format.

Context:
{context}

Conversation History:
{history}

Question: {question}

Instructions:
- Only use information from the provided context
- If the context doesn't contain relevant information, say so
- Always cite the source for each piece of information
- Be concise and technical
```

---

## 6. Testing Strategy

### 6.1 Unit Tests
| Component | Test Class | Coverage Target |
|-----------|------------|-----------------|
| RagService | `RagServiceTest.java` | 80% |
| SessionManager | `SessionManagerTest.java` | 90% |
| ReBACFilter | `ReBACFilterTest.java` | 95% |

### 6.2 Integration Tests
| Scenario | Test Class | Environment |
|----------|------------|-------------|
| End-to-end chat | `ChatIntegrationTest.java` | Docker Compose |
| WebSocket streaming | `WebSocketIntegrationTest.java` | Docker Compose |
| Session persistence | `SessionIntegrationTest.java` | Docker Compose |

### 6.3 Performance Tests
| Metric | Test Method | Target |
|--------|-------------|--------|
| Search latency | JMeter | p95 <500ms |
| First token | Custom client | <1s |
| Concurrent users | Gatling | 100 sessions |

---

## 7. Rollout Plan

### 7.1 Feature Flags
- `rag.streaming.enabled` - Enable WebSocket streaming (default: true)
- `rag.session.redis.enabled` - Use Redis for sessions (default: false)

### 7.2 Migration Steps
1. Deploy RAG service with REST endpoint
2. Enable WebSocket streaming
3. Monitor performance metrics
4. Enable Redis sessions for HA (if needed)

### 7.3 Rollback Plan
1. Disable WebSocket, fall back to REST
2. Clear session state if corrupt
3. Revert to previous container image

---

## 8. Risks and Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Ollama unavailable | Low | High | Health checks, circuit breaker |
| Session memory exhaustion | Medium | Medium | Max session limit, aggressive cleanup |
| WebSocket connection drops | Medium | Low | Auto-reconnect in client |
| High LLM latency | Medium | Medium | Timeout, fallback to search-only |

---

## 9. Monitoring and Observability

### 9.1 Metrics
| Metric | Type | Alert Threshold |
|--------|------|-----------------|
| `rag.chat.latency` | Histogram | p99 >5s |
| `rag.search.latency` | Histogram | p99 >1s |
| `rag.sessions.active` | Gauge | >500 |
| `rag.ollama.errors` | Counter | >10/min |

### 9.2 Logs
| Log Event | Level | Context |
|-----------|-------|---------|
| Chat request received | INFO | sessionId, userId |
| Search completed | INFO | resultCount, latencyMs |
| Ollama error | ERROR | error message, retry count |
| Session expired | DEBUG | sessionId, age |

---

## 10. Sign-Off

| Role | Name | Date | Approved |
|------|------|------|----------|
| Tech Lead | | | [ ] |
| Architect | | | [ ] |
| Security | | | [ ] |
