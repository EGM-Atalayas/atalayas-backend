# Seguridad JWT — Autenticación mediante Cookies HttpOnly

> **Última actualización:** Abril 2026
> **Afecta a:** `auth/`, `security/`, `common/util/CookieUtil`, `config/`, `application.properties`

---

## Índice

1. [Visión general](#1-visión-general)
2. [Tokens JWT](#2-tokens-jwt)
3. [Cookies HttpOnly](#3-cookies-httponly)
4. [Flujos de autenticación](#4-flujos-de-autenticación)
5. [Filtro de autenticación](#5-filtro-de-autenticación)
6. [Configuración por entorno](#6-configuración-por-entorno)
7. [Integración con el frontend](#7-integración-con-el-frontend)
8. [Seguridad y mitigaciones](#8-seguridad-y-mitigaciones)
9. [Referencia rápida de constantes](#9-referencia-rápida-de-constantes)
10. [Diagrama de componentes](#10-diagrama-de-componentes)

---

## 1. Visión general

El sistema utiliza **JWT (JSON Web Tokens)** transportados en **cookies `HttpOnly`** como mecanismo principal de autenticación. Adicionalmente, el `accessToken` se incluye en el body de cada respuesta de login/registro/refresh para soportar escenarios cross-origin donde el navegador bloquea las cookies.

```
┌──────────┐  credenciales   ┌───────────────┐  genera tokens  ┌──────────────┐
│ Frontend │ ─────────────► │ AuthController │ ──────────────► │  JwtService  │
│          │                 │               │                  │              │
│          │ ◄────────────── │               │ ◄─────────────── │              │
│          │  Set-Cookie:    └───────────────┘  [access, refresh]└─────────────┘
│          │  accessToken
│          │  refreshToken
│          │  Body: { ...userData, accessToken }
└──────────┘

Peticiones posteriores:
┌──────────┐  Cookie: accessToken  ┌─────────────────────┐  valida JWT  ┌────────────┐
│ Frontend │ ────────────────────► │ JwtAuthenticationFilter│ ──────────► │ JwtService │
│          │    (automático)        │                      │              │            │
└──────────┘                       └──────────────────────┘              └────────────┘
```

---

## 2. Tokens JWT

### 2.1 Tipos de token

| Token | Cookie | Duración | Propósito |
|---|---|---|---|
| `accessToken` | `accessToken` | **1 hora** (3 600 s) | Autenticar cada petición a la API |
| `refreshToken` | `refreshToken` | **7 días** (604 800 s) | Obtener un nuevo par de tokens sin reautenticar |

### 2.2 Algoritmo y firma

- **Algoritmo:** `HMAC-SHA256` (`HS256`)
- **Clave de firma:** variable de entorno `JWT_SECRET` (mínimo 32 caracteres)
- **Librería:** `io.jsonwebtoken` (JJWT 0.12.6)

### 2.3 Estructura del payload (claims)

```json
{
  "sub": "usuario@ejemplo.com",   // email del usuario (username de Spring Security)
  "empresaId": "550e8400-...",    // UUID de la empresa (claim informativo, fuente de verdad = BD)
  "iat": 1712000000,              // issued at (timestamp Unix)
  "exp": 1712003600               // expiration (iat + duración)
}
```

> ⚠️ **Nota importante:** el claim `empresaId` es **informativo**. La fuente de verdad siempre
> es la base de datos. Nunca se usan los claims para decisiones de autorización multi-tenant
> sin validar contra BD.

### 2.4 Generación de tokens — `JwtService`

```java
// Access token — expira en 1 hora
String accessToken = jwtService.generateAccessToken(userDetails);

// Refresh token — expira en 7 días
String refreshToken = jwtService.generateRefreshToken(userDetails);

// Par completo (usado por AuthService.generateTokenPair)
String[] tokens = authService.generateTokenPair(email);
// tokens[0] = accessToken
// tokens[1] = refreshToken
```

### 2.5 Validación de tokens — `JwtService`

```java
// Verificar que el token es válido para un usuario
boolean valid = jwtService.isTokenValid(token, userDetails);
// Internamente comprueba:
//   1. username del token == userDetails.getUsername()
//   2. token no expirado

// Extraer el email del subject
String email = jwtService.extractUsername(token);

// Extraer el empresaId del claim
UUID empresaId = jwtService.extractEmpresaId(token); // null si el claim no existe
```

---

## 3. Cookies HttpOnly

### 3.1 Configuración de flags

Las cookies se construyen en `CookieUtil.addTokenCookie()` con los siguientes flags:

| Flag | Dev (`secure=false`) | Prod (`secure=true`) | Descripción |
|---|---|---|---|
| `HttpOnly` | ✅ `true` | ✅ `true` | JavaScript **no puede leer** la cookie |
| `Secure` | ❌ `false` | ✅ `true` | Solo se envía por **HTTPS** |
| `SameSite` | `Lax` | `None` | `None` permite cross-site en HTTPS; `Lax` evita la mayoría de CSRF en HTTP |
| `Partitioned` | ❌ `false` | ✅ `true` | **CHIPS** — evita el bloqueo de Chrome en contextos cross-site |
| `Path` | `/` | `/` | Válida para toda la aplicación |
| `MaxAge` | 3 600 s | 3 600 s | Expiración automática (accessToken) |

### 3.2 Por qué `Partitioned` en producción

En producción, el frontend (`atalayas-frontend.onrender.com`) y el backend (`atalayas-backend.onrender.com`) son subdominios distintos de `onrender.com`, que está en la **Public Suffix List**. Chrome los trata como cross-site y requiere el atributo `Partitioned` (CHIPS) para no bloquear las cookies con `SameSite=None`.

Como `ResponseCookie.partitioned()` solo está disponible desde Spring Framework 6.4 (Boot 3.4+) y este proyecto usa Spring Boot 3.2.4, el atributo se añade **manualmente** al header `Set-Cookie`:

```java
// CookieUtil.java
String cookieHeader = secure
    ? cookie.toString() + "; Partitioned"   // Prod: añade el atributo CHIPS
    : cookie.toString();                     // Dev: sin cambio
response.addHeader("Set-Cookie", cookieHeader);
```

### 3.3 Header resultante en producción

```
Set-Cookie: accessToken=eyJhbGciOi...; Path=/; Max-Age=3600; HttpOnly; Secure; SameSite=None; Partitioned
Set-Cookie: refreshToken=eyJhbGciOi...; Path=/; Max-Age=604800; HttpOnly; Secure; SameSite=None; Partitioned
```

### 3.4 Header resultante en desarrollo

```
Set-Cookie: accessToken=eyJhbGciOi...; Path=/; Max-Age=3600; HttpOnly; SameSite=Lax
Set-Cookie: refreshToken=eyJhbGciOi...; Path=/; Max-Age=604800; HttpOnly; SameSite=Lax
```

### 3.5 Invalidación de cookies (logout)

`CookieUtil.clearCookie()` establece `MaxAge=0` con los mismos flags, lo que hace que el navegador elimine la cookie inmediatamente:

```
Set-Cookie: accessToken=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=None; Partitioned
```

---

## 4. Flujos de autenticación

### 4.1 Login / Registro

```
Cliente                          Backend
  │                                │
  │  POST /auth/login              │
  │  { email, password }           │
  │ ─────────────────────────────► │
  │                                │  1. AuthenticationManager valida credenciales
  │                                │  2. Genera accessToken + refreshToken
  │                                │  3. Escribe ambas cookies en la respuesta
  │                                │  4. Incluye accessToken en el body
  │ ◄───────────────────────────── │
  │  Set-Cookie: accessToken=...   │
  │  Set-Cookie: refreshToken=...  │
  │  Body: {                       │
  │    accessToken: "eyJ...",      │  ← fallback para Bearer token
  │    usuarioId, email, nombre,   │
  │    codigoRol, empresaId, ...   │
  │  }                             │
```

### 4.2 Petición autenticada

```
Cliente                          Backend
  │                                │
  │  GET /api/v1/modulos           │
  │  Cookie: accessToken=eyJ...    │  ← el navegador lo adjunta automáticamente
  │ ─────────────────────────────► │
  │                                │  JwtAuthenticationFilter:
  │                                │  1. Lee cookie "accessToken"
  │                                │  2. Extrae email del subject
  │                                │  3. Carga UserDetails de BD
  │                                │  4. Valida firma y expiración
  │                                │  5. Establece Authentication en SecurityContext
  │ ◄───────────────────────────── │
  │  200 OK + datos                │
```

### 4.3 Restauración de sesión tras recarga de página

> El frontend **debe** llamar a `GET /auth/me` al arrancar para restaurar el estado de sesión,
> ya que las cookies no son accesibles por JavaScript.

```
App arranca / página se recarga
  │
  ├─► GET /auth/me  (con credentials: 'include')
  │
  │   ┌── 200 OK ──► Restaurar estado: { usuarioId, email, nombre, codigoRol,
  │   │                                  accessToken, empresaId, ... }
  │   │
  │   └── 401 ──► accessToken expiró
  │               │
  │               └─► POST /auth/refresh-token  (con credentials: 'include')
  │                   │
  │                   ├── 200 OK ──► Nuevos tokens en cookies + body → restaurar estado
  │                   │
  │                   └── 400/401 ──► refreshToken expirado → redirigir al login
```

> **Nota:** `GET /auth/me` devuelve el `accessToken` activo en el body, igual que `/login`.
> Esto permite al frontend tenerlo disponible para peticiones Bearer cuando las cookies
> sean bloqueadas por el entorno cross-origin.

### 4.4 Refresco de tokens

```
Cliente                          Backend
  │                                │
  │  POST /auth/refresh-token      │
  │  Cookie: refreshToken=eyJ...   │  ← sin body, el token va en la cookie
  │ ─────────────────────────────► │
  │                                │  1. Lee cookie "refreshToken"
  │                                │  2. Valida firma y expiración
  │                                │  3. Genera nuevo par de tokens
  │                                │  4. Sobreescribe las dos cookies
  │ ◄───────────────────────────── │
  │  Set-Cookie: accessToken=...   │  ← nuevo accessToken (1 hora)
  │  Set-Cookie: refreshToken=...  │  ← nuevo refreshToken (7 días)
  │  Body: { accessToken, ... }    │
```

### 4.5 Logout

```
Cliente                          Backend
  │                                │
  │  POST /auth/logout             │
  │ ─────────────────────────────► │
  │                                │  CookieUtil.clearCookie() × 2
  │ ◄───────────────────────────── │
  │  Set-Cookie: accessToken=;     │
  │    Max-Age=0                   │  ← el navegador elimina la cookie
  │  Set-Cookie: refreshToken=;    │
  │    Max-Age=0                   │
  │  Body: { "message": "Sesión    │
  │           cerrada" }           │
```

---

## 5. Filtro de autenticación

`JwtAuthenticationFilter` se ejecuta en **cada petición** antes de los controladores. Resuelve el token en el siguiente orden de prioridad:

```
1. Header Authorization: Bearer <token>   ← APIs externas, Postman, Swagger
2. Cookie HttpOnly "accessToken"          ← navegadores web
```

```java
// JwtAuthenticationFilter.resolveToken()
private String resolveToken(HttpServletRequest request) {
    // Prioridad 1: header Authorization
    final String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        return authHeader.substring(7);
    }
    // Prioridad 2: cookie HttpOnly
    if (request.getCookies() != null) {
        return Arrays.stream(request.getCookies())
                .filter(c -> "accessToken".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
    return null;
}
```

**Flujo completo del filtro:**

```
Petición entrante
  │
  ├─ resolveToken() ──► null ──► continuar sin autenticar (endpoint público → OK, protegido → 401)
  │
  └─ token encontrado
       │
       ├─ extractUsername(token) ──► error (token malformado) ──► log + continuar sin autenticar
       │
       └─ userDetailsService.loadUserByUsername(email)
            │
            ├─ isTokenValid(token, userDetails) = false (expirado o username distinto)
            │    └─► continuar sin autenticar → 401
            │
            └─ válido ──► setAuthentication(authToken) en SecurityContext ──► continuar → 200
```

---

## 6. Configuración por entorno

### 6.1 Variables de entorno

| Variable | Dev (defecto) | Prod (Render) | Descripción |
|---|---|---|---|
| `JWT_SECRET` | `atalayas-local-dev-secret-key-must-be-at-least-32-characters-long` | Secret aleatorio ≥32 chars | Clave HMAC de firma |
| `COOKIE_SECURE` | `false` | `true` | Activa `Secure` + `SameSite=None` + `Partitioned` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,...` | URL del frontend en Render | Orígenes CORS permitidos |

### 6.2 `application.properties`

```properties
app.jwt.secret=${JWT_SECRET:atalayas-local-dev-secret-key-must-be-at-least-32-characters-long}
app.cookie.secure=${COOKIE_SECURE:false}
cors.allowed-origins=${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:5173}
```

### 6.3 Diferencias dev vs prod

| Comportamiento | Dev (`secure=false`) | Prod (`secure=true`) |
|---|---|---|
| Protocolo | HTTP | HTTPS |
| Cookie `Secure` | ❌ | ✅ |
| Cookie `SameSite` | `Lax` | `None` |
| Cookie `Partitioned` | ❌ | ✅ |
| Accesible desde HTTP | ✅ | ❌ |
| Cross-site permitido | ❌ (Lax) | ✅ (None+Partitioned) |

---

## 7. Integración con el frontend

### 7.1 Requisito fundamental

**Todas las peticiones deben incluir `credentials: 'include'`** para que el navegador envíe y reciba las cookies.

### 7.2 Fetch API

```javascript
const response = await fetch('https://api.ejemplo.com/api/v1/auth/login', {
    method: 'POST',
    credentials: 'include',          // ← obligatorio
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
});
const data = await response.json();
// data.accessToken está disponible si se necesita como Bearer
```

### 7.3 Axios (configuración global)

```javascript
import axios from 'axios';

const api = axios.create({
    baseURL: 'https://api.ejemplo.com',
    withCredentials: true,            // ← obligatorio
});
```

### 7.4 Inicialización de la app (restaurar sesión)

```javascript
async function initApp() {
    try {
        // 1. Intentar restaurar sesión con el accessToken en cookie
        const { data } = await api.get('/api/v1/auth/me');
        authStore.setUser(data);       // data incluye accessToken, rol, empresa, etc.
    } catch (error) {
        if (error.response?.status === 401) {
            try {
                // 2. accessToken expirado → intentar refrescar con refreshToken cookie
                const { data } = await api.post('/api/v1/auth/refresh-token');
                authStore.setUser(data);
            } catch {
                // 3. refreshToken también expirado → redirigir al login
                router.push('/login');
            }
        }
    }
}
```

### 7.5 Fallback con Bearer token (escenarios cross-origin bloqueados)

Cuando las cookies son bloqueadas (ej. navegadores muy restrictivos o entornos de pruebas), el frontend puede usar el `accessToken` del body como header `Authorization`:

```javascript
// Guardar el token del body en memoria (nunca en localStorage por seguridad)
let inMemoryToken = null;

async function login(email, password) {
    const { data } = await api.post('/api/v1/auth/login', { email, password });
    inMemoryToken = data.accessToken;   // en memoria, no en localStorage
    authStore.setUser(data);
}

// Interceptor que añade el Bearer si existe
api.interceptors.request.use(config => {
    if (inMemoryToken) {
        config.headers.Authorization = `Bearer ${inMemoryToken}`;
    }
    return config;
});
```

---

## 8. Seguridad y mitigaciones

### 8.1 XSS (Cross-Site Scripting)

| Aspecto | Protección |
|---|---|
| Token en `HttpOnly` | JavaScript no puede leer el token via `document.cookie` |
| Token NO en localStorage | Evita robo directo por scripts inyectados |
| Token en body (fallback) | Solo usar en memoria, **nunca en localStorage/sessionStorage** |

### 8.2 CSRF (Cross-Site Request Forgery)

| Entorno | `SameSite` | Protección CSRF |
|---|---|---|
| Dev (HTTP) | `Lax` | Las cookies NO se envían en peticiones cross-site automáticas (`<img>`, `<form>`) |
| Prod (HTTPS) | `None` + `Partitioned` | Se confía en CORS con `allowCredentials=true` + orígenes explícitos |

> ⚠️ En producción, si se requiere máxima protección CSRF, considera añadir un CSRF token
> en un header separado (ej. `X-XSRF-Token`).

### 8.3 CORS

```java
// CorsConfig.java
config.setAllowedOrigins(allowedOrigins);     // orígenes explícitos, nunca "*"
config.setAllowCredentials(true);             // obligatorio para cookies cross-origin
config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
```

> `allowCredentials(true)` con `allowedOrigins("*")` está prohibido por el navegador.
> Siempre se deben especificar orígenes explícitos.

### 8.4 Expiración y rotación

| Token | Acción al expirar |
|---|---|
| `accessToken` (1h) | El filtro intercepta la petición **antes de llegar al controlador** y responde `401` inmediato con `{"message":"Token expirado o inválido. Usa /api/v1/auth/refresh-token..."}` → el frontend debe llamar a `POST /auth/refresh-token` |
| `refreshToken` (7d) | `/refresh-token` devuelve 401 → frontend redirige al login |

Los tokens se **rotan en cada refresco**: `/refresh-token` emite un par completamente nuevo y sobreescribe ambas cookies.

> 🔜 **Mejora futura:** implementar un interceptor de refresh automático en el frontend para que los 401 por token expirado sean transparentes para el usuario. Ver → [seguridad-token-expiration.md](./seguridad-token-expiration.md).

### 8.5 Bloqueo de cuenta

La entidad `User` implementa `isAccountNonLocked()`:
- La cuenta se bloquea automáticamente tras **5 intentos fallidos** (`intentosFallidos >= 5`)
- Spring Security devuelve 401 antes de que el filtro JWT llegue a validar el token

---

## 9. Referencia rápida de constantes

```java
// SecurityConstants.java
TOKEN_PREFIX            = "Bearer "
HEADER_STRING           = "Authorization"
ACCESS_TOKEN_EXPIRATION = 3_600_000 ms  (1 hora)
REFRESH_TOKEN_EXPIRATION= 604_800_000 ms (7 días)
ACCESS_TOKEN_COOKIE     = "accessToken"
REFRESH_TOKEN_COOKIE    = "refreshToken"

// Endpoints públicos (sin autenticación)
PUBLIC_URLS = [
    "/api/v1/auth/login",
    "/api/v1/auth/register",
    "/api/v1/auth/refresh-token",
    "/api/v1/auth/logout",
    "/api/v1/empresas/aprobadas",
    "/v3/api-docs/**",
    "/swagger-ui/**",
    "/swagger-ui.html"
]
```

---

## 10. Diagrama de componentes

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Capa de Seguridad                            │
│                                                                     │
│  Petición HTTP                                                      │
│       │                                                             │
│       ▼                                                             │
│  ┌────────────────────────┐                                         │
│  │  JwtAuthenticationFilter│  resolveToken()                        │
│  │  (OncePerRequestFilter) │  1. Authorization header               │
│  │                         │  2. Cookie "accessToken"               │
│  └──────────┬─────────────┘                                         │
│             │ token                                                  │
│             ▼                                                        │
│  ┌──────────────────┐     extractUsername()    ┌──────────────────┐ │
│  │   JwtService     │ ◄────────────────────── │  JwtService      │ │
│  │  (JJWT 0.12.6)  │     isTokenValid()       │                  │ │
│  └──────────────────┘                          └──────────────────┘ │
│             │ email                                                  │
│             ▼                                                        │
│  ┌──────────────────────────┐                                        │
│  │  CustomUserDetailsService │  loadUserByUsername(email) → BD      │
│  └──────────────────────────┘                                        │
│             │ UserDetails                                            │
│             ▼                                                        │
│  SecurityContextHolder.setAuthentication(authToken)                 │
│             │                                                        │
│             ▼                                                        │
│  ┌─────────────────────────────┐                                     │
│  │  SecurityConfig             │  @PreAuthorize / hasAuthority()     │
│  │  (método/endpoint guard)    │  ROLE_ADMIN / ROLE_ADMIN_EMPRESA /  │
│  │                             │  ROLE_EMPLEADO                      │
│  └─────────────────────────────┘                                     │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                      Capa de Autenticación                          │
│                                                                     │
│  AuthController                                                     │
│  ├── POST /login        → AuthService.login()   → CookieUtil ×2    │
│  ├── POST /register     → AuthService.register() → CookieUtil ×2   │
│  ├── POST /refresh-token→ AuthService.refreshToken()→ CookieUtil×2 │
│  ├── POST /logout       → CookieUtil.clearCookie() ×2              │
│  └── GET  /me           → AuthService.getCurrentUserInfo()         │
│                           + resolveToken(request) → accessToken     │
│                                                                     │
│  CookieUtil                                                         │
│  ├── addTokenCookie(response, name, value, maxAge, secure)         │
│  │   └── Flags: HttpOnly + Secure* + SameSite* + Partitioned*      │
│  └── clearCookie(response, name, secure)                           │
│      └── MaxAge=0 → navegador elimina la cookie                    │
└─────────────────────────────────────────────────────────────────────┘
* Solo en producción (COOKIE_SECURE=true)
```
