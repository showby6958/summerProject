package com.portfolio.memo.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.memo.NotificationDispatcher;
import com.portfolio.memo.config.NotificationStreamProperties;
import com.portfolio.memo.domain.ProcessedEvent;
import com.portfolio.memo.domain.ProcessedEventRepository;
import com.portfolio.memo.dto.TaskNotificationEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


import java.time.Duration;
import java.util.List;
import java.util.Map;


@Slf4j
@Component
@RequiredArgsConstructor
public class TaskEventStreamConsumer {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final NotificationStreamProperties props;
    private final ProcessedEventRepository processedEventRepository;
    private final NotificationDispatcher dispatcher;

    @PostConstruct
    public void ensureGroup() {
        try {
            RedisConnectionFactory cf = redisTemplate.getConnectionFactory();
            try (RedisConnection conn = cf.getConnection()) {
                // XGROUP CREATE <streamKey> <groupName> 0 MKSTREAM
                conn.xGroupCreate(
                        props.getKey().getBytes(),
                        props.getGroup(),
                        ReadOffset.from("0-0"),
                        true
                );
                log.info("Created consumer group. streamKey = {} group = {}", props.getKey(), props.getGroup());
            }
        } catch (Exception e) {
            String msg = e.getMessage() == null ?  "" : e.getMessage();
            if (msg.contains("BUSYGROUP")) {
                log.info("Consumer group already exists. streamKey = {} group = {}", props.getKey(), props.getGroup());
                return;
            }
            log.warn("Failed to create consumer group. streamKey = {} group = {} err = {}", props.getKey(), props.getGroup(), msg);
        }
    }

    @Scheduled(fixedDelayString = "${notification.stream.poll-time-out-ms:2000}")
    public void poll() {
        // 1. 읽기(readBatch): Redis Streams에서 consumer group 방식으로 메시지를 읽어옴
        List<MapRecord<String, Object, Object>> records = readBatch();

        if (records == null || records.isEmpty()) {
            return;
        }

        // 2. 처리(handleOne): payload를 파싱하고, WebSocket 전송 수행
        for (MapRecord<String, Object, Object> record : records) {
            boolean ack = handleOne(record);

            // 3. ACK(acknowledge): Redis에 Ack 알림
            if (ack) {
                acknowledge(record.getId());
            }
        }
    }

    // XREADGROUP GROUP <group> <consumer> COUNT <batch> BLOCK <timeout> STREAMS <key> >
    private List<MapRecord<String, Object, Object>> readBatch() {
        try {
            StreamReadOptions options = StreamReadOptions.empty()
                    .count(props.getBatchSize())
                    .block(Duration.ofMillis(props.getPollTimeoutMs())); // busy-loop 방지

            return redisTemplate.opsForStream().read(
                    Consumer.from(props.getGroup(), props.getConsumer()),
                    options,
                    StreamOffset.create(props.getKey(), ReadOffset.lastConsumed())
            );
        } catch (Exception e) {
            log.warn("Failed to read from stream. streamKey = {} group = {} consumer = {}", props.getKey(), props.getGroup(), props.getConsumer(), e);
            return List.of();
        }
    }

    private boolean handleOne(MapRecord<String, Object, Object> record) {
        Map<Object, Object> v = record.getValue();
        String payload = asString(v.get("payload"));
        String streamEventId = asString(v.get("eventId"));
        String streamEventType = asString(v.get("eventType"));

        if (payload == null || payload.isBlank()) {
            log.warn("Empty payload. recordId = {} streamEventId = {}", record.getId(), streamEventId);
            return true;
        }

        TaskNotificationEvent event;
        try {
            event = objectMapper.readValue(payload, TaskNotificationEvent.class);
        } catch (Exception ex) {
            log.warn("Failed to parse payload. record = {} streamEventId = {} payload = {}", record.getId(), streamEventId, payload, ex);
            return true;
        }

        try {
            return processIdempotentAndDispatcher(streamEventId, streamEventType, event);
        } catch (Exception ex) {
            log.warn("Failed to process record. recordId = {} streamEventId = {}", record.getId(), streamEventId, ex);
            return false;
        }
    }

    @Transactional
    public boolean processIdempotentAndDispatcher(String streamEventId, String streamEventType, TaskNotificationEvent event) {
        String dedupKey = (event.getEventId() != null && !event.getEventId().isBlank())
                ? event.getEventId()
                : streamEventId;

        // 1. 멱등 INSERT (중복이면 예외 -> duplicate 처리)
        try {
            processedEventRepository.save(new ProcessedEvent(dedupKey, streamEventType));
            processedEventRepository.flush();
        } catch (DataIntegrityViolationException dup) {
            // 이미 처리됨 -> ACK
            return true;
        }

        // 2. 전송 (전송 실패 시 재전송 X)
        List<Long> recipients = event.getRecipientUserIds();
        if (recipients == null || recipients.isEmpty()) {
            return true; // 수신자 없으면 처리 완료로 ACK
        }

        for (Long userId : recipients) {
            dispatcher.dispatchToUser(userId, event);
        }

        // 성공/실패 무관하게 ACk
        return true;
    }

    private void acknowledge(RecordId id) {
        try {
            redisTemplate.opsForStream().acknowledge(props.getKey(), props.getGroup(), id);
        } catch (Exception e) {
            log.warn("Failed to ack record. key = {} group = {} id = {}", props.getKey(), props.getGroup(), id, e);
        }
    }

    private String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
