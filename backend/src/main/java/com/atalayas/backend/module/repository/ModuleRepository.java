package com.atalayas.backend.module.repository;

import com.atalayas.backend.module.entity.TrainingModule;
import org.springframework.data.jpa.repository.JpaRepository;
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
}