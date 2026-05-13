package com.atalayas.backend.common.service;

import com.atalayas.backend.ai.service.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024; // 5 MB
    private static final int AVATAR_SIZE = 400;

    private final SupabaseStorageService supabaseStorageService;

    /**
     * Valida, redimensiona y sube el avatar a Supabase Storage.
     *
     * El bucket "modulos" es público, así que la URL devuelta es accesible
     * directamente desde el frontend sin autenticación. La imagen sobrevive
     * a reinicios del backend (a diferencia del almacenamiento en disco local).
     *
     * @param file archivo de imagen recibido del cliente
     * @return URL pública del avatar en Supabase Storage
     */
    public String processAndSaveAvatar(MultipartFile file) {
        validateFile(file);

        String filename = UUID.randomUUID() + ".jpg";
        byte[] processedBytes;

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Thumbnails.of(file.getInputStream())
                    .size(AVATAR_SIZE, AVATAR_SIZE)
                    .crop(Positions.CENTER)
                    .outputFormat("jpg")
                    .toOutputStream(out);
            processedBytes = out.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "No se pudo procesar la imagen: " + e.getMessage(), e);
        }

        // Subir a Supabase Storage en el bucket "modulos", carpeta "avatares"
        return supabaseStorageService.subirArchivo(
                processedBytes,
                "image/jpeg",
                "avatares",
                filename
        );
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo de imagen no puede estar vacío");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Tipo de archivo no permitido. Se aceptan: image/jpeg, image/png, image/webp, image/gif");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("El archivo supera el tamaño máximo permitido de 5 MB");
        }
    }
}
