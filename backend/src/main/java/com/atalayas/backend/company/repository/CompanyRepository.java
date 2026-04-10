package com.atalayas.backend.company.repository;

import com.atalayas.backend.common.enums.EstadoSolicitud;
import com.atalayas.backend.company.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {

    /** Todas las empresas con un estado concreto. */
    List<Company> findAllByEstadoSolicitud(EstadoSolicitud estadoSolicitud);

    /** Empresas aprobadas y activas — usada en el endpoint público del selector. */
    List<Company> findAllByEstadoSolicitudAndActivoTrue(EstadoSolicitud estadoSolicitud);

    /** Validación de unicidad de CIF antes de insertar. */
    boolean existsByCif(String cif);

    /** Conteo de empresas por estado — usado en el resumen del superadmin. */
    long countByEstadoSolicitud(EstadoSolicitud estadoSolicitud);

    /** Empresas cuya fecha de solicitud es posterior a la fecha dada — "nuevas este mes". */
    long countByFechaSolicitudAfter(java.time.OffsetDateTime fecha);
}
