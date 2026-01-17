import type { ReactNode } from 'react';

interface SuggestionCardProps {
  icon: ReactNode;
  title: string;
  description: string;
  onClick: () => void;
  disabled?: boolean;
  gradient?: 'blue' | 'green' | 'purple' | 'orange';
}

const gradientColors = {
  blue: 'card-gradient-blue',
  green: 'card-gradient-green',
  purple: 'card-gradient-purple',
  orange: 'card-gradient-orange',
};

export function SuggestionCard({
  icon,
  title,
  description,
  onClick,
  disabled = false,
  gradient = 'orange',
}: SuggestionCardProps) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className={`suggestion-card-new ${gradientColors[gradient]} disabled:opacity-50 disabled:cursor-not-allowed`}
    >
      <div className="suggestion-card-new-icon">{icon}</div>
      <div className="suggestion-card-new-content">
        <span className="suggestion-card-new-title">{title}</span>
        <span className="suggestion-card-new-description">{description}</span>
      </div>
      <svg
        className="suggestion-card-new-arrow"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
      >
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          d="M13.5 4.5L21 12m0 0l-7.5 7.5M21 12H3"
        />
      </svg>
    </button>
  );
}

export default SuggestionCard;
