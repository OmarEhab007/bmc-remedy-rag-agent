import { useReducer, useCallback, useMemo } from 'react';
import { v4 as uuidv4 } from 'uuid';
import type {
  ChatState,
  ChatAction,
  ChatSession,
  ChatMessage,
} from '../types/chat';
import { ConnectionStatus } from '../types/chat';

const STORAGE_KEY = 'damee-gpt-chat-sessions';

function loadSessionsFromStorage(): ChatSession[] {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      const sessions = JSON.parse(stored);
      // Convert date strings back to Date objects
      return sessions.map((session: ChatSession) => ({
        ...session,
        createdAt: new Date(session.createdAt),
        updatedAt: new Date(session.updatedAt),
        messages: session.messages.map((msg: ChatMessage) => ({
          ...msg,
          timestamp: new Date(msg.timestamp),
        })),
      }));
    }
  } catch (error) {
    console.error('Failed to load sessions from storage:', error);
  }
  return [];
}

function saveSessionsToStorage(sessions: ChatSession[]): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(sessions));
  } catch (error) {
    console.error('Failed to save sessions to storage:', error);
  }
}

const initialState: ChatState = {
  sessions: loadSessionsFromStorage(),
  activeSessionId: null,
  connectionStatus: ConnectionStatus.DISCONNECTED,
  isThinking: false,
  error: null,
};

function chatReducer(state: ChatState, action: ChatAction): ChatState {
  let newState: ChatState;

  switch (action.type) {
    case 'SET_CONNECTION_STATUS':
      return { ...state, connectionStatus: action.status };

    case 'SET_ACTIVE_SESSION':
      return { ...state, activeSessionId: action.sessionId };

    case 'CREATE_SESSION':
      newState = {
        ...state,
        sessions: [action.session, ...state.sessions],
        activeSessionId: action.session.id,
      };
      saveSessionsToStorage(newState.sessions);
      return newState;

    case 'DELETE_SESSION': {
      const filteredSessions = state.sessions.filter((s) => s.id !== action.sessionId);
      // If deleting the active session, select the first remaining session
      const newActiveId =
        state.activeSessionId === action.sessionId
          ? filteredSessions[0]?.id || null
          : state.activeSessionId;
      newState = {
        ...state,
        sessions: filteredSessions,
        activeSessionId: newActiveId,
      };
      saveSessionsToStorage(newState.sessions);
      return newState;
    }

    case 'ADD_MESSAGE':
      newState = {
        ...state,
        sessions: state.sessions.map((session) =>
          session.id === action.sessionId
            ? {
                ...session,
                messages: [...session.messages, action.message],
                updatedAt: new Date(),
                title:
                  session.messages.length === 0 && action.message.role === 'user'
                    ? action.message.content.slice(0, 50) +
                      (action.message.content.length > 50 ? '...' : '')
                    : session.title,
              }
            : session
        ),
      };
      saveSessionsToStorage(newState.sessions);
      return newState;

    case 'UPDATE_MESSAGE':
      newState = {
        ...state,
        sessions: state.sessions.map((session) =>
          session.id === action.sessionId
            ? {
                ...session,
                messages: session.messages.map((msg) =>
                  msg.id === action.messageId ? { ...msg, ...action.updates } : msg
                ),
                updatedAt: new Date(),
              }
            : session
        ),
      };
      saveSessionsToStorage(newState.sessions);
      return newState;

    case 'APPEND_TOKEN':
      newState = {
        ...state,
        sessions: state.sessions.map((session) =>
          session.id === action.sessionId
            ? {
                ...session,
                messages: session.messages.map((msg) =>
                  msg.id === action.messageId
                    ? { ...msg, content: msg.content + action.token }
                    : msg
                ),
              }
            : session
        ),
      };
      // Don't save on every token to avoid performance issues
      return newState;

    case 'SET_THINKING':
      return { ...state, isThinking: action.isThinking };

    case 'SET_ERROR':
      return { ...state, error: action.error };

    case 'CLEAR_SESSION':
      newState = {
        ...state,
        sessions: state.sessions.map((session) =>
          session.id === action.sessionId
            ? { ...session, messages: [], updatedAt: new Date() }
            : session
        ),
      };
      saveSessionsToStorage(newState.sessions);
      return newState;

    case 'DELETE_MESSAGE':
      newState = {
        ...state,
        sessions: state.sessions.map((session) =>
          session.id === action.sessionId
            ? {
                ...session,
                messages: session.messages.filter((msg) => msg.id !== action.messageId),
                updatedAt: new Date(),
              }
            : session
        ),
      };
      saveSessionsToStorage(newState.sessions);
      return newState;

    case 'LOAD_SESSIONS':
      saveSessionsToStorage(action.sessions);
      return { ...state, sessions: action.sessions };

    default:
      return state;
  }
}

export function useChat() {
  const [state, dispatch] = useReducer(chatReducer, initialState);

  const createSession = useCallback(() => {
    const session: ChatSession = {
      id: uuidv4(),
      title: 'New Conversation',
      createdAt: new Date(),
      updatedAt: new Date(),
      messages: [],
    };
    dispatch({ type: 'CREATE_SESSION', session });
    return session;
  }, []);

  const deleteSession = useCallback((sessionId: string) => {
    dispatch({ type: 'DELETE_SESSION', sessionId });
  }, []);

  const setActiveSession = useCallback((sessionId: string) => {
    dispatch({ type: 'SET_ACTIVE_SESSION', sessionId });
  }, []);

  const addMessage = useCallback((sessionId: string, message: ChatMessage) => {
    dispatch({ type: 'ADD_MESSAGE', sessionId, message });
  }, []);

  const updateMessage = useCallback(
    (sessionId: string, messageId: string, updates: Partial<ChatMessage>) => {
      dispatch({ type: 'UPDATE_MESSAGE', sessionId, messageId, updates });
    },
    []
  );

  const appendToken = useCallback(
    (sessionId: string, messageId: string, token: string) => {
      dispatch({ type: 'APPEND_TOKEN', sessionId, messageId, token });
    },
    []
  );

  const setConnectionStatus = useCallback((status: ConnectionStatus) => {
    dispatch({ type: 'SET_CONNECTION_STATUS', status });
  }, []);

  const setThinking = useCallback((isThinking: boolean) => {
    dispatch({ type: 'SET_THINKING', isThinking });
  }, []);

  const setError = useCallback((error: string | null) => {
    dispatch({ type: 'SET_ERROR', error });
  }, []);

  const clearSession = useCallback((sessionId: string) => {
    dispatch({ type: 'CLEAR_SESSION', sessionId });
  }, []);

  const deleteMessage = useCallback((sessionId: string, messageId: string) => {
    dispatch({ type: 'DELETE_MESSAGE', sessionId, messageId });
  }, []);

  const loadSessions = useCallback((sessions: ChatSession[]) => {
    dispatch({ type: 'LOAD_SESSIONS', sessions });
  }, []);

  const activeSession = useMemo(() => {
    return state.sessions.find((s) => s.id === state.activeSessionId) || null;
  }, [state.sessions, state.activeSessionId]);

  return {
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
    deleteMessage,
    loadSessions,
  };
}
