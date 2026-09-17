package com.taskora.backend.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.taskora.backend.activity.dto.ActivityLogResponse;
import com.taskora.backend.exception.ForbiddenActionException;
import com.taskora.backend.exception.ResourceNotFoundException;
import com.taskora.backend.project.Project;
import com.taskora.backend.project.ProjectRepository;
import com.taskora.backend.team.Team;
import com.taskora.backend.team.TeamAccessService;
import com.taskora.backend.team.TeamMemberRepository;
import com.taskora.backend.user.User;

@ExtendWith(MockitoExtension.class)
class ActivityLogServiceTest {

    @Mock
    private ActivityLogRepository activityLogRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    private ActivityLogService activityLogService;

    private ActivityLogService newActivityLogService() {
        return new ActivityLogService(activityLogRepository, projectRepository, new TeamAccessService(teamMemberRepository));
    }

    private User user(long id) {
        return User.builder().id(id).email("user" + id + "@doe.com").firstName("Jane").lastName("Doe").build();
    }

    @Test
    void listForProject_shouldReturnEntries_whenUserIsTeamMember() {
        activityLogService = newActivityLogService();
        User user = user(1L);
        Team team = Team.builder().id(10L).name("Squad Alpha").owner(user).build();
        Project project = Project.builder().id(20L).team(team).name("Site web").build();
        ActivityLog entry = ActivityLog.builder()
                .id(50L)
                .project(project)
                .user(user)
                .actionType(ActivityActionType.PROJECT_CREATED)
                .description("Projet cree : Site web")
                .build();

        when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(10L, 1L)).thenReturn(true);
        when(activityLogRepository.findAllByProjectIdOrderByCreatedAtDesc(20L)).thenReturn(List.of(entry));

        List<ActivityLogResponse> response = activityLogService.listForProject(user, 20L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).actionType()).isEqualTo(ActivityActionType.PROJECT_CREATED);
        assertThat(response.get(0).projectId()).isEqualTo(20L);
    }

    @Test
    void listForProject_shouldThrowForbidden_whenUserIsNotTeamMember() {
        activityLogService = newActivityLogService();
        User user = user(1L);
        Team team = Team.builder().id(10L).name("Squad Alpha").owner(user).build();
        Project project = Project.builder().id(20L).team(team).name("Site web").build();

        when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(10L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> activityLogService.listForProject(user, 20L))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void listForProject_shouldThrowNotFound_whenProjectDoesNotExist() {
        activityLogService = newActivityLogService();
        User user = user(1L);

        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activityLogService.listForProject(user, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

}
