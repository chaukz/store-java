package com.chaukz.store.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * What an admin can submit when creating or editing ANY user, role
 * included. Only ever bound on /api/admin/users/** endpoints, which
 * SecurityConfig restricts to ROLE_ADMIN. Never expose this DTO - or
 * a `role` field on any DTO - on an endpoint a regular user can hit.
 */
public record AdminUserRequest(

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        // Optional on update: omit to leave the existing password unchanged.
        // Required on create - UserService checks that explicitly.
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        String phone,

        LocalDate dob,

        @NotNull(message = "Role is required")
        com.chaukz.store.model.enums.Role role
) {
}
