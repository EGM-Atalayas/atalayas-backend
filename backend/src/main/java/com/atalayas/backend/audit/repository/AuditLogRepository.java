package com.atalayas.backend.audit.repository;

import com.atalayas.backend.audit.entity.AuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /** Devuelve los 10 eventos más recientes, de más nuevo a más antiguo. */
    List<AuditLog> findTop10ByOrderByCreadoEnDesc();

    /** Devuelve los `limit` eventos más recientes usando paginación. */
    List<AuditLog> findAllByOrderByCreadoEnDesc(Pageable pageable);
}
