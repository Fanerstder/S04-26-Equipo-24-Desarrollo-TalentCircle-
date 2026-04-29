# 📋 Agenda - TalentCircle Content Pipeline

## 🎯 Qué es este proyecto

Pipeline de contenido comunitario automatizado para TalentCircle. Construido con Java 21 + Spring Boot 3.x + PostgreSQL + React 18 + TypeScript. Automatiza la recolección de actividad, análisis con IA y generación de borradores por canal (Newsletter, LinkedIn, Twitter).

---

## 📂 Estructura de documentación

| Carpeta | Contenido |
|---------|-----------|
| `specs/` | Documentos de diseño, requisitos y tareas |
| `.github/ISSUE_TEMPLATE/` | Templates automáticos de GitHub |

---

## 🔄 Flujo de trabajo

```
Epic → Module → Task → PR Task → PR Module → PR Final → dev
```

### Niveles

| Nivel | Descripción | Template |
|-------|-------------|----------|
| **Epic** | Proyecto grande o feature principal | `01-epic.md` |
| **Module** | Submódulo dentro de un Epic | `02-module.md` |
| **Task** | Tarea individual | `03-task.md` |

---

## 🌿 Convenciones de ramas

```
dev
  └── feat/talentcircle              ← Rama Epic
        ├── feat/talentcircle/auth         ← Rama Module
        │     └── task/auth/login        ← Rama Task
        ├── feat/talentcircle/collector
        ├── feat/talentcircle/ai-analyzer
        ├── feat/talentcircle/draft-generator
        ├── feat/talentcircle/editorial
        └── feat/talentcircle/admin
```

| Tipo | Prefijo | Ejemplo |
|------|---------|---------|
| Feature/Epic | `feat/` | `feat/talentcircle` |
| Módulo | `feat/` | `feat/talentcircle/collector` |
| Task | `task/` | `task/talentcircle/collector/activity` |

---

## 📝 Cómo usar los templates

### 1. Crear un Epic

```bash
gh issue create --template epic.md
```

O usar `specs/requirements.md` como referencia.

### 2. Crear un Module

```bash
gh issue create --template module.md
```

O usar `specs/design.md` para detalles técnicos.

### 3. Crear una Task

```bash
gh issue create --template task.md
```

O usar `specs/tasks.md` para el desglose completo.

### 4. Crear un PR

| Tipo | Formato de título | Closes |
|------|-------------------|--------|
| Task PR | `task/module/name: Descripción` | `Closes #X` |
| Module PR | `PR Módulo: Nombre del módulo` | `Closes #Y` |
| Final PR | `PR Final: Nombre del proyecto` | `Closes #1` |

---

## ✅ Módulos del proyecto

| Módulo | Estado | Rama |
|--------|--------|------|
| 🔐 Infraestructura Base | ✅ | `feat/talentcircle/setup` |
| 🔑 Autenticación JWT | ✅ | `feat/talentcircle/auth` |
| 📡 Community Collector | ⏳ | `feat/talentcircle/collector` |
| 🤖 AI Analyzer | ⏳ | `feat/talentcircle/ai-analyzer` |
| 📝 Draft Generator | ⏳ | `feat/talentcircle/draft-generator` |
| 📋 Editorial Panel | ⏳ | `feat/talentcircle/editorial` |
| 📤 Publication Service | ⏳ | `feat/talentcircle/publication` |
| ⚙️ Admin Panel | ⏳ | `feat/talentcircle/admin` |
| 🎨 Frontend React | ⏳ | `feat/talentcircle/frontend` |

---

## 🤖 Skills Disponibles

Las skills se auto-cargon según el contexto detectado. Están en `setup-ai-skills/skills/`.

### SDD (Spec-Driven Development)

| Skill | Trigger | Descripción |
|-------|---------|-------------|
| sdd-init | "sdd init", "iniciar sdd" | Inicializar SDD en el proyecto |
| sdd-explore | Explorar, investigar, pensar | Investigación antes de comprometerse |
| sdd-propose | Crear propuesta | Intent, scope, approach |
| sdd-spec | Escribir specs | Requisitos y escenarios |
| sdd-design | Diseño técnico | Arquitectura y decisiones |
| sdd-tasks | Tasks, tareas | Desglose en checklist |
| sdd-apply | Implementar, código | Escribir código siguiendo specs |
| sdd-verify | Verificar, validate | Validar implementación vs specs |
| sdd-archive | Archivar, completed | Sincronizar y archivar |

### Framework/Library

| Skill | Trigger | Descripción |
|-------|---------|-------------|
| react-18 | Componentes React | React 18 + TypeScript |
| tailwind-4 | Tailwind, styling | Tailwind CSS 4 patterns |
| zustand | Zustand, state | Zustand state management |
| typescript | TypeScript code | TypeScript strict patterns |

### Project Tools

| Skill | Trigger | Descripción |
|-------|---------|-------------|
| github-issue-generator | Crear Epic, Module | Generador de issues/PRs |
| skill-creator | Crear skill | Crear nuevas AI skills |
| java-testing | Tests en Java | JUnit 5 + Mockito patterns |

### Ejemplo de uso

```
/sdd-init                    → Inicializar SDD
/sdd-explore auth-flow       → Investigar feature
/sdd-new drafts-api          → Explore + Propose
/sdd-ff drafts-api          → Fast-forward: propose → spec → design → tasks
/sdd-apply                   → Implementar tasks
/sdd-verify                  → Verificar contra specs
```

---

## 🧠 Memoria y Persistencia (Engram)

**REGLA OBLIGATORIA: SIEMPRE buscar en engram antes de cada pedido del usuario.**

### Cuándo buscar automáticamente

| Situación | Qué buscar |
|-----------|-----------|
| **Inicio de sesión nueva** | mem_context + mem_search por project |
| **Cualquier pedido del usuario** | mem_search con keywords del proyecto |
| **Keywords del proyecto** | pipeline, content, drafts, ai, linkedin |
| **Trabajo en módulo existente** | Decisiones/artefactos previos |

### Qué buscar

```javascript
// Siempre ejecutar antes de actuar
mem_search(query: "{keywords del pedido}", project: "talentcircle-pipeline")
```

### Qué guardar

| Cuándo | Función | Ejemplo |
|--------|---------|---------|
| Arquitectura/Decisiones | `mem_save` tipo "decision" | "Elegimos hexagonal architecture porque..." |
| Bugs fixed/discovered | `mem_save` tipo "bugfix"/"discovery" | "Fix N+1 query en drafts" |
| Fin de sesión | `mem_session_summary` | Resumen completo |
| Commit realizado | `mem_save` tipo "pattern" | "Add pipeline orchestrator" |

### Proyecto keywords para búsqueda

```
pipeline, content, drafts, ai, llm, openai, claude, linkedin, 
community, collector, analyzer, generator, newsletter, twitter,
hexagonal, java21, spring-boot, react18, typescript
```

---

## 🔗 Enlaces útiles

- 📄 [Design Document](./specs/design.md) - Arquitectura técnica completa
- 📃 [Requirements Document](./specs/requirements.md) - Requisitos funcionales y no funcionales
- 📋 [Tasks Document](./specs/tasks.md) - Plan de implementación detallado
- 🏗️ [Estructura de Paquetes](./specs/design.md#estructura-de-paquetes) - Arquitectura hexagonal

---

## 📝 Convenciones de Commits

Usar **Conventional Commits**:

```
<tipo>(<scope>): descripción
```

```
feat(auth): add JWT authentication
fix(drafts): resolve N+1 query issue
docs(specs): update design document
chore(deps): upgrade spring boot version
refactor(api): simplify error handling
```

### Tipos permitidos

| Tipo | Uso |
|------|-----|
| `feat` | Nueva funcionalidad |
| `fix` | Corrección de bug |
| `docs` | Documentación |
| `chore` | Mantenimiento, dependencias |
| `refactor` | Refactoring sin cambio de funcionalidad |
| `test` | Tests |
| `style` | Formato, style (no lógica) |
| `perf` | Mejoras de performance |
| `ci` | Configuración de CI/CD |

### Reglas

- **Descripción en imperativo**: "add" no "added", "fix" no "fixed"
- **Scope opcional**: usar el nombre del módulo/componente
- **Sin punto final** en la descripción
- **Máximo 72 caracteres** en la primera línea
- **Si hay body**, separar con línea en blanco

---

## 📊 Sprint Planning

| Sprint | Fechas | Objetivos | Módulos |
|--------|---------|-----------|---------|
| **Sprint 0** | Semana 1 | Setup y Arquitectura Base | Setup, Infraestructura |
| **Sprint 1** | Semanas 2-3 | Autenticación y Config Fuentes | Auth, Admin |
| **Sprint 2** | Semanas 4-5 | Pipeline de Recolección | Community Collector |
| **Sprint 3** | Semanas 6-7 | Análisis IA y Generación | AI Analyzer, Draft Generator |
| **Sprint 4** | Semanas 8-9 | Panel Editorial | Editorial Panel |
| **Sprint 5** | Semanas 10-11 | Publicación y Exportación | Publication Service |
| **Sprint 6** | Semanas 12-13 | Admin Panel y Observabilidad | Admin, Monitoring |
| **Sprint 7** | Semana 14 | Testing, Hardening y Demo | Integration Tests, Deploy |

---

## 🎯 Definition of Done (DoD)

Para considerar una tarea/módulo como completado:

- [ ] Código implementado siguiendo arquitectura hexagonal
- [ ] Tests unitarios escritos (cobertura >= 80%)
- [ ] Tests de integración pasando (si aplica)
- [ ] Tests de propiedad validados (si aplica)
- [ ] Documentación actualizada (specs/)
- [ ] Code review realizado
- [ ] PR mergeado a `dev`
- [ ] Variables de entorno documentadas
- [ ] Cumple con RNFs aplicables

---

## 🚨 Puntos de Atención

1. **⚠️ LLM Costs**: Monitorear tokens usados, configurar límites en `PipelineConfig`
2. **⚠️ LinkedIn API**: Verificar límites de rate y políticas de contenido
3. **⚠️ Múltiples Fuentes**: Implementar una fuente a la vez, probar bien antes de agregar otra
4. **⚠️ WebSocket**: Configurar correctamente para notificaciones en tiempo real
5. **⚠️ Docker**: Asegurar que todas las variables de entorno estén documentadas
6. **⚠️ JWT Security**: Tokens de 8 horas, refresh tokens de 7 días, rotación en cada refresh

---

## 🏃 Quick Start

### Prerrequisitos

- Java 21
- Maven 3.9+
- PostgreSQL 16
- Redis 7+
- Node.js 18+ (para frontend)
- Docker & Docker Compose

### Levantar el proyecto

```bash
# Clonar el proyecto
git clone <repo-url>
cd "TalentCircle App"

# Levantar servicios con Docker
docker-compose up -d

# Ejecutar backend (Spring Boot)
cd backend
mvn spring-boot:run

# Ejecutar frontend (React)
cd frontend
npm install
npm run dev
```

### Variables de entorno requeridas

```env
# Base de datos
DATABASE_URL=jdbc:postgresql://localhost:5432/talentcircle
DATABASE_USER=talentcircle
DATABASE_PASSWORD=talentcircle

# JWT
JWT_SECRET=min-32-chars-secret-key-here

# LLM
OPENAI_API_KEY=sk-...
ANTHROPIC_API_KEY=sk-ant-...
LLM_PROVIDER=openai

# LinkedIn
LINKEDIN_ACCESS_TOKEN=...
LINKEDIN_PERSON_ID=...

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Frontend
FRONTEND_URL=http://localhost:3000
```

---

*Última actualización: 28 de abril de 2026*
