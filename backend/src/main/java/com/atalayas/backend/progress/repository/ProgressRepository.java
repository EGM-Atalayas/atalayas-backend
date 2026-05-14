package com.atalayas.backend.progress.repository;

import com.atalayas.backend.dashboard.dto.GrupoProjection;
import com.atalayas.backend.dashboard.dto.ProgressEventProjection;
import com.atalayas.backend.progress.entity.UserProgress;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // Versión paginada para el panel de admin empresa
    Page<UserProgress> findByEmpresaIdOrderByActualizadoEnDesc(UUID empresaId, Pageable pageable);

    // Progreso de un empleado filtrado por empresa (seguridad cross-company)
    List<UserProgress> findByUsuarioIdAndEmpresaId(UUID usuarioId, UUID empresaId);

    // Cuántos contenidos ha completado un empleado en su empresa
    long countByUsuarioIdAndEmpresaIdAndCompletadoTrue(UUID usuarioId, UUID empresaId);

    // Cuántos contenidos de un módulo concreto ha completado un empleado (para saber si terminó el módulo)
    long countByUsuarioIdAndModuloIdAndCompletadoTrue(UUID usuarioId, UUID moduloId);

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


    // ── ACTIVIDAD RECIENTE (dashboard admin empresa) ──────────────────────────

    /** Completaciones individuales de contenido más recientes de la empresa. */
    @Query(value = """
        SELECT
            p.usuario_id      AS usuario_id,
            p.modulo_id       AS modulo_id,
            CONCAT(u.nombre, ' ', u.apellidos) AS nombre_usuario,
            m.nombre          AS nombre_modulo,
            p.fecha_completado AS fecha
        FROM trazabilidad_lectura p
        JOIN usuario u ON u.usuario_id = p.usuario_id
        JOIN modulo  m ON m.modulo_id  = p.modulo_id
        WHERE p.empresa_id = :empresaId
          AND p.completado = true
          AND p.fecha_completado IS NOT NULL
        ORDER BY p.fecha_completado DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<ProgressEventProjection> findCompletadosRecientes(
            @Param("empresaId") UUID empresaId,
            @Param("limit") int limit);

    /** Contenidos iniciados (no completados) más recientes de la empresa. */
    @Query(value = """
        SELECT
            p.usuario_id AS usuario_id,
            p.modulo_id  AS modulo_id,
            CONCAT(u.nombre, ' ', u.apellidos) AS nombre_usuario,
            m.nombre     AS nombre_modulo,
            p.fecha_inicio AS fecha
        FROM trazabilidad_lectura p
        JOIN usuario u ON u.usuario_id = p.usuario_id
        JOIN modulo  m ON m.modulo_id  = p.modulo_id
        WHERE p.empresa_id = :empresaId
          AND p.completado = false
          AND p.tiempo_segundos > 0
          AND p.fecha_inicio IS NOT NULL
        ORDER BY p.fecha_inicio DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<ProgressEventProjection> findIniciadosRecientes(
            @Param("empresaId") UUID empresaId,
            @Param("limit") int limit);

    /**
     * Logros: empleado que completó TODOS los contenidos activos de un módulo.
     * Se agrupa por (usuario, módulo) y se filtra con HAVING COUNT = total contenidos.
     */
    @Query(value = """
        SELECT
            p.usuario_id AS usuario_id,
            p.modulo_id  AS modulo_id,
            CONCAT(u.nombre, ' ', u.apellidos) AS nombre_usuario,
            m.nombre     AS nombre_modulo,
            MAX(p.fecha_completado) AS fecha
        FROM trazabilidad_lectura p
        JOIN usuario u ON u.usuario_id = p.usuario_id
        JOIN modulo  m ON m.modulo_id  = p.modulo_id
        WHERE p.empresa_id = :empresaId
          AND p.completado = true
        GROUP BY p.usuario_id, p.modulo_id, u.nombre, u.apellidos, m.nombre
        HAVING COUNT(*) >= (
            SELECT COUNT(*) FROM contenido c
            WHERE c.modulo_id = p.modulo_id AND c.activo = true
        )
        AND (SELECT COUNT(*) FROM contenido c
             WHERE c.modulo_id = p.modulo_id AND c.activo = true) > 0
        ORDER BY MAX(p.fecha_completado) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<ProgressEventProjection> findLogros(
            @Param("empresaId") UUID empresaId,
            @Param("limit") int limit);

    /**
     * Grupo: ≥ 2 empleados completaron el mismo módulo en el mismo día.
     * Se agrupa por (módulo, día) y el timestamp es el más reciente del grupo.
     */
    @Query(value = """
        SELECT
            p.modulo_id  AS modulo_id,
            m.nombre     AS nombre_modulo,
            COUNT(DISTINCT p.usuario_id) AS cantidad,
            MAX(p.fecha_completado)      AS fecha
        FROM trazabilidad_lectura p
        JOIN modulo m ON m.modulo_id = p.modulo_id
        WHERE p.empresa_id = :empresaId
          AND p.completado = true
          AND p.fecha_completado IS NOT NULL
        GROUP BY p.modulo_id, m.nombre, CAST(p.fecha_completado AS DATE)
        HAVING COUNT(DISTINCT p.usuario_id) >= 2
        ORDER BY MAX(p.fecha_completado) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<GrupoProjection> findCompletadosGrupo(
            @Param("empresaId") UUID empresaId,
            @Param("limit") int limit);
}