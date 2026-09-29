package com.booking.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResourceRequest {

    @NotBlank(message = "Resource name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotBlank(message = "Resource type is required")
    @Size(max = 50, message = "Type must not exceed 50 characters")
    private String type;

    private Boolean available = true;
}
