package com.taskora.backend.team;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamRepository extends JpaRepository<Team, Long> {

    @Query("select tm.team from TeamMember tm where tm.user.id = :userId")
    List<Team> findAllByMemberUserId(@Param("userId") Long userId);

}
