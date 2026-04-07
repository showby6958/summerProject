package com.portfolio.memo.file;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AttachedFileRepository extends JpaRepository<AttachedFile, Long> {

    @Query("SELECT f FROM AttachedFile f " +
            "WHERE f.task.id = :taskId")
    List<AttachedFile> findFilesByTaskId(@Param("taskId") Long taskId);
}
