/**
 * Regular expression pattern to match BMC Remedy incident numbers.
 * Matches formats like: INC000001234, INC0000012345678
 */
export const INCIDENT_PATTERN = /INC[0-9]{9,12}/gi;

/**
 * Pattern for other ITSM record types.
 */
export const RECORD_PATTERNS = {
  incident: /INC[0-9]{9,12}/gi,
  change: /CRQ[0-9]{9,12}/gi,
  workOrder: /WO[0-9]{9,12}/gi,
  knowledge: /KBA[0-9]{9,12}/gi,
  problem: /PBI[0-9]{9,12}/gi,
};

/**
 * Extract all incident numbers from text.
 */
export function extractIncidentNumbers(text: string): string[] {
  const matches = text.match(INCIDENT_PATTERN);
  return matches ? [...new Set(matches.map(m => m.toUpperCase()))] : [];
}

/**
 * Extract all ITSM record references from text.
 */
export function extractAllRecordReferences(text: string): Map<string, string[]> {
  const results = new Map<string, string[]>();

  for (const [type, pattern] of Object.entries(RECORD_PATTERNS)) {
    const matches = text.match(pattern);
    if (matches) {
      results.set(type, [...new Set(matches.map(m => m.toUpperCase()))]);
    }
  }

  return results;
}

/**
 * Check if text contains any incident references.
 */
export function hasIncidentReferences(text: string): boolean {
  return INCIDENT_PATTERN.test(text);
}

/**
 * Build a Remedy Mid-Tier URL for an incident.
 * @param incidentId The incident number (e.g., INC000001234)
 * @param midTierUrl The base URL of the Remedy Mid-Tier server
 */
export function buildIncidentUrl(
  incidentId: string,
  midTierUrl: string = 'http://remedy.example.com/arsys'
): string {
  // Standard Remedy Mid-Tier URL format for incidents
  const form = 'HPD:Help Desk';
  const encodedForm = encodeURIComponent(form);
  return `${midTierUrl}/forms/${encodedForm}/Default+Administrator+View/?eid=${incidentId}`;
}

/**
 * Parse text and return segments with incident references marked.
 */
export interface TextSegment {
  type: 'text' | 'incident' | 'change' | 'workOrder' | 'knowledge' | 'problem';
  content: string;
}

export function parseTextWithReferences(text: string): TextSegment[] {
  const segments: TextSegment[] = [];

  // Combined pattern for all record types
  const combinedPattern = /(?:INC|CRQ|WO|KBA|PBI)[0-9]{9,12}/gi;

  let lastIndex = 0;
  let match: RegExpExecArray | null;

  // Reset lastIndex for global regex
  combinedPattern.lastIndex = 0;

  while ((match = combinedPattern.exec(text)) !== null) {
    // Add text before the match
    if (match.index > lastIndex) {
      segments.push({
        type: 'text',
        content: text.slice(lastIndex, match.index),
      });
    }

    // Determine the type based on prefix
    const prefix = match[0].slice(0, 3).toUpperCase();
    let type: TextSegment['type'];

    switch (prefix) {
      case 'INC':
        type = 'incident';
        break;
      case 'CRQ':
        type = 'change';
        break;
      case 'KBA':
        type = 'knowledge';
        break;
      case 'PBI':
        type = 'problem';
        break;
      default:
        type = prefix === 'WO0' || prefix === 'WO1' ? 'workOrder' : 'text';
    }

    segments.push({
      type,
      content: match[0].toUpperCase(),
    });

    lastIndex = match.index + match[0].length;
  }

  // Add remaining text
  if (lastIndex < text.length) {
    segments.push({
      type: 'text',
      content: text.slice(lastIndex),
    });
  }

  return segments;
}
