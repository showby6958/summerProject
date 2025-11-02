package com.portfolio.memo.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
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

    //  GenericJackson2JsonRedisSerializer() 가 java.time 패키지의 시간 관련 객체(LocalDateTime, LocalDate) 를 지원하지 않아서
    //  시간 객체를 직렬화 및 역직렬화 하려면 별도 모둘을 설정해야함
    //  즉, JavaTimeModule이 등록된 ObjectMapper를 생성하고, 이 ObjectMapper를 GenericJackson2JsonRedisSerializer에
    //  주입하여 RedisTemplate을 설정해야 한다.
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // LocalDateTime 타입을 올바르게 직렬화/역직렬화하기 위해 JavaTimeModule 등록
        mapper.registerModule(new JavaTimeModule());
        // 날짜를 타임스탬프(숫자)가 아닌 ISO-8601 형식의 문자열로 직렬화하도록 설정
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }


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
    public RedisTemplate<String, String> jwtRedisTemplate(
            @Qualifier("jwtRedisConnectionFactory") RedisConnectionFactory factory)
    {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
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

    // 채팅 메시지(JSON) JSON 직렬화 진행 (<키, 값> ->  <String, String> 로 설정)
    @Bean(name = "chatRedisTemplate")
    public RedisTemplate<String, String> chatRedisTemplate(
            @Qualifier("chatRedisConnectionFactory") RedisConnectionFactory factory
    ) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }

    // 읽음 상태/카운트 전용 RedisTemplate<String, String>
    // *Redis 인스턴스가 아님* , Redis랑 통신하기 위한 클라이언트 객체만 하나더 만든거임
    // 여러 RedisTemplate는 같은 Redis 인스턴스를 가리킬 수 있음. 하지만 제네릭 타입, Serializer, 목적을 다르게 설정 가능
    @Bean(name = "chatCountRedisTemplate")
    public StringRedisTemplate chatCountRedisTemplate(
            @Qualifier("chatRedisConnectionFactory") RedisConnectionFactory factory
    ) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(factory);
        return template;
    }
}
