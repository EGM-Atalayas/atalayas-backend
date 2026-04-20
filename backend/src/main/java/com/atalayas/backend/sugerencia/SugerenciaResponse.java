package com.atalayas.backend.sugerencia;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SugerenciaResponse(
    UUID id,
    String mensaje,
    String nombreUsuario,
    String emailUsuario,
    EstadoSugerencia estado,
    DestinatarioSugerencia destinatario,
    OffsetDateTime creadoEn
) {}
