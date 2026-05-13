package com.atalayas.backend.documento.repository;

import com.atalayas.backend.documento.entity.DocumentoAsignacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentoAsignacionRepository extends JpaRepository<DocumentoAsignacion, UUID> {

    List<DocumentoAsignacion> findByUsuarioIdOrderByFechaAsignacionDesc(UUID usuarioId);

    List<DocumentoAsignacion> findByDocumentoId(UUID documentoId);

    Optional<DocumentoAsignacion> findByDocumentoIdAndUsuarioId(UUID documentoId, UUID usuarioId);

    void deleteByDocumentoId(UUID documentoId);

    long countByDocumentoIdAndVistoTrue(UUID documentoId);

    long countByDocumentoIdAndFirmadoTrue(UUID documentoId);
}
