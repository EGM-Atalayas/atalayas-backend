package com.atalayas.backend.moduloprogreso;

import com.atalayas.backend.common.util.SecurityUtils;
import com.atalayas.backend.documento.CertificadoService;
import com.atalayas.backend.moduloprogreso.dto.GuardarProgresoRequest;
import com.atalayas.backend.moduloprogreso.dto.ModuloProgresoResponse;
import com.atalayas.backend.moduloprogreso.entity.ModuloProgreso;
import com.atalayas.backend.moduloprogreso.repository.ModuloProgresoRepository;
import com.atalayas.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Lógica de progreso por módulo formativo.
 *
 * Upsert por (usuario, módulo): si ya existe registro se actualiza
 * de forma monótona (el porcentaje no retrocede). Al llegar a 100 %
 * se dispara — tras commit — la generación del certificado.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ModuloProgresoService {

    private final ModuloProgresoRepository repo;
    private final CertificadoService       certificadoService;

    // ── GUARDAR PROGRESO ───────────────────────────────────────────────────

    /**
     * Crea o actualiza el progreso del usuario autenticado para un módulo.
     * Si el porcentaje resultante es 100, marca {@code completado=true}
     * y registra una callback {@code afterCommit} para generar el certificado.
     */
    @Transactional
    public ModuloProgresoResponse guardarProgreso(UUID moduloId, GuardarProgresoRequest req) {
        User user = SecurityUtils.getCurrentUser();
        UUID userId    = user.getUsuarioId();
        UUID empresaId = user.getEmpresaId();

        int completados = Math.max(0, req.getContenidosCompletados());
        int total       = Math.max(0, req.getTotalContenidos());
        if (total > 0 && completados > total) completados = total;
        int porcentaje  = total == 0 ? 0 : Math.min(100, Math.round((completados * 100f) / total));

        ModuloProgreso prog = repo.findByUsuarioIdAndModuloId(userId, moduloId)
                .orElseGet(() -> ModuloProgreso.builder()
                        .usuarioId(userId)
                        .moduloId(moduloId)
                        .empresaId(empresaId)
                        .build());

        // Monotonicidad: nunca retrocede ni el % ni la cuenta de completados
        if (porcentaje > prog.getPorcentaje())              prog.setPorcentaje(porcentaje);
        if (completados > prog.getContenidosCompletados())  prog.setContenidosCompletados(completados);
        if (total > prog.getTotalContenidos())              prog.setTotalContenidos(total);

        // 'completado' es irreversible una vez marcado
        boolean recienCompletado = false;
        if (!prog.isCompletado() && prog.getPorcentaje() >= 100) {
            prog.setCompletado(true);
            prog.setFechaCompletado(OffsetDateTime.now());
            recienCompletado = true;
            log.info("Módulo {} completado por usuario {}", moduloId, userId);
        }

        ModuloProgreso guardado = repo.save(prog);

        // Generación de certificado tras commit (igual patrón que en ProgressService)
        if (recienCompletado) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        certificadoService.solicitarCertificado(moduloId);
                    } catch (Exception e) {
                        log.warn("No se pudo generar certificado para módulo {} usuario {}: {}",
                                moduloId, userId, e.getMessage());
                    }
                }
            });
        }

        return toResponse(guardado);
    }

    // ── LECTURA ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ModuloProgresoResponse> miProgreso() {
        UUID userId = SecurityUtils.getCurrentUser().getUsuarioId();
        return repo.findByUsuarioIdOrderByActualizadoEnDesc(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ModuloProgresoResponse miProgresoModulo(UUID moduloId) {
        UUID userId = SecurityUtils.getCurrentUser().getUsuarioId();
        return repo.findByUsuarioIdAndModuloId(userId, moduloId)
                .map(this::toResponse)
                .orElse(ModuloProgresoResponse.builder()
                        .moduloId(moduloId)
                        .contenidosCompletados(0)
                        .totalContenidos(0)
                        .porcentaje(0)
                        .completado(false)
                        .build());
    }

    // ── HELPERS ───────────────────────────────────────────────────────────

    private ModuloProgresoResponse toResponse(ModuloProgreso p) {
        return ModuloProgresoResponse.builder()
                .moduloId(p.getModuloId())
                .contenidosCompletados(p.getContenidosCompletados())
                .totalContenidos(p.getTotalContenidos())
                .porcentaje(p.getPorcentaje())
                .completado(p.isCompletado())
                .fechaInicio(p.getFechaInicio())
                .fechaCompletado(p.getFechaCompletado())
                .actualizadoEn(p.getActualizadoEn())
                .build();
    }
}
