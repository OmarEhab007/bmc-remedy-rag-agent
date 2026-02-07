# Implementation Plan: Production Readiness Enhancements

**Branch**: `001-production-readiness` | **Date**: 2026-02-07 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/001-production-readiness/spec.md`

## Summary

Bring the BMC Remedy RAG Agent to production readiness by fixing critical security vulnerabilities (SQL injection, resource leaks, thread safety), adding automated CI/CD with GitHub Actions, implementing structured JSON logging with correlation IDs, and achieving meaningful test coverage across all 5 backend modules and the React frontend.

## Technical Context

**Language/Version**: Java 17+ (backend), TypeScript 5.x (frontend)
**Primary Dependencies**: Spring Boot 3.2, LangChain4j 1.0.0-beta2, React 19, Tailwind CSS
**Storage**: PostgreSQL 16 + pgvector (384-dim embeddings), Flyway migrations V1-V11
**Testing**: JUnit 5 + Mockito (backend), Vitest + React Testing Library (frontend)
**Target Platform**: Linux server (on-premise/air-gapped), Docker containers
**Project Type**: Multi-module Maven monorepo + React SPA
**Performance Goals**: CI pipeline < 15 min, health check < 5s, structured logs for 100% of events
**Constraints**: Air-gapped deployment possible, no external cloud dependencies required
**Scale/Scope**: 5 Java modules (~100 source files), 1 React app (~45 source files), 12 existing test files

## Constitution Check

*GATE: No constitution principles defined. Proceeding with industry best practices.*

No gates to enforce - constitution is an empty template. Applying standard engineering principles:
- Security fixes take highest priority
- All changes maintain backward compatibility
- Tests accompany all code changes
- CI pipeline gates merges on test passage

## Project Structure

### Documentation (this feature)

```text
specs/001-production-readiness/
├── plan.md              # This file
├── research.md          # Phase 0: Technology decisions
├── data-model.md        # Phase 1: Entity definitions
├── quickstart.md        # Phase 1: Dev setup guide
├── contracts/           # Phase 1: API contracts
│   └── health-api.yaml  # Health check endpoint spec
└── tasks.md             # Phase 2 output (via /speckit.tasks)
```

### Source Code (repository root)

```text
bmc-remedy-rag-agent/                    # Maven parent POM
├── remedy-connector/                    # BMC AR API integration
│   └── src/main/java/.../connector/
├── vectorization-engine/                # ONNX embeddings + Tika
│   └── src/main/java/.../vectorization/
├── vector-store/                        # pgvector + Flyway
│   └── src/main/java/.../store/
├── rag-service/                         # LangChain4j + tools
│   └── src/main/java/.../agent/
├── api-gateway/                         # Spring Boot entry point
│   ├── src/main/java/.../api/
│   └── src/main/resources/
│       ├── application.yml
│       └── logback-spring.xml           # NEW: Structured logging
├── frontend/web-chat/                   # React 19 + TypeScript
│   ├── src/components/
│   ├── src/__tests__/                   # NEW: Frontend tests
│   ├── vitest.config.ts                 # NEW: Test config
│   └── vitest.setup.ts                  # NEW: Test setup
├── .github/workflows/                   # NEW: CI/CD pipelines
│   ├── ci.yml                           # Build + test on push/PR
│   └── docker.yml                       # Container build on release
└── docker/
    ├── Dockerfile
    └── docker-compose.yml
```

**Structure Decision**: Existing multi-module Maven structure is well-organized. New files are added within existing modules (logback config, test files) plus new top-level directories (.github/workflows). No structural changes needed.

## Implementation Phases

### Phase A: Security Fixes (P0) - ALREADY DONE

Files already modified in current branch:
1. `vector-store/.../HybridSearchService.java` - Input validation for ef_search (bounds check 1-1000)
2. `api-gateway/.../OpenAiCompatibleController.java` - @PreDestroy shutdown hook, configurable tool-server base URL
3. `rag-service/.../RagAssistantService.java` - StringBuffer for thread-safe streaming
4. `api-gateway/src/main/resources/application.yml` - tool-server.base-url configuration

### Phase B: CI/CD Pipeline

**GitHub Actions CI workflow** (`.github/workflows/ci.yml`):
- Trigger: push to any branch, PR to main
- Steps: checkout, setup JDK 17, cache Maven, build, test, upload test reports
- Frontend: setup Node 20, npm install, lint, test, build

**Docker workflow** (`.github/workflows/docker.yml`):
- Trigger: release tags (v*)
- Steps: build Docker image, push to ghcr.io

### Phase C: Observability

**Structured logging** (`api-gateway/src/main/resources/logback-spring.xml`):
- Dev profile: Console pattern with colors
- Prod profile: JSON format with Logstash encoder
- MDC correlation ID filter for request tracing

**Health checks**:
- Spring Actuator already exposed at /actuator/health
- Add custom health indicators for LLM provider and embedding service

### Phase D: Test Coverage

**Backend tests** (JUnit 5 + Mockito):
- `vector-store`: HybridSearchService, VectorStoreService
- `rag-service`: RagAssistantService (streaming), ConfirmationService
- `api-gateway`: OpenAiCompatibleController, ChatController, WebSocketChatController

**Frontend tests** (Vitest + React Testing Library):
- Setup: vitest.config.ts, vitest.setup.ts, test scripts in package.json
- Component tests: ChatMain, MessageBubble, InputArea, CitationBlock, ConfirmationPrompt

## Risk Assessment

| Risk | Mitigation |
|------|-----------|
| CI pipeline timeout | Cache Maven dependencies, parallelize frontend/backend |
| Logback config breaks existing logs | Profile-based activation (prod only for JSON) |
| New tests are flaky | Mock all external dependencies, use deterministic test data |
| Frontend test setup conflicts with build | Isolate test config, add to CI as separate step |

## Complexity Tracking

No constitution violations to justify.
