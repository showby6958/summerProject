package com.portfolio.memo.webSocket;

import com.portfolio.memo.auth.CustomUserDetails;
import com.portfolio.memo.auth.CustomUserDetailsService;
import com.portfolio.memo.auth.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // WebSocketConfig의 HandShakeHandler에서 HTTP인증 결과를 복사해와서 사용자 정보가 있는지 확인
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            if (accessor.getUser() == null) {
                log.error("StompHandler - User is not authenticated");
                throw new SecurityException("STOMP: 사용자 인증 정보가 없습니다.");
            }
            log.info("StompHandler - User connected: {}", accessor.getUser().getName());
        }

        return message;
    }
}
