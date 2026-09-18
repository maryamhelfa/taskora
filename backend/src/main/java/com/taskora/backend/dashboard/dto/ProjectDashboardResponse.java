package com.taskora.backend.dashboard.dto;

import com.taskora.backend.project.ProjectStatus;

public record ProjectDashboardResponse(
        Long projectId,
        String projectName,
        Long teamId,
        ProjectStatus status,
        long todoCount,
        long inProgressCount,
        long doneCount,
        long overdueCount
) {
}
