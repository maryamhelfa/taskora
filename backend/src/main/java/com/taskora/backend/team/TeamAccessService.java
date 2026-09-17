package com.taskora.backend.team;

import org.springframework.stereotype.Service;

import com.taskora.backend.exception.ForbiddenActionException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeamAccessService {

    private final TeamMemberRepository teamMemberRepository;

    public boolean isMember(Long teamId, Long userId) {
        return teamMemberRepository.existsByTeamIdAndUserId(teamId, userId);
    }

    public void requireMembership(Long teamId, Long userId) {
        if (!isMember(teamId, userId)) {
            throw new ForbiddenActionException("Vous n'etes pas membre de cette equipe");
        }
    }

}
