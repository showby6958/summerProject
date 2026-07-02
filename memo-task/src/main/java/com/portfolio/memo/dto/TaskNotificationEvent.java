package com.portfolio.memo.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

// 알림 전용 이벤트 DTO (단일 DTO로 통일 - 알림 이벤트는 모두 이 DTO 사용)
@Getter
@Setter
@Builder
public class TaskNotificationEvent {
    private String eventId; // UUID
    private String eventType; // TASK_CREATED, TASK_ASSIGEND
    private LocalDateTime occurredAt;

    private Long taskId;
    private Long actorId;

    private String title;

    private Long assigneeId;
    private List<Long> memberIds;

    // 수신자 목록(담당자 + 팀원)
    private List<Long> recipientUserIds;
}
