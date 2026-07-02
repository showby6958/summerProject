package com.portfolio.memo.support;

import com.portfolio.memo.common.jwt.CustomUserPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

public class TestPrincipals {
    private TestPrincipals() {}

    public static CustomUserPrincipal user(Long userId, String username) {
        // CustomUserPrincipal 구조와 동일하게 설정
        String role = "USER";
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));

        return new CustomUserPrincipal(userId, username, role, authorities);
    }
}
