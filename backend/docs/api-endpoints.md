# Atalayas API — Documentación de Endpoints
**Base URL:** `http://localhost:8080/api/v1`
**Versión:** 1.0
**Autenticación:** Cookie HttpOnly `accessToken` (JWT) + opcionalmente cabecera `Authorization: Bearer <token>`
---
## Swagger UI
| Recurso | URL |
|---------|-----|
| Interfaz interactiva | `GET /swagger-ui.html` |
| Spec OpenAPI (JSON) | `GET /v3/api-docs` |
> La ruta de Swagger es pública (sin autenticación previa). El esquema de seguridad registrado
> en Swagger es **cookieAuth** (`apiKey in cookie`, nombre `accessToken`).
> Para probar endpoints protegidos desde Swagger: inicia sesión primero via `/auth/login` —
> la cookie se setea automáticamente en el navegador.
---
## Roles de la plataforma
| Rol | Descripción |
|-----|-------------|
| `ROLE_ADMIN` | Superadmin EGM — acceso total a toda la plataforma |
| `ROLE_ADMIN_EMPRESA` | Administrador de una empresa específica — scope limitado a su empresa |
| `ROLE_EMPLEADO` | Empleado — acceso de lectura/consumo dentro de su empresa |
---
## Códigos de respuesta comunes
| Código | Significado |
|--------|-------------|
| `200` | OK |
| `201` | Creado correctamente |
| `204` | Sin contenido (operación exitosa sin body) |
| `400` | Error de validación o regla de negocio (`BusinessException`) |
| `401` | Sin sesión activa / token inválido |
| `403` | Sin permisos para este recurso |
| `404` | Recurso no encontrado (`ResourceNotFoundException`) |
---
## 1. Autenticación · `/api/v1/auth`
> Todos los endpoints de auth son **públicos** salvo `GET /me`.
> Los tokens se envían como **cookies HttpOnly** (`accessToken`, `refreshToken`) y también en el body.
| Método | Ruta | Auth | Descripción |
|--------|------|:---:|-------------|
| `POST` | `/auth/register` | ❌ | Registrar nuevo usuario. Devuelve tokens en cookies + body. |
| `POST` | `/auth/login` | ❌ | Iniciar sesión. Devuelve tokens en cookies + body. |
| `POST` | `/auth/refresh-token` | ❌ ¹ | Renueva el access token usando la cookie `refreshToken`. |
| `POST` | `/auth/logout` | ❌ | Invalida (limpia) las cookies de tokens. |
| `GET`  | `/auth/me` | ✅ | Datos del usuario autenticado actualmente + `accessToken` activo en el body. Usar al iniciar la app para restaurar la sesión tras una recarga. |
| `POST` | `/auth/forgot-password` | ❌ | Solicita recuperación de contraseña. Envía email con enlace. Siempre devuelve `200` para no revelar si el email existe. |
| `POST` | `/auth/reset-password` | ❌ | Restablece la contraseña con el token recibido por email. `400` si el token es inválido, expirado o ya usado. |
¹ Requiere la cookie `refreshToken` válida.
**Expiración de tokens:**
- `accessToken`: **1 hora**
- `refreshToken`: **7 días**

**Flujo de restauración de sesión en el frontend (recarga de página):**
```
1. Al iniciar la app, llamar a GET /auth/me con credentials: 'include'
   → 200: restaurar estado con los datos recibidos (incluyendo accessToken)
   → 401: el accessToken expiró → llamar a POST /auth/refresh-token (con credentials: 'include')
          → 200: sesión restaurada con nuevos tokens
          → 400/401: sesión expirada → redirigir al login
```
---
## 2. Usuarios · `/api/v1/users`
| Método | Ruta | Rol mínimo | Descripción |
|--------|------|-----------|-------------|
| `POST` | `/users` | `ADMIN_EMPRESA` | Crear usuario. ADMIN_EMPRESA crea en su propia empresa (no puede asignar `ROLE_ADMIN`). ADMIN puede crear en cualquier empresa (`empresaId` obligatorio). El campo `rolId` es un UUID. |
| `GET` | `/users/me` | Cualquiera | Perfil completo del usuario autenticado (`UserProfileResponse`). |
| `PATCH` | `/users/me` | Cualquiera | Actualizar perfil propio. Campos opcionales: `nombre`, `apellidos`, `puestoTrabajo`, `bio`, `telefono`, `avatarUrl`, `bannerUrl`, `disponibilidad`, preferencias de notificación y `modoOscuro`. |
| `POST` | `/users/me/password` | Cualquiera | Cambiar contraseña propia. Requiere `passwordActual`, `passwordNueva`, `passwordConfirmar`. |
| `POST` | `/users/me/avatar` | Cualquiera | Subir avatar propio (`multipart/form-data`, campo `file`). Devuelve `{ "avatarUrl": "..." }`. |
| `GET` | `/users/{id}` | `ADMIN_EMPRESA` | Obtener usuario por ID. ADMIN_EMPRESA solo ve su empresa (devuelve `404` si es de otra, no `403`). |
| `GET` | `/users` | `ADMIN_EMPRESA` | Listar usuarios. ADMIN_EMPRESA ve su empresa; ADMIN ve todos. |
| `DELETE` | `/users/{id}/desactivar` | `ADMIN_EMPRESA` | Soft-delete de usuario (activo = false). Devuelve `404` si el usuario es de otra empresa. |

**`POST /users` — reglas de seguridad por rol:**
| Quién llama | `empresaId` en body | Roles asignables | Resultado |
|---|---|---|---|
| `ROLE_ADMIN_EMPRESA` | Ignorado (usa el suyo) | `ROLE_EMPLEADO`, `ROLE_ADMIN_EMPRESA` | `201` |
| `ROLE_ADMIN_EMPRESA` | — | `ROLE_ADMIN` | `403` |
| `ROLE_ADMIN` | Obligatorio | Cualquiera | `201` |
| `ROLE_ADMIN` | Ausente | — | `400` |
---
## 3. Empresas · `/api/v1/empresas`
| Método | Ruta | Rol / Auth | Descripción |
|--------|------|-----------|-------------|
| `POST` | `/empresas/solicitud` | ❌ Pública | Solicitar alta de empresa. Crea la empresa en estado `PENDIENTE` y su usuario admin inactivo. |
| `GET` | `/empresas/aprobadas` | ❌ Pública | Listar empresas aprobadas y activas (selector de registro). |
| `GET` | `/empresas` | `ADMIN` | Listar todas las empresas. Incluye `nombre`, `apellidos` y `emailAdmin` del administrador de cada empresa. |
| `GET` | `/empresas/pendientes` | `ADMIN` | Listar empresas en estado `PENDIENTE`. |
| `GET` | `/empresas/solicitudes` | `ADMIN` | Listar solicitudes pendientes con nombre y email del admin provisional. |
| `PATCH` | `/empresas/{id}/estado` | `ADMIN` | Cambiar estado de una empresa activa (ver tabla de transiciones). No permite rechazar. |
| `PATCH` | `/empresas/{id}/solicitud` | `ADMIN` | Aprobar o rechazar una solicitud. Body: `{ "accion": "aprobar" \| "rechazar" }`. |
| `PATCH` | `/empresas/{id}/activacion` | `ADMIN` | Toggle `activo` de una empresa aprobada y todos sus usuarios. |
| `POST` | `/empresas/{id}/reenviar-email` | `ADMIN` | Reenvía el email de aprobación al admin cuando `emailEnviado = false`. Devuelve `502` si SMTP falla. |

### `PATCH /empresas/{id}/estado` — Transiciones
**Body:** `{ "nuevoEstado": "APROBADA" | "PAUSADA" }`

| Desde \ Hacia | `APROBADA` | `PAUSADA` | Rechazo (hard delete) |
|---|:---:|:---:|:---:|
| **`PENDIENTE`** | ✅ via `/{id}/solicitud` | ❌ | ✅ via `/{id}/solicitud` |
| **`APROBADA`** | ❌ | ✅ | ❌ |
| **`PAUSADA`** | ✅ | ❌ | ❌ |

| Transición | `company.activo` | `emailEnviado` | Usuarios | Email | Notif. interna | Audit log |
|---|:---:|:---:|---|---|:---:|:---:|
| `PENDIENTE → APROBADA` | `true` | `true` tras envío | Se activan (`activo = true`) | ✉ Bienvenida (async, post-commit) | ✅ | ✅ `success` |
| `PENDIENTE → (rechazo)` | — *(borrado físico)* | — | Eliminados de BD | ✉ Rechazo (async, post-commit) | ❌ | ✅ `warning` |
| `APROBADA → PAUSADA` | `false` | sin cambio | Se desactivan (`activo = false`) | Ninguno | ❌ | ❌ |
| `PAUSADA → APROBADA` | `true` | sin cambio | Se reactivan (`activo = true`) | Ninguno | ❌ | ❌ |

> ⚠️ El **rechazo** elimina físicamente la empresa y sus usuarios de la BD (hard delete). Es irreversible.
> Los emails se envían **siempre después del commit** vía `@TransactionalEventListener(AFTER_COMMIT) + @Async` — un fallo SMTP nunca revierte el cambio de estado en BD.
> Si `emailEnviado = false` en una empresa APROBADA, el superadmin puede reenviar el email manualmente con `POST /{id}/reenviar-email`.
> El audit log de aprobación y rechazo se persiste en transacción independiente (`REQUIRES_NEW`) — siempre se graba aunque falle el envío de email.
> Transiciones prohibidas o no-op devuelven `400 Bad Request` con mensaje descriptivo.

### `POST /empresas/{id}/reenviar-email` — reenvío manual

**Body:** `{ "tipo": "aprobacion" }` _(único valor en v1)_

| Código | Situación |
|--------|-----------|
| `200`  | Email enviado y `email_enviado = true` persistido |
| `400`  | `tipo` inválido o empresa no está en estado `APROBADA` |
| `404`  | Empresa no encontrada |
| `502`  | Fallo SMTP (capturado por `GlobalExceptionHandler`) |
---
## 4. Dashboard · `/api/v1/dashboard`
| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| `GET` | `/dashboard/admin/resumen` | `ADMIN_EMPRESA` | Métricas de la empresa: usuarios activos e inactivos. |
| `GET` | `/dashboard/admin/actividad?limit=5` | `ADMIN_EMPRESA` | Actividad reciente real de la empresa (progreso + módulos nuevos). |
| `GET` | `/dashboard/superadmin/resumen` | `ADMIN` | Métricas globales de toda la plataforma (legado). |
| `GET` | `/dashboard/superadmin?limit=10` | `ADMIN` | Dashboard completo: métricas, incidencias y actividad reciente. Query param `limit` opcional (por defecto `10`). |
| `GET` | `/dashboard/superadmin/graficas` | `ADMIN` | Datos para los tres gráficos: evolución mensual, sectores y estadísticas de módulos. |

**`GET /dashboard/admin/actividad?limit=5` — actividad reciente de empresa:**

Devuelve los últimos `limit` eventos (por defecto 5) ordenados por `timestamp DESC`. Agrega 5 fuentes y deduplica eventos `logro` vs `completado` para el mismo `(usuario, módulo)`.

```json
[
  { "tipo": "completado", "texto": "Ana García completó «Prevención de Riesgos»",       "timestamp": "2026-04-24T10:15:00Z" },
  { "tipo": "inicio",     "texto": "Carlos Ruiz inició «Protección de Datos»",           "timestamp": "2026-04-24T09:58:00Z" },
  { "tipo": "logro",      "texto": "María López obtuvo el 100% en Onboarding",           "timestamp": "2026-04-24T09:00:00Z" },
  { "tipo": "grupo",      "texto": "5 empleados completaron «Habilidades Comunicación»", "timestamp": "2026-04-24T08:30:00Z" },
  { "tipo": "nuevo",      "texto": "Nuevo módulo «Excel Avanzado» publicado",             "timestamp": "2026-04-23T16:00:00Z" }
]
```

| Tipo | Origen | Regla |
|------|--------|-------|
| `completado` | `trazabilidad_lectura` | `completado = true` (excluye los que ya son `logro`) |
| `inicio` | `trazabilidad_lectura` | `completado = false AND tiempo_segundos > 0` |
| `logro` | `trazabilidad_lectura` | Empleado completó **todos** los contenidos activos del módulo |
| `grupo` | `trazabilidad_lectura` | ≥ 2 empleados completaron el mismo módulo el mismo día |
| `nuevo` | `modulo` | Módulo con `activo = true` propio de la empresa o global |

> El campo `timestamp` es **ISO 8601**. El frontend calcula el tiempo relativo.

**`GET /dashboard/superadmin?limit=10` — estructura de respuesta:**
```json
{
  "empresasAdheridas": 24,
  "empresasNuevasMes": 2,
  "empleadosRegistrados": 310,
  "empleadosNuevosMes": 15,
  "modulosPublicados": 8,
  "incidenciasAbiertas": 3,
  "incidenciasCriticas": 1,
  "actividadReciente": [
    { "id": 101, "texto": "Nueva empresa adherida: Sprinter S.L.", "tiempo": "Hace 10 minutos", "tipo": "success" },
    { "id": 100, "texto": "Solicitud rechazada: Empresa Ejemplo S.A.", "tiempo": "Hace 2 horas",  "tipo": "error"   },
    { "id": 99,  "texto": "Incidencia crítica abierta: caída de servicio", "tiempo": "Ayer",      "tipo": "error"   },
    { "id": 98,  "texto": "Nuevo empleado registrado en Famosa", "tiempo": "12 abr 2025",         "tipo": "info"    }
  ]
}
```

El campo `tiempo` es calculado en backend según la antigüedad del evento:

| Valor | Cuándo |
|-------|--------|
| `"Ahora mismo"` | < 1 minuto |
| `"Hace N minuto(s)"` | 1–59 minutos |
| `"Hace N hora(s)"` | 1–23 horas |
| `"Ayer"` | 1 día |
| `"Hace N días"` | 2–6 días |
| `"d MMM yyyy"` (ej. `"12 abr 2025"`) | 7 días o más |

**`GET /dashboard/superadmin/graficas` — estructura de respuesta:**
```json
{
  "evolucion": [{ "mes": "Nov", "empresas": 4,  "empleados": 120 }],
  "sectores":  [{ "name": "Tecnología", "value": 5 }],
  "modulos":   [{ "nombre": "Onboarding", "completados": 450, "pendientes": 120 }]
}
```
- **evolucion**: totales acumulados al final de cada uno de los últimos 6 meses (ordenado de más antiguo a más reciente).
- **sectores**: empresas agrupadas por sector, ordenadas por volumen DESC. No incluye empresas sin sector asignado.
- **modulos**: top 10 módulos activos ordenados por `completados DESC`.
---
## 5. Módulos · `/api/v1/modulos`
> Los módulos pueden ser de **empresa** o **globales** (`empresaId = null`, solo creables por `ADMIN`).
| Método | Ruta | Rol mínimo | Descripción |
|--------|------|-----------|-------------|
| `POST` | `/modulos` | `ADMIN_EMPRESA` | Crear módulo. ADMIN puede crear globales. |
| `GET` | `/modulos` | Cualquiera | Listar módulos visibles según rol. |
| `GET` | `/modulos/{id}` | Cualquiera | Obtener módulo por ID. `403` si no pertenece a su empresa. |
| `PUT` | `/modulos/{id}` | `ADMIN_EMPRESA` | Actualizar módulo completo. |
| `PATCH` | `/modulos/{id}/desactivar` | `ADMIN_EMPRESA` | Soft-delete. |
---
## 6. Contenidos · `/api/v1/contenidos`
> Tipos: `LECTURA`, `VIDEO`, `EVALUACION`. Los de tipo `EVALUACION` incluyen preguntas.
| Método | Ruta | Rol mínimo | Descripción |
|--------|------|-----------|-------------|
| `POST` | `/contenidos` | `ADMIN_EMPRESA` | Crear contenido en un módulo. |
| `GET` | `/contenidos/modulo/{moduloId}` | Cualquiera | Listar contenidos de un módulo. Empleado solo ve activos. |
| `GET` | `/contenidos/{id}` | Cualquiera | Obtener contenido por ID (incluye preguntas si es EVALUACION). |
| `PUT` | `/contenidos/{id}` | `ADMIN_EMPRESA` | Actualizar contenido. La versión se incrementa si cambia el cuerpo. |
| `PATCH` | `/contenidos/{id}/desactivar` | `ADMIN_EMPRESA` | Soft-delete. Trazabilidad histórica se conserva. |
| `POST` | `/contenidos/preguntas` | `ADMIN_EMPRESA` | Añadir pregunta a un contenido de tipo `EVALUACION`. |
| `DELETE` | `/contenidos/preguntas/{preguntaId}` | `ADMIN_EMPRESA` | Eliminar pregunta de evaluación. |
---
## 7. Progreso · `/api/v1/progreso`
> El campo `completado` es **irreversible**: una vez marcado no se puede desmarcar.
| Método | Ruta | Rol mínimo | Descripción |
|--------|------|-----------|-------------|
| `POST` | `/progreso` | Cualquiera | Registrar/actualizar progreso. El tiempo se acumula. Empleado solo registra el suyo. |
| `GET` | `/progreso/me` | Cualquiera | Todo el progreso del usuario autenticado. |
| `GET` | `/progreso/contenido/{contenidoId}` | Cualquiera | Estado del usuario sobre un contenido concreto. |
| `GET` | `/progreso/usuario/{usuarioId}` | `ADMIN_EMPRESA` | Progreso de un empleado. ADMIN_EMPRESA solo ve su empresa. |
| `GET` | `/progreso/empresa/{empresaId}` | `ADMIN_EMPRESA` | Progreso de todos los empleados de una empresa (dashboard). |
---
## 8. Anuncios · `/api/v1/anuncios`
> `GET /anuncios` es **público**: sin sesión devuelve solo anuncios globales activos. Con sesión devuelve los de la empresa + globales (o todos si es `ADMIN`).

| Método | Ruta | Rol / Auth | Descripción |
|--------|------|-----------|-------------|
| `POST` | `/anuncios` | `ADMIN_EMPRESA` | Crear anuncio. ADMIN puede crear globales (`esGlobal = true`). |
| `GET` | `/anuncios` | ❌ Pública ¹ | Listar anuncios. Sin sesión: solo globales activos. Con sesión: empresa + globales. ADMIN ve toda la plataforma. |
| `PATCH` | `/anuncios/{id}/desactivar` | `ADMIN_EMPRESA` | Soft-delete. ADMIN_EMPRESA solo desactiva los propios. |
| `DELETE` | `/anuncios/{id}` | `ADMIN_EMPRESA` | Alias REST de `PATCH /{id}/desactivar`. Misma lógica. |

¹ Sin token devuelve solo anuncios con `esGlobal = true` y `activo = true`.
---
## 9. Comunicados · `/api/v1/comunicados`
> Comunicados oficiales de EGM para toda la plataforma. Solo gestionables por `ADMIN`.
> `GET /comunicados` es **público**: sin sesión devuelve solo los vigentes y activos.

| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| `POST` | `/comunicados` | `ADMIN` | Crear comunicado oficial. |
| `GET` | `/comunicados` | ❌ Pública ¹ | Listar comunicados. Sin sesión: solo vigentes activos. ADMIN ve el histórico completo (expirados/desactivados incluidos). |
| `PATCH` | `/comunicados/{id}/desactivar` | `ADMIN` | Desactivar comunicado. |

¹ Sin token devuelve `findActivosVigentes()` — comunicados con `activo = true` y `fechaExpiracion` no superada.
---
## 10. Notificaciones · `/api/v1/notificaciones`
> Se generan automáticamente en eventos clave (ej. aprobación de empresa) via `NotificationService#crearInterna` y también manualmente via endpoint.
> → [Documentación detallada del módulo](modulos/notificaciones.md)

| Método | Ruta | Rol mínimo | Descripción |
|--------|------|-----------|-------------|
| `POST` | `/notificaciones` | `ADMIN` o `ADMIN_EMPRESA` | Crear notificación manual para un usuario. `ROLE_EMPLEADO` → `403`. |
| `GET` | `/notificaciones/me` | Cualquiera | Mis notificaciones paginadas (leídas + no leídas). Params: `?page=0&size=20`. Devuelve `Page<NotificationResponse>`. |
| `GET` | `/notificaciones/me/no-leidas` | Cualquiera | Solo las no leídas (para la campana del frontend). Devuelve `List<NotificationResponse>`. |
| `GET` | `/notificaciones/me/contador` | Cualquiera | Número de no leídas. Respuesta: `{ "noLeidas": N }`. |
| `PATCH` | `/notificaciones/{id}/leer` | Cualquiera | Marcar una notificación como leída. `400` si ya leída o no es el destinatario. `404` si no existe. |
| `PATCH` | `/notificaciones/me/leer-todas` | Cualquiera | Marcar todas las no leídas como leídas. Solo afecta al usuario autenticado. Respuesta: `{ "actualizadas": N }`. |

**`NotificationRequest` — campos:**

| Campo | Tipo | Requerido | Validación |
|-------|------|:---------:|------------|
| `destinatarioId` | `UUID` | ✅ | `@NotNull` |
| `tipo` | `String` | ✅ | `@NotBlank`, máx. 100 chars |
| `mensaje` | `String` | ✅ | `@NotBlank` |
| `enlace` | `String` | ❌ | Máx. 500 chars — URL relativa al recurso relacionado |

**`NotificationResponse` — campos:** `notificacionId`, `destinatarioId`, `tipo`, `mensaje`, `enlace`, `leido`, `creadoEn`, `actualizadoEn`.
---
## 11. Eventos de Comunidad · `/api/v1/eventos`
| Método | Ruta | Rol mínimo | Descripción |
|--------|------|-----------|-------------|
| `POST` | `/eventos` | `ADMIN_EMPRESA` | Crear evento. ADMIN puede crear globales. |
| `GET` | `/eventos` | Cualquiera | Listar eventos visibles: empresa + globales. |
| `GET` | `/eventos/{id}` | Cualquiera | Obtener evento por ID. `403` si no pertenece a su empresa. |
| `PUT` | `/eventos/{id}` | `ADMIN_EMPRESA` | Actualizar evento. Solo ADMIN puede cambiar `esGlobal`. |
| `PATCH` | `/eventos/{id}/desactivar` | `ADMIN_EMPRESA` | Soft-delete. ADMIN_EMPRESA solo desactiva los propios. |
---
## 12. Beneficios · `/api/v1/beneficios`
| Método | Ruta | Rol mínimo | Descripción |
|--------|------|-----------|-------------|
| `POST` | `/beneficios` | `ADMIN_EMPRESA` | Crear beneficio. ADMIN puede crear globales. |
| `GET` | `/beneficios` | Cualquiera | Listar beneficios visibles: empresa + globales. |
| `PUT` | `/beneficios/{id}` | `ADMIN_EMPRESA` | Actualizar beneficio. ADMIN_EMPRESA solo edita los propios. |
| `PATCH` | `/beneficios/{id}/desactivar` | `ADMIN_EMPRESA` | Soft-delete. ADMIN_EMPRESA solo desactiva los propios. |
---
## 13. Inteligencia Artificial · `/api/v1/ai`
> Integración con **Gemini 2.0 Flash** (contenido, preguntas, chat, resumen) y **Groq llama-3.3-70b-versatile** (generación desde archivo). Requiere variables de entorno `GEMINI_API_KEY`, `GROQ_API_KEY`, `ELEVENLABS_API_KEY`.
| Método | Ruta | Rol mínimo | Descripción |
|--------|------|-----------|-------------|
| `POST` | `/ai/generar-contenido` | `ADMIN_EMPRESA` | Genera contenido formativo dado un tema, descripción y tipo de módulo. |
| `POST` | `/ai/generar-preguntas` | `ADMIN_EMPRESA` | Genera preguntas de evaluación desde un prompt. `numPreguntas` opcional (por defecto 5). |
| `POST` | `/ai/chat` | Cualquiera | Chatbot para empleados. Acepta `prompt`, `nombreEmpresa` y `contexto`. |
| `POST` | `/ai/resumir` | `ADMIN_EMPRESA` | Genera un resumen estructurado de un texto largo. |
| `POST` | `/ai/generar-desde-archivo` | `ADMIN_EMPRESA` | Sube un PDF/DOCX/TXT (`multipart/form-data`, part `archivo`) y genera salidas según `tiposSalida` (query param): `documentacion`, `podcast`, `video` o combinaciones. Devuelve `AiFileResponse`. |
---
## 14. Sugerencias · `/api/v1/sugerencias`
> Buzón de sugerencias de empleados hacia su empresa o hacia EGM Atalayas.
| Método | Ruta | Rol mínimo | Descripción |
|--------|------|-----------|-------------|
| `POST` | `/sugerencias` | Cualquiera | Enviar una sugerencia. Body: `{ "mensaje": "...", "destinatario": "EMPRESA" \| "EGM" }`. Devuelve `201` sin body. |
| `GET` | `/sugerencias` | `ADMIN_EMPRESA` | Listar todas las sugerencias. |
---
## 15. Incidencias · `/api/v1/incidencias`
> Gestión de incidencias de plataforma. Solo accesible para `ROLE_ADMIN` (superadmin EGM).
> `empresa_id` es opcional: `null` = incidencia global, valor = asociada a empresa concreta.

| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| `POST` | `/incidencias` | `ADMIN` | Crear nueva incidencia. Si `prioridad = CRITICA` se genera un evento `error` en `audit_log`. |
| `GET` | `/incidencias` | `ADMIN` | Listar todas las incidencias ordenadas por `creadoEn DESC`. |
| `PATCH` | `/incidencias/{id}/estado` | `ADMIN` | Cambiar el estado de una incidencia. Body: `{ "estado": "ABIERTA" \| "EN_CURSO" \| "RESUELTA" \| "CERRADA" }`. El valor del campo `estado` es case-insensitive (el backend lo normaliza a mayúsculas). |

**`POST /incidencias` — body:**
```json
{
  "titulo":      "Caída del servicio de notificaciones",
  "descripcion": "El servicio de emails no responde desde las 10:00.",
  "prioridad":   "CRITICA",
  "empresaId":   null
}
```

**`GET /incidencias` — respuesta:**
```json
[
  {
    "incidenciaId": "5",
    "titulo": "Caída del servicio de notificaciones",
    "descripcion": "El servicio de emails no responde desde las 10:00.",
    "estado": "EN_CURSO",
    "prioridad": "CRITICA",
    "empresaId": null,
    "creadoEn": "2026-04-27T10:05:00Z"
  }
]
```

| Campo | Valores | Descripción |
|-------|---------|-------------|
| `incidenciaId` | `String` (numérico) | Identificador de la incidencia. Usar como `{id}` en el PATCH. |
| `estado` | `ABIERTA` \| `EN_CURSO` \| `RESUELTA` \| `CERRADA` | Estado actual de la incidencia |
| `prioridad` | `NORMAL` \| `CRITICA` | Las `CRITICA` generan entrada en `audit_log` automáticamente |
| `empresaId` | `UUID` \| `null` | `null` = incidencia global de plataforma |

---
## Resumen de endpoints públicos (sin autenticación)
| Método | Ruta | Nota |
|--------|------|------|
| `POST` | `/auth/login` | |
| `POST` | `/auth/register` | |
| `POST` | `/auth/refresh-token` | |
| `POST` | `/auth/logout` | |
| `POST` | `/auth/forgot-password` | |
| `POST` | `/auth/reset-password` | |
| `POST` | `/empresas/solicitud` | |
| `GET` | `/empresas/aprobadas` | |
| `GET` | `/anuncios` | Sin sesión: solo globales activos |
| `GET` | `/comunicados` | Sin sesión: solo vigentes activos |
| `GET` | `/swagger-ui/**` | |
| `GET` | `/v3/api-docs/**` | |
---
## Notas de seguridad multi-tenant
- Los usuarios con `ROLE_ADMIN_EMPRESA` están **aislados por empresa**: acceder por ID a recursos de otra empresa devuelve `404` (no `403`, para no exponer la existencia del recurso).
- El `ROLE_ADMIN` (superadmin EGM) tiene acceso transversal a todos los recursos.
- El `ROLE_EMPLEADO` tiene acceso de **solo lectura/consumo** dentro de su empresa.
