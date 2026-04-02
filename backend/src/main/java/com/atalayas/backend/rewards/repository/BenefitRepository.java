package com.atalayas.backend.rewards.repository;

import com.atalayas.backend.rewards.entity.Benefit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;


/**
 * Repositorio JPA para la tabla 'beneficio'
 * Mismo patrón de visibilidad que eventos y módulos:
 * empleado ve los de su empresa + los globales activos
 */
@Repository
public interface BenefitRepository extends JpaRepository<Benefit, UUID> {

    // Beneficios visibles para un empleado: los de su empresa + los globales
    @Query("""
        SELECT b FROM Benefit b
        WHERE b.activo = true
        AND (b.empresaId = :empresaId OR b.empresaId IS NULL)
        ORDER BY b.creadoEn DESC
    """)
    List<Benefit> findVisiblesParaEmpresa(@Param("empresaId") UUID empresaId);

    // Todos los beneficios activos de la plataforma (para superadmin)
    List<Benefit> findByActivoTrueOrderByCreadoEnDesc();

    // Beneficios de una empresa - activos e inactivos (para admin empresa)
    List<Benefit> findByEmpresaIdOrderByCreadoEnDesc(UUID empresaId);
}