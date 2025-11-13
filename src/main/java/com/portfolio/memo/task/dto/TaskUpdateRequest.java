package com.portfolio.memo.task.dto;

import com.portfolio.memo.task.TaskPriority;
import com.portfolio.memo.task.TaskStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class TaskUpdateRequest {
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private LocalDateTime dueDate;
    private Long assigneeId;
}
