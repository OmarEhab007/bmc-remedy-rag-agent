import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ConfirmationPrompt } from '../components/ConfirmationPrompt'
import type { PendingActionInfo } from '../types/chat'
import * as actionsApi from '../services/actionsApi'

// Mock react-i18next
vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string, fallback?: string) => fallback || key,
  }),
}))

// Mock actions API
vi.mock('../services/actionsApi', () => ({
  confirmAction: vi.fn(),
  cancelAction: vi.fn(),
}))

describe('ConfirmationPrompt', () => {
  const mockAction: PendingActionInfo = {
    actionId: 'action-123',
    actionType: 'INCIDENT_CREATE',
    preview: 'Summary: Network connectivity issue\nImpact: 3\nUrgency: 2',
    expiresAt: new Date(Date.now() + 300000).toISOString(), // 5 minutes from now
  }

  const defaultProps = {
    action: mockAction,
    sessionId: 'session-1',
    onConfirmed: vi.fn(),
    onCancelled: vi.fn(),
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders confirm and cancel buttons', () => {
    render(<ConfirmationPrompt {...defaultProps} />)

    expect(screen.getByText(/common.confirm/)).toBeInTheDocument()
    expect(screen.getByText(/common.cancel/)).toBeInTheDocument()
  })

  it('displays action information', () => {
    render(<ConfirmationPrompt {...defaultProps} />)

    expect(screen.getByText(/Confirmation Required/)).toBeInTheDocument()
    expect(screen.getByText(/action-123/)).toBeInTheDocument()
  })

  it('shows action type label', () => {
    render(<ConfirmationPrompt {...defaultProps} />)

    expect(screen.getByText(/Create Incident/)).toBeInTheDocument()
  })

  it('displays preview when available', () => {
    render(<ConfirmationPrompt {...defaultProps} />)

    expect(screen.getByText(/Network connectivity issue/)).toBeInTheDocument()
  })

  it('shows countdown timer', () => {
    render(<ConfirmationPrompt {...defaultProps} />)

    expect(screen.getByText(/Expires in/)).toBeInTheDocument()
    // Timer format: MM:SS
    expect(screen.getByText(/\d:\d{2}/)).toBeInTheDocument()
  })

  it('calls confirmAction when confirm button is clicked', async () => {
    const user = userEvent.setup()
    const mockConfirmResult = {
      success: true,
      message: 'Incident created successfully',
      createdRecordNumber: 'INC000001',
    }

    vi.mocked(actionsApi.confirmAction).mockResolvedValue(mockConfirmResult)

    render(<ConfirmationPrompt {...defaultProps} />)

    const confirmButton = screen.getByText(/common.confirm/)
    await user.click(confirmButton)

    await waitFor(() => {
      expect(actionsApi.confirmAction).toHaveBeenCalledWith('session-1', 'action-123')
      expect(defaultProps.onConfirmed).toHaveBeenCalledWith({
        success: true,
        message: 'Incident created successfully',
        recordId: 'INC000001',
      })
    })
  })

  it('calls cancelAction when cancel button is clicked', async () => {
    const user = userEvent.setup()
    vi.mocked(actionsApi.cancelAction).mockResolvedValue({ success: true, message: 'Cancelled' })

    render(<ConfirmationPrompt {...defaultProps} />)

    const cancelButton = screen.getByText(/common.cancel/)
    await user.click(cancelButton)

    await waitFor(() => {
      expect(actionsApi.cancelAction).toHaveBeenCalledWith('session-1', 'action-123')
      expect(defaultProps.onCancelled).toHaveBeenCalled()
    })
  })

  it('shows loading state during confirmation', async () => {
    const user = userEvent.setup()
    let resolveConfirm: (value: any) => void
    const confirmPromise = new Promise((resolve) => {
      resolveConfirm = resolve
    })
    vi.mocked(actionsApi.confirmAction).mockReturnValue(confirmPromise as any)

    render(<ConfirmationPrompt {...defaultProps} />)

    const confirmButton = screen.getByText(/common.confirm/)
    await user.click(confirmButton)

    expect(screen.getByText(/Confirming.../)).toBeInTheDocument()

    // Resolve the promise
    resolveConfirm!({ success: true, message: 'Done' })
  })

  it('displays error message when confirmation fails', async () => {
    const user = userEvent.setup()
    vi.mocked(actionsApi.confirmAction).mockRejectedValue(new Error('Network error'))

    render(<ConfirmationPrompt {...defaultProps} />)

    const confirmButton = screen.getByText(/common.confirm/)
    await user.click(confirmButton)

    await waitFor(() => {
      expect(screen.getByText('Network error')).toBeInTheDocument()
    })
  })

  it('disables buttons when expired', () => {
    const expiredAction: PendingActionInfo = {
      ...mockAction,
      expiresAt: new Date(Date.now() - 1000).toISOString(), // Expired 1 second ago
    }

    render(<ConfirmationPrompt {...defaultProps} action={expiredAction} />)

    const confirmButton = screen.getByText(/common.confirm/)
    expect(confirmButton).toBeDisabled()
  })

  it('shows expired message when action has expired', async () => {
    const expiredAction: PendingActionInfo = {
      ...mockAction,
      expiresAt: new Date(Date.now() - 1000).toISOString(),
    }

    render(<ConfirmationPrompt {...defaultProps} action={expiredAction} />)

    await waitFor(() => {
      expect(screen.getByText(/This action has expired/)).toBeInTheDocument()
    })
  })

  it('updates timer every second', () => {
    vi.useFakeTimers()

    const futureTime = Date.now() + 125000 // 2 minutes 5 seconds from now
    const actionWithTimer: PendingActionInfo = {
      ...mockAction,
      expiresAt: new Date(futureTime).toISOString(),
    }

    render(<ConfirmationPrompt {...defaultProps} action={actionWithTimer} />)

    // Initial time should be around 2:05
    expect(screen.getByText(/2:0\d/)).toBeInTheDocument()

    vi.useRealTimers()
  })

  it('handles work order creation action type', () => {
    const workOrderAction: PendingActionInfo = {
      ...mockAction,
      actionType: 'WORK_ORDER_CREATE',
    }

    render(<ConfirmationPrompt {...defaultProps} action={workOrderAction} />)

    expect(screen.getByText(/Create Work Order/)).toBeInTheDocument()
  })
})
