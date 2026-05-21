package com.atalayas.backend.documento.repository;

import com.atalayas.backend.documento.entity.DocumentoAsignacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Comprueba si ya existe un certificado asignado a un usuario para un módulo concreto.
     * La descripción actúa como clave de deduplicación ("cert:modulo:{moduloId}").
     * Usa SQL nativo porque DocumentoAsignacion no tiene @ManyToOne mapeado a Documento.
     */
    @Query(value = """
        SELECT EXISTS (
            SELECT 1 FROM documento_asignacion a
            JOIN documento d ON d.documento_id = a.documento_id
            WHERE a.usuario_id  = :usuarioId
              AND d.empresa_id  = :empresaId
              AND d.tipo        = :tipo
              AND d.descripcion = :descripcion
        )
        """, nativeQuery = true)
    boolean existsCertificado(
            @Param("usuarioId")    UUID   usuarioId,
            @Param("empresaId")    UUID   empresaId,
            @Param("tipo")         String tipo,
            @Param("descripcion")  String descripcion);

    /**
     * Devuelve la URL del certificado de un módulo concreto asignado a un usuario.
     * Se usa para que el botón de descarga del frontend priorice la versión persistida.
     */
    @Query(value = """
        SELECT d.archivo_url FROM documento_asignacion a
        JOIN documento d ON d.documento_id = a.documento_id
        WHERE a.usuario_id  = :usuarioId
          AND d.descripcion = :descripcion
          AND d.tipo        = 'CERTIFICADO'
          AND d.activo      = true
        LIMIT 1
        """, nativeQuery = true)
    java.util.Optional<String> findCertificadoUrl(
            @Param("usuarioId")   UUID   usuarioId,
            @Param("descripcion") String descripcion);
}
