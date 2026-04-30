import { useAppStore } from '../../store/useAppStore'
import styles from './Executions.module.css'

const STATUS_META = {
  completed: { label: 'Completado', color: 'var(--green)' },
  running:   { label: 'En curso',   color: 'var(--amber)' },
  failed:    { label: 'Fallido',    color: 'var(--rose)'  },
}

export default function Executions() {
  const executions = useAppStore((s) => s.executions)
  const showToast  = useAppStore((s) => s.showToast)

  return (
    <div className={styles.page}>
      <div className={styles.tableWrap}>
        <table className={styles.table}>
          <thead>
            <tr>
              {['ID Ejecución','Semana','Estado','Actividades','Borradores','Duración','Iniciado por','Progreso'].map((h) => (
                <th key={h} className={styles.th}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {executions.map((ex) => {
              const sm = STATUS_META[ex.status]
              return (
                <tr key={ex.id} className={styles.row} onClick={() => showToast('🔍', `Ejecución ${ex.id}`, `Semana: ${ex.week} · ${ex.activities} actividades procesadas`)}>
                  <td className={styles.tdId}>#{ex.id}</td>
                  <td className={styles.td}>{ex.week}</td>
                  <td className={styles.td}>
                    <span className={styles.statusChip}>
                      <span className={styles.statusDot} style={{ background: sm.color, boxShadow: ex.status==='running' ? `0 0 6px ${sm.color}` : 'none' }} />
                      {sm.label}
                    </span>
                  </td>
                  <td className={styles.tdMono}>{ex.activities}</td>
                  <td className={styles.tdMono}>{ex.drafts}</td>
                  <td className={styles.tdDuration}>{ex.duration}</td>
                  <td className={styles.td}>{ex.triggeredBy}</td>
                  <td className={styles.td}>
                    <div className={styles.progTrack}>
                      <div className={styles.progFill}
                        style={{ width:`${ex.progress}%`, background: ex.status==='failed'?'var(--rose)':'linear-gradient(90deg,var(--amber),var(--teal))' }} />
                    </div>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </div>
  )
}
