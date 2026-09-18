package com.taskora.backend.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.taskora.backend.exception.ConflictException;
import com.taskora.backend.exception.ForbiddenActionException;
import com.taskora.backend.exception.ResourceNotFoundException;
import com.taskora.backend.team.dto.AddTeamMemberRequest;
import com.taskora.backend.team.dto.CreateTeamRequest;
import com.taskora.backend.team.dto.TeamMemberResponse;
import com.taskora.backend.team.dto.TeamResponse;
import com.taskora.backend.user.User;
import com.taskora.backend.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private UserRepository userRepository;

    private TeamService teamService;

    private TeamService newTeamService() {
        return new TeamService(teamRepository, teamMemberRepository, userRepository);
    }

    private User user(long id, String email) {
        return User.builder().id(id).email(email).firstName("Jane").lastName("Doe").build();
    }

    @Test
    void createTeam_shouldCreateTeamAndAddOwnerAsAdminMember() {
        teamService = newTeamService();
        User owner = user(1L, "owner@doe.com");
        CreateTeamRequest request = new CreateTeamRequest("Squad Alpha");

        TeamResponse response = teamService.createTeam(owner, request);

        assertThat(response.name()).isEqualTo("Squad Alpha");
        assertThat(response.ownerId()).isEqualTo(1L);
    }

    @Test
    void addMember_shouldAddMemberWithDefaultRole_whenRequesterIsAdmin() {
        teamService = newTeamService();
        User admin = user(1L, "admin@doe.com");
        Team team = Team.builder().id(10L).name("Squad Alpha").owner(admin).build();
        TeamMember adminMembership = TeamMember.builder().id(100L).team(team).user(admin).role(TeamRole.ADMIN).build();
        User newUser = user(2L, "member@doe.com");
        AddTeamMemberRequest request = new AddTeamMemberRequest("member@doe.com", null);

        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(adminMembership));
        when(userRepository.findByEmail("member@doe.com")).thenReturn(Optional.of(newUser));
        when(teamMemberRepository.existsByTeamIdAndUserId(10L, 2L)).thenReturn(false);

        TeamMemberResponse response = teamService.addMember(admin, 10L, request);

        assertThat(response.email()).isEqualTo("member@doe.com");
        assertThat(response.role()).isEqualTo(TeamRole.MEMBER);
        assertThat(response.teamId()).isEqualTo(10L);
    }

    @Test
    void addMember_shouldThrowForbidden_whenRequesterIsNotAdmin() {
        teamService = newTeamService();
        User member = user(1L, "member@doe.com");
        Team team = Team.builder().id(10L).name("Squad Alpha").owner(member).build();
        TeamMember membership = TeamMember.builder().id(100L).team(team).user(member).role(TeamRole.MEMBER).build();
        AddTeamMemberRequest request = new AddTeamMemberRequest("new@doe.com", null);

        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> teamService.addMember(member, 10L, request))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void addMember_shouldThrowConflict_whenUserAlreadyMember() {
        teamService = newTeamService();
        User admin = user(1L, "admin@doe.com");
        Team team = Team.builder().id(10L).name("Squad Alpha").owner(admin).build();
        TeamMember adminMembership = TeamMember.builder().id(100L).team(team).user(admin).role(TeamRole.ADMIN).build();
        User existingUser = user(2L, "member@doe.com");
        AddTeamMemberRequest request = new AddTeamMemberRequest("member@doe.com", null);

        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(adminMembership));
        when(userRepository.findByEmail("member@doe.com")).thenReturn(Optional.of(existingUser));
        when(teamMemberRepository.existsByTeamIdAndUserId(10L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> teamService.addMember(admin, 10L, request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void addMember_shouldThrowNotFound_whenTeamDoesNotExist() {
        teamService = newTeamService();
        User admin = user(1L, "admin@doe.com");
        AddTeamMemberRequest request = new AddTeamMemberRequest("member@doe.com", null);

        when(teamRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.addMember(admin, 99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listMembers_shouldReturnMembers_whenRequesterIsMember() {
        teamService = newTeamService();
        User admin = user(1L, "admin@doe.com");
        Team team = Team.builder().id(10L).name("Squad Alpha").owner(admin).build();
        TeamMember adminMembership = TeamMember.builder().id(100L).team(team).user(admin).role(TeamRole.ADMIN).build();

        when(teamRepository.existsById(10L)).thenReturn(true);
        when(teamMemberRepository.existsByTeamIdAndUserId(10L, 1L)).thenReturn(true);
        when(teamMemberRepository.findAllByTeamId(10L)).thenReturn(List.of(adminMembership));

        List<TeamMemberResponse> response = teamService.listMembers(admin, 10L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).email()).isEqualTo("admin@doe.com");
        assertThat(response.get(0).role()).isEqualTo(TeamRole.ADMIN);
    }

    @Test
    void listMembers_shouldThrowForbidden_whenRequesterIsNotMember() {
        teamService = newTeamService();
        User outsider = user(1L, "outsider@doe.com");

        when(teamRepository.existsById(10L)).thenReturn(true);
        when(teamMemberRepository.existsByTeamIdAndUserId(10L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> teamService.listMembers(outsider, 10L))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void listMembers_shouldThrowNotFound_whenTeamDoesNotExist() {
        teamService = newTeamService();
        User user = user(1L, "user@doe.com");

        when(teamRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> teamService.listMembers(user, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listTeamsForUser_shouldReturnTeamsFromRepository() {
        teamService = newTeamService();
        User owner = user(1L, "owner@doe.com");
        Team team = Team.builder().id(10L).name("Squad Alpha").owner(owner).build();

        when(teamRepository.findAllByMemberUserId(1L)).thenReturn(List.of(team));

        List<TeamResponse> response = teamService.listTeamsForUser(owner);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).name()).isEqualTo("Squad Alpha");
    }

}
