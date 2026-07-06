package com.portfolio.memo.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

// memo-auth의 배치 조회 API(/api/auth/users/batch) 응답 파싱용
@Getter
@NoArgsConstructor
public class UserSummaryDto {
    private Long id;
    private String name;
}