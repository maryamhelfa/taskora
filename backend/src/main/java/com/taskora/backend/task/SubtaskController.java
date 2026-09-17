package com.taskora.backend.task;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taskora.backend.task.dto.SubtaskResponse;
import com.taskora.backend.task.dto.UpdateSubtaskRequest;
import com.taskora.backend.user.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/subtasks")
@RequiredArgsConstructor
public class SubtaskController {

    private final SubtaskService subtaskService;

    @PatchMapping("/{id}")
    public ResponseEntity<SubtaskResponse> updateSubtask(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @Valid @RequestBody UpdateSubtaskRequest request
    ) {
        return ResponseEntity.ok(subtaskService.updateSubtask(currentUser, id, request));
    }

}
