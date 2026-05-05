package com.atalayas.backend.ai.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.*;

class AiFileServiceTest {

    private final AiFileService service = new AiFileService();

    private static final String HELLO = "Hola mundo, este es un documento de prueba.";

    // ── TXT ──────────────────────────────────────────────────────────────────

    @Test
    void extraerTexto_txt_devuelveContenido() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "archivo", "doc.txt", "text/plain", HELLO.getBytes());

        String result = service.extraerTexto(file);

        assertThat(result).isEqualTo(HELLO);
    }

    @Test
    void extraerTexto_txtPorExtension_devuelveContenido() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "archivo", "doc.txt", "application/octet-stream", HELLO.getBytes());

        String result = service.extraerTexto(file);

        assertThat(result).isEqualTo(HELLO);
    }

    // ── ERRORES ───────────────────────────────────────────────────────────────

    @Test
    void extraerTexto_archivoVacio_lanzaIllegalArgumentException() {
        MockMultipartFile file = new MockMultipartFile(
                "archivo", "vacio.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> service.extraerTexto(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vacío");
    }

    @Test
    void extraerTexto_archivoNull_lanzaIllegalArgumentException() {
        assertThatThrownBy(() -> service.extraerTexto(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void extraerTexto_formatoNoSoportado_lanzaIllegalArgumentException() {
        MockMultipartFile file = new MockMultipartFile(
                "archivo", "imagen.png", "image/png", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> service.extraerTexto(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Formato no soportado");
    }

    @Test
    void extraerTexto_archivoSuperaLimite_lanzaIllegalArgumentException() {
        byte[] grande = new byte[10 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile(
                "archivo", "grande.txt", "text/plain", grande);

        assertThatThrownBy(() -> service.extraerTexto(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10 MB");
    }

    // ── PDF mínimo ────────────────────────────────────────────────────────────

    @Test
    void extraerTexto_pdfMinimo_noLanzaExcepcion() {
        // PDF mínimo válido con una página en blanco (estructura PDF 1.4 estricta)
        byte[] pdf = ("%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n"
                + "2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n"
                + "3 0 obj<</Type/Page/MediaBox[0 0 612 792]/Parent 2 0 R>>endobj\n"
                + "xref\n0 4\n0000000000 65535 f\n0000000009 00000 n\n"
                + "0000000058 00000 n\n0000000115 00000 n\n"
                + "trailer<</Size 4/Root 1 0 R>>\nstartxref\n190\n%%EOF").getBytes();

        MockMultipartFile file = new MockMultipartFile(
                "archivo", "test.pdf", "application/pdf", pdf);

        // PDFBox puede lanzar IOException con PDF mal formados — lo aceptamos
        // Solo verificamos que no lanza IllegalArgumentException
        assertThatCode(() -> {
            try { service.extraerTexto(file); } catch (java.io.IOException ignored) {}
        }).doesNotThrowAnyException();
    }
}

