package com.portfolio.memo.webSocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@RequiredArgsConstructor
@EnableWebSocketMessageBroker // STOMP를 사용하는 WebSocket 메시징을 활성화
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompHandler stompHandler; // JWT 처리할 핸들러

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 메시지 브로커가 처리할 주제(prefix)를 설정 (필수 O)
        // "/queue"는 1대1 메시징 "/topic"은 1:N(브로드캐스팅) 메시징에 사용
        config.enableSimpleBroker("/queue", "/topic");

        // 클라이언트가 서버로 메시지를 보낼 때 사용할 prefix
        // @MessageMapping 사용시 필수
        config.setApplicationDestinationPrefixes("/app");

        // 1:1 메시징을 위해 사용자별 목적지를 처리할 prefix (사용자 지정 메시지)
        // 위 2개는 메시지를 보낼 때 필수로 필요한 코드. 아래는 특정 유저에 메시지를 보낼 때 필요한 기능(필수 X)
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 클라이언트가 WebSocket에 연결힐 때 사용할 엔드포인트 등록
        // 예: const socket = new SockJS('/ws-chat');
        // 기존 chat.html을 위한 SockJS 엔드포인트
        registry.addEndpoint("/ws-chat").withSockJS();
        // Postman 등 표준 WebSocket 클라이언트 테스트를 위한 엔드포인트
        registry.addEndpoint("/ws-native");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 클라이언트로부터 들어오는 메시지를 처리하기 전에 stompHandler를 거치도록 인터셉터로 등록
        // 이 핸들러에서 JWT 인증을 수행
        registration.interceptors(stompHandler);
    }
}
