package com.portfolio.memo.common.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                 HttpServletResponse response,
                                 FilterChain filterChain)
            throws IOException, ServletException {

        // 1. 요청(HttpServletRequest)에서 토큰을 추출한다.
        String token = resolveToken(request);

        // 2. 토큰이 존재하고(not null), 유효한지(validate) 검사한다.
        if (token != null && jwtTokenProvider.validate(token)) {

            Long userId = jwtTokenProvider.getUserId(token);
            String userName = jwtTokenProvider.getUserName(token);
            String role = jwtTokenProvider.getRole(token);

            // 권한 생성
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);

            // CustomPrincipal 생성
            CustomUserPrincipal principal = new CustomUserPrincipal(
                    userId,
                    userName,
                    role,
                    List.of(authority)
            );

            // Authentication 객체 생성
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal, // 유저 정보
                    null, // Credentials
                    principal.getAuthorities()
            );

            // 3. 인증 객체를 SecurityContextHolder에 저장(set)한다.
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        // 4. 다음 필터 체인으로 요청과 응답을 전달한다.
        // 이 필터의 역할이 끝났으니, 다음 필터가 이어서 작업을 처리하도록 넘겨준다.
        // 만약 여기서 chain.doFilter()를 호출하지 않으면, 요청 처리가 중단된다.
        filterChain.doFilter(request, response);
    }

    // 쿠키 또는 헤더에서 jwtToken을 추출하는 헬퍼 메소드
    private String resolveToken(HttpServletRequest request) {
        // 1. Authorization 헤더에서 추출
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 2. 쿠키에서 추출
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
