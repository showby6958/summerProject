package com.portfolio.memo.chat.outbox.domain.event;

import com.portfolio.memo.chat.message.ChatMessage;
import com.portfolio.memo.chat.outbox.domain.OutboxEventType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

// Outbox payload로 담길 "MessageCreated" 이벤트 바디
@Getter
@SuperBuilder
public class MessageCreatedEvent extends CommonDomainEvent{
    private Long messageId;
    private Long roomId;

    private Long senderId;
    private String senderName;

    private String content;

    private Instant sentAt;

    public static MessageCreatedEvent from(ChatMessage saved) {
        return MessageCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(OutboxEventType.MESSAGE_CREATED)
                .occurredAt(Instant.now())
                .messageId(saved.getId())
                .roomId(saved.getRoomId())
                .senderId(saved.getSenderId())
                .senderName(saved.getSenderName())
                .content(saved.getContent())
                .sentAt(saved.getSentAt().toInstant(ZoneOffset.UTC))
                .build();
    }
}
