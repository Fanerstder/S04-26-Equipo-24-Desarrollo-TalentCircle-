import { useEffect } from 'react'
import { useAppStore } from '../store/useAppStore'
import styles from './Toast.module.css'

export default function Toast() {
  const toast = useAppStore((s) => s.toast)
  return (
    <div className={`${styles.toast} ${toast ? styles.show : ''}`}>
      <span className={styles.icon}>{toast?.icon}</span>
      <div>
        <p className={styles.title}>{toast?.title}</p>
        <p className={styles.body}>{toast?.body}</p>
      </div>
    </div>
  )
}
