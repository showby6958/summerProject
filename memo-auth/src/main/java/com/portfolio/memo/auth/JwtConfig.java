package com.portfolio.memo.auth;

import com.portfolio.memo.common.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Bean
    public JwtTokenProvider jwtTokenProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access-token-validity-ms}") long accessMs,
            @Value("${jwt.refresh-token-validity-ms}") long refreshMs) {

        return new JwtTokenProvider(secretKey, accessMs, refreshMs);
    }
}
