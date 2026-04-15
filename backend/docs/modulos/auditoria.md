# Módulo: Auditoría (`audit`)

> **Paquete:** `com.atalayas.backend.audit`
> **Tabla BD:** `audit_log`
> **Audiencia:** Backend
> **Última actualización:** Abril 2026

---

## ¿Qué hace este módulo?

Registra **eventos relevantes del sistema** en una tabla de auditoría. Estos registros alimentan la sección "Actividad reciente" del dashboard del superadmin.

No expone endpoints directamente al frontend. Los datos se consumen a través del dashboard (`GET /dashboard/superadmin`).

---

## Entidad: `AuditLog` (tabla `audit_log`)

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | Long (PK, autoincremental) | Identificador único |
| `texto` | String (max 500 chars) | Descripción del evento en lenguaje natural |
| `tipo` | String | Clasificación del evento: `info`, `success`, `warning`, `error` |
| `creadoEn` | OffsetDateTime | Fecha y hora del evento (automático, no modificable) |

---

## Tipos de entrada

| Tipo | Color en UI | Ejemplos |
|---|---|---|
| `success` | Verde | "Empresa 'Tech SL' aprobada y activada" |
| `info` | Azul | "Nuevo usuario registrado: ana@empresa.com" |
| `warning` | Naranja | "5 intentos de login fallidos para usuario@empresa.com" |
| `error` | Rojo | "Error al enviar email de bienvenida a empresa X" |

---

## Cómo se usa en el backend

El `AuditService` se inyecta en los servicios que generan eventos relevantes. Se llama con un texto descriptivo y un tipo:

```java
// Ejemplo en CompanyService al aprobar una empresa
auditService.registrar(
    "Empresa '" + empresa.getNombreEmpresa() + "' aprobada y activada",
    "success"
);

// Ejemplo en AuthService al detectar cuenta bloqueada
auditService.registrar(
    "Cuenta bloqueada por múltiples intentos fallidos: " + email,
    "warning"
);
```

---

## Cómo se consume en el dashboard

El `DashboardService` consulta los últimos N registros de `audit_log` ordenados por `creadoEn DESC` para poblar la sección "Actividad reciente" del dashboard del superadmin.

```
GET /dashboard/superadmin
  └── Response.actividadReciente = últimas 10 entradas de audit_log
```

---

## Módulo de incidencias (relacionado)

El módulo `incidencia` es complementario al de auditoría. Mientras que `audit_log` registra **eventos automáticos del sistema**, las incidencias son **registros manuales** del superadmin para problemas que requieren seguimiento.

### Entidad `Incidencia` (tabla `incidencia`)

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | Long (PK) | Identificador único |
| `titulo` | String | Título de la incidencia |
| `descripcion` | TEXT | Descripción detallada |
| `estado` | Enum | `ABIERTA`, `EN_PROGRESO`, `CERRADA` |
| `prioridad` | Enum | `BAJA`, `NORMAL`, `ALTA`, `CRITICA` |
| `creadoEn` | OffsetDateTime | Fecha de creación (automático) |

Las incidencias se muestran en el dashboard como contadores por estado:

```json
"incidencias": {
  "abiertas": 2,
  "enProgreso": 1,
  "cerradas": 8
}
```

