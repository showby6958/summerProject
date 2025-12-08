package com.portfolio.memo.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.memo.notification.dto.NotificationEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    @Qualifier("chatCountRedisTemplate")
    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    public void publish(NotificationEventDto dto) {
        try {
            String channel = "notification:user:" + dto.getUserId();
            String payload = objectMapper.writeValueAsString(dto);

            redisTemplate.convertAndSend(channel, payload);

            log.info("Published to Redis channel={} payload={}", channel, payload);

        } catch (Exception e) {
            log.error("Failed to publish notification", e);
        }
    }
}
