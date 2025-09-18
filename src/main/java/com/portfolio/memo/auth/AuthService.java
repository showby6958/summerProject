package com.portfolio.memo.auth;

import com.portfolio.memo.auth.dto.JwtToken;
import com.portfolio.memo.auth.dto.LoginRequest;
import com.portfolio.memo.auth.dto.RegisterRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    // jwtRedisTemplate 빈(Bean) 등록
    // (같은 이름의 Bean이 여러개(RedisTemplate) 있을 경우 Spring에게 특정 빈(jwtRedisTemplate)을 사용한다고 지정하는 코드임)
    @Qualifier("jwtRedisTemplate")
    private final RedisTemplate<String, String> jwtRedisTemplate;

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 이메일이 "admin@test.com"인 경우 ADMIN 권한 부여
        UserRole role = "admin@test.com".equals(request.getEmail()) ? UserRole.ADMIN : UserRole.USER;

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(role)
                .build();

        userRepository.save(user);
    }

    @Transactional
    public JwtToken login(LoginRequest request) {
        // 1. Login ID/PW 를 기반으로 Authentication 객체 생성
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword());

        // 2. 실제 검증 (사용자 비밀번호 체크)이 이루어지는 부분
        //    authenticate 메서드가 실행이 될 때 CustomUserDetailsService 에서 만들었던 loadUserByUsername 메서드가 실행됨
        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        // 3. 인증 정보를 기반으로 JWT 토큰 생성
        JwtToken jwtToken = jwtTokenProvider.generateToken(authentication);

        // 4. Redis에 저장할 Key 생성
        String key = "jwt:refresh:" + authentication.getName();

        //5. Redis에 Refresh Token 저장 (Key, Value, TTL 설정)
        jwtRedisTemplate.opsForValue().set(
                key,
                jwtToken.getRefreshToken(),
                jwtTokenProvider.getRefreshTokenValidityInMilliseconds(),
                TimeUnit.MILLISECONDS
        );

        return jwtToken;
    }

    public void logout(String email) {
        String key = "jwt:refresh:" + email;
        jwtRedisTemplate.delete(key);
    }
}
