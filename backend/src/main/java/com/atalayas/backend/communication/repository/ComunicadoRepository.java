package com.atalayas.backend.communication.repository;

import com.atalayas.backend.communication.entity.Comunicado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ComunicadoRepository extends JpaRepository<Comunicado, UUID> {

    /**
     * Todos los comunicados activos y vigentes ordenados del más reciente al más antiguo
     * Vigente = fecha_expiracion IS NULL OR fecha_expiracion > now()
     * Usado por todos los roles para el listado público de comunicados
     */
    @Query("""
            SELECT c FROM Comunicado c
            WHERE c.activo = true
              AND (c.fechaExpiracion IS NULL OR c.fechaExpiracion > CURRENT_TIMESTAMP)
            ORDER BY c.fechaPublicacion DESC
            """)
    List<Comunicado> findActivosVigentes();

    /**
     * Todos los comunicados sin filtro de fecha ni estado
     * Usado por ROLE_ADMIN para ver el histórico completo incluyendo expirados y desactivados
     */
    List<Comunicado> findAllByOrderByFechaPublicacionDesc();
}