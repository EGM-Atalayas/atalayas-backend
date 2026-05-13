# Módulo: Notificaciones (`communication/notification`)

> **Paquete:** `com.atalayas.backend.communication`
> **Tabla BD:** `notificacion`
> **Audiencia:** Frontend, Backend
> **Última actualización:** Abril 2026

---

## ¿Qué hace este módulo?

Gestiona **notificaciones individuales** dirigidas a un usuario concreto. Hay dos formas de crearlas:

| Modo | Método | Quién lo llama | Endpoint |
|------|--------|----------------|----------|
| **Manual** | `crear()` | Admin / Admin Empresa desde el panel | `POST /api/v1/notificaciones` |
| **Interna** | `crearInterna()` | Otros servicios del backend (eventos del sistema) | Sin endpoint — API privada |

Cada usuario solo puede ver y gestionar **sus propias** notificaciones (`destinatarioId = usuarioId`).

---

## Endpoints

Base URL: `/api/v1/notificaciones`

| Método | Ruta | Rol mínimo | Descripción |
|--------|------|-----------|-------------|
| `POST` | `/notificaciones` | `ADMIN` o `ADMIN_EMPRESA` | Crear notificación manual para un usuario concreto |
| `GET` | `/notificaciones/me` | Cualquiera | Mis notificaciones paginadas (leídas + no leídas). Params: `?page=0&size=20` |
| `GET` | `/notificaciones/me/no-leidas` | Cualquiera | Solo las no leídas — para la campana del header |
| `GET` | `/notificaciones/me/contador` | Cualquiera | Número de no leídas. Respuesta: `{ "noLeidas": N }` |
| `PATCH` | `/notificaciones/{id}/leer` | Cualquiera | Marcar una notificación como leída. Solo el destinatario puede ejecutarlo |
| `PATCH` | `/notificaciones/me/leer-todas` | Cualquiera | Marcar todas las no leídas como leídas. Respuesta: `{ "actualizadas": N }` |

---

## Contratos

### `POST /notificaciones` — Crear notificación manual

**Roles:** `ROLE_ADMIN`, `ROLE_ADMIN_EMPRESA`  
`ROLE_EMPLEADO` recibe `403 Forbidden`.

**Request body (`NotificationRequest`):**
```json
{
  "destinatarioId": "550e8400-e29b-41d4-a716-446655440000",
  "tipo": "ANUNCIO",
  "mensaje": "Recuerda completar el módulo de Prevención de Riesgos antes del viernes.",
  "enlace": "/modulos/prevencion-riesgos"
}
```

| Campo | Tipo | Requerido | Validación |
|-------|------|:---------:|------------|
| `destinatarioId` | `UUID` | ✅ | `@NotNull` |
| `tipo` | `String` | ✅ | `@NotBlank`, máx. 100 caracteres |
| `mensaje` | `String` | ✅ | `@NotBlank` |
| `enlace` | `String` | ❌ | Máx. 500 caracteres |

**Response `201 Created` (`NotificationResponse`):**
```json
{
  "notificacionId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "destinatarioId": "550e8400-e29b-41d4-a716-446655440000",
  "tipo": "ANUNCIO",
  "mensaje": "Recuerda completar el módulo de Prevención de Riesgos antes del viernes.",
  "enlace": "/modulos/prevencion-riesgos",
  "leido": false,
  "creadoEn": "2026-04-28T10:00:00Z",
  "actualizadoEn": null
}
```

**Errores:**
| Código | Cuándo |
|--------|--------|
| `400` | Validación fallida (`destinatarioId` nulo, `tipo`/`mensaje` vacíos) |
| `401` | Sin token |
| `403` | `ROLE_EMPLEADO` intenta crear |

---

### `GET /notificaciones/me` — Mis notificaciones paginadas

**Query params:**

| Param | Default | Descripción |
|-------|---------|-------------|
| `page` | `0` | Número de página (base 0) |
| `size` | `20` | Elementos por página |

Devuelve una `Page<NotificationResponse>` ordenada por `creadoEn DESC` (más recientes primero).

**Response `200 OK`:**
```json
{
  "content": [
    {
      "notificacionId": "...",
      "destinatarioId": "...",
      "tipo": "BIENVENIDA",
      "mensaje": "¡Bienvenido a la plataforma!",
      "enlace": null,
      "leido": false,
      "creadoEn": "2026-04-28T09:00:00Z",
      "actualizadoEn": null
    }
  ],
  "totalElements": 12,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

> Útil para el panel de notificaciones con scroll infinito: incrementar `page` en cada scroll.

---

### `GET /notificaciones/me/no-leidas` — Solo no leídas

Devuelve `List<NotificationResponse>` (sin paginar) con todas las notificaciones donde `leido = false`, ordenadas por `creadoEn DESC`.

**Response `200 OK`:** array de `NotificationResponse` (ver estructura arriba).

> Usar para poblar el dropdown de la campana del header.

---

### `GET /notificaciones/me/contador` — Badge de la campana

**Response `200 OK`:**
```json
{ "noLeidas": 3 }
```

Si no hay notificaciones no leídas, devuelve `{ "noLeidas": 0 }`.

---

### `PATCH /notificaciones/{id}/leer` — Marcar una como leída

| Código | Cuándo |
|--------|--------|
| `200` | Notificación marcada correctamente. Devuelve `NotificationResponse` con `leido: true` |
| `400` | Ya estaba leída → `"La notificación ya estaba marcada como leída"` |
| `400` | El usuario autenticado no es el destinatario → `"No tienes permiso para marcar esta notificación"` |
| `404` | No existe notificación con ese `id` |

> **El campo `leido` es irreversible**: una vez en `true` no puede volver a `false`.

---

### `PATCH /notificaciones/me/leer-todas` — Marcar todas como leídas

Marca en una sola operación todas las notificaciones `leido = false` del usuario autenticado.
Solo afecta al usuario autenticado — nunca toca notificaciones de otros usuarios.

**Response `200 OK`:**
```json
{ "actualizadas": 5 }
```

Si no había pendientes: `{ "actualizadas": 0 }`.

---

## Tipos de notificación (`tipo`)

El campo `tipo` es un `String` libre (máx. 100 chars). Los valores canónicos del sistema son:

| Valor | Cuándo se genera | Servicio que llama `crearInterna` |
|-------|-----------------|-----------------------------------|
| `MODULO_NUEVO` | Se publica un nuevo módulo visible para la empresa | `ModuleService` |
| `CONTENIDO_COMPLETADO` | Un empleado completa un contenido | `ProgressService` |
| `ANUNCIO` | Un admin crea un anuncio relevante para el empleado | `AnnouncementService` / manual |
| `BIENVENIDA` | Usuario recién registrado o empresa aprobada | `AuthService` / `CompanyService` |
| `COMUNICADO` | Se publica un comunicado oficial de EGM | `OfficialNoticeService` |
| `CONTENIDO_ACTUALIZADO` | Se actualiza el cuerpo de un contenido (versión +1) | `ContentService` |

> El frontend usa `tipo` para elegir el icono y color del badge en la campana.  
> Valores no listados aquí son válidos (creaciones manuales de admin).

---

## API interna — `crearInterna`

Método de `NotificationService` para crear notificaciones programáticamente desde otros servicios. **No tiene endpoint REST.**

### Firma

```java
@Transactional
public NotificationResponse crearInterna(
    UUID destinatarioId,
    String tipo,
    String mensaje,
    String enlace        // puede ser null
)
```

### Uso desde otro servicio

```java
// Ejemplo en CompanyService — notificación de bienvenida al aprobar empresa
@Autowired
private NotificationService notificationService;

// Dentro de resolverSolicitud(), tras activar la empresa:
notificationService.crearInterna(
    adminEmpresa.getUsuarioId(),
    "BIENVENIDA",
    "Tu empresa \"" + empresa.getNombre() + "\" ha sido aprobada. ¡Bienvenido a Atalayas!",
    null
);
```

### Notas

- Se ejecuta dentro de la **misma transacción** del servicio llamante (no usa `REQUIRES_NEW`).
  Si la transacción padre hace rollback, la notificación también se deshace.
- Si se necesita garantizar la persistencia independientemente del resultado del padre
  (igual que `AuditService`), inyectar `NotificationService` con `@Transactional(propagation = REQUIRES_NEW)` en el punto de llamada o crear un método dedicado.
- Loguea a nivel `INFO`: `"Notificación interna creada - id={} destinatario={} tipo={}"`.

---

## Reglas de negocio

| Regla | Detalle |
|-------|---------|
| **Aislamiento** | `destinatarioId` es el único criterio de propiedad. Cada query filtra por `destinatarioId = usuarioId` del token. |
| **Irreversibilidad de lectura** | `leido` solo puede pasar de `false` a `true`. Intentar marcar una ya leída lanza `BusinessException` → `400`. |
| **Autoría del marcado** | Solo el destinatario puede marcar sus propias notificaciones. Intentar marcar la de otro usuario lanza `BusinessException` → `400` (no `403`, para no exponer existencia). |
| **Creación manual restringida** | Solo `ROLE_ADMIN` y `ROLE_ADMIN_EMPRESA` pueden crear via endpoint. `ROLE_EMPLEADO` recibe `403`. |
| **`leido` siempre `false` en creación** | El campo `leido` no se acepta en el request — siempre se inicializa a `false`. |

---

## Modelo de datos

### Tabla `notificacion`

| Columna | Tipo SQL | Nullable | Descripción |
|---------|----------|:--------:|-------------|
| `notificacion_id` | `UUID PK` | ❌ | Generado por `@GeneratedValue(UUID)` en JPA |
| `destinatario_id` | `UUID NOT NULL` | ❌ | ID del usuario destinatario (sin FK explícita en JPA) |
| `tipo` | `VARCHAR(100) NOT NULL` | ❌ | Tipo de notificación (ver tabla de tipos) |
| `mensaje` | `TEXT NOT NULL` | ❌ | Cuerpo del mensaje |
| `enlace` | `VARCHAR(500)` | ✅ | URL relativa al recurso relacionado |
| `leido` | `BOOLEAN NOT NULL DEFAULT false` | ❌ | Estado de lectura |
| `creado_en` | `TIMESTAMPTZ` | ✅ | Timestamp de creación |
| `actualizado_en` | `TIMESTAMPTZ` | ✅ | Timestamp de última modificación |

---

## Detalle técnico — `creadoEn` y `actualizadoEn`

Estos dos campos tienen **doble mecanismo de gestión** que coexisten por diseño:

### `creadoEn`

```java
// Notification.java
@PrePersist
protected void onCreate() {
    if (creadoEn == null) {
        creadoEn = OffsetDateTime.now();
    }
}
```

- **`@PrePersist` (JPA):** se ejecuta justo antes del `INSERT`. Asigna `OffsetDateTime.now()` **si el campo es `null`**.
- **Trigger en BD:** si existe un trigger `BEFORE INSERT` en PostgreSQL que asigna `NOW()`, la BD también lo establece.
- **Orden de precedencia:** el valor asignado por `@PrePersist` se envía en el `INSERT`. Si el trigger también escribe `creado_en`, prevalece el trigger (la BD sobreescribe lo que JPA mandó).
- **Resultado efectivo:** `creadoEn` siempre tendrá un valor tras el `INSERT`, sea por JPA o por el trigger. El `if (creadoEn == null)` en `@PrePersist` actúa como **fallback** — protege contra entornos sin el trigger (ej. H2 en tests).

### `actualizadoEn`

```java
// Notification.java — sin @PreUpdate
@Column(name = "actualizado_en")
private OffsetDateTime actualizadoEn;
```

- **No hay `@PreUpdate`** en la entidad.
- Es gestionado **exclusivamente por trigger en BD** (`BEFORE UPDATE SET actualizado_en = NOW()`).
- En consecuencia, el valor es `null` tras el `INSERT` hasta que se produce el primer `UPDATE` (ej. al marcar como leída).
- En tests con H2 (sin trigger), `actualizadoEn` permanecerá `null` incluso después de `save()` en un update. Esto es esperado y no indica un bug.

### Resumen

| Campo | `@PrePersist` | `@PreUpdate` | Trigger BD | Valor tras INSERT | Valor tras UPDATE |
|-------|:---:|:---:|:---:|---|---|
| `creadoEn` | ✅ (fallback) | ❌ | ✅ (override) | `OffsetDateTime` siempre | Sin cambio |
| `actualizadoEn` | ❌ | ❌ | ✅ (único) | `null` | `OffsetDateTime` en prod / `null` en H2 |

---

## Clases involucradas

| Clase | Responsabilidad |
|-------|-----------------|
| `Notification` | Entidad JPA — tabla `notificacion`. Incluye `@PrePersist` para `creadoEn` |
| `NotificationRequest` | DTO de entrada: `destinatarioId`, `tipo`, `mensaje`, `enlace` (opcional) |
| `NotificationResponse` | DTO de salida: todos los campos incluidos `enlace`, `leido`, `creadoEn`, `actualizadoEn` |
| `NotificationMapper` | Convierte entre entidad y DTOs (`toEntity`, `toResponse`) |
| `NotificationService` | Lógica de negocio: `crear`, `crearInterna`, `listarMias`, `listarMiasPaginado`, `listarMisNoLeidas`, `contarNoLeidas`, `marcarComoLeida`, `marcarTodasComoLeidas` |
| `NotificationRepository` | Queries JPA: `findByDestinatarioIdOrderByCreadoEnDesc`, `findByDestinatarioIdAndLeidoFalse...`, `countByDestinatarioIdAndLeidoFalse`, `marcarTodasComoLeidas` (bulk update) |
| `NotificationController` | REST bajo `/api/v1/notificaciones`. Seguridad con `@PreAuthorize` en `POST` |

---

## Tests de integración

Cubiertos en `NotificationControllerIT` (`@SpringBootTest`, perfil `test`, H2):

| Test | Escenario |
|------|-----------|
| `crear_adminValido_devuelve201` | Admin crea notificación → `201`, `leido=false` |
| `crear_empleado_devuelve403` | Empleado intenta crear → `403` |
| `crear_bodyInvalido_devuelve400` | Sin `destinatarioId` → `400` |
| `crear_sinToken_devuelve401` | Sin Bearer → `401` |
| `listarMias_conNotifs_devuelvePaginaConContent` | Devuelve página con 2 elementos |
| `listarMias_paginacion_respetaSize` | `?size=3` de 5 → 3 en `content`, `totalElements=5` |
| `noLeidas_soloDevuelveNoLeidas` | 1 leída + 1 no leída → solo devuelve 1 con `leido=false` |
| `contador_devuelveNumeroCorrectoDeNoLeidas` | 2 no leídas + 1 leída → `noLeidas: 2` |
| `contador_sinNotifs_devuelveCero` | Sin notificaciones → `noLeidas: 0` |
| `marcarComoLeida_propiaNoLeida_devuelve200` | Marca la propia → `leido: true` |
| `marcarComoLeida_yaLeida_devuelve400` | Ya leída → `400` con `"leída"` en message |
| `marcarComoLeida_otroUsuario_devuelve400` | Notificación ajena → `400` con `"permiso"` en message |
| `marcarComoLeida_idInexistente_devuelve404` | UUID inexistente → `404` |
| `marcarTodasComoLeidas_conPendientes_devuelveActualizadas` | 2 pendientes → `actualizadas: 2` |
| `marcarTodasComoLeidas_sinPendientes_devuelveCero` | Solo leídas → `actualizadas: 0` |
| `marcarTodasComoLeidas_soloAfectaAlUsuarioAutenticado` | Notif de admin + notif de empleado → solo afecta 1 |

