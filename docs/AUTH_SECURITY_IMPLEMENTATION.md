# 🔐 Documentación técnica — Capa de Autenticación y Seguridad

> **Proyecto:** Atalayas Backend  
> **Paquete raíz:** `egm.atalayas.backend`  
> **Spring Boot:** 3.3.0 · **Java:** 21 · **JJWT:** 0.12.6

---

## 📁 Estructura de paquetes implementada

```
egm.atalayas.backend
│
├── auth/
│   ├── controller/    AuthController.java
│   ├── service/       AuthService.java
│   ├── dto/           LoginRequest · RegisterRequest · AuthResponse · RefreshTokenRequest
│   └── mapper/        (vacío, pendiente)
│
├── security/
│   ├── SecurityConfig.java
│   ├── JwtAuthenticationFilter.java
│   ├── JwtService.java
│   ├── CustomUserDetailsService.java
│   ├── AuthEntryPointJwt.java
│   └── SecurityConstants.java
│
├── user/
│   ├── controller/    UserController.java
│   ├── service/       UserService.java
│   ├── repository/    UserRepository.java
│   ├── entity/        User.java
│   ├── dto/           UserRequest · UserResponse · UserProfileResponse
│   └── mapper/        UserMapper.java
│
├── role/
│   ├── entity/        Role.java · Permission.java
│   ├── repository/    RoleRepository.java · PermissionRepository.java
│   └── service/       (vacío, pendiente)
│
├── common/
│   └── enums/         RoleType.java
│
└── exception/
    ├── GlobalExceptionHandler.java
    ├── ResourceNotFoundException.java
    ├── BusinessException.java
    └── UnauthorizedException.java
```

---

## 🗂️ Descripción detallada por capa

---

### 1. `common/enums/RoleType.java`

Enum que define los 4 roles del sistema:

| Valor | Descripción |
|---|---|
| `ROLE_SUPER_ADMIN` | Acceso total al sistema |
| `ROLE_ADMIN` | Gestión de usuarios y contenido |
| `ROLE_MANAGER` | Gestión de equipos |
| `ROLE_EMPLOYEE` | Usuario base (rol por defecto en el registro) |

Los valores llevan el prefijo `ROLE_` para ser compatibles directamente con `hasRole()` y `hasAnyRole()` de Spring Security.

---

### 2. `role/entity/`

#### `Permission.java`
Entidad JPA que representa un permiso granular del sistema.

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `Long` | PK autogenerada |
| `name` | `String` | Nombre único del permiso (ej: `USER_READ`) |

**Tabla:** `permissions`

#### `Role.java`
Entidad JPA que agrupa permisos y se asigna a usuarios.

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `Long` | PK autogenerada |
| `name` | `RoleType` | Enum almacenado como `STRING` |
| `permissions` | `Set<Permission>` | Relación `@ManyToMany` con fetch `EAGER` |

**Tabla:** `roles`  
**Tabla intermedia:** `role_permissions (role_id, permission_id)`

---

### 3. `user/entity/User.java`

Entidad JPA central del sistema. Implementa `UserDetails` de Spring Security para integrarse directamente con el mecanismo de autenticación.

| Campo | Tipo | Notas |
|---|---|---|
| `id` | `Long` | PK autogenerada |
| `firstName` | `String` | Obligatorio |
| `lastName` | `String` | Obligatorio |
| `email` | `String` | Unique · usado como `username` |
| `password` | `String` | Hasheado con BCrypt |
| `enabled` | `boolean` | `true` por defecto |
| `accountNonExpired` | `boolean` | `true` por defecto |
| `accountNonLocked` | `boolean` | `true` por defecto |
| `credentialsNonExpired` | `boolean` | `true` por defecto |
| `createdAt` | `LocalDateTime` | Rellenado en `@PrePersist` |
| `updatedAt` | `LocalDateTime` | Actualizado en `@PreUpdate` |
| `roles` | `Set<Role>` | `@ManyToMany` con fetch `EAGER` |

**Tabla:** `users`  
**Tabla intermedia:** `user_roles (user_id, role_id)`

**Métodos clave:**
- `getUsername()` → devuelve el `email`
- `getAuthorities()` → convierte cada `Role.name` en un `SimpleGrantedAuthority`
- `getFullName()` → concatena `firstName + " " + lastName`

---

### 4. `security/SecurityConstants.java`

Clase de constantes estáticas (no instanciable):

```
TOKEN_PREFIX              = "Bearer "
HEADER_STRING             = "Authorization"
ACCESS_TOKEN_EXPIRATION   = 1 hora   (en ms)
REFRESH_TOKEN_EXPIRATION  = 7 días   (en ms)
PUBLIC_URLS[]             = rutas sin autenticación
```

**URLs públicas configuradas:**
- `POST /api/v1/auth/**` — login y registro
- `/v3/api-docs/**` — OpenAPI spec
- `/swagger-ui/**` — UI de Swagger
- `/actuator/health` — health check

---

### 5. `security/JwtService.java`

Servicio que encapsula toda la lógica de tokens JWT usando **JJWT 0.12.6**.

**Configuración:**  
Lee la clave secreta desde `application.properties`:
```properties
app.jwt.secret=<clave de mínimo 256 bits>
```

**Métodos:**

| Método | Descripción |
|---|---|
| `generateAccessToken(UserDetails)` | Token de acceso con expiración de 1 hora |
| `generateRefreshToken(UserDetails)` | Token de refresco con expiración de 7 días |
| `extractUsername(token)` | Extrae el `subject` (email) del token |
| `isTokenValid(token, UserDetails)` | Valida firma + expiración + coincidencia de usuario |
| `extractClaim(token, resolver)` | Extrae cualquier claim con un `Function<Claims, T>` |

**Algoritmo:** HMAC-SHA256 (`HS256`) con clave derivada de la propiedad `app.jwt.secret`.

---

### 6. `security/CustomUserDetailsService.java`

Implementa `UserDetailsService` de Spring Security.  
Sobrescribe `loadUserByUsername(String email)` para buscar el usuario en la base de datos por email.

- Si no existe → lanza `UsernameNotFoundException`
- Si existe → devuelve el `User` (que implementa `UserDetails`)

Usado por el `DaoAuthenticationProvider` en `SecurityConfig` y por el `JwtAuthenticationFilter`.

---

### 7. `security/AuthEntryPointJwt.java`

Implementa `AuthenticationEntryPoint`.  
Se dispara automáticamente cuando un request accede a un endpoint protegido **sin** token o con token inválido.

**Respuesta JSON estandarizada (HTTP 401):**
```json
{
  "status": 401,
  "error": "No autorizado",
  "message": "Acceso denegado: ...",
  "path": "/api/v1/users/me"
}
```

---

### 8. `security/JwtAuthenticationFilter.java`

Extiende `OncePerRequestFilter`. Se ejecuta **una vez por cada request HTTP**.

**Flujo:**
```
Request entrante
    │
    ▼
¿Tiene cabecera Authorization: Bearer <token>?
    ├─ NO → continúa sin autenticar
    └─ SÍ  → extrae el token
               │
               ▼
           Extrae email del token
               │
               ▼
           ¿Ya hay autenticación en SecurityContext?
               ├─ SÍ  → no hace nada
               └─ NO  → carga UserDetails desde BD
                           │
                           ▼
                       ¿Token válido para ese usuario?
                           ├─ SÍ  → establece autenticación en SecurityContext
                           └─ NO  → loguea el error y continúa sin autenticar
```

---

### 9. `security/SecurityConfig.java`

Clase de configuración central de Spring Security.

**Beans definidos:**

| Bean | Descripción |
|---|---|
| `SecurityFilterChain` | Cadena de filtros: sin CSRF, stateless, rutas públicas, añade el JWT filter |
| `AuthenticationProvider` | `DaoAuthenticationProvider` con `CustomUserDetailsService` + `BCryptPasswordEncoder` |
| `AuthenticationManager` | Exposición del manager para uso en `AuthService` |
| `PasswordEncoder` | `BCryptPasswordEncoder` |

**Características:**
- `@EnableMethodSecurity` → activa `@PreAuthorize` en los controllers
- `SessionCreationPolicy.STATELESS` → sin sesiones HTTP
- `csrf().disable()` → desactivado (API REST con JWT no necesita CSRF)

---

### 10. `auth/dto/`

| DTO | Campos | Uso |
|---|---|---|
| `LoginRequest` | `email` · `password` | Body del `POST /login` |
| `RegisterRequest` | `firstName` · `lastName` · `email` · `password` | Body del `POST /register` |
| `AuthResponse` | `accessToken` · `refreshToken` · `tokenType` · `expiresIn` · `email` · `fullName` · `role` | Respuesta de login/register/refresh |
| `RefreshTokenRequest` | `refreshToken` | Body del `POST /refresh-token` |

Todos usan **anotaciones de validación** (`@NotBlank`, `@Email`, `@Size`) de `spring-boot-starter-validation`.

---

### 11. `auth/service/AuthService.java`

Lógica de negocio de autenticación.

#### `register(RegisterRequest)`
1. Comprueba que no existe ya un usuario con ese email
2. Busca el rol `ROLE_EMPLOYEE` en BD (lanza error si no existe)
3. Crea el `User` con la contraseña hasheada
4. Persiste el usuario
5. Genera `accessToken` + `refreshToken`
6. Devuelve `AuthResponse`

#### `login(LoginRequest)`
1. Delega en `AuthenticationManager.authenticate()` → valida credenciales automáticamente
2. Si las credenciales son incorrectas → Spring lanza `BadCredentialsException` → capturado por `GlobalExceptionHandler` → `401`
3. Carga el usuario y genera tokens
4. Devuelve `AuthResponse`

#### `refreshToken(RefreshTokenRequest)`
1. Extrae el email del refresh token
2. Carga el usuario
3. Valida que el refresh token sea válido y no haya expirado
4. Genera nuevos `accessToken` y `refreshToken`
5. Devuelve `AuthResponse`

---

### 12. `auth/controller/AuthController.java`

**Base URL:** `/api/v1/auth`

| Método | Endpoint | Descripción | Auth requerida |
|---|---|---|---|
| `POST` | `/register` | Registra un nuevo usuario | ❌ No |
| `POST` | `/login` | Inicia sesión y devuelve tokens | ❌ No |
| `POST` | `/refresh-token` | Renueva el access token | ❌ No |

---

### 13. `user/dto/`

| DTO | Campos | Uso |
|---|---|---|
| `UserRequest` | `firstName` · `lastName` · `email` | Actualización de datos |
| `UserResponse` | `id` · `firstName` · `lastName` · `email` · `enabled` · `roles` · `createdAt` | Listado de usuarios |
| `UserProfileResponse` | Todos los anteriores + `fullName` · `updatedAt` | Perfil propio |

---

### 14. `user/mapper/UserMapper.java`

Componente Spring que convierte entidades `User` a DTOs sin usar dependencias externas (MapStruct no está en el pom).

| Método | Entrada | Salida |
|---|---|---|
| `toUserResponse(User)` | `User` | `UserResponse` |
| `toUserProfileResponse(User)` | `User` | `UserProfileResponse` |

---

### 15. `user/service/UserService.java`

| Método | Descripción | Rol requerido |
|---|---|---|
| `getCurrentUserProfile()` | Lee el email del `SecurityContext` y devuelve el perfil | Cualquier autenticado |
| `getUserById(Long)` | Busca por ID o lanza `ResourceNotFoundException` | ADMIN / SUPER_ADMIN |
| `getAllUsers()` | Lista todos los usuarios | ADMIN / SUPER_ADMIN |
| `disableUser(Long)` | Pone `enabled = false` sin borrar el registro | ADMIN / SUPER_ADMIN |

---

### 16. `user/controller/UserController.java`

**Base URL:** `/api/v1/users`  
Requiere `Authorization: Bearer <token>` en todas las rutas.

| Método | Endpoint | Descripción | Rol requerido |
|---|---|---|---|
| `GET` | `/me` | Perfil del usuario autenticado | Cualquiera |
| `GET` | `/{id}` | Usuario por ID | ADMIN / SUPER_ADMIN |
| `GET` | `/` | Todos los usuarios | ADMIN / SUPER_ADMIN |
| `DELETE` | `/{id}/disable` | Deshabilitar usuario | ADMIN / SUPER_ADMIN |

---

### 17. `exception/`

| Clase | HTTP | Cuándo se lanza |
|---|---|---|
| `ResourceNotFoundException` | `404` | Entidad no encontrada en BD |
| `BusinessException` | `400` | Regla de negocio violada |
| `UnauthorizedException` | `401` | Acceso no autorizado explícito |
| `GlobalExceptionHandler` | — | Captura todas las excepciones y devuelve JSON estandarizado |

**Formato de error estandarizado:**
```json
{
  "timestamp": "2026-03-09T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Usuario no encontrado con id: 5"
}
```

---

## 🔄 Flujo completo de login

```
Cliente
  │
  ▼  POST /api/v1/auth/login  { email, password }
AuthController.login()
  │
  ▼
AuthService.login()
  │
  ├─ AuthenticationManager.authenticate()
  │     └─ DaoAuthenticationProvider
  │           ├─ CustomUserDetailsService.loadUserByUsername(email) → User desde BD
  │           └─ BCryptPasswordEncoder.matches(rawPassword, hashedPassword)
  │
  ├─ JwtService.generateAccessToken(user)  → JWT firmado (1h)
  ├─ JwtService.generateRefreshToken(user) → JWT firmado (7d)
  │
  ▼
AuthResponse { accessToken, refreshToken, tokenType, expiresIn, email, fullName, role }
  │
  ▼
Cliente almacena los tokens y usa:
  Authorization: Bearer <accessToken>  en cada request posterior
```

---

## 🔄 Flujo de request autenticado

```
Cliente  →  GET /api/v1/users/me
              Header: Authorization: Bearer eyJhbGci...
                │
                ▼
          JwtAuthenticationFilter
                │
                ├─ Extrae token del header
                ├─ JwtService.extractUsername(token) → email
                ├─ CustomUserDetailsService.loadUserByUsername(email)
                ├─ JwtService.isTokenValid(token, userDetails)
                └─ Establece UsernamePasswordAuthenticationToken en SecurityContext
                │
                ▼
          UserController.getCurrentUserProfile()
                │
                ▼
          UserService.getCurrentUserProfile()
                └─ Lee email de SecurityContext → busca User → devuelve UserProfileResponse
```

---

## ⚙️ Dependencias añadidas al `pom.xml`

```xml
<!-- JJWT 0.12.6 — generación y validación de tokens JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

---

## ⚙️ Propiedades añadidas a `application.properties`

```properties
# JWT — clave secreta mínimo 256 bits para HMAC-SHA256
app.jwt.secret=4d8a1f2e9b3c7d6a0e5f1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e
```

> ⚠️ **Importante:** en producción esta clave debe gestionarse con variables de entorno o un gestor de secretos (Vault, AWS Secrets Manager, etc.) y nunca commitearse en el repositorio.

---

## 📋 Requisitos en base de datos

Las siguientes tablas deben existir antes de arrancar la aplicación (`ddl-auto=none`):

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
    id                       BIGSERIAL PRIMARY KEY,
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
    user_id BIGINT NOT NULL REFERENCES users(id),
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

## 🧪 Prueba rápida con curl

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

