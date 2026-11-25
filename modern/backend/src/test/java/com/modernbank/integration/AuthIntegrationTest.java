package com.modernbank.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.modernbank.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbc;

    // BCrypt hash of "admin123"
    private static final String BCRYPT_ADMIN123 = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    void seedEmployee() {
        jdbc.update("DELETE FROM EMPLOYEES");
        jdbc.update("INSERT INTO EMPLOYEES (USERNAME, PASSWORD, EMPLOYEE_NAME, STATUS) VALUES (?, ?, ?, ?)",
                "admin", BCRYPT_ADMIN123, "Branch Administrator", "ACTIVE");
    }

    @Test
    void login_success_returnsToken() throws Exception {
        seedEmployee();
        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("admin123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.employeeName").value("Branch Administrator"));
    }

    @Test
    void login_wrongPassword_returns400() throws Exception {
        seedEmployee();
        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid username or password."));
    }

    @Test
    void login_inactiveEmployee_returns400() throws Exception {
        jdbc.update("DELETE FROM EMPLOYEES");
        jdbc.update("INSERT INTO EMPLOYEES (USERNAME, PASSWORD, EMPLOYEE_NAME, STATUS) VALUES (?, ?, ?, ?)",
                "inactive", BCRYPT_ADMIN123, "Inactive User", "INACTIVE");

        LoginRequest req = new LoginRequest();
        req.setUsername("inactive");
        req.setPassword("admin123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid username or password."));
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isUnauthorized());
    }
}
