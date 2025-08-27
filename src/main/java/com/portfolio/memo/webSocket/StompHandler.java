package com.portfolio.memo.webSocket;

import com.portfolio.memo.auth.CustomUserDetails;
import com.portfolio.memo.auth.CustomUserDetailsService;
import com.portfolio.memo.auth.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // STOMP 연결 요청(CONNECT)일 때만 JWT 토큰 검증
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // 헤더에서 "Authorization" 키로 토큰을 가져옴
            String bearerToken = accessor.getFirstNativeHeader("Authorization");

            // 토큰 유효성 검사
            // (jwtToken.substring(7)) 이 부분은 실제 토큰 문자열 부분임 "Bearer euJhqiEvqs..." 에서 "Bearer "(공백 포함, 총 7글자)를 자르고 실제 JWT 토큰 문자열만 추출)
            // 즉, JWT 검증 라이브러리는 "Bearer "라는 접두사를 필요하지 않아서 때어버리고 검증에 넘겨주는거임
            if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
                String token = bearerToken.substring(7); // "Bearer " 제거
                if (jwtTokenProvider.validateToken(token)) {
                    // 토큰에서 이메일(subject) 추출
                    String email = jwtTokenProvider.getUsernameFromToken(token);

                    // 이메일 기반으로 CustomUserDetails 조회
                    CustomUserDetails userDetails =
                            (CustomUserDetails) customUserDetailsService.loadUserByUsername(email);

                    // Authentication 생성
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                    // Stomp accesor에 인증 정보 설정 -> 컨트롤러에서 Principal로 사용 가능
                    accessor.setUser(authentication);
                } else {
                    throw new SecurityException("유효하지 않은 토큰입니다.");
                }
            } else {
                throw new SecurityException("JWT 토큰이 없습니다.");
            }
        }

        return message;
    }
}
