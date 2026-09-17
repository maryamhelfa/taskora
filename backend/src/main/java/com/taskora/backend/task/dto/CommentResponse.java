package com.taskora.backend.task.dto;

import java.time.Instant;

public record CommentResponse(
        Long id,
        Long taskId,
        Long authorId,
        String authorEmail,
        String authorFirstName,
        String authorLastName,
        String content,
        Instant createdAt
) {
}
