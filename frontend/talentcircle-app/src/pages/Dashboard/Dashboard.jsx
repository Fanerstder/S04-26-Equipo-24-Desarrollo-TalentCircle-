import { useNavigate } from 'react-router-dom'
import { useAppStore } from '../../store/useAppStore'
import styles from './Dashboard.module.css'

function PipelineBanner() {
  const steps = [
    { label: 'Recolección', status: 'done' },
    { label: 'Análisis IA',  status: 'done' },
    { label: 'Generación',  status: 'done' },
    { label: 'Revisión',    status: 'active' },
    { label: 'Publicación', status: 'waiting' },
  ]
  return (
    <div className={styles.banner}>
      <div className={styles.bannerLeft} />
      <div className={styles.steps}>
        {steps.map((s, i) => (
          <>
            <div key={s.label} className={styles.step}>
              <div className={`${styles.dot} ${styles[s.status]}`}>
                {s.status === 'done' ? '✓' : s.status === 'active' ? '◉' : '○'}
              </div>
              <span className={styles.stepLabel}>{s.label}</span>
            </div>
            {i < steps.length - 1 && <span className={styles.arrow} key={`a${i}`}>→</span>}
          </>
        ))}
      </div>
      <span className={styles.bannerTime}>Última ejecución: Vie 25 abr · 18:47</span>
    </div>
  )
}

const STATS = [
  { label: 'Actividades Recolectadas', value: '147', color: 'amber', change: '↑ 23% vs semana anterior' },
  { label: 'Borradores Generados',     value: '6',   color: 'teal',  change: '2 canales · 3 formatos' },
  { label: 'Pendientes de Revisión',   value: '4',   color: 'rose',  change: '2 aprobados esta semana' },
  { label: 'Publicados este mes',      value: '18',  color: 'green', change: '↑ 40% alcance estimado' },
]

export default function Dashboard() {
  const { feedItems, drafts, openModal } = useAppStore()
  const navigate = useNavigate()
  const weekDrafts = drafts.filter((d) => d.week === '21–28 abr 2025').slice(0, 3)

  return (
    <div className={styles.page}>
      <PipelineBanner />

      {/* Stats */}
      <div className={styles.statsGrid}>
        {STATS.map((s) => (
          <div key={s.label} className={`${styles.statCard} ${styles[s.color]}`}>
            <p className={styles.statLabel}>{s.label}</p>
            <p className={`${styles.statValue} ${styles[s.color + 'Text']}`}>{s.value}</p>
            <p className={styles.statChange}>{s.change}</p>
          </div>
        ))}
      </div>

      {/* Grid */}
      <div className={styles.grid}>
        {/* Feed */}
        <div>
          <div className="section-header">
            <span className="section-title">Actividad Comunitaria Destacada</span>
            <span className="section-link">Ver todo →</span>
          </div>
          <div className={styles.feed}>
            <div className={styles.feedHeader}>
              <span>📡</span><span>Feed en tiempo real</span>
              <span className={styles.liveDot} />
            </div>
            {feedItems.map((f) => (
              <div key={f.id} className={styles.feedItem}>
                <div className={styles.feedAvatar} style={{ background: f.bg, color: f.color }}>{f.initials}</div>
                <div className={styles.feedContent}>
                  <p className={styles.feedText}><strong>{f.author}</strong> {f.text}</p>
                  <div className={styles.feedMeta}>
                    <span>{f.time}</span>
                    <span>{f.reactions}</span>
                    <span style={{ color: 'var(--green)' }}>Score: {f.score}/10</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Draft Summaries */}
        <div>
          <div className="section-header">
            <span className="section-title">Borradores de la Semana</span>
            <span className="section-link" onClick={() => navigate('/drafts')}>Ver todos →</span>
          </div>
          <div className={styles.draftList}>
            {weekDrafts.map((d) => (
              <div key={d.id} className={`${styles.draftSummary} ${styles[d.channel]}`} onClick={() => openModal(d.id)}>
                <div className={styles.dscTop}>
                  <span className={`channel-badge ${d.channel}`}>{d.channelLabel}</span>
                  <span className={`status-badge ${d.status}`}>{
                    { pending:'Pendiente', approved:'Aprobado', published:'Publicado', rejected:'Rechazado' }[d.status]
                  }</span>
                </div>
                <p className={styles.dscTitle}>{d.title}</p>
                <p className={styles.dscPreview}>{d.preview}</p>
                <p className={styles.dscScore}>✦ Score IA: {d.score} / 10</p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
