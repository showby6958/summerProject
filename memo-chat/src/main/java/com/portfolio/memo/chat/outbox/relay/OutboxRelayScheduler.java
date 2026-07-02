package com.portfolio.memo.chat.outbox.relay;

import com.portfolio.memo.chat.outbox.domain.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

// 주기적으로 Relay 실행(복구 -> claim -> publish)
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(OutboxRelayProperties.class)
public class OutboxRelayScheduler {

    private final OutboxRelayService relayService;
    private final OutboxRelayProperties props;

    // fixedDelay: 이전 실행이 끝난 뒤 지정 ms 후에 다시 실행
    @Scheduled(fixedDelayString = "#{outboxRelayProperties.fixedDelayMs}")
    public void tick() {
        // 1. 서버 다운 등으로 PROCESSING에 걸린 이벤트 복구
        int recovered = relayService.requeueStaleProcessing();
        if (recovered > 0) {
            log.warn("Recovered stale PROCESSING outbox events: {}", recovered);
        }

        // 2. 배치 claim
        List<OutboxEvent> claimed = relayService.claimBatch();
        if (claimed.isEmpty()) {
            return;
        }

        // 3. 발행
        for (OutboxEvent e : claimed) {
            relayService.publishOne(e);
        }
    }
}
