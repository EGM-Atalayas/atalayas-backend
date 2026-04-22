# Arquitectura del Backend — EGM Atalayas

> **Audiencia:** Nuevos desarrolladores backend
> **Última actualización:** Abril 2026

---

## Índice

1. [Visión general](#1-visión-general)
2. [Capas de la aplicación](#2-capas-de-la-aplicación)
3. [Estructura de paquetes](#3-estructura-de-paquetes)
4. [Modelo de roles y multi-tenancy](#4-modelo-de-roles-y-multi-tenancy)
5. [Relación entre módulos](#5-relación-entre-módulos)
6. [Modelo de datos simplificado](#6-modelo-de-datos-simplificado)
7. [Convenciones del proyecto](#7-convenciones-del-proyecto)

---

## 1. Visión general

EGM Atalayas Backend es una **API REST multi-tenant** construida con Spring Boot 3.2. La plataforma gestiona:

- Formación de empleados (módulos, contenidos, progreso)
- Comunicación interna (anuncios, comunicados, notificaciones)
- Comunidad del parque empresarial (eventos, beneficios)
- Administración de empresas y usuarios
- Generación de contenido asistida por IA (Gemini)

Cada empresa del parque es un **tenant** aislado: sus datos no son accesibles para otras empresas. El superadmin (`ROLE_ADMIN`) tiene visibilidad transversal de toda la plataforma.

```
                    ┌─────────────────────────────────────────┐
                    │          EGM Atalayas Backend           │
                    │          Spring Boot 3.2 / Java 21      │
                    └──────────────────┬──────────────────────┘
                                       │
              ┌────────────────────────┼────────────────────────┐
              │                        │                        │
    ┌─────────▼────────┐   ┌──────────▼─────────┐   ┌─────────▼────────┐
    │  Empresa A        │   │  Empresa B          │   │  EGM (platform)  │
    │  admin + empleados│   │  admin + empleados  │   │  superadmin      │
    └──────────────────┘   └────────────────────┘   └──────────────────┘
        (tenant aislado)       (tenant aislado)          (acceso total)
```

---

## 2. Capas de la aplicación

Cada módulo sigue una arquitectura en capas limpia y consistente:

```
Petición HTTP
     │
     ▼
┌──────────────────────────────────────────────────────────────┐
│  Capa de Seguridad                                           │
│  JwtAuthenticationFilter  →  SecurityConfig (@PreAuthorize) │
└───────────────────────────────┬──────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────┐
│  Controller  (@RestController)                               │
│  · Recibe y valida la petición HTTP                          │
│  · Extrae el usuario autenticado del SecurityContext         │
│  · Delega en el Service                                      │
│  · Devuelve ResponseEntity con el DTO de respuesta           │
└───────────────────────────────┬──────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────┐
│  Service  (@Service)                                         │
│  · Contiene toda la lógica de negocio                        │
│  · Aplica las reglas multi-tenant (filtrado por empresaId)   │
│  · Lanza excepciones de negocio (BusinessException, etc.)    │
│  · Llama al Repository para persistir / consultar            │
└───────────────────────────────┬──────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────┐
│  Repository  (JpaRepository)                                 │
│  · Acceso a la base de datos vía Spring Data JPA             │
│  · Consultas personalizadas con @Query o métodos derivados   │
└───────────────────────────────┬──────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────┐
│  Entity  (@Entity)                                           │
│  · Mapeo directo a tabla PostgreSQL                          │
│  · Gestiona fechas con @PrePersist / @PreUpdate              │
└──────────────────────────────────────────────────────────────┘

Elementos transversales:
  DTO  — objetos de transferencia (request / response), nunca se expone la Entity directamente
  Mapper — convierte Entity ↔ DTO (métodos estáticos o clases dedicadas)
  GlobalExceptionHandler — captura excepciones y devuelve errores HTTP uniformes
```

### ¿Por qué DTOs y no exponer la Entity directamente?

- Evita exponer campos internos (ej. `password_hash`)
- Permite evolucionar el modelo de BD sin romper el contrato de la API
- Facilita la validación con `@Valid` en el request

---

## 3. Estructura de paquetes

```
com.atalayas.backend/
│
├── ai/
│   ├── client/        → HTTP client hacia la API de Gemini
│   ├── controller/    → AiController (endpoints /ai/*)
│   ├── dto/           → AiPromptRequest, AiResponse, AiFileResponse
│   └── service/       → AiContentService, AiChatService, AiSummaryService, AiFileService
│
├── audit/
│   ├── entity/        → AuditLog
│   ├── repository/    → AuditLogRepository
│   └── service/       → AuditService
│
├── auth/
│   ├── controller/    → AuthController
│   ├── dto/           → LoginRequest, RegisterRequest, AuthResponse
│   ├── mapper/        → AuthMapper
│   └── service/       → AuthService
│
├── common/
│   ├── enums/         → ContentType, EstadoSolicitud, ModuleType, ProgressStatus, RoleType
│   └── util/          → CookieUtil (gestión de cookies HttpOnly)
│
├── communication/
│   ├── controller/    → AnnouncementController, NotificationController, OfficialNoticeController
│   ├── dto/           → DTOs de cada entidad
│   ├── entity/        → Announcement, Notification, OfficialNotice
│   ├── mapper/        → Mappers correspondientes
│   ├── repository/    → Repositorios JPA
│   └── service/       → Servicios de negocio
│
├── community/         → (misma estructura: CommunityEvent)
├── company/           → (misma estructura: Company)
├── config/            → CorsConfig, JpaConfig, OpenApiConfig
├── content/           → ContentItem, Question
├── dashboard/         → DashboardService, DashboardController
├── exception/         → BusinessException, ResourceNotFoundException, UnauthorizedException, GlobalExceptionHandler
├── incidencia/        → Incidencia, enums (EstadoIncidencia, PrioridadIncidencia)
├── module/            → TrainingModule
├── progress/          → UserProgress
├── rewards/           → Benefit
├── role/              → Role
├── security/          → JwtAuthenticationFilter, JwtService, SecurityConfig, SecurityConstants, CustomUserDetailsService, AuthEntryPointJwt
└── user/              → User
```

---

## 4. Modelo de roles y multi-tenancy

### Roles del sistema

| Rol | Código | Scope |
|---|---|---|
| Superadmin EGM | `ROLE_ADMIN` | Toda la plataforma — acceso ilimitado |
| Admin de empresa | `ROLE_ADMIN_EMPRESA` | Solo su empresa (`empresaId`) |
| Empleado | `ROLE_EMPLEADO` | Solo lectura/consumo dentro de su empresa |

Los roles se guardan en la tabla `rol` y se asignan al usuario mediante FK (`rol_id`).

### Cómo se aplica el multi-tenancy

El aislamiento entre empresas **no** se implementa con Row-Level Security en PostgreSQL, sino en la **capa de servicio** de Spring:

```
Petición de ROLE_ADMIN_EMPRESA
        │
        ▼
UserController.getUsuarioById(id)
        │
        ▼
UserService.getById(id, usuarioAutenticado)
        │
        ├─ Busca el usuario por ID en BD
        │
        ├─ Si usuario.empresaId != autenticado.empresaId
        │       └── throw ResourceNotFoundException("Usuario no encontrado")
        │           (devuelve 404, no 403, para no revelar la existencia del recurso)
        │
        └─ Si misma empresa → devuelve el DTO
```

> **Importante:** se devuelve `404` (no `403`) cuando un admin intenta acceder a un recurso
> de otra empresa. Esto evita que descubra si el recurso existe en el sistema.

### Autorización en los endpoints

Se usa `@PreAuthorize` a nivel de método en los controladores:

```java
// Solo admin de empresa o superior
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN_EMPRESA', 'ROLE_ADMIN')")

// Solo superadmin
@PreAuthorize("hasAuthority('ROLE_ADMIN')")

// Cualquier usuario autenticado (sin anotación → protegido por defecto en SecurityConfig)
```

---

## 5. Relación entre módulos

```
           ┌─────────┐        ┌─────────────┐
           │ Company │◄───────│    User     │
           │(empresa)│  FK    │  (usuario)  │
           └────┬────┘        └──────┬──────┘
                │                   │
     ┌──────────┼───────────┐       │
     │          │           │       │
     ▼          ▼           ▼       ▼
┌─────────┐ ┌───────┐ ┌─────────┐ ┌──────────────┐
│Training │ │Benefit│ │Community│ │Communication │
│ Module  │ │       │ │  Event  │ │(Announcement,│
└────┬────┘ └───────┘ └─────────┘ │ Notification,│
     │                            │ OfficialNotice│
     ▼                            └──────────────┘
┌──────────┐
│ContentItem│
│(contenido)│
└────┬──────┘
     │
     ▼
┌──────────┐      ┌──────────┐
│UserProgress│    │ Question │
│(trazabilidad│   │(pregunta)│
│  _lectura)│     └──────────┘
└──────────┘
```

**Relaciones clave:**
- `User` → `Company`: un usuario pertenece a una empresa (FK `empresa_id`)
- `User` → `Role`: un usuario tiene un rol (FK `rol_id`)
- `TrainingModule` → `Company`: un módulo puede ser de empresa o global (`empresa_id = null`)
- `ContentItem` → `TrainingModule`: un contenido pertenece a un módulo
- `UserProgress` → `User` + `ContentItem`: trazabilidad única por (usuario, contenido)
- `Question` → `ContentItem`: solo para contenidos de tipo `EVALUACION`

---

## 6. Modelo de datos simplificado

```
┌───────────────────────────────────────────────────────────────────────┐
│ empresa                                                               │
│  empresa_id (PK, UUID)  nombre_empresa  cif  sector                  │
│  estado_solicitud (PENDIENTE/APROBADA/PAUSADA)  activo              │
└───────────────────────────────────────────────────────────────────────┘
                    ▲
                    │ empresa_id (FK)
┌───────────────────────────────────────────────────────────────────────┐
│ usuario                                                               │
│  usuario_id (PK, UUID)  email (UNIQUE)  password_hash                │
│  nombre  apellidos  avatar_url  puesto_trabajo                        │
│  rol_id (FK → rol)  empresa_id (FK → empresa)                        │
│  activo  intentos_fallidos  terminos_aceptados                        │
└───────────────────────────────────────────────────────────────────────┘

┌────────────────────┐       ┌────────────────────────────────────────┐
│ modulo             │       │ contenido                              │
│  modulo_id (PK)    │──────►│  contenido_id (PK)                    │
│  empresa_id        │       │  modulo_id (FK)   empresa_id          │
│  nombre  descripcion│      │  titulo  descripcion  tipo_contenido  │
│  tipo_modulo        │      │  (VIDEO/TEXTO/PDF/EVALUACION/IA_GEN)  │
│  orden  activo      │      │  cuerpo_texto  url_recurso  version   │
└────────────────────┘       │  orden  activo  es_ia_generado        │
                             └───────────────┬────────────────────────┘
                                             │
                              ┌──────────────┴────────────────────┐
                              │                                   │
                    ┌─────────▼──────────┐           ┌───────────▼──────────┐
                    │ trazabilidad_lectura│           │ contenido_pregunta   │
                    │  (UserProgress)     │           │  (Question)          │
                    │  usuario_id (FK)    │           │  contenido_id (FK)   │
                    │  contenido_id (FK)  │           │  enunciado  opciones │
                    │  completado         │           │  respuesta_correcta  │
                    │  tiempo_segundos    │           └─────────────────────┘
                    │  porcentaje         │
                    └────────────────────┘
```

---

## 7. Convenciones del proyecto

### Nombres
- **Tablas y columnas:** `snake_case` en español (ej. `trazabilidad_lectura`, `empresa_id`)
- **Clases Java:** `PascalCase` en inglés (ej. `TrainingModule`, `UserProgress`)
- **Endpoints REST:** `kebab-case` en español (ej. `/api/v1/modulos`, `/api/v1/progreso`)

### Soft delete
La mayoría de entidades no se eliminan físicamente. Tienen un campo `activo`:
- `activo = true` → el recurso está visible y operativo
- `activo = false` → soft-deleted, invisible para empleados pero conserva trazabilidad histórica

**Excepción — hard delete al rechazar una solicitud de empresa:** cuando el superadmin rechaza una solicitud de alta, la empresa y sus usuarios provisionales se **eliminan físicamente** de la BD (en ese orden para respetar la FK `usuario → empresa`). Esto solo aplica a empresas en estado `PENDIENTE` que nunca estuvieron operativas.

### IDs
Todos los IDs son `UUID` generados por Hibernate (`GenerationType.UUID`), excepto `AuditLog` e `Incidencia` que usan `IDENTITY` (Long autoincremental).

### Fechas
Todas las fechas son `OffsetDateTime` para preservar la zona horaria. Se gestionan automáticamente con `@PrePersist` / `@PreUpdate`.

### Manejo de errores
Ver `GlobalExceptionHandler`. Las excepciones de negocio siempre devuelven un body JSON:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Descripción del error",
  "timestamp": "2026-04-15T10:30:00Z"
}
```

