package com.taskora.backend.task;

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
import com.taskora.backend.project.Project;
import com.taskora.backend.project.ProjectRepository;
import com.taskora.backend.task.dto.CreateTaskRequest;
import com.taskora.backend.task.dto.TaskResponse;
import com.taskora.backend.task.dto.UpdateTaskRequest;
import com.taskora.backend.task.dto.UpdateTaskStatusRequest;
import com.taskora.backend.team.Team;
import com.taskora.backend.team.TeamAccessService;
import com.taskora.backend.team.TeamMemberRepository;
import com.taskora.backend.user.User;
import com.taskora.backend.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private SubtaskRepository subtaskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    private TaskService taskService;

    private TaskService newTaskService() {
        return new TaskService(taskRepository, subtaskRepository, projectRepository, userRepository,
                new TeamAccessService(teamMemberRepository));
    }

    private User user(long id) {
        return User.builder().id(id).email("user" + id + "@doe.com").firstName("Jane").lastName("Doe").build();
    }

    private Project project(long id, Team team) {
        return Project.builder().id(id).team(team).name("Site web").build();
    }

    @Test
    void createTask_shouldSaveTask_whenUserIsTeamMember() {
        taskService = newTaskService();
        User user = user(1L);
        Team team = Team.builder().id(10L).name("Squad Alpha").owner(user).build();
        Project project = project(20L, team);
        CreateTaskRequest request = new CreateTaskRequest(20L, "Ecrire les specs", null, null, null, null);

        when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(10L, 1L)).thenReturn(true);
        when(subtaskRepository.findAllByTaskId(null)).thenReturn(List.of());

        TaskResponse response = taskService.createTask(user, request);

        assertThat(response.title()).isEqualTo("Ecrire les specs");
        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
        assertThat(response.priority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(response.projectId()).isEqualTo(20L);
    }

    @Test
    void createTask_shouldThrowForbidden_whenUserIsNotTeamMember() {
        taskService = newTaskService();
        User user = user(1L);
        Team team = Team.builder().id(10L).name("Squad Alpha").owner(user).build();
        Project project = project(20L, team);
        CreateTaskRequest request = new CreateTaskRequest(20L, "Ecrire les specs", null, null, null, null);

        when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(10L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> taskService.createTask(user, request))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void createTask_shouldThrowForbidden_whenAssigneeIsNotTeamMember() {
        taskService = newTaskService();
        User user = user(1L);
        Team team = Team.builder().id(10L).name("Squad Alpha").owner(user).build();
        Project project = project(20L, team);
        User outsider = user(2L);
        CreateTaskRequest request = new CreateTaskRequest(20L, "Ecrire les specs", null, null, 2L, null);

        when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(10L, 1L)).thenReturn(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(outsider));
        when(teamMemberRepository.existsByTeamIdAndUserId(10L, 2L)).thenReturn(false);

        assertThatThrownBy(() -> taskService.createTask(user, request))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void updateStatus_shouldChangeStatus_whenUserIsTeamMember() {
        taskService = newTaskService();
        User user = user(1L);
        Team team = Team.builder().id(10L).name("Squad Alpha").owner(user).build();
        Project project = project(20L, team);
        Task task = Task.builder().id(30L).project(project).title("Ecrire les specs").status(TaskStatus.TODO).build();
        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS);

        when(taskRepository.findById(30L)).thenReturn(Optional.of(task));
        when(teamMemberRepository.existsByTeamIdAndUserId(10L, 1L)).thenReturn(true);
        when(subtaskRepository.findAllByTaskId(30L)).thenReturn(List.of());

        TaskResponse response = taskService.updateStatus(user, 30L, request);

        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void deleteTask_shouldThrowNotFound_whenTaskDoesNotExist() {
        taskService = newTaskService();
        User user = user(1L);

        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.deleteTask(user, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listTasksForProject_shouldReturnTasksWithSubtasks() {
        taskService = newTaskService();
        User user = user(1L);
        Team team = Team.builder().id(10L).name("Squad Alpha").owner(user).build();
        Project project = project(20L, team);
        Task task = Task.builder().id(30L).project(project).title("Ecrire les specs").build();
        Subtask subtask = Subtask.builder().id(40L).task(task).title("Rediger le brief").done(false).build();

        when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(10L, 1L)).thenReturn(true);
        when(taskRepository.findAllByProjectId(20L)).thenReturn(List.of(task));
        when(subtaskRepository.findAllByTaskId(30L)).thenReturn(List.of(subtask));

        List<TaskResponse> response = taskService.listTasksForProject(user, 20L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).subtasks()).hasSize(1);
        assertThat(response.get(0).subtasks().get(0).title()).isEqualTo("Rediger le brief");
    }

}
