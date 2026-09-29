package com.booking.system.controller;

import com.booking.system.dto.ResourceRequest;
import com.booking.system.dto.ResourceResponse;
import com.booking.system.service.ResourceService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Resource endpoints:
 *   GET    /api/resources         — authenticated (ADMIN + USER)
 *   GET    /api/resources/{id}    — authenticated (ADMIN + USER)
 *   POST   /api/resources         — ADMIN only
 *   PUT    /api/resources/{id}    — ADMIN only
 *   DELETE /api/resources/{id}    — ADMIN only
 */
@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
@Tag(name = "Resources", description = "Resource management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class ResourceController {

    private final ResourceService resourceService;

    @Operation(summary = "Get all resources (paginated)")
    @GetMapping
    public ResponseEntity<Page<ResourceResponse>> getAllResources(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        return ResponseEntity.ok(resourceService.getAllResources(pageable));
    }

    @Operation(summary = "Get resource by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ResourceResponse> getResourceById(@PathVariable Long id) {
        return ResponseEntity.ok(resourceService.getResourceById(id));
    }

    @Operation(summary = "Create a new resource (ADMIN only)")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResourceResponse> createResource(
            @Valid @RequestBody ResourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resourceService.createResource(request));
    }

    @Operation(summary = "Update a resource (ADMIN only)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResourceResponse> updateResource(
            @PathVariable Long id,
            @Valid @RequestBody ResourceRequest request) {
        return ResponseEntity.ok(resourceService.updateResource(id, request));
    }

    @Operation(summary = "Delete a resource (ADMIN only)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteResource(@PathVariable Long id) {
        resourceService.deleteResource(id);
        return ResponseEntity.noContent().build();
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
