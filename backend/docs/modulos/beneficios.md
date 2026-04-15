# Módulo: Beneficios (`rewards`)

> **Paquete:** `com.atalayas.backend.rewards`
> **Tabla BD:** `beneficio`
> **Audiencia:** Frontend, Backend
> **Última actualización:** Abril 2026

---

## ¿Qué hace este módulo?

Gestiona los **beneficios y ventajas** que ofrece el parque empresarial EGM a los empleados de las empresas del área. Los beneficios pueden ser:

- **Globales**: ofrecidos por EGM para todos los empleados del parque (descuentos en restaurantes, aparcamiento, gimnasio, etc.)
- **De empresa**: ventajas específicas que ofrece una empresa a sus propios empleados (seguro médico, teletrabajo, etc.)

---

## Entidad: `Benefit` (tabla `beneficio`)

| Campo | Tipo | Descripción |
|---|---|---|
| `beneficioId` | UUID (PK) | Identificador único |
| `empresaId` | UUID (nullable) | Empresa propietaria. `null` = beneficio global |
| `titulo` | String | Título visible del beneficio |
| `descripcion` | TEXT (nullable) | Descripción detallada y cómo acceder a él |
| `urlInfo` | String (nullable) | URL con más información o formulario de solicitud |
| `activo` | boolean | `false` = soft-delete |
| `creadoPor` | UUID | ID del usuario que lo creó |
| `creadoEn` | OffsetDateTime | Automático |
| `actualizadoEn` | OffsetDateTime | Automático |

---

## Endpoints

Base URL: `/api/v1/beneficios`

| Método | Ruta | Rol mínimo | Descripción |
|---|---|---|---|
| `POST` | `/beneficios` | `ADMIN_EMPRESA` | Crear beneficio |
| `GET` | `/beneficios` | Cualquiera | Listar beneficios visibles |
| `PUT` | `/beneficios/{id}` | `ADMIN_EMPRESA` | Actualizar beneficio |
| `PATCH` | `/beneficios/{id}/desactivar` | `ADMIN_EMPRESA` | Soft-delete |

---

## Reglas de negocio

### Visibilidad

Todos los usuarios autenticados ven:
- Beneficios de **su empresa** (activos)
- Beneficios **globales** (activos)

### Crear beneficio

- `ROLE_ADMIN_EMPRESA` → el beneficio se asigna a su empresa. No puede crear beneficios globales (`empresaId = null`).
- `ROLE_ADMIN` → puede crear beneficios globales.

### Actualizar / desactivar

- `ROLE_ADMIN_EMPRESA` solo puede modificar o desactivar beneficios de **su propia empresa**.
- Intentar modificar un beneficio de otra empresa → `403 Forbidden`.
- `ROLE_ADMIN` puede actualizar cualquier beneficio.

---

## Request / Response

### POST `/beneficios` — Crear beneficio

**Request body:**
```json
{
  "titulo": "Descuento en el restaurante EGM",
  "descripcion": "Los empleados del parque tienen un 15% de descuento en el restaurante del área.",
  "urlInfo": "https://restaurante-egm.com/descuento-empleados"
}
```

**Response `201 Created`:**
```json
{
  "beneficioId": "dd0e8400-e29b-41d4-a716-446655440000",
  "titulo": "Descuento en el restaurante EGM",
  "descripcion": "Los empleados del parque tienen un 15% de descuento en el restaurante del área.",
  "urlInfo": "https://restaurante-egm.com/descuento-empleados",
  "activo": true,
  "empresaId": null
}
```

### GET `/beneficios` — Listar beneficios

**Response `200 OK`:**
```json
[
  {
    "beneficioId": "dd0e8400-...",
    "titulo": "Descuento en el restaurante EGM",
    "descripcion": "...",
    "urlInfo": "https://...",
    "empresaId": null,
    "activo": true
  },
  {
    "beneficioId": "ee0e8400-...",
    "titulo": "Seguro médico privado",
    "descripcion": "Cobertura médica completa para todos los empleados.",
    "urlInfo": null,
    "empresaId": "660e8400-...",
    "activo": true
  }
]
```

