# Configuración y Despliegue — EGM Atalayas Backend

> **Audiencia:** DevOps, desarrolladores backend
> **Última actualización:** Abril 2026 (rev. 2)

---

## Índice

1. [Variables de entorno](#1-variables-de-entorno)
2. [Configuración por entorno (dev vs prod)](#2-configuración-por-entorno-dev-vs-prod)
3. [Arranque en local con Docker](#3-arranque-en-local-con-docker)
4. [Despliegue en Railway](#4-despliegue-en-railway)
5. [Pool de conexiones (HikariCP)](#5-pool-de-conexiones-hikaricp)
6. [Configuración de correo SMTP](#6-configuración-de-correo-smtp)
7. [Configuración de IA (Gemini, Groq, ElevenLabs, Supabase)](#7-configuración-de-ia-gemini-groq-elevenlabs-supabase)

---

## 1. Variables de entorno

Todas las variables tienen un valor por defecto para desarrollo local (indicado en la columna "Default dev").
En producción **deben configurarse explícitamente**.

### Base de datos

| Variable | Default dev | Prod (obligatorio) | Descripción |
|---|---|---|---|
| `DB_URL` | — | ✅ | URL JDBC de PostgreSQL. Ej: `jdbc:postgresql://host/db?sslmode=require` |
| `DB_USER` | — | ✅ | Usuario de la base de datos |
| `DB_PASSWORD` | — | ✅ | Contraseña de la base de datos |

> El esquema de BD **no se modifica automáticamente** en arranque (`ddl-auto=none`). Cualquier cambio de esquema debe aplicarse manualmente ejecutando los scripts de `docs/sql/`.

### Seguridad JWT

| Variable | Default dev | Prod (obligatorio) | Descripción |
|---|---|---|---|
| `JWT_SECRET` | `atalayas-local-dev-secret-key-must-be-at-least-32-characters-long` | ✅ Aleatorio ≥ 32 chars | Clave HMAC-SHA256 para firmar los JWT |
| `COOKIE_SECURE` | `false` | `true` | Activa flags `Secure`, `SameSite=None` y `Partitioned` en las cookies |

### CORS

| Variable | Default dev | Prod (obligatorio) | Descripción |
|---|---|---|---|
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173,http://localhost:4200` | ✅ URL del frontend | Lista de orígenes separados por coma |

### Servidor

| Variable | Default dev | Prod | Descripción |
|---|---|---|---|
| `PORT` | `8080` | Asignado por Railway | Puerto en el que escucha la aplicación |
| `SERVER_URL` | `http://localhost:8080` | URL del backend en Railway | URL pública usada por Swagger para el "Try it out" |

### Correo electrónico (SMTP)

| Variable | Default dev | Prod (obligatorio) | Descripción |
|---|---|---|---|
| `MAIL_HOST` | `smtp.gmail.com` | — | Servidor SMTP |
| `MAIL_PORT` | `587` | — | Puerto SMTP (587 para STARTTLS) |
| `MAIL_USER` | — | ✅ | Usuario/email de la cuenta remitente |
| `MAIL_PASSWORD` | — | ✅ | Contraseña o App Password de la cuenta |
| `FRONTEND_URL` | `http://localhost:3000` | ✅ URL del frontend | Base URL usada en los enlaces de emails (ej. link de recuperación de contraseña) |

### Subida de archivos (avatares)

| Variable | Default dev | Prod | Descripción |
|---|---|---|---|
| `UPLOAD_DIR` | `uploads` (relativo al directorio de ejecución) | Ruta a volumen persistente | Directorio donde se almacenan los avatares procesados |

> En Railway, apuntar `UPLOAD_DIR` a un volumen persistente para que los avatares no se pierdan en cada redeployment.

### Google Gemini AI

| Variable | Default dev | Prod | Descripción |
|---|---|---|---|
| `GEMINI_API_KEY` | `""` (vacío — IA desactivada) | Recomendado | API Key de Google AI Studio. Usada en `/generar-contenido`, `/generar-preguntas`, `/chat`, `/resumir` |

### Groq AI

| Variable | Default dev | Prod | Descripción |
|---|---|---|---|
| `GROQ_API_KEY` | `""` (vacío) | Recomendado | API Key de Groq. Usada en `/generar-desde-archivo` para documentación y slides |

### ElevenLabs TTS

| Variable | Default dev | Prod | Descripción |
|---|---|---|---|
| `ELEVENLABS_API_KEY` | `""` (vacío) | Opcional | API Key de ElevenLabs. Necesaria para generar el audio MP3 del podcast |
| `ELEVENLABS_VOICE_ID` | `JBFqnCBsd6RMkjVDRZzb` (voz "George") | Opcional | ID de la voz ElevenLabs |
| `ELEVENLABS_MODEL_ID` | `eleven_multilingual_v2` | Opcional | Modelo TTS de ElevenLabs |

> Si `ELEVENLABS_API_KEY` está vacío, el podcast se genera sin audio (`podcastAudioUrl = null`) pero el resto de `/generar-desde-archivo` es válido.

### Supabase Storage

| Variable | Default dev | Prod | Descripción |
|---|---|---|---|
| `SUPABASE_URL` | `""` (vacío) | Obligatorio si se usa podcast | URL del proyecto Supabase. Ej: `https://xxxxx.supabase.co` |
| `SUPABASE_SERVICE_KEY` | `""` (vacío) | Obligatorio si se usa podcast | Service role key de Supabase (escritura en Storage) |

> Los audios MP3 se suben al bucket `modulos`, ruta `podcast/{uuid}.mp3`. URL pública: `{SUPABASE_URL}/storage/v1/object/public/modulos/podcast/{uuid}.mp3`

---

## 2. Configuración por entorno (dev vs prod)

| Comportamiento | Dev (`COOKIE_SECURE=false`) | Prod (`COOKIE_SECURE=true`) |
|---|---|---|
| Protocolo | HTTP | HTTPS |
| Cookie `Secure` | ❌ | ✅ |
| Cookie `SameSite` | `Lax` | `None` |
| Cookie `Partitioned` (CHIPS) | ❌ | ✅ |
| Cross-site cookies | ❌ | ✅ |
| Swagger accesible sin login | ❌ (requiere autenticación) | ❌ (requiere autenticación) |

Ver [seguridad-cookies-jwt.md](seguridad-cookies-jwt.md) para más detalles sobre las cookies.

---

## 3. Arranque en local con Docker

El fichero `compose.yaml` del proyecto levanta un contenedor PostgreSQL:

```yaml
# compose.yaml
services:
  postgres:
    image: 'postgres:latest'
    environment:
      - POSTGRES_DB=mydatabase
      - POSTGRES_PASSWORD=secret
      - POSTGRES_USER=myuser
    ports:
      - '5432'
```

### Pasos

```bash
# 1. Levantar PostgreSQL
docker compose up -d

# 2. Variables mínimas para arrancar
export DB_URL=jdbc:postgresql://localhost:5432/mydatabase
export DB_USER=myuser
export DB_PASSWORD=secret
export MAIL_USER=tucorreo@gmail.com
export MAIL_PASSWORD=app_password_gmail

# 3. (Opcional) IA — añadir si se quieren probar los endpoints de inteligencia artificial
export GEMINI_API_KEY=tu_gemini_key
export GROQ_API_KEY=tu_groq_key

# 4. Arrancar la aplicación
./mvnw spring-boot:run
```

> **Nota:** Spring Boot detecta automáticamente el `compose.yaml` y puede levantar el
> contenedor por ti si usas `spring-boot:run` con Docker activo.

---

## 4. Despliegue en Railway

### Variables que hay que configurar en Railway

```
# ── Base de datos ──────────────────────────────────────────────────────────
DB_URL               = jdbc:postgresql://<host>/<db>?sslmode=require
DB_USER              = <usuario neon.tech>
DB_PASSWORD          = <contraseña neon.tech>

# ── Seguridad ──────────────────────────────────────────────────────────────
JWT_SECRET           = <cadena aleatoria >= 32 caracteres>
COOKIE_SECURE        = true

# ── URLs ───────────────────────────────────────────────────────────────────
CORS_ALLOWED_ORIGINS = https://tu-frontend.up.railway.app
SERVER_URL           = https://atalayas-backend-production-4777.up.railway.app
FRONTEND_URL         = https://tu-frontend.up.railway.app

# ── Correo ─────────────────────────────────────────────────────────────────
MAIL_USER            = cuenta@gmail.com
MAIL_PASSWORD        = <app password gmail>

# ── IA: Gemini (contenido, preguntas, chat, resumen) ──────────────────────
GEMINI_API_KEY       = <key de Google AI Studio>

# ── IA: Groq (generación desde archivo — documentación y slides) ───────────
GROQ_API_KEY         = <key de Groq>

# ── IA: ElevenLabs (audio podcast) — opcional ─────────────────────────────
ELEVENLABS_API_KEY   = <key de ElevenLabs>
ELEVENLABS_VOICE_ID  = JBFqnCBsd6RMkjVDRZzb   # voz George (español/inglés)
ELEVENLABS_MODEL_ID  = eleven_multilingual_v2

# ── Storage podcast: Supabase — obligatorio si se usa podcast ──────────────
SUPABASE_URL         = https://xxxxx.supabase.co
SUPABASE_SERVICE_KEY = <service role key>

# ── Archivos (avatares) ────────────────────────────────────────────────────
UPLOAD_DIR           = /app/uploads   # montar un volumen persistente en Railway
```

### Dockerfile

El proyecto incluye un `Dockerfile` en la raíz. Railway lo usa automáticamente para construir la imagen:

```
./mvnw clean package -DskipTests   → construye el JAR
java -jar target/*.jar              → arranca la aplicación
```

### Base de datos en producción

Se usa **Neon.tech** (PostgreSQL gestionado con conexión serverless). La URL de conexión lleva `?sslmode=require`. El pool de HikariCP está configurado para tolerar reconexiones del servidor serverless de Neon.

---

## 5. Pool de conexiones (HikariCP)

Configuración optimizada para Neon.tech (BD serverless que puede pausar conexiones):

| Parámetro | Valor | Descripción |
|---|---|---|
| `maximum-pool-size` | `30` | Máximo de conexiones simultáneas |
| `minimum-idle` | `2` | Mínimo de conexiones idle mantenidas |
| `connection-timeout` | `30 000 ms` | Tiempo máximo para obtener conexión del pool |
| `idle-timeout` | `600 000 ms` | Cierra conexiones idle después de 10 min |
| `max-lifetime` | `1 800 000 ms` | Duración máxima de una conexión (30 min) |
| `keepalive-time` | `30 000 ms` | Ping de keepalive cada 30 s |
| `connection-test-query` | `SELECT 1` | Query de validación de conexión |
| `validation-timeout` | `5 000 ms` | Tiempo límite para validar la conexión |
| `leak-detection-threshold` | `30 000 ms` | Alerta si una conexión se mantiene abierta más de 30 s (umbral conservador para operaciones con `REQUIRES_NEW` + email + llamadas a APIs de IA) |

---

## 6. Configuración de correo SMTP

Se usa Gmail con STARTTLS. Para generar un `MAIL_PASSWORD` seguro:

1. Ir a [myaccount.google.com](https://myaccount.google.com) → Seguridad → Verificación en dos pasos
2. Al final de la página: **Contraseñas de aplicaciones**
3. Crear una contraseña para "Correo" y copiarla como `MAIL_PASSWORD`

Los correos se envían en estos eventos:

| Evento | Destinatario |
|---|---|
| Empresa aprobada | Admin de empresa — correo de bienvenida |
| Empresa rechazada | Admin de empresa — correo de rechazo |
| Usuario creado por admin | Nuevo usuario — correo de bienvenida |
| Solicitud de recuperación de contraseña | Usuario — enlace con token de reset |

La URL base de los enlaces en los emails se toma de `FRONTEND_URL`.

---

## 7. Configuración de IA (Gemini, Groq, ElevenLabs, Supabase)

Todas las integraciones de IA usan llamadas HTTP directas (sin SDK de Java).

### Google Gemini — contenido, preguntas, chat, resumen

| Parámetro | Valor |
|---|---|
| Variable env | `GEMINI_API_KEY` |
| URL | `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent` |
| Modelo | `gemini-2.0-flash` |
| Tokens máximos | `8192` |
| Temperatura | `0.7` |
| Timeout HTTP | `120 s` |
| Auth | Query param `?key={GEMINI_API_KEY}` |

> Gemini no tiene rol `system` separado — el `GeminiClient` concatena system prompt + user prompt en un único texto de entrada.

Para obtener una API Key: [aistudio.google.com](https://aistudio.google.com)

### Groq — generación desde archivo (documentación y slides)

| Parámetro | Valor |
|---|---|
| Variable env | `GROQ_API_KEY` |
| URL | `https://api.groq.com/openai/v1/chat/completions` |
| Modelo | `llama-3.3-70b-versatile` |
| Tokens máximos | `8192` |
| Temperatura | `0.7` |
| Timeout HTTP | `120 s` |
| Auth | `Authorization: Bearer {GROQ_API_KEY}` |
| Formato | Compatible con OpenAI (`messages[].role` con `system` y `user`) |

Para obtener una API Key: [console.groq.com](https://console.groq.com)

### ElevenLabs — audio de podcast (TTS)

| Parámetro | Valor |
|---|---|
| Variable env | `ELEVENLABS_API_KEY` |
| URL | `https://api.elevenlabs.io/v1/text-to-speech/{voiceId}` |
| Voz por defecto | `JBFqnCBsd6RMkjVDRZzb` (George — multilingual) |
| Modelo por defecto | `eleven_multilingual_v2` |
| Límite de texto | `5 000 caracteres` (tier gratuito de ElevenLabs) |
| Auth | Header `xi-api-key: {ELEVENLABS_API_KEY}` |
| Respuesta | `audio/mpeg` — bytes del archivo MP3 |

> Si la generación de audio falla, el endpoint `/generar-desde-archivo` **no lanza error** — devuelve la respuesta completa con `podcastAudioUrl = null`.

Para obtener una API Key: [elevenlabs.io](https://elevenlabs.io)

### Supabase Storage — almacenamiento de MP3

| Parámetro | Valor |
|---|---|
| Variables env | `SUPABASE_URL`, `SUPABASE_SERVICE_KEY` |
| Bucket | `modulos` (debe existir con lectura pública activada) |
| Ruta de archivos | `podcast/{uuid}.mp3` |
| URL pública resultado | `{SUPABASE_URL}/storage/v1/object/public/modulos/podcast/{uuid}.mp3` |
| Auth | `Authorization: Bearer {SUPABASE_SERVICE_KEY}` + `x-upsert: true` |

> El bucket `modulos` debe crearse en Supabase con acceso público de lectura antes de usar podcast. El UUID del archivo es generado aleatoriamente en cada petición; el frontend lo asocia al módulo después.
