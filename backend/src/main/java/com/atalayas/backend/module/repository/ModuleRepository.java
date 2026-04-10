package com.atalayas.backend.module.repository;

import com.atalayas.backend.dashboard.dto.ModuloEstadisticaProjection;
import com.atalayas.backend.module.entity.TrainingModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;


/**
 * Repositorio JPA para la tabla 'modulo'
 * Las queries cubren los tres casos de acceso principales:
 *   - Superadmin:      ve todo
 *   - Admin empresa:   ve sus módulos propios + los globales activos
 *   - Empleado:        ve módulos activos de su empresa + globales activos
 */
@Repository
public interface ModuleRepository extends JpaRepository<TrainingModule, UUID> {

    // Todos los módulos de una empresa (para admin empresa)
    List<TrainingModule> findByEmpresaIdOrderByOrdenAsc(UUID empresaId);

    // Módulos activos de una empresa (para empleados)
    List<TrainingModule> findByEmpresaIdAndActivoTrueOrderByOrdenAsc(UUID empresaId);

    // Módulos globales activos (empresaId = null) visibles para todos
    List<TrainingModule> findByEmpresaIdIsNullAndActivoTrueOrderByOrdenAsc();

    // Todos los módulos activos de la plataforma (para superadmin)
    List<TrainingModule> findByActivoTrueOrderByOrdenAsc();

    /** Conteo de módulos activos — usado en el dashboard del superadmin. */
    long countByActivoTrue();

    /**
     * Estadísticas de completado por módulo activo para el gráfico de barras.
     * Incluye módulos sin ningún progreso registrado (LEFT JOIN).
     * Limita a los 10 módulos con más actividad (completados DESC).
     */
    @Query(value = """
            SELECT m.nombre AS nombre,
                   COUNT(CASE WHEN p.completado = true THEN 1 END)  AS completados,
                   COUNT(CASE WHEN p.completado = false THEN 1 END) AS pendientes
            FROM modulo m
            LEFT JOIN trazabilidad_lectura p ON p.modulo_id = m.modulo_id
            WHERE m.activo = true
            GROUP BY m.modulo_id, m.nombre
            ORDER BY completados DESC
            LIMIT 10
            """, nativeQuery = true)
    List<ModuloEstadisticaProjection> findModuloEstadisticas();
}