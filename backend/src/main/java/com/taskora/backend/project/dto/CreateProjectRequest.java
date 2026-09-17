package com.taskora.backend.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotNull(message = "L'identifiant de l'equipe est obligatoire")
        Long teamId,

        @NotBlank(message = "Le nom du projet est obligatoire")
        @Size(max = 150, message = "Le nom du projet ne doit pas depasser 150 caracteres")
        String name,

        String description
) {
}
