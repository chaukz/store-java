package com.chaukz.store.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full HTTP integration test - boots the real app, sends real requests
 * through MockMvc, checks the real response. This proves the whole chain
 * (routing, JSON parsing, Spring Security, JWT generation) works together,
 * not just that one class's logic is correct in isolation.
 *
 * Relies on AdminSeeder having created admin@store.com / admin123, and on
 * a real Postgres connection being available (same one requests.http uses) -
 * this is not an isolated test database.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void login_withValidSeededAdminCredentials_returnsTokenAndCorrectRole() throws Exception {
        String requestBody = """
                {
                  "email": "admin@store.com",
                  "password": "admin123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("admin@store.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void login_correctEmailWrongPassword_returns401WithStandardErrorShape() throws Exception {
        String requestBody = """
                {
                  "email": "admin@store.com",
                  "password": "definitelyNotTheRightPassword"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void login_emailThatDoesNotExist_returns401NotAStackTrace() throws Exception {
        String requestBody = """
                {
                  "email": "this-user-does-not-exist@example.com",
                  "password": "whatever123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_missingPasswordField_returns400ValidationError() throws Exception {
        String requestBody = """
                {
                  "email": "admin@store.com"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedRoute_withNoToken_returns401() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/admin/orders"))
                .andExpect(status().isUnauthorized());
    }
}
