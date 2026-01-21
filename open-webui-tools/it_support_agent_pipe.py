"""
title: IT Support Agent
author: BMC Remedy RAG Team
version: 1.0.0
description: Intelligent IT support agent that orchestrates incident management, knowledge search, and automated workflows
requirements: requests,pydantic
license: MIT
"""

import json
import re
import requests
from typing import Optional, Dict, List, Any, Callable, Awaitable
from pydantic import BaseModel, Field


class Valves(BaseModel):
    """Configuration for the IT Support Agent pipe."""

    api_url: str = Field(
        default="http://host.docker.internal:8080",
        description="BMC Remedy RAG API URL",
    )
    enable_auto_search: bool = Field(
        default=True, description="Automatically search for duplicates before creating incidents"
    )
    require_confirmation: bool = Field(
        default=True, description="Require user confirmation for write operations"
    )
    max_similar_incidents: int = Field(
        default=3, ge=1, le=10, description="Max similar incidents to show"
    )
    enable_kb_suggestions: bool = Field(
        default=True, description="Suggest relevant KB articles for issues"
    )
    timeout: int = Field(default=30, description="Request timeout in seconds")


class Pipe:
    """
    IT Support Agent Pipeline.

    This pipe acts as an intelligent orchestrator for IT support workflows:
    1. Detects user intent (search, create, update, escalate, help)
    2. For incident creation: Auto-searches for duplicates first
    3. Presents similar incidents and KB articles if found
    4. Stages actions requiring confirmation
    5. Handles confirm/cancel commands
    6. Enriches context with relevant knowledge
    """

    def __init__(self):
        self.valves = Valves()
        self.name = "IT Support Agent"

        # Intent detection patterns
        self.intent_patterns = {
            "create_incident": [
                r"(?i)(create|open|submit|log|raise|file|new)\s+(a\s+)?(incident|ticket|issue|request)",
                r"(?i)(i\s+need|i\s+want|can\s+you|please)\s+(a\s+)?(new\s+)?(incident|ticket)",
                r"(?i)^(incident|ticket)\s+for\s+",
                r"(?i)(report|log)\s+(a\s+)?(problem|issue)",
                # Arabic
                r"(?i)(أنشئ|افتح|سجل|ارفع)\s+(بلاغ|تذكرة|حادثة)",
            ],
            "search_incidents": [
                r"(?i)(search|find|look\s+for|check)\s+(for\s+)?(similar\s+)?(incidents?|tickets?|issues?)",
                r"(?i)(any|are\s+there)\s+(similar\s+)?(incidents?|tickets?|issues?)",
                r"(?i)show\s+(me\s+)?(incidents?|tickets?)",
            ],
            "get_incident": [
                r"(?i)(get|show|what\s+is|details?\s+(for|of|about))\s+(incident|ticket)?\s*(INC\d+)",
                r"(?i)(INC\d+)",
            ],
            "search_knowledge": [
                r"(?i)(how\s+to|how\s+do\s+i|steps?\s+(to|for)|guide\s+(to|for)|procedure\s+(to|for))",
                r"(?i)(search|find|look\s+in)\s+(the\s+)?(knowledge\s+base|kb|documentation)",
                r"(?i)(solution|fix|resolve|troubleshoot)",
            ],
            "confirm_action": [
                r"(?i)^(confirm|yes|proceed|approve|ok|go\s+ahead)",
                r"(?i)confirm\s+([a-zA-Z0-9]+)",
            ],
            "cancel_action": [
                r"(?i)^(cancel|no|stop|abort|decline|nevermind)",
                r"(?i)cancel\s+([a-zA-Z0-9]+)",
            ],
            "list_pending": [
                r"(?i)(list|show|what\s+are)\s+(my\s+)?(pending|staged)\s+(actions?|requests?)",
            ],
            "help": [
                r"(?i)^(help|what\s+can\s+you\s+do|\?)",
                r"(?i)(capabilities|features|functions)",
            ],
        }

    async def pipe(
        self,
        body: Dict,
        __user__: Optional[Dict] = None,
        __event_emitter__: Optional[Callable[[Dict], Awaitable[None]]] = None,
    ) -> Dict:
        """
        Main pipeline entry point.

        Processes incoming messages and orchestrates the IT support workflow:
        1. Extracts the latest user message
        2. Detects intent
        3. Performs appropriate actions
        4. Enriches context for the LLM
        """
        messages = body.get("messages", [])
        if not messages:
            return body

        last_message = messages[-1].get("content", "") if messages else ""

        # Detect intent
        intent, match_data = self._detect_intent(last_message)

        # Handle different intents
        if intent == "confirm_action":
            await self._handle_confirm(last_message, __event_emitter__)

        elif intent == "cancel_action":
            await self._handle_cancel(last_message, __event_emitter__)

        elif intent == "create_incident":
            # Add pre-creation context
            await self._enrich_for_creation(last_message, body, __event_emitter__)

        elif intent == "search_knowledge":
            # Add KB search context
            await self._enrich_with_knowledge(last_message, body, __event_emitter__)

        elif intent == "get_incident":
            # Extract incident ID and add context
            incident_id = self._extract_incident_id(last_message)
            if incident_id:
                await self._enrich_with_incident(incident_id, body, __event_emitter__)

        elif intent == "list_pending":
            await self._list_pending_actions(body, __event_emitter__)

        elif intent == "help":
            self._add_help_context(body)

        return body

    def _detect_intent(self, message: str) -> tuple:
        """Detect the user's intent from their message."""
        for intent, patterns in self.intent_patterns.items():
            for pattern in patterns:
                match = re.search(pattern, message)
                if match:
                    return intent, match

        return "general", None

    def _extract_incident_id(self, message: str) -> Optional[str]:
        """Extract incident ID from message."""
        match = re.search(r"(INC\d+)", message, re.IGNORECASE)
        if match:
            return match.group(1).upper()
        return None

    async def _enrich_for_creation(
        self,
        message: str,
        body: Dict,
        emitter: Optional[Callable],
    ):
        """Enrich context before incident creation."""
        if not self.valves.enable_auto_search:
            return

        if emitter:
            await emitter(
                {"type": "status", "data": {"description": "Searching for similar incidents..."}}
            )

        # Search for similar incidents
        similar = await self._search_similar(message)

        # Search for relevant KB articles
        kb_articles = []
        if self.valves.enable_kb_suggestions:
            kb_articles = await self._search_knowledge(message)

        # Add context to messages
        context_parts = []

        if similar:
            context_parts.append(
                f"**Similar Incidents Found ({len(similar)}):**\n"
                + self._format_search_results(similar)
            )

        if kb_articles:
            context_parts.append(
                f"**Relevant Knowledge Articles ({len(kb_articles)}):**\n"
                + self._format_search_results(kb_articles)
            )

        if context_parts:
            context = "\n\n".join(context_parts)
            context += "\n\n> Review these before creating a new incident."

            body["messages"].append(
                {
                    "role": "system",
                    "content": f"[IT Support Agent Context]\n\n{context}",
                }
            )

            if emitter:
                await emitter(
                    {
                        "type": "status",
                        "data": {"description": f"Found {len(similar)} similar incident(s)"},
                    }
                )

    async def _enrich_with_knowledge(
        self,
        message: str,
        body: Dict,
        emitter: Optional[Callable],
    ):
        """Enrich context with relevant KB articles."""
        if emitter:
            await emitter(
                {"type": "status", "data": {"description": "Searching knowledge base..."}}
            )

        articles = await self._search_knowledge(message)

        if articles:
            context = (
                f"**Relevant Knowledge Articles ({len(articles)}):**\n"
                + self._format_search_results(articles)
            )

            body["messages"].append(
                {
                    "role": "system",
                    "content": f"[IT Support Agent Context]\n\n{context}",
                }
            )

            if emitter:
                await emitter(
                    {
                        "type": "status",
                        "data": {"description": f"Found {len(articles)} relevant article(s)"},
                    }
                )

    async def _enrich_with_incident(
        self,
        incident_id: str,
        body: Dict,
        emitter: Optional[Callable],
    ):
        """Add incident details to context."""
        if emitter:
            await emitter(
                {"type": "status", "data": {"description": f"Fetching {incident_id}..."}}
            )

        try:
            response = requests.get(
                f"{self.valves.api_url}/tool-server/incidents/{incident_id}",
                timeout=self.valves.timeout,
            )
            response.raise_for_status()
            incident = response.json()

            if incident.get("found", True):
                context = f"**Incident {incident_id} Details:**\n"
                context += f"- Summary: {incident.get('summary', 'N/A')}\n"
                context += f"- Status: {incident.get('status', 'N/A')}\n"
                context += f"- Priority: {incident.get('priorityLabel', 'N/A')}\n"
                context += f"- Assigned: {incident.get('assignedGroup', 'N/A')}\n"

                body["messages"].append(
                    {
                        "role": "system",
                        "content": f"[IT Support Agent Context]\n\n{context}",
                    }
                )

        except Exception as e:
            # Don't fail, just skip enrichment
            pass

    async def _handle_confirm(
        self,
        message: str,
        emitter: Optional[Callable],
    ):
        """Handle confirmation commands."""
        if emitter:
            await emitter(
                {"type": "status", "data": {"description": "Processing confirmation..."}}
            )

        # The actual confirmation is handled by the tool
        # This just provides status feedback

    async def _handle_cancel(
        self,
        message: str,
        emitter: Optional[Callable],
    ):
        """Handle cancellation commands."""
        if emitter:
            await emitter(
                {"type": "status", "data": {"description": "Processing cancellation..."}}
            )

    async def _list_pending_actions(
        self,
        body: Dict,
        emitter: Optional[Callable],
    ):
        """List pending actions for the session."""
        if emitter:
            await emitter(
                {"type": "status", "data": {"description": "Fetching pending actions..."}}
            )

        try:
            response = requests.get(
                f"{self.valves.api_url}/tool-server/actions/pending",
                params={"sessionId": "default"},
                timeout=self.valves.timeout,
            )
            response.raise_for_status()
            pending = response.json()

            if pending:
                context = f"**Pending Actions ({len(pending)}):**\n"
                for action in pending:
                    context += f"- {action.get('actionType')}: `{action.get('actionId')}`\n"
                context += "\nUse `confirm <id>` to execute or `cancel <id>` to abort."
            else:
                context = "No pending actions in this session."

            body["messages"].append(
                {
                    "role": "system",
                    "content": f"[IT Support Agent Context]\n\n{context}",
                }
            )

        except Exception as e:
            pass

    def _add_help_context(self, body: Dict):
        """Add help information to context."""
        help_text = """
**IT Support Agent Capabilities:**

**Incident Management:**
- `create incident for <description>` - Create a new incident
- `search incidents about <topic>` - Find similar incidents
- `show incident INC000001` - Get incident details
- `update incident INC000001` - Update an incident

**Knowledge Base:**
- `how to <task>` - Find how-to guides
- `search knowledge for <topic>` - Search KB articles
- `solution for <problem>` - Find solutions

**Workflow:**
- `confirm` - Confirm a staged action
- `cancel` - Cancel a staged action
- `list pending actions` - Show staged actions

**Tips:**
- I'll automatically search for duplicates before creating incidents
- I'll suggest relevant KB articles for your issues
- All write operations require your confirmation
"""
        body["messages"].append(
            {
                "role": "system",
                "content": f"[IT Support Agent Help]\n\n{help_text}",
            }
        )

    async def _search_similar(self, query: str) -> List[Dict]:
        """Search for similar incidents."""
        try:
            response = requests.post(
                f"{self.valves.api_url}/tool-server/incidents/search",
                json={
                    "query": query,
                    "limit": self.valves.max_similar_incidents,
                    "minScore": 0.5,
                },
                timeout=self.valves.timeout,
            )
            response.raise_for_status()
            return response.json().get("results", [])
        except Exception:
            return []

    async def _search_knowledge(self, query: str) -> List[Dict]:
        """Search knowledge base."""
        try:
            response = requests.post(
                f"{self.valves.api_url}/tool-server/knowledge/search",
                json={
                    "query": query,
                    "limit": 3,
                    "minScore": 0.4,
                },
                timeout=self.valves.timeout,
            )
            response.raise_for_status()
            return response.json().get("results", [])
        except Exception:
            return []

    def _format_search_results(self, results: List[Dict]) -> str:
        """Format search results for display."""
        lines = []
        for item in results:
            score = item.get("scorePercent", 0)
            item_id = item.get("id", "Unknown")
            title = item.get("title", "No title")
            lines.append(f"- **{item_id}** ({score}%): {title}")
        return "\n".join(lines)


# Additional filter for processing outgoing messages
class Filter:
    """
    Optional filter for post-processing responses.
    Can be used to add action buttons or format output.
    """

    def __init__(self):
        self.valves = Valves()

    async def outlet(
        self,
        body: Dict,
        __user__: Optional[Dict] = None,
    ) -> Dict:
        """
        Post-process outgoing messages.
        Can add action buttons, format citations, etc.
        """
        # Check if response contains a staged action
        messages = body.get("messages", [])
        if messages:
            last_message = messages[-1].get("content", "")

            # If we detect a staged action, we could add UI elements here
            if "Action ID:" in last_message and "confirm" in last_message.lower():
                # In a full implementation, we could inject action buttons
                pass

        return body
