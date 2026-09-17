package com.taskora.backend.task.dto;

public record SubtaskResponse(
        Long id,
        Long taskId,
        String title,
        boolean done
) {
}
