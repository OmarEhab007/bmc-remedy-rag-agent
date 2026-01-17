import { useState, useRef, useCallback, useEffect } from 'react';
import type { KeyboardEvent } from 'react';
import { ConnectionStatus } from '../types/chat';

interface InputAreaProps {
  onSendMessage: (text: string) => void;
  connectionStatus: ConnectionStatus;
  disabled?: boolean;
  isStreaming?: boolean;
  onStopGeneration?: () => void;
  placeholder?: string;
  maxLength?: number;
}

export function InputArea({
  onSendMessage,
  connectionStatus,
  disabled = false,
  isStreaming = false,
  onStopGeneration,
  placeholder = 'Ask me anything about IT support...',
  maxLength = 4000,
}: InputAreaProps) {
  const [message, setMessage] = useState('');
  const [isFocused, setIsFocused] = useState(false);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const isConnected = connectionStatus === ConnectionStatus.CONNECTED;
  const isDisabled = disabled || !isConnected;

  // Auto-focus on mount
  useEffect(() => {
    if (textareaRef.current && isConnected) {
      textareaRef.current.focus();
    }
  }, [isConnected]);

  const handleSubmit = useCallback(() => {
    const trimmedMessage = message.trim();
    if (trimmedMessage && !isDisabled && !isStreaming) {
      onSendMessage(trimmedMessage);
      setMessage('');
      // Reset textarea height
      if (textareaRef.current) {
        textareaRef.current.style.height = 'auto';
      }
    }
  }, [message, isDisabled, isStreaming, onSendMessage]);

  const handleKeyDown = useCallback(
    (e: KeyboardEvent<HTMLTextAreaElement>) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        handleSubmit();
      }
    },
    [handleSubmit]
  );

  const handleChange = useCallback(
    (e: React.ChangeEvent<HTMLTextAreaElement>) => {
      const value = e.target.value;
      if (value.length <= maxLength) {
        setMessage(value);
        // Auto-resize textarea
        const textarea = e.target;
        textarea.style.height = 'auto';
        textarea.style.height = `${Math.min(textarea.scrollHeight, 200)}px`;
      }
    },
    [maxLength]
  );

  const remainingChars = maxLength - message.length;
  const showCharCount = message.length > maxLength * 0.8;
  const hasContent = message.trim().length > 0;

  return (
    <div className="input-area-container">
      {/* Gradient fade effect */}
      <div className="input-area-gradient" />

      <div className="input-area-content">
        {/* Main input wrapper */}
        <div className={`input-box ${isFocused ? 'input-box-focused' : ''} ${isDisabled ? 'input-box-disabled' : ''}`}>
          {/* Textarea */}
          <textarea
            ref={textareaRef}
            value={message}
            onChange={handleChange}
            onKeyDown={handleKeyDown}
            onFocus={() => setIsFocused(true)}
            onBlur={() => setIsFocused(false)}
            placeholder={isConnected ? placeholder : 'Waiting for connection...'}
            disabled={isDisabled}
            rows={1}
            className="input-textarea-new"
            aria-label="Message input"
          />

          {/* Right side actions */}
          <div className="input-actions">
            {/* Character count */}
            {showCharCount && (
              <span className={`char-count ${remainingChars < 100 ? 'char-count-warning' : ''}`}>
                {remainingChars}
              </span>
            )}

            {/* Send or Stop button */}
            {isStreaming ? (
              <button
                onClick={onStopGeneration}
                className="input-btn input-btn-stop"
                aria-label="Stop generating"
                title="Stop generating (Esc)"
              >
                <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                  <rect x="6" y="6" width="12" height="12" rx="2" />
                </svg>
              </button>
            ) : (
              <button
                onClick={handleSubmit}
                disabled={isDisabled || !hasContent}
                className={`input-btn input-btn-send ${hasContent ? 'input-btn-send-active' : ''}`}
                aria-label="Send message"
                title="Send message (Enter)"
              >
                <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none">
                  <path
                    d="M7 11L12 6L17 11M12 18V7"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                </svg>
              </button>
            )}
          </div>
        </div>

        {/* Connection status indicator */}
        {!isConnected && (
          <div className="connection-status">
            <span className="status-dot" />
            <span>
              {connectionStatus === ConnectionStatus.CONNECTING
                ? 'Connecting...'
                : 'Connection lost'}
            </span>
          </div>
        )}

        {/* Disclaimer text */}
        <p className="input-disclaimer">
          Damee GPT can make mistakes. Consider checking important information.
        </p>
      </div>
    </div>
  );
}
