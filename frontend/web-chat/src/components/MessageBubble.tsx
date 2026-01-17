import { useMemo, useState } from 'react';
import ReactMarkdown from 'react-markdown';
import type { ChatMessage } from '../types/chat';
import { CitationBlock, InlineCitation } from './CitationBlock';
import { Avatar } from './Avatar';
import { MessageActions } from './MessageActions';
import { FeedbackButtons } from './FeedbackButtons';
import { CodeBlock } from './CodeBlock';
import type { TextSegment } from '../utils/incidentParser';
import { parseTextWithReferences } from '../utils/incidentParser';

interface MessageBubbleProps {
  message: ChatMessage;
  userName?: string;
  onRegenerate?: () => void;
  onEdit?: (content: string) => void;
  onDelete?: () => void;
}

export function MessageBubble({
  message,
  userName,
  onRegenerate,
  onEdit,
  onDelete,
}: MessageBubbleProps) {
  const isUser = message.role === 'user';
  const isStreaming = message.isStreaming;
  const [isEditing, setIsEditing] = useState(false);
  const [editContent, setEditContent] = useState(message.content);

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
                    Save & Submit
                  </button>
                  <button
                    onClick={handleCancelEdit}
                    className="px-4 py-2 text-sm font-medium text-secondary hover:text-main"
                  >
                    Cancel
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

                {/* Citations */}
                {message.citations && message.citations.length > 0 && (
                  <CitationBlock citations={message.citations} />
                )}

                {/* Footer: timestamp, confidence, actions */}
                <div className="flex items-center justify-between mt-3 pt-2">
                  <div className="flex items-center gap-3 text-xs text-muted">
                    <span>{formattedTime}</span>
                    {message.confidenceScore !== undefined && (
                      <ConfidenceIndicator score={message.confidenceScore} />
                    )}
                  </div>

                  {/* Actions */}
                  <div className="flex items-center gap-2">
                    <MessageActions
                      role="assistant"
                      content={message.content}
                      onRegenerate={onRegenerate}
                      onEdit={undefined}
                      onDelete={onDelete}
                    />
                    {!isStreaming && (
                      <FeedbackButtons messageId={message.id} />
                    )}
                  </div>
                </div>
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
                Save & Submit
              </button>
              <button
                onClick={handleCancelEdit}
                className="px-4 py-2 text-sm font-medium text-secondary hover:text-main"
              >
                Cancel
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

            {/* Citations */}
            {!isUser && message.citations && message.citations.length > 0 && (
              <CitationBlock citations={message.citations} />
            )}

            {/* Footer: timestamp, confidence, actions */}
            <div className="flex items-center justify-between mt-3 pt-2">
              <div className="flex items-center gap-3 text-xs text-muted">
                <span>{formattedTime}</span>
                {!isUser && message.confidenceScore !== undefined && (
                  <ConfidenceIndicator score={message.confidenceScore} />
                )}
              </div>

              {/* Actions */}
              <div className="flex items-center gap-2">
                <MessageActions
                  role={isUser ? 'user' : 'assistant'}
                  content={message.content}
                  onRegenerate={!isUser ? onRegenerate : undefined}
                  onEdit={isUser && onEdit ? handleEdit : undefined}
                  onDelete={onDelete}
                />
                {!isUser && !isStreaming && (
                  <FeedbackButtons messageId={message.id} />
                )}
              </div>
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

interface ConfidenceIndicatorProps {
  score: number;
}

function ConfidenceIndicator({ score }: ConfidenceIndicatorProps) {
  const percentage = Math.round(score * 100);
  let colorClass = 'text-success';
  let label = 'High confidence';

  if (score < 0.5) {
    colorClass = 'text-error';
    label = 'Low confidence';
  } else if (score < 0.75) {
    colorClass = 'text-warning';
    label = 'Medium confidence';
  }

  return (
    <span className={`flex items-center gap-1 ${colorClass}`} title={label}>
      <svg
        className="w-3 h-3"
        fill="currentColor"
        viewBox="0 0 20 20"
        aria-hidden="true"
      >
        <path
          fillRule="evenodd"
          d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.857-9.809a.75.75 0 00-1.214-.882l-3.483 4.79-1.88-1.88a.75.75 0 10-1.06 1.061l2.5 2.5a.75.75 0 001.137-.089l4-5.5z"
          clipRule="evenodd"
        />
      </svg>
      <span>{percentage}%</span>
    </span>
  );
}
