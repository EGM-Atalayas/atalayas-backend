# Módulo: Inteligencia Artificial (`ai`)

> **Paquete:** `com.atalayas.backend.ai`
> **Audiencia:** Frontend, Backend
> **Última actualización:** Abril 2026

---

## ¿Qué hace este módulo?

Integra la plataforma con **Google Gemini** para ofrecer funcionalidades de IA a los administradores y empleados:

| Función | Quién la usa | Descripción |
|---|---|---|
| Generar contenido formativo | Admin empresa | Genera artículos y material de formación dado un tema |
| Generar preguntas de evaluación | Admin empresa | Genera preguntas de test para un contenido |
| Chatbot | Empleados | Asistente conversacional contextualizado con la empresa |
| Resumir texto | Admin empresa | Resume documentos largos o textos de formación |

---

## Arquitectura de la integración

```
Controller (AiController)
       │
       ▼
Service layer
  ├── AiContentService    → genera contenido formativo
  ├── AiChatService       → chatbot empleados
  ├── AiSummaryService    → resumen de texto
  └── AiFileService       → extracción de texto de PDF/DOCX + resumen
       │
       ▼
AI Client (client/)
  └── Llamada HTTP a la API REST de Gemini
      URL: https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent
      Auth: ?key={GEMINI_API_KEY}
```

La comunicación con Gemini se hace mediante llamadas HTTP directas (sin SDK de Java). Se envía un prompt y se recibe el texto generado.

---

## Endpoints

Base URL: `/api/v1/ai`

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| `POST` | `/ai/generar-contenido` | `ADMIN_EMPRESA` | Genera contenido formativo |
| `POST` | `/ai/generar-preguntas` | `ADMIN_EMPRESA` | Genera preguntas de evaluación |
| `POST` | `/ai/chat` | Cualquiera | Chatbot para empleados |
| `POST` | `/ai/resumir` | `ADMIN_EMPRESA` | Genera resumen de un texto |

> **Nota:** Todos los endpoints requieren que la variable `GEMINI_API_KEY` esté configurada.
> Si está vacía, los endpoints devuelven `500 Internal Server Error` con mensaje descriptivo.

---

## Request / Response

### POST `/ai/generar-contenido` — Generar contenido formativo

Genera el cuerpo de un contenido de tipo `TEXTO` o `IA_GENERADO` listo para guardar en la plataforma.

**Request body:**
```json
{
  "prompt": "Genera un artículo sobre las políticas de seguridad informática para empleados de una empresa tecnológica",
  "tipoModulo": "ESPECIALIZADO",
  "nombreEmpresa": "Tecnología SL"
}
```

**Response `200 OK`:**
```json
{
  "contenidoGenerado": "# Políticas de Seguridad Informática\n\n## Introducción\n\nLa seguridad informática es fundamental...",
  "tituloSugerido": "Políticas de Seguridad Informática para Empleados",
  "minutosEstimados": 8
}
```

### POST `/ai/generar-preguntas` — Generar preguntas de evaluación

**Request body:**
```json
{
  "prompt": "Genera preguntas sobre las políticas de seguridad informática",
  "numPreguntas": 5
}
```
> `numPreguntas` es opcional. Por defecto: `5`.

**Response `200 OK`:**
```json
{
  "preguntas": [
    {
      "enunciado": "¿Cuál es la longitud mínima recomendada para una contraseña segura?",
      "opciones": ["6 caracteres", "8 caracteres", "12 caracteres", "16 caracteres"],
      "respuestaCorrecta": "12 caracteres"
    },
    {
      "enunciado": "¿Qué es el phishing?",
      "opciones": [
        "Un tipo de red WiFi",
        "Un ataque que suplanta la identidad para robar credenciales",
        "Un software antivirus",
        "Un protocolo de cifrado"
      ],
      "respuestaCorrecta": "Un ataque que suplanta la identidad para robar credenciales"
    }
  ]
}
```

### POST `/ai/chat` — Chatbot para empleados

Asistente conversacional que responde en el contexto de la empresa del empleado.

**Request body:**
```json
{
  "prompt": "¿Cuántos días de vacaciones tengo?",
  "nombreEmpresa": "Tecnología SL",
  "contexto": "Política de vacaciones: 22 días laborables al año. Se pueden fraccionar en períodos de mínimo 5 días."
}
```

**Response `200 OK`:**
```json
{
  "respuesta": "Según la política de vacaciones de Tecnología SL, tienes derecho a 22 días laborables de vacaciones al año. Puedes disfrutarlos fraccionados en periodos de mínimo 5 días. Para solicitar tus vacaciones, contacta con el departamento de RRHH."
}
```

### POST `/ai/resumir` — Resumir texto

**Request body:**
```json
{
  "prompt": "Texto largo del documento a resumir...",
  "nombreEmpresa": "Tecnología SL"
}
```

**Response `200 OK`:**
```json
{
  "resumen": "## Resumen\n\n**Puntos clave:**\n- Punto 1...\n- Punto 2...\n\n**Conclusión:**..."
}
```

---

## DTOs del módulo

| DTO | Descripción |
|---|---|
| `AiPromptRequest` | Request genérico con `prompt`, `nombreEmpresa`, `contexto`, `numPreguntas` |
| `AiResponse` | Response con texto generado (`contenidoGenerado`, `respuesta`, `resumen`) |
| `AiFileResponse` | Response de extracción de texto de archivo con el texto extraído |

---

## Extracción de texto de archivos (AiFileService)

El módulo incluye soporte para extraer texto de:
- **PDF** → usando Apache PDFBox 3.0.2
- **DOCX** → usando Apache POI 5.2.5

Este texto se puede usar como `contexto` en las peticiones al chatbot o como base para generar resúmenes. El tamaño máximo de archivo es **50 MB** (configurado en `application.properties`).

---

## Notas de implementación para el backend

### Límite de tokens

Gemini está configurado con un máximo de `2000` tokens de salida (`gemini.max-tokens`). Para contenidos muy largos, puede ser necesario aumentar este valor o implementar generación en fragmentos.

### Manejo de errores de Gemini

Si Gemini devuelve un error (API Key inválida, límite de peticiones, timeout), el servicio propaga un `BusinessException` con el mensaje de error de Gemini, que `GlobalExceptionHandler` convierte en una respuesta `400` o `500` según el caso.

