package com.taskora.backend.team.dto;

import java.time.Instant;

public record TeamResponse(
        Long id,
        String name,
        Long ownerId,
        Instant createdAt
) {
}
