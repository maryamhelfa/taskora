package com.taskora.backend.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSubtaskRequest(
        @NotBlank(message = "Le titre de la sous-tache est obligatoire")
        @Size(max = 200, message = "Le titre ne doit pas depasser 200 caracteres")
        String title
) {
}
