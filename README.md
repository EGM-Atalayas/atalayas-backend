# Atalayas Backend

Backend REST API para la plataforma **EGM Atalayas**, desarrollado con **Spring Boot 3.2** y **Java 21**. Proporciona servicios de autenticación JWT, gestión de usuarios, módulos de formación, seguimiento de progreso, comunicaciones internas, beneficios para empleados e integración con IA.

---

## 📋 Índice

- [Tecnologías](#-tecnologías)
- [Arquitectura del proyecto](#-arquitectura-del-proyecto)
- [Requisitos previos](#-requisitos-previos)
- [Configuración](#-configuración)
- [Ejecución](#-ejecución)
- [API Reference](#-api-reference)
- [Autenticación](#-autenticación)
- [Base de datos](#-base-de-datos)
- [Documentación interactiva (Swagger)](#-documentación-interactiva-swagger)
- [Pruebas rápidas con curl](#-pruebas-rápidas-con-curl)

---

## 🛠 Tecnologías

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 21 | Lenguaje principal |
| Spring Boot | 3.2.4 | Framework base |
| Spring Security | (incluido en Boot) | Autenticación y autorización |
| Spring Data JPA | (incluido en Boot) | Capa de persistencia |
| PostgreSQL | Latest | Base de datos relacional |
| JJWT | 0.12.6 | Generación y validación de tokens JWT |
| SpringDoc OpenAPI | 2.5.0 | Documentación Swagger UI |
| Lombok | (incluido en Boot) | Reducción de boilerplate |
| Hypersistence Utils | 3.7.0 | Soporte de tipos JSONB en Hibernate 6 |
| Maven | (mvnw incluido) | Gestión de dependencias y build |

---

## 📁 Arquitectura del proyecto

```
backend/src/main/java/com/atalayas/backend/
│
├── auth/                  # Autenticación (login, registro, refresh token)
│   ├── controller/
│   ├── dto/
│   └── service/
│
├── user/                  # Gestión de usuarios
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── mapper/
│   ├── repository/
│   └── service/
│
├── role/                  # Roles y permisos
│   ├── entity/
│   ├── repository/
│   └── service/
│
├── module/                # Módulos de formación
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── repository/
│   └── service/
│
├── content/               # Contenido de los módulos (vídeos, textos, PDFs, evaluaciones)
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── repository/
│   └── service/
│
├── progress/              # Seguimiento del progreso del usuario
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── repository/
│   └── service/
│
├── company/               # Gestión de empresas
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── repository/
│   └── service/
│
├── communication/         # Anuncios y comunicaciones internas
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── repository/
│   └── service/
│
├── community/             # Eventos de comunidad
│   ├── controller/
│   ├── entity/
│   ├── repository/
│   └── service/
│
├── rewards/               # Beneficios y recompensas para empleados
│   ├── controller/
│   ├── entity/
│   ├── repository/
│   └── service/
│
├── ai/                    # Integración con IA (OpenAI)
│   ├── client/
│   ├── controller/
│   ├── dto/
│   └── service/
│
├── audit/                 # Registro de auditoría
│   ├── entity/
│   ├── repository/
│   └── service/
│
├── security/              # Configuración de seguridad JWT
│   ├── SecurityConfig.java
│   ├── JwtAuthenticationFilter.java
│   ├── JwtService.java
│   ├── CustomUserDetailsService.java
│   ├── AuthEntryPointJwt.java
│   └── SecurityConstants.java
│
├── config/                # Configuración de CORS, JPA, OpenAPI
├── common/                # Enums, DTOs y utilidades compartidas
└── exception/             # Manejo centralizado de excepciones
```

---

## ✅ Requisitos previos

- **Java 21** o superior
- **Maven 3.8+** (o usar el wrapper `./mvnw` incluido)
- **PostgreSQL 14+** (local o en la nube — se incluye configuración para [Neon.tech](https://neon.tech))
- *(Opcional)* **Docker** — para levantar la base de datos con `compose.yaml`

---

## ⚙️ Configuración

El fichero de configuración se encuentra en:

```
backend/src/main/resources/application.properties
```

### Variables principales

```properties
# Conexión a PostgreSQL
spring.datasource.url=jdbc:postgresql://<host>/<database>?sslmode=require
spring.datasource.username=<usuario>
spring.datasource.password=<contraseña>

# JWT — clave secreta mínimo 256 bits para HMAC-SHA256
app.jwt.secret=<clave-secreta-256-bits>

# CORS — orígenes del frontend permitidos
cors.allowed-origins=http://localhost:3000,http://localhost:5173,http://localhost:4200
```

> ⚠️ **Importante:** En producción, gestiona las credenciales con variables de entorno o un gestor de secretos (Vault, AWS Secrets Manager, etc.) y **nunca** las almacenes en el repositorio.

### Levantar PostgreSQL con Docker

```bash
cd backend
docker compose up -d
```

El `compose.yaml` incluido levanta un contenedor de PostgreSQL con una base de datos `mydatabase` lista para desarrollo.

---

## 🚀 Ejecución

### Con Maven Wrapper (recomendado)

```bash
cd backend
./mvnw spring-boot:run
```

### Con Maven instalado

```bash
cd backend
mvn spring-boot:run
```

### Construir el JAR

```bash
cd backend
./mvnw clean package -DskipTests
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

La aplicación arranca en `http://localhost:8080` por defecto.

---

## 📡 API Reference

### Autenticación — `/api/v1/auth`

| Método | Endpoint | Descripción | Auth |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Registrar nuevo usuario | ❌ Pública |
| `POST` | `/api/v1/auth/login` | Iniciar sesión y obtener tokens JWT | ❌ Pública |
| `POST` | `/api/v1/auth/refresh-token` | Renovar el access token | ❌ Pública |

### Usuarios — `/api/v1/users`

| Método | Endpoint | Descripción | Rol requerido |
|---|---|---|---|
| `GET` | `/api/v1/users/me` | Perfil del usuario autenticado | Cualquier autenticado |
| `GET` | `/api/v1/users/{id}` | Obtener usuario por ID | ADMIN / SUPER_ADMIN |
| `GET` | `/api/v1/users` | Listar todos los usuarios | ADMIN / SUPER_ADMIN |
| `DELETE` | `/api/v1/users/{id}/desactivar` | Desactivar usuario (soft delete) | ADMIN / SUPER_ADMIN |

---

## 🔐 Autenticación

El backend utiliza **JWT (JSON Web Tokens)** con el esquema **Bearer**:

- **Access Token** — válido durante **1 hora**
- **Refresh Token** — válido durante **7 días**
- **Algoritmo** — HMAC-SHA256 (`HS256`)

### Cabecera requerida en endpoints protegidos

```
Authorization: Bearer <access_token>
```

### Roles del sistema

| Rol | Descripción |
|---|---|
| `ROLE_SUPER_ADMIN` | Acceso total al sistema |
| `ROLE_ADMIN` | Gestión de usuarios y contenido |
| `ROLE_MANAGER` | Gestión de equipos |
| `ROLE_EMPLOYEE` | Usuario base (asignado por defecto al registrarse) |

### Flujo de autenticación

```
POST /api/v1/auth/login  { email, password }
  │
  ▼
AuthService valida credenciales con BCrypt
  │
  ▼
Genera accessToken (1h) + refreshToken (7d)
  │
  ▼
AuthResponse { accessToken, refreshToken, tokenType, expiresIn, email, fullName, role }
  │
  ▼
Cliente usa:  Authorization: Bearer <accessToken>
```

---

## 🗄️ Base de datos

La aplicación usa `ddl-auto=validate` — **el esquema debe existir antes de arrancar**.

### Script de inicialización

```sql
-- Permisos
CREATE TABLE permissions (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- Roles
CREATE TABLE roles (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(50)  NOT NULL UNIQUE
);

-- Relación roles ↔ permisos
CREATE TABLE role_permissions (
    role_id       BIGINT NOT NULL REFERENCES roles(id),
    permission_id BIGINT NOT NULL REFERENCES permissions(id),
    PRIMARY KEY (role_id, permission_id)
);

-- Usuarios
CREATE TABLE users (
    id                       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name               VARCHAR(100) NOT NULL,
    last_name                VARCHAR(100) NOT NULL,
    email                    VARCHAR(150) NOT NULL UNIQUE,
    password                 TEXT         NOT NULL,
    enabled                  BOOLEAN      NOT NULL DEFAULT TRUE,
    account_non_expired      BOOLEAN      NOT NULL DEFAULT TRUE,
    account_non_locked       BOOLEAN      NOT NULL DEFAULT TRUE,
    credentials_non_expired  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMP,
    updated_at               TIMESTAMP
);

-- Relación usuarios ↔ roles
CREATE TABLE user_roles (
    user_id UUID   NOT NULL REFERENCES users(id),
    role_id BIGINT NOT NULL REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

-- Datos iniciales de roles (obligatorio para el registro)
INSERT INTO roles (name) VALUES
    ('ROLE_SUPER_ADMIN'),
    ('ROLE_ADMIN'),
    ('ROLE_MANAGER'),
    ('ROLE_EMPLOYEE');
```

---

## 📖 Documentación interactiva (Swagger)

Una vez arrancada la aplicación, accede a la UI de Swagger:

```
http://localhost:8080/swagger-ui.html
```

La especificación OpenAPI en JSON/YAML está disponible en:

```
http://localhost:8080/v3/api-docs
```

---

## 🧪 Pruebas rápidas con curl

```bash
# 1. Registrar usuario
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Juan","lastName":"Pérez","email":"juan@test.com","password":"mipass123"}'

# 2. Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"juan@test.com","password":"mipass123"}'

# 3. Obtener perfil (usar el accessToken del paso anterior)
curl -X GET http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

---

## 📦 Módulos funcionales

| Módulo | Descripción |
|---|---|
| `auth` | Registro, login y refresco de tokens JWT |
| `user` | Gestión de perfiles de usuario |
| `role` | Roles y permisos del sistema |
| `module` | Módulos de formación (general, especializado, onboarding, cumplimiento) |
| `content` | Contenidos multimedia: vídeo, texto, PDF, evaluaciones, IA |
| `progress` | Seguimiento del progreso del usuario por contenido |
| `company` | Gestión de empresas y su contexto industrial |
| `communication` | Anuncios y comunicados internos |
| `community` | Eventos de comunidad |
| `rewards` | Beneficios y recompensas para empleados |
| `ai` | Generación y análisis de contenido con IA (OpenAI) |
| `audit` | Registro de auditoría de acciones del sistema |

---

## 🛡️ Seguridad

- Autenticación **stateless** con JWT (sin sesiones HTTP)
- Contraseñas hasheadas con **BCrypt**
- **CSRF desactivado** (API REST)
- Autorización basada en roles con `@PreAuthorize`
- Respuestas de error **estandarizadas en JSON** para todos los endpoints
- **CORS** configurado para los orígenes del frontend (ajustable por variable de entorno)
- URLs públicas: `POST /api/v1/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/health`
