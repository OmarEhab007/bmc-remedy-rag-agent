# BMC Remedy RAG Agent - Docker Image
# Multi-stage build for optimized image size

# Stage 1: Build
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /app

# Copy Maven wrapper and pom files
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY remedy-connector/pom.xml remedy-connector/
COPY vectorization-engine/pom.xml vectorization-engine/
COPY vector-store/pom.xml vector-store/
COPY rag-service/pom.xml rag-service/
COPY api-gateway/pom.xml api-gateway/

# Download dependencies (cached layer)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source code
COPY remedy-connector/src remedy-connector/src
COPY vectorization-engine/src vectorization-engine/src
COPY vector-store/src vector-store/src
COPY rag-service/src rag-service/src
COPY api-gateway/src api-gateway/src

# Build the application
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Create non-root user for security
RUN groupadd -r ragagent && useradd -r -g ragagent ragagent

# Install required system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copy the built JAR from builder stage
COPY --from=builder /app/api-gateway/target/*.jar app.jar

# Set ownership
RUN chown -R ragagent:ragagent /app

# Switch to non-root user
USER ragagent

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/v1/health || exit 1

# JVM options for containerized environment
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    --add-opens java.base/java.lang=ALL-UNNAMED \
    --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
    --add-opens java.base/java.util=ALL-UNNAMED"

# Entry point
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
