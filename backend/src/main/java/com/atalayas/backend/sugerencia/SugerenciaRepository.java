package com.atalayas.backend.sugerencia;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SugerenciaRepository extends JpaRepository<Sugerencia, UUID> {
    List<Sugerencia> findAllByOrderByCreadoEnDesc();
}
