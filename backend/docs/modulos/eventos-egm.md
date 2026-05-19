# Módulo: Eventos EGM (`eventos`)

> **Paquete:** `com.atalayas.backend.eventos`
> **Tabla BD:** `evento`
> **Audiencia:** Frontend, Backend
> **Última actualización:** Mayo 2026

---

## ¿Qué hace este módulo?

Gestiona los **eventos corporativos del parque EGM Atalayas** (jornadas, ferias, actividades del parque). Son eventos globales gestionados exclusivamente por el superadmin y visibles para todos los usuarios autenticados.

> ⚠️ **No confundir con Eventos de Comunidad** (`/api/v1/comunidad/eventos`), que son eventos por empresa. Este módulo es de alcance global, solo gestionable por `ROLE_ADMIN`.

---

## Endpoints

Base URL: `/api/v1/eventos`

| Método | Ruta | Rol mínimo | Descripción |
|--------|------|-----------|-------------|
| `POST` | `/eventos` | `ADMIN` | Crear evento EGM |
| `GET` | `/eventos` | Cualquier autenticado | Listar eventos activos |
| `PUT` | `/eventos/{id}` | `ADMIN` | Actualizar evento completo |
| `PATCH` | `/eventos/{id}/desactivar` | `ADMIN` | Cancelar evento (soft delete) |

---

## Contratos

### `POST /eventos` — Crear evento

**Request body (`EventoRequest`):**
```json
{
  "titulo": "Jornada de Puertas Abiertas EGM 2026",
  "descripcion": "Visita las instalaciones del parque y conoce todas las empresas",
  "fecha": "2026-06-15",
  "horaInicio": "10:00:00",
  "horaFin": "18:00:00",
  "lugar": "Parque Empresarial EGM Atalayas - Pabellón Central",
  "urlInfo": "https://www.egmatalayas.com/jornada-puertas-abiertas",
  "imagenUrl": "https://cdn.egmatalayas.com/eventos/puertas-abiertas.jpg"
}
```

| Campo | Tipo | Requerido | Descripción |
|-------|------|:---------:|-------------|
| `titulo` | `string` | ✅ | Nombre del evento (`@NotBlank`) |
| `descripcion` | `string` | ❌ | Descripción libre |
| `fecha` | `LocalDate` (`YYYY-MM-DD`) | ✅ | Fecha del evento (`@NotNull`) |
| `horaInicio` | `LocalTime` (`HH:mm:ss`) | ❌ | Hora de inicio |
| `horaFin` | `LocalTime` (`HH:mm:ss`) | ❌ | Hora de fin |
| `lugar` | `string` | ❌ | Lugar del evento |
| `urlInfo` | `string` | ❌ | URL de información adicional |
| `imagenUrl` | `string` | ❌ | URL de la imagen del evento |

**Response `201 Created` (`EventoResponse`):**
```json
{
  "eventoId": "uuid",
  "titulo": "Jornada de Puertas Abiertas EGM 2026",
  "descripcion": "...",
  "fecha": "2026-06-15",
  "horaInicio": "10:00:00",
  "horaFin": "18:00:00",
  "lugar": "Parque Empresarial EGM Atalayas - Pabellón Central",
  "urlInfo": "https://...",
  "imagenUrl": "https://...",
  "estado": "PROXIMO",
  "creadoPor": "uuid-admin",
  "activo": true,
  "creadoEn": "2026-05-14T10:00:00Z",
  "actualizadoEn": null
}
```

---

### `GET /eventos` — Listar eventos activos

Devuelve array de `EventoResponse`. Solo eventos con `activo = true`.

---

### `PUT /eventos/{id}` — Actualizar evento

Mismos campos que POST. `404` si no existe. `403` si no es `ROLE_ADMIN`.

---

### `PATCH /eventos/{id}/desactivar` — Cancelar evento

El evento pasa a `estado = CANCELADO` y `activo = false`.

| Código | Cuándo |
|--------|--------|
| `200` | Cancelado correctamente. Devuelve `EventoResponse` actualizado |
| `400` | El evento ya estaba cancelado |
| `404` | No encontrado |

---

## Enum `EstadoEvento`

| Valor | Descripción |
|-------|-------------|
| `PROXIMO` | Evento futuro pendiente |
| `EN_CURSO` | El evento está ocurriendo ahora |
| `FINALIZADO` | El evento ya ha terminado |
| `CANCELADO` | El evento fue cancelado |

---

## Diferencias con Eventos de Comunidad

| | `/api/v1/eventos` (este módulo) | `/api/v1/comunidad/eventos` |
|---|---|---|
| **Scope** | Global del parque EGM | Por empresa (multi-tenant) |
| **Quién gestiona** | Solo `ROLE_ADMIN` | `ROLE_ADMIN` + `ROLE_ADMIN_EMPRESA` |
| **Tipo de fecha** | `LocalDate` + `LocalTime` | `OffsetDateTime` |
| **Flag global** | No aplica (siempre global) | `esGlobal` flag |
| **Uso** | Eventos del parque (jornadas, ferias) | Eventos internos de empresa |

