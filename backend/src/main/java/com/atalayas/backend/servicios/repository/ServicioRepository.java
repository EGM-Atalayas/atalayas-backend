package com.atalayas.backend.servicios.repository;

import com.atalayas.backend.servicios.entity.Servicio;
import com.atalayas.backend.servicios.enums.CategoriaServicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServicioRepository extends JpaRepository<Servicio, UUID> {

    /** Todos los activos, ordenados por categoría y luego por fecha de creación */
    List<Servicio> findByActivoTrueOrderByCategoriaAscCreadoEnDesc();

    /** Filtro por categoría */
    List<Servicio> findByActivoTrueAndCategoriaOrderByCreadoEnDesc(CategoriaServicio categoria);
}
