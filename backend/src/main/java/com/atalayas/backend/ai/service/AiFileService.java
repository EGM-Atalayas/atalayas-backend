package com.atalayas.backend.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Servicio de extracción de texto a partir de archivos subidos.
 * Soporta PDF (via Apache PDFBox), DOCX (via Apache POI) y TXT plano.
 */
@Slf4j
@Service
public class AiFileService {

    private static final long MAX_SIZE_BYTES = 10 * 1024 * 1024L; // 10 MB

    /**
     * Extrae el texto de un MultipartFile.
     * El tipo se detecta por el Content-Type o, como fallback, por la extensión del nombre.
     *
     * @param file archivo recibido vía multipart/form-data
     * @return texto extraído (nunca null, puede estar vacío)
     * @throws IllegalArgumentException si el formato no está soportado o el archivo supera el límite
     * @throws IOException              si la lectura falla
     */
    public String extraerTexto(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("El archivo supera el límite de 10 MB");
        }

        String contentType = file.getContentType();
        String nombre = file.getOriginalFilename() != null
                ? file.getOriginalFilename().toLowerCase()
                : "";

        if (esPdf(contentType, nombre)) {
            return extraerDesdePdf(file.getInputStream());
        } else if (esDocx(contentType, nombre)) {
            return extraerDesdeDocx(file.getInputStream());
        } else if (esTxt(contentType, nombre)) {
            return extraerDesdeTxt(file.getInputStream());
        } else {
            throw new IllegalArgumentException(
                    "Formato no soportado: " + contentType + ". Use PDF, DOCX o TXT.");
        }
    }

    // -----------------------------------------------------------------------
    // Métodos privados de extracción
    // -----------------------------------------------------------------------

    private String extraerDesdePdf(InputStream is) throws IOException {
        try (PDDocument doc = PDDocument.load(is)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String texto = stripper.getText(doc);
            log.debug("PDF extraído: {} caracteres", texto.length());
            return texto;
        }
    }

    private String extraerDesdeDocx(InputStream is) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(is)) {
            StringBuilder sb = new StringBuilder();
            doc.getParagraphs().forEach(p -> sb.append(p.getText()).append("\n"));
            String texto = sb.toString();
            log.debug("DOCX extraído: {} caracteres", texto.length());
            return texto;
        }
    }

    private String extraerDesdeTxt(InputStream is) throws IOException {
        String texto = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        log.debug("TXT extraído: {} caracteres", texto.length());
        return texto;
    }

    // -----------------------------------------------------------------------
    // Helpers de detección de tipo
    // -----------------------------------------------------------------------

    private boolean esPdf(String contentType, String nombre) {
        return "application/pdf".equalsIgnoreCase(contentType)
                || nombre.endsWith(".pdf");
    }

    private boolean esDocx(String contentType, String nombre) {
        return "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                .equalsIgnoreCase(contentType)
                || nombre.endsWith(".docx");
    }

    private boolean esTxt(String contentType, String nombre) {
        return (contentType != null && contentType.startsWith("text/"))
                || nombre.endsWith(".txt");
    }
}
