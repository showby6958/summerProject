package com.portfolio.memo.auth;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CustomLogoutHandler implements LogoutHandler {

    @Qualifier("jwtRedisTemplate")
    private final RedisTemplate<String, String> jwtRedisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void logout(HttpServletRequest request,
                       HttpServletResponse response,
                       Authentication authentication) {


        // 클라이언트의 쿠키 삭제
        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", "")
                .maxAge(0)
                .path("/")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", "")
                .maxAge(0)
                .path("/")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        // Redis에서 Refresh Token 삭제
        String email = null;

        // 유효한 Authentication 객체에서 이메일 추출
        if (authentication != null && authentication.getName() != null) {
            email = authentication.getName();
        } else {
            // authentication 객체가 없는 경우(토큰 만료 등), 쿠키에서 직접 토큰을 파싱하여 이메일 추출
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                Optional<Cookie> accessTokenFromCookie = Arrays.stream(cookies)
                        .filter(c -> "accessToken".equals(c.getName()))
                        .findFirst();

                if (accessTokenFromCookie.isPresent()) {
                    String token = accessTokenFromCookie.get().getValue();
                    try { // 정상 토큰에서 이메일 추출
                        email = jwtTokenProvider.getUsernameFromToken(token);
                    } catch (ExpiredJwtException e) { // 만료된 토큰에서도 claims를 통해 이메일 추출
                        email = e.getClaims().getSubject();
                    } catch (Exception e) { // 그 외 다른 예외는 무시

                    }
                }
            }
        }
        // Redis에서 Refresh Token 삭제
        if (email != null) {
            String key = "jwt:refresh:" + email;
            jwtRedisTemplate.delete(key);
        }

    }
}
