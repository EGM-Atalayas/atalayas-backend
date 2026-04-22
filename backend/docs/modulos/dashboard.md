# Módulo: Dashboard (`dashboard`)

> **Paquete:** `com.atalayas.backend.dashboard`
> **Audiencia:** Frontend, Backend
> **Última actualización:** Abril 2026 (rev. 2)

---

## ¿Qué hace este módulo?

Provee los datos agregados para los **paneles de control** de la plataforma. Hay dos tipos de dashboard:

- **Dashboard de Admin Empresa**: métricas sobre sus propios empleados
- **Dashboard de Superadmin (EGM)**: métricas globales de toda la plataforma, incidencias y actividad reciente

---

## Endpoints

Base URL: `/api/v1/dashboard`

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| `GET` | `/dashboard/admin/resumen` | `ADMIN_EMPRESA` | Métricas de la empresa del admin autenticado |
| `GET` | `/dashboard/superadmin/resumen` | `ADMIN` | Métricas globales básicas (legado) |
| `GET` | `/dashboard/superadmin` | `ADMIN` | Dashboard completo: métricas + incidencias + actividad |
| `GET` | `/dashboard/superadmin/graficas` | `ADMIN` | Datos para los 3 gráficos del panel |

---

## Respuestas

### GET `/dashboard/admin/resumen` — Métricas de empresa

**Response `200 OK`:**
```json
{
  "totalEmpleados": 25,
  "empleadosActivos": 23,
  "empleadosInactivos": 2,
  "modulosActivos": 8,
  "contenidosActivos": 42
}
```

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
    {
      "id": 42,
      "texto": "Empresa \"Tech SL\" aprobada",
      "tipo": "success",
      "tiempo": "hace 5m"
    },
    {
      "id": 41,
      "texto": "Solicitud de \"OtraEmpresa SL\" rechazada y eliminada",
      "tipo": "warning",
      "tiempo": "hace 2h"
    }
  ]
}
```

> El campo `tiempo` es una cadena relativa calculada en el backend en el momento de la petición: `"ahora mismo"`, `"hace Nm"`, `"hace Nh"`, `"hace Nd"`. Se devuelven los **últimos 10 eventos** ordenados de más nuevo a más antiguo.

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
    { "name": "Consultoría", "value": 4 },
    { "name": "Industria", "value": 3 },
    { "name": "Servicios", "value": 3 }
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

## Actividad reciente

La actividad reciente proviene de la tabla `audit_log` (ver [auditoria.md](auditoria.md)). Se devuelven los **últimos 10 eventos** ordenados por `creadoEn DESC`.

El campo `tiempo` es calculado en backend como cadena relativa al momento de la petición:

| Valor | Cuándo |
|---|---|
| `"ahora mismo"` | Menos de 1 minuto |
| `"hace Nm"` | Entre 1 y 59 minutos |
| `"hace Nh"` | Entre 1 y 23 horas |
| `"hace Nd"` | 1 día o más |

Los tipos de entrada y su correspondencia visual en el frontend:

| Tipo | Color | Cuándo se genera |
|---|---|---|
| `success` | Verde | Empresa aprobada |
| `warning` | Ámbar | Solicitud rechazada y eliminada |
| `info` | Azul | Eventos informativos generales |
| `error` | Rojo | Errores del sistema, incidencias críticas |

