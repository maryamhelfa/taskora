package com.taskora.backend.dashboard.dto;

import java.util.List;

public record DashboardResponse(
        List<ProjectDashboardResponse> projects,
        long totalTasks,
        long totalOverdueTasks
) {
}
