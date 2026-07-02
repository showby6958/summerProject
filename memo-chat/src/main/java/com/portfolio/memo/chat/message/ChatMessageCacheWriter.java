package com.portfolio.memo.chat.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.memo.chat.message.dto.ChatMessageBroadcastDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZoneOffset;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageCacheWriter {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;


    private static final long CACHE_MAX_SIZE = 500; // 캐시에 저장할 최신 메시지 수
    private static final Duration MESSAGE_TTL = Duration.ofDays(7);

    public void cache(ChatMessage saved) {
        String zsetKey = "chat:room:" + saved.getRoomId() + ":messages";
        String messageKey = "chat:message:" + saved.getId();

        try {
            ChatMessageBroadcastDto dto = new ChatMessageBroadcastDto(
                    saved.getId(),
                    saved.getRoomId(),
                    saved.getSenderId(),
                    saved.getSenderName(),
                    saved.getContent(),
                    saved.getSentAt()
            );

            String json = objectMapper.writeValueAsString(dto);

            // 1. 개별 메시지 캐싱
            stringRedisTemplate.opsForValue().set(messageKey, json, MESSAGE_TTL);

            // 2. 방 타임라인 ZSET 갱신 (score = createdAt)
            double score = saved.getSentAt().atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
            stringRedisTemplate.opsForZSet().add(zsetKey, messageKey, score);

            // 3. 캐시 최대 크기 제한
            Long size = stringRedisTemplate.opsForZSet().zCard(zsetKey);
            if (size != null && size > CACHE_MAX_SIZE) {
                long over = size - CACHE_MAX_SIZE;
                stringRedisTemplate.opsForZSet().removeRange(zsetKey, 0, over-1);
            }

            // 4. 방 ZSET 자체 TTL 설정
            stringRedisTemplate.expire(zsetKey, MESSAGE_TTL);

        } catch (Exception e) {
            log.warn("Failed to cache chat message. messageId={}, roomId={}", saved.getId(), saved.getRoomId(), e);
        }

    }
}
