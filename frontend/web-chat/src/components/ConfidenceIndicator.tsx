import { getConfidenceLevel } from '../utils/citationColors';

interface ConfidenceIndicatorProps {
  /** Confidence score from 0.0 to 1.0 */
  score: number;
  /** Show tooltip on hover */
  showTooltip?: boolean;
  /** Additional CSS classes */
  className?: string;
}

/**
 * Displays a confidence score with visual indicator.
 * - ≥80%: Green checkmark (High confidence)
 * - 50-79%: Yellow warning (Moderate confidence)
 * - <50%: Red X (Low confidence)
 */
export function ConfidenceIndicator({
  score,
  showTooltip = true,
  className = '',
}: ConfidenceIndicatorProps) {
  const percentage = Math.round(score * 100);
  const { icon, color, label } = getConfidenceLevel(score);

  return (
    <span
      className={`
        inline-flex items-center gap-1
        ${color}
        font-medium text-sm
        ${className}
      `}
      title={showTooltip ? label : undefined}
      role="status"
      aria-label={`${label}: ${percentage}%`}
    >
      <span className="text-base" aria-hidden="true">{icon}</span>
      <span>{percentage}%</span>
    </span>
  );
}

interface ConfidenceBadgeProps {
  /** Confidence score from 0.0 to 1.0 */
  score: number;
  /** Additional CSS classes */
  className?: string;
}

/**
 * Displays confidence as a pill-shaped badge with background.
 */
export function ConfidenceBadge({
  score,
  className = '',
}: ConfidenceBadgeProps) {
  const percentage = Math.round(score * 100);
  const { icon, color, bgColor, label } = getConfidenceLevel(score);

  return (
    <span
      className={`
        inline-flex items-center gap-1 px-2 py-0.5 rounded-full
        ${bgColor} ${color}
        text-xs font-medium
        ${className}
      `}
      title={label}
      role="status"
      aria-label={`${label}: ${percentage}%`}
    >
      <span aria-hidden="true">{icon}</span>
      <span>{percentage}%</span>
    </span>
  );
}
