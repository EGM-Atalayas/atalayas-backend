package com.atalayas.backend.progress.repository;

import com.atalayas.backend.progress.entity.UserProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


/**
 * Repositorio JPA para la tabla 'trazabilidad_lectura'
 *
 * Las queries cubren tres casos de uso principales:
 *   1. Empleado consulta su propio progreso
 *   2. Admin empresa consulta el progreso de su equipo
 *   3. Dashboard agrega métricas por empresa o módulo
 */
@Repository
public interface ProgressRepository extends JpaRepository<UserProgress, UUID> {

    // Progreso de un empleado en un contenido concreto (registro único)
    Optional<UserProgress> findByUsuarioIdAndContenidoId(UUID usuarioId, UUID contenidoId);

    // Todo el progreso de un empleado (para su panel personal)
    List<UserProgress> findByUsuarioIdOrderByActualizadoEnDesc(UUID usuarioId);

    // Todo el progreso de una empresa (para panel de admin empresa)
    List<UserProgress> findByEmpresaIdOrderByActualizadoEnDesc(UUID empresaId);

    // Progreso de un empleado filtrado por empresa (seguridad cross-company)
    List<UserProgress> findByUsuarioIdAndEmpresaId(UUID usuarioId, UUID empresaId);

    // Cuántos contenidos ha completado un empleado en su empresa
    long countByUsuarioIdAndEmpresaIdAndCompletadoTrue(UUID usuarioId, UUID empresaId);

    // Cuántos empleados han completado un contenido concreto (para dashboard)
    long countByContenidoIdAndCompletadoTrue(UUID contenidoId);

    // Progreso de todos los empleados sobre un contenido (para admin)
    List<UserProgress> findByContenidoId(UUID contenidoId);

    // Empleados que tienen contenido con versión desactualizada (necesitan releer)
    @Query("""
        SELECT p FROM UserProgress p
        WHERE p.empresaId = :empresaId
        AND p.completado = true
        AND p.versionLeida < (
            SELECT c.version FROM ContentItem c WHERE c.contenidoId = p.contenidoId
        )
    """)
    List<UserProgress> findConVersionDesactualizada(@Param("empresaId") UUID empresaId);
}