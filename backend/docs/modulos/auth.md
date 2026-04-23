# Módulo: Autenticación (`auth`)

> **Paquete:** `com.atalayas.backend.auth`
> **Audiencia:** Frontend, Backend
> **Última actualización:** Abril 2026

---

## ¿Qué hace este módulo?

Gestiona todo el ciclo de vida de la sesión de usuario:
- **Login** y **registro** (genera tokens JWT en cookies HttpOnly)
- **Refresh** de tokens sin necesidad de volver a autenticarse
- **Logout** (invalida las cookies)
- **Restauración de sesión** al recargar la página (`GET /auth/me`)

La implementación completa de JWT y cookies está documentada en [seguridad-cookies-jwt.md](../seguridad-cookies-jwt.md).

---

## Endpoints

Base URL: `/api/v1/auth`

| Método | Ruta | Auth | Descripción |
|---|---|:---:|---|
| `POST` | `/auth/register` | ❌ | Registrar nuevo usuario |
| `POST` | `/auth/login` | ❌ | Iniciar sesión |
| `POST` | `/auth/refresh-token` | ❌ ¹ | Renovar el access token |
| `POST` | `/auth/logout` | ❌ | Cerrar sesión (limpia cookies) |
| `GET`  | `/auth/me` | ✅ | Datos del usuario autenticado |
| `POST` | `/auth/forgot-password` | ❌ | Solicitar recuperación de contraseña por email |
| `POST` | `/auth/reset-password` | ❌ | Restablecer contraseña con token recibido por email |

¹ Requiere la cookie `refreshToken` válida en la petición.

---

## Flujo de login

```
Frontend                         Backend
   │                                │
   │  POST /auth/login              │
   │  { email, password }           │
   │ ─────────────────────────────► │
   │                                │  1. AuthenticationManager valida credenciales
   │                                │  2. Genera accessToken (1h) + refreshToken (7d)
   │                                │  3. Escribe ambas cookies HttpOnly
   │                                │  4. Devuelve accessToken también en el body
   │ ◄───────────────────────────── │
   │  Set-Cookie: accessToken=...   │
   │  Set-Cookie: refreshToken=...  │
   │  Body: { accessToken, usuarioId,│
   │    email, nombre, codigoRol,   │
   │    empresaId, ... }            │
```

## Flujo de restauración de sesión (recarga de página)

```
App arranca o página se recarga
   │
   ├─► GET /auth/me  (credentials: 'include')
   │
   │   ├── 200 OK ──► Sesión activa → restaurar estado con los datos recibidos
   │   │
   │   └── 401 ──► accessToken expirado
   │               │
   │               └─► POST /auth/refresh-token  (credentials: 'include')
   │                   │
   │                   ├── 200 OK ──► Nuevos tokens → restaurar estado
   │                   │
   │                   └── 401 ──► refreshToken expirado → redirigir al /login
```

---

## Request / Response

### POST `/auth/login`

**Request body:**
```json
{
  "email": "usuario@empresa.com",
  "password": "miPassword123"
}
```

**Response `200 OK`:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "usuarioId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "usuario@empresa.com",
  "nombre": "María",
  "apellidos": "García López",
  "codigoRol": "ROLE_EMPLEADO",
  "empresaId": "660e8400-e29b-41d4-a716-446655440001",
  "avatarUrl": null
}
```

### POST `/auth/register`

**Request body:**
```json
{
  "email": "nuevo@empresa.com",
  "password": "miPassword123",
  "nombre": "Juan",
  "apellidos": "Pérez",
  "empresaId": "660e8400-e29b-41d4-a716-446655440001",
  "terminosAceptados": true
}
```

**Response `201 Created`:** mismo body que login.

### GET `/auth/me`

No requiere body. Usa la cookie `accessToken` automáticamente.

**Response `200 OK`:** mismo body que login (incluye `accessToken` activo en el body).

### POST `/auth/refresh-token`

No requiere body. Usa la cookie `refreshToken` automáticamente.

**Response `200 OK`:** mismo body que login con nuevos tokens.

### POST `/auth/logout`

No requiere body.

**Response `200 OK`:**
```json
{ "message": "Sesión cerrada correctamente" }
```
Las cookies quedan invalidadas (`Max-Age=0`).

### POST `/auth/forgot-password`

Inicia el flujo de recuperación de contraseña. Envía un email con un enlace de restablecimiento al usuario.

> **Seguridad:** para no revelar si el email está registrado en la plataforma, **siempre devuelve `200 OK`** independientemente de si el email existe o no.

**Request body:**
```json
{
  "email": "usuario@empresa.com"
}
```

**Response `200 OK`:**
```json
{ "message": "Si el correo está registrado, recibirás un enlace en breve" }
```

El token de recuperación se almacena en la tabla `password_reset_token` y expira pasado un tiempo determinado. Solo puede usarse una vez (`usado = true` tras el primer uso).

### POST `/auth/reset-password`

Restablece la contraseña del usuario usando el token recibido por email.

**Request body:**
```json
{
  "token": "a1b2c3d4-...",
  "nuevaPassword": "NuevaPassword123!"
}
```

**Response `200 OK`:**
```json
{ "message": "Contraseña restablecida correctamente" }
```

**Errores:**

| Código | Causa |
|---|---|
| `400` | Token inválido, expirado o ya utilizado |

---

## Errores comunes

| Código | Causa |
|---|---|
| `401` | Credenciales incorrectas, token inválido o expirado |
| `400` | refreshToken no presente o expirado al llamar a `/refresh-token` |
| `403` | Cuenta bloqueada por 5 intentos fallidos consecutivos |

---

## Integración en el frontend

```javascript
// Configuración base (siempre incluir credentials)
const api = axios.create({
  baseURL: 'https://api.ejemplo.com',
  withCredentials: true,  // ← obligatorio para cookies
});

// Login
const { data } = await api.post('/api/v1/auth/login', { email, password });
// data.accessToken disponible como fallback Bearer si las cookies son bloqueadas

// Al arrancar la app
async function initApp() {
  try {
    const { data } = await api.get('/api/v1/auth/me');
    store.setUser(data);
  } catch {
    try {
      const { data } = await api.post('/api/v1/auth/refresh-token');
      store.setUser(data);
    } catch {
      router.push('/login');
    }
  }
}
```

