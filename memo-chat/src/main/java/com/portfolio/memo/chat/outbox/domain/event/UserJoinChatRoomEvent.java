package com.portfolio.memo.chat.outbox.domain.event;

import com.portfolio.memo.chat.outbox.domain.OutboxEventType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Getter
@SuperBuilder
public class UserJoinChatRoomEvent extends CommonDomainEvent{

    private final Long roomId;

    private final Long userId;
    private final String userName;

    public static UserJoinChatRoomEvent of(
            Long roomId,
            Long userId,
            String userName
    ) {
        return UserJoinChatRoomEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(OutboxEventType.USER_JOINED_CHAT_ROOM)
                .occurredAt(Instant.now())
                .roomId(roomId)
                .userId(userId)
                .userName(userName)
                .build();
    }
}
