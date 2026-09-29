package com.booking.system.service;

import com.booking.system.dto.LoginRequest;
import com.booking.system.dto.LoginResponse;
import com.booking.system.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Authentication service — delegates credential check to Spring AuthenticationManager,
 * then generates and returns a JWT token.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    public LoginResponse login(LoginRequest request) {
        // Spring Security validates credentials and throws BadCredentialsException on failure
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtils.generateToken(userDetails);
        String role = userDetails.getAuthorities().iterator().next().getAuthority();

        log.info("User '{}' logged in successfully with role '{}'", userDetails.getUsername(), role);

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .username(userDetails.getUsername())
                .role(role)
                .expiresIn(jwtUtils.getExpirationMs())
                .build();
    }
}
