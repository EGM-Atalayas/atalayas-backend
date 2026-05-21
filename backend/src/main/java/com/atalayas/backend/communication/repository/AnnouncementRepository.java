package com.atalayas.backend.communication.repository;

import com.atalayas.backend.communication.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnnouncementRepository extends JpaRepository<Announcement, UUID>, JpaSpecificationExecutor<Announcement> {

    /**
     * Listado para ROLE_ADMIN_EMPRESA y ROLE_EMPLEADO:
     * anuncios activos de su empresa + todos los globales activos.
     */
    @Query("SELECT a FROM Announcement a WHERE a.activo = true AND (a.empresaId = :empresaId OR a.esGlobal = true)")
    List<Announcement> findVisiblesParaEmpresa(@Param("empresaId") UUID empresaId);

    /** Listado para ROLE_ADMIN: todos los anuncios activos de la plataforma. */
    List<Announcement> findAllByActivoTrue();

    /**
     * Fallback para usuarios sin empresa asignada (empresaId == null):
     * solo devuelve los anuncios globales activos.
     */
    List<Announcement> findAllByEsGlobalTrueAndActivoTrue();

    /**
     * Para validar propiedad antes de desactivar (ROLE_ADMIN_EMPRESA).
     * Solo devuelve resultado si el anuncio pertenece a esa empresa.
     */
    Optional<Announcement> findByAnuncioIdAndEmpresaId(UUID anuncioId, UUID empresaId);
}

