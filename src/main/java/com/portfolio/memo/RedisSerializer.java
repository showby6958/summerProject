package com.portfolio.memo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSerializer {

    private final ObjectMapper objectMapper;

    public <T> String serialize(T object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object for Redis", e);
            // 예외를 던지거나 null을 반환하는 등 정책에 맞게 처리
            throw new RuntimeException("Redis serialization error", e);
        }
    }

    public <T> T deserialize(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize object from Redis", e);
            // 예외를 던지거나 null을 반환하는 등 정책에 맞게 처리
            throw new RuntimeException("Redis deserialization error", e);
        }
    }
}
