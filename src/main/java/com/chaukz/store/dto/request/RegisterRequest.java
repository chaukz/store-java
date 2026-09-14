package com.chaukz.store.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * What a member of the public can submit to create their own account.
 * Deliberately has no `role` field - self-registration always produces
 * a ROLE_CUSTOMER account. Granting any other role happens through the
 * admin-only endpoints (AdminUserRequest), never through this one.
 */
public record RegisterRequest(

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        String phone,

        LocalDate dob
) {
}
