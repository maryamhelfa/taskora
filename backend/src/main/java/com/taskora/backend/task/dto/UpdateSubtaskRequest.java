package com.taskora.backend.task.dto;

import jakarta.validation.constraints.Size;

public record UpdateSubtaskRequest(
        @Size(max = 200, message = "Le titre ne doit pas depasser 200 caracteres")
        String title,

        Boolean done
) {
}
