package com.portfolio.memo.chatroom.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class ChatMessageDto {
    private Long roomId;
    private String sender;
    private String message;
    private String sentAt;
}
