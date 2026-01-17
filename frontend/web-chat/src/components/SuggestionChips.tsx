import type { SuggestionChip } from '../types/chat';
import { DEFAULT_SUGGESTIONS } from '../types/chat';

interface SuggestionChipsProps {
  suggestions?: SuggestionChip[];
  onSelect: (query: string) => void;
  disabled?: boolean;
}

export function SuggestionChips({
  suggestions = DEFAULT_SUGGESTIONS,
  onSelect,
  disabled = false,
}: SuggestionChipsProps) {
  return (
    <div className="flex flex-wrap gap-2 justify-center">
      {suggestions.map((suggestion) => (
        <button
          key={suggestion.id}
          onClick={() => onSelect(suggestion.query)}
          disabled={disabled}
          className="px-4 py-2 rounded-full text-sm font-medium transition-all duration-150
                     disabled:opacity-50 disabled:cursor-not-allowed"
          style={{
            backgroundColor: 'var(--color-bg-secondary)',
            border: '1px solid var(--color-border)',
            color: 'var(--color-text)',
          }}
          onMouseEnter={(e) => {
            if (!disabled) {
              e.currentTarget.style.borderColor = 'var(--color-accent)';
              e.currentTarget.style.backgroundColor = 'var(--color-accent-light)';
            }
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.borderColor = 'var(--color-border)';
            e.currentTarget.style.backgroundColor = 'var(--color-bg-secondary)';
          }}
        >
          {suggestion.label}
        </button>
      ))}
    </div>
  );
}
