package com.taskora.backend.team.dto;

import com.taskora.backend.team.TeamRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AddTeamMemberRequest(
        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "L'email n'est pas valide")
        String email,

        TeamRole role
) {
}
