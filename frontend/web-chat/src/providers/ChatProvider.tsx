import {
  createContext,
  useContext,
  useCallback,
  useEffect,
  useState,
  useRef,
} from 'react';
import type { ReactNode } from 'react';
import { v4 as uuidv4 } from 'uuid';
import { useChat } from '../hooks/useChat';
import { useWebSocket } from '../hooks/useWebSocket';
import { fetchSessions, fetchSessionHistory } from '../services/chatApi';
import type {
  ChatMessage,
  ChatQueryMessage,
  ChatResponseChunk,
  UserContext,
  ChatSession,
  ChatState,
} from '../types/chat';
import { ChunkType, ConnectionStatus } from '../types/chat';

interface ChatContextValue {
  state: ChatState;
  activeSession: ChatSession | null;
  userContext: UserContext;
  sendMessage: (text: string) => void;
  createNewSession: () => void;
  deleteSession: (sessionId: string) => void;
  deleteMessage: (messageId: string) => void;
  setActiveSession: (sessionId: string) => void;
  clearCurrentSession: () => void;
  setUserContext: (context: UserContext) => void;
  reconnect: () => void;
  clearError: () => void;
  regenerateLastResponse: () => void;
  editAndResend: (messageId: string, newContent: string) => void;
}

const ChatContext = createContext<ChatContextValue | null>(null);

// Default user context for development
const DEFAULT_USER_CONTEXT: UserContext = {
  userId: 'dev-user',
  userGroups: ['IT Support', 'Service Desk'],
  displayName: 'Development User',
};

interface ChatProviderProps {
  children: ReactNode;
}

export function ChatProvider({ children }: ChatProviderProps) {
  const {
    state,
    activeSession,
    createSession,
    deleteSession,
    setActiveSession,
    addMessage,
    updateMessage,
    appendToken,
    setConnectionStatus,
    setThinking,
    setError,
    clearSession,
    deleteMessage: deleteMessageFromChat,
    loadSessions,
  } = useChat();

  const [userContext, setUserContext] = useState<UserContext>(DEFAULT_USER_CONTEXT);
  const [pendingMessageId, setPendingMessageId] = useState<string | null>(null);
  const sessionsLoadedRef = useRef(false);

  // Handle incoming WebSocket messages
  const handleWebSocketMessage = useCallback(
    (chunk: ChatResponseChunk) => {
      const sessionId = chunk.sessionId || state.activeSessionId;
      if (!sessionId) return;

      switch (chunk.type) {
        case ChunkType.THINKING: {
          setThinking(true);
          const assistantMessage: ChatMessage = {
            id: chunk.messageId,
            role: 'assistant',
            content: '',
            timestamp: new Date(),
            isStreaming: true,
          };
          addMessage(sessionId, assistantMessage);
          setPendingMessageId(chunk.messageId);
          break;
        }

        case ChunkType.TOKEN:
          setThinking(false);
          if (chunk.token && pendingMessageId) {
            appendToken(sessionId, pendingMessageId, chunk.token);
          }
          break;

        case ChunkType.COMPLETE:
          setThinking(false);
          if (pendingMessageId) {
            updateMessage(sessionId, pendingMessageId, {
              isStreaming: false,
              citations: chunk.citations,
              confidenceScore: chunk.confidenceScore,
            });
            setPendingMessageId(null);
          }
          break;

        case ChunkType.ERROR:
          setThinking(false);
          setError(chunk.error || 'An error occurred');
          if (pendingMessageId) {
            updateMessage(sessionId, pendingMessageId, {
              isStreaming: false,
              error: chunk.error,
              content:
                chunk.error || 'Sorry, an error occurred while processing your request.',
            });
            setPendingMessageId(null);
          }
          break;

        default:
          break;
      }
    },
    [
      state.activeSessionId,
      pendingMessageId,
      addMessage,
      appendToken,
      updateMessage,
      setThinking,
      setError,
    ]
  );

  const handleConnectionChange = useCallback(
    (status: ConnectionStatus) => {
      setConnectionStatus(status);
      // Don't set error for connection status - it's already shown in header and input area
      // Only clear errors when reconnected
      if (status === ConnectionStatus.CONNECTED) {
        setError(null);
      }
    },
    [setConnectionStatus, setError]
  );

  const { sendMessage: wsSendMessage, reconnect } = useWebSocket({
    onMessage: handleWebSocketMessage,
    onConnectionChange: handleConnectionChange,
  });

  // Load sessions from backend on mount
  useEffect(() => {
    if (sessionsLoadedRef.current) return;
    sessionsLoadedRef.current = true;

    async function loadSessionsFromBackend() {
      try {
        console.log('Loading sessions from backend...');
        const backendSessions = await fetchSessions();
        console.log('Backend sessions:', backendSessions);

        if (backendSessions.length > 0) {
          // Load full history for each session
          const sessionsWithHistory: ChatSession[] = await Promise.all(
            backendSessions.map(async (summary) => {
              try {
                const history = await fetchSessionHistory(summary.sessionId);
                return {
                  id: summary.sessionId,
                  title: summary.title || 'New Conversation',
                  createdAt: new Date(summary.lastUpdated),
                  updatedAt: new Date(summary.lastUpdated),
                  messages: history.messages.map((msg, idx) => ({
                    id: `${summary.sessionId}-${idx}`,
                    role: msg.type === 'USER' ? 'user' as const : 'assistant' as const,
                    content: msg.content,
                    timestamp: new Date(summary.lastUpdated),
                  })),
                };
              } catch (err) {
                console.error(`Failed to load history for session ${summary.sessionId}:`, err);
                return {
                  id: summary.sessionId,
                  title: summary.title || 'New Conversation',
                  createdAt: new Date(summary.lastUpdated),
                  updatedAt: new Date(summary.lastUpdated),
                  messages: [],
                };
              }
            })
          );

          loadSessions(sessionsWithHistory);
          setActiveSession(sessionsWithHistory[0].id);
          console.log('Loaded sessions from backend:', sessionsWithHistory.length);
        } else {
          // No sessions in backend, create a new one
          console.log('No backend sessions, creating new session');
          createSession();
        }
      } catch (error) {
        console.error('Failed to load sessions from backend:', error);
        // Fallback: use localStorage sessions or create new
        if (state.sessions.length === 0) {
          createSession();
        } else if (!state.activeSessionId) {
          setActiveSession(state.sessions[0].id);
        }
      }
    }

    loadSessionsFromBackend();
  }, [loadSessions, createSession, setActiveSession]);

  // Select first session if none active (after initial load)
  useEffect(() => {
    if (!state.activeSessionId && state.sessions.length > 0) {
      setActiveSession(state.sessions[0].id);
    }
  }, [state.activeSessionId, state.sessions.length, setActiveSession]);

  const sendMessage = useCallback(
    (text: string) => {
      if (!text.trim() || !state.activeSessionId) return;

      if (state.connectionStatus !== ConnectionStatus.CONNECTED) {
        setError('Not connected to server. Please wait or try reconnecting.');
        return;
      }

      // Add user message to state
      const userMessage: ChatMessage = {
        id: uuidv4(),
        role: 'user',
        content: text,
        timestamp: new Date(),
      };
      addMessage(state.activeSessionId, userMessage);

      // Send via WebSocket
      const wsMessage: ChatQueryMessage = {
        messageId: uuidv4(),
        text,
        sessionId: state.activeSessionId,
        userId: userContext.userId,
        userGroups: userContext.userGroups,
        timestamp: new Date().toISOString(),
      };
      wsSendMessage(wsMessage);
    },
    [
      state.activeSessionId,
      state.connectionStatus,
      userContext,
      addMessage,
      wsSendMessage,
      setError,
    ]
  );

  const createNewSession = useCallback(() => {
    createSession();
  }, [createSession]);

  const clearCurrentSession = useCallback(() => {
    if (state.activeSessionId) {
      clearSession(state.activeSessionId);
    }
  }, [state.activeSessionId, clearSession]);

  const clearErrorMessage = useCallback(() => {
    setError(null);
  }, [setError]);

  const deleteMessageById = useCallback(
    (messageId: string) => {
      if (!state.activeSessionId) return;
      deleteMessageFromChat(state.activeSessionId, messageId);
    },
    [state.activeSessionId, deleteMessageFromChat]
  );

  const regenerateLastResponse = useCallback(() => {
    if (!activeSession || !state.activeSessionId) return;

    const messages = activeSession.messages;
    if (messages.length < 2) return;

    // Find the last user message
    let lastUserMessageIndex = -1;
    for (let i = messages.length - 1; i >= 0; i--) {
      if (messages[i].role === 'user') {
        lastUserMessageIndex = i;
        break;
      }
    }

    if (lastUserMessageIndex === -1) return;

    const lastUserMessage = messages[lastUserMessageIndex];

    // Delete the last assistant message(s) after the user message
    const messagesToDelete = messages.slice(lastUserMessageIndex + 1);
    messagesToDelete.forEach((msg) => {
      deleteMessageFromChat(state.activeSessionId!, msg.id);
    });

    // Re-send the user's message
    if (state.connectionStatus !== ConnectionStatus.CONNECTED) {
      setError('Not connected to server. Please wait or try reconnecting.');
      return;
    }

    const wsMessage: ChatQueryMessage = {
      messageId: uuidv4(),
      text: lastUserMessage.content,
      sessionId: state.activeSessionId,
      userId: userContext.userId,
      userGroups: userContext.userGroups,
      timestamp: new Date().toISOString(),
    };
    wsSendMessage(wsMessage);
  }, [
    activeSession,
    state.activeSessionId,
    state.connectionStatus,
    userContext,
    deleteMessageFromChat,
    wsSendMessage,
    setError,
  ]);

  const editAndResend = useCallback(
    (messageId: string, newContent: string) => {
      if (!activeSession || !state.activeSessionId) return;
      if (!newContent.trim()) return;

      const messageIndex = activeSession.messages.findIndex((m) => m.id === messageId);
      if (messageIndex === -1) return;

      // Delete all messages from this point onward
      const messagesToDelete = activeSession.messages.slice(messageIndex);
      messagesToDelete.forEach((msg) => {
        deleteMessageFromChat(state.activeSessionId!, msg.id);
      });

      // Send the new message
      sendMessage(newContent);
    },
    [activeSession, state.activeSessionId, deleteMessageFromChat, sendMessage]
  );

  const contextValue: ChatContextValue = {
    state,
    activeSession,
    userContext,
    sendMessage,
    createNewSession,
    deleteSession,
    deleteMessage: deleteMessageById,
    setActiveSession,
    clearCurrentSession,
    setUserContext,
    reconnect,
    clearError: clearErrorMessage,
    regenerateLastResponse,
    editAndResend,
  };

  return (
    <ChatContext.Provider value={contextValue}>{children}</ChatContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useChatContext(): ChatContextValue {
  const context = useContext(ChatContext);
  if (!context) {
    throw new Error('useChatContext must be used within a ChatProvider');
  }
  return context;
}
