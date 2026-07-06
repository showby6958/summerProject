package com.portfolio.memo.auth;

import com.portfolio.memo.auth.dto.JwtToken;
import com.portfolio.memo.auth.dto.LoginRequest;
import com.portfolio.memo.auth.dto.RegisterRequest;
import com.portfolio.memo.auth.dto.UserSummaryResponse;
import com.portfolio.memo.common.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final RedisTemplate<String, String> jwtRedisTemplate;

    // @Autowired로 자동 주입 권장 X. spring이 어떤 Bean에 주입해야하는지 못찾음
    // @Qualifier를 final 필드에 붙이는 것보다  @Qualifier도 생성자 매개변수 쪽에 붙이는게 좋음
    public AuthService (
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtTokenProvider jwtTokenProvider,
        AuthenticationManager authenticationManager,
        // jwtRedisTemplate 빈(Bean) 등록
        // (같은 이름의 Bean이 여러개(RedisTemplate) 있을 경우 Spring에게 특정 빈(jwtRedisTemplate)을 사용한다고 지정하는 코드임)
        @Qualifier("jwtRedisTemplate") RedisTemplate<String, String> jwtRedisTemplate
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
        this.jwtRedisTemplate = jwtRedisTemplate;

    }


    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }


        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(UserRole.USER)
                .build();

        try {
            // saveAndFlush로 즉시 INSERT를 실행해, 동시 가입 요청으로 인한 unique 제약 위반을
            // 트랜잭션 커밋 시점이 아니라 여기서 바로 잡아낸다.
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
    }

    @Transactional
    public JwtToken login(LoginRequest request) {

        // 1. 이메일/비밀번호 검증
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // 2. DB에서 유저 엔티티 조회 (userId, role 가져오기)
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));


        // 3. 공통 JwtTokenProvider로 Access/Refresh 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
        String refreshToken = jwtTokenProvider.createRefreshToken(
                user.getId(),
                user.getEmail()
        );

        // 4. Redis 에 RefreshToken 저장 (key는 userId 기반)
        String key = "jwt:refresh:" + user.getId();

        jwtRedisTemplate.opsForValue().set(
                key,
                refreshToken,
                jwtTokenProvider.getRefreshTokenValidityMs(),
                TimeUnit.MILLISECONDS
        );

        // 5. 응답용 DTO
        return JwtToken.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }


    public boolean existsById(Long userId) {
        return userRepository.existsById(userId);
    }

    public String getUsernameById(Long userId) {
        return userRepository.findById(userId)
                .map(User::getName)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    // 다른 서비스에서 여러 유저를 한 번에 조회 (N+1 blocking 호출 방지용 배치 조회)
    public List<UserSummaryResponse> getUsersByIds(List<Long> userIds) {
        return userRepository.findAllById(userIds).stream()
                .map(UserSummaryResponse::from)
                .toList();
    }
}
