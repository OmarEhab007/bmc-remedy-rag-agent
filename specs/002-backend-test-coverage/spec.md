# Feature Specification: Backend Test Coverage to 85%

**Feature Branch**: `002-backend-test-coverage`
**Created**: 2026-02-07
**Status**: Draft
**Input**: User description: "Achieve at least 85% backend test coverage focusing on business logic, application logic, agentic tools, system prompt testing, and all 5 Java modules"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Core RAG Service Business Logic Has Verified Test Coverage (Priority: P1)

As a developer, I need the core business logic in the RAG service module to be thoroughly tested so that changes to retrieval, assistant orchestration, confirmation workflows, and security filtering do not introduce regressions.

**Why this priority**: The rag-service module is the brain of the application - it orchestrates RAG retrieval, agentic decision-making, confirmation workflows, and security filtering. Bugs here directly affect every user interaction. Currently at 32% coverage with 23 untested source files.

**Independent Test**: Can be fully tested by running `mvn test -pl rag-service` and verifying all service, tool, security, and prompt classes have passing unit tests with meaningful assertions on business behavior.

**Acceptance Scenarios**:

1. **Given** the RagAssistantService receives a user query with context, **When** the service processes the query, **Then** it correctly retrieves relevant content, applies ReBAC filtering, and returns a response with sources
2. **Given** an agentic request to create an incident, **When** the AgenticAssistantService processes it, **Then** it correctly classifies intent, fills slots, stages a pending action, and returns a confirmation prompt
3. **Given** the ConfirmationService has a staged action, **When** the user confirms within the timeout window, **Then** the action executes; **When** the timeout expires, **Then** the action is rejected
4. **Given** a user query with group memberships, **When** SecureContentRetriever performs search, **Then** only documents matching the user's groups are returned
5. **Given** the InputValidator receives input with injection attempts, **When** validation runs, **Then** malicious input is rejected and safe input passes through

---

### User Story 2 - LangChain4j Tools and System Prompt Behave Correctly (Priority: P1)

As a developer, I need the LangChain4j @Tool classes and the system prompt builder to be tested so that AI-driven incident creation, work order management, and prompt engineering work as designed.

**Why this priority**: The agentic tools are the direct interface between the LLM and BMC Remedy - incorrect tool behavior can create wrong tickets, lose data, or expose unauthorized information. The system prompt defines the assistant's entire behavior contract. Currently zero tests exist for any of the 3 Tool classes.

**Independent Test**: Can be tested by running tool unit tests that mock Remedy dependencies and prompt builder tests that verify prompt structure, bilingual support, and intent classification rules.

**Acceptance Scenarios**:

1. **Given** RemedyIncidentTool.searchSimilarIncidents is called with a query, **When** the tool executes, **Then** it delegates to the retrieval service and returns formatted incident results
2. **Given** RemedyIncidentTool.stageIncidentCreation is called with incident details, **When** duplicate check passes, **Then** a PendingAction is staged and an actionId is returned
3. **Given** RemedyWorkOrderTool.stageWorkOrderCreation is called, **When** the tool executes, **Then** it correctly validates required fields and stages the work order
4. **Given** AgenticSystemPrompt builds a prompt, **When** the prompt is generated, **Then** it contains all required sections: identity, intent classification rules, slot filling matrix, tool usage rules, bilingual support, and output formatting
5. **Given** the system prompt is used for intent classification, **When** an INCIDENT vs WORK ORDER classification is needed, **Then** the prompt's classification rules include the correct business indicators for each type

---

### User Story 3 - Remedy Connector Business Logic is Verified (Priority: P2)

As a developer, I need the remedy-connector module's extraction, creation, and connection management logic tested so that interactions with BMC Remedy work correctly.

**Why this priority**: This module interfaces directly with the BMC AR System. While integration tests with a live Remedy server are out of scope, the business logic around field mapping, query building, data transformation, and connection management must be verified. Currently at 0% coverage with 27 untested source files.

**Independent Test**: Can be tested by running `mvn test -pl remedy-connector` with unit tests that mock ARServerUser and verify field ID mapping, qualifier building, data extraction logic, and ThreadLocal safety.

**Acceptance Scenarios**:

1. **Given** IncidentCreator receives a creation request, **When** it builds the AR entry, **Then** it maps all fields to correct Field IDs (1000000161, 1000000000, etc.) and returns a CreationResult
2. **Given** IncidentExtractor processes AR entry objects, **When** it extracts incident data, **Then** all fields are correctly mapped from Field IDs to domain model properties
3. **Given** QualifierBuilder receives filter criteria, **When** it builds a qualification string, **Then** it produces valid AR System qualifications with correct field ID references and date handling (Unix epoch)
4. **Given** ThreadLocalARContext is accessed from multiple threads, **When** each thread sets its own context, **Then** contexts are isolated and cleanup properly releases resources
5. **Given** ExtractionOrchestrator is invoked with pagination parameters, **When** it extracts records, **Then** it correctly applies firstRetrieve/maxRetrieve limits and handles timeout scenarios

---

### User Story 4 - Vectorization and Chunking Logic is Verified (Priority: P2)

As a developer, I need the vectorization-engine module's chunking strategies and embedding service tested so that ITSM data is correctly split, enriched, and embedded.

**Why this priority**: Incorrect chunking leads to poor search quality - splitting resolutions mid-sentence or losing metadata context degrades the entire RAG pipeline. Currently at 0% coverage with 9 untested source files.

**Independent Test**: Can be tested by running `mvn test -pl vectorization-engine` with unit tests that verify chunk boundaries, metadata injection, and text extraction from attachments.

**Acceptance Scenarios**:

1. **Given** SemanticChunker receives an IncidentRecord, **When** it chunks the record, **Then** it delegates to IncidentChunkStrategy and produces chunks with injected summary metadata
2. **Given** IncidentChunkStrategy processes a record with a Resolution field, **When** chunking executes, **Then** the Resolution is treated as a standalone high-value chunk
3. **Given** KnowledgeChunkStrategy processes a knowledge article, **When** chunking executes, **Then** it creates appropriately sized chunks with article metadata
4. **Given** AttachmentParser receives a PDF file, **When** parsing executes, **Then** it extracts text content and handles corrupt/empty files gracefully
5. **Given** LocalEmbeddingService receives text input, **When** embedding is computed, **Then** it produces a 384-dimensional vector

---

### User Story 5 - API Gateway Controllers and Services are Verified (Priority: P3)

As a developer, I need the api-gateway module's untested controllers, filters, and services tested so that HTTP/WebSocket endpoints correctly handle requests, errors, and security.

**Why this priority**: The API gateway is the entry point for all client interactions. Incorrect request handling, missing error responses, or filter misconfigurations break the user experience. Currently at 4% coverage with only 3 of 11 controllers tested.

**Independent Test**: Can be tested using lightweight controller tests with mocked downstream services, verifying request/response contracts, error handling, and filter behavior.

**Acceptance Scenarios**:

1. **Given** ToolServerController receives a search request, **When** the request is processed, **Then** it delegates to the correct tool and returns properly formatted results
2. **Given** IngestionController receives a sync trigger, **When** the endpoint is called, **Then** it triggers the ingestion pipeline and returns status
3. **Given** FeedbackController receives user feedback, **When** the feedback is submitted, **Then** it persists the feedback and returns acknowledgement
4. **Given** GlobalExceptionHandler catches an unhandled exception, **When** the exception is processed, **Then** it returns a structured error response with appropriate status code
5. **Given** RateLimitFilter receives requests exceeding the limit, **When** the filter processes them, **Then** excess requests are rejected with a rate-limit error

---

### User Story 6 - Vector Store Services and CDC Sync are Verified (Priority: P3)

As a developer, I need the vector-store module's remaining untested services (EmbeddingRefreshService, IncrementalSyncService) tested so that data synchronization and vector maintenance work correctly.

**Why this priority**: CDC sync correctness ensures the vector store stays up-to-date with Remedy. Incorrect sync can lead to stale or missing data. Currently at 18% coverage.

**Independent Test**: Can be tested with unit tests that mock repositories and verify sync logic, timestamp handling, and vector refresh behavior.

**Acceptance Scenarios**:

1. **Given** IncrementalSyncService runs a sync cycle, **When** new/modified records exist since last sync timestamp, **Then** it extracts them, removes old vectors, re-indexes, and updates the sync cursor
2. **Given** EmbeddingRefreshService detects stale embeddings, **When** refresh is triggered, **Then** affected embeddings are recomputed and updated in the store

---

### Edge Cases

- What happens when a tool method receives null or empty parameters?
- How does the confirmation service handle concurrent confirmations for the same actionId?
- What happens when the LLM returns an unparseable response during agentic processing?
- How does the system handle BMC Remedy connection timeout (ARERR 92/93) during extraction?
- What happens when vector search returns zero results?
- How does ReBAC filtering behave when a user has no group memberships?
- What happens when ThreadLocalARContext is not cleaned up after a request?
- How does the system prompt handle mixed Arabic/English input?
- What happens when chunking receives a record with all empty fields?
- How does the rate limiter handle burst traffic patterns?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Test suite MUST cover at least 85% of lines across all 5 backend Java modules (remedy-connector, vectorization-engine, vector-store, rag-service, api-gateway)
- **FR-002**: Tests MUST verify business logic correctness, not just exercise code paths - each test must contain meaningful assertions on behavior and output
- **FR-003**: All LangChain4j @Tool annotated methods (RemedyIncidentTool, RemedyWorkOrderTool, DameeServiceTool) MUST have unit tests that verify tool input/output contracts, error handling, and delegation to downstream services
- **FR-004**: The AgenticSystemPrompt builder MUST be tested for: prompt structure completeness, bilingual content inclusion, intent classification rules, slot filling matrix, tool usage rules, and output formatting directives
- **FR-005**: The confirmation workflow (ConfirmationService + PendingAction) MUST be tested for: staging, confirmation, rejection, timeout expiry, and concurrent access scenarios
- **FR-006**: ReBAC security filtering (ReBACFilter + SecureContentRetriever) MUST be tested for: authorized access, unauthorized access, empty groups, and metadata filtering correctness
- **FR-007**: All Remedy connector creators/extractors MUST be tested with mocked AR System API, verifying Field ID mapping correctness and data transformation logic
- **FR-008**: Vectorization chunking strategies MUST be tested for: correct chunk boundaries, metadata injection, high-value chunk identification (Resolution field), and edge cases (empty fields, oversized text)
- **FR-009**: API gateway controllers not yet tested (ToolServerController, IngestionController, AdminController, FeedbackController, HealthController, MetricsController, WebSocketChatController) MUST have controller-level tests verifying request/response contracts
- **FR-010**: Tests MUST use mocking for external dependencies (BMC AR API, database, LLM providers) so tests run without infrastructure
- **FR-011**: CDC/sync services (IncrementalSyncService, EmbeddingRefreshService) MUST be tested for sync cursor management, record extraction, and vector refresh logic
- **FR-012**: All tests MUST follow the project's naming convention: `methodName_scenario_expectedResult()`

### Key Entities

- **Test Suite**: Collection of test classes organized per module, targeting business logic classes
- **Coverage Report**: Tool-generated report measuring line, branch, and method coverage per module
- **Mocked Dependencies**: Mocks for BMC AR API (ARServerUser), data repositories, LLM models (ChatLanguageModel), and embedding services

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Backend test coverage reaches 85% or higher across all 5 modules combined, as measured by the coverage reporting tool
- **SC-002**: All previously untested source files with business logic have at least one corresponding test class (pure data containers with no logic may be excluded)
- **SC-003**: Every LangChain4j @Tool method has at least 3 test cases covering: happy path, error/edge case, and input validation
- **SC-004**: System prompt test suite verifies all prompt sections are present and correctly structured, covering both English and Arabic content
- **SC-005**: All tests pass in the CI pipeline without requiring external infrastructure (no database, no Remedy server, no LLM API)
- **SC-006**: Test execution completes within 5 minutes for the full backend suite (excluding integration tests)
- **SC-007**: Minimal use of full application context loading in tests - prefer lightweight unit tests and controller-specific tests for speed
- **SC-008**: Each module individually achieves at least 75% coverage (no module left significantly behind)
