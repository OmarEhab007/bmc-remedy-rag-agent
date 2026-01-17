import { useState, useCallback } from 'react';

interface MessageActionsProps {
  role: 'user' | 'assistant';
  content: string;
  onRegenerate?: () => void;
  onEdit?: () => void;
  onDelete?: () => void;
  className?: string;
}

export function MessageActions({
  role,
  content,
  onRegenerate,
  onEdit,
  onDelete,
  className = '',
}: MessageActionsProps) {
  const [copied, setCopied] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  const handleCopy = useCallback(async () => {
    try {
      await navigator.clipboard.writeText(content);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error('Failed to copy:', err);
    }
  }, [content]);

  const handleDeleteClick = () => {
    setShowDeleteConfirm(true);
    // Auto-hide after 3 seconds if not confirmed
    setTimeout(() => setShowDeleteConfirm(false), 3000);
  };

  const handleConfirmDelete = () => {
    onDelete?.();
    setShowDeleteConfirm(false);
  };

  return (
    <div className={`message-actions ${className}`}>
      {/* Copy button */}
      <button
        onClick={handleCopy}
        className="action-button"
        title={copied ? 'Copied!' : 'Copy'}
        aria-label={copied ? 'Copied to clipboard' : 'Copy to clipboard'}
      >
        {copied ? (
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
          </svg>
        ) : (
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"
            />
          </svg>
        )}
      </button>

      {/* Edit button (for user messages) */}
      {role === 'user' && onEdit && (
        <button
          onClick={onEdit}
          className="action-button"
          title="Edit message"
          aria-label="Edit message"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"
            />
          </svg>
        </button>
      )}

      {/* Regenerate button (for assistant messages) */}
      {role === 'assistant' && onRegenerate && (
        <button
          onClick={onRegenerate}
          className="action-button"
          title="Regenerate response"
          aria-label="Regenerate response"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
            />
          </svg>
        </button>
      )}

      {/* Delete button */}
      {onDelete && (
        showDeleteConfirm ? (
          <button
            onClick={handleConfirmDelete}
            className="action-button text-error"
            title="Confirm delete"
            aria-label="Confirm delete message"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
            </svg>
          </button>
        ) : (
          <button
            onClick={handleDeleteClick}
            className="action-button"
            title="Delete message"
            aria-label="Delete message"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
              />
            </svg>
          </button>
        )
      )}
    </div>
  );
}
