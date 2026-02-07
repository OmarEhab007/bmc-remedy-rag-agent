# Research: Production Readiness Enhancements

**Date**: 2026-02-07
**Feature**: 001-production-readiness

## Research Summary

All technical decisions are resolved. No NEEDS CLARIFICATION markers remain. Decisions are based on existing codebase conventions and industry best practices for Spring Boot + React applications.

---

## Decision 1: CI/CD Platform

**Decision**: GitHub Actions
**Rationale**: Repository is hosted on GitHub (github.com/OmarEhab007/bmc-remedy-rag-agent). GitHub Actions is the native CI/CD solution with zero additional setup, free tier for public repos, and tight integration with PRs and branch protection.
**Alternatives considered**:
- GitLab CI: Would require migration or mirroring
- Jenkins: Self-hosted overhead not justified for this project size
- CircleCI: External dependency, cost for private repos

---

## Decision 2: Structured Logging Format

**Decision**: Logback with `logstash-logback-encoder` for JSON output in prod profile
**Rationale**: Spring Boot uses Logback by default. The Logstash encoder produces JSON compatible with ELK/Loki/CloudWatch without adding new framework dependencies. Profile-based activation ensures dev experience is unchanged.
**Alternatives considered**:
- Log4j2 with JSON Layout: Would require replacing the default logging framework
- Custom JSON formatter: More maintenance burden
- OpenTelemetry Logging: Heavier dependency, better suited for full OTel adoption

---

## Decision 3: Frontend Testing Framework

**Decision**: Vitest + React Testing Library + jsdom
**Rationale**: The frontend uses Vite as bundler (vite.config.ts exists). Vitest is the native test runner for Vite projects with zero-config integration, fast HMR-based test execution, and Jest-compatible API. React Testing Library is the standard for React component testing.
**Alternatives considered**:
- Jest: Requires additional configuration for Vite/ESM projects
- Playwright Component Testing: Heavier, better for E2E than unit tests
- Cypress Component Testing: Similar weight concerns

---

## Decision 4: Health Check Implementation

**Decision**: Spring Boot Actuator custom `HealthIndicator` beans
**Rationale**: Actuator is already in the dependency tree and exposed at `/actuator/health`. Custom HealthIndicator implementations for LLM and embedding services integrate naturally. No new dependencies needed.
**Alternatives considered**:
- Custom health endpoint: Duplicates existing Actuator functionality
- Kubernetes liveness/readiness probes only: Insufficient for detailed diagnostics

---

## Decision 5: Correlation ID Strategy

**Decision**: MDC (Mapped Diagnostic Context) with Spring `OncePerRequestFilter`
**Rationale**: MDC is built into SLF4J/Logback. A filter generates a UUID per request, sets it in MDC, and all subsequent log statements automatically include it. This works for both REST and WebSocket paths.
**Alternatives considered**:
- Spring Cloud Sleuth/Micrometer Tracing: Full distributed tracing is overkill for single-service deployment
- Custom ThreadLocal: Reinventing what MDC already provides

---

## Decision 6: Container Registry

**Decision**: GitHub Container Registry (ghcr.io)
**Rationale**: Native integration with GitHub Actions, free for public repos, supports OCI images, and authentication uses the same GITHUB_TOKEN.
**Alternatives considered**:
- Docker Hub: Rate limits on free tier
- Self-hosted registry: Additional infrastructure to maintain
- AWS ECR: Adds cloud dependency (conflicts with air-gap requirement)

---

## Decision 7: Test Coverage Scope

**Decision**: Focus on service-layer unit tests for critical business logic, not aiming for a specific coverage percentage
**Rationale**: Going from 6% to meaningful coverage means testing the classes that matter most: RagAssistantService, HybridSearchService, VectorStoreService, OpenAiCompatibleController, and key frontend components. Coverage percentage is a trailing indicator; well-designed tests for critical paths provide more value than chasing numbers.
**Alternatives considered**:
- Target 80% line coverage: Would require testing boilerplate/DTOs with little value
- Integration tests only: Too slow for CI, doesn't catch unit-level logic bugs
- TDD from scratch: Not practical for existing codebase enhancement
