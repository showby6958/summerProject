package com.portfolio.memo.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    public void doFilterInternal(HttpServletRequest request,
                                 HttpServletResponse response,
                                 FilterChain filterChain)
            throws IOException, ServletException {

        // 1. 요청(HttpServletRequest)에서 토큰을 추출한다.
        String token = resolveToken(request);

        // 2. 토큰이 존재하고(not null), 유효한지(validate) 검사한다.
        if (token != null && jwtTokenProvider.validateToken(token)) {
            String email = jwtTokenProvider.getUsernameFromToken(token);
            // 이메일로 DB에서 유저 조회후 CustomUserDetails 반환
            CustomUserDetails userDetails =
                    (CustomUserDetails) customUserDetailsService.loadUserByUsername(email);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            // 가져온 인증 객체를 SecurityContextHolder에 저장(set)한다.
            // 여기에 인증 정보를 저장하면, 해당 요청을 처리하는 동안 @AuthenticationPrincipal 어노테이션 등을 통해
            // 언제든지 인증된 사용자 정보를 참조할 수 있게 된다.
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        // 5. 다음 필터 체인으로 요청과 응답을 전달한다.
        // 이 필터의 역할이 끝났으니, 다음 필터가 이어서 작업을 처리하도록 넘겨준다.
        // 만약 여기서 chain.doFilter()를 호출하지 않으면, 요청 처리가 중단된다.
        filterChain.doFilter(request, response);
    }

    //* 쿠키에서 jwtToken을 추출하는 헬퍼 메소드 (추출만 하고 검증은 doFilterInternal 에서 진행)
    private String resolveToken(HttpServletRequest request) {
        // 쿠키에서 jwtToken을 읽어옴
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // 토큰이 없거나 형식이 올바르지 않으면 null 반환
        return null;
    }
}
