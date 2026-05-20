// Feature: draft-publication, Property 12: Feedback visual correcto según resultado de publicación
// Feature: draft-publication, Property 15: El modal muestra fecha y ID externo del post publicado

/**
 * Property 12: Feedback visual correcto según resultado de publicación
 *
 * Para cualquier borrador APPROVED y cualquier respuesta SUCCESS de draftsApi.publish:
 *   - showToast es llamado con '✅' y un mensaje que contiene el nombre del canal
 *   - updateDraftStatus es llamado con 'PUBLISHED'
 *
 * Para cualquier borrador APPROVED y cualquier respuesta FAILED de draftsApi.publish:
 *   - showToast es llamado con '✗' y el errorMessage
 *   - updateDraftStatus NO es llamado con 'PUBLISHED'
 *
 * Validates: Requirements 6.1, 6.2
 *
 * Property 15: El modal muestra fecha y ID externo del post publicado
 *
 * Para cualquier borrador PUBLISHED con cualquier publishedAt y externalPostId,
 * el DraftModal debe mostrar ambos valores visibles en la interfaz.
 *
 * Validates: Requirement 8.2
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor, fireEvent, cleanup, act } from '@testing-library/react'
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
// We maintain a mutable storeState object and reset it between iterations.
let storeState = {
  drafts: [],
  setDrafts: (d) => { storeState.drafts = d },
  openModal: vi.fn(),
  updateDraftStatus: vi.fn(),
  showToast: vi.fn(),
  // DraftModal also needs these:
  modalDraftId: null,
  closeModal: vi.fn(),
  updateDraftContent: vi.fn(),
}

vi.mock('../../store/useAppStore', () => ({
  useAppStore: vi.fn(() => storeState),
}))

import draftsApi from '../../api/draftsApi'
import { useAppStore } from '../../store/useAppStore'
import Drafts from '../../pages/Drafts/Drafts'
import DraftModal from '../../components/DraftModal'

// ─── Channel label mapping ────────────────────────────────────────────────────
const CHANNEL_LABELS = {
  NEWSLETTER: 'Newsletter',
  LINKEDIN: 'LinkedIn',
  TWITTER: 'Twitter',
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

function resetStore(overrides = {}) {
  storeState.drafts = []
  storeState.setDrafts = (d) => { storeState.drafts = d }
  storeState.openModal = vi.fn()
  storeState.updateDraftStatus = vi.fn()
  storeState.showToast = vi.fn()
  storeState.modalDraftId = null
  storeState.closeModal = vi.fn()
  storeState.updateDraftContent = vi.fn()
  Object.assign(storeState, overrides)
  useAppStore.mockReturnValue(storeState)
}

// ─── Arbitraries ──────────────────────────────────────────────────────────────

/**
 * Generates an APPROVED draft with any channel.
 */
const approvedDraftArb = fc.record({
  id: fc.uuid(),
  channel: fc.constantFrom('NEWSLETTER', 'LINKEDIN', 'TWITTER'),
  status: fc.constant('APPROVED'),
  createdAt: fc.constant('2025-04-25T10:00:00Z'),
  summary: fc.string({ minLength: 1, maxLength: 80 }),
  aiScore: fc.float({ min: 0, max: 10, noNaN: true }),
})

/**
 * Generates a SUCCESS PublicationDto.
 * externalPostId uses a UUID to ensure it's unique and won't match other DOM content.
 */
const successPublicationArb = fc.record({
  id: fc.uuid(),
  draftId: fc.uuid(),
  status: fc.constant('SUCCESS'),
  externalPostId: fc.uuid(),
  publishedAt: fc.constant('2025-04-25T12:00:00Z'),
  errorMessage: fc.constant(null),
})

/**
 * Generates a FAILED PublicationDto with any errorMessage.
 * errorMessage uses a UUID prefix to ensure it's unique and identifiable.
 */
const failedPublicationArb = fc.record({
  id: fc.uuid(),
  draftId: fc.uuid(),
  status: fc.constant('FAILED'),
  externalPostId: fc.constant(null),
  publishedAt: fc.constant(null),
  // Prefix with a fixed marker so we can reliably identify the error message
  errorMessage: fc.uuid().map(id => `ERR-${id}`),
})

/**
 * Generates a PUBLISHED draft detail with UUID-based publishedAt and externalPostId.
 * Using UUIDs ensures the values are unique, won't match other DOM content,
 * and are safe to use in text matchers.
 */
const publishedDraftDetailArb = fc.record({
  id: fc.uuid(),
  channel: fc.constantFrom('NEWSLETTER', 'LINKEDIN', 'TWITTER'),
  status: fc.constant('PUBLISHED'),
  content: fc.string({ minLength: 1, maxLength: 200 }),
  editedContent: fc.constant(null),
  createdAt: fc.constant('2025-04-25T10:00:00Z'),
  summary: fc.string({ minLength: 1, maxLength: 80 }),
  aiScore: fc.float({ min: 0, max: 10, noNaN: true }),
  // Use UUIDs so values are unique and won't accidentally match other DOM text
  publishedAt: fc.uuid().map(id => `2025-${id}`),
  externalPostId: fc.uuid().map(id => `ext-${id}`),
  sources: fc.constant([]),
  versions: fc.constant([]),
})

// ─── Setup / Teardown ─────────────────────────────────────────────────────────

beforeEach(() => {
  vi.clearAllMocks()
  resetStore()
})

afterEach(() => {
  cleanup()
})

// ─── Property 12 Tests ────────────────────────────────────────────────────────

describe('Property 12: Feedback visual correcto según resultado de publicación', () => {
  it(
    'SUCCESS: showToast con ✅ y nombre del canal, updateDraftStatus con PUBLISHED',
    async () => {
      await fc.assert(
        fc.asyncProperty(approvedDraftArb, successPublicationArb, async (draft, publication) => {
          // Arrange
          resetStore()
          vi.clearAllMocks()
          draftsApi.list.mockResolvedValue([draft])
          draftsApi.publish.mockResolvedValue(publication)

          const { unmount } = render(<Drafts />)

          // Wait for loading to finish (skeleton cards disappear)
          await waitFor(() => {
            const skeletons = document.querySelectorAll('[class*="skeletonCard"]')
            expect(skeletons.length).toBe(0)
          })

          // Find the Publish button
          const publishButton = screen.queryByRole('button', { name: /Publicar/i })
          expect(publishButton).not.toBeNull()

          // Act: click the Publish button
          await act(async () => {
            fireEvent.click(publishButton)
          })

          // Wait for the async publish call to complete
          await waitFor(() => {
            expect(draftsApi.publish).toHaveBeenCalledWith(draft.id)
          })

          // Assert: showToast called with ✅ and channel name in the body
          const expectedChannelLabel = CHANNEL_LABELS[draft.channel]
          expect(storeState.showToast).toHaveBeenCalledWith(
            '✅',
            expect.any(String),
            expect.stringContaining(expectedChannelLabel)
          )

          // Assert: updateDraftStatus called with PUBLISHED
          expect(storeState.updateDraftStatus).toHaveBeenCalledWith(draft.id, 'PUBLISHED')

          unmount()
        }),
        { numRuns: 30 }
      )
    }
  )

  it(
    'FAILED: showToast con ✗ y errorMessage, updateDraftStatus NO llamado con PUBLISHED',
    async () => {
      await fc.assert(
        fc.asyncProperty(approvedDraftArb, failedPublicationArb, async (draft, publication) => {
          // Arrange
          resetStore()
          vi.clearAllMocks()
          draftsApi.list.mockResolvedValue([draft])
          draftsApi.publish.mockResolvedValue(publication)

          const { unmount } = render(<Drafts />)

          // Wait for loading to finish
          await waitFor(() => {
            const skeletons = document.querySelectorAll('[class*="skeletonCard"]')
            expect(skeletons.length).toBe(0)
          })

          // Find the Publish button
          const publishButton = screen.queryByRole('button', { name: /Publicar/i })
          expect(publishButton).not.toBeNull()

          // Act: click the Publish button
          await act(async () => {
            fireEvent.click(publishButton)
          })

          // Wait for the async publish call to complete
          await waitFor(() => {
            expect(draftsApi.publish).toHaveBeenCalledWith(draft.id)
          })

          // Assert: showToast called with ✗ and the errorMessage
          expect(storeState.showToast).toHaveBeenCalledWith(
            '✗',
            expect.any(String),
            publication.errorMessage
          )

          // Assert: updateDraftStatus NOT called with 'PUBLISHED'
          const publishedCalls = storeState.updateDraftStatus.mock.calls.filter(
            (call) => call[1] === 'PUBLISHED'
          )
          expect(publishedCalls.length).toBe(0)

          unmount()
        }),
        { numRuns: 30 }
      )
    }
  )
})

// ─── Property 15 Tests ────────────────────────────────────────────────────────

describe('Property 15: El modal muestra fecha y ID externo del post publicado', () => {
  it(
    'DraftModal muestra publishedAt y externalPostId para cualquier borrador PUBLISHED',
    async () => {
      await fc.assert(
        fc.asyncProperty(publishedDraftDetailArb, async (detail) => {
          // Arrange: set modalDraftId so DraftModal renders, mock getDetail
          resetStore({ modalDraftId: detail.id })
          vi.clearAllMocks()
          draftsApi.getDetail.mockResolvedValue(detail)

          const { unmount } = render(<DraftModal />)

          // Wait for getDetail to be called
          await waitFor(() => {
            expect(draftsApi.getDetail).toHaveBeenCalledWith(detail.id)
          })

          // Wait for the loading spinner to disappear
          await waitFor(() => {
            expect(screen.queryByText('Cargando detalle…')).toBeNull()
          })

          // Assert: publishedAt is visible somewhere in the modal body.
          // Use queryAllByText with a function matcher to handle text split across elements.
          const publishedAtElements = screen.queryAllByText(
            (content) => content.includes(detail.publishedAt)
          )
          expect(publishedAtElements.length).toBeGreaterThan(0)

          // Assert: externalPostId is visible somewhere in the modal body.
          const externalPostIdElements = screen.queryAllByText(
            (content) => content.includes(detail.externalPostId)
          )
          expect(externalPostIdElements.length).toBeGreaterThan(0)

          unmount()
        }),
        { numRuns: 30 }
      )
    }
  )
})
