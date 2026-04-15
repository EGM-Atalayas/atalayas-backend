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
| `estadoSolicitud` | Enum | `PENDIENTE` / `APROBADA` / `RECHAZADA` |
| `esEgm` | boolean | `true` si es la empresa EGM (permisos especiales) |
| `activo` | boolean | `false` = empresa suspendida o rechazada |
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
| `PATCH` | `/empresas/{id}/estado` | `ADMIN` | Cambiar el estado de una empresa |

---

## Reglas de negocio

### Estados de una empresa

```
               ┌──────────────────┐
       Alta    │                  │  Aprobación
   ──────────► │    PENDIENTE     │ ──────────────► APROBADA
               │                  │
               │                  │ ──────────────► RECHAZADA
               └──────────────────┘  Rechazo
                        ▲
                        │ Re-solicitud
               RECHAZADA ─────────────────────────► PENDIENTE
```

### Tabla de transiciones

| Desde \ Hacia | `PENDIENTE` | `APROBADA` | `RECHAZADA` |
|:---:|:---:|:---:|:---:|
| **`PENDIENTE`** | ❌ | ✅ | ✅ |
| **`RECHAZADA`** | ✅ | ❌ | ❌ |
| **`APROBADA`** | ❌ | ❌ | ❌ |

> Una empresa `APROBADA` no puede ser rechazada ni volver a pendiente. Las transiciones no permitidas devuelven `400 Bad Request`.

### Efectos secundarios de cada transición

| Transición | `activo` empresa | Usuarios de la empresa | Email enviado |
|---|:---:|---|---|
| `PENDIENTE → APROBADA` | `true` | Se activan (`activo = true`) | ✉ Bienvenida al admin |
| `PENDIENTE → RECHAZADA` | `false` | Sin cambio (siguen inactivos) | ✉ Notificación de rechazo |
| `RECHAZADA → PENDIENTE` | `false` | Sin cambio | Ninguno |

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

**Request body:**
```json
{
  "nuevoEstado": "APROBADA"
}
```
Valores válidos: `"APROBADA"`, `"RECHAZADA"`, `"PENDIENTE"`

**Response `200 OK`:** datos actualizados de la empresa.

---

## Flujo de alta de empresa

```
Empresa solicitante               Superadmin EGM                Backend
       │                               │                           │
       │  POST /empresas/solicitud     │                           │
       │ ─────────────────────────────────────────────────────► │
       │                               │                           │ Crea empresa PENDIENTE
       │  201 Created                  │                           │ Crea admin empresa INACTIVO
       │ ◄─────────────────────────────────────────────────────── │
       │                               │                           │
       │  (espera revisión)            │                           │
       │                               │  GET /empresas/pendientes │
       │                               │ ─────────────────────────►│
       │                               │  PATCH /{id}/estado       │
       │                               │  { nuevoEstado: APROBADA }│
       │                               │ ─────────────────────────►│
       │                               │                           │ Activa empresa
       │                               │                           │ Activa usuarios
       │  ✉ Email de bienvenida        │                           │ Envía email
       │ ◄─────────────────────────────────────────────────────── │
       │                               │  200 OK                   │
       │                               │ ◄─────────────────────────│
```

