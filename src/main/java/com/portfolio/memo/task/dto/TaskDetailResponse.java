package com.portfolio.memo.task.dto;

import com.portfolio.memo.file.dto.AttachedFileDownloadDto;
import com.portfolio.memo.task.Task;
import com.portfolio.memo.task.TaskPriority;
import com.portfolio.memo.task.TaskStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

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

    private List<AttachedFileDownloadDto> attachedFiles;

    // 댓글 기능 나중에 추가
//    private List<CommentDto> comments;

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
                .attachedFiles(task.getAttachedFiles().stream()
                        .map(AttachedFileDownloadDto::from)
                        .toList()
                )
                // 댓글 나중에 추가
                .build();
    }
}
