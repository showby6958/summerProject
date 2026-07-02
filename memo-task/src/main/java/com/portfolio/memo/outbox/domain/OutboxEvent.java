package com.portfolio.memo.outbox.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_event",
        indexes = {
                @Index(name = "idx_outbox_status_nextRetry", columnList = "status,next_retry_at"),
                @Index(name = "ux_outbox_event_uuid", columnList = "event_uuid", unique = true)
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_uuid", nullable = false, length = 36, unique = true)
    private String eventUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private OutboxEventType eventType;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_retry_at", nullable = false)
    private LocalDateTime nextRetryAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "locked_by", length = 64)
    private String lockedBy;

    @Column(name = "stream_record_id", length = 128)
    private String streamRecordId;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public static OutboxEvent pending(OutboxEventType type,
                                      String aggregateType,
                                      Long aggregateId,
                                      String payload) {
        OutboxEvent e = new OutboxEvent();
        e.eventUuid = UUID.randomUUID().toString();
        e.eventType = type;
        e.aggregateType = aggregateType;
        e.aggregateId = aggregateId;
        e.payload = payload;
        e.status = OutboxStatus.PENDING;
        e.attemptCount = 0;
        e.nextRetryAt = LocalDateTime.now(); // 즉시 발행 대상

        return e;
    }

    @PrePersist
    private void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.nextRetryAt == null) {
            this.nextRetryAt = LocalDateTime.now();
        }
    }


    // Relay state transitions --------------------------------------------

    public void markProcessing(String instanceId, LocalDateTime now) {
        this.status = OutboxStatus.PROCESSING;
        this.attemptCount += 1;
        this.lockedBy = instanceId;
        this.lockedAt = now;
    }

    public void markSent(LocalDateTime now, String streamRecordId) {
        this.status = OutboxStatus.SENT;
        this.publishedAt = now;
        this.lastError = null;
        this.nextRetryAt = now;
        this.lockedAt = null;
        this.lockedBy = null;
        this.streamRecordId = streamRecordId;
    }

    public void markPendingRetry(LocalDateTime nextRetryAt, String lastError) {
        this.status = OutboxStatus.PENDING;
        this.nextRetryAt = nextRetryAt;
        this.lastError = lastError;
        this.lockedAt = null;
        this.lockedBy = null;
    }

    public void markDead(LocalDateTime now, String lastError) {
        this.status = OutboxStatus.DEAD;
        this.lastError = lastError;
        this.publishedAt = now;
        this.lockedAt = null;
        this.lockedBy = null;
    }

    // -----------------------------------------------------------------------

}
