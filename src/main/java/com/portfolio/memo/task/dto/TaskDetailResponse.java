package com.portfolio.memo.task.dto;

import com.portfolio.memo.task.Task;
import com.portfolio.memo.task.TaskPriority;
import com.portfolio.memo.task.TaskStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TaskDetailResponse {

    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private LocalDateTime dueDate;

    private Long assigneeId;
    private String assigneeName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    public static TaskDetailResponse from(Task task) {
        return TaskDetailResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .assigneeId(task.getAssignee().getId())
                .assigneeName(task.getAssignee().getName())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
