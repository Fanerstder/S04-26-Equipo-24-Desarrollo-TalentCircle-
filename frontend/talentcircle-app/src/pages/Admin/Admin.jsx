import { useState } from 'react'
import { Save, Plus, ToggleLeft, ToggleRight } from 'lucide-react'
import { useAppStore } from '../../store/useAppStore'
import styles from './Admin.module.css'

const SOURCES = [
  { id:'s1', icon:'💬', name:'Discord – TalentCircle Dev',    url:'discord.gg/talentcircle',    type:'discord', active:true  },
  { id:'s2', icon:'◎', name:'Circle.so – Comunidad Principal', url:'talentcircle.circle.so',    type:'circle',  active:true  },
  { id:'s3', icon:'⬡', name:'Slack – Canal #recursos',         url:'workspace.slack.com',        type:'slack',   active:false },
]

const PROMPT_TABS = ['newsletter','linkedin','twitter']
const PROMPT_CONTENT = {
  newsletter: `Eres un editor experto de newsletters técnicos en español para la comunidad TalentCircle. 

Con base en la siguiente actividad comunitaria de la semana, genera un newsletter completo con:
- Un título atractivo y descriptivo
- Una introducción cálida y personal (2-3 párrafos)
- Sección "Lo más destacado" con los 3-5 temas principales
- Para cada tema: contexto, por qué importa y un CTA
- Cierre con invitación a participar

Tono: profesional pero cercano. Longitud: 800-1200 palabras.

Actividad semanal: {ACTIVIDADES}`,
  linkedin: `Eres un experto en contenido para LinkedIn enfocado en comunidades técnicas hispanohablantes.

Genera un post de LinkedIn con:
- Gancho poderoso en la primera línea
- 3-4 puntos clave con emojis moderados
- Un insight accionable
- CTA para unirse a la conversación
- 3-5 hashtags relevantes

Tono: profesional, inspirador y directo. Longitud: 150-300 palabras.

Actividad semanal: {ACTIVIDADES}`,
  twitter: `Eres un community manager experto en Twitter/X para comunidades técnicas.

Genera un tweet o hilo con:
- Máximo 280 caracteres por tweet
- Datos concretos si están disponibles
- 2-3 emojis estratégicos
- 2-3 hashtags relevantes
- CTA o pregunta al final

Tono: directo, ágil y conversacional.

Actividad semanal: {ACTIVIDADES}`,
}

const USERS = [
  { initials:'JC', name:'Javier Chavez',     email:'javier@talentcircle.com', role:'EDITOR', active:true,  last:'hace 2h' },
  { initials:'EP', name:'Eduin Pino', email:'eduin@talentcircle.com', role:'ADMIN',  active:true,  last:'hace 1d' },
  { initials:'FS', name:'Faner Santander', email:'faner@talentcircle.com', role:'ADMIN',  active:true,  last:'hace 1h' },
  { initials:'LA', name:'Luis Armuto',    email:'luis@talentcircle.com',   role:'EDITOR', active:false, last:'hace 5d' },
]

function Toggle({ active, onChange }) {
  return (
    <button className={`${styles.toggle} ${active?styles.toggleOn:styles.toggleOff}`} onClick={onChange}>
      {active ? <ToggleRight size={22} /> : <ToggleLeft size={22} />}
    </button>
  )
}

export default function Admin() {
  const showToast = useAppStore((s) => s.showToast)
  const [sources, setSources] = useState(SOURCES)
  const [promptTab, setPromptTab] = useState('newsletter')
  const [prompts, setPrompts] = useState(PROMPT_CONTENT)
  const [llm, setLlm] = useState('Anthropic Claude Sonnet')
  const [maxItems, setMaxItems] = useState(20)
  const [cron, setCron] = useState('0 18 * * FRI')

  const toggleSource = (id) => setSources(ss => ss.map(s => s.id===id ? {...s,active:!s.active} : s))

  return (
    <div className={styles.page}>
      <div className={styles.grid}>

        {/* Sources */}
        <div className={styles.card}>
          <h3 className={styles.cardTitle}>📡 Fuentes Comunitarias</h3>
          {sources.map((s) => (
            <div key={s.id} className={styles.sourceRow}>
              <div className={`${styles.sourceIcon} ${styles[s.type]}`}>{s.icon}</div>
              <div className={styles.sourceInfo}>
                <h4>{s.name}</h4>
                <span>{s.url} · {s.active?'Activo':'Inactivo'}</span>
              </div>
              <Toggle active={s.active} onChange={()=>toggleSource(s.id)} />
            </div>
          ))}
          <button className={styles.addBtn} onClick={()=>showToast('➕','Próximamente','Agregar fuentes estará en v1.1')}>
            <Plus size={14}/> Agregar fuente
          </button>
        </div>

        {/* Pipeline Config */}
        <div className={styles.card}>
          <h3 className={styles.cardTitle}>⚙ Configuración del Pipeline</h3>
          <div className="field">
            <label>Proveedor LLM</label>
            <select value={llm} onChange={e=>setLlm(e.target.value)}>
              <option>Anthropic Claude Sonnet</option>
              <option>OpenAI GPT-4o</option>
              <option>OpenAI GPT-4 Turbo</option>
            </select>
          </div>
          <div className="field">
            <label>Máx. actividades por ejecución</label>
            <input type="number" value={maxItems} onChange={e=>setMaxItems(e.target.value)} />
          </div>
          <div className="field">
            <label>Cron de ejecución automática</label>
            <input type="text" value={cron} onChange={e=>setCron(e.target.value)} style={{fontFamily:"'DM Mono',monospace"}} />
          </div>
          <button className="btn btn-green" style={{width:'100%'}}
            onClick={()=>showToast('✅','Configuración guardada','Aplica en la próxima ejecución')}>
            <Save size={14}/> Guardar cambios
          </button>
        </div>

        {/* Prompts */}
        <div className={`${styles.card} ${styles.fullWidth}`}>
          <h3 className={styles.cardTitle}>✎ Plantillas de Prompts por Canal</h3>
          <div className={styles.promptTabs}>
            {PROMPT_TABS.map((t) => (
              <button key={t} className={`${styles.promptTab} ${promptTab===t?styles.ptActive:''}`} onClick={()=>setPromptTab(t)}>
                {{ newsletter:'📧 Newsletter', linkedin:'🔵 LinkedIn', twitter:'🐦 Twitter' }[t]}
              </button>
            ))}
          </div>
          <textarea className={styles.promptEditor}
            value={prompts[promptTab]}
            onChange={(e)=>setPrompts({...prompts,[promptTab]:e.target.value})} />
          <div className={styles.promptActions}>
            <button className="btn btn-ghost"
              onClick={()=>showToast('🧪','Prueba iniciada','Generando borrador de muestra…')}>
              Probar prompt
            </button>
            <button className="btn btn-green"
              onClick={()=>showToast('✅','Prompt guardado','Se usará en la próxima ejecución')}>
              <Save size={14}/> Guardar
            </button>
          </div>
        </div>

        {/* Users */}
        <div className={`${styles.card} ${styles.fullWidth}`}>
          <h3 className={styles.cardTitle}>👥 Gestión de Usuarios</h3>
          <table className={styles.usersTable}>
            <thead>
              <tr>{['Usuario','Email','Rol','Estado','Último acceso','Acciones'].map(h=><th key={h} className={styles.uth}>{h}</th>)}</tr>
            </thead>
            <tbody>
              {USERS.map((u) => (
                <tr key={u.email} className={styles.urow}>
                  <td className={styles.utd}>
                    <div className={styles.userCell}>
                      <div className={styles.uavatar}>{u.initials}</div>
                      {u.name}
                    </div>
                  </td>
                  <td className={styles.utdMono}>{u.email}</td>
                  <td className={styles.utd}>
                    <span className={`status-badge ${u.role==='ADMIN'?'pending':'approved'}`}>{u.role}</span>
                  </td>
                  <td className={styles.utd}>
                    <span style={{display:'inline-flex',alignItems:'center',gap:6,fontSize:12,fontFamily:"'DM Mono',monospace"}}>
                      <span style={{width:7,height:7,borderRadius:'50%',background:u.active?'var(--green)':'var(--text3)',display:'inline-block'}}/>
                      {u.active?'Activo':'Inactivo'}
                    </span>
                  </td>
                  <td className={styles.utdMono}>{u.last}</td>
                  <td className={styles.utd}>
                    <button className="btn btn-ghost" style={{padding:'5px 12px',fontSize:11}}
                      onClick={()=>showToast('✎',`Editando ${u.name}`,'Panel de edición próximamente en v1.1')}>
                      Editar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <button className={styles.addBtn} style={{marginTop:16}}
            onClick={()=>showToast('➕','Próximamente','Crear usuarios estará en v1.1')}>
            <Plus size={14}/> Nuevo usuario
          </button>
        </div>

      </div>
    </div>
  )
}
