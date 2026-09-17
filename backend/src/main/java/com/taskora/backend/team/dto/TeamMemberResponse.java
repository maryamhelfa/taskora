package com.taskora.backend.team.dto;

import java.time.Instant;

import com.taskora.backend.team.TeamRole;

public record TeamMemberResponse(
        Long id,
        Long teamId,
        Long userId,
        String email,
        String firstName,
        String lastName,
        TeamRole role,
        Instant createdAt
) {
}
