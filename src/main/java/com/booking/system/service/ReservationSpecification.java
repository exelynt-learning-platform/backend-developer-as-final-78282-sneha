package com.booking.system.service;

import com.booking.system.model.Reservation;
import com.booking.system.model.ReservationStatus;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specification for dynamic Reservation filtering.
 * Supports: userId (for USER scope), status, minPrice, maxPrice.
 */
public class ReservationSpecification {

    private ReservationSpecification() {}

    public static Specification<Reservation> withFilters(
            Long userId,
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Scope to a specific user (USER role), null means ADMIN (all users)
            if (userId != null) {
                predicates.add(criteriaBuilder.equal(root.get("user").get("id"), userId));
            }

            // Filter by status
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            // Filter by minimum price
            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            // Filter by maximum price
            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
