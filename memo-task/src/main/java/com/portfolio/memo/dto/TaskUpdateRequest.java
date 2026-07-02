package com.portfolio.memo.dto;

import com.portfolio.memo.TaskPriority;
import com.portfolio.memo.TaskStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
public class TaskUpdateRequest {
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private LocalDateTime dueDate;
    private Long assigneeId;
    private List<Long> memberIds;
}
