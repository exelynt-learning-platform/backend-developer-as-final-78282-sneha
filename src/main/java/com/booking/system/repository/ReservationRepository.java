package com.booking.system.repository;

import com.booking.system.model.Reservation;
import com.booking.system.model.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long>,
        JpaSpecificationExecutor<Reservation> {

    // ADMIN: All reservations with filters
    Page<Reservation> findAll(Pageable pageable);

    // USER: Only their own reservations
    Page<Reservation> findByUserId(Long userId, Pageable pageable);

    Page<Reservation> findByUserIdAndStatus(Long userId, ReservationStatus status, Pageable pageable);

    Page<Reservation> findByUserIdAndPriceBetween(Long userId, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    Page<Reservation> findByUserIdAndStatusAndPriceBetween(
            Long userId, ReservationStatus status, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
}
