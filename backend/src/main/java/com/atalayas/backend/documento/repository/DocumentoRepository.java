package com.atalayas.backend.documento.repository;

import com.atalayas.backend.documento.entity.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, UUID> {

    List<Documento> findByEmpresaIdAndActivoTrueOrderByFechaSubidaDesc(UUID empresaId);

    List<Documento> findByEmpresaIdOrderByFechaSubidaDesc(UUID empresaId);
}
