package com.taskora.backend.task.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.taskora.backend.task.TaskPriority;
import com.taskora.backend.task.TaskStatus;

public record TaskResponse(
        Long id,
        Long projectId,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        Long assigneeId,
        String assigneeEmail,
        String assigneeFirstName,
        String assigneeLastName,
        LocalDate dueDate,
        Instant createdAt,
        Instant updatedAt,
        List<SubtaskResponse> subtasks,
        List<CommentResponse> comments
) {
}
