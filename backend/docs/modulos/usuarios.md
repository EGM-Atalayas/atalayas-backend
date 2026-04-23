# Módulo: Usuarios (`user`)

> **Paquete:** `com.atalayas.backend.user`
> **Tabla BD:** `usuario`
> **Audiencia:** Frontend, Backend
> **Última actualización:** Abril 2026 (rev. 2)

---

## ¿Qué hace este módulo?

Gestiona las cuentas de usuario de la plataforma:
- Crear usuarios dentro de una empresa
- Consultar el perfil propio o de otros usuarios (con restricciones de rol)
- Listar usuarios de una empresa
- Actualizar el perfil propio (datos personales, preferencias, notificaciones, avatar)
- Cambiar la contraseña propia
- Desactivar usuarios (soft-delete, no se borran de la BD)

---

## Entidad: `User` (tabla `usuario`)

| Campo | Tipo | Descripción |
|---|---|---|
| `usuarioId` | UUID (PK) | Identificador único |
| `email` | String (UNIQUE) | Email — también es el username de autenticación |
| `password` | String | Hash BCrypt de la contraseña |
| `nombre` | String | Nombre |
| `apellidos` | String | Apellidos |
| `avatarUrl` | String (nullable) | URL del avatar |
| `bannerUrl` | String (nullable) | URL del banner de perfil |
| `bio` | String (nullable) | Biografía o descripción del usuario (máx. 300 chars) |
| `telefono` | String (nullable) | Teléfono de contacto (máx. 20 chars) |
| `puestoTrabajo` | String (nullable) | Cargo visible en el header |
| `departamento` | String (nullable) | Departamento del empleado (máx. 50 chars) |
| `disponibilidad` | Enum | Estado de disponibilidad (default: `DISPONIBLE`) |
| `empresaId` | UUID | FK a la empresa a la que pertenece |
| `rol` | Role | FK al rol del usuario (eager load) |
| `activo` | boolean | `false` = cuenta desactivada (soft-delete) |
| `intentosFallidos` | int | Se incrementa en cada login fallido. Bloqueo a partir de 5 |
| `terminosAceptados` | boolean | El usuario aceptó los términos al registrarse |
| `notifNuevoModulo` | boolean | Recibir notificación cuando se publica un módulo (default: `true`) |
| `notifModuloCompletado` | boolean | Recibir notificación al completar un módulo (default: `true`) |
| `notifComunicado` | boolean | Recibir notificación de comunicados oficiales (default: `true`) |
| `notifPendiente` | boolean | Recibir notificación de tareas pendientes (default: `true`) |
| `modoOscuro` | boolean | Preferencia de tema oscuro en la interfaz (default: `false`) |
| `fechaRegistro` | OffsetDateTime | Fecha de creación (automático, no modificable) |
| `ultimoLogin` | OffsetDateTime | Fecha del último acceso |
| `actualizadoEn` | OffsetDateTime | Fecha de última modificación (automático) |

### Enum `Disponibilidad`

| Valor | Descripción |
|---|---|
| `DISPONIBLE` | Disponible (por defecto) |
| `OCUPADO` | Ocupado / en reunión |
| `TELETRABAJO` | Trabajando en remoto |
| `VACACIONES` | De vacaciones |
| `AUSENTE` | Ausente |

### Relación con Spring Security

`User` implementa la interfaz `UserDetails` de Spring Security:
- `getUsername()` → devuelve `email`
- `isEnabled()` → devuelve `activo`
- `isAccountNonLocked()` → devuelve `intentosFallidos < 5`
- `getAuthorities()` → devuelve `[ROLE_xxx]` del campo `rol`

---

## Endpoints

Base URL: `/api/v1/users`

| Método | Ruta | Rol mínimo | Descripción |
|---|---|---|---|
| `POST` | `/users` | `ADMIN_EMPRESA` | Crear un nuevo usuario |
| `GET` | `/users/me` | Cualquiera | Perfil completo del usuario autenticado |
| `PATCH` | `/users/me` | Cualquiera | Actualizar perfil propio (campos opcionales) |
| `POST` | `/users/me/password` | Cualquiera | Cambiar contraseña propia |
| `POST` | `/users/me/avatar` | Cualquiera | Subir imagen de avatar (`multipart/form-data`) |
| `GET` | `/users/{id}` | `ADMIN_EMPRESA` | Obtener usuario por ID |
| `GET` | `/users` | `ADMIN_EMPRESA` | Listar usuarios de la empresa |
| `DELETE` | `/users/{id}/desactivar` | `ADMIN_EMPRESA` | Soft-delete: desactivar usuario |

---

## Reglas de negocio

### Crear usuario (`POST /users`)

El comportamiento depende del rol de quien llama:

| Quien llama | `empresaId` en el body | Roles que puede asignar | Resultado |
|---|---|---|---|
| `ROLE_ADMIN_EMPRESA` | Se ignora (usa el suyo propio) | `ROLE_EMPLEADO`, `ROLE_ADMIN_EMPRESA` | `201 Created` |
| `ROLE_ADMIN_EMPRESA` | — | `ROLE_ADMIN` | `403 Forbidden` |
| `ROLE_ADMIN` | Obligatorio | Cualquiera | `201 Created` |
| `ROLE_ADMIN` | Ausente | — | `400 Bad Request` |

### Listar usuarios (`GET /users`)

- `ROLE_ADMIN_EMPRESA` → solo ve usuarios de su empresa
- `ROLE_ADMIN` → ve todos los usuarios de la plataforma

### Obtener usuario por ID (`GET /users/{id}`)

- `ROLE_ADMIN_EMPRESA` → si el usuario no pertenece a su empresa, devuelve `404` (no `403`, para no revelar que el recurso existe — enmascarado)
- `ROLE_ADMIN` → puede obtener cualquier usuario

### Desactivar usuario (`DELETE /users/{id}/desactivar`)

- Hace `activo = false` (soft-delete). El usuario **no se elimina de la BD**.
- El usuario desactivado no puede iniciar sesión (`isEnabled()` devuelve `false`).
- `ROLE_ADMIN_EMPRESA` solo puede desactivar usuarios de su empresa (→ `404` si es de otra).

> **Excepción — hard delete por rechazo de empresa:** cuando el superadmin rechaza una solicitud de alta desde `PATCH /empresas/{id}/solicitud`, los usuarios provisionales de esa empresa se eliminan **físicamente** de la BD en cascada (junto con la empresa). Este borrado lo gestiona `CompanyService`, no este endpoint.

### Actualizar perfil (`PATCH /users/me`)

Todos los campos son opcionales. Solo se actualizan los que vienen en el body (PATCH semántico). El usuario solo puede actualizar su propio perfil.

### Cambiar contraseña (`POST /users/me/password`)

Requiere confirmación de la contraseña actual. Si `passwordActual` no coincide con el hash almacenado → `400`. Si `passwordNueva` y `passwordConfirmar` no coinciden → `400`.

### Subir avatar (`POST /users/me/avatar`)

Acepta `multipart/form-data` con el campo `file`. El backend procesa y guarda la imagen y devuelve la URL resultante. La URL se actualiza automáticamente en el perfil del usuario.

---

## Request / Response

### POST `/users` — Crear usuario

**Request body:**
```json
{
  "email": "nuevo@empresa.com",
  "password": "Password123!",
  "nombre": "Ana",
  "apellidos": "Martínez",
  "puestoTrabajo": "Desarrolladora",
  "departamento": "Tecnología",
  "rolId": "770e8400-e29b-41d4-a716-446655440002",
  "empresaId": "660e8400-e29b-41d4-a716-446655440001"
}
```
> `empresaId` es obligatorio solo para `ROLE_ADMIN`. Para `ROLE_ADMIN_EMPRESA` se ignora.
> `rolId` es el UUID del rol (no el código de rol).
> `departamento` es opcional (máx. 50 chars).

**Response `201 Created`:**
```json
{
  "usuarioId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "nuevo@empresa.com",
  "nombre": "Ana",
  "apellidos": "Martínez",
  "puestoTrabajo": "Desarrolladora",
  "departamento": "Tecnología",
  "codigoRol": "ROLE_EMPLEADO",
  "empresaId": "660e8400-e29b-41d4-a716-446655440001",
  "activo": true,
  "fechaRegistro": "2026-04-15T10:00:00Z"
}
```

### GET `/users/me` — Perfil propio (`UserProfileResponse`)

**Response `200 OK`:**
```json
{
  "usuarioId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "usuario@empresa.com",
  "nombre": "Ana",
  "apellidos": "Martínez",
  "nombreCompleto": "Ana Martínez",
  "avatarUrl": "https://storage.ejemplo.com/avatars/uuid.webp",
  "bannerUrl": null,
  "bio": "Desarrolladora apasionada por la formación continua.",
  "telefono": "+34 600 000 000",
  "puestoTrabajo": "Desarrolladora",
  "departamento": "Tecnología",
  "disponibilidad": "DISPONIBLE",
  "empresaId": "660e8400-e29b-41d4-a716-446655440001",
  "nombreEmpresa": "Tecnología SL",
  "rolId": "770e8400-e29b-41d4-a716-446655440002",
  "codigoRol": "ROLE_EMPLEADO",
  "nombreRol": "Empleado",
  "notifNuevoModulo": true,
  "notifModuloCompletado": true,
  "notifComunicado": true,
  "notifPendiente": true,
  "modoOscuro": false,
  "activo": true,
  "terminosAceptados": true,
  "fechaRegistro": "2026-04-15T10:00:00Z",
  "ultimoLogin": "2026-04-23T08:30:00Z",
  "actualizadoEn": "2026-04-23T08:30:00Z"
}
```

### PATCH `/users/me` — Actualizar perfil

Todos los campos son opcionales. Los campos `null` o ausentes no se modifican.

**Request body:**
```json
{
  "nombre": "Ana",
  "apellidos": "Martínez García",
  "puestoTrabajo": "Tech Lead",
  "bio": "Apasionada por la formación continua.",
  "telefono": "+34 600 000 000",
  "bannerUrl": "https://storage.ejemplo.com/banners/uuid.webp",
  "disponibilidad": "TELETRABAJO",
  "notifNuevoModulo": true,
  "notifModuloCompletado": false,
  "notifComunicado": true,
  "notifPendiente": true,
  "modoOscuro": true
}
```

**Response `200 OK`:** mismo formato que `GET /users/me` (`UserProfileResponse`).

### POST `/users/me/password` — Cambiar contraseña

**Request body:**
```json
{
  "passwordActual": "MiPasswordActual123!",
  "passwordNueva": "NuevoPassword456!",
  "passwordConfirmar": "NuevoPassword456!"
}
```

**Response `204 No Content`** si se cambió correctamente.

**Errores:**
| Código | Causa |
|---|---|
| `400` | `passwordActual` incorrecta, o `passwordNueva` ≠ `passwordConfirmar` |

### POST `/users/me/avatar` — Subir avatar

**Content-Type:** `multipart/form-data`
**Campo:** `file` (imagen)

**Response `200 OK`:**
```json
{ "avatarUrl": "https://storage.ejemplo.com/avatars/uuid.webp" }
```

### DELETE `/users/{id}/desactivar`

No requiere body.

**Response `204 No Content`** si se desactivó correctamente.

---

## Flujo de creación de usuario

```
ADMIN_EMPRESA                       Backend
      │                                │
      │  POST /users                   │
      │  { email, nombre, ... }        │
      │ ─────────────────────────────► │
      │                                │  1. Verifica que el email no existe (409 si duplicado)
      │                                │  2. Asigna empresaId del admin autenticado
      │                                │  3. Valida que el rol asignado es permitido
      │                                │  4. Encripta la contraseña (BCrypt)
      │                                │  5. Guarda en BD
      │                                │  6. Crea notificación interna de bienvenida
      │                                │  7. Envía email de bienvenida (fallo no revierte)
      │ ◄───────────────────────────── │
      │  201 Created + datos usuario   │
```
