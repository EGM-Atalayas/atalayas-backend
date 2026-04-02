package com.atalayas.backend.community.repository;

import com.atalayas.backend.community.entity.CommunityEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;


/**
 * Repositorio JPA para la tabla 'evento_comunidad'
 *
 * La query principal de listado combina eventos propios de la empresa
 * con los eventos globales — mismo patrón que anuncios y módulos
 */
@Repository
public interface CommunityEventRepository extends JpaRepository<CommunityEvent, UUID> {

    // Eventos visibles para un empleado, los de su empresa + los globales activos
    @Query("""
        SELECT e FROM CommunityEvent e
        WHERE e.activo = true
        AND (e.empresaId = :empresaId OR e.esGlobal = true)
        ORDER BY e.fechaInicio ASC
    """)
    List<CommunityEvent> findVisiblesParaEmpresa(@Param("empresaId") UUID empresaId);

    // Todos los eventos activos de la plataforma (para superadmin)
    List<CommunityEvent> findByActivoTrueOrderByFechaInicioAsc();

    // Todos los eventos de una empresa - activos e inactivos (para admin empresa)
    List<CommunityEvent> findByEmpresaIdOrderByFechaInicioAsc(UUID empresaId);
}