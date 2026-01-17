import { useState, useCallback } from 'react';
import { useTranslation } from 'react-i18next';

type FeedbackType = 'positive' | 'negative' | null;

interface FeedbackButtonsProps {
  messageId: string;
  onFeedback?: (messageId: string, feedback: FeedbackType) => void;
  className?: string;
}

export function FeedbackButtons({
  messageId,
  onFeedback,
  className = '',
}: FeedbackButtonsProps) {
  const { t } = useTranslation();
  const [feedback, setFeedback] = useState<FeedbackType>(null);
  const [showFeedbackInput, setShowFeedbackInput] = useState(false);

  const handleFeedback = useCallback(
    (type: FeedbackType) => {
      // Toggle if clicking the same button
      const newFeedback = feedback === type ? null : type;
      setFeedback(newFeedback);

      // Show feedback input for negative feedback
      if (newFeedback === 'negative') {
        setShowFeedbackInput(true);
      } else {
        setShowFeedbackInput(false);
      }

      onFeedback?.(messageId, newFeedback);
    },
    [messageId, feedback, onFeedback]
  );

  return (
    <div className={`flex items-center gap-1 ${className}`}>
      {/* Thumbs up */}
      <button
        onClick={() => handleFeedback('positive')}
        className={`feedback-button ${feedback === 'positive' ? 'active-positive' : ''}`}
        title={t('messages.goodResponse')}
        aria-label={t('messages.markAsGood')}
        aria-pressed={feedback === 'positive'}
      >
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M14 10h4.764a2 2 0 011.789 2.894l-3.5 7A2 2 0 0115.263 21h-4.017c-.163 0-.326-.02-.485-.06L7 20m7-10V5a2 2 0 00-2-2h-.095c-.5 0-.905.405-.905.905 0 .714-.211 1.412-.608 2.006L7 11v9m7-10h-2M7 20H5a2 2 0 01-2-2v-6a2 2 0 012-2h2.5"
          />
        </svg>
      </button>

      {/* Thumbs down */}
      <button
        onClick={() => handleFeedback('negative')}
        className={`feedback-button ${feedback === 'negative' ? 'active-negative' : ''}`}
        title={t('messages.badResponse')}
        aria-label={t('messages.markAsBad')}
        aria-pressed={feedback === 'negative'}
      >
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M10 14H5.236a2 2 0 01-1.789-2.894l3.5-7A2 2 0 018.736 3h4.018a2 2 0 01.485.06l3.76.94m-7 10v5a2 2 0 002 2h.096c.5 0 .905-.405.905-.904 0-.715.211-1.413.608-2.008L17 13V4m-7 10h2m5-10h2a2 2 0 012 2v6a2 2 0 01-2 2h-2.5"
          />
        </svg>
      </button>

      {/* Optional feedback input for negative feedback */}
      {showFeedbackInput && (
        <FeedbackInput
          onSubmit={(text) => {
            console.log('Feedback text:', text);
            setShowFeedbackInput(false);
          }}
          onClose={() => setShowFeedbackInput(false)}
        />
      )}
    </div>
  );
}

interface FeedbackInputProps {
  onSubmit: (text: string) => void;
  onClose: () => void;
}

function FeedbackInput({ onSubmit, onClose }: FeedbackInputProps) {
  const { t } = useTranslation();
  const [text, setText] = useState('');

  const handleSubmit = () => {
    if (text.trim()) {
      onSubmit(text.trim());
      setText('');
    }
  };

  return (
    <div className="absolute bottom-full left-0 mb-2 p-3 bg-elevated border border-main rounded-lg shadow-lg w-72 animate-fade-in">
      <p className="text-sm text-secondary mb-2">{t('feedback.whatWasIssue')}</p>
      <textarea
        value={text}
        onChange={(e) => setText(e.target.value)}
        placeholder={t('feedback.provideFeedback')}
        className="w-full p-2 text-sm border border-main rounded-lg resize-none bg-main text-main focus:outline-none focus:ring-2 focus:ring-accent"
        rows={2}
        autoFocus
      />
      <div className="flex justify-end gap-2 mt-2">
        <button
          onClick={onClose}
          className="px-3 py-1 text-sm text-muted hover:text-main"
        >
          {t('common.skip')}
        </button>
        <button
          onClick={handleSubmit}
          className="px-3 py-1 text-sm bg-accent text-white rounded-md hover:bg-accent-dark"
          disabled={!text.trim()}
        >
          {t('common.submit')}
        </button>
      </div>
    </div>
  );
}
