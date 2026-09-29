package com.booking.system;

import com.booking.system.dto.LoginRequest;
import com.booking.system.dto.ResourceRequest;
import com.booking.system.model.Role;
import com.booking.system.model.User;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Resource endpoints — verifying RBAC:
 *   ADMIN: full CRUD
 *   USER: read-only (GET only)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@DisplayName("Resource RBAC Integration Tests")
class ResourceIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private ResourceRepository resourceRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        resourceRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(User.builder()
                .username("admin").password(passwordEncoder.encode("admin123"))
                .email("admin@test.com").role(Role.ROLE_ADMIN).build());

        User regularUser = userRepository.save(User.builder()
                .username("user").password(passwordEncoder.encode("user123"))
                .email("user@test.com").role(Role.ROLE_USER).build());

        adminToken = loginAndGetToken("admin", "admin123");
        userToken = loginAndGetToken("user", "user123");
    }

    // ── ADMIN tests ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("ADMIN: GET /api/resources returns 200")
    void admin_GetResources_Returns200() throws Exception {
        mockMvc.perform(get("/api/resources")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("ADMIN: POST /api/resources creates resource and returns 201")
    void admin_CreateResource_Returns201() throws Exception {
        ResourceRequest req = new ResourceRequest();
        req.setName("Conference Room A");
        req.setDescription("A large conference room");
        req.setType("room");
        req.setAvailable(true);

        mockMvc.perform(post("/api/resources")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Conference Room A"))
                .andExpect(jsonPath("$.type").value("room"));
    }

    @Test
    @DisplayName("ADMIN: DELETE /api/resources/{id} returns 204")
    void admin_DeleteResource_Returns204() throws Exception {
        // Create first
        ResourceRequest req = new ResourceRequest();
        req.setName("Delete Me");
        req.setDescription("desc");
        req.setType("equipment");
        req.setAvailable(true);

        MvcResult createResult = mockMvc.perform(post("/api/resources")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();

        // Then delete
        mockMvc.perform(delete("/api/resources/" + id)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    // ── USER tests ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("USER: GET /api/resources returns 200")
    void user_GetResources_Returns200() throws Exception {
        mockMvc.perform(get("/api/resources")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("USER: POST /api/resources returns 403 Forbidden")
    void user_CreateResource_Returns403() throws Exception {
        ResourceRequest req = new ResourceRequest();
        req.setName("Forbidden Room");
        req.setDescription("Should fail");
        req.setType("room");
        req.setAvailable(true);

        mockMvc.perform(post("/api/resources")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("USER: PUT /api/resources/{id} returns 403 Forbidden")
    void user_UpdateResource_Returns403() throws Exception {
        ResourceRequest req = new ResourceRequest();
        req.setName("Updated");
        req.setType("room");

        mockMvc.perform(put("/api/resources/999")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("USER: DELETE /api/resources/{id} returns 403 Forbidden")
    void user_DeleteResource_Returns403() throws Exception {
        mockMvc.perform(delete("/api/resources/999")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/resources - Invalid data returns 400")
    void createResource_InvalidData_Returns400() throws Exception {
        // Name is blank — should fail validation
        mockMvc.perform(post("/api/resources")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"type\":\"room\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

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
