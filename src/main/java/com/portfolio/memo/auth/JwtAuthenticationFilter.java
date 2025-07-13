package com.portfolio.memo.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends GenericFilterBean {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // 1. 요청(HttpServletRequest)에서 토큰을 추출한다.
        String token = resolveToken((HttpServletRequest) request);

        // 2. 토큰이 존재하고(not null), 유효한지(validate) 검사한다.
        if (token != null && jwtTokenProvider.validateToken(token)) {
            // 3. 토큰이 유효하다면, 토큰으로부터 인증(Authentication) 객체를 가져온다.
            Authentication authentication = jwtTokenProvider.getAuthentication(token);
            // 가져온 인증 객체를 SecurityContextHolder에 저장(set)한다.
            // 여기에 인증 정보를 저장하면, 해당 요청을 처리하는 동안 @AuthenticationPrincipal 어노테이션 등을 통해
            // 언제든지 인증된 사용자 정보를 참조할 수 있게 된다.
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        // 5. 다음 필터 체인으로 요청과 응답을 전달한다.
        // 이 필터의 역할이 끝났으니, 다음 필터가 이어서 작업을 처리하도록 넘겨준다.
        // 만약 여기서 chain.doFilter()를 호출하지 않으면, 요청 처리가 중단된다.
        chain.doFilter(request, response);
    }

    //* HttpServeletRequest에서 "Authorization" 헤더를 찾아 토큰을 추출하는 헬퍼 메소드
    private String resolveToken(HttpServletRequest request) {
        // "Authorization" 헤더 값을 가져온다.
        String bearerToken = request.getHeader("Authorization");

        // 헤더 값이 존재하고(hasText), Bearer 로 시작하는지 확인한다.
        // JWT는 보통 "Bearer <토큰값>" 형식으로 전달된다.
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer")) {
            // Bearer 접두사를 제외한 실제 토큰 문자열 부분만 잘라내서 반환한다.(7번째 인덱스 부터)
            return bearerToken.substring(7);
        }

        // 토큰이 없거나 형식이 올바르지 않으면 null 반환
        return null;
    }
}
