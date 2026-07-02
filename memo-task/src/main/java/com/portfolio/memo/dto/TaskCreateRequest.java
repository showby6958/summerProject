package com.portfolio.memo.dto;

import com.portfolio.memo.TaskPriority;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class TaskCreateRequest {
    // 제목
    private String title;
    // 설명
    private String description;
    // 팀원 userId 리스트
    private List<Long> memberIds;
    // 업무 우선도
    private TaskPriority priority;
    // 마감일
    private LocalDateTime dueDate;
}
