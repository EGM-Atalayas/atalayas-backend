# Módulo: Dashboard (`dashboard`)

> **Paquete:** `com.atalayas.backend.dashboard`
> **Audiencia:** Frontend, Backend
> **Última actualización:** Abril 2026 (rev. 3)

---

## ¿Qué hace este módulo?

Provee los datos agregados para los **paneles de control** de la plataforma. Hay dos tipos de dashboard:

- **Dashboard de Admin Empresa**: métricas sobre sus propios empleados + actividad reciente real
- **Dashboard de Superadmin (EGM)**: métricas globales de toda la plataforma, incidencias y actividad de auditoría

---

## Endpoints

Base URL: `/api/v1/dashboard`

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| `GET` | `/dashboard/admin/resumen` | `ADMIN_EMPRESA` | Métricas de la empresa del admin autenticado |
| `GET` | `/dashboard/admin/actividad` | `ADMIN_EMPRESA` | Actividad reciente real: progreso de empleados + módulos nuevos |
| `GET` | `/dashboard/superadmin/resumen` | `ADMIN` | Métricas globales básicas (legado) |
| `GET` | `/dashboard/superadmin` | `ADMIN` | Dashboard completo: métricas + incidencias + actividad de auditoría |
| `GET` | `/dashboard/superadmin/graficas` | `ADMIN` | Datos para los 3 gráficos del panel |

---

## Respuestas

### GET `/dashboard/admin/resumen` — Métricas de empresa

**Response `200 OK`:**
```json
{
  "nombreEmpresa": "Tech SL",
  "usuariosActivos": 23,
  "usuariosInactivos": 2
}
```

### GET `/dashboard/admin/actividad?limit=5` — Actividad reciente de empresa

Query param `limit` opcional (por defecto `5`). Devuelve los `limit` eventos más recientes ordenados por `timestamp DESC`.

**Response `200 OK`:**
```json
[
  { "tipo": "completado", "texto": "Ana García completó «Prevención de Riesgos»",       "timestamp": "2026-04-24T10:15:00Z" },
  { "tipo": "inicio",     "texto": "Carlos Ruiz inició «Protección de Datos»",           "timestamp": "2026-04-24T09:58:00Z" },
  { "tipo": "logro",      "texto": "María López obtuvo el 100% en Onboarding",           "timestamp": "2026-04-24T09:00:00Z" },
  { "tipo": "grupo",      "texto": "5 empleados completaron «Habilidades Comunicación»", "timestamp": "2026-04-24T08:30:00Z" },
  { "tipo": "nuevo",      "texto": "Nuevo módulo «Excel Avanzado» publicado",             "timestamp": "2026-04-23T16:00:00Z" }
]
```

#### Fuentes de datos

| Tipo | Tabla origen | Regla de generación |
|------|-------------|---------------------|
| `completado` | `trazabilidad_lectura` | `completado = true` — se excluyen los que ya generan un `logro` para el mismo usuario+módulo |
| `inicio` | `trazabilidad_lectura` | `completado = false AND tiempo_segundos > 0` |
| `logro` | `trazabilidad_lectura` | El empleado completó **todos** los contenidos activos del módulo (COUNT = total contenidos) |
| `grupo` | `trazabilidad_lectura` | ≥ 2 empleados distintos completaron el mismo módulo en el mismo día |
| `nuevo` | `modulo` | Módulo `activo = true` propio de la empresa **o** global (`empresa_id IS NULL`), ordenado por `fecha_creacion DESC` |

#### Notas

- El `timestamp` es **ISO 8601** — el frontend calcula el tiempo relativo.
- La deduplicación `logro` vs `completado` se aplica por par `(usuarioId, moduloId)`: si existe un logro para ese par, sus entradas individuales de `completado` se omiten.
- El `empresaId` se obtiene del token JWT; el admin empresa no necesita pasarlo.

### GET `/dashboard/superadmin` — Dashboard completo

**Response `200 OK`:**
```json
{
  "empresasAdheridas": 15,
  "empresasNuevasMes": 2,
  "empleadosRegistrados": 340,
  "empleadosNuevosMes": 18,
  "modulosPublicados": 67,
  "incidenciasAbiertas": 2,
  "incidenciasCriticas": 1,
  "actividadReciente": [
    { "id": 42, "texto": "Empresa \"Tech SL\" aprobada", "tipo": "success", "tiempo": "hace 5m" },
    { "id": 41, "texto": "Solicitud de \"OtraEmpresa SL\" rechazada y eliminada", "tipo": "warning", "tiempo": "hace 2h" }
  ]
}
```

> El campo `tiempo` es una cadena relativa calculada en backend: `"ahora mismo"`, `"hace Nm"`, `"hace Nh"`, `"hace Nd"`. Se devuelven los **últimos 10 eventos** del `audit_log` global.

### GET `/dashboard/superadmin/graficas` — Datos para gráficos

**Response `200 OK`:**
```json
{
  "evolucion": [
    { "mes": "Nov", "empresas": 10, "empleados": 280 },
    { "mes": "Dic", "empresas": 11, "empleados": 295 },
    { "mes": "Ene", "empresas": 12, "empleados": 310 },
    { "mes": "Feb", "empresas": 13, "empleados": 318 },
    { "mes": "Mar", "empresas": 14, "empleados": 330 },
    { "mes": "Abr", "empresas": 15, "empleados": 340 }
  ],
  "sectores": [
    { "name": "Tecnología", "value": 5 },
    { "name": "Consultoría", "value": 4 }
  ],
  "modulos": [
    { "nombre": "Onboarding General", "completados": 450, "pendientes": 120 },
    { "nombre": "Seguridad en el trabajo", "completados": 380, "pendientes": 95 }
  ]
}
```

---

## Descripción de los gráficos

### Evolución mensual (`evolucion`)

- Muestra los últimos **6 meses** con totales acumulados al final de cada mes
- Ordenado de más antiguo (izquierda) a más reciente (derecha)
- Útil para gráfico de líneas: crecimiento de empresas y empleados en el tiempo

### Sectores (`sectores`)

- Número de empresas agrupadas por sector de actividad
- Ordenado por volumen descendente
- **No incluye** empresas sin sector asignado
- Útil para gráfico de tarta/donut

### Módulos más activos (`modulos`)

- Top **10 módulos** con mayor actividad
- Ordenado por `completados` descendente
- Útil para gráfico de barras horizontales

---

## Actividad reciente — Superadmin

La actividad reciente del superadmin proviene de la tabla `audit_log` (ver [auditoria.md](auditoria.md)). Se devuelven los **últimos 10 eventos** ordenados por `creadoEn DESC`.

El campo `tiempo` es calculado en backend como cadena relativa al momento de la petición:

| Valor | Cuándo |
|---|---|
| `"ahora mismo"` | Menos de 1 minuto |
| `"hace Nm"` | Entre 1 y 59 minutos |
| `"hace Nh"` | Entre 1 y 23 horas |
| `"hace Nd"` | 1 día o más |

| Tipo | Color | Cuándo se genera |
|---|---|---|
| `success` | Verde | Empresa aprobada |
| `warning` | Ámbar | Solicitud rechazada y eliminada |
| `info` | Azul | Eventos informativos generales |
| `error` | Rojo | Errores del sistema, incidencias críticas |

---

## Actividad reciente — Admin Empresa

La actividad reciente del admin empresa se deriva en tiempo real desde `trazabilidad_lectura` y `modulo` **sin tabla adicional**. Ver endpoint `GET /dashboard/admin/actividad` arriba.

### Clases involucradas

| Clase | Responsabilidad |
|---|---|
| `DashboardService#getActividadEmpresa` | Orquesta las 5 queries, deduplica y ordena |
| `ProgressRepository#findCompletadosRecientes` | Contenidos completados más recientes |
| `ProgressRepository#findIniciadosRecientes` | Contenidos iniciados (no completados) |
| `ProgressRepository#findLogros` | Empleados que completaron todos los contenidos de un módulo |
| `ProgressRepository#findCompletadosGrupo` | ≥ 2 empleados completando el mismo módulo el mismo día |
| `ModuleRepository#findNuevosModulos` | Módulos publicados visibles para la empresa |
| `ProgressEventProjection` | Proyección JPA para eventos individuales de progreso |
| `GrupoProjection` | Proyección JPA para eventos de grupo |
| `NuevoModuloProjection` | Proyección JPA para módulos nuevos |
| `ActividadItemDto` | DTO de respuesta con `tipo`, `texto`, `timestamp` (ISO 8601) |
