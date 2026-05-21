# Módulo: Servicios (`servicios`)

> **Paquete:** `com.atalayas.backend.servicios`
> **Tabla BD:** `servicio`
> **Audiencia:** Frontend, Backend
> **Última actualización:** Mayo 2026

---

## ¿Qué hace este módulo?

Gestiona los **servicios del área empresarial EGM Atalayas** visibles para todos los empleados del parque. Son recursos compartidos (movilidad, instalaciones, iniciativas, servicios comunes) gestionados exclusivamente por el superadmin.

- Solo `ROLE_ADMIN` puede crear, editar y desactivar servicios.
- Cualquier usuario autenticado puede listarlos (con filtro opcional por categoría).

---

## Endpoints

Base URL: `/api/v1/servicios`

| Método | Ruta | Rol mínimo | Descripción |
|--------|------|-----------|-------------|
| `POST` | `/servicios` | `ADMIN` | Crear servicio |
| `GET` | `/servicios` | Cualquier autenticado | Listar servicios activos. Filtro opcional por `categoria` |
| `PUT` | `/servicios/{id}` | `ADMIN` | Actualizar servicio completo |
| `PATCH` | `/servicios/{id}/desactivar` | `ADMIN` | Soft delete |

---

## Contratos

### `POST /servicios` — Crear servicio

**Request body (`ServicioRequest`):**
```json
{
  "titulo": "Autobús Lanzadera",
  "descripcion": "Servicio de autobús directo al parque desde el centro de la ciudad",
  "categoria": "MOVILIDAD",
  "iconoUrl": "https://cdn.egmatalayas.com/icons/bus.svg",
  "urlInfo": "https://www.egmatalayas.com/movilidad",
  "telefono": "900 123 456",
  "comoAcceder": "Consulta los horarios en la intranet y reserva tu plaza con 24h de antelación"
}
```

| Campo | Tipo | Requerido | Descripción |
|-------|------|:---------:|-------------|
| `titulo` | `string` | ✅ | Nombre del servicio (`@NotBlank`) |
| `descripcion` | `string` | ❌ | Descripción libre |
| `categoria` | `CategoriaServicio` | ✅ | Ver enum abajo (`@NotNull`) |
| `iconoUrl` | `string` | ❌ | URL del icono representativo |
| `urlInfo` | `string` | ❌ | Enlace externo con más información |
| `telefono` | `string` | ❌ | Teléfono de contacto del servicio |
| `comoAcceder` | `string` | ❌ | Instrucciones de acceso/uso |

**Response `201 Created` (`ServicioResponse`):**
```json
{
  "servicioId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "titulo": "Autobús Lanzadera",
  "descripcion": "Servicio de autobús directo al parque desde el centro de la ciudad",
  "categoria": "MOVILIDAD",
  "iconoUrl": "https://cdn.egmatalayas.com/icons/bus.svg",
  "urlInfo": "https://www.egmatalayas.com/movilidad",
  "telefono": "900 123 456",
  "comoAcceder": "Consulta los horarios en la intranet...",
  "creadoPor": "uuid-del-admin",
  "activo": true,
  "creadoEn": "2026-05-14T10:00:00Z",
  "actualizadoEn": null
}
```

---

### `GET /servicios` — Listar servicios activos

**Query params:**

| Param | Tipo | Requerido | Descripción |
|-------|------|:---------:|-------------|
| `categoria` | `CategoriaServicio` | ❌ | Filtra por categoría. Si se omite, devuelve todos los activos |

**Response `200 OK`:** array de `ServicioResponse`.

---

### `PUT /servicios/{id}` — Actualizar servicio

Mismos campos que el POST. `404` si no existe. `403` si no es `ROLE_ADMIN`.

---

### `PATCH /servicios/{id}/desactivar` — Soft delete

| Código | Cuándo |
|--------|--------|
| `200` | Servicio desactivado correctamente. Devuelve `ServicioResponse` con `activo: false` |
| `400` | El servicio ya estaba desactivado |
| `403` | Rol insuficiente |
| `404` | No encontrado |

---

## Enum `CategoriaServicio`

| Valor | Descripción |
|-------|-------------|
| `MOVILIDAD` | Autobús lanzadera, coche compartido, VAO, bici |
| `INSTALACIONES` | Ludoteca, aula de formación, oficinas, sala de reuniones, correos |
| `INICIATIVAS` | Empresas solidarias, En Femenino, 100 estudiantes |
| `COMUNES` | Seguridad, limpieza, jardinería |

---

## DTOs del módulo

| DTO | Campos |
|-----|--------|
| `ServicioRequest` | `titulo`*, `descripcion`, `categoria`*, `iconoUrl`, `urlInfo`, `telefono`, `comoAcceder` |
| `ServicioResponse` | `servicioId`, `titulo`, `descripcion`, `categoria`, `iconoUrl`, `urlInfo`, `telefono`, `comoAcceder`, `creadoPor`, `activo`, `creadoEn`, `actualizadoEn` |

---

## Reglas de negocio

| Regla | Detalle |
|-------|---------|
| **Solo ADMIN gestiona** | `POST`, `PUT`, `PATCH` requieren `ROLE_ADMIN`. `GET` requiere cualquier autenticación |
| **Sin scope de empresa** | Los servicios son globales del parque: no tienen `empresaId`. Son visibles para todos |
| **Soft delete** | `activo = false`. El listado solo devuelve activos |
| **`400` si ya desactivado** | Intentar desactivar un servicio ya inactivo lanza `BusinessException` → `400` |

