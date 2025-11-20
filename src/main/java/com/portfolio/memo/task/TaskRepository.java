package com.portfolio.memo.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.attachedFiles WHERE t.id = :taskId")
    Optional<Task> findByIdWithFiles(@Param("taskId") Long taskId);
}
