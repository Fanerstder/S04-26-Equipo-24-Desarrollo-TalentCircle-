# Requirements Document

## Introduction

Esta feature agrega la capacidad de publicar borradores aprobados directamente desde el Panel Editorial de TalentCircle. Una vez que un borrador alcanza el estado `APPROVED`, el editor verá un botón "Publicar" que dispara la publicación en el canal correspondiente: LinkedIn, Twitter o Newsletter. El sistema registra el resultado de cada intento de publicación y actualiza el estado del borrador a `PUBLISHED` cuando la operación es exitosa.

El backend ya cuenta con `PublicationService`, el modelo `Publication` y el adaptador `LinkedInClientAdapter` (pendiente de implementación completa). Esta feature completa el ciclo editorial end-to-end: generación → revisión → aprobación → **publicación**.

## Glossary

- **Draft**: Borrador de contenido generado por IA, asociado a un canal y con un ciclo de vida de estados (`PENDING`, `APPROVED`, `REJECTED`, `PUBLISHED`).
- **Publication**: Registro de un intento de publicación de un borrador en un canal externo. Contiene estado, ID externo del post y mensaje de error si aplica.
- **Publication_Service**: Servicio de aplicación (`PublicationService.java`) responsable de orquestar la publicación de borradores en canales externos.
- **Editorial_Panel**: Interfaz React del panel de revisión editorial donde los editores gestionan borradores.
- **Publish_Button**: Botón de acción en el Editorial_Panel que inicia el flujo de publicación de un borrador aprobado.
- **Channel**: Canal de publicación destino. Valores válidos: `LINKEDIN`, `TWITTER`, `NEWSLETTER`.
- **LinkedIn_Adapter**: Adaptador de salida (`LinkedInClientAdapter.java`) que implementa la integración con la API de LinkedIn v2.
- **Twitter_Adapter**: Adaptador de salida que implementa la integración con la API de Twitter/X v2.
- **Newsletter_Adapter**: Adaptador de salida que publica el contenido de newsletter en la landing page pública.
- **DraftStatus**: Enumeración de estados de un borrador: `PENDING`, `APPROVED`, `REJECTED`, `PUBLISHED`.
- **PublicationStatus**: Enumeración de estados de una publicación: `SUCCESS`, `FAILED`, `RETRYING`.
- **Editor**: Usuario autenticado con rol `EDITOR` o `ADMIN` que opera el Editorial_Panel.

---

## Requirements

### Requirement 1: Visibilidad del botón Publicar en borradores aprobados

**User Story:** Como Editor, quiero ver un botón "Publicar" en los borradores con estado `APPROVED`, para poder iniciar la publicación en el canal correspondiente sin salir del Panel Editorial.

#### Acceptance Criteria

1. WHEN el Editorial_Panel carga la lista de borradores, THE Editorial_Panel SHALL mostrar el botón Publish_Button únicamente en los borradores cuyo estado sea `APPROVED`.
2. WHILE un borrador tiene estado distinto de `APPROVED`, THE Editorial_Panel SHALL ocultar el Publish_Button para ese borrador.
3. WHEN el Editorial_Panel carga el detalle de un borrador con estado `APPROVED`, THE Editorial_Panel SHALL mostrar el Publish_Button junto al canal destino del borrador.
4. THE Editorial_Panel SHALL mostrar el canal destino (`LINKEDIN`, `TWITTER` o `NEWSLETTER`) como etiqueta visible junto al Publish_Button.

---

### Requirement 2: Publicación de un borrador aprobado

**User Story:** Como Editor, quiero presionar el botón "Publicar" en un borrador aprobado, para que el sistema lo publique automáticamente en el canal correspondiente y registre el resultado.

#### Acceptance Criteria

1. WHEN el Editor activa el Publish_Button de un borrador con estado `APPROVED`, THE Publication_Service SHALL invocar el adaptador del canal correspondiente al borrador para publicar su contenido.
2. WHEN la publicación en el canal externo es exitosa, THE Publication_Service SHALL actualizar el estado del borrador a `PUBLISHED` y registrar un objeto `Publication` con estado `SUCCESS`, el ID externo del post y la fecha y hora de publicación.
3. WHEN la publicación en el canal externo falla, THE Publication_Service SHALL registrar un objeto `Publication` con estado `FAILED` y el mensaje de error recibido, sin modificar el estado del borrador.
4. IF el borrador solicitado no existe, THEN THE Publication_Service SHALL retornar un error con código HTTP 404.
5. IF el borrador tiene un estado distinto de `APPROVED`, THEN THE Publication_Service SHALL retornar un error con código HTTP 409 indicando que solo borradores `APPROVED` pueden publicarse.
6. THE Publication_Service SHALL persistir el objeto `Publication` en la base de datos independientemente del resultado de la publicación (éxito o fallo).

---

### Requirement 3: Publicación en LinkedIn

**User Story:** Como Editor, quiero publicar borradores del canal `LINKEDIN` directamente en LinkedIn, para distribuir el contenido de la comunidad en esa red social.

#### Acceptance Criteria

1. WHEN el Publication_Service procesa un borrador con canal `LINKEDIN`, THE LinkedIn_Adapter SHALL enviar el contenido del borrador a la API de LinkedIn v2 mediante el endpoint `POST /ugcPosts`.
2. WHEN la API de LinkedIn v2 responde con código HTTP 201, THE LinkedIn_Adapter SHALL retornar el ID externo del post creado al Publication_Service.
3. IF la API de LinkedIn v2 responde con un código de error HTTP (4xx o 5xx), THEN THE LinkedIn_Adapter SHALL lanzar una excepción con el código de estado y el cuerpo de la respuesta como mensaje de error.
4. THE LinkedIn_Adapter SHALL autenticarse usando el `access_token` configurado en la variable de entorno `LINKEDIN_ACCESS_TOKEN`.
5. IF la variable de entorno `LINKEDIN_ACCESS_TOKEN` está vacía o ausente, THEN THE LinkedIn_Adapter SHALL lanzar una excepción de configuración antes de intentar la llamada a la API.

---

### Requirement 4: Publicación en Twitter/X

**User Story:** Como Editor, quiero publicar borradores del canal `TWITTER` en Twitter/X, para distribuir el contenido de la comunidad en esa red social.

#### Acceptance Criteria

1. WHEN el Publication_Service procesa un borrador con canal `TWITTER`, THE Twitter_Adapter SHALL enviar el contenido del borrador a la API de Twitter v2 mediante el endpoint `POST /2/tweets`.
2. WHEN la API de Twitter v2 responde con código HTTP 201, THE Twitter_Adapter SHALL retornar el ID externo del tweet creado al Publication_Service.
3. IF el contenido del borrador supera 280 caracteres, THEN THE Twitter_Adapter SHALL truncar el contenido a 277 caracteres y agregar "..." antes de enviarlo a la API.
4. IF la API de Twitter v2 responde con un código de error HTTP (4xx o 5xx), THEN THE Twitter_Adapter SHALL lanzar una excepción con el código de estado y el cuerpo de la respuesta como mensaje de error.
5. THE Twitter_Adapter SHALL autenticarse usando el Bearer Token configurado en la variable de entorno `TWITTER_BEARER_TOKEN`.
6. IF la variable de entorno `TWITTER_BEARER_TOKEN` está vacía o ausente, THEN THE Twitter_Adapter SHALL lanzar una excepción de configuración antes de intentar la llamada a la API.

---

### Requirement 5: Publicación de Newsletter

**User Story:** Como Editor, quiero publicar borradores del canal `NEWSLETTER` para que queden disponibles en la landing page pública de TalentCircle.

#### Acceptance Criteria

1. WHEN el Publication_Service procesa un borrador con canal `NEWSLETTER`, THE Newsletter_Adapter SHALL marcar el borrador como publicado en la base de datos con estado `PUBLISHED`.
2. WHEN un borrador de tipo `NEWSLETTER` es publicado, THE Newsletter_Adapter SHALL hacer disponible su contenido a través del endpoint público `GET /api/v1/public/newsletters`.
3. THE Newsletter_Adapter SHALL usar el contenido editado (`editedContent`) del borrador si existe; en caso contrario, SHALL usar el contenido original (`content`).

---

### Requirement 6: Feedback visual del resultado de publicación

**User Story:** Como Editor, quiero recibir retroalimentación visual inmediata sobre el resultado de la publicación, para saber si el borrador fue publicado exitosamente o si ocurrió un error.

#### Acceptance Criteria

1. WHEN el Publication_Service retorna una respuesta con estado `SUCCESS`, THE Editorial_Panel SHALL mostrar una notificación de éxito con el texto "Borrador publicado exitosamente en [canal]" y actualizar el estado del borrador a `PUBLISHED` en la interfaz.
2. WHEN el Publication_Service retorna una respuesta con estado `FAILED`, THE Editorial_Panel SHALL mostrar una notificación de error con el mensaje de error recibido y mantener el estado del borrador como `APPROVED`.
3. WHILE el Editorial_Panel espera la respuesta del Publication_Service, THE Editorial_Panel SHALL deshabilitar el Publish_Button y mostrar un indicador de carga.
4. WHEN el estado del borrador cambia a `PUBLISHED` en la interfaz, THE Editorial_Panel SHALL ocultar el Publish_Button para ese borrador.

---

### Requirement 7: Seguridad y autorización de la acción de publicar

**User Story:** Como administrador del sistema, quiero que solo usuarios con rol `EDITOR` o `ADMIN` puedan publicar borradores, para proteger la integridad del contenido publicado.

#### Acceptance Criteria

1. THE Publication_Service SHALL requerir que el usuario autenticado tenga rol `EDITOR` o `ADMIN` para ejecutar la operación de publicación.
2. IF el usuario autenticado no tiene rol `EDITOR` ni `ADMIN`, THEN THE Publication_Service SHALL retornar un error con código HTTP 403.
3. THE Publication_Service SHALL requerir un token JWT válido en el encabezado `Authorization` de cada solicitud de publicación.
4. IF el token JWT está ausente o es inválido, THEN THE Publication_Service SHALL retornar un error con código HTTP 401.

---

### Requirement 8: Registro y trazabilidad de publicaciones

**User Story:** Como administrador del sistema, quiero que cada intento de publicación quede registrado con su resultado, para poder auditar el historial de publicaciones y diagnosticar fallos.

#### Acceptance Criteria

1. THE Publication_Service SHALL crear un registro `Publication` por cada intento de publicación, incluyendo: ID del borrador, canal, estado del intento, fecha y hora, ID externo del post (si aplica) y mensaje de error (si aplica).
2. WHEN el Editor consulta el detalle de un borrador publicado, THE Editorial_Panel SHALL mostrar la fecha y hora de publicación y el ID externo del post.
3. THE Publication_Service SHALL registrar en el log de la aplicación cada intento de publicación con nivel `INFO` para éxitos y nivel `ERROR` para fallos, incluyendo el ID del borrador y el canal.
