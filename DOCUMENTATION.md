# BMC Remedy RAG Agent - Complete Technical Documentation

> **Last Updated:** 2025-01-17
> **Version:** 1.0.0-SNAPSHOT
> **Status:** Active Development

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Architecture](#2-architecture)
3. [Quick Start Guide](#3-quick-start-guide)
4. [Module Documentation](#4-module-documentation)
5. [API Reference](#5-api-reference)
6. [Configuration](#6-configuration)
7. [Security](#7-security)
8. [Development Guide](#8-development-guide)
9. [Deployment](#9-deployment)
10. [Troubleshooting](#10-troubleshooting)

---

## 1. Project Overview

### 1.1 What is BMC Remedy RAG Agent?

The **BMC Remedy RAG Agent** is an enterprise-grade, on-premise Retrieval-Augmented Generation (RAG) system that integrates with BMC Remedy AR System to provide intelligent IT support capabilities.

**Key Characteristics:**
- 🏠 **100% Air-Gapped**: No external cloud dependencies for core functionality
- 🔒 **Data Sovereignty**: Your ITSM data never leaves your infrastructure
- 🤖 **Local LLM Support**: Runs with Ollama for complete offline operation
- 📊 **Semantic Search**: Vector-based search over incidents and knowledge articles
- 🌐 **Bilingual**: English and Arabic support with proper RTL formatting

### 1.2 Technology Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| **Backend** | Java 17 + Spring Boot 3.2.0 | Core application framework |
| **AI Framework** | LangChain4j 0.35.0 | LLM orchestration |
| **Vector Database** | PostgreSQL 16 + pgvector 0.1.4 | Embedding storage and similarity search |
| **Embeddings** | ONNX all-minilm-l6-v2 (384-dim) | Local text vectorization |
| **LLM** | Z.AI (GLM-4.7) or Ollama (Llama 3) | Response generation |
| **Document Processing** | Apache Tika 2.9.1 | Attachment parsing |
| **Frontend** | React 19 + TypeScript + Tailwind CSS | Web interface |
| **Container** | Docker + Docker Compose | Deployment |

### 1.3 Design Principles

1. **Field ID Over Field Names**: Uses Remedy Field IDs (immutable) instead of field names (localized)
2. **Thread-Safe Connections**: `ThreadLocal` pattern for ARServerUser (non-thread-safe)
3. **Incremental Sync**: CDC-based synchronization using Last Modified Date
4. **ReBAC Security**: Relationship-Based Access Control at vector level
5. **Local-First**: Embeddings and optional LLM run entirely on-premise

---

## 2. Architecture

### 2.1 Three-Zone Topology

```
┌─────────────────────────────────────────────────────────────────────┐
│                        PRESENTATION ZONE                           │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │                   React 19 Frontend                          │  │
│  │  - Real-time chat interface                                   │  │
│  │  - WebSocket streaming                                       │  │
│  │  - Semantic search visualization                              │  │
│  └─────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                                    ▲
                                    │ WebSocket/REST
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         API GATEWAY LAYER                           │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │              Spring Boot REST Controllers                      │  │
│  │  - ChatController (chat & search)                             │  │
│  │  - IngestionController (sync orchestration)                  │  │
│  │  - WebSocketChatController (real-time streaming)            │  │
│  └─────────────────────────────────────────────────────────────────┘  │
│                                    ▲                                 │
└────────────────────────────────────────┼─────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          AI PROCESSING ZONE                         │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │                      RAG Service                               │  │
│  │  - RagAssistantService (orchestration)                        │  │
│  │  - SecureContentRetriever (ReBAC filtering)                 │  │
│  │  - PostgresChatMemoryStore (conversation memory)              │  │
│  └─────────────────────────────────────────────────────────────────┘  │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │                   Vectorization Engine                         │  │
│  │  - LocalEmbeddingService (ONNX, all-minilm-l6-v2)             │  │
│  │  - IncidentChunkStrategy (semantic chunking)                  │  │
│  │  - AttachmentParser (Apache Tika integration)                │  │
│  └─────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                                    ▲
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          STORAGE ZONE                                │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │              PostgreSQL + pgvector Extension                     │  │
│  │  - embedding_store (vectors + chunks)                          │  │
│  │  - sync_state (CDC tracking)                                   │  │
│  │  - chat_memory (conversation history)                           │  │
│  │  - HNSW indexing for fast similarity search                    │  │
│  └─────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                                    ▲
                                    │ Native Java RPC
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       LEGACY DATA ZONE                              │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │                    BMC Remedy AR System                         │  │
│  │  - HPD:Help Desk (Incidents)                                  │  │
│  │  - HPD:WorkLog (Work Logs)                                     │  │
│  │  - Knowledge Articles (solutions)                              │  │
│  │  - Change Requests                                            │  │
│  └─────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.2 Data Flow

#### Ingestion Flow (Remedy → Database)
```
1. Trigger Sync API
   ↓
2. RemedyConnector.extractModifiedSince(timestamp)
   ↓
3. Query BMC AR System using Field IDs (1000000161, 1000000000, etc.)
   ↓
4. Extract: Incidents, Attachments, Work Logs
   ↓
5. VectorizationEngine.chunk(records)
   - Resolution → standalone chunk
   - Work logs → grouped by submitter
   - All chunks → injected with Incident Summary
   ↓
6. LocalEmbeddingService.embed(text) → vector[384]
   ↓
7. VectorStoreService.storeBatch(chunks + vectors)
   ↓
8. PostgreSQL + pgvector (HNSW index updated)
```

#### Query Flow (User → Answer)
```
1. User Question via WebSocket/REST
   ↓
2. RagAssistantService.chat(sessionId, question, userContext)
   ↓
3. LocalEmbeddingService.embed(question) → query_vector
   ↓
4. SecureContentRetriever.retrieve(question, userGroups)
   - Apply ReBAC filter (assigned_group IN userGroups)
   - Vector similarity search in pgvector
   - Return top-k results
   ↓
5. Build messages:
   - System prompt (enhanced with citation rules)
   - Retrieved context (formatted chunks)
   - User question
   ↓
6. ZaiConfig.chatLanguageModel().generate(messages)
   - Streaming response token-by-token
   - Citations included: (Source: KB0000001)
   ↓
7. Stream tokens to frontend via WebSocket
   ↓
8. Store conversation in PostgresChatMemoryStore
```

---

## 3. Quick Start Guide

### 3.1 Prerequisites

| Requirement | Minimum Version | Notes |
|-------------|------------------|-------|
| Java | 17+ | OpenJDK or Eclipse Temurin |
| Maven | 3.9+ | For building |
| PostgreSQL | 16 | With pgvector extension |
| Node.js | 20+ | For frontend development |
| BMC AR System | 9.x - 20.x | With Java API access |

### 3.2 Installation Steps

#### Step 1: Clone and Build
```bash
git clone <repository-url>
cd bmc-remedy-rag-agent

# Install BMC AR API jar
mvn install:install-file \
  -Dfile=BMC/arAPI-91.9.jar \
  -DgroupId=com.bmc.arsys \
  -DartifactId=arAPI \
  -Dversion=91.9 \
  -Dpackaging=jar

# Build project
mvn clean package -DskipTests
```

#### Step 2: Configure Environment
```bash
cp .env.example .env
# Edit .env with your settings
```

Required variables:
```bash
# Database
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=bmc_rag
POSTGRES_USER=raguser
POSTGRES_PASSWORD=your_password

# Z.AI LLM (for cloud LLM)
ZAI_API_KEY=your_api_key
ZAI_BASE_URL=https://api.z.ai/api/paas/v4/
ZAI_MODEL=glm-4.5-flash

# BMC Remedy
REMEDY_SERVER=remedy.example.com
REMEDY_PORT=7100
REMEDY_USERNAME=raguser
REMEDY_PASSWORD=your_password
```

#### Step 3: Start Services
```bash
# Using Docker Compose (Recommended)
cd docker
docker-compose up -d

# Or Development Mode
./start-dev.sh
```

#### Step 4: Verify
```bash
# Health Check
curl http://localhost:8080/api/v1/health

# Expected Response:
# {"database":"UP","pgvector":"UP","status":"UP"}
```

---

## 4. Module Documentation

### 4.1 Module: remedy-connector

**Package:** `com.bmc.rag.connector`

**Purpose:** Native integration with BMC Remedy AR System using Java API

#### Key Classes

##### ThreadLocalARContext
```java
public class ThreadLocalARContext {
    // Thread-safe connection management
    private static final ThreadLocal<ARServerUser> CONTEXT = new ThreadLocal<>();

    public ARServerUser getContext() throws ARConnectionException;
    public <T> T executeWithRetry(AROperation<T> operation);
    public boolean verifyConnection();
    public void cleanup();
}
```

**Critical Notes:**
- `ARServerUser` is NOT thread-safe
- Must use ThreadLocal pattern in multi-threaded environments
- Always call `cleanup()` after use to prevent memory leaks

##### IncidentExtractor
```java
public class IncidentExtractor {
    public List<IncidentRecord> extractModifiedSince(long timestamp);
    public List<IncidentRecord> extractWithQualification(String qualification);
    public Optional<IncidentRecord> extractByIncidentNumber(String incidentNumber);
}
```

**Features:**
- Bulk extraction using `getListEntryObjects()`
- Pagination support (configurable chunk size)
- Field ID-based queries (immutable across upgrades)

##### FieldIdConstants
```java
public class FieldIdConstants {
    // Incident Form IDs
    public static final int INCIDENT_NUMBER = 1000000161;
    public static final int SUMMARY = 1000000000;
    public static final int NOTES = 1000000151;
    public static final int RESOLUTION = 1000000156;
    public static final int STATUS = 7;
    public static final int LAST_MODIFIED_DATE = 6;
    public static final int ASSIGNED_GROUP = 1000000217;
}
```

**Why Field IDs?**
- Immutable across Remedy upgrades and localization
- Consistent across all environments
- More performant than field name lookups

---

### 4.2 Module: vectorization-engine

**Package:** `com.bmc.rag.vectorization`

**Purpose:** Text processing and local embedding generation

#### Key Classes

##### LocalEmbeddingService
```java
@Service
public class LocalEmbeddingService {
    private static final int EMBEDDING_DIMENSION = 384;

    @PostConstruct
    public void init() {
        // Loads all-minilm-l6-v2 ONNX model
        // Runs entirely on-premise
    }

    public float[] embed(String text);
    public List<float[]> embedBatch(List<String> texts);
}
```

**Model Details:**
- **Model:** all-minilm-l6-v2
- **Dimensions:** 384
- **Framework:** ONNX Runtime
- **Storage Size:** ~80MB
- **Inference:** CPU-based (no GPU required)

##### IncidentChunkStrategy
```java
@Component
public class IncidentChunkStrategy {
    public List<Chunk> chunk(IncidentRecord incident);

    // Chunking Rules:
    // 1. Resolution field → standalone high-value chunk
    // 2. Work logs → grouped by submitter/day
    // 3. All chunks → injected with Incident Summary for context
}
```

**Chunk Metadata:**
```json
{
  "incident_number": "INC000001234",
  "summary": "Email server down for Finance dept",
  "assigned_group": "IT Support",
  "status": "Resolved",
  "chunk_type": "resolution|worklog|description",
  "created_date": 1705484800
}
```

---

### 4.3 Module: vector-store

**Package:** `com.bmc.rag.store`

**Purpose:** PostgreSQL + pgvector integration with vector operations

#### Database Schema

##### Table: embedding_store
```sql
CREATE TABLE embedding_store (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    embedding vector(384) NOT NULL,
    text_segment TEXT NOT NULL,
    metadata JSONB NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    source_id VARCHAR(100) NOT NULL,
    chunk_type VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- HNSW Index for fast similarity search
CREATE INDEX idx_embedding_hnsw ON embedding_store
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- GIN Index for metadata filtering
CREATE INDEX idx_embedding_metadata ON embedding_store USING GIN (metadata);

-- Source lookup index
CREATE INDEX idx_embedding_source ON embedding_store (source_type, source_id);
```

##### Table: sync_state
```sql
CREATE TABLE sync_state (
    id SERIAL PRIMARY KEY,
    source_type VARCHAR(50) UNIQUE NOT NULL,
    last_sync_timestamp BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    last_sync_finished_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    records_processed INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

##### Table: chat_memory
```sql
CREATE TABLE chat_memory (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_chat_memory_session
        FOREIGN KEY (session_id)
        REFERENCES chat_sessions(id)
        ON DELETE CASCADE
);
```

#### Key Classes

##### VectorStoreService
```java
@Service
public class VectorStoreService {
    public void storeBatch(List<EmbeddedChunk> chunks);
    public List<SearchResult> search(String query, int maxResults, float minScore);
    public List<SearchResult> searchWithGroups(String query, int maxResults,
                                               float minScore, List<String> groups);
    public Map<String, Long> getStatistics();
    public void clearBySourceType(String sourceType);
}
```

**Search Features:**
- Cosine similarity (default for embeddings)
- ReBAC filtering (by assigned_group)
- Result deduplication
- Configurable similarity threshold

---

### 4.4 Module: rag-service

**Package:** `com.bmc.rag.agent`

**Purpose:** LangChain4j orchestration with security and memory management

#### Key Classes

##### RagAssistantService
```java
@Service
public class RagAssistantService {
    private final ChatLanguageModel chatModel;
    private final StreamingChatLanguageModel streamingChatModel;
    private final SecureContentRetriever contentRetriever;
    private final PostgresChatMemoryStore chatMemoryStore;
    private final RagConfig ragConfig;

    // Main chat method
    public ChatResponse chat(String sessionId, String question,
                             UserContext userContext);

    // Streaming chat with token-by-token response
    public void chatWithStreaming(
        String sessionId,
        String question,
        UserContext userContext,
        Consumer<String> tokenConsumer,
        StreamingCompletionHandler completionConsumer
    );

    // Memory management
    public void clearSession(String sessionId);
    public List<ChatMessage> getConversationHistory(String sessionId);
}
```

**Session Management:**
- Caffeine cache with 24-hour expiration
- Maximum 10,000 concurrent sessions
- Message window: last 20 messages per session

##### SecureContentRetriever
```java
@Service
public class SecureContentRetriever {
    private final VectorStoreService vectorStoreService;

    public RetrievalResult retrieve(String query, UserContext userContext);

    private List<SearchResult> applyReBACFilter(
        List<SearchResult> results,
        List<String> userGroups,
        boolean prioritizeKnowledge
    );
}
```

**ReBAC Implementation:**
- Filters by `assigned_group` in metadata
- Public access if `assigned_group` is null/empty
- Case-insensitive group matching
- Supports multiple groups per user

---

### 4.5 Module: api-gateway

**Package:** `com.bmc.rag.api`

**Purpose:** REST API and WebSocket endpoints

#### Controllers

##### ChatController
```java
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {
    @PostMapping
    public ResponseEntity<ChatResponse> chat(
        @Valid @RequestBody ChatRequest request
    );

    @PostMapping("/search")
    public ResponseEntity<List<SearchResponse>> search(
        @Valid @RequestBody SearchRequest request
    );

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionInfo>> getSessions();

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> clearSession(@PathVariable String sessionId);
}
```

##### WebSocketChatController
```java
@Controller
public class WebSocketChatController {
    @MessageMapping("/ws-chat")
    public void handleChatMessage(
        SimpMessageHeaderAccessor headerAccessor,
        ChatMessage message
    );
}
```

**WebSocket Flow:**
```
Client ──(1)─→ WebSocket: Connect
Client ──(2)─→ WebSocket: Send message
Server ──(3)→ Streaming: Tokens chunk by chunk
Server ──(4)→ WebSocket: Complete + sources
```

---

## 5. API Reference

### 5.1 Chat API

#### POST /api/v1/chat
Send a chat message and get AI response.

**Request:**
```json
{
  "sessionId": "session-123",
  "question": "How do I reset my VPN password?",
  "userGroups": ["IT Support", "Service Desk"]
}
```

**Response:**
```json
{
  "sessionId": "session-123",
  "response": "I can help you reset your VPN password.\n\nSolution:\n1. Go to https://vpn.cst.gov.sa/selfservice\n2. Enter your employee ID\n3. Click 'Send OTP'\n4. Enter the OTP and create new password\n\nSources: (Source: KB0001234)",
  "sources": ["KB0001234"],
  "hasContext": true
}
```

#### POST /api/v1/chat/search
Semantic search over ITSM data.

**Request:**
```json
{
  "query": "VPN password reset",
  "maxResults": 5,
  "minScore": 0.7
}
```

**Response:**
```json
{
  "results": [
    {
      "sourceType": "KB",
      "sourceId": "KB0001234",
      "text": "To reset VPN password...",
      "score": 0.92,
      "metadata": {...}
    }
  ]
}
```

---

## 6. Configuration

### 6.1 Environment Variables

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `POSTGRES_HOST` | PostgreSQL host | localhost | Yes |
| `POSTGRES_PORT` | PostgreSQL port | 5432 | No |
| `POSTGRES_DB` | Database name | bmc_rag | Yes |
| `POSTGRES_USER` | Database user | raguser | Yes |
| `POSTGRES_PASSWORD` | Database password | - | Yes |
| `ZAI_API_KEY` | Z.AI API key | - | Yes* |
| `ZAI_BASE_URL` | Z.AI API URL | https://api.z.ai/api/paas/v4/ | No |
| `ZAI_MODEL` | Model name | glm-4.7 | No |
| `REMEDY_SERVER` | Remedy server | - | Yes |
| `REMEDY_PORT` | Remedy port | 7100 | No |
| `REMEDY_USERNAME` | Remedy username | - | Yes |
| `REMEDY_PASSWORD` | Remedy password | - | Yes |
| `RAG_MAX_RESULTS` | Max search results | 5 | No |
| `RAG_MIN_SCORE` | Min similarity score | 0.7 | No |
| `RAG_REBAC_ENABLED` | Enable ReBAC | true | No |

*Required if using Z.AI LLM. For Ollama-only deployment, can be empty.

### 6.2 Application Properties

See `api-gateway/src/main/resources/application.yml` for Spring Boot configuration.

---

## 7. Security

### 7.1 Relationship-Based Access Control (ReBAC)

#### How It Works

1. **User Groups**: Users belong to groups (e.g., "IT Support", "Finance", "HR")
2. **Record Assignment**: Remedy records have `assigned_group` field
3. **Filtering**: At query time, only return records where:
   - `assigned_group` is NULL/empty (public), OR
   - `assigned_group` IN (user's groups)

#### Implementation

```java
// UserContext contains user's groups
UserContext userContext = new UserContext(
    "user123",
    Set.of("IT Support", "Service Desk")
);

// Search applies ReBAC filter
List<SearchResult> filtered = vectorStoreService.searchWithGroups(
    query,
    maxResults,
    minScore,
    userContext.getUserGroups()
);
```

#### Configuration

```bash
# Enable ReBAC (default: true)
RAG_REBAC_ENABLED=true
```

### 7.2 Authentication/Authorization

**Development Mode:**
```bash
SECURITY_ENABLED=false
```

**Production Mode:**
```bash
SECURITY_ENABLED=true
JWT_JWK_SET_URI=https://your-auth-server/.well-known/jwks.json
```

---

## 8. Development Guide

### 8.1 Building the Project

```bash
# Full build with tests
mvn clean install

# Build without tests
mvn clean package -DskipTests

# Build specific module
mvn clean package -pl remedy-connector -am
```

### 8.2 Running Locally

```bash
# Using the provided script
./start-dev.sh

# Or manually
source .env
mvn spring-boot:run -pl api-gateway -Dspring-boot.run.profiles=dev
```

### 8.3 Running Tests

```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=RagAssistantServiceTest

# Integration tests
mvn verify -Pintegration-test
```

### 8.4 Code Style

- Java: Google Java Style Guide
- TypeScript: ESLint with TypeScript rules
- All code must pass Spotless checks

---

## 9. Deployment

### 9.1 Docker Deployment

```bash
cd docker
docker-compose up -d
```

### 9.2 Kubernetes

```bash
kubectl apply -f k8s/ -n bmc-rag
```

### 9.3 Resource Requirements

| Component | Min RAM | Min CPU | Disk |
|-----------|---------|---------|------|
| PostgreSQL | 4GB | 2 cores | 50GB |
| Java App | 8GB heap | 4 cores | 10GB |
| Ollama (optional) | 16GB | 8 cores | 50GB |
| Total (with Ollama) | 32GB | 14 cores | 110GB |

---

## 10. Troubleshooting

### 10.1 Common Errors

| Error | Cause | Solution |
|-------|-------|----------|
| ARERR 93 | Server query timeout | Reduce `REMEDY_CHUNK_SIZE` to 250 |
| ARERR 92 | Network RPC timeout | Increase `REMEDY_SOCKET_TIMEOUT` |
| Port 8080 in use | Previous instance running | Kill process on port 8080 |
| Z.AI API key blank | Missing env variable | Set `ZAI_API_KEY` in .env |
| pgvector does not exist | Extension not enabled | Run `CREATE EXTENSION vector;` |

### 10.2 Logging

```bash
# Follow application logs
tail -f /tmp/bmc-rag.log

# Check database logs
docker-compose logs -f postgres

# View recent errors
grep -i "error" /tmp/bmc-rag.log | tail -50
```

### 10.3 Health Checks

```bash
# Application health
curl http://localhost:8080/api/v1/health

# Database connectivity
curl http://localhost:8080/api/v1/health/db

# Remedy connection
curl http://localhost:8080/api/v1/health/remedy
```

---

## Appendix A: Field ID Reference

### Incident Form (HPD:Help Desk)

| Field | Field ID | Type | Description |
|-------|----------|------|-------------|
| Incident Number | 1000000161 | Character | Unique identifier |
| Summary | 1000000000 | Character | Brief description |
| Notes | 1000000151 | Character | Detailed notes |
| Resolution | 1000000156 | Character | Solution steps |
| Status | 7 | Enum | Open/Closed/Resolved/etc |
| Last Modified | 6 | Integer | Unix epoch timestamp |
| Assigned Group | 1000000217 | Character | Team responsible |
| Priority | 7 | Integer | 1-5 scale |
| Impact | 8 | Integer | 1-5 scale |
| Urgency | 9 | Integer | 1-5 scale |

---

## Appendix B: API Response Codes

| Code | Description | Example |
|------|-------------|---------|
| 200 | Success | Chat response returned |
| 400 | Bad Request | Invalid request parameters |
| 401 | Unauthorized | Missing/invalid credentials |
| 403 | Forbidden | Insufficient permissions (ReBAC) |
| 404 | Not Found | Resource not found |
| 500 | Internal Error | Server error (check logs) |
| 503 | Service Unavailable | Temporarily unavailable |

---

## Appendix C: System Prompt Reference

See `.github/ENHANCED_SYSTEM_PROMPT.md` for the complete system prompt used by the LLM.

---

## Appendix D: Architecture Decision Records

### ADR-001: Why Field IDs Over Field Names?
**Decision:** Use integer Field IDs for all Remedy queries

**Rationale:**
- Field IDs are immutable across Remedy upgrades
- Field names change with localization
- Field IDs are more performant
- Consistent across all environments

### ADR-002: Why ThreadLocal for ARServerUser?
**Decision:** Use ThreadLocal pattern for ARServerUser management

**Rationale:**
- ARServerUser is NOT thread-safe
- ThreadLocal ensures thread confinement
- Prevents connection pool corruption
- Automatic cleanup with try-finally

### ADR-003: Why PostgreSQL+pgvector over Vector Database?
**Decision:** Use PostgreSQL with pgvector extension

**Rationale:**
- Enterprise-grade reliability
- SQL support for complex joins
- ACID compliance
- Existing DBA expertise
- No additional infrastructure

---

## Change Log

| Version | Date | Changes |
|--------|------|---------|
| 1.0.0 | 2025-01-17 | Initial release with enhanced system prompt |

---

**Document Version:** 1.0
**Last Reviewed:** 2025-01-17
