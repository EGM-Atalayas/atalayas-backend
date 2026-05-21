### Empleado (cualquier autenticado)

| Método | Ruta | Descripción |
|--------|------|-------------|
| `GET` | `/documentos/me` | Listar documentos asignados al usuario autenticado |
| `POST` | `/documentos/me/{id}/visto` | Marcar documento como visto → `204 No Content`. Idempotente |
| `POST` | `/documentos/me/{id}/firmar` | **Firmar documento** con firma manuscrita → devuelve `{ firmaUrl }` |
| `GET` | `/documentos/me/certificado/{moduloId}` | URL del certificado PDF auto-generado para un módulo completado |

### `POST /documentos/me/{id}/firmar` — Firmar documento

**Request body (`FirmarDocumentoRequest`):**
```json
{ "firmaBase64": "data:image/png;base64,iVBORw0KGgo..." }
```

- `firmaBase64` = salida de `canvas.toDataURL("image/png")` — con o sin el prefijo `data:image/png;base64,`.
- El backend descarga el PDF original, estampa la firma como imagen sobre el documento, sube el PDF firmado a Supabase Storage y actualiza `firmado=true`, `fechaFirma`, `firmaUrl` en la `DocumentoAsignacion`.
- Solo funciona si `documento.requiereFirma = true`.

**Response `200 OK` (`FirmarDocumentoResponse`):**
```json
{ "firmaUrl": "https://supabase.../documentos/firmado-uuid.pdf" }
```

| Código | Cuándo |
|--------|--------|
| `200` | Firmado correctamente. Devuelve `{ firmaUrl }` |
| `400` | Ya estaba firmado |
| `400` | El documento no requiere firma (`requiereFirma = false`) |
| `404` | Documento no asignado al usuario |

## Estado de implementación

| Funcionalidad | Estado |
|---|---|
| Subida / asignación | ✅ |
| Listado admin con totales | ✅ |
| Detalle asignaciones | ✅ |
| Soft delete | ✅ |
| Listado del empleado | ✅ |
| Marcar como visto | ✅ |
| **Firma electrónica** (`POST /documentos/me/{id}/firmar`) | ✅ Implementado |
| URL certificado por módulo | ✅ |
| Certificados PDF auto-generados | ✅ |
