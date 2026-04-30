import { useState } from 'react'
import { CheckCircle, XCircle, Send, Search } from 'lucide-react'
import { useAppStore } from '../../store/useAppStore'
import styles from './Drafts.module.css'

const STATUS_LABELS = { pending:'Pendiente', approved:'Aprobado', published:'Publicado', rejected:'Rechazado' }
const CHANNELS = ['Todos','linkedin','twitter','newsletter']
const STATUSES = ['Todos','pending','approved','published','rejected']
const CHANNEL_LABELS = { linkedin:'LinkedIn', twitter:'Twitter', newsletter:'Newsletter' }
const STATUS_DISPLAY = { pending:'Pendiente', approved:'Aprobado', published:'Publicado', rejected:'Rechazado' }

function DraftCard({ draft }) {
  const { openModal, updateDraftStatus, showToast } = useAppStore()
  const pct = Math.round(draft.score * 10)

  const approve = (e) => {
    e.stopPropagation()
    updateDraftStatus(draft.id, 'approved')
    showToast('✅','Borrador aprobado','Listo para publicación')
  }
  const reject = (e) => {
    e.stopPropagation()
    updateDraftStatus(draft.id, 'rejected', { rejectionReason: 'Rechazado desde el panel' })
    showToast('✗','Borrador rechazado','Registrado en el sistema')
  }
  const publish = (e) => {
    e.stopPropagation()
    updateDraftStatus(draft.id, 'published', { publishedAt: 'ahora' })
    showToast('🚀', `Publicando en ${draft.channelLabel}…`, 'Conectando con la API…')
    setTimeout(() => showToast('✅', '¡Publicado!', `Post visible en ${draft.channelLabel}`), 2000)
  }

  return (
    <div className={styles.card} onClick={() => openModal(draft.id)}>
      <div className={styles.cardHeader}>
        <span className={styles.chanIcon}>{draft.icon}</span>
        <div className={styles.chanMeta}>
          <span className={styles.chanName}>{draft.channelLabel.toUpperCase()}</span>
          <span className={styles.chanDate}>{draft.createdAt}</span>
        </div>
        <span className={`status-badge ${draft.status}`}>{STATUS_DISPLAY[draft.status]}</span>
      </div>
      <div className={styles.cardBody}>
        <h3 className={styles.cardTitle}>{draft.title}</h3>
        <p className={styles.cardPreview}>{draft.preview}</p>
      </div>
      <div className={styles.cardFooter}>
        <div className={styles.scoreBar}>
          <div className={styles.scoreTrack}><div className={styles.scoreFill} style={{ width:`${pct}%` }} /></div>
          <span className={styles.scoreVal}>✦ {draft.score}</span>
        </div>
        <div className={styles.cardActions} onClick={(e)=>e.stopPropagation()}>
          {draft.status === 'pending' && <>
            <button className="btn-sm approve" onClick={approve}><CheckCircle size={11}/> Aprobar</button>
            <button className="btn-sm reject"  onClick={reject}><XCircle size={11}/> Rechazar</button>
          </>}
          {draft.status === 'approved' && (
            <button className="btn-sm publish" onClick={publish}><Send size={11}/> Publicar</button>
          )}
          {draft.status === 'rejected' && (
            <button className="btn-sm approve" onClick={approve}>↺ Revisar</button>
          )}
          {draft.status === 'published' && (
            <span className={styles.pubLabel}>↑ Publicado · {draft.publishedAt}</span>
          )}
        </div>
      </div>
    </div>
  )
}

export default function Drafts() {
  const { drafts } = useAppStore()
  const [search, setSearch] = useState('')
  const [channel, setChannel] = useState('Todos')
  const [status, setStatus]   = useState('Todos')

  const filtered = drafts.filter((d) => {
    const matchChan   = channel === 'Todos' || d.channel === channel
    const matchStatus = status  === 'Todos' || d.status  === status
    const matchSearch = d.title.toLowerCase().includes(search.toLowerCase()) ||
                        d.preview.toLowerCase().includes(search.toLowerCase())
    return matchChan && matchStatus && matchSearch
  })

  return (
    <div className={styles.page}>
      <div className={styles.filtersBar}>
        <div className={styles.searchWrap}>
          <Search size={14} className={styles.searchIcon} />
          <input className={styles.search} placeholder="Buscar en borradores…" value={search}
            onChange={(e)=>setSearch(e.target.value)} />
        </div>
        {CHANNELS.map((c) => (
          <button key={c} className={`${styles.chip} ${channel===c?styles.active:''}`} onClick={()=>setChannel(c)}>
            {CHANNEL_LABELS[c]||c}
          </button>
        ))}
        <div className={styles.divider} />
        {STATUSES.map((s) => (
          <button key={s} className={`${styles.chip} ${status===s?styles.active:''}`} onClick={()=>setStatus(s)}>
            {STATUS_DISPLAY[s]||s}
          </button>
        ))}
      </div>

      {filtered.length === 0 ? (
        <div className="empty-state"><span className="icon">🔍</span><p>No hay borradores que coincidan con los filtros</p></div>
      ) : (
        <div className={styles.grid}>
          {filtered.map((d) => <DraftCard key={d.id} draft={d} />)}
        </div>
      )}
    </div>
  )
}
