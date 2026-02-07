# Data Model: Production Readiness Enhancements

**Date**: 2026-02-07
**Feature**: 001-production-readiness

## Overview

This feature does not introduce new data entities. It enhances operational aspects (logging, CI/CD, testing, security) of existing entities. The key entities below document the operational contracts affected by this feature.

## Entities

### HealthStatus

Represents the aggregate health of the system as reported by the `/actuator/health` endpoint.

| Field | Type | Description |
|-------|------|-------------|
| status | Enum (UP, DOWN, DEGRADED) | Overall system status |
| components.db | Object | Database connectivity check |
| components.llm | Object | LLM provider availability |
| components.embedding | Object | Embedding service status |

**Transitions**: UP -> DEGRADED (when any non-critical component is DOWN) -> DOWN (when database is unavailable)

### StructuredLogEntry

Represents a single JSON log event emitted in production mode.

| Field | Type | Description |
|-------|------|-------------|
| @timestamp | ISO 8601 | Event timestamp |
| level | String | Log level (ERROR, WARN, INFO, DEBUG) |
| logger_name | String | Logger class name |
| message | String | Log message |
| thread_name | String | Thread that produced the event |
| correlation_id | String (UUID) | Request correlation ID from MDC |
| stack_trace | String (optional) | Exception stack trace if present |

### CIPipelineRun

Represents a single execution of the CI/CD pipeline (modeled in GitHub Actions, not in application database).

| Field | Type | Description |
|-------|------|-------------|
| trigger | Enum (push, pull_request, release) | What triggered the run |
| status | Enum (success, failure, cancelled) | Run outcome |
| duration | Duration | Total pipeline execution time |
| stages | List | Build, test, lint, docker stages and their results |

## Relationships

- **HealthStatus** aggregates health from multiple independent checks (db, llm, embedding)
- **StructuredLogEntry** entries share a `correlation_id` across all log events within a single HTTP request
- **CIPipelineRun** is external to the application (GitHub-managed), triggered by git events
