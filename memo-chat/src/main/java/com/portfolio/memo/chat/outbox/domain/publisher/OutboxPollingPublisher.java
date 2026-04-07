package com.portfolio.memo.chat.outbox.domain.publisher;

import com.portfolio.memo.chat.outbox.domain.OutboxEvent;
import com.portfolio.memo.chat.outbox.domain.OutboxEventRepository;
import com.portfolio.memo.chat.outbox.domain.OutboxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPollingPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final RedisStreamOutboxPublisher redisStreamOutboxPublisher;

    private final String instanceId = UUID.randomUUID().toString();

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRY = 5;

    // 1초 마다 polling
    public void publishPendingEvents() {
        List<OutboxEvent> events = lockBatch();

        for (OutboxEvent event : events) {
            process(event);
        }
    }

    // lockBatch (트랜잭션)
    @Transactional
    public List<OutboxEvent> lockBatch() {
        List<OutboxEvent> events = outboxEventRepository.lockPendingBatch(BATCH_SIZE);

        Instant now = Instant.now();

        for (OutboxEvent event : events) {
            event.markProcessing(instanceId, now);
        }

        return events;
    }

    // Publish (트랜잭선 밖)
    public void process(OutboxEvent event) {

        try {
            RecordId recordId = redisStreamOutboxPublisher.publish(event);

            markSent(event.getId(), recordId.getValue());

        } catch (Exception e) {
            log.error("Outbox publish failed. eventId: {}", event.getEventUuid(), e);
            handleFailure(event.getId(), e);
        }
    }

    // 상태 업데이트 (별도 트랜잭션) - 성공 처리
    @Transactional
    public void markSent(Long eventId, String recordId) {
        OutboxEvent event = outboxEventRepository.findById(eventId)
                .orElseThrow();

        event.markSent(Instant.now(), recordId);
    }

    // 상태 업데이트 (별도 트랜잭션) - 실패 처리
    @Transactional
    public void handleFailure(Long eventId, Exception e) {
        OutboxEvent event = outboxEventRepository.findById(eventId)
                .orElseThrow();

        if (event.getAttemptCount() >= MAX_RETRY) {
            event.markDead(Instant.now(), e.getMessage());
            return;
        }
        Instant nextRetry = nextRetryTime(event.getAttemptCount());

        event.markPendingRetry(nextRetry, e.getMessage());
    }

    // backoff 전략
    private Instant nextRetryTime(int attempt) {
        long delay = (long) Math.pow(2, attempt); // 1, 2, 4, 8 ...
        delay = Math.min(delay, 60);

        return Instant.now().plusSeconds(delay);
    }

    // Stuck 이벤트 복구
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void recoverStuckEvents() {
        Instant cutoff = Instant.now().minusSeconds(30);

        int count = outboxEventRepository.requeueStaleProcessing(cutoff);

        if (count > 0) {
            log.warn("Recovered {} stuck events.", count);
        }
    }


}
