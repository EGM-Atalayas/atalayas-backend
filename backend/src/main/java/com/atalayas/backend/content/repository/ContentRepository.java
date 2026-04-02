package com.atalayas.backend.content.repository;

import com.atalayas.backend.content.entity.ContentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;


/**
 * Repositorio JPA para la tabla 'contenido'
 * Las queries principales filtran siempre por módulo y estado activo
 * para garantizar que empleados no ven contenido desactivado.
 */
@Repository
public interface ContentRepository extends JpaRepository<ContentItem, UUID> {

    // Contenidos activos de un módulo ordenados para el empleado
    List<ContentItem> findByModuloIdAndActivoTrueOrderByOrdenAsc(UUID moduloId);

    // Todos los contenidos de un módulo (activos e inactivos) para admin
    List<ContentItem> findByModuloIdOrderByOrdenAsc(UUID moduloId);

    // Contenidos de una empresa (para panel de admin empresa)
    List<ContentItem> findByEmpresaIdAndActivoTrueOrderByOrdenAsc(UUID empresaId);
}