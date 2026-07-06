package com.portfolio.memo.auth;

import com.portfolio.memo.common.jwt.CustomUserPrincipal;
import com.portfolio.memo.common.jwt.JwtTokenProvider;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CustomLogoutHandler implements LogoutHandler {

    @Qualifier("jwtRedisTemplate")
    private final RedisTemplate<String, String> jwtRedisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${cookie.secure}")
    private boolean cookieSecure;

    @Override
    public void logout(HttpServletRequest request,
                       HttpServletResponse response,
                       Authentication authentication) {


        // 클라이언트의 쿠키 삭제 (로그인 시 발급한 쿠키와 속성이 같아야 브라우저가 확실히 덮어씀)
        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", "")
                .maxAge(0)
                .path("/")
                .httpOnly(true)
                .sameSite("Lax")
                .secure(cookieSecure)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", "")
                .maxAge(0)
                .path("/")
                .httpOnly(true)
                .sameSite("Lax")
                .secure(cookieSecure)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        // userId 추출 (1순위: SecurityContext, 2순위: 쿠키 토큰)
        Long userId = null;

        // 1순위. SecurityContext 조회 (없으면 2순위 쿠키 토큰에서 조회)
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserPrincipal principal) {
            userId = principal.getUserId();
        }

        // 2순위. 쿠키에서 직접 추출
        if (userId == null) {
            // authentication 객체가 없는 경우(토큰 만료 등), 쿠키에서 직접 토큰을 파싱하여 추출
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                Optional<Cookie> accessTokenFromCookie = Arrays.stream(cookies)
                        .filter(c -> "accessToken".equals(c.getName()))
                        .findFirst();

                if (accessTokenFromCookie.isPresent()) {
                    String token = accessTokenFromCookie.get().getValue();
                    try { // 정상 토큰에서 이메일 추출
                        userId = jwtTokenProvider.getUserId(token);
                    } catch (ExpiredJwtException e) {
                        // 만료된 토큰에서도 claims에서 userId 추출
                        userId = e.getClaims().get("userId", Long.class);
                    } catch (Exception ignored) { // 그 외 다른 예외는 무시

                    }
                }
            }
        }

        // Redis에서 Refresh Token 삭제
        if (userId != null) {
            String key = "jwt:refresh:" + userId;
            jwtRedisTemplate.delete(key);
        }

    }
}
