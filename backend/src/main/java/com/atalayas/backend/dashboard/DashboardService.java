package com.atalayas.backend.dashboard;

import com.atalayas.backend.audit.service.AuditService;
import com.atalayas.backend.common.enums.EstadoSolicitud;
import com.atalayas.backend.common.util.SecurityUtils;
import com.atalayas.backend.company.entity.Company;
import com.atalayas.backend.company.repository.CompanyRepository;
import com.atalayas.backend.dashboard.dto.ActividadItemDto;
import com.atalayas.backend.dashboard.dto.ActividadRecienteDto;
import com.atalayas.backend.dashboard.dto.AdminEmpresaResumenResponse;
import com.atalayas.backend.dashboard.dto.DashboardChartsResponse;
import com.atalayas.backend.dashboard.dto.EvolucionMensualDto;
import com.atalayas.backend.dashboard.dto.GrupoProjection;
import com.atalayas.backend.dashboard.dto.ModuloEstadisticaDto;
import com.atalayas.backend.dashboard.dto.NuevoModuloProjection;
import com.atalayas.backend.dashboard.dto.ProgressEventProjection;
import com.atalayas.backend.dashboard.dto.SectorDistribucionDto;
import com.atalayas.backend.dashboard.dto.SuperAdminDashboardResponse;
import com.atalayas.backend.dashboard.dto.SuperAdminResumenResponse;
import com.atalayas.backend.exception.ResourceNotFoundException;
import com.atalayas.backend.incidencia.enums.EstadoIncidencia;
import com.atalayas.backend.incidencia.enums.PrioridadIncidencia;
import com.atalayas.backend.incidencia.repository.IncidenciaRepository;
import com.atalayas.backend.module.repository.ModuleRepository;
import com.atalayas.backend.progress.repository.ProgressRepository;
import com.atalayas.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CompanyRepository    companyRepository;
    private final UserRepository       userRepository;
    private final ModuleRepository     moduleRepository;
    private final IncidenciaRepository incidenciaRepository;
    private final AuditService         auditService;
    private final ProgressRepository   progressRepository;

    /**
     * Resumen para ROLE_ADMIN_EMPRESA.
     * Lanza ResourceNotFoundException (404) si la empresa del usuario no existe en BD.
     */
    @Transactional(readOnly = true)
    public AdminEmpresaResumenResponse getAdminEmpresaResumen() {
        UUID empresaId = SecurityUtils.getEmpresaId();

        Company empresa = companyRepository.findById(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Empresa no encontrada con id: " + empresaId));

        long activos   = userRepository.countByEmpresaIdAndActivoTrue(empresaId);
        long inactivos = userRepository.countByEmpresaIdAndActivoFalse(empresaId);

        return AdminEmpresaResumenResponse.builder()
                .nombreEmpresa(empresa.getNombreEmpresa())
                .usuariosActivos(activos)
                .usuariosInactivos(inactivos)
                .build();
    }

    /**
     * Resumen para ROLE_ADMIN (superadmin EGM).
     * Agrega contadores globales de toda la plataforma.
     */
    @Transactional(readOnly = true)
    public SuperAdminResumenResponse getSuperAdminResumen() {
        long totalEmpresas      = companyRepository.count();
        long pendientes         = companyRepository.countByEstadoSolicitud(EstadoSolicitud.PENDIENTE);
        long aprobadas          = companyRepository.countByEstadoSolicitud(EstadoSolicitud.APROBADA);
        long totalUsuarios      = userRepository.count();

        return SuperAdminResumenResponse.builder()
                .totalEmpresas(totalEmpresas)
                .empresasPendientes(pendientes)
                .empresasAprobadas(aprobadas)
                .totalUsuarios(totalUsuarios)
                .build();
    }

    /**
     * Dashboard completo para ROLE_ADMIN (superadmin EGM).
     * GET /api/v1/dashboard/superadmin
     */
    @Transactional(readOnly = true)
    public SuperAdminDashboardResponse getSuperAdminDashboard(int limit) {
        // Primer instante del mes en curso con offset UTC
        OffsetDateTime inicioMes = OffsetDateTime.now()
                .withDayOfMonth(1)
                .truncatedTo(ChronoUnit.DAYS);

        long empresasAdheridas    = companyRepository.count();
        long empresasNuevasMes    = companyRepository.countByFechaSolicitudAfter(inicioMes);
        long empleadosRegistrados = userRepository.count();
        long empleadosNuevosMes   = userRepository.countByFechaRegistroAfter(inicioMes);
        long modulosPublicados    = moduleRepository.countByActivoTrue();
        long incidenciasAbiertas  = incidenciaRepository.countByEstado(EstadoIncidencia.ABIERTA);
        long incidenciasCriticas  = incidenciaRepository.countByEstadoAndPrioridad(
                                        EstadoIncidencia.ABIERTA, PrioridadIncidencia.CRITICA);
        List<ActividadRecienteDto> actividad = auditService.getActividadReciente(limit);

        return SuperAdminDashboardResponse.builder()
                .empresasAdheridas(empresasAdheridas)
                .empresasNuevasMes(empresasNuevasMes)
                .empleadosRegistrados(empleadosRegistrados)
                .empleadosNuevosMes(empleadosNuevosMes)
                .modulosPublicados(modulosPublicados)
                .incidenciasAbiertas(incidenciasAbiertas)
                .incidenciasCriticas(incidenciasCriticas)
                .actividadReciente(actividad)
                .build();
    }

    /**
     * Datos para los tres gráficos del dashboard del superadmin.
     * GET /api/v1/dashboard/superadmin/graficas
     *
     * - evolucion: totales acumulados mes a mes de empresas y empleados (últimos 6 meses).
     * - sectores:  distribución de empresas por sector (pie chart).
     * - modulos:   completados vs pendientes por módulo activo, top 10 (bar chart).
     */
    @Transactional(readOnly = true)
    public DashboardChartsResponse getDashboardCharts() {

        // ── EVOLUCIÓN (últimos 6 meses, del más antiguo al más reciente) ──────
        List<EvolucionMensualDto> evolucion = new ArrayList<>();
        OffsetDateTime ahora = OffsetDateTime.now();

        for (int i = 5; i >= 0; i--) {
            // Primer instante del mes analizado
            OffsetDateTime inicioMes = ahora.minusMonths(i)
                    .withDayOfMonth(1)
                    .truncatedTo(ChronoUnit.DAYS);
            // Primer instante del mes siguiente = límite exclusivo
            OffsetDateTime finMes = inicioMes.plusMonths(1);

            long empresas  = companyRepository.countByFechaSolicitudBefore(finMes);
            long empleados = userRepository.countByFechaRegistroBefore(finMes);

            // Abreviatura del mes en español con primera letra en mayúscula: "Ene", "Feb", …
            String mes = inicioMes.getMonth()
                    .getDisplayName(TextStyle.SHORT_STANDALONE, Locale.forLanguageTag("es"));
            mes = Character.toUpperCase(mes.charAt(0)) + mes.substring(1).toLowerCase();

            evolucion.add(new EvolucionMensualDto(mes, empresas, empleados));
        }

        // ── SECTORES ──────────────────────────────────────────────────────────
        List<SectorDistribucionDto> sectores = companyRepository.findSectorDistribucion();

        // ── MÓDULOS ───────────────────────────────────────────────────────────
        List<ModuloEstadisticaDto> modulos = moduleRepository.findModuloEstadisticas()
                .stream()
                .map(p -> new ModuloEstadisticaDto(
                        p.getNombre(),
                        p.getCompletados() != null ? p.getCompletados() : 0L,
                        p.getPendientes()  != null ? p.getPendientes()  : 0L))
                .collect(Collectors.toList());

        return DashboardChartsResponse.builder()
                .evolucion(evolucion)
                .sectores(sectores)
                .modulos(modulos)
                .build();
    }

    /**
     * Actividad reciente para ROLE_ADMIN_EMPRESA.
     * GET /api/v1/dashboard/admin/actividad?limit=5
     *
     * Agrega 5 tipos de eventos desde trazabilidad_lectura y modulo,
     * todos filtrados por empresaId del token. Devuelve los `limit`
     * eventos más recientes ordenados por timestamp DESC.
     *
     * Deduplicación: si un (usuario, módulo) genera un logro (100%),
     * sus entradas individuales de tipo "completado" para ese mismo
     * módulo se omiten para evitar ruido en el feed.
     */
    @Transactional(readOnly = true)
    public List<ActividadItemDto> getActividadEmpresa(int limit) {
        UUID empresaId = SecurityUtils.getEmpresaId();

        // ── 1. Logros (100% módulo) ──────────────────────────────────────────
        List<ProgressEventProjection> logros =
                progressRepository.findLogros(empresaId, limit);

        // Claves (usuarioId:moduloId) para deduplicar completados individuales
        Set<String> logroKeys = logros.stream()
                .map(l -> l.getUsuarioId() + ":" + l.getModuloId())
                .collect(Collectors.toSet());

        // ── 2. Completados individuales (excluyendo los que ya son logro) ────
        List<ProgressEventProjection> completados =
                progressRepository.findCompletadosRecientes(empresaId, limit)
                        .stream()
                        .filter(c -> !logroKeys.contains(c.getUsuarioId() + ":" + c.getModuloId()))
                        .collect(Collectors.toList());

        // ── 3. Iniciados ─────────────────────────────────────────────────────
        List<ProgressEventProjection> iniciados =
                progressRepository.findIniciadosRecientes(empresaId, limit);

        // ── 4. Grupos (≥ 2 empleados, mismo módulo y día) ───────────────────
        List<GrupoProjection> grupos =
                progressRepository.findCompletadosGrupo(empresaId, limit);

        // ── 5. Módulos nuevos ────────────────────────────────────────────────
        List<NuevoModuloProjection> nuevos =
                moduleRepository.findNuevosModulos(empresaId, limit);

        // ── Construir lista unificada ────────────────────────────────────────
        List<ActividadItemDto> actividad = new ArrayList<>();

        completados.forEach(c -> actividad.add(ActividadItemDto.builder()
                .tipo("completado")
                .texto(c.getNombreUsuario() + " completó «" + c.getNombreModulo() + "»")
                .timestamp(c.getFecha())
                .build()));

        iniciados.forEach(i -> actividad.add(ActividadItemDto.builder()
                .tipo("inicio")
                .texto(i.getNombreUsuario() + " inició «" + i.getNombreModulo() + "»")
                .timestamp(i.getFecha())
                .build()));

        logros.forEach(l -> actividad.add(ActividadItemDto.builder()
                .tipo("logro")
                .texto(l.getNombreUsuario() + " obtuvo el 100% en " + l.getNombreModulo())
                .timestamp(l.getFecha())
                .build()));

        grupos.forEach(g -> actividad.add(ActividadItemDto.builder()
                .tipo("grupo")
                .texto(g.getCantidad() + " empleados completaron «" + g.getNombreModulo() + "»")
                .timestamp(g.getFecha())
                .build()));

        nuevos.forEach(n -> actividad.add(ActividadItemDto.builder()
                .tipo("nuevo")
                .texto("Nuevo módulo «" + n.getNombreModulo() + "» publicado")
                .timestamp(n.getFecha())
                .build()));

        // Ordenar por timestamp DESC y truncar al límite solicitado
        return actividad.stream()
                .sorted(Comparator.comparing(ActividadItemDto::getTimestamp).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
}

