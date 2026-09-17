package com.taskora.backend.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.taskora.backend.exception.ForbiddenActionException;
import com.taskora.backend.exception.ResourceNotFoundException;
import com.taskora.backend.project.Project;
import com.taskora.backend.task.dto.CreateSubtaskRequest;
import com.taskora.backend.task.dto.SubtaskResponse;
import com.taskora.backend.task.dto.UpdateSubtaskRequest;
import com.taskora.backend.team.Team;
import com.taskora.backend.team.TeamAccessService;
import com.taskora.backend.team.TeamMemberRepository;
import com.taskora.backend.user.User;

@ExtendWith(MockitoExtension.class)
class SubtaskServiceTest {

    @Mock
    private SubtaskRepository subtaskRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    private SubtaskService subtaskService;

    private SubtaskService newSubtaskService() {
        return new SubtaskService(subtaskRepository, taskRepository, new TeamAccessService(teamMemberRepository));
    }

    private User user(long id) {
        return User.builder().id(id).email("user" + id + "@doe.com").firstName("Jane").lastName("Doe").build();
    }

    private Task task(long id, Team team) {
        Project project = Project.builder().id(20L).team(team).name("Site web").build();
        return Task.builder().id(id).project(project).title("Ecrire les specs").build();
    }

    @Test
    void createSubtask_shouldSaveSubtask_whenUserIsTeamMember() {
        subtaskService = newSubtaskService();
        User user = user(1L);
        Team team = Team.builder().id(10L).name("Squad Alpha").owner(user).build();
        Task task = task(30L, team);
        CreateSubtaskRequest request = new CreateSubtaskRequest("Rediger le brief");

        when(taskRepository.findById(30L)).thenReturn(Optional.of(task));
        when(teamMemberRepository.existsByTeamIdAndUserId(10L, 1L)).thenReturn(true);

        SubtaskResponse response = subtaskService.createSubtask(user, 30L, request);

        assertThat(response.title()).isEqualTo("Rediger le brief");
        assertThat(response.done()).isFalse();
        assertThat(response.taskId()).isEqualTo(30L);
    }

    @Test
    void createSubtask_shouldThrowForbidden_whenUserIsNotTeamMember() {
        subtaskService = newSubtaskService();
        User user = user(1L);
        Team team = Team.builder().id(10L).name("Squad Alpha").owner(user).build();
        Task task = task(30L, team);
        CreateSubtaskRequest request = new CreateSubtaskRequest("Rediger le brief");

        when(taskRepository.findById(30L)).thenReturn(Optional.of(task));
        when(teamMemberRepository.existsByTeamIdAndUserId(10L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> subtaskService.createSubtask(user, 30L, request))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void createSubtask_shouldThrowNotFound_whenTaskDoesNotExist() {
        subtaskService = newSubtaskService();
        User user = user(1L);
        CreateSubtaskRequest request = new CreateSubtaskRequest("Rediger le brief");

        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subtaskService.createSubtask(user, 99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateSubtask_shouldMarkAsDone_whenUserIsTeamMember() {
        subtaskService = newSubtaskService();
        User user = user(1L);
        Team team = Team.builder().id(10L).name("Squad Alpha").owner(user).build();
        Task task = task(30L, team);
        Subtask subtask = Subtask.builder().id(40L).task(task).title("Rediger le brief").done(false).build();
        UpdateSubtaskRequest request = new UpdateSubtaskRequest(null, true);

        when(subtaskRepository.findById(40L)).thenReturn(Optional.of(subtask));
        when(teamMemberRepository.existsByTeamIdAndUserId(10L, 1L)).thenReturn(true);

        SubtaskResponse response = subtaskService.updateSubtask(user, 40L, request);

        assertThat(response.done()).isTrue();
        assertThat(response.title()).isEqualTo("Rediger le brief");
    }

}
