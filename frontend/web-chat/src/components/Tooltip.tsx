import { useState, useRef, useEffect, type ReactNode } from 'react';

type TooltipPosition = 'top' | 'bottom' | 'left' | 'right';

interface TooltipProps {
  content: string;
  children: ReactNode;
  position?: TooltipPosition;
  delay?: number;
  disabled?: boolean;
}

export function Tooltip({
  content,
  children,
  position = 'top',
  delay = 200,
  disabled = false,
}: TooltipProps) {
  const [isVisible, setIsVisible] = useState(false);
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const triggerRef = useRef<HTMLDivElement>(null);

  const showTooltip = () => {
    if (disabled) return;
    timeoutRef.current = setTimeout(() => {
      setIsVisible(true);
    }, delay);
  };

  const hideTooltip = () => {
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
    }
    setIsVisible(false);
  };

  // Cleanup timeout on unmount
  useEffect(() => {
    return () => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }
    };
  }, []);

  const positionClass = `tooltip-${position}`;

  return (
    <div
      ref={triggerRef}
      className="tooltip-wrapper"
      onMouseEnter={showTooltip}
      onMouseLeave={hideTooltip}
      onFocus={showTooltip}
      onBlur={hideTooltip}
    >
      {children}
      <span
        className={`tooltip ${positionClass} ${isVisible ? 'opacity-100' : 'opacity-0'}`}
        role="tooltip"
        aria-hidden={!isVisible}
      >
        {content}
      </span>
    </div>
  );
}

// Icon button with tooltip - common pattern
interface IconButtonWithTooltipProps {
  icon: ReactNode;
  tooltip: string;
  onClick?: () => void;
  disabled?: boolean;
  className?: string;
  ariaLabel?: string;
  tooltipPosition?: TooltipPosition;
}

export function IconButtonWithTooltip({
  icon,
  tooltip,
  onClick,
  disabled = false,
  className = '',
  ariaLabel,
  tooltipPosition = 'top',
}: IconButtonWithTooltipProps) {
  return (
    <Tooltip content={tooltip} position={tooltipPosition} disabled={disabled}>
      <button
        onClick={onClick}
        disabled={disabled}
        className={`action-button ${className}`}
        aria-label={ariaLabel || tooltip}
      >
        {icon}
      </button>
    </Tooltip>
  );
}

export default Tooltip;
