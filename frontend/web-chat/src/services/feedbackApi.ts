/**
 * Feedback API service for persisting user feedback on AI responses.
 * Integrates with backend to store feedback for model improvement.
 */

const API_BASE = '/api/v1/feedback';

export type FeedbackType = 'positive' | 'negative';

export interface FeedbackPayload {
  messageId: string;
  sessionId: string;
  feedbackType: FeedbackType;
  feedbackText?: string;
  userId?: string;
  timestamp: string;
}

export interface FeedbackResponse {
  id: string;
  messageId: string;
  status: 'received' | 'processed';
  createdAt: string;
}

/**
 * Submit feedback for a specific message.
 * @param payload - The feedback data to submit
 * @returns Promise resolving to the feedback response
 */
export async function submitFeedback(payload: FeedbackPayload): Promise<FeedbackResponse> {
  const response = await fetch(`${API_BASE}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error(`Failed to submit feedback: ${response.status}`);
  }

  return response.json();
}

/**
 * Submit detailed negative feedback with explanation.
 * @param messageId - The message ID receiving feedback
 * @param sessionId - The session ID
 * @param feedbackText - Detailed explanation of what was wrong
 * @param userId - Optional user ID for tracking
 * @returns Promise resolving to the feedback response
 */
export async function submitDetailedFeedback(
  messageId: string,
  sessionId: string,
  feedbackText: string,
  userId?: string
): Promise<FeedbackResponse> {
  return submitFeedback({
    messageId,
    sessionId,
    feedbackType: 'negative',
    feedbackText,
    userId,
    timestamp: new Date().toISOString(),
  });
}

/**
 * Get feedback statistics for a session (admin use).
 * @param sessionId - The session ID to get stats for
 */
export async function getFeedbackStats(sessionId: string): Promise<{
  positive: number;
  negative: number;
  total: number;
}> {
  const response = await fetch(`${API_BASE}/stats/${sessionId}`);

  if (!response.ok) {
    throw new Error(`Failed to get feedback stats: ${response.status}`);
  }

  return response.json();
}
