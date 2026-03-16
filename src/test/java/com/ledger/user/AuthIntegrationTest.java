package com.ledger.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledger.BaseIntegrationTest;
import com.ledger.config.JwtUtil;
import com.ledger.entity.User;
import com.ledger.entity.UserRole;
import com.ledger.repository.UserRepository;
import com.ledger.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for authentication and authorization.
 * Spec: 18-admin-configuration.md, BR-81, BR-82
 */
@AutoConfigureMockMvc
class AuthIntegrationTest extends BaseIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserService userService;

    @Autowired
    JwtUtil jwtUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        User admin = new User();
        admin.setUsername("admin");
        admin.setDisplayName("System Admin");
        admin.setEmail("admin@ledger.local");
        admin.setRole(UserRole.ADMIN);
        admin.setPasswordHash(passwordEncoder.encode("admin"));
        admin.setActive(true);
        userRepository.save(admin);
    }

    @Test
    void login_with_valid_credentials_returns_jwt_with_role_claim() throws Exception {
        // Spec: 18-admin-configuration.md, BR-80
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        Map<?, ?> responseMap = objectMapper.readValue(responseBody, Map.class);
        String token = (String) responseMap.get("token");

        // Parse JWT payload to verify role claim
        String extractedRole = jwtUtil.extractRole(token);
        assertThat(extractedRole).isEqualTo("ADMIN");
    }

    @Test
    void login_with_invalid_password_returns_401() throws Exception {
        // Spec: 18-admin-configuration.md, BR-81
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrongpassword\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deactivated_user_login_returns_401() throws Exception {
        // Spec: 18-admin-configuration.md, BR-81
        // Create second admin so we can deactivate the first
        User secondAdmin = new User();
        secondAdmin.setUsername("admin2");
        secondAdmin.setDisplayName("Admin Two");
        secondAdmin.setEmail("admin2@ledger.local");
        secondAdmin.setRole(UserRole.ADMIN);
        secondAdmin.setPasswordHash(passwordEncoder.encode("admin2"));
        secondAdmin.setActive(true);
        userRepository.save(secondAdmin);

        // Deactivate the primary admin
        User primaryAdmin = userRepository.findByUsername("admin").orElseThrow();
        userService.deactivate(primaryAdmin.getUserId());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void non_admin_calling_admin_users_returns_403() throws Exception {
        // Spec: 18-admin-configuration.md, BR-82
        // Create an ANALYST user
        User analyst = new User();
        analyst.setUsername("analyst1");
        analyst.setDisplayName("Analyst One");
        analyst.setEmail("analyst1@ledger.local");
        analyst.setRole(UserRole.ANALYST);
        analyst.setPasswordHash(passwordEncoder.encode("analystpass"));
        analyst.setActive(true);
        userRepository.save(analyst);

        // Login as analyst to get JWT token
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"analyst1\",\"password\":\"analystpass\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        Map<?, ?> responseMap = objectMapper.readValue(responseBody, Map.class);
        String token = (String) responseMap.get("token");

        // Try to access admin endpoint with analyst token
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_can_list_users() throws Exception {
        // Spec: 18-admin-configuration.md, BR-82 positive case
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        Map<?, ?> responseMap = objectMapper.readValue(responseBody, Map.class);
        String token = (String) responseMap.get("token");

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
