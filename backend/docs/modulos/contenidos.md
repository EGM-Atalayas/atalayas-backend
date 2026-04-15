# Módulo: Contenidos (`content`)

> **Paquete:** `com.atalayas.backend.content`
> **Tablas BD:** `contenido`, `contenido_pregunta`
> **Audiencia:** Frontend, Backend
> **Última actualización:** Abril 2026

---

## ¿Qué hace este módulo?

Un **contenido** es la unidad mínima de formación dentro de un módulo. Puede ser un artículo de texto, un vídeo, un PDF, una evaluación con preguntas, o contenido generado por IA.

El frontend usa el campo `tipoContenido` para saber cómo renderizar cada contenido.

---

## Entidades

### `ContentItem` (tabla `contenido`)

| Campo | Tipo | Descripción |
|---|---|---|
| `contenidoId` | UUID (PK) | Identificador único |
| `moduloId` | UUID | FK al módulo al que pertenece |
| `empresaId` | UUID (nullable) | `null` = contenido global |
| `titulo` | String | Título visible para el empleado |
| `descripcion` | TEXT (nullable) | Descripción corta para listados |
| `tipoContenido` | Enum | Ver tipos abajo |
| `urlRecurso` | String (nullable) | URL del vídeo o PDF |
| `cuerpoTexto` | TEXT (nullable) | Cuerpo del artículo (tipo `TEXTO` o `IA_GENERADO`) |
| `orden` | int | Posición dentro del módulo (menor = primero) |
| `version` | int | Se incrementa al actualizar el cuerpo del contenido |
| `minutosEstimados` | Integer (nullable) | Tiempo estimado de lectura/visualización |
| `esIaGenerado` | boolean | `true` si el contenido lo generó la IA |
| `activo` | boolean | `false` = soft-delete (conserva trazabilidad histórica) |
| `fechaCreacion` | OffsetDateTime | Automático |
| `actualizadoEn` | OffsetDateTime | Automático |

### `Question` (tabla `contenido_pregunta`)

Solo existe para contenidos de tipo `EVALUACION`.

| Campo | Tipo | Descripción |
|---|---|---|
| `preguntaId` | UUID (PK) | Identificador único |
| `contenidoId` | UUID | FK al contenido de evaluación |
| `enunciado` | TEXT | Texto de la pregunta |
| `opciones` | JSONB | Array de respuestas posibles |
| `respuestaCorrecta` | String | La respuesta correcta |

### Tipos de contenido (`ContentType`)

| Valor | Descripción | Campo usado |
|---|---|---|
| `TEXTO` | Artículo o documento de texto | `cuerpoTexto` |
| `VIDEO` | Recurso de vídeo | `urlRecurso` |
| `PDF` | Documento PDF | `urlRecurso` |
| `EVALUACION` | Cuestionario con preguntas | Tabla `contenido_pregunta` |
| `IA_GENERADO` | Contenido mixto generado por IA | `cuerpoTexto` + posible `urlRecurso` |

---

## Endpoints

Base URL: `/api/v1/contenidos`

| Método | Ruta | Rol mínimo | Descripción |
|---|---|---|---|
| `POST` | `/contenidos` | `ADMIN_EMPRESA` | Crear contenido en un módulo |
| `GET` | `/contenidos/modulo/{moduloId}` | Cualquiera | Listar contenidos de un módulo |
| `GET` | `/contenidos/{id}` | Cualquiera | Obtener contenido por ID (incluye preguntas si es EVALUACION) |
| `PUT` | `/contenidos/{id}` | `ADMIN_EMPRESA` | Actualizar contenido completo |
| `PATCH` | `/contenidos/{id}/desactivar` | `ADMIN_EMPRESA` | Soft-delete |
| `POST` | `/contenidos/preguntas` | `ADMIN_EMPRESA` | Añadir pregunta a una EVALUACION |
| `DELETE` | `/contenidos/preguntas/{preguntaId}` | `ADMIN_EMPRESA` | Eliminar pregunta |

---

## Reglas de negocio

### Visibilidad

- `ROLE_EMPLEADO` / `ROLE_ADMIN_EMPRESA` → solo ven contenidos activos de su empresa + globales activos
- `ROLE_ADMIN` → ve todos incluyendo inactivos

### Versiones

Cuando se actualiza un contenido con `PUT /{id}`:
- Si cambia el `cuerpoTexto` o la `urlRecurso` → la `version` se **incrementa automáticamente**
- Los registros de progreso del empleado guardan la `versionLeida` en el momento de completarlo
- Esto permite detectar si el empleado tiene la versión más reciente del contenido

### Soft-delete de contenido

`activo = false` oculta el contenido para los empleados pero **conserva todo el historial de progreso** asociado a él.

### Preguntas de evaluación

Solo se pueden añadir preguntas a contenidos de tipo `EVALUACION`. Si se intenta en otro tipo → `400 Bad Request`.

---

## Request / Response

### POST `/contenidos` — Crear contenido

**Request body (tipo TEXTO):**
```json
{
  "moduloId": "770e8400-e29b-41d4-a716-446655440000",
  "titulo": "Políticas de la empresa",
  "descripcion": "Normas y procedimientos internos",
  "tipoContenido": "TEXTO",
  "cuerpoTexto": "# Políticas de la empresa\n\nContenido completo...",
  "orden": 1,
  "minutosEstimados": 10
}
```

**Request body (tipo VIDEO):**
```json
{
  "moduloId": "770e8400-...",
  "titulo": "Video de bienvenida",
  "tipoContenido": "VIDEO",
  "urlRecurso": "https://youtube.com/embed/xxxxx",
  "orden": 2,
  "minutosEstimados": 5
}
```

**Response `201 Created`:**
```json
{
  "contenidoId": "880e8400-e29b-41d4-a716-446655440000",
  "moduloId": "770e8400-...",
  "titulo": "Políticas de la empresa",
  "tipoContenido": "TEXTO",
  "orden": 1,
  "version": 1,
  "activo": true,
  "esIaGenerado": false
}
```

### GET `/contenidos/{id}` — Obtener contenido

**Response `200 OK` (tipo EVALUACION):**
```json
{
  "contenidoId": "880e8400-...",
  "titulo": "Evaluación de onboarding",
  "tipoContenido": "EVALUACION",
  "preguntas": [
    {
      "preguntaId": "990e8400-...",
      "enunciado": "¿Cuál es la misión de la empresa?",
      "opciones": ["Opción A", "Opción B", "Opción C", "Opción D"],
      "respuestaCorrecta": "Opción A"
    }
  ]
}
```

### POST `/contenidos/preguntas` — Añadir pregunta

**Request body:**
```json
{
  "contenidoId": "880e8400-...",
  "enunciado": "¿Cuántos días de vacaciones tienes?",
  "opciones": ["15 días", "22 días", "30 días", "25 días"],
  "respuestaCorrecta": "22 días"
}
```

