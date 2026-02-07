# Tasks: Production Readiness Enhancements

**Input**: Design documents from `/specs/001-production-readiness/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: Test tasks ARE included as the spec explicitly requests comprehensive test coverage (User Story 4, FR-008, FR-009).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Backend**: `{module}/src/main/java/com/bmc/rag/...` and `{module}/src/test/java/com/bmc/rag/...`
- **Frontend**: `frontend/web-chat/src/...` and `frontend/web-chat/src/__tests__/...`
- **CI/CD**: `.github/workflows/...`
- **Config**: `api-gateway/src/main/resources/...`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization for new testing infrastructure and CI/CD scaffolding

- [ ] T001 Create `.github/workflows/` directory structure at repository root
- [ ] T002 [P] Install frontend test dependencies: `vitest`, `@testing-library/react`, `@testing-library/jest-dom`, `jsdom` in `frontend/web-chat/package.json`
- [ ] T003 [P] Create frontend test configuration in `frontend/web-chat/vitest.config.ts`
- [ ] T004 [P] Create frontend test setup file in `frontend/web-chat/vitest.setup.ts` with jsdom and testing-library matchers
- [ ] T005 [P] Add `"test"` and `"test:coverage"` scripts to `frontend/web-chat/package.json`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Security fixes that MUST be complete before any other story can proceed safely

**Already completed in current branch:**

- [x] T006 Fix SQL injection in `vector-store/src/main/java/com/bmc/rag/store/service/HybridSearchService.java` - add bounds validation (1-1000) for ef_search parameter
- [x] T007 Fix resource leak in `api-gateway/src/main/java/com/bmc/rag/api/controller/OpenAiCompatibleController.java` - add @PreDestroy shutdown hook for streamingExecutor
- [x] T008 Fix thread safety in `rag-service/src/main/java/com/bmc/rag/agent/service/RagAssistantService.java` - replace StringBuilder with StringBuffer for streaming
- [x] T009 Externalize hardcoded localhost URLs in `api-gateway/src/main/java/com/bmc/rag/api/controller/OpenAiCompatibleController.java` - use configurable `tool-server.base-url` property
- [x] T010 Add `tool-server.base-url` configuration to `api-gateway/src/main/resources/application.yml`

**Checkpoint**: Foundation ready - all P0 security fixes applied, user story implementation can begin in parallel

---

## Phase 3: User Story 1 - Secure Data Queries (Priority: P1) - Security Validation Tests

**Goal**: Verify that all security fixes are working correctly through dedicated test cases

**Independent Test**: Run `./mvnw test -pl vector-store -pl rag-service` and verify all security-focused tests pass

### Tests for User Story 1

- [ ] T011 [P] [US1] Create `HybridSearchServiceTest.java` in `vector-store/src/test/java/com/bmc/rag/store/service/` - test ef_search bounds validation (valid range, below minimum, above maximum, negative values)
- [ ] T012 [P] [US1] Create `VectorStoreServiceTest.java` in `vector-store/src/test/java/com/bmc/rag/store/service/` - test search, storeBatch, formatEmbedding with mocked JdbcTemplate
- [ ] T013 [P] [US1] Add streaming thread-safety test to `rag-service/src/test/java/com/bmc/rag/agent/service/RagAssistantServiceTest.java` - verify StringBuffer usage with concurrent token callbacks

**Checkpoint**: Security fixes validated with passing tests

---

## Phase 4: User Story 2 - Automated Build and Quality Verification (Priority: P1) - CI/CD

**Goal**: Every push/PR triggers automated build, test, and quality checks

**Independent Test**: Push a commit and verify GitHub Actions workflow runs successfully, reporting status on the PR

### Implementation for User Story 2

- [ ] T014 [US2] Create GitHub Actions CI workflow in `.github/workflows/ci.yml` - trigger on push/PR, JDK 17 setup, Maven cache, build, test, upload test reports
- [ ] T015 [US2] Add frontend build and lint steps to `.github/workflows/ci.yml` - Node 20 setup, npm install, lint, test, build
- [ ] T016 [P] [US2] Create GitHub Actions Docker workflow in `.github/workflows/docker.yml` - trigger on release tags, build and push to ghcr.io

**Checkpoint**: CI pipeline runs on push/PR. Docker image builds on release tags.

---

## Phase 5: User Story 3 - Reliable System Monitoring (Priority: P2) - Observability

**Goal**: Structured JSON logs in prod, correlation IDs for tracing, health endpoints for all components

**Independent Test**: Start application with prod profile, make a request, verify JSON log output with correlation ID. Check `/actuator/health` returns component details.

### Implementation for User Story 3

- [ ] T017 [US3] Add `logstash-logback-encoder` dependency to `api-gateway/pom.xml`
- [ ] T018 [US3] Create `logback-spring.xml` in `api-gateway/src/main/resources/` - console pattern for dev, JSON encoder for prod, MDC correlation ID
- [ ] T019 [US3] Create `CorrelationIdFilter.java` in `api-gateway/src/main/java/com/bmc/rag/api/filter/` - OncePerRequestFilter that generates UUID and sets MDC "correlationId"
- [ ] T020 [US3] Register CorrelationIdFilter as a Spring bean in `api-gateway/src/main/java/com/bmc/rag/api/config/WebConfig.java` or via @Component
- [ ] T021 [P] [US3] Create `LlmHealthIndicator.java` in `api-gateway/src/main/java/com/bmc/rag/api/health/` - custom HealthIndicator that checks LLM provider availability
- [ ] T022 [P] [US3] Create `EmbeddingHealthIndicator.java` in `api-gateway/src/main/java/com/bmc/rag/api/health/` - custom HealthIndicator that checks embedding service status

**Checkpoint**: Structured logs emitted in prod, correlation IDs present, health endpoint reports all components

---

## Phase 6: User Story 4 - Test Coverage (Priority: P2) - Comprehensive Tests

**Goal**: Critical service classes across all modules have meaningful unit tests. Frontend has test infrastructure with component tests.

**Independent Test**: Run `./mvnw test` for backend and `npm test` in frontend to verify all tests pass

### Backend Tests for User Story 4

- [ ] T023 [P] [US4] Create `OpenAiCompatibleControllerTest.java` in `api-gateway/src/test/java/com/bmc/rag/api/controller/` - test chat completions, model listing, streaming, rate limiting, shutdown
- [ ] T024 [P] [US4] Create `ChatControllerTest.java` in `api-gateway/src/test/java/com/bmc/rag/api/controller/` - test chat endpoint, search endpoint, session clearing
- [ ] T025 [P] [US4] Create `WebSocketChatControllerTest.java` in `api-gateway/src/test/java/com/bmc/rag/api/controller/` - test WebSocket message handling with mocked services
- [ ] T026 [P] [US4] Create `ConfirmationServiceTest.java` in `rag-service/src/test/java/com/bmc/rag/agent/service/` - test staging, confirmation, cancellation, expiry, duplicate detection
- [ ] T027 [P] [US4] Create `RemedyIncidentToolTest.java` in `rag-service/src/test/java/com/bmc/rag/agent/tools/` - test search, create, confirm tool methods

### Frontend Tests for User Story 4

- [ ] T028 [P] [US4] Create `ChatMain.test.tsx` in `frontend/web-chat/src/__tests__/` - test message rendering, input submission, loading state
- [ ] T029 [P] [US4] Create `MessageBubble.test.tsx` in `frontend/web-chat/src/__tests__/` - test user/assistant message display, markdown rendering
- [ ] T030 [P] [US4] Create `InputArea.test.tsx` in `frontend/web-chat/src/__tests__/` - test text input, submit button, disabled state, character limit
- [ ] T031 [P] [US4] Create `CitationBlock.test.tsx` in `frontend/web-chat/src/__tests__/` - test citation display, source links, score rendering
- [ ] T032 [P] [US4] Create `ConfirmationPrompt.test.tsx` in `frontend/web-chat/src/__tests__/` - test confirm/cancel buttons, action display, expiry warning

**Checkpoint**: Backend tests cover all critical service classes. Frontend has 5 component tests. Full test suite passes in CI.

---

## Phase 7: User Story 5 - Portable Configuration (Priority: P3)

**Goal**: All service URLs are configurable via environment variables with sensible defaults

**Independent Test**: Set `TOOL_SERVER_BASE_URL=http://staging:8080` and verify application uses it for all internal calls

### Implementation for User Story 5

- [x] T033 [US5] Already done - `tool-server.base-url` added to `application.yml` and `OpenAiCompatibleController.java`
- [ ] T034 [US5] Verify no other hardcoded URLs remain by searching codebase for `localhost:8080` patterns in Java source files

**Checkpoint**: All service endpoints are configurable. Application deployable to any environment via env vars.

---

## Phase 7.5: Data Layer Enhancements (Priority: P3) - JPA & Migration

**Goal**: Improve data layer with JPA configuration refactoring and Arabic/bilingual search support

### Implementation

- [ ] T039 [P] Move `@EnableJpaRepositories` and `@EntityScan` from main application class to dedicated `JpaConfig.java` in `api-gateway/src/main/java/com/bmc/rag/api/config/` to avoid forcing JPA bootstrap in `@WebMvcTest` slices
- [ ] T040 [P] Apply V11 Flyway migration `vector-store/src/main/resources/db/migration/V11__arabic_search_enhancements.sql` - adds Arabic tsvector column, bilingual hybrid search functions, detected_language column, and GIN indexes
- [ ] T041 [P] Populate `detected_language` column in `VectorStoreService.storeBatch()` in `vector-store/src/main/java/com/bmc/rag/store/service/VectorStoreService.java` - add Unicode-based Arabic character detection

**Checkpoint**: JPA config is isolated for test slices. Bilingual search functions available. Language detection populates on insert.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and cleanup

- [ ] T035 Run full backend test suite: `./mvnw test` and verify all tests pass
- [ ] T036 Run full frontend test suite: `cd frontend/web-chat && npm test` and verify all tests pass
- [ ] T037 Verify CI workflow syntax with `act` or push to branch and check GitHub Actions
- [ ] T038 Run quickstart.md validation - follow all steps and verify they work

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: ALREADY COMPLETE - security fixes applied
- **US1 Security Tests (Phase 3)**: Depends on Phase 2 completion (done)
- **US2 CI/CD (Phase 4)**: Depends on Phase 1 (needs .github dir)
- **US3 Observability (Phase 5)**: No dependencies on other stories
- **US4 Test Coverage (Phase 6)**: Depends on Phase 1 (frontend test setup)
- **US5 Configuration (Phase 7)**: MOSTLY COMPLETE
- **Polish (Phase 8)**: Depends on all stories being complete

### User Story Dependencies

- **US1 (P1)**: Independent - validates already-applied security fixes
- **US2 (P1)**: Independent - CI/CD pipeline, only needs .github directory
- **US3 (P2)**: Independent - observability infrastructure
- **US4 (P2)**: Depends on Phase 1 for frontend test setup
- **US5 (P3)**: Independent - mostly complete

### Parallel Opportunities

- T002, T003, T004, T005 can all run in parallel (frontend test setup)
- T011, T012, T013 can all run in parallel (security tests in different files)
- T014 and T016 can run in parallel (different workflow files)
- T021, T022 can run in parallel (different health indicator files)
- T023, T024, T025, T026, T027 can all run in parallel (different test files)
- T028, T029, T030, T031, T032 can all run in parallel (different test files)

---

## Parallel Example: User Story 4 (Test Coverage)

```bash
# Launch all backend tests in parallel (different files):
Task: "Create OpenAiCompatibleControllerTest.java"
Task: "Create ChatControllerTest.java"
Task: "Create WebSocketChatControllerTest.java"
Task: "Create ConfirmationServiceTest.java"
Task: "Create RemedyIncidentToolTest.java"

# Launch all frontend tests in parallel (different files):
Task: "Create ChatMain.test.tsx"
Task: "Create MessageBubble.test.tsx"
Task: "Create InputArea.test.tsx"
Task: "Create CitationBlock.test.tsx"
Task: "Create ConfirmationPrompt.test.tsx"
```

---

## Implementation Strategy

### MVP First (User Stories 1 + 2)

1. Complete Phase 1: Setup (test infra scaffolding)
2. Phase 2 already done (security fixes)
3. Complete Phase 3: US1 - Security validation tests
4. Complete Phase 4: US2 - CI/CD pipeline
5. **STOP and VALIDATE**: Push to branch, verify CI runs, all security tests pass
6. This MVP delivers: secure codebase + automated CI

### Incremental Delivery

1. Setup + Foundational → Foundation ready (DONE)
2. Add US1 (security tests) + US2 (CI/CD) → Test + Deploy (MVP!)
3. Add US3 (observability) → Structured logs + health checks
4. Add US4 (test coverage) → Comprehensive test suite
5. Add US5 (config) → Environment portability (DONE)
6. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Phase 1 (Setup) takes 5 minutes - one developer
2. Once setup done:
   - Developer A: US1 (security tests) + US2 (CI/CD)
   - Developer B: US3 (observability)
   - Developer C: US4 backend tests
   - Developer D: US4 frontend tests
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Phase 2 (Foundational) is ALREADY COMPLETE - security fixes applied to branch
- Phase 7 (US5) is MOSTLY COMPLETE - configuration externalized
- Total: 41 tasks (5 setup, 5 foundational done, 3 US1, 3 US2, 6 US3, 10 US4, 2 US5, 3 data layer, 4 polish)
- Parallel opportunities: 29 of 41 tasks can run in parallel with others
