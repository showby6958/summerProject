package com.portfolio.memo.config;

import com.portfolio.memo.common.jwt.CustomUserPrincipal;
import com.portfolio.memo.websocket.StompHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompHandler stompHandler;

    @Value("${websocket.endpoint:/ws-notification}")
    private String endpoint;

    @Value("${websocket.allowed-origins:*}")
    private String allowedOrigins;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(endpoint)
                .setAllowedOriginPatterns(allowedOrigins)
                .setHandshakeHandler(new DefaultHandshakeHandler() {
                    @Override
                    protected Principal determineUser(ServerHttpRequest request,
                                                       WebSocketHandler wsHandler,
                                                       Map<String, Object> attributes) {

                        Principal principal = request.getPrincipal();
                        // NotificationDispatcher가 userId 문자열로 convertAndSendToUser를 호출하므로
                        // 세션 Principal의 이름도 userId여야 매칭된다. (기본 Authentication.getName()은
                        // UserDetails.getUsername(), 즉 이메일을 반환해 그대로 쓰면 매칭되지 않는다.)
                        if (principal instanceof Authentication auth
                                && auth.getPrincipal() instanceof CustomUserPrincipal userPrincipal) {
                            String userId = String.valueOf(userPrincipal.getUserId());
                            return () -> userId;
                        }
                        return principal;
                    }
                });
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompHandler);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
        registry.enableSimpleBroker("/queue", "/topic");
    }
}