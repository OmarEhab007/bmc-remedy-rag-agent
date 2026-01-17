# BMC Remedy RAG Agent - API Documentation

> **Base URL:** `http://localhost:8080`
> **API Version:** v1
> **Content-Type:** `application/json`

---

## Table of Contents

1. [Overview](#1-overview)
2. [Authentication](#2-authentication)
3. [Chat APIs](#3-chat-apis)
4. [Search APIs](#4-search-apis)
5. [Ingestion APIs](#5-ingestion-apis)
6. [Admin APIs](#6-admin-apis)
7. [WebSocket APIs](#7-websocket-apis)
8. [Session Management](#8-session-management)
9. [Error Responses](#9-error-responses)
10. [Rate Limiting](#10-rate-limiting)

---

## 1. Overview

The BMC Remedy RAG Agent exposes RESTful APIs for chat, search, ingestion, and administrative operations. All endpoints are prefixed with `/api/v1/`.

### Supported Operations

| Operation | HTTP Method | Endpoint | Description |
|-----------|-------------|----------|-------------|
| Chat | POST | `/api/v1/chat` | Send question, get AI response |
| Search | POST | `/api/v1/chat/search` | Semantic search over ITSM data |
| List Sessions | GET | `/api/v1/chat/sessions` | Get all chat sessions |
| Clear Session | DELETE | `/api/v1/chat/sessions/{id}` | Delete session history |
| Get History | GET | `/api/v1/chat/sessions/{id}/history` | Get conversation history |
| Trigger Ingestion | POST | `/api/v1/admin/ingestion/trigger` | Trigger data sync |
| Ingestion Status | GET | `/api/v1/admin/ingestion/status` | Get sync status |
| Health Check | GET | `/api/v1/health` | Application health |

---

## 2. Authentication

### 2.1 Development Mode (Default)

Authentication is disabled in development mode:

```bash
SECURITY_ENABLED=false
```

All endpoints are accessible without authentication.

### 2.2 Production Mode

Authentication is enabled using OAuth2/OIDC:

```bash
SECURITY_ENABLED=true
JWT_JWK_SET_URI=https://your-auth-server/.well-known/jwks.json
```

### 2.3 User Context

For ReBAC (Relationship-Based Access Control), requests must include user context:

```json
{
  "sessionId": "session-123",
  "question": "How do I reset my VPN?",
  "userGroups": ["IT Support", "Service Desk"]
}
```

---

## 3. Chat APIs

### 3.1 Send Chat Message

Send a question and get an AI-powered response from the RAG system.

**Endpoint:** `POST /api/v1/chat`

**Request Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "sessionId": "session-123",
  "question": "How do I reset my VPN password?",
  "userGroups": ["IT Support", "Service Desk"]
}
```

**Request Parameters:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| sessionId | string | Yes | Unique session identifier (max 255 chars) |
| question | string | Yes | User's question or query |
| userGroups | string[] | Yes* | User's group memberships for ReBAC filtering |
| context | string | No | Optional additional context |

*Required in production with ReBAC enabled

**Response (200 OK):**
```json
{
  "sessionId": "session-123",
  "response": "I can help you reset your VPN password.\n\nSolution:\n1. Go to https://vpn.cst.gov.sa/selfservice\n2. Enter your employee ID and click 'Send OTP'\n3. Enter the OTP sent to your mobile\n4. Create a new password following the password policy\n\nSources: (Source: KB0001234)",
  "sources": ["KB0001234"],
  "hasContext": true,
  "confidence": 0.92
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| sessionId | string | Session identifier from request |
| response | string | AI-generated response with citations |
| sources | string[] | List of source citations in (Source: TYPE-ID) format |
| hasContext | boolean | Whether relevant context was found |
| confidence | double | Confidence score (0.0 to 1.0) |

**Error Responses:**

- **400 Bad Request**: Invalid request parameters
  ```json
  {
    "timestamp": "2025-01-17T13:00:00Z",
    "status": 400,
    "error": "Bad Request",
    "message": "sessionId is required",
    "path": "/api/v1/chat"
  }
  ```

- **500 Internal Server Error**: Server error
  ```json
  {
    "timestamp": "2025-01-17T13:00:00Z",
    "status": 500,
    "error": "Internal Server Error",
    "message": "Failed to process chat request",
    "path": "/api/v1/chat"
  }
  ```

**Example:**

```bash
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "session-123",
    "question": "How do I reset my VPN password?",
    "userGroups": ["IT Support"]
  }'
```

---

### 3.2 Chat without Context (Direct LLM)

**Endpoint:** `POST /api/v1/chat/direct`

**Request Body:**
```json
{
  "sessionId": "session-456",
  "question": "What is the capital of France?"
}
```

**Response:** Similar to `/api/v1/chat` but without RAG context (uses LLM's training data only).

---

## 4. Search APIs

### 4.1 Semantic Search

Perform semantic vector search over the ITSM knowledge base.

**Endpoint:** `POST /api/v1/chat/search`

**Request Body:**
```json
{
  "query": "VPN password reset",
  "maxResults": 10,
  "minScore": 0.7
}
```

**Request Parameters:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| query | string | Yes | Search query |
| maxResults | integer | No | Max results (default: 5, max: 50) |
| minScore | number | No | Min similarity score (0.0-1.0, default: 0.7) |
| userGroups | string[] | Yes* | User's groups for ReBAC filtering |

**Response (200 OK):**
```json
{
  "results": [
    {
      "sourceType": "KB",
      "sourceId": "KB0001234",
      "text": "To reset VPN password, go to self-service portal...",
      "score": 0.92,
      "metadata": {
        "incident_number": "INC0000456",
        "summary": "VPN password reset procedure",
        "assigned_group": "IT Support",
        "status": "Resolved"
      }
    },
    {
      "sourceType": "INC",
      "sourceId": "INC0000456",
      "text": "User reported VPN login issue...",
      "score": 0.88,
      "metadata": {...}
    }
  ],
  "totalResults": 2
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| results | array | Array of search results |
| results[].sourceType | string | Source type: KB, INC, WO, CR |
| results[].sourceId | string | Source record ID |
| results[].text | string | Text content of the chunk |
| results[].score | number | Similarity score (0.0-1.0) |
| results[].metadata | object | Additional metadata as JSON |
| totalResults | integer | Total number of results found |

---

## 5. Ingestion APIs

### 5.1 Trigger Sync

Trigger a synchronization operation with BMC Remedy.

**Endpoint:** `POST /api/v1/admin/ingestion/trigger`

**Request Body:**
```json
{
  "sourceType": "INCIDENTS",
  "fullSync": false
}
```

**Request Parameters:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| sourceType | string | Yes | Source type: INCIDENTS, KNOWLEDGE, ALL |
| fullSync | boolean | No | Force full sync (default: false) |

**Response (202 Accepted):**
```json
{
  "status": "STARTED",
  "ingestionId": "ingest-20250117-130000",
  "message": "Incremental sync started for INCIDENTS"
}
```

### 5.2 Get Ingestion Status

Check the status of sync operations.

**Endpoint:** `GET /api/v1/admin/ingestion/status`

**Response (200 OK):**
```json
{
  "status": "IDLE",
  "lastSync": {
    "sourceType": "INCIDENTS",
    "lastSyncTimestamp": 1705484800,
    "status": "COMPLETED",
    "recordsProcessed": 1523
  },
  "activeIngestions": []
}
```

**Status Values:**
- `IDLE`: No active sync
- `RUNNING`: Sync in progress
- `COMPLETED`: Sync finished
- `FAILED`: Sync failed with errors

---

## 6. Admin APIs

### 6.1 System Statistics

Get system statistics and metrics.

**Endpoint:** `GET /api/v1/admin/stats`

**Response (200 OK):**
```json
{
  "database": {
    "totalEmbeddings": 15234,
    "totalSources": 4521,
    "sourcesByType": {
      "INC": 2156,
      "KB": 1234,
      "WO": 856,
      "CR": 275
    }
  },
  "cache": {
    "hitCount": 4523,
    "missCount": 234,
    "evictionCount": 12,
    "estimatedSize": 156
  },
  "sync": {
    "lastSync": "2025-01-17T13:00:00Z",
    "status": "IDLE"
  }
}
```

### 6.2 Clear Embeddings

Delete all embeddings for a specific source type.

**Endpoint:** `DELETE /api/v1/admin/ingestion/embeddings/{sourceType}`

**Path Parameters:**
- `sourceType`: Source type (INCIDENTS, KNOWLEDGE, WORKLOGS, ALL)

**Response (204 No Content):** Embeddings deleted successfully

---

## 7. WebSocket APIs

### 7.1 WebSocket Chat Stream

Real-time streaming chat responses.

**Endpoint:** `ws://localhost:8080/ws-chat/websocket`

**Connection Handshake:**

1. Client connects with WebSocket
2. Server accepts connection
3. Client sends subscribe message

**Client → Server (Subscribe):**
```json
{
  "sessionId": "session-123",
  "question": "How do I reset my VPN?",
  "userGroups": ["IT Support"]
}
```

**Server → Client (Streaming Response):**

The server sends multiple message chunks:

1. **Thinking Start** (optional):
```json
{
  "type": "THINKING",
  "sessionId": "session-123"
}
```

2. **Token Chunks** (multiple):
```json
{
  "type": "TOKEN",
  "sessionId": "session-123",
  "token": "I can"
}
```

3. **Complete**:
```json
{
  "type": "COMPLETE",
  "sessionId": "session-123",
  "sources": ["KB0001234"],
  "confidence": 0.92
}
```

**Message Types:**

| Type | Description |
|------|-------------|
| `THINKING` | LLM is thinking (optional, only if thinking mode enabled) |
| `TOKEN` | Response token chunk |
| `COMPLETE` | Response complete with sources |
| `ERROR` | Error occurred |

---

## 8. Session Management

### 8.1 List Sessions

Get all chat sessions.

**Endpoint:** `GET /api/v1/chat/sessions`

**Response (200 OK):**
```json
{
  "sessions": [
    {
      "sessionId": "session-123",
      "messageCount": 5,
      "lastActivity": "2025-01-17T13:00:00Z",
      "createdAt": "2025-01-17T12:30:00Z"
    }
  ]
}
```

### 8.2 Get Session History

Get conversation history for a session.

**Endpoint:** `GET /api/v1/chat/sessions/{sessionId}/history`

**Path Parameters:**
- `sessionId`: Session identifier

**Response (200 OK):**
```json
{
  "history": [
    {
      "role": "USER",
      "content": "How do I reset my VPN password?",
      "timestamp": "2025-01-17T12:30:00Z"
    },
    {
      "role": "ASSISTANT",
      "content": "I can help you reset your VPN password...",
      "timestamp": "2025-01-17T12:30:05Z"
    }
  ]
}
```

### 8.3 Clear Session

Delete session history.

**Endpoint:** `DELETE /api/v1/chat/sessions/{sessionId}`

**Response (204 No Content):** Session deleted successfully

---

## 9. Error Responses

All error responses follow this format:

```json
{
  "timestamp": "2025-01-17T13:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Detailed error message",
  "path": "/api/v1/chat",
  "traceId": "abc-123"  // Only in development
}
```

### HTTP Status Codes

| Code | Title | Description |
|------|-------|-------------|
| 200 | OK | Request successful |
| 201 | Created | Resource created |
| 204 | No Content | Resource deleted successfully |
| 400 | Bad Request | Invalid request parameters |
| 401 | Unauthorized | Authentication required |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource not found |
| 409 | Conflict | Resource conflict |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Server error |
| 503 | Service Unavailable | Service temporarily unavailable |

### Common Error Scenarios

#### 1. Missing Required Field
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "sessionId is required",
  "field": "sessionId"
}
```

#### 2. ReBAC Access Denied
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "You don't have permission to access this resource",
  "requiredGroups": ["IT Support", "Admin"],
  "yourGroups": ["Finance"]
}
```

#### 3. No Context Found
```json
{
  "response": "I don't have enough information in the knowledge base to answer this question.",
  "sources": [],
  "hasContext": false,
  "confidence": 0.0
}
```

#### 4. Rate Limit Exceeded
```json
{
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Please try again later.",
  "retryAfter": 60
}
```

---

## 10. Rate Limiting

### 10.1 Rate Limit Configuration

Default rate limits (configurable via application properties):

| Endpoint | Limit | Window |
|----------|-------|--------|
| POST /api/v1/chat | 10 requests | 1 minute |
| POST /api/v1/chat/search | 30 requests | 1 minute |
| GET /api/v1/chat/sessions | 100 requests | 1 minute |
| DELETE /api/v1/chat/sessions/* | 20 requests | 1 minute |

### 10.2 Rate Limit Response

```json
{
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Please try again later.",
  "retryAfter": 60,
  "limit": 10,
  "window": "60s"
}
```

### 10.3 Rate Limit Headers

Response includes rate limit information:

```
X-RateLimit-Limit: 10
X-RateLimit-Remaining: 7
X-RateLimit-Reset: 1705484860
```

---

## Appendix A: Source Type Codes

| Code | Description | Examples |
|------|-------------|----------|
| INC | Incident | INC0001234, INC0005678 |
| KB | Knowledge Article | KB0000123, KB0004567 |
| WO | Work Order | WO0000345, WO0000789 |
| CR | Change Request | CR0000123, CR0000456 |

---

## Appendix B: Session ID Format

Session IDs should be unique identifiers. Recommended formats:

- UUID: `550e8400-e29b-41d4-a716-446655440000`
- Random string: `session-abc123def456`
- User-timestamp: `user123-1705484800`

**Best Practices:**
- Use UUIDs for guaranteed uniqueness
- Include timestamp for debugging
- Keep under 255 characters

---

## Appendix C: Bilingual Support

### English Query
```
Question: "How do I reset my VPN password?"
Response: English with English citations
```

### Arabic Query
```
Question: "كيف أعيد تعيين كلمة مرور الشبكة؟"
Response: Arabic with English citations
```

### Mixed Query
```
Question: "كيف VPN reset؟"
Response: Response in language of PRIMARY question
```

---

**API Version:** 1.0.0
**Last Updated:** 2025-01-17
