package com.taskora.backend.project.dto;

import com.taskora.backend.project.ProjectStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
        @NotBlank(message = "Le nom du projet est obligatoire")
        @Size(max = 150, message = "Le nom du projet ne doit pas depasser 150 caracteres")
        String name,

        String description,

        @NotNull(message = "Le statut est obligatoire")
        ProjectStatus status
) {
}
