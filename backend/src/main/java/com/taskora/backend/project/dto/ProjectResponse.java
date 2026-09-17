package com.taskora.backend.project.dto;

import java.time.Instant;

import com.taskora.backend.project.ProjectStatus;

public record ProjectResponse(
        Long id,
        Long teamId,
        String name,
        String description,
        ProjectStatus status,
        Instant createdAt
) {
}
