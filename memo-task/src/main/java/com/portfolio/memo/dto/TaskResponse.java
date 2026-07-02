package com.portfolio.memo.dto;

import com.portfolio.memo.file.dto.AttachedFileDto;
import com.portfolio.memo.Task;
import com.portfolio.memo.TaskPriority;
import com.portfolio.memo.TaskStatus;
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
    private String assigneeName; // 담당자 이름
    private List<String> memberNames; // 팀원 리스트
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
                .assigneeName(task.getAssigneeName() != null ? task.getAssigneeName() : null)
                .memberNames(task.getMemberNames() != null ? task.getMemberNames() : null)
                .createdAt(task.getCreatedAt())
                .attachedFiles(task.getAttachedFiles().stream()
                        .map(AttachedFileDto::from)
                        .collect(Collectors.toList()))
                .build();
    }
}
