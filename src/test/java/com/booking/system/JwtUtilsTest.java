package com.booking.system;

import com.booking.system.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for JwtUtils — token generation, validation, extraction.
 */
@DisplayName("JwtUtils Unit Tests")
class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private static final String TEST_SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 86400000L);
    }

    @Test
    @DisplayName("generateToken returns non-blank token")
    void generateToken_ReturnsNonBlankToken() {
        UserDetails userDetails = buildUserDetails("alice", "ROLE_USER");
        String token = jwtUtils.generateToken(userDetails);
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
    }

    @Test
    @DisplayName("extractUsername returns correct username")
    void extractUsername_ReturnsCorrectUsername() {
        UserDetails userDetails = buildUserDetails("bob", "ROLE_ADMIN");
        String token = jwtUtils.generateToken(userDetails);
        assertThat(jwtUtils.extractUsername(token)).isEqualTo("bob");
    }

    @Test
    @DisplayName("validateToken returns true for valid token")
    void validateToken_ValidToken_ReturnsTrue() {
        UserDetails userDetails = buildUserDetails("charlie", "ROLE_USER");
        String token = jwtUtils.generateToken(userDetails);
        assertThat(jwtUtils.validateToken(token, userDetails)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid returns false for malformed token")
    void isTokenValid_MalformedToken_ReturnsFalse() {
        assertThat(jwtUtils.isTokenValid("this.is.invalid")).isFalse();
    }

    @Test
    @DisplayName("isTokenValid returns false for empty string")
    void isTokenValid_EmptyToken_ReturnsFalse() {
        assertThat(jwtUtils.isTokenValid("")).isFalse();
    }

    @Test
    @DisplayName("validateToken returns false for wrong user")
    void validateToken_WrongUser_ReturnsFalse() {
        UserDetails alice = buildUserDetails("alice", "ROLE_USER");
        UserDetails bob   = buildUserDetails("bob",   "ROLE_USER");
        String token = jwtUtils.generateToken(alice);
        assertThat(jwtUtils.validateToken(token, bob)).isFalse();
    }

    private UserDetails buildUserDetails(String username, String role) {
        return new User(username, "password",
                List.of(new SimpleGrantedAuthority(role)));
    }
}
