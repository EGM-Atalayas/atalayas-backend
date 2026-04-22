# Módulo: Empresas (`company`)

> **Paquete:** `com.atalayas.backend.company`
> **Tabla BD:** `empresa`
> **Audiencia:** Frontend, Backend
> **Última actualización:** Abril 2026

---

## ¿Qué hace este módulo?

Gestiona el ciclo de vida de las empresas del parque EGM:
- Solicitud de alta de una nueva empresa (pública, sin autenticación)
- Revisión y aprobación/rechazo por el superadmin
- Listado de empresas aprobadas (para el selector del formulario de registro)
- Gestión completa de empresas por el superadmin

---

## Entidad: `Company` (tabla `empresa`)

| Campo | Tipo | Descripción |
|---|---|---|
| `empresaId` | UUID (PK) | Identificador único |
| `nombreEmpresa` | String | Nombre de la empresa |
| `cif` | String (UNIQUE) | CIF de la empresa |
| `sector` | String (nullable) | Sector de actividad |
| `logoUrl` | String (nullable) | URL del logo (se muestra en el header del admin empresa) |
| `emailContacto` | String | Email de contacto principal |
| `telefonoContacto` | String (nullable) | Teléfono de contacto |
| `descripcion` | TEXT (nullable) | Descripción de la empresa |
| `mision` | TEXT (nullable) | Misión corporativa (usado en módulos de onboarding) |
| `vision` | TEXT (nullable) | Visión corporativa |
| `valores` | TEXT (nullable) | Valores corporativos |
| `estadoSolicitud` | Enum | `PENDIENTE` / `APROBADA` / `PAUSADA` |
| `esEgm` | boolean | `true` si es la empresa EGM (permisos especiales) |
| `activo` | boolean | `false` = empresa pausada o pendiente de aprobación |
| `fechaSolicitud` | OffsetDateTime | Cuando se envió la solicitud (no modificable) |
| `fechaResolucion` | OffsetDateTime (nullable) | Cuando el superadmin tomó la decisión |
| `actualizadoEn` | OffsetDateTime | Última modificación (automático) |

---

## Endpoints

Base URL: `/api/v1/empresas`

| Método | Ruta | Auth | Descripción |
|---|---|:---:|---|
| `POST` | `/empresas/solicitud` | ❌ Pública | Solicitar alta de empresa |
| `GET` | `/empresas/aprobadas` | ❌ Pública | Listar empresas aprobadas (para selector de registro) |
| `GET` | `/empresas` | `ADMIN` | Listar todas las empresas |
| `GET` | `/empresas/pendientes` | `ADMIN` | Listar empresas en estado `PENDIENTE` |
| `GET` | `/empresas/solicitudes` | `ADMIN` | Listar solicitudes pendientes con datos del admin |
| `PATCH` | `/empresas/{id}/estado` | `ADMIN` | Cambiar el estado de una empresa activa |
| `PATCH` | `/empresas/{id}/solicitud` | `ADMIN` | Aprobar o rechazar una solicitud de alta |
| `PATCH` | `/empresas/{id}/activacion` | `ADMIN` | Toggle activo/inactivo de una empresa aprobada |

---

## Reglas de negocio

### Estados de una empresa

```
               ┌──────────────────┐
       Alta    │                  │  Aprobación
   ──────────► │    PENDIENTE     │ ──────────────────────────────► APROBADA ◄──┐
               │                  │                                     │        │
               │                  │ ──► (eliminación física)       PAUSADA ──────┘
               └──────────────────┘  Rechazo        ▲                   │
                                                     └───────────────────┘
```

### Tabla de transiciones

| Desde \ Hacia | `PENDIENTE` | `APROBADA` | `PAUSADA` | Rechazo (hard delete) |
|:---:|:---:|:---:|:---:|:---:|
| **`PENDIENTE`** | ❌ | ✅ via `/{id}/solicitud` | ❌ | ✅ via `/{id}/solicitud` |
| **`APROBADA`** | ❌ | ❌ | ✅ via `/{id}/estado` | ❌ |
| **`PAUSADA`** | ❌ | ✅ via `/{id}/estado` | ❌ | ❌ |

> Las transiciones no permitidas devuelven `400 Bad Request`.
> El rechazo elimina físicamente la empresa y sus usuarios de la BD.

### Efectos secundarios de cada transición

| Transición | `activo` empresa | Usuarios de la empresa | Email enviado |
|---|:---:|---|---|
| `PENDIENTE → APROBADA` | `true` | Se activan (`activo = true`) | ✉ Bienvenida al admin |
| `PENDIENTE → (rechazo)` | — (borrado) | Eliminados de BD | ✉ Notificación de rechazo |
| `APROBADA → PAUSADA` | `false` | Se desactivan (`activo = false`) | Ninguno |
| `PAUSADA → APROBADA` | `true` | Se reactivan (`activo = true`) | Ninguno |

### Solicitud de alta (`POST /empresas/solicitud`)

Al solicitar el alta se crea automáticamente:
1. La empresa en estado `PENDIENTE` con `activo = false`
2. El usuario admin de la empresa con `activo = false` (no puede iniciar sesión hasta que se apruebe)

---

## Request / Response

### POST `/empresas/solicitud` — Solicitar alta

**Request body:**
```json
{
  "nombreEmpresa": "Tecnología SL",
  "cif": "B12345678",
  "sector": "Tecnología",
  "emailContacto": "info@tecnologia.com",
  "telefonoContacto": "+34 600 000 000",
  "descripcion": "Empresa de desarrollo de software",
  "adminNombre": "Carlos",
  "adminApellidos": "Rodríguez",
  "adminEmail": "carlos@tecnologia.com",
  "adminPassword": "Password123!"
}
```

**Response `201 Created`:**
```json
{
  "empresaId": "550e8400-e29b-41d4-a716-446655440000",
  "nombreEmpresa": "Tecnología SL",
  "estadoSolicitud": "PENDIENTE",
  "mensaje": "Solicitud enviada correctamente. Recibirás un email cuando sea revisada."
}
```

### GET `/empresas/aprobadas`

No requiere autenticación. Se usa en el formulario de registro de empleados para seleccionar la empresa.

**Response `200 OK`:**
```json
[
  {
    "empresaId": "550e8400-...",
    "nombreEmpresa": "Tecnología SL",
    "sector": "Tecnología",
    "logoUrl": null
  }
]
```

### PATCH `/empresas/{id}/estado` — Cambiar estado

Gestiona el ciclo de vida de empresas activas. **No permite rechazar** — el rechazo se hace desde `/{id}/solicitud`.

**Request body:**
```json
{
  "nuevoEstado": "PAUSADA"
}
```
Valores válidos según el estado actual:
- Empresa `PENDIENTE`: `"APROBADA"`
- Empresa `APROBADA`: `"PAUSADA"`
- Empresa `PAUSADA`: `"APROBADA"`

**Response `200 OK`:** datos actualizados de la empresa.

### PATCH `/empresas/{id}/solicitud` — Aprobar o rechazar

**Request body:**
```json
{ "accion": "aprobar" }
```
o
```json
{ "accion": "rechazar" }
```

**Response `200 OK`:** sin cuerpo.

> ⚠️ Al rechazar, la empresa y sus usuarios se **eliminan físicamente** de la base de datos. Esta acción es irreversible.

---

## Migración de base de datos

Al introducir el estado `PAUSADA` y eliminar `RECHAZADA`, es necesario actualizar el CHECK constraint de PostgreSQL manualmente (el proyecto usa `ddl-auto=none`):

```sql
-- 1. Eliminar el constraint antiguo
ALTER TABLE empresa DROP CONSTRAINT IF EXISTS empresa_estado_solicitud_check;

-- 2. Crear el constraint actualizado
ALTER TABLE empresa
    ADD CONSTRAINT empresa_estado_solicitud_check
    CHECK (estado_solicitud IN ('PENDIENTE', 'APROBADA', 'PAUSADA'));
```

---

## Flujo de alta de empresa

```
Empresa solicitante               Superadmin EGM                Backend
       │                               │                           │
       │  POST /empresas/solicitud     │                           │
       │ ─────────────────────────────────────────────────────────►│
       │                               │                           │ Crea empresa PENDIENTE
       │  201 Created                  │                           │ Crea admin empresa INACTIVO
       │ ◄─────────────────────────────────────────────────────────│
       │                               │                           │
       │  (espera revisión)            │                           │
       │                               │  GET /empresas/solicitudes│
       │                               │ ─────────────────────────►│
       │                               │  PATCH /{id}/solicitud    │
       │                               │  { "accion": "aprobar" }  │
       │                               │ ─────────────────────────►│
       │                               │                           │ Activa empresa
       │                               │                           │ Activa usuarios
       │  ✉ Email de bienvenida        │                           │ Envía email
       │ ◄─────────────────────────────────────────────────────────│
       │                               │  200 OK                   │
       │                               │ ◄─────────────────────────│
```
