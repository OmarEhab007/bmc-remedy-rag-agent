# BMC Remedy RAG Agent - Development Guide

> **For Developers contributing to this project**
> **Last Updated:** 2025-01-17

---

## Table of Contents

1. [Getting Started](#1-getting-started)
2. [Project Structure](#2-project-structure)
3. [Development Workflow](#3-development-workflow)
4. [Coding Standards](#4-coding-standards)
5. [Testing](#5-testing)
6. [Debugging](#6-debugging)
7. [Contributing](#7-contributing)

---

## 1. Getting Started

### 1.1 Prerequisites

**Required Software:**
- Java Development Kit (JDK) 17+
- Maven 3.9+
- Docker Desktop (for PostgreSQL)
- IntelliJ IDEA or VS Code (recommended)
- Git

**Optional Tools:**
- Postman (for API testing)
- pgAdmin (for database inspection)
- Ollama CLI (for local LLM testing)

### 1.2 Initial Setup

```bash
# Clone repository
git clone <repository-url>
cd bmc-remedy-rag-agent

# Install BMC AR API
mvn install:install-file \
  -Dfile=BMC/arAPI-91.9.jar \
  -DgroupId=com.bmc.arsys \
  -DartifactId=arAPI \
  -Dversion=91.9 \
  -Dpackaging=jar

# Configure environment
cp .env.example .env
# Edit .env with your configuration

# Build project
mvn clean package -DskipTests

# Start PostgreSQL (Docker)
cd docker
docker-compose up -d postgres

# Start application
cd ..
./start-dev.sh
```

### 1.3 IDE Setup

#### IntelliJ IDEA

1. Open project as Maven project
2. Enable annotation processing (Settings → Build, Execution, Deployment → Compiler → Annotation Processors)
3. Configure Lombok plugin
4. Set up code style: Google Java Style Guide
5. Configure JDK 17 as project SDK

#### VS Code

1. Install extensions:
   - Extension Pack for Java
   - Spring Boot Extension Pack
   - SonarLint (optional)
2. Configure settings.json:
   ```json
   {
     "java.configuration.updateBuildConfiguration": "automatic"
   }
   ```

---

## 2. Project Structure

### 2.1 Module Overview

```
bmc-remedy-rag-agent/
├── remedy-connector/          # BMC Remedy integration
├── vectorization-engine/      # Text processing and embeddings
├── vector-store/              # PostgreSQL + pgvector storage
├── rag-service/               # RAG orchestration
├── api-gateway/               # REST API and application
└── frontend/web-chat/         # React web interface
```

### 2.2 Package Structure

#### remedy-connector
```
com.bmc.rag.connector/
├── connection/
│   └── ThreadLocalARContext.java    # Thread-safe connection management
├── extractor/
│   ├── IncidentExtractor.java       # Incident data extraction
│   ├── AttachmentExtractor.java    # Attachment binary extraction
│   └── WorkLogExtractor.java       # Work log extraction
├── util/
│   ├── FieldIdConstants.java        # Remedy field ID constants
│   └── QualifierBuilder.java        # Query qualification builder
└── config/
    └── RemedyConnectionConfig.java  # Connection configuration
```

#### vectorization-engine
```
com.bmc.rag.vectorization/
├── embedding/
│   └── LocalEmbeddingService.java  # ONNX-based embeddings
├── chunking/
│   ├── IncidentChunkStrategy.java    # Incident-specific chunking
│   └── SemanticChunker.java         # Semantic text chunking
└── parser/
    └── AttachmentParser.java       # Apache Tika integration
```

#### vector-store
```
com.bmc.rag.store/
├── entity/
│   ├── EmbeddingEntity.java          # Embedding storage entity
│   └── SyncStateEntity.java          # CDC sync state entity
├── repository/
│   ├── EmbeddingRepository.java     # Custom JPA repository
│   └── SyncStateRepository.java     # Sync state repository
├── service/
│   └── VectorStoreService.java      # Vector operations
└── sync/
    └── IncrementalSyncService.java  # CDC sync orchestration
```

#### rag-service
```
com.bmc.rag.agent/
├── config/
│   ├── RagConfig.java               # RAG configuration
│   ├── ZaiConfig.java               # Z.AI LLM configuration
│   └── OllamaConfig.java             # Ollama LLM configuration
├── service/
│   └── RagAssistantService.java      # Main RAG orchestration
├── retrieval/
│   └── SecureContentRetriever.java # ReBAC-aware retrieval
└── memory/
    └── PostgresChatMemoryStore.java # PostgreSQL chat memory
```

#### api-gateway
```
com.bmc.rag.api/
├── controller/
│   ├── ChatController.java          # Chat REST endpoints
│   ├── WebSocketChatController.java # WebSocket endpoints
│   ├── IngestionController.java    # Ingestion endpoints
│   ├── AdminController.java         # Admin endpoints
│   └── HealthController.java       # Health check endpoints
├── dto/
│   ├── ChatRequest.java             # Chat request DTO
│   ├── ChatResponse.java            # Chat response DTO
│   └── ...
├── config/
│   ├── SecurityConfig.java          # Security configuration
│   ├── WebSocketConfig.java         # WebSocket configuration
│   └── CorsConfig.java              # CORS configuration
└── BmcRemedyRagApplication.java  # Main Spring Boot application
```

---

## 3. Development Workflow

### 3.1 Feature Development Workflow

1. **Create feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make changes**
   - Edit code
   - Add/update tests
   - Update documentation

3. **Build and test locally**
   ```bash
   mvn clean package -DskipTests
   ./start-dev.sh
   ```

4. **Run tests**
   ```bash
   mvn test
   ```

5. **Commit changes**
   ```bash
   git add .
   git commit -m "feat: add your feature"
   ```

6. **Push and create PR**
   ```bash
   git push origin feature/your-feature-name
   ```

### 3.2 Code Review Checklist

Before submitting a PR, ensure:
- [ ] Code follows Google Java Style Guide
- [ ] All tests pass (`mvn test`)
- [ ] No Spotless violations (`mvn spotless:check`)
- [ ] Documentation updated
- [ ] Added tests for new functionality
- [ ] Error handling implemented
- [ ] Logging added where appropriate

---

## 4. Coding Standards

### 4.1 Java Code Style

**Follow:** [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)

**Key Rules:**
- Indentation: 2 spaces (NO tabs)
- Line length: Max 120 characters
- Method order: static → public → protected → private
- Imports: No wildcard imports, sort alphabetically
- Variable naming: camelCase for local variables
- Constant naming: UPPER_SNAKE_CASE

**Example:**
```java
// ✓ CORRECT
public class VectorStoreService {
    private static final int MAX_RESULTS = 10;
    private final VectorStoreRepository repository;

    public List<SearchResult> search(String query) {
        // Implementation
    }
}

// ✗ INCORRECT
public class vectorstoreservice {
    private static final int max_results = 10;
    List<SearchResult> search(String query) {
        // Implementation
    }
}
```

### 4.2 Spring Best Practices

#### Dependency Injection
```java
// ✓ CORRECT
@Service
public class RagAssistantService {
    private final ChatLanguageModel chatModel;
    private final SecureContentRetriever retriever;

    public RagAssistantService(
        ChatLanguageModel chatModel,
        SecureContentRetriever retriever
    ) {
        this.chatModel = chatModel;
        this.retriever = retriever;
    }
}

// ✗ INCORRECT
@Service
public class RagAssistantService {
    @Autowired
    private ChatLanguageModel chatModel;

    // Field injection discouraged
}
```

#### Configuration Properties
```java
@Configuration
@ConfigurationProperties(prefix = "rag")
@Validated
public class RagConfig {
    @NotBlank
    private String systemPrompt;

    @Min(1)
    @Max(100)
    private int maxResults = 5;
}
```

### 4.3 Error Handling

```java
// ✓ CORRECT
public List<IncidentRecord> extractModifiedSince(long timestamp) {
    try {
        return performExtraction(timestamp);
    } catch (ARException e) {
        log.error("Failed to extract incidents: {}", e.getMessage(), e);
        throw new ExtractionException("Incident extraction failed", e);
    }
}

// ✗ INCORRECT
public List<IncidentRecord> extractModifiedSince(long timestamp) {
    try {
        return performExtraction(timestamp);
    } catch (Exception e) {
        // Too broad, loses context
        throw new RuntimeException(e);
    }
}
```

### 4.4 Logging

```java
// ✓ CORRECT
log.info("Starting extraction for timestamp: {}", timestamp);
log.debug("Found {} incidents", incidents.size());
log.error("Failed to connect to Remedy", exception);

// ✗ INCORRECT
log.info("Starting extraction");
log.info("Found incidents");  // Missing the actual data
log.error("Error");  // Missing context
```

**Logging Levels:**
- `ERROR`: Application errors, exceptions
- `WARN`: Recoverable issues, deprecated usage
- `INFO**: Important business events, startup/shutdown
- `DEBUG**: Detailed debugging information
- `TRACE**: Very detailed flow tracing

---

## 5. Testing

### 5.1 Unit Tests

Located in `src/test/java/` alongside source files.

**Example:**
```java
@SpringBootTest
class RagAssistantServiceTest {

    @Autowired
    private RagAssistantService ragAssistantService;

    @MockBean
    private ChatLanguageModel chatModel;

    @Test
    void chat_WithValidRequest_ReturnsResponse() {
        // Given
        ChatRequest request = ChatRequest.builder()
            .sessionId("test-session")
            .question("Test question")
            .userGroups(List.of("IT Support"))
            .build();

        when(chatModel.generate(any())).thenReturn(
            AiMessage.from("Test response")
        );

        // When
        ChatResponse response = ragAssistantService.chat(
            "test-session",
            "Test question",
            UserContext.builder()
                .userId("test-user")
                .groups(Set.of("IT Support"))
                .build()
        );

        // Then
        assertThat(response.getResponse()).contains("Test response");
    }
}
```

### 5.2 Integration Tests

```bash
# Run all integration tests
mvn verify -Pintegration-test

# Run specific test
mvn verify -Pintegration-test -Dit.test=RagAssistantServiceIntegrationTest
```

### 5.3 Test Data Management

**Testcontainers** for database testing:
```java
@Testcontainers
class VectorStoreRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        "pgvector/pgvector:pg16"
    )
        .withDatabaseName("test_db")
        .withUsername("test")
        .withPassword("test");
}
```

---

## 6. Debugging

### 6.1 Remote Debugging

**Configure JVM for remote debugging:**
```bash
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
```

**IntelliJ IDEA Configuration:**
1. Run → Edit Configurations
2. Add Remote JVM Debug
3. Set host: localhost, port: 5005
4. Set breakpoint in code
5. Click Debug

### 6.2 Logging Configuration

**Log levels in `application.yml`:**
```yaml
logging:
  level:
    com.bmc.rag: DEBUG
    org.springframework.security: DEBUG
    org.springframework.web: DEBUG
```

**Log file location:**
```bash
# Application logs
tail -f /tmp/bmc-rag.log

# Docker logs
docker-compose logs -f rag-agent
docker-compose logs -f postgres
```

### 6.3 Common Debugging Scenarios

#### Issue: Application won't start
```bash
# Check port 8080
lsof -i:8080

# Check logs
tail -50 /tmp/bmc-rag.log

# Verify PostgreSQL
docker-compose ps
docker-compose logs postgres
```

#### Issue: ReBAC filtering not working
```bash
# Check if ReBAC is enabled
grep RAG_REBAC_ENABLED .env

# Check user groups in request
# Verify metadata contains assigned_group
```

#### Issue: Citations missing
```bash
# Check citation config
grep includeCitations .env

# Verify system prompt includes citation rules
grep "CITATION" docs/DOCUMENTATION.md
```

---

## 7. Contributing

### 7.1 Pull Request Process

1. Fork the repository
2. Create feature branch: `git checkout -b feature/your-feature`
3. Make changes and commit
4. Push to your fork
5. Create Pull Request on GitHub

### 7.2 Commit Message Format

```
<type>(<scope>): <subject>

<body>

<footer>
<co-authored-by>
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `refactor`: Code refactoring
- `test`: Test additions/changes
- `chore`: Maintenance tasks

**Scopes:**
- `remedy-connector`
- `vectorization-engine`
- `vector-store`
- `rag-service`
- `api-gateway`
- `frontend`
- `docs`

**Example:**
```
feat(rag-service): add bilingual error messages

Add Arabic and English error messages for insufficient context scenarios.
Improve user feedback when no relevant information is found.

Co-Authored-By: Claude <noreply@anthropic.com>
```

### 7.3 Code Review Guidelines

**For Reviewers:**
- Check for TODO comments and address or create issues
- Verify tests exist for new functionality
- Ensure logging is appropriate
- Check for security vulnerabilities
- Verify documentation is updated

**For Authors:**
- Respond to review comments within 48 hours
- Update tests based on feedback
- Keep PR size manageable (<500 lines if possible)
- Delete your branch after merge

---

## 8. Module-Specific Guidelines

### 8.1 remedy-connector

**Key Considerations:**
- Always use Field IDs, not field names
- Implement retry logic for ARERR 92/93
- Use ThreadLocal for ARServerUser
- Clean up connections in finally blocks

**Example:**
```java
public List<IncidentRecord> extractRecords() {
    try {
        ARServerUser ctx = CONTEXT.get();
        return retrieveWithRetry(ctx);
    } finally {
        // Always cleanup
        CONTEXT.remove();
    }
}
```

### 8.2 vectorization-engine

**Key Considerations:**
- Chunk size affects retrieval quality
- Inject metadata for context
- Handle attachments with Tika
- ONNX model loading is expensive (cache it)

**Example:**
```java
@Component
public class IncidentChunkStrategy {
    private static final int MAX_CHUNK_SIZE = 1000;

    public List<Chunk> chunk(IncidentRecord incident) {
        // Create chunks with metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("incident_number", incident.getIncidentNumber());
        metadata.put("assigned_group", incident.getAssignedGroup());
        // ...
    }
}
```

### 8.3 vector-store

**Key Considerations:**
- Use batch inserts for performance
- HNSW index tuning
- GIN index for JSONB queries
- Connection pool management

**Example:**
```java
@Service
public class VectorStoreService {
    @Transactional
    public void storeBatch(List<EmbeddedChunk> chunks) {
        jdbcTemplate.batchUpdate(
            INSERT INTO embedding_store (embedding, text_segment, metadata, ...) VALUES (?, ?, ?, ...)",
            chunks,
            chunkSize
        );
    }
}
```

### 8.4 rag-service

**Key Considerations:**
- Memory management (Caffeine cache)
- Streaming response handling
- ReBAC filtering
- System prompt configuration

**Example:**
```java
@Service
public class RagAssistantService {
    private final Cache<String, ChatMemory> sessionMemories;

    public ChatResponse chat(String sessionId, String question, UserContext userContext) {
        ChatMemory memory = sessionMemories.get(sessionId, id -> {
            // Create new memory with 20 message window
        });

        // ... orchestration
    }
}
```

### 8.5 api-gateway

**Key Considerations:**
- Request validation
- Error handling
- Rate limiting
- WebSocket session management

**Example:**
```java
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        try {
            ChatResponse response = ragAssistantService.chat(
                request.getSessionId(),
                request.getQuestion(),
                createUserContext(request)
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Chat failed for session {}", request.getSessionId(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
```

---

## 9. Build and Run

### 9.1 Build Commands

```bash
# Clean build with tests
mvn clean install

# Build without tests
mvn clean package -DskipTests

# Build specific module
mvn clean package -pl remedy-connector

# Build with profile
mvn clean package -Pprod
```

### 9.2 Run Commands

```bash
# Development mode
./start-dev.sh

# Direct Maven
mvn spring-boot:run -pl api-gateway -Dspring-boot.run.profiles=dev

# With custom profile
mvn spring-boot:run -pl api-gateway -Dspring-boot.run.profiles=dev,local-llm

# With JVM options
mvn spring-boot:run -pl api-gateway -Dspring-boot.run.jvmArguments="-Xmx4g -XX:+UseG1GC"
```

### 9.3 Docker Development

```bash
# Build image
docker build -f docker/Dockerfile -t bmc-rag-agent:latest .

# Run with docker-compose
cd docker
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Rebuild and restart
docker-compose up -d --build
```

---

## 10. Troubleshooting

### 10.1 Common Issues

#### Port Already in Use
```bash
# Kill process on port 8080
lsof -ti:8080 | xargs kill -9
```

#### Maven Build Failures
```bash
# Clean Maven cache
rm -rf ~/.m2/repository/com/bmc/rag

# Rebuild
mvn clean install
```

#### Database Connection Issues
```bash
# Check PostgreSQL is running
docker-compose ps postgres

# Check database logs
docker-compose logs postgres

# Test connection
psql -h localhost -U raguser -d bmc_rag
```

#### Ollama Connection Issues
```bash
# Check Ollama is running
ollama list

# Test Ollama API
curl http://localhost:11434/api/tags

# Pull model if needed
ollama pull llama3:8b
```

### 10.2 Debug Mode

Enable debug logging:

```yaml
# In application.yml
logging:
  level:
    com.bmc.rag: DEBUG
    dev.langchain4j: DEBUG
```

---

## 11. Performance Tuning

### 11.1 Vector Search Optimization

**Adjust these parameters in `application.yml`:**
```yaml
rag:
  maxResults: 5      # Reduce for faster search
  minScore: 0.7     # Increase to reduce results
```

### 11.2 Database Optimization

**Connection pool:**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

**JVM settings:**
```bash
JAVA_OPTS="-Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

### 11.3 Caffeine Cache Tuning

**In `RagAssistantService.java`:**
```java
this.sessionMemories = Caffeine.newBuilder()
    .expireAfterAccess(24, TimeUnit.HOURS)
    .maximumSize(10_000)
    .recordStats()
    .build();
```

---

## 12. Testing Guide

### 12.1 Unit Tests

**Test naming convention:**
```
<MethodBeingTest>_<ScenarioUnderTest>_<ExpectedResult>

Example:
chat_WithValidRequest_ReturnsResponse()
chat_WithNoContext_ReturnsErrorMessage()
```

**Mock external dependencies:**
```java
@MockBean
private ChatLanguageModel chatModel;

@MockBean
private SecureContentRetriever retriever;
```

### 12.2 Integration Tests

**Use Testcontainers for database tests:**
```java
@Testcontainers
class VectorStoreIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");
}
```

### 12.3 API Testing

**Use Postman or curl:**
```bash
# Test health endpoint
curl http://localhost:8080/api/v1/health

# Test chat endpoint
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "test-123",
    "question": "Test question",
    "userGroups": ["IT Support"]
  }'
```

---

**Document Version:** 1.0
**Last Updated:** 2025-01-17
