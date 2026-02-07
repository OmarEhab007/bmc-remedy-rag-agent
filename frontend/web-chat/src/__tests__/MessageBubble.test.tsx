import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MessageBubble } from '../components/MessageBubble'
import type { ChatMessage } from '../types/chat'

// Mock react-i18next
vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string, fallback?: string) => fallback || key,
  }),
}))

// Mock child components
vi.mock('../components/CitationBlock', () => ({
  InlineCitation: ({ recordId }: { recordId: string }) => (
    <span data-testid="inline-citation">{recordId}</span>
  ),
}))

vi.mock('../components/Avatar', () => ({
  Avatar: ({ type }: { type: string }) => (
    <div data-testid={`avatar-${type}`}>{type}</div>
  ),
}))

vi.mock('../components/MessageActions', () => ({
  MessageActions: () => <div data-testid="message-actions">Actions</div>,
}))

vi.mock('../components/MessageFooter', () => ({
  MessageFooter: () => <div data-testid="message-footer">Footer</div>,
}))

vi.mock('../components/CodeBlock', () => ({
  CodeBlock: ({ code }: { code: string }) => (
    <pre data-testid="code-block">{code}</pre>
  ),
}))

vi.mock('../components/ConfirmationPrompt', () => ({
  ConfirmationPrompt: () => <div data-testid="confirmation-prompt">Confirm Action</div>,
}))

describe('MessageBubble', () => {
  const createUserMessage = (content = 'Hello, world!'): ChatMessage => ({
    id: 'msg-1',
    role: 'user',
    content,
    timestamp: new Date('2024-01-01T12:00:00Z'),
  })

  const createAssistantMessage = (content = 'Hello, how can I help?'): ChatMessage => ({
    id: 'msg-2',
    role: 'assistant',
    content,
    timestamp: new Date('2024-01-01T12:00:01Z'),
  })

  it('renders user message with correct role', () => {
    const message = createUserMessage()
    render(<MessageBubble message={message} userName="John Doe" />)

    expect(screen.getByTestId('avatar-user')).toBeInTheDocument()
    expect(screen.getByText('Hello, world!')).toBeInTheDocument()
  })

  it('renders assistant message with correct role', () => {
    const message = createAssistantMessage()
    render(<MessageBubble message={message} />)

    expect(screen.getByTestId('avatar-assistant')).toBeInTheDocument()
    expect(screen.getByText('Hello, how can I help?')).toBeInTheDocument()
  })

  it('displays message content text', () => {
    const message = createUserMessage('Test message content')
    render(<MessageBubble message={message} />)

    expect(screen.getByText('Test message content')).toBeInTheDocument()
  })

  it('shows streaming cursor when message is streaming', () => {
    const message = { ...createAssistantMessage(), isStreaming: true }
    const { container } = render(<MessageBubble message={message} />)

    const cursor = container.querySelector('.streaming-cursor')
    expect(cursor).toBeInTheDocument()
  })

  it('displays error message when present', () => {
    const message = {
      ...createAssistantMessage(),
      error: 'An error occurred',
    }
    render(<MessageBubble message={message} />)

    expect(screen.getByText('An error occurred')).toBeInTheDocument()
  })

  it('shows message actions for user messages', () => {
    const message = createUserMessage()
    render(<MessageBubble message={message} onEdit={vi.fn()} onDelete={vi.fn()} />)

    expect(screen.getByTestId('message-actions')).toBeInTheDocument()
  })

  it('shows message footer for non-streaming assistant messages', () => {
    const message = createAssistantMessage()
    render(<MessageBubble message={message} />)

    expect(screen.getByTestId('message-footer')).toBeInTheDocument()
  })

  it.todo('enters edit mode when edit is triggered')

  it('shows confirmation prompt when pendingAction is present', () => {
    const message = {
      ...createAssistantMessage(),
      pendingAction: {
        actionId: 'action-123',
        actionType: 'INCIDENT_CREATE' as const,
        preview: 'Create incident...',
        expiresAt: new Date(Date.now() + 300000).toISOString(),
      },
    }

    render(
      <MessageBubble
        message={message}
        sessionId="session-1"
        onActionConfirmed={vi.fn()}
        onActionCancelled={vi.fn()}
      />
    )

    expect(screen.getByTestId('confirmation-prompt')).toBeInTheDocument()
  })
})
