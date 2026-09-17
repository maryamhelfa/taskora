package com.taskora.backend.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.taskora.backend.exception.ForbiddenActionException;
import com.taskora.backend.exception.ResourceNotFoundException;
import com.taskora.backend.project.dto.CreateProjectRequest;
import com.taskora.backend.project.dto.ProjectResponse;
import com.taskora.backend.project.dto.UpdateProjectRequest;
import com.taskora.backend.team.Team;
import com.taskora.backend.team.TeamAccessService;
import com.taskora.backend.team.TeamMemberRepository;
import com.taskora.backend.team.TeamRepository;
import com.taskora.backend.user.User;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    private ProjectService projectService;

    private ProjectService newProjectService() {
        return new ProjectService(projectRepository, teamRepository, teamMemberRepository, new TeamAccessService(teamMemberRepository));
    }

    private User user(long id) {
        return User.builder().id(id).email("user" + id + "@doe.com").firstName("Jane").lastName("Doe").build();
    }

    @Test
    void createProject_shouldSaveProject_whenUserIsTeamMember() {
        projectService = newProjectService();
        User user = user(1L);
        Team team = Team.builder().id(10L).name("Squad Alpha").owner(user).build();
        CreateProjectRequest request = new CreateProjectRequest(10L, "Site web", "Refonte du site");

        when(teamMemberRepository.existsByTeamIdAndUserId(10L, 1L)).thenReturn(true);
        when(teamRepository.getReferenceById(10L)).thenReturn(team);

        ProjectResponse response = projectService.createProject(user, request);

        assertThat(response.name()).isEqualTo("Site web");
        assertThat(response.teamId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(ProjectStatus.ACTIVE);
    }

    @Test
    void createProject_shouldThrowForbidden_whenUserIsNotTeamMember() {
        projectService = newProjectService();
        User user = user(1L);
        CreateProjectRequest request = new CreateProjectRequest(10L, "Site web", "Refonte du site");

        when(teamMemberRepository.existsByTeamIdAndUserId(10L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> projectService.createProject(user, request))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void updateProject_shouldApplyChanges_whenUserIsTeamMember() {
        projectService = newProjectService();
        User user = user(1L);
        Team team = Team.builder().id(10L).name("Squad Alpha").owner(user).build();
        Project project = Project.builder().id(5L).team(team).name("Site web").status(ProjectStatus.ACTIVE).build();
        UpdateProjectRequest request = new UpdateProjectRequest("Site web v2", "Nouvelle description", ProjectStatus.ARCHIVED);

        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(10L, 1L)).thenReturn(true);

        ProjectResponse response = projectService.updateProject(user, 5L, request);

        assertThat(response.name()).isEqualTo("Site web v2");
        assertThat(response.status()).isEqualTo(ProjectStatus.ARCHIVED);
    }

    @Test
    void updateProject_shouldThrowNotFound_whenProjectDoesNotExist() {
        projectService = newProjectService();
        User user = user(1L);
        UpdateProjectRequest request = new UpdateProjectRequest("Site web", null, ProjectStatus.ACTIVE);

        when(projectRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.updateProject(user, 5L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteProject_shouldThrowForbidden_whenUserIsNotTeamMember() {
        projectService = newProjectService();
        User user = user(1L);
        Team team = Team.builder().id(10L).name("Squad Alpha").owner(user).build();
        Project project = Project.builder().id(5L).team(team).name("Site web").status(ProjectStatus.ACTIVE).build();

        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(10L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> projectService.deleteProject(user, 5L))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void listProjects_shouldFilterByTeamId_whenProvided() {
        projectService = newProjectService();
        User user = user(1L);
        Team team = Team.builder().id(10L).name("Squad Alpha").owner(user).build();
        Project project = Project.builder().id(5L).team(team).name("Site web").status(ProjectStatus.ACTIVE).build();

        when(teamMemberRepository.existsByTeamIdAndUserId(10L, 1L)).thenReturn(true);
        when(projectRepository.findAllByTeamId(10L)).thenReturn(List.of(project));

        List<ProjectResponse> response = projectService.listProjects(user, 10L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).teamId()).isEqualTo(10L);
    }

}
