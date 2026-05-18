package com.atalayas.backend.documento.mapper;

import com.atalayas.backend.documento.dto.AsignacionDetalleResponse;
import com.atalayas.backend.documento.dto.DocumentoResponse;
import com.atalayas.backend.documento.entity.Documento;
import com.atalayas.backend.documento.entity.DocumentoAsignacion;
import com.atalayas.backend.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class DocumentoMapper {

    public DocumentoResponse toResponse(Documento d) {
        return DocumentoResponse.builder()
                .documentoId(d.getDocumentoId())
                .empresaId(d.getEmpresaId())
                .titulo(d.getTitulo())
                .descripcion(d.getDescripcion())
                .tipo(d.getTipo())
                .archivoUrl(d.getArchivoUrl())
                .archivoNombre(d.getArchivoNombre())
                .mimeType(d.getMimeType())
                .tamanoBytes(d.getTamanoBytes())
                .subidoPor(d.getSubidoPor())
                .requiereFirma(d.isRequiereFirma())
                .activo(d.isActivo())
                .fechaSubida(d.getFechaSubida())
                .build();
    }

    /** Versión enriquecida con los datos de la asignación al empleado actual */
    public DocumentoResponse toResponseConAsignacion(Documento d, DocumentoAsignacion a) {
        DocumentoResponse base = toResponse(d);
        base.setAsignacionId(a.getAsignacionId());
        base.setVisto(a.isVisto());
        base.setFechaVisto(a.getFechaVisto());
        base.setFirmado(a.isFirmado());
        base.setFechaFirma(a.getFechaFirma());
        base.setFirmaUrl(a.getFirmaUrl());
        return base;
    }

    public AsignacionDetalleResponse toAsignacionDetalle(DocumentoAsignacion a, User user) {
        return AsignacionDetalleResponse.builder()
                .asignacionId(a.getAsignacionId())
                .usuarioId(a.getUsuarioId())
                .nombre(user != null ? user.getNombre() : null)
                .apellidos(user != null ? user.getApellidos() : null)
                .departamento(user != null ? user.getDepartamento() : null)
                .fechaAsignacion(a.getFechaAsignacion())
                .visto(a.isVisto())
                .fechaVisto(a.getFechaVisto())
                .firmado(a.isFirmado())
                .fechaFirma(a.getFechaFirma())
                .build();
    }
}
