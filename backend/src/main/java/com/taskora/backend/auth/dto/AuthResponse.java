package com.taskora.backend.auth.dto;

public record AuthResponse(
        String token,
        Long userId,
        String email,
        String firstName,
        String lastName
) {
}
