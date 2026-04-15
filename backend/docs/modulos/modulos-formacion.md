# Módulo: Módulos de Formación (`module`)

> **Paquete:** `com.atalayas.backend.module`
> **Tabla BD:** `modulo`
> **Audiencia:** Frontend, Backend
> **Última actualización:** Abril 2026

---

## ¿Qué hace este módulo?

Un **módulo de formación** es un contenedor que agrupa contenidos formativos relacionados (artículos, vídeos, PDFs, evaluaciones). Los empleados los ven en la sección "Mi formación".

Los módulos pueden ser:
- **De empresa**: creados por el admin de una empresa para sus propios empleados
- **Globales**: creados por EGM (`empresaId = null`), visibles para todos los empleados del parque

---

## Entidad: `TrainingModule` (tabla `modulo`)

| Campo | Tipo | Descripción |
|---|---|---|
| `moduloId` | UUID (PK) | Identificador único |
| `nombre` | String | Nombre visible en la plataforma |
| `descripcion` | TEXT (nullable) | Descripción que ve el empleado antes de entrar |
| `empresaId` | UUID (nullable) | `null` = módulo global visible para todas las empresas |
| `tipoModulo` | Enum | Ver tipos abajo |
| `orden` | Integer (nullable) | Posición en el listado (menor número = primero) |
| `esEspecializadoIa` | boolean | `true` si fue generado o asistido por IA |
| `activo` | boolean | `false` = soft-delete (invisible para empleados) |
| `fechaCreacion` | OffsetDateTime | Automático, no modificable |
| `actualizadoEn` | OffsetDateTime | Automático |

### Tipos de módulo (`ModuleType`)

| Valor | Descripción |
|---|---|
| `GENERAL` | Formación general, disponible para todas las empresas |
| `ESPECIALIZADO` | Formación específica de una empresa |
| `ESPECIALIZADO_IA` | Generado o asistido por IA |
| `CUMPLIMIENTO` | Normativa y cumplimiento legal |
| `ONBOARDING` | Incorporación de nuevos empleados |

---

## Endpoints

Base URL: `/api/v1/modulos`

| Método | Ruta | Rol mínimo | Descripción |
|---|---|---|---|
| `POST` | `/modulos` | `ADMIN_EMPRESA` | Crear módulo |
| `GET` | `/modulos` | Cualquiera | Listar módulos visibles según rol |
| `GET` | `/modulos/{id}` | Cualquiera | Obtener módulo por ID |
| `PUT` | `/modulos/{id}` | `ADMIN_EMPRESA` | Actualizar módulo completo |
| `PATCH` | `/modulos/{id}/desactivar` | `ADMIN_EMPRESA` | Soft-delete |

---

## Reglas de negocio

### Visibilidad por rol

```
ROLE_EMPLEADO / ROLE_ADMIN_EMPRESA de empresa A:
  Módulos visibles = módulos de empresa A (activos) + módulos globales (activos)

ROLE_ADMIN (superadmin):
  Módulos visibles = todos los módulos de la plataforma (incluyendo inactivos)
```

### Crear módulo

- `ROLE_ADMIN_EMPRESA` → el módulo se asigna automáticamente a su empresa. No puede crear módulos globales.
- `ROLE_ADMIN` → puede crear módulos globales (`empresaId = null`) o para cualquier empresa.

### Acceso por ID

- `ROLE_ADMIN_EMPRESA` que intenta ver un módulo de otra empresa → `403 Forbidden`
- `ROLE_ADMIN` puede ver cualquier módulo

### Soft-delete

`activo = false` oculta el módulo del listado de empleados pero **conserva todos los contenidos y el progreso histórico** de los empleados que lo completaron.

---

## Request / Response

### POST `/modulos` — Crear módulo

**Request body:**
```json
{
  "nombre": "Bienvenida a la empresa",
  "descripcion": "Todo lo que necesitas saber sobre nosotros",
  "tipoModulo": "ONBOARDING",
  "orden": 1,
  "empresaId": null
}
```
> `empresaId` solo tiene efecto si quien llama es `ROLE_ADMIN`. Para `ROLE_ADMIN_EMPRESA` se ignora.

**Response `201 Created`:**
```json
{
  "moduloId": "770e8400-e29b-41d4-a716-446655440000",
  "nombre": "Bienvenida a la empresa",
  "descripcion": "Todo lo que necesitas saber sobre nosotros",
  "tipoModulo": "ONBOARDING",
  "orden": 1,
  "empresaId": "660e8400-e29b-41d4-a716-446655440001",
  "activo": true,
  "esEspecializadoIa": false,
  "fechaCreacion": "2026-04-15T10:00:00Z"
}
```

### GET `/modulos` — Listar módulos

**Response `200 OK`:**
```json
[
  {
    "moduloId": "770e8400-...",
    "nombre": "Bienvenida a la empresa",
    "tipoModulo": "ONBOARDING",
    "orden": 1,
    "activo": true,
    "numContenidos": 5
  }
]
```

---

## Relación con otros módulos

```
TrainingModule
     │
     │ 1:N
     ▼
ContentItem  ──────► UserProgress (trazabilidad por empleado)
     │
     │ 1:N (solo EVALUACION)
     ▼
 Question
```

Un módulo contiene N contenidos. El progreso del empleado se registra a nivel de contenido individual, no a nivel de módulo.

