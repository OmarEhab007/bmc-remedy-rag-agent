import type { Citation } from '../types/chat';
import { getSourceTypeStyle } from '../utils/citationColors';

interface CitationBlockProps {
  citations: Citation[];
  midTierUrl?: string;
  maxDisplay?: number;
}

/**
 * Displays source citations as color-coded badges.
 * Each badge shows the source type, ID, and relevance score.
 */
export function CitationBlock({
  citations,
  midTierUrl = 'http://remedy.example.com/arsys',
  maxDisplay = 5,
}: CitationBlockProps) {
  if (!citations || citations.length === 0) {
    return null;
  }

  const displayedCitations = citations.slice(0, maxDisplay);
  const remainingCount = citations.length - maxDisplay;

  const buildUrl = (citation: Citation): string => {
    const form = getFormName(citation.sourceType);
    const encodedForm = encodeURIComponent(form);
    return `${midTierUrl}/forms/${encodedForm}/Default+Administrator+View/?eid=${citation.sourceId}`;
  };

  const getFormName = (sourceType: string): string => {
    switch (sourceType.toUpperCase()) {
      case 'INCIDENT':
        return 'HPD:Help Desk';
      case 'CHANGE':
      case 'CHANGEREQUEST':
        return 'CHG:Infrastructure Change';
      case 'WORKORDER':
        return 'WOI:WorkOrder';
      case 'KNOWLEDGE':
      case 'KNOWLEDGEARTICLE':
        return 'RKM:KnowledgeArticleManager';
      case 'PROBLEM':
        return 'PBM:Problem Investigation';
      default:
        return 'HPD:Help Desk';
    }
  };

  return (
    <div className="sources-container">
      <div className="text-xs font-medium text-gray-500 mb-2">
        Sources
      </div>
      <div className="flex flex-wrap gap-2">
        {displayedCitations.map((citation, index) => {
          const style = getSourceTypeStyle(citation.sourceType);
          return (
            <a
              key={`${citation.sourceId}-${index}`}
              href={buildUrl(citation)}
              target="_blank"
              rel="noopener noreferrer"
              className={`
                source-badge
                inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full
                ${style.bg} ${style.text}
                text-sm font-medium
                hover:opacity-80 transition-all duration-200
                cursor-pointer border ${style.border}
              `}
              title={citation.title || `${style.label} ${citation.sourceId}`}
            >
              <span className="text-sm">{style.icon}</span>
              <span>{style.label} {citation.sourceId}</span>
              {citation.score !== undefined && (
                <span className="opacity-70 text-xs">
                  ({Math.round(citation.score * 100)}%)
                </span>
              )}
            </a>
          );
        })}
        {remainingCount > 0 && (
          <button
            className="
              inline-flex items-center px-3 py-1.5 rounded-full
              bg-gray-100 text-gray-600 text-sm font-medium
              hover:bg-gray-200 transition-colors cursor-pointer
            "
            onClick={() => {
              // Could expand to show all citations
              console.log('Show more citations');
            }}
          >
            +{remainingCount} more
          </button>
        )}
      </div>
    </div>
  );
}

interface InlineCitationProps {
  recordId: string;
  recordType?: string;
  midTierUrl?: string;
}

/**
 * Inline citation link for use within message text.
 */
export function InlineCitation({
  recordId,
  recordType = 'INCIDENT',
  midTierUrl = 'http://remedy.example.com/arsys',
}: InlineCitationProps) {
  const style = getSourceTypeStyle(recordType);

  const getFormName = (type: string): string => {
    switch (type.toUpperCase()) {
      case 'INCIDENT':
        return 'HPD:Help Desk';
      case 'CHANGE':
      case 'CHANGEREQUEST':
        return 'CHG:Infrastructure Change';
      case 'WORKORDER':
        return 'WOI:WorkOrder';
      case 'KNOWLEDGE':
      case 'KNOWLEDGEARTICLE':
        return 'RKM:KnowledgeArticleManager';
      default:
        return 'HPD:Help Desk';
    }
  };

  const form = getFormName(recordType);
  const encodedForm = encodeURIComponent(form);
  const url = `${midTierUrl}/forms/${encodedForm}/Default+Administrator+View/?eid=${recordId}`;

  return (
    <a
      href={url}
      target="_blank"
      rel="noopener noreferrer"
      className={`
        inline-flex items-center gap-1 px-2 py-0.5 rounded
        ${style.bg} ${style.text}
        text-xs font-medium
        hover:opacity-80 transition-opacity
      `}
      onClick={(e) => e.stopPropagation()}
    >
      <span>{style.icon}</span>
      <span>{recordId}</span>
    </a>
  );
}
