package com.portfolio.memo.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
// 현재 인증된 유저의 정보를 반환하는 DTO
public class UserInfoDto {
    private Long userId;
    private String email;
}