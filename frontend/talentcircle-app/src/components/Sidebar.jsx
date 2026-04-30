import { NavLink, useNavigate } from 'react-router-dom'
import { LayoutDashboard, FileText, Settings, Zap, LogOut, Layers, Link } from 'lucide-react'
import { useAppStore } from '../store/useAppStore'
import styles from './Sidebar.module.css'

const NAV = [
  { to: '/dashboard',  icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/drafts',     icon: FileText,         label: 'Borradores', badge: 4 },
  { to: '/executions', icon: Zap,              label: 'Ejecuciones' },
]
const ADMIN_NAV = [
  { to: '/admin', icon: Settings, label: 'Administración' },
  { to: '#',      icon: Link,     label: 'Integraciones' },
  { to: '#',      icon: Layers,   label: 'Plantillas IA' },
]

export default function Sidebar() {
  const { currentUser, logout } = useAppStore()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <nav className={styles.sidebar}>
      <div className={styles.logo}>
        <div className={styles.logoIcon}>✦</div>
        <span className={styles.logoText}>Talent<em>Circle</em></span>
      </div>

      <span className={styles.sectionLabel}>Principal</span>
      {NAV.map(({ to, icon: Icon, label, badge }) => (
        <NavLink key={to} to={to} className={({ isActive }) =>
          `${styles.navItem} ${isActive ? styles.active : ''}`}>
          <Icon size={16} />
          <span>{label}</span>
          {badge && <span className={styles.badge}>{badge}</span>}
        </NavLink>
      ))}

      <span className={styles.sectionLabel}>Configuración</span>
      {ADMIN_NAV.map(({ to, icon: Icon, label }) => (
        <NavLink key={label} to={to} className={({ isActive }) =>
          `${styles.navItem} ${isActive ? styles.active : ''}`}>
          <Icon size={16} />
          <span>{label}</span>
        </NavLink>
      ))}

      <div className={styles.footer}>
        <div className={styles.userChip}>
          <div className={styles.avatar}>{currentUser?.initials || 'AL'}</div>
          <div className={styles.userInfo}>
            <div className={styles.userName}>{currentUser?.name || 'Ana López'}</div>
            <div className={styles.userRole}>{currentUser?.role || 'EDITOR'}</div>
          </div>
          <button className={styles.logoutBtn} onClick={handleLogout} title="Cerrar sesión">
            <LogOut size={15} />
          </button>
        </div>
      </div>
    </nav>
  )
}
