import { useState, useMemo } from 'react';
import type { ChatSession } from '../types/chat';
import { ConfirmModal } from './Modal';

interface ChatSidebarProps {
  sessions: ChatSession[];
  activeSessionId: string | null;
  onSelectSession: (sessionId: string) => void;
  onDeleteSession: (sessionId: string) => void;
  onNewSession: () => void;
  isOpen: boolean;
  onClose: () => void;
}

// Highlight matching text in a string
function highlightMatch(text: string, query: string): React.ReactNode {
  if (!query.trim()) return text;

  const regex = new RegExp(`(${query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi');
  const parts = text.split(regex);

  return parts.map((part, index) =>
    regex.test(part) ? (
      <mark key={index} className="bg-accent/30 text-inherit rounded px-0.5">
        {part}
      </mark>
    ) : (
      part
    )
  );
}

export function ChatSidebar({
  sessions,
  activeSessionId,
  onSelectSession,
  onDeleteSession,
  onNewSession,
  isOpen,
  onClose,
}: ChatSidebarProps) {
  const [searchQuery, setSearchQuery] = useState('');

  // Filter sessions based on search query
  const filteredSessions = useMemo(() => {
    if (!searchQuery.trim()) return sessions;

    const query = searchQuery.toLowerCase();
    return sessions.filter((session) => {
      // Search in title
      if (session.title.toLowerCase().includes(query)) return true;

      // Search in message content
      return session.messages.some(
        (msg) =>
          msg.content.toLowerCase().includes(query) ||
          (msg.citations?.some((c) => c.title.toLowerCase().includes(query)))
      );
    });
  }, [sessions, searchQuery]);

  const handleClearSearch = () => {
    setSearchQuery('');
  };

  return (
    <>
      {/* Mobile overlay */}
      {isOpen && (
        <div
          className="fixed inset-0 bg-black/50 z-40 md:hidden"
          onClick={onClose}
          aria-hidden="true"
        />
      )}

      {/* Sidebar */}
      <aside
        className={`sidebar fixed md:relative inset-y-0 left-0 z-50
                    w-64 transform transition-transform duration-300 ease-in-out
                    md:transform-none
                    ${isOpen ? 'translate-x-0' : '-translate-x-full md:translate-x-0'}`}
      >
        <div className="flex flex-col h-full">
          {/* Sidebar header with new chat button */}
          <div className="p-3">
            <button
              onClick={onNewSession}
              className="sidebar-new-chat group"
            >
              <svg
                className="w-4 h-4"
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
              <span className="font-medium">New chat</span>
            </button>
          </div>

          {/* Search input */}
          <div className="px-3 pb-2">
            <div className="relative">
              <svg
                className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-sidebar-muted"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
                />
              </svg>
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search conversations..."
                className="w-full pl-9 pr-8 py-2 text-sm bg-sidebar-hover border border-transparent
                         rounded-lg text-sidebar placeholder:text-sidebar-muted
                         focus:outline-none focus:ring-2 focus:ring-accent focus:border-transparent
                         transition-all"
                aria-label="Search conversations"
              />
              {searchQuery && (
                <button
                  onClick={handleClearSearch}
                  className="absolute right-2 top-1/2 -translate-y-1/2 p-1 rounded
                           text-sidebar-muted hover:text-sidebar transition-colors"
                  aria-label="Clear search"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              )}
            </div>
          </div>

          {/* Session list */}
          <div className="flex-1 overflow-y-auto sidebar-scroll px-2">
            {sessions.length === 0 ? (
              <div className="p-4 text-center text-sidebar-muted text-sm">
                No conversations yet
              </div>
            ) : filteredSessions.length === 0 ? (
              <div className="p-4 text-center text-sidebar-muted text-sm">
                <p>No results for "{searchQuery}"</p>
                <button
                  onClick={handleClearSearch}
                  className="mt-2 text-accent hover:underline text-sm"
                >
                  Clear search
                </button>
              </div>
            ) : (
              <nav className="space-y-1">
                {filteredSessions.map((session) => (
                  <SessionItem
                    key={session.id}
                    session={session}
                    isActive={session.id === activeSessionId}
                    onSelect={() => {
                      onSelectSession(session.id);
                      onClose();
                    }}
                    onDelete={() => onDeleteSession(session.id)}
                    searchQuery={searchQuery}
                  />
                ))}
              </nav>
            )}
          </div>

          {/* Sidebar footer */}
          <div className="p-3 border-t border-sidebar">
            <div className="flex items-center gap-3 p-2 rounded-lg hover:bg-sidebar-hover cursor-pointer transition-colors">
              {/* BMC Logo */}
              <div
                className="w-8 h-8 rounded-sm flex items-center justify-center"
                style={{ backgroundColor: 'var(--color-accent)' }}
              >
                <svg
                  className="w-5 h-5 text-white"
                  viewBox="0 0 24 24"
                  fill="currentColor"
                >
                  <path d="M7 4h6.5a4.5 4.5 0 013.18 7.68l3.82 7.32h-3.5l-3.24-6.5H10V19H7V4zm3 3v4.5h3.5a1.5 1.5 0 000-3H10V7z" />
                </svg>
              </div>
              <div className="flex-1 min-w-0">
                <div className="text-sm font-medium text-sidebar truncate">
                  Damee GPT
                </div>
                <div className="text-xs text-sidebar-muted">
                  v1.0.0
                </div>
              </div>
            </div>
          </div>
        </div>
      </aside>
    </>
  );
}

interface SessionItemProps {
  session: ChatSession;
  isActive: boolean;
  onSelect: () => void;
  onDelete: () => void;
  searchQuery?: string;
}

function SessionItem({ session, isActive, onSelect, onDelete, searchQuery = '' }: SessionItemProps) {
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  const handleDeleteClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    setShowDeleteConfirm(true);
  };

  const handleConfirmDelete = () => {
    onDelete();
    setShowDeleteConfirm(false);
  };

  const formattedDate = new Date(session.updatedAt).toLocaleDateString([], {
    month: 'short',
    day: 'numeric',
  });

  return (
    <>
      <button
        onClick={onSelect}
        className={`sidebar-item w-full text-left group relative
                    ${isActive ? 'sidebar-item-active' : ''}`}
      >
        {/* Chat icon */}
        <svg
          className="w-4 h-4 flex-shrink-0 text-sidebar-muted"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"
          />
        </svg>

        {/* Title and metadata */}
        <div className="flex-1 min-w-0 pr-6">
          <div className="text-sm truncate text-sidebar">
            {highlightMatch(session.title, searchQuery)}
          </div>
          <div className="text-xs text-sidebar-muted flex items-center gap-1">
            <span>{formattedDate}</span>
            <span>·</span>
            <span>{session.messages.length} msgs</span>
          </div>
        </div>

        {/* Delete button - shown on hover */}
        <button
          onClick={handleDeleteClick}
          className="absolute right-2 top-1/2 -translate-y-1/2 p-1.5 rounded
                     opacity-0 group-hover:opacity-100
                     hover:bg-sidebar-hover text-sidebar-muted hover:text-error
                     focus:opacity-100 focus:outline-none
                     transition-all duration-150"
          aria-label="Delete conversation"
        >
          <svg
            className="w-4 h-4"
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
      </button>

      {/* Delete Confirmation Modal */}
      <ConfirmModal
        isOpen={showDeleteConfirm}
        onClose={() => setShowDeleteConfirm(false)}
        onConfirm={handleConfirmDelete}
        title="Delete Conversation"
        message={`Are you sure you want to delete "${session.title}"? This action cannot be undone.`}
        confirmText="Delete"
        cancelText="Cancel"
        variant="danger"
      />
    </>
  );
}
