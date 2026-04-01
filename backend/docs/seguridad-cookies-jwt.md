# Seguridad JWT — Autenticación mediante Cookies HttpOnly

> **Fecha de implementación:** Marzo 2026  
> **Afecta a:** `auth/`, `security/`, `common/util/`, `config/`, `application.properties`

---

## ¿Por qué se hizo este cambio?

### Problema anterior

El flujo de autenticación original devolvía los tokens JWT directamente en el **body JSON** de la respuesta:

```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "email": "usuario@ejemplo.com"
}
```

Esto implicaba dos riesgos:

1. **Visibilidad en DevTools**: Cualquier persona con acceso al navegador podía ver el token en la pestaña *Network* o *Application → LocalStorage/SessionStorage* si el frontend lo almacenaba ahí.
2. **Vulnerabilidad XSS**: Si un script malicioso se inyectaba en el frontend, podía leer el token desde JavaScript y suplantár la identidad del usuario.

### Solución implementada

Los tokens se envían ahora como **cookies `HttpOnly`**, un mecanismo de seguridad del navegador que:

- **Impide el acceso desde JavaScript**: `document.cookie` no puede leer una cookie `HttpOnly`, por lo que un ataque XSS no puede robar el token.
- **Oculta el token de DevTools**: Las cookies no aparecen en la pestaña *Network* como parte del body de la respuesta, solo en la cabecera `Set-Cookie`.
- **El navegador las gestiona automáticamente**: Se adjuntan en cada petición al servidor sin intervención del frontend.

---

## Arquitectura del nuevo flujo

```
┌─────────────┐          POST /login           ┌─────────────────┐
│             │ ─────────────────────────────> │                 │
│  Frontend   │       {email, password}         │   AuthController│
│  (Browser)  │                                 │                 │
│             │ <───────────────────────────── │                 │
│             │  Set-Cookie: accessToken=...    └────────┬────────┘
│             │  Set-Cookie: refreshToken=...            │
│             │  Body: {email, nombre, rol...}           │ genera tokens
│             │  (sin tokens en el body)                 │
│             │                                 ┌────────▼────────┐
│             │    GET /api/v1/cualquier-ruta    │                 │
│             │ ─────────────────────────────> │  JwtAuthFilter  │
│             │  Cookie: accessToken=... (auto) │                 │
│             │                                 │ lee token de    │
│             │ <─────────────────────────────  │ cookie o header │
│             │       200 OK + datos            └─────────────────┘
└─────────────┘
```

---

## Ficheros modificados

### 1. `AuthResponse.java` — DTO de respuesta de autenticación

**Antes** incluía los campos de tokens:
```java
private String accessToken;
private String refreshToken;
private String tokenType;
private long expiresIn;
```

**Ahora** solo contiene los datos del usuario. Los tokens viajan en cookies:
```java
// Los tokens JWT viajan en cookies HttpOnly y NO se exponen en el body.
private long expiresIn;   // tiempo de vida del access token (ms)
private UUID usuarioId;
private String email;
private String nombre;
// ... resto de datos del usuario, rol y empresa
```

---

### 2. `CookieUtil.java` *(nuevo)* — `common/util/CookieUtil.java`

Clase de utilidad estática para crear y eliminar cookies de seguridad. Centraliza la configuración de los flags de seguridad.

```java
CookieUtil.addTokenCookie(response, nombre, valor, maxAgeSeg, secure);
CookieUtil.clearCookie(response, nombre, secure);
```

**Flags configurados en cada cookie:**

| Flag | Valor | Descripción |
|---|---|---|
| `HttpOnly` | `true` | **Inaccesible desde JavaScript** |
| `Secure` | `true` en prod / `false` en dev | Solo se envía por HTTPS |
| `SameSite` | `None` en prod / `Lax` en dev | `None` permite cross-site en HTTPS; `Lax` es compatible con HTTP local |
| `Path` | `/` | Válida para toda la aplicación |
| `MaxAge` | Configurable | Expiración automática |

---

### 3. `AuthController.java` — Controlador de autenticación

Se añade `HttpServletResponse` a los endpoints de autenticación para poder escribir las cookies. Se elimina el body de tokens.

#### Nuevo flujo de `login` y `register`

```java
public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                          HttpServletResponse response) {
    AuthResponse body = authService.login(request);
    writeAuthCookies(response, body.getEmail()); // ← escribe las dos cookies
    return ResponseEntity.ok(body);              // ← body sin tokens
}
```

#### Endpoint `/refresh-token` — lee de cookie, no del body

**Antes** recibía el refresh token en el body:
```json
{ "refreshToken": "eyJhbGciOi..." }
```

**Ahora** lo lee directamente de la cookie `refreshToken` que el navegador envía automáticamente. El endpoint no necesita ningún body.

#### Nuevo endpoint `POST /api/v1/auth/logout`

Invalida ambas cookies poniendo `maxAge=0`, lo que hace que el navegador las elimine inmediatamente:

```java
@PostMapping("/logout")
public ResponseEntity<Map<String, String>> logout(HttpServletResponse response) {
    CookieUtil.clearCookie(response, SecurityConstants.ACCESS_TOKEN_COOKIE, cookieSecure);
    CookieUtil.clearCookie(response, SecurityConstants.REFRESH_TOKEN_COOKIE, cookieSecure);
    return ResponseEntity.ok(Map.of("message", "Sesión cerrada correctamente"));
}
```

---

### 4. `AuthService.java` — Servicio de autenticación

- Los métodos `register`, `login` y `refreshToken` ya **no generan tokens** internamente — solo devuelven los datos del usuario.
- Nuevo método `generateTokenPair(email)` que el controller llama para obtener los tokens antes de escribir las cookies:

```java
public String[] generateTokenPair(String email) {
    // Devuelve [accessToken, refreshToken]
}
```

---

### 5. `JwtAuthenticationFilter.java` — Filtro de autenticación

El filtro ahora resuelve el JWT con **doble fuente**, en orden de prioridad:

```
1. Header Authorization: Bearer <token>   ← compatibilidad con clientes API (Postman, etc.)
2. Cookie HttpOnly "accessToken"          ← navegadores web
```

```java
private String resolveToken(HttpServletRequest request) {
    // 1. Header Authorization
    final String authHeader = request.getHeader(SecurityConstants.HEADER_STRING);
    if (authHeader != null && authHeader.startsWith(SecurityConstants.TOKEN_PREFIX)) {
        return authHeader.substring(SecurityConstants.TOKEN_PREFIX.length());
    }
    // 2. Cookie HttpOnly
    if (request.getCookies() != null) {
        return Arrays.stream(request.getCookies())
                .filter(c -> SecurityConstants.ACCESS_TOKEN_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
    return null;
}
```

---

### 6. `SecurityConstants.java` — Constantes de seguridad

Se añaden los nombres de las cookies como constantes para evitar strings mágicos:

```java
public static final String ACCESS_TOKEN_COOKIE  = "accessToken";
public static final String REFRESH_TOKEN_COOKIE = "refreshToken";
```

Swagger se mantiene en `PUBLIC_URLS` para que sea accesible sin autenticación durante el desarrollo.

---

### 7. `application.properties` — Configuración

```properties
# Cookies JWT
# En producción (HTTPS) cambiar a true
app.cookie.secure=false

# Swagger
springdoc.swagger-ui.disable-swagger-default-url=true
```

> ⚠️ **En producción:** cambiar `app.cookie.secure=true` para que las cookies solo viajen por HTTPS.

---

## Resumen de cookies emitidas

| Cookie | Contenido | MaxAge | Flags |
|---|---|---|---|
| `accessToken` | JWT de acceso | 1 hora (3600 seg) | `HttpOnly`, `Secure`*, `SameSite=None`* / `SameSite=Lax` |
| `refreshToken` | JWT de refresco | 7 días (604800 seg) | `HttpOnly`, `Secure`*, `SameSite=None`* / `SameSite=Lax` |

*\* Solo en producción con `app.cookie.secure=true`. En ese caso `SameSite=None` (permite peticiones cross-site sobre HTTPS). En desarrollo (`secure=false`) se usa `SameSite=Lax` para compatibilidad con HTTP.*

---

## Endpoints de autenticación

| Método | Endpoint | Body entrada | Cookies escritas | Body salida |
|---|---|---|---|---|
| `POST` | `/api/v1/auth/register` | `RegisterRequest` | ✅ `accessToken` + `refreshToken` | Datos del usuario |
| `POST` | `/api/v1/auth/login` | `LoginRequest` | ✅ `accessToken` + `refreshToken` | Datos del usuario |
| `POST` | `/api/v1/auth/refresh-token` | *(ninguno)* | ✅ `accessToken` + `refreshToken` nuevos | Datos del usuario |
| `POST` | `/api/v1/auth/logout` | *(ninguno)* | ✅ Ambas cookies con `maxAge=0` | `{ "message": "..." }` |

---

## Integración con el Frontend

Para que el navegador envíe y reciba las cookies automáticamente, **todas las peticiones deben incluir `credentials: 'include'`**:

### Fetch API
```javascript
const response = await fetch('http://localhost:8080/api/v1/auth/login', {
  method: 'POST',
  credentials: 'include',          // ← obligatorio para cookies
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ email, password })
});
```

### Axios (configuración global)
```javascript
import axios from 'axios';

axios.defaults.withCredentials = true;  // ← obligatorio para cookies
axios.defaults.baseURL = 'http://localhost:8080';
```

### Diferencias respecto al flujo anterior

| | **Antes** | **Ahora** |
|---|---|---|
| Token visible en DevTools | ✅ Sí (en Network/Response) | ❌ No |
| Token accesible con JS | ✅ Sí (`localStorage`, etc.) | ❌ No (`HttpOnly`) |
| Vulnerable a XSS | ✅ Sí | ❌ No |
| Frontend debe guardar el token | ✅ Sí | ❌ No (el navegador lo gestiona) |
| Necesita `credentials: include` | ❌ No | ✅ Sí |
| Compatible con Postman/Swagger | ✅ Sí (header) | ✅ Sí (header o cookie) |

---

## Consideraciones de seguridad adicionales

- **CSRF**: En desarrollo (`SameSite=Lax`) las cookies no se envían en peticiones cross-site iniciadas automáticamente (p. ej. `<img>` o `<form>`), lo que mitiga la mayoría de ataques CSRF. En producción (`SameSite=None; Secure`) se confía en el origen controlado por CORS (`allowCredentials=true` + `allowedOrigins` explícitos). Si se requiere máxima protección CSRF en producción, considera añadir un CSRF token.
- **HTTPS en producción**: El flag `Secure` garantiza que las cookies nunca viajen en texto plano. Activar con `app.cookie.secure=true`.
- **Expiración automática**: Si el servidor se reinicia o el usuario no actúa en 7 días, el `refreshToken` expira y el usuario debe volver a autenticarse.

