package com.taskora.backend.project;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taskora.backend.activity.ActivityActionType;
import com.taskora.backend.activity.ActivityLogService;
import com.taskora.backend.exception.ResourceNotFoundException;
import com.taskora.backend.project.dto.CreateProjectRequest;
import com.taskora.backend.project.dto.ProjectResponse;
import com.taskora.backend.project.dto.UpdateProjectRequest;
import com.taskora.backend.team.Team;
import com.taskora.backend.team.TeamAccessService;
import com.taskora.backend.team.TeamMemberRepository;
import com.taskora.backend.team.TeamRepository;
import com.taskora.backend.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamAccessService teamAccessService;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public List<ProjectResponse> listProjects(User currentUser, Long teamId) {
        List<Project> projects;
        if (teamId != null) {
            teamAccessService.requireMembership(teamId, currentUser.getId());
            projects = projectRepository.findAllByTeamId(teamId);
        } else {
            List<Long> teamIds = teamMemberRepository.findAllByUserId(currentUser.getId()).stream()
                    .map(membership -> membership.getTeam().getId())
                    .toList();
            projects = teamIds.isEmpty() ? List.of() : projectRepository.findAllByTeamIdIn(teamIds);
        }
        return projects.stream().map(this::toProjectResponse).toList();
    }

    public ProjectResponse createProject(User currentUser, CreateProjectRequest request) {
        teamAccessService.requireMembership(request.teamId(), currentUser.getId());
        Team team = teamRepository.getReferenceById(request.teamId());

        Project project = Project.builder()
                .team(team)
                .name(request.name())
                .description(request.description())
                .status(ProjectStatus.ACTIVE)
                .build();
        projectRepository.save(project);

        activityLogService.log(currentUser, project, null, ActivityActionType.PROJECT_CREATED,
                "Projet cree : " + project.getName());

        return toProjectResponse(project);
    }

    public ProjectResponse updateProject(User currentUser, Long projectId, UpdateProjectRequest request) {
        Project project = findProjectOrThrow(projectId);
        teamAccessService.requireMembership(project.getTeam().getId(), currentUser.getId());

        ProjectStatus previousStatus = project.getStatus();

        project.setName(request.name());
        project.setDescription(request.description());
        project.setStatus(request.status());

        activityLogService.log(currentUser, project, null, ActivityActionType.PROJECT_UPDATED,
                buildProjectUpdateDescription(project, previousStatus, request.status()));

        return toProjectResponse(project);
    }

    private String buildProjectUpdateDescription(Project project, ProjectStatus previousStatus, ProjectStatus newStatus) {
        if (newStatus == ProjectStatus.ARCHIVED && previousStatus != ProjectStatus.ARCHIVED) {
            return "Projet archive : " + project.getName();
        }
        if (previousStatus == ProjectStatus.ARCHIVED && newStatus != ProjectStatus.ARCHIVED) {
            return "Projet reactive : " + project.getName();
        }
        return "Projet modifie : " + project.getName();
    }

    public void deleteProject(User currentUser, Long projectId) {
        Project project = findProjectOrThrow(projectId);
        teamAccessService.requireMembership(project.getTeam().getId(), currentUser.getId());
        projectRepository.delete(project);
    }

    private Project findProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable : " + projectId));
    }

    private ProjectResponse toProjectResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getTeam().getId(),
                project.getName(),
                project.getDescription(),
                project.getStatus(),
                project.getCreatedAt()
        );
    }

}
