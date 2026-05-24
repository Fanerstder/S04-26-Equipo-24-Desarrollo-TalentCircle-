# Implementation Plan: draft-publication

## Overview

Completar el ciclo editorial end-to-end de TalentCircle añadiendo la publicación de borradores aprobados en los tres canales (`LINKEDIN`, `TWITTER`, `NEWSLETTER`). El trabajo se divide en tres bloques: (1) infraestructura de dominio y adaptadores backend, (2) integración en el frontend React, y (3) tests unitarios y de propiedad.

---

## Tasks

- [x] 1. Añadir dependencia jqwik y configurar variables de entorno
  - Añadir `net.jqwik:jqwik:1.9.0` con scope `test` en `backend/pom.xml`
  - Añadir las propiedades en `backend/src/main/resources/application.properties`:
    ```properties
    app.linkedin.access-token=${LINKEDIN_ACCESS_TOKEN:}
    app.linkedin.person-id=${LINKEDIN_PERSON_ID:}
    app.twitter.bearer-token=${TWITTER_BEARER_TOKEN:}
    ```
  - Documentar las tres variables nuevas en el README.md (sección "Variables de entorno requeridas")
  - _Requisitos: 3.4, 3.5, 4.5, 4.6_

- [x] 2. Crear el puerto de salida ChannelPublisherPort y la excepción ChannelPublicationException
  - [x] 2.1 Crear la interfaz ChannelPublisherPort
    - Crear `domain/port/out/ChannelPublisherPort.java` con el método `String publish(String content)`
    - Añadir Javadoc indicando que lanza ChannelPublicationException ante fallos
    - _Requisitos: 2.1, 3.1, 4.1, 5.1_
  - [x] 2.2 Crear la excepción ChannelPublicationException
    - Crear `common/exception/ChannelPublicationException.java` extendiendo RuntimeException
    - Incluir campo `int httpStatus` y getter correspondiente
    - _Requisitos: 3.3, 4.4_

- [x] 3. Completar LinkedInClientAdapter
  - [x] 3.1 Implementar LinkedInClientAdapter como ChannelPublisherPort
    - Modificar `adapter/out/linkedin/LinkedInClientAdapter.java` para implementar ChannelPublisherPort
    - Inyectar `@Value("${app.linkedin.access-token:}")` y `@Value("${app.linkedin.person-id:}")`
    - Validar tokens en el constructor y lanzar IllegalStateException si están vacíos
    - Construir el body UGC Posts y llamar a `POST https://api.linkedin.com/v2/ugcPosts` con RestTemplate
    - Retornar el `id` del post si HTTP 201; lanzar ChannelPublicationException para 4xx/5xx
    - _Requisitos: 3.1, 3.2, 3.3, 3.4, 3.5_
  - [ ]* 3.2 Escribir tests unitarios para LinkedInClientAdapter
    - Crear LinkedInClientAdapterTest con JUnit 5 + Mockito
    - Ejemplo: token vacío → IllegalStateException en construcción
    - Ejemplo: respuesta 201 con ID → retorna el ID
    - Ejemplo: respuesta 400 → lanza ChannelPublicationException
    - Ejemplo: respuesta 500 → lanza ChannelPublicationException
    - _Requisitos: 3.2, 3.3, 3.5_
  - [ ]* 3.3 Escribir test de propiedad para LinkedInClientAdapter
    - Crear LinkedInClientAdapterPropertyTest con jqwik
    - Property 8: LinkedIn retorna el ID externo del post creado — Validates: Requisito 3.2
    - Property 9 (LinkedIn): Los adaptadores lanzan excepción ante errores HTTP 4xx/5xx — Validates: Requisito 3.3

- [ ] 4. Crear TwitterClientAdapter
  - [ ] 4.1 Implementar TwitterClientAdapter
    - Crear `adapter/out/twitter/TwitterClientAdapter.java` implementando ChannelPublisherPort
    - Inyectar `@Value("${app.twitter.bearer-token:}")` y validar en constructor
    - Truncar contenido > 280 caracteres a 277 + "..." antes de enviar
    - Llamar a `POST https://api.twitter.com/2/tweets` con RestTemplate
    - Retornar `data.id` si HTTP 201; lanzar ChannelPublicationException para 4xx/5xx
    - _Requisitos: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_
  - [ ]* 4.2 Escribir tests unitarios para TwitterClientAdapter
    - Crear TwitterClientAdapterTest con JUnit 5 + Mockito
    - Ejemplo: token vacío → IllegalStateException en construcción
    - Ejemplo: contenido de 280 caracteres → no trunca
    - Ejemplo: contenido de 281 caracteres → trunca a 277 + "..."
    - Ejemplo: respuesta 201 con ID → retorna el ID
    - Ejemplo: respuesta 403 → lanza ChannelPublicationException
    - _Requisitos: 4.2, 4.3, 4.4, 4.6_
  - [ ]* 4.3 Escribir test de propiedad para TwitterClientAdapter
    - Crear TwitterClientAdapterPropertyTest con jqwik
    - Property 10: Twitter trunca contenido que supera 280 caracteres — Validates: Requisito 4.3
    - Property 9 (Twitter): Los adaptadores lanzan excepción ante errores HTTP 4xx/5xx — Validates: Requisito 4.4

- [ ] 5. Crear NewsletterPublisherAdapter
  - [ ] 5.1 Implementar NewsletterPublisherAdapter
    - Crear `adapter/out/newsletter/NewsletterPublisherAdapter.java` implementando ChannelPublisherPort
    - El método publish() es un no-op que retorna null (la publicación se completa cuando PublicationService actualiza Draft.status = PUBLISHED)
    - _Requisitos: 5.1, 5.2_
  - [ ]* 5.2 Escribir tests unitarios para NewsletterPublisherAdapter
    - Crear NewsletterPublisherAdapterTest
    - Ejemplo: publish() retorna null
    - _Requisitos: 5.1_

- [ ] 6. Refactorizar PublicationService para enrutamiento multi-canal
  - [ ] 6.1 Refactorizar el constructor y la lógica de enrutamiento
    - Modificar `application/service/PublicationService.java`
    - Reemplazar LinkedInClientPort linkedInClient por Map<Draft.Channel, ChannelPublisherPort> publishers
    - Constructor recibe LinkedInClientAdapter, TwitterClientAdapter, NewsletterPublisherAdapter y construye el mapa con Map.of(...)
    - Seleccionar effectiveContent: usar editedContent si no es nulo/vacío, content en caso contrario
    - Reemplazar IllegalArgumentException por ResourceNotFoundException (404) y IllegalStateException por ConflictException (409)
    - Añadir logging INFO en éxito y ERROR en fallo con draftId y channel
    - _Requisitos: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 5.3, 8.3_
  - [ ]* 6.2 Escribir tests unitarios para PublicationService
    - Crear PublicationServiceTest con JUnit 5 + Mockito
    - Ejemplo: borrador no encontrado → ResourceNotFoundException
    - Ejemplo: borrador con estado PENDING → ConflictException
    - Ejemplo: borrador con estado PUBLISHED → ConflictException
    - Ejemplo: publicación exitosa → Draft.status = PUBLISHED, Publication.status = SUCCESS
    - Ejemplo: fallo en adaptador → Draft.status sin cambio, Publication.status = FAILED
    - Ejemplo: publicationRepository.save() llamado tanto en éxito como en fallo
    - _Requisitos: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_
  - [ ]* 6.3 Escribir tests de propiedad para PublicationService
    - Crear PublicationServicePropertyTest con jqwik
    - Property 3: El servicio enruta al adaptador correcto según el canal — Validates: Requisito 2.1
    - Property 4: Publicación exitosa actualiza estado y persiste registro SUCCESS — Validates: Requisitos 2.2, 5.1, 8.1
    - Property 5: Fallo en canal externo preserva estado del borrador y persiste registro FAILED — Validates: Requisitos 2.3, 2.6, 8.1
    - Property 6: Publicación siempre persiste un registro Publication — Validates: Requisitos 2.6, 8.1
    - Property 7: Rechazo de borradores no-APPROVED con ConflictException — Validates: Requisito 2.5
    - Property 14: El registro Publication contiene todos los campos requeridos — Validates: Requisito 8.1

- [ ] 7. Checkpoint backend
  - Asegurarse de que todos los tests del backend pasan con mvn test
  - _Requisitos: todos los anteriores_

- [ ] 8. Añadir tipo PublicationDto en el frontend
  - [ ] 8.1 Extender src/types/api.ts con la interfaz PublicationDto
    - Añadir la interfaz con campos: id, draftId, status: 'SUCCESS' | 'FAILED', externalPostId: string | null, publishedAt: string | null, errorMessage: string | null
    - _Requisitos: 6.1, 6.2, 8.2_

- [ ] 9. Añadir función publish en draftsApi
  - [ ] 9.1 Extender src/api/draftsApi.js con la función publish
    - Añadir export const publish = (id) => apiClient.post con la ruta /api/v1/drafts/{id}/publish
    - Añadir JSDoc con tipo de retorno Promise<PublicationDto>
    - Exportar publish en el objeto default draftsApi
    - _Requisitos: 2.1, 6.3_

- [ ] 10. Añadir estado de publicación en useAppStore
  - [ ] 10.1 Extender el store con publishingDraftId y su setter
    - Añadir publishingDraftId: null al estado inicial
    - Añadir setPublishingDraftId: (id) => set({ publishingDraftId: id })
    - _Requisitos: 6.3_

- [ ] 11. Implementar botón Publicar en DraftCard
  - [ ] 11.1 Añadir handlePublish y el botón en DraftCard
    - Localizar el componente DraftCard dentro de src/pages/Drafts/
    - Importar publish de draftsApi y useAppStore
    - Añadir estado local publishLoading (useState)
    - Implementar handlePublish: llama a draftsApi.publish(draft.id), deshabilita el botón durante la llamada, muestra toast de éxito con canal si status === 'SUCCESS', toast de error con errorMessage si status === 'FAILED', actualiza el estado del borrador a PUBLISHED en éxito via updateDraftStatus
    - Renderizar el botón con icono Send + spinner solo cuando draft.status === 'APPROVED'
    - Añadir aria-label con el canal destino
    - _Requisitos: 1.1, 1.2, 1.3, 1.4, 6.1, 6.2, 6.3, 6.4_
  - [ ] 11.2 Escribir tests de propiedad para DraftCard
    - Crear src/test/properties/DraftCard.property.test.jsx con Vitest + fast-check
    - Property 1: El botón Publicar aparece si y solo si el estado es APPROVED — Validates: Requisitos 1.1, 1.2, 6.4
    - Property 2: El canal destino es visible junto al botón Publicar — Validates: Requisitos 1.3, 1.4

- [ ] 12. Implementar botón Publicar en DraftModal
  - [ ] 12.1 Añadir handlePublish y el botón en DraftModal
    - Modificar src/components/DraftModal.jsx
    - Importar publish de draftsApi
    - Reutilizar actionLoading existente o añadir estado local publishLoading
    - Implementar handlePublish con la misma lógica de feedback que DraftCard
    - Renderizar el botón en el footer del modal solo cuando status === 'APPROVED'
    - Mostrar publishedAt e externalPostId cuando el borrador está PUBLISHED
    - _Requisitos: 1.3, 6.1, 6.2, 6.3, 6.4, 8.2_

- [ ] 13. Escribir tests de propiedad para el flujo de publicación frontend
  - [ ]* 13.1 Escribir tests de propiedad para el flujo publish
    - Crear src/test/properties/publishFlow.property.test.jsx con Vitest + fast-check
    - Property 12: Feedback visual correcto según resultado de publicación — Validates: Requisitos 6.1, 6.2
    - Property 15: El modal muestra fecha y ID externo del post publicado — Validates: Requisito 8.2

- [ ] 14. Escribir tests de integración del endpoint de publicación
  - [ ]* 14.1 Crear PublicationControllerIntegrationTest
    - Usar @SpringBootTest + MockMvc con mocks de los tres adaptadores de canal
    - Verificar HTTP 401 sin token JWT
    - Verificar HTTP 403 con rol incorrecto
    - Verificar HTTP 404 con ID de borrador inexistente
    - Verificar HTTP 409 con borrador en estado no-APPROVED
    - Verificar HTTP 200 con PublicationDto status SUCCESS en publicación exitosa
    - Verificar HTTP 200 con PublicationDto status FAILED cuando el adaptador lanza excepción
    - Property 13: Usuarios sin rol EDITOR/ADMIN reciben HTTP 403 — Validates: Requisito 7.2
    - _Requisitos: 2.4, 2.5, 7.1, 7.2, 7.3, 7.4_

- [ ] 15. Checkpoint final
  - Asegurarse de que todos los tests backend y frontend pasan
  - _Requisitos: todos_

## Notes

- Tasks marked with `*` are optional property-based tests (PBT). They can be skipped if the testing framework is not yet set up.
- Tasks marked with `[-]` are partially implemented and need completion.
- Tasks marked with `[~]` are in progress.
- Backend tasks (1–7) should be completed before frontend tasks (8–15).
- The checkpoint tasks (7, 15) verify that all prior tests pass.

## Task Dependency Graph

```json
{
  "waves": [
    {
      "wave": 1,
      "tasks": ["1"],
      "description": "jqwik dependency + env vars setup"
    },
    {
      "wave": 2,
      "tasks": ["2"],
      "description": "ChannelPublisherPort interface + ChannelPublicationException",
      "dependsOn": ["1"]
    },
    {
      "wave": 3,
      "tasks": ["3", "4", "5"],
      "description": "Channel adapters: LinkedIn, Twitter, Newsletter (parallel)",
      "dependsOn": ["2"]
    },
    {
      "wave": 4,
      "tasks": ["3.2", "3.3", "4.2", "4.3", "5.2", "6"],
      "description": "Adapter unit/property tests + PublicationService refactor (parallel)",
      "dependsOn": ["3", "4", "5"]
    },
    {
      "wave": 5,
      "tasks": ["6.2", "6.3"],
      "description": "PublicationService unit + property tests",
      "dependsOn": ["6"]
    },
    {
      "wave": 6,
      "tasks": ["7"],
      "description": "Backend checkpoint — all backend tests pass",
      "dependsOn": ["6.2", "6.3"]
    },
    {
      "wave": 7,
      "tasks": ["8", "9", "10"],
      "description": "Frontend type, API function, store state (parallel)",
      "dependsOn": ["7"]
    },
    {
      "wave": 8,
      "tasks": ["11"],
      "description": "DraftCard Publish button",
      "dependsOn": ["8", "9", "10"]
    },
    {
      "wave": 9,
      "tasks": ["11.2", "12"],
      "description": "DraftCard property tests + DraftModal Publish button (parallel)",
      "dependsOn": ["11"]
    },
    {
      "wave": 10,
      "tasks": ["13", "14"],
      "description": "publishFlow property tests + PublicationControllerIntegrationTest (parallel)",
      "dependsOn": ["12"]
    },
    {
      "wave": 11,
      "tasks": ["15"],
      "description": "Final checkpoint — all backend and frontend tests pass",
      "dependsOn": ["13", "14"]
    }
  ]
}
```
