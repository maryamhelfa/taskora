package com.taskora.backend.task;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taskora.backend.exception.ResourceNotFoundException;
import com.taskora.backend.task.dto.CreateSubtaskRequest;
import com.taskora.backend.task.dto.SubtaskResponse;
import com.taskora.backend.task.dto.UpdateSubtaskRequest;
import com.taskora.backend.team.TeamAccessService;
import com.taskora.backend.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SubtaskService {

    private final SubtaskRepository subtaskRepository;
    private final TaskRepository taskRepository;
    private final TeamAccessService teamAccessService;

    public SubtaskResponse createSubtask(User currentUser, Long taskId, CreateSubtaskRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Tache introuvable : " + taskId));
        teamAccessService.requireMembership(task.getProject().getTeam().getId(), currentUser.getId());

        Subtask subtask = Subtask.builder()
                .task(task)
                .title(request.title())
                .done(false)
                .build();
        subtaskRepository.save(subtask);

        return toSubtaskResponse(subtask);
    }

    public SubtaskResponse updateSubtask(User currentUser, Long subtaskId, UpdateSubtaskRequest request) {
        Subtask subtask = subtaskRepository.findById(subtaskId)
                .orElseThrow(() -> new ResourceNotFoundException("Sous-tache introuvable : " + subtaskId));
        teamAccessService.requireMembership(subtask.getTask().getProject().getTeam().getId(), currentUser.getId());

        if (request.title() != null) {
            subtask.setTitle(request.title());
        }
        if (request.done() != null) {
            subtask.setDone(request.done());
        }

        return toSubtaskResponse(subtask);
    }

    private SubtaskResponse toSubtaskResponse(Subtask subtask) {
        return new SubtaskResponse(subtask.getId(), subtask.getTask().getId(), subtask.getTitle(), subtask.isDone());
    }

}
