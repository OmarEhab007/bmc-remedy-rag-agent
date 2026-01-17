#!/bin/bash

# BMC Remedy RAG Agent - Development Startup Script
# Usage: ./start-dev.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "BMC Remedy RAG Agent - Development Mode"
echo "=========================================="

# Load environment variables
if [ -f .env ]; then
    echo "Loading environment variables from .env..."
    source .env
else
    echo "WARNING: .env file not found. Using defaults."
fi

# Check prerequisites
echo ""
echo "Checking prerequisites..."

# Check PostgreSQL
if command -v psql &> /dev/null; then
    echo "✓ PostgreSQL client found"
else
    echo "⚠ PostgreSQL client not found (optional for running)"
fi

# Check Z.AI API Key
if [ -n "$ZAI_API_KEY" ]; then
    echo "✓ Z.AI API key configured"
else
    echo "✗ Z.AI API key not set. Please set ZAI_API_KEY in .env"
    exit 1
fi

# Check Java
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1)
    echo "✓ Java found: $JAVA_VERSION"
else
    echo "✗ Java not found. Please install Java 17+"
    exit 1
fi

echo ""
echo "Environment Configuration:"
echo "  POSTGRES_HOST:     ${POSTGRES_HOST:-localhost}"
echo "  POSTGRES_DB:       ${POSTGRES_DB:-bmc_rag}"
echo "  SECURITY_ENABLED:  ${SECURITY_ENABLED:-true}"
echo "  SPRING_PROFILE:    ${SPRING_PROFILES_ACTIVE:-default}"
echo "  ZAI_BASE_URL:      ${ZAI_BASE_URL:-https://api.z.ai/api/paas/v4/}"
echo "  ZAI_MODEL:         ${ZAI_MODEL:-glm-4.7}"
echo ""

# Build if JAR doesn't exist
JAR_FILE="api-gateway/target/api-gateway-1.0.0-SNAPSHOT.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "Building application..."
    mvn package -DskipTests
fi

echo "Starting application..."
echo ""

# Start the application
java -jar "$JAR_FILE" \
    --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-dev}
