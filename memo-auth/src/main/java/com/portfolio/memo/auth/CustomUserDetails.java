package com.portfolio.memo.auth;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

// 로그인 성공 후 보관되는 사용자 정보 (Principal)
@Getter
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    // 사용자 권한(Role) 목록을 반환
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // User 엔티티의 Role 정보를 Spring Security가 이해할 수 있는 GrantedAuthority 형태로 변환
        return Collections.singleton(new SimpleGrantedAuthority(user.getRole().name()));
    }

    // 사용자 비밀번호 반환 (return 암호화된 비밀번호)
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    // 사용자의 고유 식별자(ID) 반환 (이메일)
    @Override
    public String getUsername() {
        return user.getEmail();
    }


    // --------------------계정의 상태 메서드-------------------------

    // 계정이 만료되지 않았는지 여부 반환
    @Override
    public boolean isAccountNonExpired() {
        return true; // true 계정이 만료되지 않음
    }

    // 계정이 잠기지 않았는지 여부 반환
    @Override
    public boolean isAccountNonLocked() {
        return true; // true 계정이 잠기지 않음
    }

    // 자격증명(비밀번호)이 만료되지 않았는지 여부 반환
    @Override
    public boolean isCredentialsNonExpired() {
        return true; // true 자격 증명이 만료되지 않음
    }

    // 계정이 활성화되어 있는지 여부 반환
    @Override
    public boolean isEnabled() {
        return true; // true 계정이 활성화됨
    }
}
