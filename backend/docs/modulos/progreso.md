# Módulo: Progreso (`progress`)

> **Paquete:** `com.atalayas.backend.progress`
> **Tabla BD:** `trazabilidad_lectura`
> **Audiencia:** Frontend, Backend
> **Última actualización:** Abril 2026

---

## ¿Qué hace este módulo?

Registra y consulta el **progreso de cada empleado** sobre los contenidos formativos. Permite saber:
- Si un empleado ha completado un contenido
- Cuánto tiempo ha dedicado a cada contenido
- A qué porcentaje ha llegado
- Si tiene la versión más reciente del contenido

---

## Entidad: `UserProgress` (tabla `trazabilidad_lectura`)

| Campo | Tipo | Descripción |
|---|---|---|
| `registroId` | UUID (PK) | Identificador único del registro |
| `usuarioId` | UUID | FK al empleado |
| `contenidoId` | UUID | FK al contenido |
| `moduloId` | UUID (nullable) | FK al módulo (para facilitar queries de dashboard) |
| `empresaId` | UUID | FK a la empresa del empleado |
| `completado` | boolean | `true` = el empleado marcó el contenido como terminado (**irreversible**) |
| `fechaCompletado` | OffsetDateTime (nullable) | Cuándo lo completó. `null` si aún no terminó |
| `tiempoSegundos` | int | Tiempo acumulado de lectura/visualización en segundos |
| `porcentajeCompletado` | int | Porcentaje de completado (0–100) |
| `versionLeida` | int | Versión del contenido que el empleado leyó |
| `hashAceptacion` | String (nullable) | Firma digital al completar (para cumplimiento) |
| `fechaInicio` | OffsetDateTime | Primera vez que abrió el contenido (automático) |
| `actualizadoEn` | OffsetDateTime | Última actualización (automático) |

### Restricción única

Existe una **constraint de unicidad** en `(usuario_id, contenido_id)`. Un empleado solo tiene **un registro por contenido**, que se va actualizando conforme avanza.

---

## Endpoints

Base URL: `/api/v1/progreso`

| Método | Ruta | Rol mínimo | Descripción |
|---|---|---|---|
| `POST` | `/progreso` | Cualquiera | Registrar / actualizar progreso |
| `GET` | `/progreso/me` | Cualquiera | Todo el progreso del usuario autenticado |
| `GET` | `/progreso/contenido/{contenidoId}` | Cualquiera | Estado sobre un contenido concreto |
| `GET` | `/progreso/usuario/{usuarioId}` | `ADMIN_EMPRESA` | Progreso de un empleado |
| `GET` | `/progreso/empresa/{empresaId}` | `ADMIN_EMPRESA` | Progreso de todos los empleados de una empresa |

---

## Reglas de negocio

### El campo `completado` es irreversible

Una vez que `completado = true`, **no puede volver a `false`**. Esto garantiza la integridad de la trazabilidad de cumplimiento. Si el servicio recibe un `completado = false` para un registro ya completado, simplemente lo ignora.

### El tiempo se acumula

Cada llamada a `POST /progreso` **suma** el `tiempoSegundos` enviado al tiempo ya acumulado. No se sobreescribe el total.

```
1ª llamada:  tiempoSegundos = 30  →  BD: tiempoSegundos = 30
2ª llamada:  tiempoSegundos = 45  →  BD: tiempoSegundos = 75
```

### Control de acceso

- Un `ROLE_EMPLEADO` solo puede registrar su propio progreso. Si intenta registrar el de otro usuario → `403`.
- `ROLE_ADMIN_EMPRESA` puede ver el progreso de empleados de **su empresa** → si intenta ver el de otra empresa → `403`.
- `ROLE_ADMIN` puede ver el progreso de cualquier usuario.

### Estado derivado

El campo `estado` no se persiste en BD. Se calcula en el servicio según:

```
completado = true                → estado = "COMPLETADO"
completado = false, tiempo > 0   → estado = "EN_PROGRESO"
completado = false, tiempo = 0   → estado = "NO_INICIADO"
```

---

## Request / Response

### POST `/progreso` — Registrar progreso

**Request body:**
```json
{
  "contenidoId": "880e8400-e29b-41d4-a716-446655440000",
  "tiempoSegundos": 120,
  "porcentajeCompletado": 75,
  "completado": false
}
```

Para marcar como completado:
```json
{
  "contenidoId": "880e8400-...",
  "tiempoSegundos": 45,
  "porcentajeCompletado": 100,
  "completado": true
}
```

**Response `200 OK`:**
```json
{
  "registroId": "aa0e8400-...",
  "contenidoId": "880e8400-...",
  "completado": false,
  "tiempoSegundos": 120,
  "porcentajeCompletado": 75,
  "estado": "EN_PROGRESO",
  "fechaInicio": "2026-04-15T09:00:00Z",
  "fechaCompletado": null
}
```

### GET `/progreso/empresa/{empresaId}` — Dashboard de empresa

Devuelve el progreso agregado de todos los empleados de la empresa. Útil para el dashboard del admin.

**Response `200 OK`:**
```json
[
  {
    "usuarioId": "550e8400-...",
    "nombre": "Ana Martínez",
    "contenidosCompletados": 8,
    "contenidosTotales": 12,
    "porcentajeGeneral": 66,
    "tiempoTotalSegundos": 3600
  }
]
```

---

## Diagrama de flujo

```
Empleado abre un contenido
        │
        ▼
Frontend inicia contador de tiempo
        │
        │  (cada 30s o al cerrar el contenido)
        ▼
POST /progreso
  { contenidoId, tiempoSegundos: X, porcentaje: Y, completado: false }
        │
        ▼
Backend busca registro por (usuarioId, contenidoId)
        │
        ├─ No existe → crea registro nuevo
        │
        └─ Existe     → actualiza (acumula tiempo, actualiza porcentaje)
                        Si ya estaba completado, no cambia completado=false

Empleado llega al final y hace clic en "Completar"
        │
        ▼
POST /progreso
  { contenidoId, tiempoSegundos: Z, porcentaje: 100, completado: true }
        │
        ▼
Backend marca completado=true, guarda fechaCompletado = ahora()
(Nunca se puede revertir)
```

