package com.chaukz.store.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        LocalDate dob,
        com.chaukz.store.model.enums.Role role,
        LocalDateTime createdAt
) {
}