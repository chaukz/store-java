package com.chaukz.store.controller;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * These tests exist specifically to prove the Phase 1 security fix:
 * a regular, self-registered user must NOT be able to read other
 * users' data, edit other users, or grant themselves an admin role.
 *
 * Same caveat as AuthControllerTest: full HTTP integration test,
 * needs a real Postgres connection, not an isolated test database.
 * Each test registers its own throwaway user with a random email so
 * runs don't collide with each other or with the seeded admin.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndGetToken() throws Exception {
        String email = "test-user-" + System.nanoTime() + "@example.com";
        String requestBody = """
                {
                  "firstName": "Test",
                  "lastName": "User",
                  "email": "%s",
                  "password": "password123",
                  "phone": "0000000000",
                  "dob": "2000-01-01"
                }
                """.formatted(email);

        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.get("token").asText();
    }

    @Test
    void register_neverProducesAnAdminAccount_evenIfRoleIsSmuggledIntoTheBody() throws Exception {
        String email = "sneaky-" + System.nanoTime() + "@example.com";
        // RegisterRequest has no `role` field, so this extra JSON property
        // has nowhere to bind to and is simply ignored.
        String requestBody = """
                {
                  "firstName": "Sneaky",
                  "lastName": "User",
                  "email": "%s",
                  "password": "password123",
                  "role": "ADMIN"
                }
                """.formatted(email);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void regularUser_cannotListAllUsers() throws Exception {
        String token = registerAndGetToken();

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void regularUser_cannotReadAnotherUsersProfileByGuessingAnId() throws Exception {
        String token = registerAndGetToken();

        mockMvc.perform(get("/api/admin/users/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void regularUser_canReadAndUpdateOnlyTheirOwnProfile() throws Exception {
        String token = registerAndGetToken();

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        String updateBody = """
                {
                  "firstName": "Updated",
                  "lastName": "Name",
                  "phone": "1111111111",
                  "dob": "2000-01-01"
                }
                """;

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"))
                // still USER - UserRequest has no role field for this to change
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void regularUser_updatingOwnProfile_doesNotRequireRetypingPassword() throws Exception {
        String token = registerAndGetToken();

        // No "password" field at all - should succeed, not 400.
        String updateBody = """
                {
                  "firstName": "NoPasswordChange",
                  "lastName": "User",
                  "phone": "2222222222",
                  "dob": "2000-01-01"
                }
                """;

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousCaller_cannotHitAnyUserEndpoint() throws Exception {
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
    }
}
