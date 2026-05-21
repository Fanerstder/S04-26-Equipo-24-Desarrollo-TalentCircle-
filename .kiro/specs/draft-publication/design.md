# Design Document — draft-publication

## Overview

Esta feature completa el ciclo editorial end-to-end de TalentCircle añadiendo la capacidad de publicar borradores aprobados directamente desde el Panel Editorial. El flujo es: un editor presiona "Publicar" en un borrador con estado `APPROVED`, el backend invoca el adaptador del canal correspondiente (`LINKEDIN`, `TWITTER` o `NEWSLETTER`), registra el resultado en la entidad `Publication` y actualiza el estado del borrador a `PUBLISHED` si la operación fue exitosa.

El backend ya cuenta con la estructura base: `PublicationService`, el modelo `Publication`, `LinkedInClientAdapter` (incompleto), `DraftController` con el endpoint `POST /api/v1/drafts/{id}/publish`, y `NewsletterController` con el endpoint público `GET /api/v1/public/newsletters`. Esta feature completa los adaptadores faltantes (Twitter, Newsletter) y el adaptador de LinkedIn, extiende el `PublicationService` para enrutar por canal, y añade el botón "Publicar" con feedback visual en el frontend React.

### Decisiones de diseño clave

- **Enrutamiento por canal en `PublicationService`**: el servicio usa un `Map<Channel, ChannelPublisherPort>` para delegar a cada adaptador, evitando un `switch` frágil y facilitando agregar canales futuros.
- **Puerto genérico `ChannelPublisherPort`**: los tres adaptadores implementan la misma interfaz, lo que permite testear el servicio con mocks independientes del canal.
- **Persistencia incondicional de `Publication`**: el registro se guarda tanto en éxito como en fallo, garantizando trazabilidad completa.
- **Contenido efectivo en Newsletter**: el adaptador usa `editedContent` si existe, `content` en caso contrario, alineado con la lógica ya presente en `NewsletterController`.
- **Truncado en Twitter**: el adaptador trunca a 277 caracteres + "..." antes de enviar, sin modificar el borrador original.
- **`RestTemplate` para HTTP externo**: el proyecto ya usa Spring Boot sin WebFlux; se usa `RestTemplate` con `@Value` para tokens, consistente con el patrón de `LinkedInClientAdapter`.

## Architecture

La feature sigue la arquitectura hexagonal (ports & adapters) ya establecida en el proyecto.

```
┌─────────────────────────────────────────────────────────────────────┐
│  Adapter IN (Web)                                                   │
│  DraftController  POST /api/v1/drafts/{id}/publish                 │
└────────────────────────────┬────────────────────────────────────────┘
                             │ PublicationUseCase.publishDraft(id)
┌────────────────────────────▼────────────────────────────────────────┐
│  Application Service                                                │
│  PublicationService                                                 │
│   ├─ Valida estado APPROVED                                         │
│   ├─ Enruta al ChannelPublisherPort correcto según canal            │
│   ├─ Persiste Publication (éxito o fallo)                           │
│   └─ Actualiza Draft.status → PUBLISHED (solo en éxito)            │
└──────┬──────────────────────────────────────────────────────────────┘
       │ ChannelPublisherPort (port out)
       ├──────────────────────────────────────────────────────────────
       │  LinkedInClientAdapter   → POST https://api.linkedin.com/v2/ugcPosts
       │  TwitterClientAdapter    → POST https://api.twitter.com/2/tweets
       │  NewsletterPublisherAdapter → DraftRepository.save(PUBLISHED)
       └──────────────────────────────────────────────────────────────

┌─────────────────────────────────────────────────────────────────────┐
│  Frontend (React 18 + TypeScript)                                   │
│  DraftCard / DraftModal                                             │
│   ├─ Muestra PublishButton solo si status === 'APPROVED'            │
│   ├─ Llama a draftsApi.publish(id)                                  │
│   ├─ Deshabilita botón + spinner durante la llamada                 │
│   └─ Muestra toast de éxito/error y actualiza estado local          │
└─────────────────────────────────────────────────────────────────────┘
```

### Flujo de publicación (happy path)

```
Editor → [Publish Button] → POST /api/v1/drafts/{id}/publish
  → PublicationService.publishDraft(id)
    → DraftRepository.findById(id)          // 404 si no existe
    → validar status == APPROVED            // 409 si no
    → ChannelPublisherPort.publish(content) // según draft.channel
    → Publication{status=SUCCESS, externalPostId, publishedAt}
    → Draft.status = PUBLISHED
    → publicationRepository.save(publication)
    → draftRepository.save(draft)
  ← PublicationDto{status="SUCCESS", externalPostId, publishedAt}
← 200 OK
  → Frontend: toast éxito, badge PUBLISHED, ocultar botón
```

### Flujo de publicación (fallo en canal externo)

```
  → ChannelPublisherPort.publish(content) lanza excepción
    → Publication{status=FAILED, errorMessage}
    → Draft.status NO cambia (sigue APPROVED)
    → publicationRepository.save(publication)
  ← PublicationDto{status="FAILED", errorMessage}
← 200 OK (el fallo es de negocio, no HTTP)
  → Frontend: toast error con mensaje, botón vuelve a habilitarse
```

## Components and Interfaces

### Backend — Nuevo puerto de salida: `ChannelPublisherPort`

```java
// domain/port/out/ChannelPublisherPort.java
package com.talentcircle.domain.port.out;

/**
 * Puerto genérico para publicar contenido en un canal externo.
 * Cada adaptador (LinkedIn, Twitter, Newsletter) implementa esta interfaz.
 */
public interface ChannelPublisherPort {
    /**
     * Publica el contenido en el canal externo.
     * @param content Texto a publicar (ya procesado: truncado, seleccionado, etc.)
     * @return ID externo del post/tweet creado, o null para Newsletter.
     * @throws ChannelPublicationException si la publicación falla.
     */
    String publish(String content);
}
```

### Backend — Excepción de publicación

```java
// common/exception/ChannelPublicationException.java
public class ChannelPublicationException extends RuntimeException {
    private final int httpStatus;
    public ChannelPublicationException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }
    public int getHttpStatus() { return httpStatus; }
}
```

### Backend — `PublicationService` (refactorizado)

El servicio actual solo soporta LinkedIn. Se refactoriza para enrutar por canal:

```java
@Service
@Transactional
public class PublicationService implements PublicationUseCase {

    private final DraftRepository draftRepository;
    private final PublicationRepository publicationRepository;
    private final Map<Draft.Channel, ChannelPublisherPort> publishers;

    // Constructor recibe los tres adaptadores y construye el mapa
    public PublicationService(DraftRepository draftRepository,
                              PublicationRepository publicationRepository,
                              LinkedInClientAdapter linkedInAdapter,
                              TwitterClientAdapter twitterAdapter,
                              NewsletterPublisherAdapter newsletterAdapter) {
        this.draftRepository = draftRepository;
        this.publicationRepository = publicationRepository;
        this.publishers = Map.of(
            Draft.Channel.LINKEDIN,   linkedInAdapter,
            Draft.Channel.TWITTER,    twitterAdapter,
            Draft.Channel.NEWSLETTER, newsletterAdapter
        );
    }

    @Override
    public PublicationDto publishDraft(String draftId) {
        Draft draft = draftRepository.findById(draftId)
            .orElseThrow(() -> new ResourceNotFoundException("Draft not found: " + draftId));

        if (draft.getStatus() != Draft.DraftStatus.APPROVED) {
            throw new ConflictException("Only APPROVED drafts can be published");
        }

        ChannelPublisherPort publisher = publishers.get(draft.getChannel());
        String effectiveContent = draft.getEditedContent() != null
            ? draft.getEditedContent() : draft.getContent();

        Publication publication = new Publication();
        publication.setDraft(draft);
        publication.setChannel(Publication.Channel.valueOf(draft.getChannel().name()));
        publication.setRetryCount(0);

        try {
            String externalPostId = publisher.publish(effectiveContent);
            publication.setExternalPostId(externalPostId);
            publication.setStatus(Publication.PublicationStatus.SUCCESS);
            publication.setPublishedAt(LocalDateTime.now());
            draft.setStatus(Draft.DraftStatus.PUBLISHED);
            draftRepository.save(draft);
            log.info("Draft {} published on {} — externalId: {}", draftId, draft.getChannel(), externalPostId);
        } catch (Exception e) {
            publication.setStatus(Publication.PublicationStatus.FAILED);
            publication.setErrorMessage(e.getMessage());
            log.error("Failed to publish draft {} on {}: {}", draftId, draft.getChannel(), e.getMessage());
        }

        publication = publicationRepository.save(publication);
        return toDto(publication);
    }
}
```

### Backend — `LinkedInClientAdapter` (completado)

```java
// adapter/out/linkedin/LinkedInClientAdapter.java
@Component
public class LinkedInClientAdapter implements ChannelPublisherPort, LinkedInClientPort {

    private final String accessToken;
    private final String personId;
    private final RestTemplate restTemplate;

    public LinkedInClientAdapter(
            @Value("${app.linkedin.access-token:}") String accessToken,
            @Value("${app.linkedin.person-id:}") String personId) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("LINKEDIN_ACCESS_TOKEN is not configured");
        }
        if (personId == null || personId.isBlank()) {
            throw new IllegalStateException("LINKEDIN_PERSON_ID is not configured");
        }
        this.accessToken = accessToken;
        this.personId = personId;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String publish(String content) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
            "author", "urn:li:person:" + personId,
            "lifecycleState", "PUBLISHED",
            "specificContent", Map.of(
                "com.linkedin.ugc.ShareContent", Map.of(
                    "shareCommentary", Map.of("text", content),
                    "shareMediaCategory", "NONE"
                )
            ),
            "visibility", Map.of("com.linkedin.ugc.MemberNetworkVisibility", "PUBLIC")
        );

        ResponseEntity<Map> response = restTemplate.exchange(
            "https://api.linkedin.com/v2/ugcPosts",
            HttpMethod.POST,
            new HttpEntity<>(body, headers),
            Map.class
        );

        if (response.getStatusCode() != HttpStatus.CREATED) {
            throw new ChannelPublicationException(
                "LinkedIn API error: " + response.getStatusCode(), response.getStatusCode().value());
        }
        return (String) response.getBody().get("id");
    }
}
```

### Backend — `TwitterClientAdapter` (nuevo)

```java
// adapter/out/twitter/TwitterClientAdapter.java
@Component
public class TwitterClientAdapter implements ChannelPublisherPort {

    private static final int MAX_TWEET_LENGTH = 280;
    private static final int TRUNCATE_AT = 277;

    private final String bearerToken;
    private final RestTemplate restTemplate;

    public TwitterClientAdapter(@Value("${app.twitter.bearer-token:}") String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) {
            throw new IllegalStateException("TWITTER_BEARER_TOKEN is not configured");
        }
        this.bearerToken = bearerToken;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String publish(String content) {
        String tweetText = content.length() > MAX_TWEET_LENGTH
            ? content.substring(0, TRUNCATE_AT) + "..."
            : content;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of("text", tweetText);

        ResponseEntity<Map> response = restTemplate.exchange(
            "https://api.twitter.com/2/tweets",
            HttpMethod.POST,
            new HttpEntity<>(body, headers),
            Map.class
        );

        if (response.getStatusCode() != HttpStatus.CREATED) {
            throw new ChannelPublicationException(
                "Twitter API error: " + response.getStatusCode(), response.getStatusCode().value());
        }
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        return (String) data.get("id");
    }
}
```

### Backend — `NewsletterPublisherAdapter` (nuevo)

```java
// adapter/out/newsletter/NewsletterPublisherAdapter.java
@Component
public class NewsletterPublisherAdapter implements ChannelPublisherPort {

    // El Newsletter no llama a una API externa: el borrador ya está en la BD
    // y NewsletterController lo expone en GET /api/v1/public/newsletters
    // cuando su status es PUBLISHED. El PublicationService ya actualiza el status.
    // Este adaptador es un no-op que retorna null como externalPostId.

    @Override
    public String publish(String content) {
        // La publicación de newsletter se completa cuando PublicationService
        // actualiza Draft.status = PUBLISHED. No hay llamada externa.
        return null;
    }
}
```

### Frontend — `draftsApi.js` (extensión)

Se añade la función `publish` al módulo existente:

```javascript
// src/api/draftsApi.js — nueva función
/**
 * Publica un borrador aprobado en su canal correspondiente.
 * @param {string} id UUID del borrador
 * @returns {Promise<PublicationDto>}
 */
export const publish = (id) =>
  apiClient.post(`/api/v1/drafts/${id}/publish`).then((res) => res.data)
```

### Frontend — `DraftCard` (extensión)

Se añade el botón "Publicar" para borradores con estado `APPROVED`:

```jsx
// En DraftCard, dentro de cardActions, después del bloque PENDING:
{draft.status === 'APPROVED' && (
  <button
    className={`${styles['btn-sm']} ${styles.publish}`}
    onClick={handlePublish}
    disabled={publishLoading}
    aria-label={`Publicar en ${channelLabel}`}
  >
    <Send size={11} />
    {publishLoading ? <Loader2 size={11} className={styles.spinner} /> : 'Publicar'}
  </button>
)}
```

### Frontend — `DraftModal` (extensión)

Se añade el botón "Publicar" en el footer del modal cuando el borrador está `APPROVED`:

```jsx
// En el footer del modal, después del botón "Aprobar":
{status === 'APPROVED' && (
  <button
    className="btn btn-primary"
    onClick={handlePublish}
    disabled={actionLoading || detailLoading}
    aria-label={`Publicar en ${channelLabel}`}
  >
    <Send size={14} />
    {actionLoading ? '…' : `Publicar en ${channelLabel}`}
  </button>
)}
```

### Frontend — `useAppStore.js` (extensión)

Se añade el estado de publicación al store:

```javascript
// Nuevas acciones en useAppStore
publishingDraftId: null,
setPublishingDraftId: (id) => set({ publishingDraftId: id }),
```

## Data Models

### Entidad `Publication` (existente, sin cambios de esquema)

```java
@Entity
@Table(name = "publications")
public class Publication extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "draft_id")
    private Draft draft;                    // FK al borrador

    @Enumerated(EnumType.STRING)
    private Channel channel;                // LINKEDIN | TWITTER | NEWSLETTER

    @Enumerated(EnumType.STRING)
    private PublicationStatus status;       // SUCCESS | FAILED | RETRYING

    private String externalPostId;          // ID del post en LinkedIn/Twitter; null para Newsletter
    private LocalDateTime publishedAt;      // Timestamp de publicación exitosa
    private String errorMessage;            // Mensaje de error si status=FAILED
    private Integer retryCount;             // Contador de reintentos (futuro)

    public enum Channel { LINKEDIN, TWITTER, NEWSLETTER }
    public enum PublicationStatus { SUCCESS, FAILED, RETRYING }
}
```

### Entidad `Draft` (existente, sin cambios de esquema)

El campo `status` ya incluye `PUBLISHED`. No se requieren migraciones de esquema.

### DTO de respuesta `PublicationDto` (existente en `PublicationUseCase`)

```java
record PublicationDto(
    String id,
    String draftId,
    String status,          // "SUCCESS" | "FAILED"
    String externalPostId,  // null para Newsletter o en caso de fallo
    String publishedAt,     // ISO-8601, null en caso de fallo
    String errorMessage     // null en caso de éxito
) {}
```

### Tipos TypeScript frontend (`src/types/api.ts` — extensión)

```typescript
export interface PublicationDto {
  id: string
  draftId: string
  status: 'SUCCESS' | 'FAILED'
  externalPostId: string | null
  publishedAt: string | null
  errorMessage: string | null
}
```

### Variables de entorno requeridas (nuevas)

| Variable | Descripción | Requerida para |
|---|---|---|
| `LINKEDIN_ACCESS_TOKEN` | OAuth 2.0 access token de LinkedIn | Canal LINKEDIN |
| `LINKEDIN_PERSON_ID` | URN del perfil LinkedIn (`urn:li:person:{id}`) | Canal LINKEDIN |
| `TWITTER_BEARER_TOKEN` | Bearer Token de Twitter API v2 | Canal TWITTER |

Configuración en `application.properties`:
```properties
app.linkedin.access-token=${LINKEDIN_ACCESS_TOKEN:}
app.linkedin.person-id=${LINKEDIN_PERSON_ID:}
app.twitter.bearer-token=${TWITTER_BEARER_TOKEN:}
```

## Correctness Properties

*Una propiedad es una característica o comportamiento que debe ser verdadero en todas las ejecuciones válidas del sistema — esencialmente, una declaración formal sobre lo que el sistema debe hacer. Las propiedades sirven como puente entre especificaciones legibles por humanos y garantías de corrección verificables por máquinas.*

### Property 1: El botón Publicar aparece si y solo si el estado es APPROVED

*Para cualquier* borrador con cualquier estado (`PENDING`, `APPROVED`, `REJECTED`, `PUBLISHED`), el componente `DraftCard` debe mostrar el botón "Publicar" exactamente cuando el estado es `APPROVED`, y ocultarlo en todos los demás estados.

**Validates: Requirements 1.1, 1.2, 6.4**

### Property 2: El canal destino es visible junto al botón Publicar

*Para cualquier* borrador con estado `APPROVED` y cualquier canal (`LINKEDIN`, `TWITTER`, `NEWSLETTER`), el componente debe mostrar la etiqueta del canal correcto junto al botón "Publicar".

**Validates: Requirements 1.3, 1.4**

### Property 3: El servicio enruta al adaptador correcto según el canal

*Para cualquier* borrador con estado `APPROVED` y cualquier canal válido, `PublicationService.publishDraft()` debe invocar exactamente el adaptador correspondiente a ese canal (LinkedIn → `LinkedInClientAdapter`, Twitter → `TwitterClientAdapter`, Newsletter → `NewsletterPublisherAdapter`).

**Validates: Requirements 2.1**

### Property 4: Publicación exitosa actualiza estado y persiste registro SUCCESS

*Para cualquier* borrador con estado `APPROVED` y cualquier respuesta exitosa del adaptador de canal, `PublicationService` debe: (a) actualizar `Draft.status` a `PUBLISHED`, (b) persistir un objeto `Publication` con `status=SUCCESS`, `externalPostId` no nulo (excepto Newsletter), y `publishedAt` no nulo.

**Validates: Requirements 2.2, 5.1, 8.1**

### Property 5: Fallo en canal externo preserva estado del borrador y persiste registro FAILED

*Para cualquier* borrador con estado `APPROVED` y cualquier excepción lanzada por el adaptador de canal, `PublicationService` debe: (a) NO modificar `Draft.status` (debe seguir siendo `APPROVED`), (b) persistir un objeto `Publication` con `status=FAILED` y `errorMessage` igual al mensaje de la excepción.

**Validates: Requirements 2.3, 2.6, 8.1**

### Property 6: Publicación siempre persiste un registro Publication

*Para cualquier* borrador con estado `APPROVED`, independientemente del resultado del adaptador (éxito o fallo), `publicationRepository.save()` debe ser invocado exactamente una vez con un objeto `Publication` que contenga el `draftId` y el `channel` correctos.

**Validates: Requirements 2.6, 8.1**

### Property 7: Rechazo de borradores no-APPROVED con HTTP 409

*Para cualquier* borrador con estado distinto de `APPROVED` (`PENDING`, `REJECTED`, `PUBLISHED`), llamar a `publishDraft()` debe lanzar una `ConflictException` (HTTP 409).

**Validates: Requirements 2.5**

### Property 8: LinkedIn retorna el ID externo del post creado

*Para cualquier* respuesta HTTP 201 de la API de LinkedIn con cualquier ID de post, `LinkedInClientAdapter.publish()` debe retornar exactamente ese ID.

**Validates: Requirements 3.2**

### Property 9: Los adaptadores de canal lanzan excepción ante errores HTTP 4xx/5xx

*Para cualquier* código HTTP en el rango 400–599 retornado por la API externa (LinkedIn o Twitter), el adaptador correspondiente debe lanzar una `ChannelPublicationException` que contenga el código de estado HTTP recibido.

**Validates: Requirements 3.3, 4.4**

### Property 10: Twitter trunca contenido que supera 280 caracteres

*Para cualquier* string de contenido con longitud mayor a 280 caracteres, `TwitterClientAdapter` debe enviar a la API un texto de exactamente 280 caracteres que termina en "...".

**Validates: Requirements 4.3**

### Property 11: Newsletter usa editedContent cuando existe, content en caso contrario

*Para cualquier* borrador con canal `NEWSLETTER`, el contenido efectivo publicado debe ser `editedContent` si ese campo no es nulo ni vacío, o `content` en caso contrario.

**Validates: Requirements 5.3**

### Property 12: Feedback visual correcto según resultado de publicación

*Para cualquier* respuesta `SUCCESS` del servicio con cualquier canal, el frontend debe llamar a `showToast` con un mensaje que incluya el nombre del canal. *Para cualquier* respuesta `FAILED` con cualquier mensaje de error, el frontend debe llamar a `showToast` con ese mensaje de error y el estado del borrador debe permanecer como `APPROVED`.

**Validates: Requirements 6.1, 6.2**

### Property 13: Usuarios sin rol EDITOR/ADMIN reciben HTTP 403

*Para cualquier* usuario autenticado con un rol distinto de `EDITOR` y `ADMIN`, la llamada a `POST /api/v1/drafts/{id}/publish` debe retornar HTTP 403.

**Validates: Requirements 7.2**

### Property 14: El registro Publication contiene todos los campos requeridos

*Para cualquier* intento de publicación (éxito o fallo) con cualquier borrador y canal, el objeto `Publication` persistido debe contener: `draftId` no nulo, `channel` correcto, `status` correcto, y según el resultado: `publishedAt` + `externalPostId` (éxito) o `errorMessage` (fallo).

**Validates: Requirements 8.1**

### Property 15: El modal muestra fecha y ID externo del post publicado

*Para cualquier* borrador publicado con cualquier `publishedAt` e `externalPostId`, el `DraftModal` debe renderizar ambos valores visibles en la interfaz.

**Validates: Requirements 8.2**

## Error Handling

### Backend

| Situación | Excepción | HTTP |
|---|---|---|
| Borrador no encontrado | `ResourceNotFoundException` | 404 |
| Borrador no está en estado `APPROVED` | `ConflictException` | 409 |
| Token JWT ausente o inválido | Spring Security | 401 |
| Usuario sin rol `EDITOR`/`ADMIN` | Spring Security `@PreAuthorize` | 403 |
| API externa retorna 4xx/5xx | `ChannelPublicationException` (capturada internamente) | 200 (fallo de negocio) |
| Variable de entorno de token no configurada | `IllegalStateException` en construcción del bean | 500 (startup) |

El `GlobalExceptionHandler` existente ya maneja `ResourceNotFoundException` y `ConflictException`. Se añade el manejo de `ChannelPublicationException` si escapa del servicio (aunque normalmente es capturada internamente).

Los fallos de canal externo se devuelven como HTTP 200 con `PublicationDto{status="FAILED"}` porque son fallos de negocio esperados, no errores del servidor. El frontend distingue por el campo `status`.

### Frontend

| Situación | Comportamiento |
|---|---|
| Publicación exitosa | Toast verde: "Borrador publicado exitosamente en [canal]", badge cambia a PUBLISHED, botón desaparece |
| Publicación fallida (FAILED) | Toast rojo con `errorMessage`, estado del borrador permanece APPROVED, botón se rehabilita |
| Error HTTP (4xx/5xx del servidor) | Toast rojo genérico del interceptor de `apiClient`, botón se rehabilita |
| Durante la llamada | Botón deshabilitado + spinner, overlay del modal no cierra |

### Validación de configuración al arranque

Los adaptadores de LinkedIn y Twitter validan sus tokens en el constructor. Si las variables de entorno no están configuradas, el contexto de Spring falla al arrancar con un mensaje claro. Esto es intencional: es mejor fallar rápido que descubrir el problema en tiempo de ejecución.

Para entornos donde no se usan todos los canales, se puede usar el perfil de Spring para condicionar los beans:

```java
@Component
@ConditionalOnProperty("app.linkedin.access-token")
public class LinkedInClientAdapter implements ChannelPublisherPort { ... }
```

## Testing Strategy

### Enfoque dual: tests unitarios + tests de propiedad

Esta feature tiene lógica de negocio pura (enrutamiento por canal, selección de contenido, truncado de Twitter, transformación de estado) que es ideal para property-based testing. Los tests de integración con APIs externas se limitan a 1-3 ejemplos con mocks HTTP.

### Backend — Tests unitarios (JUnit 5 + Mockito)

**`PublicationServiceTest`**
- Ejemplo: borrador no encontrado → `ResourceNotFoundException`
- Ejemplo: borrador con estado `PENDING` → `ConflictException`
- Ejemplo: borrador con estado `PUBLISHED` → `ConflictException`
- Ejemplo: publicación exitosa → `Draft.status = PUBLISHED`, `Publication.status = SUCCESS`
- Ejemplo: fallo en adaptador → `Draft.status` sin cambio, `Publication.status = FAILED`
- Ejemplo: `publicationRepository.save()` llamado en éxito y en fallo

**`LinkedInClientAdapterTest`**
- Ejemplo: token vacío → `IllegalStateException` en construcción
- Ejemplo: respuesta 201 con ID → retorna el ID
- Ejemplo: respuesta 400 → lanza `ChannelPublicationException`
- Ejemplo: respuesta 500 → lanza `ChannelPublicationException`

**`TwitterClientAdapterTest`**
- Ejemplo: token vacío → `IllegalStateException` en construcción
- Ejemplo: contenido de 280 caracteres → no trunca
- Ejemplo: contenido de 281 caracteres → trunca a 277 + "..."
- Ejemplo: respuesta 201 con ID → retorna el ID
- Ejemplo: respuesta 403 → lanza `ChannelPublicationException`

**`NewsletterPublisherAdapterTest`**
- Ejemplo: `publish()` retorna null (no hay ID externo)

### Backend — Tests de propiedad (JUnit 5 + jqwik)

El proyecto usa Java 21 + Spring Boot 3.x. Se añade `jqwik` como librería de PBT:

```xml
<!-- pom.xml -->
<dependency>
    <groupId>net.jqwik</groupId>
    <artifactId>jqwik</artifactId>
    <version>1.9.0</version>
    <scope>test</scope>
</dependency>
```

Cada test de propiedad se ejecuta con mínimo 100 iteraciones (configuración por defecto de jqwik).

**`PublicationServicePropertyTest`** — Feature: draft-publication

```java
// Property 3: Enrutamiento correcto por canal
// Feature: draft-publication, Property 3: El servicio enruta al adaptador correcto según el canal
@Property
void publishDraft_routesToCorrectAdapter(@ForAll Draft.Channel channel) {
    // Genera un borrador APPROVED con el canal dado
    // Verifica que se invoca el adaptador correspondiente
}

// Property 4 + 5: Estado correcto según resultado del adaptador
// Feature: draft-publication, Property 4: Publicación exitosa actualiza estado y persiste registro SUCCESS
@Property
void publishDraft_onSuccess_updatesDraftAndPersistsSuccessPublication(
        @ForAll @StringLength(min=1, max=5000) String content) { ... }

// Feature: draft-publication, Property 5: Fallo en canal externo preserva estado del borrador
@Property
void publishDraft_onFailure_preservesDraftStatusAndPersistsFailedPublication(
        @ForAll @StringLength(min=1, max=200) String errorMessage) { ... }

// Property 6: Persistencia incondicional
// Feature: draft-publication, Property 6: Publicación siempre persiste un registro Publication
@Property
void publishDraft_alwaysPersistsPublication(@ForAll Draft.Channel channel,
        @ForAll boolean adapterSucceeds) { ... }

// Property 7: Rechazo de estados no-APPROVED
// Feature: draft-publication, Property 7: Rechazo de borradores no-APPROVED con HTTP 409
@Property
void publishDraft_nonApprovedStatus_throwsConflict(
        @ForAll @From("nonApprovedStatuses") Draft.DraftStatus status) { ... }
```

**`TwitterClientAdapterPropertyTest`**

```java
// Property 10: Truncado de Twitter
// Feature: draft-publication, Property 10: Twitter trunca contenido que supera 280 caracteres
@Property
void publish_contentOver280_isTruncatedTo280(@ForAll @StringLength(min=281, max=10000) String content) {
    String sent = capturePublishedContent(content);
    assertThat(sent).hasSize(280);
    assertThat(sent).endsWith("...");
}

// Property 9: Excepción ante errores HTTP
// Feature: draft-publication, Property 9: Los adaptadores lanzan excepción ante errores HTTP 4xx/5xx
@Property
void publish_httpError_throwsChannelPublicationException(
        @ForAll @IntRange(min=400, max=599) int httpStatus) { ... }
```

**`LinkedInClientAdapterPropertyTest`**

```java
// Property 8: Retorna ID externo
// Feature: draft-publication, Property 8: LinkedIn retorna el ID externo del post creado
@Property
void publish_success_returnsExternalPostId(@ForAll @StringLength(min=1, max=100) String postId) { ... }

// Property 9 (LinkedIn): Excepción ante errores HTTP
@Property
void publish_httpError_throwsChannelPublicationException(
        @ForAll @IntRange(min=400, max=599) int httpStatus) { ... }
```

**`NewsletterPublisherAdapterPropertyTest`**

```java
// Property 11: Selección de contenido efectivo
// Feature: draft-publication, Property 11: Newsletter usa editedContent cuando existe
@Property
void publish_usesEditedContentWhenPresent(
        @ForAll @StringLength(min=1) String editedContent,
        @ForAll @StringLength(min=1) String originalContent) { ... }
```

### Frontend — Tests de propiedad (Vitest + fast-check)

El frontend ya tiene `fast-check` instalado (visible en `node_modules`). Se usa con Vitest.

Cada test de propiedad se configura con `{ numRuns: 100 }`.

**`DraftCard.property.test.jsx`**

```javascript
// Feature: draft-publication, Property 1: El botón Publicar aparece si y solo si APPROVED
it('shows publish button iff status is APPROVED', () => {
  fc.assert(fc.property(
    fc.constantFrom('PENDING', 'APPROVED', 'REJECTED', 'PUBLISHED'),
    (status) => {
      render(<DraftCard draft={{ ...mockDraft, status }} onStatusChange={() => {}} />)
      const btn = screen.queryByRole('button', { name: /publicar/i })
      if (status === 'APPROVED') expect(btn).toBeInTheDocument()
      else expect(btn).not.toBeInTheDocument()
    }
  ), { numRuns: 100 })
})

// Feature: draft-publication, Property 2: El canal destino es visible junto al botón
it('shows channel label next to publish button for APPROVED drafts', () => {
  fc.assert(fc.property(
    fc.constantFrom('LINKEDIN', 'TWITTER', 'NEWSLETTER'),
    (channel) => {
      render(<DraftCard draft={{ ...mockDraft, status: 'APPROVED', channel }} onStatusChange={() => {}} />)
      expect(screen.getByText(CHANNEL_LABELS[channel])).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /publicar/i })).toBeInTheDocument()
    }
  ), { numRuns: 100 })
})
```

**`publishFlow.property.test.jsx`**

```javascript
// Feature: draft-publication, Property 12: Feedback visual correcto según resultado
it('shows success toast and updates status to PUBLISHED on SUCCESS', () => {
  fc.assert(fc.property(
    fc.constantFrom('LINKEDIN', 'TWITTER', 'NEWSLETTER'),
    async (channel) => {
      // Mock draftsApi.publish → { status: 'SUCCESS', ... }
      // Verificar showToast llamado con mensaje que incluye channel
      // Verificar que el estado del borrador cambia a PUBLISHED
    }
  ), { numRuns: 100 })
})

it('shows error toast and keeps APPROVED status on FAILED', () => {
  fc.assert(fc.property(
    fc.string({ minLength: 1, maxLength: 200 }),
    async (errorMessage) => {
      // Mock draftsApi.publish → { status: 'FAILED', errorMessage }
      // Verificar showToast llamado con errorMessage
      // Verificar que el estado del borrador sigue siendo APPROVED
    }
  ), { numRuns: 100 })
})
```

### Tests de integración (backend)

- `PublicationControllerIntegrationTest`: verifica el endpoint `POST /api/v1/drafts/{id}/publish` con `@SpringBootTest` y mocks de los adaptadores de canal.
- Verifica HTTP 401 sin token, HTTP 403 con rol incorrecto, HTTP 404 con ID inexistente, HTTP 409 con borrador no-APPROVED, HTTP 200 con publicación exitosa.

### Tests de integración (frontend)

- `Drafts.test.jsx` (extensión): verifica que el botón "Publicar" aparece en la lista para borradores APPROVED y no aparece para otros estados.
- `DraftModal.test.jsx` (extensión): verifica que el botón "Publicar" aparece en el modal para borradores APPROVED.
