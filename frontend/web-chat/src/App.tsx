import { useState, useCallback, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { ChatProvider, useChatContext } from './providers/ChatProvider';
import { ThemeProvider } from './providers/ThemeProvider';
import { LanguageProvider } from './providers/LanguageProvider';
import { ToastProvider, useToast } from './providers/ToastProvider';
import { ChatHeader } from './components/ChatHeader';
import { ChatSidebar } from './components/ChatSidebar';
import { ChatMain } from './components/ChatMain';
import { ErrorBoundary } from './components/ErrorBoundary';
import { SettingsPanel } from './components/SettingsPanel';
import { ConfirmModal } from './components/Modal';
import { downloadAsJSON, downloadAsMarkdown } from './utils/exportUtils';
import './i18n';

function ChatApp() {
  const { t } = useTranslation();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [clearChatConfirmOpen, setClearChatConfirmOpen] = useState(false);
  const toast = useToast();
  const {
    state,
    activeSession,
    userContext,
    createNewSession,
    deleteSession,
    setActiveSession,
    clearCurrentSession,
    reconnect,
  } = useChatContext();

  const handleClearChat = useCallback(() => {
    if (!activeSession) return;

    if (activeSession.messages.length === 0) {
      toast.info(t('toast.noMessagesToClear'));
      return;
    }

    setClearChatConfirmOpen(true);
  }, [activeSession, toast, t]);

  const handleConfirmClearChat = useCallback(() => {
    clearCurrentSession();
    setClearChatConfirmOpen(false);
    toast.success(t('toast.conversationCleared'));
  }, [clearCurrentSession, toast, t]);

  const handleExportJSON = useCallback(() => {
    if (!activeSession) {
      toast.warning(t('toast.noConversationToExport'));
      return;
    }
    if (activeSession.messages.length === 0) {
      toast.warning(t('toast.noMessagesToExport'));
      return;
    }
    try {
      downloadAsJSON(activeSession);
      toast.success(t('toast.exportedAsJSON'));
    } catch (error) {
      toast.error(t('toast.exportFailed'));
    }
  }, [activeSession, toast, t]);

  const handleExportMarkdown = useCallback(() => {
    if (!activeSession) {
      toast.warning(t('toast.noConversationToExport'));
      return;
    }
    if (activeSession.messages.length === 0) {
      toast.warning(t('toast.noMessagesToExport'));
      return;
    }
    try {
      downloadAsMarkdown(activeSession);
      toast.success(t('toast.exportedAsMarkdown'));
    } catch (error) {
      toast.error(t('toast.exportFailed'));
    }
  }, [activeSession, toast, t]);

  const handleClearAllData = useCallback(() => {
    // Clear all sessions from localStorage
    localStorage.removeItem('damee-gpt-chat-sessions');
    // Reload the page to reset state
    window.location.reload();
  }, []);

  // Keyboard shortcuts
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // Ctrl/Cmd + N: New conversation
      if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
        e.preventDefault();
        createNewSession();
      }
      // Ctrl/Cmd + B: Toggle sidebar (mobile)
      if ((e.ctrlKey || e.metaKey) && e.key === 'b') {
        e.preventDefault();
        setSidebarOpen(prev => !prev);
      }
      // Escape: Close sidebar
      if (e.key === 'Escape' && sidebarOpen) {
        setSidebarOpen(false);
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [createNewSession, sidebarOpen]);

  return (
    <div className="h-screen flex overflow-hidden bg-main">
      {/* Skip link for keyboard navigation */}
      <a href="#main-content" className="skip-link">
        {t('accessibility.skipToMainContent')}
      </a>

      {/* Sidebar */}
      <ChatSidebar
        sessions={state.sessions}
        activeSessionId={state.activeSessionId}
        onSelectSession={setActiveSession}
        onDeleteSession={deleteSession}
        onNewSession={createNewSession}
        isOpen={sidebarOpen}
        onClose={() => setSidebarOpen(false)}
      />

      {/* Main content */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Header */}
        <ChatHeader
          connectionStatus={state.connectionStatus}
          userContext={userContext}
          onMenuClick={() => setSidebarOpen(true)}
          onNewChat={createNewSession}
          onClearChat={handleClearChat}
          onReconnect={reconnect}
          onExportJSON={handleExportJSON}
          onExportMarkdown={handleExportMarkdown}
          onOpenSettings={() => setSettingsOpen(true)}
        />

        {/* Chat area */}
        <ChatMain />
      </div>

      {/* Settings Panel */}
      <SettingsPanel
        isOpen={settingsOpen}
        onClose={() => setSettingsOpen(false)}
        onClearAllData={handleClearAllData}
      />

      {/* Clear Chat Confirmation */}
      <ConfirmModal
        isOpen={clearChatConfirmOpen}
        onClose={() => setClearChatConfirmOpen(false)}
        onConfirm={handleConfirmClearChat}
        title={t('modals.clearConversation')}
        message={t('modals.clearConversationMessage')}
        confirmText={t('common.clear')}
        cancelText={t('common.cancel')}
        variant="danger"
      />
    </div>
  );
}

function App() {
  return (
    <ErrorBoundary>
      <ThemeProvider>
        <LanguageProvider>
          <ToastProvider>
            <ChatProvider>
              <ChatApp />
            </ChatProvider>
          </ToastProvider>
        </LanguageProvider>
      </ThemeProvider>
    </ErrorBoundary>
  );
}

export default App;
