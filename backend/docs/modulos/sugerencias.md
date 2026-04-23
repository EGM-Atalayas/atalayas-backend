# Módulo: Sugerencias (`sugerencia`)

> **Paquete:** `com.atalayas.backend.sugerencia`
> **Tabla BD:** `sugerencia`
> **Audiencia:** Frontend, Backend
> **Última actualización:** Abril 2026

---

## ¿Qué hace este módulo?

Proporciona un **buzón de sugerencias** para que los empleados puedan enviar propuestas de mejora a su empresa o directamente a EGM Atalayas:

- Cualquier usuario autenticado puede enviar una sugerencia
- Los administradores (empresa y superadmin) pueden listar y revisar las sugerencias recibidas

---

## Entidad: `Sugerencia` (tabla `sugerencia`)

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | UUID (PK) | Identificador único |
| `mensaje` | String | Texto de la sugerencia (máx. 500 chars, obligatorio) |
| `usuario` | User (FK) | Usuario que envió la sugerencia |
| `estado` | Enum `EstadoSugerencia` | Estado actual de la sugerencia (default: `PENDIENTE`) |
| `destinatario` | Enum `DestinatarioSugerencia` | A quién va dirigida (default: `EMPRESA`) |
| `creadoEn` | OffsetDateTime | Fecha de creación (automático, no modificable) |

### Enum `EstadoSugerencia`

| Valor | Descripción |
|---|---|
| `PENDIENTE` | Recibida, sin revisar (estado inicial) |
| `VISTA` | El administrador la ha leído |
| `RESUELTA` | La sugerencia ha sido atendida |

### Enum `DestinatarioSugerencia`

| Valor | Descripción |
|---|---|
| `EMPRESA` | La sugerencia va dirigida al admin de la empresa del usuario |
| `EGM` | La sugerencia va dirigida al equipo EGM Atalayas (superadmin) |

---

## Endpoints

Base URL: `/api/v1/sugerencias`

| Método | Ruta | Rol mínimo | Descripción |
|---|---|---|---|
| `POST` | `/sugerencias` | Cualquiera | Enviar una sugerencia |
| `GET` | `/sugerencias` | `ADMIN_EMPRESA` | Listar todas las sugerencias |

---

## Reglas de negocio

### Enviar sugerencia (`POST /sugerencias`)

- Cualquier usuario autenticado puede enviar una sugerencia sin limitación de número.
- El campo `destinatario` determina si la recibe el admin de su empresa (`EMPRESA`) o el superadmin (`EGM`).
- El estado inicial es siempre `PENDIENTE`.

### Listar sugerencias (`GET /sugerencias`)

- `ROLE_ADMIN_EMPRESA` y `ROLE_ADMIN` pueden ver la lista completa de sugerencias.
- La respuesta incluye el nombre y email del usuario que la envió.

---

## Request / Response

### POST `/sugerencias` — Enviar sugerencia

**Request body (`SugerenciaRequest`):**
```json
{
  "mensaje": "Sería genial tener módulos de formación sobre liderazgo",
  "destinatario": "EMPRESA"
}
```

| Campo | Validación |
|---|---|
| `mensaje` | Obligatorio, máx. 500 caracteres |
| `destinatario` | Obligatorio. Valores: `EMPRESA` o `EGM` |

**Response `201 Created`** sin body.

### GET `/sugerencias` — Listar sugerencias

No requiere parámetros.

**Response `200 OK` (array de `SugerenciaResponse`):**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "mensaje": "Sería genial tener módulos de formación sobre liderazgo",
    "nombreUsuario": "Ana Martínez",
    "emailUsuario": "ana@empresa.com",
    "estado": "PENDIENTE",
    "destinatario": "EMPRESA",
    "creadoEn": "2026-04-23T10:00:00Z"
  }
]
```

---

## DTOs del módulo

| DTO | Tipo | Descripción |
|---|---|---|
| `SugerenciaRequest` | record | Request de envío: `mensaje` (String), `destinatario` (DestinatarioSugerencia) |
| `SugerenciaResponse` | record | Response: `id`, `mensaje`, `nombreUsuario`, `emailUsuario`, `estado`, `destinatario`, `creadoEn` |

