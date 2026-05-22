import { useState, useEffect } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { Bell, Play, Sun, Moon, AlertTriangle, X } from 'lucide-react'
import { useAppStore } from '../store/useAppStore'
import adminApi from '../api/adminApi'
import styles from './Topbar.module.css'

// Subtítulos estáticos para rutas que no necesitan datos dinámicos
const STATIC_META = {
  '/executions': { title: 'Historial de Ejecuciones', sub: 'Registro completo del pipeline' },
  '/admin':      { title: 'Administración',            sub: 'Configuración del sistema y usuarios' },
}

export default function Topbar() {
  const { pathname } = useLocation()
  const navigate = useNavigate()

  // ─── Estado para controlar el Modo Oscuro / Claro ───
  const [darkMode, setDarkMode] = useState(true)

  // Efecto para aplicar la clase al body de la página web completa
  useEffect(() => {
    if (darkMode) {
      document.body.classList.add('dark-mode')
      document.body.classList.remove('light-mode')
    } else {
      document.body.classList.add('light-mode')
      document.body.classList.remove('dark-mode')
    }
  }, [darkMode])

  const {
    showToast,
    pipelineRunning,
    pipelineStatus,
    pipelineError,
    pipelineAlertDismissed,
    setPipelineRunning,
    setPipelineStatus,
    dismissPipelineAlert,
    draftTotalCount,
    draftPendingCount,
  } = useAppStore()

  // ── Dynamic subtitles for routes that show draft counts ──────────────────
  const getDraftsSub = () => {
    if (draftTotalCount === null) return 'Panel editorial — revisión y aprobación'
    const pending = draftPendingCount ?? 0
    return `${draftTotalCount} borrador${draftTotalCount !== 1 ? 'es' : ''} · ${pending} pendiente${pending !== 1 ? 's' : ''} de revisión`
  }

  const getDashboardSub = () => {
    if (draftTotalCount === null) return 'Resumen semanal del pipeline de contenido'
    const pending = draftPendingCount ?? 0
    if (pending > 0) return `${pending} borrador${pending !== 1 ? 'es' : ''} pendiente${pending !== 1 ? 's' : ''} de revisión`
    return `${draftTotalCount} borrador${draftTotalCount !== 1 ? 'es' : ''} generados esta semana`
  }

  const getMeta = () => {
    if (STATIC_META[pathname]) return STATIC_META[pathname]
    if (pathname === '/drafts')    return { title: 'Borradores',        sub: getDraftsSub() }
    if (pathname === '/dashboard') return { title: 'Dashboard Semanal', sub: getDashboardSub() }
    return { title: 'TalentCircle', sub: '' }
  }

  const { title, sub } = getMeta()

  // ── Show alert bell when last execution failed ────────────────────────────
  const showAlert = pipelineStatus === 'failed' && !pipelineAlertDismissed

  // ── Trigger pipeline manually ─────────────────────────────────────────────
  const runPipeline = async () => {
    if (pipelineRunning) return

    setPipelineRunning(true)
    setPipelineStatus('running')
    showToast('⚙️', 'Pipeline iniciado', 'Recolectando actividad de la comunidad…')

    try {
      const { executionId } = await adminApi.triggerExecution()
      setPipelineStatus('completed', null, executionId)
      showToast('✅', 'Pipeline completado', `Ejecución ${executionId} finalizada. Borradores listos.`)
    } catch (err) {
      const msg =
        err?.response?.data?.message ||
        err?.message ||
        'Error desconocido al ejecutar el pipeline.'
      setPipelineStatus('failed', msg)
    } finally {
      setPipelineRunning(false)
    }
  }

  return (
    <header className={styles.topbar}>
      <div>
        <h2 className={styles.title}>{title}</h2>
        <p className={styles.sub}>{sub}</p>
      </div>

      <div className={styles.actions}>
        {/* ── Manual pipeline trigger button ── */}
        <button
          className={`${styles.btnRun} ${pipelineRunning ? styles.btnRunning : ''}`}
          onClick={runPipeline}
          disabled={pipelineRunning}
          aria-label="Ejecutar pipeline manualmente"
        >
          {pipelineRunning ? (
            <>
              <span className={styles.spinner} />
              Ejecutando…
            </>
          ) : (
            <>
              <span className={styles.pulse} />
              <Play size={12} fill="currentColor" /> Ejecutar Pipeline
            </>
          )}
        </button>

        {/* ── Alert bell — only visible when last execution failed ── */}
        <div className={styles.bellWrapper}>
          <button
            className={`${styles.iconBtn} ${showAlert ? styles.iconBtnAlert : ''}`}
            onClick={() => {
              if (showAlert) {
                navigate('/executions')
              } else {
                showToast('🔔', 'Sin alertas', 'El pipeline está funcionando correctamente.')
              }
            }}
            aria-label={showAlert ? 'Ver error del pipeline' : 'Notificaciones'}
          >
            <Bell size={16} />
            {showAlert && <span className={styles.alertDot} aria-hidden="true" />}
          </button>

          {/* ── Inline alert tooltip ── */}
          {showAlert && (
            <div className={styles.alertPopover} role="alert">
              <AlertTriangle size={14} className={styles.alertIcon} />
              <div className={styles.alertBody}>
                <p className={styles.alertTitle}>Pipeline fallido</p>
                <p className={styles.alertMsg}>{pipelineError ?? 'Revisa el historial de ejecuciones.'}</p>
              </div>
              <button
                className={styles.alertClose}
                onClick={(e) => { e.stopPropagation(); dismissPipelineAlert() }}
                aria-label="Cerrar alerta"
              >
                <X size={12} />
              </button>
            </div>
          )}
        </div>

        {/* ── BOTÓN SÚPER INTELIGENTE: Cambia entre Modo Claro y Oscuro ── */}
        <button 
          className={styles.iconBtn} 
          onClick={() => setDarkMode(!darkMode)}
          title={darkMode ? "Cambiar a Modo Claro" : "Cambiar a Modo Oscuro"}
          aria-label="Cambiar tema de color"
        >
          {darkMode ? (
            <Sun size={16} color="#ffb703" /> // Si está oscuro, te muestra el sol para cambiar a claro
          ) : (
            <Moon size={16} color="#a2d2ff" /> // Si está claro, te muestra la luna para cambiar a oscuro
          )}
        </button>
      </div>
    </header>
  )
}
