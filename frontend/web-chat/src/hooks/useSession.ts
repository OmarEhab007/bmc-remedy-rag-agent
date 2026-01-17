import { useState } from 'react';
import { v4 as uuidv4 } from 'uuid';

const SESSION_STORAGE_KEY = 'damee-gpt-session-id';

/**
 * Hook for managing session ID persistence across page reloads.
 */
export function useSession() {
  const [sessionId, setSessionId] = useState<string>(() => {
    // Try to get existing session from localStorage
    const stored = localStorage.getItem(SESSION_STORAGE_KEY);
    if (stored) {
      return stored;
    }
    // Generate new session ID
    const newId = uuidv4();
    localStorage.setItem(SESSION_STORAGE_KEY, newId);
    return newId;
  });

  const resetSession = () => {
    const newId = uuidv4();
    localStorage.setItem(SESSION_STORAGE_KEY, newId);
    setSessionId(newId);
    return newId;
  };

  return {
    sessionId,
    resetSession,
  };
}
