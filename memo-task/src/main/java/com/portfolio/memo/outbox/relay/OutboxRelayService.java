package com.portfolio.memo.outbox.relay;


import com.portfolio.memo.outbox.domain.OutboxEvent;
import com.portfolio.memo.outbox.domain.OutboxEventRepository;
import com.portfolio.memo.outbox.domain.OutboxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxRelayService {

    private final OutboxEventRepository outboxEventRepository;
    private final RedisStreamOutboxPublisher publisher;
    private final OutboxRelayProperties props;

    // 여러 인스턴스에서 실행될 수 있으므로 instanceId로 락 소유자를 남김
    private final String instanceId = UUID.randomUUID().toString();

    // 1. (옵션) stale PROCESSING 복구
    @Transactional
    public int requeueStaleProcessing() {
        LocalDateTime cutOff = LocalDateTime.now().minusSeconds(props.getProcessingTimeoutSeconds());
        return outboxEventRepository.requeueStaleProcessing(cutOff);
    }

    // 2. PENDING 이벤트를 SKIP LOCKED로 안전하게 claim 하고 PROCESSING으로 전환
    // - 반드시 트랜잭션 안에서 호출되어야 락이 의미가 있음
    @Transactional
    public List<OutboxEvent> claimBatch() {
        List<OutboxEvent> batch = outboxEventRepository.lockPendingBatch(props.getBatchSize());
        LocalDateTime now = LocalDateTime.now();
        for (OutboxEvent e : batch) {
            e.markProcessing(instanceId, now);
        }
        // 트랜잭션 커밋 시점에 PROCESSING 상태가 반영됨
        return batch;
    }

    // 3. claim된 이벤트들을 Streams에 발행 (트랜잭션 밖에서 네트워크 I/O 수행)
    public void publishOne(OutboxEvent claimed) {
        try {
            RecordId recordId = publisher.publish(claimed);
            markSent(claimed.getId(), recordId.getValue());
        } catch (Exception ex) {
            markFailed(claimed.getId(), ex);
        }
    }

    // 성공 처리: PROCESSING -> SENT
    @Transactional
    public void markSent(Long outboxId, String streamRecordId) {
        OutboxEvent e = outboxEventRepository.findById(outboxId)
                .orElseThrow(() -> new IllegalStateException("Outbox event not found: " + outboxId));

        // 이미 다른 흐름에서 상태가 바뀐 경우(복구/재시도 등)에는 방어적으로 종료
        if (e.getStatus() != OutboxStatus.PROCESSING) {
            return;
        }

        e.markSent(LocalDateTime.now(), streamRecordId);
    }

    // 실패 처리: PROCESSING -> PENDING(백오프) 또는 DEAD
    @Transactional
    public void markFailed(Long outboxId, Exception ex) {
        OutboxEvent e = outboxEventRepository.findById(outboxId)
                .orElseThrow(() -> new IllegalStateException("Outbox event not found: " + outboxId));

        if (e.getStatus() != OutboxStatus.PROCESSING) {
            return;
        }

        String err = safeError(ex);
        if (e.getAttemptCount() >= props.getMaxAttempts()) {
            e.markDead(LocalDateTime.now(), err);
            return;
        }

        LocalDateTime nextRetryAt = calcNextRetryAt(e.getAttemptCount());
        e.markPendingRetry(nextRetryAt, err);
    }

    private LocalDateTime calcNextRetryAt(int attemptCount) {
        // attemptCount는 markProcessing()에서 +1 된 값임
        long base = props.getBaseBackoffMs();
        long max = props.getMaxBackoffMs();

        int exp = Math.max(0, attemptCount - 1); // attempt=1 -> 0
        long delay = base * (1L << Math.min(exp, 20)); // overflow 방지용 캡
        delay = Math.min(delay, max);

        long jitter = (long) (delay * Math.random() * Math.max(0.0, props.getJitterRatio()));
        return LocalDateTime.now().plusNanos((delay + jitter) * 1_000_000);
    }

    private String safeError(Exception ex) {
        String msg = ex.getClass().getSimpleName() + ": " + (ex.getMessage() == null ? "" : ex.getMessage());
        // DB 컬럼/로그 과다 방지를 위한 길이 제한
        return msg.length() > 1000 ? msg.substring(0, 1000) : msg;
    }
}

