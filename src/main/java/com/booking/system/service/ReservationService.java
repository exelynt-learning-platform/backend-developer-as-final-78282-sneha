package com.booking.system.service;

import com.booking.system.dto.ReservationRequest;
import com.booking.system.dto.ReservationResponse;
import com.booking.system.exception.AccessDeniedException;
import com.booking.system.exception.BadRequestException;
import com.booking.system.exception.ResourceNotFoundException;
import com.booking.system.model.*;
import com.booking.system.repository.ReservationRepository;
import com.booking.system.repository.ResourceRepository;
import com.booking.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Reservation service — enforces ownership rules:
 *   - ADMIN can see and manage all reservations
 *   - USER can only see and manage their own reservations
 * The authenticated user's identity always comes from the JWT, never from the request body.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    // ── Read Operations ───────────────────────────────────────────────────────

    /**
     * Get reservations with optional filtering by status, minPrice, maxPrice.
     * ADMIN sees all; USER sees only their own.
     */
    @Transactional(readOnly = true)
    public Page<ReservationResponse> getReservations(
            String username,
            boolean isAdmin,
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {

        if (isAdmin) {
            return getAdminFilteredReservations(status, minPrice, maxPrice, pageable);
        } else {
            User user = getUserOrThrow(username);
            return getUserFilteredReservations(user.getId(), status, minPrice, maxPrice, pageable);
        }
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long id, String username, boolean isAdmin) {
        Reservation reservation = findReservationOrThrow(id);
        checkOwnership(reservation, username, isAdmin);
        return ReservationResponse.fromEntity(reservation);
    }

    // ── Write Operations ──────────────────────────────────────────────────────

    @Transactional
    public ReservationResponse createReservation(ReservationRequest request, String username) {
        validateTimings(request);

        User user = getUserOrThrow(username); // identity from JWT, not from request
        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resource not found with id: " + request.getResourceId()));

        ReservationStatus status = request.getStatus() != null
                ? request.getStatus()
                : ReservationStatus.PENDING;

        Reservation reservation = Reservation.builder()
                .user(user)
                .resource(resource)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(status)
                .price(request.getPrice())
                .build();

        Reservation saved = reservationRepository.save(reservation);
        log.info("Reservation created: id={}, user={}, resource={}",
                saved.getId(), username, resource.getName());
        return ReservationResponse.fromEntity(saved);
    }

    @Transactional
    public ReservationResponse updateReservation(Long id, ReservationRequest request,
                                                  String username, boolean isAdmin) {
        validateTimings(request);

        Reservation reservation = findReservationOrThrow(id);
        checkOwnership(reservation, username, isAdmin);

        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resource not found with id: " + request.getResourceId()));

        reservation.setResource(resource);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setPrice(request.getPrice());
        if (request.getStatus() != null) {
            reservation.setStatus(request.getStatus());
        }

        Reservation updated = reservationRepository.save(reservation);
        log.info("Reservation updated: id={}, by user={}", id, username);
        return ReservationResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteReservation(Long id, String username, boolean isAdmin) {
        Reservation reservation = findReservationOrThrow(id);
        checkOwnership(reservation, username, isAdmin);
        reservationRepository.delete(reservation);
        log.info("Reservation deleted: id={}, by user={}", id, username);
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private Page<ReservationResponse> getAdminFilteredReservations(
            ReservationStatus status, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {

        // Use Specification for flexible admin filtering
        return reservationRepository.findAll(
                ReservationSpecification.withFilters(null, status, minPrice, maxPrice),
                pageable
        ).map(ReservationResponse::fromEntity);
    }

    private Page<ReservationResponse> getUserFilteredReservations(
            Long userId, ReservationStatus status, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {

        return reservationRepository.findAll(
                ReservationSpecification.withFilters(userId, status, minPrice, maxPrice),
                pageable
        ).map(ReservationResponse::fromEntity);
    }

    private void checkOwnership(Reservation reservation, String username, boolean isAdmin) {
        if (!isAdmin && !reservation.getUser().getUsername().equals(username)) {
            throw new AccessDeniedException(
                    "You are not authorized to access this reservation");
        }
    }

    private void validateTimings(ReservationRequest request) {
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BadRequestException("End time must be after start time");
        }
    }

    private User getUserOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + username));
    }

    private Reservation findReservationOrThrow(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reservation not found with id: " + id));
    }
}
