package com.booking.system.controller;

import com.booking.system.dto.ReservationRequest;
import com.booking.system.dto.ReservationResponse;
import com.booking.system.model.ReservationStatus;
import com.booking.system.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Collection;

/**
 * Reservation endpoints (all require authentication):
 *   GET    /api/reservations            — ADMIN: all; USER: own only
 *   GET    /api/reservations/{id}       — ADMIN: any; USER: own only
 *   POST   /api/reservations            — Create reservation (userId from JWT)
 *   PUT    /api/reservations/{id}       — ADMIN: any; USER: own only
 *   DELETE /api/reservations/{id}       — ADMIN: any; USER: own only
 *
 * Filtering query params: status, minPrice, maxPrice
 * Pagination query params: page, size, sortBy, sortDir
 */
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "Reservation management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class ReservationController {

    private final ReservationService reservationService;

    @Operation(summary = "Get reservations (filtered, paginated)",
               description = "ADMIN sees all; USER sees only their own. Supports filtering by status, minPrice, maxPrice.")
    @GetMapping
    public ResponseEntity<Page<ReservationResponse>> getReservations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            Authentication authentication) {

        String username = authentication.getName();
        boolean isAdmin = hasRole(authentication.getAuthorities(), "ROLE_ADMIN");
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);

        Page<ReservationResponse> reservations = reservationService.getReservations(
                username, isAdmin, status, minPrice, maxPrice, pageable);

        return ResponseEntity.ok(reservations);
    }

    @Operation(summary = "Get reservation by ID",
               description = "ADMIN can access any reservation; USER can only access their own.")
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservationById(
            @PathVariable Long id,
            Authentication authentication) {

        String username = authentication.getName();
        boolean isAdmin = hasRole(authentication.getAuthorities(), "ROLE_ADMIN");
        return ResponseEntity.ok(reservationService.getReservationById(id, username, isAdmin));
    }

    @Operation(summary = "Create a reservation",
               description = "Creates a reservation. The owner is automatically set from the JWT token.")
    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody ReservationRequest request,
            Authentication authentication) {

        String username = authentication.getName(); // Identity from JWT
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservationService.createReservation(request, username));
    }

    @Operation(summary = "Update a reservation",
               description = "ADMIN can update any reservation; USER can only update their own.")
    @PutMapping("/{id}")
    public ResponseEntity<ReservationResponse> updateReservation(
            @PathVariable Long id,
            @Valid @RequestBody ReservationRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        boolean isAdmin = hasRole(authentication.getAuthorities(), "ROLE_ADMIN");
        return ResponseEntity.ok(
                reservationService.updateReservation(id, request, username, isAdmin));
    }

    @Operation(summary = "Delete a reservation",
               description = "ADMIN can delete any reservation; USER can only delete their own.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(
            @PathVariable Long id,
            Authentication authentication) {

        String username = authentication.getName();
        boolean isAdmin = hasRole(authentication.getAuthorities(), "ROLE_ADMIN");
        reservationService.deleteReservation(id, username, isAdmin);
        return ResponseEntity.noContent().build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean hasRole(Collection<? extends GrantedAuthority> authorities, String role) {
        return authorities.stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        if (sortBy != null && !sortBy.isBlank()) {
            Sort sort = sortDir.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();
            return PageRequest.of(page, size, sort);
        }
        return PageRequest.of(page, size);
    }
}
