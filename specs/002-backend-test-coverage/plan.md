# Implementation Plan: Backend Test Coverage to 85%

**Branch**: `002-backend-test-coverage` | **Date**: 2026-02-07 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/002-backend-test-coverage/spec.md`

## Summary

Increase backend test coverage from ~11% (16 tests / 148 source files) to 85%+ across all 5 Java modules. Tests focus on business logic correctness with meaningful assertions, using Mockito for external dependencies (BMC AR API, database, LLM providers). Key areas: LangChain4j @Tool methods, system prompt, Remedy connector, vectorization chunking, and API gateway controllers. All tests run without infrastructure.

## Technical Context

**Language/Version**: Java 17 (Spring Boot 3.2)
**Primary Dependencies**: LangChain4j 1.0.0-beta2, Spring Boot Starter Test (JUnit 5 + Mockito + AssertJ), BMC AR API 91.9
**Storage**: PostgreSQL 16 + pgvector (mocked in tests via Mockito)
**Testing**: JUnit 5, Mockito (MockitoExtension), AssertJ, Spring MockMvc (@WebMvcTest), JaCoCo for coverage
**Target Platform**: Linux server (tests run on any JVM)
**Project Type**: Multi-module Maven (5 modules)
**Performance Goals**: Full unit test suite < 5 minutes
**Constraints**: No external infrastructure required (no DB, no Remedy server, no LLM API)
**Scale/Scope**: ~95 new test classes, ~500+ test methods, covering 132 previously untested source files

## Constitution Check

*GATE: Constitution is a template (not yet configured for this project). No specific gates to enforce. Proceeding.*

**Post-design re-check**: N/A - no constitution violations possible.

## Project Structure

### Documentation (this feature)

```text
specs/002-backend-test-coverage/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0 research output
├── data-model.md        # Phase 1 test architecture model
├── quickstart.md        # Phase 1 quickstart guide
├── checklists/
│   └── requirements.md  # Spec validation checklist
└── tasks.md             # Phase 2 output (via /speckit.tasks)
```

### Source Code (repository root)

```text
remedy-connector/src/test/java/com/bmc/rag/connector/
├── connection/
│   └── ThreadLocalARContextTest.java
├── creator/
│   ├── IncidentCreatorTest.java
│   ├── IncidentUpdaterTest.java
│   └── WorkOrderCreatorTest.java
├── extractor/
│   ├── IncidentExtractorTest.java
│   ├── WorkOrderExtractorTest.java
│   ├── ChangeRequestExtractorTest.java
│   ├── KnowledgeExtractorTest.java
│   ├── WorkLogExtractorTest.java
│   └── AttachmentExtractorTest.java
├── service/
│   ├── ExtractionOrchestratorTest.java
│   ├── RemedyUserServiceTest.java
│   └── WorkLogServiceTest.java
├── util/
│   └── QualifierBuilderTest.java
├── dto/
│   ├── CreationResultTest.java
│   └── IncidentUpdateRequestTest.java
└── model/
    └── IncidentRecordTest.java

vectorization-engine/src/test/java/com/bmc/rag/vectorization/
├── chunking/
│   ├── SemanticChunkerTest.java
│   ├── IncidentChunkStrategyTest.java
│   ├── ChangeRequestChunkStrategyTest.java
│   ├── KnowledgeChunkStrategyTest.java
│   ├── WorkOrderChunkStrategyTest.java
│   └── TextChunkTest.java
├── embedding/
│   └── LocalEmbeddingServiceTest.java
└── tika/
    └── AttachmentParserTest.java

vector-store/src/test/java/com/bmc/rag/store/
├── service/
│   ├── VectorStoreServiceTest.java        # EXISTS - enhance
│   ├── HybridSearchServiceTest.java       # EXISTS - enhance
│   └── EmbeddingRefreshServiceTest.java
├── sync/
│   └── IncrementalSyncServiceTest.java
└── entity/
    ├── ActionAuditEntityTest.java
    └── SyncStateEntityTest.java

rag-service/src/test/java/com/bmc/rag/agent/
├── tools/
│   ├── RemedyIncidentToolTest.java
│   ├── RemedyWorkOrderToolTest.java
│   └── DameeServiceToolTest.java
├── cache/
│   └── SemanticCacheServiceTest.java
├── damee/
│   ├── DameeServiceTest.java
│   ├── DameeIngestionServiceTest.java
│   ├── DameeServiceCatalogTest.java
│   ├── ServiceIntentMatcherTest.java
│   ├── FieldCollectorTest.java
│   ├── GuidedServiceCreatorTest.java
│   ├── WorkflowPreviewBuilderTest.java
│   └── ServiceFieldDefinitionTest.java
├── memory/
│   ├── PostgresChatMemoryStoreTest.java   # EXISTS - enhance
│   └── ChatMemoryRetentionSchedulerTest.java
├── metrics/
│   └── RagMetricsServiceTest.java
├── security/
│   ├── InputValidatorTest.java            # EXISTS - enhance
│   ├── ReBACFilterTest.java               # EXISTS - enhance
│   └── AgenticRateLimiterTest.java
├── config/
│   └── RagConfigTest.java
├── confirmation/
│   └── ConfirmationServiceTest.java       # EXISTS - enhance
├── prompt/
│   └── AgenticSystemPromptTest.java       # EXISTS - enhance
├── retrieval/
│   ├── SecureContentRetrieverTest.java    # EXISTS - enhance
│   └── QueryRewriterTest.java            # EXISTS - enhance
├── service/
│   ├── RagAssistantServiceTest.java       # EXISTS - enhance
│   └── AgenticAssistantServiceTest.java   # EXISTS - enhance
└── util/
    └── ArabicTextProcessorTest.java       # EXISTS - enhance

api-gateway/src/test/java/com/bmc/rag/api/
├── controller/
│   ├── ChatControllerTest.java            # EXISTS - enhance
│   ├── OpenAiCompatibleControllerTest.java # EXISTS - enhance
│   ├── ActionControllerIntegrationTest.java # EXISTS
│   ├── ToolServerControllerTest.java
│   ├── IngestionControllerTest.java
│   ├── AdminControllerTest.java
│   ├── FeedbackControllerTest.java
│   ├── HealthControllerTest.java
│   ├── MetricsControllerTest.java
│   └── WebSocketChatControllerTest.java
├── service/
│   ├── FeedbackServiceTest.java
│   └── ToolIntentDetectorTest.java
├── filter/
│   ├── CorrelationIdFilterTest.java
│   ├── RateLimitFilterTest.java
│   └── ARContextCleanupFilterTest.java
├── health/
│   ├── LlmHealthIndicatorTest.java
│   └── EmbeddingHealthIndicatorTest.java
├── config/
│   ├── RateLimitConfigTest.java
│   └── SecurityConfigTest.java
├── exception/
│   └── GlobalExceptionHandlerTest.java
├── integration/teams/
│   ├── TeamsBotControllerTest.java
│   ├── TeamsBotHandlerTest.java
│   └── TeamsBotAuthenticatorTest.java
└── util/
    └── MdcExecutorServiceTest.java
```

**Structure Decision**: Tests follow existing Maven convention - each module has `src/test/java` mirroring the `src/main/java` package structure. No new modules needed.

## Implementation Phases

### Phase 0: Infrastructure Setup

**0.1 Add JaCoCo plugin to parent POM**
- Add `jacoco-maven-plugin` to `<build><plugins>` in parent POM
- Configure `prepare-agent` and `report` goals
- Set minimum coverage thresholds: 85% overall, 75% per module
- Add `report-aggregate` for combined coverage view

**0.2 Verify existing tests pass**
- Run `./mvnw test` to confirm all 16 existing tests pass
- Fix any pre-existing failures before adding new tests

### Phase 1: Remedy Connector Module (0% → 85%+)

**1.1 QualifierBuilder tests** (pure utility, zero dependencies)
- Test all builder methods: equals, like, dateAfter, dateBefore, in, isNotNull, isNull
- Test AND/OR combination logic
- Test special character escaping
- Test static helpers: incrementalSyncQualifier, byParentId
- Test edge cases: null values, empty lists, zero epochs

**1.2 CreationResult and DTO tests**
- Test toUserMessage() formatting
- Test static factory methods: success(), failure()
- Test IncidentUpdateRequest validation (status is Integer)
- Test WorkOrderCreationRequest date validation

**1.3 ThreadLocalARContext tests**
- Mock ARServerUser and RemedyConnectionConfig
- Test thread isolation (multiple threads, different contexts)
- Test connection creation, verification, refresh
- Test retry logic with exponential backoff
- Test cleanup via closeContext()
- Test error detection for ARERR codes (90, 92, 93, 9251, 9252)
- Test disabled Remedy scenario

**1.4 Incident/WorkOrder/ChangeRequest Creator tests**
- Mock ThreadLocalARContext → ARServerUser
- Test field ID mapping for all mandatory and optional fields
- Test validation methods (impact/urgency 1-4, required fields)
- Test successful creation and CreationResult return
- Test error handling (AR exceptions, missing incident number)
- Test work log addition (IncidentUpdater)

**1.5 Extractor tests** (IncidentExtractor, WorkOrderExtractor, etc.)
- Mock ARServerUser.getListEntryObjects() → return mock Entry lists
- Mock Entry.get(fieldId) → return type-specific Value objects
- Test pagination logic (firstRetrieve/maxRetrieve)
- Test field mapping from Entry to domain model
- Test null handling for optional fields
- Test timestamp conversion (Unix epoch int → Instant)
- Test qualification building for filtered queries

**1.6 Service tests** (ExtractionOrchestrator, RemedyUserService, WorkLogService)
- Test ExtractionOrchestrator circuit breaker (5 failures → open, 5 min recovery)
- Test concurrent extraction locking (AtomicBoolean flag)
- Test progress callback invocation
- Test batch work log fetching
- Test RemedyUserService group membership lookup
- Test WorkLogService batch retrieval by incident numbers

### Phase 2: Vectorization Engine Module (0% → 85%+)

**2.1 TextChunk tests**
- Test buildITSMMetadata() with various field combinations
- Test generateChunkId() format: "sourceType:sourceId:chunkType:sequence"
- Test addMetadata() fluent API
- Test getContentLength()

**2.2 SemanticChunker tests**
- Test paragraph splitting (double newline)
- Test sentence splitting (period + capital)
- Test context prefix injection
- Test overlap calculation
- Test hard splits for oversized sentences
- Test edge cases: null, empty, single-word, exactly-max-size text

**2.3 ChunkStrategy tests** (Incident, ChangeRequest, Knowledge, WorkOrder)
- Test chunk creation for each strategy with realistic domain objects
- Test metadata population (ITSM-specific fields)
- Test high-value chunk marking (Resolution, Implementation plans)
- Test work log grouping by day
- Test null/empty field handling
- Test KnowledgeChunkStrategy HTML cleaning (BR, P, DIV tags → newlines)

**2.4 LocalEmbeddingService tests**
- Test embed() produces 384-dimensional float array
- Test embedBatch() with batch size boundaries
- Test cosineSimilarity() with known vectors
- Test zero vector handling
- Test null/empty text handling

**2.5 AttachmentParser tests**
- Test parse(Path) with supported file types
- Test MIME type detection and filtering
- Test file size validation (>50MB rejected)
- Test timeout protection
- Test unsupported MIME types
- Test parse(InputStream, filename, mimeType) overload

### Phase 3: RAG Service Module (32% → 85%+)

**3.1 LangChain4j Tool tests** (CRITICAL - P1)

**RemedyIncidentToolTest**:
- Test searchSimilarIncidents: happy path, empty results, rate limited
- Test stageIncidentCreation: happy path, duplicate detected, vague summary
- Test stageIncidentWithDetails: optional fields, all fields
- Test listPendingIncidents: with/without pending actions
- Test context management: setContext, getContext, clearContext (ThreadLocal)
- Test extractIssueFromConversation: various conversation patterns
- Test problem indicator pattern matching

**RemedyWorkOrderToolTest**:
- Test searchSimilarWorkOrders: happy path, empty results
- Test stageWorkOrderCreation: happy path, type/priority validation
- Test stageScheduledWorkOrder: date calculation, null defaults
- Test listPendingWorkOrders
- Test context reflection (accessing RemedyIncidentTool's ThreadLocal)

**DameeServiceToolTest**:
- Test all 7 tool methods: searchServices, getServiceDetails, listCategories, getServicesByCategory, matchUserIntent, startGuidedRequest, getServiceWorkflow
- Test description truncation (150 chars search, 80 chars category)
- Test null service lookups
- Test category filtering

**3.2 Damee integration tests**
- DameeServiceCatalogTest: markdown parsing, keyword scoring, hardcoded fallback, search with category filter
- ServiceIntentMatcherTest: intent matching with English/Arabic queries
- FieldCollectorTest: field collection and validation
- GuidedServiceCreatorTest: guided flow initiation
- WorkflowPreviewBuilderTest: preview generation, VIP bypass, timeline estimation
- ServiceFieldDefinitionTest: validate() for all FieldTypes (TEXT, SELECT, DATE, EMAIL, PHONE, NUMBER), getPrompt() bilingual, getFormattedPrompt()
- DameeIngestionServiceTest: chunk creation, async ingestion, refresh catalog

**3.3 SemanticCacheService tests**
- Test cache hit/miss logic
- Test cache key generation
- Test TTL expiry

**3.4 AgenticRateLimiter tests**
- Test isRateLimited with counter under/at/over limit
- Test recordAction increments counter
- Test getRemainingTokens
- Test clearRateLimitForUser
- Test 1-hour TTL behavior
- Test blank/null userId handling

**3.5 ChatMemoryRetentionScheduler tests**
- Test triggerCleanup() delegates to store
- Test triggerCleanup(customDays)
- Test getRetentionDays()

**3.6 RagMetricsService tests**
- Test all counter methods (recordRetrieval, recordCitations, etc.)
- Test all timer methods (recordRetrievalLatency, etc.)
- Test getAverageGroundednessScore() calculation
- Test getCacheHitRate() calculation
- Test getSnapshot() returns all 15 fields

**3.7 Enhance existing rag-service tests**
- Add edge case tests to ConfirmationServiceTest (concurrent access, expired actions)
- Add more prompt section tests to AgenticSystemPromptTest
- Expand ReBACFilterTest with empty groups scenario
- Expand InputValidatorTest with more injection patterns

**3.8 RagConfig system prompt tests**
- Test that system prompt contains all required identity fields
- Test bilingual content (English + Arabic sections)
- Test service categories are present
- Test prompt structure matches expected format

### Phase 4: Vector Store Module (18% → 85%+)

**4.1 Entity tests**
- ActionAuditEntityTest: forStaged(), markExecuted/Failed/Cancelled/Expired state transitions
- SyncStateEntityTest: markSyncCompleted/Running/Failed state machine

**4.2 EmbeddingRefreshService tests**
- Mock repositories and embedding service
- Test refresh triggers and vector recomputation

**4.3 IncrementalSyncService tests**
- Test sync cycle: extract → remove old → re-index → update cursor
- Test timestamp cursor management
- Test empty extraction results
- Test partial failure handling

**4.4 Enhance existing vector-store tests**
- Expand VectorStoreServiceTest with edge cases
- Expand HybridSearchServiceTest with edge cases

### Phase 5: API Gateway Module (4% → 85%+)

**5.1 Controller tests** (all use @WebMvcTest pattern)
- ToolServerControllerTest: search, create, confirm endpoints
- IngestionControllerTest: sync trigger, status check
- AdminControllerTest: admin operations
- FeedbackControllerTest: submit feedback, list feedback
- HealthControllerTest: /health, /ready, /live endpoints (mock JdbcTemplate)
- MetricsControllerTest: /rag, /summary, /latency, /quality, /cache
- WebSocketChatControllerTest: STOMP message handling (mock RagAssistantService)
- TeamsBotControllerTest: webhook endpoint (mock TeamsBotHandler)

**5.2 Service tests**
- FeedbackServiceTest: persist feedback, retrieve, validation
- ToolIntentDetectorTest: intent detection patterns

**5.3 Filter tests**
- CorrelationIdFilterTest: generates UUID when absent, forwards existing
- RateLimitFilterTest: allows under limit, blocks over limit with 429
- ARContextCleanupFilterTest: always cleans up, skips non-API paths

**5.4 Health indicator tests**
- LlmHealthIndicatorTest: up when model exists, down when null/error
- EmbeddingHealthIndicatorTest: up when embed succeeds, down on failure

**5.5 Config and utility tests**
- RateLimitConfigTest: bucket creation per endpoint type, cleanup logic
- SecurityConfigTest: conditional security chains (enabled vs disabled)
- GlobalExceptionHandlerTest: exception → structured error response mapping
- MdcExecutorServiceTest: MDC context propagation across threads
- TeamsBotHandlerTest: webhook processing, auth validation
- TeamsBotAuthenticatorTest: HMAC signature verification

**5.6 Enhance existing api-gateway tests**
- Expand ChatControllerTest with more scenarios
- Expand OpenAiCompatibleControllerTest with streaming tests

### Phase 6: Coverage Verification & CI Integration

**6.1 Run full test suite and measure coverage**
- Execute `./mvnw clean verify` with JaCoCo
- Generate per-module and aggregate reports
- Verify each module ≥ 75%, aggregate ≥ 85%

**6.2 Fix coverage gaps**
- Identify remaining low-coverage files
- Add targeted tests for uncovered branches
- Re-run until thresholds met

**6.3 CI pipeline integration**
- Add JaCoCo report generation to CI workflow
- Add coverage threshold enforcement (fail build if < 85%)

## Complexity Tracking

No constitution violations to justify.
