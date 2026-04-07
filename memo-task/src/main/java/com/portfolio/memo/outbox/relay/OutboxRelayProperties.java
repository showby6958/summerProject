package com.portfolio.memo.outbox.relay;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "outbox.relay")
public class OutboxRelayProperties {

    // Relay 실행 주기(ms)
    private long fixedDelayMs = 200;

    // 한 번에 claim 할 outbox row 수
    private int batchSize = 100;

    // 최대 재시도 횟수(이 이상이면 DEAD)
    private int maxAttempts = 10;

    // 백오프 기본(ms), attempt=1 일 때 적용되는 시작 값
    private long baseBackoffMs = 1000;

    // 백오프 최대(ms)
    private long maxBackoffMs = 300_000;

    // 지터 비율(0.0~1.0) 예: 0.2면 최대 20% 랜덤 추가
    private double jitterRatio = 0.2;

    // PROCESSING 상태가 이 시간(초) 이상 지속되면 "stale"로 간주하고 PENDING으로 되돌림
    private long processingTimeoutSeconds = 60;

    // Redis Streams 키 (단일 스트림)
    private String streamKey = "stream:task:events:0";

}
