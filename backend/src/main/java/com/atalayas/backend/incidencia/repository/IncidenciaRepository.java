package com.atalayas.backend.incidencia.repository;
import com.atalayas.backend.incidencia.entity.Incidencia;
import com.atalayas.backend.incidencia.enums.EstadoIncidencia;
import com.atalayas.backend.incidencia.enums.PrioridadIncidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {
    long countByEstado(EstadoIncidencia estado);
    long countByEstadoAndPrioridad(EstadoIncidencia estado, PrioridadIncidencia prioridad);
    long countByEstadoIn(List<EstadoIncidencia> estados);
    long countByEstadoInAndPrioridad(List<EstadoIncidencia> estados, PrioridadIncidencia prioridad);
    List<Incidencia> findAllByOrderByCreadoEnDesc();
    List<Incidencia> findAllByEmpresaIdOrderByCreadoEnDesc(UUID empresaId);
}
