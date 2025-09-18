package com.portfolio.memo.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableRedisRepositories(redisTemplateRef = "jwtRedisTemplate")
public class RedisConfig {
    // 총 2개의 인스턴스 생성
    // JWT Refresh Token을 위한 인스턴스, 채팅 메시지 처리를 위한 인스턴스

    // JWT Redis properties (JWT Refresh Token 저장용)
    @Value("${spring.redis.jwt.host}")
    private String jwtRedisHost;
    @Value("${spring.redis.jwt.port}")
    private int jwtRedisPort;
    @Value("${spring.redis.jwt.password}")
    private String jwtRedisPassword;

    // Chat Redis properties (채팅 메시지 처리용)
    @Value("${spring.redis.chat.host}")
    private String chatRedisHost;
    @Value("${spring.redis.chat.port}")
    private int chatRedisPort;
    @Value("${spring.redis.chat.password}")
    private String chatRedisPassword;

    // --- JWT Redis 설정 ---
    @Bean(name = "jwtRedisConnectionFactory")
    public RedisConnectionFactory jwtRedisConnectionFactory() {
        RedisStandaloneConfiguration jwtRedisStandaloneConfiguration = new RedisStandaloneConfiguration();
        jwtRedisStandaloneConfiguration.setHostName(jwtRedisHost);
        jwtRedisStandaloneConfiguration.setPort(jwtRedisPort);
        jwtRedisStandaloneConfiguration.setPassword(jwtRedisPassword);

        LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(jwtRedisStandaloneConfiguration);

        return lettuceConnectionFactory;
    }

    @Bean(name = "jwtRedisTemplate")
    public RedisTemplate<String, String> jwtRedisTemplate() {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(jwtRedisConnectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }

    // --- 채팅 Redis 설정 ---
    @Bean(name = "chatRedisConnectionFactory")
    public RedisConnectionFactory chatRedisConnectionFactory() {
        RedisStandaloneConfiguration chatRedisStandaloneConfiguration = new RedisStandaloneConfiguration();
        chatRedisStandaloneConfiguration.setHostName(chatRedisHost);
        chatRedisStandaloneConfiguration.setPort(chatRedisPort);
        chatRedisStandaloneConfiguration.setPassword(chatRedisPassword);

        LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(chatRedisStandaloneConfiguration);

        return lettuceConnectionFactory;
    }

    // 채팅 메시지(JSON) JSON 직렬화 진행 (<키, 값> ->  <String, Object> 로 설정)
    @Bean(name = "chatRedisTemplate")
    public RedisTemplate<String, Object> chatRedisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(chatRedisConnectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        // GenericJackson2JsonRedisSerializer() -> 객체를 JSON 형태로 직렬화하는 구현체
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

}
