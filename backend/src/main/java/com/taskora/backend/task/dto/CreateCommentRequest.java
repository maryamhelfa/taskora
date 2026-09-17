package com.taskora.backend.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank(message = "Le contenu du commentaire est obligatoire")
        @Size(max = 2000, message = "Le commentaire ne doit pas depasser 2000 caracteres")
        String content
) {
}
