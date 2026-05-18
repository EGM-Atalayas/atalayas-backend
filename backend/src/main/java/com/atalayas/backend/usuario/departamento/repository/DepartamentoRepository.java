package com.atalayas.backend.usuario.departamento.repository;

import com.atalayas.backend.usuario.departamento.entity.Departamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartamentoRepository extends JpaRepository<Departamento, UUID> {

    /** Todos los departamentos visibles para una empresa: los globales + los propios */
    List<Departamento> findByEmpresaIdIsNullAndActivoTrueOrEmpresaIdAndActivoTrue(UUID empresaId);

    Optional<Departamento> findByNombreIgnoreCaseAndActivoTrue(String nombre);
}
