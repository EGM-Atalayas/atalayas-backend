package com.atalayas.backend.user;

import com.atalayas.backend.user.entity.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserSpecifications {

    private UserSpecifications() {}

    /**
     * Specification para buscar usuarios con filtros opcionales.
     * search: ILIKE sobre nombre, apellidos y email (OR)
     * empresaId: filtra por empresa; si es null no aplica el filtro (superadmin)
     */
    public static Specification<User> filtered(String search, UUID empresaId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (empresaId != null) {
                predicates.add(cb.equal(root.get("empresaId"), empresaId));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("nombre")), pattern),
                        cb.like(cb.lower(root.get("apellidos")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
