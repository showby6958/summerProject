package com.portfolio.memo.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignMemberRequestDto {
    // -- TaskController.assignMembers() -> taskService.assignMembers()로 전달되는 입력 데이터 --

    private List<Long> memberIds;
}
