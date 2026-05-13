package com.atalayas.backend.company;

import com.atalayas.backend.common.enums.EstadoSolicitud;
import com.atalayas.backend.company.entity.Company;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class CompanySpecifications {

    private CompanySpecifications() {}

    /**
     * Specification para buscar empresas con filtros opcionales.
     * search: ILIKE sobre nombreEmpresa y cif (OR)
     * estado: igualdad sobre estadoSolicitud; null = todos los estados
     */
    public static Specification<Company> filtered(String search, EstadoSolicitud estado) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (estado != null) {
                predicates.add(cb.equal(root.get("estadoSolicitud"), estado));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("nombreEmpresa")), pattern),
                        cb.like(cb.lower(root.get("cif")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

