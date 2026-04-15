# Configuración y Despliegue — EGM Atalayas Backend

> **Audiencia:** DevOps, desarrolladores backend
> **Última actualización:** Abril 2026

---

## Índice

1. [Variables de entorno](#1-variables-de-entorno)
2. [Configuración por entorno (dev vs prod)](#2-configuración-por-entorno-dev-vs-prod)
3. [Arranque en local con Docker](#3-arranque-en-local-con-docker)
4. [Despliegue en Railway](#4-despliegue-en-railway)
5. [Pool de conexiones (HikariCP)](#5-pool-de-conexiones-hikaricp)
6. [Configuración de correo SMTP](#6-configuración-de-correo-smtp)
7. [Configuración de Gemini AI](#7-configuración-de-gemini-ai)

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

> La BD se crea/actualiza automáticamente al arrancar (`spring.jpa.hibernate.ddl-auto=update`).

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

### Google Gemini AI

| Variable | Default dev | Prod | Descripción |
|---|---|---|---|
| `GEMINI_API_KEY` | `""` (vacío — IA desactivada) | Recomendado | API Key de Google AI Studio |

> Si `GEMINI_API_KEY` está vacío, los endpoints de IA devuelven error. El resto de la aplicación funciona con normalidad.

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

# 2. Configurar variables de entorno para que apunten al contenedor
export DB_URL=jdbc:postgresql://localhost:5432/mydatabase
export DB_USER=myuser
export DB_PASSWORD=secret
export MAIL_USER=tucorreo@gmail.com
export MAIL_PASSWORD=app_password_gmail

# 3. Arrancar la aplicación
./mvnw spring-boot:run
```

> **Nota:** Spring Boot detecta automáticamente el `compose.yaml` y puede levantar el
> contenedor por ti si usas `spring-boot:run` con Docker activo.

---

## 4. Despliegue en Railway

### Variables que hay que configurar en Railway

```
DB_URL          = jdbc:postgresql://<host>/<db>?sslmode=require
DB_USER         = <usuario neon.tech>
DB_PASSWORD     = <contraseña neon.tech>
JWT_SECRET      = <cadena aleatoria >= 32 caracteres>
COOKIE_SECURE   = true
CORS_ALLOWED_ORIGINS = https://tu-frontend.up.railway.app
SERVER_URL      = https://atalayas-backend-production-4777.up.railway.app
MAIL_USER       = cuenta@gmail.com
MAIL_PASSWORD   = <app password gmail>
GEMINI_API_KEY  = <key de Google AI Studio>
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
| `leak-detection-threshold` | `2 000 ms` | Alerta si una conexión se mantiene abierta más de 2 s |

---

## 6. Configuración de correo SMTP

Se usa Gmail con STARTTLS. Para generar un `MAIL_PASSWORD` seguro:

1. Ir a [myaccount.google.com](https://myaccount.google.com) → Seguridad → Verificación en dos pasos
2. Al final de la página: **Contraseñas de aplicaciones**
3. Crear una contraseña para "Correo" y copiarla como `MAIL_PASSWORD`

Los correos se envían en estos eventos:
- Empresa aprobada → correo de bienvenida al admin de empresa
- Empresa rechazada → correo de rechazo al admin de empresa

---

## 7. Configuración de Gemini AI

La integración usa la API REST de Google Gemini directamente (sin SDK de Java):

| Parámetro | Valor en `application.properties` |
|---|---|
| URL base | `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent` |
| Modelo | `gemini-1.5-flash` |
| Tokens máximos | `2000` |

> **Nota:** El `application.properties` referencia `gemini-1.5-flash` aunque el nombre de
> la integración en el código mencione "Gemini 2.0 Flash". Verifica la variable `gemini.model`
> si necesitas cambiar el modelo.

Para obtener una API Key:
1. Ir a [aistudio.google.com](https://aistudio.google.com)
2. Crear un proyecto y generar una API Key
3. Asignarla a `GEMINI_API_KEY` en Railway

