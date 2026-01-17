import { useMemo, useState, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import ReactMarkdown from 'react-markdown';
import type { ChatMessage } from '../types/chat';
import { InlineCitation } from './CitationBlock';
import { Avatar } from './Avatar';
import { MessageActions } from './MessageActions';
import { MessageFooter } from './MessageFooter';
import { CodeBlock } from './CodeBlock';
import { ConfirmationPrompt } from './ConfirmationPrompt';
import type { TextSegment } from '../utils/incidentParser';
import { parseTextWithReferences } from '../utils/incidentParser';
import { isConfirmationPrompt, parseActionId } from '../services/actionsApi';

interface MessageBubbleProps {
  message: ChatMessage;
  userName?: string;
  sessionId?: string;
  onRegenerate?: () => void;
  onEdit?: (content: string) => void;
  onDelete?: () => void;
  onActionConfirmed?: (result: { success: boolean; message: string; recordId?: string }) => void;
  onActionCancelled?: () => void;
}

export function MessageBubble({
  message,
  userName,
  sessionId,
  onRegenerate,
  onEdit,
  onDelete,
  onActionConfirmed,
  onActionCancelled,
}: MessageBubbleProps) {
  const { t } = useTranslation();
  const isUser = message.role === 'user';
  const isStreaming = message.isStreaming;
  const [isEditing, setIsEditing] = useState(false);
  const [editContent, setEditContent] = useState(message.content);
  const [actionHandled, setActionHandled] = useState(false);

  // Check if this message contains a confirmation prompt
  const hasConfirmationPrompt = useMemo(() => {
    if (isUser || isStreaming || actionHandled) return false;
    return message.pendingAction || isConfirmationPrompt(message.content);
  }, [isUser, isStreaming, actionHandled, message.pendingAction, message.content]);

  // Parse action ID from content if not provided in pendingAction
  // This can be used for inline confirmation buttons in the future
  const _detectedActionId = useMemo(() => {
    if (message.pendingAction?.actionId) return message.pendingAction.actionId;
    if (hasConfirmationPrompt) return parseActionId(message.content);
    return null;
  }, [message.pendingAction, hasConfirmationPrompt, message.content]);
  void _detectedActionId; // Suppress unused warning - reserved for future use

  // Handle action confirmed
  const handleActionConfirmed = useCallback(
    (result: { success: boolean; message: string; recordId?: string }) => {
      setActionHandled(true);
      onActionConfirmed?.(result);
    },
    [onActionConfirmed]
  );

  // Handle action cancelled
  const handleActionCancelled = useCallback(() => {
    setActionHandled(true);
    onActionCancelled?.();
  }, [onActionCancelled]);

  const formattedTime = useMemo(() => {
    return message.timestamp.toLocaleTimeString([], {
      hour: '2-digit',
      minute: '2-digit',
    });
  }, [message.timestamp]);

  const handleEdit = () => {
    setEditContent(message.content);
    setIsEditing(true);
  };

  const handleSaveEdit = () => {
    if (editContent.trim() && editContent !== message.content) {
      onEdit?.(editContent.trim());
    }
    setIsEditing(false);
  };

  const handleCancelEdit = () => {
    setEditContent(message.content);
    setIsEditing(false);
  };

  // For assistant messages, we need full-width background with centered content
  if (!isUser) {
    return (
      <div className="message-row-assistant-wrapper animate-fade-in">
        <div className="message-row-inner">
          {/* Avatar */}
          <Avatar
            type="assistant"
            name={undefined}
          />

          {/* Message content */}
          <div className="message-content">
            {/* Edit mode */}
            {isEditing ? (
              <div className="space-y-3">
                <textarea
                  value={editContent}
                  onChange={(e) => setEditContent(e.target.value)}
                  className="w-full p-3 border rounded-lg resize-none bg-main text-main border-main focus:outline-none focus:ring-2 focus:ring-accent"
                  rows={4}
                  autoFocus
                />
                <div className="flex gap-2">
                  <button
                    onClick={handleSaveEdit}
                    className="px-4 py-2 text-sm font-medium text-white rounded-lg"
                    style={{ backgroundColor: 'var(--color-accent)' }}
                  >
                    {t('messages.saveAndSubmit')}
                  </button>
                  <button
                    onClick={handleCancelEdit}
                    className="px-4 py-2 text-sm font-medium text-secondary hover:text-main"
                  >
                    {t('common.cancel')}
                  </button>
                </div>
              </div>
            ) : (
              <>
                {/* Message text */}
                <div className="prose prose-sm max-w-none">
                  <MessageContent
                    content={message.content}
                    isStreaming={isStreaming}
                  />
                </div>

                {/* Streaming cursor */}
                {isStreaming && (
                  <span className="streaming-cursor" aria-hidden="true" />
                )}

                {/* Error indicator */}
                {message.error && (
                  <div className="mt-3 p-3 rounded-lg text-sm flex items-start gap-2"
                       style={{ backgroundColor: 'rgba(239, 68, 68, 0.1)', color: 'var(--color-error)' }}>
                    <svg className="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    <span>{message.error}</span>
                  </div>
                )}

                {/* Confirmation prompt for agentic actions */}
                {hasConfirmationPrompt && sessionId && message.pendingAction && (
                  <ConfirmationPrompt
                    action={message.pendingAction}
                    sessionId={sessionId}
                    onConfirmed={handleActionConfirmed}
                    onCancelled={handleActionCancelled}
                  />
                )}

                {/* Action handled indicator */}
                {actionHandled && (
                  <div className="mt-3 p-2 rounded-lg text-sm text-green-700 dark:text-green-300 bg-green-50 dark:bg-green-900/20">
                    {t('actions.handled', 'Action has been processed.')}
                  </div>
                )}

                {/* Message Actions (regenerate, copy, delete) */}
                <div className="flex justify-end mt-2">
                  <MessageActions
                    role="assistant"
                    content={message.content}
                    onRegenerate={onRegenerate}
                    onEdit={undefined}
                    onDelete={onDelete}
                  />
                </div>

                {/* Citations, timestamp, confidence, feedback */}
                {!isStreaming && (
                  <MessageFooter
                    citations={message.citations}
                    confidenceScore={message.confidenceScore}
                    timestamp={message.timestamp}
                    messageId={message.id}
                  />
                )}
              </>
            )}
          </div>
        </div>
      </div>
    );
  }

  // User message
  return (
    <div className="message-row message-row-user animate-fade-in">
      {/* Avatar */}
      <Avatar
        type="user"
        name={userName}
      />

      {/* Message content */}
      <div className="message-content">
        {/* Edit mode */}
        {isEditing ? (
          <div className="space-y-3">
            <textarea
              value={editContent}
              onChange={(e) => setEditContent(e.target.value)}
              className="w-full p-3 border rounded-lg resize-none bg-main text-main border-main focus:outline-none focus:ring-2 focus:ring-accent"
              rows={4}
              autoFocus
            />
            <div className="flex gap-2">
              <button
                onClick={handleSaveEdit}
                className="px-4 py-2 text-sm font-medium text-white rounded-lg"
                style={{ backgroundColor: 'var(--color-accent)' }}
              >
                {t('messages.saveAndSubmit')}
              </button>
              <button
                onClick={handleCancelEdit}
                className="px-4 py-2 text-sm font-medium text-secondary hover:text-main"
              >
                {t('common.cancel')}
              </button>
            </div>
          </div>
        ) : (
          <>
            {/* Message text */}
            <div className="prose prose-sm max-w-none">
              {isUser ? (
                <p className="m-0 whitespace-pre-wrap">{message.content}</p>
              ) : (
                <MessageContent
                  content={message.content}
                  isStreaming={isStreaming}
                />
              )}
            </div>

            {/* Streaming cursor */}
            {isStreaming && (
              <span className="streaming-cursor" aria-hidden="true" />
            )}

            {/* Error indicator */}
            {message.error && (
              <div className="mt-3 p-3 rounded-lg text-sm flex items-start gap-2"
                   style={{ backgroundColor: 'rgba(239, 68, 68, 0.1)', color: 'var(--color-error)' }}>
                <svg className="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <span>{message.error}</span>
              </div>
            )}

            {/* Footer: timestamp and actions for user messages */}
            <div className="flex items-center justify-between mt-3 pt-2">
              <span className="text-xs text-muted">{formattedTime}</span>
              <MessageActions
                role="user"
                content={message.content}
                onRegenerate={undefined}
                onEdit={onEdit ? handleEdit : undefined}
                onDelete={onDelete}
              />
            </div>
          </>
        )}
      </div>
    </div>
  );
}

interface MessageContentProps {
  content: string;
  isStreaming?: boolean;
}

function MessageContent({ content }: MessageContentProps) {
  // Parse content for ITSM record references
  const segments = useMemo(() => parseTextWithReferences(content), [content]);

  // Check if there are code blocks
  const hasCodeBlocks = content.includes('```');

  // If there are code blocks, render with custom code block component
  if (hasCodeBlocks) {
    return (
      <ReactMarkdown
        components={{
          code({ className, children, ...props }) {
            const match = /language-(\w+)/.exec(className || '');
            const isInline = !match && !String(children).includes('\n');

            if (isInline) {
              return (
                <code className={className} {...props}>
                  {children}
                </code>
              );
            }

            return (
              <CodeBlock
                code={String(children).replace(/\n$/, '')}
                language={match?.[1]}
              />
            );
          },
          // Prevent wrapping code blocks in pre
          pre({ children }) {
            return <>{children}</>;
          },
        }}
      >
        {content}
      </ReactMarkdown>
    );
  }

  // If no special references, use ReactMarkdown for rendering
  if (segments.every((s) => s.type === 'text')) {
    return <ReactMarkdown>{content}</ReactMarkdown>;
  }

  // Render with inline citations
  return (
    <div>
      {segments.map((segment, index) => (
        <MessageSegment key={index} segment={segment} />
      ))}
    </div>
  );
}

interface MessageSegmentProps {
  segment: TextSegment;
}

function MessageSegment({ segment }: MessageSegmentProps) {
  if (segment.type === 'text') {
    // Render text segments with markdown
    return <ReactMarkdown>{segment.content}</ReactMarkdown>;
  }

  // Map segment type to record type
  const recordType = segment.type.toUpperCase();

  return (
    <InlineCitation recordId={segment.content} recordType={recordType} />
  );
}

