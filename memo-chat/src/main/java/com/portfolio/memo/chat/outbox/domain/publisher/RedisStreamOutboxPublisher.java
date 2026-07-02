package com.portfolio.memo.chat.outbox.domain.publisher;

import com.portfolio.memo.chat.outbox.domain.OutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RedisStreamOutboxPublisher {

    private final StringRedisTemplate redisTemplate;

    private static final String STREAM_KEY = "stream:chat:events:0";

    // Streams에 이벤트 발행 (XADD)
    public RecordId publish(OutboxEvent event) {

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("eventId", event.getEventUuid()); // 멱등성 키(UUID)
        fields.put("eventType", event.getEventType().name());
        fields.put("aggregateType", event.getAggregateType());
        fields.put("aggregateId", String.valueOf(event.getAggregateId()));
        fields.put("createdAt", event.getCreatedAt().toString());
        fields.put("payload", event.getPayload()); // JSON 문자열

        return redisTemplate.opsForStream()
                .add(StreamRecords.mapBacked(fields).withStreamKey(STREAM_KEY));
    }

}
