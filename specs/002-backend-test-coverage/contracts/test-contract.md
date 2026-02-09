# Test Contracts: Backend Test Coverage

This feature creates test classes only - no new API endpoints or data contracts.

## Test Contract: Each test class must

1. **Use MockitoExtension** for unit tests (not SpringBootTest)
2. **Use @WebMvcTest** for controller tests (not SpringBootTest)
3. **Follow naming**: `methodName_scenario_expectedResult()`
4. **Assert behavior** not just absence of exceptions
5. **Mock all external dependencies** (no DB, no network, no LLM API)
6. **Run in isolation** - no test order dependencies
7. **Complete in < 3 seconds** per test class

## Module-Specific Contracts

### remedy-connector
- All tests mock `ARServerUser` via `ThreadLocalARContext`
- Field ID constants used, never field name strings
- Date assertions use Unix epoch integers

### vectorization-engine
- ChunkStrategy tests verify chunk content, metadata, and sequence numbers
- LocalEmbeddingService tests verify 384-dimensional output
- AttachmentParser tests use temp files, not real documents

### rag-service
- @Tool tests verify delegation to downstream services
- System prompt tests verify content sections exist (not exact wording)
- Confirmation tests verify audit trail recording

### vector-store
- Entity tests verify state transitions (not JPA persistence)
- Service tests mock repositories

### api-gateway
- Controller tests verify HTTP status codes and JSON response structure
- Filter tests verify header manipulation and chain delegation
- All tests exclude OAuth auto-configuration
