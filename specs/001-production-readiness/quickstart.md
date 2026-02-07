# Quickstart: Production Readiness Enhancements

**Feature**: 001-production-readiness
**Branch**: `001-production-readiness`

## Prerequisites

- Java 17+ (JDK)
- Maven 3.8+ (or use included `./mvnw`)
- Node.js 20+ and npm
- Docker (for PostgreSQL and container builds)
- Git

## Setup

### 1. Clone and checkout

```bash
git clone https://github.com/OmarEhab007/bmc-remedy-rag-agent.git
cd bmc-remedy-rag-agent
git checkout 001-production-readiness
```

### 2. Build backend

```bash
source .env  # Load API keys
./mvnw clean install
```

### 3. Run tests

```bash
# Backend tests
./mvnw test

# Frontend tests
cd frontend/web-chat
npm install
npm test
```

### 4. Start application

```bash
source .env && ./start-all.sh
```

### 5. Verify health

```bash
curl http://localhost:8080/actuator/health | jq .
```

## What's New in This Feature

### Security Fixes
- SQL injection prevention in HybridSearchService
- Thread-safe streaming responses (StringBuffer)
- Graceful executor shutdown (@PreDestroy)
- Configurable tool-server URLs (no hardcoded localhost)

### CI/CD
- `.github/workflows/ci.yml` - Runs on push/PR: build, test, lint
- `.github/workflows/docker.yml` - Docker image build on release tags

### Observability
- `logback-spring.xml` - JSON structured logging in prod profile
- Correlation ID filter for request tracing
- Custom health indicators for LLM and embedding services

### Tests
- Backend: New unit tests for HybridSearchService, VectorStoreService, RagAssistantService, OpenAiCompatibleController
- Frontend: Vitest + React Testing Library setup with component tests

## Verifying Changes

### Check structured logging (prod profile)
```bash
source .env
java -jar api-gateway/target/api-gateway-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod 2>&1 | head -5
# Should output JSON lines
```

### Check CI pipeline
Push to any branch and verify GitHub Actions runs successfully.

### Run specific test modules
```bash
./mvnw test -pl vector-store     # Vector store tests
./mvnw test -pl rag-service      # RAG service tests
./mvnw test -pl api-gateway      # API gateway tests
```
