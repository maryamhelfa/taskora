package com.taskora.backend.task.dto;

import com.taskora.backend.task.TaskStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(
        @NotNull(message = "Le statut est obligatoire")
        TaskStatus status
) {
}
