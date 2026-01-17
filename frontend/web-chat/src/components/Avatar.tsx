interface AvatarProps {
  type: 'user' | 'assistant';
  name?: string;
  size?: 'sm' | 'md' | 'lg';
}

export function Avatar({ type, name, size = 'md' }: AvatarProps) {
  const sizeClasses = {
    sm: 'w-6 h-6 text-xs',
    md: 'w-[30px] h-[30px] text-sm',
    lg: 'w-10 h-10 text-base',
  };

  const sizeClass = sizeClasses[size];

  if (type === 'user') {
    const initial = name ? name[0].toUpperCase() : 'U';

    return (
      <div
        className={`avatar avatar-user ${sizeClass}`}
        title={name || 'You'}
      >
        {initial}
      </div>
    );
  }

  // Assistant avatar - BMC "R" logo style
  return (
    <div
      className={`avatar avatar-assistant ${sizeClass}`}
      title="BMC Remedy Assistant"
    >
      <svg
        className="w-4 h-4 text-white"
        viewBox="0 0 24 24"
        fill="currentColor"
      >
        {/* Stylized "R" for Remedy */}
        <path d="M7 4h6.5a4.5 4.5 0 013.18 7.68l3.82 7.32h-3.5l-3.24-6.5H10V19H7V4zm3 3v4.5h3.5a1.5 1.5 0 000-3H10V7z" />
      </svg>
    </div>
  );
}

// Alternative AI icon version
export function AssistantIcon({ className = '' }: { className?: string }) {
  return (
    <svg
      className={className}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M12 2a2 2 0 012 2c0 .74-.4 1.39-1 1.73V7h1a7 7 0 017 7h1a1 1 0 011 1v3a1 1 0 01-1 1h-1v1a2 2 0 01-2 2H5a2 2 0 01-2-2v-1H2a1 1 0 01-1-1v-3a1 1 0 011-1h1a7 7 0 017-7h1V5.73c-.6-.34-1-.99-1-1.73a2 2 0 012-2z" />
      <circle cx="9" cy="14" r="1.5" fill="currentColor" />
      <circle cx="15" cy="14" r="1.5" fill="currentColor" />
    </svg>
  );
}
