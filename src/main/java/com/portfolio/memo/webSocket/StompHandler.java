package com.portfolio.memo.webSocket;

import com.portfolio.memo.auth.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // STOMP 연결 요청(CONNECT)일 때만 JWT 토큰 검증
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // 헤더에서 "Authorization" 키로 토큰을 가져옴
            String jwtToken = accessor.getFirstNativeHeader("Authorization");

            // 토큰 유효성 검사
            // (jwtToken.substring(7)) 이 부분은 실제 토큰 문자열 부분임 "Bearer euJhqiEvqs..." 에서 "Bearer "(공백 포함, 총 7글자)를 자르고 실제 JWT 토큰 문자열만 추출)
            // 즉, JWT 검증 라이브러리는 "Bearer "라는 접두사를 필요하지 않아서 때어버리고 검증에 넘겨주는거임
            if (jwtToken != null && jwtToken.startsWith("Bearer") && jwtTokenProvider.validateToken(jwtToken.substring(7))) {
                // 토큰이 유효하면, 토큰에서 인증 정보를 가져와서 SecurityContext에 설정
                Authentication authentication = jwtTokenProvider.getAuthentication(jwtToken.substring(7));

                // 이 코드를 통해 @MessageMapping이 붙은 컨트롤러 메소드에서 Principal 객체를 통해 사용자 정보를 쉽게 꺼내 쓸 수 있음
                accessor.setUser(authentication);
            } else {
                throw new SecurityException("유효하지 않은 토큰입니다.");
            }
        }
        return message;
    }
}
