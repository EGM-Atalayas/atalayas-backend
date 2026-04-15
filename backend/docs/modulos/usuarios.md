# Módulo: Usuarios (`user`)

> **Paquete:** `com.atalayas.backend.user`
> **Tabla BD:** `usuario`
> **Audiencia:** Frontend, Backend
> **Última actualización:** Abril 2026

---

## ¿Qué hace este módulo?

Gestiona las cuentas de usuario de la plataforma:
- Crear usuarios dentro de una empresa
- Consultar el perfil propio o de otros usuarios (con restricciones de rol)
- Listar usuarios de una empresa
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
| `puestoTrabajo` | String (nullable) | Cargo visible en el header |
| `empresaId` | UUID | FK a la empresa a la que pertenece |
| `rol` | Role | FK al rol del usuario (eager load) |
| `activo` | boolean | `false` = cuenta desactivada (soft-delete) |
| `intentosFallidos` | int | Se incrementa en cada login fallido. Bloqueo a partir de 5 |
| `terminosAceptados` | boolean | El usuario aceptó los términos al registrarse |
| `fechaRegistro` | OffsetDateTime | Fecha de creación (automático, no modificable) |
| `ultimoLogin` | OffsetDateTime | Fecha del último acceso |
| `actualizadoEn` | OffsetDateTime | Fecha de última modificación (automático) |

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

- `ROLE_ADMIN_EMPRESA` → si el usuario no pertenece a su empresa, devuelve `404` (no `403`)
- `ROLE_ADMIN` → puede obtener cualquier usuario

### Desactivar usuario (`DELETE /users/{id}/desactivar`)

- Hace `activo = false` (soft-delete). El usuario **no se elimina de la BD**.
- El usuario desactivado no puede iniciar sesión (`isEnabled()` devuelve `false`).
- `ROLE_ADMIN_EMPRESA` solo puede desactivar usuarios de su empresa (→ `404` si es de otra).

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
  "codigoRol": "ROLE_EMPLEADO",
  "empresaId": "660e8400-e29b-41d4-a716-446655440001"
}
```
> `empresaId` es obligatorio solo para `ROLE_ADMIN`. Para `ROLE_ADMIN_EMPRESA` se ignora.

**Response `201 Created`:**
```json
{
  "usuarioId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "nuevo@empresa.com",
  "nombre": "Ana",
  "apellidos": "Martínez",
  "puestoTrabajo": "Desarrolladora",
  "codigoRol": "ROLE_EMPLEADO",
  "empresaId": "660e8400-e29b-41d4-a716-446655440001",
  "activo": true,
  "fechaRegistro": "2026-04-15T10:00:00Z"
}
```

### GET `/users/me` — Perfil propio

**Response `200 OK`:** mismo formato que la creación, con todos los campos del perfil.

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
      │ ◄───────────────────────────── │
      │  201 Created + datos usuario   │
```

