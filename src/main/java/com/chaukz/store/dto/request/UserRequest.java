package com.chaukz.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

import java.time.LocalDate;

public record UserRequest(

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        String password,

        String phone,

        LocalDate dob,

        com.chaukz.store.model.enums.Role role
) {
}