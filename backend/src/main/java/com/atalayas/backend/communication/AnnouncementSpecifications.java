package com.atalayas.backend.communication;

import com.atalayas.backend.communication.entity.Announcement;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AnnouncementSpecifications {

    private AnnouncementSpecifications() {}

    /**
     * Specification que replica la lógica de visibilidad del AnnouncementService:
     *  - superAdmin=true  → todos los activos de la plataforma
     *  - empresaId!=null  → activos de esa empresa + todos los globales activos
     *  - empresaId==null  → solo globales activos
     *
     * search: ILIKE sobre titulo (opcional)
     */
    public static Specification<Announcement> visible(UUID empresaId, boolean superAdmin, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filtro de visibilidad
            predicates.add(cb.isTrue(root.get("activo")));

            if (!superAdmin) {
                if (empresaId != null) {
                    predicates.add(cb.or(
                            cb.equal(root.get("empresaId"), empresaId),
                            cb.isTrue(root.get("esGlobal"))
                    ));
                } else {
                    predicates.add(cb.isTrue(root.get("esGlobal")));
                }
            }

            // Filtro de búsqueda opcional
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("titulo")), pattern));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

