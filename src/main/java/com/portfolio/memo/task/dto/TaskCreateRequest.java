package com.portfolio.memo.task.dto;

import com.portfolio.memo.task.TaskPriority;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TaskCreateRequest {
    // 제목
    private String title;
    // 설명
    private String description;
    // 담당자 id
    private Long assigneeId;
    // 마감일
    private LocalDateTime dueDate;
    // 업무 우선도
    private TaskPriority priority;
}
