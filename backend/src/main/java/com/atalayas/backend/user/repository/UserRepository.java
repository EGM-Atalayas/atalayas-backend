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

    // ── Consultas multi-tenant ────────────────────────────────────────────

    /**
     * Busca un usuario por su ID solo si pertenece a la empresa indicada.
     * Devuelve vacío si el ID existe pero pertenece a otra empresa (enmascarado como 404).
     */
    Optional<User> findByUsuarioIdAndEmpresaId(UUID usuarioId, UUID empresaId);

    /**
     * Devuelve todos los usuarios de una empresa concreta.
     * Usado por ADMIN y EMPLEADO; SUPER_ADMIN usa findAll().
     */
    List<User> findAllByEmpresaId(UUID empresaId);

    /**
     * Devuelve los usuarios inactivos de una empresa.
     * Usado al aprobar/rechazar una solicitud para activar o notificar al admin.
     */
    List<User> findAllByEmpresaIdAndActivoFalse(UUID empresaId);
}
