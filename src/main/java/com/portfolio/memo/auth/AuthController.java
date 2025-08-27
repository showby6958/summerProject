package com.portfolio.memo.auth;

import com.portfolio.memo.auth.dto.JwtToken;
import com.portfolio.memo.auth.dto.LoginRequest;
import com.portfolio.memo.auth.dto.RegisterRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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
        // 2. 쿠키 생성
        ResponseCookie cookie = ResponseCookie.from("jwtToken", token.getAccessToken())
                .maxAge(3600)
                .httpOnly(true)
                .path("/")
                .build();
        // 3. 응답헤더에 쿠키 추가
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());


        return ResponseEntity.ok("로그인에 성공했습니다.");
    }
}