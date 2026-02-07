import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { ChatMain } from '../components/ChatMain'
import type { ChatState, ChatSession, UserContext } from '../types/chat'
import { ConnectionStatus } from '../types/chat'

// Mock react-i18next
vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}))

// Mock child components
vi.mock('../components/MessageContainer', () => ({
  MessageContainer: () => <div data-testid="message-container">Messages</div>,
}))

vi.mock('../components/InputArea', () => ({
  InputArea: () => <div data-testid="input-area">Input</div>,
}))

// Default mock context - can be overridden per test via mutable variable
const createMockChatContext = (overrides: Record<string, unknown> = {}) => ({
  state: {
    sessions: [],
    activeSessionId: 'session-1',
    connectionStatus: ConnectionStatus.CONNECTED,
    isThinking: false,
    error: null,
  } as ChatState,
  activeSession: {
    id: 'session-1',
    title: 'Test Session',
    createdAt: new Date(),
    updatedAt: new Date(),
    messages: [],
  } as ChatSession,
  userContext: {
    userId: 'test-user',
    userGroups: ['IT Support'],
    displayName: 'Test User',
  } as UserContext,
  sendMessage: vi.fn(),
  clearError: vi.fn(),
  regenerateLastResponse: vi.fn(),
  editAndResend: vi.fn(),
  deleteMessage: vi.fn(),
  createNewSession: vi.fn(),
  deleteSession: vi.fn(),
  setActiveSession: vi.fn(),
  clearCurrentSession: vi.fn(),
  setUserContext: vi.fn(),
  reconnect: vi.fn(),
  ...overrides,
})

let mockChatContext = createMockChatContext()

vi.mock('../providers/ChatProvider', () => ({
  useChatContext: () => mockChatContext,
}))

beforeEach(() => {
  mockChatContext = createMockChatContext()
})

describe('ChatMain', () => {
  it('renders without crashing', () => {
    render(<ChatMain />)

    expect(screen.getByTestId('message-container')).toBeInTheDocument()
    expect(screen.getByTestId('input-area')).toBeInTheDocument()
  })

  it('renders MessageContainer component', () => {
    render(<ChatMain />)

    expect(screen.getByTestId('message-container')).toBeInTheDocument()
  })

  it('renders InputArea component', () => {
    render(<ChatMain />)

    expect(screen.getByTestId('input-area')).toBeInTheDocument()
  })

  it.todo('displays error banner when error exists')

  it('has correct main element with tabIndex', () => {
    const { container } = render(<ChatMain />)

    const main = container.querySelector('main#main-content')
    expect(main).toBeInTheDocument()
    expect(main).toHaveAttribute('tabIndex', '-1')
  })

  it('applies correct layout classes', () => {
    const { container } = render(<ChatMain />)

    const main = container.querySelector('main')
    expect(main).toHaveClass('flex-1', 'flex', 'flex-col', 'min-h-0')
  })
})

describe('ChatMain - Integration scenarios', () => {
  it('handles messages from active session', () => {
    const sessionWithMessages: ChatSession = {
      id: 'session-1',
      title: 'Test Session',
      createdAt: new Date(),
      updatedAt: new Date(),
      messages: [
        {
          id: 'msg-1',
          role: 'user',
          content: 'Hello',
          timestamp: new Date(),
        },
        {
          id: 'msg-2',
          role: 'assistant',
          content: 'Hi there!',
          timestamp: new Date(),
        },
      ],
    }

    mockChatContext = createMockChatContext({
      activeSession: sessionWithMessages,
    })

    render(<ChatMain />)

    // Component should render successfully with messages
    expect(screen.getByTestId('message-container')).toBeInTheDocument()
  })

  it('handles empty session', () => {
    const emptySession: ChatSession = {
      id: 'session-1',
      title: 'Empty Session',
      createdAt: new Date(),
      updatedAt: new Date(),
      messages: [],
    }

    mockChatContext = createMockChatContext({
      activeSession: emptySession,
    })

    render(<ChatMain />)

    expect(screen.getByTestId('message-container')).toBeInTheDocument()
  })

  it('handles null active session', () => {
    mockChatContext = createMockChatContext({
      activeSession: null,
    })

    render(<ChatMain />)

    // Should still render without crashing
    expect(screen.getByTestId('message-container')).toBeInTheDocument()
  })
})
