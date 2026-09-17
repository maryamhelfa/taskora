package com.taskora.backend.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.taskora.backend.activity.ActivityLogService;
import com.taskora.backend.exception.ForbiddenActionException;
import com.taskora.backend.exception.ResourceNotFoundException;
import com.taskora.backend.project.Project;
import com.taskora.backend.task.dto.CommentResponse;
import com.taskora.backend.task.dto.CreateCommentRequest;
import com.taskora.backend.team.Team;
import com.taskora.backend.team.TeamAccessService;
import com.taskora.backend.team.TeamMemberRepository;
import com.taskora.backend.user.User;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private ActivityLogService activityLogService;

    private CommentService commentService;

    private CommentService newCommentService() {
        return new CommentService(commentRepository, taskRepository, new TeamAccessService(teamMemberRepository), activityLogService);
    }

    private User user(long id) {
        return User.builder().id(id).email("user" + id + "@doe.com").firstName("Jane").lastName("Doe").build();
    }

    private Task task(long id, Team team) {
        Project project = Project.builder().id(20L).team(team).name("Site web").build();
        return Task.builder().id(id).project(project).title("Ecrire les specs").build();
    }

    @Test
    void addComment_shouldSaveComment_whenUserIsTeamMember() {
        commentService = newCommentService();
        User user = user(1L);
        Team team = Team.builder().id(10L).name("Squad Alpha").owner(user).build();
        Task task = task(30L, team);
        CreateCommentRequest request = new CreateCommentRequest("Bien avance");

        when(taskRepository.findById(30L)).thenReturn(Optional.of(task));
        when(teamMemberRepository.existsByTeamIdAndUserId(10L, 1L)).thenReturn(true);

        CommentResponse response = commentService.addComment(user, 30L, request);

        assertThat(response.content()).isEqualTo("Bien avance");
        assertThat(response.taskId()).isEqualTo(30L);
        assertThat(response.authorId()).isEqualTo(1L);
    }

    @Test
    void addComment_shouldThrowForbidden_whenUserIsNotTeamMember() {
        commentService = newCommentService();
        User user = user(1L);
        Team team = Team.builder().id(10L).name("Squad Alpha").owner(user).build();
        Task task = task(30L, team);
        CreateCommentRequest request = new CreateCommentRequest("Bien avance");

        when(taskRepository.findById(30L)).thenReturn(Optional.of(task));
        when(teamMemberRepository.existsByTeamIdAndUserId(10L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> commentService.addComment(user, 30L, request))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void addComment_shouldThrowNotFound_whenTaskDoesNotExist() {
        commentService = newCommentService();
        User user = user(1L);
        CreateCommentRequest request = new CreateCommentRequest("Bien avance");

        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.addComment(user, 99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

}
