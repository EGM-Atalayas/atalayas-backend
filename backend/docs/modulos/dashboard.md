# Módulo: Dashboard (`dashboard`)

> **Paquete:** `com.atalayas.backend.dashboard`
> **Audiencia:** Frontend, Backend
> **Última actualización:** Abril 2026

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
  "metricas": {
    "totalEmpresas": 15,
    "empresasActivas": 12,
    "totalEmpleados": 340,
    "empleadosActivos": 318,
    "modulosActivos": 67,
    "contenidosActivos": 420
  },
  "incidencias": {
    "abiertas": 2,
    "enProgreso": 1,
    "cerradas": 8
  },
  "actividadReciente": [
    {
      "id": 42,
      "texto": "Nueva empresa 'Tech SL' aprobada",
      "tipo": "success",
      "creadoEn": "2026-04-15T09:30:00Z"
    },
    {
      "id": 41,
      "texto": "Incidencia #3 marcada como resuelta",
      "tipo": "info",
      "creadoEn": "2026-04-14T16:00:00Z"
    }
  ]
}
```

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

La actividad reciente proviene de la tabla `audit_log`. Los tipos de entrada son:

| Tipo | Color sugerido | Cuándo se genera |
|---|---|---|
| `success` | Verde | Empresa aprobada, módulo completado masivamente |
| `info` | Azul | Nuevo usuario registrado, nuevo módulo publicado |
| `warning` | Naranja | Intento de login fallido repetido, empresa próxima a expirar |
| `error` | Rojo | Error del sistema, incidencia crítica |

Ver [auditoria.md](auditoria.md) para más detalles sobre el sistema de auditoría.

