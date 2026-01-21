# Open WebUI Tools for BMC Remedy RAG

This directory contains Python tools and pipes for integrating BMC Remedy agentic capabilities with Open WebUI.

## Overview

| Component | Type | Description |
|-----------|------|-------------|
| `bmc_remedy_incidents.py` | Tool | Incident CRUD operations |
| `bmc_knowledge_search.py` | Tool | Knowledge base search |
| `it_support_agent_pipe.py` | Pipe | Workflow orchestration |

## Prerequisites

1. **BMC Remedy RAG Backend** running with agentic features enabled:
   ```yaml
   # In application.yml
   agentic:
     enabled: true
   ```

2. **Tool Server Endpoints** available at:
   - `http://host.docker.internal:8080/tool-server/`

## Installation

### Method 1: Manual Installation via Admin UI

1. Log into Open WebUI as an administrator
2. Navigate to **Admin Panel → Tools**
3. Click **Add Tool** (+ button)
4. Copy and paste the contents of each `.py` file
5. Click **Save**

### Method 2: Tool Server Connection (Recommended)

1. Open WebUI supports connecting to OpenAPI-compliant tool servers
2. Add the following environment variable to your Open WebUI deployment:

```yaml
environment:
  - TOOL_SERVER_CONNECTIONS=[{"url":"http://host.docker.internal:8080","path":"/tool-server/openapi.json","type":"openapi","auth_type":"none","config":{"enable":true}}]
```

This will auto-discover and register all tool server endpoints.

## Tool Details

### BMC Remedy Incident Manager (`bmc_remedy_incidents.py`)

**Functions:**

| Function | Description |
|----------|-------------|
| `search_incidents(query)` | Semantic search for similar incidents |
| `get_incident_details(incident_id)` | Get full incident details |
| `create_incident(summary, description, impact, urgency)` | Create new incident (staged) |
| `update_incident(incident_id, ...)` | Update incident status/notes |
| `confirm_action(action_id)` | Confirm a staged action |
| `cancel_action(action_id)` | Cancel a staged action |
| `list_pending_actions()` | List pending confirmations |

**Configuration (Valves):**

| Setting | Default | Description |
|---------|---------|-------------|
| `remedy_api_url` | `http://host.docker.internal:8080` | Backend API URL |
| `timeout` | 30 | Request timeout seconds |
| `verify_ssl` | false | SSL certificate verification |

**User Settings (UserValves):**

| Setting | Default | Description |
|---------|---------|-------------|
| `default_impact` | 3 | Default impact level (1-4) |
| `default_urgency` | 3 | Default urgency level (1-4) |
| `max_results` | 5 | Max search results |

### BMC Knowledge Base Search (`bmc_knowledge_search.py`)

**Functions:**

| Function | Description |
|----------|-------------|
| `search_knowledge(query)` | Search KB articles |
| `get_article(article_id)` | Get full article content |
| `search_solutions(problem)` | Search solutions across KB and incidents |
| `find_how_to(task)` | Find how-to guides |
| `search_work_orders(query)` | Search work orders |

### IT Support Agent Pipe (`it_support_agent_pipe.py`)

The pipe provides intelligent workflow orchestration:

**Intent Detection:**
- `create_incident` - Creating new tickets
- `search_incidents` - Finding similar issues
- `get_incident` - Viewing ticket details
- `search_knowledge` - Finding documentation
- `confirm_action` - Confirming staged operations
- `cancel_action` - Cancelling staged operations

**Automatic Features:**
1. **Duplicate Detection**: Automatically searches for similar incidents before creation
2. **KB Suggestions**: Suggests relevant knowledge articles for reported issues
3. **Context Enrichment**: Adds relevant information to the conversation context

**Configuration:**

| Setting | Default | Description |
|---------|---------|-------------|
| `enable_auto_search` | true | Auto-search for duplicates |
| `require_confirmation` | true | Require confirmation for writes |
| `max_similar_incidents` | 3 | Max duplicates to show |
| `enable_kb_suggestions` | true | Suggest KB articles |

## Usage Examples

### Search for Incidents
```
User: Find incidents about VPN connection issues
Assistant: [Uses search_incidents tool]
```

### Create an Incident
```
User: Create an incident for email sync failure
Assistant: [Searches for duplicates, then stages creation]
         "Found 2 similar incidents. Review before confirming."

User: confirm
Assistant: [Creates incident]
         "Incident INC000001234 created successfully."
```

### Get Knowledge
```
User: How do I reset my password?
Assistant: [Uses search_knowledge tool]
```

## API Endpoints Used

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/tool-server/incidents/search` | POST | Search incidents |
| `/tool-server/incidents/{id}` | GET | Get incident details |
| `/tool-server/incidents` | POST | Create incident |
| `/tool-server/incidents/{id}` | PUT | Update incident |
| `/tool-server/knowledge/search` | POST | Search KB |
| `/tool-server/knowledge/{id}` | GET | Get KB article |
| `/tool-server/actions/confirm` | POST | Confirm action |
| `/tool-server/actions/cancel` | POST | Cancel action |
| `/tool-server/actions/pending` | GET | List pending |
| `/tool-server/openapi.json` | GET | OpenAPI spec |

## Troubleshooting

### "Cannot connect to BMC Remedy API"
- Verify backend is running on port 8080
- Check `host.docker.internal` resolves correctly
- For Linux, ensure `extra_hosts` is configured in docker-compose

### "Tool not available"
- Verify `agentic.enabled=true` in backend config
- Check that ToolServerController is loaded (check logs)
- Verify OpenAPI spec is accessible at `/tool-server/openapi.json`

### "Rate limit exceeded"
- Default: 10 incident creations per hour per user
- Configure via `agentic.rateLimit.maxCreationsPerHour`

## Security Notes

1. All write operations require user confirmation by default
2. Rate limiting prevents abuse
3. Input validation on all fields
4. No credentials stored in tools (uses backend authentication)
