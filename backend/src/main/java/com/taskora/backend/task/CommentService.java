package com.taskora.backend.task;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taskora.backend.activity.ActivityActionType;
import com.taskora.backend.activity.ActivityLogService;
import com.taskora.backend.exception.ResourceNotFoundException;
import com.taskora.backend.task.dto.CommentResponse;
import com.taskora.backend.task.dto.CreateCommentRequest;
import com.taskora.backend.team.TeamAccessService;
import com.taskora.backend.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final TeamAccessService teamAccessService;
    private final ActivityLogService activityLogService;

    public CommentResponse addComment(User currentUser, Long taskId, CreateCommentRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Tache introuvable : " + taskId));
        teamAccessService.requireMembership(task.getProject().getTeam().getId(), currentUser.getId());

        Comment comment = Comment.builder()
                .task(task)
                .author(currentUser)
                .content(request.content())
                .build();
        commentRepository.save(comment);

        activityLogService.log(currentUser, task.getProject(), task, ActivityActionType.COMMENT_ADDED,
                "Commentaire ajoute sur la tache : " + task.getTitle());

        return toCommentResponse(comment);
    }

    private CommentResponse toCommentResponse(Comment comment) {
        User author = comment.getAuthor();
        return new CommentResponse(
                comment.getId(),
                comment.getTask().getId(),
                author.getId(),
                author.getEmail(),
                author.getFirstName(),
                author.getLastName(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }

}
