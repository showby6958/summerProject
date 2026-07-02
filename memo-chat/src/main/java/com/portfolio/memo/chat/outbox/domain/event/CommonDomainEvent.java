package com.portfolio.memo.chat.outbox.domain.event;

import com.portfolio.memo.chat.outbox.domain.OutboxEventType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Getter
@SuperBuilder
public abstract class CommonDomainEvent {

    // 이벤트 고유 ID (중복 처리 방지)
    private final String eventId;

    // 이벤트 타입
    private final OutboxEventType eventType;

    // 이벤트 발생 시각
    private final Instant occurredAt;

}
