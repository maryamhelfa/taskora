package com.taskora.backend.activity;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    List<ActivityLog> findAllByProjectIdOrderByCreatedAtDesc(Long projectId);

}
