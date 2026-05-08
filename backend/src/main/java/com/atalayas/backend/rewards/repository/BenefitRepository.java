package com.atalayas.backend.rewards.repository;

import com.atalayas.backend.rewards.entity.Benefit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BenefitRepository extends JpaRepository<Benefit, UUID> {

    List<Benefit> findByActivoTrueOrderByCreadoEnDesc();

    @Query("SELECT b FROM Benefit b WHERE b.activo = true AND (b.empresaId = :empresaId OR b.empresaId IS NULL) ORDER BY b.creadoEn DESC")
    List<Benefit> findVisiblesParaEmpresa(@Param("empresaId") UUID empresaId);
}

