# Quickstart: Backend Test Coverage to 85%

**Branch**: `002-backend-test-coverage` | **Date**: 2026-02-07

## Prerequisites

1. Java 17+ installed
2. Maven wrapper available (`./mvnw`)
3. BMC AR API JAR installed in local Maven repo:
   ```bash
   mvn install:install-file \
     -Dfile=BMC/arAPI-91.9.jar \
     -DgroupId=com.bmc.arsys -DartifactId=arAPI -Dversion=91.9 -Dpackaging=jar
   ```

## Running Tests

```bash
# All tests across all modules
./mvnw test

# Single module tests
./mvnw test -pl remedy-connector
./mvnw test -pl vectorization-engine
./mvnw test -pl vector-store
./mvnw test -pl rag-service
./mvnw test -pl api-gateway

# Specific test class
./mvnw test -pl rag-service -Dtest=RemedyIncidentToolTest

# Specific test method
./mvnw test -pl rag-service -Dtest=RemedyIncidentToolTest#searchSimilarIncidents_happyPath_returnsFormattedResults

# With coverage report
./mvnw clean verify
# Reports at: {module}/target/site/jacoco/index.html
```

## Test Patterns

### Unit Test (most classes)
```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MyServiceTest {
    @Mock private DependencyA depA;
    @Mock private DependencyB depB;
    @InjectMocks private MyService service;

    @Test
    void methodName_scenario_expectedResult() {
        // Given
        when(depA.doSomething()).thenReturn(expected);
        // When
        var result = service.method();
        // Then
        assertThat(result).isNotNull();
        verify(depA).doSomething();
    }
}
```

### Controller Test (api-gateway)
```java
@WebMvcTest(
    controllers = MyController.class,
    excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
class MyControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private MyService service;
    @MockBean private ThreadLocalARContext arContext;  // auto-detected component
    @MockBean private RateLimitConfig rateLimitConfig; // auto-detected component

    @Test
    void endpoint_scenario_expectedResult() throws Exception {
        when(service.process(any())).thenReturn(response);
        mockMvc.perform(post("/api/v1/endpoint")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\":\"value\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("expected"));
    }
}
```

### Nested Test Organization
```java
@ExtendWith(MockitoExtension.class)
class ComplexServiceTest {
    @Nested @DisplayName("Feature A")
    class FeatureA {
        @Test void scenario1() { }
        @Test void scenario2() { }
    }

    @Nested @DisplayName("Feature B")
    class FeatureB {
        @Test void scenario1() { }
    }
}
```

## Key Mocking Patterns

### BMC AR API (remedy-connector)
```java
@Mock ARServerUser mockCtx;
@Mock ThreadLocalARContext arContext;

// Mock getListEntryObjects for extraction
Entry mockEntry = mock(Entry.class);
Value mockValue = new Value("INC000001");
when(mockEntry.get(1000000161)).thenReturn(mockValue);  // Incident Number
when(mockCtx.getListEntryObjects(...)).thenReturn(List.of(mockEntry));

// Mock createEntry for creation
when(mockCtx.createEntry(anyString(), any(Entry.class))).thenReturn("entry-id-123");
```

### LLM Models (rag-service)
```java
@Mock ChatLanguageModel chatModel;
@Mock StreamingChatLanguageModel streamingModel;

when(chatModel.chat(any(ChatRequest.class)))
    .thenReturn(ChatResponse.from(AiMessage.from("response text")));
```

### Lombok DTOs (use builder in different packages)
```java
// CORRECT - use builder pattern
ChatResponseDto.builder().response("text").sources(List.of()).hasContext(true).build();

// WRONG - constructor is package-private with @Data + @Builder
new ChatResponseDto("text", List.of(), true);  // compile error in test package
```

## Naming Convention

All test methods follow: `methodName_scenario_expectedResult()`

Examples:
- `searchSimilarIncidents_validQuery_returnsFormattedResults()`
- `stageIncidentCreation_duplicateDetected_returnsWarning()`
- `parse_oversizedFile_returnsEmpty()`
- `isRateLimited_counterAtLimit_returnsTrue()`

## Coverage Report

After running `./mvnw clean verify`, check:
- Per-module: `{module}/target/site/jacoco/index.html`
- Target: 85%+ overall, 75%+ per module
