import { useTranslation } from 'react-i18next';
import { SuggestionCard } from './SuggestionCard';
import { QuickChip } from './QuickChip';

interface EmptyStateProps {
  onSuggestionSelect: (query: string) => void;
  disabled?: boolean;
}

export function EmptyState({ onSuggestionSelect, disabled = false }: EmptyStateProps) {
  const { t } = useTranslation();

  return (
    <div className="empty-state">
      <div className="empty-state-content">
        {/* Animated Logo */}
        <div className="empty-state-logo">
          <div className="empty-state-logo-inner">
            <svg
              className="w-10 h-10"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.5"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09zM18.259 8.715L18 9.75l-.259-1.035a3.375 3.375 0 00-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 002.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 002.456 2.456L21.75 6l-1.035.259a3.375 3.375 0 00-2.456 2.456zM16.894 20.567L16.5 21.75l-.394-1.183a2.25 2.25 0 00-1.423-1.423L13.5 18.75l1.183-.394a2.25 2.25 0 001.423-1.423l.394-1.183.394 1.183a2.25 2.25 0 001.423 1.423l1.183.394-1.183.394a2.25 2.25 0 00-1.423 1.423z"
              />
            </svg>
          </div>
          <div className="empty-state-logo-ring" />
        </div>

        {/* Welcome Text */}
        <h1 className="empty-state-title">
          {t('emptyState.welcome')}
        </h1>
        <p className="empty-state-subtitle">
          {t('emptyState.subtitle')}
        </p>

        {/* Suggestion Cards */}
        <div className="suggestion-cards">
          <SuggestionCard
            icon={
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" />
              </svg>
            }
            title={t('emptyState.searchIncidents')}
            description={t('emptyState.searchIncidentsDesc')}
            onClick={() => onSuggestionSelect(t('suggestions.showRecentP1'))}
            disabled={disabled}
            gradient="blue"
          />
          <SuggestionCard
            icon={
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <path strokeLinecap="round" strokeLinejoin="round" d="M16.5 10.5V6.75a4.5 4.5 0 10-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 002.25-2.25v-6.75a2.25 2.25 0 00-2.25-2.25H6.75a2.25 2.25 0 00-2.25 2.25v6.75a2.25 2.25 0 002.25 2.25z" />
              </svg>
            }
            title={t('emptyState.passwordReset')}
            description={t('emptyState.passwordResetDesc')}
            onClick={() => onSuggestionSelect(t('suggestions.resetPassword'))}
            disabled={disabled}
            gradient="green"
          />
          <SuggestionCard
            icon={
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <path strokeLinecap="round" strokeLinejoin="round" d="M8.288 15.038a5.25 5.25 0 017.424 0M5.106 11.856c3.807-3.808 9.98-3.808 13.788 0M1.924 8.674c5.565-5.565 14.587-5.565 20.152 0M12.53 18.22l-.53.53-.53-.53a.75.75 0 011.06 0z" />
              </svg>
            }
            title={t('emptyState.vpnTroubleshooting')}
            description={t('emptyState.vpnTroubleshootingDesc')}
            onClick={() => onSuggestionSelect(t('suggestions.vpnSteps'))}
            disabled={disabled}
            gradient="purple"
          />
          <SuggestionCard
            icon={
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <path strokeLinecap="round" strokeLinejoin="round" d="M21.75 6.75v10.5a2.25 2.25 0 01-2.25 2.25h-15a2.25 2.25 0 01-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0019.5 4.5h-15a2.25 2.25 0 00-2.25 2.25m19.5 0v.243a2.25 2.25 0 01-1.07 1.916l-7.5 4.615a2.25 2.25 0 01-2.36 0L3.32 8.91a2.25 2.25 0 01-1.07-1.916V6.75" />
            </svg>
            }
            title={t('emptyState.emailIssues')}
            description={t('emptyState.emailIssuesDesc')}
            onClick={() => onSuggestionSelect(t('suggestions.outlookSync'))}
            disabled={disabled}
            gradient="orange"
          />
        </div>

        {/* Quick Actions */}
        <div className="quick-actions">
          <span className="quick-actions-label">{t('emptyState.quickActions')}</span>
          <div className="quick-actions-chips">
            <QuickChip
              label={t('emptyState.recentIncidents')}
              onClick={() => onSuggestionSelect(t('suggestions.showRecent'))}
              disabled={disabled}
            />
            <QuickChip
              label={t('emptyState.myOpenTickets')}
              onClick={() => onSuggestionSelect(t('suggestions.openTickets'))}
              disabled={disabled}
            />
            <QuickChip
              label={t('emptyState.systemStatus')}
              onClick={() => onSuggestionSelect(t('suggestions.currentStatus'))}
              disabled={disabled}
            />
            <QuickChip
              label={t('emptyState.knowledgeBase')}
              onClick={() => onSuggestionSelect(t('suggestions.searchKB'))}
              disabled={disabled}
            />
          </div>
        </div>
      </div>
    </div>
  );
}

export default EmptyState;
