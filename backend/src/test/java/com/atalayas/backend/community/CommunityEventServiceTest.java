package com.atalayas.backend.community;

import com.atalayas.backend.community.dto.CommunityEventRequest;
import com.atalayas.backend.community.dto.CommunityEventResponse;
import com.atalayas.backend.community.entity.CommunityEvent;
import com.atalayas.backend.community.mapper.CommunityEventMapper;
import com.atalayas.backend.community.repository.CommunityEventRepository;
import com.atalayas.backend.exception.ResourceNotFoundException;
import com.atalayas.backend.exception.UnauthorizedException;
import com.atalayas.backend.support.TestFixtures;
import com.atalayas.backend.usuario.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommunityEventServiceTest {

    @Mock CommunityEventRepository repo;
    @Mock CommunityEventMapper mapper;
    @InjectMocks CommunityEventService service;

    private CommunityEventRequest req;
    private CommunityEvent evento;
    private CommunityEventResponse resp;
    private final UUID eventoId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        req = new CommunityEventRequest();
        req.setTitulo("Test Event");
        req.setFechaInicio(OffsetDateTime.now().plusDays(1));

        evento = CommunityEvent.builder()
                .eventoId(eventoId)
                .titulo("Test Event")
                .empresaId(TestFixtures.EMPRESA_A)
                .esGlobal(false)
                .activo(true)
                .fechaInicio(req.getFechaInicio())
                .build();

        resp = CommunityEventResponse.builder()
                .eventoId(eventoId)
                .titulo("Test Event")
                .activo(true)
                .build();
    }

    // ── CREAR ────────────────────────────────────────────────────────────────

    @Test
    void crear_adminEmpresa_asignaEmpresaDelToken() {
        User admin = TestFixtures.adminEmpresa();
        when(mapper.toEntity(any(), any(), anyBoolean(), any())).thenReturn(evento);
        when(repo.save(evento)).thenReturn(evento);
        when(mapper.toResponse(evento)).thenReturn(resp);

        service.crear(req, admin);

        ArgumentCaptor<UUID> empresaCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(mapper).toEntity(eq(req), empresaCaptor.capture(), eq(false), eq(admin.getUsuarioId()));
        assertThat(empresaCaptor.getValue()).isEqualTo(TestFixtures.EMPRESA_A);
    }

    @Test
    void crear_superAdmin_puedeCrearGlobal() {
        req.setEsGlobal(true);
        req.setEmpresaId(null);
        User admin = TestFixtures.superAdmin();
        CommunityEvent global = CommunityEvent.builder().eventoId(UUID.randomUUID())
                .esGlobal(true).activo(true).titulo("Global").fechaInicio(req.getFechaInicio()).build();
        CommunityEventResponse globalResp = CommunityEventResponse.builder().esGlobal(true).build();

        when(mapper.toEntity(any(), isNull(), eq(true), any())).thenReturn(global);
        when(repo.save(global)).thenReturn(global);
        when(mapper.toResponse(global)).thenReturn(globalResp);

        CommunityEventResponse result = service.crear(req, admin);

        assertThat(result.isEsGlobal()).isTrue();
    }

    @Test
    void crear_adminEmpresa_noPodraMarcaEsGlobal() {
        req.setEsGlobal(true);
        User admin = TestFixtures.adminEmpresa();
        when(mapper.toEntity(any(), any(), eq(false), any())).thenReturn(evento);
        when(repo.save(evento)).thenReturn(evento);
        when(mapper.toResponse(evento)).thenReturn(resp);

        service.crear(req, admin);

        verify(mapper).toEntity(any(), any(), eq(false), any());
    }

    // ── LISTAR ───────────────────────────────────────────────────────────────

    @Test
    void listar_superAdmin_devuelveTodosActivos() {
        User admin = TestFixtures.superAdmin();
        when(repo.findByActivoTrueOrderByFechaInicioAsc()).thenReturn(List.of(evento));
        when(mapper.toResponse(evento)).thenReturn(resp);

        List<CommunityEventResponse> result = service.listar(admin);

        assertThat(result).hasSize(1);
        verify(repo).findByActivoTrueOrderByFechaInicioAsc();
    }

    @Test
    void listar_adminEmpresa_incluyeGlobales() {
        User admin = TestFixtures.adminEmpresa();
        CommunityEvent global = CommunityEvent.builder().eventoId(UUID.randomUUID())
                .esGlobal(true).activo(true).titulo("Global").build();

        when(repo.findByEmpresaIdOrderByFechaInicioAsc(TestFixtures.EMPRESA_A)).thenReturn(List.of(evento));
        when(repo.findByActivoTrueOrderByFechaInicioAsc()).thenReturn(List.of(global));
        when(mapper.toResponse(any())).thenReturn(resp);

        List<CommunityEventResponse> result = service.listar(admin);

        assertThat(result).hasSize(2);
    }

    @Test
    void listar_empleado_usaQueryOptimizada() {
        User emp = TestFixtures.empleado();
        when(repo.findVisiblesParaEmpresa(TestFixtures.EMPRESA_A)).thenReturn(List.of(evento));
        when(mapper.toResponse(evento)).thenReturn(resp);

        List<CommunityEventResponse> result = service.listar(emp);

        assertThat(result).hasSize(1);
        verify(repo).findVisiblesParaEmpresa(TestFixtures.EMPRESA_A);
    }

    // ── OBTENER POR ID ───────────────────────────────────────────────────────

    @Test
    void obtenerPorId_existeYAccesible_devuelveResponse() {
        User admin = TestFixtures.superAdmin();
        when(repo.findById(eventoId)).thenReturn(Optional.of(evento));
        when(mapper.toResponse(evento)).thenReturn(resp);

        CommunityEventResponse result = service.obtenerPorId(eventoId, admin);

        assertThat(result.getEventoId()).isEqualTo(eventoId);
    }

    @Test
    void obtenerPorId_noExiste_lanzaResourceNotFoundException() {
        when(repo.findById(eventoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorId(eventoId, TestFixtures.superAdmin()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtenerPorId_empleadoOtraEmpresa_lanzaUnauthorizedException() {
        User emp = TestFixtures.empleado(TestFixtures.EMPRESA_B);
        CommunityEvent eventoEmprA = CommunityEvent.builder()
                .eventoId(eventoId).empresaId(TestFixtures.EMPRESA_A).esGlobal(false).activo(true)
                .titulo("X").fechaInicio(OffsetDateTime.now()).build();
        when(repo.findById(eventoId)).thenReturn(Optional.of(eventoEmprA));

        assertThatThrownBy(() -> service.obtenerPorId(eventoId, emp))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ── ACTUALIZAR ───────────────────────────────────────────────────────────

    @Test
    void actualizar_adminEmpresaPropio_actualizaYGuarda() {
        User admin = TestFixtures.adminEmpresa();
        when(repo.findById(eventoId)).thenReturn(Optional.of(evento));
        when(repo.save(evento)).thenReturn(evento);
        when(mapper.toResponse(evento)).thenReturn(resp);

        req.setTitulo("Nuevo titulo");
        service.actualizar(eventoId, req, admin);

        verify(repo).save(evento);
        assertThat(evento.getTitulo()).isEqualTo("Nuevo titulo");
    }

    @Test
    void actualizar_adminEmpresaGlobal_lanzaUnauthorizedException() {
        User admin = TestFixtures.adminEmpresa();
        CommunityEvent global = CommunityEvent.builder().eventoId(eventoId)
                .esGlobal(true).activo(true).titulo("Global").fechaInicio(OffsetDateTime.now()).build();
        when(repo.findById(eventoId)).thenReturn(Optional.of(global));

        assertThatThrownBy(() -> service.actualizar(eventoId, req, admin))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void actualizar_adminEmpresaOtraEmpresa_lanzaUnauthorizedException() {
        User admin = TestFixtures.adminEmpresa(TestFixtures.EMPRESA_B);
        when(repo.findById(eventoId)).thenReturn(Optional.of(evento)); // evento es de EMPRESA_A

        assertThatThrownBy(() -> service.actualizar(eventoId, req, admin))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ── DESACTIVAR ───────────────────────────────────────────────────────────

    @Test
    void desactivar_activoYPropio_ponActivoFalse() {
        User admin = TestFixtures.adminEmpresa();
        assertThat(evento.isActivo()).isTrue();
        when(repo.findById(eventoId)).thenReturn(Optional.of(evento));
        when(repo.save(evento)).thenReturn(evento);
        when(mapper.toResponse(evento)).thenReturn(resp);

        service.desactivar(eventoId, admin);

        assertThat(evento.isActivo()).isFalse();
        verify(repo).save(evento);
    }

    @Test
    void desactivar_yaInactivo_lanzaIllegalStateException() {
        evento.setActivo(false);
        User admin = TestFixtures.adminEmpresa();
        when(repo.findById(eventoId)).thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> service.desactivar(eventoId, admin))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("desactivado");
    }

    @Test
    void desactivar_global_lanzaUnauthorizedException() {
        CommunityEvent global = CommunityEvent.builder().eventoId(eventoId)
                .esGlobal(true).activo(true).titulo("Global").fechaInicio(OffsetDateTime.now()).build();
        User admin = TestFixtures.adminEmpresa();
        when(repo.findById(eventoId)).thenReturn(Optional.of(global));

        assertThatThrownBy(() -> service.desactivar(eventoId, admin))
                .isInstanceOf(UnauthorizedException.class);
    }
}

