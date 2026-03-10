package com.atalayas.backend.role.repository;

import com.atalayas.backend.common.enums.RoleType;
import com.atalayas.backend.role.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleType name);
}
