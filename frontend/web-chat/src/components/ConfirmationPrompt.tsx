import { useState, useEffect, useCallback, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { confirmAction, cancelAction } from '../services/actionsApi';
import type { PendingActionInfo } from '../types/chat';

interface ConfirmationPromptProps {
  action: PendingActionInfo;
  sessionId: string;
  onConfirmed: (result: { success: boolean; message: string; recordId?: string }) => void;
  onCancelled: () => void;
}

/**
 * Component for displaying and handling action confirmation prompts.
 * Used for agentic operations like incident/work order creation.
 */
export function ConfirmationPrompt({
  action,
  sessionId,
  onConfirmed,
  onCancelled,
}: ConfirmationPromptProps) {
  const { t } = useTranslation();
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [timeRemaining, setTimeRemaining] = useState<number | null>(null);

  // Calculate time remaining until expiry
  useEffect(() => {
    const updateTimeRemaining = () => {
      const expiresAt = new Date(action.expiresAt).getTime();
      const now = Date.now();
      const remaining = Math.max(0, Math.floor((expiresAt - now) / 1000));
      setTimeRemaining(remaining);
    };

    updateTimeRemaining();
    const interval = setInterval(updateTimeRemaining, 1000);

    return () => clearInterval(interval);
  }, [action.expiresAt]);

  // Format time remaining
  const formattedTimeRemaining = useMemo(() => {
    if (timeRemaining === null) return '';
    const minutes = Math.floor(timeRemaining / 60);
    const seconds = timeRemaining % 60;
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  }, [timeRemaining]);

  // Check if expired
  const isExpired = timeRemaining !== null && timeRemaining <= 0;

  const handleConfirm = useCallback(async () => {
    if (isLoading || isExpired) return;

    setIsLoading(true);
    setError(null);

    try {
      const result = await confirmAction(sessionId, action.actionId);
      onConfirmed({
        success: result.success,
        message: result.message,
        recordId: result.createdRecordNumber || result.createdRecordId,
      });
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to confirm action';
      setError(errorMessage);
    } finally {
      setIsLoading(false);
    }
  }, [sessionId, action.actionId, isLoading, isExpired, onConfirmed]);

  const handleCancel = useCallback(async () => {
    if (isLoading) return;

    setIsLoading(true);
    setError(null);

    try {
      await cancelAction(sessionId, action.actionId);
      onCancelled();
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to cancel action';
      setError(errorMessage);
    } finally {
      setIsLoading(false);
    }
  }, [sessionId, action.actionId, isLoading, onCancelled]);

  // Get action type label
  const actionTypeLabel = useMemo(() => {
    switch (action.actionType) {
      case 'INCIDENT_CREATE':
        return t('actions.createIncident', 'Create Incident');
      case 'WORK_ORDER_CREATE':
        return t('actions.createWorkOrder', 'Create Work Order');
      default:
        return action.actionType;
    }
  }, [action.actionType, t]);

  return (
    <div className="confirmation-prompt mt-4 p-4 rounded-lg border border-amber-200 bg-amber-50 dark:border-amber-800 dark:bg-amber-900/20">
      {/* Header */}
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2">
          <svg
            className="w-5 h-5 text-amber-600 dark:text-amber-400"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
            />
          </svg>
          <span className="font-medium text-amber-800 dark:text-amber-200">
            {t('actions.confirmationRequired', 'Confirmation Required')}
          </span>
        </div>

        {/* Timer */}
        {!isExpired && timeRemaining !== null && (
          <span
            className={`text-sm font-mono ${
              timeRemaining < 60
                ? 'text-red-600 dark:text-red-400'
                : 'text-amber-600 dark:text-amber-400'
            }`}
          >
            {t('actions.expiresIn', 'Expires in')} {formattedTimeRemaining}
          </span>
        )}
      </div>

      {/* Action type badge */}
      <div className="mb-3">
        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200">
          {actionTypeLabel}
        </span>
        <span className="ml-2 text-xs text-gray-500 dark:text-gray-400">
          ID: {action.actionId}
        </span>
      </div>

      {/* Preview */}
      {action.preview && (
        <div className="mb-4 p-3 rounded bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700">
          <pre className="text-sm whitespace-pre-wrap text-gray-700 dark:text-gray-300 font-sans">
            {action.preview}
          </pre>
        </div>
      )}

      {/* Error message */}
      {error && (
        <div className="mb-3 p-2 rounded bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300 text-sm">
          {error}
        </div>
      )}

      {/* Expired message */}
      {isExpired && (
        <div className="mb-3 p-2 rounded bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 text-sm">
          {t('actions.expired', 'This action has expired. Please request a new one.')}
        </div>
      )}

      {/* Action buttons */}
      <div className="flex gap-3">
        <button
          onClick={handleConfirm}
          disabled={isLoading || isExpired}
          className={`flex-1 px-4 py-2 rounded-lg font-medium text-white transition-colors ${
            isLoading || isExpired
              ? 'bg-gray-400 cursor-not-allowed'
              : 'bg-green-600 hover:bg-green-700 dark:bg-green-500 dark:hover:bg-green-600'
          }`}
        >
          {isLoading ? (
            <span className="flex items-center justify-center gap-2">
              <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24">
                <circle
                  className="opacity-25"
                  cx="12"
                  cy="12"
                  r="10"
                  stroke="currentColor"
                  strokeWidth="4"
                  fill="none"
                />
                <path
                  className="opacity-75"
                  fill="currentColor"
                  d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                />
              </svg>
              {t('actions.confirming', 'Confirming...')}
            </span>
          ) : (
            <>
              <svg className="inline w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
              </svg>
              {t('common.confirm')}
            </>
          )}
        </button>

        <button
          onClick={handleCancel}
          disabled={isLoading}
          className={`flex-1 px-4 py-2 rounded-lg font-medium transition-colors ${
            isLoading
              ? 'bg-gray-200 text-gray-400 cursor-not-allowed dark:bg-gray-700'
              : 'bg-gray-200 text-gray-700 hover:bg-gray-300 dark:bg-gray-700 dark:text-gray-300 dark:hover:bg-gray-600'
          }`}
        >
          <svg className="inline w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
          </svg>
          {t('common.cancel')}
        </button>
      </div>
    </div>
  );
}

/**
 * Compact inline confirmation buttons for use within message content.
 */
export function InlineConfirmationButtons({
  actionId,
  sessionId,
  onConfirmed,
  onCancelled,
}: {
  actionId: string;
  sessionId: string;
  onConfirmed: () => void;
  onCancelled: () => void;
}) {
  const { t } = useTranslation();
  const [isLoading, setIsLoading] = useState(false);

  const handleConfirm = useCallback(async () => {
    setIsLoading(true);
    try {
      await confirmAction(sessionId, actionId);
      onConfirmed();
    } catch {
      // Error handling would go here
    } finally {
      setIsLoading(false);
    }
  }, [sessionId, actionId, onConfirmed]);

  const handleCancel = useCallback(async () => {
    setIsLoading(true);
    try {
      await cancelAction(sessionId, actionId);
      onCancelled();
    } catch {
      // Error handling would go here
    } finally {
      setIsLoading(false);
    }
  }, [sessionId, actionId, onCancelled]);

  return (
    <span className="inline-flex gap-2 ml-2">
      <button
        onClick={handleConfirm}
        disabled={isLoading}
        className="px-2 py-0.5 text-xs rounded bg-green-600 text-white hover:bg-green-700 disabled:opacity-50"
      >
        {t('common.confirm')}
      </button>
      <button
        onClick={handleCancel}
        disabled={isLoading}
        className="px-2 py-0.5 text-xs rounded bg-gray-300 text-gray-700 hover:bg-gray-400 disabled:opacity-50 dark:bg-gray-600 dark:text-gray-200"
      >
        {t('common.cancel')}
      </button>
    </span>
  );
}
