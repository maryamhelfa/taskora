package com.taskora.backend.task;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taskora.backend.exception.ForbiddenActionException;
import com.taskora.backend.exception.ResourceNotFoundException;
import com.taskora.backend.project.Project;
import com.taskora.backend.project.ProjectRepository;
import com.taskora.backend.task.dto.CreateTaskRequest;
import com.taskora.backend.task.dto.SubtaskResponse;
import com.taskora.backend.task.dto.TaskResponse;
import com.taskora.backend.task.dto.UpdateTaskRequest;
import com.taskora.backend.task.dto.UpdateTaskStatusRequest;
import com.taskora.backend.team.TeamAccessService;
import com.taskora.backend.user.User;
import com.taskora.backend.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final SubtaskRepository subtaskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TeamAccessService teamAccessService;

    @Transactional(readOnly = true)
    public List<TaskResponse> listTasksForProject(User currentUser, Long projectId) {
        Project project = findProjectOrThrow(projectId);
        teamAccessService.requireMembership(project.getTeam().getId(), currentUser.getId());
        return taskRepository.findAllByProjectId(projectId).stream().map(this::toTaskResponse).toList();
    }

    public TaskResponse createTask(User currentUser, CreateTaskRequest request) {
        Project project = findProjectOrThrow(request.projectId());
        Long teamId = project.getTeam().getId();
        teamAccessService.requireMembership(teamId, currentUser.getId());

        User assignee = resolveAssignee(request.assigneeId(), teamId);

        Task task = Task.builder()
                .project(project)
                .title(request.title())
                .description(request.description())
                .priority(request.priority() != null ? request.priority() : TaskPriority.MEDIUM)
                .assignee(assignee)
                .dueDate(request.dueDate())
                .build();
        taskRepository.save(task);

        return toTaskResponse(task);
    }

    public TaskResponse updateTask(User currentUser, Long taskId, UpdateTaskRequest request) {
        Task task = findTaskOrThrow(taskId);
        Long teamId = task.getProject().getTeam().getId();
        teamAccessService.requireMembership(teamId, currentUser.getId());

        User assignee = resolveAssignee(request.assigneeId(), teamId);

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setPriority(request.priority());
        task.setAssignee(assignee);
        task.setDueDate(request.dueDate());

        return toTaskResponse(task);
    }

    public TaskResponse updateStatus(User currentUser, Long taskId, UpdateTaskStatusRequest request) {
        Task task = findTaskOrThrow(taskId);
        teamAccessService.requireMembership(task.getProject().getTeam().getId(), currentUser.getId());
        task.setStatus(request.status());
        return toTaskResponse(task);
    }

    public void deleteTask(User currentUser, Long taskId) {
        Task task = findTaskOrThrow(taskId);
        teamAccessService.requireMembership(task.getProject().getTeam().getId(), currentUser.getId());
        taskRepository.delete(task);
    }

    private User resolveAssignee(Long assigneeId, Long teamId) {
        if (assigneeId == null) {
            return null;
        }
        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + assigneeId));
        if (!teamAccessService.isMember(teamId, assigneeId)) {
            throw new ForbiddenActionException("Le membre assigne doit appartenir a l'equipe du projet");
        }
        return assignee;
    }

    private Project findProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable : " + projectId));
    }

    private Task findTaskOrThrow(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Tache introuvable : " + taskId));
    }

    private TaskResponse toTaskResponse(Task task) {
        User assignee = task.getAssignee();
        List<SubtaskResponse> subtasks = subtaskRepository.findAllByTaskId(task.getId()).stream()
                .map(subtask -> new SubtaskResponse(subtask.getId(), subtask.getTask().getId(), subtask.getTitle(), subtask.isDone()))
                .toList();

        return new TaskResponse(
                task.getId(),
                task.getProject().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                assignee != null ? assignee.getId() : null,
                assignee != null ? assignee.getEmail() : null,
                assignee != null ? assignee.getFirstName() : null,
                assignee != null ? assignee.getLastName() : null,
                task.getDueDate(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                subtasks
        );
    }

}
