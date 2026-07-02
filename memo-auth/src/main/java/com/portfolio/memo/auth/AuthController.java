package com.portfolio.memo.auth;

import com.portfolio.memo.auth.dto.JwtToken;
import com.portfolio.memo.auth.dto.LoginRequest;
import com.portfolio.memo.auth.dto.RegisterRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${jwt.access-token-validity-seconds}")
    private Long accessTokenValiditySeconds;

    @Value("${jwt.refresh-token-validity-seconds}")
    private Long refreshTokenValiditySeconds;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("회원가입이 완료되었습니다.");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        // 1. AuthService에서 로그인 처리 후 토큰 받아오기
        JwtToken token = authService.login(request);

        // jwt 저장소 -> cookie 에 저장
        // 2. Access Token을 httpOny 쿠키로 전달
        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", token.getAccessToken())
                .maxAge(accessTokenValiditySeconds) // Access Token 만료시간 = 1시간
                .path("/")
                .httpOnly(true)
                .sameSite("Lax")
//                .secure(true) // HTTPS 환경에서만 전송
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());

        // 3. Refresh Token을 httpOnly 쿠키로 전달
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", token.getRefreshToken())
                .maxAge(refreshTokenValiditySeconds) // Refresh Token 만료시간 = 7일
                .path("/")
                .httpOnly(true)
                .sameSite("Lax")
//                .secure(true) // HTTPS 환경에서만 전송
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        return ResponseEntity.ok("로그인에 성공했습니다.");
    }



    // -- 다른 서비스에서 사용자 조회용 API --
    // 사용자 존재 여부 확인 API
    @GetMapping("users/{userId}/exists")
    public ResponseEntity<Boolean> existsUser(@PathVariable Long userId) {
        boolean exists = authService.existsById(userId);

        return ResponseEntity.ok(exists);
    }

    // 사용자 이름 조회 API
    @GetMapping("users/{userId}/name")
    public ResponseEntity<String> getUsername(@PathVariable Long userId) {
        String username = authService.getUsernameById(userId);

        return ResponseEntity.ok(username);
    }

}