package com.atalayas.backend.common.util;

import com.atalayas.backend.common.enums.RoleType;
import com.atalayas.backend.exception.UnauthorizedException;
import com.atalayas.backend.user.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Utilidad estática de acceso al SecurityContext.
 *
 * <p>Úsala en cualquier servicio que necesite el usuario autenticado
 * o su empresaId. No requiere inyección de dependencias.</p>
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * Devuelve el {@link User} autenticado del SecurityContext.
     *
     * @throws UnauthorizedException si no hay sesión activa o el principal no es un User.
     */
    public static User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof User user)) {
            throw new UnauthorizedException("No hay sesión activa");
        }
        return user;
    }

    /**
     * Devuelve el empresaId del usuario autenticado.
     *
     * @throws UnauthorizedException si no hay sesión activa.
     */
    public static UUID getEmpresaId() {
        return getCurrentUser().getEmpresaId();
    }

    /**
     * Devuelve {@code true} si el usuario autenticado tiene el rol SUPER_ADMIN.
     * SUPER_ADMIN opera sin restricción de empresa en todos los servicios.
     *
     * @throws UnauthorizedException si no hay sesión activa.
     */
    public static boolean isSuperAdmin() {
        return getCurrentUser().getRol().getRoleType() == RoleType.SUPER_ADMIN;
    }
}

