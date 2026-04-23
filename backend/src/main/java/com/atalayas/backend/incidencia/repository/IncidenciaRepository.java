package com.atalayas.backend.incidencia.repository;
import com.atalayas.backend.incidencia.entity.Incidencia;
import com.atalayas.backend.incidencia.enums.EstadoIncidencia;
import com.atalayas.backend.incidencia.enums.PrioridadIncidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {
    /** Cuenta incidencias por estado — retorna 0 mientras la tabla está vacía. */
    long countByEstado(EstadoIncidencia estado);
    /** Cuenta incidencias por estado y prioridad. */
    long countByEstadoAndPrioridad(EstadoIncidencia estado, PrioridadIncidencia prioridad);
}
