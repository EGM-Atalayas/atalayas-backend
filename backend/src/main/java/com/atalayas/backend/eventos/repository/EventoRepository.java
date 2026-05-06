package com.atalayas.backend.eventos.repository;

import com.atalayas.backend.eventos.entity.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventoRepository extends JpaRepository<Evento, UUID> {

    /** Todos los activos ordenados por fecha ascendente (próximos primero) */
    @Query("SELECT e FROM Evento e WHERE e.activo = true ORDER BY e.fecha ASC, e.horaInicio ASC NULLS LAST")
    List<Evento> findActivosOrdenados();
}
