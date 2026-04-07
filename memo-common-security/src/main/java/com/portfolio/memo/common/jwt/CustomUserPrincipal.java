package com.portfolio.memo.common.jwt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Getter
@AllArgsConstructor
public class CustomUserPrincipal implements UserDetails {

    private final Long userId;
    private final String userName;
    private final String role;
    private final Collection<? extends GrantedAuthority> authorities;

    @Override
    public String getUsername() {
        return userName;
    }

    @Override
    public String getPassword() {
        return null; // JWT 기반 로그인, password 불필요
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
