package com.booking.system.service;

import com.booking.system.dto.ResourceRequest;
import com.booking.system.dto.ResourceResponse;
import com.booking.system.exception.ResourceNotFoundException;
import com.booking.system.model.Resource;
import com.booking.system.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resource service — full CRUD for ADMIN, read-only for USER (enforced at security layer).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceService {

    private final ResourceRepository resourceRepository;

    @Transactional(readOnly = true)
    public Page<ResourceResponse> getAllResources(Pageable pageable) {
        return resourceRepository.findAll(pageable)
                .map(ResourceResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public ResourceResponse getResourceById(Long id) {
        Resource resource = findResourceOrThrow(id);
        return ResourceResponse.fromEntity(resource);
    }

    @Transactional
    public ResourceResponse createResource(ResourceRequest request) {
        Resource resource = Resource.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .available(request.getAvailable() != null ? request.getAvailable() : true)
                .build();

        Resource saved = resourceRepository.save(resource);
        log.info("Resource created: id={}, name={}", saved.getId(), saved.getName());
        return ResourceResponse.fromEntity(saved);
    }

    @Transactional
    public ResourceResponse updateResource(Long id, ResourceRequest request) {
        Resource resource = findResourceOrThrow(id);

        resource.setName(request.getName());
        resource.setDescription(request.getDescription());
        resource.setType(request.getType());
        if (request.getAvailable() != null) {
            resource.setAvailable(request.getAvailable());
        }

        Resource updated = resourceRepository.save(resource);
        log.info("Resource updated: id={}", updated.getId());
        return ResourceResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteResource(Long id) {
        Resource resource = findResourceOrThrow(id);
        resourceRepository.delete(resource);
        log.info("Resource deleted: id={}", id);
    }

    private Resource findResourceOrThrow(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resource not found with id: " + id));
    }
}
