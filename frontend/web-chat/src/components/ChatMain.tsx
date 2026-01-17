import { useChatContext } from '../providers/ChatProvider';
import { MessageContainer } from './MessageContainer';
import { InputArea } from './InputArea';

export function ChatMain() {
  const {
    state,
    activeSession,
    sendMessage,
    clearError,
    userContext,
    regenerateLastResponse,
    editAndResend,
    deleteMessage,
  } = useChatContext();

  const messages = activeSession?.messages || [];

  // Check if the last message is streaming
  const isStreaming = messages.some((m) => m.isStreaming);

  // Handle regeneration - re-send the last user message
  const handleRegenerate = () => {
    regenerateLastResponse();
  };

  // Handle message editing - edit user message and resend
  const handleEditMessage = (messageId: string, newContent: string) => {
    editAndResend(messageId, newContent);
  };

  // Handle message deletion
  const handleDeleteMessage = (messageId: string) => {
    deleteMessage(messageId);
  };

  return (
    <main id="main-content" className="flex-1 flex flex-col min-h-0 bg-main" tabIndex={-1}>
      {/* Error banner */}
      {state.error && (
        <div
          className="px-4 py-3 text-sm flex items-center justify-between"
          style={{
            backgroundColor: 'rgba(239, 68, 68, 0.1)',
            color: 'var(--color-error)',
          }}
        >
          <div className="flex items-center gap-2">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
              />
            </svg>
            <span>{state.error}</span>
          </div>
          <button
            onClick={clearError}
            className="text-xs font-medium hover:underline"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Messages */}
      <MessageContainer
        messages={messages}
        isThinking={state.isThinking}
        onSuggestionSelect={sendMessage}
        isConnected={state.connectionStatus === 'CONNECTED'}
        userName={userContext.displayName || userContext.userId}
        onRegenerate={handleRegenerate}
        onEditMessage={handleEditMessage}
        onDeleteMessage={handleDeleteMessage}
      />

      {/* Input */}
      <InputArea
        onSendMessage={sendMessage}
        connectionStatus={state.connectionStatus}
        disabled={state.isThinking}
        isStreaming={isStreaming}
      />
    </main>
  );
}
