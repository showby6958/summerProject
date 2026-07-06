package com.portfolio.memo.auth.dto;

import com.portfolio.memo.auth.User;
import lombok.Builder;
import lombok.Getter;

// 다른 서비스에서 여러 유저를 한 번에 조회할 때 사용하는 DTO
@Getter
@Builder
public class UserSummaryResponse {
    private Long id;
    private String name;

    public static UserSummaryResponse from(User user) {
        return UserSummaryResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .build();
    }
}
