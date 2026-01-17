<h1 align="center">BMC Remedy RAG Agent</h1>

<p align="center">
  <b>🤖 On-premise Retrieval-Augmented Generation agent for BMC Remedy ITSM</b>
</p>

<p align="center">
  <a href="#quick-start">Quick Start</a> •
  <a href="#architecture">Architecture</a> •
  <a href="#api-endpoints">API</a> •
  <a href="#deployment">Deployment</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17+-orange?logo=java&logoColor=white" alt="Java"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2.0-green?logo=springboot&logoColor=white" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/LangChain4j-0.35.0-blue?logo=java&logoColor=white" alt="LangChain4j"/>
  <img src="https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql&logoColor=white" alt="PostgreSQL"/>
  <img src="https://img.shields.io/badge/pgvector-0.1.4-purple?logo=postgresql&logoColor=white" alt="pgvector"/>
  <img src="https://img.shields.io/badge/React-19+-cyan?logo=react&logoColor=white" alt="React"/>
  <img src="https://img.shields.io/badge/RAG-✓-brightgreen" alt="RAG"/>
  <img src="https://img.shields.io/badge/ITSM-✓-blue" alt="ITSM"/>
</p>

<p align="center">
  <i>Enterprise-grade AI for IT support workflows</i>
</p>

## Overview

This is an enterprise-grade, air-gapped RAG agent that extracts ITSM data from BMC Remedy AR System (versions 9.x through 20.x), vectorizes it using local embeddings, and enables semantic search with AI-powered responses. The system is designed for on-premise deployment with no external cloud dependencies.

### Key Features

- **Native BMC Remedy Integration**: Uses the Java AR API (not REST) for direct RPC communication with Remedy servers
- **Local Embeddings**: ONNX-based `all-minilm-l6-v2` model (384-dimensional vectors) runs locally
- **Flexible LLM Backend**: Supports Z.AI cloud API (default) with optional Ollama for local inference
- **Vector Storage**: PostgreSQL with pgvector extension for efficient similarity search
- **Incremental Sync**: CDC-based synchronization using Remedy's `Last Modified Date` field
- **ReBAC Security**: Relationship-Based Access Control filtering at the vector level
- **Modern Web UI**: React 19 + TypeScript frontend with WebSocket streaming

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                     BMC Remedy AR System                            │
│                   (Legacy Data Zone - Source)                       │
└─────────────────────────────────────┬───────────────────────────────┘
                                      │ Native Java RPC
                                      ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      Remedy Connector Module                        │
│  - ThreadLocal connection pooling                                  │
│  - Field ID-based queries (immutable across upgrades)              │
│  - Attachment & Work Log extraction                                │
└─────────────────────────────────────┬───────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────┐
│                   Vectorization Engine                              │
│  - ONNX embeddings (all-minilm-l6-v2)                              │
│  - Chunking strategies for Resolution, Work Logs                   │
│  - Apache Tika for attachment parsing                              │
└─────────────────────────────────────┬───────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────┐
│              PostgreSQL + pgvector Extension                        │
│              (Storage Zone - HNSW indexing)                        │
└─────────────────────────────────────┬───────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      RAG Service                                    │
│  - LangChain4j orchestration                                       │
│  - ReBAC security filtering                                        │
│  - Chat memory management (PostgreSQL-backed)                      │
└─────────────────────────────────────┬───────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      API Gateway                                    │
│  - REST API + WebSocket streaming                                  │
│  - Ingestion orchestration                                         │
│  - Admin endpoints                                                 │
└─────────────────────────────────────┬───────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────┐
│                   React Web UI                                      │
│  - Real-time chat interface                                        │
│  - Semantic search visualization                                   │
│  - Admin dashboard                                                 │
└─────────────────────────────────────────────────────────────────────┘
```

## Modules

| Module | Description |
|--------|-------------|
| `remedy-connector` | BMC AR API integration with connection pooling and data extraction |
| `vectorization-engine` | ONNX-based embedding generation and chunking strategies |
| `vector-store` | PostgreSQL + pgvector integration with Flyway migrations |
| `rag-service` | LangChain4j orchestration, ReBAC, and chat memory |
| `api-gateway` | REST/WebSocket APIs and application bootstrap |
| `frontend/web-chat` | React 19 + TypeScript + Tailwind CSS UI |

## Prerequisites

- **Java 17+** (OpenJDK or Eclipse Temurin)
- **Maven 3.9+**
- **PostgreSQL 16** with pgvector extension
- **Node.js 20+** (for frontend development)
- **BMC AR System 9.x - 20.x** with Java API access
- **Z.AI API Key** (or Ollama for local LLM)

## Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/your-org/bmc-remedy-rag-agent.git
cd bmc-remedy-rag-agent
```

### 2. Install BMC AR API Jar

```bash
# Install the BMC AR API jar to your local Maven repository
mvn install:install-file \
  -Dfile=BMC/arAPI-91.9.jar \
  -DgroupId=com.bmc.arsys \
  -DartifactId=arAPI \
  -Dversion=91.9 \
  -Dpackaging=jar
```

### 3. Configure Environment

```bash
cp .env.example .env
# Edit .env with your configuration
```

Required environment variables:

```bash
# Database
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=bmc_rag
POSTGRES_USER=raguser
POSTGRES_PASSWORD=your_password

# Z.AI LLM (or configure Ollama for local inference)
ZAI_API_KEY=your_zai_api_key
ZAI_BASE_URL=https://api.z.ai/api/paas/v4/
ZAI_MODEL=glm-4.7

# BMC Remedy Connection
REMEDY_SERVER=remedy.example.com
REMEDY_PORT=7100
REMEDY_USERNAME=raguser
REMEDY_PASSWORD=your_password

# RAG Configuration
RAG_MAX_RESULTS=5
RAG_MIN_SCORE=0.7
RAG_REBAC_ENABLED=true
```

### 4. Start with Docker Compose (Recommended)

```bash
cd docker
docker-compose up -d
```

### 5. Development Mode

```bash
# Backend
./start-dev.sh

# Frontend (separate terminal)
cd frontend/web-chat
npm install
npm run dev
```

### 6. Access the Application

- **API**: http://localhost:8080
- **Web UI**: http://localhost:5173
- **Health Check**: http://localhost:8080/api/v1/health

## API Endpoints

### Chat & Search

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/chat` | POST | Send chat message (returns streaming response) |
| `/api/v1/chat/stream` | WebSocket | Real-time chat streaming |
| `/api/v1/search` | POST | Semantic search over incidents |

### Ingestion

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/ingest/incidents` | POST | Trigger incident ingestion |
| `/api/v1/ingest/status` | GET | Get ingestion status |
| `/api/v1/ingest/sync` | POST | Trigger incremental sync |

### Admin

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/admin/stats` | GET | System statistics |
| `/api/v1/admin/clear-cache` | DELETE | Clear vector cache |

## BMC Remedy Integration

### Critical Field IDs

| Field | ID | Type | Notes |
|-------|-----|------|-------|
| Incident Number | 1000000161 | Character | Primary identifier |
| Summary | 1000000000 | Character | Injected into all chunk metadata |
| Notes/Description | 1000000151 | Character | Multi-line text |
| Resolution | 1000000156 | Character | High-value standalone chunk |
| Status | 7 | Enum | Open/Closed/Resolved |
| Assigned Group | 1000000217 | Character | Used for ReBAC filtering |
| Last Modified Date | 6 | Integer | Unix epoch, used for CDC |

### Connection Management

- **Thread Safety**: `ARServerUser` is NOT thread-safe - uses `ThreadLocal` pattern
- **License**: Use "Fixed" license accounts for background processes
- **Timeouts**: Configure `setSocketTimeOut()` for RPC operations
- **Pagination**: Use `firstRetrieve`/`maxRetrieve` with chunk size 100-500

### Query Best Practices

```java
// ✓ CORRECT: Use Field IDs (immutable)
QualifierInfo qualifier = new QualifierInfo(
    "'1000000156' != $NULL$"  // Resolution field
);

// ✗ INCORRECT: Field names (localized, mutable)
QualifierInfo qualifier = new QualifierInfo(
    "'Resolution' != $NULL$"
);

// ✓ CORRECT: Date as Unix epoch
QualifierInfo qualifier = new QualifierInfo(
    "6 > " + lastSyncTimestamp
);
```

## Vectorization Strategy

### Chunking Guidelines

1. **Resolution Field**: Treat as standalone high-value chunk
2. **Work Logs**: Group by submitter or timestamp (avoid context overflow)
3. **All Chunks**: Inject Incident Summary into metadata for context

### pgvector Schema

```sql
CREATE TABLE embedding_store (
    id UUID PRIMARY KEY,
    embedding vector(384),
    text_segment TEXT,
    metadata JSONB
);

CREATE INDEX ON embedding_store
USING hnsw (embedding vector_cosine_ops);
```

## Security

### Relationship-Based Access Control (ReBAC)

1. Store `Assigned Group` in vector metadata during ingestion
2. Query user's group memberships at runtime
3. Filter vector searches: `metadata.groups IN (user_groups)`

### Authentication/Authorization

```java
// Development: Disabled (see .env)
SECURITY_ENABLED=false

// Production: Configure OAuth2/OIDC
export SECURITY_ENABLED=true
export JWT_JWK_SET_URI=https://your-auth-server/.well-known/jwks.json
```

## Development

### Building

```bash
# Full build with tests
mvn clean install

# Build without tests
mvn clean package -DskipTests

# Build specific module
mvn clean package -pl remedy-connector -am
```

### Running Tests

```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=IncidentExtractorTest

# Integration tests with Testcontainers
mvn verify -Pintegration-test
```

### Frontend Development

```bash
cd frontend/web-chat

# Development server
npm run dev

# Production build
npm run build

# Lint
npm run lint
```

## Deployment

### Docker

```bash
# Build image
docker build -f docker/Dockerfile -t bmc-rag-agent:latest .

# Run with docker-compose
cd docker
docker-compose up -d

# View logs
docker-compose logs -f rag-agent
```

### Kubernetes

```bash
# Create namespace
kubectl create namespace bmc-rag

# Deploy
kubectl apply -f k8s/ -n bmc-rag

# Check status
kubectl get pods -n bmc-rag
```

## Monitoring

### Health Endpoints

- `/api/v1/health` - Application health
- `/api/v1/health/db` - Database connectivity
- `/api/v1/health/remedy` - Remedy connection status

### Metrics

The application exposes Spring Boot Actuator metrics at `/actuator`:

- JVM memory/GC
- HTTP request metrics
- Database connection pool stats
- Custom RAG metrics (embedding time, search latency)

## Troubleshooting

### Common Errors

| Error | Cause | Solution |
|-------|-------|----------|
| ARERR 93 | Server-side query timeout | Reduce `REMEDY_CHUNK_SIZE` |
| ARERR 92 | Network-level RPC timeout | Increase `REMEDY_SOCKET_TIMEOUT` |
| Connection refused | BMC API jar not installed | Run `mvn install:install-file` |
| pgvector does not exist | Extension not enabled | Run `CREATE EXTENSION vector;` |

### Logs

```bash
# Docker logs
docker-compose logs -f rag-agent

# Application logs
tail -f api-gateway/logs/application.log
```

## Contributing

This is proprietary software. For questions or support, contact the BMC Remedy team.

## License

Proprietary - Copyright © 2025. All rights reserved.

## Acknowledgments

- **LangChain4j** - Java LLM orchestration framework
- **pgvector** - PostgreSQL vector similarity search
- **Apache Tika** - Document content extraction
- **BMC Software** - AR System Java API

---
