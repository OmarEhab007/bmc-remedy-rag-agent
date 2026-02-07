import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { CitationBlock, InlineCitation } from '../components/CitationBlock'
import type { Citation } from '../types/chat'

// Mock citation colors utility
vi.mock('../utils/citationColors', () => ({
  getSourceTypeStyle: (sourceType: string) => ({
    label: sourceType.toUpperCase(),
    icon: '📋',
    bg: 'bg-blue-100',
    text: 'text-blue-800',
    border: 'border-blue-200',
  }),
}))

describe('CitationBlock', () => {
  const mockCitations: Citation[] = [
    {
      sourceType: 'INCIDENT',
      sourceId: 'INC000001',
      title: 'Network connectivity issue',
      score: 0.95,
    },
    {
      sourceType: 'KNOWLEDGE',
      sourceId: 'KB000042',
      title: 'How to reset password',
      score: 0.87,
    },
  ]

  it('renders citation source information', () => {
    render(<CitationBlock citations={mockCitations} />)

    expect(screen.getByText(/INCIDENT INC000001/)).toBeInTheDocument()
    expect(screen.getByText(/KNOWLEDGE KB000042/)).toBeInTheDocument()
  })

  it('displays score/confidence when available', () => {
    render(<CitationBlock citations={mockCitations} />)

    // Scores are displayed as percentages
    expect(screen.getByText(/95%/)).toBeInTheDocument()
    expect(screen.getByText(/87%/)).toBeInTheDocument()
  })

  it('renders nothing when citations array is empty', () => {
    const { container } = render(<CitationBlock citations={[]} />)

    expect(container.firstChild).toBeNull()
  })

  it('renders nothing when citations is undefined', () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const { container } = render(<CitationBlock citations={undefined as any} />)

    expect(container.firstChild).toBeNull()
  })

  it('limits displayed citations to maxDisplay', () => {
    const manyCitations: Citation[] = Array.from({ length: 10 }, (_, i) => ({
      sourceType: 'INCIDENT',
      sourceId: `INC00000${i}`,
      title: `Incident ${i}`,
      score: 0.8,
    }))

    render(<CitationBlock citations={manyCitations} maxDisplay={3} />)

    // Should show 3 citations
    expect(screen.getByText(/INC000000/)).toBeInTheDocument()
    expect(screen.getByText(/INC000001/)).toBeInTheDocument()
    expect(screen.getByText(/INC000002/)).toBeInTheDocument()

    // Should show "+7 more" button
    expect(screen.getByText('+7 more')).toBeInTheDocument()
  })

  it('renders citation as clickable link', () => {
    render(<CitationBlock citations={mockCitations} />)

    const link = screen.getByText(/INCIDENT INC000001/).closest('a')
    expect(link).toHaveAttribute('href')
    expect(link).toHaveAttribute('target', '_blank')
    expect(link).toHaveAttribute('rel', 'noopener noreferrer')
  })

  it('builds correct URL for incident citation', () => {
    const midTierUrl = 'http://remedy.test.com/arsys'
    render(<CitationBlock citations={mockCitations} midTierUrl={midTierUrl} />)

    const link = screen.getByText(/INCIDENT INC000001/).closest('a')
    expect(link).toHaveAttribute(
      'href',
      expect.stringContaining('remedy.test.com/arsys/forms')
    )
  })

  it('uses title attribute for hover text', () => {
    render(<CitationBlock citations={mockCitations} />)

    const link = screen.getByText(/INCIDENT INC000001/).closest('a')
    expect(link).toHaveAttribute('title', 'Network connectivity issue')
  })
})

describe('InlineCitation', () => {
  it('renders inline citation with record ID', () => {
    render(<InlineCitation recordId="INC000123" recordType="INCIDENT" />)

    expect(screen.getByText('INC000123')).toBeInTheDocument()
  })

  it('renders as clickable link', () => {
    render(<InlineCitation recordId="INC000123" recordType="INCIDENT" />)

    const link = screen.getByText('INC000123').closest('a')
    expect(link).toHaveAttribute('href')
    expect(link).toHaveAttribute('target', '_blank')
  })

  it('defaults to INCIDENT type when not specified', () => {
    render(<InlineCitation recordId="INC000123" />)

    // Should render without error
    expect(screen.getByText('INC000123')).toBeInTheDocument()
  })

  it('renders as a clickable link with href', () => {
    const { container } = render(<InlineCitation recordId="INC000123" />)

    const link = container.querySelector('a')
    expect(link).toBeInTheDocument()
    expect(link?.getAttribute('href')).toBeTruthy()
  })

  it('uses custom midTierUrl when provided', () => {
    const customUrl = 'http://custom.remedy.com/arsys'
    render(
      <InlineCitation
        recordId="INC000123"
        recordType="INCIDENT"
        midTierUrl={customUrl}
      />
    )

    const link = screen.getByText('INC000123').closest('a')
    expect(link?.getAttribute('href')).toContain('custom.remedy.com')
  })
})
