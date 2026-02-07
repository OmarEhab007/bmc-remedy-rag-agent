import { ReactElement } from 'react'
import { render, RenderOptions } from '@testing-library/react'

/**
 * Custom render function that wraps components with common providers.
 * Use this for components that need context providers.
 */
export function renderWithProviders(
  ui: ReactElement,
  options?: Omit<RenderOptions, 'wrapper'>
) {
  // Add providers here as needed
  // For now, just use the default render
  return render(ui, options)
}

/**
 * Mock chat message factory for tests
 */
export function createMockMessage(overrides = {}) {
  return {
    id: 'test-msg-1',
    role: 'user' as const,
    content: 'Test message',
    timestamp: new Date('2024-01-01T12:00:00Z'),
    ...overrides,
  }
}

/**
 * Mock citation factory for tests
 */
export function createMockCitation(overrides = {}) {
  return {
    sourceType: 'INCIDENT',
    sourceId: 'INC000001',
    title: 'Test Incident',
    score: 0.95,
    ...overrides,
  }
}

/**
 * Mock pending action factory for tests
 */
export function createMockPendingAction(overrides = {}) {
  return {
    actionId: 'action-123',
    actionType: 'INCIDENT_CREATE' as const,
    preview: 'Test action preview',
    expiresAt: new Date(Date.now() + 300000).toISOString(),
    ...overrides,
  }
}

/**
 * Mock chat session factory for tests
 */
export function createMockSession(overrides = {}) {
  return {
    id: 'session-1',
    title: 'Test Session',
    createdAt: new Date('2024-01-01T10:00:00Z'),
    updatedAt: new Date('2024-01-01T12:00:00Z'),
    messages: [],
    ...overrides,
  }
}

// Re-export everything from @testing-library/react
export * from '@testing-library/react'
