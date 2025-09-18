package com.portfolio.memo.auth;

import com.portfolio.memo.auth.dto.JwtToken;
import com.portfolio.memo.auth.dto.LoginRequest;
import com.portfolio.memo.auth.dto.RegisterRequest;
import com.portfolio.memo.auth.dto.UserInfoDto;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${jwt.access-token-validity-ms}")
    private Long accessTokenValidityInMilliseconds;

    @Value("${jwt.refresh-token-validity-ms}")
    private Long refreshTokenValidityInMilliseconds;

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
                .maxAge(accessTokenValidityInMilliseconds) // Access Token 만료시간 = 1시간
                .path("/")
                .httpOnly(true)
                .sameSite("Lax")
//                .secure(true) // HTTPS 환경에서만 전송
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());

        // 3. Refresh Token을 httpOnly 쿠키로 전달
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", token.getRefreshToken())
                .maxAge(refreshTokenValidityInMilliseconds)
                .httpOnly(true)
                .sameSite("Lax")
//                .secure(true) // HTTPS 환경에서만 전송
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        return ResponseEntity.ok("로그인에 성공했습니다.");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletResponse response) {
        authService.logout(userDetails.getUsername());

        ResponseCookie cookie = ResponseCookie.from("jwtToken", null)
                .maxAge(0)
                .path("/")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok("로그아웃 되었습니다.");
    }

    // 현재 유저의 정보를 가져오는 api
    // httpOnly 속성 때문에 자바스크립트에서 쿠키를 읽을 수 없어서,
    // 이 엔드포인트로 요청을 보내서 현재 인증된 사용자의 이메일을 알 수 있음)
    @GetMapping("/me")
    public ResponseEntity<UserInfoDto> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails ) {
        if (userDetails != null) {
            // UserInfoDto 객체를 생성해서 사용자 정보를 담아 반환
            UserInfoDto userInfoDto = new UserInfoDto(userDetails.getUsername());
            return ResponseEntity.ok(userInfoDto);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}