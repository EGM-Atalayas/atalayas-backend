# Módulo: Comunicación (`communication`)

> **Paquete:** `com.atalayas.backend.communication`
> **Tablas BD:** `anuncio`, `comunicado`, `notificacion`
> **Audiencia:** Frontend, Backend
> **Última actualización:** Abril 2026

---

## ¿Qué hace este módulo?

Agrupa tres tipos de comunicación distintos de la plataforma:

| Tipo | Entidad | Creado por | Dirigido a |
|---|---|---|---|
| **Anuncio** | `Announcement` | Admin empresa / Superadmin | Empleados de la empresa (o toda la plataforma si es global) |
| **Comunicado oficial** | `OfficialNotice` | Solo Superadmin (`ROLE_ADMIN`) | Todos los usuarios de la plataforma |
| **Notificación** | `Notification` | Sistema (automático) o Admin manualmente | Un usuario concreto |

---

## Entidad 1: `Announcement` (tabla `anuncio`)

Anuncios de empresa a sus empleados. Pueden ser globales (toda la plataforma) o de empresa.

| Campo | Tipo | Descripción |
|---|---|---|
| `anuncioId` | UUID (PK) | Identificador único |
| `empresaId` | UUID (nullable) | Empresa propietaria. `null` cuando `esGlobal = true` |
| `titulo` | String | Título del anuncio |
| `contenido` | TEXT | Cuerpo del anuncio |
| `esGlobal` | boolean | `true` = visible para todos los usuarios de la plataforma |
| `activo` | boolean | `false` = soft-delete |
| `creadoPor` | UUID | ID del usuario que lo creó |
| `creadoEn` | OffsetDateTime | Fecha de creación (automático) |

**Consulta de listado:** `WHERE (empresa_id = :empresaId OR es_global = true) AND activo = true`

### Endpoints — Anuncios (`/api/v1/anuncios`)

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| `POST` | `/anuncios` | `ADMIN_EMPRESA` | Crear anuncio. Solo `ADMIN` puede crear globales |
| `GET` | `/anuncios` | Cualquiera | Listar anuncios de empresa + globales |
| `PATCH` | `/anuncios/{id}/desactivar` | `ADMIN_EMPRESA` | Soft-delete |
| `DELETE` | `/anuncios/{id}` | `ADMIN_EMPRESA` | Alias de `PATCH /{id}/desactivar` |

---

## Entidad 2: `OfficialNotice` (tabla `comunicado`)

Comunicados oficiales publicados por EGM para toda la plataforma. Admiten **publicación programada** y **expiración automática**.

| Campo | Tipo | Descripción |
|---|---|---|
| `comunicadoId` | UUID (PK) | Identificador único |
| `creadoPor` | UUID | ID del superadmin que lo publicó |
| `titulo` | String | Título del comunicado |
| `mensaje` | TEXT | Contenido del comunicado |
| `imagenUrl` | String (nullable) | URL de imagen opcional |
| `fechaPublicacion` | OffsetDateTime | Fecha de publicación (puede ser futura para programar) |
| `fechaExpiracion` | OffsetDateTime (nullable) | `null` = no caduca automáticamente |
| `activo` | boolean | `false` = soft-delete inmediato |

### Endpoints — Comunicados (`/api/v1/comunicados`)

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| `POST` | `/comunicados` | `ADMIN` | Crear comunicado oficial |
| `GET` | `/comunicados` | Cualquiera | Listar vigentes. `ADMIN` ve el histórico completo |
| `PATCH` | `/comunicados/{id}/desactivar` | `ADMIN` | Desactivar comunicado |

**¿Qué ven los usuarios normales?** Solo comunicados con `activo = true` y `fechaPublicacion <= ahora()` y `(fechaExpiracion IS NULL OR fechaExpiracion > ahora())`.

**¿Qué ve el superadmin?** Todo el histórico incluyendo expirados y desactivados.

---

## Entidad 3: `Notification` (tabla `notificacion`)

Notificaciones individuales dirigidas a un usuario concreto. Se usan para la **campana de notificaciones** del header.

| Campo | Tipo | Descripción |
|---|---|---|
| `notificacionId` | UUID (PK) | Identificador único |
| `destinatarioId` | UUID | ID del usuario que recibe la notificación |
| `tipo` | String | Ver tipos abajo |
| `mensaje` | TEXT | Texto de la notificación |
| `enlace` | String (nullable) | URL opcional al recurso relacionado |
| `leido` | boolean | `false` = no leída, `true` = leída (irreversible) |
| `creadoEn` | OffsetDateTime | Fecha de creación (automático) |

### Tipos de notificación

| Valor | Cuándo se genera |
|---|---|
| `BIENVENIDA` | Al registrarse un nuevo usuario |
| `MODULO_NUEVO` | Cuando el admin publica un nuevo módulo |
| `CONTENIDO_ACTUALIZADO` | Cuando se actualiza un contenido ya visto |
| `CONTENIDO_COMPLETADO` | Cuando el empleado completa un contenido |
| `ANUNCIO` | Cuando se publica un anuncio |
| `COMUNICADO` | Cuando EGM publica un comunicado oficial |

### Endpoints — Notificaciones (`/api/v1/notificaciones`)

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| `POST` | `/notificaciones` | `ADMIN_EMPRESA` | Crear notificación manual para un usuario |
| `GET` | `/notificaciones/me` | Cualquiera | Todas mis notificaciones |
| `GET` | `/notificaciones/me/no-leidas` | Cualquiera | Solo las no leídas (para la campana) |
| `GET` | `/notificaciones/me/contador` | Cualquiera | Número de no leídas |
| `PATCH` | `/notificaciones/{id}/leer` | Cualquiera | Marcar una notificación como leída |
| `PATCH` | `/notificaciones/me/leer-todas` | Cualquiera | Marcar todas como leídas |

---

## Request / Response

### POST `/anuncios` — Crear anuncio

```json
{
  "titulo": "Reunión de equipo este viernes",
  "contenido": "El viernes 18 a las 10h tendremos reunión general.",
  "esGlobal": false
}
```
**Response `201 Created`:** datos del anuncio creado.

### POST `/comunicados` — Crear comunicado oficial

```json
{
  "titulo": "Mantenimiento del sistema",
  "mensaje": "El sábado 20 de abril habrá mantenimiento de 2h a 4h.",
  "fechaPublicacion": "2026-04-20T02:00:00Z",
  "fechaExpiracion": "2026-04-20T06:00:00Z",
  "imagenUrl": null
}
```

### GET `/notificaciones/me/contador`

**Response `200 OK`:**
```json
{ "noLeidas": 3 }
```

### PATCH `/notificaciones/me/leer-todas`

**Response `200 OK`:**
```json
{ "actualizadas": 3 }
```

---

## Diagrama de flujo — notificaciones automáticas

```
Evento en el sistema (ej. empresa aprobada)
        │
        ▼
CompanyService.aprobarEmpresa()
        │
        ├─► Activa empresa y usuarios
        ├─► Envía email de bienvenida (SMTP)
        └─► NotificationService.crear(adminEmpresa, tipo="BIENVENIDA", mensaje="...")
                │
                ▼
            BD: INSERT INTO notificacion (destinatario_id, tipo, mensaje, leido=false)

Frontend (campana del header):
        │
        └─► GET /notificaciones/me/contador  → { noLeidas: 1 }
            GET /notificaciones/me/no-leidas → lista de notificaciones
```

