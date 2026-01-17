import { Avatar } from './Avatar';

export function TypingIndicator() {
  return (
    <div className="message-row-assistant-wrapper animate-fade-in">
      <div className="message-row-inner">
        {/* Avatar */}
        <Avatar type="assistant" />

        {/* Thinking indicator */}
        <div className="message-content">
          <div className="thinking-indicator">
            <div className="thinking-dots">
              <span className="thinking-dot" />
              <span className="thinking-dot" />
              <span className="thinking-dot" />
            </div>
          </div>
          <span className="sr-only">Assistant is thinking...</span>
        </div>
      </div>
    </div>
  );
}
