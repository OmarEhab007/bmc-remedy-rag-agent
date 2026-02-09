# Tasks: Backend Test Coverage to 85%

**Input**: Design documents from `/specs/002-backend-test-coverage/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/test-contract.md, quickstart.md

**Tests**: This feature IS test creation. Every task creates test classes. Tests are the primary deliverable.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story. Within each user story, tests are ordered: pure utilities → DTOs/models → service dependencies → services.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Backend modules**: `{module}/src/test/java/com/bmc/rag/{module-package}/`
- **remedy-connector**: `remedy-connector/src/test/java/com/bmc/rag/connector/`
- **vectorization-engine**: `vectorization-engine/src/test/java/com/bmc/rag/vectorization/`
- **vector-store**: `vector-store/src/test/java/com/bmc/rag/store/`
- **rag-service**: `rag-service/src/test/java/com/bmc/rag/agent/`
- **api-gateway**: `api-gateway/src/test/java/com/bmc/rag/api/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Add coverage tooling and verify existing tests pass before writing new tests

- [x] T001 Add JaCoCo Maven plugin to parent `pom.xml` with `prepare-agent`, `report`, and `report-aggregate` goals; set 85% overall / 75% per-module thresholds
- [x] T002 Verify all 16 existing tests pass by running `./mvnw test` and fix any pre-existing failures
- [x] T003 [P] Configure maven-surefire-plugin with `<forkCount>1C</forkCount>` for parallel test execution in parent `pom.xml`

**Checkpoint**: JaCoCo generates coverage reports, existing tests pass, parallel execution configured

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: No blocking foundational work needed - JaCoCo setup (Phase 1) is the only prerequisite. All user story phases depend only on Phase 1 completion.

**Note**: Phases 3-8 (user stories) are independent of each other and can proceed in parallel after Phase 1.

**Checkpoint**: Phase 1 complete - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Core RAG Service Business Logic (Priority: P1)

**Goal**: Bring rag-service module from 32% to 85%+ coverage by testing all business logic services, security filters, Damee services, caching, rate limiting, metrics, and memory management

**Independent Test**: Run `./mvnw test -pl rag-service` - all new and enhanced tests pass with meaningful assertions on business behavior

### Enhance Existing Tests

- [ ] T004 [US1] Enhance `ConfirmationServiceTest` - add concurrent access, expired actions, duplicate confirmations, and timeout edge cases in `rag-service/src/test/java/com/bmc/rag/agent/confirmation/ConfirmationServiceTest.java`
- [ ] T005 [US1] Enhance `SecureContentRetrieverTest` - add empty groups, no matching documents, metadata filtering correctness, and ReBAC bypass scenarios in `rag-service/src/test/java/com/bmc/rag/agent/retrieval/SecureContentRetrieverTest.java`
- [ ] T006 [US1] Enhance `QueryRewriterTest` - add Arabic query rewriting, multi-turn context, and empty history edge cases in `rag-service/src/test/java/com/bmc/rag/agent/retrieval/QueryRewriterTest.java`
- [ ] T007 [US1] Enhance `ReBACFilterTest` - add empty groups scenario, wildcard groups, null user context, and multiple group memberships in `rag-service/src/test/java/com/bmc/rag/agent/security/ReBACFilterTest.java`
- [ ] T008 [US1] Enhance `InputValidatorTest` - add more injection patterns (SQL, XSS, prompt injection), Arabic text passthrough, and boundary length tests in `rag-service/src/test/java/com/bmc/rag/agent/security/InputValidatorTest.java`
- [ ] T009 [US1] Enhance `RagAssistantServiceTest` - add query processing with context, empty retrieval results, streaming response, and error handling in `rag-service/src/test/java/com/bmc/rag/agent/service/RagAssistantServiceTest.java`
- [ ] T010 [US1] Enhance `AgenticAssistantServiceTest` - add intent classification, slot filling, tool delegation, and confirmation flow in `rag-service/src/test/java/com/bmc/rag/agent/service/AgenticAssistantServiceTest.java`
- [ ] T011 [US1] Enhance `PostgresChatMemoryStoreTest` - add session listing, message count, TTL expiry, and concurrent access in `rag-service/src/test/java/com/bmc/rag/agent/memory/PostgresChatMemoryStoreTest.java`
- [ ] T012 [US1] Enhance `ArabicTextProcessorTest` - add mixed Arabic/English, diacritics removal, normalization, and empty text handling in `rag-service/src/test/java/com/bmc/rag/agent/util/ArabicTextProcessorTest.java`

### New Test Classes

- [ ] T013 [P] [US1] Create `DameeServiceTest` testing service search, detail lookup, and category operations in `rag-service/src/test/java/com/bmc/rag/agent/damee/DameeServiceTest.java`
- [ ] T014 [P] [US1] Create `DameeServiceCatalogTest` testing markdown parsing, keyword scoring, hardcoded fallback, and search with category filter in `rag-service/src/test/java/com/bmc/rag/agent/damee/DameeServiceCatalogTest.java`
- [ ] T015 [P] [US1] Create `DameeIngestionServiceTest` testing chunk creation, async ingestion, and catalog refresh in `rag-service/src/test/java/com/bmc/rag/agent/damee/DameeIngestionServiceTest.java`
- [ ] T016 [P] [US1] Create `ServiceIntentMatcherTest` testing intent matching with English and Arabic queries in `rag-service/src/test/java/com/bmc/rag/agent/damee/ServiceIntentMatcherTest.java`
- [ ] T017 [P] [US1] Create `FieldCollectorTest` testing field collection, validation per FieldType, and required field enforcement in `rag-service/src/test/java/com/bmc/rag/agent/damee/FieldCollectorTest.java`
- [ ] T018 [P] [US1] Create `GuidedServiceCreatorTest` testing guided flow initiation and step-by-step field collection in `rag-service/src/test/java/com/bmc/rag/agent/damee/GuidedServiceCreatorTest.java`
- [ ] T019 [P] [US1] Create `WorkflowPreviewBuilderTest` testing preview generation, VIP bypass, and timeline estimation in `rag-service/src/test/java/com/bmc/rag/agent/damee/WorkflowPreviewBuilderTest.java`
- [ ] T020 [P] [US1] Create `ServiceFieldDefinitionTest` testing validate() for all FieldTypes (TEXT, SELECT, DATE, EMAIL, PHONE, NUMBER), getPrompt() bilingual output, and getFormattedPrompt() in `rag-service/src/test/java/com/bmc/rag/agent/damee/ServiceFieldDefinitionTest.java`
- [ ] T021 [P] [US1] Create `SemanticCacheServiceTest` testing cache hit/miss, cache key generation, and TTL expiry in `rag-service/src/test/java/com/bmc/rag/agent/cache/SemanticCacheServiceTest.java`
- [ ] T022 [P] [US1] Create `AgenticRateLimiterTest` testing isRateLimited under/at/over limit, recordAction, getRemainingTokens, clearRateLimitForUser, 1-hour TTL, and null userId in `rag-service/src/test/java/com/bmc/rag/agent/security/AgenticRateLimiterTest.java`
- [ ] T023 [P] [US1] Create `ChatMemoryRetentionSchedulerTest` testing triggerCleanup() delegation, custom days, and getRetentionDays() in `rag-service/src/test/java/com/bmc/rag/agent/memory/ChatMemoryRetentionSchedulerTest.java`
- [ ] T024 [P] [US1] Create `RagMetricsServiceTest` testing all counter methods, timer methods, getAverageGroundednessScore(), getCacheHitRate(), and getSnapshot() (15 fields) in `rag-service/src/test/java/com/bmc/rag/agent/metrics/RagMetricsServiceTest.java`
- [ ] T025 [US1] Create `RagConfigTest` testing system prompt identity fields, bilingual content, service categories, and prompt structure in `rag-service/src/test/java/com/bmc/rag/agent/config/RagConfigTest.java`

**Checkpoint**: rag-service module reaches 85%+ coverage. Run `./mvnw test -pl rag-service` - all tests pass.

---

## Phase 4: User Story 2 - LangChain4j Tools and System Prompt (Priority: P1)

**Goal**: Create comprehensive tests for all 3 @Tool classes and the AgenticSystemPrompt builder, verifying AI-Remedy integration contracts and prompt engineering correctness

**Independent Test**: Run `./mvnw test -pl rag-service -Dtest="RemedyIncidentToolTest,RemedyWorkOrderToolTest,DameeServiceToolTest,AgenticSystemPromptTest"` - all tool and prompt tests pass

### Implementation

- [ ] T026 [P] [US2] Create `RemedyIncidentToolTest` testing searchSimilarIncidents (happy path, empty results, rate limited), stageIncidentCreation (happy path, duplicate detected, vague summary), stageIncidentWithDetails (optional/all fields), listPendingIncidents, context management (set/get/clear ThreadLocal), extractIssueFromConversation, and problem indicator pattern matching in `rag-service/src/test/java/com/bmc/rag/agent/tools/RemedyIncidentToolTest.java`
- [ ] T027 [P] [US2] Create `RemedyWorkOrderToolTest` testing searchSimilarWorkOrders (happy path, empty results), stageWorkOrderCreation (happy path, type/priority validation), stageScheduledWorkOrder (date calculation, null defaults), listPendingWorkOrders, and context reflection (accessing RemedyIncidentTool's ThreadLocal) in `rag-service/src/test/java/com/bmc/rag/agent/tools/RemedyWorkOrderToolTest.java`
- [ ] T028 [P] [US2] Create `DameeServiceToolTest` testing all 7 tool methods: searchServices, getServiceDetails, listCategories, getServicesByCategory, matchUserIntent, startGuidedRequest, getServiceWorkflow; plus description truncation (150/80 chars), null service lookups, and category filtering in `rag-service/src/test/java/com/bmc/rag/agent/tools/DameeServiceToolTest.java`
- [ ] T029 [US2] Enhance `AgenticSystemPromptTest` testing prompt structure completeness (identity, intent classification, slot filling matrix, tool usage rules, bilingual English/Arabic, output formatting), all required sections present, and classification rules for INCIDENT vs WORK ORDER in `rag-service/src/test/java/com/bmc/rag/agent/prompt/AgenticSystemPromptTest.java`

**Checkpoint**: All 3 @Tool classes and system prompt builder have comprehensive tests. Each @Tool method has >= 3 test cases (happy path, error, validation).

---

## Phase 5: User Story 3 - Remedy Connector Business Logic (Priority: P2)

**Goal**: Bring remedy-connector module from 0% to 85%+ coverage by testing all connection management, creators, extractors, services, and utilities

**Independent Test**: Run `./mvnw test -pl remedy-connector` - all tests pass with mocked ARServerUser

### Pure Utilities (no dependencies)

- [ ] T030 [P] [US3] Create `QualifierBuilderTest` testing all builder methods (equals, like, dateAfter, dateBefore, in, isNotNull, isNull), AND/OR combination logic, special character escaping, static helpers (incrementalSyncQualifier, byParentId), and edge cases (null values, empty lists, zero epochs) in `remedy-connector/src/test/java/com/bmc/rag/connector/util/QualifierBuilderTest.java`

### DTOs and Models

- [ ] T031 [P] [US3] Create `CreationResultTest` testing toUserMessage() formatting, static factory methods success() and failure(), and builder in `remedy-connector/src/test/java/com/bmc/rag/connector/dto/CreationResultTest.java`
- [ ] T032 [P] [US3] Create `IncidentUpdateRequestTest` testing validation (status is Integer type, not String), required fields, and builder pattern in `remedy-connector/src/test/java/com/bmc/rag/connector/dto/IncidentUpdateRequestTest.java`
- [ ] T033 [P] [US3] Create `IncidentRecordTest` testing record construction, field accessors, and toUserMessage() if present in `remedy-connector/src/test/java/com/bmc/rag/connector/model/IncidentRecordTest.java`

### Connection Management

- [ ] T034 [US3] Create `ThreadLocalARContextTest` testing thread isolation (multiple threads, different contexts), connection creation/verification/refresh, retry logic with exponential backoff, cleanup via closeContext(), error detection for ARERR codes (90, 92, 93, 9251, 9252), and disabled Remedy scenario in `remedy-connector/src/test/java/com/bmc/rag/connector/connection/ThreadLocalARContextTest.java`

### Creators (depend on ThreadLocalARContext mock)

- [ ] T035 [P] [US3] Create `IncidentCreatorTest` testing field ID mapping for all mandatory/optional fields, validation (impact/urgency 1-4, required fields), successful creation with CreationResult return, and error handling (AR exceptions, missing incident number) in `remedy-connector/src/test/java/com/bmc/rag/connector/creator/IncidentCreatorTest.java`
- [ ] T036 [P] [US3] Create `IncidentUpdaterTest` testing update field mapping, work log addition, validation, status transition, and error handling in `remedy-connector/src/test/java/com/bmc/rag/connector/creator/IncidentUpdaterTest.java`
- [ ] T037 [P] [US3] Create `WorkOrderCreatorTest` testing field ID mapping, date validation, type/priority mapping, successful creation, and error handling in `remedy-connector/src/test/java/com/bmc/rag/connector/creator/WorkOrderCreatorTest.java`

### Extractors (depend on ThreadLocalARContext mock)

- [ ] T038 [P] [US3] Create `IncidentExtractorTest` testing getListEntryObjects mock → Entry list, Entry.get(fieldId) → Value objects, pagination logic (firstRetrieve/maxRetrieve), field mapping to IncidentRecord, null handling for optional fields, and timestamp conversion (Unix epoch → Instant) in `remedy-connector/src/test/java/com/bmc/rag/connector/extractor/IncidentExtractorTest.java`
- [ ] T039 [P] [US3] Create `WorkOrderExtractorTest` testing extraction, field mapping, pagination, and qualification building in `remedy-connector/src/test/java/com/bmc/rag/connector/extractor/WorkOrderExtractorTest.java`
- [ ] T040 [P] [US3] Create `ChangeRequestExtractorTest` testing extraction, field mapping, and filtered queries in `remedy-connector/src/test/java/com/bmc/rag/connector/extractor/ChangeRequestExtractorTest.java`
- [ ] T041 [P] [US3] Create `KnowledgeExtractorTest` testing knowledge article extraction and field mapping in `remedy-connector/src/test/java/com/bmc/rag/connector/extractor/KnowledgeExtractorTest.java`
- [ ] T042 [P] [US3] Create `WorkLogExtractorTest` testing work log extraction linked by incident number in `remedy-connector/src/test/java/com/bmc/rag/connector/extractor/WorkLogExtractorTest.java`
- [ ] T043 [P] [US3] Create `AttachmentExtractorTest` testing attachment metadata extraction and blob retrieval mocking in `remedy-connector/src/test/java/com/bmc/rag/connector/extractor/AttachmentExtractorTest.java`

### Services (depend on extractor mocks)

- [ ] T044 [US3] Create `ExtractionOrchestratorTest` testing circuit breaker (5 failures → open, 5 min recovery), concurrent extraction locking (AtomicBoolean), progress callback invocation, batch extraction across all record types, and error recovery in `remedy-connector/src/test/java/com/bmc/rag/connector/service/ExtractionOrchestratorTest.java`
- [ ] T045 [P] [US3] Create `RemedyUserServiceTest` testing group membership lookup and user verification in `remedy-connector/src/test/java/com/bmc/rag/connector/service/RemedyUserServiceTest.java`
- [ ] T046 [P] [US3] Create `WorkLogServiceTest` testing batch work log retrieval by incident numbers in `remedy-connector/src/test/java/com/bmc/rag/connector/service/WorkLogServiceTest.java`

**Checkpoint**: remedy-connector module reaches 85%+ coverage. Run `./mvnw test -pl remedy-connector` - all tests pass.

---

## Phase 6: User Story 4 - Vectorization and Chunking Logic (Priority: P2)

**Goal**: Bring vectorization-engine module from 0% to 85%+ coverage by testing chunking strategies, embedding service, and attachment parsing

**Independent Test**: Run `./mvnw test -pl vectorization-engine` - all tests pass

### Pure Utilities (no dependencies)

- [ ] T047 [P] [US4] Create `TextChunkTest` testing buildITSMMetadata() with various field combinations, generateChunkId() format ("sourceType:sourceId:chunkType:sequence"), addMetadata() fluent API, and getContentLength() in `vectorization-engine/src/test/java/com/bmc/rag/vectorization/chunking/TextChunkTest.java`

### Chunking Engine

- [ ] T048 [US4] Create `SemanticChunkerTest` testing paragraph splitting (double newline), sentence splitting (period + capital), context prefix injection, overlap calculation, hard splits for oversized sentences, and edge cases (null, empty, single-word, exactly-max-size text) in `vectorization-engine/src/test/java/com/bmc/rag/vectorization/chunking/SemanticChunkerTest.java`

### Chunk Strategies (depend on SemanticChunker)

- [ ] T049 [P] [US4] Create `IncidentChunkStrategyTest` testing chunk creation with realistic IncidentRecord, metadata population (ITSM fields), high-value chunk marking for Resolution field, work log grouping by day, and null/empty field handling in `vectorization-engine/src/test/java/com/bmc/rag/vectorization/chunking/IncidentChunkStrategyTest.java`
- [ ] T050 [P] [US4] Create `ChangeRequestChunkStrategyTest` testing chunk creation, metadata for change requests, implementation plan as high-value chunk, and edge cases in `vectorization-engine/src/test/java/com/bmc/rag/vectorization/chunking/ChangeRequestChunkStrategyTest.java`
- [ ] T051 [P] [US4] Create `KnowledgeChunkStrategyTest` testing chunk creation, HTML cleaning (BR, P, DIV tags → newlines), article metadata, and edge cases in `vectorization-engine/src/test/java/com/bmc/rag/vectorization/chunking/KnowledgeChunkStrategyTest.java`
- [ ] T052 [P] [US4] Create `WorkOrderChunkStrategyTest` testing chunk creation, work order metadata, and edge cases in `vectorization-engine/src/test/java/com/bmc/rag/vectorization/chunking/WorkOrderChunkStrategyTest.java`

### Embedding Service

- [ ] T053 [US4] Create `LocalEmbeddingServiceTest` testing embed() produces 384-dimensional float array, embedBatch() with batch size boundaries, cosineSimilarity() with known vectors, zero vector handling, and null/empty text handling in `vectorization-engine/src/test/java/com/bmc/rag/vectorization/embedding/LocalEmbeddingServiceTest.java`

### Attachment Parsing

- [ ] T054 [US4] Create `AttachmentParserTest` testing parse(Path) with supported file types using temp files, MIME type detection and filtering, file size validation (>50MB rejected), timeout protection, unsupported MIME types, and parse(InputStream, filename, mimeType) overload in `vectorization-engine/src/test/java/com/bmc/rag/vectorization/tika/AttachmentParserTest.java`

**Checkpoint**: vectorization-engine module reaches 85%+ coverage. Run `./mvnw test -pl vectorization-engine` - all tests pass.

---

## Phase 7: User Story 5 - API Gateway Controllers and Services (Priority: P3)

**Goal**: Bring api-gateway module from 4% to 85%+ coverage by testing all controllers, filters, services, health indicators, and configuration classes

**Independent Test**: Run `./mvnw test -pl api-gateway` - all tests pass with mocked downstream services

### Enhance Existing Tests

- [ ] T055 [US5] Enhance `ChatControllerTest` - add more scenarios (empty request, streaming, error responses) in `api-gateway/src/test/java/com/bmc/rag/api/controller/ChatControllerTest.java`
- [ ] T056 [US5] Enhance `OpenAiCompatibleControllerTest` - add streaming tests, model listing, error handling in `api-gateway/src/test/java/com/bmc/rag/api/controller/OpenAiCompatibleControllerTest.java`

### New Controller Tests (all @WebMvcTest pattern)

- [ ] T057 [P] [US5] Create `ToolServerControllerTest` testing search, create, confirm endpoints with mocked tool services; verify JSON response structure and HTTP status codes in `api-gateway/src/test/java/com/bmc/rag/api/controller/ToolServerControllerTest.java`
- [ ] T058 [P] [US5] Create `IngestionControllerTest` testing sync trigger, status check endpoints in `api-gateway/src/test/java/com/bmc/rag/api/controller/IngestionControllerTest.java`
- [ ] T059 [P] [US5] Create `AdminControllerTest` testing admin operations endpoints in `api-gateway/src/test/java/com/bmc/rag/api/controller/AdminControllerTest.java`
- [ ] T060 [P] [US5] Create `FeedbackControllerTest` testing submit feedback and list feedback endpoints in `api-gateway/src/test/java/com/bmc/rag/api/controller/FeedbackControllerTest.java`
- [ ] T061 [P] [US5] Create `HealthControllerTest` testing /health, /ready, /live endpoints with mocked JdbcTemplate in `api-gateway/src/test/java/com/bmc/rag/api/controller/HealthControllerTest.java`
- [ ] T062 [P] [US5] Create `MetricsControllerTest` testing /rag, /summary, /latency, /quality, /cache endpoints with mocked RagMetricsService in `api-gateway/src/test/java/com/bmc/rag/api/controller/MetricsControllerTest.java`
- [ ] T063 [P] [US5] Create `WebSocketChatControllerTest` testing STOMP message handling with mocked RagAssistantService and SimpMessagingTemplate in `api-gateway/src/test/java/com/bmc/rag/api/controller/WebSocketChatControllerTest.java`
- [ ] T064 [P] [US5] Create `TeamsBotControllerTest` testing webhook endpoint with mocked TeamsBotHandler in `api-gateway/src/test/java/com/bmc/rag/api/controller/TeamsBotControllerTest.java`

### Service Tests

- [ ] T065 [P] [US5] Create `FeedbackServiceTest` testing persist feedback, retrieve, and validation in `api-gateway/src/test/java/com/bmc/rag/api/service/FeedbackServiceTest.java`
- [ ] T066 [P] [US5] Create `ToolIntentDetectorTest` testing intent detection patterns for incident vs work order vs service request in `api-gateway/src/test/java/com/bmc/rag/api/service/ToolIntentDetectorTest.java`

### Filter Tests

- [ ] T067 [P] [US5] Create `CorrelationIdFilterTest` testing UUID generation when absent, forwarding existing correlation ID, and header propagation in `api-gateway/src/test/java/com/bmc/rag/api/filter/CorrelationIdFilterTest.java`
- [ ] T068 [P] [US5] Create `RateLimitFilterTest` testing allow under limit, block over limit with 429 response, and bucket refill in `api-gateway/src/test/java/com/bmc/rag/api/filter/RateLimitFilterTest.java`
- [ ] T069 [P] [US5] Create `ARContextCleanupFilterTest` testing always cleans up context, skips non-API paths, and cleanup on exception in `api-gateway/src/test/java/com/bmc/rag/api/filter/ARContextCleanupFilterTest.java`

### Health Indicators

- [ ] T070 [P] [US5] Create `LlmHealthIndicatorTest` testing up when model exists, down when null, and down on error in `api-gateway/src/test/java/com/bmc/rag/api/health/LlmHealthIndicatorTest.java`
- [ ] T071 [P] [US5] Create `EmbeddingHealthIndicatorTest` testing up when embed succeeds and down on failure in `api-gateway/src/test/java/com/bmc/rag/api/health/EmbeddingHealthIndicatorTest.java`

### Config and Utility Tests

- [ ] T072 [P] [US5] Create `RateLimitConfigTest` testing bucket creation per endpoint type, cleanup logic, and configuration binding in `api-gateway/src/test/java/com/bmc/rag/api/config/RateLimitConfigTest.java`
- [ ] T073 [P] [US5] Create `SecurityConfigTest` testing conditional security chains (enabled vs disabled profiles) in `api-gateway/src/test/java/com/bmc/rag/api/config/SecurityConfigTest.java`
- [ ] T074 [P] [US5] Create `GlobalExceptionHandlerTest` testing exception → structured error response mapping for all handled exception types in `api-gateway/src/test/java/com/bmc/rag/api/exception/GlobalExceptionHandlerTest.java`
- [ ] T075 [P] [US5] Create `MdcExecutorServiceTest` testing MDC context propagation across threads, submit, and execute in `api-gateway/src/test/java/com/bmc/rag/api/util/MdcExecutorServiceTest.java`

### Teams Integration Tests

- [ ] T076 [P] [US5] Create `TeamsBotHandlerTest` testing webhook processing and message delegation in `api-gateway/src/test/java/com/bmc/rag/api/integration/teams/TeamsBotHandlerTest.java`
- [ ] T077 [P] [US5] Create `TeamsBotAuthenticatorTest` testing HMAC signature verification (valid, invalid, missing) in `api-gateway/src/test/java/com/bmc/rag/api/integration/teams/TeamsBotAuthenticatorTest.java`

**Checkpoint**: api-gateway module reaches 85%+ coverage. Run `./mvnw test -pl api-gateway` - all tests pass.

---

## Phase 8: User Story 6 - Vector Store Services and CDC Sync (Priority: P3)

**Goal**: Bring vector-store module from 18% to 85%+ coverage by testing entity state transitions, sync services, and enhancing existing tests

**Independent Test**: Run `./mvnw test -pl vector-store` - all tests pass with mocked repositories

### Enhance Existing Tests

- [ ] T078 [US6] Enhance `VectorStoreServiceTest` - add edge cases (empty results, null embeddings, batch operations) in `vector-store/src/test/java/com/bmc/rag/store/service/VectorStoreServiceTest.java`
- [ ] T079 [US6] Enhance `HybridSearchServiceTest` - add edge cases (keyword-only, semantic-only, combined scoring, empty queries) in `vector-store/src/test/java/com/bmc/rag/store/service/HybridSearchServiceTest.java`

### Entity Tests

- [ ] T080 [P] [US6] Create `ActionAuditEntityTest` testing forStaged() factory, markExecuted/Failed/Cancelled/Expired state transitions, and timestamp tracking in `vector-store/src/test/java/com/bmc/rag/store/entity/ActionAuditEntityTest.java`
- [ ] T081 [P] [US6] Create `SyncStateEntityTest` testing markSyncCompleted/Running/Failed state machine and cursor management in `vector-store/src/test/java/com/bmc/rag/store/entity/SyncStateEntityTest.java`

### Service Tests

- [ ] T082 [US6] Create `EmbeddingRefreshServiceTest` testing refresh triggers, vector recomputation with mocked repos, and partial refresh scenarios in `vector-store/src/test/java/com/bmc/rag/store/service/EmbeddingRefreshServiceTest.java`
- [ ] T083 [US6] Create `IncrementalSyncServiceTest` testing sync cycle (extract → remove old → re-index → update cursor), timestamp cursor management, empty extraction results, and partial failure handling in `vector-store/src/test/java/com/bmc/rag/store/sync/IncrementalSyncServiceTest.java`

**Checkpoint**: vector-store module reaches 85%+ coverage. Run `./mvnw test -pl vector-store` - all tests pass.

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Verify overall coverage, fix gaps, and integrate into CI

- [ ] T084 Run full test suite with JaCoCo via `./mvnw clean verify` and generate per-module and aggregate coverage reports
- [ ] T085 Identify remaining low-coverage files from JaCoCo reports and add targeted tests for uncovered branches
- [ ] T086 [P] Add JaCoCo report generation to CI workflow in `.github/workflows/` with coverage threshold enforcement (fail build if < 85%)
- [ ] T087 Run quickstart.md validation - verify all documented test commands work correctly
- [ ] T088 Final coverage verification - confirm each module >= 75% and aggregate >= 85%

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - start immediately
- **Foundational (Phase 2)**: N/A (no foundational work beyond Phase 1)
- **User Stories (Phases 3-8)**: All depend on Phase 1 completion only
  - Phases 3-8 can proceed in parallel (different modules)
  - Or sequentially in priority order: Phase 3+4 (P1) → Phase 5+6 (P2) → Phase 7+8 (P3)
- **Polish (Phase 9)**: Depends on all user stories (Phases 3-8) being complete

### User Story Dependencies

- **US1 - Core RAG Service (P1)**: Can start after Phase 1 - No dependencies on other stories
- **US2 - Tools & Prompt (P1)**: Can start after Phase 1 - Same module as US1 but tests different classes, can run in parallel
- **US3 - Remedy Connector (P2)**: Can start after Phase 1 - Independent module, fully parallel with US1/US2
- **US4 - Vectorization (P2)**: Can start after Phase 1 - Independent module, fully parallel with all others
- **US5 - API Gateway (P3)**: Can start after Phase 1 - Independent module, fully parallel with all others
- **US6 - Vector Store (P3)**: Can start after Phase 1 - Independent module, fully parallel with all others

### Within Each User Story

- Pure utilities/DTOs before service tests
- Enhance existing tests before creating new ones (avoid merge conflicts)
- Service dependency mocks must be understood before service tests
- Run module tests after each completed story phase

### Parallel Opportunities

- All tasks marked [P] within a phase can run in parallel
- All 6 user story phases (3-8) can run in parallel after Phase 1
- Within Phase 5 (remedy-connector): T030-T033 (utils/DTOs) in parallel, then T034 (ThreadLocal), then T035-T043 (creators/extractors) in parallel, then T044-T046 (services)
- Within Phase 6 (vectorization): T047 (TextChunk) in parallel with T048 (SemanticChunker), then T049-T052 (strategies) in parallel, then T053-T054
- Within Phase 7 (api-gateway): All controller tests (T057-T064) in parallel, all filter tests (T067-T069) in parallel, all health/config tests (T070-T077) in parallel

---

## Parallel Example: User Story 3 (Remedy Connector)

```bash
# Launch all pure utility/DTO tests in parallel:
Task: "Create QualifierBuilderTest in remedy-connector/.../util/QualifierBuilderTest.java"
Task: "Create CreationResultTest in remedy-connector/.../dto/CreationResultTest.java"
Task: "Create IncidentUpdateRequestTest in remedy-connector/.../dto/IncidentUpdateRequestTest.java"
Task: "Create IncidentRecordTest in remedy-connector/.../model/IncidentRecordTest.java"

# Then ThreadLocalARContext (dependency for creators/extractors):
Task: "Create ThreadLocalARContextTest in remedy-connector/.../connection/ThreadLocalARContextTest.java"

# Then all creators and extractors in parallel:
Task: "Create IncidentCreatorTest in remedy-connector/.../creator/IncidentCreatorTest.java"
Task: "Create IncidentUpdaterTest in remedy-connector/.../creator/IncidentUpdaterTest.java"
Task: "Create WorkOrderCreatorTest in remedy-connector/.../creator/WorkOrderCreatorTest.java"
Task: "Create IncidentExtractorTest in remedy-connector/.../extractor/IncidentExtractorTest.java"
# ... (all extractors in parallel)

# Finally services:
Task: "Create ExtractionOrchestratorTest in remedy-connector/.../service/ExtractionOrchestratorTest.java"
```

## Parallel Example: User Story 5 (API Gateway)

```bash
# Launch all controller tests in parallel (different files, no deps):
Task: "Create ToolServerControllerTest in api-gateway/.../controller/ToolServerControllerTest.java"
Task: "Create IngestionControllerTest in api-gateway/.../controller/IngestionControllerTest.java"
Task: "Create AdminControllerTest in api-gateway/.../controller/AdminControllerTest.java"
Task: "Create FeedbackControllerTest in api-gateway/.../controller/FeedbackControllerTest.java"
# ... (all controllers, services, filters, health, config in parallel)
```

---

## Implementation Strategy

### MVP First (User Stories 1 + 2 Only)

1. Complete Phase 1: JaCoCo Setup
2. Complete Phase 3: US1 - Core RAG Service (enhance + new)
3. Complete Phase 4: US2 - Tools & System Prompt
4. **STOP and VALIDATE**: Run `./mvnw test -pl rag-service` - verify rag-service at 85%+
5. The most critical module (rag-service) is now fully tested

### Incremental Delivery

1. Phase 1 → JaCoCo ready
2. Phases 3+4 → rag-service at 85%+ (MVP - covers P1 stories)
3. Phases 5+6 → remedy-connector + vectorization-engine at 85%+ (P2 stories)
4. Phases 7+8 → api-gateway + vector-store at 85%+ (P3 stories)
5. Phase 9 → Polish, verify aggregate 85%+, CI integration

### Parallel Team Strategy

With multiple developers:

1. All complete Phase 1 together
2. Once Phase 1 is done:
   - Developer A: Phases 3+4 (rag-service - US1 + US2)
   - Developer B: Phase 5 (remedy-connector - US3)
   - Developer C: Phase 6 (vectorization-engine - US4)
   - Developer D: Phase 7 (api-gateway - US5)
   - Developer E: Phase 8 (vector-store - US6)
3. All modules tested independently, merge and verify aggregate

---

## Summary

| Metric | Count |
|--------|-------|
| **Total Tasks** | 88 |
| **Phase 1 (Setup)** | 3 |
| **Phase 3 (US1 - Core RAG)** | 22 |
| **Phase 4 (US2 - Tools/Prompt)** | 4 |
| **Phase 5 (US3 - Remedy Connector)** | 17 |
| **Phase 6 (US4 - Vectorization)** | 8 |
| **Phase 7 (US5 - API Gateway)** | 23 |
| **Phase 8 (US6 - Vector Store)** | 6 |
| **Phase 9 (Polish)** | 5 |
| **Parallelizable Tasks** | 62 (70%) |
| **New Test Classes** | ~70 |
| **Enhanced Test Classes** | ~15 |
| **Estimated Test Methods** | ~500+ |

---

## Notes

- [P] tasks = different files, no dependencies - safe to run in parallel
- [Story] label maps task to specific user story for traceability
- Each module can be tested independently: `./mvnw test -pl {module}`
- Use builder pattern for Lombok DTOs in test data (constructors are package-private)
- All controller tests use @WebMvcTest with `excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class`
- All controller tests need `@MockBean` for `ThreadLocalARContext` and `RateLimitConfig`
- Naming convention: `methodName_scenario_expectedResult()`
- Target: < 3 seconds per test class, < 5 minutes total suite
