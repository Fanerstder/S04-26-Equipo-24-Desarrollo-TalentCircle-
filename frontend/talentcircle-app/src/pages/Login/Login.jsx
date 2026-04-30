import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAppStore } from '../../store/useAppStore'
import styles from './Login.module.css'

export default function Login() {
  const [email, setEmail] = useState('editor@talentcircle.com')
  const [password, setPassword] = useState('password123')
  const [loading, setLoading] = useState(false)
  const { login, showToast } = useAppStore()
  const navigate = useNavigate()

  const handleSubmit = (e) => {
    e.preventDefault()
    if (!email || !password) { showToast('⚠️','Campos requeridos','Ingresa tu email y contraseña'); return }
    setLoading(true)
    setTimeout(() => {
      login({ name: 'Faner Santander', initials: 'FS', role: 'ADMIN', email })
      showToast('✅','Bienvenido(a), Faner','Tienes 4 borradores pendientes de revisión')
      navigate('/dashboard')
    }, 900)
  }

  return (
    <div className={styles.page}>
      <div className={styles.bg}>
        <div className={styles.grid} />
        <div className={styles.glow1} />
        <div className={styles.glow2} />
      </div>
      <form className={styles.card} onSubmit={handleSubmit}>
        <div className={styles.logoRow}>
          <div className={styles.logoIcon}>✦</div>
          <span className={styles.logoText}>Talent<em>Circle</em></span>
        </div>
        <h1 className={styles.heading}>Bienvenido de vuelta</h1>
        <p className={styles.sub}>Pipeline Inteligente de Contenido Comunitario</p>

        <div className="field">
          <label>Correo electrónico</label>
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="editor@talentcircle.com" />
        </div>
        <div className="field">
          <label>Contraseña</label>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="••••••••" />
        </div>

        <button type="submit" className={styles.btnLogin} disabled={loading}>
          {loading ? <span className={styles.spinner} /> : 'Ingresar al panel →'}
        </button>
        <p className={styles.hint}>
          Demo: <code>editor@talentcircle.com</code> / <code>password123</code>
        </p>
      </form>
    </div>
  )
}
