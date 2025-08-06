package com.portfolio.memo.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import com.portfolio.memo.auth.dto.JwtToken;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

    private final Key key;

    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    //* Authentication 객체를 기반으로 Access Token과 Refresh Token을 생성함 */
    public JwtToken generateToken(Authentication authentication) {
        // 사용자의 권한(Role) 정보를 쉼표로 구분된 문자열로 만듬
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime();
        // Access Token 만료 시간 설정 (햔재 시간으로부터 24시간)
        Date accessTokenExpiresIn = new Date(now + 86400000);

        // Access Token 생성
        String accessToken = Jwts.builder()
                .setSubject(authentication.getName()) // 토큰 주체로 사용자 이름(ID)을 설정
                .claim("auth", authorities) // auth 라는 이름의 클레임에 권한 정보를 저장
                .setExpiration(accessTokenExpiresIn) // 만료 시간 설정
                .signWith(key, SignatureAlgorithm.HS256) // 준비된 key랑 HS256 알고리즘으로 서명
                .compact(); // 최종적으로 토큰 문자열 생성

        // Refresh Token 생성 (별도 정보 없이 만료 시간만 설정)
        String refreshToken = Jwts.builder()
                .setExpiration(new Date(now + 86400000)) // 만료 시간 설정 (24시간)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        // 생성된 토큰들을 JwtToken DTO에 담아 반환
        return JwtToken.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    //* Access Token 문자열을 받아 인증 객체를 반환함 */
    public Authentication getAuthentication(String accessToken) {
        // 토큰을 복호화해서 클레임(토큰에 담긴 정보)를 추출함
        Claims claims = parseClaims(accessToken);

        if (claims.get("auth") == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }

        // 클레임에서 권한 정보를 추출해서 Spring Security가 이해할 수 있는 GrantedAuthority 컬렉션으로 반환함
        Collection<? extends GrantedAuthority> authorities = Arrays.stream(claims.get("auth").toString().split(","))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        // 클레임의 subject를 사용해서 UserDetails 객체(사용자 정보)를 생성함
        UserDetails principal = new User(claims.getSubject(), "", authorities);

        // UserDetails, 권한 정보를 기반으로 최종 인증 객체를 생성해서 반환함
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    //* 토큰 문자열의 유효성을 검증함 */
    public boolean validateToken(String token) {
        try {
            // 준비된 key를 사용해서 토큰을 파싱(해석)함
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true; // 성공하면 true 반환
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("Invalid JWT Token", e); // 잘못된 JWT 서명 또는 형식일 경우
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT Token", e); // 토큰이 만료된 경우
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT Token", e); // 지원하지 않는 JWT 토큰인 경우
        } catch (IllegalArgumentException e) {
            log.info("JWT claims string is empty.", e); // JWT 클레임 문자열이 비어있는 경우
        }
        return false; // 실패하면 false 반환
    }

    //* Access Token에서 클레임 정보를 추출하는 private 헬퍼 메소드 */
    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(accessToken).getBody();
        } catch (ExpiredJwtException e) {
            // 토큰이 만료되었더라도 클레임 정보는 반환하도록 처리함
            return e.getClaims();
        }
    }
}