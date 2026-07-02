package com.portfolio.memo.chat.outbox.domain;

// Outbox 처리 상태 정의(PENDING -> SENT/DEAD 등)
public enum OutboxStatus {
    PENDING,
    PROCESSING,
    SENT,
    DEAD
}
