package com.atalayas.backend.common.enums;

/**
 * Códigos de rol conocidos del sistema.
 * Cada valor corresponde al campo codigo_rol de la tabla rol en BD.
 * Se usa con @Enumerated(EnumType.STRING) en la entidad Rol.
 */
public enum RoleType {

    /** Administrador global de la plataforma EGM. */
    SUPER_ADMIN,

    /** Administrador de una empresa cliente. */
    ADMIN,

    /** Empleado estándar de una empresa cliente. */
    EMPLEADO;

    /**
     * Resuelve un RoleType a partir del codigo_rol almacenado en BD.
     * Lanza IllegalArgumentException si el código no es reconocido.
     */
    public static RoleType fromCodigo(String codigoRol) {
        for (RoleType r : values()) {
            if (r.name().equalsIgnoreCase(codigoRol)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Código de rol desconocido: " + codigoRol);
    }
}
