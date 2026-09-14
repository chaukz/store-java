package com.chaukz.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * What a logged-in user can submit to edit THEIR OWN profile, via
 * /api/users/me. No `role` field - self-service can never change a
 * user's role, only the admin-only AdminUserRequest can do that.
 * No `email` field either - changing your login email is a separate,
 * more sensitive flow (would need re-verification) and isn't wired
 * up yet; this DTO intentionally doesn't allow it by accident.
 * Password is optional: omit it (null) to leave the current password
 * unchanged, so editing your name doesn't force you to retype it.
 */
public record UserRequest(

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        String phone,

        LocalDate dob,

        @Size(min = 8, message = "Password must be at least 8 characters")
        String password
) {
}
