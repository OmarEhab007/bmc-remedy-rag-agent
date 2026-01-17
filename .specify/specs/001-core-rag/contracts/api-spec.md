# API Contracts: Core RAG Functionality

**Spec Reference**: 001-core-rag
**Version**: 1.0
**Last Updated**: 2024-01-15

---

## 1. REST API Endpoints

### 1.1 POST /api/v1/chat

Synchronous chat endpoint for single question-answer interactions.

**Request**
```json
{
  "sessionId": "string (optional, UUID)",
  "userId": "string (required)",
  "userGroups": ["string"] ,
  "text": "string (required, max 4000 chars)",
  "skipContext": false
}
```

**Response**
```json
{
  "sessionId": "string (UUID)",
  "response": "string",
  "sources": ["INC000001234", "KA000005678"],
  "hasContext": true,
  "timestamp": 1705334400000
}
```

**Status Codes**
| Code | Description |
|------|-------------|
| 200 | Success |
| 400 | Invalid request (missing required fields) |
| 401 | Unauthorized (invalid/missing JWT) |
| 429 | Rate limit exceeded |
| 500 | Internal server error |
| 503 | LLM service unavailable |

**Example**
```bash
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <jwt>" \
  -d '{
    "userId": "user123",
    "userGroups": ["ServiceDesk", "NetworkOps"],
    "text": "How do I reset a user password in Active Directory?"
  }'
```

---

### 1.2 POST /api/v1/chat/search

Semantic search endpoint that returns relevant documents without LLM generation.

**Request**
```json
{
  "userId": "string (required)",
  "userGroups": ["string"],
  "query": "string (required, max 1000 chars)",
  "limit": 10,
  "offset": 0,
  "sourceTypes": ["INCIDENT", "KNOWLEDGE", "CHANGE", "WORKORDER"],
  "prioritizeKnowledge": true,
  "deduplicate": true
}
```

**Response**
```json
{
  "resultCount": 10,
  "results": [
    {
      "sourceType": "KNOWLEDGE",
      "sourceId": "KA000012345",
      "chunkType": "ARTICLE_CONTENT",
      "content": "To reset a password in Active Directory...",
      "title": "Password Reset Procedure",
      "category": "Identity Management",
      "score": 0.92,
      "metadata": {
        "assigned_group": "ServiceDesk",
        "author": "admin",
        "created_date": "1704067200"
      }
    }
  ]
}
```

**Status Codes**
| Code | Description |
|------|-------------|
| 200 | Success |
| 400 | Invalid request |
| 401 | Unauthorized |
| 429 | Rate limit exceeded |
| 500 | Internal server error |

---

### 1.3 GET /api/v1/chat/sessions/{sessionId}

Retrieve session history.

**Response**
```json
{
  "sessionId": "uuid",
  "userId": "user123",
  "createdAt": "2024-01-15T10:30:00Z",
  "lastActivityAt": "2024-01-15T10:35:00Z",
  "messageCount": 4,
  "messages": [
    {
      "messageId": "uuid",
      "role": "USER",
      "content": "How do I reset a password?",
      "timestamp": "2024-01-15T10:30:00Z"
    },
    {
      "messageId": "uuid",
      "role": "ASSISTANT",
      "content": "To reset a password in Active Directory...",
      "citations": [
        {
          "sourceType": "KNOWLEDGE",
          "sourceId": "KA000012345",
          "title": "Password Reset Procedure",
          "score": 0.92
        }
      ],
      "timestamp": "2024-01-15T10:30:05Z"
    }
  ]
}
```

---

### 1.4 DELETE /api/v1/chat/sessions/{sessionId}

End a chat session and clear its history.

**Response**
```json
{
  "message": "Session terminated",
  "sessionId": "uuid"
}
```

---

## 2. WebSocket API

### 2.1 Connection

**Endpoint**: `ws://localhost:8080/ws/chat`

**Connection Headers**
```
Authorization: Bearer <jwt>
Sec-WebSocket-Protocol: v1.chat
```

**Connection Acknowledgment**
```json
{
  "messageId": null,
  "sessionId": null,
  "token": null,
  "isComplete": false,
  "citations": null,
  "confidenceScore": null,
  "error": null,
  "type": "CONNECTED"
}
```

---

### 2.2 ChatQueryMessage (Client -> Server)

Sent by client to submit a query.

```json
{
  "messageId": "uuid",
  "text": "How do I troubleshoot VPN connectivity issues?",
  "sessionId": "uuid (optional)",
  "userId": "user123",
  "userGroups": ["NetworkOps", "ServiceDesk"],
  "timestamp": "2024-01-15T10:30:00Z",
  "skipContext": false
}
```

**Field Descriptions**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| messageId | UUID | Yes | Unique message ID for correlation |
| text | String | Yes | User's question (max 4000 chars) |
| sessionId | UUID | No | Existing session ID for context |
| userId | String | Yes | User identifier from JWT |
| userGroups | String[] | Yes | User's group memberships for ReBAC |
| timestamp | ISO8601 | Yes | Message timestamp |
| skipContext | Boolean | No | Skip RAG retrieval (default: false) |

---

### 2.3 ChatResponseChunk (Server -> Client)

Sent by server during response generation.

**THINKING chunk** (processing started)
```json
{
  "messageId": "uuid",
  "sessionId": "uuid",
  "token": null,
  "isComplete": false,
  "citations": null,
  "confidenceScore": null,
  "error": null,
  "type": "THINKING"
}
```

**TOKEN chunk** (streaming text)
```json
{
  "messageId": "uuid",
  "sessionId": "uuid",
  "token": "To troubleshoot",
  "isComplete": false,
  "citations": null,
  "confidenceScore": null,
  "error": null,
  "type": "TOKEN"
}
```

**COMPLETE chunk** (final message)
```json
{
  "messageId": "uuid",
  "sessionId": "uuid",
  "token": null,
  "isComplete": true,
  "citations": [
    {
      "sourceType": "INCIDENT",
      "sourceId": "INC000054321",
      "title": "VPN Connection Failure - Cisco AnyConnect",
      "score": 0.89
    },
    {
      "sourceType": "KNOWLEDGE",
      "sourceId": "KA000098765",
      "title": "VPN Troubleshooting Guide",
      "score": 0.85
    }
  ],
  "confidenceScore": 0.87,
  "error": null,
  "type": "COMPLETE"
}
```

**ERROR chunk**
```json
{
  "messageId": "uuid",
  "sessionId": null,
  "token": null,
  "isComplete": true,
  "citations": null,
  "confidenceScore": null,
  "error": "LLM service unavailable. Please try again.",
  "type": "ERROR"
}
```

---

### 2.4 ChunkType Enum

| Value | Description |
|-------|-------------|
| CONNECTED | WebSocket connection established |
| THINKING | Query received, processing started |
| TOKEN | Streaming text token |
| COMPLETE | Response complete with metadata |
| ERROR | Error occurred |

---

## 3. Error Responses

### 3.1 Standard Error Format

```json
{
  "error": {
    "code": "ERR_001",
    "message": "Human readable message",
    "details": {
      "field": "additional context"
    },
    "timestamp": "2024-01-15T10:30:00Z"
  }
}
```

### 3.2 Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| ERR_001 | 400 | Invalid request format |
| ERR_002 | 400 | Missing required field |
| ERR_003 | 400 | Text exceeds maximum length |
| ERR_004 | 401 | Invalid or expired JWT |
| ERR_005 | 403 | Insufficient permissions |
| ERR_006 | 404 | Session not found |
| ERR_007 | 429 | Rate limit exceeded |
| ERR_008 | 500 | Internal server error |
| ERR_009 | 503 | LLM service unavailable |
| ERR_010 | 503 | Vector store unavailable |

---

## 4. Rate Limiting

| Endpoint | Limit | Window |
|----------|-------|--------|
| POST /api/v1/chat | 10 requests | 1 minute |
| POST /api/v1/chat/search | 30 requests | 1 minute |
| WebSocket messages | 20 messages | 1 minute |

Rate limit headers included in responses:
```
X-RateLimit-Limit: 10
X-RateLimit-Remaining: 7
X-RateLimit-Reset: 1705334460
```

---

## 5. WebSocket Client Implementation Notes

### 5.1 Connection Lifecycle

1. Open WebSocket connection with JWT in header
2. Wait for CONNECTED message
3. Send ChatQueryMessage
4. Receive THINKING, TOKEN*, COMPLETE sequence
5. Maintain connection for session continuity
6. Handle reconnection on disconnect

### 5.2 Heartbeat

- Client should send ping every 30 seconds
- Server responds with pong
- Connection closed if no activity for 60 seconds

### 5.3 Reconnection Strategy

```
1. Exponential backoff: 1s, 2s, 4s, 8s, 16s (max)
2. Preserve sessionId for context continuity
3. Max reconnection attempts: 5
4. Reset attempt counter on successful connection
```

---

## 6. Security Headers

All responses include:
```
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Content-Security-Policy: default-src 'self'
Strict-Transport-Security: max-age=31536000; includeSubDomains
```

---

## 7. Versioning

API version included in URL path (`/api/v1/...`).

Breaking changes will increment version number.
Non-breaking additions allowed within version.
