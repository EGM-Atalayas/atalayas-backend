package com.atalayas.backend.company.repository;

import com.atalayas.backend.common.enums.EstadoSolicitud;
import com.atalayas.backend.company.entity.Company;
import com.atalayas.backend.dashboard.dto.SectorDistribucionDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
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

    /** Total acumulado de empresas cuya fecha de solicitud es anterior a la fecha dada (evolución). */
    long countByFechaSolicitudBefore(OffsetDateTime fecha);

    /**
     * Distribución de empresas por sector para el gráfico de tarta.
     * Excluye empresas sin sector asignado o con sector vacío.
     */
    @Query("SELECT new com.atalayas.backend.dashboard.dto.SectorDistribucionDto(c.sector, COUNT(c)) " +
           "FROM Company c " +
           "WHERE c.sector IS NOT NULL AND c.sector <> '' " +
           "GROUP BY c.sector " +
           "ORDER BY COUNT(c) DESC")
    List<SectorDistribucionDto> findSectorDistribucion();
}
