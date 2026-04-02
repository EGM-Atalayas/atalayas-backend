# Atalayas API — Documentación de Endpoints
**Base URL:** `http://localhost:8080/api/v1`
**Versión:** 1.0
**Autenticación:** Cookie HttpOnly `accessToken` (JWT) + opcionalmente cabecera `Authorization: Bearer <token>`
---
## Swagger UI
| Recurso | URL |
|---------|-----|
| Interfaz interactiva | `GET /swagger-ui.html` |
| Spec OpenAPI (JSON) | `GET /v3/api-docs` |
> La ruta de Swagger es pública (sin autenticación previa). El esquema de seguridad registrado
> en Swagger es **cookieAuth** (`apiKey in cookie`, nombre `accessToken`).
> Para probar endpoints protegidos desde Swagger: inicia sesión primero via `/auth/login` —
> la cookie se setea automáticamente en el navegador.
---
## Roles de la plataforma
| Rol | Descripción |
|-----|-------------|
| `ROLE_ADMIN` | Superadmin EGM — acceso total a toda la plataforma |
| `ROLE_ADMIN_EMPRESA` | Administrador de una empresa específica — scope limitado a su empresa |
| `ROLE_EMPLEADO` | Empleado — acceso de lectura/consumo dentro de su empresa |
---
## Códigos de respuesta comunes
| Código | Significado |
|--------|-------------|
| `200` | OK |
| `201` | Creado correctamente |
| `204` | Sin contenido (operación exitosa sin body) |
| `400` | Error de validación o regla de negocio (`BusinessException`) |
| `401` | Sin sesión activa / token inválido |
| `403` | Sin permisos para este recurso |
| `404` | Recurso no encontrado (`ResourceNotFoundException`) |
---
## 1. Autenticación · `/api/v1/auth`
> Todos los endpoints de auth son **públicos** salvo `GET /me`.
> Los tokens se envían como **cookies HttpOnly** (`accessToken`, `refreshToken`) y también en el body.
| Método | Ruta | Auth | Descripción |
|--------|------|:---:|-------------|
| `POST` | `/auth/register` | ❌ | Registrar nuevo usuario. Devuelve tokens en cookies + body. |
| `POST` | `/auth/login` | ❌ | Iniciar sesión. Devuelve tokens en cookies + body. |
| `POST` | `/auth/refresh-token` | ❌ ¹ | Renueva el access token usando la cookie `refreshToken`. |
| `POST` | `/auth/logout` | ❌ | Invalida (limpia) las cookies de tokens. |
| `GET`  | `/auth/me` | ✅ | Datos del usuario autenticado actualmente. |
¹ Requiere la cookie `refreshToken` válida.
**Expiración de tokens:**
- `accessToken`: **1 hora**
- `refreshToken`: **7 días**
---
## 2. Usuarios · `/api/v1/users`
| Método | Ruta | Rol mínimo | Descripción |
|--------|------|-----------|-------------|
| `GET` | `/users/me` | Cualquiera | Perfil completo del usuario autenticado. |
| `GET` | `/users/{id}` | `ADMIN_EMPRESA` | Obtener usuario por ID. ADMIN_EMPRESA solo ve su empresa. |
| `GET` | `/users` | `ADMIN_EMPRESA` | Listar usuarios. ADMIN_EMPRESA ve su empresa; ADMIN ve todos. |
| `DELETE` | `/users/{id}/desactivar` | `ADMIN_EMPRESA` | Soft-delete de usuario (activo = false). |
---
## 3. Empresas · `/api/v1/empresas`
| Método | Ruta | Rol / Auth | Descripción |
|--------|------|-----------|-------------|
| `POST` | `/empresas/solicitud` | ❌ Pública | Solicitar alta de empresa. Crea la empresa en estado `PENDIENTE` y su usuario admin inactivo. |
| `GET` | `/empresas/aprobadas` | ❌ Pública | Listar empresas aprobadas y activas (selector de registro). |
| `GET` | `/empresas` | `ADMIN` | Listar todas las empresas. |
| `GET` | `/empresas/pendientes` | `ADMIN` | Listar empresas en estado `PENDIENTE`. |
| `PATCH` | `/empresas/{id}/estado` | `ADMIN` | Cambiar estado de una empresa (ver tabla de transiciones). |
### `PATCH /empresas/{id}/estado` — Transiciones
**Body:** `{ "nuevoEstado": "APROBADA" | "RECHAZADA" | "PENDIENTE" }`
| Desde \ Hacia | `PENDIENTE` | `APROBADA` | `RECHAZADA` |
|---|:---:|:---:|:---:|
| **`PENDIENTE`** | ❌ | ✅ | ✅ |
| **`RECHAZADA`** | ✅ | ❌ | ❌ |
| **`APROBADA`** | ❌ | ❌ | ❌ |
| Transición | `company.activo` | Usuarios | Email | Notif. interna |
|---|:---:|---|---|:---:|
| `PENDIENTE → APROBADA` | `true` | Se activan (activo = true) | ✉ Bienvenida | ✅ |
| `PENDIENTE → RECHAZADA` | `false` | Sin cambio (siguen inactivos) | ✉ Rechazo | ❌ |
| `RECHAZADA → PENDIENTE` | `false` | Sin cambio | Ninguno | ❌ |
> Transiciones prohibidas o no-op devuelven `400 Bad Request` con mensaje descriptivo.
---
## 4. Dashboard · `/api/v1/dashboard`
| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| `GET` | `/dashboard/admin/resumen` | `ADMIN_EMPRESA` | Métricas de la empresa: usuarios, módulos, progreso. |
| `GET` | `/dashboard/superadmin/resumen` | `ADMIN` | Métricas globales de toda la plataforma. |
---
## 5. Módulos · `/api/v1/modulos`
> Los módulos pueden ser de **empresa** o **globales** (`empresaId = null`, solo creables por `ADMIN`).
| Método | Ruta | Rol mínimo | Descripción |
|--------|------|-----------|-------------|
| `POST` | `/modulos` | `ADMIN_EMPRESA` | Crear módulo. ADMIN puede crear globales. |
| `GET` | `/modulos` | Cualquiera | Listar módulos visibles según rol. |
| `GET` | `/modulos/{id}` | Cualquiera | Obtener módulo por ID. `403` si no pertenece a su empresa. |
| `PUT` | `/modulos/{id}` | `ADMIN_EMPRESA` | Actualizar módulo completo. |
| `PATCH` | `/modulos/{id}/desactivar` | `ADMIN_EMPRESA` | Soft-delete. |
---
## 6. Contenidos · `/api/v1/contenidos`
> Tipos: `LECTURA`, `VIDEO`, `EVALUACION`. Los de tipo `EVALUACION` incluyen preguntas.
| Método | Ruta | Rol mínimo | Descripción |
|--------|------|-----------|-------------|
| `POST` | `/contenidos` | `ADMIN_EMPRESA` | Crear contenido en un módulo. |
| `GET` | `/contenidos/modulo/{moduloId}` | Cualquiera | Listar contenidos de un módulo. Empleado solo ve activos. |
| `GET` | `/contenidos/{id}` | Cualquiera | Obtener contenido por ID (incluye preguntas si es EVALUACION). |
| `PUT` | `/contenidos/{id}` | `ADMIN_EMPRESA` | Actualizar contenido. La versión se incrementa si cambia el cuerpo. |
| `PATCH` | `/contenidos/{id}/desactivar` | `ADMIN_EMPRESA` | Soft-delete. Trazabilidad histórica se conserva. |
| `POST` | `/contenidos/preguntas` | `ADMIN_EMPRESA` | Añadir pregunta a un contenido de tipo `EVALUACION`. |
| `DELETE` | `/contenidos/preguntas/{preguntaId}` | `ADMIN_EMPRESA` | Eliminar pregunta de evaluación. |
---
## 7. Progreso · `/api/v1/progreso`
> El campo `completado` es **irreversible**: una vez marcado no se puede desmarcar.
| Método | Ruta | Rol mínimo | Descripción |
|--------|------|-----------|-------------|
| `POST` | `/progreso` | Cualquiera | Registrar/actualizar progreso. El tiempo se acumula. Empleado solo registra el suyo. |
| `GET` | `/progreso/me` | Cualquiera | Todo el progreso del usuario autenticado. |
| `GET` | `/progreso/contenido/{contenidoId}` | Cualquiera | Estado del usuario sobre un contenido concreto. |
| `GET` | `/progreso/usuario/{usuarioId}` | `ADMIN_EMPRESA` | Progreso de un empleado. ADMIN_EMPRESA solo ve su empresa. |
| `GET` | `/progreso/empresa/{empresaId}` | `ADMIN_EMPRESA` | Progreso de todos los empleados de una empresa (dashboard). |
---
## 8. Anuncios · `/api/v1/anuncios`
| Método | Ruta | Rol mínimo | Descripción |
|--------|------|-----------|-------------|
| `POST` | `/anuncios` | `ADMIN_EMPRESA` | Crear anuncio. ADMIN puede crear globales (`esGlobal = true`). |
| `GET` | `/anuncios` | Cualquiera | Listar anuncios de empresa + globales. ADMIN ve toda la plataforma. |
| `PATCH` | `/anuncios/{id}/desactivar` | `ADMIN_EMPRESA` | Soft-delete. ADMIN_EMPRESA solo desactiva los propios. |
| `DELETE` | `/anuncios/{id}` | `ADMIN_EMPRESA` | Alias REST de `PATCH /{id}/desactivar`. Misma lógica. |
---
## 9. Comunicados · `/api/v1/comunicados`
> Comunicados oficiales de EGM para toda la plataforma. Solo gestionables por `ADMIN`.
| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| `POST` | `/comunicados` | `ADMIN` | Crear comunicado oficial. |
| `GET` | `/comunicados` | Cualquiera | Listar vigentes. ADMIN ve el histórico completo (expirados/desactivados incluidos). |
| `PATCH` | `/comunicados/{id}/desactivar` | `ADMIN` | Desactivar comunicado. |
---
## 10. Notificaciones · `/api/v1/notificaciones`
> Se generan automáticamente en eventos clave (ej. aprobación de empresa) y también manualmente.
| Método | Ruta | Rol mínimo | Descripción |
|--------|------|-----------|-------------|
| `POST` | `/notificaciones` | `ADMIN_EMPRESA` | Crear notificación manual para un usuario. |
| `GET` | `/notificaciones/me` | Cualquiera | Todas mis notificaciones (leídas + no leídas). |
| `GET` | `/notificaciones/me/no-leidas` | Cualquiera | Solo las no leídas (para la campana del frontend). |
| `GET` | `/notificaciones/me/contador` | Cualquiera | Número de no leídas. Respuesta: `{ "noLeidas": N }`. |
| `PATCH` | `/notificaciones/{id}/leer` | Cualquiera | Marcar una notificación como leída. Solo el destinatario puede ejecutarlo. |
| `PATCH` | `/notificaciones/me/leer-todas` | Cualquiera | Marcar todas como leídas. Respuesta: `{ "actualizadas": N }`. |
---
## 11. Eventos de Comunidad · `/api/v1/eventos`
| Método | Ruta | Rol mínimo | Descripción |
|--------|------|-----------|-------------|
| `POST` | `/eventos` | `ADMIN_EMPRESA` | Crear evento. ADMIN puede crear globales. |
| `GET` | `/eventos` | Cualquiera | Listar eventos visibles: empresa + globales. |
| `GET` | `/eventos/{id}` | Cualquiera | Obtener evento por ID. `403` si no pertenece a su empresa. |
| `PUT` | `/eventos/{id}` | `ADMIN_EMPRESA` | Actualizar evento. Solo ADMIN puede cambiar `esGlobal`. |
| `PATCH` | `/eventos/{id}/desactivar` | `ADMIN_EMPRESA` | Soft-delete. ADMIN_EMPRESA solo desactiva los propios. |
---
## 12. Beneficios · `/api/v1/beneficios`
| Método | Ruta | Rol mínimo | Descripción |
|--------|------|-----------|-------------|
| `POST` | `/beneficios` | `ADMIN_EMPRESA` | Crear beneficio. ADMIN puede crear globales. |
| `GET` | `/beneficios` | Cualquiera | Listar beneficios visibles: empresa + globales. |
| `PUT` | `/beneficios/{id}` | `ADMIN_EMPRESA` | Actualizar beneficio. ADMIN_EMPRESA solo edita los propios. |
| `PATCH` | `/beneficios/{id}/desactivar` | `ADMIN_EMPRESA` | Soft-delete. ADMIN_EMPRESA solo desactiva los propios. |
---
## 13. Inteligencia Artificial · `/api/v1/ai`
> Integración con **Gemini 2.0 Flash**. Requiere la variable de entorno `GEMINI_API_KEY`.
| Método | Ruta | Rol mínimo | Descripción |
|--------|------|-----------|-------------|
| `POST` | `/ai/generar-contenido` | `ADMIN_EMPRESA` | Genera contenido formativo dado un tema, descripción y tipo de módulo. |
| `POST` | `/ai/generar-preguntas` | `ADMIN_EMPRESA` | Genera preguntas de evaluación desde un prompt. `numPreguntas` opcional (por defecto 5). |
| `POST` | `/ai/chat` | Cualquiera | Chatbot para empleados. Acepta `prompt`, `nombreEmpresa` y `contexto`. |
| `POST` | `/ai/resumir` | `ADMIN_EMPRESA` | Genera un resumen estructurado de un texto largo. |
---
## Resumen de endpoints públicos (sin autenticación)
| Método | Ruta |
|--------|------|
| `POST` | `/auth/login` |
| `POST` | `/auth/register` |
| `POST` | `/auth/refresh-token` |
| `POST` | `/auth/logout` |
| `POST` | `/empresas/solicitud` |
| `GET` | `/empresas/aprobadas` |
| `GET` | `/swagger-ui/**` |
| `GET` | `/v3/api-docs/**` |
---
## Notas de seguridad multi-tenant
- Los usuarios con `ROLE_ADMIN_EMPRESA` están **aislados por empresa**: acceder a recursos de otra empresa devuelve `403` (no `404`, para no exponer la existencia del recurso).
- El `ROLE_ADMIN` (superadmin EGM) tiene acceso transversal a todos los recursos.
- El `ROLE_EMPLEADO` tiene acceso de **solo lectura/consumo** dentro de su empresa.
