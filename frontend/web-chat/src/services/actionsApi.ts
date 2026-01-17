/**
 * Actions API service for agentic operations (Section 12).
 * Handles confirmation/cancellation of staged actions.
 */

const API_BASE = '/api/v1/actions';

/**
 * Pending action details.
 */
export interface PendingAction {
  actionId: string;
  actionType: 'INCIDENT_CREATE' | 'WORK_ORDER_CREATE';
  sessionId: string;
  userId: string;
  preview: string;
  confirmationPrompt: string;
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'EXPIRED' | 'EXECUTED' | 'FAILED';
  createdAt: string;
  expiresAt: string;
}

/**
 * Action execution result.
 */
export interface ActionResult {
  success: boolean;
  actionId: string;
  actionType: string;
  message: string;
  createdRecordId?: string;
  createdRecordNumber?: string;
  formName?: string;
  error?: string;
}

/**
 * Rate limit status for agentic operations.
 */
export interface RateLimitStatus {
  maxPerHour: number;
  remaining: number;
  isLimited: boolean;
}

/**
 * Agentic operations status.
 */
export interface AgenticStatus {
  enabled: boolean;
  confirmationTimeoutMinutes: number;
  maxCreationsPerHour: number;
  duplicateDetectionThreshold: number;
  auditEnabled: boolean;
}

/**
 * Confirm a pending action.
 */
export async function confirmAction(
  sessionId: string,
  actionId: string
): Promise<ActionResult> {
  const response = await fetch(`${API_BASE}/confirm`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ sessionId, actionId }),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Failed to confirm action: ${errorText}`);
  }

  return response.json();
}

/**
 * Cancel a pending action.
 */
export async function cancelAction(
  sessionId: string,
  actionId: string
): Promise<{ message: string }> {
  const response = await fetch(
    `${API_BASE}/cancel?sessionId=${encodeURIComponent(sessionId)}&actionId=${encodeURIComponent(actionId)}`,
    {
      method: 'DELETE',
    }
  );

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Failed to cancel action: ${errorText}`);
  }

  return response.json();
}

/**
 * Get all pending actions for a session.
 */
export async function getPendingActions(
  sessionId: string
): Promise<PendingAction[]> {
  const response = await fetch(
    `${API_BASE}/pending?sessionId=${encodeURIComponent(sessionId)}`
  );

  if (!response.ok) {
    throw new Error(`Failed to fetch pending actions: ${response.status}`);
  }

  return response.json();
}

/**
 * Get details of a specific action.
 */
export async function getActionDetails(
  actionId: string
): Promise<PendingAction> {
  const response = await fetch(`${API_BASE}/${encodeURIComponent(actionId)}`);

  if (!response.ok) {
    throw new Error(`Failed to fetch action details: ${response.status}`);
  }

  return response.json();
}

/**
 * Get rate limit status for a user.
 */
export async function getRateLimitStatus(
  userId: string
): Promise<RateLimitStatus> {
  const response = await fetch(
    `${API_BASE}/rate-limit?userId=${encodeURIComponent(userId)}`
  );

  if (!response.ok) {
    throw new Error(`Failed to fetch rate limit status: ${response.status}`);
  }

  return response.json();
}

/**
 * Get agentic operations status.
 */
export async function getAgenticStatus(): Promise<AgenticStatus> {
  const response = await fetch(`${API_BASE}/status`);

  if (!response.ok) {
    throw new Error(`Failed to fetch agentic status: ${response.status}`);
  }

  return response.json();
}

/**
 * Parse action ID from confirmation prompt text.
 * Matches patterns like: "confirm abc123" or "Action ID: abc123"
 */
export function parseActionId(text: string): string | null {
  // Look for action ID patterns in the text
  const patterns = [
    /confirm[:\s]+([a-zA-Z0-9-]+)/i,
    /action[_\s]?id[:\s]+([a-zA-Z0-9-]+)/i,
    /`confirm\s+([a-zA-Z0-9-]+)`/i,
  ];

  for (const pattern of patterns) {
    const match = text.match(pattern);
    if (match) {
      return match[1];
    }
  }

  return null;
}

/**
 * Check if a message contains a confirmation prompt.
 */
export function isConfirmationPrompt(content: string): boolean {
  const indicators = [
    'confirm',
    'Reply with',
    'To proceed',
    'confirmation required',
    'staged for creation',
    'before it\'s created',
  ];

  const lowerContent = content.toLowerCase();
  return indicators.some((indicator) => lowerContent.includes(indicator.toLowerCase()));
}
