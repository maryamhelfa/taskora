package com.taskora.backend.team;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taskora.backend.exception.ConflictException;
import com.taskora.backend.exception.ForbiddenActionException;
import com.taskora.backend.exception.ResourceNotFoundException;
import com.taskora.backend.team.dto.AddTeamMemberRequest;
import com.taskora.backend.team.dto.CreateTeamRequest;
import com.taskora.backend.team.dto.TeamMemberResponse;
import com.taskora.backend.team.dto.TeamResponse;
import com.taskora.backend.user.User;
import com.taskora.backend.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<TeamResponse> listTeamsForUser(User currentUser) {
        return teamRepository.findAllByMemberUserId(currentUser.getId()).stream()
                .map(this::toTeamResponse)
                .toList();
    }

    public TeamResponse createTeam(User owner, CreateTeamRequest request) {
        Team team = Team.builder()
                .name(request.name())
                .owner(owner)
                .build();
        teamRepository.save(team);

        TeamMember ownerMembership = TeamMember.builder()
                .team(team)
                .user(owner)
                .role(TeamRole.ADMIN)
                .build();
        teamMemberRepository.save(ownerMembership);

        return toTeamResponse(team);
    }

    @Transactional(readOnly = true)
    public List<TeamMemberResponse> listMembers(User currentUser, Long teamId) {
        if (!teamRepository.existsById(teamId)) {
            throw new ResourceNotFoundException("Equipe introuvable : " + teamId);
        }
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, currentUser.getId())) {
            throw new ForbiddenActionException("Vous n'etes pas membre de cette equipe");
        }
        return teamMemberRepository.findAllByTeamId(teamId).stream()
                .map(this::toTeamMemberResponse)
                .toList();
    }

    public TeamMemberResponse addMember(User currentUser, Long teamId, AddTeamMemberRequest request) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipe introuvable : " + teamId));

        requireAdmin(team.getId(), currentUser.getId());

        User targetUser = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + request.email()));

        if (teamMemberRepository.existsByTeamIdAndUserId(team.getId(), targetUser.getId())) {
            throw new ConflictException(targetUser.getEmail() + " est deja membre de cette equipe");
        }

        TeamMember membership = TeamMember.builder()
                .team(team)
                .user(targetUser)
                .role(request.role() != null ? request.role() : TeamRole.MEMBER)
                .build();
        teamMemberRepository.save(membership);

        return toTeamMemberResponse(membership);
    }

    private void requireAdmin(Long teamId, Long userId) {
        TeamMember membership = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new ForbiddenActionException("Vous n'etes pas membre de cette equipe"));
        if (membership.getRole() != TeamRole.ADMIN) {
            throw new ForbiddenActionException("Seul un administrateur de l'equipe peut effectuer cette action");
        }
    }

    private TeamResponse toTeamResponse(Team team) {
        return new TeamResponse(team.getId(), team.getName(), team.getOwner().getId(), team.getCreatedAt());
    }

    private TeamMemberResponse toTeamMemberResponse(TeamMember membership) {
        User user = membership.getUser();
        return new TeamMemberResponse(
                membership.getId(),
                membership.getTeam().getId(),
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                membership.getRole(),
                membership.getCreatedAt()
        );
    }

}
