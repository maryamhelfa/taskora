package com.taskora.backend.project;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findAllByTeamId(Long teamId);

    List<Project> findAllByTeamIdIn(List<Long> teamIds);

}
