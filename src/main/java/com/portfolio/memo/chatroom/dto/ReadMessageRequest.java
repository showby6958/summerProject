package com.portfolio.memo.chatroom.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
// 클라이언트가 읽음 요청 시 보낼 데이터
public class ReadMessageRequest {
    private Long messageId;
    private Long roomId;
}
