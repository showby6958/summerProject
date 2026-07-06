package com.portfolio.memo.domain;

import com.portfolio.memo.dto.TaskNotificationEvent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

// 오프라인 사용자를 위한 알림 영속화 (재접속 시 조회 가능)
@Entity
@Table(name = "notifications",
        indexes = {
                @Index(name = "idx_notification_user_read", columnList = "user_id,is_read,created_at")
        },
        uniqueConstraints = @UniqueConstraint(name = "ux_notification_event_user", columnNames = {"event_id", "user_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(nullable = false)
    private String title;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static Notification of(Long userId, TaskNotificationEvent event) {
        Notification n = new Notification();
        n.userId = userId;
        n.eventId = event.getEventId();
        n.eventType = event.getEventType();
        n.taskId = event.getTaskId();
        n.actorId = event.getActorId();
        n.title = event.getTitle();
        n.occurredAt = event.getOccurredAt();
        n.read = false;
        return n;
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = Instant.now();
    }

    public void markRead() {
        this.read = true;
        this.readAt = Instant.now();
    }
}