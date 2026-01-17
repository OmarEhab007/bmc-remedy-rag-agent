import { useEffect, useRef, useState } from 'react';
import type { ChatMessage } from '../types/chat';
import { MessageBubble } from './MessageBubble';
import { TypingIndicator } from './TypingIndicator';
import { EmptyState } from './EmptyState';

interface MessageContainerProps {
  messages: ChatMessage[];
  isThinking: boolean;
  onSuggestionSelect: (query: string) => void;
  isConnected: boolean;
  userName?: string;
  onRegenerate?: () => void;
  onEditMessage?: (messageId: string, newContent: string) => void;
  onDeleteMessage?: (messageId: string) => void;
}

export function MessageContainer({
  messages,
  isThinking,
  onSuggestionSelect,
  isConnected,
  userName,
  onRegenerate,
  onEditMessage,
  onDeleteMessage,
}: MessageContainerProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const bottomRef = useRef<HTMLDivElement>(null);
  const [showScrollButton, setShowScrollButton] = useState(false);

  // Auto-scroll to bottom when new messages arrive
  useEffect(() => {
    if (bottomRef.current) {
      bottomRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages, isThinking]);

  // Detect scroll position for scroll-to-bottom button
  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    const handleScroll = () => {
      const { scrollTop, scrollHeight, clientHeight } = container;
      const distanceFromBottom = scrollHeight - scrollTop - clientHeight;
      setShowScrollButton(distanceFromBottom > 100);
    };

    container.addEventListener('scroll', handleScroll);
    return () => container.removeEventListener('scroll', handleScroll);
  }, []);

  const scrollToBottom = () => {
    if (bottomRef.current) {
      bottomRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  };

  const isEmpty = messages.length === 0;

  // Check if any message is streaming
  const isStreaming = messages.some((m) => m.isStreaming);

  return (
    <div className="message-container-wrapper">
      <div
        ref={containerRef}
        className="message-container-scroll"
      >
        {isEmpty ? (
          <EmptyState onSuggestionSelect={onSuggestionSelect} disabled={!isConnected} />
        ) : (
          <div className="py-4">
            {messages.map((message, index) => (
              <MessageBubble
                key={message.id}
                message={message}
                userName={userName}
                onRegenerate={
                  message.role === 'assistant' && index === messages.length - 1 && !isStreaming
                    ? onRegenerate
                    : undefined
                }
                onEdit={
                  message.role === 'user'
                    ? (newContent) => onEditMessage?.(message.id, newContent)
                    : undefined
                }
                onDelete={
                  !message.isStreaming
                    ? () => onDeleteMessage?.(message.id)
                    : undefined
                }
              />
            ))}

            {isThinking && <TypingIndicator />}

            {/* Scroll anchor */}
            <div ref={bottomRef} className="h-4" />
          </div>
        )}
      </div>

      {/* Scroll to bottom button - positioned relative to wrapper */}
      {showScrollButton && (
        <button
          onClick={scrollToBottom}
          className="scroll-to-bottom-btn animate-fade-in"
          aria-label="Scroll to bottom"
          title="Scroll to bottom"
        >
          <svg
            className="w-4 h-4"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M19 14l-7 7m0 0l-7-7m7 7V3"
            />
          </svg>
        </button>
      )}
    </div>
  );
}
