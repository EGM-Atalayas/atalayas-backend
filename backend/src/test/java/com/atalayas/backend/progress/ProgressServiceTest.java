package com.atalayas.backend.progress;

import com.atalayas.backend.common.enums.ProgressStatus;
import com.atalayas.backend.communication.service.NotificationService;
import com.atalayas.backend.content.entity.ContentItem;
import com.atalayas.backend.content.repository.ContentRepository;
import com.atalayas.backend.exception.ResourceNotFoundException;
import com.atalayas.backend.exception.UnauthorizedException;
import com.atalayas.backend.progress.dto.CompleteContentRequest;
import com.atalayas.backend.progress.dto.ProgressResponse;
import com.atalayas.backend.progress.entity.UserProgress;
import com.atalayas.backend.progress.repository.ProgressRepository;
import com.atalayas.backend.support.TestFixtures;
import com.atalayas.backend.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    @Mock ProgressRepository progressRepo;
    @Mock ContentRepository contentRepo;
    @Mock NotificationService notificationService;
    @InjectMocks ProgressService service;

    private final UUID contenidoId = UUID.randomUUID();
    private final UUID moduloId    = UUID.randomUUID();
    private ContentItem contenido;
    private CompleteContentRequest req;

    @BeforeEach
    void setUp() {
        contenido = ContentItem.builder()
                .contenidoId(contenidoId)
                .moduloId(moduloId)
                .titulo("Contenido de prueba")
                .build();

        req = new CompleteContentRequest();
        req.setContenidoId(contenidoId);
        req.setTiempoSegundos(120);
        req.setPorcentajeCompletado(50);
        req.setVersionLeida(1);
        req.setCompletado(false);
    }

    // ── REGISTRAR PROGRESO ────────────────────────────────────────────────────

    @Test
    void registrarProgreso_nuevoRegistro_creaConTiempoInicial() {
        User emp = TestFixtures.empleado();
        when(contentRepo.findById(contenidoId)).thenReturn(Optional.of(contenido));
        when(progressRepo.findByUsuarioIdAndContenidoId(emp.getUsuarioId(), contenidoId))
                .thenReturn(Optional.empty());

        UserProgress guardado = UserProgress.builder()
                .registroId(UUID.randomUUID())
                .usuarioId(emp.getUsuarioId())
                .contenidoId(contenidoId)
                .moduloId(moduloId)
                .empresaId(emp.getEmpresaId())
                .tiempoSegundos(120)
                .porcentajeCompletado(50)
                .completado(false)
                .build();
        when(progressRepo.save(any())).thenReturn(guardado);

        ProgressResponse result = service.registrarProgreso(req, emp);

        assertThat(result.getTiempoSegundos()).isEqualTo(120);
        assertThat(result.getEstado()).isEqualTo(ProgressStatus.EN_PROGRESO);
        verify(progressRepo).save(any());
    }

    @Test
    void registrarProgreso_registroExistente_acumulaTiempo() {
        User emp = TestFixtures.empleado();
        UserProgress existente = UserProgress.builder()
                .usuarioId(emp.getUsuarioId())
                .contenidoId(contenidoId)
                .moduloId(moduloId)
                .empresaId(emp.getEmpresaId())
                .tiempoSegundos(300)
                .completado(false)
                .build();

        when(contentRepo.findById(contenidoId)).thenReturn(Optional.of(contenido));
        when(progressRepo.findByUsuarioIdAndContenidoId(emp.getUsuarioId(), contenidoId))
                .thenReturn(Optional.of(existente));

        UserProgress guardado = UserProgress.builder()
                .usuarioId(emp.getUsuarioId()).contenidoId(contenidoId)
                .tiempoSegundos(420).completado(false).build();
        when(progressRepo.save(existente)).thenReturn(guardado);

        ProgressResponse result = service.registrarProgreso(req, emp);

        assertThat(existente.getTiempoSegundos()).isEqualTo(420); // 300 + 120
        verify(progressRepo).save(existente);
    }

    @Test
    void registrarProgreso_completado_disparaNotificacion() {
        User emp = TestFixtures.empleado();
        req.setCompletado(true);
        req.setPorcentajeCompletado(100);

        when(contentRepo.findById(contenidoId)).thenReturn(Optional.of(contenido));
        when(progressRepo.findByUsuarioIdAndContenidoId(any(), any())).thenReturn(Optional.empty());

        UserProgress guardado = UserProgress.builder()
                .usuarioId(emp.getUsuarioId()).contenidoId(contenidoId)
                .moduloId(moduloId).empresaId(emp.getEmpresaId())
                .tiempoSegundos(120).completado(true).porcentajeCompletado(100).build();
        when(progressRepo.save(any())).thenReturn(guardado);

        service.registrarProgreso(req, emp);

        verify(notificationService).crearInterna(
                eq(emp.getUsuarioId()),
                eq("CONTENIDO_COMPLETADO"),
                contains("Contenido de prueba"),
                any()
        );
    }

    @Test
    void registrarProgreso_completadoIrreversible_noRevierte() {
        User emp = TestFixtures.empleado();
        req.setCompletado(false); // intento de quitar completado

        UserProgress yaCompletado = UserProgress.builder()
                .usuarioId(emp.getUsuarioId()).contenidoId(contenidoId)
                .moduloId(moduloId).empresaId(emp.getEmpresaId())
                .tiempoSegundos(500).completado(true).porcentajeCompletado(100).build();

        when(contentRepo.findById(contenidoId)).thenReturn(Optional.of(contenido));
        when(progressRepo.findByUsuarioIdAndContenidoId(emp.getUsuarioId(), contenidoId))
                .thenReturn(Optional.of(yaCompletado));
        when(progressRepo.save(yaCompletado)).thenReturn(yaCompletado);

        service.registrarProgreso(req, emp);

        assertThat(yaCompletado.isCompletado()).isTrue(); // no se revirtió
        verify(notificationService, never()).crearInterna(any(), any(), any(), any());
    }

    @Test
    void registrarProgreso_contenidoNoExiste_lanzaResourceNotFoundException() {
        when(contentRepo.findById(contenidoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarProgreso(req, TestFixtures.empleado()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void registrarProgreso_porcentajeNoRetrocede() {
        User emp = TestFixtures.empleado();
        UserProgress existente = UserProgress.builder()
                .usuarioId(emp.getUsuarioId()).contenidoId(contenidoId)
                .moduloId(moduloId).empresaId(emp.getEmpresaId())
                .porcentajeCompletado(80).tiempoSegundos(200).completado(false).build();

        req.setPorcentajeCompletado(30); // valor menor

        when(contentRepo.findById(contenidoId)).thenReturn(Optional.of(contenido));
        when(progressRepo.findByUsuarioIdAndContenidoId(any(), any())).thenReturn(Optional.of(existente));
        when(progressRepo.save(existente)).thenReturn(existente);

        service.registrarProgreso(req, emp);

        assertThat(existente.getPorcentajeCompletado()).isEqualTo(80); // no retrocede
    }

    // ── MI PROGRESO ───────────────────────────────────────────────────────────

    @Test
    void miProgreso_devuelveListaOrdenada() {
        User emp = TestFixtures.empleado();
        UserProgress p = UserProgress.builder().usuarioId(emp.getUsuarioId())
                .contenidoId(contenidoId).moduloId(moduloId).empresaId(emp.getEmpresaId())
                .tiempoSegundos(60).completado(false).build();
        when(progressRepo.findByUsuarioIdOrderByActualizadoEnDesc(emp.getUsuarioId()))
                .thenReturn(List.of(p));

        List<ProgressResponse> result = service.miProgreso(emp);

        assertThat(result).hasSize(1);
    }

    // ── PROGRESO POR CONTENIDO ────────────────────────────────────────────────

    @Test
    void progresoPorContenido_sinRegistro_devuelvePendienteVirtual() {
        User emp = TestFixtures.empleado();
        when(progressRepo.findByUsuarioIdAndContenidoId(emp.getUsuarioId(), contenidoId))
                .thenReturn(Optional.empty());

        ProgressResponse result = service.progresoPorContenido(contenidoId, emp);

        assertThat(result.getEstado()).isEqualTo(ProgressStatus.PENDIENTE);
        assertThat(result.getRegistroId()).isNull(); // virtual, no persistido
        verify(progressRepo, never()).save(any());
    }

    @Test
    void progresoPorContenido_conRegistro_devuelveEstadoDerivado() {
        User emp = TestFixtures.empleado();
        UserProgress p = UserProgress.builder().registroId(UUID.randomUUID())
                .usuarioId(emp.getUsuarioId()).contenidoId(contenidoId)
                .moduloId(moduloId).empresaId(emp.getEmpresaId())
                .tiempoSegundos(200).completado(false).build();
        when(progressRepo.findByUsuarioIdAndContenidoId(emp.getUsuarioId(), contenidoId))
                .thenReturn(Optional.of(p));

        ProgressResponse result = service.progresoPorContenido(contenidoId, emp);

        assertThat(result.getEstado()).isEqualTo(ProgressStatus.EN_PROGRESO);
    }

    // ── PROGRESO POR EMPRESA ──────────────────────────────────────────────────

    @Test
    void progresoPorEmpresa_adminEmpresaSuya_devuelveProgreso() {
        User admin = TestFixtures.adminEmpresa();
        when(progressRepo.findByEmpresaIdOrderByActualizadoEnDesc(TestFixtures.EMPRESA_A))
                .thenReturn(List.of());

        List<ProgressResponse> result = service.progresoPorEmpresa(TestFixtures.EMPRESA_A, admin);

        assertThat(result).isEmpty();
    }

    @Test
    void progresoPorEmpresa_adminEmpresaOtraEmpresa_lanzaUnauthorizedException() {
        User admin = TestFixtures.adminEmpresa(TestFixtures.EMPRESA_A);

        assertThatThrownBy(() -> service.progresoPorEmpresa(TestFixtures.EMPRESA_B, admin))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void progresoPorEmpresa_superAdmin_puedeConsultarCualquier() {
        User admin = TestFixtures.superAdmin();
        when(progressRepo.findByEmpresaIdOrderByActualizadoEnDesc(TestFixtures.EMPRESA_B))
                .thenReturn(List.of());

        assertThatNoException().isThrownBy(() -> service.progresoPorEmpresa(TestFixtures.EMPRESA_B, admin));
    }
}

