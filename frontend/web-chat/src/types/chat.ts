/**
 * WebSocket outbound message for chat queries.
 */
export interface ChatQueryMessage {
  messageId: string;
  text: string;
  sessionId: string;
  userId?: string;
  userGroups?: string[];
  timestamp: string;
  skipContext?: boolean;
}

/**
 * Citation reference from ITSM knowledge base.
 */
export interface Citation {
  sourceType: string;
  sourceId: string;
  title: string;
  score?: number;
}

/**
 * Chunk type indicator for streaming responses.
 */
export const ChunkType = {
  TOKEN: 'TOKEN',
  CONNECTED: 'CONNECTED',
  THINKING: 'THINKING',
  COMPLETE: 'COMPLETE',
  ERROR: 'ERROR',
} as const;

export type ChunkType = (typeof ChunkType)[keyof typeof ChunkType];

/**
 * WebSocket inbound message for streaming chat responses.
 */
export interface ChatResponseChunk {
  messageId: string;
  sessionId: string;
  token?: string;
  isComplete: boolean;
  citations?: Citation[];
  confidenceScore?: number;
  error?: string;
  type: ChunkType;
}

/**
 * Message role identifier.
 */
export type MessageRole = 'user' | 'assistant' | 'system';

/**
 * Pending action for agentic operations (Section 12).
 */
export interface PendingActionInfo {
  actionId: string;
  actionType: 'INCIDENT_CREATE' | 'WORK_ORDER_CREATE';
  preview: string;
  expiresAt: string;
}

/**
 * Individual chat message in the conversation.
 */
export interface ChatMessage {
  id: string;
  role: MessageRole;
  content: string;
  timestamp: Date;
  isStreaming?: boolean;
  citations?: Citation[];
  confidenceScore?: number;
  error?: string;
  /** Pending action if this message contains a confirmation prompt */
  pendingAction?: PendingActionInfo;
  /** Whether this message represents a confirmed/cancelled action result */
  isActionResult?: boolean;
}

/**
 * Conversation session.
 */
export interface ChatSession {
  id: string;
  title: string;
  createdAt: Date;
  updatedAt: Date;
  messages: ChatMessage[];
}

/**
 * User context for access control (ReBAC).
 */
export interface UserContext {
  userId: string;
  userGroups: string[];
  displayName?: string;
}

/**
 * WebSocket connection status.
 */
export const ConnectionStatus = {
  CONNECTING: 'CONNECTING',
  CONNECTED: 'CONNECTED',
  DISCONNECTED: 'DISCONNECTED',
  ERROR: 'ERROR',
} as const;

export type ConnectionStatus = (typeof ConnectionStatus)[keyof typeof ConnectionStatus];

/**
 * Chat state for the provider.
 */
export interface ChatState {
  sessions: ChatSession[];
  activeSessionId: string | null;
  connectionStatus: ConnectionStatus;
  isThinking: boolean;
  error: string | null;
}

/**
 * Chat action types for reducer.
 */
export type ChatAction =
  | { type: 'SET_CONNECTION_STATUS'; status: ConnectionStatus }
  | { type: 'SET_ACTIVE_SESSION'; sessionId: string }
  | { type: 'CREATE_SESSION'; session: ChatSession }
  | { type: 'DELETE_SESSION'; sessionId: string }
  | { type: 'ADD_MESSAGE'; sessionId: string; message: ChatMessage }
  | { type: 'UPDATE_MESSAGE'; sessionId: string; messageId: string; updates: Partial<ChatMessage> }
  | { type: 'APPEND_TOKEN'; sessionId: string; messageId: string; token: string }
  | { type: 'SET_THINKING'; isThinking: boolean }
  | { type: 'SET_ERROR'; error: string | null }
  | { type: 'CLEAR_SESSION'; sessionId: string }
  | { type: 'DELETE_MESSAGE'; sessionId: string; messageId: string }
  | { type: 'LOAD_SESSIONS'; sessions: ChatSession[] };

/**
 * Suggestion chip for quick actions.
 */
export interface SuggestionChip {
  id: string;
  label: string;
  query: string;
}

/**
 * Default suggestion chips.
 */
export const DEFAULT_SUGGESTIONS: SuggestionChip[] = [
  {
    id: '1',
    label: 'Recent incidents',
    query: 'Show me the most recent P1 incidents',
  },
  {
    id: '2',
    label: 'Password reset',
    query: 'How do I reset a user password?',
  },
  {
    id: '3',
    label: 'VPN issues',
    query: 'Common VPN connection troubleshooting steps',
  },
  {
    id: '4',
    label: 'Email problems',
    query: 'How to resolve Outlook sync issues?',
  },
];

/**
 * Agentic suggestion chips for ticket creation.
 */
export const AGENTIC_SUGGESTIONS: SuggestionChip[] = [
  {
    id: 'agentic-1',
    label: 'Create incident',
    query: 'Create a new incident for network connectivity issues in Building A',
  },
  {
    id: 'agentic-2',
    label: 'Create work order',
    query: 'Create a work order for scheduled server maintenance this weekend',
  },
  {
    id: 'agentic-3',
    label: 'Find similar',
    query: 'Search for similar incidents about printer problems',
  },
];
