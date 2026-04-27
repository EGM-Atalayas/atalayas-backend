# Módulo: Incidencias (`incidencia`)

> **Paquete:** `com.atalayas.backend.incidencia`
> **Audiencia:** Frontend (superadmin), Backend
> **Última actualización:** Abril 2026 (rev. 1)

---

## ¿Qué hace este módulo?

Permite al superadmin EGM registrar, consultar y cerrar **incidencias de plataforma**.

- Las incidencias pueden ser **globales** (`empresaId = null`) o **asociadas a una empresa concreta**.
- Al crear una incidencia con `prioridad = CRITICA`, se genera automáticamente un evento `error` en el `audit_log`, que aparece en `actividadReciente` del dashboard del superadmin.

---

## Endpoints

Base URL: `/api/v1/incidencias`  
Rol requerido: **`ROLE_ADMIN`** en todos los endpoints.

| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/incidencias` | Crear nueva incidencia |
| `GET` | `/incidencias` | Listar todas ordenadas por `creadoEn DESC` |
| `PATCH` | `/incidencias/{id}/cerrar` | Cerrar incidencia (estado → `CERRADA`) |

---

## Contratos

### `POST /incidencias` — Crear incidencia

**Request body:**
```json
{
  "titulo":      "Caída del servicio de notificaciones",
  "descripcion": "El servicio de emails no responde desde las 10:00.",
  "prioridad":   "CRITICA",
  "empresaId":   null
}
```

| Campo | Tipo | Requerido | Descripción |
|-------|------|:---------:|-------------|
| `titulo` | `string` | ✅ | Descripción corta (max 300 chars) |
| `descripcion` | `string` | ❌ | Detalle libre |
| `prioridad` | `NORMAL` \| `CRITICA` | ❌ | Por defecto `NORMAL` |
| `empresaId` | `UUID` \| `null` | ❌ | `null` = incidencia global |

**Response `201 Created`:** objeto `IncidenciaResponse` (ver abajo).

**Efecto secundario:** si `prioridad = CRITICA` → entrada en `audit_log` con `tipo = "error"` y texto `"Incidencia crítica abierta: {titulo}"`.

---

### `GET /incidencias` — Listar todas

**Response `200 OK`:**
```json
[
  {
    "id": 5,
    "titulo": "Caída del servicio de notificaciones",
    "descripcion": "El servicio de emails no responde desde las 10:00.",
    "estado": "ABIERTA",
    "prioridad": "CRITICA",
    "empresaId": null,
    "creadoEn": "2026-04-27T10:05:00Z"
  },
  {
    "id": 4,
    "titulo": "Error de importación CSV en Famosa S.L.",
    "descripcion": null,
    "estado": "CERRADA",
    "prioridad": "NORMAL",
    "empresaId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "creadoEn": "2026-04-20T08:30:00Z"
  }
]
```

---

### `PATCH /incidencias/{id}/cerrar` — Cerrar incidencia

**Response `200 OK`:** objeto `IncidenciaResponse` con `estado = "CERRADA"`.  
**`404 Not Found`** si el `id` no existe.

---

## Modelo de datos

### Entidad `Incidencia` → tabla `incidencia`

| Columna | Tipo SQL | Descripción |
|---------|----------|-------------|
| `id` | `BIGSERIAL PK` | Identificador autoincremental |
| `titulo` | `VARCHAR(300) NOT NULL` | Título de la incidencia |
| `descripcion` | `TEXT` | Descripción detallada (nullable) |
| `estado` | `VARCHAR(20) NOT NULL` | `ABIERTA` \| `CERRADA` (default `ABIERTA`) |
| `prioridad` | `VARCHAR(20) NOT NULL` | `NORMAL` \| `CRITICA` (default `NORMAL`) |
| `empresa_id` | `UUID REFERENCES empresa` | FK nullable — `null` = global |
| `creado_en` | `TIMESTAMPTZ NOT NULL` | Timestamp de creación (auto) |

> DDL completo en [`docs/sql/create_audit_log_and_incidencia.sql`](../sql/create_audit_log_and_incidencia.sql).

---

## Integración con Audit Log

```
POST /incidencias  (prioridad=CRITICA)
        │
        ▼
IncidenciaService#crear()
        │
        ├── incidenciaRepository.save(...)
        │
        └── auditService.registrar(
                "Incidencia crítica abierta: {titulo}",
                "error"
            )  ← Propagation.REQUIRES_NEW
                  (se graba aunque falle la transacción padre)
```

El evento aparece inmediatamente en `GET /dashboard/superadmin` → `actividadReciente`.

---

## Clases involucradas

| Clase | Responsabilidad |
|-------|-----------------|
| `Incidencia` | Entidad JPA — tabla `incidencia` |
| `EstadoIncidencia` | Enum: `ABIERTA`, `CERRADA` |
| `PrioridadIncidencia` | Enum: `NORMAL`, `CRITICA` |
| `IncidenciaRequest` | DTO de entrada con validación `@NotBlank` |
| `IncidenciaResponse` | DTO de salida con todos los campos + `creadoEn` |
| `IncidenciaMapper` | `toEntity()` / `toResponse()` — patrón `@Component` |
| `IncidenciaService` | Lógica de negocio + disparo de audit |
| `IncidenciaController` | REST bajo `/api/v1/incidencias` |
| `IncidenciaRepository` | `findAllByOrderByCreadoEnDesc()`, `findAllByEmpresaIdOrderByCreadoEnDesc(UUID)` |

