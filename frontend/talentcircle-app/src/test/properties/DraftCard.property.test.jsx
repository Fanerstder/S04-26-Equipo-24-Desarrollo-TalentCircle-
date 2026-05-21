// Feature: draft-publication, Property 1: El botón Publicar aparece si y solo si el estado es APPROVED
// Feature: draft-publication, Property 2: El canal destino es visible junto al botón Publicar

/**
 * Property 1: El botón Publicar aparece si y solo si el estado es APPROVED
 *
 * Para cualquier borrador con cualquier estado (PENDING, APPROVED, REJECTED, PUBLISHED),
 * el componente DraftCard debe mostrar el botón "Publicar" exactamente cuando el estado
 * es APPROVED, y ocultarlo en todos los demás estados.
 *
 * Validates: Requirements 1.1, 1.2, 6.4
 *
 * Property 2: El canal destino es visible junto al botón Publicar
 *
 * Para cualquier borrador con estado APPROVED y cualquier canal (LINKEDIN, TWITTER, NEWSLETTER),
 * el componente debe mostrar la etiqueta del canal correcto junto al botón "Publicar" mediante
 * el aria-label del botón.
 *
 * Validates: Requirements 1.3, 1.4
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor, cleanup } from '@testing-library/react'
import * as fc from 'fast-check'

// ─── Mock react-router-dom ────────────────────────────────────────────────────
vi.mock('react-router-dom', () => ({
  useNavigate: () => vi.fn(),
}))

// ─── Mock draftsApi ───────────────────────────────────────────────────────────
vi.mock('../../api/draftsApi', () => ({
  default: {
    list: vi.fn(),
    getDetail: vi.fn(),
    approve: vi.fn(),
    reject: vi.fn(),
    updateContent: vi.fn(),
    publish: vi.fn(),
  },
}))

// ─── Mock useAppStore ─────────────────────────────────────────────────────────
// Drafts.jsx calls: const { drafts, setDrafts } = useAppStore()
// DraftCard calls:  const { openModal, updateDraftStatus, showToast } = useAppStore()
// We need a real reactive store so setDrafts updates drafts correctly.
let storeState = {
  drafts: [],
  setDrafts: (d) => { storeState.drafts = d },
  openModal: vi.fn(),
  updateDraftStatus: vi.fn(),
  showToast: vi.fn(),
}

vi.mock('../../store/useAppStore', () => ({
  useAppStore: vi.fn(() => storeState),
}))

import draftsApi from '../../api/draftsApi'
import { useAppStore } from '../../store/useAppStore'
import Drafts from '../../pages/Drafts/Drafts'

// ─── Channel label mapping (mirrors Drafts.jsx) ───────────────────────────────
const CHANNEL_LABELS = {
  NEWSLETTER: 'Newsletter',
  LINKEDIN: 'LinkedIn',
  TWITTER: 'Twitter',
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

/**
 * Reset the mock store state between iterations so each render starts fresh.
 */
function resetStore() {
  storeState.drafts = []
  storeState.setDrafts = (d) => { storeState.drafts = d }
  storeState.openModal = vi.fn()
  storeState.updateDraftStatus = vi.fn()
  storeState.showToast = vi.fn()
  useAppStore.mockReturnValue(storeState)
}

// ─── Arbitraries ──────────────────────────────────────────────────────────────

/**
 * Generates a minimal draft object with a specific status and any channel.
 */
const draftWithStatusArb = (status) =>
  fc.record({
    id: fc.uuid(),
    channel: fc.constantFrom('NEWSLETTER', 'LINKEDIN', 'TWITTER'),
    status: fc.constant(status),
    createdAt: fc.constant('2025-04-25T10:00:00Z'),
    summary: fc.string({ minLength: 1, maxLength: 80 }),
    aiScore: fc.float({ min: 0, max: 10, noNaN: true }),
  })

/**
 * Generates a draft with any status (for Property 1).
 */
const draftAnyStatusArb = fc.record({
  id: fc.uuid(),
  channel: fc.constantFrom('NEWSLETTER', 'LINKEDIN', 'TWITTER'),
  status: fc.constantFrom('PENDING', 'APPROVED', 'REJECTED', 'PUBLISHED'),
  createdAt: fc.constant('2025-04-25T10:00:00Z'),
  summary: fc.string({ minLength: 1, maxLength: 80 }),
  aiScore: fc.float({ min: 0, max: 10, noNaN: true }),
})

/**
 * Generates a draft with status APPROVED and any channel (for Property 2).
 */
const approvedDraftAnyChannelArb = fc.record({
  id: fc.uuid(),
  channel: fc.constantFrom('NEWSLETTER', 'LINKEDIN', 'TWITTER'),
  status: fc.constant('APPROVED'),
  createdAt: fc.constant('2025-04-25T10:00:00Z'),
  summary: fc.string({ minLength: 1, maxLength: 80 }),
  aiScore: fc.float({ min: 0, max: 10, noNaN: true }),
})

// ─── Setup / Teardown ─────────────────────────────────────────────────────────

beforeEach(() => {
  vi.clearAllMocks()
  resetStore()
})

afterEach(() => {
  cleanup()
})

// ─── Property Tests ───────────────────────────────────────────────────────────

describe('Property 1: El botón Publicar aparece si y solo si el estado es APPROVED', () => {
  it(
    'muestra el botón Publicar exactamente cuando status === APPROVED, lo oculta en cualquier otro estado',
    async () => {
      await fc.assert(
        fc.asyncProperty(draftAnyStatusArb, async (draft) => {
          // Arrange: reset store and mock API to return a single draft
          resetStore()
          vi.clearAllMocks()
          draftsApi.list.mockResolvedValue([draft])

          // Act: render the Drafts page (which renders DraftCard internally)
          const { unmount } = render(<Drafts />)

          // Wait for loading to finish (skeleton cards disappear)
          await waitFor(() => {
            const skeletons = document.querySelectorAll('[class*="skeletonCard"]')
            expect(skeletons.length).toBe(0)
          })

          // Assert: the Publish button (aria-label containing "Publicar") should
          // exist if and only if the draft status is APPROVED
          const publishButtons = screen.queryAllByRole('button', {
            name: /Publicar/i,
          })

          if (draft.status === 'APPROVED') {
            // Must have exactly one Publish button
            expect(publishButtons.length).toBe(1)
          } else {
            // Must have no Publish button for PENDING, REJECTED, PUBLISHED
            expect(publishButtons.length).toBe(0)
          }

          unmount()
        }),
        { numRuns: 100 }
      )
    }
  )

  it(
    'nunca muestra el botón Publicar cuando el estado es PENDING',
    async () => {
      await fc.assert(
        fc.asyncProperty(draftWithStatusArb('PENDING'), async (draft) => {
          resetStore()
          vi.clearAllMocks()
          draftsApi.list.mockResolvedValue([draft])

          const { unmount } = render(<Drafts />)

          await waitFor(() => {
            const skeletons = document.querySelectorAll('[class*="skeletonCard"]')
            expect(skeletons.length).toBe(0)
          })

          const publishButtons = screen.queryAllByRole('button', {
            name: /Publicar/i,
          })
          expect(publishButtons.length).toBe(0)

          unmount()
        }),
        { numRuns: 50 }
      )
    }
  )

  it(
    'nunca muestra el botón Publicar cuando el estado es REJECTED',
    async () => {
      await fc.assert(
        fc.asyncProperty(draftWithStatusArb('REJECTED'), async (draft) => {
          resetStore()
          vi.clearAllMocks()
          draftsApi.list.mockResolvedValue([draft])

          const { unmount } = render(<Drafts />)

          await waitFor(() => {
            const skeletons = document.querySelectorAll('[class*="skeletonCard"]')
            expect(skeletons.length).toBe(0)
          })

          const publishButtons = screen.queryAllByRole('button', {
            name: /Publicar/i,
          })
          expect(publishButtons.length).toBe(0)

          unmount()
        }),
        { numRuns: 50 }
      )
    }
  )

  it(
    'nunca muestra el botón Publicar cuando el estado es PUBLISHED',
    async () => {
      await fc.assert(
        fc.asyncProperty(draftWithStatusArb('PUBLISHED'), async (draft) => {
          resetStore()
          vi.clearAllMocks()
          draftsApi.list.mockResolvedValue([draft])

          const { unmount } = render(<Drafts />)

          await waitFor(() => {
            const skeletons = document.querySelectorAll('[class*="skeletonCard"]')
            expect(skeletons.length).toBe(0)
          })

          const publishButtons = screen.queryAllByRole('button', {
            name: /Publicar/i,
          })
          expect(publishButtons.length).toBe(0)

          unmount()
        }),
        { numRuns: 50 }
      )
    }
  )

  it(
    'siempre muestra el botón Publicar cuando el estado es APPROVED',
    async () => {
      await fc.assert(
        fc.asyncProperty(draftWithStatusArb('APPROVED'), async (draft) => {
          resetStore()
          vi.clearAllMocks()
          draftsApi.list.mockResolvedValue([draft])

          const { unmount } = render(<Drafts />)

          await waitFor(() => {
            const skeletons = document.querySelectorAll('[class*="skeletonCard"]')
            expect(skeletons.length).toBe(0)
          })

          const publishButtons = screen.queryAllByRole('button', {
            name: /Publicar/i,
          })
          expect(publishButtons.length).toBe(1)

          unmount()
        }),
        { numRuns: 50 }
      )
    }
  )
})

describe('Property 2: El canal destino es visible junto al botón Publicar', () => {
  it(
    'el aria-label del botón Publicar contiene la etiqueta correcta del canal destino',
    async () => {
      await fc.assert(
        fc.asyncProperty(approvedDraftAnyChannelArb, async (draft) => {
          // Arrange
          resetStore()
          vi.clearAllMocks()
          draftsApi.list.mockResolvedValue([draft])

          // Act
          const { unmount } = render(<Drafts />)

          // Wait for loading to finish
          await waitFor(() => {
            const skeletons = document.querySelectorAll('[class*="skeletonCard"]')
            expect(skeletons.length).toBe(0)
          })

          // Assert: the Publish button must exist (status is APPROVED)
          const expectedChannelLabel = CHANNEL_LABELS[draft.channel]
          const expectedAriaLabel = `Publicar en ${expectedChannelLabel}`

          // The button aria-label must contain the correct channel label
          const publishButton = screen.queryByRole('button', {
            name: expectedAriaLabel,
          })

          expect(publishButton).not.toBeNull()

          // Additionally verify the aria-label contains the channel label
          expect(publishButton.getAttribute('aria-label')).toContain(expectedChannelLabel)

          unmount()
        }),
        { numRuns: 100 }
      )
    }
  )

  it(
    'el botón Publicar para canal NEWSLETTER tiene aria-label con "Newsletter"',
    async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.record({
            id: fc.uuid(),
            channel: fc.constant('NEWSLETTER'),
            status: fc.constant('APPROVED'),
            createdAt: fc.constant('2025-04-25T10:00:00Z'),
            summary: fc.string({ minLength: 1, maxLength: 80 }),
            aiScore: fc.float({ min: 0, max: 10, noNaN: true }),
          }),
          async (draft) => {
            resetStore()
            vi.clearAllMocks()
            draftsApi.list.mockResolvedValue([draft])

            const { unmount } = render(<Drafts />)

            await waitFor(() => {
              const skeletons = document.querySelectorAll('[class*="skeletonCard"]')
              expect(skeletons.length).toBe(0)
            })

            const publishButton = screen.queryByRole('button', {
              name: 'Publicar en Newsletter',
            })
            expect(publishButton).not.toBeNull()
            expect(publishButton.getAttribute('aria-label')).toBe('Publicar en Newsletter')

            unmount()
          }
        ),
        { numRuns: 50 }
      )
    }
  )

  it(
    'el botón Publicar para canal LINKEDIN tiene aria-label con "LinkedIn"',
    async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.record({
            id: fc.uuid(),
            channel: fc.constant('LINKEDIN'),
            status: fc.constant('APPROVED'),
            createdAt: fc.constant('2025-04-25T10:00:00Z'),
            summary: fc.string({ minLength: 1, maxLength: 80 }),
            aiScore: fc.float({ min: 0, max: 10, noNaN: true }),
          }),
          async (draft) => {
            resetStore()
            vi.clearAllMocks()
            draftsApi.list.mockResolvedValue([draft])

            const { unmount } = render(<Drafts />)

            await waitFor(() => {
              const skeletons = document.querySelectorAll('[class*="skeletonCard"]')
              expect(skeletons.length).toBe(0)
            })

            const publishButton = screen.queryByRole('button', {
              name: 'Publicar en LinkedIn',
            })
            expect(publishButton).not.toBeNull()
            expect(publishButton.getAttribute('aria-label')).toBe('Publicar en LinkedIn')

            unmount()
          }
        ),
        { numRuns: 50 }
      )
    }
  )

  it(
    'el botón Publicar para canal TWITTER tiene aria-label con "Twitter"',
    async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.record({
            id: fc.uuid(),
            channel: fc.constant('TWITTER'),
            status: fc.constant('APPROVED'),
            createdAt: fc.constant('2025-04-25T10:00:00Z'),
            summary: fc.string({ minLength: 1, maxLength: 80 }),
            aiScore: fc.float({ min: 0, max: 10, noNaN: true }),
          }),
          async (draft) => {
            resetStore()
            vi.clearAllMocks()
            draftsApi.list.mockResolvedValue([draft])

            const { unmount } = render(<Drafts />)

            await waitFor(() => {
              const skeletons = document.querySelectorAll('[class*="skeletonCard"]')
              expect(skeletons.length).toBe(0)
            })

            const publishButton = screen.queryByRole('button', {
              name: 'Publicar en Twitter',
            })
            expect(publishButton).not.toBeNull()
            expect(publishButton.getAttribute('aria-label')).toBe('Publicar en Twitter')

            unmount()
          }
        ),
        { numRuns: 50 }
      )
    }
  )
})
