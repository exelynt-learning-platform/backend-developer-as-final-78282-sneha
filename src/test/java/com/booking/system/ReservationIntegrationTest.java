package com.booking.system;

import com.booking.system.dto.LoginRequest;
import com.booking.system.dto.ReservationRequest;
import com.booking.system.dto.ResourceRequest;
import com.booking.system.model.Resource;
import com.booking.system.model.Role;
import com.booking.system.model.User;
import com.booking.system.repository.ReservationRepository;
import com.booking.system.repository.ResourceRepository;
import com.booking.system.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Reservation endpoints — testing ownership, RBAC, filtering, and pagination.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@DisplayName("Reservation Integration Tests")
class ReservationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private ResourceRepository resourceRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String userToken;
    private String user2Token;
    private Resource testResource;
    private Long resourceId;

    @BeforeEach
    void setUp() throws Exception {
        reservationRepository.deleteAll();
        resourceRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(User.builder()
                .username("admin").password(passwordEncoder.encode("admin123"))
                .email("admin@test.com").role(Role.ROLE_ADMIN).build());

        userRepository.save(User.builder()
                .username("user1").password(passwordEncoder.encode("user123"))
                .email("user1@test.com").role(Role.ROLE_USER).build());

        userRepository.save(User.builder()
                .username("user2").password(passwordEncoder.encode("user123"))
                .email("user2@test.com").role(Role.ROLE_USER).build());

        testResource = resourceRepository.save(Resource.builder()
                .name("Test Room").description("A test room").type("room").available(true).build());
        resourceId = testResource.getId();

        adminToken = loginAndGetToken("admin", "admin123");
        userToken  = loginAndGetToken("user1", "user123");
        user2Token = loginAndGetToken("user2", "user123");
    }

    @Test
    @DisplayName("USER: Create reservation — userId set from JWT, not from body")
    void user_CreateReservation_UserIdFromJwt() throws Exception {
        ReservationRequest req = buildReservationRequest();

        MvcResult result = mockMvc.perform(post("/api/reservations")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("user1"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
    }

    @Test
    @DisplayName("USER: Cannot see other user's reservation — returns 403")
    void user_CannotAccessOtherUsersReservation_Returns403() throws Exception {
        // user1 creates a reservation
        ReservationRequest req = buildReservationRequest();
        MvcResult createResult = mockMvc.perform(post("/api/reservations")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long reservationId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();

        // user2 tries to access user1's reservation
        mockMvc.perform(get("/api/reservations/" + reservationId)
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN: Can access any user's reservation")
    void admin_CanAccessAnyReservation() throws Exception {
        // user1 creates a reservation
        ReservationRequest req = buildReservationRequest();
        MvcResult createResult = mockMvc.perform(post("/api/reservations")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long reservationId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();

        // ADMIN accesses it
        mockMvc.perform(get("/api/reservations/" + reservationId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservationId));
    }

    @Test
    @DisplayName("USER: GET /api/reservations only returns their own")
    void user_GetReservations_OnlyOwn() throws Exception {
        // user1 creates reservation
        mockMvc.perform(post("/api/reservations")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildReservationRequest())))
                .andExpect(status().isCreated());

        // user1 lists — should see 1 reservation owned by user1
        mockMvc.perform(get("/api/reservations")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].username").value("user1"));
    }

    @Test
    @DisplayName("ADMIN: GET /api/reservations returns all reservations")
    void admin_GetReservations_ReturnsAll() throws Exception {
        // user1 creates a reservation
        mockMvc.perform(post("/api/reservations")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildReservationRequest())))
                .andExpect(status().isCreated());

        // ADMIN sees all
        mockMvc.perform(get("/api/reservations")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/reservations - Filter by status works")
    void getReservations_FilterByStatus() throws Exception {
        mockMvc.perform(get("/api/reservations?status=PENDING")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("Reservation - Validation: end time before start time returns 400")
    void createReservation_EndBeforeStart_Returns400() throws Exception {
        ReservationRequest req = new ReservationRequest();
        req.setResourceId(resourceId);
        req.setStartTime(LocalDateTime.now().plusDays(2));
        req.setEndTime(LocalDateTime.now().plusDays(1)); // end < start
        req.setPrice(BigDecimal.valueOf(100.00));

        mockMvc.perform(post("/api/reservations")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Reservation - Validation: negative price returns 400")
    void createReservation_NegativePrice_Returns400() throws Exception {
        ReservationRequest req = new ReservationRequest();
        req.setResourceId(resourceId);
        req.setStartTime(LocalDateTime.now().plusDays(1));
        req.setEndTime(LocalDateTime.now().plusDays(2));
        req.setPrice(BigDecimal.valueOf(-50));

        mockMvc.perform(post("/api/reservations")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Pagination: page and size parameters work correctly")
    void getReservations_Pagination_Works() throws Exception {
        mockMvc.perform(get("/api/reservations?page=0&size=5")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.number").value(0));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private ReservationRequest buildReservationRequest() {
        ReservationRequest req = new ReservationRequest();
        req.setResourceId(resourceId);
        req.setStartTime(LocalDateTime.now().plusDays(1));
        req.setEndTime(LocalDateTime.now().plusDays(2));
        req.setPrice(BigDecimal.valueOf(150.00));
        return req;
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);

        MvcResult result = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }
}
