package com.atalayas.backend.moduloprogreso.repository;

import com.atalayas.backend.moduloprogreso.entity.ModuloProgreso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ModuloProgresoRepository extends JpaRepository<ModuloProgreso, UUID> {

    /** Progreso de un usuario en un módulo concreto (registro único). */
    Optional<ModuloProgreso> findByUsuarioIdAndModuloId(UUID usuarioId, UUID moduloId);

    /** Todos los progresos del usuario (para hidratar dashboard / listados). */
    List<ModuloProgreso> findByUsuarioIdOrderByActualizadoEnDesc(UUID usuarioId);

    /** Todos los progresos de una empresa (para dashboards admin). */
    List<ModuloProgreso> findByEmpresaIdOrderByActualizadoEnDesc(UUID empresaId);

    /** Cuántos módulos ha completado un usuario en su empresa. */
    long countByUsuarioIdAndEmpresaIdAndCompletadoTrue(UUID usuarioId, UUID empresaId);
}
