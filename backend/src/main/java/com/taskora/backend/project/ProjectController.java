package com.taskora.backend.project;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.taskora.backend.activity.ActivityLogService;
import com.taskora.backend.activity.dto.ActivityLogResponse;
import com.taskora.backend.ai.AiSummaryService;
import com.taskora.backend.ai.dto.AiSummaryResponse;
import com.taskora.backend.project.dto.CreateProjectRequest;
import com.taskora.backend.project.dto.ProjectResponse;
import com.taskora.backend.project.dto.UpdateProjectRequest;
import com.taskora.backend.task.TaskService;
import com.taskora.backend.task.dto.TaskResponse;
import com.taskora.backend.user.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final TaskService taskService;
    private final ActivityLogService activityLogService;
    private final AiSummaryService aiSummaryService;

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> listProjects(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) Long teamId
    ) {
        return ResponseEntity.ok(projectService.listProjects(currentUser, teamId));
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateProjectRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(currentUser, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectRequest request
    ) {
        return ResponseEntity.ok(projectService.updateProject(currentUser, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id
    ) {
        projectService.deleteProject(currentUser, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/tasks")
    public ResponseEntity<List<TaskResponse>> listTasks(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(taskService.listTasksForProject(currentUser, id));
    }

    @GetMapping("/{id}/activity")
    public ResponseEntity<List<ActivityLogResponse>> listActivity(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(activityLogService.listForProject(currentUser, id));
    }

    @PostMapping("/{id}/ai-summary")
    public ResponseEntity<AiSummaryResponse> generateAiSummary(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(aiSummaryService.generateSummary(currentUser, id));
    }

}
