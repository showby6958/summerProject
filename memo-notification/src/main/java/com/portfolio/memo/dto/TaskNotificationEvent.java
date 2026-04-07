package com.portfolio.memo.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

// producer(Task, Chat 서비스)에서 payload로 넣은 JSON을 Notification 서비스에서 파싱하기 위한 DTO
@Getter
@Setter
public class TaskNotificationEvent {
    private String eventId; // UUID
    private String eventType; // TASK_CREATED, TASK_ASSIGEND
    private Instant occurredAt;

    private Long taskId;
    private Long actorId;

    private String title;

    private Long assigneeId;
    private List<Long> memberIds;

    // 수신자 목록(담당자 + 팀원)
    private List<Long> recipientUserIds;
}
