import { useEffect, useRef, useState } from 'react'
import { X, ThumbsDown, ThumbsUp, Loader2, Send } from 'lucide-react'
import { useAppStore } from '../store/useAppStore'
import draftsApi from '../api/draftsApi'
import styles from './DraftModal.module.css'

// Map API channel enum → display label
const CHANNEL_LABELS = {
  NEWSLETTER: 'Newsletter',
  LINKEDIN: 'LinkedIn',
  TWITTER: 'Twitter',
}

export default function DraftModal() {
  const {
    modalDraftId,
    closeModal,
    drafts,
    updateDraftContent,
    updateDraftStatus,
    showToast,
  } = useAppStore()

  // Full detail loaded from API (includes sources, versions)
  const [detail, setDetail]           = useState(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [text, setText]               = useState('')
  const [actionLoading, setActionLoading] = useState(false)
  const [publishLoading, setPublishLoading] = useState(false)

  const overlayRef = useRef()

  // Load full detail whenever the modal opens for a new draft
  useEffect(() => {
    if (!modalDraftId) {
      setDetail(null)
      setText('')
      return
    }

    let cancelled = false
    setDetailLoading(true)
    draftsApi
      .getDetail(modalDraftId)
      .then((d) => {
        if (!cancelled) {
          setDetail(d)
          setText(d.editedContent ?? d.content ?? '')
        }
      })
      .catch(() => { /* error already toasted by apiClient interceptor */ })
      .finally(() => { if (!cancelled) setDetailLoading(false) })

    return () => { cancelled = true }
  }, [modalDraftId])

  // Keyboard close
  useEffect(() => {
    const handler = (e) => { if (e.key === 'Escape') closeModal() }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [closeModal])

  if (!modalDraftId) return null

  // Use summary-level data from store while detail is loading
  const summaryDraft = drafts.find((d) => d.id === modalDraftId)
  const channel      = detail?.channel ?? summaryDraft?.channel
  const status       = detail?.status  ?? summaryDraft?.status
  const channelLabel = CHANNEL_LABELS[channel] ?? channel ?? ''
  const aiScore      = detail?.aiScore ?? summaryDraft?.aiScore
  const charLimit    = channel === 'TWITTER' ? 280 : null
  const isOver       = charLimit != null && text.length > charLimit

  const statusLabels = {
    PENDING: 'Pendiente',
    APPROVED: 'Aprobado',
    REJECTED: 'Rechazado',
    PUBLISHED: 'Publicado',
  }

  // ── Save (approve with content update) ──────────────────────────────────
  const handleApprove = async () => {
    setActionLoading(true)
    try {
      await draftsApi.updateContent(modalDraftId, text)
      updateDraftContent(modalDraftId, text)
      await draftsApi.approve(modalDraftId)
      updateDraftStatus(modalDraftId, 'APPROVED')
      closeModal()
      showToast('✅', 'Borrador aprobado', 'El contenido fue guardado y aprobado exitosamente.')
    } finally {
      setActionLoading(false)
    }
  }

  // ── Reject ───────────────────────────────────────────────────────────────
  const handleReject = async () => {
    setActionLoading(true)
    try {
      await draftsApi.reject(modalDraftId, 'Rechazado desde el panel')
      updateDraftStatus(modalDraftId, 'REJECTED')
      closeModal()
      showToast('✗', 'Borrador rechazado', 'El borrador fue rechazado exitosamente.')
    } finally {
      setActionLoading(false)
    }
  }

  // ── Save content only (without approving) ───────────────────────────────
  const handleSave = async () => {
    setActionLoading(true)
    try {
      await draftsApi.updateContent(modalDraftId, text)
      updateDraftContent(modalDraftId, text)
      closeModal()
      showToast('✅', 'Contenido guardado', 'Los cambios fueron guardados exitosamente.')
    } finally {
      setActionLoading(false)
    }
  }

  // ── Publish ──────────────────────────────────────────────────────────────
  const handlePublish = async () => {
    setPublishLoading(true)
    try {
      const result = await draftsApi.publish(modalDraftId)
      if (result.status === 'SUCCESS') {
        updateDraftStatus(modalDraftId, 'PUBLISHED')
        closeModal()
        showToast('✅', 'Borrador publicado', `Publicado exitosamente en ${channelLabel}`)
      } else {
        showToast('✗', 'Error al publicar', result.errorMessage ?? 'Error desconocido')
      }
    } catch {
      // HTTP errors already toasted by apiClient interceptor
    } finally {
      setPublishLoading(false)
    }
  }

  return (
    <div
      className={`${styles.overlay} ${modalDraftId ? styles.open : ''}`}
      ref={overlayRef}
      onClick={(e) => { if (e.target === overlayRef.current) closeModal() }}
    >
      <div className={styles.modal}>
        {/* Header */}
        <div className={styles.header}>
          <div className={styles.meta}>
            <h3 className={styles.title}>{channelLabel || '—'}</h3>
            <div className={styles.metaRow}>
              <span className={`channel-badge ${channel}`}>{channelLabel}</span>
              <span className={`status-badge ${status}`}>
                {statusLabels[status] ?? status}
              </span>
              {aiScore != null && (
                <span className={styles.score}>
                  ✦ Score IA: <b>{aiScore}</b>/10
                </span>
              )}
            </div>
          </div>
          <button className={styles.closeBtn} onClick={closeModal} disabled={actionLoading}>
            <X size={16} />
          </button>
        </div>

        {/* Body */}
        <div className={styles.body}>
          {detailLoading ? (
            <div className={styles.detailLoading}>
              <Loader2 size={20} className={styles.spinner} />
              <span>Cargando detalle…</span>
            </div>
          ) : (
            <>
              <p className={styles.editorLabel}>Borrador — edita directamente</p>
              <textarea
                className={`${styles.editor} ${isOver ? styles.over : ''}`}
                value={text}
                onChange={(e) => setText(e.target.value)}
                disabled={actionLoading}
              />
              <p className={`${styles.charCount} ${isOver ? styles.overCount : ''}`}>
                {charLimit
                  ? `${text.length} / ${charLimit} caracteres`
                  : `${text.length} caracteres`}
              </p>

              {/* Sources */}
              {detail?.sources?.length > 0 && (
                <div className={styles.sources}>
                  <p className={styles.sourcesTitle}>Contribuciones fuente utilizadas</p>
                  {detail.sources.map((s) => (
                    <div key={s.id} className={styles.sourceItem}>
                      <div className={styles.sourceInfo}>
                        <h5>{s.title}</h5>
                      </div>
                      <span className={styles.sourceScore}>✦ {s.relevanceScore}</span>
                    </div>
                  ))}
                </div>
              )}

              {/* Version history */}
              {detail?.versions?.length > 0 && (
                <div className={styles.sources}>
                  <p className={styles.sourcesTitle}>Historial de versiones</p>
                  {detail.versions.map((v) => (
                    <div key={v.id} className={styles.sourceItem}>
                      <div className={styles.sourceInfo}>
                        <h5>v{v.versionNumber} — {v.editedBy ?? 'Sistema'}</h5>
                        <p>{v.editedAt}</p>
                      </div>
                    </div>
                  ))}
                </div>
              )}

              {/* Publication info */}
              {status === 'PUBLISHED' && detail?.publishedAt && (
                <div className={styles.sources}>
                  <p className={styles.sourcesTitle}>Información de publicación</p>
                  <div className={styles.sourceItem}>
                    <div className={styles.sourceInfo}>
                      <h5>Publicado el {detail.publishedAt}</h5>
                      {detail?.externalPostId && <p>ID externo: {detail.externalPostId}</p>}
                    </div>
                  </div>
                </div>
              )}
            </>
          )}
        </div>

        {/* Footer */}
        <div className={styles.footer}>
          <button className="btn btn-ghost" onClick={closeModal} disabled={actionLoading}>
            Cancelar
          </button>
          <button
            className="btn btn-ghost"
            onClick={handleSave}
            disabled={actionLoading || detailLoading}
          >
            {actionLoading ? <Loader2 size={14} className={styles.spinner} /> : null}
            Guardar
          </button>
          <button
            className="btn btn-red"
            onClick={handleReject}
            disabled={actionLoading || detailLoading}
          >
            <ThumbsDown size={14} />
            {actionLoading ? '…' : 'Rechazar'}
          </button>
          <button
            className="btn btn-green"
            onClick={handleApprove}
            disabled={actionLoading || detailLoading || isOver}
          >
            <ThumbsUp size={14} />
            {actionLoading ? '…' : 'Aprobar'}
          </button>
          {status === 'APPROVED' && (
            <button
              className="btn btn-green"
              onClick={handlePublish}
              disabled={actionLoading || detailLoading || publishLoading}
              aria-label={`Publicar en ${channelLabel}`}
            >
              <Send size={14} />
              {publishLoading ? '…' : `Publicar en ${channelLabel}`}
            </button>
          )}
        </div>
      </div>
    </div>
  )
}
