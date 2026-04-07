package com.portfolio.memo.chat.participant;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class InviteRequest {
    private List<Long> userIds;
}
