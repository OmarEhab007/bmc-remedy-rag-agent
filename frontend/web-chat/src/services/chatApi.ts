/**
 * Chat API service for fetching session data from backend.
 */

const API_BASE = '/api/v1/chat';

export interface SessionSummary {
  sessionId: string;
  title: string;
  messageCount: number;
  lastUpdated: number;
}

export interface HistoryMessage {
  type: 'USER' | 'AI' | 'SYSTEM';
  content: string;
}

export interface HistoryResponse {
  sessionId: string;
  messageCount: number;
  messages: HistoryMessage[];
}

/**
 * Fetch all chat sessions from the backend.
 */
export async function fetchSessions(): Promise<SessionSummary[]> {
  const response = await fetch(`${API_BASE}/sessions`);
  if (!response.ok) {
    throw new Error(`Failed to fetch sessions: ${response.status}`);
  }
  return response.json();
}

/**
 * Fetch chat history for a specific session.
 */
export async function fetchSessionHistory(sessionId: string): Promise<HistoryResponse> {
  const response = await fetch(`${API_BASE}/sessions/${sessionId}/history`);
  if (!response.ok) {
    throw new Error(`Failed to fetch session history: ${response.status}`);
  }
  return response.json();
}

/**
 * Delete a chat session.
 */
export async function deleteSessionApi(sessionId: string): Promise<void> {
  const response = await fetch(`${API_BASE}/sessions/${sessionId}`, {
    method: 'DELETE',
  });
  if (!response.ok) {
    throw new Error(`Failed to delete session: ${response.status}`);
  }
}
