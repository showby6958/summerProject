package com.portfolio.memo.chat.outbox.domain;

// 이벤트 타입 상수
public enum OutboxEventType {
    MESSAGE_CREATED,
    USER_JOINED_CHAT_ROOM,
    USER_LEFT_CHAT_ROOM
}
