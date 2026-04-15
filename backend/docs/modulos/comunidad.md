# Módulo: Comunidad (`community`)

> **Paquete:** `com.atalayas.backend.community`
> **Tabla BD:** `evento_comunidad`
> **Audiencia:** Frontend, Backend
> **Última actualización:** Abril 2026

---

## ¿Qué hace este módulo?

Gestiona los **eventos del parque empresarial EGM**. Los eventos son actividades que se celebran en el área empresarial y pueden ser visibles para toda la plataforma (globales) o solo para una empresa concreta.

---

## Entidad: `CommunityEvent` (tabla `evento_comunidad`)

| Campo | Tipo | Descripción |
|---|---|---|
| `eventoId` | UUID (PK) | Identificador único |
| `empresaId` | UUID (nullable) | Empresa propietaria. `null` si es global |
| `titulo` | String | Título del evento |
| `descripcion` | TEXT (nullable) | Descripción detallada |
| `esGlobal` | boolean | `true` = visible para todos los usuarios de la plataforma |
| `fechaInicio` | OffsetDateTime | Fecha y hora de inicio del evento |
| `fechaFin` | OffsetDateTime (nullable) | Fecha y hora de fin (opcional) |
| `activo` | boolean | `false` = soft-delete |
| `creadoPor` | UUID | ID del usuario que creó el evento |
| `creadoEn` | OffsetDateTime | Automático |
| `actualizadoEn` | OffsetDateTime | Automático |

---

## Endpoints

Base URL: `/api/v1/eventos`

| Método | Ruta | Rol mínimo | Descripción |
|---|---|---|---|
| `POST` | `/eventos` | `ADMIN_EMPRESA` | Crear evento |
| `GET` | `/eventos` | Cualquiera | Listar eventos visibles |
| `GET` | `/eventos/{id}` | Cualquiera | Obtener evento por ID |
| `PUT` | `/eventos/{id}` | `ADMIN_EMPRESA` | Actualizar evento |
| `PATCH` | `/eventos/{id}/desactivar` | `ADMIN_EMPRESA` | Soft-delete |

---

## Reglas de negocio

### Visibilidad

- Todos los usuarios autenticados ven: eventos de **su empresa** + eventos **globales** (activos)
- `ROLE_ADMIN` ve todos los eventos de la plataforma

### Crear evento

- `ROLE_ADMIN_EMPRESA` → el evento se asigna a su empresa. No puede crear eventos globales.
- `ROLE_ADMIN` → puede crear eventos globales (`esGlobal = true`) o para cualquier empresa.

### Actualizar evento

- Solo `ROLE_ADMIN` puede cambiar el campo `esGlobal` de un evento existente.
- `ROLE_ADMIN_EMPRESA` solo puede modificar los eventos de su propia empresa.
- Intentar modificar un evento de otra empresa → `403 Forbidden`.

### Soft-delete

- `ROLE_ADMIN_EMPRESA` solo puede desactivar eventos de su empresa.
- `ROLE_ADMIN` puede desactivar cualquier evento.

---

## Request / Response

### POST `/eventos` — Crear evento

**Request body:**
```json
{
  "titulo": "Jornada de puertas abiertas",
  "descripcion": "Te invitamos a conocer nuestras instalaciones.",
  "fechaInicio": "2026-05-10T10:00:00Z",
  "fechaFin": "2026-05-10T14:00:00Z",
  "esGlobal": false
}
```

**Response `201 Created`:**
```json
{
  "eventoId": "bb0e8400-e29b-41d4-a716-446655440000",
  "titulo": "Jornada de puertas abiertas",
  "descripcion": "Te invitamos a conocer nuestras instalaciones.",
  "esGlobal": false,
  "fechaInicio": "2026-05-10T10:00:00Z",
  "fechaFin": "2026-05-10T14:00:00Z",
  "activo": true,
  "empresaId": "660e8400-..."
}
```

### GET `/eventos` — Listar eventos

**Response `200 OK`:**
```json
[
  {
    "eventoId": "bb0e8400-...",
    "titulo": "Jornada de puertas abiertas",
    "fechaInicio": "2026-05-10T10:00:00Z",
    "fechaFin": "2026-05-10T14:00:00Z",
    "esGlobal": false,
    "empresaId": "660e8400-..."
  },
  {
    "eventoId": "cc0e8400-...",
    "titulo": "Feria de empresas EGM 2026",
    "fechaInicio": "2026-06-01T09:00:00Z",
    "fechaFin": null,
    "esGlobal": true,
    "empresaId": null
  }
]
```

