package com.portfolio.memo.common.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;


public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;


    public JwtTokenProvider(String secretKey,
                            long accessTokenValidityMs,
                            long refreshTokenValidityMs) {

        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenValidityMs = accessTokenValidityMs;
        this.refreshTokenValidityMs = refreshTokenValidityMs;
    }

    // Auth 서버 전용 - AccessToken 생성
    public String createAccessToken(Long userId, String email, String userName, String role) {
        Date now = new Date();

        return Jwts.builder()
                .setSubject(email)
                .claim("userId", userId)
                .claim("userName", userName) // 다른 서비스에서 유저 이름이 필요해서 넣음(유저 서비스에 API 요청 하기 싫어서)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + accessTokenValidityMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // Auth 서버 전용 - RefreshToken 생성
    public String createRefreshToken(Long userId, String email) {
        Date now = new Date();

        return Jwts.builder()
                .setSubject(email)
                .claim("userId", userId)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + refreshTokenValidityMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }


    // 모든 서비스 공통 - 토큰 검증
    public boolean validate(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // 모든 서비스 공통 - userId 추출
    public Long getUserId(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("userId", Long.class);
    }

    public String getUserName(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("userName", String.class);
    }

    // 모든 서비스 공통 - role 추출
    public String getRole(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("role", String.class);
    }


    public long getRefreshTokenValidityMs() {
        return refreshTokenValidityMs;
    }
}
