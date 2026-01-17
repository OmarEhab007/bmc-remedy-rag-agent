import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Modal } from './Modal';
import { useTheme } from '../providers/ThemeProvider';
import { useLanguage } from '../providers/LanguageProvider';

interface SettingsPanelProps {
  isOpen: boolean;
  onClose: () => void;
  onClearAllData?: () => void;
}

type ThemeOption = 'light' | 'dark' | 'system';
type LanguageOption = 'en' | 'ar';

export function SettingsPanel({ isOpen, onClose, onClearAllData }: SettingsPanelProps) {
  const { t } = useTranslation();
  const { theme, setTheme } = useTheme();
  const { language, setLanguage } = useLanguage();
  const [showClearConfirm, setShowClearConfirm] = useState(false);

  const themeOptions: { value: ThemeOption; label: string; description: string }[] = [
    { value: 'light', label: t('settings.light'), description: t('settings.lightDesc') },
    { value: 'dark', label: t('settings.dark'), description: t('settings.darkDesc') },
    { value: 'system', label: t('settings.system'), description: t('settings.systemDesc') },
  ];

  const languageOptions: { value: LanguageOption; label: string; nativeLabel: string }[] = [
    { value: 'en', label: t('settings.english'), nativeLabel: 'English' },
    { value: 'ar', label: t('settings.arabic'), nativeLabel: 'العربية' },
  ];

  const handleClearAllData = () => {
    onClearAllData?.();
    setShowClearConfirm(false);
    onClose();
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={t('settings.title')} size="md">
      <div className="space-y-6">
        {/* Language Selection */}
        <section>
          <h3 className="text-sm font-medium text-main mb-3">{t('settings.language')}</h3>
          <div className="space-y-2">
            {languageOptions.map((option) => (
              <label
                key={option.value}
                className={`flex items-center gap-3 p-3 rounded-lg border cursor-pointer transition-colors ${
                  language === option.value
                    ? 'border-accent bg-accent-light'
                    : 'border-main hover:bg-secondary'
                }`}
              >
                <input
                  type="radio"
                  name="language"
                  value={option.value}
                  checked={language === option.value}
                  onChange={() => setLanguage(option.value)}
                  className="sr-only"
                />
                <div
                  className={`w-4 h-4 rounded-full border-2 flex items-center justify-center ${
                    language === option.value
                      ? 'border-accent'
                      : 'border-muted'
                  }`}
                >
                  {language === option.value && (
                    <div className="w-2 h-2 rounded-full bg-accent" />
                  )}
                </div>
                <div className="flex-1">
                  <div className="text-sm font-medium text-main">{option.nativeLabel}</div>
                  <div className="text-xs text-muted">{option.value === 'ar' ? 'Arabic' : 'English'}</div>
                </div>
              </label>
            ))}
          </div>
        </section>

        {/* Theme Selection */}
        <section>
          <h3 className="text-sm font-medium text-main mb-3">{t('settings.appearance')}</h3>
          <div className="space-y-2">
            {themeOptions.map((option) => (
              <label
                key={option.value}
                className={`flex items-center gap-3 p-3 rounded-lg border cursor-pointer transition-colors ${
                  theme === option.value
                    ? 'border-accent bg-accent-light'
                    : 'border-main hover:bg-secondary'
                }`}
              >
                <input
                  type="radio"
                  name="theme"
                  value={option.value}
                  checked={theme === option.value}
                  onChange={() => setTheme(option.value)}
                  className="sr-only"
                />
                <div
                  className={`w-4 h-4 rounded-full border-2 flex items-center justify-center ${
                    theme === option.value
                      ? 'border-accent'
                      : 'border-muted'
                  }`}
                >
                  {theme === option.value && (
                    <div className="w-2 h-2 rounded-full bg-accent" />
                  )}
                </div>
                <div className="flex-1">
                  <div className="text-sm font-medium text-main">{option.label}</div>
                  <div className="text-xs text-muted">{option.description}</div>
                </div>
              </label>
            ))}
          </div>
        </section>

        {/* Keyboard Shortcuts */}
        <section>
          <h3 className="text-sm font-medium text-main mb-3">{t('settings.keyboardShortcuts')}</h3>
          <div className="bg-secondary rounded-lg p-3 space-y-2">
            <div className="flex justify-between items-center text-sm">
              <span className="text-secondary">{t('settings.newConversation')}</span>
              <kbd className="px-2 py-1 bg-main rounded text-xs font-mono">Ctrl+N</kbd>
            </div>
            <div className="flex justify-between items-center text-sm">
              <span className="text-secondary">{t('settings.toggleSidebar')}</span>
              <kbd className="px-2 py-1 bg-main rounded text-xs font-mono">Ctrl+B</kbd>
            </div>
            <div className="flex justify-between items-center text-sm">
              <span className="text-secondary">{t('settings.closeDialogs')}</span>
              <kbd className="px-2 py-1 bg-main rounded text-xs font-mono">Esc</kbd>
            </div>
          </div>
        </section>

        {/* Danger Zone */}
        <section>
          <h3 className="text-sm font-medium text-error mb-3">{t('settings.dangerZone')}</h3>
          <div className="border border-error/30 rounded-lg p-4 bg-error/5">
            <div className="flex items-center justify-between">
              <div>
                <div className="text-sm font-medium text-main">{t('settings.clearAllData')}</div>
                <div className="text-xs text-muted">
                  {t('settings.clearAllDataDesc')}
                </div>
              </div>
              {showClearConfirm ? (
                <div className="flex gap-2">
                  <button
                    onClick={() => setShowClearConfirm(false)}
                    className="btn-ghost text-sm"
                  >
                    {t('common.cancel')}
                  </button>
                  <button
                    onClick={handleClearAllData}
                    className="btn-danger text-sm"
                  >
                    {t('common.confirm')}
                  </button>
                </div>
              ) : (
                <button
                  onClick={() => setShowClearConfirm(true)}
                  className="btn-danger text-sm"
                >
                  {t('settings.clearData')}
                </button>
              )}
            </div>
          </div>
        </section>

        {/* About */}
        <section className="pt-4 border-t border-main">
          <div className="flex items-center gap-3">
            <div
              className="w-10 h-10 rounded-lg flex items-center justify-center"
              style={{ background: 'linear-gradient(135deg, var(--color-accent) 0%, var(--bmc-orange-gradient-end) 100%)' }}
            >
              <svg className="w-6 h-6 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <path strokeLinecap="round" strokeLinejoin="round" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09z" />
              </svg>
            </div>
            <div>
              <div className="text-sm font-medium text-main">Damee GPT</div>
              <div className="text-xs text-muted">{t('common.version')} 1.0.0</div>
            </div>
          </div>
        </section>
      </div>
    </Modal>
  );
}

export default SettingsPanel;
