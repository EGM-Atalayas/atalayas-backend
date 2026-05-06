# Seguridad JWT — Expiración del Access Token y Renovación

> **Última actualización:** Mayo 2026
> **Afecta a:** `security/JwtAuthenticationFilter`, `security/SecurityConstants`, frontend (`lib/api/`)

---

## 1. Problema detectado

**Síntoma:** Al cambiar el estado de una incidencia desde la página de gestión, el cambio visual revertía al valor anterior tras cerrar sesión y volver a entrar.

**Causa raíz:** El `accessToken` tiene una duración de **1 hora** (`SecurityConstants.ACCESS_TOKEN_EXPIRATION`).  
Cuando el usuario permanecía en la página más de 1 hora sin recargar, el token expiraba. Las siguientes peticiones `PATCH` al backend fallaban con **401** porque el filtro JWT no establecía autenticación.  
El frontend recibía el error como non-ok, no persistía el cambio y revertía el estado visualmente.

---

## 2. Solución implementada (Mayo 2026)

### `JwtAuthenticationFilter`

El filtro anterior swallowed las excepciones de token y continuaba la cadena de filtros sin autenticación:

```java
// ANTES — problemático
} catch (Exception e) {
    log.error("No se pudo autenticar el token JWT: {}", e.getMessage());
}
filterChain.doFilter(request, response); // continuaba sin auth → 401 indeterminado
```

El filtro ahora distingue tres casos:

| Caso | Comportamiento anterior | Comportamiento actual |
|---|---|---|
| Sin token | Continúa sin auth | ✅ Igual — Spring Security gestiona según el endpoint |
| Token inválido / expirado | Continúa sin auth → 401 indeterminado | ✅ **401 inmediato** con JSON claro antes de llegar al controlador |
| Token malformado | Continúa sin auth → 401 indeterminado | ✅ **401 inmediato** con JSON claro |

```java
// DESPUÉS — respuesta 401 explícita cuando el token existe pero falla
} else {
    log.warn("Token JWT expirado o inválido para usuario: {}", userEmail);
    sendUnauthorized(response, "Token expirado o inválido. Usa /api/v1/auth/refresh-token para renovar la sesión.");
    return; // corta la cadena de filtros
}
```

**Respuesta 401 estructurada:**
```json
{
  "status": 401,
  "error": "No autorizado",
  "message": "Token expirado o inválido. Usa /api/v1/auth/refresh-token para renovar la sesión."
}
```

---

## 3. Mejora futura recomendada — Aumentar duración del access token

La solución del punto 2 hace que el frontend reciba un 401 claro. Sin embargo, todavía interrumpe la sesión del usuario al cabo de 1 hora si no hay un interceptor de refresh automático.

### Opción A (Backend) — Aumentar `ACCESS_TOKEN_EXPIRATION`

**Archivo:** `security/SecurityConstants.java`

```java
// Actual
public static final long ACCESS_TOKEN_EXPIRATION = 1000L * 60 * 60;  // 1 hora

// Recomendado para uso en jornada laboral sin interrupciones
public static final long ACCESS_TOKEN_EXPIRATION = 1000L * 60 * 60 * 8;  // 8 horas
```

**Impacto en seguridad:**
- Un token robado tiene una ventana de uso más larga (8h vs 1h).
- Mitigado por las cookies `HttpOnly` + `Secure` + `SameSite`, que impiden el acceso desde JavaScript y ataques CSRF.
- El `refreshToken` de 7 días ya tiene una ventana mucho mayor; aumentar el `accessToken` a 8h no cambia significativamente la superficie de ataque real.

> ⚠️ **No aplicar** si el sistema requiere revocación rápida de sesiones activas (ej. tras cambio de rol o desactivación de cuenta). En ese caso, implementar la Opción B.

### Opción B (Frontend) — Interceptor de refresh automático

Implementar un interceptor en el cliente que, al recibir un `401` en cualquier petición autenticada, intente renovar el token automáticamente antes de reintentar la petición original:

```typescript
// lib/api/client.ts — ejemplo con axios
api.interceptors.response.use(
    response => response,
    async error => {
        const original = error.config;
        if (error.response?.status === 401 && !original._retry) {
            original._retry = true;
            try {
                await api.post('/auth/refresh-token'); // usa la cookie refreshToken automáticamente
                return api(original);                  // reintenta la petición original
            } catch {
                // refreshToken también expirado → redirigir al login
                window.location.href = '/login';
            }
        }
        return Promise.reject(error);
    }
);
```

Esta opción es **transparente para el usuario**: la sesión se renueva automáticamente sin interrupciones, siempre que el `refreshToken` (7 días) siga siendo válido.

---

## 4. Recomendación final

| Prioridad | Acción | Dónde |
|-----------|--------|-------|
| ✅ Implementado | `JwtAuthenticationFilter` con 401 explícito | Backend |
| 🔜 Recomendado | Interceptor de refresh automático en 401 | **Frontend** |
| ⚙️ Opcional | Aumentar `ACCESS_TOKEN_EXPIRATION` a 8h | Backend — `SecurityConstants.java` |

La combinación **401 explícito (backend)** + **interceptor de refresh (frontend)** es la solución completa y robusta. Aumentar la duración del token es una alternativa rápida si no se implementa el interceptor frontend a corto plazo.

---

## 5. Referencias

- [`JwtAuthenticationFilter.java`](../src/main/java/com/atalayas/backend/security/JwtAuthenticationFilter.java)
- [`SecurityConstants.java`](../src/main/java/com/atalayas/backend/security/SecurityConstants.java)
- [`seguridad-cookies-jwt.md`](./seguridad-cookies-jwt.md) — sección 5 (filtro) y sección 8.4 (expiración)
- [`api-endpoints.md`](./api-endpoints.md) — sección 1 (auth / refresh-token)

