package com.atalayas.backend.user.repository;

import com.atalayas.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);


    // ── CONSULTAR MULTI-TENANT ───────────────────────────────────────────────
    /**
     * Busca un usuario por su ID solo si pertenece a la empresa indicada
     * Devuelve vacío si el ID existe pero pertenece a otra empresa (enmascarado como 404)
     */
    Optional<User> findByUsuarioIdAndEmpresaId(UUID usuarioId, UUID empresaId);

    /**
     * Devuelve todos los usuarios de una empresa concreta
     * Usado por ADMIN_EMPRESA; ROLE_ADMIN usa findAll()
     */
    List<User> findAllByEmpresaId(UUID empresaId);

    /**
     * Devuelve los usuarios activos de una empresa
     * Usado al publicar un módulo para notificar a todos los empleados
     */
    List<User> findAllByEmpresaIdAndActivoTrue(UUID empresaId);

    /**
     * Devuelve los usuarios inactivos de una empresa
     * Usado al aprobar/rechazar una solicitud para activar o notificar al admin
     */
    List<User> findAllByEmpresaIdAndActivoFalse(UUID empresaId);


    // ── CONTEOS PARA EL DASHBOARD ────────────────────────────────────────────
    /** Usuarios activos de una empresa - resumen del admin de empresa */
    long countByEmpresaIdAndActivoTrue(UUID empresaId);

    /** Usuarios inactivos de una empresa - resumen del admin de empresa */
    long countByEmpresaIdAndActivoFalse(UUID empresaId);

    /** Usuarios cuya fecha de registro es posterior a la fecha dada — "nuevos este mes". */
    long countByFechaRegistroAfter(java.time.OffsetDateTime fecha);
}