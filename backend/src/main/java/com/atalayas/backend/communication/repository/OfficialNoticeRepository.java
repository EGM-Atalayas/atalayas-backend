package com.atalayas.backend.communication.repository;

import com.atalayas.backend.communication.entity.OfficialNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para la tabla 'comunicado'
 *
 * Las queries cubren dos casos de uso:
 *   1. Listado público  - solo comunicados activos y vigentes
 *   2. Listado de admin - histórico completo para gestión
 */
public interface OfficialNoticeRepository extends JpaRepository<OfficialNotice, UUID> {

    /**
     * Comunicados activos y vigentes ordenados del más reciente al más antiguo.
     * Vigente significa que no ha expirado o no tiene fecha de expiración.
     */
    @Query("""
            SELECT n FROM OfficialNotice n
            WHERE n.activo = true
              AND (n.fechaExpiracion IS NULL OR n.fechaExpiracion > CURRENT_TIMESTAMP)
            ORDER BY n.fechaPublicacion DESC
            """)
    List<OfficialNotice> findActivosVigentes();

    /**
     * Todos los comunicados sin ningún filtro, para el panel del superadmin.
     * Incluye expirados y desactivados para que pueda ver el historial completo.
     */
    List<OfficialNotice> findAllByOrderByFechaPublicacionDesc();
}