package com.taskora.backend.dashboard;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taskora.backend.dashboard.dto.DashboardResponse;
import com.taskora.backend.dashboard.dto.ProjectDashboardResponse;
import com.taskora.backend.project.Project;
import com.taskora.backend.project.ProjectRepository;
import com.taskora.backend.task.Task;
import com.taskora.backend.task.TaskRepository;
import com.taskora.backend.task.TaskStatus;
import com.taskora.backend.team.TeamAccessService;
import com.taskora.backend.team.TeamMemberRepository;
import com.taskora.backend.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamAccessService teamAccessService;

    public DashboardResponse getDashboard(User currentUser, Long teamId) {
        List<Project> projects = resolveProjects(currentUser, teamId);
        if (projects.isEmpty()) {
            return new DashboardResponse(List.of(), 0, 0);
        }

        List<Long> projectIds = projects.stream().map(Project::getId).toList();
        List<Task> tasks = taskRepository.findAllByProjectIdIn(projectIds);
        Map<Long, List<Task>> tasksByProject = tasks.stream()
                .collect(Collectors.groupingBy(task -> task.getProject().getId()));

        LocalDate today = LocalDate.now();
        List<ProjectDashboardResponse> items = projects.stream()
                .map(project -> toProjectDashboard(project, tasksByProject.getOrDefault(project.getId(), List.of()), today))
                .toList();

        long totalOverdue = items.stream().mapToLong(ProjectDashboardResponse::overdueCount).sum();

        return new DashboardResponse(items, tasks.size(), totalOverdue);
    }

    private List<Project> resolveProjects(User currentUser, Long teamId) {
        if (teamId != null) {
            teamAccessService.requireMembership(teamId, currentUser.getId());
            return projectRepository.findAllByTeamId(teamId);
        }
        List<Long> teamIds = teamMemberRepository.findAllByUserId(currentUser.getId()).stream()
                .map(membership -> membership.getTeam().getId())
                .toList();
        return teamIds.isEmpty() ? List.of() : projectRepository.findAllByTeamIdIn(teamIds);
    }

    private ProjectDashboardResponse toProjectDashboard(Project project, List<Task> tasks, LocalDate today) {
        long todo = tasks.stream().filter(task -> task.getStatus() == TaskStatus.TODO).count();
        long inProgress = tasks.stream().filter(task -> task.getStatus() == TaskStatus.IN_PROGRESS).count();
        long done = tasks.stream().filter(task -> task.getStatus() == TaskStatus.DONE).count();
        long overdue = tasks.stream()
                .filter(task -> task.getStatus() != TaskStatus.DONE)
                .filter(task -> task.getDueDate() != null && task.getDueDate().isBefore(today))
                .count();

        return new ProjectDashboardResponse(project.getId(), project.getName(), project.getTeam().getId(),
                project.getStatus(), todo, inProgress, done, overdue);
    }

}
