"""
title: BMC Remedy Incident Manager
author: BMC Remedy RAG Team
version: 1.0.0
description: Search, view, create, and update incidents in BMC Remedy
requirements: requests,pydantic
license: MIT
"""

import json
import requests
from typing import Optional, List, Dict, Any
from pydantic import BaseModel, Field


class Valves(BaseModel):
    """Configuration for the BMC Remedy Incident Manager tool."""

    remedy_api_url: str = Field(
        default="http://host.docker.internal:8080",
        description="BMC Remedy RAG API URL (host.docker.internal for Docker, localhost for local dev)",
    )
    timeout: int = Field(default=30, description="Request timeout in seconds")
    verify_ssl: bool = Field(default=False, description="Verify SSL certificates")


class UserValves(BaseModel):
    """User-configurable settings."""

    default_impact: int = Field(
        default=3, ge=1, le=4, description="Default impact level (1-4)"
    )
    default_urgency: int = Field(
        default=3, ge=1, le=4, description="Default urgency level (1-4)"
    )
    max_results: int = Field(
        default=5, ge=1, le=20, description="Default max search results"
    )


class Tools:
    """BMC Remedy Incident Manager - Tools for incident operations."""

    def __init__(self):
        self.valves = Valves()

    def _get_session_id(self, __user__: Optional[Dict] = None) -> str:
        """Get or generate a session ID for the current user."""
        if __user__:
            # Use user ID as session identifier for consistent session tracking
            user_id = __user__.get("id") or __user__.get("email") or "anonymous"
            return f"openwebui-{user_id}"
        return "openwebui-default"

    def _make_request(
        self,
        method: str,
        endpoint: str,
        data: Optional[Dict] = None,
        params: Optional[Dict] = None,
        session_id: Optional[str] = None,
    ) -> Dict:
        """Make HTTP request to the Tool Server API."""
        url = f"{self.valves.remedy_api_url}/tool-server{endpoint}"

        headers = {"Content-Type": "application/json"}
        if session_id:
            headers["X-Session-Id"] = session_id

        try:
            if method == "GET":
                response = requests.get(
                    url,
                    params=params,
                    headers=headers,
                    timeout=self.valves.timeout,
                    verify=self.valves.verify_ssl,
                )
            elif method == "POST":
                response = requests.post(
                    url,
                    json=data,
                    params=params,
                    headers=headers,
                    timeout=self.valves.timeout,
                    verify=self.valves.verify_ssl,
                )
            elif method == "PUT":
                response = requests.put(
                    url,
                    json=data,
                    params=params,
                    headers=headers,
                    timeout=self.valves.timeout,
                    verify=self.valves.verify_ssl,
                )
            else:
                return {"error": f"Unsupported method: {method}"}

            response.raise_for_status()
            return response.json()

        except requests.exceptions.Timeout:
            return {"error": "Request timed out. Please try again."}
        except requests.exceptions.ConnectionError:
            return {"error": "Cannot connect to BMC Remedy API. Ensure the backend is running."}
        except requests.exceptions.HTTPError as e:
            return {"error": f"HTTP error: {e.response.status_code} - {e.response.text}"}
        except Exception as e:
            return {"error": f"Unexpected error: {str(e)}"}

    def search_incidents(
        self,
        query: str,
        max_results: Optional[int] = None,
        __user__: Optional[Dict] = None,
    ) -> str:
        """
        Search for similar incidents in BMC Remedy using semantic search.
        Use this to find related incidents before creating new ones, or to look up
        incidents matching a description.

        :param query: Description of the issue to search for
        :param max_results: Maximum number of results to return (default: 5)
        :return: Formatted list of similar incidents with relevance scores
        """
        # Get user-specific settings
        user_max = 5
        if __user__ and hasattr(__user__, "valves") and __user__.valves:
            user_max = __user__.valves.max_results

        limit = max_results if max_results else user_max

        result = self._make_request(
            "POST",
            "/incidents/search",
            data={"query": query, "limit": limit, "minScore": 0.3},
        )

        if "error" in result:
            return f"**Error:** {result['error']}"

        results = result.get("results", [])
        if not results:
            return "No similar incidents found in BMC Remedy."

        # Format results
        output = [f"**Found {len(results)} similar incident(s):**\n"]

        for item in results:
            score_pct = item.get("scorePercent", 0)
            incident_id = item.get("id", "Unknown")
            title = item.get("title", "No title")
            status = item.get("status", "Unknown")
            snippet = item.get("snippet", "")

            output.append(f"### {incident_id} ({score_pct}% match)")
            output.append(f"**Summary:** {title}")
            output.append(f"**Status:** {status}")
            if snippet:
                output.append(f"**Preview:** {snippet[:200]}...")
            output.append("")

        # Add duplicate warning if applicable
        if result.get("hasPotentialDuplicates"):
            output.append(
                "\n> **Warning:** Highly similar incidents found. "
                "Review these before creating a new incident."
            )

        return "\n".join(output)

    def get_incident_details(
        self,
        incident_id: str,
        __user__: Optional[Dict] = None,
    ) -> str:
        """
        Get full details of a specific incident including description, status,
        work logs, and resolution information.

        :param incident_id: Incident number (e.g., INC000000001)
        :return: Formatted incident details
        """
        result = self._make_request("GET", f"/incidents/{incident_id}")

        if "error" in result:
            return f"**Error:** {result['error']}"

        if not result.get("found", True):
            return f"**Incident {incident_id} not found.**"

        # Format the incident details
        output = [f"# Incident: {result.get('incidentNumber', incident_id)}\n"]

        output.append(f"**Summary:** {result.get('summary', 'N/A')}")
        output.append(f"**Status:** {result.get('status', 'N/A')}")
        output.append("")

        # Impact/Urgency/Priority
        output.append("## Classification")
        output.append(f"- **Impact:** {result.get('impactLabel', result.get('impact', 'N/A'))}")
        output.append(f"- **Urgency:** {result.get('urgencyLabel', result.get('urgency', 'N/A'))}")
        output.append(f"- **Priority:** {result.get('priorityLabel', result.get('priority', 'N/A'))}")
        output.append("")

        # Assignment
        output.append("## Assignment")
        output.append(f"- **Assigned Group:** {result.get('assignedGroup', 'N/A')}")
        output.append(f"- **Assigned To:** {result.get('assignedTo', 'N/A')}")
        output.append(f"- **Submitter:** {result.get('submitter', 'N/A')}")
        output.append("")

        # Description
        if result.get("description"):
            output.append("## Description")
            output.append(result["description"])
            output.append("")

        # Resolution
        if result.get("resolution"):
            output.append("## Resolution")
            output.append(result["resolution"])
            output.append("")

        # Work logs
        work_logs = result.get("workLogs", [])
        if work_logs:
            output.append("## Recent Work Logs")
            for log in work_logs[:5]:
                output.append(f"### {log.get('type', 'Log')} - {log.get('submitDate', '')}")
                output.append(f"**By:** {log.get('submitter', 'Unknown')}")
                if log.get("notes"):
                    output.append(log["notes"][:500])
                output.append("")

        return "\n".join(output)

    def create_incident(
        self,
        summary: str,
        description: str,
        impact: Optional[int] = None,
        urgency: Optional[int] = None,
        category: Optional[str] = None,
        assigned_group: Optional[str] = None,
        __user__: Optional[Dict] = None,
        __event_emitter__: Optional[callable] = None,
    ) -> str:
        """
        Create a new incident in BMC Remedy. The incident will be staged
        for user confirmation before being created.

        Impact levels: 1=Extensive/Widespread, 2=Significant, 3=Moderate, 4=Minor
        Urgency levels: 1=Critical, 2=High, 3=Medium, 4=Low

        :param summary: Brief summary of the incident (max 255 characters)
        :param description: Detailed description of the issue
        :param impact: Impact level (1-4, default: 3)
        :param urgency: Urgency level (1-4, default: 3)
        :param category: Category for the incident (optional)
        :param assigned_group: Initial assignment group (optional)
        :return: Confirmation prompt with staged action details
        """
        # Get session ID for this user
        session_id = self._get_session_id(__user__)

        # Get user defaults
        default_impact = 3
        default_urgency = 3
        if __user__ and hasattr(__user__, "valves") and __user__.valves:
            default_impact = __user__.valves.default_impact
            default_urgency = __user__.valves.default_urgency

        # Prepare request with session ID
        data = {
            "summary": summary,
            "description": description,
            "impact": impact if impact else default_impact,
            "urgency": urgency if urgency else default_urgency,
            "sessionId": session_id,
            "requireConfirmation": True,
        }

        if category:
            data["category"] = category
        if assigned_group:
            data["assignedGroup"] = assigned_group

        # Emit status if available
        if __event_emitter__:
            __event_emitter__(
                {"type": "status", "data": {"description": "Staging incident creation..."}}
            )

        result = self._make_request("POST", "/incidents", data=data, session_id=session_id)

        if "error" in result:
            return f"**Error:** {result['error']}"

        # Handle different response statuses
        status = result.get("status", "")

        if status == "STAGED":
            output = ["# Incident Staged for Confirmation\n"]
            output.append(result.get("preview", ""))
            output.append("")
            output.append(f"**Action ID:** `{result.get('actionId', 'N/A')}`")
            output.append(f"**Expires:** {result.get('expiresAt', 'N/A')}")
            output.append("")
            output.append("> To confirm: say **'confirm'** or use the confirm action")
            output.append("> To cancel: say **'cancel'**")
            return "\n".join(output)

        elif status == "DUPLICATE_WARNING":
            output = ["# Potential Duplicates Found\n"]
            output.append("> **Warning:** Similar incidents exist. Review before confirming.\n")

            similar = result.get("similarIncidents", [])
            for item in similar:
                output.append(f"- **{item.get('id')}**: {item.get('title')} ({item.get('scorePercent')}% match)")
            output.append("")
            output.append(result.get("preview", ""))
            output.append("")
            output.append(f"**Action ID:** `{result.get('actionId', 'N/A')}`")
            output.append("")
            output.append("> To create anyway: confirm the action")
            output.append("> To cancel: say **'cancel'**")
            return "\n".join(output)

        elif status == "RATE_LIMITED":
            return f"**Rate Limit Exceeded:** {result.get('message', 'Please wait before creating more incidents.')}"

        elif status == "CREATED":
            return f"**Success!** Incident **{result.get('incidentNumber')}** has been created."

        else:
            return f"**Error:** {result.get('message', 'Unknown error occurred')}"

    def update_incident(
        self,
        incident_id: str,
        status: Optional[str] = None,
        work_log_notes: Optional[str] = None,
        resolution: Optional[str] = None,
        __user__: Optional[Dict] = None,
    ) -> str:
        """
        Update an existing incident in BMC Remedy. You can change the status,
        add work log notes, or provide a resolution.

        Status values: New, Assigned, In Progress, Pending, Resolved, Closed

        :param incident_id: Incident number to update (e.g., INC000000001)
        :param status: New status for the incident (optional)
        :param work_log_notes: Notes to add to the work log (optional)
        :param resolution: Resolution notes if resolving the incident (optional)
        :return: Update confirmation
        """
        data = {"requireConfirmation": True}

        if status:
            data["status"] = status
        if work_log_notes:
            data["workLogNotes"] = work_log_notes
        if resolution:
            data["resolution"] = resolution

        if len(data) == 1:
            return "**Error:** No updates specified. Please provide status, work_log_notes, or resolution."

        result = self._make_request("PUT", f"/incidents/{incident_id}", data=data)

        if "error" in result:
            return f"**Error:** {result['error']}"

        status_resp = result.get("status", "")

        if status_resp == "STAGED":
            output = [f"# Update Staged for {incident_id}\n"]
            output.append(f"**Changes:** {result.get('preview', 'N/A')}")
            output.append("")
            output.append(f"**Action ID:** `{result.get('actionId', 'N/A')}`")
            output.append("")
            output.append("> To confirm: say **'confirm'**")
            return "\n".join(output)

        elif status_resp == "UPDATED":
            return f"**Success!** Incident **{incident_id}** has been updated."

        elif status_resp == "NOT_FOUND":
            return f"**Error:** Incident {incident_id} not found."

        else:
            return f"**{status_resp}:** {result.get('message', 'Update processed')}"

    def list_pending_actions(
        self,
        __user__: Optional[Dict] = None,
    ) -> str:
        """
        List all pending actions awaiting confirmation in the current session.

        :return: List of pending actions
        """
        session_id = self._get_session_id(__user__)
        params = {"sessionId": session_id}

        result = self._make_request("GET", "/actions/pending", params=params, session_id=session_id)

        if "error" in result:
            return f"**Error:** {result['error']}"

        if not result or len(result) == 0:
            return "No pending actions in this session."

        output = ["# Pending Actions\n"]

        for action in result:
            output.append(f"### {action.get('actionType', 'Action')}")
            output.append(f"**ID:** `{action.get('actionId', 'N/A')}`")
            output.append(f"**Status:** {action.get('status', 'N/A')}")
            output.append(f"**Expires:** {action.get('expiresAt', 'N/A')}")
            output.append("")

        output.append("> To confirm: `confirm <action_id>`")
        output.append("> To cancel: `cancel <action_id>`")

        return "\n".join(output)

    def confirm_action(
        self,
        action_id: str,
        __user__: Optional[Dict] = None,
    ) -> str:
        """
        Confirm a staged action to execute it (e.g., create the incident).

        :param action_id: The action ID to confirm
        :return: Confirmation result
        """
        session_id = self._get_session_id(__user__)
        params = {"actionId": action_id, "sessionId": session_id}

        result = self._make_request("POST", "/actions/confirm", params=params, session_id=session_id)

        if "error" in result:
            return f"**Error:** {result['error']}"

        status = result.get("status", "")

        if status == "EXECUTED":
            record_id = result.get("recordId", "N/A")
            record_type = result.get("recordType", "Record")
            return f"**Success!** {record_type} **{record_id}** has been created."

        elif status == "EXPIRED":
            return "**Action Expired:** This action has expired. Please create a new request."

        elif status == "NOT_FOUND":
            return "**Not Found:** This action was not found or has already been processed."

        else:
            return f"**{status}:** {result.get('message', 'Action processed')}"

    def cancel_action(
        self,
        action_id: str,
        __user__: Optional[Dict] = None,
    ) -> str:
        """
        Cancel a staged action without executing it.

        :param action_id: The action ID to cancel
        :return: Cancellation result
        """
        session_id = self._get_session_id(__user__)
        params = {"actionId": action_id, "sessionId": session_id}

        result = self._make_request("POST", "/actions/cancel", params=params, session_id=session_id)

        if "error" in result:
            return f"**Error:** {result['error']}"

        if result.get("status") == "CANCELLED":
            return "**Action Cancelled:** The action has been cancelled successfully."

        return f"**{result.get('status', 'Unknown')}:** {result.get('message', 'Action processed')}"
