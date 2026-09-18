package com.taskora.backend.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.taskora.backend.dashboard.dto.DashboardResponse;
import com.taskora.backend.dashboard.dto.ProjectDashboardResponse;
import com.taskora.backend.exception.ForbiddenActionException;
import com.taskora.backend.project.Project;
import com.taskora.backend.project.ProjectRepository;
import com.taskora.backend.project.ProjectStatus;
import com.taskora.backend.task.Task;
import com.taskora.backend.task.TaskRepository;
import com.taskora.backend.task.TaskStatus;
import com.taskora.backend.team.Team;
import com.taskora.backend.team.TeamAccessService;
import com.taskora.backend.team.TeamMember;
import com.taskora.backend.team.TeamMemberRepository;
import com.taskora.backend.user.User;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    private DashboardService newService() {
        return new DashboardService(projectRepository, taskRepository, teamMemberRepository,
                new TeamAccessService(teamMemberRepository));
    }

    private User user(long id) {
        return User.builder().id(id).email("user" + id + "@doe.com").firstName("Jane").lastName("Doe").build();
    }

    @Test
    void getDashboard_shouldAggregateTaskCountsAndOverdue_forProjectsOfUserTeams() {
        DashboardService service = newService();
        User user = user(1L);
        Team team = Team.builder().id(10L).name("Squad Alpha").owner(user).build();
        Project project = Project.builder().id(5L).team(team).name("Site web").status(ProjectStatus.ACTIVE).build();
        TeamMember membership = TeamMember.builder().id(1L).team(team).user(user).build();

        LocalDate today = LocalDate.now();
        Task todoTask = Task.builder().id(1L).project(project).title("A").status(TaskStatus.TODO).build();
        Task inProgressTask = Task.builder().id(2L).project(project).title("B").status(TaskStatus.IN_PROGRESS).build();
        Task doneOverdueLooking = Task.builder().id(3L).project(project).title("C").status(TaskStatus.DONE)
                .dueDate(today.minusDays(5)).build();
        Task overdueTask = Task.builder().id(4L).project(project).title("D").status(TaskStatus.IN_PROGRESS)
                .dueDate(today.minusDays(1)).build();

        when(teamMemberRepository.findAllByUserId(1L)).thenReturn(List.of(membership));
        when(projectRepository.findAllByTeamIdIn(List.of(10L))).thenReturn(List.of(project));
        when(taskRepository.findAllByProjectIdIn(List.of(5L)))
                .thenReturn(List.of(todoTask, inProgressTask, doneOverdueLooking, overdueTask));

        DashboardResponse response = service.getDashboard(user, null);

        assertThat(response.totalTasks()).isEqualTo(4);
        assertThat(response.totalOverdueTasks()).isEqualTo(1);
        assertThat(response.projects()).hasSize(1);

        ProjectDashboardResponse projectStats = response.projects().get(0);
        assertThat(projectStats.projectId()).isEqualTo(5L);
        assertThat(projectStats.todoCount()).isEqualTo(1);
        assertThat(projectStats.inProgressCount()).isEqualTo(2);
        assertThat(projectStats.doneCount()).isEqualTo(1);
        assertThat(projectStats.overdueCount()).isEqualTo(1);
    }

    @Test
    void getDashboard_shouldReturnEmptyDashboard_whenUserHasNoTeam() {
        DashboardService service = newService();
        User user = user(1L);

        when(teamMemberRepository.findAllByUserId(1L)).thenReturn(List.of());

        DashboardResponse response = service.getDashboard(user, null);

        assertThat(response.projects()).isEmpty();
        assertThat(response.totalTasks()).isZero();
        assertThat(response.totalOverdueTasks()).isZero();
    }

    @Test
    void getDashboard_shouldFilterByTeamId_whenProvided() {
        DashboardService service = newService();
        User user = user(1L);
        Team team = Team.builder().id(10L).name("Squad Alpha").owner(user).build();
        Project project = Project.builder().id(5L).team(team).name("Site web").status(ProjectStatus.ACTIVE).build();

        when(teamMemberRepository.existsByTeamIdAndUserId(10L, 1L)).thenReturn(true);
        when(projectRepository.findAllByTeamId(10L)).thenReturn(List.of(project));
        when(taskRepository.findAllByProjectIdIn(List.of(5L))).thenReturn(List.of());

        DashboardResponse response = service.getDashboard(user, 10L);

        assertThat(response.projects()).hasSize(1);
        assertThat(response.projects().get(0).teamId()).isEqualTo(10L);
    }

    @Test
    void getDashboard_shouldThrowForbidden_whenUserIsNotMemberOfRequestedTeam() {
        DashboardService service = newService();
        User user = user(1L);

        when(teamMemberRepository.existsByTeamIdAndUserId(10L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> service.getDashboard(user, 10L))
                .isInstanceOf(ForbiddenActionException.class);
    }

}
