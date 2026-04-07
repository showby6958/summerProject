package com.portfolio.memo.chat.outbox.domain.event;

import com.portfolio.memo.chat.outbox.domain.OutboxEventType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Getter
@SuperBuilder
public class UserLeftChatRoomEvent extends CommonDomainEvent{

    private final Long roomId;

    private final Long userId;

    private static UserLeftChatRoomEvent of(
            Long roomId,
            Long userId
    ) {
        return UserLeftChatRoomEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(OutboxEventType.USER_LEFT_CHAT_ROOM)
                .occurredAt(Instant.now())
                .roomId(roomId)
                .userId(userId)
                .build();
    }
}
