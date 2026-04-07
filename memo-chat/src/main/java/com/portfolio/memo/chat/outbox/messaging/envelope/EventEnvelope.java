package com.portfolio.memo.chat.outbox.messaging.envelope;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class EventEnvelope {

    private String eventId; //UUID
    private String eventType; // CHAT.MESSAGE_CRAETED
    private String sourceService; // chat-service, task-service, ...
    private Instant occurredAt;
    private Long aggregateId; // roomId, taskId 등

    private Object payload; // 실제 이벤트 DTO
}
