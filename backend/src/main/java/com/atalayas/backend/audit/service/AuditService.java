package com.atalayas.backend.audit.service;

import com.atalayas.backend.audit.entity.AuditLog;
import com.atalayas.backend.audit.repository.AuditLogRepository;
import com.atalayas.backend.dashboard.dto.ActividadRecienteDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
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

    private final AuditLogRepository auditLogRepository;

    /**
     * Persiste un nuevo evento de auditoría en su propia transacción independiente.
     * REQUIRES_NEW garantiza que el log se graba aunque la transacción del llamador
     * haga rollback (p. ej. si un email falla después de aprobar una empresa).
     *
     * @param texto Descripción legible del evento.
     * @param tipo  "info" | "success" | "warning" | "error"
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String texto, String tipo) {
        auditLogRepository.save(
                AuditLog.builder()
                        .texto(texto)
                        .tipo(tipo)
                        .build()
        );
    }

    /**
     * Devuelve los últimos 10 eventos con el campo {@code tiempo} calculado
     * de forma relativa al momento actual.
     */
    @Transactional(readOnly = true)
    public List<ActividadRecienteDto> getActividadReciente() {
        return auditLogRepository.findTop10ByOrderByCreadoEnDesc().stream()
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
        if (minutos < 1)  return "ahora mismo";
        if (minutos < 60) return "hace " + minutos + "m";
        long horas = ChronoUnit.HOURS.between(creadoEn, ahora);
        if (horas < 24)   return "hace " + horas + "h";
        long dias = ChronoUnit.DAYS.between(creadoEn, ahora);
        return "hace " + dias + "d";
    }
}

