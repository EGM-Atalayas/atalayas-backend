# Módulo: Inteligencia Artificial (`ai`)

> **Paquete:** `com.atalayas.backend.ai`
> **Audiencia:** Frontend, Backend
> **Última actualización:** Abril 2026 (rev. 2)

---

## ¿Qué hace este módulo?

Integra la plataforma con **Google Gemini** y **Groq** para ofrecer funcionalidades de IA a los administradores y empleados:

| Función | Quién la usa | Descripción |
|---|---|---|
| Generar contenido formativo | Admin empresa | Genera artículos y material de formación dado un tema |
| Generar preguntas de evaluación | Admin empresa | Genera preguntas de test para un contenido |
| Chatbot | Todos los usuarios | Asistente conversacional contextualizado con la empresa |
| Resumir texto | Admin empresa | Resume documentos largos o textos de formación |
| Generar desde archivo | Admin empresa | Sube un PDF/DOCX/TXT y genera documentación, podcast y/o vídeo |

---

## Arquitectura de la integración

```
Controller (AiController)
       │
       ▼
Service layer
  ├── AiContentService    → genera contenido formativo y preguntas (Gemini)
  ├── AiChatService       → chatbot empleados (Gemini)
  ├── AiSummaryService    → resumen de texto (Gemini)
  └── AiFileService       → extracción de texto de PDF/DOCX/TXT
       │
       ▼
AI Clients (client/)
  ├── GeminiClient        → Llamada HTTP a Gemini 2.0 Flash
  │   URL: https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent
  │   Auth: ?key={GEMINI_API_KEY}
  ├── GroqClient          → Llamada HTTP a Groq (llama-3.3-70b-versatile)
  │   Usado en generar-desde-archivo para texto y slides
  └── ElevenLabsClient    → Text-to-Speech para generación de podcasts
       │
       ▼
SupabaseStorageService    → Sube el audio MP3 del podcast a Supabase Storage
```

La comunicación con cada proveedor se hace mediante llamadas HTTP directas (sin SDK de Java).

---

## Variables de entorno requeridas

| Variable | Proveedor | Descripción |
|---|---|---|
| `GEMINI_API_KEY` | Google Gemini | Generación de contenido, preguntas, chat y resumen |
| `GROQ_API_KEY` | Groq | Generación desde archivo (doc, slides) |
| `ELEVENLABS_API_KEY` | ElevenLabs | Text-to-Speech para el guion de podcast |
| `SUPABASE_URL` | Supabase | URL del proyecto Supabase |
| `SUPABASE_KEY` | Supabase | Service role key para acceso al Storage |

---

## Endpoints

Base URL: `/api/v1/ai`

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| `POST` | `/ai/generar-contenido` | `ADMIN_EMPRESA` | Genera contenido formativo con Gemini |
| `POST` | `/ai/generar-preguntas` | `ADMIN_EMPRESA` | Genera preguntas de evaluación con Gemini |
| `POST` | `/ai/chat` | Cualquiera | Chatbot para empleados |
| `POST` | `/ai/resumir` | `ADMIN_EMPRESA` | Genera resumen de un texto |
| `POST` | `/ai/generar-desde-archivo` | `ADMIN_EMPRESA` | Genera contenido a partir de un archivo PDF/DOCX/TXT |

> **Nota:** Si alguna clave de API no está configurada, el endpoint afectado devuelve `500 Internal Server Error` con mensaje descriptivo.

---

## Request / Response

### POST `/ai/generar-contenido` — Generar contenido formativo

**Request body (`AiPromptRequest`):**
```json
{
  "tema": "Políticas de seguridad informática",
  "descripcion": "Dirigido a empleados sin formación técnica previa",
  "tipoModulo": "ESPECIALIZADO"
}
```

**Response `200 OK` (`AiResponse`):**
```json
{
  "contenido": "# Políticas de Seguridad Informática\n\n## Introducción\n\n...",
  "modelo": "gemini-2.0-flash",
  "generadoEn": "2026-04-23T10:00:00Z"
}
```

### POST `/ai/generar-preguntas` — Generar preguntas de evaluación

**Request body (`AiPromptRequest`):**
```json
{
  "prompt": "Genera preguntas sobre las políticas de seguridad informática",
  "numPreguntas": 5
}
```
> `numPreguntas` es opcional. Por defecto: `5`.

**Response `200 OK` (`AiResponse`):**
```json
{
  "contenido": "[{\"enunciado\": \"¿Cuál es la longitud mínima recomendada para una contraseña?\", \"opciones\": [...], \"respuestaCorrecta\": \"12 caracteres\"}]",
  "modelo": "gemini-2.0-flash",
  "generadoEn": "2026-04-23T10:00:00Z"
}
```

### POST `/ai/chat` — Chatbot para empleados

**Request body (`AiPromptRequest`):**
```json
{
  "prompt": "¿Cuántos días de vacaciones tengo?",
  "nombreEmpresa": "Tecnología SL",
  "contexto": "Política de vacaciones: 22 días laborables al año."
}
```

**Response `200 OK` (`AiResponse`):**
```json
{
  "contenido": "Según la política de Tecnología SL, tienes 22 días de vacaciones anuales...",
  "modelo": "gemini-2.0-flash",
  "generadoEn": "2026-04-23T10:00:00Z"
}
```

### POST `/ai/resumir` — Resumir texto

**Request body (`AiPromptRequest`):**
```json
{
  "prompt": "Texto largo del documento a resumir..."
}
```

**Response `200 OK` (`AiResponse`):**
```json
{
  "contenido": "## Resumen\n\n**Puntos clave:**\n- Punto 1...",
  "modelo": "gemini-2.0-flash",
  "generadoEn": "2026-04-23T10:00:00Z"
}
```

### POST `/ai/generar-desde-archivo` — Generar desde archivo

Sube un archivo y genera una o varias salidas combinables.

**Content-Type:** `multipart/form-data`

| Parte/Parámetro | Tipo | Descripción |
|---|---|---|
| `archivo` | file (part) | PDF, DOCX o TXT. El texto se limita a las primeras 12 000 chars para no exceder el contexto de Groq |
| `tiposSalida` | query param | Combinación de valores separados por coma. Por defecto: `documentacion` |

**Valores de `tiposSalida`:**

| Valor | Descripción |
|---|---|
| `documentacion` | Genera contenido formativo en Markdown |
| `podcast` | Genera guion de podcast y audio MP3 con ElevenLabs (subido a Supabase Storage) |
| `video` | Genera array JSON de slides para presentación |

Se pueden combinar: `documentacion,podcast`, `documentacion,video`, `documentacion,podcast,video`.

**Response `200 OK` (`AiFileResponse`):**
```json
{
  "titulo": "Políticas de Seguridad Informática",
  "descripcion": "Introducción a las principales políticas de seguridad para empleados.",
  "contenido": "# Políticas de Seguridad...\n\n## Introducción...",
  "scriptPodcast": "Hoy vamos a hablar sobre la seguridad informática en el trabajo...",
  "scriptVideo": "[{\"numero\": 1, \"titulo\": \"Introducción\", \"contenido\": \"...\", \"notas\": \"...\"}]",
  "podcastAudioUrl": "https://supabase.ejemplo.com/storage/v1/object/public/podcasts/uuid.mp3",
  "tiposSalida": "documentacion,podcast",
  "modelo": "llama-3.3-70b-versatile",
  "generadoEn": "2026-04-23T10:00:00Z"
}
```

> Los campos `contenido`, `scriptPodcast`, `scriptVideo` y `podcastAudioUrl` son `null` si no se solicitaron en `tiposSalida`.
> Si la generación de audio con ElevenLabs falla, `podcastAudioUrl` es `null` pero el resto de la respuesta se devuelve igualmente (el fallo de audio no bloquea la respuesta).

**Errores:**

| Código | Causa |
|---|---|
| `400` | Archivo vacío, formato no soportado (solo PDF/DOCX/TXT), o texto extraído en blanco |
| `403` | Rol insuficiente |
| `500` | Error en la API de Groq/Gemini o al leer el archivo |

---

## DTOs del módulo

| DTO | Descripción |
|---|---|
| `AiPromptRequest` | Request genérico: `tema`, `descripcion`, `tipoModulo`, `prompt`, `nombreEmpresa`, `contexto`, `numPreguntas` |
| `AiResponse` | Response con texto generado: `contenido`, `modelo`, `generadoEn` |
| `AiFileResponse` | Response de generación desde archivo: `titulo`, `descripcion`, `contenido`, `scriptPodcast`, `scriptVideo`, `podcastAudioUrl`, `tiposSalida`, `modelo`, `generadoEn` |

---

## Extracción de texto de archivos (AiFileService)

El módulo incluye soporte para extraer texto de:
- **PDF** → usando Apache PDFBox 3.0.2
- **DOCX** → usando Apache POI 5.2.5
- **TXT** → lectura directa

El texto extraído se trunca a **12 000 caracteres** antes de enviarse al modelo para no exceder el contexto. El tamaño máximo de archivo aceptado es **50 MB** (configurado en `spring.servlet.multipart.max-file-size`).

---

## Notas de implementación para el backend

### Generación desde archivo vs. otros endpoints

| Endpoint | Modelo | Proveedor |
|---|---|---|
| `/generar-contenido`, `/generar-preguntas`, `/chat`, `/resumir` | `gemini-2.0-flash` | Google Gemini |
| `/generar-desde-archivo` (texto y slides) | `llama-3.3-70b-versatile` | Groq |
| `/generar-desde-archivo` (audio podcast) | — | ElevenLabs TTS |

### Manejo de errores de APIs externas

Si cualquier proveedor externo (Gemini, Groq, ElevenLabs) devuelve un error (API Key inválida, límite de peticiones, timeout), el servicio propaga una `BusinessException` con el mensaje de error, que `GlobalExceptionHandler` convierte en `400` o `500` según el caso. **Excepción:** el fallo de ElevenLabs en `generar-desde-archivo` no revierte la respuesta — se devuelve sin `podcastAudioUrl`.
