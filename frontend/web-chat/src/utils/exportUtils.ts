import type { ChatSession } from '../types/chat';

/**
 * Export conversation to JSON format
 */
export function exportToJSON(session: ChatSession): string {
  const exportData = {
    id: session.id,
    title: session.title,
    createdAt: session.createdAt,
    updatedAt: session.updatedAt,
    messages: session.messages.map((msg) => ({
      id: msg.id,
      role: msg.role,
      content: msg.content,
      timestamp: msg.timestamp,
      citations: msg.citations,
      confidenceScore: msg.confidenceScore,
    })),
    exportedAt: new Date().toISOString(),
  };

  return JSON.stringify(exportData, null, 2);
}

/**
 * Export conversation to Markdown format
 */
export function exportToMarkdown(session: ChatSession): string {
  const lines: string[] = [];

  // Header
  lines.push(`# ${session.title}`);
  lines.push('');
  lines.push(`*Exported on ${new Date().toLocaleString()}*`);
  lines.push('');
  lines.push('---');
  lines.push('');

  // Messages
  session.messages.forEach((msg) => {
    const timestamp = new Date(msg.timestamp).toLocaleTimeString([], {
      hour: '2-digit',
      minute: '2-digit',
    });

    if (msg.role === 'user') {
      lines.push(`## User [${timestamp}]`);
    } else {
      lines.push(`## Assistant [${timestamp}]`);
    }
    lines.push('');
    lines.push(msg.content);
    lines.push('');

    // Add citations if present
    if (msg.citations && msg.citations.length > 0) {
      lines.push('**Sources:**');
      msg.citations.forEach((citation, index) => {
        lines.push(`${index + 1}. [${citation.sourceType}] ${citation.sourceId} - ${citation.title || 'N/A'}`);
      });
      lines.push('');
    }

    // Add confidence if present
    if (msg.confidenceScore !== undefined) {
      lines.push(`*Confidence: ${Math.round(msg.confidenceScore * 100)}%*`);
      lines.push('');
    }

    lines.push('---');
    lines.push('');
  });

  return lines.join('\n');
}

/**
 * Download content as file
 */
export function downloadFile(content: string, filename: string, mimeType: string): void {
  const blob = new Blob([content], { type: mimeType });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

/**
 * Export and download conversation as JSON
 */
export function downloadAsJSON(session: ChatSession): void {
  const content = exportToJSON(session);
  const filename = `conversation-${session.id.slice(0, 8)}-${Date.now()}.json`;
  downloadFile(content, filename, 'application/json');
}

/**
 * Export and download conversation as Markdown
 */
export function downloadAsMarkdown(session: ChatSession): void {
  const content = exportToMarkdown(session);
  const filename = `conversation-${session.id.slice(0, 8)}-${Date.now()}.md`;
  downloadFile(content, filename, 'text/markdown');
}
