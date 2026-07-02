package com.portfolio.memo.chat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class ChatRedisConfig {

    // Chat Redis properties (채팅 메시지 처리용)
    @Value("${spring.redis.chat.host}")
    private String chatRedisHost;
    @Value("${spring.redis.chat.port}")
    private int chatRedisPort;
    @Value("${spring.redis.chat.password}")
    private String chatRedisPassword;

    @Bean
    public RedisConnectionFactory chatRedisConnectionFactory() {
        RedisStandaloneConfiguration conf = new RedisStandaloneConfiguration();
        conf.setHostName(chatRedisHost);
        conf.setPort(chatRedisPort);
        conf.setPassword(chatRedisPassword);

        LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(conf);

        return lettuceConnectionFactory;
    }

    // 채팅 메시지(JSON) JSON 직렬화 진행 (<키, 값> ->  <String, String> 로 설정)
    @Bean
    public StringRedisTemplate chatStringRedisTemplate(RedisConnectionFactory chatRedisConnectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(chatRedisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        return template;
    }
//
//    // 읽음 상태/카운트 전용 RedisTemplate<String, String>
//    // *Redis 인스턴스가 아님* , Redis랑 통신하기 위한 클라이언트 객체만 하나더 만든거임
//    // 여러 RedisTemplate는 같은 Redis 인스턴스를 가리킬 수 있음. 하지만 제네릭 타입, Serializer, 목적을 다르게 설정 가능
//    @Bean(name = "chatCountRedisTemplate")
//    public StringRedisTemplate chatCountRedisTemplate(
//            @Qualifier("chatRedisConnectionFactory") RedisConnectionFactory factory
//    ) {
//        StringRedisTemplate template = new StringRedisTemplate();
//        template.setConnectionFactory(factory);
//        return template;
//    }

}
