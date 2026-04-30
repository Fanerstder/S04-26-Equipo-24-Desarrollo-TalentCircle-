import { useEffect, useRef, useState } from 'react'
import { X, ThumbsDown, ThumbsUp, Send } from 'lucide-react'
import { useAppStore } from '../store/useAppStore'
import styles from './DraftModal.module.css'

export default function DraftModal() {
  const { modalDraftId, closeModal, drafts, updateDraftContent, updateDraftStatus, showToast } = useAppStore()
  const draft = drafts.find((d) => d.id === modalDraftId)
  const [text, setText] = useState('')
  const overlayRef = useRef()

  useEffect(() => {
    if (draft) setText(draft.content)
  }, [draft])

  useEffect(() => {
    const handler = (e) => { if (e.key === 'Escape') closeModal() }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [closeModal])

  if (!draft) return null

  const charLimit = draft.channel === 'twitter' ? 280 : null
  const isOver = charLimit && text.length > charLimit

  const handleApprove = () => {
    updateDraftContent(draft.id, text)
    updateDraftStatus(draft.id, 'approved')
    closeModal()
    showToast('✅', 'Borrador aprobado', 'El contenido está listo para publicación')
  }
  const handleReject = () => {
    updateDraftStatus(draft.id, 'rejected', { rejectionReason: 'Rechazado desde el panel de revisión' })
    closeModal()
    showToast('✗', 'Borrador rechazado', 'Se registró el rechazo en el sistema')
  }
  const handlePublish = () => {
    updateDraftContent(draft.id, text)
    updateDraftStatus(draft.id, 'published', { publishedAt: 'ahora' })
    closeModal()
    showToast('🚀', `Publicando en ${draft.channelLabel}…`, 'Conectando con la API…')
    setTimeout(() => showToast('✅', '¡Publicado!', `Post visible en ${draft.channelLabel}`), 2000)
  }

  return (
    <div className={`${styles.overlay} ${modalDraftId ? styles.open : ''}`}
      ref={overlayRef}
      onClick={(e) => { if (e.target === overlayRef.current) closeModal() }}>
      <div className={styles.modal}>
        {/* Header */}
        <div className={styles.header}>
          <span className={styles.chanIcon}>{draft.icon}</span>
          <div className={styles.meta}>
            <h3 className={styles.title}>{draft.title}</h3>
            <div className={styles.metaRow}>
              <span className={`channel-badge ${draft.channel}`}>{draft.channelLabel}</span>
              <span className={`status-badge ${draft.status}`}>{
                { pending:'Pendiente', approved:'Aprobado', rejected:'Rechazado', published:'Publicado' }[draft.status]
              }</span>
              <span className={styles.score}>✦ Score IA: <b>{draft.score}</b>/10</span>
            </div>
          </div>
          <button className={styles.closeBtn} onClick={closeModal}><X size={16} /></button>
        </div>

        {/* Body */}
        <div className={styles.body}>
          <p className={styles.editorLabel}>Borrador — edita directamente</p>
          <textarea
            className={`${styles.editor} ${isOver ? styles.over : ''}`}
            value={text}
            onChange={(e) => setText(e.target.value)}
          />
          <p className={`${styles.charCount} ${isOver ? styles.overCount : ''}`}>
            {charLimit ? `${text.length} / ${charLimit} caracteres` : `${text.length} caracteres`}
          </p>

          {draft.sources.length > 0 && (
            <div className={styles.sources}>
              <p className={styles.sourcesTitle}>Contribuciones fuente utilizadas</p>
              {draft.sources.map((s, i) => (
                <div key={i} className={styles.sourceItem}>
                  <span className={styles.sourceIcon}>{s.icon}</span>
                  <div className={styles.sourceInfo}>
                    <h5>{s.title}</h5>
                    <p>{s.preview}</p>
                  </div>
                  <span className={styles.sourceScore}>✦ {s.score}</span>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className={styles.footer}>
          <button className="btn btn-ghost" onClick={closeModal}>Cancelar</button>
          <button className="btn btn-red" onClick={handleReject}><ThumbsDown size={14} /> Rechazar</button>
          <button className="btn btn-green" onClick={handleApprove}><ThumbsUp size={14} /> Aprobar</button>
          {draft.status === 'approved' && (
            <button className="btn btn-amber" onClick={handlePublish}><Send size={14} /> Publicar en {draft.channelLabel}</button>
          )}
        </div>
      </div>
    </div>
  )
}
