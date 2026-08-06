package com.chaukz.store.dto.response;

public record AuthResponse(
        String token,
        Long userId,
        String email,
        com.chaukz.store.model.enums.Role role
) {}