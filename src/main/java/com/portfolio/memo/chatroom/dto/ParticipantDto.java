package com.portfolio.memo.chatroom.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class ParticipantDto {

    private Long userId;
    private String username;

    @Builder
    public ParticipantDto(Long userId, String username) {
        this.userId = userId;
        this.username = username;
    }
}
