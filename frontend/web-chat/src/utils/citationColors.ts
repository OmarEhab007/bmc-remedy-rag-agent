/**
 * Color and icon mapping for different ITSM source types.
 * Used by CitationBlock to render color-coded badges.
 */

export interface SourceTypeStyle {
  bg: string;
  text: string;
  border: string;
  icon: string;
  label: string;
}

/**
 * Style mappings for each source type.
 * Colors are Tailwind CSS classes.
 */
export const SOURCE_TYPE_STYLES: Record<string, SourceTypeStyle> = {
  // Knowledge Articles - Green theme (per spec: KB=green)
  KNOWLEDGE: {
    bg: 'bg-green-50',
    text: 'text-green-600',
    border: 'border-green-200',
    icon: '📚',
    label: 'KB',
  },
  KNOWLEDGEARTICLE: {
    bg: 'bg-green-50',
    text: 'text-green-600',
    border: 'border-green-200',
    icon: '📚',
    label: 'KB',
  },
  KB: {
    bg: 'bg-green-50',
    text: 'text-green-600',
    border: 'border-green-200',
    icon: '📚',
    label: 'KB',
  },

  // Incidents - Blue theme (per spec: INC=blue)
  INCIDENT: {
    bg: 'bg-blue-50',
    text: 'text-blue-600',
    border: 'border-blue-200',
    icon: '🎫',
    label: 'INC',
  },
  INC: {
    bg: 'bg-blue-50',
    text: 'text-blue-600',
    border: 'border-blue-200',
    icon: '🎫',
    label: 'INC',
  },

  // Work Orders - Orange theme (per spec: WO=orange)
  WORKORDER: {
    bg: 'bg-orange-50',
    text: 'text-orange-600',
    border: 'border-orange-200',
    icon: '📋',
    label: 'WO',
  },
  WO: {
    bg: 'bg-orange-50',
    text: 'text-orange-600',
    border: 'border-orange-200',
    icon: '📋',
    label: 'WO',
  },

  // Change Requests - Purple theme
  CHANGE: {
    bg: 'bg-purple-50',
    text: 'text-purple-600',
    border: 'border-purple-200',
    icon: '🔄',
    label: 'CHG',
  },
  CHANGEREQUEST: {
    bg: 'bg-purple-50',
    text: 'text-purple-600',
    border: 'border-purple-200',
    icon: '🔄',
    label: 'CHG',
  },
  CHG: {
    bg: 'bg-purple-50',
    text: 'text-purple-600',
    border: 'border-purple-200',
    icon: '🔄',
    label: 'CHG',
  },

  // Problems - Red theme
  PROBLEM: {
    bg: 'bg-red-50',
    text: 'text-red-600',
    border: 'border-red-200',
    icon: '🔍',
    label: 'PBM',
  },
  PBM: {
    bg: 'bg-red-50',
    text: 'text-red-600',
    border: 'border-red-200',
    icon: '🔍',
    label: 'PBM',
  },
};

/**
 * Default style for unknown source types.
 */
const DEFAULT_STYLE: SourceTypeStyle = {
  bg: 'bg-gray-50',
  text: 'text-gray-600',
  border: 'border-gray-200',
  icon: '📄',
  label: 'Document',
};

/**
 * Get the style configuration for a given source type.
 * @param sourceType - The source type string (case-insensitive)
 * @returns SourceTypeStyle object with colors and icon
 */
export function getSourceTypeStyle(sourceType: string): SourceTypeStyle {
  if (!sourceType) {
    return DEFAULT_STYLE;
  }

  // Normalize the key: uppercase, remove spaces
  const key = sourceType.toUpperCase().replace(/\s+/g, '');
  return SOURCE_TYPE_STYLES[key] || DEFAULT_STYLE;
}

/**
 * Get confidence level configuration based on score.
 * @param score - Confidence score from 0.0 to 1.0
 * @returns Configuration with icon, color, and label
 */
export function getConfidenceLevel(score: number): {
  icon: string;
  color: string;
  bgColor: string;
  label: string;
} {
  const percentage = Math.round(score * 100);

  if (percentage >= 80) {
    return {
      icon: '✓',
      color: 'text-green-600',
      bgColor: 'bg-green-50',
      label: 'High confidence',
    };
  } else if (percentage >= 50) {
    return {
      icon: '⚠',
      color: 'text-yellow-600',
      bgColor: 'bg-yellow-50',
      label: 'Moderate confidence',
    };
  } else {
    return {
      icon: '✗',
      color: 'text-red-600',
      bgColor: 'bg-red-50',
      label: 'Low confidence',
    };
  }
}
