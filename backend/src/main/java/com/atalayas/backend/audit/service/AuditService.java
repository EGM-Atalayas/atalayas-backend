package com.atalayas.backend.audit.service;

import com.atalayas.backend.audit.entity.AuditLog;
import com.atalayas.backend.audit.repository.AuditLogRepository;
import com.atalayas.backend.dashboard.dto.ActividadRecienteDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Servicio de auditoría: registra eventos relevantes de la plataforma
 * y los expone como actividad reciente para el dashboard del superadmin.
 *
 * Llama a {@link #registrar(String, String)} desde cualquier servicio
 * que deba dejar traza (aprobación de empresa, publicación de módulo, etc.)
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es"));

    private final AuditLogRepository auditLogRepository;

    /**
     * Persiste un nuevo evento de auditoría.
     *
     * @param texto Descripción legible del evento.
     * @param tipo  "info" | "success" | "warning" | "error"
     */
    @Transactional
    public void registrar(String texto, String tipo) {
        auditLogRepository.save(
                AuditLog.builder()
                        .texto(texto)
                        .tipo(tipo)
                        .build()
        );
    }

    /** Devuelve los últimos {@code limit} eventos con tiempo relativo calculado. */
    @Transactional(readOnly = true)
    public List<ActividadRecienteDto> getActividadReciente(int limit) {
        return auditLogRepository
                .findAllByOrderByCreadoEnDesc(PageRequest.of(0, limit))
                .stream()
                .map(log -> ActividadRecienteDto.builder()
                        .id(log.getId())
                        .texto(log.getTexto())
                        .tiempo(tiempoRelativo(log.getCreadoEn()))
                        .tipo(log.getTipo())
                        .build())
                .collect(Collectors.toList());
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private String tiempoRelativo(OffsetDateTime creadoEn) {
        OffsetDateTime ahora = OffsetDateTime.now();
        long minutos = ChronoUnit.MINUTES.between(creadoEn, ahora);
        if (minutos < 1)   return "Ahora mismo";
        if (minutos < 60)  return "Hace " + minutos + " minuto" + (minutos == 1 ? "" : "s");
        long horas = ChronoUnit.HOURS.between(creadoEn, ahora);
        if (horas < 24)    return "Hace " + horas + " hora" + (horas == 1 ? "" : "s");
        long dias = ChronoUnit.DAYS.between(creadoEn.toLocalDate(), ahora.toLocalDate());
        if (dias == 1)     return "Ayer";
        if (dias < 7)      return "Hace " + dias + " días";
        return capitalize(creadoEn.format(FORMATTER));
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
