import type { Citation } from '../types/chat';
import { CitationBlock } from './CitationBlock';
import { ConfidenceIndicator } from './ConfidenceIndicator';
import { FeedbackButtons } from './FeedbackButtons';

type FeedbackType = 'positive' | 'negative' | null;

interface MessageFooterProps {
  /** Source citations from the response */
  citations?: Citation[];
  /** Confidence score from 0.0 to 1.0 */
  confidenceScore?: number;
  /** Message timestamp */
  timestamp: Date;
  /** Unique message ID for feedback tracking */
  messageId: string;
  /** Callback when user submits feedback */
  onFeedback?: (messageId: string, feedback: FeedbackType) => void;
  /** Mid-tier URL for citation links */
  midTierUrl?: string;
  /** Additional CSS classes */
  className?: string;
}

/**
 * Unified footer for assistant messages.
 * Displays citations, timestamp, confidence indicator, and feedback buttons.
 *
 * Layout:
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  Sources                                                         │
 * │  [Badge] [Badge] [Badge]                                        │
 * │                                                                  │
 * │  04:01 PM    ✓ 85%                                    👍  👎   │
 * └─────────────────────────────────────────────────────────────────┘
 */
export function MessageFooter({
  citations,
  confidenceScore,
  timestamp,
  messageId,
  onFeedback,
  midTierUrl,
  className = '',
}: MessageFooterProps) {
  const hasCitations = citations && citations.length > 0;
  const hasMetadata = confidenceScore !== undefined || hasCitations;

  // Don't render if no content
  if (!hasMetadata) {
    return null;
  }

  // Format timestamp as "04:01 PM"
  const formattedTime = timestamp.toLocaleTimeString('en-US', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: true,
  });

  return (
    <div
      className={`
        message-footer
        mt-4 border border-orange-300 rounded-lg overflow-hidden
        bg-gradient-to-b from-orange-50/50 to-white
        ${className}
      `}
    >
      {/* Citations Section */}
      {hasCitations && (
        <div className="p-4 pb-0">
          <CitationBlock
            citations={citations}
            midTierUrl={midTierUrl}
          />
        </div>
      )}

      {/* Footer Row: Timestamp, Confidence, Feedback */}
      <div
        className={`
          flex items-center justify-between
          px-4 py-3
          ${hasCitations ? 'border-t border-orange-200 mt-3' : ''}
        `}
      >
        {/* Left side: Timestamp and Confidence */}
        <div className="flex items-center gap-4 text-sm text-gray-500">
          <span className="font-medium">{formattedTime}</span>
          {confidenceScore !== undefined && (
            <ConfidenceIndicator score={confidenceScore} />
          )}
        </div>

        {/* Right side: Feedback buttons */}
        <FeedbackButtons
          messageId={messageId}
          onFeedback={onFeedback}
        />
      </div>
    </div>
  );
}

/**
 * Compact version of the footer for smaller displays.
 */
export function MessageFooterCompact({
  citations,
  confidenceScore,
  timestamp,
  messageId,
  onFeedback,
  midTierUrl,
  className = '',
}: MessageFooterProps) {
  const hasCitations = citations && citations.length > 0;

  const formattedTime = timestamp.toLocaleTimeString('en-US', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: true,
  });

  return (
    <div className={`message-footer-compact mt-3 ${className}`}>
      {/* Inline citations */}
      {hasCitations && (
        <div className="mb-2">
          <CitationBlock
            citations={citations}
            midTierUrl={midTierUrl}
            maxDisplay={3}
          />
        </div>
      )}

      {/* Footer row */}
      <div className="flex items-center justify-between text-xs text-gray-500">
        <div className="flex items-center gap-2">
          <span>{formattedTime}</span>
          {confidenceScore !== undefined && (
            <ConfidenceIndicator
              score={confidenceScore}
              className="text-xs"
            />
          )}
        </div>
        <FeedbackButtons
          messageId={messageId}
          onFeedback={onFeedback}
          className="scale-90"
        />
      </div>
    </div>
  );
}
