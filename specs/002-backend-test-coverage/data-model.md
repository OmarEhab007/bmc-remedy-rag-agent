# Data Model: Backend Test Coverage to 85%

**Branch**: `002-backend-test-coverage` | **Date**: 2026-02-07

## Test Architecture Model

This feature creates test classes - no new data entities. The "data model" here describes the test organization and the mock data structures needed.

## Test Classification

### Entity: Test Class

| Attribute | Description |
|-----------|-------------|
| Module | Which Maven module (remedy-connector, vectorization-engine, vector-store, rag-service, api-gateway) |
| Type | unit, controller, integration |
| Target Class | Source class being tested |
| Test Framework | MockitoExtension, WebMvcTest, SpringBootTest |
| Mock Dependencies | List of mocked classes |
| Test Count | Number of @Test methods |

### Entity: Mock Data Factory

Each module needs test data builders for creating realistic domain objects:

**remedy-connector test data:**
- `Entry` mock with field-value mappings (using BMC Field IDs)
- `ARServerUser` mock with verifyUser/getListEntryObjects/createEntry stubs
- `IncidentCreationRequest` builders
- `IncidentUpdateRequest` builders
- `WorkOrderCreationRequest` builders

**vectorization-engine test data:**
- `IncidentRecord` builders with all fields populated
- `WorkOrderRecord` builders
- `KnowledgeArticle` builders
- `ChangeRequestRecord` builders
- Sample text content for chunking (short, medium, long, Arabic)

**rag-service test data:**
- `ChatMessage` lists for conversation context
- `RetrievedDocument` lists for search results
- `PendingAction` builders for confirmation workflow
- `DameeService` builders for catalog tests

**api-gateway test data:**
- `ChatRequest` DTOs
- JSON request/response payloads
- MockMvc result matchers

## Test Dependency Graph

```text
Phase 0: JaCoCo Setup (no dependencies)
    │
    ├── Phase 1: remedy-connector (depends on: Phase 0)
    │   └── 1.1 QualifierBuilder (no deps)
    │   └── 1.2 DTOs (no deps)
    │   └── 1.3 ThreadLocalARContext (depends on: Config mock)
    │   └── 1.4 Creators (depends on: 1.3 ThreadLocalARContext mock)
    │   └── 1.5 Extractors (depends on: 1.3 ThreadLocalARContext mock)
    │   └── 1.6 Services (depends on: 1.5 Extractor mocks)
    │
    ├── Phase 2: vectorization-engine (depends on: Phase 0)
    │   └── 2.1 TextChunk (no deps)
    │   └── 2.2 SemanticChunker (no deps)
    │   └── 2.3 ChunkStrategies (depends on: 2.2 SemanticChunker)
    │   └── 2.4 LocalEmbeddingService (depends on: ONNX model)
    │   └── 2.5 AttachmentParser (depends on: Tika)
    │
    ├── Phase 3: rag-service (depends on: Phase 0)
    │   └── 3.1 Tools (depends on: VectorStore, Confirmation mocks)
    │   └── 3.2 Damee (depends on: Catalog, IntentMatcher mocks)
    │   └── 3.3-3.6 Services (depends on: various mocks)
    │   └── 3.7 Enhance existing (depends on: existing test patterns)
    │   └── 3.8 RagConfig (depends on: config properties)
    │
    ├── Phase 4: vector-store (depends on: Phase 0)
    │   └── 4.1 Entities (no deps)
    │   └── 4.2-4.3 Services (depends on: repository mocks)
    │   └── 4.4 Enhance existing (depends on: existing tests)
    │
    ├── Phase 5: api-gateway (depends on: Phase 0)
    │   └── 5.1 Controllers (depends on: service mocks)
    │   └── 5.2 Services (depends on: repository mocks)
    │   └── 5.3 Filters (depends on: config mocks)
    │   └── 5.4-5.5 Health/Config (depends on: bean mocks)
    │   └── 5.6 Enhance existing (depends on: existing tests)
    │
    └── Phase 6: Coverage Verification (depends on: Phases 1-5)
```

**Note**: Phases 1-5 are independent of each other and can be worked on in parallel. Within each phase, sub-tasks should be done in order (utilities first, then services that depend on them).

## Coverage Target Matrix

| Module | Source Files | Current Tests | Current % | Target Tests | Target % |
|--------|-------------|--------------|-----------|--------------|----------|
| remedy-connector | 27 | 0 | 0% | 16 | 85%+ |
| vectorization-engine | 9 | 0 | 0% | 8 | 85%+ |
| vector-store | 11 | 2 | 18% | 6 | 85%+ |
| rag-service | 34 | 11 | 32% | 30 | 85%+ |
| api-gateway | 67 | 3 | 4% | 25 | 85%+ |
| **Total** | **148** | **16** | **~11%** | **~85** | **85%+** |

> **Coverage Metric:** Line coverage measured by JaCoCo 0.8.12

Files excluded from test requirement (no business logic): ~20 (pure DTOs, constants, deprecated config, declarative config)
