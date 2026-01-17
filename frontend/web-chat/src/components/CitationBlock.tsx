import type { Citation } from '../types/chat';

interface CitationBlockProps {
  citations: Citation[];
  midTierUrl?: string;
}

const RECORD_TYPE_ICONS: Record<string, string> = {
  INCIDENT: '\u26A0\uFE0F', // Warning sign
  CHANGE: '\u{1F504}', // Counterclockwise arrows
  WORKORDER: '\u{1F4CB}', // Clipboard
  KNOWLEDGE: '\u{1F4D6}', // Open book
  PROBLEM: '\u{1F50D}', // Magnifying glass
};

export function CitationBlock({
  citations,
  midTierUrl = 'http://remedy.example.com/arsys',
}: CitationBlockProps) {
  if (!citations || citations.length === 0) {
    return null;
  }

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
        return 'CHG:Infrastructure Change';
      case 'WORKORDER':
        return 'WOI:WorkOrder';
      case 'KNOWLEDGE':
        return 'RKM:KnowledgeArticleManager';
      case 'PROBLEM':
        return 'PBM:Problem Investigation';
      default:
        return 'HPD:Help Desk';
    }
  };

  return (
    <div className="citation-container">
      <div className="text-xs font-medium text-muted mb-2">
        Sources
      </div>
      <div className="flex flex-wrap gap-2">
        {citations.map((citation, index) => (
          <a
            key={`${citation.sourceId}-${index}`}
            href={buildUrl(citation)}
            target="_blank"
            rel="noopener noreferrer"
            className="citation-badge"
            title={citation.title || citation.sourceId}
          >
            <span className="text-xs">
              {RECORD_TYPE_ICONS[citation.sourceType.toUpperCase()] || '\u{1F4C4}'}
            </span>
            <span>{citation.sourceId}</span>
            {citation.score !== undefined && (
              <span className="opacity-70">
                ({Math.round(citation.score * 100)}%)
              </span>
            )}
          </a>
        ))}
      </div>
    </div>
  );
}

interface InlineCitationProps {
  recordId: string;
  recordType?: string;
  midTierUrl?: string;
}

export function InlineCitation({
  recordId,
  recordType = 'INCIDENT',
  midTierUrl = 'http://remedy.example.com/arsys',
}: InlineCitationProps) {
  const getFormName = (type: string): string => {
    switch (type.toUpperCase()) {
      case 'INCIDENT':
        return 'HPD:Help Desk';
      case 'CHANGE':
        return 'CHG:Infrastructure Change';
      case 'WORKORDER':
        return 'WOI:WorkOrder';
      case 'KNOWLEDGE':
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
      className="citation-badge"
      onClick={(e) => e.stopPropagation()}
    >
      {recordId}
    </a>
  );
}
