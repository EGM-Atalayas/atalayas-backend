package com.atalayas.backend.role.repository;

import com.atalayas.backend.role.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para la tabla 'rol'.
 *
 * Se usa en el registro y alta de usuarios
 * para buscar el rol por código antes de asignarlo
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    // Busca un rol por su código técnico, ej. "ROLE_ADMIN_EMPRESA"
    Optional<Role> findByCodigoRol(String codigoRol);

    // Busca un rol por su nombre legible. ej. "Administrador General"
    Optional<Role> findByNombreRol(String nombreRol);
}