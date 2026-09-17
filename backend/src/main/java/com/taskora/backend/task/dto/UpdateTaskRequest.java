package com.taskora.backend.task.dto;

import java.time.LocalDate;

import com.taskora.backend.task.TaskPriority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTaskRequest(
        @NotBlank(message = "Le titre de la tache est obligatoire")
        @Size(max = 200, message = "Le titre ne doit pas depasser 200 caracteres")
        String title,

        String description,

        @NotNull(message = "La priorite est obligatoire")
        TaskPriority priority,

        Long assigneeId,

        LocalDate dueDate
) {
}
