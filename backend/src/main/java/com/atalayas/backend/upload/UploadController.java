package com.atalayas.backend.upload;

import com.atalayas.backend.ai.service.SupabaseStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

/**
 * Endpoints para subir archivos al Storage de Supabase desde el frontend.
 * Al pasar la subida por el backend, los compañeros no necesitan credenciales
 * de Supabase en su .env.local — solo el backend las necesita.
 */
@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
@Tag(name = "Upload", description = "Subida de archivos a Supabase Storage")
@SecurityRequirement(name = "bearerAuth")
public class UploadController {

    private final SupabaseStorageService supabaseStorageService;

    /**
     * POST /api/v1/upload/imagen
     * Sube una imagen (JPG, PNG, WebP...) a la carpeta "portadas" del bucket "modulos".
     * Devuelve la URL pública.
     */
    @PostMapping(value = "/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir imagen — devuelve URL pública de Supabase")
    public ResponseEntity<Map<String, String>> subirImagen(
            @RequestParam("file") MultipartFile file) {

        String ext      = getExtension(file.getOriginalFilename(), "jpg");
        String fileName = UUID.randomUUID() + "." + ext;

        try {
            String url = supabaseStorageService.subirArchivo(
                    file.getBytes(),
                    file.getContentType() != null ? file.getContentType() : "image/jpeg",
                    "portadas",
                    fileName
            );
            return ResponseEntity.ok(Map.of("url", url));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/v1/upload/adjunto
     * Sube un documento (PDF, DOCX...) a la carpeta "adjuntos" del bucket "modulos".
     * Devuelve la URL pública y el nombre original del archivo.
     */
    @PostMapping(value = "/adjunto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir adjunto — devuelve URL pública y nombre original")
    public ResponseEntity<Map<String, String>> subirAdjunto(
            @RequestParam("file") MultipartFile file) {

        String ext      = getExtension(file.getOriginalFilename(), "pdf");
        String fileName = UUID.randomUUID() + "." + ext;
        String nombre   = file.getOriginalFilename() != null ? file.getOriginalFilename() : fileName;

        try {
            String url = supabaseStorageService.subirArchivo(
                    file.getBytes(),
                    file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                    "adjuntos",
                    fileName
            );
            return ResponseEntity.ok(Map.of("url", url, "nombre", nombre));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    private String getExtension(String filename, String fallback) {
        if (filename == null || !filename.contains(".")) return fallback;
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
