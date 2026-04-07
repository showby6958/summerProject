package com.portfolio.memo.outbox.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

// Outbox 이벤트 저장용 Repository (Relay 단계에서 조회 쿼리 추가 예정)
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    // PENDING + next_retry_at 이벤트를 배치로 잠그고 가져옴(트랜잭션 안에서 호출)
    // SKIP LOCKED로 다른 Relay 인스턴스와 충돌 회피
    @Query(value = """
            SELECT *
            FROM outbox_event
            WHERE status = 'PENDING'
                AND next_retry_at <= NOW()
            ORDER BY id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> lockPendingBatch(@Param("batchSize") int batchSize);


    // 오래 걸린 PROCESSING을 PENDING으로 되돌려 재시도 가능하게 함
    @Modifying
    @Query(value = """
            UPDATE outbox_event
            SET status = 'PENDING',
                locked_at = NULL,
                locked_by = NULL
            WHERE status = 'PROCESSING'
                AND locked_at IS NOT NULL
                AND locked_at < :cutoff
            """, nativeQuery = true)
    int requeueStaleProcessing(@Param("cutoff")LocalDateTime cutoff);
}
