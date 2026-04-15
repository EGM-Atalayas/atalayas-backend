# Índice de documentación — EGM Atalayas Backend

> **Última actualización:** Abril 2026

Todos los documentos están en la carpeta `docs/` del proyecto.

---

## Documentos generales

| Documento | Descripción | Audiencia principal |
|---|---|---|
| [README.md](../README.md) | Arranque rápido, stack y estructura del proyecto | Todos |
| [arquitectura.md](arquitectura.md) | Diagrama de capas, modelo multi-tenant, relación entre módulos | Nuevo desarrollador backend |
| [api-endpoints.md](api-endpoints.md) | Referencia completa de todos los endpoints REST (métodos, rutas, roles, respuestas) | Frontend, Backend |
| [seguridad-cookies-jwt.md](seguridad-cookies-jwt.md) | JWT, cookies HttpOnly, flujos de autenticación, integración frontend | Frontend, Backend |
| [configuracion-despliegue.md](configuracion-despliegue.md) | Variables de entorno, diferencias dev/prod, Docker, Railway | DevOps, Backend |
| [tests.md](tests.md) | Estrategia de tests, cómo ejecutarlos, qué cubre cada suite | Backend |

---

## Módulos funcionales (`docs/modulos/`)

Un fichero por cada módulo del sistema. Cada documento incluye: propósito, entidades, reglas de negocio, endpoints y diagrama de flujo.

| Módulo | Fichero | Descripción breve |
|---|---|---|
| Autenticación | [modulos/auth.md](modulos/auth.md) | Login, registro, refresh-token, logout, restauración de sesión |
| Usuarios | [modulos/usuarios.md](modulos/usuarios.md) | Gestión de cuentas de usuario, perfiles, soft-delete |
| Empresas | [modulos/empresas.md](modulos/empresas.md) | Alta de empresas, flujo de aprobación, estados |
| Módulos de formación | [modulos/modulos-formacion.md](modulos/modulos-formacion.md) | Contenedores de contenido formativo, tipos, globales vs empresa |
| Contenidos | [modulos/contenidos.md](modulos/contenidos.md) | Artículos, vídeos, PDFs, evaluaciones, preguntas, versiones |
| Progreso | [modulos/progreso.md](modulos/progreso.md) | Trazabilidad de lectura y completado de contenidos por empleado |
| Comunicación | [modulos/comunicacion.md](modulos/comunicacion.md) | Anuncios de empresa, comunicados oficiales EGM y notificaciones personales |
| Comunidad | [modulos/comunidad.md](modulos/comunidad.md) | Eventos del parque empresarial, globales y de empresa |
| Beneficios | [modulos/beneficios.md](modulos/beneficios.md) | Ventajas y beneficios del área EGM para empleados |
| Dashboard | [modulos/dashboard.md](modulos/dashboard.md) | Métricas, gráficas e incidencias para los paneles de control |
| Inteligencia Artificial | [modulos/ia.md](modulos/ia.md) | Generación de contenido, preguntas, resumen y chatbot con Gemini |
| Auditoría | [modulos/auditoria.md](modulos/auditoria.md) | Registro de eventos relevantes del sistema |

---

## Cómo navegar esta documentación

### Si eres desarrollador frontend
1. Empieza por **[seguridad-cookies-jwt.md](seguridad-cookies-jwt.md)** — entiende cómo funciona la autenticación con cookies
2. Lee **[api-endpoints.md](api-endpoints.md)** — referencia de todos los endpoints
3. Consulta el fichero del módulo concreto que necesites integrar

### Si eres nuevo desarrollador backend
1. Empieza por **[README.md](../README.md)** — para arrancar el proyecto en local
2. Lee **[arquitectura.md](arquitectura.md)** — para entender la estructura general
3. Lee **[seguridad-cookies-jwt.md](seguridad-cookies-jwt.md)** — la seguridad es transversal a todo
4. Lee los módulos funcionales en orden: `empresas → usuarios → modulos-formacion → contenidos → progreso`

### Si eres DevOps
1. Consulta **[configuracion-despliegue.md](configuracion-despliegue.md)** directamente

