package com.portfolio.memo.dto;

import com.portfolio.memo.domain.Notification;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class NotificationResponse {

    private Long id;
    private String eventId;
    private String eventType;
    private Long taskId;
    private Long actorId;
    private String title;
    private Instant occurredAt;
    private boolean read;

    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .eventId(n.getEventId())
                .eventType(n.getEventType())
                .taskId(n.getTaskId())
                .actorId(n.getActorId())
                .title(n.getTitle())
                .occurredAt(n.getOccurredAt())
                .read(n.isRead())
                .build();
    }
}