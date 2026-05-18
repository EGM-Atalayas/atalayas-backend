package com.atalayas.backend.rewards;

import com.atalayas.backend.exception.ResourceNotFoundException;
import com.atalayas.backend.exception.UnauthorizedException;
import com.atalayas.backend.rewards.dto.BenefitRequest;
import com.atalayas.backend.rewards.dto.BenefitResponse;
import com.atalayas.backend.rewards.entity.Benefit;
import com.atalayas.backend.rewards.mapper.BenefitMapper;
import com.atalayas.backend.rewards.repository.BenefitRepository;
import com.atalayas.backend.support.TestFixtures;
import com.atalayas.backend.usuario.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BenefitServiceTest {

    @Mock BenefitRepository repo;
    @Mock BenefitMapper mapper;
    @InjectMocks BenefitService service;

    private BenefitRequest req;
    private Benefit beneficio;
    private BenefitResponse resp;
    private final UUID beneficioId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        req = new BenefitRequest();
        req.setTitulo("Seguro médico");
        req.setDescripcion("Cobertura completa");

        beneficio = Benefit.builder()
                .beneficioId(beneficioId)
                .titulo("Seguro médico")
                .empresaId(TestFixtures.EMPRESA_A)
                .activo(true)
                .build();

        resp = BenefitResponse.builder()
                .beneficioId(beneficioId)
                .titulo("Seguro médico")
                .activo(true)
                .build();
    }

    // ── CREAR ────────────────────────────────────────────────────────────────

    @Test
    void crear_adminEmpresa_asignaEmpresaDelToken() {
        User admin = TestFixtures.adminEmpresa();
        when(mapper.toEntity(any(), any(), any())).thenReturn(beneficio);
        when(repo.save(beneficio)).thenReturn(beneficio);
        when(mapper.toResponse(beneficio)).thenReturn(resp);

        service.crear(req, admin);

        ArgumentCaptor<UUID> empresaCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(mapper).toEntity(eq(req), empresaCaptor.capture(), eq(admin.getUsuarioId()));
        assertThat(empresaCaptor.getValue()).isEqualTo(TestFixtures.EMPRESA_A);
    }

    @Test
    void crear_superAdmin_puedeCrearGlobal() {
        req.setEmpresaId(null);
        User admin = TestFixtures.superAdmin();
        Benefit global = Benefit.builder().beneficioId(UUID.randomUUID()).empresaId(null).activo(true).titulo("Global").build();
        BenefitResponse globalResp = BenefitResponse.builder().empresaId(null).build();

        when(mapper.toEntity(any(), isNull(), any())).thenReturn(global);
        when(repo.save(global)).thenReturn(global);
        when(mapper.toResponse(global)).thenReturn(globalResp);

        BenefitResponse result = service.crear(req, admin);

        assertThat(result.getEmpresaId()).isNull();
    }

    // ── LISTAR ───────────────────────────────────────────────────────────────

    @Test
    void listar_superAdmin_devuelveTodosActivos() {
        User admin = TestFixtures.superAdmin();
        when(repo.findByActivoTrueOrderByCreadoEnDesc()).thenReturn(List.of(beneficio));
        when(mapper.toResponse(beneficio)).thenReturn(resp);

        List<BenefitResponse> result = service.listar(admin);

        assertThat(result).hasSize(1);
        verify(repo).findByActivoTrueOrderByCreadoEnDesc();
    }

    @Test
    void listar_empleado_usaQueryVisibilidadEmpresa() {
        User emp = TestFixtures.empleado();
        when(repo.findVisiblesParaEmpresa(TestFixtures.EMPRESA_A)).thenReturn(List.of(beneficio));
        when(mapper.toResponse(beneficio)).thenReturn(resp);

        List<BenefitResponse> result = service.listar(emp);

        assertThat(result).hasSize(1);
        verify(repo).findVisiblesParaEmpresa(TestFixtures.EMPRESA_A);
    }

    @Test
    void listar_adminEmpresa_usaQueryVisibilidadEmpresa() {
        User admin = TestFixtures.adminEmpresa();
        when(repo.findVisiblesParaEmpresa(TestFixtures.EMPRESA_A)).thenReturn(List.of(beneficio));
        when(mapper.toResponse(beneficio)).thenReturn(resp);

        service.listar(admin);

        verify(repo).findVisiblesParaEmpresa(TestFixtures.EMPRESA_A);
    }

    // ── ACTUALIZAR ───────────────────────────────────────────────────────────

    @Test
    void actualizar_adminEmpresaPropio_actualizaYGuarda() {
        User admin = TestFixtures.adminEmpresa();
        when(repo.findById(beneficioId)).thenReturn(Optional.of(beneficio));
        when(repo.save(beneficio)).thenReturn(beneficio);
        when(mapper.toResponse(beneficio)).thenReturn(resp);

        req.setTitulo("Nuevo titulo");
        service.actualizar(beneficioId, req, admin);

        verify(repo).save(beneficio);
        assertThat(beneficio.getTitulo()).isEqualTo("Nuevo titulo");
    }

    @Test
    void actualizar_noExiste_lanzaResourceNotFoundException() {
        when(repo.findById(beneficioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(beneficioId, req, TestFixtures.adminEmpresa()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void actualizar_beneficioGlobal_lanzaUnauthorizedException() {
        User admin = TestFixtures.adminEmpresa();
        Benefit global = Benefit.builder().beneficioId(beneficioId).empresaId(null).activo(true).titulo("Global").build();
        when(repo.findById(beneficioId)).thenReturn(Optional.of(global));

        assertThatThrownBy(() -> service.actualizar(beneficioId, req, admin))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void actualizar_adminEmpresaOtraEmpresa_lanzaUnauthorizedException() {
        User admin = TestFixtures.adminEmpresa(TestFixtures.EMPRESA_B);
        when(repo.findById(beneficioId)).thenReturn(Optional.of(beneficio)); // beneficio es de EMPRESA_A

        assertThatThrownBy(() -> service.actualizar(beneficioId, req, admin))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ── DESACTIVAR ───────────────────────────────────────────────────────────

    @Test
    void desactivar_activoYPropio_ponActivoFalse() {
        User admin = TestFixtures.adminEmpresa();
        when(repo.findById(beneficioId)).thenReturn(Optional.of(beneficio));
        when(repo.save(beneficio)).thenReturn(beneficio);
        when(mapper.toResponse(beneficio)).thenReturn(resp);

        service.desactivar(beneficioId, admin);

        assertThat(beneficio.isActivo()).isFalse();
        verify(repo).save(beneficio);
    }

    @Test
    void desactivar_yaInactivo_lanzaIllegalStateException() {
        beneficio.setActivo(false);
        User admin = TestFixtures.adminEmpresa();
        when(repo.findById(beneficioId)).thenReturn(Optional.of(beneficio));

        assertThatThrownBy(() -> service.desactivar(beneficioId, admin))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("desactivado");
    }

    @Test
    void desactivar_beneficioGlobal_lanzaUnauthorizedException() {
        Benefit global = Benefit.builder().beneficioId(beneficioId).empresaId(null).activo(true).titulo("Global").build();
        User admin = TestFixtures.adminEmpresa();
        when(repo.findById(beneficioId)).thenReturn(Optional.of(global));

        assertThatThrownBy(() -> service.desactivar(beneficioId, admin))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void desactivar_superAdmin_puedeDesactivarCualquier() {
        User admin = TestFixtures.superAdmin();
        when(repo.findById(beneficioId)).thenReturn(Optional.of(beneficio));
        when(repo.save(beneficio)).thenReturn(beneficio);
        when(mapper.toResponse(beneficio)).thenReturn(resp);

        service.desactivar(beneficioId, admin);

        assertThat(beneficio.isActivo()).isFalse();
    }
}

