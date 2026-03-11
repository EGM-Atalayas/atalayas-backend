package com.atalayas.backend.role.repository;

import com.atalayas.backend.role.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Rol, UUID> {

    Optional<Rol> findByCodigoRol(String codigoRol);

    Optional<Rol> findByNombreRol(String nombreRol);
}
