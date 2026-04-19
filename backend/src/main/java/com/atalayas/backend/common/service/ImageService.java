package com.atalayas.backend.common.service;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class ImageService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024; // 5 MB
    private static final int AVATAR_SIZE = 400;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /**
     * Valida, redimensiona y guarda el avatar.
     *
     * @param file archivo de imagen recibido del cliente
     * @return URL relativa accesible vía HTTP, p.ej. {@code /uploads/avatars/{uuid}.jpg}
     */
    public String processAndSaveAvatar(MultipartFile file) {
        validateFile(file);

        String filename = UUID.randomUUID() + ".jpg";
        Path avatarsDir = Paths.get(uploadDir, "avatars");

        try {
            Files.createDirectories(avatarsDir);
            Path destination = avatarsDir.resolve(filename);

            Thumbnails.of(file.getInputStream())
                    .size(AVATAR_SIZE, AVATAR_SIZE)
                    .crop(Positions.CENTER)
                    .outputFormat("jpg")
                    .toFile(destination.toFile());

        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "No se pudo procesar la imagen: " + e.getMessage(), e);
        }

        return "/uploads/avatars/" + filename;
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
