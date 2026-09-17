package com.taskora.backend.activity.dto;

import java.time.Instant;

import com.taskora.backend.activity.ActivityActionType;

public record ActivityLogResponse(
        Long id,
        Long projectId,
        Long taskId,
        Long userId,
        String userFirstName,
        String userLastName,
        ActivityActionType actionType,
        String description,
        Instant createdAt
) {
}
