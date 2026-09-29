package com.booking.system.controller;

import com.booking.system.dto.LoginRequest;
import com.booking.system.dto.LoginResponse;
import com.booking.system.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * POST /auth/login — authenticate and receive a JWT token.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "JWT-based authentication endpoints")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Login and receive JWT token",
               description = "Authenticate with username/password; returns a Bearer JWT token.")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
