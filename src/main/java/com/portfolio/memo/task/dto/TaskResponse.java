package com.portfolio.memo.task.dto;

import com.portfolio.memo.file.dto.AttachedFileDto;
import com.portfolio.memo.task.Task;
import com.portfolio.memo.task.TaskPriority;
import com.portfolio.memo.task.TaskStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private LocalDateTime dueDate;
    private Long assigneeId;
    private String assigneeName;
    private LocalDateTime createdAt;
    private List<AttachedFileDto> attachedFiles; // 첨부파일 리스트

    // Task 엔티티를 TaskResponse DTO로 변환하는 메소드
    public static TaskResponse from(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .assigneeId(task.getAssignee() != null ? task.getAssignee().getId() : null)
                .assigneeName(task.getAssignee() != null ? task.getAssignee().getName() : null)
                .createdAt(task.getCreatedAt())
                .attachedFiles(task.getAttachedFiles().stream()
                        .map(AttachedFileDto::from)
                        .collect(Collectors.toList()))
                .build();
    }
}
