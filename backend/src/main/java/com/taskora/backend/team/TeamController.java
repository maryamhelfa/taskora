package com.taskora.backend.team;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taskora.backend.team.dto.AddTeamMemberRequest;
import com.taskora.backend.team.dto.CreateTeamRequest;
import com.taskora.backend.team.dto.TeamMemberResponse;
import com.taskora.backend.team.dto.TeamResponse;
import com.taskora.backend.user.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @GetMapping
    public ResponseEntity<List<TeamResponse>> listTeams(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(teamService.listTeamsForUser(currentUser));
    }

    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateTeamRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teamService.createTeam(currentUser, request));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<TeamMemberResponse> addMember(
            @AuthenticationPrincipal User currentUser,
            @PathVariable("id") Long teamId,
            @Valid @RequestBody AddTeamMemberRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teamService.addMember(currentUser, teamId, request));
    }

}
