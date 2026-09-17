package com.taskora.backend.activity;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taskora.backend.activity.dto.ActivityLogResponse;
import com.taskora.backend.exception.ResourceNotFoundException;
import com.taskora.backend.project.Project;
import com.taskora.backend.project.ProjectRepository;
import com.taskora.backend.task.Task;
import com.taskora.backend.team.TeamAccessService;
import com.taskora.backend.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final ProjectRepository projectRepository;
    private final TeamAccessService teamAccessService;

    public void log(User actor, Project project, Task task, ActivityActionType actionType, String description) {
        ActivityLog entry = ActivityLog.builder()
                .project(project)
                .task(task)
                .user(actor)
                .actionType(actionType)
                .description(description)
                .build();
        activityLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<ActivityLogResponse> listForProject(User currentUser, Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable : " + projectId));
        teamAccessService.requireMembership(project.getTeam().getId(), currentUser.getId());

        return activityLogRepository.findAllByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    private ActivityLogResponse toResponse(ActivityLog entry) {
        User user = entry.getUser();
        Task task = entry.getTask();
        return new ActivityLogResponse(
                entry.getId(),
                entry.getProject() != null ? entry.getProject().getId() : null,
                task != null ? task.getId() : null,
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                entry.getActionType(),
                entry.getDescription(),
                entry.getCreatedAt()
        );
    }

}
