package com.taskora.backend.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.anthropic.core.JsonValue;
import com.anthropic.core.ObjectMappers;
import com.anthropic.core.http.Headers;
import com.anthropic.errors.RateLimitException;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.services.blocking.MessageService;
import com.taskora.backend.exception.AiServiceException;
import com.taskora.backend.exception.ForbiddenActionException;
import com.taskora.backend.exception.ResourceNotFoundException;
import com.taskora.backend.project.Project;
import com.taskora.backend.project.ProjectRepository;
import com.taskora.backend.task.Task;
import com.taskora.backend.task.TaskRepository;
import com.taskora.backend.team.Team;
import com.taskora.backend.team.TeamAccessService;
import com.taskora.backend.team.TeamMemberRepository;
import com.taskora.backend.user.User;

@ExtendWith(MockitoExtension.class)
class AiSummaryServiceTest {

    @Mock
    private com.anthropic.client.AnthropicClient anthropicClient;

    @Mock
    private MessageService messageService;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    private AiSummaryService newService() {
        AiSummaryService service = new AiSummaryService(anthropicClient, projectRepository, taskRepository,
                new TeamAccessService(teamMemberRepository));
        ReflectionTestUtils.setField(service, "model", "claude-haiku-4-5");
        return service;
    }

    private User user(long id) {
        return User.builder().id(id).email("user" + id + "@doe.com").firstName("Jane").lastName("Doe").build();
    }

    @Test
    void generateSummary_shouldReturnText_whenClaudeRespondsSuccessfully() throws Exception {
        AiSummaryService service = newService();
        User user = user(1L);
        Team team = Team.builder().id(10L).name("Squad Alpha").owner(user).build();
        Project project = Project.builder().id(5L).team(team).name("Site web").build();
        Task task = Task.builder().id(1L).project(project).title("Maquette").build();

        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(10L, 1L)).thenReturn(true);
        when(taskRepository.findAllByProjectId(5L)).thenReturn(List.of(task));
        when(anthropicClient.messages()).thenReturn(messageService);

        Message message = ObjectMappers.jsonMapper().readValue("""
                {
                  "id": "msg_1",
                  "type": "message",
                  "role": "assistant",
                  "model": "claude-haiku-4-5",
                  "content": [{"type": "text", "text": "Resume genere."}],
                  "stop_reason": "end_turn",
                  "stop_sequence": null,
                  "usage": {"input_tokens": 10, "output_tokens": 20}
                }
                """, Message.class);
        when(messageService.create(any(MessageCreateParams.class))).thenReturn(message);

        var response = service.generateSummary(user, 5L);

        assertThat(response.summary()).isEqualTo("Resume genere.");
    }

    @Test
    void generateSummary_shouldThrowNotFound_whenProjectDoesNotExist() {
        AiSummaryService service = newService();
        User user = user(1L);

        when(projectRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateSummary(user, 5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void generateSummary_shouldThrowForbidden_whenUserIsNotTeamMember() {
        AiSummaryService service = newService();
        User user = user(1L);
        Team team = Team.builder().id(10L).name("Squad Alpha").owner(user).build();
        Project project = Project.builder().id(5L).team(team).name("Site web").build();

        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(10L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> service.generateSummary(user, 5L))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void generateSummary_shouldThrowAiServiceException_whenClaudeIsRateLimited() {
        AiSummaryService service = newService();
        User user = user(1L);
        Team team = Team.builder().id(10L).name("Squad Alpha").owner(user).build();
        Project project = Project.builder().id(5L).team(team).name("Site web").build();

        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(10L, 1L)).thenReturn(true);
        when(taskRepository.findAllByProjectId(5L)).thenReturn(List.of());
        when(anthropicClient.messages()).thenReturn(messageService);

        RateLimitException rateLimitException = RateLimitException.builder()
                .headers(Headers.builder().build())
                .body(JsonValue.from(Map.of()))
                .build();
        when(messageService.create(any(MessageCreateParams.class))).thenThrow(rateLimitException);

        assertThatThrownBy(() -> service.generateSummary(user, 5L))
                .isInstanceOf(AiServiceException.class);
    }

}
