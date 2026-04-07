package com.portfolio.memo.chat.participant;

import lombok.Builder;
import lombok.Getter;

@Getter
public class ParticipantDto {

    private final Long userId;
    private final String userName;

    @Builder
    public ParticipantDto(Long userId, String userName) {
        this.userId = userId;
        this.userName = userName;
    }
}
