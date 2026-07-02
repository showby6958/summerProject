package com.portfolio.memo.chat.message.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 메시지 전송 요청 DTO
@Getter
@Setter
@NoArgsConstructor
public class SendMessageRequest {
    private String content;
}
