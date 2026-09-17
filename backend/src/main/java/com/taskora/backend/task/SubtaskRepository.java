package com.taskora.backend.task;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SubtaskRepository extends JpaRepository<Subtask, Long> {

    List<Subtask> findAllByTaskId(Long taskId);

}
