package com.portfolio.memo.comment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c " +
            "WHERE c.task.id = :taskId " +
            "ORDER BY c.createdAt ASC")
    List<Comment> findCommentsByTaskId(@Param("taskId") Long taskId);
}
