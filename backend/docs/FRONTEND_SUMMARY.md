# Atalayas Backend — Resumen de Funcionalidades para Frontend
> **Base URL:** `http://localhost:8080/api/v1`
> **Autenticación:** Cookie HttpOnly `accessToken` + `refreshToken` (JWT). Enviar `credentials: 'include'` en todas las peticiones.
> **Swagger UI:** `GET /swagger-ui.html`
> **Última actualización:** Mayo 2026
---
## Índice de módulos
| # | Módulo | Base URL | Doc detallada |
|---|--------|----------|---------------|
| 1 | Autenticación | `/auth` | `docs/api-endpoints.md §1` |
| 2 | Usuarios | `/users` | `docs/api-endpoints.md §2` |
| 3 | Empresas | `/empresas` | `docs/api-endpoints.md §3` |
| 4 | Dashboard | `/dashboard` | `docs/modulos/dashboard.md` |
| 5 | Módulos formativos | `/modulos` | `docs/api-endpoints.md §5` |
| 6 | Contenidos | `/contenidos` | `docs/api-endpoints.md §6` |
| 7 | Progreso | `/progreso` | `docs/api-endpoints.md §7` |
| 8 | Anuncios | `/anuncios` | `docs/api-endpoints.md §8` |
| 9 | Comunicados | `/comunicados` | `docs/api-endpoints.md §9` |
| 10 | Notificaciones | `/notificaciones` | `docs/modulos/notificaciones.md` |
| 11 | Eventos EGM | `/eventos` | `docs/modulos/eventos-egm.md` |
| 11b | Eventos de Comunidad | `/comunidad/eventos` | `docs/modulos/comunidad-eventos.md` |
| 12 | Beneficios | `/beneficios` | `docs/api-endpoints.md §12` |
| 13 | Inteligencia Artificial | `/ai` | `docs/api-endpoints.md §13` |
| 14 | Sugerencias | `/sugerencias` | `docs/modulos/sugerencias.md` |
| 15 | Incidencias | `/incidencias` | `docs/modulos/incidencias.md` |
| 16 | Servicios EGM | `/servicios` | `docs/modulos/servicios.md` |
| 17 | Documentos | `/documentos` | `docs/modulos/documentos.md` |
---
## Roles
| Rol | Descripción |
|-----|-------------|
| `ROLE_ADMIN` | Superadmin EGM — acceso total |
| `ROLE_ADMIN_EMPRESA` | Admin de empresa — scope limitado a su empresa |
| `ROLE_EMPLEADO` | Empleado — acceso de consumo dentro de su empresa |
---
## Endpoints sin autenticación (públicos)
| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/auth/login` | Iniciar sesión |
| `POST` | `/auth/register` | Registrar usuario |
| `POST` | `/auth/refresh-token` | Renovar access token (requiere cookie `refreshToken`) |
| `POST` | `/auth/logout` | Cerrar sesión (limpia cookies) |
| `POST` | `/auth/forgot-password` | Solicitar recuperación de contraseña |
| `POST` | `/auth/reset-password` | Restablecer contraseña con token de email |
| `POST` | `/empresas/solicitud` | Solicitar alta de empresa (estado PENDIENTE) |
| `GET` | `/empresas/aprobadas` | Lista de empresas activas (selector de registro) |
| `GET` | `/anuncios` | Anuncios globales activos (sin sesión) |
| `GET` | `/comunicados` | Comunicados vigentes activos (sin sesión) |
---
## Flujo de sesión (restauración tras recarga)
```
1. Al iniciar la app → GET /auth/me (credentials: 'include')
   → 200: sesión activa, restaurar estado con datos + accessToken del body
   → 401: token expirado → POST /auth/refresh-token (credentials: 'include')
            → 200: nueva sesión con nuevos tokens
            → 400/401: sesión expirada → redirigir a login
```
**Expiración:** `accessToken` = 1 hora · `refreshToken` = 7 días
---
## Módulo 1 — Autenticación · `/api/v1/auth`
| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/auth/register` | Registrar usuario. Devuelve tokens en cookies + body |
| `POST` | `/auth/login` | Login. Devuelve tokens en cookies + body |
| `POST` | `/auth/refresh-token` | Renueva accessToken usando cookie `refreshToken` |
| `POST` | `/auth/logout` | Limpia las cookies de tokens |
| `GET` | `/auth/me` | 🔒 Datos del usuario autenticado + `accessToken` activo en body |
| `POST` | `/auth/forgot-password` | Envía email de recuperación. Siempre `200` |
| `POST` | `/auth/reset-password` | Restablece contraseña con token. `400` si inválido/expirado/usado |
---
## Módulo 2 — Usuarios · `/api/v1/users`
| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| `POST` | `/users` | `ADMIN_EMPRESA` | Crear usuario en la empresa. Body: `email`, `nombre`, `apellidos`, `rolId` (UUID), `empresaId` (solo ADMIN) |
| `GET` | `/users` | `ADMIN_EMPRESA` | Listar usuarios (ADMIN ve todos, ADMIN_EMPRESA solo su empresa) |
| `GET` | `/users/me` | Cualquiera | Perfil completo del usuario autenticado |
| `PATCH` | `/users/me` | Cualquiera | Actualizar perfil: `nombre`, `apellidos`, `puestoTrabajo`, `bio`, `telefono`, `avatarUrl`, `bannerUrl`, `disponibilidad`, preferencias de notif., `modoOscuro` |
| `POST` | `/users/me/password` | Cualquiera | Cambiar contraseña: `passwordActual`, `passwordNueva`, `passwordConfirmar` |
| `POST` | `/users/me/avatar` | Cualquiera | Subir avatar (`multipart`, campo `file`). Devuelve `{ "avatarUrl": "..." }` |
| `GET` | `/users/{id}` | `ADMIN_EMPRESA` | Obtener usuario por ID. `404` si es de otra empresa |
| `DELETE` | `/users/{id}/desactivar` | `ADMIN_EMPRESA` | Soft delete de usuario |
---
## Módulo 3 — Empresas · `/api/v1/empresas`
| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| `POST` | `/empresas/solicitud` | ❌ Público | Solicitar alta. Crea empresa en estado `PENDIENTE` |
| `GET` | `/empresas/aprobadas` | ❌ Público | Empresas aprobadas activas |
| `GET` | `/empresas` | `ADMIN` | Todas las empresas |
| `GET` | `/empresas/pendientes` | `ADMIN` | Empresas en estado `PENDIENTE` |
| `GET` | `/empresas/solicitudes` | `ADMIN` | Solicitudes pendientes con datos del admin provisional |
| `PATCH` | `/empresas/{id}/estado` | `ADMIN` | Cambiar estado: `{ "nuevoEstado": "APROBADA" \| "PAUSADA" }` |
| `PATCH` | `/empresas/{id}/solicitud` | `ADMIN` | Aprobar/rechazar solicitud: `{ "accion": "aprobar" \| "rechazar" }` |
| `PATCH` | `/empresas/{id}/activacion` | `ADMIN` | Toggle `activo` de empresa + usuarios |
| `POST` | `/empresas/{id}/reenviar-email` | `ADMIN` | Reenvía email de aprobación si `emailEnviado = false`. Body: `{ "tipo": "aprobacion" }` |
**Estados empresa:** `PENDIENTE` → `APROBADA` ↔ `PAUSADA`. El rechazo es hard delete irreversible.
---
## Módulo 4 — Dashboard · `/api/v1/dashboard`
| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| `GET` | `/dashboard/admin/resumen` | `ADMIN_EMPRESA` | `{ nombreEmpresa, usuariosActivos, usuariosInactivos }` |
| `GET` | `/dashboard/admin/actividad?limit=5` | `ADMIN_EMPRESA` | Actividad reciente: array `[{ tipo, texto, timestamp }]`. Tipos: `completado`, `inicio`, `logro`, `grupo`, `nuevo` |
| `GET` | `/dashboard/superadmin/resumen` | `ADMIN` | Métricas globales básicas (legado) |
| `GET` | `/dashboard/superadmin?limit=10` | `ADMIN` | Dashboard completo: métricas + `actividadReciente` del audit_log |
| `GET` | `/dashboard/superadmin/graficas` | `ADMIN` | `{ evolucion[], sectores[], modulos[] }` para gráficos |
**Nota:** el campo `tiempo` en actividad del superadmin es texto relativo calculado en backend (`"Ahora mismo"`, `"Hace N minutos"`, etc.).
---
## Módulo 5 — Módulos · `/api/v1/modulos`
| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| `POST` | `/modulos` | `ADMIN_EMPRESA` | Crear módulo. ADMIN puede crear globales (`empresaId = null`) |
| `GET` | `/modulos` | Cualquiera | Listar módulos visibles según rol |
| `GET` | `/modulos/{id}` | Cualquiera | Obtener módulo. `403` si no pertenece a su empresa |
| `PUT` | `/modulos/{id}` | `ADMIN_EMPRESA` | Actualizar módulo completo |
| `PATCH` | `/modulos/{id}/desactivar` | `ADMIN_EMPRESA` | Soft delete |
---
## Módulo 6 — Contenidos · `/api/v1/contenidos`
**Tipos:** `LECTURA` | `VIDEO` | `EVALUACION` (los de tipo `EVALUACION` incluyen preguntas).
| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| `POST` | `/contenidos` | `ADMIN_EMPRESA` | Crear contenido en un módulo |
| `GET` | `/contenidos/modulo/{moduloId}` | Cualquiera | Listar contenidos. Empleado solo ve activos |
| `GET` | `/contenidos/{id}` | Cualquiera | Obtener contenido con preguntas si es EVALUACION |
| `PUT` | `/contenidos/{id}` | `ADMIN_EMPRESA` | Actualizar. Versión se incrementa si cambia el cuerpo → notif. `CONTENIDO_ACTUALIZADO` |
| `PATCH` | `/contenidos/{id}/desactivar` | `ADMIN_EMPRESA` | Soft delete. Trazabilidad histórica se conserva |
| `POST` | `/contenidos/preguntas` | `ADMIN_EMPRESA` | Añadir pregunta a `EVALUACION` |
| `DELETE` | `/contenidos/preguntas/{preguntaId}` | `ADMIN_EMPRESA` | Eliminar pregunta |
---
## Módulo 7 — Progreso · `/api/v1/progreso`
> `completado` es **irreversible**: una vez `true` no puede volver a `false`.
> Al completar todos los contenidos de un módulo → se genera certificado PDF automáticamente.
| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| `POST` | `/progreso` | Cualquiera | Registrar/actualizar progreso. Tiempo se acumula. Body: `{ contenidoId, completado, tiempoSegundos }` |
| `GET` | `/progreso/me` | Cualquiera | Todo el progreso del usuario autenticado |
| `GET` | `/progreso/contenido/{contenidoId}` | Cualquiera | Estado sobre un contenido concreto |
| `GET` | `/progreso/usuario/{usuarioId}` | `ADMIN_EMPRESA` | Progreso de un empleado (scope empresa) |
| `GET` | `/progreso/empresa/{empresaId}` | `ADMIN_EMPRESA` | Progreso de todos los empleados de una empresa |
---
## Módulo 8 — Anuncios · `/api/v1/anuncios`
| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| `POST` | `/anuncios` | `ADMIN_EMPRESA` | Crear anuncio. ADMIN puede crear globales (`esGlobal = true`) |
| `GET` | `/anuncios` | ❌ Público¹ | Sin sesión: solo globales activos. Con sesión: empresa + globales. ADMIN: todos |
| `PATCH` | `/anuncios/{id}/desactivar` | `ADMIN_EMPRESA` | Soft delete. Solo desactiva los propios |
| `DELETE` | `/anuncios/{id}` | `ADMIN_EMPRESA` | Alias de PATCH anterior |
¹ Sin token devuelve solo anuncios `esGlobal = true` y `activo = true`.
---
## Módulo 9 — Comunicados · `/api/v1/comunicados`
> Comunicados oficiales de EGM para toda la plataforma. Solo `ROLE_ADMIN` gestiona.
| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| `POST` | `/comunicados` | `ADMIN` | Crear comunicado oficial |
| `GET` | `/comunicados` | ❌ Público¹ | Sin sesión: vigentes activos. ADMIN: histórico completo |
| `PATCH` | `/comunicados/{id}/desactivar` | `ADMIN` | Desactivar comunicado |
¹ Filtra `activo = true` y `fechaExpiracion` no superada.
---
## Módulo 10 — Notificaciones · `/api/v1/notificaciones`
> → [Documentación detallada](modulos/notificaciones.md)
| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| `POST` | `/notificaciones` | `ADMIN` o `ADMIN_EMPRESA` | Crear notificación manual |
| `GET` | `/notificaciones/me` | Cualquiera | Mis notificaciones paginadas. Params: `?page=0&size=20` |
| `GET` | `/notificaciones/me/no-leidas` | Cualquiera | Solo no leídas (para campana del header) |
| `GET` | `/notificaciones/me/contador` | Cualquiera | `{ "noLeidas": N }` (badge del header) |
| `PATCH` | `/notificaciones/{id}/leer` | Cualquiera | Marcar una como leída. `400` si ya leída o no es el destinatario |
| `PATCH` | `/notificaciones/me/leer-todas` | Cualquiera | Marcar todas como leídas. Devuelve `{ "actualizadas": N }` |
**Tipos de notificación canónicos:** `MODULO_NUEVO`, `CONTENIDO_COMPLETADO`, `ANUNCIO`, `BIENVENIDA`, `COMUNICADO`, `CONTENIDO_ACTUALIZADO`, `DOCUMENTO_NUEVO`, `CERTIFICADO_GENERADO`.
---
## Módulo 11 — Eventos EGM · `/api/v1/eventos`
> Eventos globales del parque EGM. Solo `ROLE_ADMIN` gestiona. → [Doc](modulos/eventos-egm.md)
| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| `POST` | `/eventos` | `ADMIN` | Crear evento. Campos: `titulo`*, `fecha`* (YYYY-MM-DD), `horaInicio`, `horaFin`, `lugar`, `urlInfo`, `imagenUrl` |
| `GET` | `/eventos` | Cualquier autenticado | Listar activos |
| `PUT` | `/eventos/{id}` | `ADMIN` | Actualizar completo |
| `PATCH` | `/eventos/{id}/desactivar` | `ADMIN` | Cancelar (`estado → CANCELADO`) |
**`EstadoEvento`:** `PROXIMO` | `EN_CURSO` | `FINALIZADO` | `CANCELADO`
---
## Módulo 11b — Eventos de Comunidad · `/api/v1/comunidad/eventos`
> Eventos internos por empresa. → [Doc](modulos/comunidad-eventos.md)
| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| `POST` | `/comunidad/eventos` | `ADMIN_EMPRESA` | Crear evento. Campos: `titulo`*, `fechaInicio`* (OffsetDateTime), `fechaFin`, `esGlobal`, `descripcion` |
| `GET` | `/comunidad/eventos` | Cualquier autenticado | Listar visibles según rol |
| `GET` | `/comunidad/eventos/{id}` | Cualquier autenticado | Por ID. `403` si ajeno |
| `PUT` | `/comunidad/eventos/{id}` | `ADMIN_EMPRESA` | Actualizar. Solo ADMIN cambia `esGlobal` |
| `PATCH` | `/comunidad/eventos/{id}/desactivar` | `ADMIN_EMPRESA` | Soft delete propio |
---
## Módulo 12 — Beneficios · `/api/v1/beneficios`
| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| `POST` | `/beneficios` | `ADMIN_EMPRESA` | Crear beneficio. ADMIN puede crear globales |
| `GET` | `/beneficios` | Cualquier autenticado | Activos de empresa + globales. ADMIN ve todos |
| `PUT` | `/beneficios/{id}` | `ADMIN_EMPRESA` | Actualizar propio. `403` si ajeno o global |
| `PATCH` | `/beneficios/{id}/desactivar` | `ADMIN_EMPRESA` | Soft delete propio. `403` si ajeno o global |
---
## Módulo 13 — Inteligencia Artificial · `/api/v1/ai`
> Principal: **Groq llama-3.3-70b-versatile**. Fallback/archivo: **Gemini 2.0 Flash**. Audio: **ElevenLabs**.
> El chat usa **streaming** (`text/plain`). Leer con `ReadableStream` o `EventSource`.
| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| `POST` | `/ai/generar-contenido` | `ADMIN_EMPRESA` | Body: `{ tema, descripcion, tipoModulo }`. Devuelve `{ contenido, modelo, generadoEn }` |
| `POST` | `/ai/generar-preguntas` | `ADMIN_EMPRESA` | Body: `{ prompt, numPreguntas? }`. Devuelve `{ contenido, modelo, generadoEn }` |
| `POST` | `/ai/chat` | Cualquiera | Body: `{ messages: [{role, content}], systemPrompt }`. Respuesta: **stream `text/plain`** |
| `POST` | `/ai/resumir` | `ADMIN_EMPRESA` | Body: `{ prompt }`. Devuelve `{ contenido, modelo, generadoEn }` |
| `POST` | `/ai/generar-desde-archivo` | `ADMIN_EMPRESA` | `multipart/form-data`, part `archivo` (PDF/DOCX/TXT). Query: `?tiposSalida=documentacion,podcast,video`. Devuelve `AiFileResponse` |
**`AiFileResponse`:** `titulo`, `descripcion`, `contenido` (Markdown), `scriptPodcast` (texto plano), `scriptVideo` (JSON array slides), `podcastAudioUrl`, `tiposSalida`, `modelo`, `generadoEn`.
---
## Módulo 14 — Sugerencias · `/api/v1/sugerencias`
| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| `POST` | `/sugerencias` | Cualquiera | Body: `{ mensaje, destinatario: "EMPRESA" \| "EGM" }`. Devuelve `201` sin body |
| `GET` | `/sugerencias` | `ADMIN_EMPRESA` | Listar todas con `{ id, mensaje, nombreUsuario, emailUsuario, estado, destinatario, creadoEn }` |
**`EstadoSugerencia`:** `PENDIENTE` | `VISTA` | `RESUELTA`
---
## Módulo 15 — Incidencias · `/api/v1/incidencias`
> Solo `ROLE_ADMIN` (superadmin EGM). → [Doc](modulos/incidencias.md)
| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/incidencias` | Body: `{ titulo*, descripcion, prioridad, empresaId }`. Si `prioridad=CRITICA` → entrada en audit_log |
| `GET` | `/incidencias` | Lista todas ordenadas por `creadoEn DESC` |
| `PATCH` | `/incidencias/{id}/estado` | Body: `{ "estado": "ABIERTA" \| "EN_CURSO" \| "RESUELTA" \| "CERRADA" }` (case-insensitive) |
**`EstadoIncidencia`:** `ABIERTA` | `EN_CURSO` | `RESUELTA` | `CERRADA`
**`PrioridadIncidencia`:** `NORMAL` | `CRITICA`
---
## Módulo 16 — Servicios EGM · `/api/v1/servicios`
> Recursos del parque EGM (movilidad, instalaciones, iniciativas). Solo `ROLE_ADMIN` gestiona. → [Doc](modulos/servicios.md)
| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| `POST` | `/servicios` | `ADMIN` | Body: `{ titulo*, categoria*, descripcion, iconoUrl, urlInfo, telefono, comoAcceder }` |
| `GET` | `/servicios` | Cualquier autenticado | Listar activos. Query: `?categoria=MOVILIDAD\|INSTALACIONES\|INICIATIVAS\|COMUNES` |
| `PUT` | `/servicios/{id}` | `ADMIN` | Actualizar completo |
| `PATCH` | `/servicios/{id}/desactivar` | `ADMIN` | Soft delete. `400` si ya inactivo |
---
## Módulo 17 — Documentos · `/api/v1/documentos`
> Gestión documental laboral. Incluye generación automática de certificados PDF. → [Doc](modulos/documentos.md)
### Admin
| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/documentos` | `multipart/form-data`: `file`, `titulo`, `tipo`, `requiereFirma`, `asignarATodos`, `notificar`, `usuariosIds[]`, `departamentos[]`. Máx. 25 MB |
| `GET` | `/documentos` | Lista documentos empresa con `totalAsignados`, `totalVistos`, `totalFirmados` |
| `GET` | `/documentos/{id}/asignaciones` | Estado por empleado: `visto`, `firmado`, fechas |
| `DELETE` | `/documentos/{id}` | Soft delete → `204` |
### Empleado
| Método | Ruta | Descripción |
|--------|------|-------------|
| `GET` | `/documentos/me` | Mis documentos asignados con estado de lectura |
| `POST` | `/documentos/me/{id}/visto` | Marcar como visto → `204`. Idempotente |
| `GET` | `/documentos/me/certificado/{moduloId}` | URL del certificado PDF del módulo. `404` si no completado |
**`TipoDocumento`:** `NOMINA` | `CONTRATO` | `CERTIFICADO` | `POLITICA` | `OTRO`
> ⚠️ Firma electrónica: campos en el modelo (`firmado`, `firmaUrl`) — **endpoint pendiente de implementación**.
---
## Enums de referencia rápida
### `TipoDocumento`
`NOMINA` · `CONTRATO` · `CERTIFICADO` · `POLITICA` · `OTRO`
### `CategoriaServicio`
`MOVILIDAD` · `INSTALACIONES` · `INICIATIVAS` · `COMUNES`
### `EstadoEvento` (Eventos EGM)
`PROXIMO` · `EN_CURSO` · `FINALIZADO` · `CANCELADO`
### `EstadoSugerencia`
`PENDIENTE` · `VISTA` · `RESUELTA`
### `DestinatarioSugerencia`
`EMPRESA` · `EGM`
### `EstadoIncidencia`
`ABIERTA` · `EN_CURSO` · `RESUELTA` · `CERRADA`
### `PrioridadIncidencia`
`NORMAL` · `CRITICA`
### Tipos de notificación canónicos
`MODULO_NUEVO` · `CONTENIDO_COMPLETADO` · `ANUNCIO` · `BIENVENIDA` · `COMUNICADO` · `CONTENIDO_ACTUALIZADO` · `DOCUMENTO_NUEVO` · `CERTIFICADO_GENERADO`
---
## Notas de seguridad multi-tenant
- **`ROLE_ADMIN_EMPRESA`**: aislado por empresa. Acceso a recursos ajenos → `404` (no `403`, para no revelar existencia). **Excepción:** eventos de comunidad devuelven `403` en acceso ajeno.
- **`ROLE_ADMIN`**: acceso transversal a todos los recursos de la plataforma.
- **`ROLE_EMPLEADO`**: solo lectura/consumo dentro de su empresa.
---
## Checklist de implementación Frontend
Usar esta lista para confirmar qué módulos están integrados en el frontend:
### Auth & Usuarios
- [ ] Login / Logout / Registro
- [ ] Flujo de restauración de sesión (`GET /auth/me` → refresh → login)
- [ ] Recuperación de contraseña (forgot + reset)
- [ ] Perfil propio (editar, cambiar contraseña, subir avatar)
### Admin Empresa
- [ ] Gestión de usuarios (crear, listar, desactivar)
- [ ] Dashboard empresa (resumen + actividad reciente)
- [ ] Módulos formativos (CRUD)
- [ ] Contenidos (CRUD + preguntas)
- [ ] Documentos (subir, asignar, ver estado por empleado)
- [ ] Anuncios (crear, desactivar)
- [ ] Eventos de Comunidad (crear, editar, desactivar)
- [ ] Beneficios (crear, editar, desactivar)
- [ ] Sugerencias (ver lista)
- [ ] IA: generar contenido, preguntas, resumir, desde archivo
### Empleado
- [ ] Dashboard empleado (progreso, módulos disponibles)
- [ ] Módulos y contenidos (leer, ver vídeo, evaluación)
- [ ] Registro de progreso
- [ ] Mis documentos (ver, marcar visto, descargar certificado)
- [ ] Notificaciones (campana, lista paginada, marcar leídas)
- [ ] Anuncios y comunicados
- [ ] Eventos (EGM + comunidad)
- [ ] Beneficios
- [ ] Servicios EGM
- [ ] Chatbot IA
- [ ] Sugerencias (enviar)
### Superadmin (ROLE_ADMIN)
- [ ] Dashboard Superadmin (métricas + gráficos + actividad)
- [ ] Gestión de empresas (solicitudes, aprobar/rechazar, estado, activar)
- [ ] Reenvío de email de aprobación
- [ ] Incidencias (crear, listar, cambiar estado)
- [ ] Comunicados oficiales
- [ ] Eventos EGM (crear, editar, cancelar)
- [ ] Servicios EGM (crear, editar, desactivar)
- [ ] Módulos globales
---
## Discrepancias documentadas (corregidas en esta versión)
| Problema anterior | Corrección |
|---|---|
| `api-endpoints.md §11` documentaba `/api/v1/eventos` con `ROLE_ADMIN_EMPRESA` como "Eventos de Comunidad" | Ahora separados: `/api/v1/eventos` (solo `ROLE_ADMIN`) y `/api/v1/comunidad/eventos` (`ADMIN_EMPRESA`) |
| Módulo **Servicios** ausente de la documentación | Añadido como §16 en `api-endpoints.md` y `docs/modulos/servicios.md` |
| Módulo **Documentos** ausente de la documentación | Añadido como §17 en `api-endpoints.md` ulos/documentos.md` |
| AI: doc decía Gemini como principal | Corregido: Groq llama-3.3-70b-versatile es el principal; Gemini es secundario/fallback |
| `POST /ai/chat` documentado con `prompt/nombreEmpresa/contexto` | Corregido: body real es `{ messages: [{role,content}], systemPrompt }` con response streaming `text/plain` |
| Tipo de notificación `DOCUMENTO_NUEVO` y `CERTIFICADO_GENERADO` no documentados | Añadidos a tipos canónicos de notificación |
| `PATCH /users/{id}/activar` — frontend lo creía inexistente | ✅ Existe e implementado |
| `POST /documentos/me/{id}/firmar` — marcado como "pendiente" | ✅ Completamente implementado |
| `PUT /anuncios/{id}` y `PATCH /anuncios/{id}/vistas` — marcados como dudosos | ✅ Ambos implementados |
| `PATCH /users/{id}`, `GET /users/paginado`, `GET /anuncios/paginado` — no documentados | Añadidos |
| Firma electrónica marcada como "endpoint pendiente" en `docs/modulos/documentos.md` | Corregido |
---
## Respuestas al plan del frontend (revisión Mayo 2026)

### Pasos de resolución — Estado backend

| # | Tarea frontend | Estado backend |
|---|---|---|
| 1 | Módulo 6 Contenidos — modelo "contenido embebido en módulo" vs `/contenidos` | ✅ **Ambos modelos coexisten**. `TrainingModule` incluye campos embebidos (`contenidoMarkdown`, `scriptPodcast`, `scriptVideo`, `podcastAudioUrl`, `adjuntoUrl`, `testPreguntas`) para módulos IA. Los `/contenidos` (LECTURA/VIDEO/EVALUACION) son para contenido granular. No hay brecha — el frontend elige según `esEspecializadoIa`. |
| 2 | Módulo 11b Eventos de Comunidad — integrar `/comunidad/eventos` | ✅ Backend completamente implementado. Ver §11b. |
| 3 | IA — conectar endpoints `/ai/*` al flujo del admin | ✅ Todos implementados. Ver §13. |
| 4 | Usuarios — usar `POST /users` en lugar de `/auth/register` para crear empleados | ✅ Implementado. Body: `email`, `nombre`, `apellidos`, `rolId` (UUID), `empresaId` (solo ADMIN). |
| 5 | Empresas — `GET /empresas/solicitudes` + botón reenvío email | ✅ Ambos implementados. Ver §3. |
| 6 | Notificaciones — panel completo con `GET /notificaciones/me` | ✅ Paginado implementado. `?page=0&size=20`. Ver §10. |
| 7 | Sugerencias admin — `GET /sugerencias` | ✅ Implementado. Ver §14. |

---

### Consideraciones adicionales — Verificación directa en código

#### `/users/{id}/activar` — ✅ EXISTE

```
PATCH  /api/v1/users/{id}/activar    → activa usuario (activo = true)
DELETE /api/v1/users/{id}/desactivar → desactiva usuario (activo = false)
```

Ambos requieren `ROLE_ADMIN` o `ROLE_ADMIN_EMPRESA`. No requiere cambio alguno en el backend.

**Endpoints de usuarios adicionales descubiertos (no documentados previamente):**

| Método | Ruta | Descripción |
|--------|------|-------------|
| `PATCH` | `/users/{id}` | Actualizar datos de un usuario por admin |
| `PATCH` | `/users/{id}/activar` | Activar usuario (`activo = true`) |
| `GET` | `/users/paginado` | Listar paginado con búsqueda: `?page=0&size=20&search=texto` |

---

#### `POST /documentos/me/{id}/firmar` — ✅ COMPLETAMENTE IMPLEMENTADO

La firma **no estaba pendiente** — estaba implementada. La documentación anterior era incorrecta.

**Request body (`FirmarDocumentoRequest`):**
```json
{ "firmaBase64": "data:image/png;base64,iVBORw0KGgo..." }
```

- `firmaBase64` = salida de `canvas.toDataURL("image/png")`, con o sin el prefijo.
- El backend descarga el PDF, estampa la firma como imagen PNG, sube el PDF firmado a Supabase y actualiza `firmado=true`, `fechaFirma`, `firmaUrl` en la `DocumentoAsignacion`.
- Requiere `documento.requiereFirma = true`. Si ya firmado → `400`.

**Response `200 OK` (`FirmarDocumentoResponse`):**
```json
{ "firmaUrl": "https://supabase.../documentos/firmado-uuid.pdf" }
```

| Código | Cuándo |
|--------|--------|
| `200` | Firmado correctamente |
| `400` | Ya firmado o `requiereFirma = false` |
| `404` | Documento no asignado al usuario |

---

#### `PUT /anuncios/{id}` y `PATCH /anuncios/{id}/vistas` — ✅ AMBOS IMPLEMENTADOS

| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| `PUT` | `/anuncios/{id}` | `ADMIN_EMPRESA` | Editar anuncio propio. `403` si es ajeno o global |
| `PATCH` | `/anuncios/{id}/vistas` | Cualquier autenticado | Registrar vista → `204 No Content` |
| `GET` | `/anuncios/paginado` | Cualquier autenticado | `?page=0&size=20&search=texto` |

---

### Campos embebidos en TrainingModule (complemento al §5)

El modelo `TrainingModule` soporta contenido IA embebido directamente en la entidad del módulo. Gestionado via `POST /modulos` y `PUT /modulos/{id}`:

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `contenidoMarkdown` | `TEXT` | Contenido formativo en Markdown |
| `scriptPodcast` | `TEXT` | Guion conversacional para TTS |
| `scriptVideo` | `TEXT` | Slides en JSON array |
| `podcastAudioUrl` | `string` | URL del MP3 en Supabase |
| `adjuntoUrl` | `string` | URL de archivo adjunto manual |
| `adjuntoNombre` | `string` | Nombre original del adjunto |
| `testPreguntas` | `TEXT` | Preguntas del test en JSON |
| `tiposSalida` | `string` | `documentacion,podcast,video` (coma-separados) |
| `audiencia` | `string` | `todos` / `administradores` / `departamento` |
| `departamentos` | `TEXT` | JSON array de departamentos |
| `esEspecializadoIa` | `boolean` | Generado/asistido por IA |
| `duracion` | `string` | corto / medio / largo |
| `imagenPortadaUrl` | `string` | URL de portada en Supabase |

> **Patrón**: módulos generados con `/ai/generar-desde-archivo` usan los campos embebidos. Módulos con contenido granular (lecturas, vídeos, tests) usan `/contenidos`. Ambos coexisten sin conflicto. El frontend distingue por `esEspecializadoIa`.

---

### Historial completo de correcciones de documentación

| Problema | Corrección |
|---|---|
| §11 mezclaba dos controllers distintos | Separados en §11 (Eventos EGM, solo ADMIN, `/api/v1/eventos`) y §11b (Comunidad, ADMIN_EMPRESA, `/api/v1/comunidad/eventos`) |
| Módulo Servicios ausente | Añadido §16 + `docs/modulos/servicios.md` |
| Módulo Documentos ausente | Añadido §17 + `docs/modulos/documentos.md` |
| AI — Gemini documentado como principal | Groq es el principal; Gemini es fallback |
| Chat AI — body documentado incorrecto | `{ messages: [{role,content}], systemPrompt }` con response streaming `text/plain` |
| `DOCUMENTO_NUEVO` y `CERTIFICADO_GENERADO` no documentados | Añadidos a tipos canónicos de notificación |
| `PATCH /users/{id}/activar` — frontend lo creía inexistente | ✅ Existe e implementado |
| `POST /documentos/me/{id}/firmar` — marcado como "pendiente" | ✅ Completamente implementado |
| `PUT /anuncios/{id}` y `PATCH /anuncios/{id}/vistas` — marcados como dudosos | ✅ Ambos implementados |
| `PATCH /users/{id}`, `GET /users/paginado`, `GET /anuncios/paginado` — no documentados | Añadidos |
| Firma electrónica marcada como "endpoint pendiente" en `docs/modulos/documentos.md` | Corregido |
