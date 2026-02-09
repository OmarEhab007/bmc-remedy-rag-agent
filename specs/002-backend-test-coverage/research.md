# Research: Backend Test Coverage to 85%

**Branch**: `002-backend-test-coverage` | **Date**: 2026-02-07

## R1: Current Test Infrastructure

### Decision: Use existing spring-boot-starter-test (JUnit 5 + Mockito + AssertJ)
- **Rationale**: Already declared in parent POM as common dependency for all modules. All 16 existing tests use this stack consistently.
- **Alternatives considered**: TestNG (rejected - would require migration), JMockit (rejected - unfamiliar to project)

### Test Pattern Established in Codebase:
- `@ExtendWith(MockitoExtension.class)` with `@MockitoSettings(strictness = Strictness.LENIENT)` for unit tests
- `@WebMvcTest` with `excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class` for controllers
- `@AutoConfigureMockMvc(addFilters = false)` to bypass security filters in controller tests
- `@MockBean` required for `ThreadLocalARContext` and `RateLimitConfig` (auto-detected components)
- `@Nested` + `@DisplayName` for logical test grouping
- `@ParameterizedTest` + `@ValueSource` for variant testing
- Builder pattern for Lombok DTOs in test data
- `ArgumentCaptor` for verifying mock interactions

## R2: Coverage Measurement

### Decision: Add JaCoCo Maven Plugin to parent POM
- **Rationale**: Industry standard for Java coverage measurement. Integrates with Maven lifecycle and CI. Generates HTML + XML reports.
- **Alternatives considered**: Cobertura (rejected - less maintained), IntelliJ built-in (rejected - not CI-compatible)
- **Configuration**: Aggregate report via `jacoco:report-aggregate` in a dedicated reporting module or parent POM execution

## R3: Test Classification by Effort and Value

### Decision: Prioritize by business logic density, not module size

**Tier 1 - High Value, Critical Business Logic (~60 test classes)**:
| Class | Module | Methods to Test | Mocking Complexity | Priority |
|-------|--------|----------------|-------------------|----------|
| RemedyIncidentTool | rag-service | 4 @Tool + context mgmt | Medium (VectorStore, Confirmation, Validator) | P1 |
| RemedyWorkOrderTool | rag-service | 4 @Tool + context | Medium (same as above + reflection) | P1 |
| DameeServiceTool | rag-service | 7 @Tool methods | Medium (Catalog, IntentMatcher, Creator, Preview) | P1 |
| IncidentCreator | remedy-connector | 3 (create, validate, isAvailable) | High (ARServerUser mock) | P1 |
| IncidentUpdater | remedy-connector | 4 (update, worklog, validate) | High (ARServerUser mock) | P1 |
| WorkOrderCreator | remedy-connector | 3 (create, validate, isAvailable) | High (ARServerUser mock) | P1 |
| IncidentExtractor | remedy-connector | 7 extraction methods | High (ARServerUser, Entry mocks) | P1 |
| ThreadLocalARContext | remedy-connector | 6 (get, verify, refresh, close, retry) | High (ARServerUser, Config) | P1 |
| QualifierBuilder | remedy-connector | 12+ builder methods | None (pure utility) | P1 |
| ExtractionOrchestrator | remedy-connector | 8 extraction methods | High (6 extractors + context + config) | P2 |
| SemanticChunker | vectorization | 3 split methods | None (pure logic) | P1 |
| IncidentChunkStrategy | vectorization | 2 (chunk, getRecordType) | Low (SemanticChunker) | P1 |
| ChangeRequestChunkStrategy | vectorization | 2 | Low (SemanticChunker) | P2 |
| KnowledgeChunkStrategy | vectorization | 2 | Low (SemanticChunker) | P2 |
| WorkOrderChunkStrategy | vectorization | 2 | Low (SemanticChunker) | P2 |
| AttachmentParser | vectorization | 2 parse methods | Medium (Tika, filesystem) | P2 |
| LocalEmbeddingService | vectorization | 6 (embed, batch, cosine) | Medium (ONNX model) | P2 |
| IncrementalSyncService | vector-store | sync methods | Medium (extractors, vectorStore) | P2 |
| EmbeddingRefreshService | vector-store | refresh methods | Medium (repos, embeddings) | P2 |

**Tier 2 - Controllers & API Layer (~15 test classes)**:
| Class | Methods | Mocking |
|-------|---------|---------|
| ToolServerController | 5+ endpoints | rag-service tools |
| IngestionController | 3+ endpoints | extraction orchestrator |
| AdminController | 3+ endpoints | services |
| FeedbackController | 2+ endpoints | FeedbackService |
| HealthController | 3 endpoints | JdbcTemplate |
| MetricsController | 5 endpoints | RagMetricsService |
| WebSocketChatController | STOMP handler | RagAssistant, SimpMessaging |
| TeamsBotController | webhook | TeamsBotHandler |

**Tier 3 - Services, Filters, Utilities (~20 test classes)**:
| Class | Methods | Mocking |
|-------|---------|---------|
| FeedbackService | 3+ | Repository |
| ToolIntentDetector | 2+ | None |
| AgenticRateLimiter | 6 | Caffeine cache |
| ChatMemoryRetentionScheduler | 3 | ChatMemoryStore |
| RagMetricsService | 15+ | MeterRegistry |
| SemanticCacheService | 5+ | Redis |
| DameeService | 5+ | Catalog, Creator |
| DameeServiceCatalog | 8+ | Markdown resource |
| ServiceIntentMatcher | 3+ | Catalog |
| FieldCollector | 4+ | ServiceFieldDefinition |
| GuidedServiceCreator | 3+ | DameeService |
| WorkflowPreviewBuilder | 3 | None (pure logic) |
| ServiceFieldDefinition | 3 (validate, getPrompt) | None |
| CorrelationIdFilter | 1 | MockHttpServletRequest |
| ARContextCleanupFilter | 2 | ThreadLocalARContext |
| RateLimitFilter | 1 | RateLimitConfig |
| RateLimitConfig | 10+ bucket methods | None |
| LlmHealthIndicator | 1 | ChatLanguageModel |
| EmbeddingHealthIndicator | 1 | LocalEmbeddingService |
| MdcExecutorService | 3 | ExecutorService |

**Tier 4 - Models/DTOs with Logic (~10 test classes)**:
| Class | Testable Logic |
|-------|---------------|
| CreationResult | toUserMessage(), static factories |
| IncidentCreationRequest | Validation annotations |
| IncidentUpdateRequest | Validation, status Integer type |
| WorkOrderCreationRequest | Validation, date handling |
| TextChunk | buildITSMMetadata(), generateChunkId() |
| ActionAuditEntity | forStaged(), markExecuted/Failed/Cancelled/Expired |
| SyncStateEntity | markSyncCompleted/Running/Failed |
| PendingAction | isExpired(), builder |
| IncidentRecord | toUserMessage() if exists |

## R4: BMC AR API Mocking Strategy

### Decision: Mock ARServerUser at the interface level
- **Rationale**: ARServerUser is not thread-safe and requires network connectivity. All remedy-connector classes depend on it via ThreadLocalARContext. Mocking the ARServerUser instance returned by getContext() allows testing all business logic.
- **Key mock behaviors needed**:
  - `ctx.verifyUser()` - success/failure
  - `ctx.getListEntryObjects()` - return mock Entry lists
  - `ctx.createEntry()` - return entry ID string
  - `ctx.setEntry()` - void, verify calls
  - `ctx.getEntry()` - return mock Entry
  - `Entry.get(fieldId)` - return Value objects with type-specific data

## R5: ONNX Embedding Model Mocking

### Decision: Mock LocalEmbeddingService at the interface level for downstream tests; test LocalEmbeddingService itself with the real ONNX model
- **Rationale**: The ONNX model (all-minilm-l6-v2) is included as a Maven dependency and initializes via @PostConstruct. For the service's own tests, the real model should be used to verify 384-dimensional output. Downstream consumers should mock it.
- **Risk**: ONNX model initialization may be slow (~2s). Use @BeforeAll to init once per test class.

## R6: Files Excluded from Coverage Target

### Decision: Exclude pure configuration and data-only classes from 85% line target
- **Excluded from test requirement** (no corresponding test class needed):
  - `JpaConfig.java` - declarative JPA setup only
  - `CorsConfig.java` - declarative CORS setup
  - `WebSocketConfig.java` - declarative WebSocket setup
  - `SchedulerConfig.java` - declarative scheduler setup
  - `CredentialValidationConfig.java` - startup validation only
  - `BmcRemedyRagApplication.java` - Spring Boot main class
  - `OllamaConfig.java` - deprecated, commented out
  - Pure Lombok `@Data` DTOs in api-gateway with no custom methods (43 files)
  - `RemedyConnectionConfig.java` - configuration properties only
  - `FieldIdConstants.java` - static final constants only
  - Model records with no logic: `AttachmentInfo`, `WorkLogEntry`, `ITSMRecord` (interface)
- **Still require tests** despite being "config-like":
  - `SecurityConfig.java` - conditional security chains with business rules
  - `RateLimitConfig.java` - bucket creation logic
  - `AgenticConfig.java` - isOperational() logic
  - `GoogleAiConfig.java` - mock model fallback logic

## R7: Test Execution Time Budget

### Decision: Target <5 minutes for full unit test suite
- **Strategy**:
  - Use `@ExtendWith(MockitoExtension.class)` over `@SpringBootTest` (10x faster)
  - Use `@WebMvcTest` for controllers (loads only web layer)
  - Avoid Testcontainers in unit tests (save for integration profile)
  - Parallel test execution via maven-surefire-plugin `<forkCount>1C</forkCount>`
- **Estimated budget**:
  - remedy-connector: ~15s (27 files, all mocked)
  - vectorization-engine: ~20s (ONNX model init ~2s, then fast)
  - vector-store: ~10s (mocked repos)
  - rag-service: ~30s (largest module, complex mocking)
  - api-gateway: ~25s (WebMvcTest context loads)
  - Total: ~100s with safety margin to 5 min
