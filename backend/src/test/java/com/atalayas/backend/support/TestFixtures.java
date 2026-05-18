package com.atalayas.backend.support;

import com.atalayas.backend.role.entity.Rol;
import com.atalayas.backend.user.entity.User;

import java.util.UUID;

/**
 * Helpers compartidos para construir objetos de dominio en tests unitarios.
 */
public final class TestFixtures {

    public static final UUID EMPRESA_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    public static final UUID EMPRESA_B = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");

    private TestFixtures() {}

    public static User superAdmin() {
        return User.builder()
                .usuarioId(UUID.randomUUID())
                .email("admin@egm.com")
                .nombre("Super").apellidos("Admin")
                .password("x")
                .empresaId(null)
                .rol(rol("ROLE_ADMIN", "Administrador"))
                .build();
    }

    public static User adminEmpresa() {
        return adminEmpresa(EMPRESA_A);
    }

    public static User adminEmpresa(UUID empresaId) {
        return User.builder()
                .usuarioId(UUID.randomUUID())
                .email("admin-empresa@test.com")
                .nombre("Admin").apellidos("Empresa")
                .password("x")
                .empresaId(empresaId)
                .rol(rol("ROLE_ADMIN_EMPRESA", "Admin Empresa"))
                .build();
    }

    public static User empleado() {
        return empleado(EMPRESA_A);
    }

    public static User empleado(UUID empresaId) {
        return User.builder()
                .usuarioId(UUID.randomUUID())
                .email("empleado@test.com")
                .nombre("Juan").apellidos("Empleado")
                .password("x")
                .empresaId(empresaId)
                .rol(rol("ROLE_EMPLEADO", "Empleado"))
                .build();
    }

    private static Rol rol(String codigo, String nombre) {
        return Rol.builder()
                .rolId(UUID.randomUUID())
                .codigoRol(codigo)
                .nombreRol(nombre)
                .build();
    }
}

