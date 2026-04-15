# EGM Atalayas — Backend

API REST del **Parque Empresarial EGM Atalayas**: plataforma de formación, comunicación y gestión de empleados para las empresas del área empresarial.

---

## Stack tecnológico

| Tecnología | Versión | Rol |
|---|---|---|
| Java | 21 | Lenguaje principal |
| Spring Boot | 3.2.4 | Framework base |
| Spring Security | 6.x | Autenticación y autorización |
| Spring Data JPA / Hibernate | 6.x | ORM y acceso a datos |
| PostgreSQL (Neon.tech) | — | Base de datos principal |
| JJWT | 0.12.6 | Generación y validación de JWT |
| SpringDoc OpenAPI | 2.5.0 | Documentación Swagger |
| Lombok | — | Reducción de boilerplate |
| Gemini 2.0 Flash (Google AI) | — | Generación de contenido con IA |
| Apache PDFBox + POI | — | Extracción de texto de PDF/DOCX |
| Spring Mail (SMTP) | — | Envío de correos transaccionales |
| H2 (solo tests) | — | BD en memoria para tests CI |

---

## Arranque en local

### Requisitos previos
- Java 21
- Maven 3.9+ (o usar el wrapper `./mvnw`)
- Una base de datos PostgreSQL accesible (puedes usar Neon.tech o Docker)

### 1. Clonar y configurar variables de entorno

```bash
# Mínimo necesario para arrancar en local
export DB_URL=jdbc:postgresql://localhost:5432/atalayas
export DB_USER=myuser
export DB_PASSWORD=secret
export MAIL_USER=tucorreo@gmail.com
export MAIL_PASSWORD=tu_app_password
# Opcionales (tienen valores por defecto para dev)
export JWT_SECRET=atalayas-local-dev-secret-key-must-be-at-least-32-characters-long
export COOKIE_SECURE=false
export CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
```

### 2. (Alternativa) Levantar PostgreSQL con Docker

```bash
docker compose up -d
# Luego actualiza DB_URL, DB_USER, DB_PASSWORD con los valores del compose.yaml
```

### 3. Compilar y arrancar

```bash
./mvnw spring-boot:run
# En Windows:
mvnw.cmd spring-boot:run
```

La API queda disponible en: `http://localhost:8080`

### 4. Verificar que funciona

```
GET http://localhost:8080/swagger-ui.html   → Interfaz Swagger (requiere login)
GET http://localhost:8080/v3/api-docs       → Especificación OpenAPI en JSON
```

---

## Ejecutar los tests

```bash
./mvnw test
```

Los tests usan una BD H2 en memoria (perfil `test`). No necesitan PostgreSQL ni variables de entorno.

---

## Estructura del proyecto

```
src/main/java/com/atalayas/backend/
├── ai/              → Integración con Gemini AI
├── audit/           → Log de auditoría del sistema
├── auth/            → Login, registro, refresh-token, logout
├── common/          → Enums compartidos y utilidades (CookieUtil)
├── communication/   → Anuncios, comunicados oficiales y notificaciones
├── community/       → Eventos de comunidad del parque empresarial
├── company/         → Gestión de empresas y solicitudes de alta
├── config/          → CORS, JPA, OpenAPI
├── content/         → Contenidos formativos y preguntas de evaluación
├── dashboard/       → Métricas y gráficas para los dashboards
├── exception/       → Manejo global de errores
├── incidencia/      → Incidencias del sistema (solo superadmin)
├── module/          → Módulos de formación
├── progress/        → Trazabilidad de progreso de empleados
├── rewards/         → Beneficios del parque empresarial
├── role/            → Roles del sistema
├── security/        → Filtro JWT, SecurityConfig, JwtService
└── user/            → Gestión de usuarios
```

---

## Documentación

| Documento | Descripción | Audiencia |
|---|---|---|
| [docs/indice.md](docs/indice.md) | Índice completo de toda la documentación | Todos |
| [docs/arquitectura.md](docs/arquitectura.md) | Visión general, capas y modelo de roles | Backend |
| [docs/api-endpoints.md](docs/api-endpoints.md) | Referencia completa de todos los endpoints REST | Frontend / Backend |
| [docs/seguridad-cookies-jwt.md](docs/seguridad-cookies-jwt.md) | JWT, cookies HttpOnly, flujos de auth | Frontend / Backend |
| [docs/configuracion-despliegue.md](docs/configuracion-despliegue.md) | Variables de entorno, Docker, Railway | DevOps / Backend |
| [docs/tests.md](docs/tests.md) | Estrategia de tests y cómo ejecutarlos | Backend |
| [docs/modulos/](docs/modulos/) | Un fichero por cada módulo funcional | Frontend / Backend |

---

## Roles de la plataforma

| Rol | Descripción |
|---|---|
| `ROLE_ADMIN` | Superadmin EGM — acceso total a toda la plataforma |
| `ROLE_ADMIN_EMPRESA` | Admin de una empresa — scope limitado a su empresa |
| `ROLE_EMPLEADO` | Empleado — acceso de lectura/consumo dentro de su empresa |

---

## Despliegue en producción

El backend está desplegado en **Railway**. Ver [docs/configuracion-despliegue.md](docs/configuracion-despliegue.md) para la lista completa de variables de entorno y las diferencias entre dev y prod (cookies `Secure`, `SameSite=None`, `Partitioned`).

