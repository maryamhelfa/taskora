package com.taskora.backend.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTeamRequest(
        @NotBlank(message = "Le nom de l'equipe est obligatoire")
        @Size(max = 150, message = "Le nom de l'equipe ne doit pas depasser 150 caracteres")
        String name
) {
}
