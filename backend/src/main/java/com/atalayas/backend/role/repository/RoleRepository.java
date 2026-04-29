package com.atalayas.backend.role.repository;

import com.atalayas.backend.role.entity.Rol;
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
public interface RoleRepository extends JpaRepository<Rol, UUID> {

    // Busca un rol por su código técnico, ej. "ROLE_ADMIN_EMPRESA"
    Optional<Rol> findByCodigoRol(String codigoRol);

    // Busca un rol por su nombre legible. ej. "Administrador General"
    Optional<Rol> findByNombreRol(String nombreRol);
}