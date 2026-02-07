import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { InputArea } from '../components/InputArea'
import { ConnectionStatus } from '../types/chat'

// Mock react-i18next
vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}))

describe('InputArea', () => {
  const defaultProps = {
    onSendMessage: vi.fn(),
    connectionStatus: ConnectionStatus.CONNECTED,
  }

  it('renders textarea element', () => {
    render(<InputArea {...defaultProps} />)

    const textarea = screen.getByRole('textbox')
    expect(textarea).toBeInTheDocument()
  })

  it('handles text input changes', async () => {
    const user = userEvent.setup()
    render(<InputArea {...defaultProps} />)

    const textarea = screen.getByRole('textbox')
    await user.type(textarea, 'Hello')

    expect(textarea).toHaveValue('Hello')
  })

  it('submit button exists and is clickable when there is content', async () => {
    const user = userEvent.setup()
    const onSendMessage = vi.fn()
    render(<InputArea {...defaultProps} onSendMessage={onSendMessage} />)

    const textarea = screen.getByRole('textbox')
    await user.type(textarea, 'Test message')

    const sendButton = screen.getByLabelText('chat.sendMessage')
    await user.click(sendButton)

    expect(onSendMessage).toHaveBeenCalledWith('Test message')
  })

  it('clears input after sending message', async () => {
    const user = userEvent.setup()
    const onSendMessage = vi.fn()
    render(<InputArea {...defaultProps} onSendMessage={onSendMessage} />)

    const textarea = screen.getByRole('textbox')
    await user.type(textarea, 'Test message')

    const sendButton = screen.getByLabelText('chat.sendMessage')
    await user.click(sendButton)

    expect(textarea).toHaveValue('')
  })

  it('sends message on Enter key press', async () => {
    const user = userEvent.setup()
    const onSendMessage = vi.fn()
    render(<InputArea {...defaultProps} onSendMessage={onSendMessage} />)

    const textarea = screen.getByRole('textbox')
    await user.type(textarea, 'Test message{Enter}')

    expect(onSendMessage).toHaveBeenCalledWith('Test message')
  })

  it('does not send message on Shift+Enter', async () => {
    const user = userEvent.setup()
    const onSendMessage = vi.fn()
    render(<InputArea {...defaultProps} onSendMessage={onSendMessage} />)

    const textarea = screen.getByRole('textbox')
    await user.type(textarea, 'Line 1{Shift>}{Enter}{/Shift}Line 2')

    expect(onSendMessage).not.toHaveBeenCalled()
    expect(textarea).toHaveValue('Line 1\nLine 2')
  })

  it('disables input when not connected', () => {
    render(
      <InputArea
        {...defaultProps}
        connectionStatus={ConnectionStatus.DISCONNECTED}
      />
    )

    const textarea = screen.getByRole('textbox')
    expect(textarea).toBeDisabled()
  })

  it('shows stop button when streaming', () => {
    const onStopGeneration = vi.fn()
    render(
      <InputArea
        {...defaultProps}
        isStreaming={true}
        onStopGeneration={onStopGeneration}
      />
    )

    const stopButton = screen.getByLabelText('chat.stopGenerating')
    expect(stopButton).toBeInTheDocument()
  })

  it('shows character count when approaching limit', async () => {
    const user = userEvent.setup()
    render(<InputArea {...defaultProps} maxLength={100} />)

    const textarea = screen.getByRole('textbox')
    // Type more than 80% of max length (80 chars)
    const longText = 'a'.repeat(85)
    await user.type(textarea, longText)

    // Character count should be visible
    const remaining = 100 - 85
    expect(screen.getByText(remaining.toString())).toBeInTheDocument()
  })

  it('uses custom placeholder when provided', () => {
    render(<InputArea {...defaultProps} placeholder="Type your message here" />)

    const textarea = screen.getByPlaceholderText('Type your message here')
    expect(textarea).toBeInTheDocument()
  })

  it('enforces max length constraint', async () => {
    const user = userEvent.setup()
    render(<InputArea {...defaultProps} maxLength={10} />)

    const textarea = screen.getByRole('textbox')
    await user.type(textarea, 'This is a very long message')

    // Should only accept 10 characters
    expect(textarea).toHaveValue('This is a ')
  })

  it('disables send button when input is empty', () => {
    render(<InputArea {...defaultProps} />)

    const sendButton = screen.getByLabelText('chat.sendMessage')
    expect(sendButton).toBeDisabled()
  })

  it('shows connection status when disconnected', () => {
    render(
      <InputArea
        {...defaultProps}
        connectionStatus={ConnectionStatus.DISCONNECTED}
      />
    )

    expect(screen.getByText('chat.connectionLost')).toBeInTheDocument()
  })
})
