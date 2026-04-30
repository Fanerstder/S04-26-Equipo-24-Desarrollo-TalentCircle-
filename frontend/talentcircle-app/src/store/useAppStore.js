import { create } from 'zustand'

const INITIAL_DRAFTS = [
  { id:'draft-1',channel:'linkedin',channelLabel:'LinkedIn',icon:'🔵',title:'Microservicios con Spring Boot: lo que aprendimos esta semana en TalentCircle',preview:'Esta semana la comunidad debatió extensamente sobre los patrones de diseño más efectivos para arquitecturas de microservicios. Carlos Reyes compartió su experiencia.',content:'🚀 Esta semana en TalentCircle exploramos a fondo los microservicios con Spring Boot.\n\nCarlos Reyes compartió su experiencia real migrando un monolito de 5 años a microservicios con Docker y Kubernetes. El post generó 47 reacciones y 18 comentarios técnicos.\n\nAprendizajes clave:\n✅ El tamaño del microservicio importa.\n✅ Spring Boot 3 + GraalVM redujo el arranque de 8s a 180ms.\n✅ RabbitMQ fue clave para desacoplar servicios.\n\n#SpringBoot #Microservices #Java #TalentCircle',status:'pending',score:9.2,createdAt:'25 abr 2025 · 19:02',week:'21–28 abr 2025',sources:[{icon:'🔥',title:'Migración a microservicios – Carlos Reyes',preview:'47 reacciones y 18 comentarios técnicos.',score:9.2},{icon:'💬',title:'Hilo: ¿cuándo NO usar microservicios?',preview:'17 devs compartieron casos reales.',score:7.8},{icon:'⭐',title:'Spring Boot Native Image Guide',preview:'22 compartidas.',score:7.3}]},
  { id:'draft-2',channel:'twitter',channelLabel:'Twitter / X',icon:'🐦',title:'¿Zustand o Redux? La comunidad TalentCircle tiene su veredicto',preview:'34 devs debatieron estado global en React. El veredicto: Zustand gana en simplicidad.',content:'🧵 Esta semana en @TalentCircle: 34 devs debatieron estado global en React.\n\nEl veredicto:\n→ Zustand: simplicidad para proyectos medianos\n→ Redux: enterprise con equipos grandes\n→ Context API: solo para estado de UI local\n\n¿Tu stack? 👇\n\n#ReactJS #Frontend #JavaScript #TalentCircle',status:'approved',score:8.7,createdAt:'25 abr 2025 · 19:04',week:'21–28 abr 2025',sources:[{icon:'💬',title:'Debate: Zustand vs Redux – 34 respuestas',preview:'María Valdez abrió el debate con experiencia real.',score:8.7},{icon:'📊',title:'Poll: ¿Qué estado usas en React?',preview:'Zustand 42% · Redux 35% · Context 15%. 89 votos.',score:6.5}]},
  { id:'draft-3',channel:'newsletter',channelLabel:'Newsletter',icon:'📧',title:'El Digest Semanal: APIs, Testing y la evolución del stack Java moderno',preview:'Esta semana nuestra comunidad generó 147 contribuciones técnicas de alta calidad.',content:'Hola comunidad TalentCircle 👋\n\nEsta semana fue una de las más activas del año. 147 contribuciones técnicas.\n\n─── Lo más destacado ───\n\n🚀 MICROSERVICIOS EN LA PRÁCTICA\nCarlos Reyes documentó su migración. GraalVM redujo el arranque de 8s a 180ms.\n\n⚛️ ZUSTAND VS REDUX\n34 desarrolladores compartieron su experiencia. Zustand para medianos, Redux para enterprise.\n\n🧪 TESTCONTAINERS\nLaura Gómez planteó la pregunta clave. 21 comentarios de alto valor técnico.\n\n─── Recursos ───\n📁 Spring Boot Native Guide – 22 compartidas\n📁 GitHub Actions Templates – 18 compartidas\n📁 Guía Testcontainers – 15 compartidas\n\nHasta pronto, El equipo TalentCircle ✦',status:'pending',score:8.4,createdAt:'25 abr 2025 · 19:08',week:'21–28 abr 2025',sources:[{icon:'🔥',title:'Migración microservicios – Carlos Reyes',preview:'47 reacciones.',score:9.2},{icon:'💬',title:'Debate Zustand vs Redux',preview:'Mayor participación de la semana.',score:8.7},{icon:'📁',title:'Recursos compartidos – top 3',preview:'Spring Boot, GitHub Actions, Testcontainers.',score:7.6},{icon:'❓',title:'Testing Testcontainers – Laura Gómez',preview:'21 respuestas sobre tests de integración.',score:7.4}]},
  { id:'draft-4',channel:'linkedin',channelLabel:'LinkedIn',icon:'🔵',title:'Testing en Spring Boot: Testcontainers cambia las reglas del juego',preview:'Debate sobre cómo Testcontainers simplifica los tests de integración.',content:'🧪 Testing en Spring Boot nunca fue tan sencillo.\n\nLaura Gómez planteó la pregunta y la comunidad respondió con 21 comentarios.\n\n✅ Testcontainers elimina mocks de BD: tests con BD real en Docker.\n✅ @DynamicPropertySource inyecta la URL automáticamente.\n✅ Testcontainers + GitHub Actions = CI/CD con tests reales.\n\n#SpringBoot #Testing #Java #TalentCircle',status:'published',score:7.4,createdAt:'25 abr 2025 · 19:12',week:'21–28 abr 2025',publishedAt:'26 abr 2025',sources:[]},
  { id:'draft-5',channel:'twitter',channelLabel:'Twitter / X',icon:'🐦',title:'CI/CD con GitHub Actions: del código a producción en 3 pasos',preview:'Andrés Silva compartió su flujo completo. 18 devs lo guardaron.',content:'🚀 Andrés Silva compartió su flujo CI/CD con GitHub Actions + Docker.\n\n18 devs lo guardaron en @TalentCircle.\n\nLos 3 pasos:\n1️⃣ Build y test en cada PR\n2️⃣ Imagen Docker con caché\n3️⃣ Deploy a prod solo en main\n\n#DevOps #Java #GitHubActions',status:'rejected',rejectionReason:'El tono no refleja la voz de la comunidad. Revisar.',score:7.1,createdAt:'25 abr 2025 · 19:15',week:'21–28 abr 2025',sources:[]},
  { id:'draft-6',channel:'newsletter',channelLabel:'Newsletter',icon:'📧',title:'Semana pasada: Arquitectura Hexagonal y el debate sobre DDD',preview:'La semana del 14 al 18 de abril estuvo marcada por conversaciones sobre arquitectura hexagonal.',content:'Hola comunidad TalentCircle 👋\n\nLa semana del 14 al 18 de abril fue especial.\n\n⬡ ARQUITECTURA HEXAGONAL\nRoberto Castro implementó ports & adapters en un proyecto real con Spring Boot.\n\n📐 DDD PARA PROYECTOS MEDIANOS\n¿Vale la pena DDD en equipos pequeños? La respuesta sorprendió a todos.\n\nHasta pronto, El equipo TalentCircle ✦',status:'published',score:8.8,createdAt:'18 abr 2025 · 19:00',week:'14–18 abr 2025',publishedAt:'21 abr 2025',sources:[]},
]

export const useAppStore = create((set) => ({
  isAuthenticated: false,
  currentUser: null,
  login: (user) => set({ isAuthenticated: true, currentUser: user }),
  logout: () => set({ isAuthenticated: false, currentUser: null }),
  toast: null,
  showToast: (icon, title, body) => {
    set({ toast: { icon, title, body, id: Date.now() } })
    setTimeout(() => set({ toast: null }), 3800)
  },
  modalDraftId: null,
  openModal: (id) => set({ modalDraftId: id }),
  closeModal: () => set({ modalDraftId: null }),
  drafts: INITIAL_DRAFTS,
  updateDraftContent: (id, content) =>
    set((s) => ({ drafts: s.drafts.map((d) => d.id === id ? { ...d, content } : d) })),
  updateDraftStatus: (id, status, extra = {}) =>
    set((s) => ({ drafts: s.drafts.map((d) => d.id === id ? { ...d, status, ...extra } : d) })),
  feedItems: [
    { id:'f1',initials:'CR',bg:'rgba(245,166,35,.2)',color:'#f5a623',author:'Carlos Reyes',text:'publicó un recurso sobre arquitecturas de microservicios con Spring Boot y Kubernetes',time:'hace 2h',reactions:'🔥 47 reacciones',score:'9.2'},
    { id:'f2',initials:'MV',bg:'rgba(78,205,196,.2)',color:'#4ecdc4',author:'María Valdez',text:'respondió la pregunta más popular sobre manejo de estados en React con Zustand vs Redux',time:'hace 5h',reactions:'💬 34 respuestas',score:'8.7'},
    { id:'f3',initials:'JP',bg:'rgba(255,107,138,.2)',color:'#ff6b8a',author:'Jorge Paredes',text:'compartió su repositorio de plantillas de APIs REST con Java y OpenAPI 3',time:'hace 8h',reactions:'⭐ 29 compartidos',score:'7.9'},
    { id:'f4',initials:'LG',bg:'rgba(155,89,182,.2)',color:'#bf8dd6',author:'Laura Gómez',text:'preguntó sobre mejores prácticas de testing en Spring Boot con JUnit 5 y Testcontainers',time:'hace 12h',reactions:'💬 21 respuestas',score:'7.4'},
    { id:'f5',initials:'AS',bg:'rgba(82,214,138,.2)',color:'#52d68a',author:'Andrés Silva',text:'compartió una guía de CI/CD con GitHub Actions para proyectos Java',time:'hace 1d',reactions:'🔥 18 reacciones',score:'7.1'},
  ],
  executions: [
    { id:'EXC-2025-17',week:'21–28 abr 2025',status:'completed',activities:147,drafts:6,duration:'42m 18s',triggeredBy:'Scheduler',progress:100},
    { id:'EXC-2025-16',week:'14–18 abr 2025',status:'completed',activities:119,drafts:6,duration:'38m 52s',triggeredBy:'Scheduler',progress:100},
    { id:'EXC-2025-15',week:'07–11 abr 2025',status:'failed',activities:84,drafts:0,duration:'12m 04s',triggeredBy:'Scheduler',progress:30},
    { id:'EXC-2025-14',week:'31 mar–04 abr',status:'completed',activities:103,drafts:6,duration:'45m 30s',triggeredBy:'Ana López',progress:100},
    { id:'EXC-2025-13',week:'24–28 mar 2025',status:'completed',activities:97,drafts:5,duration:'36m 11s',triggeredBy:'Scheduler',progress:100},
  ],
}))
