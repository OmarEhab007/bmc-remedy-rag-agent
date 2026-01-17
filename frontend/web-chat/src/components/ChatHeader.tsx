import { useState, useRef, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import type { UserContext } from '../types/chat';
import { ConnectionStatus } from '../types/chat';
import { ThemeToggle } from '../providers/ThemeProvider';
import { appConfig, getModelDisplayString } from '../config/appConfig';

// Custom hook for focus trap
function useFocusTrap(isOpen: boolean, containerRef: React.RefObject<HTMLElement | null>) {
  useEffect(() => {
    if (!isOpen || !containerRef.current) return;

    const container = containerRef.current;
    const focusableElements = container.querySelectorAll<HTMLElement>(
      'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
    );
    const firstElement = focusableElements[0];
    const lastElement = focusableElements[focusableElements.length - 1];

    // Focus first element when opened
    firstElement?.focus();

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key !== 'Tab') return;

      if (e.shiftKey) {
        if (document.activeElement === firstElement) {
          e.preventDefault();
          lastElement?.focus();
        }
      } else {
        if (document.activeElement === lastElement) {
          e.preventDefault();
          firstElement?.focus();
        }
      }
    };

    container.addEventListener('keydown', handleKeyDown);
    return () => container.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, containerRef]);
}

interface ChatHeaderProps {
  connectionStatus: ConnectionStatus;
  userContext: UserContext;
  onMenuClick?: () => void;
  onNewChat?: () => void;
  onClearChat?: () => void;
  onReconnect?: () => void;
  onExportJSON?: () => void;
  onExportMarkdown?: () => void;
  onOpenSettings?: () => void;
}

export function ChatHeader({
  connectionStatus,
  userContext,
  onMenuClick,
  onNewChat,
  onClearChat,
  onReconnect,
  onExportJSON,
  onExportMarkdown,
  onOpenSettings,
}: ChatHeaderProps) {
  const { t } = useTranslation();
  const [showExportMenu, setShowExportMenu] = useState(false);
  const exportMenuRef = useRef<HTMLDivElement>(null);

  // Close export menu when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (exportMenuRef.current && !exportMenuRef.current.contains(event.target as Node)) {
        setShowExportMenu(false);
      }
    };

    if (showExportMenu) {
      document.addEventListener('mousedown', handleClickOutside);
    }

    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [showExportMenu]);
  return (
    <header className="header-minimal relative">
      {/* Left section */}
      <div className="flex items-center gap-3">
        {/* Mobile menu button */}
        <button
          onClick={onMenuClick}
          className="md:hidden p-2 rounded-lg hover:bg-tertiary transition-colors"
          aria-label={t('header.openMenu')}
        >
          <svg
            className="w-5 h-5 text-main"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M4 6h16M4 12h16M4 18h16"
            />
          </svg>
        </button>
      </div>

      {/* Center - Model selector */}
      <div className="header-center">
        <ModelSelector connectionStatus={connectionStatus} onReconnect={onReconnect} />
      </div>

      {/* Right section */}
      <div className="flex items-center gap-1">
        {/* New chat button */}
        <button
          onClick={onNewChat}
          className="p-2 rounded-lg hover:bg-tertiary transition-colors"
          title={t('header.newChatShortcut')}
          aria-label={t('header.newChat')}
        >
          <svg
            className="w-5 h-5 text-secondary"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M12 4v16m8-8H4"
            />
          </svg>
        </button>

        {/* Clear chat button */}
        <button
          onClick={onClearChat}
          className="p-2 rounded-lg hover:bg-tertiary transition-colors"
          title={t('header.clearConversation')}
          aria-label={t('header.clearConversation')}
        >
          <svg
            className="w-5 h-5 text-secondary"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
            />
          </svg>
        </button>

        {/* Export button with dropdown */}
        {(onExportJSON || onExportMarkdown) && (
          <div className="relative" ref={exportMenuRef}>
            <button
              onClick={() => setShowExportMenu(!showExportMenu)}
              className="p-2 rounded-lg hover:bg-tertiary transition-colors"
              title={t('header.exportConversation')}
              aria-label={t('header.exportConversation')}
              aria-expanded={showExportMenu}
              aria-haspopup="true"
            >
              <svg
                className="w-5 h-5 text-secondary"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                />
              </svg>
            </button>

            {showExportMenu && (
              <div
                className="absolute right-0 top-full mt-2 w-48 bg-elevated border border-main rounded-lg shadow-lg z-50 overflow-hidden animate-fade-in"
                role="menu"
              >
                {onExportJSON && (
                  <button
                    onClick={() => {
                      onExportJSON();
                      setShowExportMenu(false);
                    }}
                    className="w-full px-4 py-3 text-left text-sm hover:bg-secondary transition-colors flex items-center gap-2"
                    role="menuitem"
                  >
                    <svg className="w-4 h-4 text-muted" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 8l4 4m0 0l-4 4m4-4H3" />
                    </svg>
                    <span className="text-main">{t('header.exportAsJSON')}</span>
                  </button>
                )}
                {onExportMarkdown && (
                  <button
                    onClick={() => {
                      onExportMarkdown();
                      setShowExportMenu(false);
                    }}
                    className="w-full px-4 py-3 text-left text-sm hover:bg-secondary transition-colors flex items-center gap-2"
                    role="menuitem"
                  >
                    <svg className="w-4 h-4 text-muted" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                    <span className="text-main">{t('header.exportAsMarkdown')}</span>
                  </button>
                )}
              </div>
            )}
          </div>
        )}

        {/* Settings button */}
        {onOpenSettings && (
          <button
            onClick={onOpenSettings}
            className="p-2 rounded-lg hover:bg-tertiary transition-colors"
            title={t('header.settings')}
            aria-label={t('header.openSettings')}
          >
            <svg
              className="w-5 h-5 text-secondary"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"
              />
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
              />
            </svg>
          </button>
        )}

        {/* Theme toggle */}
        <ThemeToggle />

        {/* User menu */}
        <UserMenu userContext={userContext} />
      </div>
    </header>
  );
}

interface ModelSelectorProps {
  connectionStatus: ConnectionStatus;
  onReconnect?: () => void;
}

function ModelSelector({ connectionStatus, onReconnect }: ModelSelectorProps) {
  const { t } = useTranslation();
  const [isOpen, setIsOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);

  // Apply focus trap when dropdown is open
  useFocusTrap(isOpen, dropdownRef);

  // Handle click outside and escape key
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && isOpen) {
        setIsOpen(false);
        triggerRef.current?.focus();
      }
    };

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
      document.addEventListener('keydown', handleEscape);
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleEscape);
    };
  }, [isOpen]);

  const getStatusIndicator = () => {
    switch (connectionStatus) {
      case ConnectionStatus.CONNECTED:
        return <span className="w-2 h-2 rounded-full bg-success" />;
      case ConnectionStatus.CONNECTING:
        return <span className="w-2 h-2 rounded-full bg-warning animate-pulse" />;
      case ConnectionStatus.DISCONNECTED:
      case ConnectionStatus.ERROR:
        return <span className="w-2 h-2 rounded-full bg-error" />;
      default:
        return <span className="w-2 h-2 rounded-full bg-muted" />;
    }
  };

  const getStatusText = () => {
    switch (connectionStatus) {
      case ConnectionStatus.CONNECTED:
        return t('header.connected');
      case ConnectionStatus.CONNECTING:
        return t('header.connecting');
      case ConnectionStatus.DISCONNECTED:
        return t('header.disconnected');
      case ConnectionStatus.ERROR:
        return t('header.connectionError');
      default:
        return '';
    }
  };

  return (
    <div className="relative" ref={menuRef}>
      <button
        ref={triggerRef}
        onClick={() => setIsOpen(!isOpen)}
        className="model-selector focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2"
        aria-expanded={isOpen}
        aria-haspopup="true"
        aria-controls="model-selector-dropdown"
      >
        <span className="font-semibold">{appConfig.appTitle}</span>
        {getStatusIndicator()}
        <svg
          className={`w-4 h-4 text-muted transition-transform ${isOpen ? 'rotate-180' : ''}`}
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
          aria-hidden="true"
        >
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
        </svg>
      </button>

      {/* Dropdown */}
      {isOpen && (
        <div
          ref={dropdownRef}
          id="model-selector-dropdown"
          role="menu"
          aria-label={t('header.modelInfo')}
          className="absolute top-full left-1/2 -translate-x-1/2 mt-2 w-64 bg-elevated border border-main rounded-lg shadow-lg z-50 overflow-hidden animate-fade-in">
          {/* Model info */}
          <div className="p-4 border-b border-main">
            <div className="flex items-center gap-3 mb-2">
              <div
                className="w-10 h-10 rounded-lg flex items-center justify-center"
                style={{ background: 'linear-gradient(135deg, var(--color-accent) 0%, var(--bmc-orange-gradient-end) 100%)' }}
              >
                <svg className="w-6 h-6 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09z" />
                </svg>
              </div>
              <div>
                <div className="font-semibold text-main">{appConfig.appName}</div>
                <div className="text-xs text-muted">{getModelDisplayString()}</div>
              </div>
            </div>
            <p className="text-xs text-secondary">
              {appConfig.model.description}
            </p>
          </div>

          {/* Connection status */}
          <div className="p-3 bg-secondary">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                {getStatusIndicator()}
                <span className="text-sm text-main">
                  {getStatusText()}
                </span>
              </div>
              {(connectionStatus === ConnectionStatus.DISCONNECTED ||
                connectionStatus === ConnectionStatus.ERROR) &&
                onReconnect && (
                  <button
                    onClick={() => {
                      onReconnect();
                      setIsOpen(false);
                    }}
                    className="text-xs font-medium hover:underline"
                    style={{ color: 'var(--color-accent)' }}
                  >
                    {t('header.reconnect')}
                  </button>
                )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

interface UserMenuProps {
  userContext: UserContext;
}

function UserMenu({ userContext }: UserMenuProps) {
  const { t } = useTranslation();
  const [isOpen, setIsOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);

  // Apply focus trap when dropdown is open
  useFocusTrap(isOpen, dropdownRef);

  // Handle click outside and escape key
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && isOpen) {
        setIsOpen(false);
        triggerRef.current?.focus();
      }
    };

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
      document.addEventListener('keydown', handleEscape);
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleEscape);
    };
  }, [isOpen]);

  const initial = userContext.displayName?.[0] || userContext.userId[0].toUpperCase();

  return (
    <div className="relative" ref={menuRef}>
      <button
        ref={triggerRef}
        onClick={() => setIsOpen(!isOpen)}
        className="p-1 rounded-lg hover:bg-tertiary transition-colors focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2"
        aria-expanded={isOpen}
        aria-haspopup="true"
        aria-label={t('header.userMenu')}
        aria-controls="user-menu-dropdown"
      >
        <div
          className="w-8 h-8 rounded-sm flex items-center justify-center text-white font-semibold text-sm"
          style={{ backgroundColor: 'var(--color-accent)' }}
        >
          {initial}
        </div>
      </button>

      {/* Dropdown */}
      {isOpen && (
        <div
          ref={dropdownRef}
          id="user-menu-dropdown"
          role="menu"
          aria-label={t('header.userMenu')}
          className="absolute right-0 top-full mt-2 w-64 bg-elevated border border-main rounded-lg shadow-lg z-50 overflow-hidden animate-fade-in">
          {/* User info */}
          <div className="p-4 border-b border-main">
            <div className="font-medium text-main">
              {userContext.displayName || userContext.userId}
            </div>
            <div className="text-sm text-secondary">{userContext.userId}</div>
          </div>

          {/* Groups */}
          <div className="p-4">
            <div className="text-xs font-medium text-muted uppercase tracking-wider mb-2">
              {t('header.groups')}
            </div>
            <div className="flex flex-wrap gap-1.5">
              {userContext.userGroups.map((group, index) => (
                <span
                  key={index}
                  className="inline-flex items-center px-2.5 py-1 text-xs rounded-full font-medium"
                  style={{
                    backgroundColor: 'var(--color-accent-light)',
                    color: 'var(--color-accent)',
                  }}
                >
                  {group}
                </span>
              ))}
            </div>
          </div>

          {/* Footer */}
          <div className="px-4 py-3 bg-secondary border-t border-main">
            <div className="text-xs text-muted">{t('header.developmentMode')}</div>
          </div>
        </div>
      )}
    </div>
  );
}
