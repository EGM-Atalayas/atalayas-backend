# Módulo: Dashboard (`dashboard`)

> **Paquete:** `com.atalayas.backend.dashboard`
> **Audiencia:** Frontend, Backend
> **Última actualización:** Abril 2026 (rev. 4)

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
| `GET` | `/dashboard/admin/actividad?limit=5` | `ADMIN_EMPRESA` | Actividad reciente real: progreso de empleados + módulos nuevos |
| `GET` | `/dashboard/superadmin/resumen` | `ADMIN` | Métricas globales básicas (legado) |
| `GET` | `/dashboard/superadmin?limit=10` | `ADMIN` | Dashboard completo: métricas + incidencias + actividad de auditoría. Query param `limit` opcional (por defecto `10`). |
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
- La deduplicación `logro` vs `completado` se aplica por par `(usuarioId, moduloId)`.
- El `empresaId` se obtiene del token JWT.

### GET `/dashboard/superadmin?limit=10` — Dashboard completo

Query param `limit` controla cuántos eventos devuelve `actividadReciente` (por defecto `10`).

**Response `200 OK`:**
```json
{
  "empresasAdheridas": 24,
  "empresasNuevasMes": 2,
  "empleadosRegistrados": 310,
  "empleadosNuevosMes": 15,
  "modulosPublicados": 8,
  "incidenciasAbiertas": 3,
  "incidenciasCriticas": 1,
  "actividadReciente": [
    { "id": 101, "texto": "Nueva empresa adherida: Sprinter S.L.",         "tiempo": "Hace 10 minutos", "tipo": "success" },
    { "id": 100, "texto": "Solicitud rechazada: Empresa Ejemplo S.A.",     "tiempo": "Hace 2 horas",    "tipo": "error"   },
    { "id": 99,  "texto": "Incidencia crítica abierta: caída de servicio", "tiempo": "Ayer",            "tipo": "error"   },
    { "id": 98,  "texto": "Nuevo empleado registrado en Famosa",           "tiempo": "12 abr 2025",     "tipo": "info"    }
  ]
}
```

### GET `/dashboard/superadmin/graficas` — Datos para gráficos

**Response `200 OK`:**
```json
{
  "evolucion": [
    { "mes": "Nov", "empresas": 10, "empleados": 280 },
    { "mes": "Abr", "empresas": 15, "empleados": 340 }
  ],
  "sectores": [
    { "name": "Tecnología", "value": 5 }
  ],
  "modulos": [
    { "nombre": "Onboarding General", "completados": 450, "pendientes": 120 }
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

La actividad reciente del superadmin proviene de la tabla `audit_log`. Se devuelven los últimos `limit` eventos (por defecto **10**) ordenados por `creadoEn DESC`, paginados con `PageRequest`.

El campo `tiempo` es calculado en backend como cadena relativa al momento de la petición:

| Valor | Cuándo |
|---|---|
| `"Ahora mismo"` | < 1 minuto |
| `"Hace N minuto(s)"` | 1–59 minutos |
| `"Hace N hora(s)"` | 1–23 horas |
| `"Ayer"` | 1 día exacto |
| `"Hace N días"` | 2–6 días |
| `"d MMM yyyy"` (ej. `"12 abr 2025"`) | 7 días o más |

#### Eventos registrados en `audit_log`

| Sección | Texto generado | `tipo` | Servicio que lo registra |
|---------|---------------|--------|--------------------------|
| Empresas | `Empresa "X" aprobada` | `success` | `CompanyService#resolverSolicitud` |
| Empresas | `Solicitud de "X" rechazada y eliminada` | `warning` | `CompanyService#resolverSolicitud` |
| Incidencias | `Incidencia crítica abierta: {titulo}` | `error` | `IncidenciaService#crear` |

> Todos los eventos se persisten con `Propagation.REQUIRES_NEW` — se graban aunque falle la transacción padre.

---

## Actividad reciente — Admin Empresa

La actividad reciente del admin empresa se deriva en tiempo real desde `trazabilidad_lectura` y `modulo` **sin tabla adicional**. Ver endpoint `GET /dashboard/admin/actividad` arriba.

### Clases involucradas

| Clase | Responsabilidad |
|---|---|
| `DashboardService#getActividadEmpresa` | Orquesta las 5 queries, deduplica y ordena |
| `DashboardService#getSuperAdminDashboard(int limit)` | Agrega métricas globales y delega en `AuditService` |
| `AuditService#getActividadReciente(int limit)` | Pagina `audit_log` con `PageRequest.of(0, limit)` |
| `AuditService#registrar(texto, tipo)` | Persiste evento en transacción independiente |
| `ProgressRepository#findCompletadosRecientes` | Contenidos completados más recientes |
| `ProgressRepository#findIniciadosRecientes` | Contenidos iniciados (no completados) |
| `ProgressRepository#findLogros` | Empleados que completaron todos los contenidos de un módulo |
| `ProgressRepository#findCompletadosGrupo` | ≥ 2 empleados completando el mismo módulo el mismo día |
| `ModuleRepository#findNuevosModulos` | Módulos publicados visibles para la empresa |
| `AuditLogRepository#findAllByOrderByCreadoEnDesc(Pageable)` | Query paginada del log de auditoría |
| `ActividadItemDto` | DTO de actividad empresa con `tipo`, `texto`, `timestamp` (ISO 8601) |
| `ActividadRecienteDto` | DTO de actividad superadmin con `id`, `texto`, `tiempo`, `tipo` |

---

## Tablas de base de datos

| Tabla | Descripción |
|-------|-------------|
| `audit_log` | Eventos de auditoría global. DDL en [`docs/sql/create_audit_log_and_incidencia.sql`](../sql/create_audit_log_and_incidencia.sql) |
| `incidencia` | Incidencias de plataforma. Gestionadas vía `/api/v1/incidencias` |
| `trazabilidad_lectura` | Progreso de empleados (fuente de actividad del admin empresa) |
