"""
title: BMC Knowledge Base Search
author: BMC Remedy RAG Team
version: 1.0.0
description: Search IT knowledge articles, solutions, and documentation in BMC Remedy
requirements: requests,pydantic
license: MIT
"""

import json
import requests
from typing import Optional, Dict, List
from pydantic import BaseModel, Field


class Valves(BaseModel):
    """Configuration for the BMC Knowledge Base Search tool."""

    remedy_api_url: str = Field(
        default="http://host.docker.internal:8080",
        description="BMC Remedy RAG API URL",
    )
    timeout: int = Field(default=30, description="Request timeout in seconds")
    verify_ssl: bool = Field(default=False, description="Verify SSL certificates")


class UserValves(BaseModel):
    """User-configurable settings."""

    max_results: int = Field(
        default=5, ge=1, le=20, description="Default max search results"
    )
    show_full_content: bool = Field(
        default=False, description="Show full article content in search results"
    )


class Tools:
    """BMC Knowledge Base Search - Tools for knowledge article operations."""

    def __init__(self):
        self.valves = Valves()

    def _make_request(
        self,
        method: str,
        endpoint: str,
        data: Optional[Dict] = None,
        params: Optional[Dict] = None,
    ) -> Dict:
        """Make HTTP request to the Tool Server API."""
        url = f"{self.valves.remedy_api_url}/tool-server{endpoint}"

        try:
            if method == "GET":
                response = requests.get(
                    url,
                    params=params,
                    timeout=self.valves.timeout,
                    verify=self.valves.verify_ssl,
                )
            elif method == "POST":
                response = requests.post(
                    url,
                    json=data,
                    params=params,
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

    def search_knowledge(
        self,
        query: str,
        max_results: Optional[int] = None,
        category: Optional[str] = None,
        __user__: Optional[Dict] = None,
    ) -> str:
        """
        Search the IT knowledge base for solutions, how-to guides, and troubleshooting articles.
        Use this when you need to find documentation or solutions to common IT issues.

        :param query: Search query describing what you're looking for
        :param max_results: Maximum number of results to return (default: 5)
        :param category: Filter by category (optional)
        :return: List of matching knowledge articles with relevance scores
        """
        # Get user-specific settings
        user_max = 5
        if __user__ and hasattr(__user__, "valves") and __user__.valves:
            user_max = __user__.valves.max_results

        limit = max_results if max_results else user_max

        data = {"query": query, "limit": limit, "minScore": 0.3}

        if category:
            data["filters"] = {"category": category}

        result = self._make_request("POST", "/knowledge/search", data=data)

        if "error" in result:
            return f"**Error:** {result['error']}"

        results = result.get("results", [])
        if not results:
            return f"No knowledge articles found matching '{query}'."

        # Format results
        output = [f"**Found {len(results)} knowledge article(s):**\n"]

        for item in results:
            score_pct = item.get("scorePercent", 0)
            article_id = item.get("id", "Unknown")
            title = item.get("title", "No title")
            status = item.get("status", "")
            snippet = item.get("snippet", "")
            metadata = item.get("metadata", {})

            output.append(f"### {article_id} ({score_pct}% match)")
            output.append(f"**Title:** {title}")

            if metadata.get("category"):
                output.append(f"**Category:** {metadata['category']}")

            if status:
                output.append(f"**Status:** {status}")

            if snippet:
                # Clean up snippet
                clean_snippet = snippet.replace("\n", " ").strip()
                if len(clean_snippet) > 300:
                    clean_snippet = clean_snippet[:300] + "..."
                output.append(f"\n{clean_snippet}")

            output.append("")

        output.append(
            "> Use `get_article('<article_id>')` to view full article content."
        )

        return "\n".join(output)

    def get_article(
        self,
        article_id: str,
        __user__: Optional[Dict] = None,
    ) -> str:
        """
        Get the full content of a specific knowledge article including all sections,
        resolution steps, and related information.

        :param article_id: Knowledge article ID (e.g., KB000000001)
        :return: Full article content
        """
        result = self._make_request("GET", f"/knowledge/{article_id}")

        if "error" in result:
            return f"**Error:** {result['error']}"

        if not result.get("found", True):
            return f"**Knowledge article {article_id} not found.**"

        # Format the article
        output = [f"# {result.get('title', article_id)}\n"]

        # Metadata
        output.append("## Article Information")
        output.append(f"- **Article ID:** {result.get('articleId', 'N/A')}")

        if result.get("articleType"):
            output.append(f"- **Type:** {result['articleType']}")

        if result.get("categoryPath"):
            output.append(f"- **Category:** {result['categoryPath']}")

        if result.get("status"):
            output.append(f"- **Status:** {result['status']}")

        if result.get("author"):
            output.append(f"- **Author:** {result['author']}")

        if result.get("viewCount"):
            output.append(f"- **Views:** {result['viewCount']}")

        if result.get("publishedDate"):
            output.append(f"- **Published:** {result['publishedDate']}")

        output.append("")

        # Keywords
        keywords = result.get("keywords", [])
        if keywords:
            output.append(f"**Keywords:** {', '.join(keywords)}")
            output.append("")

        # Summary
        if result.get("summary"):
            output.append("## Summary")
            output.append(result["summary"])
            output.append("")

        # Main content
        if result.get("content"):
            output.append("## Content")
            output.append(result["content"])
            output.append("")

        # Attachments
        attachments = result.get("attachments", [])
        if attachments:
            output.append("## Attachments")
            for att in attachments:
                name = att.get("name", "Unknown")
                size = att.get("sizeBytes", 0)
                size_kb = size / 1024 if size else 0
                output.append(f"- {name} ({size_kb:.1f} KB)")
            output.append("")

        # Related articles
        related = result.get("relatedArticles", [])
        if related:
            output.append("## Related Articles")
            for rel_id in related:
                output.append(f"- {rel_id}")
            output.append("")

        return "\n".join(output)

    def search_solutions(
        self,
        problem_description: str,
        max_results: Optional[int] = None,
        __user__: Optional[Dict] = None,
    ) -> str:
        """
        Search for solutions to a specific IT problem. This searches both
        knowledge articles and resolved incidents to find potential solutions.

        :param problem_description: Description of the problem you're trying to solve
        :param max_results: Maximum number of results to return (default: 5)
        :return: Solutions and relevant documentation
        """
        # Get user-specific settings
        user_max = 5
        if __user__ and hasattr(__user__, "valves") and __user__.valves:
            user_max = __user__.valves.max_results

        limit = max_results if max_results else user_max

        # Search knowledge base
        kb_result = self._make_request(
            "POST",
            "/knowledge/search",
            data={"query": problem_description, "limit": limit, "minScore": 0.4},
        )

        # Also search incidents for resolutions
        inc_result = self._make_request(
            "POST",
            "/incidents/search",
            data={
                "query": f"resolution {problem_description}",
                "limit": limit,
                "minScore": 0.4,
            },
        )

        output = ["# Potential Solutions\n"]

        # Knowledge articles
        kb_results = kb_result.get("results", []) if "error" not in kb_result else []
        if kb_results:
            output.append("## From Knowledge Base")
            for item in kb_results[:3]:
                score_pct = item.get("scorePercent", 0)
                article_id = item.get("id", "Unknown")
                title = item.get("title", "No title")
                output.append(f"- **{article_id}** ({score_pct}%): {title}")
            output.append("")

        # Incident resolutions
        inc_results = inc_result.get("results", []) if "error" not in inc_result else []
        resolved_incidents = [
            inc for inc in inc_results
            if inc.get("metadata", {}).get("status") in ["Resolved", "Closed"]
        ]

        if resolved_incidents:
            output.append("## From Resolved Incidents")
            for item in resolved_incidents[:3]:
                score_pct = item.get("scorePercent", 0)
                incident_id = item.get("id", "Unknown")
                title = item.get("title", "No title")
                output.append(f"- **{incident_id}** ({score_pct}%): {title}")
            output.append("")

        if not kb_results and not resolved_incidents:
            output.append("No solutions found matching your problem description.")
            output.append("")
            output.append("**Suggestions:**")
            output.append("- Try rephrasing your problem description")
            output.append("- Use more specific technical terms")
            output.append("- Search for individual symptoms separately")
        else:
            output.append(
                "> Use `get_article('<id>')` or `get_incident_details('<id>')` to view full content."
            )

        return "\n".join(output)

    def find_how_to(
        self,
        task_description: str,
        max_results: Optional[int] = None,
        __user__: Optional[Dict] = None,
    ) -> str:
        """
        Find how-to guides and step-by-step instructions for common IT tasks.

        :param task_description: What you want to learn how to do
        :param max_results: Maximum number of results to return (default: 3)
        :return: Relevant how-to guides and documentation
        """
        user_max = 3
        if __user__ and hasattr(__user__, "valves") and __user__.valves:
            user_max = min(__user__.valves.max_results, 5)

        limit = max_results if max_results else user_max

        # Enhance query for how-to content
        enhanced_query = f"how to {task_description} steps guide procedure"

        result = self._make_request(
            "POST",
            "/knowledge/search",
            data={"query": enhanced_query, "limit": limit, "minScore": 0.35},
        )

        if "error" in result:
            return f"**Error:** {result['error']}"

        results = result.get("results", [])
        if not results:
            return f"No how-to guides found for '{task_description}'."

        output = [f"**How-To Guides for: {task_description}**\n"]

        for item in results:
            score_pct = item.get("scorePercent", 0)
            article_id = item.get("id", "Unknown")
            title = item.get("title", "No title")
            snippet = item.get("snippet", "")

            output.append(f"### {title}")
            output.append(f"**Article:** {article_id} ({score_pct}% relevant)")

            if snippet:
                clean_snippet = snippet.replace("\n", " ").strip()
                if len(clean_snippet) > 250:
                    clean_snippet = clean_snippet[:250] + "..."
                output.append(f"\n{clean_snippet}")

            output.append("")

        output.append(
            "> Use `get_article('<article_id>')` to view the complete guide."
        )

        return "\n".join(output)

    def search_work_orders(
        self,
        query: str,
        max_results: Optional[int] = None,
        __user__: Optional[Dict] = None,
    ) -> str:
        """
        Search for work orders in BMC Remedy. Work orders represent scheduled
        tasks, maintenance activities, or service requests.

        :param query: Search query for work orders
        :param max_results: Maximum number of results to return (default: 5)
        :return: List of matching work orders
        """
        user_max = 5
        if __user__ and hasattr(__user__, "valves") and __user__.valves:
            user_max = __user__.valves.max_results

        limit = max_results if max_results else user_max

        result = self._make_request(
            "POST",
            "/workorders/search",
            data={"query": query, "limit": limit, "minScore": 0.3},
        )

        if "error" in result:
            return f"**Error:** {result['error']}"

        results = result.get("results", [])
        if not results:
            return f"No work orders found matching '{query}'."

        output = [f"**Found {len(results)} work order(s):**\n"]

        for item in results:
            score_pct = item.get("scorePercent", 0)
            wo_id = item.get("id", "Unknown")
            title = item.get("title", "No title")
            status = item.get("status", "Unknown")
            snippet = item.get("snippet", "")

            output.append(f"### {wo_id} ({score_pct}% match)")
            output.append(f"**Summary:** {title}")
            output.append(f"**Status:** {status}")

            if snippet:
                clean_snippet = snippet.replace("\n", " ").strip()[:200]
                output.append(f"**Preview:** {clean_snippet}...")

            output.append("")

        return "\n".join(output)
